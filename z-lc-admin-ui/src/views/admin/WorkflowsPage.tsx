import { useCallback, useState } from 'react';
import { Button, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { WorkflowBindingEntity } from '@/api/types';
import {
  createWorkflowBinding,
  deleteWorkflowBinding,
  listWorkflowBindings,
  updateWorkflowBinding,
} from '@/api/workflowBinding';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

const TRIGGERS = [
  { value: 'AFTER_CREATE', label: '创建后' },
  { value: 'AFTER_UPDATE', label: '更新后' },
  { value: 'AFTER_DELETE', label: '删除后' },
];

export function WorkflowsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const [editing, setEditing] = useState<WorkflowBindingEntity | null>(null);

  const { rows, state, error, loading, reload } = useResourceList<WorkflowBindingEntity>(
    () => listWorkflowBindings(appCode),
    appCode || null,
  );

  const save = useCallback(async () => {
    if (!editing) return;
    if (!editing.entityCode || !editing.processDefinitionKey?.trim()) {
      message.warning('实体与流程定义 Key 都不能为空');
      return;
    }
    const payload = { ...editing, appCode, tenantCode: DEFAULT_TENANT_CODE };
    try {
      if (editing.id) await updateWorkflowBinding(payload);
      else await createWorkflowBinding(payload);
      message.success('已保存');
      setEditing(null);
      reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [editing, appCode, reload]);

  const columns: ColumnsType<WorkflowBindingEntity> = [
    { title: '实体', dataIndex: 'entityCode', width: 180 },
    {
      title: '触发时机',
      dataIndex: 'triggerEvent',
      width: 120,
      render: (value: string) => <Tag>{TRIGGERS.find((item) => item.value === value)?.label ?? value}</Tag>,
    },
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      render: (value: string) => <Text code>{value}</Text>,
    },
    {
      title: '自动提单',
      dataIndex: 'autoSubmit',
      width: 90,
      render: (value: number | null | undefined) => (Number(value ?? 0) === 1 ? '是' : '否'),
    },
    { title: '更新', dataIndex: 'updateTime', width: 150, render: (v: string | number | null | undefined) => formatTime(v) },
    {
      title: '',
      key: 'ops',
      width: 110,
      render: (_v, row) => (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => setEditing({ ...row })}>
            编辑
          </Button>
          <Popconfirm
            title="解绑这条流程？"
            onConfirm={async () => {
              if (!row.id) return;
              await deleteWorkflowBinding(row.id);
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

  return (
    <AdminScaffold
      title="流程绑定"
      description="把实体事件挂到工作流定义上（由 z-wf 承载流程引擎），记录变更时自动提单或触发审批。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
      actions={
        <Button
          type="primary"
          icon={<PlusOutlined />}
          disabled={!appCode}
          onClick={() =>
            setEditing({
              entityCode: entityOptions[0]?.value ?? '',
              appCode,
              triggerEvent: 'AFTER_CREATE',
              processDefinitionKey: '',
              autoSubmit: 0,
              tenantCode: DEFAULT_TENANT_CODE,
            })
          }
        >
          新建绑定
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="流程绑定" />
      <Table<WorkflowBindingEntity>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.entityCode}-${row.triggerEvent}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '流程绑定') }}
      />

      <Modal
        open={Boolean(editing)}
        title={editing?.id ? '编辑绑定' : '新建绑定'}
        onOk={() => void save()}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
      >
        {editing ? (
          <Space direction="vertical" style={{ width: '100%' }} size={10}>
            <Space wrap size={10}>
              <Select
                style={{ minWidth: 220 }}
                placeholder="实体"
                showSearch
                value={editing.entityCode || undefined}
                options={entityOptions}
                notFoundContent={entityNotFoundContent(entitiesSource)}
                onChange={(entityCode) => setEditing({ ...editing, entityCode })}
              />
              <Select
                style={{ minWidth: 150 }}
                value={editing.triggerEvent}
                options={TRIGGERS}
                onChange={(triggerEvent) => setEditing({ ...editing, triggerEvent })}
              />
            </Space>
            <Input
              addonBefore="流程定义 Key"
              value={editing.processDefinitionKey ?? ''}
              placeholder="例如 leave_approval"
              onChange={(event) => setEditing({ ...editing, processDefinitionKey: event.target.value })}
            />
            <Space size={6}>
              <Switch
                size="small"
                checked={Number(editing.autoSubmit ?? 0) === 1}
                onChange={(checked) => setEditing({ ...editing, autoSubmit: checked ? 1 : 0 })}
              />
              <Text>记录创建后自动提交流程</Text>
            </Space>
          </Space>
        ) : null}
      </Modal>
    </AdminScaffold>
  );
}
