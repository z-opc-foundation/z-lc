import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { PivotView } from '@/lc/views/workspace/PivotView';
import type { EntityDefDTO, FieldDefDTO } from '@/lc/api/types';

/**
 * 交叉表视图的行为测试。
 *
 * 钉住五件事：维度没选齐就不发请求（否则画出一张不是交叉表的表）、一次挂载只发一次
 * 请求、空格子不能显示成 0、读失败不能读成"没有数据"、后端结构不合预期时要说"结构不对"。
 * 后两条是这个仓反复出现的那一类谎。
 */

const APP = 'crm';
const ENT = 'deal';

function entityDef(): EntityDefDTO {
  const fields = [
    { fieldCode: 'name', fieldName: '商机名', fieldType: 'STRING', required: true, sortOrder: 1 },
    { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', sortOrder: 2 },
    { fieldCode: 'amount', fieldName: '金额', fieldType: 'DECIMAL', sortOrder: 3 },
  ] as unknown as FieldDefDTO[];
  return {
    id: 3, entityCode: ENT, entityName: '商机', tableName: 't_deal',
    tenantCode: 'default', appCode: APP, fields,
  } as unknown as EntityDefDTO;
}

interface Call { url: string; body: Record<string, unknown>; }
const calls: Call[] = [];

const OK = { success: true, code: 200, message: null, data: null };
const MATRIX = [
  { group_key: 'OK', group_label: '已完成', 甲: 10, 乙: null },
  { group_key: 'NEW', group_label: '新建', 甲: 4, 乙: 6 },
];

let payload: unknown = MATRIX;

function stubBackend(fail = false) {
  calls.length = 0;
  const text = (body: unknown) => {
    const raw = JSON.stringify(body);
    return { ok: !fail, status: fail ? 500 : 200, text: async () => raw, json: async () => body };
  };
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    if (url.includes('/runtime/shape')) {
      return fail ? text({ success: false, code: 500, message: '后端 500', data: null }) : text({ ...OK, data: payload });
    }
    return text({ success: true, code: 200, message: null, data: [] });
  }));
}

function seedConfig(config: Record<string, unknown>): void {
  window.localStorage.setItem(`zlc:state:${APP}:${ENT}:PIVOT`, JSON.stringify(config));
}

function renderView() {
  const entity = entityDef();
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [entity] });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <PivotView
        entity={entity}
        resolvedFields={resolveEntityFields(entity, ws)}
        views={[]}
        appCode={APP}
        tenantCode="default"
        conditions={[]}
        conjunction="AND"
      />
    </QueryClientProvider>,
  );
}

function shapeCalls() {
  return calls.filter((call) => call.url.includes('/runtime/shape'));
}

/** 取第 n 次 shape 请求的 body。少了这一层，"请求根本没发"会以 undefined 的形式被 expect 吃掉。 */
function shapeBody(index: number): Record<string, unknown> {
  const call = shapeCalls()[index];
  if (!call) throw new Error(`只有 ${shapeCalls().length} 次 shape 请求，拿不到第 ${index + 1} 次的请求体`);
  return call.body;
}

function pickOption(container: HTMLElement, className: string, title: string) {
  const select = container.querySelector(`.${className}`) as HTMLElement;
  if (!select) throw new Error(`没有 ${className} 这个下拉框`);
  fireEvent.mouseDown(select.querySelector('.ant-select-content') as Element);
  const option = document.querySelector(
    `.ant-select-dropdown:not(.ant-select-dropdown-hidden) [title="${title}"]`,
  ) as HTMLElement;
  if (!option) throw new Error(`下拉里没有「${title}」`);
  fireEvent.click(option);
}

beforeEach(() => {
  window.localStorage.clear();
  payload = MATRIX;
  stubBackend();
});

describe('PivotView', () => {
  it('只选了行维度时不发请求，并说清还缺什么', () => {
    const { container } = renderView();
    expect(shapeCalls()).toHaveLength(0);
    expect(container.querySelector('.zlc-pivot-incomplete')?.textContent).toContain('还没选列维度');
  });

  it('选齐两个维度后发一次请求：行维度在前、程序按 group_count 数记录', async () => {
    const { container } = renderView();
    pickOption(container, 'zlc-pivot-col', '阶段');

    await waitFor(() => expect(shapeCalls()).toHaveLength(1));
    const body = shapeBody(0);
    expect(body.groupFields).toEqual(['name', 'stage']);
    // COUNT 指标不需要后端再算任何聚合列，度量来自分组自带的 group_count
    expect(body.aggregations).toEqual({});
    const [step] = body.shape as { op: string; agg: string; value: string; on: string }[];
    expect(step).toMatchObject({ op: 'pivot', agg: 'SUM', value: 'group_count', on: 'group_label_2' });
    // 等待渲染完成之后再数一次：重查循环正是在"已经画出来了"之后才多发的那几条
    await waitFor(() => expect(container.querySelectorAll('.zlc-pivot-table tbody tr').length).toBeGreaterThan(0));
    expect(shapeCalls()).toHaveLength(1);
  });

  it('空格子显示成 ·，不让它冒充 0', async () => {
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(container.querySelectorAll('.zlc-pivot-table tbody tr')).toHaveLength(2));
    const empties = container.querySelectorAll('.zlc-pivot-table td.zlc-pivot-empty');
    expect(empties).toHaveLength(1);
    expect(empties[0]?.textContent).toBe('·');
    // 同一张表里不许出现把空格子写成 0 的单元格
    expect([...container.querySelectorAll('.zlc-pivot-table td.zlc-pivot-cell')]
      .filter((node) => node.textContent === '0')).toHaveLength(0);
  });

  it('行头列显示字典标签而不是裸编码', async () => {
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(container.querySelectorAll('.zlc-pivot-table tbody tr')).toHaveLength(2));
    const heads = [...container.querySelectorAll('.zlc-pivot-table tbody th')].map((node) => node.textContent);
    // 两档行合计相同 (10)，先后由标签的字典序定 —— 这里要钉的是"显示标签不是显示编码"，
    // 顺序由上面的排序用例负责，不在此处重复断言（重复断言会在改排序时以无关的理由变红）
    expect([...new Set(heads)].sort()).toEqual(['已完成', '新建'].sort());
    expect(heads).not.toContain('OK');
    expect(heads).not.toContain('NEW');
  });

  it('第二维为空的那一列有标题，且它的数落在自己那一列下面', async () => {
    // 后端 pivot 直接拿第二维的值当列键，该维为 NULL 时键就是空串（18090 实测）。
    // 原样画出去是一根没有任何标题的列 —— 只断言"表头里有字"不够，还要证明格子没跟着挪位。
    payload = [
      { group_key: 'OK', group_label: '已完成', '': 3, 甲: 10 },
      { group_key: 'NEW', group_label: '新建', '': 5, 甲: 4 },
    ];
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(container.querySelectorAll('.zlc-pivot-table tbody tr')).toHaveLength(2));
    const heads = [...container.querySelectorAll('.zlc-pivot-table thead th')];
    expect(heads[1]?.textContent).toBe('（未填写）');
    expect(heads[1]?.getAttribute('data-col-key')).toBe('');
    const cells = [...(container.querySelector('.zlc-pivot-table tbody tr')?.querySelectorAll('td') ?? [])];
    expect(cells.map((node) => node.textContent)).toEqual(['3', '10', '13']);
  });

  it('读失败时显示错误，不把故障说成"没有可透视的记录"', async () => {
    stubBackend(true);
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(shapeCalls()).toHaveLength(1));
    await waitFor(() => expect(container.textContent).toMatch(/500|失败|错误|Error/i));
    expect(container.textContent).not.toContain('当前筛选条件下没有可透视的记录');
  });

  it('后端结构不合预期时说"结构"问题，不说成空表', async () => {
    payload = { group_key: 'OK', group_label: '已完成' };
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(shapeCalls()).toHaveLength(1));
    await waitFor(() => expect(container.textContent).toContain('整形结果不是行数组'));
    expect(container.textContent).not.toContain('当前筛选条件下没有可透视的记录');
  });

  it('折叠掉的行仍在合计里：出现"其余 N 行"，总计不缩水', async () => {
    payload = [
      { group_key: 'A', group_label: '甲', x: 10 },
      { group_key: 'B', group_label: '乙', x: 7 },
      { group_key: 'C', group_label: '丙', x: 3 },
    ];
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 2 });
    const { container } = renderView();
    await waitFor(() => expect(container.querySelector('.zlc-pivot-rest')).toBeTruthy());
    const rows = [...container.querySelectorAll('.zlc-pivot-table tbody tr')];
    expect(rows).toHaveLength(3);
    expect(container.querySelector('.zlc-pivot-rest')?.textContent).toContain('其余 1 行');
    const foot = container.querySelector('.zlc-pivot-coltotals')?.textContent ?? '';
    expect(foot).toContain('20');
    expect(container.querySelector('.zlc-pivot-grandtotal')?.textContent).toBe('20');
  });

  it('平均数不显示合计，并说明为什么', async () => {
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'AVG', metricField: 'amount', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(shapeCalls()).toHaveLength(1));
    expect(shapeBody(0).aggregations).toEqual({ amount: ['AVG'] });
    await waitFor(() => expect(container.querySelectorAll('.zlc-pivot-table tbody tr')).toHaveLength(2));
    expect(container.querySelector('.zlc-pivot-coltotals')).toBeNull();
    expect(container.querySelector('.zlc-pivot-totalhead')).toBeNull();
    expect(container.querySelector('.zlc-pivot-no-total')?.textContent).toContain('平均数不横向相加');
  });

  it('切指标到 SUM 时度量列进了请求，页面副标题跟着口径走', async () => {
    seedConfig({ rowField: 'stage', colField: 'name', metricFn: 'COUNT', maxRows: 20 });
    const { container } = renderView();
    await waitFor(() => expect(shapeCalls()).toHaveLength(1));
    const caption = () => container.querySelector('.zlc-pivot-caption')?.textContent ?? '';
    expect(caption()).toContain('记录数');
    pickOption(container, 'zlc-pivot-fn', '合计');
    await waitFor(() => expect(shapeCalls()).toHaveLength(2));
    const body = shapeBody(1);
    expect(body.aggregations).toEqual({ amount: ['SUM'] });
    const [step] = body.shape as { value: string }[];
    expect(step?.value).toBe('sum_amount');
    // 口径文案必须换成切换后才有的字样：断"阶段"在点之前也是绿的，等于什么都没断
    await waitFor(() => expect(caption()).toContain('金额 合计'));
    expect(caption()).not.toContain('记录数');
  });
});
