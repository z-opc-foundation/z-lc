import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Input,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ImportOutlined, ReloadOutlined } from '@ant-design/icons';
import type { EntityDefDTO } from '@/lc/api/types';
import { listApps } from '@/lc/api/app';
import { batchImportDbTables, importDbTable, previewDbTable, scanDbTables } from '@/lc/api/admin';
import { DEFAULT_TENANT_CODE } from '@/lc/api/client';
import { StateBlock } from '@/lc/components/StateBlock';

const { Text, Title } = Typography;

/**
 * Reverse claiming: turn physical tables that already exist into low-code
 * entities, so an existing schema can be governed without a rewrite.
 */
export function DbImportPage() {
  const [searchParams] = useSearchParams();
  const [appCode, setAppCode] = useState(searchParams.get('appCode') ?? '');
  const [prefix, setPrefix] = useState('');
  const [schema, setSchema] = useState('');
  const [autoProvision, setAutoProvision] = useState(false);
  const [tables, setTables] = useState<EntityDefDTO[]>([]);
  const [picked, setPicked] = useState<string[]>([]);
  const [preview, setPreview] = useState<EntityDefDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [apps, setApps] = useState<{ value: string; label: string }[]>([]);

  const scan = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await scanDbTables(prefix, schema || undefined);
      setTables(list);
      setPicked([]);
    } catch (err) {
      setError(err);
      setTables([]);
    } finally {
      setLoading(false);
    }
  }, [prefix, schema]);

  useEffect(() => {
    void (async () => {
      try {
        const list = await listApps();
        setApps(list.map((app) => ({ value: app.appCode ?? '', label: app.appName || app.appCode })));
      } catch {
        /* app list is only used to choose an import target */
      }
    })();
  }, []);

  useEffect(() => {
    void scan();
  }, [scan]);

  const columns = useMemo<ColumnsType<EntityDefDTO>>(
    () => [
      { title: '表名', dataIndex: 'tableName', width: 240 },
      { title: '推断实体名', dataIndex: 'entityName', width: 200 },
      {
        title: '字段',
        dataIndex: 'fields',
        render: (_v, row) => (
          <Text type="secondary" style={{ fontSize: 12 }}>
            {(row.fields ?? []).map((field) => `${field.fieldCode}:${field.fieldType}`).slice(0, 12).join(', ')}
            {(row.fields?.length ?? 0) > 12 ? ` …共 ${row.fields?.length} 个` : ''}
          </Text>
        ),
      },
      {
        title: '',
        key: 'preview',
        width: 80,
        render: (_v, row) => (
          <Button
            size="small"
            type="link"
            onClick={async () => {
              if (!row.tableName) return;
              try {
                setPreview(await previewDbTable(row.tableName, schema || undefined));
              } catch (err) {
                message.error(err instanceof Error ? err.message : '预览失败');
              }
            }}
          >
            预览
          </Button>
        ),
      },
    ],
    [schema],
  );

  const doImport = useCallback(async () => {
    if (!appCode) {
      message.warning('请先选择目标应用');
      return;
    }
    const targets = picked.length
      ? tables.filter((table) => picked.includes(table.tableName ?? ''))
      : tables;
    if (!targets.length) {
      message.warning('没有可导入的表');
      return;
    }
    const hide = message.loading(`正在导入 ${targets.length} 张表…`, 0);
    const failures: string[] = [];
    let imported = 0;
    if (picked.length === 0) {
      // Nothing ticked => claim the whole prefix in one server-side pass.
      try {
        const created = await batchImportDbTables({
          appCode,
          prefix,
          tenantCode: DEFAULT_TENANT_CODE,
          schema: schema || undefined,
          autoProvision,
        });
        imported = created.length;
      } catch (err) {
        failures.push(err instanceof Error ? err.message : '批量导入失败');
      }
    } else {
      for (const table of targets) {
        try {
          await importDbTable({
            tableName: table.tableName ?? '',
            appCode,
            tenantCode: DEFAULT_TENANT_CODE,
            schema: schema || undefined,
            autoProvision,
          });
          imported += 1;
        } catch (err) {
          failures.push(`${table.tableName}: ${err instanceof Error ? err.message : '失败'}`);
        }
      }
    }
    hide();
    if (failures.length) {
      message.warning(`${imported} 张成功，${failures.length} 张失败`);
    } else {
      message.success(`已导入 ${imported} 张表`);
    }
    void scan();
  }, [appCode, picked, tables, prefix, schema, autoProvision, scan]);

  return (
    <div style={{ padding: 16, maxWidth: 1180 }}>
      <Title level={4}>从数据库导入</Title>
      <Text type="secondary">
        扫描当前库里的物理表，推断成实体定义后认领进应用 —— 存量表无需重建即可纳入低代码治理。
      </Text>

      <Card size="small" style={{ margin: '12px 0' }}>
        <Space wrap size={12}>
          <Select
            style={{ minWidth: 220 }}
            placeholder="目标应用"
            value={appCode || undefined}
            options={apps}
            onChange={setAppCode}
            showSearch
          />
          <Input
            addonBefore="表名前缀"
            style={{ width: 260 }}
            placeholder="例如 z_lc_ / 留空扫全部"
            value={prefix}
            onChange={(event) => setPrefix(event.target.value)}
            onPressEnter={() => void scan()}
          />
          <Input
            addonBefore="schema"
            style={{ width: 240 }}
            placeholder="默认当前库"
            value={schema}
            onChange={(event) => setSchema(event.target.value)}
          />
          <Space size={6}>
            <Switch checked={autoProvision} onChange={setAutoProvision} size="small" />
            <Text>导入后立即 provision</Text>
          </Space>
          <Button icon={<ReloadOutlined />} onClick={() => void scan()} loading={loading}>
            重新扫描
          </Button>
          <Button
            type="primary"
            icon={<ImportOutlined />}
            disabled={tables.length === 0}
            onClick={() => void doImport()}
          >
            导入{picked.length ? ` ${picked.length} 张` : '全部'}
          </Button>
        </Space>
      </Card>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="说明"
        description="扫描依赖 JDBC 元数据，MySQL 与 H2 均可用；列注释等扩展信息在部分数据库上不可得，导入后请在设计器里补全字段中文名。"
      />

      <StateBlock
        isLoading={loading && tables.length === 0}
        isError={Boolean(error)}
        error={error}
        onRetry={() => void scan()}
        isEmpty={!loading && tables.length === 0}
        empty={<Text type="secondary">没有匹配前缀的表。</Text>}
      >
        <Table<EntityDefDTO>
          size="small"
          rowKey={(row) => row.tableName ?? ''}
          columns={columns}
          dataSource={tables}
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: true }}
          rowSelection={{
            selectedRowKeys: picked,
            onChange: (keys) => keys.map((key) => String(key)),
            onSelect: (row, selected) =>
              setPicked((prev) =>
                selected
                  ? [...prev, row.tableName ?? '']
                  : prev.filter((code) => code !== row.tableName),
              ),
          }}
        />
      </StateBlock>

      {preview ? (
        <Card
          size="small"
          style={{ marginTop: 12 }}
          title={
            <Space>
              推断结果
              <Tag>{preview.tableName}</Tag>
              <Text type="secondary">{preview.fields?.length ?? 0} 字段</Text>
            </Space>
          }
          extra={<Button size="small" onClick={() => setPreview(null)}>关闭</Button>}
        >
          <Table
            size="small"
            rowKey={(row) => String(row.fieldCode)}
            dataSource={preview.fields ?? []}
            pagination={false}
            columns={[
              { title: '字段', dataIndex: 'fieldCode', width: 200 },
              { title: '类型', dataIndex: 'fieldType', width: 110 },
              { title: '长度', dataIndex: 'fieldLength', width: 80 },
              { title: '必填', dataIndex: 'required', width: 70, render: (v: unknown) => (v ? '是' : '否') },
              { title: '注释', dataIndex: 'description' },
            ]}
          />
        </Card>
      ) : null}
    </div>
  );
}
