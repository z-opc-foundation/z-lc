import { describe, expect, it } from 'vitest';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';
import {
  DEFAULT_PIVOT_CONFIG,
  PivotStructureError,
  buildPivotMatrix,
  buildPivotProgram,
  normalisePivotConfig,
  pivotDimensionCandidates,
  pivotMeasureColumn,
  pivotMeasureFields,
  pivotColumnLabel,
  type PivotConfig,
} from './pivotModel';
import { UNFILLED_GROUP_LABEL, categoryLabel } from './chartModel';

/**
 * 交叉表口径测试。
 *
 * 钉住四件"表画出来了但数是错的"事：
 * ① 进来的行已经是聚合行，所以合并必须用 SUM(group_count)，用 COUNT(*) 会数出一片 1；
 * ② 空格子是 null 不是 0，不能被加总吃进去；
 * ③ 行/列顺序不能跟着数据库的返回序漂；
 * ④ 折叠掉的行必须仍在总计里，否则"合计"说的是半张表。
 */

const ENT = 'deal';

function entityDef(): EntityDefDTO {
  const fields = [
    { fieldCode: 'name', fieldName: '商机名', fieldType: 'STRING', required: true, sortOrder: 1 },
    { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', sortOrder: 2 },
    { fieldCode: 'amount', fieldName: '金额', fieldType: 'DECIMAL', sortOrder: 3 },
    { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 4 },
    { fieldCode: 'extra', fieldName: '附件', fieldType: 'JSON', sortOrder: 5 },
  ] as unknown as FieldDefDTO[];
  return {
    id: 3, entityCode: ENT, entityName: '商机', tableName: 't_deal',
    tenantCode: 'default', appCode: 'crm', fields,
  } as unknown as EntityDefDTO;
}

const ws = createWorkspaceContext({ appCode: 'crm', tenantCode: 'default', entities: [entityDef()] });
const resolved = resolveEntityFields(entityDef(), ws);
const codes = (fields: typeof resolved) => fields.map((field) => field.ctx.field.fieldCode);

function config(overrides: Partial<PivotConfig> = {}): PivotConfig {
  return { ...DEFAULT_PIVOT_CONFIG, rowField: 'stage', colField: 'name', ...overrides };
}

describe('维度与度量候选', () => {
  it('标量显示列都能当维度，只有数值列能做度量', () => {
    // JSON 列在 descriptor 里显示为 String，所以它也在维度候选里（取的是 JSON 原文）
    expect(codes(pivotDimensionCandidates(resolved))).toEqual(['name', 'stage', 'amount', 'due', 'extra']);
    expect(codes(pivotMeasureFields(resolved))).toEqual(['amount']);
  });

  it('行维度与列维度撞同一列时被错开，而不是画出一张对角线假表', () => {
    const out = normalisePivotConfig(config({ rowField: 'stage', colField: 'stage' }),
      pivotDimensionCandidates(resolved), pivotMeasureFields(resolved));
    expect(out.rowField).toBe('stage');
    expect(out.colField).toBe('name');
  });

  it('维度列被删掉后回落到可用列，而不是把未知列发给后端吃 400', () => {
    const out = normalisePivotConfig(config({ rowField: 'gone', colField: 'also_gone' }),
      pivotDimensionCandidates(resolved), pivotMeasureFields(resolved));
    expect(out.rowField).toBe('name');
    expect(out.colField).toBe('stage');
  });

  it('列维度没选时保持没选，不替用户猜一列', () => {
    const out = normalisePivotConfig(config({ colField: undefined }),
      pivotDimensionCandidates(resolved), pivotMeasureFields(resolved));
    expect(out.colField).toBeUndefined();
  });

  it('非 COUNT 指标必须有度量列，缺了取第一个数值列', () => {
    const out = normalisePivotConfig(config({ metricFn: 'SUM', metricField: undefined }),
      pivotDimensionCandidates(resolved), pivotMeasureFields(resolved));
    expect(out.metricField).toBe('amount');
  });
});

describe('发给后端的整形程序', () => {
  it('COUNT 走 group_count 这一列，而不是给聚合行再数一次', () => {
    expect(pivotMeasureColumn(config())).toBe('group_count');
    expect(pivotMeasureColumn(config({ metricFn: 'SUM', metricField: 'amount' }))).toBe('sum_amount');
    expect(pivotMeasureColumn(config({ metricFn: 'AVG', metricField: 'amount' }))).toBe('avg_amount');
  });

  it('合并算子恒为 SUM：进来的每行已经是一个桶，COUNT(*) 会恒为 1', () => {
    const [step] = buildPivotProgram('group_count') as [{ op: string; agg: string; value: string;
      by: string[]; on: string }];
    expect(step.op).toBe('pivot');
    expect(step.agg).toBe('SUM');
    expect(step.value).toBe('group_count');
    // 行维度要同时取编码和标签：编码保证两个不同码不会因重名并成一行，标签负责显示
    expect(step.by).toEqual(['group_key', 'group_label']);
    expect(step.on).toBe('group_label_2');
  });
});

describe('矩阵构造', () => {
  it('列序自己排定，不跟着后端"第一次出现"的顺序漂', () => {
    const matrix = buildPivotMatrix([
      { group_key: 'OK', group_label: 'Done', Q3: 5, Q1: 1 },
      { group_key: 'OK', group_label: 'Done', Q2: 2 },
    ], config());
    expect(matrix.colKeys).toEqual(['Q1', 'Q2', 'Q3']);
  });

  it('行按合计从大到小排，标签只做兜底平序', () => {
    const matrix = buildPivotMatrix([
      { group_key: 'A', group_label: '甲', x: 3 },
      { group_key: 'B', group_label: '乙', x: 9 },
      { group_key: 'C', group_label: '丙', x: 5 },
    ], config());
    expect(matrix.rows.map((row) => row.label)).toEqual(['乙', '丙', '甲']);
    expect(matrix.rows[0]?.total).toBe(9);
  });

  it('空格子是 null：不加进合计，也不冒充 0', () => {
    const matrix = buildPivotMatrix([
      { group_key: 'A', group_label: '甲', x: 10, y: null },
      { group_key: 'B', group_label: '乙', x: 4, y: 6 },
    ], config());
    const a = matrix.rows.find((row) => row.key === 'A');
    expect(a?.cells).toEqual([10, null]);
    expect(a?.total).toBe(10);
    expect(matrix.colTotals).toEqual([14, 6]);
    expect(matrix.grandTotal).toBe(20);
  });

  it('MIN / MAX 的合计取小取大，不是把格子加起来的 30', () => {
    const rows = [
      { group_key: 'A', group_label: '甲', x: 10, y: 20 },
      { group_key: 'B', group_label: '乙', x: 1, y: 3 },
    ];
    expect(buildPivotMatrix(rows, config({ metricFn: 'MIN' })).colTotals).toEqual([1, 3]);
    expect(buildPivotMatrix(rows, config({ metricFn: 'MAX' })).grandTotal).toBe(20);
  });

  it('平均数不参与横向加总：合计口径缺失时宁可不显示', () => {
    const matrix = buildPivotMatrix([
      { group_key: 'A', group_label: '甲', x: 10, y: 20 },
    ], config({ metricFn: 'AVG', metricField: 'amount' }));
    expect(matrix.totalsMeaningful).toBe(false);
    expect(matrix.grandTotal).toBe(null);
    expect(matrix.colTotals).toEqual([null, null]);
    expect(matrix.rows[0]?.total).toBe(null);
  });

  it('折叠掉的行仍进总计：可见格 + 其余行 == 列合计 == 总计', () => {
    const matrix = buildPivotMatrix([
      { group_key: 'A', group_label: '甲', x: 10 },
      { group_key: 'B', group_label: '乙', x: 7 },
      { group_key: 'C', group_label: '丙', x: 3 },
    ], config({ maxRows: 2 }));
    expect(matrix.rows.map((row) => row.key)).toEqual(['A', 'B']);
    expect(matrix.rest?.label).toBe('其余 1 行');
    expect(matrix.rest?.total).toBe(3);
    expect(matrix.rowCount).toBe(3);
    expect(matrix.colTotals).toEqual([20]);
    expect(matrix.grandTotal).toBe(20);
  });

  it('分组值为 NULL 的那一档：行和列都要报出名字，但取格子仍用原始空键', () => {
    // 后端 pivot 拿第二维的值当列键，该维为 NULL 时列键就是空串（18090 实测形状）。
    // 空列键画出来是一根没有标题的列，所以只改显示标签、不改键 —— 键一改，格子就取不到了。
    const matrix = buildPivotMatrix([
      { group_key: null, group_label: null, '': 4, 甲: 6 },
    ], config());
    expect(matrix.rows[0]?.label).toBe('（未填写）');
    expect(matrix.rows[0]?.key).toBe('');
    expect(matrix.colKeys).toEqual(['', '甲']);
    expect(matrix.colLabels).toEqual(['（未填写）', '甲']);
    expect(matrix.rows[0]?.cells).toEqual([4, 6]);
    expect(matrix.colTotals[0]).toBe(4);
  });

  it('交叉表与柱状图对同一个空档用同一个字', () => {
    // 两个视图对着同一批数据叫出两个名字，用户会当成两档不同的数据。
    expect(pivotColumnLabel('')).toBe(UNFILLED_GROUP_LABEL);
    expect(pivotColumnLabel('  ')).toBe(UNFILLED_GROUP_LABEL);
    expect(pivotColumnLabel('0')).toBe('0');
    expect(categoryLabel({ group_key: null })).toBe(UNFILLED_GROUP_LABEL);
  });

  it('程序产出不是矩阵时抛结构错误，不回退成一张空表', () => {
    expect(() => buildPivotMatrix({ group_key: 'A' }, config())).toThrow(PivotStructureError);
    expect(() => buildPivotMatrix([null], config())).toThrow(PivotStructureError);
    // 报错要说清拿到的是什么，否则排查的人只能猜
    expect(() => buildPivotMatrix(null, config())).toThrow(/null/);
  });
});
