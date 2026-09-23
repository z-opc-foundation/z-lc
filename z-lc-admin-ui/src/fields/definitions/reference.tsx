import { useState } from 'react';
import type { ReactNode } from 'react';
import { InputNumber, Select, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { listRecords } from '@/api/runtime';
import type { LcRow } from '@/api/types';
import type { FieldContext, FieldDefinition } from '../types';
import { DEFAULT_FIELD_TYPE_INDEX, operatorsForValueType } from '../defaults';
import { asNumber, asText, isBlank, toWireValue } from '../coerce';
import { NullCell, editorFillStyle } from './shared';

const { Text } = Typography;

/**
 * Reference (REF / `refEntity`) field.
 *
 * The runtime LEFT JOINs `refEntity` as a *table name* on `id` and exposes
 * `{field}_name` sourced from a column literally called `entity_name`. Business
 * tables rarely carry that column, so the derived label is honoured only when
 * actually present; otherwise the picker's own loaded options provide the label
 * and the cell falls back to the visible id.
 */

function rowLabel(row: LcRow, labelField?: string): string {
  if (labelField && !isBlank(row[labelField])) return asText(row[labelField]);
  const derived = row['entity_name'] ?? row['name'] ?? row['title'];
  if (!isBlank(derived)) return asText(derived);
  return `#${asText(row['id'])}`;
}

function useReferenceOptions(ctx: FieldContext, keyword: string) {
  const target = ctx.refEntity?.entityCode;
  const labelField = ctx.refLabelField?.fieldCode;
  return useQuery({
    queryKey: ['reference-options', ctx.appCode, ctx.tenantCode, target, labelField, keyword],
    enabled: Boolean(target),
    staleTime: 30_000,
    queryFn: async (): Promise<LcRow[]> => {
      if (!target) return [];
      const page = await listRecords(
        target,
        { appCode: ctx.appCode, tenantCode: ctx.tenantCode },
        {
          page: 1,
          size: 50,
          conditions:
            keyword && labelField
              ? [{ fieldCode: labelField, operator: 'like' as const, value: keyword }]
              : [],
        },
      );
      return page.records ?? [];
    },
  });
}

function Ellipsis({ children }: { children: ReactNode }) {
  return (
    <span
      style={{
        display: 'block',
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}
    >
      {children}
    </span>
  );
}

function ReferenceSelect({
  ctx,
  value,
  onCommit,
  autoFocus,
  disabled,
  inline,
}: {
  ctx: FieldContext;
  value: unknown;
  onCommit: (next: unknown) => void;
  autoFocus?: boolean;
  disabled?: boolean;
  inline?: boolean;
}) {
  const [keyword, setKeyword] = useState('');
  const { data, isFetching, error } = useReferenceOptions(ctx, keyword);
  const labelField = ctx.refLabelField?.fieldCode;

  const options = (data ?? []).map((row) => ({
    value: asNumber(row['id']) ?? asText(row['id']),
    label: rowLabel(row, labelField),
  }));

  // Keep the current selection visible even when it is outside the loaded page.
  const current = asNumber(value);
  if (current !== null && !options.some((option) => option.value === current)) {
    options.unshift({ value: current, label: `#${current}` });
  }

  return (
    <Select
      size={inline ? 'small' : 'middle'}
      style={editorFillStyle}
      showSearch
      allowClear
      autoFocus={autoFocus}
      disabled={disabled || !ctx.refEntity}
      filterOption={false}
      onSearch={setKeyword}
      loading={isFetching}
      placeholder={ctx.refEntity ? '搜索并选择' : '未配置关联实体'}
      value={current ?? undefined}
      options={options}
      onChange={(next) => onCommit(next ?? null)}
      notFoundContent={
        error ? (
          <Text type="danger" style={{ fontSize: 12 }}>
            关联数据加载失败
          </Text>
        ) : isFetching ? (
          <Text type="secondary" style={{ fontSize: 12 }}>
            加载中
          </Text>
        ) : (
          <Text type="secondary" style={{ fontSize: 12 }}>
            无可选记录
          </Text>
        )
      }
    />
  );
}

export const referenceDefinition: FieldDefinition = {
  key: 'ref',
  label: '关联记录',
  order: 3,
  descriptor: {
    ...DEFAULT_FIELD_TYPE_INDEX['REF']!,
    widget: 'reference',
    label: '关联记录',
  },
  operators: operatorsForValueType('Number'),
  configFields: [
    { key: 'refEntity', label: '关联实体', kind: 'entitySelect', required: true },
    { key: 'defaultValue', label: '默认值', kind: 'text', help: '目标记录 id' },
    { key: 'description', label: '说明', kind: 'text' },
  ],
  renderCell: ({ value, displayValue }) => {
    if (isBlank(value)) return <NullCell />;
    // `_name` is only trustworthy when the backend actually returned it.
    const label = isBlank(displayValue) ? `#${asText(value)}` : asText(displayValue);
    return <Ellipsis>{label}</Ellipsis>;
  },
  renderEditor: ({ value, ctx, onCommit }) => (
    <ReferenceSelect ctx={ctx} value={value} onCommit={onCommit} autoFocus inline />
  ),
  renderFormInput: ({ value, onChange, ctx, disabled, preview }) =>
    preview ? (
      <span>{isBlank(value) ? '—' : `#${asText(value)}`}</span>
    ) : (
      <ReferenceSelect ctx={ctx} value={value} onCommit={onChange} disabled={disabled} />
    ),
  renderFilterInput: ({ value, onChange }) => (
    <InputNumber
      size="small"
      style={editorFillStyle}
      controls={false}
      placeholder="记录 id"
      value={asNumber(value)}
      onChange={(next) => onChange(next === null ? undefined : Number(next))}
    />
  ),
  toCellText: ({ value, displayValue }) =>
    isBlank(displayValue) ? asText(value) : asText(displayValue),
  toFormValue: (raw) => asNumber(raw),
  toFieldValue: (formValue) => toWireValue('REF', formValue),
  supportsInlineEdit: (ctx) => Boolean(ctx.refEntity),
  groupValue: ({ value }) => (isBlank(value) ? '(空)' : `#${asText(value)}`),
};

export const referenceDefinitions: FieldDefinition[] = [referenceDefinition];
