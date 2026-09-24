import { describe, expect, it } from 'vitest';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { groupableFields } from '@/views/workspace/kanbanModel';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * 看板分组字段的候选排序。
 *
 * 这条测试来自我实际犯的 bug：候选按 schema 顺序取第一个，于是 VARCHAR(64) 的
 * "商机名" 赢过了真正低基数的字典字段 "阶段"，看板退化成一条记录一列。
 * 浏览器里才看得出来 —— 所以把它固化成测试。
 */
function resolve(fields: FieldDefDTO[]) {
  const ws = createWorkspaceContext({ appCode: 'a1', tenantCode: 'default', entities: [] });
  const entity = { entityCode: 'deal', tableName: 't', fields } as EntityDefDTO;
  return resolveEntityFields(entity, ws);
}

const code = (list: ReturnType<typeof resolve>) => list.map((f) => f.ctx.field.fieldCode);

describe('看板分组字段候选', () => {
  it('字典字段赢过排在它前面的短文本', () => {
    const ranked = groupableFields(resolve([
      { fieldCode: 'title', fieldName: '商机名', fieldType: 'STRING', fieldLength: 64 } as FieldDefDTO,
      { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage' } as FieldDefDTO,
    ]));
    expect(code(ranked)[0]).toBe('stage');
  });

  it('布尔排在短文本之前', () => {
    const ranked = groupableFields(resolve([
      { fieldCode: 'name', fieldType: 'STRING', fieldLength: 32 } as FieldDefDTO,
      { fieldCode: 'done', fieldType: 'BOOLEAN' } as FieldDefDTO,
    ]));
    expect(code(ranked)[0]).toBe('done');
  });

  it('同档内保持 schema 顺序，结果稳定不随调用次数变', () => {
    const fields = [
      { fieldCode: 's1', fieldType: 'STRING', dictCode: 'd' },
      { fieldCode: 's2', fieldType: 'STRING', dictCode: 'd' },
    ] as FieldDefDTO[];
    const first = code(groupableFields(resolve(fields)));
    expect(first).toEqual(['s1', 's2']);
    expect(code(groupableFields(resolve(fields)))).toEqual(first);
  });

  it('长文本不参与分组，但没有任何候选时退回全集，不让看板变成空白', () => {
    const onlyLong = resolve([
      { fieldCode: 'body', fieldType: 'TEXT' },
      { fieldCode: 'note', fieldType: 'STRING', fieldLength: 1000 },
    ] as FieldDefDTO[]);
    expect(code(groupableFields(onlyLong))).toEqual(code(onlyLong));
  });
});
