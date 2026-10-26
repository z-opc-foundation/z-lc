import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Empty,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DatabaseOutlined, ReloadOutlined, TableOutlined } from '@ant-design/icons';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';
import {
  listDataSources,
  previewDbTableOn,
  scanDbTablesOn,
  type DataSourceCatalogItem,
} from '@/api/datasource';

/**
 * 数据源管理（V1 孵化，2026-10-26，lead 009/013 §六 D14 拍板）。
 *
 * 目录与连接全部来自 z-dms；本页只做"选源 → 浏览表结构 → 看字段"，
 * 不承载登记/凭据/建池（那些归 z-dms 管控面）。
 */
export function DataSourcesPage() {
  const [catalog, setCatalog] = useState<DataSourceCatalogItem[]>([]);
  const [available, setAvailable] = useState<boolean | null>(null);
  const [sourceId, setSourceId] = useState<string | undefined>(undefined);
  const [prefix, setPrefix] = useState('');
  const [tables, setTables] = useState<EntityDefDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [detail, setDetail] = useState<EntityDefDTO | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        const cat = await listDataSources();
        setAvailable(cat.available);
        setCatalog(cat.items);
        const first = cat.items[0];
        if (first) {
          setSourceId(first.id);
        }
      } catch {
        setAvailable(false);
      }
    })();
  }, []);

  const scan = useCallback(async () => {
    if (!sourceId) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const list = await scanDbTablesOn(sourceId, prefix);
      setTables(list);
    } catch (err) {
      setError(err);
      setTables([]);
    } finally {
      setLoading(false);
    }
  }, [sourceId, prefix]);

  useEffect(() => {
    if (sourceId) {
      void scan();
    }
  }, [sourceId, scan]);

  const openDetail = useCallback(
    async (tableName: string) => {
      if (!sourceId) {
        return;
      }
      setDetailLoading(true);
      try {
        const entity = await previewDbTableOn(sourceId, tableName);
        setDetail(entity);
      } catch (err) {
        void message.error('表结构预览失败');
        setDetail(null);
      } finally {
        setDetailLoading(false);
      }
    },
    [sourceId],
  );

  const columns = useMemo<ColumnsType<EntityDefDTO>>(
    () => [
      {
        title: '表名',
        dataIndex: 'tableName',
        width: 260,
        render: (v: string) => <Typography.Text code>{v}</Typography.Text>,
      },
      { title: '推断实体名', dataIndex: 'entityName', width: 200 },
      { title: '说明', dataIndex: 'description', ellipsis: true },
      {
        title: '字段数',
        width: 90,
        render: (_v, row) => (row.fields ? row.fields.length : 0),
      },
      {
        title: '操作',
        width: 110,
        render: (_v, row) => (
          <Button
            size="small"
            type="link"
            icon={<TableOutlined />}
            onClick={() => void openDetail(row.tableName ?? '')}
          >
            表结构
          </Button>
        ),
      },
    ],
    [openDetail],
  );

  const fieldColumns = useMemo<ColumnsType<FieldDefDTO>>(
    () => [
      { title: '字段', dataIndex: 'fieldCode', width: 160 },
      { title: '名称', dataIndex: 'fieldName', width: 160 },
      { title: '类型', dataIndex: 'fieldType', width: 100 },
      { title: '长度', dataIndex: 'fieldLength', width: 80 },
      { title: '小数位', dataIndex: 'scale', width: 80 },
      {
        title: '必填',
        dataIndex: 'required',
        width: 80,
        render: (v: boolean | undefined) =>
          v ? <Tag color="orange">必填</Tag> : <Tag>可空</Tag>,
      },
      { title: '备注', dataIndex: 'description', ellipsis: true },
    ],
    [],
  );

  return (
    <Card
      title={
        <Space>
          <DatabaseOutlined />
          数据源管理
        </Space>
      }
      extra={
        <Space>
          <Select
            style={{ minWidth: 220 }}
            placeholder="选择数据源"
            value={sourceId}
            onChange={setSourceId}
            options={catalog.map((item) => ({
              value: item.id,
              label: `${item.name}（${item.id} · ${item.type}）`,
            }))}
          />
          <Input
            style={{ width: 180 }}
            placeholder="表名前缀过滤"
            value={prefix}
            onChange={(e) => setPrefix(e.target.value)}
            onPressEnter={() => void scan()}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void scan()} loading={loading}>
            扫描
          </Button>
        </Space>
      }
    >
      {available === false && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="数据源目录未装配"
          description="z-dms 桥接不在位（未引入 z-dms starter 或服务未启动）。目录与连接管理归 z-dms；装配后本页自动可用。"
        />
      )}
      {error !== null && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message="扫描失败"
          description={String(error)}
        />
      )}
      <Table<EntityDefDTO>
        rowKey={(row) => row.tableName ?? row.entityCode ?? String(row.id ?? '')}
        size="small"
        loading={loading}
        columns={columns}
        dataSource={tables}
        pagination={{ pageSize: 15, showTotal: (n) => `共 ${n} 张表` }}
        locale={{
          emptyText: (
            <Empty
              description={
                sourceId ? '没有命中的表（试试清空前缀过滤）' : '目录里还没有数据源'
              }
            />
          ),
        }}
      />
      <Drawer
        title={
          detail ? (
            <Typography.Text code>{detail.tableName}</Typography.Text>
          ) : (
            '表结构'
          )
        }
        width={720}
        open={detail !== null}
        onClose={() => setDetail(null)}
        destroyOnClose
      >
        {detail && (
          <>
            <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
              <Typography.Text>
                推断实体：<Typography.Text strong>{detail.entityName}</Typography.Text>
                （{detail.entityCode}）
              </Typography.Text>
              {detail.description && (
                <Typography.Text type="secondary">{detail.description}</Typography.Text>
              )}
            </Space>
            <Table<FieldDefDTO>
              rowKey={(row) => row.fieldCode ?? String(row.sortOrder ?? '')}
              size="small"
              loading={detailLoading}
              columns={fieldColumns}
              dataSource={detail.fields ?? []}
              pagination={false}
            />
          </>
        )}
      </Drawer>
    </Card>
  );
}

export default DataSourcesPage;
