import { Select, Switch, Tag } from 'antd';
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { DatePicker } from 'antd';
import type { Dayjs } from 'dayjs';
import type { FieldDefinition } from '../types';
import { DEFAULT_FIELD_TYPE_INDEX, operatorsForValueType } from '../defaults';
import {
  DATE_FORMAT,
  DATETIME_FORMAT,
  asBoolean,
  asText,
  formatDate,
  isBlank,
  parseDate,
  toWireValue,
} from '../coerce';
import { NullCell, editorFillStyle } from './shared';

/** BOOLEAN. Server-side `Boolean.parseBoolean` makes any non-"true" value false. */
export const booleanDefinition: FieldDefinition = {
  key: 'BOOLEAN',
  label: '布尔',
  order: 50,
  descriptor: DEFAULT_FIELD_TYPE_INDEX['BOOLEAN']!,
  operators: operatorsForValueType('Boolean'),
  configFields: [
    { key: 'defaultValue', label: '默认值', kind: 'text', help: 'true / false' },
    { key: 'description', label: '说明', kind: 'text' },
  ],
  renderCell: ({ value }) => {
    if (isBlank(value)) return <NullCell />;
    const on = asBoolean(value);
    return (
      <Tag
        color={on ? 'green' : 'default'}
        icon={on ? <CheckOutlined /> : <CloseOutlined />}
        style={{ marginInlineEnd: 0 }}
      >
        {on ? '是' : '否'}
      </Tag>
    );
  },
  // A switch has no intermediate state, so it commits immediately.
  renderEditor: ({ value, onCommit }) => (
    <Switch size="small" autoFocus checked={asBoolean(value)} onChange={(next) => onCommit(next)} />
  ),
  renderFormInput: ({ value, onChange, disabled, preview }) =>
    preview ? (
      <span>{isBlank(value) ? '—' : asBoolean(value) ? '是' : '否'}</span>
    ) : (
      <Switch checked={asBoolean(value)} disabled={disabled} onChange={(next) => onChange(next)} />
    ),
  renderFilterInput: ({ value, onChange }) => (
    <Select
      size="small"
      style={editorFillStyle}
      placeholder="任意"
      allowClear
      value={isBlank(value) ? undefined : asBoolean(value)}
      onChange={(next) => onChange(next)}
      options={[
        { value: true, label: '是' },
        { value: false, label: '否' },
      ]}
    />
  ),
  toCellText: ({ value }) => (isBlank(value) ? '' : asBoolean(value) ? 'TRUE' : 'FALSE'),
  toFormValue: (raw) => asBoolean(raw),
  toFieldValue: (formValue) => toWireValue('BOOLEAN', formValue),
  groupValue: ({ value }) => (isBlank(value) ? '(空)' : asBoolean(value) ? '是' : '否'),
};

/** Widget alias for `widget === "switch"`. */
export const switchWidgetDefinition: FieldDefinition = {
  ...booleanDefinition,
  key: 'widget:switch',
  label: '开关 (widget)',
};

/** 真正生效的类型以字段定义为准，缺失时才退回本定义自己的 key。 */
function wireTypeOf(fieldType: string | null | undefined, fallback: 'DATE' | 'DATETIME'): 'DATE' | 'DATETIME' {
  const declared = (fieldType ?? '').toUpperCase();
  return declared === 'DATE' || declared === 'DATETIME' ? (declared as 'DATE' | 'DATETIME') : fallback;
}

function patternOf(fieldType: string | null | undefined, fallback: 'DATE' | 'DATETIME'): string {
  return wireTypeOf(fieldType, fallback) === 'DATE' ? DATE_FORMAT : DATETIME_FORMAT;
}

function temporalBase(key: 'DATE' | 'DATETIME', label: string, order: number): FieldDefinition {
  const pattern = key === 'DATE' ? DATE_FORMAT : DATETIME_FORMAT;
  return {
    key,
    label,
    order,
    descriptor: DEFAULT_FIELD_TYPE_INDEX[key]!,
    operators: operatorsForValueType('DateTime'),
    configFields: [
      { key: 'defaultValue', label: '默认值', kind: 'text', help: pattern.toLowerCase() },
      { key: 'description', label: '说明', kind: 'text' },
    ],
    renderCell: ({ value }) =>
      isBlank(value) ? <NullCell /> : <ClampedDate value={formatDate(value, pattern)} />,
    renderEditor: ({ value, onCommit, onCancel }) => (
      <DatePicker
        size="small"
        autoFocus
        defaultOpen
        showTime={key === 'DATETIME'}
        defaultValue={parseDate(value) ?? undefined}
        style={editorFillStyle}
        onChange={(next: Dayjs | null) => onCommit(next)}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            onCancel();
          }
        }}
      />
    ),
    renderFormInput: ({ value, onChange, disabled, preview }) =>
      preview ? (
        <span>{isBlank(value) ? '—' : formatDate(value, pattern)}</span>
      ) : (
        <DatePicker
          style={editorFillStyle}
          showTime={key === 'DATETIME'}
          disabled={disabled}
          value={parseDate(value) ?? null}
          onChange={(next) => onChange(next)}
          placeholder={key === 'DATE' ? '选择日期' : '选择日期时间'}
        />
      ),
    renderFilterInput: ({ value, onChange }) => (
      <DatePicker
        size="small"
        style={editorFillStyle}
        showTime={key === 'DATETIME'}
        value={parseDate(value) ?? null}
        onChange={(next) => onChange(next)}
        placeholder="选择日期"
      />
    ),
    // 类型身份必须跟着 ctx.field.fieldType 走，不能跟定义走：
    // widget 覆盖 (datePicker / radio 这类) 只该换"控件"，
    // 而本定义会被 `widget:datePicker` 原样继承给 DATE 列，
    // 写死 key 就会让 DATE 列按 DATETIME 序列化（后端 DATE 口径是 yyyy-MM-dd）。
    toCellText: ({ value, ctx }) => {
      const own = patternOf(ctx.field.fieldType, key);
      return isBlank(value) ? '' : formatDate(value, own);
    },
    toFormValue: (raw) => parseDate(raw),
    toFieldValue: (formValue, ctx) => toWireValue(wireTypeOf(ctx.field.fieldType, key), formValue),
    groupValue: ({ value }) => {
      if (isBlank(value)) return '(空)';
      const parsed = parseDate(value);
      if (!parsed) return asText(value);
      // Group by day so DATETIME columns still produce useful buckets.
      return parsed.format(DATE_FORMAT);
    },
  };
}

/** Local import kept below to avoid a cycle with `shared` in the temporal cell. */
function ClampedDate({ value }: { value: string }) {
  return <span style={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>{value}</span>;
}

export const dateDefinition = temporalBase('DATE', '日期', 60);
export const dateTimeDefinition = temporalBase('DATETIME', '日期时间', 70);

export const temporalDefinitions: FieldDefinition[] = [dateDefinition, dateTimeDefinition];

/** Widget alias for `widget === "datePicker"`. */
export const datePickerWidgetDefinition: FieldDefinition = {
  ...dateTimeDefinition,
  key: 'widget:datePicker',
  label: '日期时间 (widget)',
};
