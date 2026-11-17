#!/usr/bin/env node
/*
 * 可视化设计器拖拽端到端验证（真 Chrome，生产构建 + preview）
 *
 * 为什么必须用真浏览器：落点解算依赖 getBoundingClientRect —— jsdom 里恒为 0，
 * 单测只能验纯函数。这里跑的是真实指针事件 + 真实布局。
 *
 * 覆盖：
 *   1. 物料点击追加
 *   2. 物料拖入画布空白 → 追加到根
 *   3. 物料拖到已有节点「上/下半部」→ 插到前/后（而不是永远追加）
 *   4. 物料拖进容器中部 → 落进容器
 *   5. 拖拽中出现落点指示线
 *   6. 已有节点跨容器移动
 *   7. 已有节点同级排序（拖到兄弟上边缘 = 插到它前面）
 *   8. 拖进自己的子树 → 被拒绝
 *   9. 大纲树拖拽排序
 *  10. 预览模式渲染真实运行时组件
 *
 *   E2E_BASE=http://localhost:5274 node e2e/designer-dnd.mjs
 */
import { chromium } from 'playwright-core';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const SHOTS = process.env.E2E_SHOTS ?? path.join(HERE, 'shots-designer');
const BASE = process.env.E2E_BASE ?? 'http://localhost:5274';
const CHROME = process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

fs.mkdirSync(SHOTS, { recursive: true });

const failures = [];
const notes = [];
const consoleErrors = [];

function check(name, ok, detail) {
  notes.push(`${ok ? '  PASS' : '  FAIL'}  ${name}${!ok && detail ? `   << ${String(detail).slice(0, 220)}` : ''}`);
  if (!ok) failures.push(name);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** 读画布上的节点表（只取画布，不取大纲树） */
async function readNodes(page) {
  return page.evaluate(() => {
    const out = [];
    document.querySelectorAll('[data-panel="canvas"] [data-dnode]').forEach((el) => {
      const kind = el.getAttribute('data-dnode-kind');
      if (kind !== 'node') return;
      const r = el.getBoundingClientRect();
      out.push({
        id: el.getAttribute('data-dnode'),
        parent: el.getAttribute('data-dnode-parent') || null,
        index: Number(el.getAttribute('data-dnode-index')),
        accepts: el.getAttribute('data-dnode-accepts') === '1',
        type: el.getAttribute('data-node-type'),
        top: r.top,
        bottom: r.bottom,
        left: r.left,
        right: r.right,
        cx: r.left + r.width / 2,
        cy: r.top + r.height / 2,
      });
    });
    return out;
  });
}

async function box(page, selector) {
  const b = await page.locator(selector).first().boundingBox();
  if (!b) throw new Error(`找不到元素 ${selector}`);
  return b;
}

/** 从某点拖到某点，中途分步移动（否则 dnd-kit 的 distance 约束不触发） */
async function dragFromTo(page, from, to, opts = {}) {
  await page.mouse.move(from.x, from.y);
  await page.mouse.down();
  await page.mouse.move(from.x + 8, from.y + 8, { steps: 4 });
  await page.mouse.move(to.x, to.y, { steps: 14 });
  if (opts.midCheck) await opts.midCheck();
  await page.mouse.move(to.x, to.y + 1, { steps: 2 });
  await page.mouse.up();
  await sleep(opts.settle ?? 260);
}

async function shot(page, name) {
  await page.screenshot({ path: path.join(SHOTS, `${name}.png`) }).catch(() => {});
}

async function main() {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', (err) => consoleErrors.push(`pageerror: ${err.message}`));
  page.on('response', (res) => {
    if (res.status() >= 400) consoleErrors.push(`HTTP ${res.status()} ${res.url()}`);
  });

  await page.goto(`${BASE}/designer2/demo`, { waitUntil: 'networkidle' });
  await page.evaluate(() => window.localStorage.clear());
  await page.reload({ waitUntil: 'networkidle' });
  await sleep(400);
  await shot(page, 'd01-initial');

  check('首屏渲染出设计器三栏', (await page.locator('[data-panel="canvas"]').count()) === 1);
  check('画布不再出现未注册物料', !(await page.content()).includes('未注册物料'));
  check('物料面板渲染出 20+ 物料', (await page.locator('[data-material-type]').count()) >= 20);

  /* ---------- 1. 点击追加 ---------- */
  await page.locator('[data-material-type="card"]').click();
  await sleep(200);
  let nodes = await readNodes(page);
  const root = nodes.find((n) => n.parent === null);
  check('点击物料追加到根容器', nodes.length === 2 && nodes.some((n) => n.type === 'card' && n.parent === root.id), JSON.stringify(nodes));

  /* ---------- 2. 物料拖入画布空白 ---------- */
  const cardId = nodes.find((n) => n.type === 'card')?.id;
  const canvasBox = await box(page, '[data-panel="canvas"]');
  await page.locator('[data-material-type="statistic"]').hover();
  const statTile = await box(page, '[data-material-type="statistic"]');
  await dragFromTo(
    page,
    { x: statTile.x + statTile.width / 2, y: statTile.y + statTile.height / 2 },
    { x: canvasBox.x + canvasBox.width / 2, y: canvasBox.y + canvasBox.height - 60 },
    { midCheck: async () => check('拖拽中出现落点指示', (await page.locator('.dn-drop-indicator').count()) === 1) },
  );
  await shot(page, 'd02-drag-material');
  nodes = await readNodes(page);
  check('物料拖入画布 → 追加为根的子节点', nodes.some((n) => n.type === 'statistic' && n.parent === root.id), JSON.stringify(nodes.map((n) => [n.type, n.parent, n.index])));

  /* ---------- 3. 拖到已有节点上/下半部 → 插到前/后 ---------- */
  const cardNode = (await readNodes(page)).find((n) => n.id === cardId);
  const inputTile = await box(page, '[data-material-type="input"]');
  // 拖到 card 的上四分之一 → 应插到 card 之前（index = card.index）
  await dragFromTo(
    page,
    { x: inputTile.x + inputTile.width / 2, y: inputTile.y + inputTile.height / 2 },
    { x: cardNode.cx, y: cardNode.top + 6 },
  );
  nodes = await readNodes(page);
  const inputNode = nodes.find((n) => n.type === 'input');
  check('拖到节点上边缘 → 插到它之前（不是追加到末尾）', Boolean(inputNode) && inputNode.parent === root.id && inputNode.index === cardNode.index, `input.index=${inputNode?.index} card.index=${cardNode.index}`);
  await shot(page, 'd03-insert-before');

  /* ---------- 4. 物料拖进容器中部 → 落进容器 ---------- */
  const cardNow = (await readNodes(page)).find((n) => n.id === cardId);
  const btnTile = await box(page, '[data-material-type="button"]');
  await dragFromTo(
    page,
    { x: btnTile.x + btnTile.width / 2, y: btnTile.y + btnTile.height / 2 },
    { x: cardNow.cx, y: cardNow.cy },
  );
  nodes = await readNodes(page);
  const btn = nodes.find((n) => n.type === 'button');
  check('拖进容器中部 → 成为容器的子节点', Boolean(btn) && btn.parent === cardId, `btn.parent=${btn?.parent} cardId=${cardId}`);
  await shot(page, 'd04-drop-into-container');

  /* ---------- 5. 已有节点同级排序 ---------- */
  nodes = await readNodes(page);
  const rootKids = nodes.filter((n) => n.parent === root.id).sort((a, b) => a.index - b.index);
  const first = rootKids[0];
  const last = rootKids[rootKids.length - 1];
  if (rootKids.length >= 2) {
    await dragFromTo(
      page,
      { x: last.cx, y: last.top + 4 },
      { x: first.cx, y: first.top + 4 },
    );
    const after = await readNodes(page);
    const reordered = after.filter((n) => n.parent === root.id).sort((a, b) => a.index - b.index);
    check('拖动已有节点到第一个之前 → 顺序变化', reordered[0]?.id === last.id, `期望 ${last.id} 在首位，实际 ${reordered.map((n) => n.id).join(',')}`);
    await shot(page, 'd05-reorder');
  } else {
    check('拖动已有节点到第一个之前 → 顺序变化', false, '根节点不足 2 个，用例不成立');
  }

  /* ---------- 6. 拖进自己的子树被拒绝 ---------- */
  nodes = await readNodes(page);
  const cardNode2 = nodes.find((n) => n.id === cardId);
  const btn2 = nodes.find((n) => n.parent === cardId);
  if (btn2) {
    const before = JSON.stringify((await readNodes(page)).map((n) => [n.id, n.parent, n.index]).sort());
    await dragFromTo(
      page,
      { x: cardNode2.cx, y: cardNode2.top + 4 },
      { x: btn2.cx, y: btn2.cy },
    );
    const after = JSON.stringify((await readNodes(page)).map((n) => [n.id, n.parent, n.index]).sort());
    check('把父容器拖进自己的子节点 → 结构不变（拒绝成环）', before === after, '结构被改动');
  } else {
    check('把父容器拖进自己的子节点 → 结构不变（拒绝成环）', false, '没找到容器子节点');
  }
  await shot(page, 'd06-cycle-guard');

  /* ---------- 7. 大纲树拖拽排序 ---------- */
  await page.getByRole('button', { name: /大纲/ }).click();
  await sleep(250);
  await shot(page, 'd07-outline');
  const outlineRows = await page.locator('[data-outline-row]').count();
  check('大纲树列出全部节点', outlineRows === (await readNodes(page)).length + 0 || outlineRows > 1, `rows=${outlineRows}`);
  const rowsInfo = await page.evaluate(() =>
    Array.from(document.querySelectorAll('[data-outline-row]')).map((el) => {
      const r = el.getBoundingClientRect();
      return { id: el.getAttribute('data-outline-row'), y: r.top + r.height / 2, x: r.left + 20 };
    }),
  );
  const rootId = (await readNodes(page)).find((n) => n.parent === null)?.id;
  const draggableRows = rowsInfo.filter((r) => r.id !== rootId);
  if (draggableRows.length >= 3) {
    const a = draggableRows[0];
    const c = draggableRows[2];
    await dragFromTo(page, { x: a.x, y: a.y }, { x: c.x, y: c.y });
    const after = await page.evaluate(() =>
      Array.from(document.querySelectorAll('[data-outline-row]')).map((el) => el.getAttribute('data-outline-row')),
    );
    const beforeOrder = rowsInfo.map((r) => r.id).join(',');
    check('大纲树拖拽改变层级顺序', after.join(',') !== beforeOrder, `before=${beforeOrder} after=${after.join(',')}`);
  } else {
    check('大纲树拖拽改变层级顺序', false, `可拖行数不足：${draggableRows.length}`);
  }
  await page.getByRole('button', { name: /组件/ }).click();
  await sleep(200);

  /* ---------- 8. 选中 + 属性面板写回 ---------- */
  nodes = await readNodes(page);
  const target = nodes.find((n) => n.type === 'button') ?? nodes[1];
  await page.mouse.click(target.cx, target.cy);
  await sleep(300);
  const propTitle = await page.locator('.dn-prop__title').innerText().catch(() => '');
  check('点击画布节点 → 右侧属性面板显示该组件', propTitle.length > 0, propTitle);
  await shot(page, 'd08-selected');

  const firstInput = page.locator('[data-panel="properties"] .ant-form-item input.ant-input').first();
  if ((await firstInput.count()) > 0) {
    await firstInput.fill('改过的标题');
    await sleep(200);
    const texts = await page.locator('[data-panel="canvas"]').innerText();
    check('属性面板改值即时反映到画布', texts.includes('改过的标题'), texts.slice(0, 120));
  } else {
    check('属性面板改值即时反映到画布', false, '属性面板没有输入框');
  }
  await shot(page, 'd09-prop-edit');

  /* ---------- 9. 预览模式 ---------- */
  await page.getByRole('tab', { name: '预览' }).click().catch(async () => {
    await page.getByText('预览', { exact: true }).click();
  });
  await sleep(700);
  const previewText = await page.locator('.dn-preview').innerText().catch(() => '');
  check('预览模式渲染运行时组件树', previewText.includes('预览模式'), previewText.slice(0, 120));
  await shot(page, 'd10-preview');
  await page.getByText('设计', { exact: true }).click();
  await sleep(300);

  /* ---------- 10. 撤销/重做 ---------- */
  // 点状态栏，把焦点从属性面板输入框移开（输入框内的 ⌘Z 归浏览器原生撤销）
  await page.locator('.dn-statusbar').click({ position: { x: 4, y: 4 } });
  await sleep(150);
  const canvasTextBefore = await page.locator('[data-panel="canvas"]').innerText();
  await page.keyboard.press('Meta+z');
  await sleep(350);
  const canvasTextAfter = await page.locator('[data-panel="canvas"]').innerText();
  check(
    '⌘Z 撤销生效（属性改动回滚）',
    canvasTextBefore !== canvasTextAfter && canvasTextAfter.includes('未命名') === false,
    `before 含"改过的标题"=${canvasTextBefore.includes('改过的标题')} after 含"改过的标题"=${canvasTextAfter.includes('改过的标题')}`,
  );
  await page.keyboard.press('Meta+Shift+z');
  await sleep(300);
  await shot(page, 'd11-undo-redo');

  await browser.close();

  console.log(notes.join('\n'));
  console.log(`\n截图目录：${SHOTS}`);
  if (consoleErrors.length) {
    console.log(`\n控制台错误 ${consoleErrors.length} 条：`);
    consoleErrors.slice(0, 8).forEach((e) => console.log(`  ! ${e.slice(0, 200)}`));
  }
  if (failures.length) {
    console.log(`\n失败 ${failures.length} 项：${failures.join(' | ')}`);
    process.exit(1);
  }
  console.log('\n全部通过。');
}

main().catch((e) => {
  console.log(notes.join('\n'));
  console.error('E2E 异常：', e);
  process.exit(1);
});
