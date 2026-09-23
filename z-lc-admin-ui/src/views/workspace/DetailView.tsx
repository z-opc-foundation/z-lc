import { useEffect, useState } from 'react';
import { Button, Descriptions, Tooltip, Typography, message } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import type { EntityDefDTO, LcRow } from '@/api/types';
import { getRecord } from '@/api/runtime';
import { readFieldValue } from '@/fields';
import type { ResolvedField } from '@/fields';
import { StateBlock } from '@/components/StateBlock';

const { Text } = Typography;

/** Read-only projection of one record, rendered entirely through the registry. */
export interface DetailViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  appCode: string;
  tenantCode: string;
  recordId?: number;
  onEdit: (id: number) => void;
  onBack: () => void;
}

export function DetailView({
  entity,
  resolvedFields,
  appCode,
  tenantCode,
  recordId,
  onEdit,
  onBack,
}: DetailViewProps) {
  const [row, setRow] = useState<LcRow | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    if (!recordId) {
      setRow(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    getRecord(entity.entityCode ?? '', recordId, { appCode, tenantCode })
      .then((data) => {
        if (!cancelled) setRow(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [recordId, entity.entityCode, appCode, tenantCode]);

  if (!recordId) {
    return (
      <div style={{ padding: 24 }}>
        <Text type="secondary">从表格点「详情」查看某条记录。</Text>
      </div>
    );
  }

  return (
    <div style={{ padding: 16, maxWidth: 860 }}>
      <StateBlock
        isLoading={loading}
        isError={Boolean(error)}
        error={error}
        onRetry={() => {
          setLoading(true);
          setError(null);
          getRecord(entity.entityCode ?? '', recordId, { appCode, tenantCode })
            .then(setRow)
            .catch((err: unknown) => {
              message.error(err instanceof Error ? err.message : '加载失败');
            })
            .finally(() => setLoading(false));
        }}
        isEmpty={!loading && !error && !row}
        empty={<Text type="secondary">记录不存在，可能已被删除。</Text>}
      >
        <Descriptions
          bordered
          size="small"
          column={1}
          title={
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {entity.entityName || entity.entityCode}
              <Tooltip title="编辑这条记录">
                <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(recordId)}>
                  编辑
                </Button>
              </Tooltip>
              <Button size="small" onClick={onBack}>
                返回表格
              </Button>
            </span>
          }
        >
          {resolvedFields.map((resolved) => {
            const { value, displayValue } = row ? readFieldValue(row, resolved) : { value: null, displayValue: undefined };
            return (
              <Descriptions.Item key={resolved.ctx.field.fieldCode} label={resolved.ctx.field.fieldName || resolved.ctx.field.fieldCode}>
                {resolved.def.renderCell({
                  ctx: resolved.ctx,
                  row: row ?? {},
                  value,
                  displayValue,
                })}
              </Descriptions.Item>
            );
          })}
          {row?.create_time ? (
            <Descriptions.Item label="创建时间">{String(row.create_time)}</Descriptions.Item>
          ) : null}
          {row?.update_time ? (
            <Descriptions.Item label="更新时间">{String(row.update_time)}</Descriptions.Item>
          ) : null}
        </Descriptions>
      </StateBlock>
    </div>
  );
}
