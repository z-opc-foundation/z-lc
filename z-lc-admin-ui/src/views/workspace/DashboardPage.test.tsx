import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DashboardPage } from '@/views/workspace/DashboardPage';
import type { EntityDefDTO, FieldDefDTO, ViewConfigDTO } from '@/api/types';

/**
 * 仪表盘页面的行为测试。
 *
 * 钉住的都是"页面看着像在正常工作"的错法：组件拿页面当前的口径去取数（而不是
 * 视图落库的口径）、引用断了那块组件静默消失、保存时把仪表盘写成了图表视图的一行、
 * 切了仪表盘却把上一份没保存的布局带过去。
 */

const APP = 'crm';
const TENANT = 'default';

function dealEntity(): EntityDefDTO {
  return {
    id: 3,
    entityCode: 'deal',
    entityName: '商机',
    tableName: 't_deal',
    tenantCode: TENANT,
    appCode: APP,
    fields: [
      { fieldCode: 'name', fieldName: '商机名', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
      { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', fieldLength: 16, sortOrder: 2 },
      { fieldCode: 'amount', fieldName: '金额', fieldType: 'DECIMAL', sortOrder: 3 },
      { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 4 },
    ] as FieldDefDTO[],
  } as EntityDefDTO;
}

function leadEntity(): EntityDefDTO {
  return {
    id: 4,
    entityCode: 'lead',
    entityName: '线索',
    tableName: 't_lead',
    tenantCode: TENANT,
    appCode: APP,
    fields: [
      { fieldCode: 'title', fieldName: '线索标题', fieldType: 'STRING', fieldLength: 64, sortOrder: 1 },
      { fieldCode: 'source', fieldName: '来源渠道', fieldType: 'STRING', dictCode: 'source', fieldLength: 16, sortOrder: 2 },
    ] as FieldDefDTO[],
  } as EntityDefDTO;
}

function chartView(id: number, entityCode: string, name: string, chart: Record<string, unknown>): ViewConfigDTO {
  return {
    id,
    appCode: APP,
    entityCode,
    viewType: 'CHART',
    tenantCode: TENANT,
    config: JSON.stringify({ ...chart, name }),
  } as ViewConfigDTO;
}

function dashboardView(id: number, name: string, widgets: unknown[]): ViewConfigDTO {
  return {
    id,
    appCode: APP,
    entityCode: '*',
    viewType: 'DASHBOARD',
    tenantCode: TENANT,
    config: JSON.stringify({ name, widgets }),
  } as ViewConfigDTO;
}

/** 三张落库的图：日期列按天 + 带一条筛选、带度量的饼图、另一个实体的柱图。 */
const TREND = () =>
  chartView(42, 'deal', '按天趋势', {
    kind: 'line',
    groupField: 'due',
    timeGroup: 'DAY',
    metricFn: 'COUNT',
    topN: 12,
    conditions: [{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }],
    conjunction: 'AND',
  });
const PIE = () =>
  chartView(43, 'deal', '阶段金额占比', {
    kind: 'pie',
    groupField: 'stage',
    metricFn: 'SUM',
    metricField: 'amount',
    topN: 8,
    conditions: [],
    conjunction: 'AND',
  });
const LEAD_BAR = () =>
  chartView(44, 'lead', '线索来源分布', {
    kind: 'bar',
    groupField: 'source',
    metricFn: 'COUNT',
    topN: 12,
  });

/** 后端按 id 倒序返回，数组顺序就是这个顺序。 */
function defaultViews(): ViewConfigDTO[] {
  return [
    dashboardView(51, '备用看板', [{ viewId: 44, width: 2 }]),
    dashboardView(50, '经营看板', [
      { viewId: 42, width: 1 },
      { viewId: 43, width: 2 },
      { viewId: 99, width: 1 },
    ]),
    LEAD_BAR(),
    PIE(),
    TREND(),
  ];
}

interface Call {
  url: string;
  body: Record<string, unknown>;
}

const calls: Call[] = [];
let serverViews: ViewConfigDTO[] = [];
let serverEntities: EntityDefDTO[] = [];
let nextId = 60;

/** 一次请求只答一个分组列：两个组件各自取自己的数，服务端不会替它们合并。 */
function aggregateRows(body: Record<string, unknown>) {
  switch (body.groupField) {
    case 'due':
      return [
        { bucket_year: 2026, bucket_month: 9, bucket_day: 5, group_count: 2 },
        { bucket_year: 2026, bucket_month: 9, bucket_day: 19, group_count: 3 },
      ];
    case 'stage':
      return [
        { group_key: 'NEW', group_label: '新建', group_count: 2, sum_amount: 300 },
        { group_key: 'ARK', group_label: '归档', group_count: 9, sum_amount: null },
      ];
    case 'source':
      return [
        { group_key: 'WEB', group_label: '官网', group_count: 7 },
        { group_key: 'REF', group_label: '转介绍', group_count: 4 },
      ];
    default:
      return [{ group_count: 12 }];
  }
}

function stubBackend() {
  const text = (payload: unknown) => {
    const body = JSON.stringify(payload);
    return { ok: true, status: 200, text: async () => body, json: async () => payload };
  };
  const ok = (data: unknown) => text({ success: true, code: 200, message: null, data });

  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });

    if (url.includes('/meta/bundle')) {
      return ok({
        app: { appCode: APP, appName: 'CRM' },
        entities: serverEntities,
        dicts: [],
        views: serverViews,
        fieldTypes: [],
      });
    }
    if (url.includes('/meta/field-types')) return ok([]);
    if (url.includes('/runtime/aggregate')) return ok(aggregateRows(body));
    if (url.includes('/view-config/create')) {
      const created = {
        id: nextId,
        appCode: body.appCode,
        entityCode: body.entityCode,
        viewType: body.viewType,
        tenantCode: body.tenantCode,
        config: body.config,
      } as ViewConfigDTO;
      nextId += 1;
      serverViews = [created, ...serverViews];
      return ok(created);
    }
    if (url.includes('/view-config/update')) {
      serverViews = serverViews.map((view) =>
        view.id === body.id ? { ...view, config: String(body.config ?? '') } : view,
      );
      return ok(serverViews.find((view) => view.id === body.id) ?? null);
    }
    return ok([]);
  }));
}

/**
 * 只让某一个资源读不到的 stub —— 页面必须据此改口，不能把故障说成"被删了"。
 *
 * - viewsMissingInBundle: bundle 里干脆不带 views 这个 key。后端 MetaController 给每个
 *   子资源各自包了 try/catch + log.warn，坏掉的那个就是被省略，整体仍回 success:true。
 * - viewsEndpointFails:   补拉单接口也 500，于是 read.views 才会是 false。
 * - schemaFails:          /app/schema 500，且 bundle 整体不可用（走 bundle 分支时 entities
 *   必然是读到的，那条路径不可能出现 read.entities=false）。
 */
function stubMeta(opts: {
  viewsMissingInBundle?: boolean;
  viewsEndpointFails?: boolean;
  schemaFails?: boolean;
}) {
  const text = (payload: unknown) => {
    const body = JSON.stringify(payload);
    return { ok: true, status: 200, text: async () => body, json: async () => payload };
  };
  const ok = (data: unknown) => text({ success: true, code: 200, message: null, data });
  const fail = () => {
    const payload = { success: false, code: 500, message: 'boom', data: null };
    const body = JSON.stringify(payload);
    return {
      ok: false,
      status: 500,
      statusText: 'Server Error',
      text: async () => body,
      json: async () => payload,
    };
  };

  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });

    if (url.includes('/meta/bundle')) {
      if (opts.schemaFails) return fail();
      const bundle: Record<string, unknown> = {
        app: { appCode: APP, appName: 'CRM' },
        entities: serverEntities,
        dicts: [],
        views: serverViews,
        fieldTypes: [],
      };
      if (opts.viewsMissingInBundle) delete bundle.views;
      return ok(bundle);
    }
    if (url.includes('/meta/field-types')) return ok([]);
    if (url.includes('/app/schema')) return opts.schemaFails ? fail() : ok(serverEntities);
    if (url.includes('/view-config/list')) return opts.viewsEndpointFails ? fail() : ok(serverViews);
    if (url.includes('/relation/list') || url.includes('/dict/list')) return ok([]);
    if (url.includes('/runtime/aggregate')) return ok(aggregateRows(body));
    return ok([]);
  }));
}

function LocationProbe() {
  const { pathname } = useLocation();
  return <div data-testid="location">{pathname}</div>;
}

function renderDashboard(entry = `/${APP}/dashboard/50`) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route path="/:appCode/dashboard" element={<DashboardPage />} />
          <Route path="/:appCode/dashboard/:dashboardId" element={<DashboardPage />} />
        </Routes>
        <LocationProbe />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function aggCalls(): Call[] {
  return calls.filter((call) => call.url.includes('/runtime/aggregate'));
}

function aggCallFor(groupField: string): Call {
  const found = aggCalls().find((call) => call.body.groupField === groupField);
  if (!found) throw new Error(`没有发出 groupField=${groupField} 的聚合请求`);
  return found;
}

function callFor(path: string): Call {
  const found = calls.find((call) => call.url.includes(path));
  if (!found) throw new Error(`没有发出 ${path} 请求`);
  return found;
}

/** antd 的 Select 要 mouseDown 展开下拉再点选项；下拉里可能残留另一个 Select 的节点。 */
async function pickOption(container: HTMLElement, selector: string, label: string) {
  const select = container.querySelector(selector) as HTMLElement;
  fireEvent.mouseDown(select.querySelector('.ant-select-selector') as Element);
  const option = await waitFor(() => {
    const nodes = Array.from(
      document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
    );
    const found = nodes.find((node) => (node.getAttribute('title') ?? node.textContent ?? '') === label);
    if (!found) throw new Error(`下拉里没有「${label}」`);
    return found;
  });
  fireEvent.click(option as HTMLElement);
}

/** 组件块（含引用失效的占位块）的宽度序列。 */
function widths(container: HTMLElement): number[] {
  return Array.from(container.querySelectorAll('.zlc-dash-tile')).map((node) =>
    node.classList.contains('zlc-dash-tile--w2') ? 2 : 1,
  );
}

beforeEach(() => {
  calls.length = 0;
  window.localStorage.clear();
  vi.unstubAllGlobals();
  nextId = 60;
  serverEntities = [dealEntity(), leadEntity()];
  serverViews = defaultViews();
  stubBackend();
});

describe('DashboardPage', () => {
  it('每个组件用自己的落库口径取数，引用断掉的那块点名说是谁丢了', async () => {
    const { container } = renderDashboard();
    await waitFor(() => expect(aggCalls()).toHaveLength(2));

    // 折线那块：分桶粒度和筛选条件都来自视图 config，不是页面当前状态
    const trend = aggCallFor('due');
    expect(trend.body.timeGroup).toBe('DAY');
    expect(trend.body.conditions).toEqual([{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }]);
    expect(trend.url).toContain('entityCode=deal');
    // 饼图那块：度量列下发，且不硬塞 timeGroup（分组列是文本列，塞了后端会拒掉整个请求）
    const pie = aggCallFor('stage');
    expect(pie.body.aggregations).toEqual({ amount: ['SUM'] });
    expect(pie.body.timeGroup ?? null).toBeNull();
    expect(pie.body.conditions).toEqual([]);

    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-slice')).toHaveLength(2));
    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-dot')).toHaveLength(2));

    // 第三块指向已删除的视图：留一块写清楚编号的牌子，取数请求不能多一发
    expect(widths(container)).toEqual([1, 2, 1]);
    expect(screen.getByText('图表视图 #99 已被删除')).toBeInTheDocument();
    expect(aggCalls()).toHaveLength(2);

    // 口径说明各说各的：共用一行说明就等于有一张图在说谎
    const captions = Array.from(container.querySelectorAll('.zlc-dash-tile-caption')).map(
      (node) => node.textContent ?? '',
    );
    expect(captions[0]).toContain('按天');
    expect(captions[0]).toContain('含 1 条筛选');
    expect(captions[1]).toContain('未筛选');
    expect(captions[1]).toContain('金额 合计');

    expect(container.querySelector('.zlc-dash-tile-open')?.getAttribute('href')).toBe(
      `/${APP}/deal/CHART`,
    );
    expect(container.querySelector('.zlc-dash-dirty')).toBeNull();
  });

  it('引用坏掉的三种情况各说一种，不许静默少一块', async () => {
    serverViews = [
      dashboardView(55, '引用检查', [
        { viewId: 42, width: 1 },
        { viewId: 53, width: 1 },
        { viewId: 54, width: 1 },
      ]),
      // 53：viewType 说是 CHART，config 里却没有图 —— 不能画一张空图糊过去
      {
        id: 53,
        appCode: APP,
        entityCode: 'deal',
        viewType: 'CHART',
        tenantCode: TENANT,
        config: JSON.stringify({ columnMeta: [], name: '其实是个表格' }),
      } as ViewConfigDTO,
      // 54：图还在，但它所属的实体被删了
      chartView(54, 'ghost', '已失效实体的图', { kind: 'bar', groupField: 'stage', metricFn: 'COUNT' }),
      PIE(),
      TREND(),
    ];
    const { container } = renderDashboard(`/${APP}/dashboard/55`);
    await waitFor(() => expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(3));

    expect(screen.getByText('图表视图「其实是个表格」存的不是图表配置')).toBeInTheDocument();
    expect(screen.getByText(/实体「ghost」已被删除/)).toBeInTheDocument();
    expect(widths(container)).toEqual([1, 1, 1]);
    // 只有健康的那一块发了取数请求
    await waitFor(() => expect(aggCalls()).toHaveLength(1));
    expect(aggCalls()[0]?.url).toContain('entityCode=deal');
  });

  it('views 没读到整页报加载故障，不许逐块说"视图已被删除"', async () => {
    stubMeta({ viewsMissingInBundle: true, viewsEndpointFails: true });
    const { container } = renderDashboard();

    await waitFor(() => expect(screen.getByText('图表视图元数据没读到')).toBeInTheDocument());
    expect(screen.getByText(/这是加载故障，不代表视图已被删除/)).toBeInTheDocument();
    // 逐块追责的两种说法都不许出现（上面那句"不代表…已被删除"是澄清，不是指控，
    // 所以这里必须按具体句式匹配，不能拿 /已被删除/ 全文扫 —— 那样连自己的澄清都会被当成谎话）
    expect(screen.queryByText(/图表视图 #\d+ 已被删除/)).toBeNull();
    expect(screen.queryByText(/存的不是图表配置/)).toBeNull();
    // 一块占位牌都不该有：整页已经改成了"读不到"的口径
    expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(0);
    expect(container.querySelector('.zlc-dash-grid')).toBeNull();
    // 布局都没确认，一发取数请求都不该发
    expect(aggCalls()).toHaveLength(0);
  });

  it('entities 没读到、views 读到时，占位块说"没读到"而不是"实体已被删除"', async () => {
    stubMeta({ schemaFails: true });
    const { container } = renderDashboard();
    await waitFor(() => expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(3));

    expect(screen.getByText('实体元数据没读到，下面的图暂时取不到字段')).toBeInTheDocument();
    // 两块 deal 图都改了口径（引用同一实体的组件要一起说实话，不能只修第一块）
    expect(screen.getAllByText(/实体「deal」的字段没读到/)).toHaveLength(2);
    expect(screen.queryByText(/实体「deal」已被删除/)).toBeNull();
    // 提醒里不许劝人移除组件：照默认文案去做，用户会因一次接口故障删掉自己的布局
    // （两块 deal 图各带一句，和上面同理 —— getByText 命中两个就是测试写错了）
    expect(screen.getAllByText(/先别移除这个组件/)).toHaveLength(2);
    // views 是读到了的，所以 #99 那句"已被删除"依然成立 —— 修谎话不能把真话也抹了
    expect(screen.getByText('图表视图 #99 已被删除')).toBeInTheDocument();
    expect(aggCalls()).toHaveLength(0);
  });

  it('添加图表视图：立刻按自己的口径取数，保存写回同一行 DASHBOARD', async () => {
    const { container } = renderDashboard();
    await waitFor(() => expect(aggCalls()).toHaveLength(2));
    // 没改过就不许保存 —— 否则一次误点会把落库配置换成页面当前状态
    expect(container.querySelector('.zlc-dash-save')).toBeDisabled();

    await pickOption(container, '.zlc-dash-add', '线索来源分布');

    await waitFor(() => expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(4));
    expect(container.querySelector('.zlc-dash-dirty')).toBeInTheDocument();
    // 跨实体组合：新组件打的是自己实体的请求，不会蹭商机那张图
    await waitFor(() => expect(aggCalls()).toHaveLength(3));
    const lead = aggCallFor('source');
    expect(lead.url).toContain('entityCode=lead');
    expect(lead.body.appCode).toBe(APP);
    expect(container.querySelector('.zlc-dash-tile[data-entity="lead"]')).toBeTruthy();
    // 已经在仪表盘里的图不能再加一次
    await waitFor(() =>
      expect(container.querySelector('.zlc-dash-add')?.classList.contains('ant-select-disabled')).toBe(true),
    );

    fireEvent.click(container.querySelector('.zlc-dash-save') as HTMLElement);
    await waitFor(() => expect(callFor('/view-config/update').body.id).toBe(50));

    const saved = callFor('/view-config/update');
    expect(saved.body.viewType).toBe('DASHBOARD');
    expect(JSON.parse(String(saved.body.config))).toEqual({
      name: '经营看板',
      widgets: [
        { viewId: 42, width: 1 },
        { viewId: 43, width: 2 },
        { viewId: 99, width: 1 },
        { viewId: 44, width: 1 },
      ],
    });
    // 保存仪表盘不是保存图表：不该顺手多写一行图表视图
    expect(calls.some((call) => call.url.includes('/view-config/create'))).toBe(false);
    await waitFor(() => expect(container.querySelector('.zlc-dash-dirty')).toBeNull());
  });

  it('新建一份仪表盘走 create：viewType=DASHBOARD、entity_code 用应用级占位，地址栏跟着新编号', async () => {
    const { container } = renderDashboard();
    await waitFor(() => expect(aggCalls()).toHaveLength(2));

    fireEvent.click(container.querySelector('.zlc-dash-new') as HTMLElement);
    await waitFor(() => expect(screen.getByDisplayValue('仪表盘 3')).toBeInTheDocument());
    expect(container.querySelector('.zlc-dash-empty')).toBeInTheDocument();

    fireEvent.click(container.querySelector('.zlc-dash-save') as HTMLElement);
    await waitFor(() =>
      expect(calls.some((call) => call.url.includes('/view-config/create'))).toBe(true),
    );

    const created = callFor('/view-config/create');
    expect(created.body.appCode).toBe(APP);
    // entity_code 是 NOT NULL，应用级的仪表盘填 '*'；取视图的地方按 (entityCode, viewType) 双条件过滤，所以不会串
    expect(created.body.entityCode).toBe('*');
    expect(created.body.viewType).toBe('DASHBOARD');
    expect(created.body.tenantCode).toBe(TENANT);
    expect(JSON.parse(String(created.body.config))).toEqual({ name: '仪表盘 3', widgets: [] });

    // 保存之后页面必须就是刚存下的这一份：地址栏说 #60、内容却还是 #50 是最难解释的一种错
    await waitFor(() =>
      expect(screen.getByTestId('location').textContent).toBe(`/${APP}/dashboard/60`),
    );
    await waitFor(() => expect(screen.getByDisplayValue('仪表盘 3')).toBeInTheDocument());
    expect(calls.some((call) => call.url.includes('/view-config/update'))).toBe(false);
  });

  it('切仪表盘要丢掉上一份未保存的布局', async () => {
    const { container } = renderDashboard();
    await waitFor(() => expect(aggCalls()).toHaveLength(2));

    // 把第一块改成整行 —— 只改本地草稿，不保存
    fireEvent.click(container.querySelector('.zlc-dash-tile-width') as HTMLElement);
    await waitFor(() => expect(widths(container)).toEqual([2, 2, 1]));
    expect(container.querySelector('.zlc-dash-dirty')).toBeInTheDocument();

    await pickOption(container, '.zlc-dash-switch', '备用看板');
    await waitFor(() =>
      expect(screen.getByTestId('location').textContent).toBe(`/${APP}/dashboard/51`),
    );
    await waitFor(() => expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(1));
    expect(widths(container)).toEqual([2]);
    expect(screen.getByDisplayValue('备用看板')).toBeInTheDocument();
    expect(container.querySelector('.zlc-dash-dirty')).toBeNull();

    // 再切回来：草稿不跟人跑，页面上是落库的那三份
    await pickOption(container, '.zlc-dash-switch', '经营看板');
    await waitFor(() => expect(container.querySelectorAll('.zlc-dash-tile')).toHaveLength(3));
    expect(widths(container)).toEqual([1, 2, 1]);
    expect(container.querySelector('.zlc-dash-dirty')).toBeNull();
  });

  it('URL 不带编号时打开最新的一份，不是数组里恰好第一个', async () => {
    const { container } = renderDashboard(`/${APP}/dashboard`);
    await waitFor(() => expect(screen.getByDisplayValue('备用看板')).toBeInTheDocument());
    expect(widths(container)).toEqual([2]);
  });

  it('config 里认不出组件清单的 DASHBOARD 行：说清楚坏在哪，并且不给一个能覆盖它的空仪表盘', async () => {
    serverViews = [
      {
        id: 52,
        appCode: APP,
        entityCode: '*',
        viewType: 'DASHBOARD',
        tenantCode: TENANT,
        config: JSON.stringify({ columnMeta: [], sorts: [] }),
      } as ViewConfigDTO,
      ...defaultViews(),
    ];
    const { container } = renderDashboard(`/${APP}/dashboard/52`);
    await waitFor(() => expect(screen.getByText('视图 #52 存的不是仪表盘配置')).toBeInTheDocument());
    // 认不出组件清单时不能只给一个"空仪表盘"：那样用户分不清是空的还是坏掉的
    expect(container.querySelector('.zlc-dash-grid')).toBeNull();
    expect(container.querySelector('.zlc-dash-save')).toBeDisabled();
    expect(aggCalls()).toHaveLength(0);
  });

  it('一份仪表盘都还没有时，空态指向"新建"，不是说"这个仪表盘是空的"', async () => {
    serverViews = [LEAD_BAR(), PIE(), TREND()];
    const { container } = renderDashboard(`/${APP}/dashboard`);
    await waitFor(() => expect(container.querySelector('.zlc-dash-empty')).toBeInTheDocument());
    expect(container.textContent).toContain('还没有仪表盘');
    expect(container.textContent).not.toContain('这个仪表盘还是空的');
    // 还没有仪表盘，就不该有"删除这个仪表盘"的入口
    expect(container.querySelector('.zlc-dash-delete')).toBeNull();
    expect(aggCalls()).toHaveLength(0);
  });
});
