import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DesignerPage } from '@/lc/views/designer/DesignerPage';
import type { EntityDefDTO } from '@/lc/api/types';

/**
 * 字段编码那道闸（缺陷 #34 / #35 的前端半边）。
 *
 * 界面原来写着"这可以用"：设计器只按 `^[A-Za-z][A-Za-z0-9_]*$` 判合法，而
 * `id` / `tenant_code` / `deleted` / `create_time` / `update_time` **全都是合法标识符** ——
 * 它们是引擎给每张受管表自建的列。实测后果分两种，都比"名字不好看"重：
 *  ① 撞名：`entity/create` 照样 HTTP 200 收下元数据，要等 provision 建表才炸裸 500
 *     `Duplicate column name`；而 `provision-all` 在第一个坏实体上抛，同一个应用里
 *     干净实体的表一起没建成（实测：`runtime/list ctl` → 500 Table not found）。
 *  ② 非法编码（中文、空格、数字开头）：`buildCreateTableDdl` 是 `continue` 静默跳过，
 *     建表**照样返回成功**，于是元数据说有三列、物理表一列都没有。
 *
 * 所以这两类都必须在"点保存之前"就拦住。这里的 7 条由 e2e/mutate_designer_field_code.py
 * 逐个注入反证；后端那半边（真正的写入口）另有 7 条单测 + mutate_field_code_guard.py。
 */

const APP = 'crm';
const OWNED = ['id', 'tenant_code', 'deleted', 'create_time', 'update_time'];

let calls: string[] = [];
/** 桩后端唯一"写得进去"的一张表：PUT 之后 entity/list 要能把它吐回来。 */
let saved: EntityDefDTO | null = null;

function entity(id: number, code: string, name: string, fields: number): EntityDefDTO {
  return {
    id,
    entityCode: code,
    entityName: name,
    tableName: `t_${code}`,
    tenantCode: 'default',
    appCode: APP,
    fields: Array.from({ length: fields }, (_unused, index) => ({
      fieldCode: `f${index}`,
      fieldName: `字段${index}`,
      fieldType: 'STRING',
      sortOrder: index,
    })),
  } as EntityDefDTO;
}

function envelope(data: unknown, success = true) {
  const text = JSON.stringify({
    success,
    code: success ? 200 : 500,
    message: success ? null : '接口炸了',
    data,
  });
  return {
    ok: success,
    status: success ? 200 : 500,
    statusText: success ? 'OK' : 'Server Error',
    text: async () => text,
    json: async () => JSON.parse(text),
  };
}

function stubBackend() {
  calls = [];
  saved = null;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown, init?: RequestInit) => {
      const url = String(input);
      calls.push(`${init?.method ?? 'GET'} ${url}`);
      await new Promise((resolve) => setTimeout(resolve, 0));
      if (url.includes('/admin/app/entity/list')) {
        return envelope([saved ?? entity(11, 'deal', '商机', 3), entity(12, 'lead', '线索', 1)]);
      }
      if (url.includes('/meta/bundle')) return envelope(null, false);
      // 保存这条走 PUT /admin/entity?id=…：正例要真的成功**并且存住**，否则"能保存"只证明了
      // 请求发出去，没证明重新拉回来之后用户改的那一格还在。
      if (url.includes('/admin/entity') && init?.method === 'PUT') {
        saved = { ...entity(11, 'deal', '商机', 3), ...(JSON.parse(String(init.body)) as EntityDefDTO) };
        return envelope(11);
      }
      if (url.includes('/app/list')) return envelope([{ appCode: APP, appName: 'CRM' }]);
      if (url.includes('/app/schema')) return envelope([]);
      if (
        url.includes('/dict/list') ||
        url.includes('/view-config/list') ||
        url.includes('/relation/list') ||
        url.includes('/meta/field-types')
      ) {
        return envelope([]);
      }
      return envelope(null, false);
    }),
  );
}

function designerTree() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <MemoryRouter initialEntries={[`/designer/${APP}`]}>
      <QueryClientProvider client={client}>
        <Routes>
          <Route path="/designer/:appCode" element={<DesignerPage />} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>
  );
}

/**
 * 字段表的那一行。
 *
 * 按**下标**取而不是按编码文本取 —— 这一版最初写成 `textContent.includes('f0')`，
 * 7 条用例全在夹具里红：「字段编码」那一格是 `<Input value="f0">`，而 input 的 value
 * 不在 textContent 里，文本匹配永远命中不了。行定位靠不住的话，改宽断言也测不到东西，
 * 所以下面 `openDesignerWithFields` 里加了一条"这格确实是 f0"的夹具自证。
 */
function fieldRow(container: HTMLElement, index = 0): HTMLElement {
  const rows = Array.from(container.querySelectorAll('.ant-table-row'));
  if (rows.length <= index) throw new Error(`字段表只有 ${rows.length} 行，取不到第 ${index + 1} 行`);
  return rows[index] as HTMLElement;
}

/** 字段行的第一格就是"字段编码"输入框（第二格才是显示名）。 */
function codeInput(container: HTMLElement, rowIndex = 0): HTMLInputElement {
  const row = fieldRow(container, rowIndex);
  const input = row.querySelector('input');
  if (!input) throw new Error(`字段表第 ${rowIndex + 1} 行里没有编码输入框`);
  return input as HTMLInputElement;
}

/**
 * 打字：一次 change 就够了。
 *
 * 这里原来写成"先清空再输入"两连发，实测 5/7 条用例停在 `字段编码不能为空` —— 也就是
 * 只有第一发落了地，第二发没进 draft，于是所有断言实际测的都是"清空"这一个动作。
 * 机制没去深挖（受控 input + act 的刷新次序），因为修法就是去掉那一发：一次全量替换，
 * 7 条全绿，且"清空编码"那条仍然单独覆盖空值。
 */
function typeCode(container: HTMLElement, next: string) {
  fireEvent.change(codeInput(container), { target: { value: next } });
}

function saveButton() {
  const matches = Array.from(document.querySelectorAll('button')).filter(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === '保存',
  );
  if (matches.length !== 1) throw new Error(`「保存」按钮应有且只有一个，实际 ${matches.length} 个`);
  return matches[0] as HTMLButtonElement;
}

function blockAlert() {
  return Array.from(document.querySelectorAll('.ant-alert')).find(
    (node) => (node.textContent ?? '').includes('无法保存'),
  );
}

function blockText() {
  return blockAlert()?.textContent ?? '';
}

function updateCalls() {
  return calls.filter((line) => line.startsWith('PUT') && line.includes('/admin/entity'));
}

const settle = () => new Promise((resolve) => setTimeout(resolve, 200));

function fieldRowCount(container: HTMLElement) {
  return container.querySelectorAll('.ant-table-row').length;
}

/** 侧栏那个只有图标、没有文字的新建按钮（"添加字段"也带 plus 图标，但它有文字）。 */
function newEntityButton(container: HTMLElement): HTMLButtonElement {
  const matches = Array.from(container.querySelectorAll('button')).filter(
    (node) => node.querySelector('.anticon-plus') && !(node.textContent ?? '').trim(),
  );
  if (matches.length !== 1) throw new Error(`新建实体按钮应只有 1 个，实际 ${matches.length} 个`);
  return matches[0] as HTMLButtonElement;
}

/** 头部那三栏都是 `addonBefore`，按 addon 的文案定位到同一组的 input。 */
function headerInput(label: string): HTMLInputElement {
  const addon = Array.from(document.querySelectorAll('.ant-input-group-addon')).find(
    (node) => (node.textContent ?? '').trim() === label,
  );
  if (!addon) {
    const seen = Array.from(document.querySelectorAll('.ant-input-group-addon'))
      .map((node) => JSON.stringify((node.textContent ?? '').trim()));
    throw new Error(`没有「${label}」这一栏，页面上的 addon 只有 [${seen.join(', ')}]`);
  }
  const input = addon.parentElement?.querySelector('input');
  if (!input) throw new Error(`「${label}」旁边找不到输入框`);
  return input as HTMLInputElement;
}

async function openDesignerWithFields() {
  const tree = render(designerTree());
  await waitFor(() => expect(screen.getByText('商机')).toBeTruthy());
  fireEvent.click(screen.getByText('商机'));
  // 夹具自证：确认第一行第一格**就是** f0 那个编码框。这行断言站不住，
  // 下面所有"改成 id 就拦住了"的结论都可能是往别的输入框里打字打出来的。
  await waitFor(() => expect(codeInput(tree.container).value).toBe('f0'));
  return tree;
}

beforeEach(() => {
  vi.unstubAllGlobals();
  stubBackend();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('设计器不许让字段编码撞上引擎自建列', () => {
  it('把字段编码改成 id：要说清撞了引擎自建列，并且保存要按住', async () => {
    const { container } = await openDesignerWithFields();
    expect(blockAlert(), '夹具：正常编码时不该有「无法保存」').toBeFalsy();

    typeCode(container, 'id');

    await waitFor(() => expect(blockText()).toContain('撞了引擎自建列'));
    expect(blockText(), '要点名是哪一个编码撞了').toContain('id');
    expect(saveButton(), '撞名的实体不许保存').toBeDisabled();
    // 红框要长在**这一格**上：只有一条文字提示、输入框毫无标记，用户在表格里还是找不出哪个字段坏了。
    const cell = codeInput(container).closest('td');
    expect(cell?.querySelector('.ant-input-status-error, .ant-input-affix-wrapper-status-error'),
      '撞名的那一格要标红').toBeTruthy();
    // 拦在点保存之前：一次请求都不该发出去（发了就是后端收下、provision 才炸那条老路）。
    expect(updateCalls(), '按住保存意味着一个 PUT 都不发').toHaveLength(0);
  });

  it('五个引擎自建列逐个试，一个都不许放行', async () => {
    const { container } = await openDesignerWithFields();
    for (const code of OWNED) {
      typeCode(container, code);
      // eslint-disable-next-line no-await-in-loop
      await waitFor(() => expect(blockText()).toContain('撞了引擎自建列'));
      expect(saveButton(), `${code} 是引擎自建的列，不许保存`).toBeDisabled();
    }
  });

  it('大写 ID 一样要拦（MySQL 列名不分大小写，它照样撞 id）', async () => {
    const { container } = await openDesignerWithFields();
    typeCode(container, 'ID');
    await waitFor(() => expect(blockText()).toContain('撞了引擎自建列'));
    expect(saveButton()).toBeDisabled();
  });

  it('合法编码不该被误伤：改了名要能真的保存出去', async () => {
    const { container } = await openDesignerWithFields();
    typeCode(container, 'customer_name');
    await settle();
    expect(blockAlert(), '合法编码不该出现「无法保存」').toBeFalsy();
    expect(saveButton()).toBeEnabled();
    fireEvent.click(saveButton());
    await waitFor(() => expect(updateCalls().length).toBeGreaterThan(0));
    // 存完重新拉回来，那一格还得是用户写的值：闸门放开但一改就丢，等于白放。
    await waitFor(() => expect(codeInput(container).value).toBe('customer_name'));
    expect(blockAlert(), '保存成功后不该留「无法保存」这块横幅').toBeFalsy();
  });

  it('撞名修好之后，闸门要跟着松开（不能一旦拦过就永远按住）', async () => {
    const { container } = await openDesignerWithFields();
    typeCode(container, 'deleted');
    // 断的是"因为撞名而按住"，不是"因为空而按住"：只查 disabled 的话这条会被误清空的值满足。
    await waitFor(() => expect(blockText()).toContain('撞了引擎自建列'));
    expect(saveButton()).toBeDisabled();
    typeCode(container, 'flag');
    await waitFor(() => expect(saveButton()).toBeEnabled());
    expect(blockAlert(), '改回合法编码后「无法保存」要消失').toBeFalsy();
  });
});

describe('非法列名也不许留着"建表成功"骗人', () => {
  it('中文、空格、数字开头三种都拦在保存之前', async () => {
    const { container } = await openDesignerWithFields();
    for (const code of ['我的字段', 'has space', '2bad']) {
      typeCode(container, code);
      // eslint-disable-next-line no-await-in-loop
      await waitFor(() => expect(blockText()).toContain('不合法'));
      expect(saveButton(), `${code} 进不了 DDL，不该让保存`).toBeDisabled();
    }
  });

  it('清空编码不算改好', async () => {
    const { container } = await openDesignerWithFields();
    typeCode(container, '');
    await waitFor(() => expect(blockText()).toContain('字段编码不能为空'));
    expect(saveButton()).toBeDisabled();
  });
});

/**
 * 这一条本来是为 #34 那道闸写的"别把新建堵死"回归，结果它当场抓到一个更前面的坏东西（#36）：
 * 点"新建实体"**根本没有草稿可用**。实测症状是 `没有「实体编码」这一栏，页面上的 addon 只有 []` ——
 * 编辑区整个回到 Empty 占位（"选择左侧实体开始编辑，或新建一个实体"）。
 * 根因在派生草稿那个 effect：按钮先 `setSelectedId(null)` 再 `setDraft(新草稿)`，
 * 而 effect 依赖 `[selectedId, entities]`，看见 selectedId 变 null 就 `setDraft(null)` 把刚给的草稿冲掉。
 *
 * 第二个毛病藏在后面（修好 #36 才看得见）：新草稿原来预置 `systemFieldDefs()`
 * （id / create_time / update_time）当用户字段 —— 那三列是引擎给每张受管表自建的，
 * 加上 #34 那道闸之后，一份干净的新草稿一开口就是三条"撞了引擎自建列"、保存永久按住。
 * 所以字段表要真的是空的（视图那边 `registry.systemFieldDefs` 自己会合成这三列）。
 */
describe('闸不许把新建实体这条路堵死', () => {
  it('新建一份干净草稿：填完编码就该能保存，字段表里不该预置引擎自建列', async () => {
    const { container } = await openDesignerWithFields();
    fireEvent.click(newEntityButton(container));

    fireEvent.change(headerInput('实体编码'), { target: { value: 'contract' } });
    fireEvent.change(headerInput('实体名称'), { target: { value: '合同' } });
    fireEvent.change(headerInput('物理表名'), { target: { value: 't_contract' } });
    await settle();

    expect(fieldRowCount(container), '引擎自建的 id/create_time/update_time 不该出现在可编辑字段表里')
      .toBe(0);
    expect(blockText(), '干净的新草稿不该被拦').not.toContain('撞了引擎自建列');
    expect(saveButton(), '填完编码的新实体要能保存').toBeEnabled();
  });
});
