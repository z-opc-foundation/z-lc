import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { createWorkspaceContext, resolveEntityFields } from '@/fields';
import { GalleryView } from '@/views/workspace/GalleryView';
import { CalendarView } from '@/views/workspace/CalendarView';
import { UndoHistoryDrawer } from '@/views/workspace/UndoHistoryDrawer';
import type { EntityDefDTO, FieldDefDTO, QueryCondition } from '@/api/types';

/**
 * 同一批记录，三个视图不能各说各话。
 *
 * 表格早就把"读失败"和"读到空"分成两句话了（StateBlock），但画廊/日历/变更历史没接：
 * 接口 500 之后画廊亮"暂无数据"、日历摆 42 个空格子、变更历史说"这个实体还没有数据变更"
 * —— 全是把"我没读到"讲成"库里没有"。这里钉的就是这两句话必须分开，外加"重试要真的再发一次请求"。
 *
 * 最后一条是写这批测试时撞出来的另一个缺陷：日历的 monthStart 每轮渲染都造新 dayjs 对象，
 * 带着 monthConditions、loadPage 一起换身份，useEffect 就无限重发当月查询 —— 挂载后 300ms
 * 内实测 43 次 /runtime/list（画廊 1 次）。所以这里也钉住"一次挂载只查一次"。
 */

const APP = 'crm';
const ENT = 'deal';

function fieldDefs(): FieldDefDTO[] {
  return [
    { fieldCode: 'name', fieldName: '商机名', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
    { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', fieldLength: 16, sortOrder: 2 },
    { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 3 },
  ] as FieldDefDTO[];
}

function entityDef(): EntityDefDTO {
  return {
    id: 3, entityCode: ENT, entityName: '商机', tableName: 't_deal',
    tenantCode: 'default', appCode: APP, fields: fieldDefs(),
  } as EntityDefDTO;
}

function resolvedFields() {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  return resolveEntityFields(entityDef(), ws);
}

/** 落在当月的两天：日历要能把贴片摆进对应的格子。 */
const DAY_A = dayjs().startOf('month').add(2, 'day');
const DAY_B = dayjs().startOf('month').add(3, 'day');
const RECORDS = [
  { id: 11, name: '甲商机', stage: 'NEW', stage_label: '新建', due: DAY_A.format('YYYY-MM-DD') },
  { id: 12, name: '乙商机', stage: 'NEW', stage_label: '新建', due: DAY_A.format('YYYY-MM-DD') },
  { id: 13, name: '丙商机', stage: 'DONE', stage_label: '已完成', due: DAY_B.format('YYYY-MM-DD') },
];

const CHANGE = {
  id: 5,
  entityCode: ENT,
  recordId: 12,
  operation: 'UPDATE',
  actor: 'zifang',
  createTime: '2026-09-01 10:00:00',
  undone: false,
  beforeImage: '{"amount":1}',
  afterImage: '{"amount":2}',
};

type Mode = 'ok' | 'fail' | 'empty';
const modes: { list: Mode; history: Mode } = { list: 'ok', history: 'ok' };
const requestLog: string[] = [];
const bodies: Record<string, unknown>[] = [];

function respond(payload: unknown) {
  const text = JSON.stringify(payload);
  // client.ts 读的是 response.text()，不是 json()
  return { ok: true, status: 200, text: async () => text, json: async () => JSON.parse(text) };
}

function listPayload() {
  if (modes.list === 'fail') {
    return { success: false, code: 500, message: '记录接口炸了', data: null };
  }
  const rows = modes.list === 'empty' ? [] : RECORDS;
  return {
    success: true, code: 200, message: null,
    data: { records: rows, total: rows.length, pageNum: 1, pageSize: 12 },
  };
}

function historyPayload() {
  if (modes.history === 'fail') {
    return { success: false, code: 502, message: '历史接口炸了', data: null };
  }
  const rows = modes.history === 'empty' ? [] : [CHANGE];
  return { success: true, code: 200, message: null, data: rows };
}

const listCalls = () => requestLog.filter((url) => url.includes('/runtime/list')).length;
const historyCalls = () => requestLog.filter((url) => url.includes('/undo/history')).length;

/** 最后一次 /runtime/list 的请求体：断言"重新查询用的是新条件"要落到真实 payload 上才算数。 */
function lastListBody(): Record<string, unknown> | undefined {
  return bodies[bodies.length - 1];
}

function stubBackend() {
  requestLog.length = 0;
  bodies.length = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    requestLog.push(url);
    if (url.includes('/runtime/list')) {
      if (init?.body) bodies.push(JSON.parse(String(init.body)) as Record<string, unknown>);
    }
    // 让出一个宏任务再答复：视图若在 microtask 上自旋重查，测试计时器就永远轮不到，
    // 那时"红"会变成"整个文件挂住"，看不出是谁的错。
    await new Promise((resolve) => setTimeout(resolve, 0));
    if (url.includes('/runtime/list')) return respond(listPayload());
    if (url.includes('/undo/history')) return respond(historyPayload());
    return respond({ success: true, code: 200, message: null, data: null });
  }));
}

/** 每次调用都造一棵新树；rerender 时换 conditions 用同一棵，React 才会当成同一个组件实例。 */
function galleryElement(over: { conditions?: QueryCondition[] } = {}): ReactElement {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <GalleryView
        entity={entityDef()}
        resolvedFields={resolvedFields()}
        appCode={APP}
        tenantCode="default"
        conditions={over.conditions ?? []}
        conjunction="AND"
        onOpenRecord={() => undefined}
        onCreateRecord={() => undefined}
      />
    </QueryClientProvider>
  );
}

function calendarElement(): ReactElement {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <CalendarView
        entity={entityDef()}
        resolvedFields={resolvedFields()}
        appCode={APP}
        tenantCode="default"
        conditions={[]}
        conjunction="AND"
        onOpenRecord={() => undefined}
        onCreateRecord={() => undefined}
      />
    </QueryClientProvider>
  );
}

function historyElement(): ReactElement {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const node: ReactNode = (
    <UndoHistoryDrawer
      open
      onClose={() => undefined}
      appCode={APP}
      entityCode={ENT}
      tenantCode="default"
      resolvedFields={resolvedFields()}
      onUndone={() => undefined}
    />
  );
  return <QueryClientProvider client={client}>{node}</QueryClientProvider>;
}

/** antd 会在两个汉字的按钮里插空格，所以按去掉空白后的文本找，而不是 '重试' 字面量。 */
function clickRetry(where: ParentNode) {
  const button = Array.from(where.querySelectorAll('button'))
    .find((node) => (node.textContent ?? '').replace(/\s/g, '') === '重试');
  if (!button) throw new Error('错误块里没有可点的重试按钮');
  expect(button).toBeEnabled();
  fireEvent.click(button);
}

function errorAlert(text: string): HTMLElement {
  const banner = screen.getByText(text);
  const alert = banner.closest('.ant-alert');
  if (!alert) throw new Error(`「${text}」不在错误块里`);
  return alert as HTMLElement;
}

/** 翻页箭头是图标按钮（jsdom 里 Tooltip 给不出可访问名），按位置取第二个。 */
function nextMonthButton(where: ParentNode): HTMLElement {
  const arrows = Array.from(where.querySelectorAll('button.ant-btn-icon-only'));
  expect(arrows).toHaveLength(2);
  return arrows[1] as HTMLElement;
}

/** 给"自己转圈重查"留一个可观测的窗口：安静下来的页面在这段里不该再多发一次。 */
const settle = () => new Promise((resolve) => setTimeout(resolve, 250));

beforeEach(() => {
  window.localStorage.clear();
  modes.list = 'ok';
  modes.history = 'ok';
  stubBackend();
});

describe('GalleryView：读失败不是一句"暂无数据"', () => {
  it('记录接口失败时说"记录没有读到"，不许说"还没有记录"', async () => {
    modes.list = 'fail';
    const { container } = render(galleryElement());
    expect(await screen.findByText('记录没有读到')).toBeTruthy();
    expect(screen.getByText('记录接口炸了')).toBeTruthy();
    expect(container.querySelectorAll('.ant-card')).toHaveLength(0);
    expect(screen.queryByText(/还没有记录/)).toBeNull();
    expect(screen.queryByText('暂无数据')).toBeNull();
  });

  it('读到空数组才说"还没有记录"', async () => {
    modes.list = 'empty';
    render(galleryElement());
    expect(await screen.findByText(/还没有记录/)).toBeTruthy();
    expect(screen.queryByText(/没有读到/)).toBeNull();
  });

  it('读到记录就画卡片，一句谎都不说', async () => {
    const { container } = render(galleryElement());
    await waitFor(() => expect(container.querySelectorAll('.ant-card')).toHaveLength(3));
    expect(screen.queryByText(/没有读到/)).toBeNull();
    expect(screen.queryByText(/还没有记录/)).toBeNull();
    expect(container.textContent).toContain('丙商机');
  });

  it('横幅里的重试要真的再发一次 /runtime/list 并把卡片画回来', async () => {
    modes.list = 'fail';
    const { container } = render(galleryElement());
    await screen.findByText('记录没有读到');
    const alert = errorAlert('记录没有读到');

    modes.list = 'ok';
    const before = listCalls();
    clickRetry(alert);
    await waitFor(() => expect(container.querySelectorAll('.ant-card')).toHaveLength(3));
    expect(listCalls()).toBeGreaterThan(before);
    expect(container.querySelector('.ant-alert'), '重试成功后错误块该收走').toBeNull();
  });

  it('换筛选后读失败，旧卡片不许留在页面上装作还在', async () => {
    const first = render(galleryElement());
    await waitFor(() => expect(first.container.querySelectorAll('.ant-card')).toHaveLength(3));

    modes.list = 'fail';
    // 改 conditions 是真实用户动作（应用一条筛选），它会带着新的 conditions 重新查询
    first.rerender(galleryElement({ conditions: [{ fieldCode: 'name', operator: 'like', value: '甲' }] }));
    await screen.findByText('记录没有读到');
    expect(first.container.querySelectorAll('.ant-card')).toHaveLength(0);
    expect(screen.queryByText(/还没有记录/)).toBeNull();
    // 重新查询用的是新条件，不是第一次那批
    expect(lastListBody()?.conditions).toEqual([{ fieldCode: 'name', operator: 'like', value: '甲' }]);
  });
});

describe('CalendarView：挂掉的月份不能长得像空的月份', () => {
  it('记录接口失败时说"本月记录没有读到"，整月空格子不许留在这里', async () => {
    modes.list = 'fail';
    const { container } = render(calendarElement());
    expect(await screen.findByText('本月记录没有读到')).toBeTruthy();
    expect(screen.getByText('记录接口炸了')).toBeTruthy();
    expect(container.querySelectorAll('.zlc-cal-cell')).toHaveLength(0);
  });

  it('读到空月份要照画整月网格，不算失败', async () => {
    modes.list = 'empty';
    const { container } = render(calendarElement());
    await waitFor(() => expect(container.querySelectorAll('.zlc-cal-cell').length).toBe(42));
    expect(screen.queryByText(/没有读到/)).toBeNull();
    expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(0);
  });

  it('读到记录要把贴片摆进各自当天的格子', async () => {
    const { container } = render(calendarElement());
    await waitFor(() => expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(3));
    const dayA = container.querySelector(`[data-date="${DAY_A.format('YYYY-MM-DD')}"]`);
    const dayB = container.querySelector(`[data-date="${DAY_B.format('YYYY-MM-DD')}"]`);
    expect(dayA?.querySelectorAll('.zlc-cal-chip')).toHaveLength(2);
    expect(dayB?.querySelectorAll('.zlc-cal-chip')).toHaveLength(1);
    expect(dayA?.textContent).toContain('甲商机');
  });

  it('换到读不到的月份，旧月的贴片不许继续摆在上面', async () => {
    const { container } = render(calendarElement());
    await waitFor(() => expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(3));

    modes.list = 'fail';
    fireEvent.click(nextMonthButton(container));

    await screen.findByText('本月记录没有读到');
    expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(0);
    expect(container.querySelectorAll('.zlc-cal-cell')).toHaveLength(0);
  });

  it('横幅里的重试要真的再发一次当月查询', async () => {
    modes.list = 'fail';
    const { container } = render(calendarElement());
    await screen.findByText('本月记录没有读到');
    const alert = errorAlert('本月记录没有读到');

    modes.list = 'ok';
    const before = listCalls();
    clickRetry(alert);
    await waitFor(() => expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(3));
    expect(listCalls()).toBeGreaterThan(before);
    expect(container.querySelector('.ant-alert'), '重试成功后错误块该收走').toBeNull();
  });
});

describe('UndoHistoryDrawer：没查到不是"没有改动"', () => {
  it('历史接口失败时说"变更记录没有读到"，不许说"这个实体还没有数据变更"', async () => {
    modes.history = 'fail';
    render(historyElement());
    expect(await screen.findByText('变更记录没有读到')).toBeTruthy();
    expect(screen.getByText('历史接口炸了')).toBeTruthy();
    expect(screen.queryByText('这个实体还没有数据变更')).toBeNull();
  });

  it('读到空历史才说"这个实体还没有数据变更"', async () => {
    modes.history = 'empty';
    render(historyElement());
    expect(await screen.findByText('这个实体还没有数据变更')).toBeTruthy();
    expect(screen.queryByText(/没有读到/)).toBeNull();
  });

  it('读到条目就列出来，一句谎都不说', async () => {
    render(historyElement());
    expect(await screen.findByText('记录 #12')).toBeTruthy();
    expect(screen.queryByText('这个实体还没有数据变更')).toBeNull();
    expect(screen.queryByText(/没有读到/)).toBeNull();
  });

  it('面板里的重试要真的再发一次 /undo/history', async () => {
    modes.history = 'fail';
    render(historyElement());
    await screen.findByText('变更记录没有读到');
    const alert = errorAlert('变更记录没有读到');

    modes.history = 'ok';
    const before = historyCalls();
    clickRetry(alert);
    expect(await screen.findByText('记录 #12')).toBeTruthy();
    expect(historyCalls()).toBeGreaterThan(before);
    expect(screen.queryByText('变更记录没有读到')).toBeNull();
  });
});

describe('一次挂载只查一次（日历曾经 300ms 内发了 43 次当月查询）', () => {
  it('日历安静下来后不再自己重查，换月只补一次', async () => {
    const { container } = render(calendarElement());
    await waitFor(() => expect(container.querySelectorAll('.zlc-cal-chip')).toHaveLength(3));
    await settle();
    expect(listCalls(), '挂载之后不该再有第二次当月查询').toBe(1);

    fireEvent.click(nextMonthButton(container));
    await waitFor(() => expect(listCalls()).toBe(2));
    await settle();
    expect(listCalls(), '换月只该补一次查询').toBe(2);
  });

  it('画廊安静下来后不再自己重查，换筛选只补一次', async () => {
    const first = render(galleryElement());
    await waitFor(() => expect(first.container.querySelectorAll('.ant-card')).toHaveLength(3));
    await settle();
    expect(listCalls(), '挂载之后不该再有第二次查询').toBe(1);

    first.rerender(galleryElement({ conditions: [{ fieldCode: 'name', operator: 'like', value: '甲' }] }));
    await waitFor(() => expect(listCalls()).toBe(2));
    await settle();
    expect(listCalls(), '换筛选只该补一次查询').toBe(2);
  });
});
