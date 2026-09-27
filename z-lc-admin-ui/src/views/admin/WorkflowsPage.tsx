import { useCallback, useEffect, useState } from 'react';
import { Button, Drawer, Input, Modal, Popconfirm, Select, Space, Table, Tag, Tooltip, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { WorkflowBindingEntity, WorkflowFireEntity } from '@/api/types';
import {
  createWorkflowBinding,
  deleteWorkflowBinding,
  listWorkflowBindings,
  listWorkflowFires,
  updateWorkflowBinding,
} from '@/api/workflowBinding';
import type { FireWindow } from '@/api/workflowBinding';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { formatTime } from './_scope';
import { triggerLabel, useWorkflowVocabulary } from './_workflow';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

/** 发起记录一页读多少条。界面的分页器和请求里的这个数必须是同一个，否则"第几页"对不上。 */
const FIRE_PAGE_SIZE = 20;

export function WorkflowsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const triggers = useWorkflowVocabulary();
  const [editing, setEditing] = useState<WorkflowBindingEntity | null>(null);
  const [firesOf, setFiresOf] = useState<WorkflowBindingEntity | null>(null);

  const { rows, state, error, loading, reload } = useResourceList<WorkflowBindingEntity>(
    () => listWorkflowBindings(appCode),
    appCode || null,
  );

  const implemented = triggers.vocabulary.implemented;
  const rejectedReasons = new Map(triggers.vocabulary.rejected.map((item) => [item.event, item.reason]));
  const triggerSelectOptions = implemented.map((event) => ({ value: event, label: triggerLabel(event) }));

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
      // 这一句不能只留"保存失败"：写入口拒的是兑现不了的形态，原因点名了要改哪一格
      // （哪个事件没有挂接点、autoSubmit 关掉等于没这条绑定）。用户看不见原因就是白挨一次 400。
      message.error(err instanceof Error && err.message ? err.message : '保存失败');
    }
  }, [editing, appCode, reload]);

  const columns: ColumnsType<WorkflowBindingEntity> = [
    { title: '实体', dataIndex: 'entityCode', width: 160 },
    {
      title: '触发时机',
      dataIndex: 'triggerEvent',
      width: 170,
      render: (value: string, row) => {
        const event = (value ?? '').trim();
        if (implemented.includes(event)) {
          return <Tag data-testid={`workflow-trigger-${row.id ?? event}`}>{triggerLabel(event)}</Tag>;
        }
        // 词表读失败时不许把每一行都标成"引擎不兑现" —— 那是把量具的故障说成数据的问题。
        if (triggers.status !== 'ready') {
          return <Tag data-testid={`workflow-trigger-${row.id ?? event}`}>{triggerLabel(event)} 时机未校对</Tag>;
        }
        const reason = rejectedReasons.get(event);
        return (
          <Tooltip title={reason ?? '引擎没有这个事件的挂接点，这条绑定不会发起任何流程'}>
            <Tag color="red" data-testid={`workflow-trigger-${row.id ?? event}`}>
              {triggerLabel(event)} 引擎不兑现
            </Tag>
          </Tooltip>
        );
      },
    },
    {
      title: '流程定义 Key',
      dataIndex: 'processDefinitionKey',
      render: (value: string) => <Text code>{value}</Text>,
    },
    {
      title: '自动提单',
      dataIndex: 'autoSubmit',
      width: 120,
      render: (value: number | null | undefined, row) => {
        // 运行期按 auto_submit=1 筛绑定（WorkflowBindingService.listByEvent），所以这一格为 0
        // 的老数据是真的不会发起 —— 标出来，而不是显示一个"否"让人以为只是没开开关。
        if (Number(value ?? 1) === 1) return '是';
        return (
          <Tooltip title="关掉自动提单之后这条绑定没有任何运行时行为：记录创建时不发起流程。写入口现在也拒这种形态。">
            <Tag color="red" data-testid={`workflow-autosubmit-${row.id ?? 'x'}`}>
              不会发起
            </Tag>
          </Tooltip>
        );
      },
    },
    { title: '更新', dataIndex: 'updateTime', width: 150, render: (v: string | number | null | undefined) => formatTime(v) },
    {
      title: '',
      key: 'ops',
      width: 170,
      render: (_v, row) => (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => setFiresOf(row)} data-testid={`workflow-fires-${row.id ?? 'x'}`}>
            发起记录
          </Button>
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

  const canDraft = triggers.status === 'ready' && implemented.length > 0;

  return (
    <AdminScaffold
      title="流程绑定"
      description="把实体事件挂到工作流定义上（由 z-wf 承载流程引擎）：记录创建之后自动向审批中心发起一个流程实例。边界：只有新建单条记录会发起，批量导入与撤销/重做都不会。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
        triggers.reload();
      }}
      actions={
        <Tooltip title={canDraft ? undefined : '触发时机词表没有读到，先重试右上方的刷新'}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            disabled={!appCode || !canDraft}
            data-testid="workflow-new-binding"
            onClick={() =>
              setEditing({
                entityCode: entityOptions[0]?.value ?? '',
                appCode,
                triggerEvent: implemented[0] ?? '',
                processDefinitionKey: '',
                // 送 1 而不是留空也不是 0：这条绑定真要发起流程，而关掉它的绑定现在写不进去。
                autoSubmit: 1,
                tenantCode: DEFAULT_TENANT_CODE,
              })
            }
          >
            新建绑定
          </Button>
        </Tooltip>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="流程绑定" />
      {triggers.status === 'error' ? (
        <div style={{ marginBottom: 12 }}>
          <ListBanner
            state="error"
            error={triggers.error}
            onRetry={triggers.reload}
            label="触发时机词表"
          />
        </div>
      ) : null}
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
                data-testid="workflow-trigger-select"
                value={editing.triggerEvent || undefined}
                placeholder="触发时机"
                options={triggerSelectOptions}
                notFoundContent={
                  triggers.status === 'loading'
                    ? '触发时机正在读取'
                    : triggers.status === 'error'
                      ? `触发时机没有读到：${triggers.error instanceof Error ? triggers.error.message : String(triggers.error)}`
                      : '引擎没有报告任何可兑现的触发时机'
                }
                onChange={(triggerEvent) => setEditing({ ...editing, triggerEvent })}
              />
            </Space>
            <Input
              addonBefore="流程定义 Key"
              value={editing.processDefinitionKey ?? ''}
              placeholder="例如 leave_approval"
              onChange={(event) => setEditing({ ...editing, processDefinitionKey: event.target.value })}
            />
            {triggers.vocabulary.rejected.length ? (
              <div data-testid="workflow-rejected" style={{ color: 'rgba(0,0,0,0.45)', fontSize: 12 }}>
                引擎现在还不兑现这些时机，选了也发不出流程：
                {triggers.vocabulary.rejected.map((item) => (
                  <div key={item.event} data-testid={`workflow-rejected-${item.event}`}>
                    <Text code>{item.event}</Text> —— {item.reason}
                  </div>
                ))}
              </div>
            ) : null}
          </Space>
        ) : null}
      </Modal>

      <WorkflowFiresDrawer binding={firesOf} onClose={() => setFiresOf(null)} />
    </AdminScaffold>
  );
}

/**
 * 一条绑定的发起账：绑定存在 ≠ 流程发起过。这一格是 #61 那一族里"配置与运行之间那段路"的
 * 唯一用户可见证据 —— 之前它只活在日志和库里。
 * <p/>
 * 缺陷 #65 之后这一页是<b>服务器分页</b>：接口回 `{records, total, pageNum, pageSize}`，
 * 界面把"库里一共多少条"和"这一页读出来几条"分开说。早先那版一次拉 200 条、客户端切 20 行，
 * 账超过 200 条时会把"第 201 条之后没读出来"演成"这条记录没发起过流程"，
 * 而空态那句「这个实体还没有发起记录」正好把误读说圆了。
 */
function WorkflowFiresDrawer({ binding, onClose }: { binding: WorkflowBindingEntity | null; onClose: () => void }) {
  const [ledger, setLedger] = useState<FireWindow | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<'idle' | 'loading' | 'error' | 'done'>('idle');
  const [reason, setReason] = useState<string>('');

  useEffect(() => {
    if (!binding) {
      setStatus('idle');
      setLedger(null);
      setPage(1);
      return;
    }
    let stale = false;
    setStatus('loading');
    void (async () => {
      try {
        // 读不出形状时 listWorkflowFires 会抛（见 readFireWindow）：那要走下面那句"没有读到"，
        // 不能退化成"读到了 0 条"。
        const next = await listWorkflowFires(binding.appCode, binding.entityCode, undefined, page, FIRE_PAGE_SIZE);
        if (stale) return;
        setLedger(next);
        setStatus('done');
      } catch (err) {
        if (stale) return;
        setLedger(null);
        setStatus('error');
        setReason(err instanceof Error ? err.message : String(err));
      }
    })();
    return () => {
      stale = true;
    };
  }, [binding, page]);

  const columns: ColumnsType<WorkflowFireEntity> = [
    { title: '记录', dataIndex: 'recordId', width: 110 },
    {
      title: '结果',
      dataIndex: 'status',
      width: 100,
      render: (value: string) => <Tag color={value === 'STARTED' ? 'green' : 'red'} data-testid={`fire-status-${value}`}>{value}</Tag>,
    },
    { title: '流程实例', dataIndex: 'instanceId', width: 150, render: (v: string | null | undefined) => v || '—' },
    {
      title: '为什么',
      dataIndex: 'detail',
      render: (v: string | null | undefined) => (v ? <Text style={{ fontSize: 12 }}>{v}</Text> : '—'),
    },
    { title: '时间', dataIndex: 'createTime', width: 150, render: (v: string | number | null | undefined) => formatTime(v) },
  ];

  const rows = ledger?.records ?? [];
  // 差额单独算：界面上一句"另外 N 条没有读在这一页里"里的 N 只能来自服务器的 total，
  // 不许写成 rows.length（那正是 #65 那个把页数当总数的形状）。
  const hiddenRows = ledger ? ledger.total - rows.length : 0;

  return (
    <Drawer
      open={Boolean(binding)}
      width={720}
      title={binding ? `${binding.entityCode} 的发起记录` : '发起记录'}
      onClose={onClose}
    >
      {status === 'error' ? (
        <div data-testid="fire-load-error">发起记录没有读到：{reason}</div>
      ) : (
        <>
          {ledger ? (
            <div data-testid="fire-window-line">
              这个实体一共有 <Text strong>{ledger.total}</Text> 条发起记录，这里按 id 倒序读第{' '}
              <Text strong>{ledger.pageNum}</Text> 页（每页 {ledger.pageSize} 条）
            </div>
          ) : null}
          {hiddenRows > 0 ? (
            <div data-testid="fire-window-partial">
              这一页只读出 {rows.length} 条，另外 <b>{hiddenRows} 条没有读在这一页里</b> ——
              翻页看，别把"没读出来"当成"没发起过"
            </div>
          ) : null}
          <Table<WorkflowFireEntity>
            size="small"
            rowKey={(row) => String(row.id ?? `${row.recordId}-${row.status}`)}
            loading={status === 'loading'}
            columns={columns}
            dataSource={rows}
            pagination={{
              current: ledger?.pageNum ?? page,
              pageSize: ledger?.pageSize ?? FIRE_PAGE_SIZE,
              total: ledger?.total ?? 0,
              onChange: (next) => setPage(next),
              showSizeChanger: false,
            }}
            locale={{
              // 三句分开，不许互相顶：读失败走上面那一格（带原因）；读到了空页要说"这一页没有、
              // 但一共有 N 条"；只有 total 真的是 0 才配说"还没有发起记录"。
              emptyText:
                ledger && ledger.total > 0 ? (
                  <div data-testid="fire-empty-past-the-end">
                    第 {ledger.pageNum} 页没有结局行，但这个实体一共有 {ledger.total} 条 ——
                    「这一页是空的」不是「没有发起过」
                  </div>
                ) : status === 'done' ? (
                  <div data-testid="fire-empty-none">这个实体还没有发起记录 —— 新建一条记录，流程才会在这里出现</div>
                ) : (
                  <div data-testid="fire-reading">正在读这个实体的发起记录…</div>
                ),
            }}
          />
        </>
      )}
    </Drawer>
  );
}
