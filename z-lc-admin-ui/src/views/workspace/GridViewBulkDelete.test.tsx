import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { message } from 'antd';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GridView } from '@/views/workspace/GridView';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import type { EntityDefDTO, FieldDefDTO } from '@/api/types';

/**
 * 批量删除的交互测试，单独一个文件。
 *
 * 分文件的真正理由写在这个文件里，不在这里猜：这条链路要点开 Popconfirm、发请求、
 * 重渲染整张表，还会留下 antd 的 body 级 message 单例（cleanup 收不掉），
 * 所以它跟 GridView.test.tsx 的 8 条用例混在一起时，两边都要为对方造成的
 * 「同进程 DOM 越堆越多」买单。独立文件让每条用例从干净的 jsdom 起步。
 *
 * 一开始这个文件单跑一条要 12~17 秒，把并行的 ImportDialog / KanbanView 拖到超时红。
 * 我第一版把原因写成"antd 弹层容器累积"，那是错的 —— 实测定位到的元凶是本文件
 * 自己的查询写法（见 deleteSelected 上方的坑④），改成作用域/文案查询后单条约 1 秒。
 * 记下来是因为"工具很慢"这种解释听起来总像环境问题，而实际几乎都是我们用了 O(文档) 的查询。
 */

const APP = 'crm';
const ENT = 'customer';

function respond(payload: unknown) {
  const text = JSON.stringify(payload);
  return {
    ok: true,
    status: 200,
    // client.ts 读的是 response.text()，不是 json()
    text: async () => text,
    json: async () => payload,
  };
}

function entityDef(): EntityDefDTO {
  const fields: FieldDefDTO[] = [
    { fieldCode: 'customer_name', fieldName: '客户名称', fieldType: 'STRING', required: true, fieldLength: 128, sortOrder: 1 },
    { fieldCode: 'level', fieldName: '客户等级', fieldType: 'STRING', dictCode: 'lvl', fieldLength: 16, sortOrder: 2 },
    { fieldCode: 'balance', fieldName: '余额', fieldType: 'DECIMAL', fieldLength: 18, scale: 2, sortOrder: 3 },
  ];
  return {
    id: 7, entityCode: ENT, entityName: '客户', tableName: 't_customer',
    tenantCode: 'default', appCode: APP, fields: fields.map((f) => f),
  } as EntityDefDTO;
}

function resolvedFields() {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  return resolveEntityFields(entityDef(), ws);
}

interface FetchCall {
  url: string;
  body: Record<string, unknown>;
}

const calls: FetchCall[] = [];

/** 批量删除的服务端回包，逐条用例改它 —— 界面报几行只认这里，不认"我发了几个请求"。 */
let batchDeleteReply: Record<string, unknown> = {
  total: 2, deletedCount: 2, applied: true, rolledBack: false, ids: [1, 2], errors: [], message: '已删除 2 条',
};

function stubBackend() {
  calls.length = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    if (url.includes('/runtime/list')) {
      return respond({
        success: true, code: 200, message: null,
        data: {
          records: [
            { id: 1, customer_name: '张三', level: 'C', level_label: '金卡', balance: 9999.99 },
            { id: 2, customer_name: '李四', level: 'B', level_label: '银卡', balance: 88.5 },
          ],
          total: 2, pageNum: 1, pageSize: 20,
        },
      });
    }
    if (url.includes('/runtime/delete-batch')) {
      return respond({ success: true, code: 200, message: null, data: batchDeleteReply });
    }
    return respond({ success: true, code: 200, message: null, data: null });
  }));
}

function renderGrid() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <GridView
        entity={entityDef()}
        resolvedFields={resolvedFields()}
        views={[]}
        appCode={APP}
        tenantCode="default"
        onEditRecord={() => undefined}
        onOpenRecord={() => undefined}
        onCreateRecord={() => undefined}
        conditions={[]}
        conjunction="AND"
        onFilterChange={() => undefined}
      />
    </QueryClientProvider>,
  );
}

/**
 * 勾掉前 n 行 → 点「删除 n 条」→ Popconfirm 确认。
 *
 * 四个只有真跑才撞上的坑，别改回去：
 * ① 带图标的 antd Button **取不到精确的 accessible name**（图标的 role=img / aria-label 会参与计算），
 *    所以 `name: '删除 2 条'` 恒为 0 个；而 `findByRole` 配一个匹配不上的名字**不会快速失败**，
 *    会把整条用例的超时吃干，报错只剩一句 "Test timed out"。
 * ② `userEvent.click` 点 Popconfirm 的触发按钮在 jsdom 里会挂住不返回（rc-trigger 渲染 +
 *    pointer/act 交互），`fireEvent.click` 正常。真实按键链路归浏览器门禁用真 Chrome 验。
 * ③ 勾选框数组要一次取好：点完第一行表格会重渲染，再查一次会拿到正在被替换的节点。
 * ④ 不要用全文档的 `getByRole`。实测（同一条链路单独跑，见下方注释）：
 *    `getAllByRole('button')` 在每次 DOM 变更后第一次调用要 1372ms / 1472ms（24 个候选按钮），
 *    而 `within(小容器)` 只要 1~36ms，纯文本扫描 0~1ms。原因是 jsdom 每次 DOM 变更后都要重算
 *    getComputedStyle（本仓库里还会打出 "Not implemented: window.getComputedStyle" 那句噪音）。
 *    一次用例里三次全文档扫描就是 4 秒起步 —— 单跑 12~17 秒、并把同机并行的 ImportDialog /
 *    KanbanView 拖到超时红。所以这里定位一律走文本 + 作用域查询，a11y 命名本身归浏览器门禁验。
 */
/** antd 会在两个汉字的按钮里插空格（渲染成「完 成」），所以文案匹配一律容忍空白。 */
function labelOf(n: number) {
  return new RegExp(`删除\\s*${n}\\s*条`);
}

function buttonsByText(label: RegExp): HTMLButtonElement[] {
  const hits = within(document.body as HTMLElement)
    .queryAllByText(label)
    .map((t) => t.closest('button'))
    .filter((b): b is HTMLButtonElement => b !== null);
  return [...new Set(hits)];
}

function buttonByText(label: RegExp): HTMLButtonElement {
  const found = buttonsByText(label);
  expect(found.length, `应当正好有一个文案匹配 /${label.source}/ 的按钮`).toBe(1);
  return found[0]!;
}

async function deleteSelected(n = 2) {
  // 行勾选框：一次取好（坑③），并且不经过 role 计算（坑④）。
  // 必须按 tr[data-row-key] 收窄：antd 在 tbody 里还塞了一行 ant-table-measure-row
  // （虚拟列表用来量列宽的），它带一个一模一样的 ant-checkbox-input —— 用
  // '.ant-table-tbody input[type=checkbox]' 会多量到它，于是勾两行实际只勾中一行。
  const boxes = Array.from(document.querySelectorAll<HTMLInputElement>('tr[data-row-key] input[type="checkbox"]'));
  expect(boxes.length, `表格该有 ${n} 行可勾选`).toBe(n);
  for (let i = 0; i < n; i++) {
    fireEvent.click(boxes[i]!);
  }
  fireEvent.click(buttonByText(labelOf(n)));
  const pop = document.querySelector('.ant-popover, .ant-popconfirm') as HTMLElement;
  expect(pop, '点批量删除必须先弹出 Popconfirm（这是一道确认，不是直接删）').toBeTruthy();
  const ok = within(pop).getByRole('button', { name: /^(OK|确\s*定)$/ });
  fireEvent.click(ok);
}

async function renderAndSettle() {
  renderGrid();
  await waitFor(() => expect(screen.getByText('张三')).toBeInTheDocument());
}

afterEach(() => {
  // antd 的 message 是挂在 body 上的单例，testing-library 的 cleanup 收不掉它。
  // 不收的话，上一条用例的「已删除 2 条」会一直浮在那儿，下一条用例里的
  // queryByText('已删除 2 条') 就变成 "Found multiple elements" —— 看着像断言坏了，
  // 其实是上一条的吐司没散。
  message.destroy();
});

beforeEach(() => {
  window.localStorage.clear();
  batchDeleteReply = {
    total: 2, deletedCount: 2, applied: true, rolledBack: false, ids: [1, 2], errors: [], message: '已删除 2 条',
  };
  stubBackend();
});

describe('GridView 批量删除', () => {
  it('只发一个 delete-batch 请求，把所有 id 带走', { timeout: 15000 }, async () => {
    await renderAndSettle();
    calls.length = 0;
    await deleteSelected();

    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/delete-batch'))).toBe(true));
    const batch = calls.filter((c) => c.url.includes('/runtime/delete-batch'));
    // 这条断言就是这个改动的全部理由：以前是 for 循环发 N 个单删，中途失败就留下"删了一半"。
    expect(batch.length, `批量删除该一个请求搞定，实际发了 ${batch.length} 个`).toBe(1);
    expect(batch[0]!.url).toContain('entityCode=customer');
    expect(batch[0]!.body).toMatchObject({ appCode: 'crm', tenantCode: 'default', ids: [1, 2] });
    // 一次都不该再走单删入口
    expect(
      calls.filter((c) => /\/runtime\/delete(\?|$)/.test(c.url)),
      '不该再逐行发 /runtime/delete',
    ).toEqual([]);
  });

  it('报的条数来自服务端，不是"我发了几个请求"', { timeout: 15000 }, async () => {
    await renderAndSettle();
    await deleteSelected();
    await waitFor(() => expect(screen.getByText('已删除 2 条')).toBeInTheDocument());
    // 删成功后勾选清空，那个按钮不该还挂着
    await waitFor(() => expect(buttonsByText(labelOf(2)).length).toBe(0));
  });

  it('整批预检没过时说"一条都没删"，并且保留勾选让人重来', { timeout: 15000 }, async () => {
    batchDeleteReply = {
      total: 3, deletedCount: 0, applied: false, rolledBack: false, ids: [],
      errors: [{ index: 2, id: 999, message: '记录 999 不存在或已被删除' }],
      message: '有 1 条记录不能删除，整批未删除',
    };
    await renderAndSettle();
    await deleteSelected();

    await waitFor(() => expect(screen.getByText(/整批未删除/)).toBeInTheDocument());
    // 没删成就不许报成功 —— 谎报"已删除"是数据事故级别的问题。
    expect(screen.queryByText('已删除 2 条'), '一条都没删时不许出现成功文案').not.toBeInTheDocument();
    // 要说清是"一条都没删"，还要点名是哪一条
    expect(screen.getByText(/整批未删除/).textContent).toContain('1 条记录不能删除');
    expect(screen.getByText(/整批未删除/).textContent).toContain('记录 999');
    // 勾选保持原样，用户修好问题可以直接重来
    expect(buttonsByText(labelOf(2)).length, '删除按钮应当还在，好让人重来').toBe(1);
    const checked = Array.from(document.querySelectorAll<HTMLInputElement>('tr[data-row-key] input[type="checkbox"]'))
      .filter((b) => b.checked);
    expect(checked.length, '失败后勾选必须原样保留').toBe(2);
  });
});
