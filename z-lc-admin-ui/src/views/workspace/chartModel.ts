import { parseViewConfig } from '@/api/viewConfig';
import type { Conjunction, QueryCondition, ViewConfigDTO } from '@/api/types';
import type { AggregateRow, TimeGroup } from '@/api/runtime';
import type { ResolvedField } from '@/fields';
import { normalise } from './viewConfigModel';
import type { WorkspaceViewState } from './viewConfigModel';

export type { TimeGroup };

export const CHART_KINDS = ['bar', 'line', 'pie', 'number'] as const;
export type ChartKind = (typeof CHART_KINDS)[number];

export const CHART_KIND_LABELS: Record<ChartKind, string> = {
  bar: '柱状图',
  line: '折线图',
  pie: '饼图',
  number: '指标卡',
};

export const TIME_GROUPS: readonly TimeGroup[] = ['DAY', 'MONTH', 'YEAR'];

export const TIME_GROUP_LABELS: Record<TimeGroup, string> = {
  DAY: '按天',
  MONTH: '按月',
  YEAR: '按年',
};

/** 与后端 /runtime/aggregate 的白名单一致，不多不少。 */
export const METRIC_FUNCTIONS = ['COUNT', 'SUM', 'AVG', 'MIN', 'MAX'] as const;
export type MetricFunction = (typeof METRIC_FUNCTIONS)[number];

export const METRIC_LABELS: Record<MetricFunction, string> = {
  COUNT: '记录数',
  SUM: '合计',
  AVG: '平均',
  MIN: '最小',
  MAX: '最大',
};

export interface ChartConfig {
  kind: ChartKind;
  /** 分组字段的 fieldCode；留空表示只出一根总量柱。 */
  groupField?: string;
  /** 分组字段是日期列时的分桶粒度；其他情况忽略。 */
  timeGroup?: TimeGroup;
  metricFn: MetricFunction;
  /** COUNT 之外必填。 */
  metricField?: string;
  /** 最多画几组，超出折叠进「其他」。 */
  topN: number;
}

export const DEFAULT_CONFIG: ChartConfig = {
  kind: 'bar',
  timeGroup: 'MONTH',
  metricFn: 'COUNT',
  topN: 12,
};

export interface Point {
  label: string;
  /** null = 这一组没有值（聚合结果为 NULL），不是"等于 0"。 */
  value: number | null;
  count: number;
}

/** 后端聚合列名：`SUM(amount)` -> `sum_amount`，行数永远是 `group_count`。 */
export function metricColumn(cfg: ChartConfig): string {
  if (cfg.metricFn === 'COUNT') return 'group_count';
  return `${cfg.metricFn.toLowerCase()}_${cfg.metricField}`;
}

/** 时间桶标签：后端只回数字段（不用各库方言不同的日期格式化函数），拼字符串归前端。 */
export function bucketLabel(row: AggregateRow, timeGroup?: TimeGroup): string | undefined {
  if (!timeGroup) return undefined;
  const year = row.bucket_year;
  if (year === null || year === undefined) return undefined;
  const month = row.bucket_month;
  const day = row.bucket_day;
  const pad = (n: number) => String(n).padStart(2, '0');
  if (timeGroup === 'YEAR') return String(year);
  if (month === null || month === undefined) return String(year);
  if (timeGroup === 'MONTH') return `${year}-${pad(month)}`;
  if (day === null || day === undefined) return `${year}-${pad(month)}`;
  return `${year}-${pad(month)}-${pad(day)}`;
}

/** 空值分组要显式说出来：图上冒出一个没人认领的柱子，比标成「未填写」更容易被误读。 */
export function categoryLabel(row: AggregateRow): string {
  const label = row.group_label;
  if (label !== null && label !== undefined && String(label) !== '') return String(label);
  const key = row.group_key;
  if (key === null || key === undefined || String(key) === '') return '（未填写）';
  return String(key);
}

function toNumber(raw: unknown): number | null {
  if (raw === null || raw === undefined || raw === '') return null;
  const n = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(n) ? n : null;
}

/** 分组键：时间桶优先，其次分类值；两者都缺（无 groupField 的总量查询）归到"总计"。 */
export function rowKey(row: AggregateRow, cfg: ChartConfig): string {
  const bucket = bucketLabel(row, cfg.timeGroup);
  if (bucket) return bucket;
  if (cfg.groupField) return categoryLabel(row);
  return '总计';
}

export interface Series {
  points: Point[];
  /** 被 topN 折叠掉的组数。 */
  folded: number;
  isTime: boolean;
}

/**
 * 聚合行 -> 可绘制序列。
 * 分类组按值倒序（柱状图的惯例），时间组保持时间正序 —— 把时间轴排成倒序
 * 会把折线图画成锯齿，这是肉眼最容易发现错误的地方。
 */
export function buildSeries(rows: AggregateRow[], cfg: ChartConfig): Series {
  const column = metricColumn(cfg);
  const isTime = Boolean(cfg.groupField && cfg.timeGroup);
  const points: Point[] = rows.map((row) => ({
    label: rowKey(row, cfg),
    value: toNumber(row[column]),
    count: toNumber(row.group_count) ?? 0,
  }));

  if (isTime) {
    // 时间轴前端再排一次：后端有 ORDER BY，但"倒序/乱序的折线"是静默错误，
    // 值得用一条本地不变量兜住。桶标签是 YYYY-MM[-DD] 定宽格式，字典序即时间序
    // （所以补零不是美观问题，是排序正确性问题）。
    points.sort((a, b) => a.label.localeCompare(b.label));
  } else if (cfg.kind !== 'number') {
    points.sort((a, b) => (b.value ?? -1) - (a.value ?? -1));
  }

  let folded = 0;
  let kept = points;
  if (cfg.topN > 0 && points.length > cfg.topN) {
    if (isTime) {
      // 时间轴折叠要留尾：最近的那段才是要看的，留头会把"这个月怎么样"藏起来
      kept = points.slice(points.length - cfg.topN);
      folded = points.length - cfg.topN;
    } else {
      const rest = points.slice(cfg.topN);
      const restSum = rest.reduce((acc, p) => acc + (p.value ?? 0), 0);
      kept = [...points.slice(0, cfg.topN), { label: `其他 ${rest.length} 组`, value: restSum, count: restSum }];
      folded = rest.length;
    }
  }
  return { points: kept, folded, isTime };
}

/**
 * "好看的"刻度：步长只取 1/2/5 × 10^n。
 * 0.37 这样的轴标题看着就像没做完的东西，1/2/5 才像。
 */
export function niceStep(span: number, targetTicks = 4): number {
  if (!(span > 0)) return 1;
  const rough = span / Math.max(1, targetTicks);
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const normalized = rough / magnitude;
  const factor = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return factor * magnitude;
}

/** 轴从 0 起（柱长编码数量，截底会让差异看起来比实际大）。 */
export function niceAxis(values: Array<number | null>, targetTicks = 4): { max: number; ticks: number[] } {
  const numeric = values.filter((v): v is number => typeof v === 'number' && Number.isFinite(v));
  const raw = numeric.length ? Math.max(...numeric) : 0;
  if (raw <= 0) return { max: 1, ticks: [0, 1] };
  let step = niceStep(raw, targetTicks);
  // 计数轴的刻度不该出现 0.5 个人 —— 值全是整数时步长至少为 1
  if (step < 1 && numeric.every((v) => Number.isInteger(v))) step = 1;
  const max = Math.ceil(raw / step) * step;
  const ticks: number[] = [];
  for (let v = 0; v <= max + step / 1000; v += step) ticks.push(Number(v.toFixed(6)));
  return { max, ticks };
}

export interface Arc {
  start: number;
  end: number;
  fraction: number;
  label: string;
  /** null = 这组没有值；扇区按 0 算，但图例必须写"—"而不是"0"（和柱状图同一条规矩）。 */
  value: number | null;
}

/**
 * 饼/环图扇区。0 值组不能画成"看不见"—— 它在图例里必须出现，
 * 否则"这一组存在但没有量"这个事实会彻底消失。
 */
export function donutArcs(points: Point[]): Arc[] {
  const positive = points.map((p) => Math.max(0, p.value ?? 0));
  const total = positive.reduce((a, b) => a + b, 0);
  const arcs: Arc[] = [];
  let angle = 0;
  points.forEach((point, index) => {
    const value = positive[index] ?? 0;
    const fraction = total > 0 ? value / total : 1 / points.length;
    const sweep = fraction * Math.PI * 2;
    arcs.push({ start: angle, end: angle + sweep, fraction, label: point.label, value: point.value });
    angle += sweep;
  });
  return arcs;
}

/** 环图路径；整圆要拆成两段弧，否则 SVG 的 A 命令画不出 360°。 */
export function arcPath(radius: number, inner: number, start: number, end: number): string {
  const sweep = end - start;
  if (sweep >= Math.PI * 2 - 1e-6) {
    const half = start + Math.PI;
    return [
      arcPath(radius, inner, start, half),
      arcPath(radius, inner, half, end),
    ].join(' ');
  }
  const pt = (r: number, a: number) => `${(r * Math.cos(a)).toFixed(3)} ${(r * Math.sin(a)).toFixed(3)}`;
  const large = sweep > Math.PI ? 1 : 0;
  return [
    `M ${pt(radius, start)}`,
    `A ${radius} ${radius} 0 ${large} 1 ${pt(radius, end)}`,
    `L ${pt(inner, end)}`,
    // 回程那条弧也必须用内半径：写成 radius 的话，SVG 会找一条"半径 = 外半径且过这两点"
    // 的圆弧，圆心被挪到别处，扇区就甩到环外面去了（图照样画满，只是每一块都是错的）
    `A ${inner} ${inner} 0 ${large} 0 ${pt(inner, start)}`,
    'Z',
  ].join(' ');
}

export const CHART_COLORS = [
  '#2f6fed', '#00b96b', '#fa8c16', '#eb2f96', '#722ed1',
  '#13c2c2', '#f5222d', '#a0d911', '#597ef7', '#faad14',
];

export function colorFor(index: number): string {
  return CHART_COLORS[index % CHART_COLORS.length] ?? CHART_COLORS[0] ?? '#2f6fed';
}

/** 大数压缩：轴标签放不下 1,234,567 这种长度。 */
export function formatNumber(value: number | null): string {
  if (value === null || Number.isNaN(value)) return '—';
  const abs = Math.abs(value);
  if (abs >= 1e8) return `${trimZero(value / 1e8)} 亿`;
  if (abs >= 1e4) return `${trimZero(value / 1e4)} 万`;
  if (Number.isInteger(value)) return value.toLocaleString('en-US');
  return trimZero(Number(value.toFixed(2)));
}

function trimZero(n: number): string {
  return String(Number(n.toFixed(2)));
}

/**
 * 分组轴候选排序 —— 图表最容易做坏的一件事就是"默认按 schema 第一个字段分组"：
 * 第一个字段往往是名称列，于是一张每个值都只出现一次的图铺满屏幕
 * （看板就栽过这个跟头，见 kanbanModel.groupableFields）。
 * 字典列基数低且有中文标签，优先；其次日期（时间趋势是图表最主要用途）；
 * 数值列基数高，只在别无可选时才当分组轴。
 */
export function chartGroupCandidates(resolvedFields: ResolvedField[]): ResolvedField[] {
  const rankOf = (field: ResolvedField): number => {
    const def = field.ctx.field;
    const type = (def.fieldType ?? '').toUpperCase();
    if (def.dictCode) return 0;
    if (field.ctx.descriptor.cellValueType === 'DateTime') return 1;
    if (field.ctx.descriptor.cellValueType === 'Boolean') return 2;
    if (type === 'STRING' && (def.fieldLength ?? 0) <= 64) return 3;
    if (type === 'STRING' || type === 'TEXT') return 4;
    return 5;
  };
  return resolvedFields
    .map((field, index) => ({ field, index, rank: rankOf(field) }))
    .sort((a, b) => a.rank - b.rank || a.index - b.index)
    .map((entry) => entry.field);
}

/** 归一化：从后端/本地读回来的配置补默认值，未知值一律回落而不是抛错。 */
export function normaliseConfig(raw: Partial<ChartConfig> | null | undefined): ChartConfig {
  const kind = raw?.kind && CHART_KINDS.includes(raw.kind) ? raw.kind : DEFAULT_CONFIG.kind;
  const metricFn = raw?.metricFn && METRIC_FUNCTIONS.includes(raw.metricFn)
    ? raw.metricFn
    : 'COUNT';
  const timeGroup = raw?.timeGroup && TIME_GROUPS.includes(raw.timeGroup) ? raw.timeGroup : undefined;
  return {
    kind,
    groupField: raw?.groupField || undefined,
    timeGroup,
    metricFn,
    metricField: metricFn === 'COUNT' ? undefined : raw?.metricField || undefined,
    topN: typeof raw?.topN === 'number' && raw.topN > 0 ? Math.min(raw.topN, 60) : DEFAULT_CONFIG.topN,
  };
}

/** 命名图表视图还原出来的完整快照：图长什么样 + 它当初是在什么筛选下存的。 */
export interface ChartViewSnapshot {
  config: ChartConfig;
  conditions: QueryCondition[];
  conjunction: Conjunction;
}

/**
 * 图下面那行口径说明。单图视图和仪表盘共用同一个函数：
 * 两处写两遍措辞，早晚会有一处漏说"含 N 条筛选"，而一个没说筛选条件的图表
 * 就是让人读错的图表。
 */
export function chartCaption(args: {
  kind: ChartKind;
  fieldLabel: string;
  metricName: string;
  timeEnabled: boolean;
  timeGroup?: TimeGroup;
  conditionCount: number;
}): string {
  const { kind, fieldLabel, metricName, timeEnabled, timeGroup, conditionCount } = args;
  const parts = [kind === 'number' ? '全部记录' : `按「${fieldLabel}」分组`, metricName];
  if (timeEnabled && kind !== 'number') parts.push(TIME_GROUP_LABELS[timeGroup ?? 'MONTH']);
  parts.push(conditionCount ? `含 ${conditionCount} 条筛选` : '未筛选');
  return parts.join(' · ');
}

/**
 * 从落库的 config 还原命名图表视图。
 *
 * 认不出来时返回 null，而不是给一套默认配置 —— 管理页能手改这一列，把表格视图
 * 的配置存成 viewType=CHART 是完全可能的；静默回落成"默认柱状图"会让人以为
 * 视图坏了，返回 null 才能明确说"这个视图不是图表配置，打不开"。
 *
 * 筛选条件一起还原：图表的口径是"当前筛选命中的全部行"，只存图表配置的话，
 * 一个名字叫「P0 任务趋势」的视图在别的筛选状态下会画出完全不同的数。
 */
export function chartViewSnapshot(
  view: ViewConfigDTO | null | undefined,
): ChartViewSnapshot | null {
  if (!view?.config) return null;
  const parsed = parseViewConfig<Record<string, unknown> | null>(view.config, null);
  if (!parsed || typeof parsed !== 'object') return null;
  const chart = parsed as Partial<ChartConfig>;
  const looksLikeChart =
    (typeof chart.kind === 'string' && CHART_KINDS.includes(chart.kind as ChartKind)) ||
    (typeof chart.metricFn === 'string' && METRIC_FUNCTIONS.includes(chart.metricFn as MetricFunction)) ||
    typeof chart.groupField === 'string';
  if (!looksLikeChart) return null;
  const state = normalise(parsed as Partial<WorkspaceViewState>);
  return {
    config: normaliseConfig(chart),
    conditions: state.conditions,
    conjunction: state.conjunction,
  };
}

/**
 * 两份配置是不是同一张图 —— 用来判断当前工具栏状态对应哪个命名视图。
 * 比较归一化后的结果而不是原始载荷：字段名会大小写/缺省不一，直接比 JSON
 * 会让刚存下的视图在下拉框里显示成未选中。
 */
export function sameChartConfig(a: ChartConfig, b: ChartConfig | null | undefined): boolean {
  if (!b) return false;
  return JSON.stringify(normaliseConfig(a)) === JSON.stringify(normaliseConfig(b));
}
