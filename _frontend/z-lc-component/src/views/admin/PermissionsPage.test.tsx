import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, waitFor } from '@testing-library/react';
import { message } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PermissionsPage } from '@/lc/views/admin/PermissionsPage';
import type { PermissionEntity } from '@/lc/api/types';

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
        return respond(envelope([
          { appCode: APP, appName: 'CRM', tenantCode: 'default' },
          { appCode: 'erp', appName: 'ERP', tenantCode: 'default' },
        ]));
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

/**
 * 当前**还挂在屏上**的 toast，按 `data-notification-index` 从新到旧排。
 *
 * ⚠️ 这里不要试图用"屏上到底有几条"来定位目标那条。实测（antd v6 静态 message
 * + PermissionsPage 连续授权两次）：第二次之后 DOM 里**只剩 1 条**，
 * 而且它的 `data-notification-index` 被**重置成 0**（第一条曾经是 1）——
 * holder 被重建、旧节点被替换掉了。也就是说"两条并存、按新旧挑"这个假设
 * 在这里不成立：读数形状是"产品只发了一条 toast"。
 *
 * 正确姿势是**就地断言**：先记住当前 notice 的文本，再等"屏上出现一条不同于它的"。
 * 目标 toast 一定会在它之后出现，所以这个条件严格、且不依赖任何内部顺序假设。
 */
async function toasts(): Promise<string[]> {
  await waitFor(() => {
    if (document.querySelectorAll('.ant-message-notice').length === 0) {
      throw new Error('还没有任何 toast');
    }
  });
  return Array.from(document.querySelectorAll('.ant-message-notice')).map((node) =>
    (node.textContent ?? '').replace(/\s/g, ''),
  );
}

/**
 * 等一条**新于 `previous`** 的 toast 并返回它。
 *
 * @param previous 之前已经看到过的 toast 文本；不传表示只要有任何一条。
 */
async function toastAfter(previous?: string): Promise<string> {
  return waitFor(() => {
    const current = Array.from(document.querySelectorAll('.ant-message-notice')).map((node) =>
      (node.textContent ?? '').replace(/\s/g, ''),
    );
    const fresh = current.filter((text) => text !== previous);
    if (fresh.length === 0) {
      throw new Error(
        `还没有一条不同于「${previous ?? ''}」的新 toast，当前: ${current.join(' / ') || '(空)'}`,
      );
    }
    return fresh[fresh.length - 1];
  });
}

/** antd 的 Select 要 mouseDown 展开再点；按 id 定位以免靠"第几个下拉框"这种脆索引。 */
async function pick(id: string, label: string) {
  const anchor = document.getElementById(id);
  if (!anchor) throw new Error(`页面上没有 #${id}`);
  const select = (anchor.closest('.ant-select') ?? anchor) as HTMLElement;
  fireEvent.mouseDown((select.querySelector('.ant-select-content') ?? select) as Element);
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

/** 矩阵现在的行 = 授权里出现过的角色 ∪ 手动「加入矩阵」的角色（#50）。 */
function matrixRoleRows(): string[] {
  return Array.from(matrixTable().querySelectorAll('tbody tr[data-row-key]')).map(
    (tr) => tr.getAttribute('data-row-key') ?? '',
  );
}

/** 填角色名 → 点「加入矩阵」。整段用真 DOM 事件走，不碰组件内部状态。 */
async function addMatrixRole(name: string) {
  const input = document.getElementById('permission-new-role');
  if (!input) throw new Error('页面上没有「新角色」输入框，新角色的第一条权限没有入口');
  fireEvent.change(input, { target: { value: name } });
  const button = document.getElementById('permission-add-role') as HTMLButtonElement | null;
  if (!button) throw new Error('没有「加入矩阵」按钮');
  if (button.disabled) throw new Error(`填了「${name}」之后「加入矩阵」还是禁用的`);
  fireEvent.click(button);
  await waitFor(() => expect(matrixRoleRows()).toContain(name.trim()));
}

/** 顶部那个「应用」下拉：按它当前显示的应用名定位，不靠"第几个 .ant-select"。 */
async function switchApp(fromLabel: string, toLabel: string) {
  // title 挂在 trigger（.ant-select-content）上，不在 .ant-select 根节点上 ——
  // v6 的老写法读根节点会读到 null，于是报「没有当前显示「CRM」的应用下拉框」。
  const select = Array.from(document.querySelectorAll('.ant-select-content')).find(
    (node) => (node.getAttribute('title') ?? '') === fromLabel,
  );
  if (!select) throw new Error(`没有当前显示「${fromLabel}」的应用下拉框`);
  fireEvent.mouseDown(select);
  const option = await waitFor(() => {
    const nodes = Array.from(
      document.querySelectorAll('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option'),
    );
    const found = nodes.find((node) => (node.getAttribute('title') ?? node.textContent ?? '') === toLabel);
    if (!found) throw new Error(`应用下拉里没有「${toLabel}」，实际: ${nodes.map((node) => node.textContent).join(' / ')}`);
    return found;
  });
  fireEvent.click(option as HTMLElement);
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
    const firstToast = await toastAfter();
    expect(firstToast).toContain('已授予OWNER·UPDATE（整个应用）');

    await pick('permission-scope-filter', '工单');
    // 产品代码刻意印了这个读数面（PermissionsPage.tsx:215-217）：
    // rc-select 自己的显示文字证明不了"组件状态真的换了"（value 传 undefined 时它压根不受控），
    // 所以换档生效只能认 data-entity。少了这一句，后面的点击可能打在一份还没换档的矩阵上，
    // 症状是"第二次授权没有新 toast"—— 看起来像产品 bug，其实是这一步没等。
    await waitFor(() =>
      expect(document.querySelector('[data-testid="perm-filter-scope"]')?.getAttribute('data-entity')).toBe('task'),
    );
    fireEvent.click(cell('SALES', 'EXPORT').querySelector('button') as Element);
    await waitFor(() => expect(grantBodies()).toHaveLength(2));
    expect(grantBodies()[1], '选了实体就得授给那个实体').toMatchObject({
      entityCode: 'task',
      permission: 'EXPORT',
      roleCode: 'SALES',
    });
    expect(await toastAfter(firstToast)).toContain('已授予SALES·EXPORT（task）');
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
    // 必须等**读回来**，不能只等"表格在"或"行数为 0"：
    // 这两个条件在请求发出前就成立，于是 state 还停在 'idle'，空态渲染的是
    // 「选择应用后再看这里」而不是「该应用还没有权限配置」。
    // 判据落在读完成这个时点上（mountedListOnly 只等列表接口发出过请求）。
    await waitFor(() => expect(requestsFor('/permission/list')).toHaveLength(1));
    await waitFor(() =>
      expect(document.body.textContent).toContain('该应用还没有权限配置'),
    );
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

  /* ---- #50：矩阵的行以前是从"已经有授权"的角色里推出来的 ---- */
  it('一条授权都没有的角色，也能从这一页授出第一条', async () => {
    await mounted();
    expect(matrixRoleRows()).toEqual(['OWNER', 'SALES']);
    const blank = document.getElementById('permission-add-role') as HTMLButtonElement;
    expect(blank.disabled, '角色名还没填就能点，会往矩阵里塞一行空角色').toBe(true);

    await addMatrixRole('  Manager  ');
    expect(matrixRoleRows()).toEqual(['OWNER', 'SALES', 'Manager']);
    // 新那一行五格全得是"能点的授予"：只要有一格是 disabled，入口就是画的。
    for (const key of ['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT']) {
      expect(cellText('Manager', key), `${key} 那一格不该一上来就亮`).toBe('授予');
      expect((cell('Manager', key).querySelector('button') as HTMLButtonElement).disabled).toBe(false);
    }
    expect(requestsFor('/permission/grant'), '光是把角色加进矩阵就该发请求？').toHaveLength(0);

    fireEvent.click(cell('Manager', 'VIEW').querySelector('button') as Element);
    await waitFor(() => expect(requestsFor('/permission/grant')).toHaveLength(1));
    expect(grantBodies()[0], '名字两端空白没 trim，库里会同时有 "Manager" 和 " Manager" 两个角色').toMatchObject({
      roleCode: 'Manager',
      permission: 'VIEW',
      appCode: APP,
    });
    expect(grantBodies()[0]).not.toHaveProperty('entityCode');
    expect((await toasts()).join(' / ')).toContain('已授予Manager·VIEW（整个应用）');
  });

  it('同一个角色加两次只多一行（两行会给出同一格互相矛盾的答案）', async () => {
    await mounted();
    await addMatrixRole('Manager');
    await addMatrixRole('Manager');
    expect(matrixRoleRows().filter((role) => role === 'Manager')).toHaveLength(1);
    // 已经出现在授权里的角色也不许多一行
    await addMatrixRole('OWNER');
    expect(matrixRoleRows().filter((role) => role === 'OWNER')).toHaveLength(1);
  });

  it('按实体过滤不许把"只有别的实体有授权"的角色整行藏掉', async () => {
    await mounted();
    await pick('permission-scope-filter', '商机');
    // SALES 只有 task 上的 UPDATE：旧口径下矩阵是从筛过的行推角色的，这一整行会消失，
    // 「另有 N 个实体单独授予」那句话就永远没有出现的对象。
    await waitFor(() => expect(matrixRoleRows()).toContain('SALES'));
    expect(cellText('SALES', 'UPDATE')).toBe('授予另有1个实体单独授予');
    // 清单那张表照旧按实体筛（应用级也算覆盖）：矩阵不缩行、清单缩行，两者各有各的用处
    expect(listRowCount()).toBe(2);
  });

  it('换应用要把上一轮手动加进来的角色带走（它不属于这个应用）', async () => {
    await mounted();
    await addMatrixRole('Manager');
    expect(matrixRoleRows()).toContain('Manager');

    await switchApp('CRM', 'ERP');
    await waitFor(() => expect(matrixRoleRows()).not.toContain('Manager'));
    const input = document.getElementById('permission-new-role') as HTMLInputElement;
    expect(input.value, '输入框里还留着上个应用的角色名，切回来手一抖就授错应用').toBe('');
    // 授权里推出来的角色行不受影响 —— 它们是真的属于当前这个应用的读结果
    expect(matrixRoleRows()).toEqual(['OWNER', 'SALES']);
  });
});
