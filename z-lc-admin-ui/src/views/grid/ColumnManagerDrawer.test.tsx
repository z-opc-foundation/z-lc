import { describe, expect, it } from 'vitest';
import { fireEvent, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { ColumnManagerDrawer } from '@/views/grid/ColumnManagerDrawer';
import { projectColumns, visibleColumns } from '@/views/grid/gridModel';
import type { ColumnMeta, EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * 列设置抽屉的"顺序契约"。
 *
 * 抽屉是 `columnMeta` 的唯一写者（顺序/显隐/宽度都从这儿出），表格读的是
 * `projectColumns(resolvedFields, columnMeta)`。这两条路径以前用的是**两套顺序**：
 * 抽屉按 schema 排自己的行，表格按 columnMeta 排列 —— 于是量出来三个现象
 * （2026-09-23，真浏览器同一轮，都是当场读数读出来的）：
 *   1. 只给"工时"输一个宽度，表头变成 `工时|标题|优先级|截止日期` —— 改个宽度把整列挪到最前面；
 *   2. 键盘排序成功之后，抽屉里那几行纹丝不动，还是 schema 顺序；
 *   3. 再排第二次，表头和第一次**一模一样** —— 第二次是从 schema 顺序起算的，第一次的结果被丢掉。
 * 现象 1 出在 `patch` 往 `columnMeta` 尾巴上追加（而表格把 columnMeta 的顺序当首选顺序，
 * 追加=插到最前），现象 2、3 出在同一处：`rows` 只跟着 `resolvedFields` 走。
 *
 * 这一层钉的是"抽屉列出的顺序 == 表格要用的顺序"和"改宽度/显隐不许换顺序、不许丢邻居的配置"。
 *
 * ⚠ 拖拽本身（含界面上写着的那句「空格 + 方向键可键盘排序」）**不在这一层测**：
 * jsdom 里 dnd-kit 的 KeyboardSensor 一按 Space 就抛
 * `TypeError: element.scrollIntoView is not a function`（jsdom 没实现这个方法，实测抛在
 * drag start 那一步，整个流程直接中断），PointerSensor 又依赖 `document.elementFromPoint`。
 * 所以"键盘真的能排序""两次排序会叠加"归浏览器门禁，这一层只钉它们的前提：
 * 抽屉读到的是当前顺序，不是 schema 顺序。**不要在单测里声称验过拖拽。**
 */

const APP = 'crm';
const ENT = 'task';

function resolved() {
  const fields: FieldDefDTO[] = [
    { fieldCode: 'title', fieldName: '标题', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
    { fieldCode: 'prio', fieldName: '优先级', fieldType: 'STRING', fieldLength: 16, sortOrder: 2 },
    { fieldCode: 'hours', fieldName: '工时', fieldType: 'DECIMAL', fieldLength: 10, scale: 1, sortOrder: 3 },
    { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 4 },
  ];
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  const entity = {
    id: 7, entityCode: ENT, entityName: '任务', tableName: 'ui_task_probe',
    tenantCode: 'default', appCode: APP, fields,
  } as EntityDefDTO;
  return resolveEntityFields(entity, ws);
}

type RF = ReturnType<typeof resolved>;

/** 表格真正会用的顺序。带 hidden 的那一份也列出来：抽屉里隐藏列照样占一格。 */
const projectedOrder = (rf: RF, meta: ColumnMeta[]) =>
  projectColumns(rf, meta).map((c) => c.resolved.ctx.field.fieldCode);
const visibleOrder = (rf: RF, meta: ColumnMeta[]) =>
  visibleColumns(projectColumns(rf, meta)).map((c) => c.resolved.ctx.field.fieldCode);

/** 抽屉里此刻真正列出的 fieldCode，按 DOM 顺序。 */
function drawerOrder(): string[] {
  return Array.from(document.querySelectorAll('.ant-drawer [role="button"]')).map((handle) => {
    const text = handle.parentElement?.textContent ?? '';
    return /\((\w+)\)/.exec(text)?.[1] ?? text;
  });
}

function rowOf(fieldCode: string): HTMLElement {
  const handle = Array.from(document.querySelectorAll<HTMLElement>('.ant-drawer [role="button"]'))
    .find((h) => (h.parentElement?.textContent ?? '').includes(`(${fieldCode})`));
  if (!handle?.parentElement) {
    throw new Error(`抽屉里没列出 ${fieldCode}，当前列出：${JSON.stringify(drawerOrder())}`);
  }
  return handle.parentElement;
}

/** 空数组下标在 TS 里是 `| undefined`，这里让它红得清楚：一次都没写回就是不通过。 */
function last(arr: ColumnMeta[][]): ColumnMeta[] {
  const value = arr[arr.length - 1];
  if (!value) {
    throw new Error('抽屉一次都没有写回过 columnMeta');
  }
  return value;
}

const SCHEMA_ORDER = ['title', 'prio', 'hours', 'due'];
/** 一个用户已经排过的顺序：故意和 schema 顺序不同，抽屉必须照抄它。 */
const CUSTOM_ORDER = ['hours', 'title', 'due', 'prio'];
const customMeta = (): ColumnMeta[] => CUSTOM_ORDER.map((fieldCode) => ({ fieldCode }));

describe('ColumnManagerDrawer', () => {
  it('抽屉列出的顺序就是表格将要用的顺序，不是 schema 顺序', () => {
    const rf = resolved();
    render(<ColumnManagerDrawer open resolvedFields={rf} onClose={() => {}} columnMeta={customMeta()} onChange={() => {}} />);
    // 这一条是现象 2、3 的根：拖第二下从哪里起算，取决于这里列出的是什么顺序。
    expect(drawerOrder(), '抽屉顺序必须等于表格投影顺序').toEqual(visibleOrder(rf, customMeta()));
    expect(drawerOrder(), '顺带钉住夹具：它确实不是 schema 顺序').not.toEqual(SCHEMA_ORDER);
  });

  it('给一个还没持久化的列改宽度，不许把该列挪到表格最前面', async () => {
    const rf = resolved();
    const emitted: ColumnMeta[][] = [];
    render(
      <ColumnManagerDrawer open resolvedFields={rf} onClose={() => {}} columnMeta={[]} onChange={(next) => emitted.push(next)} />,
    );
    const before = visibleOrder(rf, []);
    const input = rowOf('hours').querySelector('input') as HTMLInputElement;
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    await user.click(input);
    await user.type(input, '233');
    await user.tab();
    expect(emitted.length, '改宽度应当写回一次').toBeGreaterThan(0);
    const next = last(emitted);
    expect(next.find((item) => item.fieldCode === 'hours')?.width, '宽度本身要写进去').toBe(233);
    expect(visibleOrder(rf, next), '表格列顺序不该因为一个宽度而变').toEqual(before);
  });

  it('隐藏一列：写回的必须是"当前顺序 + 这一项加 hidden"，邻居的宽度要原样带着', () => {
    const rf = resolved();
    const emitted: ColumnMeta[][] = [];
    const meta: ColumnMeta[] = [
      { fieldCode: 'hours', width: 233 },
      { fieldCode: 'title' },
      { fieldCode: 'due', hidden: true },
      { fieldCode: 'prio' },
    ];
    render(<ColumnManagerDrawer open resolvedFields={rf} onClose={() => {}} columnMeta={meta} onChange={(next) => emitted.push(next)} />);
    expect(drawerOrder(), '夹具：抽屉列出的是这个已排好的顺序').toEqual(CUSTOM_ORDER);
    fireEvent.click(rowOf('prio').querySelector('button') as HTMLElement);
    expect(emitted.length, '点开关应当写回一次').toBe(1);
    // 整份数组逐个比：顺序、槽位、邻居的 width/hidden 都在这一条里。
    expect(emitted[0]).toEqual([
      { fieldCode: 'hours', width: 233 },
      { fieldCode: 'title' },
      { fieldCode: 'due', hidden: true },
      { fieldCode: 'prio', hidden: true },
    ]);
    expect(projectedOrder(rf, last(emitted)), '写回之后表格顺序仍等于点之前的顺序').toEqual(CUSTOM_ORDER);
  });
});
