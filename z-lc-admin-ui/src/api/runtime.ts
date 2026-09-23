import { DEFAULT_TENANT_CODE, request } from './client';
import type {
  Conjunction,
  LcFilters,
  LcRow,
  PageResult,
  QueryCondition,
  QuerySort,
  RuntimeQuery,
} from './types';

/** `DynamicSqlBuilder` clamps `size` with `Math.min(size, 200)`. */
export const MAX_PAGE_SIZE = 200;

export interface RuntimeScope {
  appCode: string;
  tenantCode?: string;
}

function tenantOf(ctx: RuntimeScope): string {
  return ctx.tenantCode ?? DEFAULT_TENANT_CODE;
}

/**
 * Paged record query.
 *
 * `entityCode`/`appCode`/`tenantCode` go as **query params** (they override the
 * body), while the filter map goes as the body. `filters` keys carry the
 * operator suffix (`name:like`, `age:gte`, `status:in`); unknown suffix
 * silently degrades to `eq` server-side, so only send supported operators.
 */
export function listRecords(
  entityCode: string,
  ctx: RuntimeScope,
  query: Partial<RuntimeQuery> = {},
): Promise<PageResult<LcRow>> {
  return request<PageResult<LcRow>>('/runtime/list', {
    method: 'POST',
    query: { entityCode, tenantCode: tenantOf(ctx), appCode: ctx.appCode },
    body: {
      page: query.page ?? 1,
      size: Math.min(query.size ?? 20, MAX_PAGE_SIZE),
      filters: query.filters ?? {},
      conditions: query.conditions ?? [],
      conjunction: query.conjunction ?? 'AND',
      sorts: query.sorts ?? [],
      orderBy: query.orderBy,
    },
  });
}

export function getRecord(entityCode: string, id: number, ctx: RuntimeScope): Promise<LcRow | null> {
  return request<LcRow | null>('/runtime/get', {
    method: 'POST',
    query: { entityCode },
    body: { entityCode, appCode: ctx.appCode, tenantCode: tenantOf(ctx), id },
  });
}

/** Returns the newly generated primary key. */
export function createRecord(
  entityCode: string,
  fieldValues: Record<string, unknown>,
  ctx: RuntimeScope,
): Promise<number> {
  return request<number>('/runtime/create', {
    method: 'POST',
    query: { entityCode },
    body: { entityCode, appCode: ctx.appCode, tenantCode: tenantOf(ctx), fieldValues },
  });
}

/**
 * Update a record. `fieldValues.id` is **required** and consumed by the
 * backend (it is removed from the map and used as the WHERE key).
 * Returns affected-row count, not the id.
 */
export function updateRecord(
  entityCode: string,
  id: number,
  fieldValues: Record<string, unknown>,
  ctx: RuntimeScope,
): Promise<number> {
  return request<number>('/runtime/update', {
    method: 'POST',
    query: { entityCode },
    body: {
      entityCode,
      appCode: ctx.appCode,
      tenantCode: tenantOf(ctx),
      // Backend does `fieldValues.remove("id")`, so id must ride inside.
      fieldValues: { ...fieldValues, id },
    },
  });
}

/** Soft delete. Returns affected-row count. */
export function deleteRecord(entityCode: string, id: number, ctx: RuntimeScope): Promise<number> {
  return request<number>('/runtime/delete', {
    method: 'POST',
    query: { entityCode },
    body: { entityCode, appCode: ctx.appCode, tenantCode: tenantOf(ctx), id },
  });
}

/** One record the server refused to delete; `id` rides along so the UI needn't re-index. */
export interface BatchDeleteError {
  index: number;
  id: number;
  message: string;
}

export interface BatchDeleteResult {
  /** Deduped count the server actually looked at — the "N 条" shown to users. */
  total: number;
  deletedCount: number;
  applied: boolean;
  rolledBack: boolean;
  ids: number[];
  errors: BatchDeleteError[];
  message?: string | null;
}

/**
 * Bulk delete in **one** request. Server-side semantics match `/import/commit`:
 * the whole batch is pre-flighted, and a single un-deletable id means nothing was
 * deleted (`applied: false`, `deletedCount: 0`) rather than a half-deleted set.
 *
 * ⚠ Callers must read the counts off this envelope — never infer "how many were
 * deleted" from how many requests were sent. That is the exact accounting bug the
 * old sequential loop had.
 */
export function deleteBatch(
  entityCode: string,
  ids: number[],
  ctx: RuntimeScope,
): Promise<BatchDeleteResult> {
  return request<BatchDeleteResult>('/runtime/delete-batch', {
    method: 'POST',
    query: { entityCode },
    body: { entityCode, appCode: ctx.appCode, tenantCode: tenantOf(ctx), ids },
  });
}

/* ------------------------------------------------------------------ */
/* Filter helpers                                                      */
/* ------------------------------------------------------------------ */

/** A condition is blank when it has no usable value (except null-tests). */
export function isBlankCondition(condition: QueryCondition): boolean {
  if (condition.operator === 'isNull' || condition.operator === 'isNotNull') return false;
  const { value } = condition;
  if (value === null || value === undefined || value === '') return true;
  if (Array.isArray(value)) return value.length === 0;
  return false;
}

export function pruneConditions(conditions: QueryCondition[]): QueryCondition[] {
  return conditions.filter((condition) => condition.fieldCode && !isBlankCondition(condition));
}

/**
 * Legacy operator-suffixed filter map (`name:like`). Kept for completeness and
 * used when a caller explicitly wants the AND-only dialect.
 */
export function toFilterMap(conditions: QueryCondition[]): LcFilters {
  const filters: LcFilters = {};
  for (const condition of pruneConditions(conditions)) {
    const key =
      condition.operator === 'eq' ? condition.fieldCode : `${condition.fieldCode}:${condition.operator}`;
    if (Array.isArray(condition.value)) {
      filters[key] = condition.value as (string | number)[];
    } else if (typeof condition.value === 'object') {
      filters[key] = JSON.stringify(condition.value);
    } else {
      filters[key] = condition.value as string | number | boolean;
    }
  }
  return filters;
}

/** `{field, dir}` pairs for the structured `sorts` field. */
export function toSorts(
  sort?: { field: string; order: 'ascend' | 'descend' } | null,
): QuerySort[] {
  if (!sort?.field) return [];
  return [{ fieldCode: sort.field, dir: sort.order === 'ascend' ? 'asc' : 'desc' }];
}

/** Legacy single `orderBy` string, e.g. `"create_time desc"`. */
export function buildOrderBy(field?: string, direction?: 'ascend' | 'descend'): string | undefined {
  if (!field || !direction) return undefined;
  return `${field} ${direction === 'ascend' ? 'asc' : 'desc'}`;
}

export type { Conjunction };

/**
 * One grouped row from `POST /runtime/aggregate`.
 * `group_key` / `group_label` are absent when no groupField was sent (总计行),
 * and `sum_<field>` / `avg_<field>` keys appear per requested aggregation.
 */
export interface AggregateRow {
  group_key?: string | null;
  group_label?: string | null;
  group_count?: number;
  /** 时间分桶 (timeGroup) 才出现：后端只回数字段，标签由前端拼，避免各库日期格式化方言。 */
  bucket_year?: number | null;
  bucket_month?: number | null;
  bucket_day?: number | null;
  [aggregationColumn: string]: string | number | null | undefined;
}

/** 与后端 DynamicSqlBuilder 的白名单一致；WEEK 没有跨数据库可移植写法，所以不支持。 */
export type TimeGroup = 'DAY' | 'MONTH' | 'YEAR';

export interface AggregateRequest {
  groupField?: string;
  /** groupField 是日期列时按 日/月/年 分桶。 */
  timeGroup?: TimeGroup;
  aggregations?: Record<string, string[]>;
  filters?: LcFilters;
  conditions?: QueryCondition[];
  conjunction?: Conjunction;
  limit?: number;
}

/** Server-side group-by / statistics; shares /list's filter dialect. */
export function aggregateRecords(
  entityCode: string,
  ctx: RuntimeScope,
  // 参数名不能叫 request —— 会遮蔽从 client 导入的同名函数
  query: AggregateRequest = {},
): Promise<AggregateRow[]> {
  return request<AggregateRow[]>('/runtime/aggregate', {
    method: 'POST',
    query: { entityCode, appCode: ctx.appCode, tenantCode: tenantOf(ctx) },
    body: {
      appCode: ctx.appCode,
      tenantCode: tenantOf(ctx),
      groupField: query.groupField,
      timeGroup: query.timeGroup,
      aggregations: query.aggregations ?? {},
      filters: query.filters ?? {},
      conditions: query.conditions ?? [],
      conjunction: query.conjunction ?? 'AND',
      limit: query.limit ?? 100,
    },
  });
}

/* ------------------------------------------------------------------ */
/* Server-side batch import                                            */
/* ------------------------------------------------------------------ */

export interface ImportRowError {
  index: number;
  message: string;
}

export interface ImportRowWarning {
  index: number;
  fieldCode: string | null;
  message: string;
}

export interface ImportResult {
  total: number;
  validCount: number;
  insertedCount: number;
  applied: boolean;
  rolledBack: boolean;
  ids: number[];
  errors: ImportRowError[];
  /** 不阻断的数据质量提示，比如值不在字典码表里。 */
  warnings: ImportRowWarning[];
  message: string | null;
}

/** 单次请求的行数上限，与后端 ImportDto.MAX_ROWS 对齐。 */
export const IMPORT_MAX_ROWS = 2000;

/** 一批最多回传多少条行错误/警告，与后端 ImportDto.MAX_ERRORS_RETURNED 对齐。 */
export const IMPORT_MAX_ROWS_REPORTED = 50;

function importBody(ctx: RuntimeScope, records: Record<string, unknown>[]) {
  return { appCode: ctx.appCode, tenantCode: tenantOf(ctx), records };
}

/** 第一段：只校验，零写入。和单条 create 共用同一套 pipeline，所以预览即真值。 */
export function importPreview(
  entityCode: string,
  ctx: RuntimeScope,
  records: Record<string, unknown>[],
): Promise<ImportResult> {
  return request<ImportResult>('/runtime/import/preview', {
    method: 'POST',
    query: { entityCode },
    body: importBody(ctx, records),
  });
}

/** 第二段：落库。任一行不合法则整批不写；中途失败会补偿回滚本批已插入的行。 */
export function importCommit(
  entityCode: string,
  ctx: RuntimeScope,
  records: Record<string, unknown>[],
): Promise<ImportResult> {
  return request<ImportResult>('/runtime/import/commit', {
    method: 'POST',
    query: { entityCode },
    body: importBody(ctx, records),
  });
}
