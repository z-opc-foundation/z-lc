import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { WorkflowsPage } from '@/lc/views/admin/WorkflowsPage';
import type { WorkflowBindingEntity, WorkflowFireEntity } from '@/lc/api/types';
import { selectValue } from '@/test/antd-select';

/**
 * 流程绑定页的"界面上摆出来的，引擎真的做得到"。
 *
 * 这一页此前的形状是缺陷 #61 的标准标本：触发时机自己抄了一份清单（AFTER_CREATE / AFTER_UPDATE /
 * AFTER_DELETE），其中两个引擎没有挂接点；草稿把 autoSubmit 留成关闭，而运行期按 auto_submit=1
 * 筛绑定 —— 于是"保存成功而一个字都不执行"，写入口现在干脆把它拒成 400。
 * 更糟的是 `/workflow-binding/fires`（唯一能证明"真的发出去了"的那张账）在界面上根本没有出口。
 *
 * 三格各自 mount 一次（jsdom + antd 的挂载在本仓库是墙钟大头，一次渲染多验几点更抗并行抖动）：
 * ① 选项只从 /vocabulary 长出来 + 保存送出的那一坨字节 + 400 的原因要原样回到用户眼前；
 * ② 词表读不出来（HTTP 失败 / 形状漂移）时不许把每一行判成"引擎不兑现"，也不许开一个会挨 400 的草稿；
 * ③ 发起记录的两张脸（跑成了什么样 / 没读到）。
 */

const APP = 'crm';

/** 引擎"报告"的词表：故意掺一个中文表里没有的事件码，来验清单是不是真的从接口长出来的。 */
const REJECT_REASON = '引擎还没有更新后的回调落点，绑了也不会发起流程';
const VOCAB = {
  implemented: ['AFTER_CREATE', 'AFTER_ARCHIVE'],
  rejected: [{ event: 'AFTER_UPDATE', reason: REJECT_REASON }],
};

/** 写入口那句 400 的原文（后端按 autoSubmit / 事件码给的 reasons）。 */
const WRITE_REJECT = 'autoSubmit=0 的绑定不会发起任何流程，写入口不接受这种形态';

function bindingRow(patch: Partial<WorkflowBindingEntity>): WorkflowBindingEntity {
  return {
    id: 21,
    appCode: APP,
    entityCode: 'deal',
    triggerEvent: 'AFTER_CREATE',
    processDefinitionKey: 'leave_approval',
    autoSubmit: 1,
    tenantCode: 'default',
    ...patch,
  };
}

function fireRow(patch: Partial<WorkflowFireEntity>): WorkflowFireEntity {
  return {
    id: 1,
    appCode: APP,
    entityCode: 'deal',
    recordId: 900,
    bindingId: 21,
    triggerEvent: 'AFTER_CREATE',
    processDefinitionKey: 'leave_approval',
    status: 'STARTED',
    instanceId: 'wf-inst-7',
    detail: null,
    createTime: '2026-09-26T10:00:00',
    ...patch,
  };
}

let vocabMode: 'ok' | 'http-500' | 'shape-drift' = 'ok';
/**
 * `/fires` 这一格回什么。`window` 是缺陷 #65 的靶子：库里 25 条、一页 20 条，
 * 界面必须把"一共有多少"和"这一页读出来几条"分开说；`bare-array` 是 #65 之前的形状；
 * `no-total` 是"信封形状对、唯独 total 那一格缺席" —— 顺手补一个 `records.length` 就等于把 #65 复活。
 */
let fireMode: 'ok' | 'broken' | 'empty' | 'window' | 'past-window' | 'bare-array' | 'no-total' = 'ok';
const fireRequests: { page: string | null; size: string | null }[] = [];
let rows: WorkflowBindingEntity[] = [];
const posts: { url: string; body: Record<string, unknown> }[] = [];

function queryOf(url: string): URL {
  return new URL(url, 'http://backend.local');
}

/** 库里 25 条结局账时，服务器按 page/size 切出来的那一页。 */
function fireWindowOf(page: number, size: number) {
  const all: WorkflowFireEntity[] = [];
  for (let i = 1; i <= 25; i++) {
    all.push(fireRow({ id: i, recordId: 1000 + i, instanceId: `wf-inst-${i}` }));
  }
  const from = (page - 1) * size;
  return { records: all.slice(from, from + size), total: all.length, pageNum: page, pageSize: size };
}

function envelope(data: unknown) {
  return { success: true, code: 200, message: null, data };
}

function failure(code: number, msg: string) {
  return { success: false, code, message: msg, data: null };
}

function respond(payload: unknown) {
  const body = JSON.stringify(payload);
  return { ok: true, status: 200, text: async () => body, json: async () => JSON.parse(body) };
}

function stubBackend() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown, init?: { method?: string; body?: string }) => {
      const url = String(input);
      if (init?.method === 'POST') {
        const sent = JSON.parse(String(init.body ?? '{}')) as Record<string, unknown>;
        posts.push({ url, body: sent });
        // 一律先拒: 这一格要同时量到"发出去的是什么"和"拒的原因有没有回到用户眼前"
        if (url.includes('/workflow-binding/create')) return respond(failure(400, WRITE_REJECT));
        return respond(envelope(true));
      }
      if (url.includes('/app/list')) {
        return respond(envelope([{ appCode: APP, appName: 'CRM', tenantCode: 'default' }]));
      }
      if (url.includes('/meta/bundle')) {
        return respond(
          envelope({
            app: { appCode: APP, appName: 'CRM' },
            entities: [{ entityCode: 'deal', entityName: '商机', appCode: APP }],
            dicts: [],
            views: [],
            fieldTypes: [],
          }),
        );
      }
      if (url.includes('/workflow-binding/vocabulary')) {
        if (vocabMode === 'http-500') return respond(failure(500, '词表接口 500'));
        // 形状漂移: implemented 那一栏整个缺席。不能读成"引擎一个事件都不支持"。
        if (vocabMode === 'shape-drift') return respond(envelope({ rejected: VOCAB.rejected }));
        return respond(envelope(VOCAB));
      }
      if (url.includes('/workflow-binding/fires')) {
        const q = queryOf(url);
        const page = Number(q.searchParams.get('page') ?? '1');
        const size = Number(q.searchParams.get('size') ?? '20');
        fireRequests.push({ page: q.searchParams.get('page'), size: q.searchParams.get('size') });
        if (url.includes('entityCode=broken')) return respond(failure(500, '发起记录接口 500'));
        const two = [
          fireRow({}),
          fireRow({
            id: 2,
            recordId: 901,
            status: 'FAILED',
            instanceId: null,
            detail: '连接审批中心超时（250ms 上限）',
          }),
        ];
        if (fireMode === 'bare-array') {
          // 缺陷 #65 之前的形状：裸数组、没有总数。界面不许把它读成"库里就这么多条"，
          // 更不许自己补一个 total = records.length（那等于给截断配一句看起来可信的谎）。
          return respond(envelope(two));
        }
        if (fireMode === 'empty') {
          return respond(envelope({ records: [], total: 0, pageNum: page, pageSize: size }));
        }
        if (fireMode === 'no-total') {
          // 信封在、records 在，只有 total 那一格没了 —— 比裸数组更难发现：
          // 补一个 `total = records.length` 就能让界面"正常运行"，而 #65 那句谎原地复活。
          return respond(envelope({ records: two, pageNum: page, pageSize: size }));
        }
        if (fireMode === 'past-window') {
          // 库里 25 条，而这一页读出来是空的：只有 total 说得出"不是没有账"。
          return respond(envelope({ records: [], total: 25, pageNum: page, pageSize: size }));
        }
        if (fireMode === 'window') return respond(envelope(fireWindowOf(page, size)));
        return respond(envelope({ records: two, total: two.length, pageNum: page, pageSize: size }));
      }
      if (url.includes('/workflow-binding/list')) return respond(envelope(rows));
      return respond(envelope([]));
    }),
  );
}

const mounted: { unmount: () => void }[] = [];

function unmountAll() {
  // 只调 React 的 unmount：抽屉的 portal 是 React 自己挂到 body 的，卸载时会连壳一起摘。
  // 手动清 body 会把 antd 退场动画还引用的节点先摘掉，留下一条"not a child of this node"的
  // 未捕获异常 —— 量具自己制造噪声，比不清还糟。
  while (mounted.length) mounted.pop()?.unmount();
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const result = render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <WorkflowsPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
  mounted.push(result);
  return result;
}

function tagOf(testid: string): HTMLElement {
  const node = document.querySelector(`[data-testid="${testid}"]`);
  if (!node) throw new Error(`页面上找不到 ${testid} —— 这一格根本没渲染出来`);
  return node as HTMLElement;
}

/** 抽屉分页控件上那个「2」。找不到就把抽屉内容吐出来 —— 否则只有一条 null 红，看不出是控件没长出来。 */
async function openSecondPage(): Promise<HTMLElement> {
  await waitFor(() => {
    const found = Array.from(document.querySelectorAll('.ant-drawer .ant-pagination-item')).find(
      (node) => (node.textContent ?? '').trim() === '2',
    );
    if (!found) {
      const drawer = document.querySelector('.ant-drawer');
      throw new Error(
        `抽屉里没有"第 2 页"这个可点的页码 —— 分页控件根本没长出来。当前抽屉内容：${drawer?.innerHTML.slice(0, 600) ?? '(整个抽屉都不在)'}`,
      );
    }
  });
  return Array.from(document.querySelectorAll('.ant-drawer .ant-pagination-item')).find(
    (node) => (node.textContent ?? '').trim() === '2',
  ) as HTMLElement;
}

function buttonByText(label: string): HTMLButtonElement {
  const button = Array.from(document.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === label,
  );
  if (!button) {
    const seen = Array.from(document.querySelectorAll('button')).map((node) =>
      (node.textContent ?? '').replace(/\s/g, ''),
    );
    throw new Error(`没找到「${label}」按钮，实际: ${seen.join(' / ')}`);
  }
  return button as HTMLButtonElement;
}

function clickButton(label: string) {
  const button = buttonByText(label);
  if (button.disabled) throw new Error(`「${label}」是禁用的`);
  fireEvent.click(button);
}

/** antd Select 要 mouseDown 才展开，弹层挂在 body 上。同一个下拉框只开一次：再 mouseDown 会把它收回去。 */
async function selectOptions(index: number): Promise<string[]> {
  const selector = document.querySelectorAll('.ant-modal .ant-select-content')[index];
  if (!selector) throw new Error(`第 ${index + 1} 个下拉框没渲染出来`);
  fireEvent.mouseDown(selector);
  await waitFor(() =>
    expect(document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').length).toBeGreaterThan(0),
  );
  return Array.from(
    document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
  ).map((node) => node.textContent ?? '');
}

/**
 * 「新建绑定」在应用/词表没读出来时是禁用的：等它可用再点，别把"没点开"当成"页面坏了"。
 * 实体清单也要等到 —— 草稿的 entityCode 取的是它的第一项，没到就会带一个空实体开草稿。
 */
async function openDraft() {
  await waitFor(() => expect(buttonByText('新建绑定').disabled).toBe(false));
  fireEvent.click(buttonByText('新建绑定'));
  await waitFor(() => {
    expect(document.querySelectorAll('.ant-modal .ant-select-content').length).toBe(2);
    expect(selectValue(document, 0)).toBe('商机');
  });
}

/** 词表那一条告警里的「重试」—— 点它要走完整条重读路径，不是把 banner 藏起来。 */
function clickVocabularyRetry() {
  const alert = Array.from(document.querySelectorAll('.ant-alert')).find((node) =>
    (node.textContent ?? '').includes('触发时机词表'),
  );
  if (!alert) throw new Error('没有「触发时机词表没有读到」这一条告警');
  const retry = Array.from(alert.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === '重试',
  );
  if (!retry) throw new Error('词表告警里没有「重试」按钮');
  fireEvent.click(retry);
}

async function toasts(): Promise<string> {
  await waitFor(() => {
    expect(document.querySelectorAll('.ant-message-notice').length).toBeGreaterThan(0);
  });
  return Array.from(document.querySelectorAll('.ant-message-notice'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

function alerts(): string {
  return Array.from(document.querySelectorAll('.ant-alert'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

beforeEach(() => {
  // 每条用例从干净盘面起跑：上一例留下的抽屉会把它的 /fires 请求记到这一例的账上，
  // 而"一次打开只该发一次请求"这一格量的正是请求条数。
  unmountAll();
  posts.length = 0;
  rows = [];
  vocabMode = 'ok';
  fireMode = 'ok';
  fireRequests.length = 0;
  stubBackend();
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

describe('WorkflowsPage', () => {
  it('触发时机只有接口报告能兑现的那几个，而保存送出去的是引擎跑得起来的形态', async () => {
    rows = [
      bindingRow({ id: 21 }),
      // 库里确实躺着后端上线之前写进去的写后事件 —— 界面要说出"这条不会发起"，并带上引擎给的原因
      bindingRow({ id: 22, entityCode: 'contract', triggerEvent: 'AFTER_UPDATE' }),
    ];
    renderPage();

    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-trigger-21"]').length).toBe(1));
    expect(tagOf('workflow-trigger-21').textContent).toBe('创建后');
    expect(tagOf('workflow-trigger-21').className).not.toContain('ant-tag-red');
    expect(tagOf('workflow-trigger-22').textContent).toBe('AFTER_UPDATE 引擎不兑现');
    expect(tagOf('workflow-trigger-22').className).toContain('ant-tag-red');

    await openDraft();

    // 选择项 = 接口那份 implemented 的原样顺序，一个不多一个不少：
    // AFTER_ARCHIVE 只有接口才知道（页面自己抄不出来），AFTER_UPDATE 是引擎明确不兑现的。
    const options = await selectOptions(1);
    expect(options).toEqual(['创建后', 'AFTER_ARCHIVE']);
    const joined = options.join('|');
    for (const ghost of ['AFTER_UPDATE', 'AFTER_DELETE', 'BEFORE_CREATE']) {
      expect(joined.includes(ghost), `${ghost} 在引擎里没有挂接点，不该能选`).toBe(false);
    }

    // 不兑现的时机不是"藏起来"就算完：界面上要指名它为什么不行（引擎的原话，不是前端编的）
    expect(tagOf('workflow-rejected-AFTER_UPDATE').textContent).toContain(REJECT_REASON);

    // 草稿默认那份：时机取引擎清单的第一项，autoSubmit 必须是 1（写入口现在拒 0）
    expect(selectValue(document, 1)).toBe('创建后');

    const keyInput = document.querySelector('.ant-modal-body input[placeholder="例如 leave_approval"]');
    if (!keyInput) throw new Error('流程定义 Key 的输入框没渲染出来');
    fireEvent.change(keyInput, { target: { value: 'leave_approval' } });
    clickButton('保存');

    await waitFor(() => expect(posts.some((post) => post.url.includes('/workflow-binding/create'))).toBe(true));
    const body = posts.find((post) => post.url.includes('/workflow-binding/create'))?.body as Record<string, unknown>;
    expect(body.appCode).toBe(APP);
    expect(body.entityCode).toBe('deal');
    expect(body.triggerEvent).toBe('AFTER_CREATE');
    expect(body.autoSubmit, 'autoSubmit 送 0 = 一条永远不发起的绑定，而这正是旧草稿的默认值').toBe(1);
    expect(body.processDefinitionKey).toBe('leave_approval');

    // 后端给 400 时，原因必须原样回到用户眼前，并留在原地改
    const toast = await toasts();
    expect(toast).toContain('autoSubmit=0');
    expect(toast.includes('保存失败'), '只说"保存失败"等于没说 —— 400 的原因在后端 message 里').toBe(false);
    expect(document.querySelector('.ant-modal-body')).toBeTruthy();

    // 页面那句边界文案是契约层钉住的口径（批量导入/撤销/重做不发单）写在界面上的那份影子
    expect(document.body.textContent).toContain('只有新建单条记录会发起');
    expect(document.body.textContent).toContain('批量导入与撤销/重做都不会');
  }, 90_000);

  it('词表读不出来时不猜：不判数据有罪，也不开一个会挨 400 的草稿', async () => {
    vocabMode = 'http-500';
    rows = [bindingRow({ id: 21 }), bindingRow({ id: 22, triggerEvent: 'AFTER_UPDATE' })];
    renderPage();

    // 列表本身是好的：只许出现"词表没有读到"这一句，不许顺手把绑定列表也说成读不到
    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-trigger-21"]').length).toBe(1));
    await waitFor(() => expect(alerts()).toContain('触发时机词表没有读到'));
    expect(alerts()).toContain('词表接口 500');
    expect(alerts().includes('流程绑定没有读到'), '绑定列表读成功了').toBe(false);

    // 量具坏了不能算数据有问题 —— 两行都只能标"时机未校对"，一个红 Tag 都不许有
    expect(tagOf('workflow-trigger-21').textContent).toBe('创建后 时机未校对');
    expect(tagOf('workflow-trigger-22').textContent).toBe('AFTER_UPDATE 时机未校对');
    expect(document.querySelectorAll('.ant-tag-red').length).toBe(0);

    // 没有可信清单时不许开一个会写出 400 的草稿
    expect(buttonByText('新建绑定').disabled).toBe(true);

    // 重试真的重读: 换成"接口回了但形状不对"这一种失败, 同样不许读成"引擎没有可兑现的事件"
    vocabMode = 'shape-drift';
    clickVocabularyRetry();
    await waitFor(() => expect(alerts()).toContain('implemented 不是数组'));
    expect(buttonByText('新建绑定').disabled).toBe(true);
    expect(tagOf('workflow-trigger-21').textContent).toBe('创建后 时机未校对');
  }, 90_000);

  it('发起记录：绑定存在不等于流程发出去了，这一页要能翻到那张账', async () => {
    rows = [bindingRow({ id: 31, entityCode: 'deal' }), bindingRow({ id: 32, entityCode: 'broken' })];
    renderPage();

    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));

    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-status-STARTED"]').length).toBe(1));
    expect(document.querySelector('.ant-drawer-title')?.textContent).toBe('deal 的发起记录');
    const startedRow = document.querySelector('[data-testid="fire-status-STARTED"]')?.closest('tr');
    expect(startedRow?.textContent).toContain('wf-inst-7');
    expect(startedRow?.textContent).toContain('900');
    // 失败那一条的 detail 是"为什么没发出去"的唯一去处，不能只给一个红 Tag
    const failedRow = document.querySelector('[data-testid="fire-status-FAILED"]')?.closest('tr');
    expect(failedRow?.textContent).toContain('250ms');
    expect(failedRow?.textContent).not.toContain('wf-inst-7');

    fireEvent.click(document.querySelector('.ant-drawer-close') as Element);

    // 接口没读到 ≠ 没有发起记录：这两句必须分开，否则用户以为自己的流程没跑过
    fireEvent.click(tagOf('workflow-fires-32'));
    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-load-error"]').length).toBe(1));
    expect(tagOf('fire-load-error').textContent).toContain('发起记录没有读到');
    expect(tagOf('fire-load-error').textContent).toContain('发起记录接口 500');
    expect(document.body.textContent?.includes('这个实体还没有发起记录'), '没读到不能读成没有').toBe(false);
  }, 90_000);

  it('发起记录的第一页：「一共有多少」与「这一页读出来几条」是两句话（缺陷 #65）', async () => {
    // 这一例只管**说**：总数、页码、这一页的行数、以及"另外几条没读在这一页里"那句差额。
    // 翻页那一段单独一例 —— 两件事挤在一格里，注入时两支会红在同一个标题下，判据分不开。
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    fireMode = 'window';
    renderPage();

    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));

    // 起跑时那一页：库里 25 条，接口按 size=20 只给 20 条
    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-window-line"]').length).toBe(1));
    expect(fireRequests.length, '一次打开只该发一次请求').toBe(1);
    expect(fireRequests[0]).toEqual({ page: '1', size: '20' });
    expect(tagOf('fire-window-line').textContent).toContain('25');
    expect(tagOf('fire-window-line').textContent).toContain('第 1 页');
    expect(document.querySelectorAll('.ant-drawer [data-testid="fire-status-STARTED"]').length).toBe(20);
    // 这一页不是全部 —— 少了这一句，"20 行"和"25 条账"的差就又隐身了（#65 的原始形状）
    expect(tagOf('fire-window-partial').textContent).toContain('没有读在这一页里');
    expect(tagOf('fire-window-partial').textContent).toContain('5 条');
  }, 90_000);

  it('翻页要真的换一次请求：点第 2 页得带着页码重读，而不是把第 1 页再画一遍（缺陷 #65）', async () => {
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    fireMode = 'window';
    renderPage();

    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));
    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-window-line"]').length).toBe(1));
    expect(fireRequests.length).toBe(1);

    const pager = await openSecondPage();
    fireEvent.click(pager.querySelector('a') ?? pager);
    await waitFor(() => expect(fireRequests.length).toBe(2));
    expect(fireRequests[1]).toEqual({ page: '2', size: '20' });
    await waitFor(() =>
      expect(document.querySelectorAll('.ant-drawer [data-testid="fire-status-STARTED"]').length).toBe(5),
    );
    expect(tagOf('fire-window-line').textContent).toContain('第 2 页');
    expect(tagOf('fire-window-line').textContent).toContain('25');
  }, 90_000);

  it('空页 ≠ 没有账；真的没有账才配说「还没有发起记录」', async () => {
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    fireMode = 'past-window';
    renderPage();
    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));

    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-empty-past-the-end"]').length).toBe(1));
    expect(tagOf('fire-empty-past-the-end').textContent).toContain('25');
    // 总数那一格在**任何**一页都该是库里的总数，不是"这一页读出来几条"。
    // 没有这一句，U3（把「共 N 条」画成 rows.length）与 U4（摘掉差额提示）红在同一格标题下，
    // 判据就分不开两种修法 —— 而那是两种不同的谎。
    expect(tagOf('fire-window-line').textContent).toContain('25');
    expect(document.body.textContent?.includes('这个实体还没有发起记录'), '有 total 就不许说"还没有"').toBe(false);

    // 反方向：total 真的是 0 时，那句"还没有"必须说得出（否则这一族改动会把它一路删成谁都不说）
    unmountAll();
    fireMode = 'empty';
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    renderPage();
    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));
    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-empty-none"]').length).toBe(1));
    expect(document.querySelectorAll('[data-testid="fire-window-partial"]').length).toBe(0);
    expect(document.querySelectorAll('[data-testid="fire-empty-past-the-end"]').length).toBe(0);
  }, 90_000);

  it('接口退回 #65 之前那个裸数组 ⇒ 判"没有读到"，不许把行数当总数', async () => {
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    fireMode = 'bare-array';
    renderPage();
    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));

    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-load-error"]').length).toBe(1));
    expect(tagOf('fire-load-error').textContent).toContain('裸数组');
    expect(document.querySelectorAll('[data-testid="fire-status-STARTED"]').length).toBe(0);
    expect(document.body.textContent?.includes('这个实体还没有发起记录'), '读不出来不能读成没有').toBe(false);
  }, 90_000);

  it('total 那一格缺席 ⇒ 判"没有如实回总数"，不许拿这一页的行数补一个总数', async () => {
    // 这一例是 U2 那支注入的猎物：`readFireWindow` 里把 `total` 缺省成 `records.length`
    // 看起来什么都对（两行就写"共 2 条"），只有这一格能把它读成谎。
    rows = [bindingRow({ id: 31, entityCode: 'deal' })];
    fireMode = 'no-total';
    renderPage();
    await waitFor(() => expect(document.querySelectorAll('[data-testid="workflow-fires-31"]').length).toBe(1));
    fireEvent.click(tagOf('workflow-fires-31'));

    await waitFor(() => expect(document.querySelectorAll('[data-testid="fire-load-error"]').length).toBe(1));
    expect(tagOf('fire-load-error').textContent).toContain('没有如实回总数');
    expect(document.body.textContent?.includes('一共有'), '补出来的总数不许上界面').toBe(false);
    expect(document.querySelectorAll('[data-testid="fire-status-STARTED"]').length).toBe(0);
    expect(document.body.textContent?.includes('这个实体还没有发起记录'), '读不出来不能读成没有').toBe(false);
  }, 90_000);
});
