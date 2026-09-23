import { useState } from 'react';
import { InputNumber } from 'antd';
import type { FieldDefinition } from '../types';
import { DEFAULT_FIELD_TYPE_INDEX, operatorsForValueType } from '../defaults';
import { asNumber, asText, isBlank, toWireValue } from '../coerce';
import { ClampedText, NullCell, editorFillStyle, inlineKeyHandlers } from './shared';

function formatNumber(value: unknown, decimals?: number | null): string {
  const num = asNumber(value);
  if (num === null) return '';
  if (typeof decimals === 'number' && decimals >= 0) return num.toFixed(decimals);
  return String(num);
}

function InlineNumberEditor({
  initial,
  step,
  onCommit,
  onCancel,
}: {
  initial: number | null;
  step: number;
  onCommit: (next: number | null) => void;
  onCancel: () => void;
}) {
  const [draft, setDraft] = useState<number | null>(initial);
  const commit = () => onCommit(draft);
  return (
    <InputNumber
      autoFocus
      size="small"
      style={editorFillStyle}
      step={step}
      value={draft}
      controls={false}
      onChange={(next) => setDraft(next === null ? null : Number(next))}
      onBlur={commit}
      {...inlineKeyHandlers(commit, onCancel)}
    />
  );
}

function numericBase(key: string, label: string, order: number, step: number): FieldDefinition {
  const descriptor = DEFAULT_FIELD_TYPE_INDEX[key] ?? DEFAULT_FIELD_TYPE_INDEX['INT']!;
  return {
    key,
    label,
    order,
    descriptor,
    operators: operatorsForValueType('Number'),
    configFields: [
      { key: 'defaultValue', label: '默认值', kind: 'text' },
      ...(key === 'DECIMAL'
        ? [
            {
              key: 'fieldLength' as const,
              label: '精度 (precision)',
              kind: 'number' as const,
              min: 1,
              max: 65,
              help: 'DECIMAL(p,s)',
            },
            { key: 'scale' as const, label: '小数位 (scale)', kind: 'number' as const, min: 0, max: 30 },
          ]
        : []),
      { key: 'description', label: '说明', kind: 'text' as const },
    ],
    renderCell: ({ value }) => {
      if (isBlank(value)) return <NullCell />;
      const num = asNumber(value);
      return (
        <ClampedText
          value={
            num === null
              ? asText(value)
              : formatNumber(value, key === 'DECIMAL' ? undefined : 0)
          }
        />
      );
    },
    renderEditor: ({ value, onCommit, onCancel }) => (
      <InlineNumberEditor
        initial={asNumber(value)}
        step={step}
        onCommit={onCommit}
        onCancel={onCancel}
      />
    ),
    renderFormInput: ({ value, onChange, disabled, preview }) =>
      preview ? (
        <span>{isBlank(value) ? '—' : formatNumber(value, key === 'DECIMAL' ? undefined : 0)}</span>
      ) : (
        <InputNumber
          style={editorFillStyle}
          disabled={disabled}
          step={step}
          value={asNumber(value)}
          onChange={(next) => onChange(next === null ? null : Number(next))}
          placeholder={key === 'DECIMAL' ? '十进制数值' : '数值'}
        />
      ),
    renderFilterInput: ({ value, onChange }) => (
      <InputNumber
        size="small"
        style={editorFillStyle}
        step={step}
        value={asNumber(value)}
        onChange={(next) => onChange(next === null ? undefined : Number(next))}
        placeholder="数值"
      />
    ),
    toCellText: ({ value }) => (isBlank(value) ? '' : asText(formatNumber(value, undefined))),
    toFormValue: (raw) => asNumber(raw),
    toFieldValue: (formValue) => toWireValue(key, formValue),
  };
}

export const intDefinition = numericBase('INT', '整数', 30, 1);
export const longDefinition = numericBase('LONG', '长整数', 35, 1);
export const decimalDefinition = numericBase('DECIMAL', '小数', 40, 0.01);

export const numericDefinitions: FieldDefinition[] = [intDefinition, longDefinition, decimalDefinition];

/** Widget alias used by `/meta/field-types` when `widget === "number"`. */
export const numberWidgetDefinition: FieldDefinition = {
  ...intDefinition,
  key: 'widget:number',
  label: '数值 (widget)',
};
