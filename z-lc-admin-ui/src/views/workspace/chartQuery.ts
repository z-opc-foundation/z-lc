import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { Conjunction, QueryCondition } from '@/api/types';
import {
  aggregateRecords,
  type AggregateRequest,
  type AggregateRow,
} from '@/api/runtime';
import type { ResolvedField } from '@/fields';
import {
  METRIC_LABELS,
  buildSeries,
  chartGroupCandidates,
  normaliseConfig,
  type ChartConfig,
  type Series,
} from './chartModel';

/**
 * 「一份图表配置 -> 一个服务端聚合请求 -> 一条可绘制序列」的唯一实现。
 *
 * 单图视图和仪表盘必须走同一条链路：命名视图存的就是这份配置，如果仪表盘另写一套
 * 口径（topN 折叠、指标卡不带分组、日期列默认按月分桶），同一张图在两个地方会画出
 * 两个数 —— 那是最难被发现也最难解释的一类错误。
 */

function isTemporal(field?: ResolvedField): boolean {
  return field?.ctx.descriptor.cellValueType === 'DateTime';
}

function isNumeric(field?: ResolvedField): boolean {
  return field?.ctx.descriptor.cellValueType === 'Number';
}

export interface ChartQueryArgs {
  appCode: string;
  tenantCode: string;
  entityCode: string;
  resolvedFields: ResolvedField[];
  /** 未归一化的原始配置：分组列可能已经被删掉了，钩子会把它回落成真实可分组的列。 */
  config: ChartConfig;
  conditions: QueryCondition[];
  conjunction: Conjunction;
}

export interface ChartQuery {
  /** 分组轴候选（图表没有可分组字段时为 []，页面要据此给提示而不是画空图）。 */
  candidates: ResolvedField[];
  numericFields: ResolvedField[];
  /** 已把 groupField / metricField 落到真实字段上、并按画法裁剪过的配置。 */
  query: ChartConfig;
  /** 真正发给后端的形状，包含分桶门控与 limit。 */
  request: AggregateRequest;
  /** 分组列是不是日期列：决定 timeGroup 能不能下发。 */
  timeEnabled: boolean;
  fieldLabel: string;
  metricName: string;
  /** 缺数值字段时不发请求 —— 发出去只会被后端拒，然后页面上是个错误而不是"请先选指标"。 */
  canRun: boolean;
  /** 非 COUNT 的指标函数必须有度量列。 */
  needsMetricField: boolean;
  data: ReturnType<typeof useQuery<AggregateRow[]>>;
  series: Series;
}

export function useChartQuery({
  appCode,
  tenantCode,
  entityCode,
  resolvedFields,
  config,
  conditions,
  conjunction,
}: ChartQueryArgs): ChartQuery {
  const candidates = useMemo(() => chartGroupCandidates(resolvedFields), [resolvedFields]);
  const numericFields = useMemo(() => resolvedFields.filter((field) => isNumeric(field)), [resolvedFields]);

  // 默认分组轴不能取 schema 第一个字段（那通常是名称列，会画出一张每组只有一条记录的图）
  const groupField = candidates.find((field) => field.ctx.field.fieldCode === config.groupField)
    ? config.groupField
    : candidates[0]?.ctx.field.fieldCode;
  const groupResolved = candidates.find((field) => field.ctx.field.fieldCode === groupField);
  const timeEnabled = isTemporal(groupResolved);

  const metricField = config.metricField && numericFields.some((f) => f.ctx.field.fieldCode === config.metricField)
    ? config.metricField
    : numericFields[0]?.ctx.field.fieldCode;
  const needsMetricField = config.metricFn !== 'COUNT';

  const query = useMemo(() => normaliseConfig({ ...config, groupField, metricField }), [
    config,
    groupField,
    metricField,
  ]);

  const fieldLabel = groupResolved?.ctx.field.fieldName || groupField || '全部记录';
  const metricName = needsMetricField && metricField
    ? `${numericFields.find((f) => f.ctx.field.fieldCode === metricField)?.ctx.field.fieldName ?? metricField} ${METRIC_LABELS[query.metricFn]}`
    : METRIC_LABELS.COUNT;

  const aggregateQuery = useMemo<ChartConfig>(() => {
    // 指标卡只问一个数：带分组会让它拿到多行却只显示第一行，那不是用户要的
    if (query.kind === 'number') return { ...query, groupField: undefined, timeGroup: undefined };
    // timeGroup 只在分组列真是日期列时下发，否则后端会按"非日期列分桶"拒掉整个请求。
    // 日期列默认按月：下拉框显示"按月"而请求里不带 timeGroup 的话，实际会按"精确日期"
    // 分组，图上就会写着按月却画出每一天。
    return { ...query, timeGroup: timeEnabled ? query.timeGroup ?? 'MONTH' : undefined };
  }, [query, timeEnabled]);

  const request = useMemo<AggregateRequest>(() => {
    const aggregations =
      needsMetricField && metricField ? { [metricField]: [aggregateQuery.metricFn] } : undefined;
    return {
      groupField: aggregateQuery.groupField,
      timeGroup: aggregateQuery.timeGroup,
      aggregations,
      conditions,
      conjunction,
      // 前端还要按 topN 折叠，所以多取一些回来；后端自己封顶 500
      limit: Math.max(40, Math.min(200, (aggregateQuery.topN || 12) * 6)),
    };
  }, [aggregateQuery, needsMetricField, metricField, conditions, conjunction]);

  const canRun = candidates.length > 0 && (!needsMetricField || Boolean(metricField));

  const data = useQuery<AggregateRow[]>({
    queryKey: ['chart', appCode, entityCode, tenantCode, request],
    queryFn: () => aggregateRecords(entityCode, { appCode, tenantCode }, request),
    enabled: canRun,
  });

  const series = useMemo(() => buildSeries(data.data ?? [], aggregateQuery), [data.data, aggregateQuery]);

  return {
    candidates,
    numericFields,
    query,
    request,
    timeEnabled,
    fieldLabel,
    metricName,
    canRun,
    needsMetricField,
    data,
    series,
  };
}
