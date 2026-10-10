import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createWorkspaceContext, resolveEntityFields } from '@yuku123/render/fields';
import { KanbanView } from '@/lc/views/workspace/KanbanView';
import type { EntityDefDTO, FieldDefDTO } from '@/lc/api/types';

/**
 * 看板的行为测试。
 *
 * 重点是那条"不用鼠标也能改"的路径：卡片上的「移到」菜单和拖拽走的是同一个 move()，
 * 所以它必须发出**同一形状的部分更新请求**（只提交分组字段 + id），
 * 否则键盘用户点的按钮其实是坏的、只有拖拽能用 —— 那种事不会有任何报错，只会静默不一致。
 *
 * 「能用键盘打开」和「能用整条键盘路径完成」是两件事（缺陷 #32）：antd 的 Dropdown 打开时
 * 不把焦点交给菜单，而方向键/Enter 都挂在菜单根节点上，于是焦点留在按钮时整条路径是死的，
 * 卡片上的 tooltip 却写着"键盘可用"。所以这里断言的是**焦点位置 + 真按键之后的请求**，
 * 不是"菜单渲染出来了"。
 */

const APP = 'crm';
const ENT = 'deal';

function fields(): FieldDefDTO[] {
  return [
    { fieldCode: 'title', fieldName: '标题', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
    { fieldCode: 'stage', fieldName: '阶段', fieldType: 'STRING', dictCode: 'stage', fieldLength: 16, sortOrder: 2 },
  ] as FieldDefDTO[];
}

function entity(): EntityDefDTO {
  return {
    id: 3, entityCode: ENT, entityName: '商机', tableName: 't_deal',
    tenantCode: 'default', appCode: APP, fields: fields(),
  } as EntityDefDTO;
}

interface Call {
  url: string;
  body: Record<string, unknown>;
}

const calls: Call[] = [];

function stubBackend() {
  calls.length = 0;
  const text = (payload: unknown) => {
    const body = JSON.stringify(payload);
    return { ok: true, status: 200, text: async () => body, json: async () => payload };
  };
  vi.stubGlobal('fetch', vi.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    const body = (init?.body ? JSON.parse(String(init.body)) : {}) as Record<string, unknown>;
    calls.push({ url, body });
    if (url.includes('/runtime/aggregate')) {
      return text({
        success: true, code: 200, message: null,
        data: [
          { group_key: 'NEW', group_label: '新建', group_count: 2 },
          { group_key: 'DONE', group_label: '已完成', group_count: 1 },
        ],
      });
    }
    if (url.includes('/runtime/list')) {
      return text({
        success: true, code: 200, message: null,
        data: {
          records: [
            { id: 11, title: '甲', stage: 'NEW' },
            { id: 12, title: '乙', stage: 'NEW' },
            { id: 13, title: '丙', stage: 'DONE' },
          ],
          total: 3, pageNum: 1, pageSize: 200,
        },
      });
    }
    return text({ success: true, code: 200, message: null, data: 1 });
  }));
}

function renderBoard() {
  const ws = createWorkspaceContext({ appCode: APP, tenantCode: 'default', entities: [] });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <KanbanView
        entity={entity()}
        resolvedFields={resolveEntityFields(entity(), ws)}
        appCode={APP}
        tenantCode="default"
        conditions={[]}
        conjunction="AND"
      />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  window.localStorage.clear();
  stubBackend();
});

/** 焦点位置就是这条路径的被测量，失败信息里先带上它 —— 红了不用回头再跑一遍取证。 */
function focusWhere(): string {
  const el = document.activeElement as HTMLElement | null;
  if (!el || el === document.body) {
    return 'body';
  }
  return `${el.tagName.toLowerCase()}[${el.getAttribute('aria-label') || el.className}]`;
}

/**
 * 打开某张卡的「移到」菜单（触发按钮顺带拿到焦点，与真实点击的顺序一致）。
 * 完整的 Tab → Enter → ArrowDown → Enter 不在这儿测，归浏览器门禁（可信按键注入）；
 * 这里只钉我们自己那三层桥：焦点交给菜单、二次打开仍然给、关掉后还回去。
 */
async function openMoveMenu(user: ReturnType<typeof userEvent.setup>, title: string) {
  const trigger = screen.getByLabelText(`移动 ${title} 到其他分组`);
  await user.click(trigger);
  await waitFor(() => expect(document.querySelector('.ant-dropdown-menu')).toBeTruthy());
  return trigger;
}

/** 点一张卡片关掉菜单（等价于用户改主意去了别处）。 */
async function clickAwayToClose() {
  fireEvent.mouseDown(screen.getByText('乙'));
  await waitFor(() => expect(document.querySelector('.ant-dropdown-menu')).toBeNull());
}

describe('KanbanView', () => {
  it('列走服务端 group-by：列名用字典标签，计数是全量口径', async () => {
    renderBoard();
    await waitFor(() => expect(screen.getByText('新建')).toBeInTheDocument());
    expect(screen.getByText('已完成')).toBeInTheDocument();
    const agg = calls.find((c) => c.url.includes('/runtime/aggregate'));
    expect(agg?.body.groupField).toBe('stage');
  });

  it('默认按字典字段分组，不会退化成一条记录一列', async () => {
    renderBoard();
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/aggregate'))).toBe(true));
    const agg = calls.find((c) => c.url.includes('/runtime/aggregate'));
    expect(agg?.body.groupField, '分组字段必须是 stage 而不是标题').toBe('stage');
  });

  it('卡片按自己的分组值落列，不是按顺序占位', async () => {
    renderBoard();
    await waitFor(() => expect(screen.getByText('甲')).toBeInTheDocument());
    const owned = Array.from(document.querySelectorAll('.zlc-kb-card')).map((card) => ({
      id: card.getAttribute('data-record-id'),
      column: card.closest('.zlc-kb-column')?.getAttribute('data-group-key'),
    }));
    // 13 的 stage 是 DONE：按记录下标派列会把它塞进第一列，看起来"两列都有卡"其实归属是错的
    expect(owned).toEqual([
      { id: '11', column: 'NEW' },
      { id: '12', column: 'NEW' },
      { id: '13', column: 'DONE' },
    ]);
  });

  it('「移到」菜单和拖拽走同一条部分更新：只提交分组字段和 id', async () => {
    const user = userEvent.setup();
    renderBoard();
    await waitFor(() => expect(screen.getByText('甲')).toBeInTheDocument());

    const trigger = screen.getByLabelText('移动 甲 到其他分组');
    await user.click(trigger);
    const option = await waitFor(() => {
      const found = Array.from(document.querySelectorAll('.ant-dropdown .ant-dropdown-menu-item'))
        .find((item) => item.textContent?.trim() === '已完成');
      if (!found) {
        throw new Error('下拉里没看到目标分组');
      }
      return found;
    });
    await user.click(option);

    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/update'))).toBe(true));
    const update = calls.find((c) => c.url.includes('/runtime/update'))!;
    expect(update.url).toContain('entityCode=deal');
    // 后端契约：updateRecord 把主键塞在 fieldValues.id 里（服务端 remove("id") 当 WHERE 键），
    // 关键是除它之外不能夹带别的列 —— 带上整行就会被必填校验拦下（历史缺陷 #13）。
    const values = update.body.fieldValues as Record<string, unknown>;
    expect(values).toEqual({ stage: 'DONE', id: 11 });
    expect(Object.keys(values).sort(), '只能提交被改的那一列 + id').toEqual(['id', 'stage']);
  });

  it('「移到」菜单打开后焦点交给菜单，Enter 选中发出与鼠标同形的部分更新', async () => {
    const user = userEvent.setup();
    renderBoard();
    await waitFor(() => expect(screen.getByText('甲')).toBeInTheDocument());

    await openMoveMenu(user, '甲');
    const menu = document.querySelector('.ant-dropdown-menu') as HTMLElement;
    // 这一条是缺陷 #32 的全部：菜单渲染出来 ≠ 键盘能用。方向键/Enter 的处理挂在菜单根节点，
    // 焦点还停在触发按钮上时一条都收不到，而卡片上的 tooltip 写着"键盘可用"。
    await waitFor(() => expect(menu.contains(document.activeElement),
      `打开后焦点没进菜单，而在 ${focusWhere()}`).toBe(true));

    // ArrowDown 那一步不在这里测：rc-menu 挑可聚焦项要过 `isVisible`，而它读 offsetParent /
    // getBoundingClientRect —— jsdom 没有布局，两者恒为 null/0，于是菜单项在 jsdom 里一律
    // "不可见"，方向键走不动（真量过：焦点钉在 ul 上）。完整的 Tab → Enter → ArrowDown → Enter
    // 归浏览器门禁（可信按键注入）。这里只钉我们自己的两层：焦点进菜单、项上 Enter 真改列。
    const item = document.querySelector('.ant-dropdown-menu-item') as HTMLElement;
    item.focus();
    fireEvent.keyDown(item, {key: 'Enter', code: 'Enter', keyCode: 13, which: 13});
    await waitFor(() => expect(calls.some((c) => c.url.includes('/runtime/update'))).toBe(true));
    const update = calls.find((c) => c.url.includes('/runtime/update'))!;
    // 键盘路径与鼠标路径必须发同一个形状，否则"看着有、其实是坏的"
    expect(update.body.fieldValues as Record<string, unknown>, '键盘选中后应当发出与点击同样的部分更新').toEqual({
      stage: 'DONE', id: 11,
    });
  });

  it('菜单关掉后焦点回到「移到」按钮，键盘用户不会在一次移动之后丢了位置', async () => {
    const user = userEvent.setup();
    renderBoard();
    await waitFor(() => expect(screen.getByText('甲')).toBeInTheDocument());

    const trigger = await openMoveMenu(user, '甲');
    await waitFor(() => expect(trigger.contains(document.activeElement)).toBe(false));
    // 没有"还焦点"那一层的话，焦点会留在已经销毁的菜单里，键盘用户下一次 Tab 从页面开头重新数。
    await clickAwayToClose();
    await waitFor(() => expect(document.activeElement, `关掉后焦点在 ${focusWhere()}`).toBe(trigger));
  });

  it('第二次打开仍然把焦点交给菜单（destroyOnHidden 不是装饰）', async () => {
    const user = userEvent.setup();
    renderBoard();
    await waitFor(() => expect(screen.getByText('甲')).toBeInTheDocument());

    const focusMenu = async () => {
      const menu = document.querySelector('.ant-dropdown-menu') as HTMLElement;
      await waitFor(() => expect(menu.contains(document.activeElement),
        `焦点没进菜单，而在 ${focusWhere()}`).toBe(true));
    };
    await openMoveMenu(user, '甲');
    await focusMenu();
    // 桥的 effect 只在挂载时跑一次。弹层关着仍留在 DOM 里的话，第二次打开就不会再跑它，
    // 于是"第一次能用、第二次悄悄回到不能用"—— 这种半好半坏最难被看一眼发现。
    await clickAwayToClose();
    await openMoveMenu(user, '甲');
    await focusMenu();
  });
});
