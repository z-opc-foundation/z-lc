import { message } from 'antd';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DesignerPage } from '@/lc/views/designer/DesignerPage';
import type { ProvisionItem, ProvisionReport } from '@/lc/api/admin';
import type { EntityDefDTO } from '@/lc/api/types';

/**
 * provision 的每一个状态在界面上必须长得不一样（缺陷 #43 的前端半边，#47 又加了一态）。
 *
 * 后端旧口径把**那条 DDL 的文本**当返回值：`CREATE TABLE IF NOT EXISTS` 打在一张已经在的表上
 * 是**空操作**，于是两个实体抢同一张物理表时 HTTP 200、DDL 字符串照发，而这份定义声明的列
 * 一列都没落地。前端那两层把谎说圆了：
 *  ① `provision()` 只要不抛异常就弹「物理表已 provision」；
 *  ② `provisionAll()` 弹 `已 provision ${Object.keys(result).length} 张表` —— 那个 Map 的键是
 *     entityCode，**坏的那支也有键**，所以"没建成"永远被算进"已建成"。
 * 现在后端逐项回 status/missingColumns，这里钉的是：成功话术必须由 status 决定，计数必须由
 * 服务端的 created/unchanged/failedCount 决定，且没建成的那些要**常驻**可见（toast 三秒就收，
 * 用户回头只看到一个没建成的表和一份"成功"记录）。
 *
 * 每条用例只钉一句话，由 e2e/mutate_provision_report_guard.py 的注入逐个反证
 * （每支的预期红集合都不同 —— 用例标题就是那本分母）。#43 那 12 条对应 M1–M14。
 *
 * 缺陷 #47 之后多出来的第四态是 `ALTERED`：表本来就在，但定义跑到表前面了（新增过栏），
 * provision 这一次**真的执行了 ALTER TABLE ADD COLUMN**。它不能并进 EXISTS_INTACT ——
 * 并进去之后界面会对一张刚被改过的表说"这次没有执行 DDL"，那是第三句假话。
 * 末尾三条（T13–T15）钉的就是这一态，对应注入 M15–M18。
 */

const APP = 'crm';

/** 桩后端这一次 provision 的结论；每条用例自己设。 */
let singleVerdict: ProvisionItem | null = null;
let batchReport: ProvisionReport | null = null;
let provisionCalls: string[] = [];

function entity(id: number, code: string, name: string): EntityDefDTO {
  return {
    id,
    entityCode: code,
    entityName: name,
    tableName: `t_${code}`,
    tenantCode: 'default',
    appCode: APP,
    fields: [{ fieldCode: 'title', fieldName: '标题', fieldType: 'STRING', sortOrder: 0 }],
  } as EntityDefDTO;
}

function item(entityCode: string, over: Partial<ProvisionItem> = {}): ProvisionItem {
  return {
    entityCode,
    tableName: `t_${entityCode}`,
    status: 'CREATED',
    ddl: 'CREATE TABLE IF NOT EXISTS `t_x` (`id` BIGINT)',
    message: '已按这份定义建表',
    missingColumns: [],
    ...over,
  };
}

/**
 * 一支没建成的：形状与实测一致（占表的旧表缺这份定义声明的 amount 列）。
 *
 * message 里**故意不带**实体编码、表名、列名 —— 后端真实文案是带的。三条断言各认一个来源：
 * 实体编码只能来自 `<Text code>`，缺列只能来自 `missingColumns` 那一段，表名只能来自 toast。
 * 否则 message 自己就把三条喂饱了，删掉任何一处渲染都照样绿（"负向断言要有猎物"的正向版）。
 */
function broken(entityCode = 'invoice'): ProvisionItem {
  return item(entityCode, {
    status: 'FAILED',
    // 服务端真的回过一条 DDL 时，面板该显示**那一条**（这一串就是标记），
    // 而不是本地 previewDdl 生成的那一份"看起来会建 amount 列"的猜想。
    ddl: 'CREATE TABLE IF NOT EXISTS `t_invoice` (`probe_server_ddl` VARCHAR(32))',
    message: '物理表已存在，但这份定义声明的列没有全部建出来，先看是不是有别的实体占了同一张表',
    missingColumns: ['amount'],
  });
}

/**
 * 一支"表本来就在，但按定义补了列"的（缺陷 #47 之后多出来的第四态）。
 *
 * 和 {@link broken} 同一套夹具纪律：message 里**不带**列名、也不带"补过"这两个字，
 * 于是界面只要还说「列一列不缺」或漏掉清单，就必然红 —— 读数额外落在 `addedColumns` 上，
 * 这是"这次真的执行过 DDL"这句话唯一的证据来源。
 */
function altered(entityCode = 'deal', addedColumns = ['amount', 'stage']): ProvisionItem {
  return item(entityCode, {
    status: 'ALTERED',
    addedColumns,
    ddl: 'CREATE TABLE IF NOT EXISTS `t_deal` (`id` BIGINT)\n'
      + 'ALTER TABLE `t_deal` ADD COLUMN `amount` DECIMAL(18,2)',
    message: '表本来就在，按这份定义补了列（只加列，不动已有列、不改类型、不删数据）',
  });
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

function report(items: ProvisionItem[]): ProvisionReport {
  const created = items.filter((one) => one.status === 'CREATED').length;
  const unchanged = items.filter((one) => one.status === 'EXISTS_INTACT').length;
  const altered = items.filter((one) => one.status === 'ALTERED').length;
  const failedCount = items.length - created - unchanged - altered;
  return {
    appCode: APP,
    total: items.length,
    created,
    unchanged,
    altered,
    failedCount,
    allOk: failedCount === 0,
    items,
  };
}

function stubBackend() {
  provisionCalls = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      await new Promise((resolve) => setTimeout(resolve, 0));
      if (url.includes('/admin/entity/provision') || url.includes('/admin/app/entity/provision')) {
        provisionCalls.push(`one ${url}`);
        return envelope(singleVerdict);
      }
      if (url.includes('/admin/app/provision-all')) {
        provisionCalls.push(`all ${url}`);
        return envelope(batchReport);
      }
      if (url.includes('/admin/app/entity/list')) {
        return envelope([entity(11, 'deal', '商机'), entity(12, 'lead', '线索')]);
      }
      if (url.includes('/app/list')) return envelope([{ appCode: APP, appName: 'CRM' }]);
      if (url.includes('/meta/bundle')) return envelope(null, false);
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

function toastText(): string {
  return Array.from(document.querySelectorAll('.ant-message-notice'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

/**
 * 页面内的读数一律限定在**本次 render 的容器**里查。
 *
 * 这个目录下的用例每支都 render 一棵新树，而 RTL 的自动 cleanup 在本仓没注册
 * （`DesignerFieldCode.test.tsx` 那份靠"前面几支不产生同类节点"侥幸躲过）。整页
 * `document.querySelectorAll('.ant-alert')` 于是会把上一支残留的那棵树一起读进来 ——
 * 实测症状：CREATED 那一支读到前一分支留下的「未建成（1）」，红得像是产品坏了。
 * toast 只能按全局读（antd 挂在 body 上），那一类靠每次点之前 destroy 一次兜住。
 */
let root: HTMLElement = document.createElement('div');

function alertText(): string {
  return Array.from(root.querySelectorAll('.ant-alert'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

/** DDL 那张卡片整块的文字（pre + 那只状态 Tag）。 */
function ddlCardText(): string {
  const pre = root.querySelector('.zlc-ddl');
  return pre?.parentElement?.textContent ?? '';
}

function buttons(): HTMLButtonElement[] {
  return Array.from(root.querySelectorAll('button'));
}

function clickButtonByText(label: string) {
  const button = buttons().find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === label,
  );
  if (!button) {
    const seen = buttons().map((node) => JSON.stringify((node.textContent ?? '').trim()));
    throw new Error(`没有 ${label} 这个按钮，页面上的按钮有 [${seen.join(', ')}]`);
  }
  expect(button, `${label} 不该是按住的`).toBeEnabled();
  fireEvent.click(button);
}

/**
 * 打开设计器并等它停在 `deal` 上。
 *
 * 中间那两句是夹具自证：两颗 provision 按钮都得**只有一颗**真实存在，否则"点了 Provision
 * 却没红"可能只是"根本没点到"。antd 的 toast 挂在 body 上、不在容器里，跨用例会残留，
 * 所以每次点之前先 destroy 一次 —— 不然上一条用例的成功话术会被下一条读成自己的读数。
 */
async function openDesigner() {
  const tree = render(designerTree());
  root = tree.container;
  await waitFor(() => expect(screen.getByText('商机')).toBeTruthy());
  await waitFor(() => expect(buttons().filter(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === 'Provision',
  )).toHaveLength(1));
  expect(buttons().filter(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === 'Provision全部实体',
  ), '侧栏那颗批量 provision 按钮要在').toHaveLength(1);
  message.destroy();
  return tree;
}

const settle = () => new Promise((resolve) => setTimeout(resolve, 150));

beforeEach(() => {
  vi.unstubAllGlobals();
  singleVerdict = null;
  batchReport = null;
  stubBackend();
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

describe('单实体 provision：四态四话术', () => {
  it('HTTP 200 但 status=FAILED：一句"已建成"的话都不许出现', async () => {
    singleVerdict = broken('deal');
    await openDesigner();

    clickButtonByText('Provision');
    // 这一支**不看横幅**：先确认请求真的发出去了（桩里同步记账），再等一会儿读 toast。
    // 如果拿"等横幅出现"当前置，摘掉横幅接线会让这里以"没等到"红掉，
    // 而这句话管的是"成功话术出没出没"，两件事得分开红。
    await waitFor(() => expect(provisionCalls).toHaveLength(1));
    await settle();
    expect(toastText(), `一次 provision 都没弹出来，读数无意义: ${toastText()}`).not.toBe('');
    expect(toastText()).not.toMatch(/已按这份定义建出物理表|表本来就在|物理表已 provision/);
  });

  it('没建成的那个实体要被点名（不点名就没人知道该改哪一份定义）', async () => {
    singleVerdict = broken('deal');
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(alertText()).toContain('未建成'));
    // 点的是侧栏第 0 行 = deal，所以这里被点名的必须是 deal（不是别的实体的编码）。
    expect(alertText()).toContain('deal');
  });

  it('缺列清单要写出来：光说"没建成"，用户不知道该动哪一栏', async () => {
    singleVerdict = broken('deal');
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(alertText()).toContain('未建成'));
    expect(alertText()).toContain('amount');
  });

  it('DDL 面板不许把没建成说成"这是服务端实际执行的 DDL"', async () => {
    singleVerdict = broken('deal');
    await openDesigner();

    clickButtonByText('Provision');
    // 这一支原来无论成败都是绿色那句 —— DDL 确实发出去了，但它什么都没建成。
    await waitFor(() => expect(ddlCardText()).toContain('这张表没有建成'));
    expect(ddlCardText()).not.toContain('这是服务端实际执行的 DDL');
    // settle 之后再核一次：provision 完必定 reload 一次实体清单，而清单刷新过去
    // 把结论冲掉的话，面板会**悄悄**退回本地 previewDdl —— 于是"服务端到底执行了什么"又变回猜测。
    await settle();
    expect(ddlCardText(), '面板要留着服务端回的那条 DDL，不许退回本地预览')
      .toContain('probe_server_ddl');
  });

  it('CREATED：说清是按这份定义建出来的哪张表，并且不留「未建成」横幅', async () => {
    singleVerdict = item('deal', { status: 'CREATED' });
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(toastText()).toContain('已按这份定义建出物理表'));
    expect(toastText()).toContain('t_deal');
    expect(alertText()).not.toContain('未建成');
  });

  it('EXISTS_INTACT：要说"表本来就在、没执行建表"，不许混成"已建出"', async () => {
    singleVerdict = item('deal', {
      status: 'EXISTS_INTACT',
      message: '表本来就在，列一列不缺，跳过建表',
    });
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(toastText()).toContain('表本来就在'));
    // 把"核对完、跳过"说成"这次建了一张表"，是同一族谎的第二个方向。
    expect(toastText()).not.toContain('已按这份定义建出物理表');
    expect(alertText()).not.toContain('未建成');
    expect(ddlCardText(), 'Tag 也要说这条 DDL 是空操作').toContain('空操作');
  });

  it('ALTERED：要说清补了哪几列，不许退回"列一列不缺"', async () => {
    singleVerdict = altered('deal', ['amount', 'stage']);
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(toastText()).toContain('补了 2 列'));
    // 列名只能来自 addedColumns（夹具的 message 里故意没有它们）。
    expect(toastText()).toContain('amount');
    expect(toastText()).toContain('stage');
    // 这句在 ALTERED 上是假的：表本来就在，但这一趟真的执行了 ALTER。
    expect(toastText()).not.toContain('列一列不缺');
    expect(toastText()).not.toContain('已按这份定义建出物理表');
    expect(alertText()).not.toContain('未建成');
  });

  it('ALTERED 的 DDL 面板要说这次真的执行过，不许说成"空操作"', async () => {
    singleVerdict = altered('deal');
    await openDesigner();

    clickButtonByText('Provision');
    await waitFor(() => expect(ddlCardText()).toContain('真的执行过'));
    expect(ddlCardText()).not.toContain('空操作');
    expect(ddlCardText(), '面板要留着服务端回的那条 ALTER').toContain('ALTER TABLE');
  });
});

describe('provision-all：计数与横幅只能来自服务端的四态汇总', () => {
  it('三个实体一个没建成：要说清"有 1 个没建成"，不许报整体成功', async () => {
    batchReport = report([item('deal'), item('lead', { status: 'EXISTS_INTACT' }), broken()]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(toastText()).toContain('1 个实体没建成'));
    // 旧口径的 Object.keys(报告).length 在这个响应下数出 7（报告本身的键数），
    // 按 items 数出 3 —— 两者都在宣布"都 provision 好了"。
    expect(toastText()).not.toMatch(/已 provision\s*\d+\s*张新表/);
    expect(alertText()).toContain('invoice');
  });

  it('好的那两支不许被连坐进「未建成」横幅', async () => {
    batchReport = report([item('deal'), item('lead', { status: 'EXISTS_INTACT' }), broken()]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(alertText()).toContain('invoice'));
    expect(alertText()).not.toContain('deal');
    expect(alertText()).not.toContain('lead');
    expect(alertText(), '横幅只该有一块，且只数坏的那一支').toContain('未建成（1）');
  });

  it('全部建成：新表数取服务端的 created，不许拿实体数凑', async () => {
    batchReport = report([item('deal'), item('lead', { status: 'EXISTS_INTACT' }),
      item('invoice', { status: 'EXISTS_INTACT' })]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(toastText()).toContain('1 张新表'));
    expect(toastText()).not.toMatch(/已 provision\s*[237]\s*张/);
    expect(alertText()).not.toContain('未建成');
  });

  it('"本来就在"要单独数（unchanged），不能合成一个更大的成功数', async () => {
    batchReport = report([item('deal'), item('lead', { status: 'EXISTS_INTACT' }),
      item('invoice', { status: 'EXISTS_INTACT' })]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(toastText()).toContain('2 张表本来就在'));
    expect(toastText()).not.toContain('3 张新表');
  });

  it('补过列的表要单独数（altered），不许并进新表或"本来就在"', async () => {
    // 三个数刻意各不相同（1 新 / 1 原位 / 2 补列）：凑成一样的数，
    // "那一句取的是 report.altered" 和 "取的是 report.created" 在界面上就长得一样，
    // 这条断言会空跑（M18 那一支注入正是取错数，靠这个夹具才有猎物）。
    batchReport = report([item('deal'), item('lead', { status: 'EXISTS_INTACT' }),
      item('invoice', { status: 'ALTERED', addedColumns: ['amount'] }),
      item('task', { status: 'ALTERED', addedColumns: ['stage'] })]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(toastText()).toContain('2 张表按定义补了列'));
    expect(toastText()).toContain('1 张新表');
    expect(toastText()).toContain('1 张表本来就在');
    expect(toastText()).not.toMatch(/[23] 张新表/);
    expect(alertText()).not.toContain('未建成');
  });

  it('应用一个实体都没有：那是"没有要建的表"，不是"provision 完成"', async () => {
    batchReport = report([]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(toastText()).toContain('这个应用还没有实体'));
    expect(toastText()).not.toMatch(/已 provision/);
  });
});

describe('provision 的调用形状', () => {
  it('批量按钮打的是 provision-all，一次点击只发一次请求', async () => {
    batchReport = report([item('deal')]);
    await openDesigner();

    clickButtonByText('Provision全部实体');
    await waitFor(() => expect(provisionCalls.filter((line) => line.startsWith('all'))).toHaveLength(1));
    expect(provisionCalls.filter((line) => line.startsWith('one'))).toHaveLength(0);
    await settle();
  });
});
