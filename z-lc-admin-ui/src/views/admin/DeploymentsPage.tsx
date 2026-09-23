import { useCallback, useState } from 'react';
import { Button, Drawer, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { DeploymentDTO } from '@/api/types';
import { createDeployment, getDeployment, listDeployments } from '@/api/deployment';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { listEmptyText, useAppSelection, useResourceList } from './_scope';

const { Text, Paragraph } = Typography;

const DEPLOY_TYPES = [
  { value: 'HOT_LOAD', label: '热加载' },
  { value: 'DOCKER', label: 'Docker 镜像' },
  { value: 'GIT_PUSH', label: 'Git 推送' },
];

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
};

export function DeploymentsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const [createOpen, setCreateOpen] = useState(false);
  const [deployType, setDeployType] = useState('HOT_LOAD');
  const [version, setVersion] = useState('');
  const [detail, setDetail] = useState<DeploymentDTO | null>(null);

  const { rows, state, error, loading, reload } = useResourceList<DeploymentDTO>(
    () => listDeployments(appCode),
    appCode || null,
  );

  const create = useCallback(async () => {
    try {
      await createDeployment({
        appCode,
        deployType,
        version: version.trim() || undefined,
        tenantCode: DEFAULT_TENANT_CODE,
      });
      message.success('部署已创建');
      setCreateOpen(false);
      setVersion('');
      reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '创建失败');
    }
  }, [appCode, deployType, version, reload]);

  const columns: ColumnsType<DeploymentDTO> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '方式',
      dataIndex: 'deployType',
      width: 130,
      render: (value: string) => DEPLOY_TYPES.find((item) => item.value === value)?.label ?? value,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: string) => <Tag color={STATUS_COLOR[String(value)] ?? 'default'}>{value}</Tag>,
    },
    { title: '版本', dataIndex: 'version', width: 130, render: (value?: string | null) => value ?? '—' },
    {
      title: '物化批次',
      dataIndex: 'materializationId',
      width: 110,
      render: (value?: number | null) => value ?? '—',
    },
    { title: '创建时间', dataIndex: 'createTime', width: 170, render: (v: string | number | null | undefined) => formatTime(v) },
    {
      title: '',
      key: 'ops',
      width: 80,
      render: (_v, row) => (
        <Button
          size="small"
          type="link"
          disabled={!row.id}
          onClick={async () => {
            if (!row.id) return;
            try {
              setDetail(await getDeployment(row.id));
            } catch (err) {
              message.error(err instanceof Error ? err.message : '加载详情失败');
            }
          }}
        >
          日志
        </Button>
      ),
    },
  ];

  return (
    <AdminScaffold
      title="部署"
      description="把当前设计态推到运行环境：热加载只刷新元数据，Docker/Git 走外部流水线。日志逐条留档。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
      actions={
        <Button type="primary" icon={<PlusOutlined />} disabled={!appCode} onClick={() => setCreateOpen(true)}>
          新建部署
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="部署记录" />
      <Table<DeploymentDTO>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.deployType}-${row.createTime}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '部署记录') }}
      />

      <Modal open={createOpen} title="新建部署" onOk={() => void create()} onCancel={() => setCreateOpen(false)} okText="开始部署" cancelText="取消">
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Select style={{ width: '100%' }} value={deployType} options={DEPLOY_TYPES} onChange={setDeployType} />
          <Input placeholder="版本号（可留空自动生成）" value={version} onChange={(event) => setVersion(event.target.value)} />
        </Space>
      </Modal>

      <Drawer
        open={Boolean(detail)}
        width={620}
        title={detail ? `部署 #${detail.id} · ${detail.status}` : ''}
        onClose={() => setDetail(null)}
      >
        {detail ? (
          <>
            <Paragraph>
              <Text type="secondary">方式</Text> {detail.deployType} <Text type="secondary">版本</Text> {detail.version ?? '—'}
            </Paragraph>
            <pre className="zlc-ddl">{detail.deployLog || '（暂无日志）'}</pre>
          </>
        ) : null}
      </Drawer>
    </AdminScaffold>
  );
}
