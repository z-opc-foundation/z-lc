#!/usr/bin/env node
/*
 * 设计器宿主冒烟：验证「共享包设计器」在任意宿主里都能挂载 + 拖拽落位。
 *
 * 用途：z-lc 控制台（5274 preview）与本仓之外的主壳（3000 dev）跑同一套断言，
 * 确认两边吃的是同一份实现，而不是各留一份手写版。
 *
 *   E2E_BASE=http://localhost:3000 E2E_PATH=/lc-designer node e2e/designer-smoke.mjs
 */
import { chromium } from 'playwright-core';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const BASE = process.env.E2E_BASE ?? 'http://localhost:5274';
const PAGE_PATH = process.env.E2E_PATH ?? '/designer2/demo';
const SHOTS = process.env.E2E_SHOTS ?? path.join(HERE, 'shots-smoke');
const CHROME = process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

fs.mkdirSync(SHOTS, { recursive: true });
const failures = [];
const notes = [];
const check = (name, ok, detail) => {
  notes.push(`${ok ? '  PASS' : '  FAIL'}  ${name}${!ok && detail ? `   << ${String(detail).slice(0, 200)}` : ''}`);
  if (!ok) failures.push(name);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ executablePath: CHROME, headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
const errors = [];
page.on('pageerror', (e) => errors.push(e.message));
page.on('console', (m) => {
  if (m.type() === 'error' && !m.text().includes('404')) errors.push(m.text());
});

try {
  await page.goto(`${BASE}${PAGE_PATH}`, { waitUntil: 'networkidle' });
  await page.evaluate(() => window.localStorage.clear());
  await page.reload({ waitUntil: 'networkidle' });
  await page.waitForSelector('.dn-app', { timeout: 20000 });
  await sleep(500);

  check('设计器挂载成功（.dn-app）', (await page.locator('.dn-app').count()) === 1);
  check('物料面板渲染（>=20 项）', (await page.locator('[data-material-type]').count()) >= 20);
  check('画布根节点带契约属性', (await page.locator('[data-panel="canvas"] [data-dnode="root"]').count()) === 1);
  check('无「未注册物料」占位', !(await page.content()).includes('未注册物料'));
  await page.screenshot({ path: path.join(SHOTS, 'smoke-01-load.png') });

  // 拖一个输入框进画布
  const tile = await page.locator('[data-material-type="input"]').boundingBox();
  const canvas = await page.locator('[data-panel="canvas"]').boundingBox();
  await page.mouse.move(tile.x + tile.width / 2, tile.y + tile.height / 2);
  await page.mouse.down();
  await page.mouse.move(tile.x + 40, tile.y + 40, { steps: 5 });
  await page.mouse.move(canvas.x + canvas.width / 2, canvas.y + 200, { steps: 14 });
  const indicator = await page.locator('.dn-drop-indicator').count();
  await page.mouse.up();
  await sleep(400);

  check('拖拽中出现落点指示', indicator === 1, `indicator=${indicator}`);
  const nodes = await page.evaluate(() =>
    Array.from(document.querySelectorAll('[data-panel="canvas"] [data-dnode]'))
      .filter((el) => el.getAttribute('data-dnode-kind') === 'node')
      .map((el) => el.getAttribute('data-node-type')),
  );
  check('物料落进画布（节点数 > 1）', nodes.length > 1, nodes.join(','));
  await page.screenshot({ path: path.join(SHOTS, 'smoke-02-drop.png') });

  check('无运行时异常', errors.length === 0, errors.slice(0, 3).join(' | '));
} finally {
  await browser.close();
}

console.log(notes.join('\n'));
console.log(`宿主：${BASE}${PAGE_PATH}`);
console.log(`截图：${SHOTS}`);
if (failures.length) {
  console.log(`失败 ${failures.length} 项`);
  process.exit(1);
}
console.log('全部通过。');
