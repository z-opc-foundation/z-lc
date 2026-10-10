import { useCallback, useMemo, useState } from 'react';
import {
  Button,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnMeta, ViewConfigDTO } from '@/lc/api/types';
import {
  createViewConfig,
  deleteViewConfig,
  listViewConfigs,
  newViewConfigDraft,
  parseViewConfig,
  updateViewConfig,
} from '@/lc/api/viewConfig';
import { formatTime } from './_scope';
import { AdminScaffold, ListBanner } from './_shared';
import { entityNotFoundContent, listEmptyText, useAppSelection, useEntityOptions, useResourceList } from './_scope';

const { Text } = Typography;

/** 与工作区 VIEW_TABS 一一对应：少一项，这里就会把一个能用的视图显示成裸枚举名。 */
const VIEW_TYPES = [
  { value: 'LIST', label: '表格' },
  { value: 'FORM', label: '表单' },
  { value: 'DETAIL', label: '详情' },
  { value: 'KANBAN', label: '看板' },
  { value: 'GALLERY', label: '画廊' },
  { value: 'CALENDAR', label: '日历' },
  { value: 'CHART', label: '图表' },
];

interface Draft {
  id?: number | null;
  entityCode: string;
  viewType: string;
  name: string;
  columnMeta: ColumnMeta[];
  raw: string;
  useRaw: boolean;
}

function draftFrom(view?: Partial<ViewConfigDTO>): Draft {
  const config = view?.config ? parseViewConfig<Record<string, unknown>>(view.config, {}) : {};
  return {
    id: view?.id,
    entityCode: view?.entityCode ?? '',
    viewType: view?.viewType ?? 'LIST',
    name: typeof config.name === 'string' ? config.name : '',
    columnMeta: Array.isArray(config.columnMeta) ? (config.columnMeta as ColumnMeta[]) : [],
    raw: JSON.stringify(config, null, 2),
    useRaw: !Array.isArray(config.columnMeta),
  };
}

export function ViewConfigsPage() {
  const { appCode, setAppCode, options, error: appError, reload: reloadApps } = useAppSelection();
  const entitiesSource = useEntityOptions(appCode);
  const entityOptions = entitiesSource.options;
  const [draft, setDraft] = useState<Draft | null>(null);

  const { rows, state, error, loading, reload } = useResourceList<ViewConfigDTO>(
    () => listViewConfigs(appCode),
    appCode || null,
  );

  const save = useCallback(async () => {
    if (!draft) return;
    if (!draft.entityCode) {
      message.warning('请选择实体');
      return;
    }
    let config: Record<string, unknown>;
    if (draft.useRaw) {
      try {
        config = JSON.parse(draft.raw || '{}') as Record<string, unknown>;
      } catch {
        message.error('config 不是合法 JSON');
        return;
      }
    } else {
      config = { name: draft.name, columnMeta: draft.columnMeta };
    }
    const payload = { ...config };
    const configText = JSON.stringify(payload);
    try {
      if (draft.id) {
        await updateViewConfig({ id: draft.id, config: configText, viewType: draft.viewType });
      } else {
        await createViewConfig(
          newViewConfigDraft({ appCode, entityCode: draft.entityCode, viewType: draft.viewType, config: payload }),
        );
      }
      message.success('已保存');
      setDraft(null);
      reload();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [draft, appCode, reload]);

  const columns: ColumnsType<ViewConfigDTO> = [
    { title: '实体', dataIndex: 'entityCode', width: 170 },
    {
      title: '视图类型',
      dataIndex: 'viewType',
      width: 110,
      render: (value: string) => <Tag>{VIEW_TYPES.find((item) => item.value === value)?.label ?? value}</Tag>,
    },
    {
      title: '名称',
      dataIndex: 'config',
      width: 190,
      render: (value: string) => {
        const parsed = parseViewConfig<{ name?: string }>(value, {});
        return parsed.name || <Text type="secondary">未命名</Text>;
      },
    },
    {
      title: '列数',
      key: 'cols',
      width: 90,
      render: (_v, row) => {
        const parsed = parseViewConfig<{ columnMeta?: ColumnMeta[] }>(row.config, {});
        return parsed.columnMeta?.length ?? 0;
      },
    },
    {
      title: 'config 摘要',
      dataIndex: 'config',
      key: 'summary',
      ellipsis: true,
      render: (value: string) => <Text type="secondary" style={{ fontSize: 12 }}>{(value ?? '').slice(0, 120)}</Text>,
    },
    { title: '更新', dataIndex: 'updateTime', width: 150, render: (v: string | number | null | undefined) => formatTime(v) },
    {
      title: '',
      key: 'ops',
      width: 110,
      render: (_v, row) => (
        <Space size={0}>
          <Button size="small" type="link" onClick={() => setDraft(draftFrom(row))}>
            编辑
          </Button>
          <Popconfirm
            title="删除这个视图？"
            onConfirm={async () => {
              if (!row.id) return;
              await deleteViewConfig(row.id);
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

  const metaColumns = useMemo<ColumnsType<ColumnMeta>>(
    () => [
      { title: '字段', dataIndex: 'fieldCode', width: 200 },
      {
        title: '顺序',
        key: 'order',
        width: 130,
        render: (_v, _row, index) => (
          <Space size={0}>
            <Tag>{index + 1}</Tag>
            <Button
              size="small"
              type="text"
              disabled={index === 0}
              onClick={() =>
                setDraft((prev) => {
                  if (!prev) return prev;
                  const next = [...prev.columnMeta];
                  const [moved] = next.splice(index - 1, 1);
                  if (moved) next.splice(index, 0, moved);
                  return { ...prev, columnMeta: next };
                })
              }
            >
              上移
            </Button>
            <Button
              size="small"
              type="text"
              disabled={index === (draft?.columnMeta.length ?? 0) - 1}
              onClick={() =>
                setDraft((prev) => {
                  if (!prev) return prev;
                  const next = [...prev.columnMeta];
                  const [moved] = next.splice(index + 1, 1);
                  if (moved) next.splice(index, 0, moved);
                  return { ...prev, columnMeta: next };
                })
              }
            >
              下移
            </Button>
          </Space>
        ),
      },
      {
        title: '宽度',
        dataIndex: 'width',
        width: 100,
        render: (_v, row, index) => (
          <Input
            size="small"
            value={row.width === undefined ? '' : String(row.width)}
            onChange={(event) =>
              setDraft((prev) =>
                prev
                  ? {
                      ...prev,
                      columnMeta: prev.columnMeta.map((item, i) =>
                        i === index ? { ...item, width: Number(event.target.value) || undefined } : item,
                      ),
                    }
                  : prev,
              )
            }
          />
        ),
      },
      {
        title: '隐藏',
        dataIndex: 'hidden',
        width: 70,
        render: (_v, row, index) => (
          <input
            type="checkbox"
            checked={row.hidden === true}
            onChange={(event) =>
              setDraft((prev) =>
                prev
                  ? {
                      ...prev,
                      columnMeta: prev.columnMeta.map((item, i) =>
                        i === index ? { ...item, hidden: event.target.checked } : item,
                      ),
                    }
                  : prev,
              )
            }
          />
        ),
      },
    ],
    [draft?.columnMeta.length],
  );

  return (
    <AdminScaffold
      title="视图配置"
      description="保存下来的列布局与筛选组合。工作区里点「保存为视图」写入的就是这里的数据。"
      appCode={appCode}
      onAppCode={setAppCode}
      appOptions={options}
      appError={appError}
      onRefresh={() => {
        reloadApps();
        reload();
      }}
      actions={
        <Button type="primary" icon={<PlusOutlined />} disabled={!appCode} onClick={() => setDraft(draftFrom())}>
          新建视图
        </Button>
      }
    >
      <ListBanner state={state} error={error} onRetry={reload} label="视图配置" />
      <Table<ViewConfigDTO>
        size="small"
        rowKey={(row) => String(row.id ?? `${row.entityCode}-${row.viewType}`)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 20 }}
        locale={{ emptyText: listEmptyText(state, '视图') }}
      />

      <Modal
        open={Boolean(draft)}
        title={draft?.id ? '编辑视图' : '新建视图'}
        width={760}
        onOk={() => void save()}
        onCancel={() => setDraft(null)}
        okText="保存"
        cancelText="取消"
      >
        {draft ? (
          <Space direction="vertical" style={{ width: '100%' }} size={10}>
            <Space wrap size={10}>
              <Select
                style={{ minWidth: 220 }}
                placeholder="实体"
                showSearch
                value={draft.entityCode || undefined}
                options={entityOptions}
                notFoundContent={entityNotFoundContent(entitiesSource)}
                onChange={(entityCode) => setDraft({ ...draft, entityCode })}
              />
              <Select
                style={{ minWidth: 120 }}
                value={draft.viewType}
                options={VIEW_TYPES}
                onChange={(viewType) => setDraft({ ...draft, viewType })}
              />
              <Input
                addonBefore="名称"
                style={{ width: 260 }}
                value={draft.name}
                onChange={(event) => setDraft({ ...draft, name: event.target.value })}
              />
            </Space>

            {!draft.useRaw ? (
              <>
                <Table<ColumnMeta>
                  size="small"
                  rowKey={(row) => row.fieldCode}
                  columns={metaColumns}
                  dataSource={draft.columnMeta}
                  pagination={false}
                  locale={{ emptyText: '还没有列，回工作区调好列布局后用「保存为视图」，或直接切到 JSON 编辑' }}
                />
                <Space>
                  <Input
                    placeholder="字段编码"
                    style={{ width: 220 }}
                    onChange={() => undefined}
                    onPressEnter={(event) => {
                      const fieldCode = (event.target as HTMLInputElement).value.trim();
                      if (!fieldCode) return;
                      setDraft({ ...draft, columnMeta: [...draft.columnMeta, { fieldCode }] });
                      (event.target as HTMLInputElement).value = '';
                    }}
                  />
                  <Text type="secondary">回车添加一列</Text>
                  <Button size="small" onClick={() => setDraft({ ...draft, useRaw: true, raw: JSON.stringify({ name: draft.name, columnMeta: draft.columnMeta }, null, 2) })}>
                    改用 JSON 编辑
                  </Button>
                </Space>
              </>
            ) : (
              <>
                <Input.TextArea
                  rows={12}
                  value={draft.raw}
                  onChange={(event) => setDraft({ ...draft, raw: event.target.value })}
                />
                <Button size="small" onClick={() => setDraft(draftFrom({ ...draft, config: draft.raw }))}>
                  改用结构化编辑
                </Button>
              </>
            )}
          </Space>
        ) : null}
      </Modal>
    </AdminScaffold>
  );
}
