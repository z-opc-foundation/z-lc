import { useCallback, useState } from 'react';
import { Button, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { RelationCreateReq, RelationDTO } from '@/api/types';
import { createRelation, deleteRelation, listRelations, updateRelation } from '@/api/relation';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

const RELATION_TYPES = [
  { value: 'ONE_TO_MANY', label: '一对多' },
  { value: 'MANY_TO_ONE', label: '多对一' },
  { value: 'MANY_TO_MANY', label: '多对多' },
];

const LABEL: Record<string, string> = Object.fromEntries(RELATION_TYPES.map((item) => [item.value, item.label]));

export function RelationsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const { rows, state, error, loading, reload } = useResourceList<RelationDTO>(
    () => listRelations(appCode),
    appCode || null,
  );
  const [editing, setEditing] = useState<RelationDTO | null>(null);

  const save = useCallback(async () => {
    if (!editing) return;
    if (!editing.relationCode?.trim() || !editing.sourceEntityCode || !editing.targetEntityCode) {
      message.warning('关系编码与两端实体都不能为空');
      return;
    }
    // Build the request explicitly: the *Req types do not carry id/timestamps
    // and use `string | undefined` rather than the DTO's `string | null`.
    const base: RelationCreateReq = {
      relationCode: editing.relationCode,
      relationName: editing.relationName ?? undefined,
      sourceEntityCode: editing.sourceEntityCode,
      targetEntityCode: editing.targetEntityCode,
      relationType: editing.relationType,
      sourceFieldCode: editing.sourceFieldCode ?? undefined,
      throughTable: editing.throughTable ?? undefined,
      appCode,
      tenantCode: DEFAULT_TENANT_CODE,
    };
    try {
      if (editing.id) await updateRelation({ ...base, id: editing.id });
      else await createRelation(base);
      message.success('已保存');
      setEditing(null);
      reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [editing, appCode, reload]);

  const columns: ColumnsType<RelationDTO> = [
    { title: '编码', dataIndex: 'relationCode', width: 160 },
    { title: '名称', dataIndex: 'relationName', width: 160 },
    {
      title: '关联',
      key: 'path',
      render: (_v, row) => (
        <Space size={6}>
          <Tag>{row.sourceEntityCode}</Tag>
          <Text type="secondary">{LABEL[String(row.relationType)] ?? row.relationType}</Text>
          <Tag color="blue">{row.targetEntityCode}</Tag>
        </Space>
      ),
    },
    { title: '外键字段', dataIndex: 'sourceFieldCode', width: 140 },
    { title: '中间表', dataIndex: 'throughTable', width: 160 },
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
            title="删除这条关系定义？"
            onConfirm={async () => {
              if (!row.id) return;
              await deleteRelation(row.id);
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
      title="实体关系"
      description="声明实体之间的引用关系；多对多需要指定中间表。字段绑定 refEntity 后表格会自动带出 {字段}_name。"
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
              relationCode: '',
              relationName: '',
              sourceEntityCode: entityOptions[0]?.value ?? '',
              targetEntityCode: entityOptions[0]?.value ?? '',
              relationType: 'ONE_TO_MANY',
              sourceFieldCode: 'id',
              appCode,
              tenantCode: DEFAULT_TENANT_CODE,
            })
          }
        >
          新建关系
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="实体关系" />
      <Table<RelationDTO>
        size="small"
        rowKey={(row) => String(row.id ?? row.relationCode)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '关系定义') }}
      />

      <Modal
        open={Boolean(editing)}
        title={editing?.id ? '编辑关系' : '新建关系'}
        onOk={() => void save()}
        onCancel={() => setEditing(null)}
        okText="保存"
        cancelText="取消"
      >
        {editing ? (
          <Space direction="vertical" style={{ width: '100%' }} size={10}>
            <Space wrap size={10}>
              <Input
                addonBefore="编码"
                style={{ width: 250 }}
                disabled={Boolean(editing.id)}
                value={editing.relationCode ?? ''}
                onChange={(event) => setEditing({ ...editing, relationCode: event.target.value })}
              />
              <Input
                addonBefore="名称"
                style={{ width: 250 }}
                value={editing.relationName ?? ''}
                onChange={(event) => setEditing({ ...editing, relationName: event.target.value })}
              />
            </Space>
            <Space wrap size={10}>
              <Select
                style={{ minWidth: 200 }}
                placeholder="源实体"
                showSearch
                value={editing.sourceEntityCode || undefined}
                options={entityOptions}
                notFoundContent={entityNotFoundContent(entitiesSource)}
                onChange={(sourceEntityCode) => setEditing({ ...editing, sourceEntityCode })}
              />
              <Select
                style={{ minWidth: 140 }}
                value={editing.relationType}
                options={RELATION_TYPES}
                onChange={(relationType) => setEditing({ ...editing, relationType })}
              />
              <Select
                style={{ minWidth: 200 }}
                placeholder="目标实体"
                showSearch
                value={editing.targetEntityCode || undefined}
                options={entityOptions}
                notFoundContent={entityNotFoundContent(entitiesSource)}
                onChange={(targetEntityCode) => setEditing({ ...editing, targetEntityCode })}
              />
            </Space>
            <Space wrap size={10}>
              <Input
                addonBefore="外键字段"
                style={{ width: 250 }}
                value={editing.sourceFieldCode ?? ''}
                onChange={(event) => setEditing({ ...editing, sourceFieldCode: event.target.value })}
              />
              {editing.relationType === 'MANY_TO_MANY' ? (
                <Input
                  addonBefore="中间表"
                  style={{ width: 280 }}
                  value={editing.throughTable ?? ''}
                  onChange={(event) => setEditing({ ...editing, throughTable: event.target.value })}
                />
              ) : null}
            </Space>
          </Space>
        ) : null}
      </Modal>
    </AdminScaffold>
  );
}
