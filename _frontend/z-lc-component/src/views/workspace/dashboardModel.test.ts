import { describe, expect, it } from 'vitest';
import type { ViewConfigDTO } from '@/lc/api/types';
import { newViewConfigDraft } from '@/lc/api/viewConfig';
import {
  APP_LEVEL_ENTITY_CODE,
  DASHBOARD_VIEW_TYPE,
  dashboardConfig,
  parseDashboard,
  pickableChartViews,
  resolveWidgets,
} from './dashboardModel';

/**
 * 仪表盘落库模型的契约测试。
 *
 * 钉住三件会静默出错的事：config 被手改过之后静默当成空仪表盘（然后保存时把原视图
 * 覆盖掉）、引用的图表视图被删后组件凭空少一块、以及仪表盘行串进按实体取视图的下拉。
 */

function view(partial: Partial<ViewConfigDTO>): ViewConfigDTO {
  return {
    id: 1,
    entityCode: 'deal',
    appCode: 'crm',
    viewType: 'CHART',
    config: null,
    tenantCode: 'default',
    ...partial,
  } as ViewConfigDTO;
}

const chartConfig = { kind: 'line', groupField: 'due', timeGroup: 'MONTH', metricFn: 'COUNT', topN: 12 };

function chartRow(id: number, entityCode = 'deal', config: Record<string, unknown> = chartConfig): ViewConfigDTO {
  return view({ id, entityCode, viewType: 'CHART', config: JSON.stringify({ ...config, name: `图 ${id}` }) });
}

describe('parseDashboard', () => {
  it('认得出仪表盘，组件顺序就是页面上的顺序', () => {
    const model = parseDashboard(
      view({
        id: 7,
        entityCode: APP_LEVEL_ENTITY_CODE,
        viewType: DASHBOARD_VIEW_TYPE,
        config: JSON.stringify({ name: '老板看板', widgets: [{ viewId: 11 }, { viewId: 12, width: 2 }] }),
      }),
    );
    expect(model).toEqual({ name: '老板看板', widgets: [{ viewId: 11, width: 1 }, { viewId: 12, width: 2 }] });
  });

  it('存的不是仪表盘配置时返回 null，而不是一个空仪表盘', () => {
    // 空仪表盘会被页面当成"真的没有组件"，用户点保存就把这行原配置覆盖了
    expect(parseDashboard(view({ config: JSON.stringify({ columnMeta: [] }) }))).toBeNull();
    expect(parseDashboard(view({ config: '{oops' }))).toBeNull();
    expect(parseDashboard(view({ config: null }))).toBeNull();
    expect(parseDashboard(undefined)).toBeNull();
  });

  it('脏组件一律丢掉：编号非整数、重复、负数；宽度只认白名单', () => {
    const model = parseDashboard(
      view({
        id: 3,
        config: JSON.stringify({
          widgets: [
            { viewId: 11 },
            { viewId: 11, width: 2 },
            { viewId: '12' },
            { viewId: 0 },
            { viewId: -5 },
            { viewId: null },
            null,
            { viewId: 13, width: 99 },
          ],
        }),
      }),
    );
    expect(model?.widgets).toEqual([
      { viewId: 11, width: 1 },
      { viewId: 12, width: 1 },
      { viewId: 13, width: 1 },
    ]);
  });

  it('没起名时用编号顶上，名字两边空格去掉', () => {
    expect(parseDashboard(view({ id: 9, config: JSON.stringify({ widgets: [] }) }))?.name).toBe('仪表盘 #9');
    expect(parseDashboard(view({ id: 9, config: JSON.stringify({ name: '  经营盘  ', widgets: [] }) }))?.name)
      .toBe('经营盘');
  });

  it('存出去再读回来是同一份（config 走 JSON 字符串这一层不能改变语义）', () => {
    const model = { name: '经营盘', widgets: [{ viewId: 4, width: 2 as const }, { viewId: 5, width: 1 as const }] };
    const draft = newViewConfigDraft({
      appCode: 'crm',
      entityCode: APP_LEVEL_ENTITY_CODE,
      viewType: DASHBOARD_VIEW_TYPE,
      config: dashboardConfig(model),
    });
    expect(draft.viewType).toBe('DASHBOARD');
    expect(draft.entityCode).toBe('*');
    const restored = parseDashboard(view({ id: 21, ...draft, config: draft.config }));
    expect(restored?.name).toBe('经营盘');
    expect(restored?.widgets).toEqual(model.widgets);
  });
});

describe('resolveWidgets', () => {
  it('每个组件解析出自己的实体与图表配置', () => {
    const views = [chartRow(11), chartRow(12, 'task')];
    const widgets = resolveWidgets({ name: 'x', widgets: [{ viewId: 11, width: 1 }, { viewId: 12, width: 2 }] }, views);
    expect(widgets.map((item) => item.entityCode)).toEqual(['deal', 'task']);
    expect(widgets[0]?.snapshot?.config.groupField).toBe('due');
    expect(widgets[1]?.snapshot?.config.kind).toBe('line');
  });

  it('视图被删了要说得出是哪一个，而不是把组件变没', () => {
    const widgets = resolveWidgets({ name: 'x', widgets: [{ viewId: 99, width: 1 }] }, [chartRow(11)]);
    expect(widgets).toHaveLength(1);
    expect(widgets[0]?.view).toBeNull();
    expect(widgets[0]?.snapshot).toBeNull();
  });

  it('引用的视图存在但存的不是图表配置 -> snapshot 为 null，页面据此报错', () => {
    const widgets = resolveWidgets(
      { name: 'x', widgets: [{ viewId: 8, width: 1 }] },
      [view({ id: 8, entityCode: 'deal', viewType: 'CHART', config: JSON.stringify({ columnMeta: [] }) })],
    );
    expect(widgets[0]?.view).not.toBeNull();
    expect(widgets[0]?.snapshot).toBeNull();
  });
});

describe('pickableChartViews', () => {
  it('只有能还原成图表配置的 CHART 行可以被放进仪表盘', () => {
    const views = [
      chartRow(11),
      view({ id: 12, viewType: 'GRID', config: JSON.stringify(chartConfig) }),
      view({ id: 13, viewType: 'CHART', config: JSON.stringify({ columnMeta: [] }) }),
      view({ id: 14, entityCode: APP_LEVEL_ENTITY_CODE, viewType: DASHBOARD_VIEW_TYPE, config: JSON.stringify({ widgets: [{ viewId: 11 }] }) }),
    ];
    expect(pickableChartViews(views).map((view_) => view_.id)).toEqual([11]);
  });
});
