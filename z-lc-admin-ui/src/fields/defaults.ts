import type { FieldTypeDescriptor, FilterOperator } from '@/api/types';
import { normalizeOperator } from '@/api/types';

/**
 * Operator groups mirrored from `AbstractFieldTypeHandler` (the server is the
 * authority; `appendOneCondition` throws a 400 for anything outside the
 * per-type list, so this table is load-bearing, not cosmetic).
 */
export const TEXT_OPS: readonly FilterOperator[] = ['eq', 'ne', 'like', 'notLike', 'in', 'notIn', 'isNull', 'isNotNull'];
export const COMPARE_OPS: readonly FilterOperator[] = ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'in', 'notIn', 'isNull', 'isNotNull'];
export const BOOL_OPS: readonly FilterOperator[] = ['eq', 'ne', 'isNull', 'isNotNull'];
export const DATE_OPS: readonly FilterOperator[] = ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'isNull', 'isNotNull'];
export const JSON_OPS: readonly FilterOperator[] = ['eq', 'ne', 'isNull', 'isNotNull'];

/**
 * Frontend fallback descriptors, mirroring `FieldTypeRegistry.describe()` =
 * the payload of `GET /api/lc/meta/field-types`, one row per `FieldTypeHandler`.
 *
 * Used when that endpoint is not deployed yet, so the UI never breaks.
 */
export const DEFAULT_FIELD_TYPES: FieldTypeDescriptor[] = [
  row('STRING', 'String', '单行文本', 'VARCHAR', 'input', true, true, true, true, TEXT_OPS),
  row('TEXT', 'String', '多行长文本', 'TEXT', 'textarea', false, false, true, true, TEXT_OPS),
  row('INT', 'Number', '整数', 'BIGINT', 'number', true, true, true, true, COMPARE_OPS),
  row('LONG', 'Number', '长整数', 'BIGINT', 'number', true, true, true, true, COMPARE_OPS),
  row('DECIMAL', 'Number', '高精度小数', 'DECIMAL', 'number', true, true, true, true, COMPARE_OPS),
  row('BOOLEAN', 'Boolean', '布尔', 'TINYINT(1)', 'switch', true, true, true, true, BOOL_OPS),
  row('DATE', 'DateTime', '日期', 'DATE', 'datePicker', true, true, true, true, DATE_OPS),
  row('DATETIME', 'DateTime', '日期时间', 'DATETIME', 'datePicker', true, true, true, true, DATE_OPS),
  row('JSON', 'String', 'JSON对象', 'JSON', 'json', false, false, true, false, JSON_OPS),
  row('REF', 'Number', '实体引用', 'BIGINT', 'ref', true, true, true, false, COMPARE_OPS),
];

function row(
  fieldType: string,
  cellValueType: FieldTypeDescriptor['cellValueType'],
  label: string,
  dbType: string,
  widget: string,
  sortable: boolean,
  groupable: boolean,
  filterable: boolean,
  inlineEditable: boolean,
  operators: readonly FilterOperator[],
): FieldTypeDescriptor {
  return {
    fieldType,
    cellValueType,
    label,
    dbType,
    widget,
    sortable,
    groupable,
    filterable,
    inlineEditable,
    operators: [...operators],
  };
}

/**
 * `CellValueType` is a Java enum, so `describe()` may emit `NUMBER` while the
 * documented contract says `Number`. Match case-insensitively.
 */
export type NormalizedCellValueType = 'String' | 'Number' | 'Boolean' | 'DateTime';

export function normalizeCellValueType(raw: string | null | undefined): NormalizedCellValueType {
  const key = (raw ?? 'string').toLowerCase();
  const hit = (['string', 'number', 'boolean', 'datetime'] as const).find((item) => item === key);
  return (hit ?? 'string') as NormalizedCellValueType;
}

/** Operators per value type, used when a descriptor carries no operator list. */
export const OPERATORS_BY_VALUE_TYPE: Record<NormalizedCellValueType, readonly FilterOperator[]> = {
  String: TEXT_OPS,
  Number: COMPARE_OPS,
  Boolean: BOOL_OPS,
  DateTime: DATE_OPS,
};

export function operatorsForValueType(valueType: string): readonly FilterOperator[] {
  return OPERATORS_BY_VALUE_TYPE[normalizeCellValueType(valueType)];
}

/**
 * Clamp a server-advertised operator list down to operators the query builder
 * implements, normalising aliases (`notNull` -> `isNotNull`).
 */
export function sanitizeOperators(
  advertised: readonly string[] | null | undefined,
  fallback: readonly FilterOperator[],
): FilterOperator[] {
  if (!advertised || advertised.length === 0) return [...fallback];
  const out: FilterOperator[] = [];
  for (const candidate of advertised) {
    const normalized = normalizeOperator(candidate);
    if (normalized && !out.includes(normalized)) out.push(normalized);
  }
  return out.length > 0 ? out : [...fallback];
}

export const OPERATOR_LABELS: Record<FilterOperator, string> = {
  eq: '等于',
  ne: '不等于',
  like: '包含',
  notLike: '不包含',
  gt: '大于',
  gte: '大于等于',
  lt: '小于',
  lte: '小于等于',
  in: '属于',
  notIn: '不属于',
  isNull: '为空',
  isNotNull: '不为空',
};

/** Index the default table by upper-cased `fieldType`. */
export const DEFAULT_FIELD_TYPE_INDEX: Record<string, FieldTypeDescriptor> = Object.fromEntries(
  DEFAULT_FIELD_TYPES.map((item) => [item.fieldType.toUpperCase(), item]),
);

export function defaultDescriptor(fieldType: string | null | undefined): FieldTypeDescriptor {
  const key = (fieldType ?? 'STRING').toUpperCase();
  return DEFAULT_FIELD_TYPE_INDEX[key] ?? DEFAULT_FIELD_TYPE_INDEX['STRING']!;
}
