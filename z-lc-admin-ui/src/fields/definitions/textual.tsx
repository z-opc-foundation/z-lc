import { useState } from 'react';
import type { ChangeEvent } from 'react';
import { Input } from 'antd';
import type { FilterOperator } from '@/api/types';
import type { FieldDefinition } from '../types';
import { DEFAULT_FIELD_TYPE_INDEX, operatorsForValueType } from '../defaults';
import { asText, isBlank, toWireValue } from '../coerce';
import { ClampedText, NullCell, editorFillStyle, inlineKeyHandlers } from './shared';

/** Uncontrolled-on-purpose inline editor: keeps a local draft until commit. */
function InlineTextEditor({
  initial,
  multiline,
  onCommit,
  onCancel,
}: {
  initial: string;
  multiline?: boolean;
  onCommit: (next: string) => void;
  onCancel: () => void;
}) {
  const [draft, setDraft] = useState(initial);
  const commit = () => onCommit(draft);
  const common = {
    autoFocus: true,
    size: 'small' as const,
    value: draft,
    onChange: (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setDraft(e.target.value),
    onBlur: commit,
    ...inlineKeyHandlers(commit, onCancel),
  };
  return multiline ? (
    <Input.TextArea {...common} rows={4} style={editorFillStyle} />
  ) : (
    <Input {...common} style={editorFillStyle} onPressEnter={commit} />
  );
}

function cellText(value: unknown): string {
  return isBlank(value) ? '' : asText(value);
}

/** STRING — single line text. */
export const stringDefinition: FieldDefinition = {
  key: 'STRING',
  label: '单行文本',
  order: 10,
  descriptor: DEFAULT_FIELD_TYPE_INDEX['STRING']!,
  operators: operatorsForValueType('String'),
  configFields: [
    { key: 'fieldLength', label: '最大长度', kind: 'number', min: 1, max: 4000, help: 'VARCHAR(n)' },
    { key: 'defaultValue', label: '默认值', kind: 'text' },
    { key: 'description', label: '说明', kind: 'text' },
  ],
  renderCell: ({ value }) =>
    isBlank(value) ? <NullCell /> : <ClampedText value={cellText(value)} />,
  renderEditor: ({ value, onCommit, onCancel }) => (
    <InlineTextEditor initial={cellText(value)} onCommit={onCommit} onCancel={onCancel} />
  ),
  renderFormInput: ({ value, onChange, disabled, preview }) =>
    preview ? (
      <span>{cellText(value) || '—'}</span>
    ) : (
      <Input
        style={editorFillStyle}
        disabled={disabled}
        value={cellText(value)}
        onChange={(e) => onChange(e.target.value)}
        allowClear
      />
    ),
  renderFilterInput: ({ value, onChange, operator }) => (
    <Input
      size="small"
      style={editorFillStyle}
      placeholder={operator === 'in' ? '多个值用英文逗号分隔' : '输入文本'}
      value={cellText(value)}
      onChange={(e) =>
        onChange(operator === 'in' ? e.target.value.split(',').map((s) => s.trim()).filter(Boolean) : e.target.value)
      }
    />
  ),
  toCellText: ({ value }) => cellText(value),
  toFormValue: (raw) => (isBlank(raw) ? undefined : asText(raw)),
  toFieldValue: (formValue) => toWireValue('STRING', formValue),
};

/** TEXT — multiline / long string. Not inline editable (opens the record). */
export const textDefinition: FieldDefinition = {
  ...stringDefinition,
  key: 'TEXT',
  label: '多行文本',
  order: 20,
  descriptor: DEFAULT_FIELD_TYPE_INDEX['TEXT']!,
  renderCell: ({ value }) =>
    isBlank(value) ? <NullCell /> : <ClampedText value={cellText(value)} />,
  renderEditor: ({ value, onCommit, onCancel }) => (
    <InlineTextEditor initial={cellText(value)} multiline onCommit={onCommit} onCancel={onCancel} />
  ),
  renderFormInput: ({ value, onChange, disabled, preview }) =>
    preview ? (
      <span style={{ whiteSpace: 'pre-wrap' }}>{cellText(value) || '—'}</span>
    ) : (
      <Input.TextArea
        rows={4}
        disabled={disabled}
        value={cellText(value)}
        onChange={(e) => onChange(e.target.value)}
      />
    ),
  toFieldValue: (formValue) => toWireValue('TEXT', formValue),
  supportsInlineEdit: () => false,
};

/** JSON — stored as TEXT/JSON, transported as a JSON string. */
function prettyJson(value: unknown): string {
  if (isBlank(value)) return '';
  if (typeof value === 'object') return JSON.stringify(value);
  try {
    return JSON.stringify(JSON.parse(asText(value)));
  } catch {
    return asText(value);
  }
}

export const jsonDefinition: FieldDefinition = {
  ...stringDefinition,
  key: 'JSON',
  label: 'JSON',
  order: 90,
  descriptor: DEFAULT_FIELD_TYPE_INDEX['JSON']!,
  operators: operatorsForValueType('String'),
  configFields: [{ key: 'description', label: '说明', kind: 'text' }],
  renderCell: ({ value }) =>
    isBlank(value) ? <NullCell /> : <ClampedText value={prettyJson(value)} />,
  renderFormInput: ({ value, onChange, disabled, preview }) =>
    preview ? (
      <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{prettyJson(value) || '—'}</pre>
    ) : (
      <Input.TextArea
        rows={6}
        disabled={disabled}
        value={isBlank(value) ? '' : typeof value === 'string' ? value : JSON.stringify(value, null, 2)}
        onChange={(e) => onChange(e.target.value)}
        placeholder='{"key": "value"}'
      />
    ),
  toCellText: ({ value }) => prettyJson(value),
  toFormValue: (raw) => (isBlank(raw) ? undefined : asText(raw)),
  // Objects must be stringified: the server would otherwise write "[object Object]".
  toFieldValue: (formValue) => toWireValue('JSON', formValue),
  supportsInlineEdit: () => false,
};

export const textualDefinitions: FieldDefinition[] = [stringDefinition, textDefinition, jsonDefinition];

/** Widget-driven variants, used when `/meta/field-types` maps a type to a widget. */
export const textareaWidgetDefinition: FieldDefinition = {
  ...textDefinition,
  key: 'widget:textarea',
  label: '多行文本 (widget)',
};

export const STRING_OPERATORS: readonly FilterOperator[] = stringDefinition.operators;
