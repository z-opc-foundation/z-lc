import type { ResolvedField } from '@/fields';

/**
 * 看板分组字段的候选与优先级.
 *
 * 单独成模块而不是留在 KanbanView.tsx 里: 一是这条规则值得被直接单测覆盖
 * (见 KanbanGrouping.test.ts —— 它守的就是我实际犯过的"看板退化成一条记录一列"),
 * 二是 react-refresh 要求组件文件只导出组件.
 */
export function groupableFields(resolvedFields: ResolvedField[]): ResolvedField[] {
  const rankOf = (field: ResolvedField): number => {
    const def = field.ctx.field;
    const type = (def.fieldType ?? '').toUpperCase();
    if (def.dictCode) return 0;
    if (type === 'BOOLEAN') return 1;
    if (type === 'STRING' && (def.fieldLength ?? 0) <= 64) return 2;
    return -1;
  };
  const ranked = resolvedFields
    .map((field, index) => ({ field, index, rank: rankOf(field) }))
    .filter((entry) => entry.rank >= 0)
    .sort((a, b) => a.rank - b.rank || a.index - b.index)
    .map((entry) => entry.field);
  return ranked.length ? ranked : resolvedFields;
}
