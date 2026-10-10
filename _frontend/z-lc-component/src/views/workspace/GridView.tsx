import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Button,
  Dropdown,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  DeleteOutlined,
  DownOutlined,
  DownloadOutlined,
  FundOutlined,
  HistoryOutlined,
  ReloadOutlined,
  SaveOutlined,
  SettingOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { Conjunction, EntityDefDTO, LcRow, QueryCondition, ViewConfigDTO } from '@/lc/api/types';
import { createViewConfig } from '@/lc/api/viewConfig';
import { deleteBatch, listRecords, updateRecord } from '@/lc/api/runtime';
import { readFieldValue } from '@yuku123/render/fields';
import { aggregateRecords } from '@/lc/api/runtime';
import type { ResolvedField } from '@yuku123/render/fields';
import { FilterBar } from '@/lc/views/grid/FilterBar';
import type { FilterState } from '@/lc/views/grid/FilterBar';
import { ColumnManagerDrawer } from '@/lc/views/grid/ColumnManagerDrawer';
import {
  projectColumns,
  pruneStaleConditions,
  pruneStaleSorts,
  visibleColumns,
} from '@/lc/views/grid/gridModel';
import { buildCsv, downloadText, fetchAllForExport } from '@/lc/views/grid/csv';
import { StateBlock } from '@/lc/components/StateBlock';
import { ImportDialog } from './ImportDialog';
import { UndoHistoryDrawer } from './UndoHistoryDrawer';
import {
  STAT_FUNCTIONS,
  UNIVERSAL_STATS,
  EMPTY_STATE,
  namedViews,
  readStoredState,
  viewLabel,
  viewState,
  withViewName,
  writeStoredState,
} from './viewConfigModel';
import type { StatFunction, WorkspaceViewState } from './viewConfigModel';

const { Text } = Typography;

const STAT_LABEL: Record<string, string> = {
  SUM: '合计',
  AVG: '平均',
  MIN: '最小',
  MAX: '最大',
  DISTINCT: '去重数',
  FILLED: '非空数',
};

function formatStat(value: unknown): string {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '—';
  }
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

/**
 * Meta-driven data grid: columns, editors, filter operators and CSV formatting
 * all come from the field registry, so nothing here knows what a `DECIMAL` or a
 * `DATE` is. Layout/filter/sort state is per (entity, view) and shareable.
 */
export interface GridViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  views: ViewConfigDTO[];
  appCode: string;
  tenantCode: string;
  onEditRecord: (id: number) => void;
  onOpenRecord: (id: number) => void;
  onCreateRecord: () => void;
  /** 筛选状态提升到页面级，切到看板/表单不会丢筛选. */
  conditions: QueryCondition[];
  conjunction: Conjunction;
  onFilterChange: (next: FilterState) => void;
}

interface Editing {
  rowId: number;
  fieldCode: string;
}

export function GridView({
  entity,
  resolvedFields,
  views,
  appCode,
  tenantCode,
  onEditRecord,
  onOpenRecord,
  onCreateRecord,
  conditions,
  conjunction,
  onFilterChange,
}: GridViewProps) {
  const entityCode = entity.entityCode ?? '';
  const allowed = useMemo(
    () => new Set(resolvedFields.map((field) => field.ctx.field.fieldCode)),
    [resolvedFields],
  );

  const [state, setState] = useState<WorkspaceViewState>(
    () => readStoredState(appCode, entityCode, 'LIST') ?? EMPTY_STATE,
  );
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [rows, setRows] = useState<LcRow[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [selected, setSelected] = useState<number[]>([]);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [columnDrawer, setColumnDrawer] = useState(false);
  const [saveOpen, setSaveOpen] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [importOpen, setImportOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [showStats, setShowStats] = useState(true);
  const requestId = useRef(0);

  /**
   * 页脚统计走服务端聚合，口径是"当前筛选命中的全部行"，
   * 不是这一页 20 行 —— 用分页数据前端加总会让人读出错误结论。
   */
  const stats = useQuery({
    queryKey: ['grid-stats', appCode, entityCode, tenantCode, state.stats, conditions, conjunction],
    queryFn: () =>
      aggregateRecords(entityCode, { appCode, tenantCode }, {
        aggregations: Object.fromEntries(
          Object.entries(state.stats).map(([fieldCode, fn]) => [fieldCode, [fn]]),
        ),
        conditions,
        conjunction,
      }),
    enabled: showStats && Object.keys(state.stats).length > 0,
    staleTime: 15_000,
  });

  const statsRow = stats.data?.[0];

  const columns = useMemo(() => {
    const projected = projectColumns(resolvedFields, state.columnMeta);
    return {
      all: projected,
      visible: visibleColumns(projected),
    };
  }, [resolvedFields, state.columnMeta]);

  const activeViewId = useMemo(() => {
    const match = namedViews(views, entityCode, 'LIST').find((view) => {
      const restored = viewState(view);
      return !!restored
        && JSON.stringify(restored.columnMeta) === JSON.stringify(state.columnMeta)
        && JSON.stringify(restored.sorts) === JSON.stringify(state.sorts)
        && JSON.stringify(restored.conditions) === JSON.stringify(conditions);
    });
    return match?.id;
  }, [views, entityCode, state, conditions]);

  useEffect(() => {
    writeStoredState(appCode, entityCode, 'LIST', { ...state, conditions, conjunction });
  }, [appCode, entityCode, state, conditions, conjunction]);

  const load = useCallback(async () => {
    const token = ++requestId.current;
    setLoading(true);
    setLoadError(null);
    try {
      const result = await listRecords(entityCode, { appCode, tenantCode }, {
        page,
        size: pageSize,
        conditions,
        conjunction,
        sorts: state.sorts,
      });
      if (token !== requestId.current) return;
      setRows(result?.records ?? []);
      setTotal(Number(result?.total ?? 0));
    } catch (err) {
      if (token !== requestId.current) return;
      setLoadError(err);
    } finally {
      if (token === requestId.current) setLoading(false);
    }
  }, [entityCode, appCode, tenantCode, page, pageSize, conditions, conjunction, state.sorts]);

  useEffect(() => {
    void load();
  }, [load]);

  const setStat = useCallback(
    (fieldCode: string, fn: StatFunction | undefined) =>
      setState((prev) => {
        const next = { ...prev.stats };
        if (!fn) {
          delete next[fieldCode];
        } else {
          next[fieldCode] = fn;
        }
        return { ...prev, stats: next };
      }),
    [],
  );

  const patchState = useCallback(
    (patch: Partial<WorkspaceViewState>) =>
      setState((prev) => ({ ...prev, ...patch })),
    [],
  );

  const commitInlineEdit = useCallback(
    async (row: LcRow, resolved: ResolvedField, next: unknown) => {
      const fieldCode = resolved.ctx.field.fieldCode;
      const wire = resolved.def.toFieldValue(next, resolved.ctx);
      const id = Number(row.id);
      try {
        await updateRecord(entityCode, id, { [fieldCode]: wire }, { appCode, tenantCode });
        setRows((prev) =>
          prev.map((item) => (Number(item.id) === id ? { ...item, [fieldCode]: wire } : item)),
        );
        message.success('已保存');
      } catch (err) {
        message.error(err instanceof Error ? err.message : '保存失败');
      } finally {
        setEditing(null);
      }
    },
    [entityCode, appCode, tenantCode],
  );

  const tableColumns = useMemo<ColumnsType<LcRow>>(() => {
    const list: ColumnsType<LcRow> = columns.visible.map(({ resolved, meta }) => {
      const fieldCode = resolved.ctx.field.fieldCode;
      return {
        title: resolved.ctx.field.fieldName || fieldCode,
        dataIndex: resolved.valueKey,
        key: fieldCode,
        width: meta.width,
        ellipsis: true,
        sorter: resolved.sortable,
        render: (_unknown: unknown, row: LcRow) => {
          const { value, displayValue } = readFieldValue(row, resolved);
          const inline = resolved.inlineEditable && resolved.def.supportsInlineEdit?.(resolved.ctx) !== false;
          const isEditing = editing?.rowId === Number(row.id) && editing?.fieldCode === fieldCode;
          if (isEditing) {
            return resolved.def.renderEditor({
              ctx: resolved.ctx,
              row,
              value,
              displayValue,
              onCommit: (next) => void commitInlineEdit(row, resolved, next),
              onCancel: () => setEditing(null),
            });
          }
          const cell = resolved.def.renderCell({ ctx: resolved.ctx, row, value, displayValue });
          if (!inline || !resolved.filterable) return cell;
          return (
            // Native title avoids antd Tooltip's DOM wrapper which can intercept
            // click events in headless Chrome (Playwright).
            <span
              className="zlc-cell-editable"
              role="button"
              tabIndex={0}
              aria-label={`编辑 ${resolved.ctx.field.fieldName || fieldCode}`}
              title="单击编辑"
              onClick={() => setEditing({ rowId: Number(row.id), fieldCode })}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  setEditing({ rowId: Number(row.id), fieldCode });
                }
              }}
            >
              {cell}
            </span>
          );
        },
      };
    });

    list.push({
      title: '操作',
      dataIndex: '__ops',
      key: '__ops',
      width: 96,
      fixed: 'right' as const,
      render: (_unknown: unknown, row: LcRow) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => onOpenRecord(Number(row.id))}>
            详情
          </Button>
          <Button type="link" size="small" onClick={() => onEditRecord(Number(row.id))}>
            编辑
          </Button>
        </Space>
      ),
    });
    return list;
  }, [columns.visible, editing, commitInlineEdit, onOpenRecord, onEditRecord]);

  const exportCsv = useCallback(async () => {
    const hide = message.loading('正在导出…', 0);
    try {
      const { rows: all, truncated } = await fetchAllForExport({
        entityCode,
        appCode,
        tenantCode,
        columns: columns.visible.map((column) => column.resolved),
        conditions,
        conjunction,
        sorts: state.sorts,
      });
      downloadText(
        `${entityCode}-${new Date().toISOString().slice(0, 10)}.csv`,
        buildCsv(
          columns.visible.map((column) => column.resolved),
          all,
        ),
      );
      if (truncated) message.warning(`仅导出前 ${all.length} 行（命中 ${total} 行）`);
      else message.success(`已导出 ${all.length} 行`);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '导出失败');
    } finally {
      hide();
    }
  }, [entityCode, appCode, tenantCode, columns.visible, conditions, conjunction, state.sorts, total]);

  const bulkDelete = useCallback(async () => {
    const ids = selected;
    if (!ids.length) return;
    try {
      const res = await deleteBatch(entityCode, ids, { appCode, tenantCode });
      if (res.applied) {
        // 只信服务端数出来的条数：以前这里报的是"我发了几个请求"，两者不等价。
        message.success(`已删除 ${res.deletedCount} 条`);
        setSelected([]);
      } else {
        // 整批预检没过 = 一条都没删。这时保留勾选，用户修好问题可以直接重来。
        const first = res.errors[0];
        message.error(
          `${res.message ?? '一条都没删'}` + (first ? `：${first.message}` : ''),
        );
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '批量删除失败');
    }
    void load();
  }, [entityCode, selected, appCode, tenantCode, load]);

  const saveView = useCallback(async () => {
    const name = saveName.trim();
    if (!name) {
      message.warning('请输入视图名称');
      return;
    }
    try {
      await createViewConfig({
        appCode,
        entityCode,
        viewType: 'LIST',
        config: withViewName({ ...state, conditions, conjunction }, name),
        tenantCode,
      });
      setSaveOpen(false);
      setSaveName('');
      message.success('视图已保存');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  }, [appCode, entityCode, tenantCode, state, conditions, conjunction, saveName]);

  const viewOptions = useMemo(
    () => namedViews(views, entityCode, 'LIST').map((view) => ({ value: view.id, label: viewLabel(view) })),
    [views, entityCode],
  );

  return (
    <div style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
      <Space wrap size={8} style={{ justifyContent: 'space-between', width: '100%' }}>
        <Space wrap size={8}>
          <Button type="primary" onClick={onCreateRecord}>
            新建记录
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => setImportOpen(true)}>
            导入 CSV
          </Button>
          <Tooltip title="查看数据变更历史，并可一键撤销上一次改动">
            <Button icon={<HistoryOutlined />} onClick={() => setHistoryOpen(true)}>
              变更历史
            </Button>
          </Tooltip>
          <Button icon={<SettingOutlined />} onClick={() => setColumnDrawer(true)}>
            列设置
          </Button>
          <Tooltip title="页脚统计行：按当前筛选命中的全部行做服务端聚合，不是当前这一页">
            <Button
              type={showStats ? 'primary' : 'default'}
              ghost={showStats}
              icon={<FundOutlined />}
              onClick={() => setShowStats((prev) => !prev)}
            >
              统计
            </Button>
          </Tooltip>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'csv', label: '导出当前视图 CSV' },
                { key: 'import', label: '导入 CSV' },
                { key: 'refresh', label: '重新载入' },
              ],
              onClick: ({ key }) => {
                if (key === 'csv') void exportCsv();
                if (key === 'import') setImportOpen(true);
                if (key === 'refresh') void load();
              },
            }}
          >
            <Button icon={<DownloadOutlined />}>
              更多 <DownOutlined />
            </Button>
          </Dropdown>
          {selected.length > 0 ? (
            <Popconfirm
              title={`确认删除选中的 ${selected.length} 条记录？`}
              onConfirm={() => void bulkDelete()}
            >
              <Button danger icon={<DeleteOutlined />}>
                删除 {selected.length} 条
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
        <Space size={6}>
          <Text type="secondary">视图</Text>
          <Select
            style={{ minWidth: 168 }}
            placeholder="默认视图"
            allowClear
            value={activeViewId}
            options={viewOptions}
            onChange={(id) => {
              const view = views.find((item) => item.id === id);
              const restored = viewState(view);
              if (restored) {
        setState({
          ...EMPTY_STATE,
          columnMeta: restored.columnMeta,
          sorts: restored.sorts,
          stats: restored.stats,
        });
        onFilterChange({ conditions: restored.conditions, conjunction: restored.conjunction });
      }
            }}
          />
          <Tooltip title="保存为视图">
            <Button icon={<SaveOutlined />} onClick={() => setSaveOpen(true)} />
          </Tooltip>
          <Tooltip title="重新载入">
            <Button icon={<ReloadOutlined />} onClick={() => void load()} />
          </Tooltip>
        </Space>
      </Space>

      <FilterBar
        fields={resolvedFields}
        value={{ conditions, conjunction }}
        onChange={(next: FilterState) =>
          onFilterChange({
            conditions: pruneStaleConditions(next.conditions, allowed),
            conjunction: next.conjunction,
          })
        }
      />

      <StateBlock
        isLoading={loading && rows.length === 0}
        isError={Boolean(loadError)}
        error={loadError}
        onRetry={() => void load()}
        isEmpty={!loading && rows.length === 0}
        empty={
          <div style={{ padding: 24, textAlign: 'center' }}>
            <Text type="secondary">
              {conditions.length
                ? '没有符合条件的记录，试试放宽筛选条件'
                : '还没有记录，点击「新建记录」开始录入'}
            </Text>
          </div>
        }
      >
        <Table<LcRow>
          size="small"
          rowKey={(row) => String(row.id)}
          columns={tableColumns}
          dataSource={rows}
          loading={loading}
          scroll={{ x: 'max-content' }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50, 100, 200],
            showTotal: (count) => `共 ${count} 条`,
          }}
          onChange={(pager, _filters, sorter) => {
            setPage(pager.current ?? 1);
            setPageSize(pager.pageSize ?? 20);
            const field = Array.isArray(sorter) ? undefined : sorter.field;
            const order = Array.isArray(sorter) ? undefined : sorter.order;
            const sorts = field && order
              ? [{ fieldCode: String(field), dir: order === 'ascend' ? ('asc' as const) : ('desc' as const) }]
              : [];
            patchState({ sorts: pruneStaleSorts(sorts, new Set([...allowed, 'id', 'create_time', 'update_time'])) });
          }}
          rowSelection={{
            selectedRowKeys: selected.map(String),
            onChange: (keys) => setSelected(keys.map(Number)),
          }}
          summary={
            showStats
              ? () => (
                  <Table.Summary fixed>
                    <Table.Summary.Row style={{ background: '#fafbfd' }}>
                      {/* 勾选框只占 1 格；之前按 2 格 + slice(2) 写，会把所有统计整体左移一列 */}
                      <Table.Summary.Cell index={0}>
                        <Space size={6}>
                          <Text strong style={{ fontSize: 12 }}>
                            共 {total} 条
                          </Text>
                          {stats.isFetching ? <Text type="secondary" style={{ fontSize: 11 }}>统计中…</Text> : null}
                        </Space>
                      </Table.Summary.Cell>
                      {columns.visible.map((column, offset) => {
                        const code = column.resolved.ctx.field.fieldCode;
                        const isNumeric = column.resolved.ctx.descriptor.cellValueType === 'Number';
                        const fn = state.stats[code];
                        // 数值列给全套，文本/字典/日期列至少能去重计数与非空计数
                        const options = (isNumeric ? STAT_FUNCTIONS : UNIVERSAL_STATS).map((item) => ({
                          value: item,
                          label: STAT_LABEL[item],
                        }));
                        return (
                          <Table.Summary.Cell key={code} index={offset + 1}>
                            {options.length === 0 ? (
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                —
                              </Text>
                            ) : (
                              <Space size={4} wrap>
                                <Select
                                  size="small"
                                  variant="borderless"
                                  style={{ width: 74, fontSize: 11 }}
                                  placeholder="统计"
                                  allowClear
                                  value={fn}
                                  options={options}
                                  onChange={(value) => setStat(code, value as StatFunction | undefined)}
                                />
                                {fn && statsRow ? (
                                  <Text strong style={{ fontSize: 12 }}>
                                    {formatStat(statsRow[`${fn.toLowerCase()}_${code}`])}
                                    {/* 填充率只是个除法，但服务端不做，避免"分母是谁"含糊 */}
                                    {fn === 'FILLED' && total > 0
                                      ? ` (${((Number(statsRow[`filled_${code}`] ?? 0) / total) * 100).toFixed(0)}%)`
                                      : null}
                                  </Text>
                                ) : null}
                              </Space>
                            )}
                          </Table.Summary.Cell>
                        );
                      })}
                      <Table.Summary.Cell index={columns.visible.length + 1} />
                    </Table.Summary.Row>
                  </Table.Summary>
                )
              : undefined
          }
        />
      </StateBlock>

      <ColumnManagerDrawer
        open={columnDrawer}
        onClose={() => setColumnDrawer(false)}
        resolvedFields={resolvedFields}
        columnMeta={state.columnMeta}
        onChange={(next) => patchState({ columnMeta: next })}
      />

      <UndoHistoryDrawer
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        appCode={appCode}
        entityCode={entityCode}
        tenantCode={tenantCode}
        resolvedFields={resolvedFields}
        onUndone={() => void load()}
      />

      <ImportDialog
        open={importOpen}
        onClose={() => setImportOpen(false)}
        entity={entity}
        resolvedFields={resolvedFields}
        appCode={appCode}
        tenantCode={tenantCode}
        onImported={() => void load()}
      />

      <Modal
        title="保存为视图"
        open={saveOpen}
        onOk={() => void saveView()}
        onCancel={() => setSaveOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <Input
          autoFocus
          value={saveName}
          placeholder="例如：VIP 客户（按余额倒序）"
          onChange={(event) => setSaveName(event.target.value)}
          onPressEnter={() => void saveView()}
        />
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          将保存当前 {columns.visible.length} 个可见列、{conditions.length} 条筛选条件与排序，同应用成员可见。
        </Text>
      </Modal>
    </div>
  );
}
