import { useCallback, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Descriptions, Drawer, Empty, Segmented, Space, Tag, Tooltip, Typography, message } from 'antd';
import { UndoOutlined } from '@ant-design/icons';
import { changeHistory, parseImage, undoLast } from '@/api/undo';
import { getActor } from '@/api/actor';
import { ErrorBlock } from '@/components/StateBlock';
import type { ChangeEntry } from '@/api/undo';
import type { ResolvedField } from '@yuku123/render/fields';
import { formatTime } from '@/views/admin/_scope';

const { Text } = Typography;

const OP_LABEL: Record<string, string> = { CREATE: '新建', UPDATE: '修改', DELETE: '删除' };
const OP_COLOR: Record<string, string> = { CREATE: 'green', UPDATE: 'blue', DELETE: 'red' };

/**
 * Server-backed change log for the current entity, plus one-click undo.
 * The revert itself happens server-side against the stored before-image,
 * so this panel is a viewer + trigger, never a client-side replay.
 */
export interface UndoHistoryDrawerProps {
  open: boolean;
  onClose: () => void;
  appCode: string;
  entityCode: string;
  tenantCode: string;
  resolvedFields: ResolvedField[];
  onUndone: () => void;
}

export function UndoHistoryDrawer({
  open,
  onClose,
  appCode,
  entityCode,
  tenantCode,
  resolvedFields,
  onUndone,
}: UndoHistoryDrawerProps) {
  const queryClient = useQueryClient();
  const [owner, setOwner] = useState<'mine' | 'all'>('mine');
  const labelOf = useCallback(
    (code: string) => {
      const resolved = resolvedFields.find((field) => field.ctx.field.fieldCode === code);
      return resolved?.ctx.field.fieldName || code;
    },
    [resolvedFields],
  );

  const query = useQuery({
    queryKey: ['undo-history', appCode, entityCode, tenantCode, owner],
    queryFn: () => changeHistory(appCode, entityCode, tenantCode, 50, owner),
    enabled: open,
  });

  const entries = query.data ?? [];

  const undo = async () => {
    try {
      const outcome = await undoLast(entityCode, appCode, tenantCode, owner === 'all');
      if (!outcome.applied) {
        message.info(outcome.message ?? '没有可撤销的变更');
        return;
      }
      message.success(`已撤销「${OP_LABEL[outcome.operation ?? ''] ?? outcome.operation}」(记录 ${outcome.recordId})`);
      await queryClient.invalidateQueries({ queryKey: ['undo-history', appCode, entityCode, tenantCode] });
      await queryClient.invalidateQueries({ queryKey: ['undo-history'] });
      onUndone();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '撤销失败');
    }
  };

  return (
    <Drawer
      open={open}
      onClose={onClose}
      width={560}
      title="变更历史"
      extra={
        <Tooltip title="撤销该实体最近一次未被撤销的改动">
          <Button type="primary" icon={<UndoOutlined />} loading={query.isFetching} onClick={() => void undo()}>
            {owner === 'all' ? '撤销最近一次改动（含他人）' : '撤销我的上一次改动'}
          </Button>
        </Tooltip>
      }
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="撤销在服务端按前像回放，不在浏览器重放"
        description="若一条记录之后又被改过，撤销会被拒绝而不是悄悄覆盖；重做新建时自增主键会变。"
      />
      {query.isLoading ? <Text type="secondary">加载中…</Text> : null}
      {query.isError ? (
        // 读失败时"这个实体还没有数据变更"是一句假话: 它把"我没查到"说成"没有过改动",
        // 用户会以为没人动过数据。错误要留在面板上, 并且给出重试。
        <ErrorBlock error={query.error} title="变更记录没有读到" onRetry={() => void query.refetch()} />
      ) : null}
      {!query.isLoading && !query.isError && entries.length === 0 ? (
        <Empty description="这个实体还没有数据变更" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : null}
      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
        <Segmented
          size="small"
          value={owner}
          options={[{ value: 'mine', label: '只看我的' }, { value: 'all', label: '全部成员' }]}
          onChange={(value) => setOwner(value as 'mine' | 'all')}
        />
        <Text type="secondary" style={{ fontSize: 12 }}>
          当前身份 {getActor()}
        </Text>
      </Space>
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        {entries.map((entry: ChangeEntry) => (
          <Descriptions
            key={entry.id}
            size="small"
            bordered
            column={1}
            title={
              <Space size={6}>
                <Tag color={OP_COLOR[entry.operation] ?? 'default'}>
                  {OP_LABEL[entry.operation] ?? entry.operation}
                </Tag>
                <Text style={{ fontSize: 12 }}>记录 #{entry.recordId}</Text>
                <Text type="secondary" style={{ fontSize: 11 }}>{entry.actor ?? 'anonymous'}</Text>
                {entry.undone ? <Tag>已撤销</Tag> : null}
              </Space>
            }
          >
            <Descriptions.Item label="时间">{formatTime(entry.createTime)}</Descriptions.Item>
            {entry.operation === 'UPDATE' ? (
              <Descriptions.Item label="改动">
                <DiffSummary entry={entry} labelOf={labelOf} />
              </Descriptions.Item>
            ) : (
              <Descriptions.Item label="快照">
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {summarise(entry, labelOf)}
                </Text>
              </Descriptions.Item>
            )}
          </Descriptions>
        ))}
      </Space>
    </Drawer>
  );
}

function DiffSummary({ entry, labelOf }: { entry: ChangeEntry; labelOf: (code: string) => string }) {
  const before = parseImage(entry.beforeImage);
  const after = parseImage(entry.afterImage);
  const changed = Object.keys(after).filter((key) => String(after[key]) !== String(before[key]));
  if (!changed.length) {
    return <Text type="secondary">无字段变化</Text>;
  }
  return (
    <Space direction="vertical" size={2}>
      {changed.slice(0, 8).map((key) => (
        <Text key={key} style={{ fontSize: 12 }}>
          {labelOf(key)}: <Text delete type="danger">{display(before[key])}</Text>
          {' → '}
          <Text strong>{display(after[key])}</Text>
        </Text>
      ))}
      {changed.length > 8 ? <Text type="secondary">…另 {changed.length - 8} 个字段</Text> : null}
    </Space>
  );
}

function summarise(entry: ChangeEntry, labelOf: (code: string) => string): string {
  const image = parseImage(entry.beforeImage ?? entry.afterImage);
  const keys = Object.keys(image).slice(0, 4);
  if (!keys.length) {
    return entry.operation === 'CREATE' ? '新建记录' : '—';
  }
  return keys.map((key) => `${labelOf(key)}=${display(image[key])}`).join('，');
}

function display(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '空';
  }
  return String(value);
}
