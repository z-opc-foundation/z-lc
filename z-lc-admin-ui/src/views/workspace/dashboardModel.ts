import type { ViewConfigDTO } from '@/api/types';
import { parseViewConfig } from '@/api/viewConfig';
import { chartViewSnapshot, type ChartViewSnapshot } from './chartModel';

/**
 * 仪表盘的落库模型：一行 `z_lc_view_config`，config 里存组件清单，
 * 每个组件指向一个已落库的命名图表视图（`viewId`）。
 *
 * 指向而不是内联复制，是为了让"改一次图，所有仪表盘都跟着改"成立 ——
 * 内联一份配置会出现同名图在不同仪表盘数字不一样的情况，那比没有仪表盘更糟。
 */

export const DASHBOARD_VIEW_TYPE = 'DASHBOARD';

/**
 * `z_lc_view_config.entity_code` 是 NOT NULL，而仪表盘是**应用级**的：它的组件
 * 可以来自任意实体，没有"所属实体"可填，所以用这个占位值。
 *
 * 复用同一张表带来两个好处（一套 CRUD、视图管理页能看见），一个必须守住的代价：
 * 按实体取视图的地方不能把它当真实体 —— `namedViews` 是 (entityCode, viewType)
 * 双条件过滤，所以表格/图表的视图下拉永远捞不到它。
 */
export const APP_LEVEL_ENTITY_CODE = '*';

/**
 * 组件宽度：网格固定 2 列，1 = 半宽、2 = 整行。
 * 不做 3 列：柱/折线的 SVG 是 720×320 等比缩放，挤进三分之一宽时轴标签会缩到
 * 看不清字，宁可让一张图占半行、一行放两张。
 */
export const TILE_WIDTHS = [1, 2] as const;
export type TileWidth = (typeof TILE_WIDTHS)[number];
export const DEFAULT_TILE_WIDTH: TileWidth = 1;

export interface DashboardWidget {
  /** 指向一行 viewType=CHART 的视图配置。 */
  viewId: number;
  width: TileWidth;
}

export interface DashboardModel {
  name: string;
  widgets: DashboardWidget[];
}

export interface ResolvedWidget extends DashboardWidget {
  view: ViewConfigDTO | null;
  entityCode: string;
  snapshot: ChartViewSnapshot | null;
}

function isTileWidth(value: unknown): value is TileWidth {
  return TILE_WIDTHS.includes(value as TileWidth);
}

/**
 * 认不出是仪表盘时返回 null，而不是给一个空仪表盘：
 * 只有返回 null，页面才能明确说"这一行存的不是仪表盘配置，打不开"，
 * 而不是把用户已有的视图当成空仪表盘然后保存时覆盖掉。
 */
export function parseDashboard(view: ViewConfigDTO | null | undefined): DashboardModel | null {
  if (!view) return null;
  const parsed = parseViewConfig<Record<string, unknown> | null>(view.config, null);
  if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.widgets)) return null;

  const widgets: DashboardWidget[] = [];
  const seen = new Set<number>();
  for (const entry of parsed.widgets as unknown[]) {
    const raw = (entry ?? {}) as { viewId?: unknown; width?: unknown };
    const viewId = Number(raw.viewId);
    if (!Number.isInteger(viewId) || viewId <= 0 || seen.has(viewId)) continue;
    seen.add(viewId);
    widgets.push({ viewId, width: isTileWidth(raw.width) ? raw.width : DEFAULT_TILE_WIDTH });
  }

  const name = typeof parsed.name === 'string' && parsed.name.trim() ? parsed.name.trim() : null;
  return { name: name ?? `仪表盘 #${view.id ?? '?'}`, widgets };
}

export function dashboardConfig(model: DashboardModel): Record<string, unknown> {
  return { name: model.name, widgets: model.widgets };
}

/**
 * 组件指向的图表视图解析。视图被删了不会把组件悄悄变没 —— 页面要能说出
 * "#id 已删除"，否则用户看到的是"我的仪表盘少了一张图"，只能猜。
 */
export function resolveWidgets(
  model: DashboardModel | null,
  views: ViewConfigDTO[] | undefined,
): ResolvedWidget[] {
  if (!model) return [];
  const all = views ?? [];
  return model.widgets.map((widget) => {
    const view = all.find((item) => item.id === widget.viewId) ?? null;
    const snapshot = view ? chartViewSnapshot(view) : null;
    return { ...widget, view, entityCode: view?.entityCode ?? '', snapshot };
  });
}

/** 可以被加进出仪表盘的图表视图（按实体分组给下拉用）。 */
export function pickableChartViews(views: ViewConfigDTO[] | undefined): ViewConfigDTO[] {
  return (views ?? []).filter(
    (view) =>
      (view.viewType ?? '').toUpperCase() === 'CHART' &&
      !!view.id &&
      chartViewSnapshot(view) !== null,
  );
}
