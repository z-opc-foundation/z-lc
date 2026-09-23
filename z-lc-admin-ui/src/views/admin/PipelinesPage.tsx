import { useCallback, useState } from 'react';
import {
  Button,
  Card,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { PipelineConfigEntity, PipelineStage, TriggerEvent } from '@/api/types';
import {
  createPipelineConfig,
  deletePipelineConfig,
  listPipelineConfigs,
  newPipelineDraft,
  parseStages,
  stringifyStages,
  togglePipelineConfig,
  updatePipelineConfig,
} from '@/api/pipeline';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

const TRIGGER_LABELS: Record<string, string> = {
  BEFORE_CREATE: '创建前',
  BEFORE_UPDATE: '更新前',
  AFTER_CREATE: '创建后',
  AFTER_UPDATE: '更新后',
  AFTER_DELETE: '删除后',
};

const STAGE_TYPES = [
  'DICT_RESOLVE',
  'TYPE_CONVERT',
  'REQUIRED_CHECK',
  'REF_CHECK',
  'WEBHOOK',
  'SCRIPT',
];

export function PipelinesPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const [editing, setEditing] = useState<PipelineConfigEntity | null>(null);
  const [stages, setStages] = useState<PipelineStage[]>([]);

  const { rows, state, error, loading, setRows, reload } = useResourceList<PipelineConfigEntity>(
    () => listPipelineConfigs(appCode),
    appCode || null,
  );

  const openEditor = useCallback((record: PipelineConfigEntity) => {
    setEditing({ ...record });
    setStages(parseStages(record.stages));
  }, []);

  const save = useCallback(async () => {
    if (!editing?.entityCode?.trim()) {
      message.warning('请选择实体');
      return;
    }
    const payload: PipelineConfigEntity = {
      ...editing,
      appCode,
      tenantCode: DEFAULT_TENANT_CODE,
      stages: stringifyStages(stages),
    };
    try {
      if (editing.id) await updatePipelineConfig(payload);
      else await createPipelineConfig(payload);
      message.success('已保存');
      setEditing(null);
      reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [editing, stages, appCode, reload]);

  const columns: ColumnsType<PipelineConfigEntity> = [
    { title: '实体', dataIndex: 'entityCode', width: 180 },
    {
      title: '触发时机',
      dataIndex: 'triggerEvent',
      width: 130,
      render: (value: TriggerEvent | string) => <Tag>{TRIGGER_LABELS[String(value)] ?? value}</Tag>,
    },
    {
      title: '阶段链',
      dataIndex: 'stages',
      render: (_v, row) => {
        const list = parseStages(row.stages);
        if (!list.length) return <Text type="secondary">空</Text>;
        return (
          <Space size={4} wrap>
            {list.map((stage, index) => (
              <Tag key={`${stage.type}-${index}`} color="blue">
                {index + 1}. {stage.type || '未命名'}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 80,
      render: (_v, row) => (
        <Switch
          size="small"
          checked={Number(row.enabled ?? 0) === 1}
          onChange={async (checked) => {
            if (!row.id) return;
            try {
              await togglePipelineConfig(row.id, checked);
              setRows((prev) => prev.map((item) => (item.id === row.id ? { ...item, enabled: checked ? 1 : 0 } : item)));
            } catch (err) {
              message.error(err instanceof Error ? err.message : '切换失败');
            }
          }}
        />
      ),
    },
    { title: '更新', dataIndex: 'updateTime', width: 150, render: (v: string | number | null | undefined) => formatTime(v) },
    {
      title: '',
      key: 'ops',
      width: 120,
      render: (_v, row) => (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => openEditor(row)}>
            编辑
          </Button>
          <Popconfirm
            title="删除这条流水线配置？"
            onConfirm={async () => {
              if (!row.id) return;
              await deletePipelineConfig(row.id);
              message.success('已删除');
              reload();
            }}
          >
            <Button size="small" type="link" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const patchStage = (index: number, patch: Partial<PipelineStage>) =>
    setStages((prev) => prev.map((stage, i) => (i === index ? { ...stage, ...patch } : stage)));

  return (
    <AdminScaffold
      title="处理流水线"
      description="挂在实体生命周期上的阶段链（校验、字典解析、外部通知等），按 order 顺序执行。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
      actions={
        <Button type="primary" icon={<PlusOutlined />} disabled={!appCode} onClick={() => openEditor(newPipelineDraft(appCode))}>
          新建流水线
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="流水线" />
      <Table<PipelineConfigEntity>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.entityCode}-${row.triggerEvent}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '流水线') }}
      />

      <Modal
        open={Boolean(editing)}
        title={editing?.id ? '编辑流水线' : '新建流水线'}
        width={720}
        onOk={() => void save()}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
      >
        {editing ? (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Space wrap size={10}>
              <Select
                style={{ minWidth: 200 }}
                placeholder="选择实体"
                showSearch
                value={editing.entityCode || undefined}
                options={entityOptions}
                notFoundContent={entityNotFoundContent(entitiesSource)}
                onChange={(entityCode) => setEditing({ ...editing, entityCode })}
              />
              <Select
                style={{ minWidth: 160 }}
                value={editing.triggerEvent}
                options={Object.entries(TRIGGER_LABELS).map(([value, label]) => ({ value, label }))}
                onChange={(triggerEvent) => setEditing({ ...editing, triggerEvent })}
              />
              <Space size={6}>
                <Switch
                  size="small"
                  checked={Number(editing.enabled ?? 0) === 1}
                  onChange={(checked) => setEditing({ ...editing, enabled: checked ? 1 : 0 })}
                />
                <Text>启用</Text>
              </Space>
            </Space>

            <Card
              size="small"
              title="阶段链"
              extra={
                <Button
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={() => setStages((prev) => [...prev, { type: STAGE_TYPES[0] ?? '', config: {}, order: prev.length }])}
                >
                  添加阶段
                </Button>
              }
            >
              {stages.length === 0 ? (
                <Text type="secondary">还没有阶段</Text>
              ) : (
                stages.map((stage, index) => (
                  <Space key={index} style={{ display: 'flex', marginBottom: 8 }} align="start">
                    <Tag style={{ minWidth: 24, textAlign: 'center' }}>{index + 1}</Tag>
                    <Select
                      size="small"
                      style={{ width: 180 }}
                      value={stage.type || undefined}
                      placeholder="阶段类型"
                      options={STAGE_TYPES.map((type) => ({ value: type, label: type }))}
                      onChange={(type) => patchStage(index, { type })}
                    />
                    <Input.TextArea
                      size="small"
                      style={{ width: 260 }}
                      rows={1}
                      value={JSON.stringify(stage.config ?? {})}
                      placeholder='{"url":"https://..."}'
                      onChange={(event) => {
                        try {
                          patchStage(index, { config: JSON.parse(event.target.value || '{}') as Record<string, unknown> });
                        } catch {
                          /* keep editing an invalid JSON without clobbering the draft */
                        }
                      }}
                    />
                    <Space size={0}>
                      <Button size="small" type="text" icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => setStages((prev) => {
                        const next = [...prev];
                        const [moved] = next.splice(index - 1, 1);
                        if (moved) next.splice(index, 0, moved);
                        return next;
                      })} />
                      <Button size="small" type="text" icon={<ArrowDownOutlined />} disabled={index === stages.length - 1} onClick={() => setStages((prev) => {
                        const next = [...prev];
                        const [moved] = next.splice(index + 1, 1);
                        if (moved) next.splice(index, 0, moved);
                        return next;
                      })} />
                      <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={() => setStages((prev) => prev.filter((_, i) => i !== index))} />
                    </Space>
                  </Space>
                ))
              )}
            </Card>
          </Space>
        ) : null}
      </Modal>
    </AdminScaffold>
  );
}
