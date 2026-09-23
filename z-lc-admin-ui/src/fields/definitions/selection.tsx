import { Radio, Select, Tag } from 'antd';
import type { DictItemDTO } from '@/api/types';
import type { FieldDefinition } from '../types';
import { DEFAULT_FIELD_TYPE_INDEX, operatorsForValueType } from '../defaults';
import { asText, isBlank, toWireValue } from '../coerce';
import { NullCell, editorFillStyle } from './shared';

/**
 * Dict-backed field.
 *
 * The runtime joins `z_lc_dict_item` on **item_code** and returns
 * `{field}_label`, so `itemCode` is the stored value and `itemLabel` is what
 * the user reads. Options come from the workspace meta (`/meta/bundle` dict
 * section or `/dict/items`).
 */
function optionsOf(items?: DictItemDTO[]) {
  return (items ?? [])
    .slice()
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map((item) => ({ value: item.itemCode, label: item.itemLabel || item.itemValue || item.itemCode }));
}

function labelOf(items: DictItemDTO[] | undefined, code: unknown): string {
  if (isBlank(code)) return '';
  const hit = (items ?? []).find((item) => item.itemCode === asText(code));
  return hit ? hit.itemLabel || hit.itemValue || asText(code) : asText(code);
}

export const dictDefinition: FieldDefinition = {
  key: 'dict',
  label: '字典选项',
  order: 5,
  descriptor: {
    ...DEFAULT_FIELD_TYPE_INDEX['STRING']!,
    fieldType: 'STRING',
    widget: 'select',
    label: '字典选项',
    groupable: true,
  },
  operators: operatorsForValueType('String'),
  configFields: [
    { key: 'dictCode', label: '字典', kind: 'dictSelect', required: true },
    { key: 'defaultValue', label: '默认值', kind: 'text', help: '填写字典项 itemCode' },
    { key: 'description', label: '说明', kind: 'text' },
  ],
  renderCell: ({ value, displayValue, ctx }) => {
    const label = isBlank(displayValue) ? labelOf(ctx.dictItems, value) : asText(displayValue);
    if (isBlank(value) && isBlank(label)) return <NullCell />;
    return (
      <Tag
        style={{
          maxWidth: '100%',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          verticalAlign: 'middle',
        }}
        bordered={false}
        color="processing"
      >
        {label || asText(value)}
      </Tag>
    );
  },
  renderEditor: ({ value, ctx, onCommit }) => (
    <Select
      size="small"
      autoFocus
      defaultOpen
      showSearch
      optionFilterProp="label"
      style={editorFillStyle}
      defaultValue={isBlank(value) ? undefined : asText(value)}
      options={optionsOf(ctx.dictItems)}
      onChange={(next) => onCommit(next)}
      placeholder="选择"
    />
  ),
  renderFormInput: ({ value, onChange, ctx, disabled, preview }) =>
    preview ? (
      <span>{labelOf(ctx.dictItems, value) || '—'}</span>
    ) : (
      <Select
        style={editorFillStyle}
        disabled={disabled}
        allowClear
        showSearch
        optionFilterProp="label"
        value={isBlank(value) ? undefined : asText(value)}
        options={optionsOf(ctx.dictItems)}
        onChange={(next) => onChange(next)}
        placeholder="请选择"
        notFoundContent="字典无选项，请先在字典管理中维护"
      />
    ),
  renderFilterInput: ({ value, onChange, ctx, operator }) =>
    operator === 'in' || operator === 'notIn' ? (
      <Select
        mode="multiple"
        size="small"
        style={editorFillStyle}
        maxTagCount="responsive"
        value={Array.isArray(value) ? value : []}
        options={optionsOf(ctx.dictItems)}
        onChange={(next) => onChange(next)}
        placeholder="多选"
      />
    ) : (
      <Select
        size="small"
        style={editorFillStyle}
        allowClear
        showSearch
        optionFilterProp="label"
        value={isBlank(value) ? undefined : asText(value)}
        options={optionsOf(ctx.dictItems)}
        onChange={(next) => onChange(next)}
        placeholder="选择"
      />
    ),
  toCellText: ({ value, displayValue, ctx }) =>
    isBlank(displayValue) ? labelOf(ctx.dictItems, value) : asText(displayValue),
  toFormValue: (raw) => (isBlank(raw) ? undefined : asText(raw)),
  toFieldValue: (formValue) => toWireValue('STRING', formValue),
  groupValue: ({ value, displayValue, ctx }) =>
    isBlank(displayValue) ? labelOf(ctx.dictItems, value) || '(空)' : asText(displayValue) || '(空)',
};

/** Radio variant, selected when `/meta/field-types` reports `widget: "radio"`. */
export const radioDefinition: FieldDefinition = {
  ...dictDefinition,
  key: 'widget:radio',
  label: '单选 (widget)',
  descriptor: { ...dictDefinition.descriptor, widget: 'radio' },
  // Radio has no intermediate state: pick one, commit immediately.
  renderEditor: ({ value, ctx, onCommit }) => (
    <Radio.Group
      size="small"
      value={isBlank(value) ? undefined : asText(value)}
      options={optionsOf(ctx.dictItems)}
      onChange={(e) => onCommit(e.target.value)}
    />
  ),
  renderFormInput: ({ value, onChange, ctx, disabled }) => (
    <Radio.Group
      disabled={disabled}
      value={isBlank(value) ? undefined : asText(value)}
      options={optionsOf(ctx.dictItems)}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
};

export const selectionDefinitions: FieldDefinition[] = [dictDefinition, radioDefinition];
