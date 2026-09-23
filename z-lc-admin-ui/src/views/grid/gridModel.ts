import type { ColumnMeta, QueryCondition, QuerySort } from '@/api/types';
import type { ResolvedField } from '@/fields';

/**
 * Pure grid model helpers: merging the entity schema with the user's persisted
 * `columnMeta`, plus condition/sort normalisation. Kept free of React so it is
 * trivially testable and reusable by CSV export.
 */

export interface VisibleColumn {
  resolved: ResolvedField;
  meta: ColumnMeta;
}

/**
 * Order/visibility/width come from `columnMeta`; the schema is authoritative
 * about *existence*. New schema fields land at the end, and stale meta entries
 * are dropped.
 */
export function projectColumns(
  resolvedFields: ResolvedField[],
  columnMeta: ColumnMeta[],
): VisibleColumn[] {
  const byCode = new Map(resolvedFields.map((field) => [field.ctx.field.fieldCode, field]));
  const used = new Set<string>();
  const ordered: VisibleColumn[] = [];

  for (const meta of columnMeta) {
    const resolved = byCode.get(meta.fieldCode);
    if (!resolved || used.has(meta.fieldCode)) continue;
    used.add(meta.fieldCode);
    ordered.push({ resolved, meta });
  }

  for (const resolved of resolvedFields) {
    const code = resolved.ctx.field.fieldCode;
    if (used.has(code)) continue;
    ordered.push({ resolved, meta: { fieldCode: code } });
  }

  return ordered;
}

export function isHidden(column: VisibleColumn): boolean {
  return column.meta.hidden === true;
}

export function visibleColumns(columns: VisibleColumn[]): VisibleColumn[] {
  return columns.filter((column) => !isHidden(column));
}

/** Reorder `moveCode` to sit immediately before/after `targetCode`. */
export function reorderColumnMeta(
  meta: ColumnMeta[],
  moveCode: string,
  targetCode: string,
): ColumnMeta[] {
  const from = meta.findIndex((item) => item.fieldCode === moveCode);
  const to = meta.findIndex((item) => item.fieldCode === targetCode);
  if (from < 0 || to < 0 || from === to) return meta;
  const next = [...meta];
  const [moved] = next.splice(from, 1);
  if (!moved) return next;
  next.splice(to, 0, moved);
  return next;
}

export function setColumnMeta(
  meta: ColumnMeta[],
  fieldCode: string,
  patch: Partial<ColumnMeta>,
): ColumnMeta[] {
  const exists = meta.some((item) => item.fieldCode === fieldCode);
  if (!exists) return [...meta, { fieldCode, ...patch }];
  return meta.map((item) => (item.fieldCode === fieldCode ? { ...item, ...patch } : item));
}

/** Drop conditions referencing fields that no longer exist in the schema. */
export function pruneStaleConditions(
  conditions: QueryCondition[],
  allowedFieldCodes: Set<string>,
): QueryCondition[] {
  return conditions.filter((condition) => allowedFieldCodes.has(condition.fieldCode));
}

export function pruneStaleSorts(sorts: QuerySort[], allowedFieldCodes: Set<string>): QuerySort[] {
  return sorts.filter((sort) => allowedFieldCodes.has(sort.fieldCode));
}

/** Antd table sort object -> our persisted sort list. */
export function toSorts(sort: unknown): QuerySort[] {
  const list = Array.isArray(sort) ? sort : sort ? [sort] : [];
  const out: QuerySort[] = [];
  for (const item of list) {
    const entry = item as { field?: string | number; order?: string | false | null };
    if (!entry?.field || !entry.order) continue;
    out.push({
      fieldCode: String(entry.field),
      dir: entry.order === 'ascend' ? 'asc' : 'desc',
    });
  }
  return out;
}
