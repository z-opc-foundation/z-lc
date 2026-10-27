import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { CaretRightOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { listApps } from '@/api/app';
import { createVapi, deleteVapi, invokeVapi, listVapis, updateVapi } from '@/api/vapi';
import type { VapiDTO } from '@/api/vapi';

/**
 * 接口脚本 (V3 接口/脚本域, lead 013 §四): vApi 注册表管理 + 在线调试。
 * 注册 app+method+path+DSL 脚本 → 分发 POST /api/lc/vapi/invoke/{app}/{path}
 * → z-script ScriptEngine 执行 (GROOVY/EL 带沙箱)。页面部件经 events.onClick
 * 绑定这里注册的路径, 完成"页面 ↔ 接口"闭环。
 */

const DSL_TYPES = [
  { value: 'EL', label: 'EL (SpEL 表达式)' },
  { value: 'GROOVY', label: 'Groovy (沙箱)' },
  { value: 'LUA', label: 'Lua' },
  { value: 'SQL', label: 'SQL' },
  { value: 'MOCK', label: 'MOCK (模板)' },
];

const METHODS = ['GET', 'POST', 'PUT', 'DELETE'];

const DSL_SAMPLE: Record<string, string> = {
  EL: "'Hello, ' + #name + '!'",
  GROOVY: "return 'sum=' + ((a as int) + (b as int));",
  LUA: "return 'lua ok'",
  SQL: "select 1",
  MOCK: '{"msg":"@pick([\'a\',\'b\'])"}',
};

export function VapisPage() {
  const [apps, setApps] = useState<{ appCode: string; appName: string }[]>([]);
  const [appCode, setAppCode] = useState<string>('');
  const [vapis, setVapis] = useState<VapiDTO[]>([]);
  const [editing, setEditing] = useState<VapiDTO | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [debugTarget, setDebugTarget] = useState<VapiDTO | null>(null);

  useEffect(() => {
    listApps()
      .then((list) => {
        setApps(list.map((a) => ({ appCode: a.appCode, appName: a.appName })));
        const first = list[0];
        if (first) setAppCode((cur) => cur || first.appCode);
      })
      .catch(() => setApps([]));
  }, []);

  const reload = useCallback((code: string) => {
    if (!code) {
      setVapis([]);
      return;
    }
    listVapis(code)
      .then(setVapis)
      .catch(() => setVapis([]));
  }, []);

  useEffect(() => {
    reload(appCode);
  }, [appCode, reload]);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (dto: VapiDTO) => {
    setEditing(dto);
    setModalOpen(true);
  };

  const remove = async (dto: VapiDTO) => {
    await deleteVapi(dto.id as number);
    message.success('已删除');
    reload(appCode);
  };

  const toggleStatus = async (dto: VapiDTO) => {
    await updateVapi({ id: dto.id as number, status: dto.status === 1 ? 0 : 1 });
    reload(appCode);
  };

  const runDebug = async (dto: VapiDTO, params: Record<string, unknown>) => {
    const result = await invokeVapi(dto.appCode, dto.path, dto.method as 'GET', params);
    Modal.info({
      title: `执行结果: ${dto.method} /${dto.path} (${dto.dslType})`,
      width: 640,
      content: <pre style={{ maxHeight: 400, overflow: 'auto', fontSize: 12 }}>{JSON.stringify(result, null, 2)}</pre>,
    });
  };

  const columns = [
    { title: '标识', dataIndex: 'vapiCode', width: 110, render: (v: string | null) => v ?? '-' },
    {
      title: '定义', dataIndex: 'method', width: 260,
      render: (_: unknown, r: VapiDTO) => (
        <span>
          <Tag color="blue">{r.method}</Tag>
          <code>/{r.path}</code>
        </span>
      ),
    },
    { title: 'DSL', dataIndex: 'dslType', width: 90, render: (v: string) => <Tag>{v}</Tag> },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (v: number) => (v === 1 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作', width: 230,
      render: (_: unknown, r: VapiDTO) => (
        <Space size={4}>
          <Button size="small" icon={<CaretRightOutlined />} disabled={r.status !== 1}
            onClick={() => setDebugTarget(r)}>调试</Button>
          <Button size="small" onClick={() => openEdit(r)}>编辑</Button>
          <Button size="small" onClick={() => toggleStatus(r)}>{r.status === 1 ? '停用' : '启用'}</Button>
          <Popconfirm title="确认删除该接口?" onConfirm={() => remove(r)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <span>应用：</span>
          <Select style={{ width: 220 }} value={appCode || undefined} onChange={(v) => setAppCode(v)}
            options={apps.map((a) => ({ value: a.appCode, label: `${a.appName} (${a.appCode})` }))}
            placeholder="选择应用" />
          <Button type="primary" icon={<PlusOutlined />} disabled={!appCode} onClick={openCreate}>注册接口</Button>
        </Space>
      </Card>

      <Card title={`vApi 注册表 (${appCode || '—'})`} styles={{ body: { padding: 0 } }}>
        {vapis.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无注册接口" style={{ padding: 32 }} />
        ) : (
          <Table rowKey="id" size="small" pagination={false} dataSource={vapis} columns={columns} />
        )}
      </Card>

      <VapiEditModal
        open={modalOpen}
        appCode={appCode}
        editing={editing}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false);
          reload(appCode);
        }}
      />

      <VapiDebugModal
        target={debugTarget}
        onClose={() => setDebugTarget(null)}
        onRun={runDebug}
      />
    </div>
  );
}

function VapiEditModal({ open, appCode, editing, onClose, onSaved }: {
  open: boolean;
  appCode: string;
  editing: VapiDTO | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [formInst] = Form.useForm();
  const [dsl, setDsl] = useState('EL');

  useEffect(() => {
    if (open) {
      if (editing) {
        setDsl(editing.dslType);
        formInst.setFieldsValue({
          vapiCode: editing.vapiCode,
          method: editing.method,
          path: editing.path,
          dslType: editing.dslType,
          sourceCode: editing.sourceCode,
          description: editing.description,
        });
      } else {
        setDsl('EL');
        formInst.setFieldsValue({ method: 'POST', path: '', dslType: 'EL', sourceCode: DSL_SAMPLE.EL });
      }
    }
  }, [open, editing, formInst]);

  const save = async () => {
    const values = await formInst.validateFields();
    if (editing) {
      await updateVapi({ id: editing.id as number, ...values });
    } else {
      await createVapi({ appCode, ...values });
    }
    message.success(editing ? '已更新' : '已注册');
    onSaved();
  };

  return (
    <Modal title={editing ? `编辑接口: ${editing.vapiCode ?? editing.path}` : '注册虚拟接口'}
      open={open} onOk={save} onCancel={onClose} width={720} destroyOnClose>
      <Alert type="info" showIcon style={{ marginBottom: 12 }}
        message="脚本参数经 params 透传 (query+body 合并); EL 用 #name 取值, Groovy 用 a/b 直接引用" />
      <Form form={formInst} layout="vertical">
        <Space size={12} wrap>
          <Form.Item name="vapiCode" label="接口标识"><Input placeholder="如 hello-el" style={{ width: 160 }} /></Form.Item>
          <Form.Item name="method" label="HTTP 方法" rules={[{ required: true }]}>
            <Select style={{ width: 100 }} options={METHODS.map((m) => ({ value: m, label: m }))} />
          </Form.Item>
          <Form.Item name="path" label="路径 (应用内相对)" rules={[{ required: true }]}>
            <Input placeholder="如 order/greet" style={{ width: 220 }} />
          </Form.Item>
          <Form.Item name="dslType" label="DSL" rules={[{ required: true }]}>
            <Select style={{ width: 160 }} options={DSL_TYPES}
              onChange={(v: string) => {
                setDsl(v);
                if (!editing) formInst.setFieldValue('sourceCode', DSL_SAMPLE[v] ?? '');
              }} />
          </Form.Item>
        </Space>
        <Form.Item name="sourceCode" label="脚本源码" rules={[{ required: true }]}>
          <Input.TextArea rows={5} style={{ fontFamily: 'monospace' }} placeholder={DSL_SAMPLE[dsl]} />
        </Form.Item>
        <Form.Item name="description" label="描述"><Input /></Form.Item>
      </Form>
    </Modal>
  );
}

function VapiDebugModal({ target, onClose, onRun }: {
  target: VapiDTO | null;
  onClose: () => void;
  onRun: (dto: VapiDTO, params: Record<string, unknown>) => Promise<void>;
}) {
  const [formInst] = Form.useForm();
  const [running, setRunning] = useState(false);

  const run = async () => {
    if (!target) return;
    let params: Record<string, unknown> = {};
    const raw = (formInst.getFieldValue('params') as string | undefined)?.trim();
    if (raw) {
      try {
        params = JSON.parse(raw) as Record<string, unknown>;
      } catch {
        message.error('参数不是合法 JSON');
        return;
      }
    }
    setRunning(true);
    try {
      await onRun(target, params);
    } finally {
      setRunning(false);
    }
  };

  return (
    <Modal title={`调试: ${target?.method ?? ''} /${target?.path ?? ''}`} open={target != null}
      onCancel={onClose} footer={[
        <Button key="close" onClick={onClose}>关闭</Button>,
        <Button key="run" type="primary" loading={running} icon={<CaretRightOutlined />} onClick={run}>执行</Button>,
      ]}>
      {target && (
        <>
          <Alert type="info" showIcon style={{ marginBottom: 12 }}
            message={`DSL: ${target.dslType} · 分发 POST /api/lc/vapi/invoke/${target.appCode}/${target.path}`} />
          <Form form={formInst} layout="vertical">
            <Form.Item name="params" label="参数 (JSON 对象, GET 也可走 body 参数合并)">
              <Input.TextArea rows={4} style={{ fontFamily: 'monospace' }} placeholder='{"name":"CEO"}' />
            </Form.Item>
          </Form>
        </>
      )}
    </Modal>
  );
}
