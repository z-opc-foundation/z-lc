import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Space, Spin, Typography, message } from 'antd';
import type { EntityDefDTO, LcRow } from '@/lc/api/types';
import { createRecord, getRecord, updateRecord } from '@/lc/api/runtime';
import { isBlank } from '@yuku123/render/fields';
import type { ResolvedField } from '@yuku123/render/fields';
import { StateBlock } from '@/lc/components/StateBlock';

const { Text, Title } = Typography;

/**
 * Schema-driven create / edit form. Every control comes from the field
 * registry's `renderFormInput`, so adding a field type never touches this file.
 */
export interface RecordFormProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  /** Present ⇒ edit mode; absent ⇒ create mode. */
  recordId?: number;
  onSaved: (id: number) => void;
  onCancel: () => void;
}

type FormValues = Record<string, unknown>;

function defaultsOf(fields: ResolvedField[]): FormValues {
  const values: FormValues = {};
  for (const resolved of fields) {
    const def = resolved.ctx.field;
    values[def.fieldCode] = resolved.def.toFormValue(def.defaultValue ?? null, resolved.ctx);
  }
  return values;
}

/** Client-side mirror of the backend's field rules, so a bad submit never round-trips. */
function validateOf(resolved: ResolvedField, value: unknown): string | null {
  const def = resolved.ctx.field;
  if (def.required && isBlank(value)) return `${def.fieldName || def.fieldCode} 不能为空`;
  const max = def.fieldLength;
  if (max && typeof value === 'string' && value.length > max) {
    return `${def.fieldName || def.fieldCode} 最多 ${max} 个字符`;
  }
  if ((def.fieldType === 'INT' || def.fieldType === 'LONG') && typeof value === 'number') {
    if (!Number.isFinite(value)) return `${def.fieldName || def.fieldCode} 必须是数字`;
  }
  return null;
}

export function RecordForm({
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  recordId,
  onSaved,
  onCancel,
}: RecordFormProps) {
  const entityCode = entity.entityCode ?? '';
  const [values, setValues] = useState<FormValues>(() => defaultsOf(resolvedFields));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setValues(defaultsOf(resolvedFields));
    setErrors({});
  }, [resolvedFields, recordId]);

  useEffect(() => {
    if (!recordId) return;
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    getRecord(entityCode, recordId, { appCode, tenantCode })
      .then((row: LcRow | null) => {
        if (cancelled || !row) return;
        setValues((prev) => {
          const next = { ...prev };
          for (const resolved of resolvedFields) {
            const code = resolved.ctx.field.fieldCode;
            next[code] = resolved.def.toFormValue(row[code], resolved.ctx);
          }
          return next;
        });
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoadError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [recordId, entityCode, appCode, tenantCode, resolvedFields]);

  const setValue = useCallback((code: string, value: unknown) => {
    setValues((prev) => ({ ...prev, [code]: value }));
    setErrors((prev) => {
      if (!prev[code]) return prev;
      const next = { ...prev };
      delete next[code];
      return next;
    });
  }, []);

  const submit = useCallback(async () => {
    const nextErrors: Record<string, string> = {};
    for (const resolved of resolvedFields) {
      const code = resolved.ctx.field.fieldCode;
      const error = validateOf(resolved, values[code]);
      if (error) nextErrors[code] = error;
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      message.warning(`请检查 ${Object.keys(nextErrors).length} 处填写`);
      return;
    }

    const payload: Record<string, unknown> = {};
    for (const resolved of resolvedFields) {
      const code = resolved.ctx.field.fieldCode;
      payload[code] = resolved.def.toFieldValue(values[code], resolved.ctx);
    }

    setSaving(true);
    try {
      if (recordId) {
        await updateRecord(entityCode, recordId, payload, { appCode, tenantCode });
        message.success('已保存');
        onSaved(recordId);
      } else {
        const created = await createRecord(entityCode, payload, { appCode, tenantCode });
        message.success('已创建');
        onSaved(Number(created));
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '提交失败');
    } finally {
      setSaving(false);
    }
  }, [resolvedFields, values, recordId, entityCode, appCode, tenantCode, onSaved]);

  const formItems = useMemo(
    () =>
      resolvedFields.map((resolved) => {
        const def = resolved.ctx.field;
        return (
          <div key={def.fieldCode} className="zlc-form-row">
            <label htmlFor={`f-${def.fieldCode}`} className="zlc-form-label">
              {def.fieldName || def.fieldCode}
              {def.required ? <span style={{ color: '#d4380d', marginLeft: 4 }}>*</span> : null}
            </label>
            <div className="zlc-form-control">
              {resolved.def.renderFormInput({
                ctx: resolved.ctx,
                value: values[def.fieldCode],
                onChange: (next) => setValue(def.fieldCode, next),
                disabled: saving,
              })}
              {errors[def.fieldCode] ? (
                <Text type="danger" style={{ display: 'block', marginTop: 4 }}>
                  {errors[def.fieldCode]}
                </Text>
              ) : def.description ? (
                <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
                  {def.description}
                </Text>
              ) : null}
            </div>
          </div>
        );
      }),
    [resolvedFields, values, errors, saving, setValue],
  );

  return (
    <div style={{ padding: 16, maxWidth: 720 }}>
      <Title level={5} style={{ marginTop: 0 }}>
        {recordId ? `编辑「${entity.entityName || entityCode}」` : `新建「${entity.entityName || entityCode}」`}
      </Title>
      <StateBlock isLoading={loading} isError={Boolean(loadError)} error={loadError} isEmpty={false}>
        <Spin spinning={saving}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>{formItems}</div>
          <Space style={{ marginTop: 20 }}>
            <Button type="primary" onClick={() => void submit()} loading={saving}>
              {recordId ? '保存' : '创建'}
            </Button>
            <Button onClick={onCancel} disabled={saving}>
              取消
            </Button>
          </Space>
        </Spin>
      </StateBlock>
    </div>
  );
}
