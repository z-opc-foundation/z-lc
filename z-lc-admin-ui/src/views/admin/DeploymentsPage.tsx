import { useCallback, useMemo, useState } from 'react';
import { Button, Drawer, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { DeploymentDTO } from '@/api/types';
import { createDeployment, getDeployment, listDeployments } from '@/api/deployment';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { listEmptyText, useAppSelection, useResourceList } from './_scope';
import { deployTypeLabel, useDeploymentVocabulary } from './_deployment';

const { Text, Paragraph } = Typography;

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
};

/** 部署日志里挑一句能当标题的话：第一行是"这次动了什么"的汇总。 */
function firstLine(log: string | null | undefined): string {
  if (!log) return '服务器没有留下原因';
  const head = (log.split('\n')[0] ?? '').trim();
  return head || '服务器没有留下原因';
}

export function DeploymentsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const [createOpen, setCreateOpen] = useState(false);
  const [deployType, setDeployType] = useState('');
  const [version, setVersion] = useState('');
  const [detail, setDetail] = useState<DeploymentDTO | null>(null);

  const kinds = useDeploymentVocabulary();
  const executable = kinds.vocabulary.executable;
  const rejectedReasons = kinds.vocabulary.rejected;

  const selectOptions = useMemo(
    () => [
      ...executable.map((value) => ({ value, label: deployTypeLabel(value), disabled: false })),
      ...rejectedReasons.map((item) => ({
        value: item.type,
        label: `${deployTypeLabel(item.type)}（服务器不执行）`,
        disabled: true,
      })),
    ],
    [executable, rejectedReasons],
  );

  // 没选或选中的那个已经不可选了 ⇒ 用服务器清单里的第一种，而不是页面里钉死一个 HOT_LOAD。
  const chosen = executable.includes(deployType) ? deployType : executable[0] ?? '';

  const { rows, state, error, loading, reload } = useResourceList<DeploymentDTO>(
    () => listDeployments(appCode),
    appCode || null,
  );

  const create = useCallback(async () => {
    try {
      // 返回的就是执行后的那一行：这一句必须按结局分叉，不能不看状态就报成功（缺陷 #70）。
      const done = await createDeployment({
        appCode,
        deployType: chosen,
        version: version.trim() || undefined,
        tenantCode: DEFAULT_TENANT_CODE,
      });
      reload();
      if (done.status === 'SUCCESS') {
        message.success(`部署完成 · ${firstLine(done.deployLog)}`);
        setCreateOpen(false);
        setVersion('');
        return;
      }
      message.error(`部署没做成（${done.status}）：${firstLine(done.deployLog)}`);
      setCreateOpen(false);
      setDetail(done);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '创建失败');
    }
  }, [appCode, chosen, version, reload]);

  const columns: ColumnsType<DeploymentDTO> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '方式',
      dataIndex: 'deployType',
      width: 130,
      render: (value: string) => deployTypeLabel(value),
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
      // 这一格现在是"指得回一批真产物"的 id：写入口会拒掉不存在或属于别的应用的批次。
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
          disabled={Boolean(row.id)}
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
      description="一次部署就是把该应用当前的实体定义应用到运行时库（逐实体建表、只补缺的列），并把结局写进这条记录的日志。执行是当场做的，状态列与日志说的就是这一次的真结果。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
        kinds.reload();
      }}
      actions={
        <Button
          type="primary"
          icon={<PlusOutlined />}
          // 词表没读到时不许开草稿：那种草稿只能送出一个空 deployType，而写入口现在把它拒成 400。
          disabled={!appCode || kinds.status !== 'ready'}
          onClick={() => setCreateOpen(true)}
        >
          新建部署
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="部署记录" />
      {kinds.status === 'error' ? (
        <div style={{ marginBottom: 12 }} data-testid="deployment-vocabulary-error">
          <ListBanner state="error" error={kinds.error} onRetry={kinds.reload} label="部署方式词表" />
        </div>
      ) : null}
      <Table<DeploymentDTO>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.deployType}-${row.createTime}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '部署记录') }}
      />

      <Modal
        open={createOpen}
        title="新建部署"
        data-testid="deployment-create-modal"
        onOk={() => void create()}
        onCancel={() => setCreateOpen(false)}
        okText="开始部署"
        cancelText="取消"
      >
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Select
            style={{ width: '100%' }}
            data-testid="deployment-type-select"
            value={chosen || undefined}
            options={selectOptions}
            notFoundContent={
              kinds.status === 'loading'
                ? '部署方式正在读取'
                : kinds.status === 'error'
                  ? `部署方式没有读到：${kinds.error instanceof Error ? kinds.error.message : String(kinds.error)}`
                  : '服务器没有报告任何会执行的部署方式'
            }
            onChange={setDeployType}
          />
          <Input
            placeholder="版本号（留空则不记版本）"
            value={version}
            onChange={(event) => setVersion(event.target.value)}
          />
          {rejectedReasons.length ? (
            <div data-testid="deployment-rejected" style={{ color: 'rgba(0,0,0,0.45)', fontSize: 12 }}>
              这几种方式服务器执行不了，选了也只会留下一行做不成的账：
              {rejectedReasons.map((item) => (
                <div key={item.type} data-testid={`deployment-rejected-${item.type}`}>
                  <Text code>{item.type}</Text> —— {item.reason}
                </div>
              ))}
            </div>
          ) : null}
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
              <Text type="secondary">方式</Text> {deployTypeLabel(detail.deployType)}{' '}
              <Text type="secondary">版本</Text> {detail.version ?? '—'}{' '}
              <Text type="secondary">物化批次</Text> {detail.materializationId ?? '—'}
            </Paragraph>
            <pre className="zlc-ddl" data-testid="deployment-log">
              {detail.deployLog || '（这一行没有日志：它是在部署真的会执行之前登记的老记录）'}
            </pre>
          </>
        ) : null}
      </Drawer>
    </AdminScaffold>
  );
}
