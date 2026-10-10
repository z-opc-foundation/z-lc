import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RelationsPage } from '@/lc/views/admin/RelationsPage';
import { DictsPage } from '@/lc/views/admin/DictsPage';
import { useResourceList } from '@/lc/views/admin/_scope';
import type { RelationDTO } from '@/lc/api/types';

/**
 * 管理页的"读到了什么"口径。
 *
 * 钉住的都是"页面看着像在正常工作"的错法：接口失败时表格显示 antd 默认的「暂无数据」，
 * 于是排查的人去建数据；失败的刷新把上一批行留在原地，界面看着比失败前还健康；
 * `success:true` + `data:null` 被当成空列表；切应用时慢一步的旧响应盖掉新应用的结果。
 */

/** 管理页挂在 useSearchParams 上，useWorkspace 挂在 react-query 上，两个都要有。 */
function renderPage(node: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>{node}</QueryClientProvider>
    </MemoryRouter>,
  );
}

const APP = 'crm';

function relation(id: number, code: string): RelationDTO {
  return {
    id,
    relationCode: code,
    relationName: code,
    sourceEntityCode: 'deal',
    targetEntityCode: 'lead',
    relationType: 'ONE_TO_MANY',
    appCode: APP,
    tenantCode: 'default',
  } as RelationDTO;
}

type Mode = 'ok' | 'fail' | 'empty' | 'null';

const modes: Record<string, Mode> = {};
const requestLog: string[] = [];

function envelope(data: unknown) {
  return { success: true, code: 200, message: null, data };
}

function respond(payload: unknown) {
  const body = JSON.stringify(payload);
  return { ok: true, status: 200, text: async () => body, json: async () => payload };
}

function fail(msg: string) {
  return { ok: false, status: 500, text: async () => JSON.stringify({ success: false, code: 500, message: msg, data: null }) };
}

/** 每个接口按 `modes[key]` 决定答什么，测试中途改一下就能模拟"刷新时后端挂了"。 */
function stubBackend() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      requestLog.push(url);
      const mode = (key: string): Mode => modes[key] ?? 'ok';
      if (url.includes('/app/list')) {
        if (mode('app') === 'fail') return fail('应用接口炸了');
        return respond(envelope([{ appCode: APP, appName: 'CRM', tenantCode: 'default' }]));
      }
      if (url.includes('/meta/bundle')) {
        return respond(
          envelope({
            app: { appCode: APP, appName: 'CRM' },
            entities: [{ entityCode: 'deal', entityName: '商机', appCode: APP }],
            dicts: [],
            views: [],
            fieldTypes: [],
          }),
        );
      }
      if (url.includes('/meta/field-types')) return respond(envelope([]));
      if (url.includes('/relation/list')) {
        const current = mode('relation');
        if (current === 'fail') return fail('关系接口炸了');
        if (current === 'null') return respond(envelope(null));
        if (current === 'empty') return respond(envelope([]));
        return respond(envelope([relation(1, 'deal_lead'), relation(2, 'deal_task')]));
      }
      if (url.includes('/dict/items')) return respond(envelope([]));
      if (url.includes('/dict/list')) {
        const current = mode('dict');
        if (current === 'fail') return fail('字典接口炸了');
        return respond(envelope([{ id: 7, dictCode: 'stage', dictName: '阶段', appCode: APP }]));
      }
      return respond(envelope([]));
    }),
  );
}

function tableRowCount(): number {
  return document.querySelectorAll('.ant-table-tbody tr[data-row-key]').length;
}

/** 顶栏那个刷新按钮只有图标，取不到 accessible name，直接用 DOM。 */
function clickToolbarRefresh() {
  const icon = document.querySelector('.anticon-reload');
  const button = icon?.closest('button');
  if (!button) throw new Error('没找到刷新按钮');
  fireEvent.click(button);
}

/** antd 会把两个汉字的按钮文案渲染成「重 试」，比对前先去掉空白。 */
function clickButtonByText(root: Element, label: string) {
  const button = Array.from(root.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === label,
  );
  if (!button) throw new Error(`没找到「${label}」按钮`);
  fireEvent.click(button);
}

beforeEach(() => {
  Object.keys(modes).forEach((key) => delete modes[key]);
  requestLog.length = 0;
  stubBackend();
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

describe('useResourceList：读到 / 没读到 / 真没有', () => {
  it('失败要清空 rows 并报 error，不能留下"看着健康"的旧数据', async () => {
    modes.relation = 'ok';
    const { result } = renderHook(() =>
      useResourceList<RelationDTO>(() => {
        if (modes.relation === 'fail') return Promise.reject(new Error('关系接口炸了'));
        return Promise.resolve([relation(1, 'deal_lead')]);
      }, 'crm'),
    );

    await waitFor(() => expect(result.current.state).toBe('ready'));
    expect(result.current.rows).toHaveLength(1);

    modes.relation = 'fail';
    result.current.reload();
    await waitFor(() => expect(result.current.state).toBe('error'));
    expect(result.current.rows).toEqual([]);
  });

  it('success:true 但 data:null 是没读到，不是空列表', async () => {
    const { result } = renderHook(() =>
      useResourceList<RelationDTO>(() => Promise.resolve(null as unknown as RelationDTO[]), 'crm'),
    );
    await waitFor(() => expect(result.current.state).toBe('error'));
    expect(result.current.error).toBeTruthy();
  });

  it('读到空数组才是 empty，和 error 是两种状态', async () => {
    const { result } = renderHook(() =>
      useResourceList<RelationDTO>(() => Promise.resolve([]), 'crm'),
    );
    await waitFor(() => expect(result.current.state).toBe('empty'));
    expect(result.current.error).toBeNull();
  });

  it('没有作用域时是 idle，既不是空也不是错', async () => {
    const { result } = renderHook(() =>
      useResourceList<RelationDTO>(() => Promise.resolve([relation(1, 'deal_lead')]), ''),
    );
    await waitFor(() => expect(result.current.state).toBe('idle'));
    expect(result.current.rows).toEqual([]);
  });

  it('换应用后，慢一步到达的旧响应不能盖掉新应用的结果', async () => {
    let resolveSlow: (rows: RelationDTO[]) => void = () => {};
    const slow = new Promise<RelationDTO[]>((resolve) => {
      resolveSlow = resolve;
    });
    const fast = Promise.resolve([relation(9, 'b_only')]);
    const { result, rerender } = renderHook(
      ({ scope }: { scope: string }) =>
        useResourceList<RelationDTO>(() => (scope === 'a' ? slow : fast), scope),
      { initialProps: { scope: 'a' } },
    );

    rerender({ scope: 'b' });
    await waitFor(() => expect(result.current.rows).toEqual([relation(9, 'b_only')]));
    resolveSlow([relation(1, 'deal_lead')]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(result.current.rows).toEqual([relation(9, 'b_only')]);
    expect(result.current.state).toBe('ready');
  });
});

describe('RelationsPage：三句话要分得开', () => {
  it('接口失败时说"没有读到"，不许说"该应用还没有关系定义"', async () => {
    modes.relation = 'fail';
    renderPage(<RelationsPage />);
    expect(await screen.findByText(/实体关系没有读到/)).toBeTruthy();
    expect(screen.getByText('关系接口炸了')).toBeTruthy();
    expect(screen.queryByText(/该应用还没有关系定义/)).toBeNull();
    expect(screen.queryByText('暂无数据')).toBeNull();
  });

  it('读到空数组时才说"还没有"', async () => {
    modes.relation = 'empty';
    renderPage(<RelationsPage />);
    expect(await screen.findByText('该应用还没有关系定义')).toBeTruthy();
    expect(screen.queryByText(/没有读到/)).toBeNull();
  });

  it('读到数据就正常列行；之后刷新失败要清行并报错', async () => {
    renderPage(<RelationsPage />);
    await waitFor(() => expect(tableRowCount()).toBe(2));
    expect(screen.queryByText(/没有读到/)).toBeNull();

    modes.relation = 'fail';
    clickToolbarRefresh();
    expect(await screen.findByText(/实体关系没有读到/)).toBeTruthy();
    await waitFor(() => expect(tableRowCount()).toBe(0));
    expect(screen.queryByText(/该应用还没有关系定义/)).toBeNull();
  });

  it('一个应用都没读到时，不许说"该应用还没有关系定义"', async () => {
    // /app/list 答空列表 -> 没有作用域，这是"还没开始读"，不是"库里真没有"
    vi.stubGlobal('fetch', vi.fn(async (input: unknown) => {
      const url = String(input);
      const body = JSON.stringify({
        success: true,
        code: 200,
        message: null,
        data: url.includes('/app/list') ? [] : [],
      });
      return { ok: true, status: 200, text: async () => body, json: async () => JSON.parse(body) };
    }));
    renderPage(<RelationsPage />);
    expect(await screen.findByText('选择应用后再看这里')).toBeTruthy();
    expect(screen.queryByText(/该应用还没有/)).toBeNull();
  });

  it('应用列表读不到时要说"没有读到"，不能只留一个空下拉框', async () => {
    //  picker 空 + 列表 idle 时，页面最容易说的是"这里什么都没有"
    modes.app = 'fail';
    modes.relation = 'fail';
    renderPage(<RelationsPage />);
    expect(await screen.findByText(/应用列表没有读到/)).toBeTruthy();
    expect(screen.getByText('应用接口炸了')).toBeTruthy();
    expect(screen.queryByText(/该应用还没有/)).toBeNull();
  });

  it('横幅里的重试要真的再打一次接口', async () => {
    modes.relation = 'fail';
    renderPage(<RelationsPage />);
    const banner = await screen.findByText(/实体关系没有读到/);
    const alert = banner.closest('.ant-alert') as HTMLElement;

    modes.relation = 'ok';
    const before = requestLog.filter((url) => url.includes('/relation/list')).length;
    clickButtonByText(alert, '重试');
    await waitFor(() => expect(tableRowCount()).toBe(2));
    expect(requestLog.filter((url) => url.includes('/relation/list')).length).toBeGreaterThan(before);
  });
});

describe('DictsPage：字典列表的失败不是一句"还没有字典"', () => {
  it('字典接口失败时显示"没有读到"，不显示"该应用还没有字典"', async () => {
    modes.dict = 'fail';
    renderPage(<DictsPage />);
    expect(await screen.findByText(/字典列表没有读到/)).toBeTruthy();
    expect(screen.queryByText('该应用还没有字典')).toBeNull();
  });

  it('字典读到时列出字典', async () => {
    renderPage(<DictsPage />);
    expect(await screen.findByText('阶段')).toBeTruthy();
    expect(screen.queryByText(/没有读到/)).toBeNull();
  });
});
