import type {
  DictDTO,
  DictItemDTO,
  EntityDefDTO,
  FieldDefDTO,
  FieldTypeDescriptor,
  FilterOperator,
  ViewConfigDTO,
} from '@/api/types';
import type { FieldContext, FieldDefinition, ResolvedField } from './types';
import {
  DEFAULT_FIELD_TYPES,
  defaultDescriptor,
  normalizeCellValueType,
  sanitizeOperators,
} from './defaults';
import { jsonDefinition, stringDefinition, textDefinition } from './definitions/textual';
import { numericDefinitions } from './definitions/numeric';
import { booleanDefinition, dateDefinition, dateTimeDefinition } from './definitions/temporal';
import { radioDefinition, dictDefinition } from './definitions/selection';
import { referenceDefinition } from './definitions/reference';

/**
 * Field registry.
 *
 * This module is the ONLY place in the app allowed to interpret a `fieldType`
 * / `widget` / `dictCode` / `refEntity` string. Views receive a
 * {@link ResolvedField} and call its behaviour methods, which keeps grid,
 * form, filter bar, designer and CSV export type-agnostic.
 */

const definitions = new Map<string, FieldDefinition>();

function register(definition: FieldDefinition): void {
  definitions.set(definition.key, definition);
}

// Primary keys: one entry per `fieldType`.
for (const definition of [
  stringDefinition,
  textDefinition,
  jsonDefinition,
  ...numericDefinitions,
  booleanDefinition,
  dateDefinition,
  dateTimeDefinition,
]) {
  register(definition);
}

// Synthetic keys driven by field configuration rather than by type.
register(dictDefinition);
register(radioDefinition);
register(referenceDefinition);

// Widget overrides: used when `/meta/field-types` maps a type onto a different
// editor than the type default (e.g. STRING with `widget: "radio"`).
register({ ...textDefinition, key: 'widget:textarea' });
register({ ...booleanDefinition, key: 'widget:switch' });
register({ ...dateTimeDefinition, key: 'widget:datePicker' });
register({ ...referenceDefinition, key: 'widget:ref' });
register({ ...referenceDefinition, key: 'widget:reference' });
register({ ...jsonDefinition, key: 'widget:json' });
register({ ...stringDefinition, key: 'widget:input' });

export function registerFieldDefinition(definition: FieldDefinition): void {
  register(definition);
}

/**
 * 按 key 取定义。
 *
 * 大小写与两个合成别名（dict / ref）都在这里收口：调用方常常直接把后端
 * `/meta/field-types` 或字段定义里的 fieldType 传进来，原来是精确匹配，
 * 传 'REF' 就会拿到 undefined 然后在页面上炸成 "Cannot read properties of undefined"。
 */
export function getFieldDefinition(key: string): FieldDefinition | undefined {
  if (!key) {
    return undefined;
  }
  const exact = definitions.get(key);
  if (exact) {
    return exact;
  }
  const upper = key.toUpperCase();
  return definitions.get(upper)
    ?? (upper === 'REF' ? definitions.get('ref') : undefined)
    ?? (upper === 'DICT' ? definitions.get('dict') : undefined);
}

/** All registered definitions, ordered for the designer's type palette. */
export function listFieldDefinitions(): FieldDefinition[] {
  return [...definitions.values()]
    .filter((definition) => !definition.key.startsWith('widget:'))
    .filter((definition) => definition.key !== 'dict' && definition.key !== 'ref')
    .sort((a, b) => a.order - b.order);
}

/** Index `/meta/field-types` (or the bundle's `fieldTypes`) by upper-case type. */
export function buildDescriptorIndex(
  fieldTypes: FieldTypeDescriptor[] | null | undefined,
): Record<string, FieldTypeDescriptor> {
  const index: Record<string, FieldTypeDescriptor> = {};
  for (const fallback of DEFAULT_FIELD_TYPES) {
    index[fallback.fieldType.toUpperCase()] = fallback;
  }
  for (const descriptor of fieldTypes ?? []) {
    if (!descriptor || typeof descriptor.fieldType !== 'string') continue;
    index[descriptor.fieldType.toUpperCase()] = descriptor;
  }
  return index;
}

export const DEFAULT_DESCRIPTORS = buildDescriptorIndex(DEFAULT_FIELD_TYPES);

export interface WorkspaceContext {
  appCode: string;
  tenantCode: string;
  entities: EntityDefDTO[];
  dicts: DictDTO[];
  views: ViewConfigDTO[];
  /** `fieldType` (upper-case) -> descriptor. */
  descriptors: Record<string, FieldTypeDescriptor>;
  /** `dictCode` -> items, when the bundle carried them. */
  dictItems: Record<string, DictItemDTO[]>;
}

export function createWorkspaceContext(params: {
  appCode: string;
  tenantCode: string;
  entities?: EntityDefDTO[] | null;
  dicts?: DictDTO[] | null;
  views?: ViewConfigDTO[] | null;
  fieldTypes?: FieldTypeDescriptor[] | null;
}): WorkspaceContext {
  const dicts = params.dicts ?? [];
  const dictItems: Record<string, DictItemDTO[]> = {};
  for (const dict of dicts) {
    if (dict?.dictCode && Array.isArray(dict.items)) {
      dictItems[dict.dictCode] = dict.items;
    }
  }
  return {
    appCode: params.appCode,
    tenantCode: params.tenantCode,
    entities: params.entities ?? [],
    dicts,
    views: params.views ?? [],
    descriptors: buildDescriptorIndex(params.fieldTypes),
    dictItems,
  };
}

/** Always-present runtime columns. Not declared fields, so not filterable. */
export const SYSTEM_FIELD_CODES = ['id', 'create_time', 'update_time'] as const;

export function isSystemField(field: FieldDefDTO): boolean {
  return (SYSTEM_FIELD_CODES as readonly string[]).includes(field.fieldCode);
}

/**
 * Synthetic defs for `id` / `create_time` / `update_time`, which every row
 * carries (`DynamicSqlBuilder.joinColumns` always selects them) but which are
 * usually absent from `fields`. They render and export like real columns but
 * must never reach `conditions`/`sorts`, because `appendOneCondition` and
 * `appendOrderBy` throw for anything outside the declared whitelist.
 */
export function systemFieldDefs(): FieldDefDTO[] {
  return [
    field('id', 'ID', 'LONG'),
    field('create_time', '创建时间', 'DATETIME'),
    field('update_time', '更新时间', 'DATETIME'),
  ];
}

function field(fieldCode: string, fieldName: string, fieldType: string): FieldDefDTO {
  return { fieldCode, fieldName, fieldType, required: true, sortOrder: -1 };
}

/**
 * Resolve a single field into everything a view needs.
 *
 * Precedence: `dictCode` -> dict/radio, `refEntity` or `REF` -> reference,
 * then the descriptor's `widget`, then the raw `fieldType`, then STRING.
 */
export function resolveField(
  fieldDef: FieldDefDTO,
  entity: EntityDefDTO,
  ws: WorkspaceContext,
): ResolvedField {
  const rawType = (fieldDef.fieldType ?? 'STRING').toUpperCase();
  const descriptor = ws.descriptors[rawType] ?? defaultDescriptor(rawType);

  const dict = fieldDef.dictCode ? ws.dicts.find((item) => item.dictCode === fieldDef.dictCode) : undefined;
  const refEntity = fieldDef.refEntity
    ? ws.entities.find((item) => item.entityCode === fieldDef.refEntity)
    : undefined;

  const ctx: FieldContext = {
    field: fieldDef,
    entity,
    appCode: ws.appCode,
    tenantCode: ws.tenantCode,
    descriptor,
    dict,
    dictItems: fieldDef.dictCode ? ws.dictItems[fieldDef.dictCode] ?? dict?.items : undefined,
    refEntity,
    refLabelField: pickLabelField(refEntity),
    dicts: ws.dicts,
    entities: ws.entities,
    views: ws.views,
  };

  const def = pickDefinition(fieldDef, rawType, descriptor, ctx);
  const operators: readonly FilterOperator[] = sanitizeOperators(descriptor.operators, def.operators);

  const system = isSystemField(fieldDef);
  // Derived columns exist whenever the *field* declares the relation; the
  // backend JOINs on dictCode/refEntity regardless of whether our local copy of
  // the dict/entity metadata resolved.
  const displayKey = fieldDef.dictCode
    ? `${fieldDef.fieldCode}_label`
    : fieldDef.refEntity
      ? `${fieldDef.fieldCode}_name`
      : undefined;

  return {
    ctx,
    def,
    valueKey: fieldDef.fieldCode,
    displayKey,
    // System columns are display/export only: the query whitelist would reject
    // them in `conditions`/`sorts` with a 400.
    sortable: !system && descriptor.sortable,
    groupable: !system && descriptor.groupable,
    filterable: !system && descriptor.filterable,
    inlineEditable:
      !system && descriptor.inlineEditable && (def.supportsInlineEdit?.(ctx) ?? true),
    operators,
  };
}

function pickDefinition(
  fieldDef: FieldDefDTO,
  rawType: string,
  descriptor: FieldTypeDescriptor,
  _ctx: FieldContext,
): FieldDefinition {
  const fallback = definitions.get('STRING')!;

  if (fieldDef.dictCode) {
    const key = descriptor.widget === 'radio' ? 'widget:radio' : 'dict';
    return definitions.get(key) ?? definitions.get('dict') ?? fallback;
  }

  if (fieldDef.refEntity || rawType === 'REF') {
    return definitions.get('ref') ?? fallback;
  }

  const byWidget = definitions.get(`widget:${descriptor.widget}`);
  if (byWidget) return byWidget;

  return definitions.get(rawType) ?? fallback;
}

/**
 * Best display column of a referenced entity: an explicit `name`/`title`, else
 * the first non-system text-ish field, else nothing (picker falls back to id).
 */
export function pickLabelField(entity?: EntityDefDTO): FieldDefDTO | undefined {
  if (!entity?.fields?.length) return undefined;
  const fields = entity.fields.filter((item) => item?.fieldCode);
  const named = fields.find((item) => /^(name|title|label)$/i.test(item.fieldCode));
  if (named) return named;
  return (
    fields.find((item) => ['STRING', 'TEXT'].includes((item.fieldType ?? '').toUpperCase())) ??
    fields.find((item) => item.fieldCode !== 'id')
  );
}

/** Resolve every declared field of an entity, ordered by `sortOrder` then name. */
export function resolveEntityFields(
  entity: EntityDefDTO,
  ws: WorkspaceContext,
): ResolvedField[] {
  const fields = [...(entity.fields ?? [])].filter((item) => item?.fieldCode);
  fields.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.fieldCode.localeCompare(b.fieldCode));
  return fields.map((fieldDef) => resolveField(fieldDef, entity, ws));
}

/** Cell value + derived display value pulled out of a row for a resolved field. */
export function readFieldValue(row: Record<string, unknown>, resolved: ResolvedField) {
  const value = row[resolved.valueKey];
  const displayValue = resolved.displayKey ? row[resolved.displayKey] : undefined;
  return { value, displayValue };
}

export type { FieldContext, FieldDefinition, ResolvedField };
export { normalizeCellValueType };
