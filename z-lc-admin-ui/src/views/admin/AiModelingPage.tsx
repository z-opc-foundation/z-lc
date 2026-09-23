import { useCallback, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { BulbOutlined, ExperimentOutlined, ThunderboltOutlined } from '@ant-design/icons';
import type { EntityDefDTO, FieldDefDTO, JsonMap } from '@/api/types';
import {
  analyzeData,
  applyModelSuggestion,
  generateFormLayout,
  oneClickProductize,
  requestModelSuggestion,
} from '@/api/ai';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { listFieldDefinitions } from '@/fields';
import { AdminScaffold } from './_shared';
import { useAppSelection, useEntityOptions } from './_scope';

const { Text, Title, Paragraph } = Typography;

const FIELD_TYPES = listFieldDefinitions()
  .filter((definition) => definition.key === definition.key.toUpperCase())
  .map((definition) => ({ value: definition.key, label: definition.label }));

/** Pull `entities[]` out of whatever shape the model endpoint returned. */
function entitiesOf(suggestion: JsonMap | null): EntityDefDTO[] {
  const raw = suggestion?.entities ?? suggestion?.model;
  if (Array.isArray(raw)) return raw as EntityDefDTO[];
  if (raw && typeof raw === 'object') return [raw as EntityDefDTO];
  return [];
}

export function AiModelingPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  /** 「这个应用没有实体」让用户去建模，「实体没读到」让用户去查接口 —— 合并成一句就两头都指错。 */
  const entityGateText = (fallback: string) =>
    entitiesSource.read ? fallback : `实体列表没有读到：${entitiesSource.reason}`;
  const [prompt, setPrompt] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [proposed, setProposed] = useState<EntityDefDTO[]>([]);
  const [suggestion, setSuggestion] = useState<JsonMap | null>(null);
  const [report, setReport] = useState<string | null>(null);
  /** Which proposed entity the edit table below is bound to. */
  const [entityIndex, setEntityIndex] = useState(0);

  const run = useCallback(
    async (label: string, fn: () => Promise<unknown>) => {
      setBusy(label);
      try {
        const result = await fn();
        if (label === 'suggest') {
          const map = (result ?? {}) as JsonMap;
          setSuggestion(map);
          const list = entitiesOf(map);
          setProposed(list);
          if (!list.length) message.info('模型没有返回可解析的实体结构，请查看原始结果');
        }
        return result;
      } catch (err) {
        message.error(err instanceof Error ? err.message : '调用失败');
        return null;
      } finally {
        setBusy(null);
      }
    },
    [],
  );

  const patchField = useCallback((entityIndex: number, fieldIndex: number, patch: Partial<FieldDefDTO>) => {
    setProposed((prev) =>
      prev.map((entity, i) =>
        i !== entityIndex
          ? entity
          : {
              ...entity,
              fields: (entity.fields ?? []).map((field, j) => (j === fieldIndex ? { ...field, ...patch } : field)),
            },
      ),
    );
  }, []);

  const apply = useCallback(async () => {
    if (!suggestion) return;
    const result = await run('apply', () =>
      applyModelSuggestion({ ...suggestion, appCode, tenantCode: DEFAULT_TENANT_CODE, entities: proposed }),
    );
    if (result) message.success('建议已落库，去模型设计器确认字段与 provision');
  }, [suggestion, run, appCode, proposed]);

  const columns: ColumnsType<FieldDefDTO> = [
    {
      title: '字段编码',
      dataIndex: 'fieldCode',
      width: 180,
      render: (value: string, _row, index) => (
        <Input size="small" value={value} onChange={(event) => patchField(entityIndex, index, { fieldCode: event.target.value })} />
      ),
    },
    {
      title: '名称',
      dataIndex: 'fieldName',
      width: 150,
      render: (value: string, _row, index) => (
        <Input size="small" value={value ?? ''} onChange={(event) => patchField(entityIndex, index, { fieldName: event.target.value })} />
      ),
    },
    {
      title: '类型',
      dataIndex: 'fieldType',
      width: 130,
      render: (value: string, _row, index) => (
        <Select
          size="small"
          style={{ width: '100%' }}
          value={(value ?? 'STRING').toUpperCase()}
          options={FIELD_TYPES}
          onChange={(next) => patchField(entityIndex, index, { fieldType: next })}
        />
      ),
    },
    {
      title: '必填',
      dataIndex: 'required',
      width: 60,
      render: (value: boolean, _row, index) => (
        <input
          type="checkbox"
          checked={Boolean(value)}
          onChange={(event) => patchField(entityIndex, index, { required: event.target.checked })}
        />
      ),
    },
    { title: '说明', dataIndex: 'description' },
  ];

  return (
    <AdminScaffold
      title="AI 建模"
      description="用自然语言描述业务，让模型给出实体与字段草案，人工确认后再落库 —— 草案永远可以改，不自动 provision。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={reloadApps}
    >
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space direction="vertical" style={{ width: '100%' }} size={10}>
          <Input.TextArea
            rows={3}
            value={prompt}
            placeholder="例如：我要管客户，记录名称、手机号、等级（用字典 vip_level）、余额和最近联系时间；一个客户有多条跟进记录。"
            onChange={(event) => setPrompt(event.target.value)}
          />
          <Space wrap size={8}>
            <Button
              type="primary"
              icon={<BulbOutlined />}
              loading={busy === 'suggest'}
              disabled={!appCode || !prompt.trim()}
              onClick={() => void run('suggest', () => requestModelSuggestion(appCode, prompt))}
            >
              生成模型建议
            </Button>
            <Button
              icon={<ExperimentOutlined />}
              loading={busy === 'analyze'}
              disabled={!appCode}
              onClick={async () => {
                const target = entityOptions[0]?.value;
                if (!target) {
                  message.warning(entityGateText('该应用还没有实体可分析'));
                  return;
                }
                const result = await run('analyze', () => analyzeData(appCode, target));
                setReport(result ? JSON.stringify(result, null, 2) : null);
              }}
            >
              数据分析
            </Button>
            <Button
              icon={<ThunderboltOutlined />}
              loading={busy === 'productize'}
              disabled={!appCode}
              onClick={async () => {
                const result = await run('productize', () => oneClickProductize(appCode));
                setReport(result ? JSON.stringify(result, null, 2) : null);
              }}
            >
              一键产品化
            </Button>
            <Button
              disabled={!appCode}
              onClick={async () => {
                const target = entityOptions[0]?.value;
                if (!target) {
                  message.warning(entityGateText('该应用还没有实体'));
                  return;
                }
                const result = await run('form', () => generateFormLayout(appCode, target));
                setReport(result ? JSON.stringify(result, null, 2) : null);
              }}
            >
              生成表单布局
            </Button>
          </Space>
        </Space>
      </Card>

      {proposed.length > 0 ? (
        <Card
          size="small"
          title="待确认的模型草案"
          extra={
            <Space size={8}>
              {proposed.length > 1 ? (
                <Select
                  size="small"
                  style={{ minWidth: 160 }}
                  value={entityIndex}
                  options={proposed.map((entity, index) => ({
                    value: index,
                    label: entity.entityName || entity.entityCode || `实体 ${index + 1}`,
                  }))}
                  onChange={(value) => setEntityIndex(Number(value))}
                />
              ) : null}
              <Button type="primary" size="small" loading={busy === 'apply'} onClick={() => void apply()}>
                应用到模型
              </Button>
            </Space>
          }
        >
          {proposed.map((entity, entityIndex) => (
            <div key={`${entity.entityCode}-${entityIndex}`} style={{ marginBottom: 16 }}>
              <Title level={5} style={{ marginTop: 0 }}>
                {entity.entityName || entity.entityCode}{' '}
                <Text type="secondary" style={{ fontSize: 12, fontWeight: 400 }}>
                  <Tag>{entity.entityCode}</Tag>
                  {entity.tableName ? <Tag>{entity.tableName}</Tag> : null}
                </Text>
              </Title>
              <Table<FieldDefDTO>
                size="small"
                rowKey={(row) => String(row.fieldCode)}
                columns={columns}
                dataSource={entity.fields ?? []}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            </div>
          ))}
        </Card>
      ) : null}

      {report ? (
        <Card size="small" title="返回结果" style={{ marginTop: 12 }} extra={<Button size="small" onClick={() => setReport(null)}>关闭</Button>}>
          <pre className="zlc-ddl">{report}</pre>
        </Card>
      ) : null}

      <Alert
        style={{ marginTop: 12 }}
        type="warning"
        showIcon
        message="AI 只是草稿来源"
        description={
          <Paragraph style={{ margin: 0 }}>
            模型输出的字段类型与命名不保证正确，所以上面的表格可逐格改。落库后仍需在设计器里 provision 才会生成物理表。
          </Paragraph>
        }
      />
    </AdminScaffold>
  );
}
