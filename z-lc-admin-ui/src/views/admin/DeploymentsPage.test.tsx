import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { DeploymentsPage } from '@/views/admin/DeploymentsPage';
import type { DeploymentDTO } from '@/api/types';

/**
 * 部署页的"界面上摆出来的，服务器真的做得到"（缺陷 #70）。
 *
 * 这一页此前的形状：方式下拉自己抄了三种（热加载 / Docker 镜像 / Git 推送），点任何一种都只是
 * insert 一行 PENDING 然后 `// TODO: 异步执行`，而全仓没有执行器 —— 于是界面弹「部署已创建」、
 * 状态列永远灰、日志抽屉永远写「（暂无日志）」。
 *
 * 三格各自 mount 一次（jsdom + antd 的挂载在本仓库是墙钟大头，一次渲染多验几点更抗并行抖动）：
 * ① 选项只从 /deployment/vocabulary 长出来 + 「开始部署」送出去的就是选中的那一种；
 * ② 部署没做成时报的是失败与服务器写下的那句原因（这一条是 #70 的本体），400 的原话要回到眼前；
 * ③ 词表读不出来（HTTP 失败 / 形状漂移）时不许开一个只会挨 400 的草稿。
 */

const APP = 'crm';

/** 服务器"报告"的词表：掺一个前端从没见过的编码，用来验清单是不是真的从接口长出来的。 */
const DOCKER_REASON = '服务器不做镜像构建：仓内没有 docker client、也没有镜像仓库的凭据';
const VOCAB = {
  executable: ['HOT_LOAD', 'BLUE_GREEN'],
  rejected: [{ type: 'DOCKER', reason: DOCKER_REASON }],
};

/** 写入口那句 400 的原文（缺 appCode / 不存在或属于别的应用的物化批次）。 */
const WRITE_REJECT = '物化批次 999999 不存在，没有可以落地的定义';

const SUCCESS_LOG = '应用 [crm] 的定义已应用到运行时库：2 个实体 —— 新建 1 张，补列 1 张，本来就是对的 0 张，没建成 0 张\ndeal: 补列 amount, owner';
const FAILED_LOG =
  '应用 [crm] 的定义没有全部落地：2 个实体 —— 新建 0 张，补列 0 张，本来就是对的 1 张，没建成 1 张\n[FAILED] deal —— 表 deal 缺引擎自建列 tenant_code';

let vocabMode: 'ok' | 'http-500' | 'shape-drift' = 'ok';
let createMode: 'success' | 'failed' | 'http-400' = 'success';
let rows: DeploymentDTO[] = [];
const posts: { url: string; body: Record<string, unknown> }[] = [];

function deploymentRow(patch: Partial<DeploymentDTO>): DeploymentDTO {
  return {
    id: 7,
    appCode: APP,
    deployType: 'HOT_LOAD',
    status: 'SUCCESS',
    deployLog: SUCCESS_LOG,
    version: null,
    materializationId: null,
    tenantCode: 'default',
    createTime: '2026-09-27T09:00:00',
    ...patch,
  };
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
        if (url.includes('/deployment/create')) {
          if (createMode === 'http-400') return respond(failure(400, WRITE_REJECT));
          // 返回的就是执行后的那一行：deployType 原样带回收件箱，好比对"送出的"与"报回来的"是不是同一种。
          return respond(
            envelope(
              createMode === 'failed'
                ? deploymentRow({ id: 9, status: 'FAILED', deployLog: FAILED_LOG, deployType: String(sent.deployType) })
                : deploymentRow({ id: 8, deployLog: SUCCESS_LOG, deployType: String(sent.deployType) }),
            ),
          );
        }
        return respond(envelope(true));
      }
      if (url.includes('/app/list')) {
        return respond(envelope([{ appCode: APP, appName: 'CRM', tenantCode: 'default' }]));
      }
      if (url.includes('/deployment/vocabulary')) {
        if (vocabMode === 'http-500') return respond(failure(500, '部署方式接口 500'));
        // 形状漂移: executable 那一栏整个缺席。不能读成"服务器没有会执行的方式"。
        if (vocabMode === 'shape-drift') return respond(envelope({ rejected: VOCAB.rejected }));
        return respond(envelope(VOCAB));
      }
      if (url.includes('/deployment/list')) return respond(envelope(rows));
      return respond(envelope([]));
    }),
  );
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <DeploymentsPage />
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

/** 「新建部署」在应用或词表没读出来时是禁用的：等它可用再点，别把"没点开"当成"页面坏了"。 */
async function openDraft() {
  await waitFor(() => expect(buttonByText('新建部署').disabled).toBe(false));
  fireEvent.click(buttonByText('新建部署'));
  await waitFor(() => expect(document.querySelectorAll('.ant-modal .ant-select-selector').length).toBe(1));
}

/** antd Select 要 mouseDown 才展开，弹层挂在 body 上。同一个下拉框只开一次：再 mouseDown 会把它收回去。 */
async function openOptions(index: number): Promise<HTMLElement[]> {
  const selector = document.querySelectorAll('.ant-modal .ant-select-selector')[index];
  if (!selector) throw new Error(`第 ${index + 1} 个下拉框没渲染出来`);
  fireEvent.mouseDown(selector);
  await waitFor(() =>
    expect(document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').length).toBeGreaterThan(0),
  );
  return Array.from(
    document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
  ) as HTMLElement[];
}

function selectedType(): string {
  return document.querySelectorAll('.ant-modal .ant-select-selection-item')[0]?.textContent ?? '';
}

/** 词表那一条告警里的「重试」—— 点它要走完整条重读路径，不是把 banner 藏起来。 */
function clickVocabularyRetry() {
  const alert = Array.from(document.querySelectorAll('.ant-alert')).find((node) =>
    (node.textContent ?? '').includes('部署方式词表'),
  );
  if (!alert) throw new Error('没有「部署方式词表没有读到」这一条告警');
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

function clearToasts() {
  message.destroy();
}

beforeEach(() => {
  posts.length = 0;
  rows = [];
  vocabMode = 'ok';
  createMode = 'success';
  stubBackend();
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

describe('DeploymentsPage', () => {
  it('部署方式只有接口报告真执行的那几种，而「开始部署」送出去的就是选中的那一种', async () => {
    rows = [deploymentRow({ id: 7 })];
    renderPage();

    // 列表那格的中文名也来自同一份显示名表（不是编码裸奔）。
    // 注意 `.ant-table-tbody tr` 也包括空态那行占位，所以要等真数据行 —— 应用清单是异步进来的。
    await waitFor(() => expect(document.querySelectorAll('.ant-table-tbody .ant-table-row').length).toBe(1));
    expect(document.body.textContent).toContain('热加载');
    expect(document.body.textContent).toContain('SUCCESS');

    await openDraft();

    // 选择项 = 接口那份 executable 的原样顺序 + 不执行的那几种排在后面：
    // BLUE_GREEN 只有接口才知道（页面自己抄不出来），DOCKER 是服务器明确不执行的 ⇒ 只能看不能选。
    const options = await openOptions(0);
    expect(
      options.map((node) => node.textContent ?? ''),
      '下拉项必须逐字等于从接口长出来的那份（executable 原序，不执行的排在后面并标注）',
    ).toEqual(['热加载', 'BLUE_GREEN', 'Docker 镜像（服务器不执行）']);
    expect(options[2]?.className, '服务器不执行的方式在界面上必须是禁用的').toContain('ant-select-item-option-disabled');
    const joined = options.map((node) => node.textContent ?? '').join('|');
    expect(joined.includes('Git 推送'), '接口没报告的方式不该凭空出现在下拉里').toBe(false);

    // 不执行的方式不是"藏起来"就算完：界面上要指名它为什么不执行（服务器的原话，不是前端编的）
    expect(tagOf('deployment-rejected-DOCKER').textContent).toContain(DOCKER_REASON);

    // 默认取接口清单的第一项
    expect(selectedType(), '默认必须取接口清单里的第一种').toBe('热加载');

    // 选一个只有接口才知道的编码：页面不许把它翻译掉，也不许回落到 HOT_LOAD
    fireEvent.click(options[1] as HTMLElement);
    await waitFor(() => expect(selectedType(), '选了接口报告的新编码，界面不许回落到 HOT_LOAD 或翻译掉它').toBe('BLUE_GREEN'));
    const versionInput = document.querySelector('.ant-modal-body input[placeholder="版本号（留空则不记版本）"]');
    if (!versionInput) throw new Error('版本号的输入框没渲染出来');
    fireEvent.change(versionInput, { target: { value: '1.2.3' } });
    fireEvent.click(buttonByText('开始部署'));

    await waitFor(() => expect(posts.some((post) => post.url.includes('/deployment/create'))).toBe(true));
    const body = posts.find((post) => post.url.includes('/deployment/create'))?.body as Record<string, unknown>;
    expect(body.appCode).toBe(APP);
    expect(body.deployType, '送出的必须是界面上选中的那一种，而不是页面里钉死的 HOT_LOAD').toBe('BLUE_GREEN');
    expect(body.version).toBe('1.2.3');
    expect(body.tenantCode).toBe('default');

    // 做成了才报"完成"，而且报的是服务器写下的那一句账
    const toast = await toasts();
    expect(toast).toContain('部署完成');
    expect(toast).toContain('新建 1 张，补列 1 张');
    expect(document.querySelector('.ant-message-error')).toBeNull();
    expect(document.querySelector('[data-testid="deployment-log"]'), '做成了不该弹日志抽屉').toBeNull();
  }, 90_000);

  it('没做成时报的是失败与服务器那句原因，而不是「部署已创建」', async () => {
    createMode = 'failed';
    renderPage();
    await openDraft();
    fireEvent.click(buttonByText('开始部署'));

    const toast = await toasts();
    // 状态参与提示：这一句是 #70 的本体 —— 老代码在这里无条件弹「部署已创建」。
    expect(toast).toContain('部署没做成（FAILED）');
    expect(toast).toContain('没建成 1 张');
    expect(toast.includes('部署已创建'), 'PENDING 时代的谎话回来了').toBe(false);
    expect(toast.includes('部署完成'), 'FAILED 不能报成完成').toBe(false);
    expect(document.querySelector('.ant-message-success')).toBeNull();

    // 失败时把抽屉打开在那一行的日志上：逐条原因（哪张表没建成、缺哪一列）不能只留一句汇总
    expect(document.querySelector('.ant-drawer-title')?.textContent).toBe('部署 #9 · FAILED');
    const log = tagOf('deployment-log').textContent ?? '';
    expect(log).toContain('没建成 1 张');
    expect(log).toContain('[FAILED] deal');
    expect(log).toContain('缺引擎自建列 tenant_code');
    expect(log.includes('（这一行没有日志'), '服务器给了日志却读成没有').toBe(false);

    // 关掉抽屉再走一次：写入口拒掉的形态，原话要回到用户眼前，且人得留在原地改
    fireEvent.click(document.querySelector('.ant-drawer-close') as Element);
    clearToasts();
    createMode = 'http-400';
    fireEvent.click(buttonByText('新建部署'));
    await waitFor(() => expect(document.querySelectorAll('.ant-modal .ant-select-selector').length).toBe(1));
    fireEvent.click(buttonByText('开始部署'));

    const rejectedToast = await toasts();
    expect(rejectedToast).toContain('物化批次 999999 不存在');
    expect(rejectedToast.includes('创建失败'), '只说"创建失败"等于没说 —— 400 的原因在后端 message 里').toBe(false);
    expect(document.querySelector('.ant-modal-body'), '被拒了要留在弹窗里改').toBeTruthy();
  }, 90_000);

  it('词表读不出来时不猜：说"没有读到"，也不开一个只会挨 400 的草稿', async () => {
    vocabMode = 'http-500';
    rows = [deploymentRow({ id: 7 })];
    renderPage();

    // 列表本身是好的：只许出现"词表没有读到"这一句，不许顺手把部署记录也说成读不到
    await waitFor(() => expect(alerts()).toContain('部署方式词表没有读到'));
    expect(alerts()).toContain('部署方式接口 500');
    expect(alerts().includes('部署记录没有读到'), '部署记录读成功了').toBe(false);

    // 没有可信清单时不许开一个只能送空 deployType 的草稿（写入口现在把它拒成 400）
    expect(buttonByText('新建部署').disabled).toBe(true);

    // 重试真的重读: 换成"接口回了但形状不对"这一种失败, 同样不许读成"服务器没有会执行的方式"
    vocabMode = 'shape-drift';
    clickVocabularyRetry();
    await waitFor(() => expect(alerts()).toContain('executable 不是数组'));
    expect(buttonByText('新建部署').disabled).toBe(true);
  }, 90_000);
});
