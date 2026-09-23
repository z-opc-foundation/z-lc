import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout';
import { AppOverviewPage } from '@/views/apps/AppOverviewPage';
import { AiModelingPage } from '@/views/admin/AiModelingPage';
import { entityNotFoundContent, useEntityOptions } from '@/views/admin/_scope';
import type { EntityDefDTO } from '@/api/types';

/**
 * 「实体没读到」在 workspace 侧的三种出口。
 *
 * 上一轮把管理页列表接上了五态口径，这一轮是同一个谎话剩下的三个出口：侧边栏、应用概览的
 * 统计位、以及吃 `useEntityOptions` 的实体候选。三处的共同点是 `fetchWorkspaceMeta`
 * **从不抛出** —— 单资源失败只记进 `degraded`，react-query 的 `isError` 永远不红，
 * 于是页面拿到一支空数组就开始替用户宣布"这个应用什么都没有"。
 */

const APP = 'crm';
type Mode = 'ok' | 'fail' | 'empty' | 'null';

const modes: { apps: Mode; schema: Mode } = { apps: 'ok', schema: 'ok' };
let calls: string[] = [];

function envelope(data: unknown, success = true) {
  const text = JSON.stringify({ success, code: success ? 200 : 500, message: success ? null : 'boom', data });
  return {
    ok: success,
    status: success ? 200 : 500,
    statusText: success ? 'OK' : 'Server Error',
    text: async () => text,
    json: async () => JSON.parse(text),
  };
}

function entity(code: string, name: string, fields: number): EntityDefDTO {
  return {
    id: code.length,
    entityCode: code,
    entityName: name,
    tableName: `t_${code}`,
    tenantCode: 'default',
    appCode: APP,
    fields: Array.from({ length: fields }, (_unused, index) => ({
      fieldCode: `f${index}`,
      fieldName: `字段${index}`,
      fieldType: 'STRING',
      sortOrder: index,
    })),
  } as EntityDefDTO;
}

function stubBackend() {
  calls = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      calls.push(url);
      // bundle 先按"不可用"处理，强制走逐接口回退路径：这样 schema 的故障才是本次的自变量。
      if (url.includes('/meta/bundle')) return envelope(null, false);
      if (url.includes('/app/list')) {
        return modes.apps === 'fail' ? envelope(null, false) : envelope([{ appCode: APP, appName: 'CRM' }]);
      }
      if (url.includes('/app/schema')) {
        if (modes.schema === 'fail') return envelope(null, false);
        if (modes.schema === 'null') return envelope(null, true);
        if (modes.schema === 'empty') return envelope([]);
        return envelope([entity('deal', '商机', 4), entity('lead', '线索', 2)]);
      }
      if (
        url.includes('/dict/list') ||
        url.includes('/view-config/list') ||
        url.includes('/relation/list') ||
        url.includes('/meta/field-types')
      ) {
        return envelope([]);
      }
      return envelope(null, false);
    }),
  );
}

function schemaCalls() {
  return calls.filter((url) => url.includes('/app/schema')).length;
}

function renderAt(path: string, node: React.ReactElement, entry = path) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route path="/:appCode" element={node}>
            <Route path=":entityCode/:viewType" element={<div>页面主体</div>} />
          </Route>
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function workspaceWrapper() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <MemoryRouter initialEntries={[`/${APP}`]}>
        <QueryClientProvider client={client}>{children}</QueryClientProvider>
      </MemoryRouter>
    );
  };
}

/** antd Statistic 把值单独渲染在一个 span 里，按类名取比按整页文字找词可靠。 */
function statisticValues(): string[] {
  return Array.from(document.querySelectorAll('.ant-statistic-content-value')).map(
    (node) => node.textContent ?? '',
  );
}

/**
 * 只认 message 容器里的文字。整页 `document.body.textContent` 会被别的元素喂饱 ——
 * 侧边栏自己就写着「实体列表没有读到」，那样"toast 说了这句话"就成了别人的功劳。
 */
function toastText(): string {
  return Array.from(document.querySelectorAll('.ant-message-notice-content'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

function clickButtonByText(label: string) {
  const button = screen
    .getAllByRole('button')
    .find((node) => (node.textContent ?? '').replace(/\s/g, '') === label);
  if (!button) throw new Error(`没有 ${label} 这个按钮`);
  fireEvent.click(button);
}

beforeEach(() => {
  vi.unstubAllGlobals();
  modes.apps = 'ok';
  modes.schema = 'ok';
  // 必须在每个用例前重装：不装的话 fetch 打到 Node 的相对 URL 上直接 reject，
  // 所有接口一起"降级"，第一条用例照样绿 —— 但它测的是"网络全挂"而不是我要的那个故障。
  stubBackend();
});

afterEach(() => {
  message.destroy();
});

describe('WorkspaceLayout 侧边栏：实体"没读到"不是"还没有"', () => {
  it('实体接口失败时说"实体列表没有读到"，不许说"该应用还没有实体"', async () => {
    modes.schema = 'fail';
    renderAt(`/${APP}/deal/LIST`, <WorkspaceLayout />);

    await waitFor(() => expect(screen.getByText('实体列表没有读到')).toBeInTheDocument());
    expect(screen.getByText(/失败接口: schema/)).toBeInTheDocument();
    expect(screen.queryByText(/该应用还没有实体/)).toBeNull();
    // 计数位也不能给一个像真有的 0
    expect(screen.getByText('实体 (—)')).toBeInTheDocument();
  });

  it('接口回 success:true 但 data 为 null 时同样算没读到', async () => {
    modes.schema = 'null';
    renderAt(`/${APP}/deal/LIST`, <WorkspaceLayout />);

    await waitFor(() => expect(screen.getByText('实体列表没有读到')).toBeInTheDocument());
    expect(screen.queryByText(/该应用还没有实体/)).toBeNull();
  });

  it('读到空数组时才说"还没有实体"，并把建模入口给出来', async () => {
    modes.schema = 'empty';
    renderAt(`/${APP}/deal/LIST`, <WorkspaceLayout />);

    await waitFor(() => expect(screen.getByText('该应用还没有实体。')).toBeInTheDocument());
    expect(screen.queryByText('实体列表没有读到')).toBeNull();
    expect(screen.getByText('实体 (0)')).toBeInTheDocument();
    expect(screen.getByText('打开 Schema 设计器')).toBeInTheDocument();
  });

  it('读到实体就正常列出来，计数是条数不是破折号', async () => {
    renderAt(`/${APP}/deal/LIST`, <WorkspaceLayout />);

    await waitFor(() => expect(screen.getByText('商机')).toBeInTheDocument());
    expect(screen.getByText('线索')).toBeInTheDocument();
    expect(screen.getByText('实体 (2)')).toBeInTheDocument();
  });

  it('侧边栏的重试要真的再拉一次 schema', async () => {
    modes.schema = 'fail';
    renderAt(`/${APP}/deal/LIST`, <WorkspaceLayout />);

    await waitFor(() => expect(screen.getByText('实体列表没有读到')).toBeInTheDocument());
    const before = schemaCalls();
    expect(before).toBeGreaterThan(0);
    clickButtonByText('重试');
    await waitFor(() => expect(schemaCalls()).toBeGreaterThan(before));
  });
});

describe('AppOverviewPage：统计位不许把"没读到"报成 0', () => {
  it('实体没读到时报"没有读到"，统计位是破折号', async () => {
    modes.schema = 'fail';
    renderAt(`/${APP}`, <AppOverviewPage />);

    await waitFor(() => expect(screen.getByText('实体列表没有读到')).toBeInTheDocument());
    expect(screen.queryByText('该应用还没有实体')).toBeNull();
    await waitFor(() => expect(statisticValues().length).toBeGreaterThan(0));
    // 只有"没读到"的那两格让位成破折号；字典/视图是真读到了空，就该老实写 0。
    expect(statisticValues()).toEqual(['—', '—', '0', '0']);
  });

  it('读到实体时统计出真实条数，空态才提"还没有"', async () => {
    modes.schema = 'ok';
    renderAt(`/${APP}`, <AppOverviewPage />);

    await waitFor(() => expect(screen.getByText('商机')).toBeInTheDocument());
    expect(statisticValues().slice(0, 2)).toEqual(['2', '6']);
    expect(screen.queryByText('实体列表没有读到')).toBeNull();
  });

  it('读到但确实没有实体时，才说"该应用还没有实体"', async () => {
    modes.schema = 'empty';
    renderAt(`/${APP}`, <AppOverviewPage />);

    await waitFor(() => expect(screen.getByText('该应用还没有实体')).toBeInTheDocument());
    expect(screen.queryByText('实体列表没有读到')).toBeNull();
    expect(screen.getByText('打开 Schema 设计器')).toBeInTheDocument();
  });
});

describe('实体候选的口径：下拉空 != 应用没有实体', () => {
  it('元数据降级时 read 是 false，reason 点名是哪个接口', async () => {
    modes.schema = 'fail';
    const { result } = renderHook(() => useEntityOptions(APP), { wrapper: workspaceWrapper() });

    // reason 从「元数据还没加载完」变成点名接口，才是"这次查询真的跑完了"的证据 ——
    // 拿 read 当等待条件会立刻为真（meta 还没到时它也是 false），等于没等。
    await waitFor(() => expect(result.current.reason).toContain('失败接口: schema'));
    expect(result.current.read).toBe(false);
    expect(result.current.options).toEqual([]);
  });

  it('读到实体时 read 是 true，helper 不接管 antd 的默认空态', async () => {
    modes.schema = 'ok';
    const { result } = renderHook(() => useEntityOptions(APP), { wrapper: workspaceWrapper() });

    await waitFor(() => expect(result.current.options).toHaveLength(2));
    expect(result.current.read).toBe(true);
    expect(entityNotFoundContent(result.current)).toBeUndefined();
  });

  it('降级时 helper 给的是"没有读到"这句话，不是「暂无数据」', async () => {
    modes.schema = 'null';
    const { result } = renderHook(() => useEntityOptions(APP), { wrapper: workspaceWrapper() });

    // schema='null' 是 success:true + data:null：没进 degraded，但同样算没读到。
    await waitFor(() => expect(result.current.reason).toContain('不是列表'));
    expect(result.current.read).toBe(false);
    expect(entityNotFoundContent(result.current)).toContain('实体列表没有读到');
  });

  it('AI 建模点"数据分析"时，元数据降级要说没读到而不是说没实体', async () => {
    modes.schema = 'fail';
    // 用 ?appCode= 把应用选好在首次渲染就成型：`useAppSelection` 的 appCode 是从 /app/list
    // 兜底填进来的，不等它就点击会落到 disabled 按钮上 —— 全量跑负载高时这条用例已经因此
    // 假红过一次（红的原因和被测口径无关，这种红不能拿去给守卫背书）。
    renderAt(`/${APP}`, <AiModelingPage />, `/${APP}?appCode=${APP}`);

    const analyze = screen
      .getAllByRole('button')
      .find((candidate) => (candidate.textContent ?? '').replace(/\s/g, '') === '数据分析');
    if (!analyze) throw new Error('没有 数据分析 这个按钮');
    expect(analyze).toBeEnabled();
    fireEvent.click(analyze);

    // 两半必须落在同一次读取上：toast 3 秒就自己收走，分开读会让反断言在空串上"通过"。
    await waitFor(
      () => {
        const text = toastText();
        expect(text).toContain('实体列表没有读到');
        expect(text).not.toContain('该应用还没有实体');
      },
      {timeout: 4000},
    );
  });
});
