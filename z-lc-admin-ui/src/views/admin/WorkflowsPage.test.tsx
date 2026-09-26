import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { WorkflowsPage } from '@/views/admin/WorkflowsPage';
import type { WorkflowBindingEntity, WorkflowFireEntity } from '@/api/types';

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
let rows: WorkflowBindingEntity[] = [];
const posts: { url: string; body: Record<string, unknown> }[] = [];

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
        if (url.includes('entityCode=broken')) return respond(failure(500, '发起记录接口 500'));
        return respond(
          envelope([
            fireRow({}),
            fireRow({
              id: 2,
              recordId: 901,
              status: 'FAILED',
              instanceId: null,
              detail: '连接审批中心超时（250ms 上限）',
            }),
          ]),
        );
      }
      if (url.includes('/workflow-binding/list')) return respond(envelope(rows));
      return respond(envelope([]));
    }),
  );
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <WorkflowsPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function tagOf(testid: string): HTMLElement {
  const node = document.querySelector(`[data-testid="${testid}"]`);
  if (!node) throw new Error(`页面上找不到 ${testid} —— 这一格根本没渲染出来`);
  return node as HTMLElement;
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
  const selector = document.querySelectorAll('.ant-modal .ant-select-selector')[index];
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
    expect(document.querySelectorAll('.ant-modal .ant-select-selector').length).toBe(2);
    expect(document.querySelectorAll('.ant-modal .ant-select-selection-item')[0]?.textContent).toBe('商机');
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
    expect(document.querySelectorAll('.ant-message-notice-content').length).toBeGreaterThan(0);
  });
  return Array.from(document.querySelectorAll('.ant-message-notice-content'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

function alerts(): string {
  return Array.from(document.querySelectorAll('.ant-alert'))
    .map((node) => node.textContent ?? '')
    .join(' | ');
}

beforeEach(() => {
  posts.length = 0;
  rows = [];
  vocabMode = 'ok';
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
    expect(document.querySelectorAll('.ant-modal .ant-select-selection-item')[1]?.textContent).toBe('创建后');

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
});
