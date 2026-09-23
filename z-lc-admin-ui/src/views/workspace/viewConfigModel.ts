import type { ColumnMeta, Conjunction, QueryCondition, QuerySort, ViewConfigDTO } from '@/api/types';
import { parseViewConfig } from '@/api/viewConfig';

/**
 * Persisted grid/form state for one (entity, viewType) pair.
 *
 * Two tiers on purpose:
 *  - localStorage holds the *ephemeral* working state, so a refresh does not
 *    lose column tweaks before anything is explicitly saved;
 *  - `z_lc_view_config` holds *named* views shared by everyone on the tenant.
 *    Its `config` column is a JSON string, so everything passes through
 *    `parseViewConfig` rather than a bare JSON.parse.
 */
export interface WorkspaceViewState {
  columnMeta: ColumnMeta[];
  conditions: QueryCondition[];
  conjunction: Conjunction;
  sorts: QuerySort[];
  /** 页脚统计：字段编码 -> 聚合函数。跟着视图一起存，不是只活在本地。 */
  stats: Record<string, StatFunction>;
}

/** `COUNT` 不在这里：行数是页脚固定项，不需要每列各选一次。 */
export const STAT_FUNCTIONS = ['SUM', 'AVG', 'MIN', 'MAX', 'DISTINCT', 'FILLED'] as const;

/** 数值专属 vs 任意列都能算，前后端共用同一份口径，别写两处。 */
export const NUMERIC_STATS = ['SUM', 'AVG', 'MIN', 'MAX'] as const;
export const UNIVERSAL_STATS = ['DISTINCT', 'FILLED'] as const;
export type StatFunction = (typeof STAT_FUNCTIONS)[number];

export const EMPTY_STATE: WorkspaceViewState = {
  columnMeta: [],
  conditions: [],
  conjunction: 'AND',
  sorts: [],
  stats: {},
};

export const SYSTEM_SORT_COLUMNS = ['id', 'create_time', 'update_time'];

function storageKey(appCode: string, entityCode: string, viewType: string): string {
  return `zlc:state:${appCode}:${entityCode}:${viewType}`;
}

export function readStoredState(
  appCode: string,
  entityCode: string,
  viewType: string,
): WorkspaceViewState | null {
  try {
    const raw = window.localStorage.getItem(storageKey(appCode, entityCode, viewType));
    if (!raw) return null;
    return normalise(JSON.parse(raw) as Partial<WorkspaceViewState>);
  } catch {
    return null;
  }
}

export function writeStoredState(
  appCode: string,
  entityCode: string,
  viewType: string,
  state: WorkspaceViewState,
): void {
  try {
    window.localStorage.setItem(storageKey(appCode, entityCode, viewType), JSON.stringify(state));
  } catch {
    /* private mode / quota: ephemeral state is a nicety, never fatal */
  }
}

/** Tolerate partial or hand-edited payloads from either tier. */
export function normalise(input: Partial<WorkspaceViewState> | null | undefined): WorkspaceViewState {
  const source = input ?? {};
  return {
    columnMeta: Array.isArray(source.columnMeta)
      ? source.columnMeta.filter((item): item is ColumnMeta => Boolean(item?.fieldCode))
      : [],
    conditions: Array.isArray(source.conditions)
      ? source.conditions.filter(
          (item): item is QueryCondition => Boolean(item?.fieldCode) && Boolean(item?.operator),
        )
      : [],
    conjunction: source.conjunction === 'OR' ? 'OR' : 'AND',
    sorts: Array.isArray(source.sorts)
      ? source.sorts.filter(
          (item): item is QuerySort =>
            Boolean(item?.fieldCode) && (item.dir === 'asc' || item.dir === 'desc'),
        )
      : [],
    stats: normaliseStats(source.stats),
  };
}

/** 只认白名单函数名，脏数据里冒出来的别的值一律丢掉。 */
function normaliseStats(input: unknown): Record<string, StatFunction> {
  const out: Record<string, StatFunction> = {};
  if (!input || typeof input !== 'object') {
    return out;
  }
  for (const [key, value] of Object.entries(input as Record<string, unknown>)) {
    const fn = String(value).trim().toUpperCase();
    if (STAT_FUNCTIONS.includes(fn as StatFunction)) {
      out[key] = fn as StatFunction;
    }
  }
  return out;
}

export function namedViews(
  views: ViewConfigDTO[] | undefined,
  entityCode: string,
  viewType: string,
): ViewConfigDTO[] {
  return (views ?? []).filter(
    (view) => view.entityCode === entityCode && (view.viewType ?? '').toUpperCase() === viewType,
  );
}

/** A named view's config, or `null` when it is missing/malformed. */
export function viewState(view: ViewConfigDTO | undefined): WorkspaceViewState | null {
  if (!view) return null;
  const parsed = parseViewConfig<Partial<WorkspaceViewState> | null>(view.config, null);
  return parsed ? normalise(parsed) : null;
}

/** Human label for the view switcher / save dialog. */
export function viewLabel(view: ViewConfigDTO): string {
  try {
    const parsed = JSON.parse(view.config ?? '{}') as { name?: string };
    if (parsed?.name) return parsed.name;
  } catch {
    /* fall through */
  }
  return `视图 #${view.id ?? '?'}`;
}

export function withViewName(state: WorkspaceViewState, name: string): string {
  return JSON.stringify({ ...state, name });
}
