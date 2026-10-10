import { useCallback, useState } from 'react';
import {
  Button,
  Card,
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
import type { PipelineConfigEntity, PipelineStage, TriggerEvent } from '@/lc/api/types';
import {
  PIPELINE_STAGE_TYPES,
  PIPELINE_SUPPORTED_TRIGGERS,
  PIPELINE_TRIGGER_LABELS,
  createPipelineConfig,
  deletePipelineConfig,
  isPipelineTriggerSupported,
  listPipelineConfigs,
  newPipelineDraft,
  parseStages,
  stageTypeLabel,
  stringifyStages,
  togglePipelineConfig,
  updatePipelineConfig,
} from '@/lc/api/pipeline';
import { DEFAULT_TENANT_CODE } from '@/lc/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

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
      width: 150,
      render: (value: TriggerEvent | string, row) => {
        const trigger = String(value);
        // 历史数据里可能存在 AFTER_* 这种没有挂接点的行: 说清"不会执行", 不要给个中文标签蒙过去
        return isPipelineTriggerSupported(trigger) ? (
          <Tag data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>{PIPELINE_TRIGGER_LABELS[trigger] ?? trigger}</Tag>
        ) : (
          <Tag color="red" data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>
            {trigger} 无挂接点
          </Tag>
        );
      },
    },
    {
      title: '阶段链',
      dataIndex: 'stages',
      render: (_v, row) => {
        const list = parseStages(row.stages);
        if (!list.length)
          return (
            <Text type="danger" data-testid={`pipeline-empty-chain-${row.id ?? 'draft'}`}>
              空链（保存会被拒）
            </Text>
          );
        return (
          <Space size={4} wrap>
            {list.map((stage, index) => {
              const meta = PIPELINE_STAGE_TYPES.find((item) => item.type === stage.type);
              return (
                <Tag
                  key={`${stage.type}-${index}`}
                  color={meta ? (meta.noOpOnWrite ? 'default' : 'blue') : 'red'}
                  data-testid={`pipeline-stage-${row.id ?? 'draft'}-${index}`}
                >
                  {index + 1}. {meta ? `${stageTypeLabel(stage.type)} ${stage.type}` : `${stage.type} 无执行器`}
                  {meta?.noOpOnWrite ? '（写路径暂不做事）' : ''}
                </Tag>
              );
            })}
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
      description="写在提交之前跑的处理器链：一次写入只跑「应用+实体+触发事件」匹配的那一条，按链里的顺序执行。引擎目前只在写前有挂接点（创建前/更新前），必填校验、类型转换、值域校验三道闸门必须留在链里；字典解析、引用检查今天在写路径上不改变数据。"
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
                // AFTER_* 不再提供: 引擎里没有任何写后回调落点, 选了就是存一行永远不跑的配置
                options={PIPELINE_SUPPORTED_TRIGGERS.map((value) => ({ value, label: PIPELINE_TRIGGER_LABELS[value] }))}
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
                  onClick={() => setStages((prev) => [
                    ...prev,
                    {
                      type: PIPELINE_STAGE_TYPES[prev.length % PIPELINE_STAGE_TYPES.length]?.type ?? '',
                      config: {},
                      order: prev.length,
                    },
                  ])}
                >
                  添加阶段
                </Button>
              }
            >
              {stages.length === 0 ? (
                <Text type="danger">还没有阶段 —— 空链保存会被后端拒绝，因为它绕过必填/类型/值域三道闸门</Text>
              ) : (
                stages.map((stage, index) => (
                  <Space key={index} style={{ display: 'flex', marginBottom: 8 }} align="start">
                    <Tag style={{ minWidth: 24, textAlign: 'center' }}>{index + 1}</Tag>
                    <Select
                      size="small"
                      style={{ width: 240 }}
                      value={stage.type || undefined}
                      placeholder="阶段类型"
                      options={PIPELINE_STAGE_TYPES.map((item) => ({
                        value: item.type,
                        label: `${item.label} ${item.type}${item.mandatory ? '（必填）' : ''}${item.noOpOnWrite ? '（写路径暂不做事）' : ''}`,
                      }))}
                      onChange={(type) => patchStage(index, { type })}
                    />
                    {/* 这里原来是一个可以打的"阶段参数"输入框, 而五个处理器没有一个读 config:
                        填进去的东西改变不了任何行为 (#42)。后端现在直接 400 拒收这种配置,
                        所以界面上不许再留一个"看着能填"的口子。老配置行里真带着参数时,
                        这一格说的是实话 (保存会被拒), 而不是把它藏起来。 */}
                    <Text
                      type={Object.keys(stage.config ?? {}).length > 0 ? 'danger' : 'secondary'}
                      data-testid={`pipeline-stage-config-${index}`}
                      style={{ display: 'inline-block', width: 260 }}
                    >
                      {Object.keys(stage.config ?? {}).length > 0
                        ? `参数 ${Object.keys(stage.config ?? {}).join(' / ')} 引擎不读取，保存会被拒`
                        : '这一档没有可配参数（引擎不读取阶段参数）'}
                    </Text>
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
