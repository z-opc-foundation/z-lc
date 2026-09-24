import { describe, expect, it } from 'vitest';
import type { AggregateRow } from '@/api/runtime';
import type { ResolvedField } from '@yuku123/render/fields';
import {
  buildSeries,
  bucketLabel,
  categoryLabel,
  chartGroupCandidates,
  chartViewSnapshot,
  donutArcs,
  arcPath,
  formatNumber,
  metricColumn,
  niceAxis,
  niceStep,
  normaliseConfig,
  sameChartConfig,
  type ChartConfig,
  type Point,
} from './chartModel';

/**
 * 图表口径的单元测试。
 *
 * 这里钉住的都是"图画错了但不会报错"的地方：时间桶没补零会让轴顺序变成
 * 1,10,11,2；分类轴把时间序列倒序画会成锯齿；0/0 的饼图会算出 NaN 角度而整块消失；
 * 分组轴默认取第一个字段会退化成一条记录一组（看板真的栽过）。
 */

function cfg(over: Partial<ChartConfig> = {}): ChartConfig {
  return { kind: 'bar', groupField: 'stage', metricFn: 'COUNT', topN: 12, ...over };
}

function point(label: string, value: number | null): Point {
  return { label, value, count: value ?? 0 };
}

type ArcCmd = { rx: number; large: number; sweep: number; from: [number, number]; to: [number, number] };

/**
 * 把 arcPath 的产物拆成命令序列，并跟踪当前点，这样每条 A 都能拿到自己的起点。
 * 之所以要在测试里重新解析一遍 SVG：环图的错都是"画得出来、但画错了"，
 * 只查 NaN / 命令条数的那些断言对这类缺陷完全免疫。
 */
function arcsOf(path: string): ArcCmd[] {
  const arcs: ArcCmd[] = [];
  let cursor: [number, number] = [0, 0];
  for (const matched of path.matchAll(/(M|A|L)([^MALZ]+)/g)) {
    const nums = (matched[2] ?? '').match(/-?\d+(?:\.\d+)?/g)?.map(Number) ?? [];
    const to: [number, number] = [nums[nums.length - 2] ?? 0, nums[nums.length - 1] ?? 0];
    if (matched[1] === 'A') {
      arcs.push({ rx: nums[0] ?? 0, large: nums[3] ?? 0, sweep: nums[4] ?? 0, from: cursor, to });
    }
    cursor = to;
  }
  return arcs;
}

/**
 * 圆心在原点是这张图唯一的正确解：半径必须同时等于两端点到原点的距离。
 * 内弧错写成外半径时 SVG 不报错，而是另找一个圆心把弧补上，扇区就甩到环外面。
 */
function arcGeometryErrors(arc: ArcCmd): string[] {
  const errors: string[] = [];
  if (Math.abs(arc.rx - Math.hypot(...arc.from)) > 0.01) {
    errors.push(`弧起点 ${arc.from} 到原点距离 ${Math.hypot(...arc.from).toFixed(3)} ≠ 半径 ${arc.rx}`);
  }
  if (Math.abs(arc.rx - Math.hypot(...arc.to)) > 0.01) {
    errors.push(`弧终点 ${arc.to} 到原点距离 ${Math.hypot(...arc.to).toFixed(3)} ≠ 半径 ${arc.rx}`);
  }
  if (arc.sweep !== 1 && arc.sweep !== 0) errors.push(`sweep flag 非法：${arc.sweep}`);
  return errors;
}

function field(code: string, type: string, extra: Record<string, unknown> = {}) {
  return {
    ctx: {
      field: { fieldCode: code, fieldName: code, fieldType: type, ...extra },
      descriptor: {
        cellValueType: extra.dictCode
          ? 'String'
          : type === 'DATE' || type === 'DATETIME'
            ? 'DateTime'
            : type === 'BOOLEAN'
              ? 'Boolean'
              : type === 'DECIMAL' || type === 'INT'
                ? 'Number'
                : 'String',
      },
    },
  } as unknown as ResolvedField;
}

describe('metricColumn', () => {
  it('COUNT 走 group_count，其他函数走 <fn>_<field>', () => {
    expect(metricColumn(cfg({ metricFn: 'COUNT' }))).toBe('group_count');
    expect(metricColumn(cfg({ metricFn: 'SUM', metricField: 'amount' }))).toBe('sum_amount');
    expect(metricColumn(cfg({ metricFn: 'AVG', metricField: 'score' }))).toBe('avg_score');
  });
});

describe('bucketLabel', () => {
  const row = { bucket_year: 2026, bucket_month: 9, bucket_day: 5 } as AggregateRow;

  it('月和日必须补零，否则轴上会出现 1,10,11,2 的顺序', () => {
    expect(bucketLabel(row, 'DAY')).toBe('2026-09-05');
    expect(bucketLabel(row, 'MONTH')).toBe('2026-09');
    expect(bucketLabel(row, 'YEAR')).toBe('2026');
  });

  it('缺年份字段就不是时间桶，交给分类标签处理', () => {
    expect(bucketLabel({ group_key: 'OK' } as AggregateRow, 'MONTH')).toBeUndefined();
    expect(bucketLabel({ bucket_year: null } as AggregateRow, 'MONTH')).toBeUndefined();
  });
});

describe('categoryLabel', () => {
  it('字典标签优先于原始码', () => {
    expect(categoryLabel({ group_key: 'OK', group_label: 'Done' })).toBe('Done');
  });

  it('空值分组要显式标出来，不能画一根没人认领的柱子', () => {
    expect(categoryLabel({ group_key: null })).toBe('（未填写）');
    expect(categoryLabel({ group_key: '' })).toBe('（未填写）');
  });

  it('没绑字典时回落到原始码', () => {
    expect(categoryLabel({ group_key: 'RAW' })).toBe('RAW');
  });
});

describe('buildSeries', () => {
  const rows: AggregateRow[] = [
    { group_key: 'NEW', group_label: '新建', group_count: 2 },
    { group_key: 'DONE', group_label: '已完成', group_count: 5 },
    { group_key: 'ARK', group_label: '归档', group_count: 3 },
  ];

  it('分类组按值倒序（柱状图惯例）', () => {
    expect(buildSeries(rows, cfg()).points.map((p) => p.label)).toEqual(['已完成', '归档', '新建']);
  });

  it('时间组保持时间正序，倒序会把折线图画成锯齿', () => {
    const timeRows: AggregateRow[] = [
      { bucket_year: 2026, bucket_month: 3, group_count: 1 },
      { bucket_year: 2026, bucket_month: 1, group_count: 4 },
      { bucket_year: 2026, bucket_month: 2, group_count: 2 },
    ];
    const built = buildSeries(timeRows, cfg({ groupField: 'due', timeGroup: 'MONTH' }));
    expect(built.points.map((p) => p.label)).toEqual(['2026-01', '2026-02', '2026-03']);
    expect(built.isTime).toBe(true);
  });

  it('聚合结果为 NULL 要留成 null，不能偷偷当 0', () => {
    const built = buildSeries(
      [{ group_key: 'A', group_count: 1, sum_amount: null }],
      cfg({ metricFn: 'SUM', metricField: 'amount' }),
    );
    expect(built.points[0]?.value).toBeNull();
  });

  it('超出 topN 的分类折叠成「其他」，数值合计不丢', () => {
    const many: AggregateRow[] = Array.from({ length: 5 }, (_, i) => ({
      group_key: `k${i}`,
      group_label: `组${i}`,
      group_count: i + 1,
    }));
    const built = buildSeries(many, cfg({ topN: 2 }));
    const labels = built.points.map((p) => p.label);
    expect(labels).toEqual(['组4', '组3', '其他 3 组']);
    expect(built.folded).toBe(3);
    expect(built.points.reduce((acc, p) => acc + (p.value ?? 0), 0)).toBe(15);
  });

  it('时间轴折叠保留最近的那段，而不是最早那段', () => {
    const many: AggregateRow[] = Array.from({ length: 6 }, (_, i) => ({
      bucket_year: 2026,
      bucket_month: i + 1,
      group_count: i,
    }));
    const built = buildSeries(many, cfg({ groupField: 'due', timeGroup: 'MONTH', topN: 2 }));
    expect(built.points.map((p) => p.label)).toEqual(['2026-05', '2026-06']);
    expect(built.folded).toBe(4);
  });

  it('无分组字段时归到「总计」一个柱', () => {
    const built = buildSeries([{ group_count: 42 }], cfg({ groupField: undefined }));
    expect(built.points).toEqual([{ label: '总计', value: 42, count: 42 }]);
  });
});

describe('niceAxis', () => {
  it('步长只取 1/2/5 × 10 的幂，轴上不该出现 0.37 这种数', () => {
    expect(niceStep(37)).toBe(10);
    expect(niceStep(3.7)).toBe(1);
    expect(niceStep(0.37)).toBe(0.1);
    expect(niceStep(750)).toBe(200);
  });

  it('轴从 0 起，并把最大值抬到整齐的刻度上', () => {
    expect(niceAxis([12, 30, 7])).toEqual({ max: 30, ticks: [0, 10, 20, 30] });
    expect(niceAxis([12, 37, 7])).toEqual({ max: 40, ticks: [0, 10, 20, 30, 40] });
  });

  it('全 0 / 空数据也要有可用的轴，而不是 max=0 导致除零', () => {
    expect(niceAxis([]).max).toBe(1);
    expect(niceAxis([0, 0]).ticks).toEqual([0, 1]);
    expect(niceAxis([null, undefined as unknown as number]).max).toBe(1);
  });

  it('计数轴的刻度必须是整数，但小数度量照常按 0.1 步长跑', () => {
    expect(niceAxis([1, 2, 1])).toEqual({ max: 2, ticks: [0, 1, 2] });
    expect(niceAxis([0.2, 0.4])).toEqual({ max: 0.4, ticks: [0, 0.1, 0.2, 0.3, 0.4] });
  });
});

describe('donutArcs 的 null 语义', () => {
  it('没有值的组扇区按 0 算，但 value 保持 null 让图例写「—」', () => {
    const arcs = donutArcs([point('甲', 10), point('乙', null)]);
    expect(arcs.map((a) => a.value)).toEqual([10, null]);
    expect(arcs[1]?.fraction).toBe(0);
    expect(arcs[1]?.start).toBe(arcs[0]?.end);
  });
});

describe('donutArcs / arcPath', () => {
  it('扇区占比之和为 1，角度铺满一圈', () => {
    const arcs = donutArcs([point('甲', 10), point('乙', 30)]);
    expect(arcs[0]?.fraction).toBeCloseTo(0.25);
    expect(arcs[1]?.fraction).toBeCloseTo(0.75);
    expect(arcs[arcs.length - 1]?.end).toBeCloseTo(Math.PI * 2);
  });

  it('全 0 不能算出 NaN 角度（否则整张图直接消失）', () => {
    const arcs = donutArcs([point('甲', 0), point('乙', 0)]);
    expect(arcs.every((arc) => Number.isFinite(arc.start) && Number.isFinite(arc.end))).toBe(true);
    expect(arcs[0]?.end).toBeGreaterThan(arcs[0]?.start ?? 0);
  });

  it('单一 100% 的扇区要拆成两段弧，SVG 的 A 命令画不出整圆', () => {
    const arcs = donutArcs([point('只有一组', 5)]);
    const path = arcPath(100, 60, arcs[0]?.start ?? 0, arcs[0]?.end ?? 0);
    expect((path.match(/M /g) ?? []).length).toBe(2);
    expect(path).not.toContain('NaN');
    const cmds = arcsOf(path);
    expect(cmds.map((c) => c.rx), '两段弧 = 两条外弧 + 两条内弧').toEqual([100, 60, 100, 60]);
    cmds.forEach((c) => expect(arcGeometryErrors(c)).toEqual([]));
  });

  it('外弧用外半径、内弧用内半径：写反了扇区会甩到环外面', () => {
    // 这条检查是被一个真缺陷补出来的：内弧曾写成 `A ${radius}`，41 个图表单测 +
    // 64 项浏览器检查全绿，只有截图看得出每一块扇区都是歪的。
    const halfShort = arcPath(108, 62, 0, Math.PI * 0.6);
    expect(arcsOf(halfShort).map((c) => c.rx)).toEqual([108, 62]);
    expect(arcsOf(halfShort).map((c) => c.sweep), '外弧顺时针、内弧逆时针回程').toEqual([1, 0]);
    expect(arcsOf(halfShort).map((c) => c.large), '不到半圆，large-arc-flag 不该翻').toEqual([0, 0]);
    arcsOf(halfShort).forEach((c) => expect(arcGeometryErrors(c)).toEqual([]));

    const pastHalf = arcPath(108, 62, 0, Math.PI * 1.4);
    expect(arcsOf(pastHalf).map((c) => c.rx)).toEqual([108, 62]);
    expect(arcsOf(pastHalf).map((c) => c.large), '过了半圆，两条弧都得翻 large-arc-flag').toEqual([1, 1]);
    arcsOf(pastHalf).forEach((c) => expect(arcGeometryErrors(c)).toEqual([]));
  });
});

describe('formatNumber', () => {
  it('大数压缩成万/亿，null 显示成破折号而不是 0', () => {
    expect(formatNumber(12345)).toBe('1.23 万');
    expect(formatNumber(123456789)).toBe('1.23 亿');
    expect(formatNumber(1234)).toBe('1,234');
    expect(formatNumber(0)).toBe('0');
    expect(formatNumber(null)).toBe('—');
  });
});

describe('chartGroupCandidates', () => {
  it('字典列优先，其次日期列，名称列排到最后', () => {
    const ordered = chartGroupCandidates([
      field('name', 'STRING', { fieldLength: 64 }),
      field('amount', 'DECIMAL'),
      field('due', 'DATE'),
      field('stage', 'STRING', { dictCode: 'stage' }),
    ]).map((f) => f.ctx.field.fieldCode);
    expect(ordered).toEqual(['stage', 'due', 'name', 'amount']);
  });

  it('没有别的可选时，数值列也能当分组轴（空图比有图更糟）', () => {
    const ordered = chartGroupCandidates([field('qty', 'INT')]).map((f) => f.ctx.field.fieldCode);
    expect(ordered).toEqual(['qty']);
  });
});

describe('normaliseConfig', () => {
  it('未知图表类型/聚合函数/分桶粒度都回落到默认，不抛错', () => {
    const safe = normaliseConfig({ kind: 'sunburst' as ChartConfig['kind'], metricFn: 'STDDEV' as ChartConfig['metricFn'], timeGroup: 'WEEK' as ChartConfig['timeGroup'] });
    expect(safe.kind).toBe('bar');
    expect(safe.metricFn).toBe('COUNT');
    expect(safe.timeGroup).toBeUndefined();
  });

  it('COUNT 不需要度量列；未知 topN 回落默认，超上限要夹紧', () => {
    expect(normaliseConfig({ metricFn: 'COUNT', metricField: 'amount' }).metricField).toBeUndefined();
    expect(normaliseConfig({ topN: 0 }).topN).toBe(12);
    expect(normaliseConfig({ topN: 9999 }).topN).toBe(60);
  });

  it('null / 空对象都能出一份可用配置', () => {
    expect(normaliseConfig(null)).toEqual({
      kind: 'bar',
      groupField: undefined,
      // 分桶粒度不预设：只有分组列真是日期列时才有意义，由 ChartView 决定默认「按月」
      timeGroup: undefined,
      metricFn: 'COUNT',
      metricField: undefined,
      topN: 12,
    });
  });
});

describe('命名图表视图的落库与还原', () => {
  function view(config: string | null) {
    return { id: 1, appCode: 'crm', entityCode: 'deal', viewType: 'CHART', config, tenantCode: 'default' };
  }

  it('存的筛选条件一起回来：同一个视图名在别的筛选下会画出另一个数', () => {
    const restored = chartViewSnapshot(
      view(
        JSON.stringify({
          kind: 'line',
          groupField: 'due',
          timeGroup: 'MONTH',
          metricFn: 'COUNT',
          topN: 6,
          conditions: [
            { fieldCode: 'stage', operator: 'eq', value: 'NEW' },
            { fieldCode: 'amount', operator: '', value: '1' },
          ],
          conjunction: 'OR',
          name: '每月新增趋势',
        }),
      ),
    );
    expect(restored?.config).toEqual({
      kind: 'line',
      groupField: 'due',
      timeGroup: 'MONTH',
      metricFn: 'COUNT',
      metricField: undefined,
      topN: 6,
    });
    // 缺 operator 的那条是脏数据，还原时必须已经被丢掉而不是带着发给后端
    expect(restored?.conditions).toEqual([{ fieldCode: 'stage', operator: 'eq', value: 'NEW' }]);
    expect(restored?.conjunction).toBe('OR');
  });

  it('认不出的载荷返回 null，不静默回落成一张默认柱状图', () => {
    // 管理页能手改 config，也能把表格视图存成 viewType=CHART
    expect(chartViewSnapshot(view('{"columnMeta":[{"fieldCode":"name"}],"sorts":[]}'))).toBeNull();
    expect(chartViewSnapshot(view('{ 坏 JSON'))).toBeNull();
    expect(chartViewSnapshot(view(''))).toBeNull();
    expect(chartViewSnapshot(view(null))).toBeNull();
    expect(chartViewSnapshot(undefined)).toBeNull();
  });

  it('sameChartConfig 比的是归一化结果：补默认值后的两份配置算同一张图', () => {
    expect(sameChartConfig(cfg(), { kind: 'bar', groupField: 'stage', metricFn: 'COUNT', topN: 12 })).toBe(true);
    // 多写一个 COUNT 用不上的度量列、或者脏 kind 回落 —— 画出来的还是同一张图
    expect(sameChartConfig(cfg({ metricField: 'amount' }), cfg())).toBe(true);
    expect(sameChartConfig(cfg({ kind: 'sunburst' as ChartConfig['kind'] }), cfg())).toBe(true);
    expect(sameChartConfig(cfg({ topN: 5 }), cfg())).toBe(false);
    expect(sameChartConfig(cfg(), null)).toBe(false);
  });
});
