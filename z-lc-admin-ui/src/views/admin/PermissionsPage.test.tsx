import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PermissionsPage } from '@/views/admin/PermissionsPage';
import type { PermissionEntity } from '@/api/types';

/**
 * 权限矩阵与后端 `/permission/check` 必须回答同一个问题。#48 之前它们各说一套：
 * 格子取的是"这个角色这一项在任意一行出现过"，于是一个实体的单独授权会把「整个应用」
 * 那一格点亮（而 `/check` 问应用级那一档时答"拒绝"）；选了实体之后应用级那些行又被
 * 后端的 `entity_code = ?` 筛掉，格子整体灭掉。下面每一条对应一处实测过的错法。
 *
 * 行内文本一律按"去掉空白后精确相等"来断：antd 会在两个汉字的按钮里插空格（`授予`
 * 渲染成 `授 起`），而 `toContain('授予')` 在"已授予"上是永真的 —— 那正是要抓的那一格。
 */

const APP = 'crm';

/** (角色, 权限, 作用范围) —— entityCode 为 null 就是「整个应用」。 */
function perm(id: number, role: string, permission: string, entityCode: string | null): PermissionEntity {
  return { id, appCode: APP, entityCode, roleCode: role, permission, tenantCode: 'default' };
}

function rows(): PermissionEntity[] {
  return [
    perm(1, 'OWNER', 'VIEW', null),
    perm(2, 'OWNER', 'DELETE', 'deal'),
    perm(3, 'SALES', 'UPDATE', 'task'),
  ];
}

type Mode = 'ok' | 'fail' | 'empty';

const modes: Record<string, Mode> = {};
const calls: { url: string; body: string | null }[] = [];

function envelope(data: unknown) {
  return { success: true, code: 200, message: null, data };
}

function respond(payload: unknown) {
  const body = JSON.stringify(payload);
  return { ok: true, status: 200, text: async () => body, json: async () => payload };
}

/** Spring 的异常处理器在 400 上照样发信封。 */
function badRequest(msg: string) {
  const body = JSON.stringify({ success: false, code: 400, message: msg, data: null });
  return { ok: false, status: 400, text: async () => body, json: async () => JSON.parse(body) };
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <PermissionsPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  Object.keys(modes).forEach((key) => delete modes[key]);
  calls.length = 0;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown, init?: { body?: unknown }) => {
      const url = String(input);
      calls.push({ url, body: typeof init?.body === 'string' ? init.body : null });
      const mode = (key: string): Mode => modes[key] ?? 'ok';
      if (url.includes('/app/list')) {
        return respond(envelope([{ appCode: APP, appName: 'CRM', tenantCode: 'default' }]));
      }
      if (url.includes('/meta/bundle')) {
        return respond(
          envelope({
            app: { appCode: APP, appName: 'CRM' },
            entities: [
              { entityCode: 'deal', entityName: '商机', appCode: APP },
              { entityCode: 'task', entityName: '工单', appCode: APP },
            ],
            dicts: [],
            views: [],
            fieldTypes: [],
          }),
        );
      }
      if (url.includes('/meta/field-types')) return respond(envelope([]));
      if (url.includes('/permission/list')) {
        if (mode('list') === 'fail') return badRequest('权限接口炸了');
        if (mode('list') === 'empty') return respond(envelope([]));
        return respond(envelope(rows()));
      }
      if (url.includes('/permission/grant')) return respond(envelope(rows()[0]));
      if (url.includes('/permission/revoke')) {
        if (mode('revoke') === 'fail') {
          return badRequest('没有可回收的授权: id=1 (不存在，或不属于当前租户)');
        }
        return respond(envelope(true));
      }
      if (url.includes('/permission/check')) return respond(envelope(true));
      return respond(envelope([]));
    }),
  );
});

afterEach(() => {
  message.destroy();
  vi.unstubAllGlobals();
});

/** 两张表都是 antd Table，矩阵是"表头里有 角色 \ 权限"的那一张。 */
function matrixTable(): Element {
  const found = Array.from(document.querySelectorAll('.ant-table-wrapper')).find((node) =>
    (node.querySelector('thead')?.textContent ?? '').includes('角色 \\'),
  );
  if (!found) throw new Error('矩阵还没渲染');
  return found;
}

function listTable(): Element {
  const found = Array.from(document.querySelectorAll('.ant-table-wrapper')).find(
    (node) => !(node.querySelector('thead')?.textContent ?? '').includes('角色 \\'),
  );
  if (!found) throw new Error('没有第二张表（授权清单）');
  return found;
}

async function mounted() {
  renderPage();
  // 矩阵要等实体列表 + 权限清单都读到才渲染
  await waitFor(() => expect(matrixTable()).toBeTruthy());
}

/** 库里一条授权都没有时矩阵不渲染（没有角色可列），这时页面上只有清单那一张表。 */
async function mountedListOnly() {
  renderPage();
  await waitFor(() => expect(listTable()).toBeTruthy());
}

function cell(role: string, key: string): HTMLElement {
  const table = matrixTable();
  const heads = Array.from(table.querySelectorAll('thead th'));
  const column = heads.findIndex((th) => (th.textContent ?? '').trim() === key);
  if (column < 0) {
    throw new Error(`矩阵列头里没有 ${key}，实际: ${heads.map((th) => th.textContent).join(' / ')}`);
  }
  const row = Array.from(table.querySelectorAll('tbody tr[data-row-key]')).find(
    (tr) => (tr.querySelector('td')?.textContent ?? '').trim() === role,
  );
  if (!row) throw new Error(`矩阵里没有角色 ${role}`);
  const box = row.querySelectorAll('td')[column] as HTMLElement | undefined;
  if (!box) throw new Error(`${role} 行没有第 ${column + 1} 格`);
  return box;
}

/** 按钮文案 + 那行小字，去掉所有空白后精确比对。 */
function cellText(role: string, key: string): string {
  return (cell(role, key).textContent ?? '').replace(/\s/g, '');
}

function clickIn(root: Element, label: string) {
  const button = Array.from(root.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === label,
  );
  if (!button) {
    const seen = Array.from(root.querySelectorAll('button')).map((node) =>
      (node.textContent ?? '').replace(/\s/g, ''),
    );
    throw new Error(`容器里没有「${label}」按钮，实际: ${seen.join(' / ')}`);
  }
  fireEvent.click(button);
}

function requestsFor(fragment: string): string[] {
  return calls.filter((call) => call.url.includes(fragment)).map((call) => call.url);
}

function grantBodies(): Record<string, unknown>[] {
  return calls
    .filter((call) => call.url.includes('/permission/grant'))
    .map((call) => JSON.parse(call.body ?? '{}') as Record<string, unknown>);
}

async function toasts(): Promise<string[]> {
  await waitFor(() => {
    const texts = Array.from(document.querySelectorAll('.ant-message-notice-content')).map(
      (node) => (node.textContent ?? '').replace(/\s/g, ''),
    );
    if (texts.length === 0) throw new Error('还没有任何 toast');
    return texts;
  });
  return Array.from(document.querySelectorAll('.ant-message-notice-content')).map((node) =>
    (node.textContent ?? '').replace(/\s/g, ''),
  );
}

/** antd 的 Select 要 mouseDown 展开再点；按 id 定位以免靠"第几个下拉框"这种脆索引。 */
async function pick(id: string, label: string) {
  const anchor = document.getElementById(id);
  if (!anchor) throw new Error(`页面上没有 #${id}`);
  const select = (anchor.closest('.ant-select') ?? anchor) as HTMLElement;
  fireEvent.mouseDown((select.querySelector('.ant-select-selector') ?? select) as Element);
  const option = await waitFor(() => {
    const nodes = Array.from(
      document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
    );
    const found = nodes.find((node) => (node.getAttribute('title') ?? node.textContent ?? '') === label);
    if (!found) throw new Error(`下拉里没有「${label}」，实际: ${nodes.map((node) => node.textContent).join(' / ')}`);
    return found;
  });
  fireEvent.click(option as HTMLElement);
}

function listRowCount(): number {
  return listTable().querySelectorAll('tbody tr[data-row-key]').length;
}

describe('权限矩阵与 /check 同一个口径', () => {
  it('未选实体时，某个实体的单独授权不许点亮「整个应用」那一格', async () => {
    await mounted();
    // 应用级那一条 VIEW 在 —— 这一格该亮，并说清亮在哪一档
    expect(cellText('OWNER', 'VIEW')).toBe('已授予来自整个应用');
    // OWNER 确实有 DELETE，但范围只有 deal 一个实体，"整个应用"不能算它
    expect(cellText('OWNER', 'DELETE')).toBe('授予另有1个实体单独授予');
    expect(cellText('SALES', 'UPDATE')).toBe('授予另有1个实体单独授予');
    // 也不许伪装成"这里什么都没有"：那几行在清单里是看得见的
    expect(listRowCount()).toBe(3);
  });

  it('选了实体：应用级授权覆盖它，别的实体的授权不覆盖', async () => {
    await mounted();
    await pick('permission-scope-filter', '商机');
    await waitFor(() => expect(cellText('OWNER', 'VIEW')).toBe('已授予来自整个应用'));
    expect(cellText('OWNER', 'DELETE')).toBe('已授予该实体单独授予');
    // 已授予的格子不再重复授予（再点会在实体上多一条和整表重复的授权）
    const button = cell('OWNER', 'VIEW').querySelector('button') as HTMLButtonElement;
    expect(button.disabled, '已授予的格子还能点，会再插一条实体级重复授权').toBe(true);

    await pick('permission-scope-filter', '工单');
    await waitFor(() => expect(cellText('OWNER', 'DELETE')).toBe('授予另有1个实体单独授予'));
    expect(cellText('OWNER', 'VIEW')).toBe('已授予来自整个应用');
    expect(cellText('SALES', 'UPDATE')).toBe('已授予该实体单独授予');
  });

  it('格子授的是当前档位，payload 与提示都要说清范围', async () => {
    await mounted();
    fireEvent.click(cell('OWNER', 'UPDATE').querySelector('button') as Element);
    await waitFor(() => expect(grantBodies()).toHaveLength(1));
    expect(grantBodies()[0], '未选实体时授的必须是应用级（payload 里不许有 entityCode）').not.toHaveProperty('entityCode');
    expect(grantBodies()[0]).toMatchObject({ roleCode: 'OWNER', permission: 'UPDATE', appCode: APP });
    expect((await toasts()).join(' / ')).toContain('已授予OWNER·UPDATE（整个应用）');

    await pick('permission-scope-filter', '工单');
    fireEvent.click(cell('SALES', 'EXPORT').querySelector('button') as Element);
    await waitFor(() => expect(grantBodies()).toHaveLength(2));
    expect(grantBodies()[1], '选了实体就得授给那个实体').toMatchObject({
      entityCode: 'task',
      permission: 'EXPORT',
      roleCode: 'SALES',
    });
    expect((await toasts()).join(' / ')).toContain('已授予SALES·EXPORT（task）');
  });

  it('两个筛选要真的叠加（后端 roleCode 优先于 entityCode，所以筛选放在客户端）', async () => {
    await mounted();
    expect(requestsFor('/permission/list')).toHaveLength(1);
    expect(requestsFor('/permission/list')[0]).not.toContain('entityCode');
    expect(requestsFor('/permission/list')[0]).not.toContain('roleCode');

    await pick('permission-scope-filter', '工单');
    fireEvent.change(document.querySelector('input[placeholder="按角色过滤"]') as Element, {
      target: { value: 'OWNER' },
    });
    // 工单 + OWNER：只剩那条应用级 VIEW。旧口径会让后端按 roleCode 答，把 deal 的授权也带进来。
    await waitFor(() => expect(listRowCount()).toBe(1));
    expect(cellText('OWNER', 'VIEW')).toBe('已授予来自整个应用');
    // 改完两次筛选还是只有一次请求 —— 筛选搬回后端就会丢应用级那些行
    expect(requestsFor('/permission/list')).toHaveLength(1);
  });

  it('筛完没有匹配行时不许说"该应用还没有权限配置"', async () => {
    await mounted();
    fireEvent.change(document.querySelector('input[placeholder="按角色过滤"]') as Element, {
      target: { value: 'NOPE' },
    });
    await waitFor(() => expect(listRowCount()).toBe(0));
    expect(document.body.textContent).toContain('当前筛选下没有匹配的授权');
    expect(document.body.textContent).not.toContain('该应用还没有权限配置');
  });

  it('库里真的没有授权时说"还没有权限配置"，不套用「筛选后没有匹配」那句', async () => {
    // 与上一支互补: 空表有两种成因，读成功 + 零行才是"真的没有"，读成功 + 有行 + 筛空了是"条件不合"。
    modes.list = 'empty';
    await mountedListOnly();
    await waitFor(() => expect(listRowCount()).toBe(0));
    expect(requestsFor('/permission/list')).toHaveLength(1);
    expect(document.body.textContent).toContain('该应用还没有权限配置');
    expect(document.body.textContent).not.toContain('当前筛选下没有匹配的授权');
    // 一次成功的空读不是故障: 那句"接口没有读到数据"更不能出现
    expect(document.body.textContent).not.toContain('接口没有读到数据');
  });

  it('回收失败要报后端那句 400，不许报「已回收」', async () => {
    modes.revoke = 'fail';
    await mounted();
    const before = requestsFor('/permission/list').length;
    clickIn(listTable(), '回收');
    await waitFor(() => expect(document.body.textContent).toContain('没有可回收的授权'));
    expect((await toasts()).join(' / ')).not.toContain('已回收');
    // 失败也要重读一遍：那一行可能已经不在了，留在原地就是"看着还能再删一次"
    await waitFor(() => expect(requestsFor('/permission/list').length).toBeGreaterThan(before));
  });

  it('校验框自己选实体，不该顺手改掉整页筛选', async () => {
    await mounted();
    fireEvent.change(document.querySelector('input[placeholder="角色"]') as Element, {
      target: { value: 'OWNER' },
    });
    await pick('probe-entity', '工单');
    expect(listRowCount(), '校验用的实体选择器把列表也筛了').toBe(3);
    expect(requestsFor('/permission/list')).toHaveLength(1);

    fireEvent.click(document.getElementById('probe-run') as Element);
    await waitFor(() => expect(requestsFor('/permission/check').length).toBeGreaterThan(0));
    expect(requestsFor('/permission/check')[0]).toContain('entityCode=task');
    await waitFor(() => expect(document.body.textContent).toContain('OWNER 对 task 的 VIEW：允许'));

    // 清掉实体 = 问「整个应用」那一档（后端这一支只比 entity_code IS NULL）
    const select = document.getElementById('probe-entity')?.closest('.ant-select');
    const clear = select?.querySelector('.ant-select-clear');
    if (!clear) throw new Error('校验框没有 allowClear，问不出"整个应用"');
    fireEvent.mouseDown(clear);
    fireEvent.click(document.getElementById('probe-run') as Element);
    await waitFor(() => expect(requestsFor('/permission/check').length).toBeGreaterThan(1));
    const wide = requestsFor('/permission/check').at(-1) ?? '';
    expect(wide, '留空还在带 entityCode，问的就不是整个应用').not.toContain('entityCode');
  });
});
