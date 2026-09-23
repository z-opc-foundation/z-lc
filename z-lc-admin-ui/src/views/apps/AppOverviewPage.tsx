import { Link, useParams } from 'react-router-dom';
import { Button, Card, Space, Statistic, Table, Tag, Tooltip, Typography } from 'antd';
import { DashboardOutlined, PlusOutlined, ReloadOutlined, TableOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useWorkspace } from '@/hooks/useWorkspace';
import { unreadReason } from '@/api/meta';
import type { EntityDefDTO } from '@/api/types';
import { EmptyBlock, ErrorBlock, LoadingBlock } from '@/components/StateBlock';

const { Text, Paragraph } = Typography;

/** Landing page for `/:appCode` — summarises the model and routes into entities. */
export function AppOverviewPage() {
  const { appCode = '' } = useParams();
  const { meta, isLoading, isError, error, refetch } = useWorkspace(appCode);

  const entities = meta?.entities ?? [];
  const fieldTotal = entities.reduce((sum, entity) => sum + (entity.fields?.length ?? 0), 0);
  // react-query 的 isError 看不见单资源降级：`fetchWorkspaceMeta` 从不抛出，
  // 读失败的资源到这儿就是一支空数组，而 0 和"没读到"在统计位上是两句话。
  const read = meta?.read;
  /** A count that was never read must not render as a confident `0`. */
  const count = (key: 'entities' | 'dicts' | 'views', value: number) => (read?.[key] ? value : '—');

  const columns: ColumnsType<EntityDefDTO> = [
    {
      title: '实体',
      dataIndex: 'entityName',
      render: (_value, entity) => (
        <Space size={8}>
          <TableOutlined style={{ color: '#2f6feb' }} />
          <Link to={`/${appCode}/${entity.entityCode}/LIST`}>{entity.entityName || entity.entityCode}</Link>
          <Text code style={{ fontSize: 11 }}>
            {entity.entityCode}
          </Text>
        </Space>
      ),
    },
    {
      title: '物理表',
      dataIndex: 'tableName',
      width: 200,
      render: (value: string | null | undefined) => <Text code style={{ fontSize: 11 }}>{value ?? '—'}</Text>,
    },
    {
      title: '字段',
      width: 80,
      render: (_value, entity) => entity.fields?.length ?? 0,
    },
    {
      title: '版本',
      dataIndex: 'currentVersion',
      width: 80,
      render: (value: number | null | undefined) => (value == null ? '—' : `v${value}`),
    },
    {
      title: '说明',
      dataIndex: 'description',
      ellipsis: true,
      render: (value: string | null | undefined) => (
        <Tooltip title={value ?? undefined}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {value || '—'}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      width: 150,
      render: (_value, entity) => (
        <Space size={6}>
          <Link to={`/${appCode}/${entity.entityCode}/LIST`}>
            <Button size="small">表格</Button>
          </Link>
          <Link to={`/${appCode}/${entity.entityCode}/FORM`}>
            <Button size="small">新增</Button>
          </Link>
        </Space>
      ),
    },
  ];

  if (isLoading) return <LoadingBlock />;
  if (isError) return <ErrorBlock error={error} onRetry={refetch} />;

  return (
    <div style={{ padding: 12 }}>
      <Card
        variant="borderless"
        styles={{ body: { padding: 16 } }}
        title={
          <Space size={10}>
            <Text strong style={{ fontSize: 15 }}>
              {meta?.app?.appName || appCode}
            </Text>
            {meta?.app?.status ? <Tag bordered={false}>{meta.app.status}</Tag> : null}
          </Space>
        }
        extra={
          <Space size={6}>
            <Link to={`/${appCode}/dashboard`}>
              <Button size="small" icon={<DashboardOutlined />}>
                仪表盘
              </Button>
            </Link>
            <Link to={`/designer/${appCode}`}>
              <Button size="small" icon={<PlusOutlined />}>
                建模
              </Button>
            </Link>
          </Space>
        }
      >
        <Paragraph type="secondary" style={{ marginBottom: 12, fontSize: 12 }}>
          {meta?.app?.description || '暂无应用描述'}
        </Paragraph>
        <Space size={32}>
          <Statistic title="实体" value={count('entities', entities.length)} valueStyle={{ fontSize: 18 }} />
          <Statistic title="字段" value={count('entities', fieldTotal)} valueStyle={{ fontSize: 18 }} />
          <Statistic title="字典" value={count('dicts', meta?.dicts.length ?? 0)} valueStyle={{ fontSize: 18 }} />
          <Statistic title="视图" value={count('views', meta?.views.length ?? 0)} valueStyle={{ fontSize: 18 }} />
        </Space>
      </Card>

      <Card variant="borderless" style={{ marginTop: 12 }} title="实体列表" styles={{ body: { padding: 0 } }}>
        <Table
          size="small"
          rowKey="entityCode"
          columns={columns}
          dataSource={entities}
          pagination={false}
          locale={{
            emptyText: read?.entities ? (
              <EmptyBlock
                title="该应用还没有实体"
                description="在 Schema 设计器中创建实体，或从已有数据库表逆向导入。"
                action={
                  <Link to={`/designer/${appCode}`}>
                    <Button type="primary">打开 Schema 设计器</Button>
                  </Link>
                }
              />
            ) : (
              <EmptyBlock
                title="实体列表没有读到"
                description={`${unreadReason(meta, 'entities')}。这里的空不代表这个应用真的没有实体。`}
                action={
                  <Button type="primary" icon={<ReloadOutlined />} onClick={() => void refetch()}>
                    重试
                  </Button>
                }
              />
            ),
          }}
        />
      </Card>
    </div>
  );
}
