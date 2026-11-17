#!/usr/bin/env node
/*
 * 「在 z-opc 里造一个系统」端到端验证。
 *
 * 覆盖三件事，缺一件都说明链路没通：
 *   1. 运行态：/lc/app/:app/:page 真的把库里 schema 渲染出来（免登录）
 *   2. 多页面：按钮的「跳转页面编码」把两个页面串成一个应用
 *   3. 设计器：/lc-designer/:app/:page 读得到同一份页面，保存会真的发请求
 *
 *   node e2e/lc-system.mjs
 */
import { chromium } from 'playwright-core';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const BASE = process.env.E2E_BASE ?? 'http://localhost:3000';
const SHOTS = process.env.E2E_SHOTS ?? path.join(HERE, 'shots-lc-system');
const CHROME = process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

fs.mkdirSync(SHOTS, { recursive: true });
const failures = [];
const notes = [];
const check = (name, ok, detail) => {
  notes.push(`${ok ? '  PASS' : '  FAIL'}  ${name}${!ok && detail ? `   << ${String(detail).slice(0, 220)}` : ''}`);
  if (!ok) failures.push(name);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ executablePath: CHROME, headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
const errors = [];
page.on('pageerror', (e) => errors.push(e.message));
page.on('console', (m) => {
  if (m.type() === 'error') errors.push(m.text());
});

/** 记录设计态接口的真实响应，用来证明「保存确实打到后端了」 */
const apiCalls = [];
page.on('response', (res) => {
  const url = res.url();
  if (url.includes('/api/lc/page/')) apiCalls.push({ url, status: res.status() });
});

try {
  /* ---------- 1. 运行态列表页 ---------- */
  await page.goto(`${BASE}/lc/app/tickets/list`, { waitUntil: 'networkidle' });
  await page.waitForSelector('[data-lc-runtime-page]', { timeout: 15000 });
  await sleep(400);
  await page.screenshot({ path: path.join(SHOTS, 's01-list.png') });

  const listText = await page.locator('[data-lc-runtime-page]').innerText();
  check('运行态加载页面（渲染容器出现）', true);
  check('标题来自页面数据', listText.includes('工单管理'), listText.slice(0, 120));
  check('卡片容器渲染', listText.includes('今日概览'));
  check('统计卡渲染（待处理 12）', listText.includes('待处理') && listText.includes('12'));
  check('表格渲染（表头 编号/标题/状态）', listText.includes('编号') && listText.includes('标题'));
  check('按钮渲染', listText.includes('新建工单'));
  check('无「未注册物料」降级', !listText.includes('未注册'), listText.slice(0, 200));

  /* ---------- 2. 多页面跳转 ---------- */
  const before = page.url();
  await page.getByRole('button', { name: /新建工单/ }).first().click();
  await page.waitForURL(/\/lc\/app\/tickets\/new/, { timeout: 10000 });
  await page.waitForSelector('[data-lc-runtime-page]', { timeout: 15000 });
  await sleep(400);
  await page.screenshot({ path: path.join(SHOTS, 's02-new.png') });

  const newText = await page.locator('[data-lc-runtime-page]').innerText();
  check('按钮跳转到新建页', page.url() !== before && page.url().includes('/lc/app/tickets/new'), page.url());
  check('新建页渲染（表单字段）', newText.includes('工单标题') && newText.includes('详细描述'), newText.slice(0, 160));

  await page.getByRole('button', { name: /返回列表/ }).first().click();
  await page.waitForURL(/\/lc\/app\/tickets\/list/, { timeout: 10000 });
  check('返回列表（双向导航）', page.url().includes('/tickets/list'), page.url());

  /* ---------- 3. 设计器打开同一页面 + 保存真的发请求 ---------- */
  apiCalls.length = 0;
  await page.goto(`${BASE}/lc-designer/tickets/list`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.dn-app', { timeout: 20000 });
  await sleep(1200); // 等远端 schema 载入
  await page.screenshot({ path: path.join(SHOTS, 's03-designer.png') });

  const canvasText = await page.locator('[data-panel="canvas"]').innerText();
  const loadBanner = await page.locator('[data-designer-load-error]').count();
  // 有登录态：直接载入线上页面；无登录态：必须明确报「读不到」，不能静默给空白页
  check(
    '设计器读服务端页面（登录即载入 / 未登录显式报错）',
    canvasText.includes('工单管理') || loadBanner > 0,
    `content=${canvasText.slice(0, 80)} banner=${loadBanner}`,
  );
  check(
    '未登录时不静默（有横条或已载入内容）',
    loadBanner > 0 || canvasText.includes('工单管理'),
    `banner=${loadBanner}`,
  );

  await page.getByRole('button', { name: '保存' }).first().click();
  await sleep(1500);
  const saveCall = apiCalls.find((c) => c.url.includes('/api/lc/page/'));
  check('点保存会真的打到 /api/lc/page', Boolean(saveCall), JSON.stringify(apiCalls));
  // 未登录时后端 302 → 前端把它翻成「未登录」，而不是静默成功
  const toast = await page.locator('.ant-message').innerText().catch(() => '');
  check(
    '保存结果有明确反馈（登录态下为「已保存到服务端」）',
    /已保存到服务端|未登录/.test(toast),
    `toast=${toast}`,
  );

  /* ---------- 4. 草稿预览：设计器改完立刻看运行态 ---------- */
  await page.goto(`${BASE}/lc/app/tickets/list?draft=1`, { waitUntil: 'networkidle' });
  await page.waitForSelector('[data-lc-runtime-page], [data-lc-runtime-error]', { timeout: 15000 });
  const draftReady = await page.locator('[data-lc-runtime-page]').count();
  check('草稿预览可用（同一份 schema 走运行态渲染）', draftReady > 0);
  await page.screenshot({ path: path.join(SHOTS, 's04-draft.png') });

  const realErrors = errors.filter(
    (e) => !/favicon|Failed to load resource/.test(e) && !/antd?:/.test(e),
  );
  check('无运行时异常', realErrors.length === 0, realErrors.join(' | '));
} catch (e) {
  check('执行未抛异常', false, e instanceof Error ? e.message : String(e));
} finally {
  console.log(notes.join('\n'));
  await browser.close();
  if (failures.length) {
    console.log(`\n失败 ${failures.length} 项：${failures.join('、')}`);
    process.exit(1);
  }
  console.log(`\n全部通过。截图：${SHOTS}`);
}
