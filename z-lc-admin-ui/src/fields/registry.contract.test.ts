import { describe, expect, it } from 'vitest';
import {
  createWorkspaceContext,
  getFieldDefinition,
  listFieldDefinitions,
  resolveEntityFields,
} from '@/fields';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * 字段注册表的契约测试。
 *
 * 这个注册表是整个前端的架构支点 —— 所有页面都只跟它打交道，不允许自己 switch(fieldType)。
 * 所以这里守的是"契约完整性"：每种类型都必须齐活儿，缺一个渲染器就会在运行时才炸。
 */

/** noUncheckedIndexedAccess 开着，取下标统一走这里，失败时还能带上上下文。 */
function one<T>(list: readonly T[], what: string): T {
  return at(list, 0, what);
}

function at<T>(list: readonly T[], index: number, what: string): T {
  const item = list[index];
  if (item === undefined) {
    throw new Error(`${what}: 解析结果不足 ${index + 1} 项`);
  }
  return item;
}

const ALL_TYPES = ['STRING', 'INT', 'LONG', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'TEXT'];

/** REF/dict 是注册表里的合成分支（按 refEntity / dictCode 命中），不占类型键。 */
const SYNTHETIC = ['REF', 'JSON'];

function field(over: Partial<FieldDefDTO>): FieldDefDTO {
  return {
    fieldCode: 'f',
    fieldName: '字段',
    fieldType: 'STRING',
    ...over,
  } as FieldDefDTO;
}

function entity(fields: FieldDefDTO[]): EntityDefDTO {
  return {
    entityCode: 'demo',
    entityName: '演示',
    tableName: 't_demo',
    tenantCode: 'default',
    appCode: 'a1',
    fields,
  } as EntityDefDTO;
}

function ctxFor(fields: FieldDefDTO[]) {
  const ws = createWorkspaceContext({ appCode: 'a1', tenantCode: 'default', entities: [] });
  return { ws, resolved: resolveEntityFields(entity(fields), ws) };
}

function onlyResolved(fields: FieldDefDTO[]) {
  return one(ctxFor(fields).resolved, fields[0]?.fieldCode ?? 'field');
}

describe('字段注册表契约', () => {
  it('10 种运行时字段类型全部可解析，且渲染器齐全', () => {
    for (const type of ALL_TYPES) {
      const def = getFieldDefinition(type);
      expect(def, `${type} 必须注册`).toBeDefined();
      if (!def) {
        // 让类型收窄生效；真走到这里上一条断言已经红了
        throw new Error(`${type} 未注册`);
      }
      expect(typeof def.renderCell, `${type}.renderCell`).toBe('function');
      expect(typeof def.renderEditor, `${type}.renderEditor`).toBe('function');
      expect(typeof def.renderFormInput, `${type}.renderFormInput`).toBe('function');
      expect(typeof def.renderFilterInput, `${type}.renderFilterInput`).toBe('function');
      expect(typeof def.toCellText, `${type}.toCellText`).toBe('function');
      expect(typeof def.toFieldValue, `${type}.toFieldValue`).toBe('function');
      expect(typeof def.toFormValue, `${type}.toFormValue`).toBe('function');
      expect(def.descriptor.cellValueType, `${type} 必须声明 cellValueType`).toBeTruthy();
    }
  });

  /**
   * 算子的真实契约在"解析后的字段"这一层 —— 消费方（FilterBar / 聚合 / 内联编辑）
   * 读的都是 resolved.operators，它可以由服务端 descriptor 补齐。
   * 所以这里按消费方视角断言，而不是按注册表原始条目的视角。
   */
  it('每种类型解析后都有可用算子，且与描述符一致', () => {
    for (const type of [...ALL_TYPES, ...SYNTHETIC]) {
      const ws = createWorkspaceContext({ appCode: 'a1', tenantCode: 'default' });
      const resolved = resolveEntityFields(
        entity([field({ fieldCode: `f_${type}`, fieldType: type, refEntity: type === 'REF' ? 'other' : undefined })]),
        ws,
      );
      expect(resolved.length, `${type} 应解析出字段`).toBe(1);
      const resolvedField = one(resolved, type);
      expect(Array.isArray(resolvedField.operators), `${type} 解析后 operators 必须是数组`).toBe(true);
      expect(resolvedField.operators.length, `${type} 至少要有一个可用算子`).toBeGreaterThan(0);
      expect(resolvedField.ctx.descriptor.cellValueType, `${type} 必须有 cellValueType`).toBeTruthy();
    }
  });

  it('每个注册项的 key 都是大写类型名，避免前端大小写两种写法并存', () => {
    for (const def of listFieldDefinitions()) {
      expect(def.key, `key ${def.key} 应大写`).toBe(def.key.toUpperCase());
    }
  });

  it('布尔写入接受 1/0/Y/N —— 与后端 BooleanTypeHandler 的口径保持一致', () => {
    const { resolved } = ctxFor([field({ fieldCode: 'flag', fieldType: 'BOOLEAN' })]);
    const bool = one(resolved, 'BOOLEAN');
    expect(bool.def.toFieldValue(true, bool.ctx)).toBe(true);
    expect(bool.def.toFieldValue('1', bool.ctx)).toBe(true);
    expect(bool.def.toFieldValue(1, bool.ctx)).toBe(true);
    expect(bool.def.toFieldValue('0', bool.ctx)).toBe(false);
    expect(bool.def.toFieldValue('Y', bool.ctx)).toBe(true);
  });

  it('日期落到引擎口径 yyyy-MM-dd / yyyy-MM-dd HH:mm:ss', () => {
    const { resolved } = ctxFor([
      field({ fieldCode: 'd', fieldType: 'DATE' }),
      field({ fieldCode: 'ts', fieldType: 'DATETIME' }),
    ]);
    const date = one(resolved, 'DATE');
    expect(String(date.def.toFieldValue('2026-01-02', date.ctx))).toBe('2026-01-02');
    const ts = at(resolved, 1, 'DATETIME');
    expect(String(ts.def.toFieldValue('2026-01-02 03:04:05', ts.ctx))).toBe('2026-01-02 03:04:05');
  });

  it('数值列把字符串转成数字，DECIMAL 不被整数化', () => {
    const { resolved } = ctxFor([
      field({ fieldCode: 'n', fieldType: 'INT' }),
      field({ fieldCode: 'b', fieldType: 'DECIMAL' }),
    ]);
    expect(one(resolved, 'INT').def.toFieldValue('42', one(resolved, 'INT').ctx)).toBe(42);
    expect((resolved[1] ?? one(resolved, 'x')).def.toFieldValue('12.5', (resolved[1] ?? one(resolved, 'x')).ctx)).toBe(12.5);
  });

  it('字典列优先用后端 JOIN 出来的 _label，取不到才回落原始码', () => {
    const level = onlyResolved([field({ fieldCode: 'level', fieldType: 'STRING', dictCode: 'lvl' })]);
    expect(
      level.def.toCellText({
        ctx: level.ctx,
        row: { level: 'C', level_label: '金卡' },
        value: 'C',
        displayValue: '金卡',
      }),
    ).toBe('金卡');
    expect(
      level.def.toCellText({ ctx: level.ctx, row: { level: 'C' }, value: 'C', displayValue: undefined }),
    ).toBe('C');
    // getFieldDefinition 必须容忍大小写与合成别名：直接把后端 fieldType 传进来不能拿到 undefined
    expect(getFieldDefinition('ref'), '小写别名').toBeDefined();
    expect(getFieldDefinition('dict'), '小写别名').toBeDefined();
  });
});
