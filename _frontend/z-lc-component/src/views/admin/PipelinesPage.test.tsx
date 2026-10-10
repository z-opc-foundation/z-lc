import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { PipelinesPage } from '@/lc/views/admin/PipelinesPage';
import type { PipelineConfigEntity } from '@/lc/api/types';

/**
 * 流水线配置页的"说得出这个引擎会干什么"。
 *
 * 钉住的错法都长同一张脸：界面上摆出来的能力，后端根本没有对应实现。
 * 这页以前给的是 WEBHOOK / SCRIPT 两个没有执行器的阶段、五个触发事件（其中三个写后事件
 * 没有任何回调落点），保存时还把旧 order 原样写回，于是上下移动改了界面顺序而没改执行顺序。
 *
 * 两个用例各只 mount 一次整页：jsdom + antd 的挂载在这个仓库里是墙钟大头，
 * 一次渲染多验几点，比拆成五个各挂一次更抗并行抖动（配置里 testTimeout 已经因此抬到 20s）。
 */

const APP = 'crm';

const GOOD_CHAIN = '[{"type":"REQUIRED_CHECK","order":0},{"type":"TYPE_CONVERT","order":1},{"type":"VALUE_VALIDATE","order":2}]';

function configRow(patch: Partial<PipelineConfigEntity>): PipelineConfigEntity {
  return {
    id: 1,
    appCode: APP,
    entityCode: 'deal',
    triggerEvent: 'BEFORE_CREATE',
    stages: GOOD_CHAIN,
    enabled: 1,
    tenantCode: 'default',
    ...patch,
  };
}

let rows: PipelineConfigEntity[] = [];
const posts: { url: string; body: Record<string, unknown> }[] = [];

function envelope(data: unknown) {
  return { success: true, code: 200, message: null, data };
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
        if (url.includes('/pipeline-config/create')) return respond(envelope({ ...sent, id: 99 }));
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
      if (url.includes('/pipeline-config/list')) return respond(envelope(rows));
      return respond(envelope([]));
    }),
  );
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <PipelinesPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

/** 某一行的阶段标签文本, 按界面上的先后 = 引擎真要跑的顺序。 */
function stageTextsOfRow(rowId: number | string): string[] {
  return Array.from(document.querySelectorAll(`[data-testid^="pipeline-stage-${rowId}-"]`)).map(
    (node) => node.textContent ?? '',
  );
}

function tagOf(testid: string): Element {
  const node = document.querySelector(`[data-testid="${testid}"]`);
  if (!node) throw new Error(`页面上找不到 ${testid} —— 这一格根本没渲染出来`);
  return node;
}

function clickButton(root: ParentNode, label: string) {
  const button = Array.from(root.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === label,
  );
  if (!button) throw new Error(`没找到「${label}」按钮`);
  if (button.disabled) throw new Error(`「${label}」是禁用的`);
  fireEvent.click(button);
}

function clickModalIcon(iconClass: string, index = 0) {
  const icon = document.querySelectorAll(`.ant-modal .anticon-${iconClass}`)[index];
  const button = icon?.closest('button');
  if (!button) throw new Error(`没找到第 ${index + 1} 个 ${iconClass} 按钮`);
  if (button.disabled) throw new Error(`${iconClass} 按钮是禁用的`);
  fireEvent.click(button);
}

/**
 * antd v6 把 Select 的 trigger 类名从 `.ant-select-selector` 换成了 `.ant-select-content`。
 * 老选择器现在匹配数为 0（实测: `.ant-select-selector`=0 / `.ant-select-content`=1，
 * 且只有把 mouseDown 打在 `.ant-select-content` 上才会真的展开下拉）。
 * 这一版第一遍还在数老类名，于是「等 4 个下拉框」永远等不到 ——
 * 而配合 openDraft 里那句写在重试回调内的点击，表现成了整个 vitest 挂死而不是一条普通失败。
 */
const SELECT_IN_MODAL = '.ant-modal .ant-select';

/** antd Select 要 mouseDown 才展开, 弹层挂在 body 上; 换一个下拉框时上一个会自动隐藏。 */
async function selectOptions(index: number): Promise<string[]> {
  const selector = document.querySelectorAll(SELECT_IN_MODAL)[index];
  if (!selector) throw new Error(`第 ${index + 1} 个下拉框没渲染出来`);
  // 同 openDraft: mouseDown 是副作用, 只能发一次, 不能塞进 waitFor 的重试回调。
  fireEvent.mouseDown(selector);
  await waitFor(() =>
    expect(document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden)').length).toBeGreaterThan(0),
  );
  return Array.from(
    document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
  ).map((node) => node.textContent ?? '');
}

async function pickOption(index: number, wanted: string) {
  const options = await selectOptions(index);
  const dropdown = document.querySelector('.ant-select-dropdown:not(.ant-select-dropdown-hidden)');
  const node = Array.from(dropdown?.querySelectorAll('.ant-select-item-option') ?? []).find((item) =>
    (item.textContent ?? '').includes(wanted),
  );
  if (!node) throw new Error(`下拉框 ${index} 里没有「${wanted}」，实际: ${options.join(' / ')}`);
  fireEvent.click(node);
}

function draftButton(): HTMLButtonElement | undefined {
  return Array.from(document.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === '新建流水线',
  ) as HTMLButtonElement | undefined;
}

/**
 * 「新建流水线」在应用还没读出来时是禁用的: 等它可用再点, 别把"没点开"当成"页面坏了"。
 *
 * ⚠️ 点击**只能发生一次**, 必须在 waitFor 的重试回调之外。
 * 这一版第一遍写成「点击放进 waitFor 回调里」, 于是每次重试都点一次:
 * 重试由 DOM 变更驱动(antd Modal 挂载就是一次变更) → 点 → 再变更 → 再重试, 闭合成环。
 * 环里全是微任务(promise/react 调度), 定时器一个都排不上 ——
 * 所以连 waitFor 自己的超时、以及用例级 testTimeout 都救不了场, 表现为「整个 vitest 挂死、
 * 连汇总行都打不出来」(macOS `sample` 抓到栈顶是 RunMicrotasks → PromiseFulfillReactionJob 自旋)。
 * 判据: 挂死时 CPU 满载且 testTimeout 不触发 —— 这是饿死, 不是"慢"。
 */
async function openDraft() {
  await waitFor(() => {
    const button = draftButton();
    if (!button) throw new Error('没有「新建流水线」按钮');
    if (button.disabled) throw new Error('按钮还禁着, 应用没选出来');
  });
  fireEvent.click(draftButton() as HTMLButtonElement);
  await waitFor(() =>
    expect(document.querySelectorAll(SELECT_IN_MODAL).length).toBeGreaterThanOrEqual(4),
  );
}

beforeEach(() => {
  posts.length = 0;
  rows = [];
  stubBackend();
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

describe('PipelinesPage', () => {
  // 整页 mount 是这里唯一的慢操作: 本机 load 冲上三位数时一次渲染就要 18s+（实测），
  // 20s 的全局墙钟额度会被渲染本身吃光而跟断言无关。给这一格单独放宽，断言一条不减。
  it('列表把引擎兑现不了的东西逐个标出来, 且不按数组位置假装是执行顺序', async () => {
    rows = [
      // 后端收下校验之前的脏数据: 写后事件 + 没有执行器的阶段 + 只打日志的阶段
      configRow({
        id: 11,
        entityCode: 'legacy',
        triggerEvent: 'AFTER_CREATE',
        stages: '[{"type":"WEBHOOK","order":0},{"type":"DICT_RESOLVE","order":1}]',
      }),
      // order 与数组位置不一致: 界面上必须按 order 排, 那才是引擎跑的顺序
      configRow({
        id: 12,
        entityCode: 'deal',
        stages: '[{"type":"VALUE_VALIDATE","order":2},{"type":"REQUIRED_CHECK","order":0},{"type":"TYPE_CONVERT","order":1}]',
      }),
      configRow({ id: 13, entityCode: 'empty', stages: '[]' }),
    ];
    renderPage();

    await waitFor(() => expect(stageTextsOfRow(11).length).toBe(2));

    expect(tagOf('pipeline-trigger-11').textContent).toBe('AFTER_CREATE 无挂接点');
    expect(tagOf('pipeline-trigger-11').className).toContain('ant-tag-red');
    expect(tagOf('pipeline-trigger-12').textContent).toBe('创建前');
    expect(stageTextsOfRow(11)[0]).toMatch(/WEBHOOK 无执行器/);
    expect(stageTextsOfRow(11)[0]).toMatch(/ant-tag-red|无执行器/);
    expect(document.querySelector('[data-testid="pipeline-stage-11-0"]')?.className).toContain('ant-tag-red');
    expect(stageTextsOfRow(11)[1]).toContain('DICT_RESOLVE（写路径暂不做事）');
    expect(document.querySelector('[data-testid="pipeline-stage-11-1"]')?.className).not.toContain('ant-tag-blue');

    expect(stageTextsOfRow(12).map((text) => text.split(' ')[1])).toEqual(['必填校验', '类型转换', '值域校验']);

    expect(tagOf('pipeline-empty-chain-13').textContent).toBe('空链（保存会被拒）');
  }, 90_000);

  it('草稿里只给后端真支持的选择项, 且移动阶段会改写落库的 order', async () => {
    rows = [configRow({ id: 14 })];
    renderPage();
    await openDraft();

    // 触发事件: 写后事件一个都不许出现
    expect(await selectOptions(1)).toEqual(['创建前', '更新前']);

    // 阶段: 清单与后端同源, 幽灵阶段不许能被选中
    const stageOptions = await selectOptions(2);
    expect(stageOptions.map((text) => (text.match(/[A-Z_]+/) ?? ['<没有阶段编码>'])[0])).toEqual([
      'DICT_RESOLVE',
      'REF_CHECK',
      'REQUIRED_CHECK',
      'TYPE_CONVERT',
      'VALUE_VALIDATE',
    ]);
    const joined = stageOptions.join('|');
    for (const ghost of ['WEBHOOK', 'SCRIPT']) {
      expect(joined.includes(ghost), `${ghost} 没有执行器，不该能选`).toBe(false);
    }
    expect(joined.includes('（写路径暂不做事）'), '什么都不做的阶段要在选择项里说明').toBe(true);
    expect(joined.includes('（必填）'), '摘不得的三道闸门要标出来').toBe(true);

    await pickOption(0, '商机');
    // 草稿默认是三道闸门; 把第二行(类型转换)上移到第一位
    clickModalIcon('arrow-up', 1);
    clickButton(document.querySelector('.ant-modal') as Element, '保存');

    await waitFor(() => expect(posts.some((post) => post.url.includes('/pipeline-config/create'))).toBe(true));
    const body = posts.find((post) => post.url.includes('/pipeline-config/create'))?.body as {
      entityCode: string;
      triggerEvent: string;
      enabled: number;
      stages: string;
    };
    expect(body.entityCode).toBe('deal');
    expect(body.triggerEvent, '草稿默认就该是一份引擎跑得起来的配置').toBe('BEFORE_CREATE');
    expect(body.enabled).toBe(1);
    const stages = JSON.parse(body.stages) as { type: string; order: number }[];
    expect(stages.map((item) => item.type)).toEqual(['TYPE_CONVERT', 'REQUIRED_CHECK', 'VALUE_VALIDATE']);
    expect(stages.map((item) => item.order), 'order 若原样写回, 后端按 order 排出来的还是移动前的顺序').toEqual([0, 1, 2]);
  }, 90_000);
});
