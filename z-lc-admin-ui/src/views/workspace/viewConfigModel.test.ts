import { beforeEach, describe, expect, it } from 'vitest';
import {
  EMPTY_STATE,
  normalise,
  readStoredState,
  viewState,
  withViewName,
  writeStoredState,
} from '@/views/workspace/viewConfigModel';
import type { ColumnMeta } from '@/api/types';

/**
 * 视图状态的归一化。
 *
 * 直接守在我这轮真踩过的坑上：脏 stats / 未知排序方向 / 手改坏的 JSON
 * 不能让页面崩，也不能被当成合法配置发到后端。
 */
describe('视图状态归一化', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('丢掉未知聚合函数与非法排序方向', () => {
    const clean = normalise({
      stats: { age: 'SUM', phone: 'DROP_TABLE', level: 'sum', note: 'MEDIAN' } as never,
      sorts: [
        { fieldCode: 'id', dir: 'asc' },
        { fieldCode: 'evil', dir: 'sideways' },
        null,
      ] as never,
      columnMeta: [{ fieldCode: 'a' }, { foo: 1 }, null] as unknown as ColumnMeta[],
    });
    expect(clean.stats).toEqual({ age: 'SUM', level: 'SUM' });
    expect(clean.sorts).toEqual([{ fieldCode: 'id', dir: 'asc' }]);
    expect(clean.columnMeta).toEqual([{ fieldCode: 'a' }]);
  });

  it('大小写与空白都吸收，函数名统一大写', () => {
    expect(normalise({ stats: { age: ' avg ' } as never }).stats).toEqual({ age: 'AVG' });
  });

  it('完全空/垃圾输入退化成 EMPTY_STATE 而不是抛错', () => {
    for (const garbage of [null, undefined, {}, 'nope', 42]) {
      expect(normalise(garbage as never)).toEqual(EMPTY_STATE);
    }
  });

  it('localStorage 里是坏 JSON 时读回 null，不抛', () => {
    window.localStorage.setItem('zlc:state:a1:customer:LIST', '{ not json');
    expect(readStoredState('a1', 'customer', 'LIST')).toBeNull();
  });

  it('写入再读回是原样的（统计配置不能在这一层丢）', () => {
    writeStoredState('a1', 'customer', 'LIST', {
      ...EMPTY_STATE,
      columnMeta: [{ fieldCode: 'age', width: 90 }],
      stats: { age: 'SUM' },
    });
    const back = readStoredState('a1', 'customer', 'LIST');
    expect(back?.stats).toEqual({ age: 'SUM' });
    expect(back?.columnMeta).toEqual([{ fieldCode: 'age', width: 90 }]);
  });

  it('命名视图的 config 是 JSON 字符串，坏字符串给 null 让调用方回退', () => {
    expect(viewState({ id: 1, config: '{"columnMeta":[],"stats":{"age":"MAX"}}' } as never)?.stats)
      .toEqual({ age: 'MAX' });
    expect(viewState({ id: 2, config: '{' } as never)).toBeNull();
    expect(viewState(undefined)).toBeNull();
  });

  it('保存视图时名字与统计一起进 config', () => {
    const raw = withViewName({ ...EMPTY_STATE, stats: { balance: 'AVG' } }, '我的视图');
    expect(JSON.parse(raw).name).toBe('我的视图');
    expect(JSON.parse(raw).stats).toEqual({ balance: 'AVG' });
  });
});
