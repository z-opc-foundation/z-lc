import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { useState } from 'react';
import type { ComponentProps } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createWorkspaceContext, resolveEntityFields } from '@/fields';
import { ChartView } from '@/views/workspace/ChartView';
import type { EntityDefDTO, FieldDefDTO, ViewConfigDTO } from '@/api/types';
import type { FilterState } from '@/views/grid/FilterBar';

/**
 * 图表视图的行为测试。
 *
 * 钉住三类"图能画出来但是错的"事故：默认分组轴挑了名称列（一条记录一组）、
 * 下拉写着"按月"而请求其实按月精确到每一天、聚合值为 NULL 被画成 0。
 */

const APP = 'crm';
const ENT = 'deal';

function fields(): FieldDefDTO[] {
  return [
    { fieldCode: 'name', fieldName: '商机名', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
    { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', fieldLength: 16, sortOrder: 2 },
    { fieldCode: 'amount', fieldName: '金额', fieldType: 'DECIMAL', sortOrder: 3 },
    { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 4 },
  ] as FieldDefDTO[];
}

function entity(): EntityDefDTO {
  return {
    id: 3, entityCode: ENT, entityName: '商机', tableName: 't_deal',
    tenantCode: 'default', appCode: APP, fields: fields(),
  } as EntityDefDTO;
}

interface Call {
  url: string;
  body: Record<string, unknown>;
}

const calls: Call[] = [];

/** 每次 render 前决定聚合返回什么；信封沿用 client.ts 读的 text() 口径。 */
let aggregatePayload: unknown = {
  success: true, code: 200, message: null,
  data: [
    { group_key: 'NEW', group_label: '新建', group_count: 2 },
    { group_key: 'DONE', group_label: '已完成', group_count: 5 },
  ],
};

function stubBackend() {
  calls.length = 0;
  const text = (payload: unknown) => {
    const body = JSON.stringify(payload);
    return { ok: true, status: 200, text: async () => body, json: async () => payload };
  };
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    if (url.includes('/runtime/aggregate')) return text(aggregatePayload);
    return text({ success: true, code: 200, message: null, data: [] });
  }));
}

function renderChart(overrides: Partial<ComponentProps<typeof ChartView>> = {}) {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ChartView
        entity={entity()}
        resolvedFields={resolveEntityFields(entity(), ws)}
        views={[]}
        appCode={APP}
        tenantCode="default"
        conditions={[]}
        conjunction="AND"
        onFilterChange={() => undefined}
        {...overrides}
      />
    </QueryClientProvider>,
  );
}

function aggCall() {
  const found = calls.find((c) => c.url.includes('/runtime/aggregate'));
  if (!found) throw new Error('没有发出聚合请求');
  return found;
}

const hostText = (node: HTMLElement) => node.textContent ?? '';

/**
 * 用图**自己画出来的刻度**当尺子，从 y 反算值。
 *
 * 不在测试里重跑一遍 `yOf(value, max)`：那是拿组件的公式验组件，公式本身错了就一起错。
 * 刻度线是同一份 `max` 画出来的，所以"柱子按数据最大值缩放、轴按 niceAxis 最大值缩放"
 * 这类轴与图形各画各的缺陷，只有用刻度当尺子才量得出来。
 */
function axisRuler(container: Element) {
  const marks = Array.from(container.querySelectorAll('.zlc-chart-grid g'))
    .map((g) => {
      const y = Number(g.querySelector('line')?.getAttribute('y1'));
      const value = Number((g.querySelector('.zlc-chart-tick')?.textContent ?? '').replace(/,/g, ''));
      return { y, value };
    })
    .filter((m) => Number.isFinite(m.y) && Number.isFinite(m.value));
  const zero = marks.find((m) => m.value === 0);
  const top = marks.length ? marks.reduce((a, m) => (m.value > a.value ? m : a)) : undefined;
  if (!zero || !top || top.value === zero.value) throw new Error(`刻度不够拼出尺子：${JSON.stringify(marks)}`);
  return { valueAt: (y: number) => ((zero.y - y) / (zero.y - top.y)) * top.value, marks };
}

/** 每根柱子/每个点：图形顶部的反算值 vs 图上印出来的那个数。 */
function drawnNumbers(container: HTMLElement) {
  const { valueAt } = axisRuler(container);
  // 只取末尾的数字：柱子上方是裸数字，折线点的 <title> 是「标签 · 记录数 37」
  const trailing = (text: string) => {
    const raw = /(-?[\d,]+(?:\.\d+)?)\s*$/.exec(text.trim())?.[1];
    return raw ? Number(raw.replace(/,/g, '')) : null;
  };
  const bars = Array.from(container.querySelectorAll('.zlc-chart-bar-item'));
  if (bars.length) {
    return bars.map((item) => ({
      printed: trailing(item.querySelector('text')?.textContent ?? ''),
      measured: valueAt(Number(item.querySelector('rect')?.getAttribute('y'))),
    }));
  }
  return Array.from(container.querySelectorAll('.zlc-chart-dot')).map((dot) => ({
    printed: trailing(dot.querySelector('title')?.textContent ?? ''),
    measured: valueAt(Number(dot.getAttribute('cy'))),
  }));
}

/** 一个"按天趋势"命名视图：折线 + 截止日期按天分桶 + 一条筛选条件。 */
function savedTrendView(): ViewConfigDTO {
  return {
    id: 42,
    appCode: APP,
    entityCode: ENT,
    viewType: 'CHART',
    tenantCode: 'default',
    config: JSON.stringify({
      kind: 'line',
      groupField: 'due',
      timeGroup: 'DAY',
      metricFn: 'COUNT',
      topN: 12,
      conditions: [{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }],
      conjunction: 'OR',
      name: '按天趋势',
    }),
  };
}

/**
 * 带"页面"宿主的渲染：筛选状态本来就归页面（表格/看板/图表共用一份），
 * 图表只是请求宿主换一份。没有这个宿主，"视图把筛选一起换过来"就只能证明到
 * 回调边界，证明不了聚合请求体里真的换了 —— 而后者才是用户看到的口径。
 */
function renderChartHost() {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  function Host() {
    const [filter, setFilter] = useState<FilterState>({ conditions: [], conjunction: 'AND' });
    return (
      <ChartView
        entity={entity()}
        resolvedFields={resolveEntityFields(entity(), ws)}
        views={[savedTrendView()]}
        appCode={APP}
        tenantCode="default"
        conditions={filter.conditions}
        conjunction={filter.conjunction}
        onFilterChange={setFilter}
      />
    );
  }
  return render(
    <QueryClientProvider client={client}>
      <Host />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  window.localStorage.clear();
  aggregatePayload = {
    success: true, code: 200, message: null,
    data: [
      { group_key: 'NEW', group_label: '新建', group_count: 2 },
      { group_key: 'DONE', group_label: 'Completed', group_count: 5 },
    ],
  };
  stubBackend();
});

describe('ChartView', () => {
  it('默认按字典列分组而不是名称列，且不会给非日期列硬塞 timeGroup', async () => {
    renderChart();
    await waitFor(() => expect(screen.getAllByText('Completed').length).toBeGreaterThan(0));
    const agg = aggCall();
    expect(agg.body.groupField, '默认分组轴必须是 stage').toBe('stage');
    expect(agg.body.timeGroup ?? null, '文本列没有分桶粒度').toBeNull();
    expect(agg.url).toContain('entityCode=deal');
  });

  it('柱子数与值都来自服务端聚合结果，标签用字典中文', async () => {
    const { container } = renderChart();
    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-bar-item').length).toBe(2));
    // 值倒序：5 的那组先画
    const heights = Array.from(container.querySelectorAll('.zlc-chart-bar-item rect'))
      .map((rect) => Number(rect.getAttribute('height')));
    expect(heights[0]).toBeGreaterThan(heights[1] ?? 0);
    expect(container.textContent).toContain('合计 7');
    expect(container.textContent).toContain('未筛选');
  });

  it('存回来的配置说"按月"，请求就必须真的带 MONTH', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'line', groupField: 'due', timeGroup: 'MONTH', metricFn: 'COUNT', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [
        { bucket_year: 2026, bucket_month: 2, group_count: 1 },
        { bucket_year: 2026, bucket_month: 1, group_count: 3 },
      ],
    };
    const { container } = renderChart();
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    const agg = aggCall();
    expect(agg.body.groupField).toBe('due');
    expect(agg.body.timeGroup).toBe('MONTH');

    // 后端返回乱序也要按时间正序画，否则折线会自己交叉
    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-dot').length).toBe(2));
    const labels = Array.from(container.querySelectorAll('.zlc-chart-axis-label')).map((t) => t.textContent);
    expect(labels.slice(0, 2)).toEqual(['2026-01', '2026-02']);
    const xs = Array.from(container.querySelectorAll('.zlc-chart-dot')).map((d) => Number(d.getAttribute('cx')));
    expect(xs[0]).toBeLessThan(xs[1] ?? 0);
    // 轴标签的 x 必须落在对应点的正下方，否则标签会指着别的点
    const tickXs = Array.from(container.querySelectorAll('.zlc-chart-axis-label')).map((t) => Number(t.getAttribute('x')));
    expect(Math.abs((tickXs[0] ?? 0) - (xs[0] ?? 0)) ).toBeLessThan(0.01);
  });

  it('柱子与折线点的高度要用轴刻度量得回来，不能只比大小', async () => {
    // 12 和 37 会推出 max=40 的整齐轴。若柱子偷偷按"数据最大值 37"缩放，最高的那根会一路
    // 顶到绘图区上沿 —— 序关系完全没变（37 仍然比 12 高），但每根柱子的高度都在说谎。
    // 这正是环图那条 P1 的同一类缺陷：原来的检查是"数个数 + 比大小"，对几何免疫。
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'bar', groupField: 'stage', metricFn: 'COUNT', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [
        { group_key: 'NEW', group_label: '新建', group_count: 12 },
        { group_key: 'DONE', group_label: '已完成', group_count: 37 },
      ],
    };
    const bar = renderChart();
    await waitFor(() => expect(bar.container.querySelectorAll('.zlc-chart-bar-item').length).toBe(2));
    expect(axisRuler(bar.container).marks.map((m) => m.value)).toEqual([0, 10, 20, 30, 40]);
    expect(drawnNumbers(bar.container)).toEqual([
      { printed: 37, measured: expect.closeTo(37, 1) },
      { printed: 12, measured: expect.closeTo(12, 1) },
    ]);
    bar.unmount();

    // 同一份数据换画法：反算出来的数必须还是那两个数
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'line', groupField: 'stage', metricFn: 'COUNT', topN: 12 }),
    );
    const line = renderChart();
    await waitFor(() => expect(line.container.querySelectorAll('.zlc-chart-dot').length).toBe(2));
    expect(drawnNumbers(line.container)).toEqual([
      { printed: 37, measured: expect.closeTo(37, 1) },
      { printed: 12, measured: expect.closeTo(12, 1) },
    ]);
  });

  it('度量选「合计」时画的是 sum_amount，NULL 不能被画成 0', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'bar', groupField: 'stage', metricFn: 'SUM', metricField: 'amount', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [
        { group_key: 'NEW', group_label: '新建', group_count: 2, sum_amount: 300 },
        { group_key: 'ARK', group_label: '归档', group_count: 9, sum_amount: null },
      ],
    };
    const { container } = renderChart();
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    expect(aggCall().body.aggregations).toEqual({ amount: ['SUM'] });

    await waitFor(() => expect(container.textContent).toContain('金额 合计'));
    // 结构断言而不是拍平文本的正则：NULL 那组要"标成破折号 + 零高柱子"，
    // 0 那组才是"真的有量"。在 textContent 里糊一段正则迟早会匹配到隔壁柱子。
    const bars = Array.from(container.querySelectorAll('.zlc-chart-bar-item'));
    expect(bars.map((item) => item.querySelector('text')?.textContent)).toEqual(['300', '—']);
    expect(bars.map((item) => Number(item.querySelector('rect')?.getAttribute('height')))).toEqual([
      expect.any(Number), 0,
    ]);
    expect(Number(bars[0]?.querySelector('rect')?.getAttribute('height'))).toBeGreaterThan(0);
  });

  it('接口报错要说"接口返回错误"并能重试，不能悄悄画一张空图', async () => {
    aggregatePayload = { success: false, code: 400, message: 'timeGroup 只支持 DATE / DATETIME 字段', data: null };
    const { container } = renderChart();
    // antd 会在两个汉字的按钮里插空格，字面量匹配不到
    await waitFor(() => expect(screen.getByText(/重\s*试/)).toBeInTheDocument());
    expect(container.querySelector('.zlc-chart-bar-item'), '出错时不该有柱子').toBeNull();
    expect(screen.getByText(/timeGroup 只支持/)).toBeInTheDocument();
  });

  it('饼图图例不许把 NULL 组写成 0（和柱子同一条规矩）', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'pie', groupField: 'stage', metricFn: 'SUM', metricField: 'amount', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [
        { group_key: 'NEW', group_label: '新建', group_count: 2, sum_amount: 300 },
        { group_key: 'ARK', group_label: '归档', group_count: 9, sum_amount: null },
      ],
    };
    const { container } = renderChart();
    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-slice').length).toBe(2));
    const labels = Array.from(container.querySelectorAll('.zlc-chart-legend-label')).map((n) => n.textContent);
    const values = Array.from(container.querySelectorAll('.zlc-chart-legend-value')).map((n) => n.textContent);
    expect(labels.map((l, i) => `${l}=${values[i]}`).sort()).toEqual(['归档=—', '新建=300']);
    // 0 值扇区仍然占一个图例行：这一组"存在但没有量"这件事不能消失
    expect(container.querySelectorAll('.zlc-chart-legend-share')).toHaveLength(2);
  });

  it('轴标签按显示宽度截断：时间标签完整留下，超长中文名才省略并留 title', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'line', groupField: 'due', timeGroup: 'DAY', metricFn: 'COUNT', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [
        { bucket_year: 2026, bucket_month: 9, bucket_day: 5, group_count: 1 },
        { bucket_year: 2026, bucket_month: 9, bucket_day: 19, group_count: 2 },
      ],
    };
    const first = renderChart();
    await waitFor(() => expect(first.container.querySelectorAll('.zlc-chart-dot').length).toBe(2));
    // '2026-09-05' 有 10 个字符：按字符数截断会砍成 '2026-0…'，整条时间轴就废了
    expect(Array.from(first.container.querySelectorAll('.zlc-chart-axis-label')).map((t) => t.textContent))
      .toEqual(['2026-09-05', '2026-09-19']);
    first.unmount();

    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'bar', groupField: 'stage', metricFn: 'COUNT', topN: 12 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: [{ group_key: 'LONG', group_label: '这是一个非常长的分组名称', group_count: 4 }],
    };
    const second = renderChart();
    await waitFor(() => expect(second.container.querySelectorAll('.zlc-chart-axis-label').length).toBe(1));
    const label = second.container.querySelector('.zlc-chart-axis-label');
    expect(label?.textContent).toContain('…');
    expect(label?.querySelector('title')?.textContent).toBe('这是一个非常长的分组名称');
  });

  it('筛选条件要原样带进聚合请求，否则图会显示成全库口径', async () => {
    renderChart({ conditions: [{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }] });
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    expect(aggCall().body.conditions).toEqual([{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }]);
    await waitFor(() => expect(screen.getByText(/含 1 条筛选/)).toBeInTheDocument());
  });

  it('命名视图落库：viewType=CHART，配置里带上筛出口径的所有键', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'pie', groupField: 'stage', metricFn: 'SUM', metricField: 'amount', topN: 8 }),
    );
    const conditions = [{ fieldCode: 'stage', operator: 'ne' as const, value: 'NEW' }];
    const { container } = renderChart({ conditions });
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));

    // 保存按钮是图标按钮（Tooltip 不给 jsdom 提供可访问名），按类名找
    fireEvent.click(container.querySelector('.zlc-chart-save-view') as HTMLElement);
    const dialog = await screen.findByRole('dialog');
    const input = dialog.querySelector('input') as HTMLInputElement;
    fireEvent.change(input, { target: { value: '阶段金额占比' } });
    // antd 会给两个汉字的按钮插空格，字面量 '保存' 匹配不到；
    // 按 role 查而不是 getByText，否则连"保存为命名图表视图"这个标题一起命中。
    fireEvent.click(within(dialog).getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      const saved = calls.find((c) => c.url.includes('/view-config/create'));
      expect(saved, '没有发出保存视图请求').toBeTruthy();
    });
    const saved = calls.find((c) => c.url.includes('/view-config/create')) as Call;
    expect(saved.body.appCode).toBe(APP);
    expect(saved.body.entityCode).toBe(ENT);
    expect(saved.body.viewType).toBe('CHART');
    expect(saved.body.tenantCode).toBe('default');
    expect(JSON.parse(String(saved.body.config))).toEqual({
      kind: 'pie',
      groupField: 'stage',
      timeGroup: undefined,
      metricFn: 'SUM',
      metricField: 'amount',
      topN: 8,
      conditions,
      conjunction: 'AND',
      name: '阶段金额占比',
    });
  });

  it('落库的命名视图能被认出来：下拉框里显示它的名字，而不是"未命名"', async () => {
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'pie', groupField: 'stage', metricFn: 'SUM', metricField: 'amount', topN: 8 }),
    );
    const config = JSON.stringify({
      kind: 'pie',
      groupField: 'stage',
      metricFn: 'SUM',
      metricField: 'amount',
      topN: 8,
      conditions: [],
      conjunction: 'AND',
      name: '阶段金额占比',
    });
    const { container } = renderChart({
      views: [{ id: 41, appCode: APP, entityCode: ENT, viewType: 'CHART', config, tenantCode: 'default' }],
    });
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    expect(container.querySelector('.zlc-chart-view-select .ant-select-selection-item')?.textContent).toBe(
      '阶段金额占比',
    );
  });

  it('选中命名视图会把图表配置和筛选条件一起换过来', async () => {
    const { container } = renderChartHost();
    await waitFor(() => expect(aggCall().body.groupField).toBe('stage'));

    const select = container.querySelector('.zlc-chart-view-select') as HTMLElement;
    fireEvent.mouseDown(select.querySelector('.ant-select-selector') as Element);
    const option = await waitFor(() => {
      const found = document.querySelector('.ant-select-dropdown:not(.ant-select-dropdown-hidden) [title="按天趋势"]');
      if (!found) throw new Error('下拉里没有这个视图');
      return found;
    });
    fireEvent.click(option as HTMLElement);

    // 视图带的筛选条件必须一起换：只换图不换筛选，视图名字说的口径就对不上画出来的数。
    // 筛选状态属于页面（表格/看板/图表共用一份），所以要一路走到真实请求体才算数。
    await waitFor(() => {
      const last = [...calls].reverse().find((c) => c.url.includes('/runtime/aggregate')) as Call;
      expect(last.body.groupField).toBe('due');
      expect(last.body.timeGroup).toBe('DAY');
      expect(last.body.conditions).toEqual([{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }]);
      expect(last.body.conjunction).toBe('OR');
    });
    expect(hostText(container)).toMatch(/含 1 条筛选/);
  });

  it('视图名字来自 config.name，缺了就回落到编号而不是显示空白', async () => {
    const { container } = renderChart({
      views: [
        { id: 7, appCode: APP, entityCode: ENT, viewType: 'CHART', config: '{"kind":"bar"}', tenantCode: 'default' },
        { id: 8, appCode: APP, entityCode: ENT, viewType: 'CHART', config: '{ 坏 JSON', tenantCode: 'default' },
      ],
    });
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    // 两条都进下拉框（视图列表不按 config 是否可解析过滤），标签分别来自 name 和兜底编号
    const options: string[] = [];
    const select = container.querySelector('.zlc-chart-view-select') as HTMLElement;
    fireEvent.mouseDown(select.querySelector('.ant-select-selector') as Element);
    await waitFor(() => {
      document
        .querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option')
        .forEach((node) => options.push(String(node.getAttribute('title') ?? node.textContent)));
      expect(options.length).toBe(2);
    });
    expect(options.sort()).toEqual(['视图 #7', '视图 #8']);
  });

  it('点多到要抽稀轴标签时，留下的标签必须仍指着自己的点', async () => {
    // 一天一桶很容易超过 12 个，逐根标会糊成一片，所以 XLabels 按密度抽样 —— 但"少标"
    // 绝不能变成"标错"：标签 x 落不到真实点上，读者就会把没标签的那个点读成"这天没数据"。
    // 这条与环图/柱高那两条同族：检查要跨元素对得上，不能各自算各自的。
    window.localStorage.setItem(
      `zlc:chart:${APP}:${ENT}`,
      JSON.stringify({ kind: 'line', groupField: 'due', timeGroup: 'DAY', metricFn: 'COUNT', topN: 30 }),
    );
    aggregatePayload = {
      success: true, code: 200, message: null,
      data: Array.from({ length: 15 }, (_, i) => ({
        group_key: `2026-09-${String(i + 1).padStart(2, '0')}`,
        group_count: i + 1,
      })),
    };
    const { container } = renderChart();
    await waitFor(() => expect(container.querySelectorAll('.zlc-chart-dot').length).toBe(15));
    const dots = Array.from(container.querySelectorAll('.zlc-chart-dot')).map((dot) => ({
      x: Number(dot.getAttribute('cx')),
      label: (dot.querySelector('title')?.textContent ?? '').split(' · ')[0],
    }));
    const labels = Array.from(container.querySelectorAll('.zlc-chart-axis-label'))
      .map((node) => ({ x: Number(node.getAttribute('x')), text: node.textContent ?? '' }));
    expect(labels.length, '15 个点该被抽稀，但仍然要有标签').toBeGreaterThan(7);
    expect(labels.length, '15 个点该被抽稀').toBeLessThan(15);
    for (const label of labels) {
      const owner = dots.find((dot) => Math.abs(dot.x - label.x) < 0.5);
      expect(owner, `标签「${label.text}」没对到任何一个点 x=${label.x}`).toBeDefined();
      expect(owner?.label, `标签「${label.text}」印到了别的点的刻度上`).toBe(label.text);
    }
    const lastLabel = labels[labels.length - 1] ?? { x: -1, text: '(没有标签)' };
    const lastDot = dots[dots.length - 1] ?? { x: -1, label: '' };
    expect(lastLabel.x, '最后一个点必须有标签').toBeCloseTo(lastDot.x, 1);
  });
});
