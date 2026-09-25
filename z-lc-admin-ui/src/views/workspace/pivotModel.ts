import type { ResolvedField } from '@/fields';
import { UNFILLED_GROUP_LABEL } from './chartModel';

/**
 * 交叉表（透视）的唯一口径实现。
 *
 * 分工是刻意切的：**行转列发生在后端**（`POST /runtime/shape` 的 `pivot` 步骤，对着二维聚合结果做），
 * 这里只做"读一张矩阵"该做的事——列序、行序、截断、合计。
 * 之所以把合计留在前端：它是一次纯算术的行列加总，不需要第二条 SQL；
 * 但**平均数不在能加总的行列里**，所以它被显式排除（见 `totalsMeaningful`）。
 */

export const PIVOT_METRIC_FNS = ['COUNT', 'SUM', 'AVG', 'MIN', 'MAX'] as const;
export type PivotMetricFn = (typeof PIVOT_METRIC_FNS)[number];

export const PIVOT_METRIC_LABELS: Record<PivotMetricFn, string> = {
  COUNT: '记录数',
  SUM: '合计',
  AVG: '平均',
  MIN: '最小',
  MAX: '最大',
};

export interface PivotConfig {
  rowField?: string;
  colField?: string;
  metricFn: PivotMetricFn;
  metricField?: string;
  /** 交叉表的行数是两个维度乘出来的，不设上限会把几百行塞进一屏。 */
  maxRows: number;
}

export const DEFAULT_PIVOT_CONFIG: PivotConfig = {
  rowField: undefined,
  colField: undefined,
  metricFn: 'COUNT',
  metricField: undefined,
  maxRows: 20,
};

const code = (field: ResolvedField) => field.ctx.field.fieldCode;
const cellType = (field: ResolvedField) => field.ctx.descriptor.cellValueType;

/**
 * 维度候选按"格子怎么显示"取：能当维度的是有标量显示的列。
 * JSON 列在 descriptor 里就是 String（后端把整个值当文本存），所以它可以当维度 ——
 * 只是这一维的取值会是 JSON 原文，用户真要用得自己负责。
 */
export function pivotDimensionCandidates(fields: ResolvedField[]): ResolvedField[] {
  return fields.filter((field) => {
    const type = cellType(field);
    return type === 'String' || type === 'Number' || type === 'DateTime' || type === 'Boolean';
  });
}

export function pivotMeasureFields(fields: ResolvedField[]): ResolvedField[] {
  return fields.filter((field) => cellType(field) === 'Number');
}

export function pivotFieldLabel(fields: ResolvedField[], fieldCode?: string): string {
  if (!fieldCode) return '未选';
  return fields.find((field) => code(field) === fieldCode)?.ctx.field.fieldName ?? fieldCode;
}

/**
 * 把配置落到真实字段上：维度被删掉/还没选时回落到第一个可用列，
 * 而不是原样发给后端 —— 未知维度是 400，而"列设置里刚删掉一列"不该让页面变成错误态。
 */
export function normalisePivotConfig(
  config: PivotConfig,
  dimensions: ResolvedField[],
  measures: ResolvedField[],
): PivotConfig {
  const dimensionCodes = dimensions.map(code);
  const rowField = dimensionCodes.includes(String(config.rowField))
    ? config.rowField
    : dimensionCodes[0];
  // 列维度"没选"就是没选（页面要提示，不能替用户猜一列）；
  // "选了但那列已经不存在"（命名视图引用的列被删了）才回落到另一列。
  let colField = config.colField
    ? (dimensionCodes.includes(config.colField)
        ? config.colField
        : dimensionCodes.find((item) => item !== rowField))
    : undefined;
  // 行维度与列维度同一列会产出一张对角线表：每个格子里只有它自己，看着像数据像极了但全是重复。
  if (colField && colField === rowField) {
    colField = dimensionCodes.find((item) => item !== rowField);
  }
  const metricCodes = measures.map(code);
  const metricField = config.metricFn === 'COUNT' ? undefined : metricCodes.includes(String(config.metricField))
    ? config.metricField
    : metricCodes[0];
  // 下界是 1 而不是"看起来合理"的 3：折到只剩 1 行 + 其余 N 行是合法诉求，
  // 钳高等于静默改掉用户存的配置，表格里显示的行数就会和他刚选的数对不上。
  const maxRows = Math.max(1, Math.min(100, Math.round(config.maxRows || DEFAULT_PIVOT_CONFIG.maxRows)));
  return { rowField, colField, metricFn: config.metricFn, metricField, maxRows };
}

/**
 * 度量在聚合响应里的列名。`COUNT` 用 `group_count` —— 它是这条分组本身带回来的记录数。
 */
export function pivotMeasureColumn(config: PivotConfig): string {
  if (config.metricFn === 'COUNT') return 'group_count';
  return `${config.metricFn.toLowerCase()}_${config.metricField}`;
}

/**
 * 交给 `pivot` 步骤的程序。
 *
 * ⚠ `agg` 恒为 SUM，与用户选的指标函数无关，而且这是这段代码里最容易写错的一处：
 * 进来的行**已经是聚合行**（每行代表一个 行维度 x 列维度 的桶，桶里只有一个值），
 * 所以 `COUNT(*)` 会数出"这个桶有几行"（恒为 1），把"记录数"这张表画成一片 1；
 * 而 `COUNT` 指标的口径要靠 `SUM(group_count)` 才是真实记录数。
 * MIN/MAX/AVG 指标同理：桶里只有一个值，任何合并算子取到的都是它本身，
 * 用 SUM 是为了让程序与指标函数解耦（也最容易讲清楚）。
 */
export function buildPivotProgram(measureColumn: string): unknown[] {
  return [
    {
      op: 'pivot',
      // 行维度取 (code, label) 两个表达式：code 保证两个不同编码不会因标签重名而被并成一行，
      // label 让页面上显示的是字典文案而不是裸码。列维度只能取一个表达式，取 label（后端保证非空）。
      by: ['group_key', 'group_label'],
      on: 'group_label_2',
      agg: 'SUM',
      value: measureColumn,
    },
  ];
}

/** 后端程序产出不是矩阵时抛这个，页面要显示"结构不对"而不是"没有数据"。 */
export class PivotStructureError extends Error {}

export interface PivotRow {
  key: string;
  label: string;
  cells: (number | null)[];
  total: number | null;
}

export interface PivotMatrix {
  colKeys: string[];
  /** 列头显示用的标签，与 colKeys 一一对应。colKeys 必须保持原样，它同时是取格子的键。 */
  colLabels: string[];
  rows: PivotRow[];
  /** 被 maxRows 折掉的行合起来的一行；为 null 表示没截断。它保证"看不见的数"不会被当成 0。 */
  rest: PivotRow | null;
  colTotals: (number | null)[];
  grandTotal: number | null;
  /** 合计对这指标有没有数学意义（AVG 没有）。 */
  totalsMeaningful: boolean;
  /** 透视出来的行总数（截断前）。 */
  rowCount: number;
  cellCount: number;
}

const ROW_KEY_COLUMN = 'group_key';
const ROW_LABEL_COLUMN = 'group_label';

/**
 * 列头的键是后端 pivot 拿第二维的值当键：该维为空时那个键就是空串，
 * 直接画出来会是一根**没有任何标题**的列 —— 用户只能猜它属于哪一档。
 */
export const pivotColumnLabel = (rawKey: string): string => (rawKey.trim() === '' ? UNFILLED_GROUP_LABEL : rawKey);

function combine(values: (number | null | undefined)[], metricFn: PivotMetricFn): number | null {
  const present = values.filter((value): value is number => typeof value === 'number');
  if (metricFn === 'AVG') return null;
  if (present.length === 0) return null;
  if (metricFn === 'MIN') return present.reduce((a, b) => Math.min(a, b));
  if (metricFn === 'MAX') return present.reduce((a, b) => Math.max(a, b));
  return present.reduce((a, b) => a + b, 0);
}

/** 列序必须自己排：后端返回的列是按"第一次出现"的顺序，而行是按频次排的，于是列序跟着数据分布漂。 */
function compareLabels(a: string, b: string): number {
  return a.localeCompare(b, 'zh-Hans-CN', { numeric: true, sensitivity: 'base' });
}

export function buildPivotMatrix(
  payload: unknown,
  config: PivotConfig,
): PivotMatrix {
  if (!Array.isArray(payload)) {
    throw new PivotStructureError(
      `整形结果不是行数组（拿到 ${payload === null ? 'null' : typeof payload}），`
        + '交叉表程序产出应为 pivot 的矩阵行。',
    );
  }
  const parsed: { key: string; label: string; values: Map<string, number | null> }[] = [];
  const columns = new Set<string>();
  for (const item of payload) {
    if (item === null || typeof item !== 'object' || Array.isArray(item)) {
      throw new PivotStructureError('整形结果里有一行不是对象，无法当交叉表的一行读。');
    }
    const record = item as Record<string, unknown>;
    const rawKey = record[ROW_KEY_COLUMN];
    // pivot 的空行键是 null（分组值为 NULL 的那一档），它是一档真实数据不是坏数据，
    // 但不能让它和"没有这个键"混在一起 —— 所以显式给一个可读的占位标签。
    const key = rawKey === null || rawKey === undefined ? '' : String(rawKey);
    const rawLabel = record[ROW_LABEL_COLUMN];
    const label = rawLabel === null || rawLabel === undefined || rawLabel === ''
      ? (key === '' ? UNFILLED_GROUP_LABEL : key)
      : String(rawLabel);
    const values = new Map<string, number | null>();
    for (const [field, value] of Object.entries(record)) {
      if (field === ROW_KEY_COLUMN || field === ROW_LABEL_COLUMN) continue;
      columns.add(field);
      values.set(field, typeof value === 'number' ? value : null);
    }
    parsed.push({ key, label, values });
  }

  const colKeys = [...columns].sort(compareLabels);
  const totalsMeaningful = config.metricFn !== 'AVG';
  const all: PivotRow[] = parsed.map((row) => {
    const cells = colKeys.map((column) => row.values.get(column) ?? null);
    return { key: row.key, label: row.label, cells, total: combine(cells, config.metricFn) };
  });

  // 行序：能加总就按行合计从大到小（交叉表要回答"哪几行最重"），否则按标签，最后都用标签兜平
  all.sort((a, b) => {
    if (totalsMeaningful && a.total !== null && b.total !== null && a.total !== b.total) {
      return b.total - a.total;
    }
    return compareLabels(a.label, b.label);
  });

  const visible = all.slice(0, config.maxRows);
  const hidden = all.slice(config.maxRows);
  const rest: PivotRow | null = hidden.length === 0
    ? null
    : {
        key: '',
        label: `其余 ${hidden.length} 行`,
        cells: colKeys.map((_, index) => combine(hidden.map((row) => row.cells[index]), config.metricFn)),
        total: combine(hidden.map((row) => row.total), config.metricFn),
      };

  const everyRow = rest ? [...visible, rest] : visible;
  const colTotals = colKeys.map((_, index) => combine(everyRow.map((row) => row.cells[index]), config.metricFn));

  return {
    colKeys,
    colLabels: colKeys.map(pivotColumnLabel),
    rows: visible,
    rest,
    colTotals,
    grandTotal: combine(everyRow.map((row) => row.total), config.metricFn),
    totalsMeaningful,
    rowCount: all.length,
    cellCount: all.length * colKeys.length,
  };
}
