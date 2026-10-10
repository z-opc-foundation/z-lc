import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DesignerPage } from '@/lc/views/designer/DesignerPage';
import type { EntityDefDTO } from '@/lc/api/types';

/**
 * 设计器侧栏的实体清单（#25）：同一类谎话在收尾扫描时找到的最后一处出口。
 *
 * 改动前的形状：`loading` 初值 false、`entities` 初值 `[]`，拉取失败只 `message.error(...)`
 * 弹一条三秒就消失的 toast，然后那台 `<List locale={{ emptyText: '还没有实体' }}>` 就永久
 * 挂着"还没有实体"。这比管理页那种更糟 —— 设计器正是**决定这句话真假**的地方，用户照着它
 * 去新建一个其实已经存在的实体，就会撞唯一编码（缺陷 #21 那一族）。
 *
 * 每条用例都写成"接口挂了时界面会说的那句谎"的反面：先说清谎话是哪句，再断言它不许出现。
 * 全部由 e2e/mutate_designer_entity_guard.py 逐个注入反证 —— 新测试不改坏守卫就永远绿的话，
 * 那它不算"补的测试"。
 */

const APP = 'crm';
type Mode = 'ok' | 'fail' | 'empty' | 'hang';

const modes: { entities: Mode } = { entities: 'ok' };
let calls: string[] = [];

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
    message: success ? null : '实体接口炸了',
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
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: unknown) => {
      const url = String(input);
      calls.push(url);
      // 让出一拍再回：只交微任务的话，一旦守卫被改坏成"无限重查"，jsdom 的计时器会被
      // 饿死，测试表现为**挂住 12 分钟**而不是变红（本轮真栽过一次）。
      await new Promise((resolve) => setTimeout(resolve, 0));
      if (url.includes('/admin/app/entity/list')) {
        if (modes.entities === 'fail') return envelope(null, false);
        if (modes.entities === 'empty') return envelope([]);
        // 永不 resolve：用来量"首帧说了什么"，此时请求确实还没回来。
        if (modes.entities === 'hang') return new Promise(() => undefined);
        return envelope([entity(11, 'deal', '商机', 4), entity(12, 'lead', '线索', 2)]);
      }
      // bundle 先按"不可用"处理，强制 useWorkspace 走逐接口回退路径。
      if (url.includes('/meta/bundle')) return envelope(null, false);
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

function entityCalls() {
  return calls.filter((url) => url.includes('/admin/app/entity/list')).length;
}

/** 按路径挂载设计器。换路径要用 Link 点，不能改 initialEntries（MemoryRouter 只读一次）。 */
/**
 * 路由前缀 `/lc/*`：宿主 main.jsx 用 `<Route path="/lc/*">` 接住剩余段，
 * 产品代码（WorkspaceViewPage 等）也全仓统一 navigate 到 `/lc/designer/...`。
 *
 * 这一支原来 Link 写了 `/lc/designer/other`、路由却挂在 `/designer/:appCode`，
 * 于是"换到另一个应用"落到一条没挂载的路径上（stderr: No routes matched location
 * "/lc/designer/other"），组件直接卸载 —— 用例想验的"复用实例换 params 后
 * 旧行不许留着"根本没机会发生，红得像产品没清侧栏。挂载与入口一起补上前缀。
 */
function designerTree() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <MemoryRouter initialEntries={[`/lc/designer/${APP}`]}>
      <QueryClientProvider client={client}>
        <Link to="/lc/designer/other">换到另一个应用</Link>
        <Routes>
          <Route path="/lc/designer/:appCode" element={<DesignerPage />} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>
  );
}

/** 侧栏那一列：按宽度锁定，免得右侧编辑区的文字混进断言。 */
function sidebar(container: HTMLElement) {
  const card = Array.from(container.querySelectorAll('.ant-card')).find(
    (node) => (node as HTMLElement).style.width === '248px',
  );
  if (!card) throw new Error('没找到侧栏卡片（width:248px）');
  return card as HTMLElement;
}

function sidebarText(container: HTMLElement) {
  return sidebar(container).textContent ?? '';
}

function listItems(container: HTMLElement) {
  return Array.from(sidebar(container).querySelectorAll('.ant-list-item'));
}

/** 错误必须长在**侧栏自己**的 .ant-alert 上：别处冒出一句同样的话不算这条用例过了。 */
function errorAlert(container: HTMLElement, title: string) {
  const alert = Array.from(sidebar(container).querySelectorAll('.ant-alert')).find((node) =>
    (node.textContent ?? '').includes(title),
  );
  if (!alert) throw new Error(`「${title}」没有长在侧栏的 .ant-alert 上`);
  return alert;
}

function clickRetry(container: HTMLElement) {
  const alert = errorAlert(container, '实体列表没有读到');
  const button = Array.from(alert.querySelectorAll('button')).find(
    (node) => (node.textContent ?? '').replace(/\s/g, '') === '重试',
  );
  if (!button) throw new Error('错误块里没有「重试」按钮');
  expect(button).toBeEnabled();
  fireEvent.click(button);
}

const settle = () => new Promise((resolve) => setTimeout(resolve, 200));

beforeEach(() => {
  vi.unstubAllGlobals();
  modes.entities = 'ok';
  // 必须在每个用例前重装：不装的话 fetch 打到 Node 的相对 URL 上直接 reject，
  // 所有接口一起"降级"，用例照样绿 —— 但它测的是"网络全挂"，不是我要的那个故障。
  stubBackend();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('DesignerPage 侧栏：读不到实体不是"这个应用没有实体"', () => {
  it('实体接口失败时说"实体列表没有读到"，不许说"还没有实体"', async () => {
    modes.entities = 'fail';
    const { container } = render(designerTree());
    await waitFor(() => expect(entityCalls()).toBeGreaterThan(0));
    await settle();
    expect(errorAlert(container, '实体列表没有读到')).toBeTruthy();
    expect(sidebarText(container)).not.toContain('还没有实体');
    // toast 不算交代：故障必须留在页面上。
    expect(listItems(container)).toHaveLength(0);
  });

  it('读到实体就列出来，一句谎都不说', async () => {
    const { container } = render(designerTree());
    await waitFor(() => expect(listItems(container)).toHaveLength(2));
    expect(sidebarText(container)).toContain('商机');
    expect(sidebarText(container)).toContain('线索');
    expect(sidebarText(container)).not.toContain('没有读到');
  });

  it('读到空数组才说"还没有实体"', async () => {
    modes.entities = 'empty';
    const { container } = render(designerTree());
    await waitFor(() => expect(sidebarText(container)).toContain('还没有实体'));
    expect(container.querySelector('.ant-alert')).toBeNull();
  });

  it('挂载到响应回来之间那一帧说的是"在读"，不是"还没有实体"', async () => {
    modes.entities = 'hang';
    const { container } = render(designerTree());
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(entityCalls()).toBe(1);
    expect(sidebar(container).textContent ?? '').not.toContain('还没有实体');
    expect(container.querySelector('.zlc-empty-block')).toBeTruthy();
  });
  // 上面这条钉的是 StateBlock 的 isLoading 那一挡（D6 注入 `isLoading={false}` 会把它判红），
  // **不是** `loading` 的初值。取证：把初值改回改动前的 false，这条照样绿 —— 因为 RTL 的
  // act() 会先把挂载 effect 跑完，reload() 进来就 setLoading(true)，等我拿到 DOM 时首帧早过去了。
  // 真浏览器里 useEffect 在 paint 之后才跑，所以初值 false 确实会闪一下"还没有实体"；那是个
  // sub-frame 的竞态，jsdom 里测不出来，注入也抓不住。初值 true 因此按代码约定保留，
  // 而不是按"有测试守着"记账 —— 少报一条守卫，比多报一条假的强。

  it('横幅里的重试要真的再发一次实体请求并把清单画回来', async () => {
    modes.entities = 'fail';
    const { container } = render(designerTree());
    await waitFor(() => expect(entityCalls()).toBe(1));
    await settle();
    const before = entityCalls();
    modes.entities = 'ok';
    clickRetry(container);
    await waitFor(() => expect(listItems(container)).toHaveLength(2));
    expect(entityCalls()).toBeGreaterThan(before);
    expect(container.querySelector('.ant-alert')).toBeNull();
  });

  it('切到另一个应用时读失败，上一个应用的实体清单不许留在侧栏', async () => {
    const { container } = render(designerTree());
    await waitFor(() => expect(listItems(container)).toHaveLength(2));
    // 用真实导航（同一组件实例换 params），而不是重新 mount：路由 /designer/:appCode
    // 切实体时 React 是复用实例的，这条顺带钉住"旧应用的行留着装作还在"。
    fireEvent.click(screen.getByText('换到另一个应用'));
    modes.entities = 'fail';
    await waitFor(() => expect(entityCalls()).toBeGreaterThan(1));
    await settle();
    expect(listItems(container)).toHaveLength(0);
    expect(sidebarText(container)).not.toContain('商机');
    expect(errorAlert(container, '实体列表没有读到')).toBeTruthy();
  });
});

describe('一次挂载只查一次（日历就是这么栽的，设计器同样容易犯）', () => {
  it('设计器安静下来后不再自己重查', async () => {
    const { container } = render(designerTree());
    await waitFor(() => expect(listItems(container)).toHaveLength(2));
    await settle();
    expect(entityCalls(), '挂载之后不该再有第二次实体查询').toBe(1);
    await settle();
    expect(entityCalls(), '安静期不该有后台重查').toBe(1);
  });
});
