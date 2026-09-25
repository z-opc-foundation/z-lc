import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Input, InputNumber, Modal, Select, Space, Typography, message } from 'antd';
import { ExportOutlined, SaveOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { createViewConfig, newViewConfigDraft, parseViewConfig } from '@/api/viewConfig';
import { shapeRecords, type ShapeRequest } from '@/api/runtime';
import type { Conjunction, EntityDefDTO, QueryCondition, ViewConfigDTO } from '@/api/types';
import type { ResolvedField } from '@/fields';
import { StateBlock } from '@/components/StateBlock';
import {
  DEFAULT_PIVOT_CONFIG,
  PIVOT_METRIC_FNS,
  PIVOT_METRIC_LABELS,
  buildPivotMatrix,
  buildPivotProgram,
  normalisePivotConfig,
  pivotDimensionCandidates,
  pivotFieldLabel,
  pivotMeasureColumn,
  pivotMeasureFields,
  pivotColumnLabel,
  type PivotConfig,
  type PivotMetricFn,
} from './pivotModel';

const { Text } = Typography;

/**
 * 交叉表（透视）视图：行维度 x 列维度 x 一个度量。
 *
 * 它存在的意义是把 `/runtime/shape` 的 `pivot` 步骤接到界面上 —— 这个后端能力此前
 * 前端零引用，也就是对用户不存在。行转列本身在库里做（一次请求），这里只负责
 * 把回来的矩阵读成一张表，并把"合计"这件事限制在有数学意义的指标上。
 */

function storageKey(appCode: string, entityCode: string): string {
  return `zlc:state:${appCode}:${entityCode}:PIVOT`;
}

function readConfig(appCode: string, entityCode: string): PivotConfig {
  try {
    const raw = window.localStorage.getItem(storageKey(appCode, entityCode));
    if (!raw) return DEFAULT_PIVOT_CONFIG;
    const parsed = JSON.parse(raw) as Partial<PivotConfig>;
    return {
      rowField: parsed.rowField,
      colField: parsed.colField,
      metricFn: PIVOT_METRIC_FNS.includes(parsed.metricFn as PivotMetricFn)
        ? (parsed.metricFn as PivotMetricFn)
        : DEFAULT_PIVOT_CONFIG.metricFn,
      metricField: parsed.metricField,
      maxRows: typeof parsed.maxRows === 'number' ? parsed.maxRows : DEFAULT_PIVOT_CONFIG.maxRows,
    };
  } catch {
    // 手改过的 localStorage 不该让视图打不开
    return DEFAULT_PIVOT_CONFIG;
  }
}

function pivotSnapshot(view: ViewConfigDTO): PivotConfig | null {
  const parsed = parseViewConfig<Partial<PivotConfig> | null>(view.config, null);
  if (!parsed) return null;
  return {
    rowField: parsed.rowField,
    colField: parsed.colField,
    metricFn: PIVOT_METRIC_FNS.includes(parsed.metricFn as PivotMetricFn)
      ? (parsed.metricFn as PivotMetricFn)
      : DEFAULT_PIVOT_CONFIG.metricFn,
    metricField: parsed.metricField,
    maxRows: typeof parsed.maxRows === 'number' ? parsed.maxRows : DEFAULT_PIVOT_CONFIG.maxRows,
  };
}

const sameConfig = (a: PivotConfig, b: PivotConfig): boolean =>
  a.rowField === b.rowField
  && a.colField === b.colField
  && a.metricFn === b.metricFn
  && a.metricField === b.metricField
  && a.maxRows === b.maxRows;

const numberFormat = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 });

interface PivotViewProps {
  entity: EntityDefDTO;
  resolvedFields: ResolvedField[];
  views: ViewConfigDTO[];
  appCode: string;
  tenantCode: string;
  conditions: QueryCondition[];
  conjunction: Conjunction;
}

export function PivotView({
  entity,
  resolvedFields,
  views,
  appCode,
  tenantCode,
  conditions,
  conjunction,
}: PivotViewProps) {
  const entityCode = entity.entityCode ?? '';
  const [config, setConfig] = useState<PivotConfig>(() => readConfig(appCode, entityCode));
  const [saveOpen, setSaveOpen] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    try {
      window.localStorage.setItem(storageKey(appCode, entityCode), JSON.stringify(config));
    } catch {
      // 隐私模式：交叉表还能看，不该整页崩
    }
  }, [appCode, entityCode, config]);

  const dimensions = useMemo(() => pivotDimensionCandidates(resolvedFields), [resolvedFields]);
  const measures = useMemo(() => pivotMeasureFields(resolvedFields), [resolvedFields]);
  const query = useMemo(
    () => normalisePivotConfig(config, dimensions, measures),
    [config, dimensions, measures],
  );

  // 缺列维度就不发请求：只给行维度时后端会退化成单维聚合，页面会画出一张
  // "每行一个格子"的表，那不是交叉表，用户看不出它错了。
  const canRun = Boolean(query.rowField)
    && Boolean(query.colField)
    && (query.metricFn === 'COUNT' || Boolean(query.metricField));

  const request = useMemo<ShapeRequest>(() => ({
    // 顺序即语义：第一个是行维度，第二个是列维度（后端按 group_key / group_key_2 出列）
    groupFields: [query.rowField ?? '', query.colField ?? ''],
    aggregations: query.metricFn === 'COUNT' ? {} : { [String(query.metricField)]: [query.metricFn] },
    conditions,
    conjunction,
    // 聚合行数是两个维度乘出来的，limit 太小会静默切掉列（用户看到的是一张"恰好没有数据"的表）
    limit: Math.min(500, Math.max(100, query.maxRows * 12)),
    shape: buildPivotProgram(pivotMeasureColumn(query)),
  }), [query, conditions, conjunction]);

  const data = useQuery<unknown>({
    queryKey: ['pivot', appCode, entityCode, tenantCode, request],
    queryFn: () => shapeRecords(entityCode, { appCode, tenantCode }, request),
    enabled: canRun,
  });

  // 结构不合预期是一次"读到了但读不懂"，必须显示成错误而不是空表
  const parsed = useMemo(() => {
    if (data.data === undefined) return null;
    try {
      return { matrix: buildPivotMatrix(data.data, query), error: null as Error | null };
    } catch (err) {
      return { matrix: null, error: err instanceof Error ? err : new Error(String(err)) };
    }
  }, [data.data, query]);

  const matrix = parsed?.matrix ?? null;
  const readError = data.error ?? parsed?.error ?? null;

  const pivotViews = useMemo(
    () => (views ?? []).filter(
      (view) => view.entityCode === entityCode && (view.viewType ?? '').toUpperCase() === 'PIVOT',
    ),
    [views, entityCode],
  );
  const activeViewId = useMemo(
    () => pivotViews.find((view) => {
      const snapshot = pivotSnapshot(view);
      return !!snapshot && sameConfig(query, snapshot);
    })?.id,
    [pivotViews, query],
  );

  const saveView = useCallback(async () => {
    const name = saveName.trim();
    if (!name) {
      message.warning('请输入交叉表视图名称');
      return;
    }
    setSaving(true);
    try {
      await createViewConfig(newViewConfigDraft({
        appCode,
        entityCode,
        viewType: 'PIVOT',
        // 存落到真实字段之后的 query，不存原始 config
        config: { ...query, name },
        tenantCode,
      }));
      setSaveOpen(false);
      setSaveName('');
      message.success('交叉表视图已保存，同应用成员可见');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    } finally {
      setSaving(false);
    }
  }, [appCode, entityCode, tenantCode, query, saveName]);

  const dimensionOptions = dimensions.map((field) => ({
    value: field.ctx.field.fieldCode,
    label: field.ctx.field.fieldName || field.ctx.field.fieldCode,
  }));
  const measureOptions = measures.map((field) => ({
    value: field.ctx.field.fieldCode,
    label: field.ctx.field.fieldName || field.ctx.field.fieldCode,
  }));

  const cell = (value: number | null): string => (value === null ? '·' : numberFormat.format(value));

  return (
    <div className="zlc-pivot-view" style={{ padding: 16 }}>
      <Space wrap size={8} style={{ marginBottom: 12 }} className="zlc-pivot-toolbar">
        <Text type="secondary">行维度</Text>
        <Select
          className="zlc-pivot-row"
          style={{ minWidth: 140 }}
          value={query.rowField}
          options={dimensionOptions}
          placeholder="选一行一个的列"
          onChange={(value) => setConfig({ ...config, rowField: value })}
        />
        <Text type="secondary">列维度</Text>
        <Select
          className="zlc-pivot-col"
          style={{ minWidth: 140 }}
          allowClear
          value={query.colField}
          options={dimensionOptions.filter((option) => option.value !== query.rowField)}
          placeholder="选一列一个的列"
          onChange={(value) => setConfig({ ...config, colField: value })}
        />
        <Text type="secondary">指标</Text>
        <Select
          className="zlc-pivot-fn"
          style={{ minWidth: 110 }}
          value={query.metricFn}
          options={PIVOT_METRIC_FNS.map((fn) => ({ value: fn, label: PIVOT_METRIC_LABELS[fn] }))}
          onChange={(value) => setConfig({ ...config, metricFn: value })}
        />
        {query.metricFn === 'COUNT' ? null : (
          <>
            <Text type="secondary">度量</Text>
            <Select
              className="zlc-pivot-field"
              style={{ minWidth: 140 }}
              value={query.metricField}
              options={measureOptions}
              placeholder="选数值列"
              onChange={(value) => setConfig({ ...config, metricField: value })}
            />
          </>
        )}
        <Text type="secondary">显示行数</Text>
        <InputNumber
          className="zlc-pivot-rows"
          min={1}
          max={100}
          value={query.maxRows}
          onChange={(value) => setConfig({ ...config, maxRows: Number(value ?? DEFAULT_PIVOT_CONFIG.maxRows) })}
        />
        <Select
          className="zlc-pivot-named"
          style={{ minWidth: 160 }}
          allowClear
          placeholder="命名视图"
          value={activeViewId}
          options={pivotViews.map((view) => ({
            value: view.id,
            label: parseViewConfig<{ name?: string }>(view.config, {})?.name || `视图 #${view.id ?? '?'}`,
          }))}
          onChange={(id) => {
            const view = pivotViews.find((item) => item.id === id);
            const snapshot = view ? pivotSnapshot(view) : null;
            if (snapshot) setConfig(snapshot);
          }}
        />
        <Button icon={<SaveOutlined />} onClick={() => setSaveOpen(true)}>保存为视图</Button>
      </Space>

      <div className="zlc-pivot-caption" style={{ marginBottom: 8 }}>
        <Text type="secondary">
          {`${pivotFieldLabel(resolvedFields, query.rowField)} × ${pivotFieldLabel(resolvedFields, query.colField)}`
            + ` · ${query.metricFn === 'COUNT' ? PIVOT_METRIC_LABELS.COUNT : `${pivotFieldLabel(resolvedFields, query.metricField)} ${PIVOT_METRIC_LABELS[query.metricFn]}`}`}
        </Text>
        {matrix && !matrix.totalsMeaningful ? (
          <Text type="warning" className="zlc-pivot-no-total" style={{ marginLeft: 8 }}>
            平均数不横向相加：合计需要按各格记录数加权，透视结果里没有这个权重，所以这里不出合计。
          </Text>
        ) : null}
        {!canRun ? (
          <Text type="danger" className="zlc-pivot-incomplete" style={{ marginLeft: 8 }}>
            {query.metricFn === 'COUNT' ? '还没选列维度' : '还缺列维度或数值度量'}
          </Text>
        ) : null}
      </div>

      <StateBlock
        isLoading={data.isLoading && canRun}
        isError={canRun && Boolean(readError)}
        error={readError}
        isEmpty={canRun && !data.isFetching && !readError && (!matrix || matrix.rows.length === 0)}
        empty={<Text type="secondary">当前筛选条件下没有可透视的记录。</Text>}
        onRetry={() => void data.refetch()}
      >
        <table className="zlc-pivot-table">
          <thead>
            <tr>
              <th className="zlc-pivot-rowhead">{pivotFieldLabel(resolvedFields, query.rowField)}</th>
              {(matrix?.colKeys ?? []).map((column, index) => (
                <th key={column} className="zlc-pivot-colhead" data-col-key={column}>
                  {matrix?.colLabels[index] ?? pivotColumnLabel(column)}
                </th>
              ))}
              {matrix?.totalsMeaningful ? <th className="zlc-pivot-totalhead">合计</th> : null}
            </tr>
          </thead>
          <tbody>
            {(matrix?.rows ?? []).map((row) => (
              <tr key={row.key || '(空)'} data-row-key={row.key}>
                <th className="zlc-pivot-rowhead">{row.label}</th>
                {row.cells.map((value, index) => (
                  <td
                    key={matrix?.colKeys[index] ?? index}
                    className={value === null ? 'zlc-pivot-cell zlc-pivot-empty' : 'zlc-pivot-cell'}
                  >
                    {cell(value)}
                  </td>
                ))}
                {matrix?.totalsMeaningful ? <td className="zlc-pivot-rowtotal">{cell(row.total)}</td> : null}
              </tr>
            ))}
            {matrix?.rest ? (
              <tr className="zlc-pivot-rest" data-row-key="">
                <th className="zlc-pivot-rowhead">{matrix.rest.label}</th>
                {matrix.rest.cells.map((value, index) => (
                  <td key={matrix.colKeys[index] ?? index} className="zlc-pivot-cell">{cell(value)}</td>
                ))}
                {matrix.totalsMeaningful ? <td className="zlc-pivot-rowtotal">{cell(matrix.rest.total)}</td> : null}
              </tr>
            ) : null}
          </tbody>
          {matrix?.totalsMeaningful ? (
            <tfoot>
              <tr className="zlc-pivot-coltotals">
                <th className="zlc-pivot-rowhead">合计</th>
                {matrix.colTotals.map((value, index) => (
                  <td key={matrix.colKeys[index] ?? index} className="zlc-pivot-cell">{cell(value)}</td>
                ))}
                <td className="zlc-pivot-grandtotal">{cell(matrix.grandTotal)}</td>
              </tr>
            </tfoot>
          ) : null}
        </table>
        {matrix && matrix.rows.length > 0 ? (
          <div className="zlc-pivot-foot" style={{ marginTop: 8 }}>
            <Text type="secondary">
              {`${matrix.rowCount} 行 × ${matrix.colKeys.length} 列 · ${matrix.cellCount} 格`
                + (matrix.rest ? ` · 已折叠 ${matrix.rest.label.replace(/^其余 /, '')}` : '')
                + ' · “·” 表示这个组合下一条记录都没有（不是 0）'}
            </Text>
            <Button
              size="small"
              icon={<ExportOutlined />}
              onClick={() => {
                const header = [pivotFieldLabel(resolvedFields, query.rowField), ...matrix.colKeys];
                const totalHead = matrix.totalsMeaningful ? ['合计'] : [];
                const lines = [
                  [...header, ...totalHead].join('\t'),
                  ...matrix.rows.map((row) => [
                    row.label,
                    ...row.cells.map((value) => (value === null ? '' : String(value))),
                    ...(matrix.totalsMeaningful ? [row.total === null ? '' : String(row.total)] : []),
                  ].join('\t')),
                ];
                void navigator.clipboard?.writeText(lines.join('\n')).then(
                  () => message.success('已复制为制表符分隔，可直接贴进表格'),
                  () => message.error('复制失败，浏览器没有给剪贴板权限'),
                );
              }}
            >
              复制成表格
            </Button>
          </div>
        ) : null}
      </StateBlock>

      <Modal
        title="保存为命名交叉表"
        open={saveOpen}
        confirmLoading={saving}
        onOk={() => void saveView()}
        onCancel={() => setSaveOpen(false)}
        okText="保存"
        cancelText="取消"
      >
        <Input
          autoFocus
          value={saveName}
          placeholder="例如：客户 x 状态的金额分布"
          onChange={(event) => setSaveName(event.target.value)}
          onPressEnter={() => void saveView()}
        />
        <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
          保存行维度、列维度、指标、度量与显示行数；存在服务端，同应用成员都能看到。
        </Text>
      </Modal>
    </div>
  );
}
