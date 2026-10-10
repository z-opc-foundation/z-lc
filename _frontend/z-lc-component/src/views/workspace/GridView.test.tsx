import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { GridView } from '@/lc/views/workspace/GridView';
import type { EntityDefDTO, FieldDefDTO } from '@/lc/api/types';

/**
 * GridView 的组件级测试。
 *
 * 这一层原来一个测试都没有，而我在这个文件里连着犯过三次"只有真点浏览器才发现"的错：
 * 换实体时状态串味、保存视图高亮不跟着筛选变、页脚统计取错列。
 * 所以这里守的都是"提交给后端的 payload 形状"和"筛选变了界面要不要跟着变"，
 * 而不是快照像素。
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
  const entity = {
    id: 7, entityCode: ENT, entityName: '客户', tableName: 't_customer',
    tenantCode: 'default', appCode: APP, fields,
  } as EntityDefDTO;
  return { ...entity, fields: fields.map((f) => f) } as EntityDefDTO;
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

function stubBackend() {
  calls.length = 0;
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    // 让出一拍再回：只交微任务的话，一旦这个文件将来被改出"无限重查"，jsdom 的计时器会被
    // 饿死，测试表现为**挂住**而不是变红（记录侧那轮真栽过一次，见 _e2e/README「记录侧」一节）。
    await new Promise((resolve) => setTimeout(resolve, 0));
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
    if (url.includes('/runtime/aggregate')) {
      return respond({
        success: true, code: 200, message: null,
        data: [{ group_count: 2, sum_balance: 10088.49 }],
      });
    }
    if (url.includes('/runtime/update')) {
      return respond({ success: true, code: 200, message: null, data: 1 });
    }
    return respond({ success: true, code: 200, message: null, data: null });
  }));
}

function renderGrid(over: Partial<React.ComponentProps<typeof GridView>> = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const props = {
    entity: entityDef(),
    resolvedFields: resolvedFields(),
    views: [],
    appCode: APP,
    tenantCode: 'default',
    onEditRecord: () => undefined,
    onOpenRecord: () => undefined,
    onCreateRecord: () => undefined,
    conditions: [],
    conjunction: 'AND' as const,
    onFilterChange: () => undefined,
    ...over,
  };
  return render(
    <QueryClientProvider client={client}>
      <GridView {...props} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  window.localStorage.clear();
  stubBackend();
});


describe('GridView', () => {
  it('列由 schema 生成，标题用字段的中文名', async () => {
    renderGrid();
    // 表头与页脚会同时出现同一个列名，用 getAllByRole('columnheader') 精确一点
    await waitFor(() => expect(screen.getAllByText('客户名称').length).toBeGreaterThan(0));
    expect(screen.getAllByText('客户等级').length).toBeGreaterThan(0);
    expect(screen.getAllByText('余额').length).toBeGreaterThan(0);
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers.join('|')).toContain('客户名称');
    // 数据真的渲染出来了
    expect(screen.getByText('张三')).toBeInTheDocument();
  });

  it('字典列直接显示后端 JOIN 出的标签，不显示原始码', async () => {
    renderGrid();
    await waitFor(() => expect(screen.getByText('金卡')).toBeInTheDocument());
    expect(screen.getByText('银卡')).toBeInTheDocument();
  });

  it('列表请求带得上 entityCode/appCode/tenantCode（后端少了 appCode 会直接 400）', async () => {
    renderGrid();
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/list'))).toBe(true));
    const list = calls.find((c) => c.url.includes('/runtime/list'))!;
    expect(list.url).toContain('entityCode=customer');
    expect(list.url).toContain('appCode=crm');
    expect(list.url).toContain('tenantCode=default');
    expect(list.body).toMatchObject({ page: 1, size: 20 });
  });

  it('筛选条件与 conjunction 会原样进请求体，且变化后立刻重新拉数', async () => {
    calls.length = 0;
    renderGrid({
      conditions: [{ fieldCode: 'balance', operator: 'gte', value: 100 }],
      conjunction: 'OR',
    });
    await waitFor(() => expect(calls.filter((c) => c.url.includes('/runtime/list')).length).toBeGreaterThan(0));
    const sent = calls.find((c) => c.url.includes('/runtime/list'))!;
    expect(sent.body.conjunction).toBe('OR');
    expect(sent.body.conditions).toEqual([{ fieldCode: 'balance', operator: 'gte', value: 100 }]);
  });

  it('页脚统计按持久化的配置发聚合请求，并把服务端结果渲染出来', async () => {
    // 与浏览器里验证过的同一条链路：stats -> /runtime/aggregate -> 页脚数值
    window.localStorage.setItem(
      `zlc:state:${APP}:${ENT}:LIST`,
      JSON.stringify({ columnMeta: [], sorts: [], stats: { balance: 'SUM' } }),
    );
    renderGrid();
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    const agg = calls.find((c) => c.url.includes('/runtime/aggregate'))!;
    expect(agg.body.aggregations).toEqual({ balance: ['SUM'] });
    // 总计行必须展示服务端算出来的值，而不是前端拿当前页凑的
    await waitFor(() => expect(screen.getByText('10088.49')).toBeInTheDocument());
  });

  it('统计口径写的是"命中多少行"，不是"这一页有几行"', async () => {
    renderGrid();
    // 页脚与分页器都会显示命中数；关键是它取的是服务端 total，不是当前页条数
    await waitFor(() => expect(screen.getAllByText('共 2 条').length).toBeGreaterThan(0));
    expect(screen.queryByText(/共 20 条/)).not.toBeInTheDocument();
  });

  it('每个统计值落在它自己那一列下面，不能整体错位', async () => {
    window.localStorage.setItem(
      `zlc:state:${APP}:${ENT}:LIST`,
      JSON.stringify({ columnMeta: [], sorts: [], stats: { balance: 'SUM' } }),
    );
    renderGrid();
    await waitFor(() => expect(screen.getByText('10088.49')).toBeInTheDocument());
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent?.trim());
    const cells = Array.from(document.querySelectorAll('.ant-table-summary td')).map((td) => td.textContent ?? '');
    // 表头里 余额 是第几列（含勾选框空列），统计行里带值的那个格子必须在同一个下标
    const headerIndex = headers.indexOf('余额');
    const valueIndex = cells.findIndex((text) => text.includes('10088.49'));
    expect(headerIndex, `表头列序: ${JSON.stringify(headers)}`).toBeGreaterThan(0);
    expect(valueIndex, `统计行列序: ${JSON.stringify(cells)}`).toBe(headerIndex);
  });

  it('内联编辑只提交被改的那一列，并把 id 放进 fieldValues', async () => {
    const user = userEvent.setup();
    renderGrid();
    await waitFor(() => expect(screen.getByText('张三')).toBeInTheDocument());
    // 点单元格进入编辑（GridView 只在 registry 说 inlineEditable 时给出这个入口）
    await user.click(screen.getByText('张三'));
    // 别选中行首的选择框：只认真正可输入的编辑控件
    const boxes = Array.from(document.querySelectorAll<HTMLInputElement>(
      '.ant-table-tbody input:not([type="checkbox"]):not([type="radio"])',
    ));
    const input = boxes[0];
    expect(input, '点单元格后应该出现内联编辑器').toBeDefined();
    if (!input) {
      return;
    }
    await user.clear(input);
    await user.type(input, '张三丰');
    await user.keyboard('{Enter}');
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/update'))).toBe(true));
    const upd = calls.find((c) => c.url.includes('/runtime/update'))!;
    const values = upd.body.fieldValues as Record<string, unknown>;
    expect(values.id).toBe(1);
    expect(values).not.toHaveProperty('balance');
    expect(values).not.toHaveProperty('level');
    expect(Object.keys(values).sort()).toEqual(['customer_name', 'id']);
  });

});

function listCalls() {
  return calls.filter((c) => c.url.includes('/runtime/list')).length;
}

/**
 * `load` 是手写的 `useCallback + useEffect`（不是 react-query），依赖里带着 `state.sorts`
 * 和父组件传进来的 `conditions` —— 这一族是唯一能自己把自己点起重查的写法：
 * 日历就靠同样的形状在挂载后 300ms 内发了 43 次 `/runtime/list`，而**画面完全正常**，
 * 上面那 8 条 UI/请求体断言一条都不会红。react-query 那几处（看板、图表、页脚统计）
 * 不受这类影响：queryKey 是按内容 hash 的，数组换身份不会多打一次。
 */
describe('GridView 一次挂载只查一次', () => {
  it('画出行之后安静期不再自己重查', async () => {
    renderGrid();
    await waitFor(() => expect(screen.getByText('张三')).toBeInTheDocument());
    expect(listCalls(), '挂载之后不该有第二次查询').toBe(1);
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(listCalls(), '安静期不该有后台重查').toBe(1);
  });
});
