import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import type { DictDTO, EntityDefDTO, FieldDefDTO } from '@/api/types';
import { listApps } from '@/api/app';
import {
  createEntity,
  deleteEntity,
  listAdminEntities,
  provisionAllEntities,
  provisionEntity,
  updateEntity,
} from '@/api/admin';
import { DEFAULT_TENANT_CODE } from '@/api/client';
import { listFieldDefinitions } from '@/fields';
import type { ConfigFieldSpec } from '@/fields';
import { fieldCodeProblem } from '@/fields/columnRules';
import { useWorkspace } from '@/hooks/useWorkspace';
import { StateBlock } from '@/components/StateBlock';

const { Text, Title } = Typography;

/**
 * Client-side mirror of the backend DDL, for the preview pane only.
 * `fieldLength` / `scale` have to be re-applied here: the descriptor's `dbType`
 * is the bare type name, and the server stamps `(n)` / `(p,s)` from the field.
 */
function ddlTypeOf(base: string, field: FieldDefDTO): string {
  // Descriptors may hand back `VARCHAR(255)` already, so compare on the bare name.
  const kind = base.toUpperCase().replace(/\s*\(.*\)\s*/, '');
  if (kind === 'VARCHAR' || kind === 'CHAR') return `${kind}(${field.fieldLength ?? 255})`;
  if (kind === 'DECIMAL' || kind === 'NUMERIC') {
    return `${kind}(${field.fieldLength ?? 18},${field.scale ?? 2})`;
  }
  return base;
}

function previewDdl(entity: EntityDefDTO, dbTypeOf: (fieldType: string) => string): string {
  const lines: string[] = [];
  lines.push(`CREATE TABLE IF NOT EXISTS \`${entity.tableName || entity.entityCode}\` (`);
  lines.push('  `id` BIGINT NOT NULL AUTO_INCREMENT,');
  for (const field of entity.fields ?? []) {
    if (!field.fieldCode) continue;
    const type = ddlTypeOf(dbTypeOf(field.fieldType || 'STRING'), field);
    let column = `  \`${field.fieldCode}\` ${type}`;
    if (field.required) column += ' NOT NULL';
    if (field.defaultValue) column += ` DEFAULT '${String(field.defaultValue).replace(/'/g, "''")}'`;
    lines.push(column + ',');
  }
  lines.push('  `tenant_code` VARCHAR(64),');
  lines.push('  `deleted` TINYINT DEFAULT 0,');
  lines.push('  `create_time` DATETIME,');
  lines.push('  `update_time` DATETIME,');
  lines.push('  PRIMARY KEY (`id`)');
  lines.push(`) COMMENT='${entity.entityName || entity.entityCode || ''}'`);
  return lines.join('\n');
}

/** Landing page: pick which app's model to design. */
export function DesignerIndexPage() {
  const query = useQueryApps();
  return (
    <div style={{ padding: 16, maxWidth: 720 }}>
      <Title level={4}>模型设计器</Title>
      <Text type="secondary">选择一个应用，编辑它的实体与字段，再 provision 成物理表。</Text>
      <StateBlock
        isLoading={query.isLoading}
        isError={query.isError}
        error={query.error}
        onRetry={() => void query.refetch()}
        isEmpty={!query.isLoading && (query.data?.length ?? 0) === 0}
        empty={<Empty description="还没有应用，先在「应用」页创建一个" />}
      >
        <List
          dataSource={query.data ?? []}
          renderItem={(app) => (
            <List.Item
              actions={[
                <Link key="open" to={`/designer/${app.appCode}`}>
                  设计模型
                </Link>,
              ]}
            >
              <List.Item.Meta
                title={app.appName || app.appCode}
                description={`${app.appCode} · ${app.entityCount ?? 0} 实体 · ${app.fieldCount ?? 0} 字段`}
              />
            </List.Item>
          )}
        />
      </StateBlock>
    </div>
  );
}

function useQueryApps() {
  // Kept local so the index page does not depend on the workspace query keys.
  const [data, setData] = useState<Awaited<ReturnType<typeof listApps>>>([]);
  const [isLoading, setLoading] = useState(true);
  const [isError, setError] = useState(false);
  const [error, setErrorDetail] = useState<unknown>(null);
  const refetch = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      setData(await listApps());
    } catch (err) {
      setError(true);
      setErrorDetail(err);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => {
    void refetch();
  }, [refetch]);
  return { data, isLoading, isError, error, refetch };
}

export function DesignerPage() {
  const { appCode = '' } = useParams();
  const navigate = useNavigate();
  const { ctx, meta } = useWorkspace(appCode);
  const [entities, setEntities] = useState<EntityDefDTO[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  /** 草稿是"新建按钮给的那份"还是"从清单里选出来的那份"——决定下面那个派生 effect 要不要跑。 */
  const [creating, setCreating] = useState(false);
  const [draft, setDraft] = useState<EntityDefDTO | null>(null);
  const [dirty, setDirty] = useState(false);
  // 挂载即发请求：首帧该是"在读"，不是"还没有实体"。
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [provisioning, setProvisioning] = useState(false);
  const [ddl, setDdl] = useState<string | null>(null);
  const [addFieldOpen, setAddFieldOpen] = useState(false);

  const reload = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const list = await listAdminEntities(appCode, DEFAULT_TENANT_CODE);
      setEntities(list);
      setSelectedId((prev) => prev ?? list[0]?.id ?? null);
    } catch (err) {
      // 只弹一条三秒就消失的 toast，之后侧栏会一直挂着"还没有实体" —— 而设计器正是
      // 判断这句话真假的地方，用户会照着它去新建一个其实已经存在的实体（撞唯一编码那族）。
      setLoadError(err);
    } finally {
      setLoading(false);
    }
  }, [appCode]);

  useEffect(() => {
    void reload();
  }, [reload]);

  useEffect(() => {
    // 新建按钮自己拥有那份草稿：切实体、刷清单都不该把它冲掉。
    // 少了这一句，点"新建实体"会把 selectedId 置空 → 这里立刻 setDraft(null) →
    // 界面回到"选择左侧实体开始编辑"，那个按钮等于空操作（实测：整条新建路径走不通）。
    if (creating) return;
    const found = entities.find((item) => item.id === selectedId);
    setDraft(found ? { ...found, fields: (found.fields ?? []).map((field) => ({ ...field })) } : null);
    setDirty(false);
    setDdl(null);
  }, [selectedId, entities, creating]);

  const dbTypeOf = useCallback(
    (fieldType: string) => {
      const key = (fieldType || 'STRING').toUpperCase();
      return ctx.descriptors[key]?.dbType ?? 'VARCHAR(255)';
    },
    [ctx.descriptors],
  );

  const patchDraft = useCallback((patch: Partial<EntityDefDTO>) => {
    setDraft((prev) => (prev ? { ...prev, ...patch } : prev));
    setDirty(true);
  }, []);

  const patchField = useCallback((index: number, patch: Partial<FieldDefDTO>) => {
    setDraft((prev) => {
      if (!prev) return prev;
      const fields = [...(prev.fields ?? [])];
      const target = fields[index];
      if (!target) return prev;
      fields[index] = { ...target, ...patch };
      return { ...prev, fields };
    });
    setDirty(true);
  }, []);

  const moveField = useCallback((index: number, delta: number) => {
    setDraft((prev) => {
      if (!prev) return prev;
      const fields = [...(prev.fields ?? [])];
      const to = index + delta;
      if (to < 0 || to >= fields.length) return prev;
      const [moved] = fields.splice(index, 1);
      if (!moved) return prev;
      fields.splice(to, 0, moved);
      return {
        ...prev,
        fields: fields.map((field, i) => ({ ...field, sortOrder: i + 1 })),
      };
    });
    setDirty(true);
  }, []);

  const removeField = useCallback((index: number) => {
    setDraft((prev) => {
      if (!prev) return prev;
      const fields = [...(prev.fields ?? [])];
      fields.splice(index, 1);
      return { ...prev, fields: fields.map((field, i) => ({ ...field, sortOrder: i + 1 })) };
    });
    setDirty(true);
  }, []);

  const addField = useCallback(
    (fieldType: string) => {
      const used = new Set((draft?.fields ?? []).map((field) => field.fieldCode));
      let seq = (draft?.fields?.length ?? 0) + 1;
      let code = `field_${seq}`;
      while (used.has(code)) code = `field_${++seq}`;
      setDraft((prev) => {
        if (!prev) return prev;
        const fields = [...(prev.fields ?? [])];
        fields.push({
          fieldCode: code,
          fieldName: code,
          fieldType,
          required: false,
          sortOrder: fields.length + 1,
        } as FieldDefDTO);
        return { ...prev, fields };
      });
      setDirty(true);
      setAddFieldOpen(false);
    },
    [draft, setDraft],
  );

  const codeIssues = useMemo(() => {
    if (!draft) return [] as string[];
    const seen = new Map<string, number>();
    for (const field of draft.fields ?? []) {
      seen.set(field.fieldCode, (seen.get(field.fieldCode) ?? 0) + 1);
    }
    const problems: string[] = [];
    for (const field of draft.fields ?? []) {
      const problem = fieldCodeProblem(field.fieldCode);
      if (problem) problems.push(problem);
    }
    for (const [code, count] of seen) if (count > 1) problems.push(`字段编码「${code}」重复`);
    if (!draft.entityCode?.trim()) problems.push('实体编码不能为空');
    if (!draft.tableName?.trim()) problems.push('物理表名不能为空');
    return problems;
  }, [draft]);

  const save = useCallback(async () => {
    if (!draft || codeIssues.length) return;
    try {
      if (draft.id) {
        await updateEntity(draft.id, draft);
        message.success('实体已保存');
      } else {
        const created = await createEntity(appCode, draft, DEFAULT_TENANT_CODE);
        setCreating(false);
        setSelectedId(created.id ?? null);
        message.success('实体已创建');
      }
      setDirty(false);
      await reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [draft, codeIssues, appCode, reload]);

  const provision = useCallback(async () => {
    if (!draft?.id) {
      message.warning('请先保存实体');
      return;
    }
    setProvisioning(true);
    try {
      const executed = await provisionEntity(draft.id);
      setDdl(executed);
      message.success('物理表已 provision');
      await reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'provision 失败');
    } finally {
      setProvisioning(false);
    }
  }, [draft, reload]);

  const provisionAll = useCallback(async () => {
    setProvisioning(true);
    try {
      const result = await provisionAllEntities(appCode, DEFAULT_TENANT_CODE);
      message.success(`已 provision ${Object.keys(result).length} 张表`);
      await reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '批量 provision 失败');
    } finally {
      setProvisioning(false);
    }
  }, [appCode, reload]);

  const fieldColumns = useMemo<ColumnsType<FieldDefDTO>>(() => {
    const defs = draft?.fields ?? [];
    return [
      {
        title: '字段编码',
        dataIndex: 'fieldCode',
        width: 168,
        render: (_v, _r, index) => (
          <Input
            size="small"
            value={defs[index]?.fieldCode}
            status={defs[index]?.fieldCode && fieldCodeProblem(defs[index].fieldCode) ? 'error' : undefined}
            onChange={(event) => patchField(index, { fieldCode: event.target.value })}
          />
        ),
      },
      {
        title: '显示名',
        dataIndex: 'fieldName',
        width: 140,
        render: (_v, _r, index) => (
          <Input
            size="small"
            value={defs[index]?.fieldName}
            onChange={(event) => patchField(index, { fieldName: event.target.value })}
          />
        ),
      },
      {
        title: '类型',
        dataIndex: 'fieldType',
        width: 120,
        render: (_v, field, index) => (
          <Select
            size="small"
            style={{ width: '100%' }}
            value={(field.fieldType ?? 'STRING').toUpperCase()}
            options={listFieldDefinitions()
              .filter((definition) => definition.key === definition.key.toUpperCase())
              .sort((a, b) => a.order - b.order)
              .map((definition) => ({ value: definition.key, label: definition.label }))}
            onChange={(next) => patchField(index, { fieldType: next })}
          />
        ),
      },
      {
        title: '必填',
        dataIndex: 'required',
        width: 56,
        render: (_v, field, index) => (
          <Switch
            size="small"
            checked={Boolean(field.required)}
            onChange={(checked) => patchField(index, { required: checked })}
          />
        ),
      },
      {
        title: '类型配置',
        dataIndex: 'fieldType',
        key: 'config',
        render: (_v, field, index) => (
          <FieldConfigEditors
            fieldType={(field.fieldType ?? 'STRING').toUpperCase()}
            field={field}
            dicts={ctx.dicts}
            entities={ctx.entities}
            onPatch={(patch) => patchField(index, patch)}
          />
        ),
      },
      {
        title: '',
        key: 'ops',
        width: 104,
        render: (_v, _r, index) => (
          <Space size={0}>
            <Button size="small" type="text" icon={<ArrowUpOutlined />} onClick={() => moveField(index, -1)} />
            <Button size="small" type="text" icon={<ArrowDownOutlined />} onClick={() => moveField(index, 1)} />
            <Popconfirm title="删除这个字段？" onConfirm={() => removeField(index)}>
              <Button size="small" type="text" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Space>
        ),
      },
    ];
  }, [draft?.fields, ctx.dicts, ctx.entities, patchField, moveField, removeField]);

  const palette = useMemo(
    () =>
      listFieldDefinitions()
        .filter((definition) => definition.key === definition.key.toUpperCase())
        .sort((a, b) => a.order - b.order),
    [],
  );

  return (
    <div style={{ display: 'flex', gap: 12, padding: 12, alignItems: 'flex-start' }}>
      <Card
        size="small"
        title={
          <Space>
            <Text>{meta?.app?.appName || appCode}</Text>
            <Link to={`/${appCode}`}>进入工作区</Link>
          </Space>
        }
        style={{ width: 248, flex: '0 0 248px' }}
        extra={
          <Space size={4}>
            <Tooltip title="新建实体">
              <Button
                size="small"
                icon={<PlusOutlined />}
                onClick={() => {
                  setCreating(true);
                  setSelectedId(null);
                  setDraft({
                    appCode,
                    tenantCode: DEFAULT_TENANT_CODE,
                    entityCode: '',
                    entityName: '',
                    tableName: '',
                    // 不预置 systemFieldDefs()：id / create_time / update_time 是引擎自建列，
                    // 进不了用户字段表（后端会 400，见 fields/columnRules.ts），视图那边本来就自己合成它们。
                    fields: [],
                  });
                  setDirty(true);
                }}
              />
            </Tooltip>
            <Tooltip title="从数据库导入表">
              <Button size="small" icon={<DatabaseOutlined />} onClick={() => navigate(`/db-import?appCode=${appCode}`)} />
            </Tooltip>
          </Space>
        }
      >
        <StateBlock
          isLoading={loading}
          isError={Boolean(loadError)}
          error={loadError}
          errorTitle="实体列表没有读到"
          onRetry={() => void reload()}
          isEmpty={entities.length === 0}
          empty={
            <div style={{ padding: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                还没有实体，点右上角「+」新建，或从数据库导入表
              </Text>
            </div>
          }
        >
          <List
            size="small"
            dataSource={entities}
            renderItem={(item) => (
            <List.Item
              style={{
                cursor: 'pointer',
                background: item.id === selectedId ? '#eef4ff' : undefined,
                paddingLeft: 8,
              }}
              onClick={() => {
                setCreating(false);
                setSelectedId(item.id ?? null);
              }}
              actions={[
                <Popconfirm
                  key="del"
                  title="删除实体？不会动已建好的物理表。"
                  onConfirm={async () => {
                    if (!item.id) return;
                    await deleteEntity(item.id);
                    message.success('已删除');
                    if (selectedId === item.id) setSelectedId(null);
                    void reload();
                  }}
                >
                  <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={(event) => event.stopPropagation()} />
                </Popconfirm>,
              ]}
            >
              <List.Item.Meta
                title={<Text style={{ fontSize: 13 }}>{item.entityName || item.entityCode}</Text>}
                description={<Text type="secondary" style={{ fontSize: 12 }}>{item.entityCode} · {item.fields?.length ?? 0} 字段</Text>}
              />
            </List.Item>
          )}
          />
        </StateBlock>
        <Button size="small" block icon={<CloudUploadOutlined />} loading={provisioning} onClick={() => void provisionAll()}>
          Provision 全部实体
        </Button>
      </Card>

      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {!draft ? (
          <Empty description="选择左侧实体开始编辑，或新建一个实体" />
        ) : (
          <>
            <Card size="small">
              <Space wrap size={12} style={{ width: '100%' }}>
                <Input
                  addonBefore="实体编码"
                  style={{ width: 260 }}
                  value={draft.entityCode}
                  onChange={(event) => patchDraft({ entityCode: event.target.value })}
                />
                <Input
                  addonBefore="实体名称"
                  style={{ width: 240 }}
                  value={draft.entityName}
                  onChange={(event) => patchDraft({ entityName: event.target.value })}
                />
                <Input
                  addonBefore="物理表名"
                  style={{ width: 300 }}
                  value={draft.tableName ?? ''}
                  onChange={(event) => patchDraft({ tableName: event.target.value })}
                />
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  disabled={!dirty || codeIssues.length > 0}
                  onClick={() => void save()}
                >
                  保存
                </Button>
                <Button
                  icon={<CloudUploadOutlined />}
                  loading={provisioning}
                  disabled={!draft.id}
                  onClick={() => void provision()}
                >
                  Provision
                </Button>
                <Button icon={<PlusOutlined />} onClick={() => setAddFieldOpen(true)}>
                  添加字段
                </Button>
              </Space>
              {codeIssues.length > 0 ? (
                <Alert
                  style={{ marginTop: 10 }}
                  type="warning"
                  showIcon
                  message="无法保存"
                  description={<ul style={{ margin: 0, paddingLeft: 18 }}>{codeIssues.map((issue) => <li key={issue}>{issue}</li>)}</ul>}
                />
              ) : null}
            </Card>

            <Card size="small" title={`字段（${draft.fields?.length ?? 0}）`}>
              <Table<FieldDefDTO>
                size="small"
                rowKey={(row) => `${row.fieldCode}-${row.sortOrder}`}
                columns={fieldColumns}
                dataSource={draft.fields ?? []}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            </Card>

            <Card size="small" title="DDL 预览" extra={<Text type="secondary">provision 时以服务端实际生成为准</Text>}>
              <pre className="zlc-ddl">{ddl ?? previewDdl(draft, dbTypeOf)}</pre>
              {ddl ? <Tag color="green">这是服务端实际执行的 DDL</Tag> : null}
            </Card>
          </>
        )}
      </div>

      <Modal open={addFieldOpen} title="选择字段类型" footer={null} onCancel={() => setAddFieldOpen(false)}>
        <Space wrap size={8}>
          {palette.map((definition) => (
            <Button key={definition.key} onClick={() => addField(definition.key)}>
              {definition.label}
              <Text type="secondary" style={{ marginLeft: 6, fontSize: 12 }}>{definition.key}</Text>
            </Button>
          ))}
        </Space>
      </Modal>
    </div>
  );
}

/**
 * Renders a field type's own config sub-form from the registry's declarative
 * `configFields` — so supporting a new type never means editing this page.
 */
function FieldConfigEditors({
  fieldType,
  field,
  dicts,
  entities,
  onPatch,
}: {
  fieldType: string;
  field: FieldDefDTO;
  dicts: DictDTO[];
  entities: EntityDefDTO[];
  onPatch: (patch: Partial<FieldDefDTO>) => void;
}) {
  const definition = listFieldDefinitions().find((item) => item.key === fieldType);
  const specs: ConfigFieldSpec[] = [...(definition?.configFields ?? [])];
  if (!specs.length) {
    return (
      <Input
        size="small"
        placeholder="说明"
        value={field.description ?? ''}
        onChange={(event) => onPatch({ description: event.target.value })}
      />
    );
  }
  return (
    <Space wrap size={6}>
      {specs.map((spec) => {
        const value = field[spec.key] as string | number | undefined;
        if (spec.kind === 'number') {
          return (
            <InputNumberPair key={spec.key} spec={spec} value={typeof value === 'number' ? value : undefined} onPatch={onPatch} />
          );
        }
        if (spec.kind === 'dictSelect') {
          return (
            <Select
              key={spec.key}
              size="small"
              allowClear
              style={{ minWidth: 140 }}
              placeholder={spec.label}
              value={value as string | undefined}
              options={dicts.map((dict) => ({ value: dict.dictCode, label: dict.dictName || dict.dictCode }))}
              onChange={(next) => onPatch({ [spec.key]: next } as Partial<FieldDefDTO>)}
            />
          );
        }
        if (spec.kind === 'entitySelect') {
          return (
            <Select
              key={spec.key}
              size="small"
              allowClear
              style={{ minWidth: 140 }}
              placeholder={spec.label}
              value={value as string | undefined}
              options={entities.map((entity) => ({
                value: entity.entityCode,
                label: entity.entityName || entity.entityCode,
              }))}
              onChange={(next) => onPatch({ [spec.key]: next } as Partial<FieldDefDTO>)}
            />
          );
        }
        return (
          <Input
            key={spec.key}
            size="small"
            style={{ minWidth: 130 }}
            placeholder={spec.label}
            value={(value as string | undefined) ?? ''}
            onChange={(event) => onPatch({ [spec.key]: event.target.value } as Partial<FieldDefDTO>)}
          />
        );
      })}
    </Space>
  );
}

function InputNumberPair({
  spec,
  value,
  onPatch,
}: {
  spec: ConfigFieldSpec;
  value: number | undefined;
  onPatch: (patch: Partial<FieldDefDTO>) => void;
}) {
  return (
    <Tooltip title={spec.help ?? spec.label}>
      <Input
        size="small"
        style={{ width: 96 }}
        placeholder={spec.label}
        value={value === undefined ? '' : String(value)}
        onChange={(event) => {
          const raw = event.target.value.trim();
          onPatch({ [spec.key]: raw === '' ? undefined : Number(raw) } as Partial<FieldDefDTO>);
        }}
      />
    </Tooltip>
  );
}
