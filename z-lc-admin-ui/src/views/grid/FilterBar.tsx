import { useMemo } from 'react';
import { Button, Select, Space, Tooltip, Typography } from 'antd';
import { CloseOutlined, PlusOutlined, SwapOutlined } from '@ant-design/icons';
import type { Conjunction, QueryCondition } from '@/api/types';
import { OPERATOR_LABELS } from '@/fields/defaults';
import { NULL_OPERATORS } from '@/api/types';
import type { ResolvedField } from '@/fields';

const { Text } = Typography;

export interface FilterState {
  conditions: QueryCondition[];
  conjunction: Conjunction;
}

/**
 * AND/OR filter bar.
 *
 * Field and operator pickers are constrained to what the backend will accept:
 * only declared entity fields (anything else is a 400 from
 * `appendOneCondition`) and only that field type's operator whitelist. The
 * value control itself comes from the field registry, so every type renders
 * its own editor without this component knowing any types.
 */
export function FilterBar({
  fields,
  value,
  onChange,
}: {
  fields: ResolvedField[];
  value: FilterState;
  onChange: (next: FilterState) => void;
}) {
  const filterable = useMemo(() => fields.filter((field) => field.filterable), [fields]);
  const byCode = useMemo(
    () => new Map(filterable.map((field) => [field.ctx.field.fieldCode, field])),
    [filterable],
  );

  const conditions = value.conditions;

  const update = (index: number, patch: Partial<QueryCondition>) => {
    const next = conditions.map((condition, position) =>
      position === index ? { ...condition, ...patch } : condition,
    );
    onChange({ ...value, conditions: next });
  };

  const addRow = () => {
    const first = filterable[0];
    if (!first) return;
    onChange({
      ...value,
      conditions: [
        ...conditions,
        { fieldCode: first.ctx.field.fieldCode, operator: first.operators[0] ?? 'eq', value: undefined },
      ],
    });
  };

  const removeRow = (index: number) => {
    onChange({ ...value, conditions: conditions.filter((_, position) => position !== index) });
  };

  if (filterable.length === 0) {
    return (
      <Text type="secondary" style={{ fontSize: 12 }}>
        该实体的字段均不支持过滤（未声明字段或仅含系统列）。
      </Text>
    );
  }

  return (
    <div style={{ width: 640 }}>
      <Space size={8} style={{ marginBottom: 10 }}>
        <Text strong style={{ fontSize: 13 }}>
          筛选条件
        </Text>
        <Tooltip title="后端按此连接字符合并全部条件；OR 会把条件包在一个括号组内">
          <Button
            size="small"
            icon={<SwapOutlined />}
            onClick={() =>
              onChange({ ...value, conjunction: value.conjunction === 'AND' ? 'OR' : 'AND' })
            }
          >
            {value.conjunction === 'AND' ? '并且 (AND)' : '或者 (OR)'}
          </Button>
        </Tooltip>
        <div style={{ flex: 1 }} />
        <Button size="small" icon={<PlusOutlined />} onClick={addRow} disabled={conditions.length >= 8}>
          添加条件
        </Button>
      </Space>

      {conditions.length === 0 ? (
        <div className="zlc-empty-block" style={{ padding: '18px 8px' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            暂无条件，点击「添加条件」开始
          </Text>
        </div>
      ) : (
        conditions.map((condition, index) => {
          const resolved = byCode.get(condition.fieldCode) ?? filterable[0];
          if (!resolved) return null;
          const needsValue = !NULL_OPERATORS.includes(condition.operator);
          return (
            <div key={index} className="zlc-filter-row">
              <Select
                size="small"
                style={{ width: '100%' }}
                showSearch
                optionFilterProp="label"
                value={resolved.ctx.field.fieldCode}
                options={filterable.map((field) => ({
                  value: field.ctx.field.fieldCode,
                  label: field.ctx.field.fieldName || field.ctx.field.fieldCode,
                }))}
                onChange={(nextCode) => {
                  const nextField = byCode.get(nextCode) ?? resolved;
                  update(index, {
                    fieldCode: nextCode,
                    operator: nextField.operators[0] ?? 'eq',
                    value: undefined,
                  });
                }}
              />
              <Select
                size="small"
                style={{ width: '100%' }}
                value={condition.operator}
                options={resolved.operators.map((operator) => ({
                  value: operator,
                  label: OPERATOR_LABELS[operator] ?? operator,
                }))}
                onChange={(nextOperator) => update(index, { operator: nextOperator, value: undefined })}
              />
              <div style={{ minWidth: 0 }}>
                {needsValue ? (
                  resolved.def.renderFilterInput({
                    ctx: resolved.ctx,
                    operator: condition.operator,
                    value: condition.value,
                    onChange: (next) => update(index, { value: next }),
                  })
                ) : (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    （无需取值）
                  </Text>
                )}
              </div>
              <Tooltip title="移除该条件">
                <Button
                  size="small"
                  type="text"
                  danger
                  icon={<CloseOutlined />}
                  onClick={() => removeRow(index)}
                />
              </Tooltip>
            </div>
          );
        })
      )}

      <Space style={{ marginTop: 6 }}>
        <Button
          size="small"
          type="link"
          style={{ paddingInline: 0 }}
          onClick={() => onChange({ ...value, conditions: [] })}
          disabled={conditions.length === 0}
        >
          清空全部
        </Button>
      </Space>
    </div>
  );
}
