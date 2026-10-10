import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  App,
  Button,
  Card,
  Col,
  Dropdown,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import {
  AppstoreAddOutlined,
  EllipsisOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { archiveApp, createApp, deleteApp, listApps, publishApp, updateApp } from '@/lc/api/app';
import { listAdminEntities } from '@/lc/api/admin';
import type { AppDTO, AppCreateReq } from '@/lc/api/types';
import { isApiError } from '@/lc/api/client';
import { EmptyBlock, ErrorBlock, LoadingBlock } from '@/lc/components/StateBlock';

const { Text, Paragraph } = Typography;

interface AppFormValues {
  appCode: string;
  appName: string;
  description?: string;
  icon?: string;
}

const APP_CODE_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/;

export function AppListPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [editing, setEditing] = useState<AppDTO | null>(null);
  const [creating, setCreating] = useState(false);
  const [form] = Form.useForm<AppFormValues>();

  const appsQuery = useQuery({
    queryKey: ['apps'],
    queryFn: listApps,
  });

  // 稳定引用：`?? []` 每次渲染都是新数组，会让下面的 useMemo 每帧重算
  const apps = useMemo(() => appsQuery.data ?? [], [appsQuery.data]);

  const filtered = useMemo(() => {
    const needle = keyword.trim().toLowerCase();
    if (!needle) return apps;
    return apps.filter(
      (app) =>
        app.appCode.toLowerCase().includes(needle) ||
        (app.appName ?? '').toLowerCase().includes(needle) ||
        (app.description ?? '').toLowerCase().includes(needle),
    );
  }, [apps, keyword]);

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['apps'] });
  };

  const mutationError = (action: string) => (err: unknown) => {
    const text = isApiError(err) ? `${err.message} (code ${err.code})` : String(err);
    message.error(`${action}失败: ${text}`);
  };

  const saveMutation = useMutation({
    mutationFn: async (values: AppFormValues) => {
      if (editing?.id != null) {
        return updateApp({ id: editing.id, appName: values.appName, description: values.description, icon: values.icon });
      }
      const payload: AppCreateReq = {
        appCode: values.appCode,
        appName: values.appName,
        description: values.description,
        icon: values.icon,
      };
      return createApp(payload);
    },
    onSuccess: () => {
      message.success(editing ? '应用已更新' : '应用已创建');
      closeEditor();
      refresh();
    },
    onError: mutationError('保存应用'),
  });

  const lifecycleMutation = useMutation({
    mutationFn: async ({ action, appCode }: { action: 'publish' | 'archive' | 'delete'; appCode: string }) => {
      if (action === 'publish') return publishApp(appCode);
      if (action === 'archive') return archiveApp(appCode);
      return deleteApp(appCode);
    },
    onSuccess: (_data, variables) => {
      message.success(
        variables.action === 'publish' ? '已发布' : variables.action === 'archive' ? '已归档' : '已删除',
      );
      refresh();
    },
    onError: mutationError('应用操作'),
  });

  function openCreate() {
    setEditing(null);
    setCreating(true);
    form.resetFields();
  }

  function openEdit(app: AppDTO) {
    setEditing(app);
    setCreating(true);
    form.setFieldsValue({
      appCode: app.appCode,
      appName: app.appName,
      description: app.description ?? undefined,
      icon: app.icon ?? undefined,
    });
  }

  function closeEditor() {
    setCreating(false);
    setEditing(null);
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
        <Text strong style={{ fontSize: 15 }}>
          应用
        </Text>
        <Text type="secondary" style={{ fontSize: 12 }}>
          共 {apps.length} 个
        </Text>
        <div style={{ flex: 1 }} />
        <Input
          allowClear
          size="small"
          style={{ width: 220 }}
          prefix={<SearchOutlined />}
          placeholder="搜索应用编码或名称"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <Button size="small" icon={<ReloadOutlined />} onClick={refresh}>
          刷新
        </Button>
        <Button size="small" type="primary" icon={<AppstoreAddOutlined />} onClick={openCreate}>
          新建应用
        </Button>
      </div>

      {appsQuery.isLoading ? <LoadingBlock /> : null}
      {appsQuery.isError ? <ErrorBlock error={appsQuery.error} onRetry={refresh} /> : null}
      {!appsQuery.isLoading && !appsQuery.isError && filtered.length === 0 ? (
        apps.length === 0 ? (
          <Card variant="borderless">
            <EmptyBlock
              title="还没有应用"
              description="应用是实体、字典与视图的容器。先创建一个应用，再进入 Schema 设计器建模。"
              action={
                <Button type="primary" onClick={openCreate}>
                  新建应用
                </Button>
              }
            />
          </Card>
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={`没有匹配 "${keyword}" 的应用`} />
        )
      ) : null}

      <Row gutter={[12, 12]}>
        {filtered.map((app) => (
          <Col key={app.appCode} xs={24} sm={12} lg={8} xxl={6}>
            <AppCard
              app={app}
              onOpen={() => navigate(`/lc/${app.appCode}`)}
              onEdit={() => openEdit(app)}
              onAction={(action) => lifecycleMutation.mutate({ action, appCode: app.appCode })}
              busy={lifecycleMutation.isPending}
            />
          </Col>
        ))}
      </Row>

      <Modal
        title={editing ? `编辑应用 · ${editing.appCode}` : '新建应用'}
        open={creating}
        onCancel={closeEditor}
        onOk={() => form.submit()}
        confirmLoading={saveMutation.isPending}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)} requiredMark={false}>
          <Form.Item
            name="appCode"
            label="应用编码"
            tooltip="作为 URL 片段与后端 appCode 使用，创建后不可修改"
            rules={[
              { required: true, message: '请填写应用编码' },
              { pattern: APP_CODE_PATTERN, message: '以字母开头，仅允许字母、数字与下划线' },
              { max: 64, message: '不超过 64 个字符' },
            ]}
          >
            <Input placeholder="例如 crm" disabled={Boolean(editing)} />
          </Form.Item>
          <Form.Item
            name="appName"
            label="应用名称"
            rules={[{ required: true, message: '请填写应用名称' }, { max: 128, message: '不超过 128 个字符' }]}
          >
            <Input placeholder="例如 客户关系管理" />
          </Form.Item>
          <Form.Item name="description" label="描述" rules={[{ max: 512, message: '不超过 512 个字符' }]}>
            <Input.TextArea rows={3} placeholder="这个应用解决什么问题" />
          </Form.Item>
          <Form.Item name="icon" label="图标标识" tooltip="仅存储字符串标识，由前端映射为展示图标">
            <Input placeholder="例如 appstore" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function AppCard({
  app,
  onOpen,
  onEdit,
  onAction,
  busy,
}: {
  app: AppDTO;
  onOpen: () => void;
  onEdit: () => void;
  onAction: (action: 'publish' | 'archive' | 'delete') => void;
  busy: boolean;
}) {
  // Counts are not on the list payload for every backend revision, so fetch the
  // entity list per card lazily; failures degrade to a dash instead of an error.
  const entitiesQuery = useQuery({
    queryKey: ['entity-count', app.appCode],
    queryFn: () => listAdminEntities(app.appCode),
    staleTime: 60_000,
    retry: 0,
  });

  const entityCount = entitiesQuery.data?.length ?? app.entityCount;
  const fieldCount =
    entitiesQuery.data?.reduce((sum, entity) => sum + (entity.fields?.length ?? 0), 0) ?? app.fieldCount;

  const status = (app.status ?? 'DRAFT').toUpperCase();

  return (
    <Card
      variant="borderless"
      styles={{ body: { padding: 14 } }}
      hoverable
      onClick={onOpen}
      title={
        <Space size={8}>
          <Text strong style={{ fontSize: 14 }}>
            {app.appName || app.appCode}
          </Text>
          <StatusTag status={status} />
        </Space>
      }
      extra={
        <span onClick={(event) => event.stopPropagation()}>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'open', label: '进入工作区' },
                { key: 'edit', label: '编辑信息' },
                { key: 'publish', label: '发布', disabled: status === 'PUBLISHED' },
                { key: 'archive', label: '归档', disabled: status === 'ARCHIVED' },
                { type: 'divider' },
                { key: 'delete', label: '删除', danger: true },
              ],
              onClick: ({ key, domEvent }) => {
                domEvent.stopPropagation();
                if (key === 'open') return onOpen();
                if (key === 'edit') return onEdit();
                if (key === 'delete') {
                  Modal.confirm({
                    title: `删除应用 ${app.appCode}?`,
                    content: '该操作不可撤销，实体定义与运行时数据不会被自动清理。',
                    okText: '确认删除',
                    okButtonProps: { danger: true },
                    cancelText: '取消',
                    onOk: () => onAction('delete'),
                  });
                  return;
                }
                onAction(key as 'publish' | 'archive');
              },
            }}
          >
            <Button size="small" type="text" icon={<EllipsisOutlined />} disabled={busy} />
          </Dropdown>
        </span>
      }
    >
      <Paragraph
        type="secondary"
        ellipsis={{ rows: 2, tooltip: app.description ?? undefined }}
        style={{ minHeight: 36, marginBottom: 10, fontSize: 12 }}
      >
        {app.description || '暂无描述'}
      </Paragraph>

      <div className="zlc-app-card__meta">
        <Tooltip title="实体数量">
          <span>实体 {entityCount ?? '—'}</span>
        </Tooltip>
        <Tooltip title="字段总数">
          <span>字段 {fieldCount ?? '—'}</span>
        </Tooltip>
        <Text code style={{ fontSize: 11 }}>
          {app.appCode}
        </Text>
      </div>
    </Card>
  );
}

function StatusTag({ status }: { status: string }) {
  const color = status === 'PUBLISHED' ? 'green' : status === 'ARCHIVED' ? 'default' : 'blue';
  const label = status === 'PUBLISHED' ? '已发布' : status === 'ARCHIVED' ? '已归档' : '草稿';
  return (
    <Tag color={color} bordered={false} style={{ marginInlineEnd: 0 }}>
      {label}
    </Tag>
  );
}
