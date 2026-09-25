#!/usr/bin/env node
/*
 * 真浏览器端到端验证 — 确定性版本
 *
 * 关键改进（相比上一版）：
 *  1. 每个 check 独立 try/except — 一个失败不吞后面所有检查
 *  2. 失败时自动截图 + DOM 快照 + 时间线写入 timeline.json
 *  3. REPEATS 循环真正执行，输出"通过率"而非"跑一次看绿"
 *  4. 没有 KNOWN 豁免：每条检查都计失败。曾经豁免过的内联编辑缺陷已定位修复
 *     （字典项重复导致 list SQL fan-out），豁免机制随之删掉 —— 留着就会变成
 *     "永远绿的那几条"，正是它当初掩盖的东西。
 */
import { chromium } from 'playwright-core';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const SHOTS = process.env.E2E_SHOTS ?? path.join(HERE, 'shots');
const BASE = process.env.E2E_BASE ?? 'http://localhost:5274';
const API = process.env.E2E_API ?? 'http://localhost:18090';
const CHROME = process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const REPEATS = Number(process.env.E2E_REPEATS ?? 1) || 1;

/* ------------------------------------------------------------------ */
/*  timeline: 每步记录时间戳 + DOM 快照，失败时 dump 到 timeline.json */
/* ------------------------------------------------------------------ */
const timeline = [];
function tl(event, detail = '') {
  timeline.push({ ts: Date.now(), event, detail: String(detail).slice(0, 500) });
}

/* ------------------------------------------------------------------ */
/*  check —— 单步检查，不会抛出                                       */
/* ------------------------------------------------------------------ */
const runFailures = [];
const runNotes = [];

function check(name, condition, detail) {
  if (condition) {
    runNotes.push(`  PASS  ${name}`);
  } else {
    runFailures.push(name);
    runNotes.push(`  FAIL  ${name}${detail ? `   << ${String(detail).slice(0, 140)}` : ''}`);
  }
}

async function shot(page, name) {
  const p = path.join(SHOTS, `${name}.png`);
  await page.screenshot({ path: p, fullPage: false }).catch(() => {});
  tl('screenshot', p);
}

/** 快速 DOM 快照 —— 只取关键信息，不 dump 整个 document */
async function domSnapshot(page, label) {
  try {
    const snap = await page.evaluate(() => {
      const active = document.activeElement;
      const editableInputs = document.querySelectorAll('.ant-table-tbody input:not([type="checkbox"]):not([type="radio"])');
      const numInputs = document.querySelectorAll('.ant-table-tbody .ant-input-number input');
      const editingCells = document.querySelectorAll('.zlc-cell-editable');
      return {
        activeTag: active?.tagName,
        activeClass: active?.className?.slice(0, 100),
        editableInputCount: editableInputs.length,
        numInputCount: numInputs.length,
        editableCellCount: editingCells.length,
        url: location.href,
      };
    });
    tl('dom', `${label}: ${JSON.stringify(snap)}`);
    return snap;
  } catch {
    tl('dom', `${label}: <page closed>`);
    return null;
  }
}

/* ------------------------------------------------------------------ */
/*  建测试数据（每轮开始前调用一次）                                     */
/* ------------------------------------------------------------------ */
async function seedTestData() {
  const appCode = `uitest${Date.now().toString().slice(-6)}`;
  const created = await fetch(`${API}/api/lc/app/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
    body: JSON.stringify({ tenantCode: 'default', appCode, appName: 'UI 冒烟', description: 'e2e browser' }),
  }).then((r) => r.json());
  if (!created.success) throw new Error(`建应用失败: ${created.message}`);

  const entityRes = await fetch(`${API}/api/lc/admin/app/entity/create?appCode=${appCode}&tenantCode=default`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
    body: JSON.stringify({
      tenantCode: 'default', appCode, entityCode: 'task', entityName: '任务',
      tableName: `ui_task${appCode.slice(-4)}`,
      fields: [
        { fieldCode: 'title', fieldName: '标题', fieldType: 'STRING', required: true, fieldLength: 64, sortOrder: 1 },
        { fieldCode: 'prio', fieldName: '优先级', fieldType: 'STRING', dictCode: 'ui_prio', fieldLength: 16, sortOrder: 2 },
        { fieldCode: 'hours', fieldName: '工时', fieldType: 'DECIMAL', fieldLength: 10, scale: 1, sortOrder: 3 },
        { fieldCode: 'due', fieldName: '截止日期', fieldType: 'DATE', sortOrder: 4 },
      ],
    }),
  }).then((r) => r.json());
  if (!entityRes.success) console.error('  entity create failed:', entityRes.message);

  // 原来这一句连返回值都不看：重复创建在旧代码里是一次 500 + JDBC 堆栈，日志里却一片祥和。
  // 现在服务端把重复返回成 400 "字典已存在"，那是跨轮复用同一个 H2 时的正常幂等结果；
  // 其他失败才需要喊。
  const dictRes = await fetch(`${API}/api/lc/dict/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
    body: JSON.stringify({ tenantCode: 'default', dictCode: 'ui_prio', dictName: '优先级', status: 'ENABLED' }),
  }).then((r) => r.json()).catch(() => ({}));
  if (!dictRes.success && !String(dictRes.message || '').includes('字典已存在')) {
    console.error('  dict create failed:', dictRes.message);
  }
  // save-all 是幂等的整表替换; items/create 现在会拒绝重复 item_code (字典项重复会让
  // list SQL 的 dict join 把每条记录 fan-out 成多行)。
  const itemsRes = await fetch(`${API}/api/lc/dict/items/save-all?dictCode=ui_prio`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
    body: JSON.stringify([
      { tenantCode: 'default', dictCode: 'ui_prio', itemCode: 'P0', itemLabel: '紧急', itemValue: 'P0', sortOrder: 0 },
      { tenantCode: 'default', dictCode: 'ui_prio', itemCode: 'P1', itemLabel: '常规', itemValue: 'P1', sortOrder: 1 },
      { tenantCode: 'default', dictCode: 'ui_prio', itemCode: 'P2', itemLabel: '缓', itemValue: 'P2', sortOrder: 2 },
    ]),
  }).then((r) => r.json());
  if (!itemsRes.success) console.error('  dict items save-all failed:', itemsRes.message);

  const provRes = await fetch(`${API}/api/lc/admin/app/provision-all?appCode=${appCode}&tenantCode=default`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
    body: '{}',
  }).then((r) => r.json());
  if (!provRes.success) console.error('  provision failed:', provRes.message);

  const now = new Date();
  const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const seedRecords = [
    ['写迁移脚本', 'P0', 4, `${ym}-05`],
    ['补测试', 'P1', 2.5, `${ym}-12`],
    ['清理旧数据', 'P2', 1, `${ym}-19`],
  ];
  for (const [title, prio, hours, due] of seedRecords) {
    const recRes = await fetch(`${API}/api/lc/runtime/create?entityCode=task`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
      body: JSON.stringify({ entityCode: 'task', appCode, tenantCode: 'default', fieldValues: { title, prio, hours, due } }),
    }).then((r) => r.json());
    if (!recRes.success) console.error(`  record create failed (${title}):`, recRes.message);
  }
  return appCode;
}

/* ------------------------------------------------------------------ */
/*  单轮运行                                                          */
/* ------------------------------------------------------------------ */
async function runOnce(runNum, appCode) {
  runFailures.length = 0;
  runNotes.length = 0;
  timeline.length = 0;
  tl('run-start', `run=${runNum} app=${appCode}`);

  const browser = await chromium.launch({
    executablePath: CHROME,
    headless: true,
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const pageErrors = [];
  const badResponses = [];
  // 图表的口径必须真的走服务端：只验"图上画了几根柱子"分不出前端数的还是 SQL 数的
  const chartReqs = [];
  // 导入向导的两段是不是各走一次、坏批到底有没有发 commit，只能看网络请求，看不了界面文案
  const importReqs = [];
  // 批量删除同理：一个请求还是 N 个请求，界面上一模一样，只有网络层分得出来
  const deleteReqs = [];
  // 交叉表的行转列在服务端做，界面上一张"看着对"的表也可能是前端自己拼的假透视 ——
  // 只有 /runtime/shape 的请求体（两个维度 + 程序）能证明透视真发生在数据库那边。
  const shapeReqs = [];
  // 界面断言对"页面自己在那儿转圈重查"完全免疫：每次都返回同样的数据，格子照样在。
  // 日历就曾经挂载后 300ms 内发了 43 次当月查询，只有数请求条数才看得见。
  const listReqs = [];
  // 设计器侧栏的实体清单同理：一次挂载查一次还是在那儿自循环，界面上一模一样。
  const entityListReqs = [];
  // 字段编码那道闸必须能证明"拦住 = 一次写请求都没发出去"，只看界面文案是不够的。
  const entityWriteReqs = [];
  // 流水线那一节**故意**要撞一次后端拒绝（那正是它要验的那道闸），所以它得能证明自己只撞了一次。
  const pipelineCreateReqs = [];
  // 但全局哨兵（"任何 >=400 都是意外"）不能因此按 URL 整条放行 —— 改成由那一节按**实际发生的
  // 那一次**登记 `status + ' ' + url`，哨兵按多重集扣减：同一格多发出来的第二次仍然会红。
  const sanctionedRejections = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && req.url().includes('/pipeline-config/create')) {
      pipelineCreateReqs.push(req.url());
    }
  });
  page.on('request', (req) => {
    if (req.method() === 'POST' && req.url().includes('/admin/app/entity/create')) {
      entityWriteReqs.push(req.url());
    }
    if (req.method() === 'PUT' && /\/admin\/entity\b/.test(req.url())) entityWriteReqs.push(req.url());
  });
  page.on('request', (req) => {
    if (req.url().includes('/admin/app/entity/list')) entityListReqs.push(req.url());
  });
  page.on('request', (req) => {
    if (req.method() !== 'POST') return;
    if (req.url().includes('/runtime/list')) listReqs.push(req.url());
    if (req.url().includes('/runtime/aggregate')) {
      try {
        chartReqs.push(req.postDataJSON());
      } catch {
        chartReqs.push(null);
      }
    }
    if (req.url().includes('/runtime/shape')) {
      try {
        shapeReqs.push(req.postDataJSON());
      } catch {
        shapeReqs.push(null);
      }
    }
    if (/\/runtime\/import\/(preview|commit)/.test(req.url())) {
      importReqs.push(req.url().includes('/commit') ? 'commit' : 'preview');
    }
    if (/\/runtime\/delete(-batch)?(\?|$)/.test(req.url())) {
      deleteReqs.push(req.url().includes('delete-batch') ? 'batch' : 'single');
    }
  });
  page.on('pageerror', (err) => {
    const msg = String(err?.message ?? err);
    pageErrors.push(msg);
    tl('pageerror', msg);
  });
  page.on('response', (res) => {
    if (res.status() >= 400) badResponses.push(`${res.status()} ${res.url()}`);
  });

  /**
   * 看板每张卡的归属：它在哪一列（列的 data-group-key）、那一列叫什么、它的矩形是否真的
   * 落在那一列的矩形里。
   *
   * 必须量几何而不是查文本：「卡片标题在 body 里出现过」这种断言，卡片原地不动也成立 ——
   * 那是一条永远不会红的检查，比没有检查更糟（它会让人以为拖拽被验过了）。
   */
  const kanbanCards = () => page.$$eval('.zlc-kb-card', (nodes) => nodes.map((card) => {
    const column = card.closest('.zlc-kb-column');
    const c = card.getBoundingClientRect();
    const b = column?.getBoundingClientRect();
    return {
      id: card.getAttribute('data-record-id') ?? '',
      key: column?.getAttribute('data-group-key') ?? null,
      label: column?.querySelector('.zlc-kb-column-label')?.textContent ?? '',
      inside: !!b && c.left >= b.left - 0.5 && c.right <= b.right + 0.5
        && c.top >= b.top - 0.5 && c.bottom <= b.bottom + 0.5,
    };
  }));

  /** 行级真值现拉：同一轮前面的步骤早就把数据改过了，写死常量只会测出假绿灯。 */
  const listRows = async () => {
    const res = await fetch(
      `${API}/api/lc/runtime/list?entityCode=task&appCode=${appCode}&tenantCode=default`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
        body: JSON.stringify({ page: 1, size: 200 }),
      },
    ).then((r) => r.json());
    return res?.data?.records ?? [];
  };

  try {
    /* ---- 1. 应用列表 ---- */
    try {
      await page.goto(`${BASE}/apps`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.getByText('UI 冒烟').first().waitFor({ timeout: 15000 });
      check('应用列表能看到新建的应用', true);
      await shot(page, `r${runNum}-01-apps`);
    } catch (e) {
      check('应用列表能看到新建的应用', false, e?.message);
      await shot(page, `r${runNum}-01-apps-FAIL`);
      // 后续步骤依赖列表页成功，如果列表都打不开就跳过
      throw new skipRemaining('应用列表失败，跳过后续步骤');
    }

    /* ---- 2. 表格：schema 出列 + 字典标签 ---- */
    try {
      const gridReqsAt = listReqs.length;
      await page.goto(`${BASE}/${appCode}/task/LIST`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.getByText('写迁移脚本').first().waitFor({ timeout: 15000 });
      const headers = await page.locator('.ant-table-thead th').allInnerTexts();
      check('表头来自 schema（标题/优先级/工时）',
        headers.join('|').includes('标题') && headers.join('|').includes('优先级') && headers.join('|').includes('工时'),
        headers.join('|'));
      const priorityCells = await page.locator('.ant-table-tbody td').allInnerTexts();
      check('字典列出标签而不是原始码',
        priorityCells.some((t) => t.includes('紧急')) && !priorityCells.some((t) => t === 'P0'),
        priorityCells.slice(0, 12).join('|'));
      // 表格是唯一还手写着 useCallback+useEffect 取数的视图（`load` 的依赖里带 state.sorts 与
      // 父组件传进来的 conditions），也就是唯一能自己把自己点起重查的一处 —— 而它每次拿回的
      // 都是同一份数据，上面这些断言在循环下照样全绿。只有数请求能看见。
      await page.waitForTimeout(500);
      check('表格挂载只发一次 /runtime/list', listReqs.length - gridReqsAt === 1,
        `reqs=${listReqs.length - gridReqsAt}`);
      const gridChurnAt = listReqs.length;
      await page.waitForTimeout(2000);
      check('表格安静下来后不再自己重查（2s 内不该再有 /runtime/list）',
        listReqs.length - gridChurnAt === 0, `churn=${listReqs.length - gridChurnAt}`);
      await shot(page, `r${runNum}-02-grid`);
    } catch (e) {
      check('表格渲染', false, e?.message);
      await shot(page, `r${runNum}-02-grid-FAIL`);
      throw new skipRemaining('表格失败');
    }

    /* ---- 3. 内联编辑 ---- */
    try {
      tl('inline-edit-start');

      const editorSel = '.ant-table-tbody input:not([type="checkbox"]):not([type="radio"])';

      // Playwright click 不触发 React 合成事件（已通过5轮REPEATS确认），
      // 改用 page.evaluate dispatch MouseEvent 到 span.zlc-cell-editable。
      async function clickEditableCell(text) {
        await page.evaluate((t) => {
          for (const s of document.querySelectorAll('span.zlc-cell-editable')) {
            if (s.textContent.includes(t)) {
              s.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
              break;
            }
          }
        }, text);
      }

      // 辅助：点击 span.zlc-cell-editable
      async function clickCell(text) {
        // 策略 1: Playwright page.click 直接打 span
        const span = page.locator('span.zlc-cell-editable', { hasText: text }).first();
        if (await span.count()) {
          await span.click({ timeout: 3000 }).catch(() => {});
        }
        // 策略 2: page.evaluate dispatch 作为兜底
        await page.evaluate((t) => {
          for (const s of document.querySelectorAll('span.zlc-cell-editable')) {
            if (s.textContent.includes(t)) {
              s.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
              break;
            }
          }
        }, text);
      }

      // 文本列：dispatch click → 等编辑框出现（带重试）
      let editorAppeared = false;
      for (let attempt = 1; attempt <= 5 && !editorAppeared; attempt += 1) {
        await clickCell('清理旧数据');
        await page.waitForTimeout(800);
        try {
          await page.locator(editorSel).first().waitFor({ state: 'visible', timeout: 1500 });
          editorAppeared = true;
        } catch {
          tl(`text-attempt-${attempt}`, 'editor not visible, retrying');
        }
      }
      if (editorAppeared) {
        await page.locator(editorSel).first().fill('清理旧数据（改过）');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(2000);
        const after = (await page.locator('.ant-table-tbody').first().innerText()).includes('清理旧数据（改过）');
        check('内联编辑真的写回并刷新显示', after, after ? '' : '提交后列表里没看到新值');
      } else {
        check('内联编辑真的写回并刷新显示', false, '编辑框未出现');
      }

      // 数值列：点击工时单元格的 span
      await page.evaluate(() => {
        for (const row of document.querySelectorAll('tr.ant-table-row')) {
          if (row.textContent.includes('写迁移脚本')) {
            const cells = row.querySelectorAll('td');
            const hoursTd = cells[3];
            if (hoursTd) {
              const span = hoursTd.querySelector('span.zlc-cell-editable');
              if (span) {
                span.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
              }
            }
            break;
          }
        }
      });
      await page.waitForTimeout(600);
      const numEditor = page.locator('.ant-table-tbody .ant-input-number input').first();
      let numAppeared = true;
      try {
        await numEditor.waitFor({ state: 'visible', timeout: 3000 });
      } catch {
        numAppeared = false;
        await domSnapshot(page, 'num-editor-missing');
      }
      if (numAppeared) {
        await numEditor.fill('7.5');
        await page.keyboard.press('Tab');
        await page.waitForTimeout(1500);
        const rowText = await page.locator('tr.ant-table-row', { hasText: '写迁移脚本' }).first().innerText();
        check('数值列内联编辑也能写回', rowText.includes('7.5'), rowText.replace(/\n/g, ' | '));
      } else {
        check('数值列内联编辑也能写回', false, 'InputNumber 未出现');
      }
      await shot(page, `r${runNum}-03-inline-edit`);
    } catch (e) {
      if (e instanceof skipRemaining) throw e;
      check('内联编辑异常', false, e?.message);
      await shot(page, `r${runNum}-03-inline-FAIL`);
    }

    /* ---- 4. 页脚统计 ---- */
    try {
      let pickerCount = await page.locator('.ant-table-summary .ant-select').count();
      if (pickerCount === 0) {
        await page.getByRole('button', { name: /统计/ }).click();
        await page.waitForTimeout(1000);
        pickerCount = await page.locator('.ant-table-summary .ant-select').count();
      }
      check('页脚统计行有列级选择器', pickerCount > 0, `pickers=${pickerCount}`);
      if (pickerCount > 0) {
        await page.locator('.ant-table-summary .ant-select').last().click();
        await page.waitForTimeout(500);
        const options = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').allInnerTexts();
        check('数值列统计下拉能用真实鼠标打开', options.length > 0, options.join('|'));
        const sumOption = page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: '合计' }).first();
        if (await sumOption.count()) {
          await sumOption.click();
          await page.waitForTimeout(1500);
          const footText = await page.locator('.ant-table-summary').innerText();
          check('选完统计函数后页脚出现服务端算出的值', /合计\s*[\d.]+/.test(footText), footText.replace(/\n/g, ' | '));
        }
      }
      await shot(page, `r${runNum}-04-footer-stats`);
    } catch (e) {
      check('页脚统计', false, e?.message);
      await shot(page, `r${runNum}-04-stats-FAIL`);
    }

    /* ---- 4b. 列设置抽屉：顺序契约 + 界面上那句「空格 + 方向键可键盘排序」 ---- */
    // 抽屉是 columnMeta 的唯一写者，表格读的是 projectColumns 的结果。这一对曾经用两套
    // 顺序（抽屉按 schema 排自己的行，表格按 columnMeta 排列），量出三个现象：只改一个列宽
    // 就把该列挪到表格第一列、排完一次抽屉纹丝不动、再排第二次把第一次的结果整份覆盖掉。
    // 三个都在这里收口 —— 单测那一层钉不了拖拽本身（jsdom 没有 scrollIntoView，
    // dnd-kit 的 KeyboardSensor 一按 Space 就抛），所以"真的能排序""两次会叠加"只有这里能算验过。
    try {
      const L2C = { 标题: 'title', 优先级: 'prio', 工时: 'hours', 截止日期: 'due' };
      /**
       * 表头顺序换成 fieldCode 好和抽屉比。注意这一比只在"没隐藏任何列"时成立：
       * 抽屉里隐藏列照样占一格，而表头不列它 —— 这一段自始至终不碰显隐开关。
       */
      const headerCodes = async () => (await page.$$eval('.ant-table-thead th', (ns) => ns
        .map((n) => n.innerText.trim())
        .filter((t) => t && t !== '操作'))).map((t) => L2C[t] ?? t);
      /** 抽屉列出的 fieldCode，按 DOM 顺序：每行文本是「标题(title)单行文本」这种拼接。 */
      const drawerCodes = () => page.$$eval('.ant-drawer [aria-roledescription="sortable"]', (ns) => ns
        .map((h) => /\((\w+)\)/.exec(h.parentElement?.textContent ?? '')?.[1] ?? ''));
      const headerWidthOf = (label) => page.$$eval('.ant-table-thead th', (ns, want) => {
        const cell = ns.find((n) => n.innerText.trim() === want);
        return cell ? Math.round(cell.getBoundingClientRect().width) : -1;
      }, label);
      /** 只用键盘把焦点挪到第 index 个拖拽句柄上：走不到就是没可达，别用 focus() 蒙过去。 */
      async function tabToHandle(index, cap = 24) {
        for (let i = 0; i < cap; i++) {
          const at = await page.evaluate(() => {
            const el = document.activeElement;
            if (!el || el.getAttribute('aria-roledescription') !== 'sortable') return -1;
            return Array.from(document.querySelectorAll('.ant-drawer [aria-roledescription="sortable"]')).indexOf(el);
          });
          if (at === index) return true;
          await page.keyboard.press('Tab');
        }
        return false;
      }
      /** 键盘排序一次：Space 抓住 → 方向键挪一格 → Space 放下，全程不碰鼠标。 */
      async function keyboardSortOnce(index, direction) {
        if (!(await tabToHandle(index))) return 'Tab 走不到该句柄';
        await page.keyboard.press('Space');
        await page.waitForTimeout(250);
        await page.keyboard.press(direction);
        await page.waitForTimeout(250);
        await page.keyboard.press('Space');
        await page.waitForTimeout(700);
        return null;
      }
      const swap = (list, from, delta) => {
        const to = from + delta;
        if (to < 0 || to >= list.length) return null;
        const next = [...list];
        [next[from], next[to]] = [next[to], next[from]];
        return next;
      };

      await page.getByText('列设置').click();
      await page.locator('.ant-drawer').waitFor({ timeout: 8000 });
      const base = await headerCodes();
      const baseDrawer = await drawerCodes();
      check('抽屉列出的顺序等于表头顺序（未排序时）',
        JSON.stringify(baseDrawer) === JSON.stringify(base), `抽屉=${baseDrawer} 表头=${base}`);

      // 只动一个宽度，别的什么都不碰 —— 这正是原来会跳位的那个入口。
      const widthInput = page.locator('.ant-drawer input[placeholder="宽度"]').nth(2);
      await widthInput.click();
      await widthInput.fill('233');
      await page.keyboard.press('Tab');
      await page.waitForTimeout(700);
      const afterWidth = await headerCodes();
      const hoursWidth = await headerWidthOf('工时');
      check('改一列宽度不会把该列挪位，且宽度落在这一列自己身上',
        JSON.stringify(afterWidth) === JSON.stringify(base) && Math.abs(hoursWidth - 233) <= 2,
        `表头=${afterWidth} 工时实测宽=${hoursWidth}`);

      // 第一次键盘排序：句柄 1 往下挪一格
      const beforeSort = await headerCodes();
      const fail1 = await keyboardSortOnce(1, 'ArrowDown');
      const afterSort1 = await headerCodes();
      check('键盘排序（Tab→Space→↓→Space）确实交换了相邻两列',
        !fail1 && JSON.stringify(afterSort1) === JSON.stringify(swap(beforeSort, 1, 1))
        && JSON.stringify(afterSort1) !== JSON.stringify(beforeSort),
        `${fail1 ?? ''} 前=${beforeSort} 后=${afterSort1}`);
      // 缺陷 #33 的那一半：抽屉必须把自己写出去的顺序显示出来，否则用户以为没生效
      const drawerAfter1 = await drawerCodes();
      check('排完一次，抽屉列出的顺序跟着表头变',
        JSON.stringify(drawerAfter1) === JSON.stringify(afterSort1),
        `抽屉=${drawerAfter1} 表头=${afterSort1}`);

      // 第二次：换一对再排。原来这一次是从 schema 顺序起算的，会把第一次的结果整份丢掉。
      const fail2 = await keyboardSortOnce(0, 'ArrowDown');
      const afterSort2 = await headerCodes();
      check('第二次键盘排序叠加在第一次之上（不是每次都从 schema 顺序重来）',
        !fail2 && JSON.stringify(afterSort2) === JSON.stringify(swap(afterSort1, 0, 1))
        && JSON.stringify(afterSort2) !== JSON.stringify(afterSort1),
        `${fail2 ?? ''} 第一次后=${afterSort1} 第二次后=${afterSort2}`);

      await page.getByText('恢复默认').click();
      await page.waitForTimeout(700);
      const restored = await headerCodes();
      check('「恢复默认」把列顺序还回 schema',
        JSON.stringify(restored) === JSON.stringify(base), `还原后=${restored}`);
      await page.getByRole('button', { name: '完 成' }).click();
      await page.waitForTimeout(300);
      await shot(page, `r${runNum}-04b-column-drawer`);
    } catch (e) {
      check('列设置抽屉', false, e?.message);
      await shot(page, `r${runNum}-04b-columns-FAIL`);
    }

    /* ---- 5. 看板 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/KANBAN`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.waitForTimeout(1500);
      const columns = await page.locator('div[style*="flex: 0 0 268px"]').count();
      check('看板按优先级分列', columns >= 2 && columns <= 4, `columns=${columns}`);
      // 归属检查：卡片必须待在"分组值等于它自己那一列"里。列数和列名都对，但卡按记录下标
      // 派列的话，看起来仍然"每列都有卡"，而每一张卡都站错了队。
      const truth = await listRows();
      const prioById = new Map(truth.map((r) => [String(r.id), String(r.prio ?? '')]));
      const cards0 = await kanbanCards();
      check('看板每张卡都在分组值相等的那一列里',
        cards0.length > 0 && cards0.every((c) => c.key === prioById.get(c.id)),
        `cards=${cards0.map((c) => `${c.id}:${c.key}`).join(' ')} truth=${[...prioById].map(([i, k]) => `${i}:${k}`).join(' ')}`);
      check('看板既没丢卡也没重复挂卡',
        new Set(cards0.map((c) => c.id)).size === cards0.length && cards0.length === truth.length,
        `cards=${cards0.length} rows=${truth.length}`);
      check('卡片矩形落在自己的列里，没溢到隔壁列',
        cards0.every((c) => c.inside),
        `溢出=${cards0.filter((c) => !c.inside).map((c) => c.id).join(',')}`);
      const moveButton = page.getByRole('button', { name: /移动 .* 到其他分组/ }).first();
      await moveButton.focus();
      const focusedLabel = await page.evaluate(() => document.activeElement?.getAttribute('aria-label') ?? null);
      check('「移到」按钮可被键盘聚焦', Boolean(focusedLabel), String(focusedLabel));
      // 记下这张卡原本在哪一列，才能验"键盘路径真的换了列"而不是"菜单开过"
      const movedId = await page.evaluate(() => document.activeElement
        ?.closest('.zlc-kb-card')?.getAttribute('data-record-id') ?? null);
      const keyBefore = (await kanbanCards()).find((c) => c.id === movedId)?.key ?? null;
      await page.keyboard.press('Enter');
      await page.waitForTimeout(700);
      // 打开之后焦点在哪儿是"键盘能不能走完"的前提，先量下来（红了不用回头再跑一遍取证）
      const focusAfterOpen = await page.evaluate(() => {
        const el = document.activeElement;
        return `tag=${el?.tagName ?? '-'} inDropdown=${Boolean(el?.closest?.('.ant-dropdown'))} aria=${el?.getAttribute?.('aria-label') ?? '-'}`;
      });
      let menuItems = await page.locator('.ant-dropdown:visible .ant-dropdown-menu-item').allInnerTexts();
      check('按 Enter 能打开「移到」菜单', menuItems.length > 0, menuItems.join('|'));
      if (menuItems.length > 0) {
        const targetText = menuItems[0].trim();
        // 全程用键盘：Enter 打开之后要靠 ArrowDown + Enter 选中，**不让鼠标代劳**。
        // 「能用键盘打开」和「能用整条键盘路径完成」是两件事，只测前者会把无障碍说成已验证。
        await page.keyboard.press('ArrowDown');
        await page.waitForTimeout(250);
        await page.keyboard.press('Enter');
        await page.waitForTimeout(2000);
        const after = await kanbanCards();
        const mine = after.find((c) => c.id === movedId);
        check('键盘全程把卡片移到了目标列（打开与选中都不碰鼠标）',
          !!mine && mine.label === targetText && mine.key !== keyBefore,
          `id=${movedId} from=${keyBefore} to=${mine?.key}(${mine?.label}) expect=${targetText} focusAfterOpen=${focusAfterOpen}`);
      }
      await shot(page, `r${runNum}-05-kanban-keyboard`);
    } catch (e) {
      check('看板', false, e?.message);
      await shot(page, `r${runNum}-05-kanban-FAIL`);
    }

    /* ---- 6. 看板拖拽 ---- */
    try {
      const cards = page.locator('.zlc-kb-card');
      const cardCount = await cards.count();
      const columnEls = page.locator('.zlc-kb-column');
      const columnCount = await columnEls.count();
      if (cardCount >= 1 && columnCount >= 2) {
        const from = cards.first();
        const fromId = await from.getAttribute('data-record-id');
        const keyBefore = (await kanbanCards()).find((c) => c.id === fromId)?.key ?? null;
        // 目标列必须真的和当前列不同 —— 之前用 nth(2) 直取，碰上"卡片本来就在第 3 列"时
        // 拖了等于没拖，而旧断言（body 里还能搜到卡片标题）照样绿
        let toIndex = -1;
        for (let i = 0; i < columnCount; i += 1) {
          const key = await columnEls.nth(i).getAttribute('data-group-key');
          if (key !== keyBefore) { toIndex = i; break; }
        }
        if (toIndex < 0) throw new Error(`找不到与当前列(${keyBefore})不同的列`);
        const to = columnEls.nth(toIndex);
        const keyExpect = await to.getAttribute('data-group-key');
        const toBox = await to.boundingBox();
        const fromBox = await from.boundingBox();
        if (!toBox || !fromBox) throw new Error('拿不到列或卡片的矩形');
        await page.mouse.move(fromBox.x + fromBox.width / 2, fromBox.y + fromBox.height / 2);
        await page.mouse.down();
        await page.mouse.move(toBox.x + toBox.width / 2, toBox.y + 40, { steps: 12 });
        await page.mouse.up();
        await page.waitForFunction(
          (args) => {
            const card = document.querySelector(`.zlc-kb-card[data-record-id="${args[0]}"]`);
            return card?.closest('.zlc-kb-column')?.getAttribute('data-group-key') === args[1];
          },
          [fromId, keyExpect],
          { timeout: 12000 },
        );
        const after = await kanbanCards();
        const moved = after.find((c) => c.id === fromId);
        check('真实鼠标拖拽把卡片移到了目标列',
          !!moved && moved.key === keyExpect && moved.key !== keyBefore,
          `id=${fromId} from=${keyBefore} to=${moved?.key} expect=${keyExpect}`);
        check('拖拽后卡片仍在自己那一列的盒子里', !!moved?.inside, `inside=${moved?.inside}`);
      } else {
        check('真实鼠标拖拽把卡片移到了目标列', false, `cards=${cardCount} columns=${columnCount}`);
      }
      await shot(page, `r${runNum}-06-kanban-drag`);
    } catch (e) {
      check('看板拖拽', false, e?.message);
      await shot(page, `r${runNum}-06-drag-FAIL`);
    }

    /* ---- 7. 表单 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/FORM`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.waitForTimeout(1200);
      const formRows = await page.locator('.zlc-form-row').count();
      check('表单按 schema 生成 4 行控件（含 DATE 字段）', formRows === 4, `rows=${formRows}`);
      await page.locator('.zlc-form-row').nth(0).locator('input').first().fill('表单建的标题');
      await page.keyboard.press('Tab');
      await page.waitForTimeout(300);
      await page.getByRole('button', { name: /创\s*建|创建/ }).click();
      await page.waitForTimeout(2200);
      check('表单提交后跳到详情并能看到刚写的标题',
        page.url().includes('/DETAIL/') && (await page.locator('body').innerText()).includes('表单建的标题'),
        page.url());
      await shot(page, `r${runNum}-07-form-detail`);
    } catch (e) {
      check('表单', false, e?.message);
      await shot(page, `r${runNum}-07-form-FAIL`);
    }

    /* ---- 8. 画廊视图 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/GALLERY`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.waitForTimeout(1500);
      const cards = await page.locator('.ant-card').count();
      check('画廊视图渲染卡片', cards >= 2, `cards=${cards}`);
      const galleryText = await page.locator('body').innerText();
      check('画廊视图显示数据', galleryText.includes('画廊视图') && galleryText.includes('清理旧数据'), galleryText.slice(0, 80));
      await shot(page, `r${runNum}-08-gallery`);
    } catch (e) {
      check('画廊视图', false, e?.message);
      await shot(page, `r${runNum}-08-gallery-FAIL`);
    }

    /* ---- 9. 日历视图 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/CALENDAR`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.locator('.zlc-cal-cell-current').first().waitFor({ state: 'visible', timeout: 15000 });
      await page.waitForTimeout(800);
      const cells = await page.locator('.zlc-cal-cell-current').count();
      check('日历视图渲染当月日期格', cells >= 28 && cells <= 31, `cells=${cells}`);
      const chips = await page.locator('.zlc-cal-chip').count();
      check('日历视图按日期挂上事件', chips === 3, `chips=${chips}`);

      // 安静下来之后不许自己重查：这一句是本轮才加的门禁，因为日历原来就在无限重查
      // （monthStart 每轮渲染换新身份 → loadPage 换身份 → useEffect 重发），
      // 而上面所有断言在重查下照样全绿 —— 数据每次都一样，格子照样在。
      const churnAt = listReqs.length;
      await page.waitForTimeout(2000);
      const churn = listReqs.length - churnAt;
      check('日历安静下来后不再自己重查（2s 内不该再有 /runtime/list）', churn === 0, `churn=${churn}`);

      const now = new Date();
      const expectedMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
      const monthLabel = (await page.locator('.zlc-cal-month').textContent().catch(() => '')) ?? '';
      check('日历视图默认锚定当前月', monthLabel.trim() === `${now.getFullYear()}年${String(now.getMonth() + 1).padStart(2, '0')}月`, monthLabel);
      await shot(page, `r${runNum}-09-calendar`);

      // 事件落在正确的日期格：5 号那一格应有 1 个 chip
      const day5 = page.locator(`.zlc-cal-cell[data-date="${expectedMonth}-05"]`);
      const day5Chips = await day5.locator('.zlc-cal-chip').count();
      check('日历事件落在正确日期格', day5Chips === 1, `${expectedMonth}-05 chips=${day5Chips}`);

      // 网格几何：每个日期格**上方那个星期标签**必须真是这一天的星期。
      // 只数"多少格、某天几个事件"查不出整张网往右错一格（周一起头算错），而那是日历最刺眼的错：
      // 每个事件都还在"自己的格子"里，可那个格子的表头说的是另一天。
      const grid = await page.evaluate(() => {
        const heads = Array.from(document.querySelectorAll('.zlc-cal-weekday')).map((n) => {
          const r = n.getBoundingClientRect();
          return { text: n.textContent ?? '', x: r.left + r.width / 2 };
        });
        const chars = ['一', '二', '三', '四', '五', '六', '日'];
        return Array.from(document.querySelectorAll('.zlc-cal-cell')).map((cell) => {
          const r = cell.getBoundingClientRect();
          const raw = cell.getAttribute('data-date') ?? '';
          const [y, m, d] = raw.split('-').map(Number);
          const jsDay = Number.isFinite(y) ? new Date(Date.UTC(y, m - 1, d)).getUTCDay() : -1;
          const centre = r.left + r.width / 2;
          const nearest = heads.reduce(
            (best, h) => (Math.abs(h.x - centre) < Math.abs(best.x - centre) ? h : best),
            heads[0] ?? { text: '', x: -1 },
          );
          return { raw, want: chars[jsDay === 0 ? 6 : jsDay - 1] ?? '?', got: nearest.text };
        });
      });
      check('每个日期格上方的星期标签真是这一天的星期（周一起头不许错位）',
        grid.length === 42 && grid.every((c) => c.got === c.want),
        `错格=${grid.filter((c) => c.got !== c.want).map((c) => `${c.raw}:${c.want}≠${c.got}`).slice(0, 6).join(' ')}`);

      const dueById = new Map((await listRows()).filter((r) => r.due)
        .map((r) => [String(r.id), String(r.due).slice(0, 10)]));
      const chipGeo = await page.$$eval('.zlc-cal-chip', (ns) => ns.map((n) => {
        const cell = n.closest('.zlc-cal-cell');
        const c = n.getBoundingClientRect();
        const r = cell?.getBoundingClientRect();
        return {
          id: n.getAttribute('data-record-id') ?? '',
          date: cell?.getAttribute('data-date') ?? null,
          inside: !!r && c.left >= r.left - 0.5 && c.right <= r.right + 0.5
            && c.top >= r.top - 0.5 && c.bottom <= r.bottom + 0.5,
        };
      }));
      check('每条事件都画在自己那条记录日期对应的格子里',
        chipGeo.length > 0 && chipGeo.every((chip) => chip.date === dueById.get(chip.id) && chip.inside),
        `chips=${chipGeo.map((c) => `${c.id}@${c.date}${c.inside ? '' : '(溢出)'}`).join(' ')} truth=${[...dueById].map(([i, dt]) => `${i}:${dt}`).join(' ')}`);
      const monthDueCount = Array.from(dueById.values()).filter((dt) => dt.startsWith(expectedMonth)).length;
      check('当月带日期的记录一条都没被日历静默丢掉',
        chipGeo.length === monthDueCount, `chips=${chipGeo.length} monthRows=${monthDueCount}`);

      // 点击 chip 跳转详情
      const chipText = (await page.locator('.zlc-cal-chip').first().textContent().catch(() => '')) ?? '';
      await page.locator('.zlc-cal-chip').first().click({ timeout: 3000 }).catch(() => {});
      await page.waitForTimeout(1200);
      const bodyText = await page.locator('body').innerText();
      check('点击日历事件打开详情',
        page.url().includes('/DETAIL/') && chipText && bodyText.includes(chipText.trim()),
        `${page.url()} chip=${chipText}`);
      await shot(page, `r${runNum}-09-calendar-detail`);
    } catch (e) {
      check('日历视图', false, e?.message);
      await shot(page, `r${runNum}-09-calendar-FAIL`);
    }

    /* ---- 10. 图表视图（服务端聚合 + 时间分桶，真浏览器全链路） ---- */
    try {
      const waitCount = async (selector, expected) => {
        await page.waitForFunction(
          ([sel, n]) => document.querySelectorAll(sel).length === n,
          [selector, expected],
          { timeout: 15000 },
        );
      };
      const textsIn = (selector) => page.$$eval(selector, (ns) => ns.map((n) => n.textContent ?? ''));
      const pickOption = async (trigger, title) => {
        await page.locator(trigger).first().click({ timeout: 5000 });
        await page.locator(
          `.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option[title="${title}"]`,
        ).first().click({ timeout: 5000 });
        // 收起浮层，否则截图里图被下拉挡住，肉眼 QA 看不出图对不对
        await page.keyboard.press('Escape');
        await page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
          .waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {});
      };
      const lastAgg = () => chartReqs[chartReqs.length - 1] ?? {};

      // 环图几何的渲染端体检：沿扇区描边采样一圈，量到的半径必须全部落在 62–108 的
      // 环带内。内弧错写成外半径时 SVG 不报错，而是另找一个圆心把弧补上，采样点会探到
      // 半径 20 多的地方 —— "画得满但每一块都画歪"这类缺陷只有让浏览器自己算几何才看得见，
      // 数扇区个数、查 NaN 都免疫。108/62 是 Charts.tsx 里 PieChart 的两个半径。
      const checkRing = async (label, selector) => {
        const bands = await page.$$eval(selector, (nodes) => nodes.map((n) => {
          const total = n.getTotalLength();
          let min = Infinity;
          let max = 0;
          for (let i = 0; i <= 240; i++) {
            const p = n.getPointAtLength((total * i) / 240);
            const r = Math.hypot(p.x, p.y);
            if (r < min) min = r;
            if (r > max) max = r;
          }
          return { min, max, d: n.getAttribute('d') ?? '' };
        }));
        const bad = bands.filter((b) => b.min < 61.5 || b.max > 108.5);
        const radii = bands.map((b) => `${b.min.toFixed(1)}~${b.max.toFixed(1)}`).join(' ');
        check(`${label}：每块扇区都落在 62–108 的环带内`,
          bands.length > 0 && bad.length === 0,
          `slices=${bands.length} radii=${radii}${bad.length ? ` 首块越界的 d=${bad[0].d}` : ''}`);
      };

      // 「图形高度」与「图上印的数」必须互相对得上，用浏览器真实布局量（getBoundingClientRect），
      // 尺子取图自己画出来的刻度线 —— 不在测试里重跑一遍 yOf，那是拿组件的公式验组件。
      // 这条和「柱状图分组轴与计数都等于行级真值」是接力关系：真值 == 印的数、印的数 == 画的形状，
      // 两段都成立才谈得上"图没骗人"。环图那条 P1 缺的就是第二段：数个数、比大小全绿。
      const checkDrawn = async (label, svgSelector) => {
        const got = await page.evaluate((sel) => {
          const svg = document.querySelector(sel);
          if (!svg) return { error: `找不到 ${sel}` };
          const box = svg.getBoundingClientRect();
          const vb = svg.viewBox.baseVal;
          if (!box.height || !vb?.height) return { error: `svg 没有可量的布局 h=${box.height}` };
          // CSS px → SVG user unit：整张图可能被 CSS 缩放过，只能用同一把尺换算
          const userY = (clientY) => (clientY - box.top) * (vb.height / box.height);
          const marks = Array.from(svg.querySelectorAll('.zlc-chart-grid g'))
            .map((g) => {
              const line = g.querySelector('line')?.getBoundingClientRect();
              return {
                value: Number((g.querySelector('.zlc-chart-tick')?.textContent ?? '').replace(/,/g, '')),
                y: line ? userY(line.top + line.height / 2) : NaN,
              };
            })
            .filter((m) => Number.isFinite(m.value) && Number.isFinite(m.y));
          const zero = marks.find((m) => m.value === 0);
          const top = marks.reduce((a, m) => (m.value > a.value ? m : a), marks[0] ?? { value: NaN, y: NaN });
          if (!zero || !Number.isFinite(top.value) || top.value === zero.value) {
            return { error: `刻度不够拼出尺子：${JSON.stringify(marks)}` };
          }
          const valueAt = (y) => ((zero.y - y) / (zero.y - top.y)) * top.value;
          // 只取末尾的数字：柱子上方是裸数字，折线点的 <title> 是「标签 · 记录数 37」
          const printedNum = (text) => {
            const found = /(-?[\d,]+(?:\.\d+)?)\s*$/.exec(String(text ?? '').trim());
            return found ? Number(found[1].replace(/,/g, '')) : null;
          };
          const rows = [
            ...Array.from(svg.querySelectorAll('.zlc-chart-bar-item')).map((item) => {
              const rect = item.querySelector('rect')?.getBoundingClientRect();
              const text = String(item.querySelector('text')?.textContent ?? '').trim();
              return {
                text,
                printed: printedNum(text),
                measured: rect ? Number(valueAt(userY(rect.top)).toFixed(3)) : NaN,
              };
            }),
            ...Array.from(svg.querySelectorAll('.zlc-chart-dot')).map((dot) => {
              const circle = dot.getBoundingClientRect();
              const text = String(dot.querySelector('title')?.textContent ?? '').trim();
              return {
                text,
                printed: printedNum(text),
                measured: circle ? Number(valueAt(userY(circle.top + circle.height / 2)).toFixed(3)) : NaN,
              };
            }),
          ];
          return { ticks: marks.map((m) => m.value), rows };
        }, svgSelector);
        if (got.error) {
          check(`${label}：图形高度用轴刻度量回来 == 图上印的数`, false, got.error);
          return;
        }
        const bad = got.rows.filter((row) => {
          if (!Number.isFinite(row.measured)) return true;
          // SQL NULL 那组印的是「—」，规矩是"标成破折号 + 零高"，画出一根看得见高度的柱子就是骗人
          if (row.text.includes('—')) return Math.abs(row.measured) > 0.05;
          if (row.printed === null) return true;
          if (row.printed === 0) return Math.abs(row.measured) > 0.05;
          return Math.abs(row.measured - row.printed) > Math.max(0.25, Math.abs(row.printed) * 0.02);
        });
        check(`${label}：图形高度用轴刻度量回来 == 图上印的数`,
          got.rows.length > 0 && bad.length === 0,
          `ticks=${got.ticks.join(',')} rows=${got.rows.map((r) => `${r.text || '∅'}~${r.measured}`).join(' ')}`);
      };

      // 期望值来自行级真值，而不是 seed 常量：前面几步（内联编辑、看板拖拽、表单新建）
      // 早就把数据改过了，写死 4/2.5/1 只会测出一个假绿灯。
      const rowsRes = await fetch(
        `${API}/api/lc/runtime/list?entityCode=task&appCode=${appCode}&tenantCode=default`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({ page: 1, size: 200 }),
        },
      ).then((r) => r.json());
      const recs = rowsRes?.data?.records ?? [];
      check('图表用例读到了行级数据', recs.length >= 3, `records=${recs.length}`);

      const groupOf = (r) => r.prio_label || r.prio || '（未填写）';
      const sumBy = (keyFn) => {
        const acc = new Map();
        for (const r of recs) {
          const k = keyFn(r);
          if (k === null) continue;
          const cur = acc.get(k) ?? { sum: 0, seen: 0 };
          const v = r.hours === null || r.hours === undefined ? null : Number(r.hours);
          if (v !== null && Number.isFinite(v)) {
            cur.sum += v;
            cur.seen += 1;
          }
          acc.set(k, cur);
        }
        return acc;
      };
      const countBy = (keyFn) => {
        const acc = new Map();
        for (const r of recs) {
          const k = keyFn(r);
          if (k === null) continue;
          acc.set(k, (acc.get(k) ?? 0) + 1);
        }
        return acc;
      };
      const fmt = (n) => {
        if (n === null) return '—';
        const v = Number(n.toFixed(4));
        return Number.isInteger(v) ? v.toLocaleString('en-US') : String(Number(v.toFixed(2)));
      };
      const pairs = (map, valueOf) => Array.from(map, ([label, agg]) => `${label}=${valueOf(agg)}`).sort();
      const countMap = countBy(groupOf);
      const hoursMap = sumBy(groupOf);
      const dayMap = countBy((r) => (r.due ? String(r.due).slice(0, 10) : null));
      const monthMap = countBy((r) => (r.due ? String(r.due).slice(0, 7) : null));
      const totalHours = recs.reduce((acc, r) => acc + (Number.isFinite(Number(r.hours)) ? Number(r.hours) : 0), 0);

      await page.goto(`${BASE}/${appCode}/task/CHART`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await waitCount('.zlc-chart-bar-item', countMap.size);
      const barLabels = await textsIn('.zlc-chart-axis-label');
      const barValues = await textsIn('.zlc-chart-bar-item text');
      check('柱状图分组轴与计数都等于行级真值',
        pairs(countMap, (c) => c).join('|') === barLabels.map((l, i) => `${l}=${barValues[i]}`).sort().join('|'),
        `expect=${pairs(countMap, (c) => c).join('|')} got=${barLabels.map((l, i) => `${l}=${barValues[i]}`).sort().join('|')}`);
      const barText = await page.locator('body').innerText();
      check('图表说清口径（分组列 + 度量 + 是否筛选）',
        barText.includes('按「优先级」分组') && barText.includes('记录数') && barText.includes('未筛选'),
        barText.slice(0, 120));
      check('默认分组轴不是标题列', lastAgg().groupField === 'prio', JSON.stringify(lastAgg()));
      await checkDrawn('柱状图(计数)', '.zlc-chart-bar');
      await shot(page, `r${runNum}-10-chart-bar`);

      // 合计(工时)：这些数只能来自 SQL SUM —— 尤其"某组全是 NULL"必须画成 —，不是 0
      await pickOption('.zlc-chart-metric-fn', '合计');
      await waitCount('.zlc-chart-bar-item', countMap.size);
      const expectedSums = pairs(hoursMap, (a) => fmt(a.seen ? a.sum : null));
      const expectedSumTexts = Array.from(hoursMap.values(), (a) => fmt(a.seen ? a.sum : null)).sort().join('|');
      await page.waitForFunction(
        (exp) => Array.from(document.querySelectorAll('.zlc-chart-bar-item text'))
          .map((t) => t.textContent).sort().join('|') === exp,
        expectedSumTexts,
        { timeout: 15000 },
      );
      // 换了度量就换了排序键，轴标签必须重读 —— 拿旧标签配新数值会得出假结论
      const sumLabels = await textsIn('.zlc-chart-bar-item text');
      const sumAxis = await textsIn('.zlc-chart-axis-label');
      const sumPairs = sumAxis.map((l, i) => `${l}=${sumLabels[i]}`).sort().join('|');
      check('数值聚合走服务端', sumPairs === expectedSums.join('|'), `expect=${expectedSums.join('|')} got=${sumPairs}`);
      check('聚合请求带上了 SUM(hours)',
        JSON.stringify(lastAgg().aggregations) === JSON.stringify({ hours: ['SUM'] }), JSON.stringify(lastAgg()));
      const nullGroup = Array.from(hoursMap, ([label, a]) => (a.seen ? null : label)).filter(Boolean);
      check('全 NULL 的组画破折号而不是 0',
        nullGroup.length === 0 || sumPairs.includes(`${nullGroup[0]}=—`), `nullGroup=${nullGroup.join(',')} ${sumPairs}`);
      await checkDrawn('柱状图(合计工时)', '.zlc-chart-bar');

      // 时间轴：先按月（同月 -> 1 个点），再按天，证明分桶粒度真的换了 SQL 而不是前端重排
      await page.locator('.ant-segmented-item', { hasText: '折线图' }).first().click({ timeout: 5000 });
      await pickOption('.zlc-chart-group', '分组：截止日期');
      await waitCount('.zlc-chart-dot', monthMap.size);
      check('日期列默认按月分桶，同月记录收进一个桶',
        lastAgg().timeGroup === 'MONTH' && lastAgg().groupField === 'due', JSON.stringify(lastAgg()));
      await pickOption('.zlc-chart-timegroup', '按天');
      await waitCount('.zlc-chart-dot', dayMap.size);
      check('切到按天真的换了下发的 timeGroup', lastAgg().timeGroup === 'DAY', JSON.stringify(lastAgg()));
      const dayAxis = await textsIn('.zlc-chart-axis-label');
      const expectedDays = Array.from(dayMap.keys()).sort();
      // 点多于 12 个时前端会抽稀轴标签，所以"标签 == 整条期望轴"是假命题（数据一长就假红）。
      // 换成：标签必须是期望轴的**保序子集**，并且首尾都在 —— 首尾丢了读者就不知道这根轴覆盖哪一段。
      const isOrderedSubset = (sub, all) => {
        let i = 0;
        for (const x of all) if (x === sub[i]) i += 1;
        return sub.length > 0 && i === sub.length;
      };
      check('时间轴正序、桶标签补零，且未填日期的记录没进轴',
        isOrderedSubset(dayAxis, expectedDays)
        && dayAxis[0] === expectedDays[0]
        && dayAxis[dayAxis.length - 1] === expectedDays[expectedDays.length - 1],
        `expect⊇=${expectedDays.join('|')} got=${dayAxis.join('|')}`);
      const dots = await page.$$eval('.zlc-chart-dot', (ns) => ns.map((n) => ({
        x: Number(n.getAttribute('cx')),
        label: (n.querySelector('title')?.textContent ?? '').split(' · ')[0],
      })));
      const labelled = await page.$$eval('.zlc-chart-axis-label', (ns) => ns
        .map((n) => ({ x: Number(n.getAttribute('x')), text: n.textContent ?? '' })));
      check('折线点 x 递增、每个轴标签落在自己那个点上且文字就是那个桶',
        dots.every((d, i) => i === 0 || d.x > dots[i - 1].x)
        && labelled.every((l) => {
          const owner = dots.find((d) => Math.abs(d.x - l.x) < 0.5);
          return !!owner && owner.label === l.text;
        })
        && Math.abs(labelled[labelled.length - 1].x - dots[dots.length - 1].x) < 0.5,
        `dots=${dots.map((d) => d.x).join(',')} labels=${labelled.map((l) => `${l.text}@${l.x}`).join(',')}`);
      await checkDrawn('折线图(按天)', '.zlc-chart-line');
      await shot(page, `r${runNum}-10-chart-line`);

      // 饼图 + 指标卡：同一份聚合结果换画法，指标卡不许再带分组
      await page.locator('.ant-segmented-item', { hasText: '饼图' }).first().click({ timeout: 5000 });
      await pickOption('.zlc-chart-group', '分组：优先级');
      // 扇区数在换组前后可能一样，只能等内容真的变成期望的分组
      await page.waitForFunction(
        (exp) => Array.from(document.querySelectorAll('.zlc-chart-legend li'))
          .map((li) => `${li.querySelector('.zlc-chart-legend-label')?.textContent ?? ''}=${li.querySelector('.zlc-chart-legend-value')?.textContent ?? ''}`)
          .sort().join('|') === exp,
        expectedSums.join('|'),
        { timeout: 15000 },
      );
      const legendLabels = await textsIn('.zlc-chart-legend-label');
      const legendValues = await textsIn('.zlc-chart-legend-value');
      const legendPairs = legendLabels.map((l, i) => `${l}=${legendValues[i]}`).sort().join('|');
      check('饼图图例数值与聚合一致', legendPairs === expectedSums.join('|'), `expect=${expectedSums.join('|')} got=${legendPairs}`);
      check('饼图扇区数与分组数一致',
        (await page.locator('.zlc-chart-slice').count()) === countMap.size, `slices=${await page.locator('.zlc-chart-slice').count()}`);
      // 尺寸断言：SVG 的 width:100% 会把 260 的甜甜圈撑满 940，圆心数字飞出画面
      const pieBox = await page.locator('.zlc-chart-pie').boundingBox();
      check('环图按自身尺寸渲染，没被容器撑成一张巨饼',
        !!pieBox && pieBox.width > 200 && pieBox.width < 320, `w=${pieBox?.width}`);
      await checkRing('单图页环图', '.zlc-chart-slice');
      await shot(page, `r${runNum}-10-chart-pie`);
      await page.locator('.ant-segmented-item', { hasText: '指标卡' }).first().click({ timeout: 5000 });
      await page.waitForFunction(
        (exp) => document.querySelector('.zlc-chart-number-value')?.textContent === exp,
        fmt(totalHours),
        { timeout: 15000 },
      );
      check('指标卡问的是总量，不再带分组轴',
        lastAgg().groupField === undefined && lastAgg().timeGroup === undefined, JSON.stringify(lastAgg()));
      check('指标卡数值等于全表 SUM(hours)',
        (await textsIn('.zlc-chart-number-value')).join('') === fmt(totalHours), `expect=${fmt(totalHours)}`);
      await shot(page, `r${runNum}-10-chart-number`);

      /* 10b. 命名图表视图：存的是服务端，不是这台浏览器的 localStorage */
      const modal = () => page.locator(".ant-modal-wrap:not([style*='display: none']) .ant-modal-content");
      const viewName = `按天工时趋势 r${runNum}`;

      await page.locator('.ant-segmented-item', { hasText: '折线图' }).first().click({ timeout: 5000 });
      await pickOption('.zlc-chart-group', '分组：截止日期');
      await pickOption('.zlc-chart-timegroup', '按天');
      await waitCount('.zlc-chart-dot', dayMap.size);
      await page.locator('.zlc-chart-save-view').click({ timeout: 5000 });
      await modal().locator('input').first().fill(viewName, { timeout: 5000 });
      await modal().locator('.ant-btn-primary').click({ timeout: 5000 });

      // 存完立刻就该在切换器里 —— 视图列表读的是 workspace 缓存，不主动失效
      // 就会"toast 说保存了、下拉里没有"，得刷页面才出现（这就是那类没做完的坑）
      let selectedName = '';
      await page.waitForFunction(
        (exp) => document.querySelector('.zlc-chart-view-select .ant-select-selection-item')?.textContent === exp,
        viewName,
        { timeout: 15000 },
      ).then(() => { selectedName = viewName; }).catch(async () => {
        selectedName = await page.locator('.zlc-chart-view-select .ant-select-selection-item')
          .textContent().catch(() => '');
      });
      check('保存后不用刷新，命名视图就出现在切换器里',
        selectedName === viewName, `selection=${selectedName}`);

      // 落库真值：直接读接口，确认存进去的确实是这份图表口径而不是别的东西
      const stored = await fetch(
        `${API}/api/lc/view-config/list?appCode=${appCode}&entityCode=task&tenantCode=default`,
        { headers: { 'X-Tenant-Code': 'default' } },
      ).then((r) => r.json());
      const savedRow = (stored?.data ?? []).find((v) => {
        try { return JSON.parse(v.config ?? '{}').name === viewName; } catch { return false; }
      });
      const savedCfg = savedRow ? JSON.parse(savedRow.config) : {};
      check('命名视图按 viewType=CHART 落库，配置是折线+截止日期+按天',
        !!savedRow && savedRow.viewType === 'CHART'
        && savedCfg.kind === 'line' && savedCfg.groupField === 'due' && savedCfg.timeGroup === 'DAY',
        `row=${JSON.stringify({ viewType: savedRow?.viewType, cfg: savedCfg })}`);

      // 换掉工具栏状态再重载：证明"能从服务端还原"不是 localStorage 的功劳
      await page.locator('.ant-segmented-item', { hasText: '柱状图' }).first().click({ timeout: 5000 });
      await pickOption('.zlc-chart-group', '分组：优先级');
      await waitCount('.zlc-chart-bar-item', countMap.size);
      await page.goto(`${BASE}/${appCode}/task/CHART`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      chartReqs.length = 0;
      await waitCount('.zlc-chart-bar-item', countMap.size);
      check('重载后是本机上次那份配置（柱状图），说明还原前并没偷用服务端那份',
        lastAgg().groupField === 'prio' && (lastAgg().timeGroup ?? null) === null, JSON.stringify(lastAgg()));
      await page.locator('.zlc-chart-view-select').first().click({ timeout: 5000 });
      await page.locator(
        `.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option[title="${viewName}"]`,
      ).first().click({ timeout: 5000 });
      await waitCount('.zlc-chart-dot', dayMap.size);
      check('从服务端视图还原：折线、按天分桶、分组列都回到存进去时的样子',
        lastAgg().groupField === 'due' && lastAgg().timeGroup === 'DAY', JSON.stringify(lastAgg()));
      await shot(page, `r${runNum}-10b-chart-saved-view`);

      /* ---- 10c. 仪表盘：把落库的命名图表视图拼成一屏，口径必须和单图页一模一样 ---- */
      try {
        const listViews = async () => {
          const res = await fetch(`${API}/api/lc/view-config/list?appCode=${appCode}&tenantCode=default`, {
            headers: { 'X-Tenant-Code': 'default' },
          }).then((r) => r.json());
          return res?.data ?? [];
        };
        const parseCfg = (row) => {
          try { return JSON.parse(row?.config ?? '{}'); } catch { return {}; }
        };

        // 第二张图走接口建：这一步只验仪表盘，"把图存成命名视图"已由 10b 覆盖
        const pieViewName = `优先级工时占比 r${runNum}`;
        const second = await fetch(`${API}/api/lc/view-config/create`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({
            tenantCode: 'default',
            appCode,
            entityCode: 'task',
            viewType: 'CHART',
            config: JSON.stringify({
              kind: 'pie', groupField: 'prio', metricFn: 'SUM', metricField: 'hours',
              topN: 8, conditions: [], conjunction: 'AND', name: pieViewName,
            }),
          }),
        }).then((r) => r.json());
        check('仪表盘用例备好了第二张命名图表视图', !!second.success, second.message);

        await page.goto(`${BASE}/${appCode}/dashboard`, { waitUntil: 'domcontentloaded', timeout: 20000 });
        chartReqs.length = 0;
        await page.locator('.zlc-dash-empty').waitFor({ timeout: 15000 });
        const emptyText = await page.locator('body').innerText();
        check('一份仪表盘都还没有时，空态说的是"还没有仪表盘"',
          emptyText.includes('还没有仪表盘') && !emptyText.includes('这个仪表盘还是空的'),
          emptyText.slice(0, 100));

        const dashName = `经营看板 r${runNum}`;
        await page.locator('.zlc-dash-new').click({ timeout: 5000 });
        await page.locator('input.zlc-dash-name').first().fill(dashName, { timeout: 5000 });
        await pickOption('.zlc-dash-add', viewName);
        await waitCount('.zlc-dash-tile', 1);
        await pickOption('.zlc-dash-add', pieViewName);
        await waitCount('.zlc-dash-tile', 2);
        // 两块组件各发各的聚合请求 —— 拼一屏不能变成"一次查询画两张图"
        await waitCount('.zlc-chart-dot', dayMap.size);
        await waitCount('.zlc-chart-slice', countMap.size);
        check('放两张图就是两次各自口径的聚合请求',
          chartReqs.filter((r) => r && (r.groupField === 'due' || r.groupField === 'prio')).length === 2,
          JSON.stringify(chartReqs));

        await page.locator('.zlc-dash-save').click({ timeout: 5000 });
        await page.waitForFunction(() => !document.querySelector('.zlc-dash-dirty'), null, { timeout: 15000 });
        const dashUrl = page.url();
        check('保存后地址栏指向服务端新建的那一份',
          new RegExp(`/${appCode}/dashboard/\\d+$`).test(dashUrl), dashUrl);

        const rows = await listViews();
        const dashRow = rows.find((row) => row.viewType === 'DASHBOARD' && parseCfg(row).name === dashName);
        const dashCfg = parseCfg(dashRow);
        const trendId = savedRow?.id;
        const pieId = rows.find((row) => parseCfg(row).name === pieViewName)?.id;
        check('仪表盘落库是一行 viewType=DASHBOARD、entity_code 用应用级占位',
          !!dashRow && dashRow.entityCode === '*' && Array.isArray(dashCfg.widgets) && dashCfg.widgets.length === 2,
          JSON.stringify({ entityCode: dashRow?.entityCode, viewType: dashRow?.viewType, cfg: dashCfg }));
        check('组件存的是命名视图的编号而不是配置副本',
          JSON.stringify((dashCfg.widgets ?? []).map((w) => w.viewId)) === JSON.stringify([trendId, pieId]),
          `expect=${JSON.stringify([trendId, pieId])} got=${JSON.stringify((dashCfg.widgets ?? []).map((w) => w.viewId))}`);
        await shot(page, `r${runNum}-10c-dashboard`);

        // 仪表盘与实体视图共用 z_lc_view_config，代价是那一行的 entity_code 是 '*'：
        // 实体的图表下拉绝不能把它捞出来，否则"给表格用的视图"和"整个应用的仪表盘"会混在一个列表里
        await page.goto(`${BASE}/${appCode}/task/CHART`, { waitUntil: 'domcontentloaded', timeout: 20000 });
        await page.locator('.zlc-chart-view-select').first().click({ timeout: 5000 });
        const chartOptions = await page.$$eval(
          '.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option',
          (ns) => ns.map((n) => n.getAttribute('title') ?? n.textContent ?? ''),
        );
        check('应用级的仪表盘行不会混进实体的图表视图下拉',
          !chartOptions.includes(dashName) && chartOptions.includes(viewName),
          chartOptions.join('|'));
        await page.keyboard.press('Escape');

        // 重新打开：整屏来自服务端，不靠这台浏览器的 localStorage
        await page.goto(dashUrl, { waitUntil: 'domcontentloaded', timeout: 20000 });
        chartReqs.length = 0;
        await waitCount('.zlc-dash-tile', 2);
        await waitCount('.zlc-chart-dot', dayMap.size);
        await waitCount('.zlc-chart-slice', countMap.size);
        await checkRing('仪表盘缩略后的环图', '.zlc-dash-tile .zlc-chart-slice');
        const dueReq = chartReqs.find((r) => r?.groupField === 'due') ?? {};
        const prioReq = chartReqs.find((r) => r?.groupField === 'prio') ?? {};
        check('重开后日期那张仍按天分桶（服务端 config 说了算）',
          dueReq.timeGroup === 'DAY', JSON.stringify(dueReq));
        check('重开后度量那张仍走服务端 SUM(hours)',
          JSON.stringify(prioReq.aggregations) === JSON.stringify({ hours: ['SUM'] }), JSON.stringify(prioReq));

        // 轴的刻度是 SVG <text>：innerText 在它上面是 undefined，只能读 textContent
        const lineAxis = (await textsIn('.zlc-dash-tile .zlc-chart-axis-label')).map((s) => s.trim());
        check('仪表盘上折线的日期轴等于行级真值（和单图页同一份期望）',
          lineAxis.join('|') === expectedDays.join('|'),
          `expect=${expectedDays.join('|')} got=${lineAxis.join('|')}`);
        const legendText = await page.$$eval('.zlc-chart-legend li', (ns) => ns.map((li) =>
          `${li.querySelector('.zlc-chart-legend-label')?.textContent ?? ''}=${li.querySelector('.zlc-chart-legend-value')?.textContent ?? ''}`).sort().join('|'));
        check('仪表盘上饼图的数等于单图页那份聚合真值',
          legendText === expectedSums.join('|'),
          `expect=${expectedSums.join('|')} got=${legendText}`);
        const captions = await page.locator('.zlc-dash-tile-caption').allInnerTexts();
        check('两块组件各自说清自己的口径',
          (captions[0] ?? '').includes('按「截止日期」分组') && (captions[0] ?? '').includes('按天')
          && (captions[1] ?? '').includes('工时 合计') && (captions[1] ?? '').includes('未筛选'),
          captions.join(' || '));

        // 布局也落库：改宽度 -> 保存 -> 再开还是整行
        await page.locator('.zlc-dash-tile-width').first().click({ timeout: 5000 });
        await page.waitForFunction(() => !!document.querySelector('.zlc-dash-dirty'), null, { timeout: 10000 });
        await page.locator('.zlc-dash-save').click({ timeout: 5000 });
        await page.waitForFunction(() => !document.querySelector('.zlc-dash-dirty'), null, { timeout: 15000 });
        const afterWidth = parseCfg((await listViews()).find((row) => row.id === dashRow?.id));
        check('改组件宽度写回服务端',
          (afterWidth.widgets ?? [])[0]?.width === 2 && (afterWidth.widgets ?? [])[1]?.width === 1,
          JSON.stringify(afterWidth.widgets));

        // 引用断掉：删掉一张被引用的图，仪表盘必须点名，而不是悄悄少一块
        const del = await fetch(`${API}/api/lc/view-config/delete`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({ id: trendId }),
        }).then((r) => r.json());
        check('用例删掉了被引用的图表视图', !!del.success, del.message);
        await page.goto(dashUrl, { waitUntil: 'domcontentloaded', timeout: 20000 });
        chartReqs.length = 0;
        await waitCount('.zlc-dash-tile', 2);
        const brokenText = await page.locator('body').innerText();
        check('引用断掉的那块组件点名说是谁丢了，另一块照常出图',
          brokenText.includes(`图表视图 #${trendId} 已被删除`),
          brokenText.slice(0, 160).replace(/\n/g, ' | '));
        // 等到剩下的那块"画出了东西"再数：只等两块组件挂上，是在等一个还没回来的聚合响应 ——
        // 实测 4 轮里输过一次（slices=0，而 reqs 里那条 prio 请求明明在飞）。
        // 这里只等 ">0" 不等 "==真值"：等成断言本身会让下面那条变成永远绿。
        await page.waitForFunction(
          () => document.querySelectorAll('.zlc-chart-slice').length > 0,
          null, { timeout: 15000 },
        );
        check('丢了一块不会连带让剩下的图取不到数',
          (await page.locator('.zlc-chart-slice').count()) === countMap.size
          && chartReqs.some((r) => r?.groupField === 'prio')
          && !chartReqs.some((r) => r?.groupField === 'due'),
          `slices=${await page.locator('.zlc-chart-slice').count()} reqs=${JSON.stringify(chartReqs.map((r) => r?.groupField))}`);
        await shot(page, `r${runNum}-10d-dashboard-missing`);
      } catch (e) {
        check('仪表盘', false, e?.message);
        await shot(page, `r${runNum}-10c-dashboard-FAIL`);
      }
    } catch (e) {
      check('图表视图', false, e?.message);
      await shot(page, `r${runNum}-10-chart-FAIL`);
    }

    /* ---- 10e. 交叉表：两维分组 + 服务端 pivot，格子按渲染几何对行级真值 ---- */
    try {
      const pivotWaitCount = async (selector, expected) => {
        await page.waitForFunction(
          ([sel, n]) => document.querySelectorAll(sel).length === n,
          [selector, expected],
          { timeout: 15000 },
        );
      };
      const pivotPick = async (trigger, title) => {
        await page.locator(trigger).first().click({ timeout: 5000 });
        await page.locator(
          `.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option[title="${title}"]`,
        ).first().click({ timeout: 5000 });
        await page.keyboard.press('Escape');
        await page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)')
          .waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {});
      };
      // 格式化必须与被测组件同一把尺：这里自己数一遍数字再"看起来一样"是最省事的假绿来源
      const nf = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 });
      // 与 chartModel.UNFILLED_GROUP_LABEL 同一句话。写成两份就会漂：漂了不会假绿，
      // 会在真有未填写记录时以"列头对不上"的红出现，但那时先怀疑的是产品。
      const UNFILLED = '（未填写）';

      // 交叉表的几何读取 + 逐格核对，两个维度组合共用一份（下面 10e 与 10e-2 各跑一次）。
      // 每一格用它自己的矩形去对：列头的 x 区间 + 本行行头的 y 区间。
      // 数"有几个数字"对这类缺陷完全免疫 —— colspan 少写一格、列序错位，格子照样都在。
      const readPivotGeometry = () => page.evaluate(() => {
        const table = document.querySelector('.zlc-pivot-table');
        if (!table) return { error: '页面上没有 .zlc-pivot-table' };
        const box = (n) => {
          const r = n.getBoundingClientRect();
          return { left: r.left, right: r.right, top: r.top, bottom: r.bottom };
        };
        const headOf = (n) => ({
          label: (n.textContent ?? '').trim(),
          key: n.getAttribute('data-col-key'),
          ...box(n),
        });
        return {
          heads: [...table.querySelectorAll('thead th.zlc-pivot-colhead')].map(headOf),
          rowHeads: [...table.querySelectorAll('tbody tr th.zlc-pivot-rowhead')].map(headOf),
          cells: [...table.querySelectorAll('tbody tr')].map((tr) => ({
            row: (tr.querySelector('th.zlc-pivot-rowhead')?.textContent ?? '').trim(),
            tds: [...tr.querySelectorAll('td.zlc-pivot-cell')].map((td) => ({
              text: (td.textContent ?? '').trim(), ...box(td),
            })),
          })),
          foot: [...table.querySelectorAll('tfoot td.zlc-pivot-cell')].map((td) => (td.textContent ?? '').trim()),
          grand: (table.querySelector('.zlc-pivot-grandtotal')?.textContent ?? '').trim(),
        };
      });
      // 真值是"行标签 → 列标签 → 记录数"，列标签用的是界面那句话（未填写那一档已经折成 UNFILLED）
      const verifyPivotCells = (geo, truth) => {
        const misplaced = [];
        const wrongValue = [];
        geo.cells.forEach((row, rowIndex) => {
          const rowHead = geo.rowHeads[rowIndex];
          const rowTruth = truth.get(row.row) ?? new Map();
          row.tds.forEach((td, i) => {
            const head = geo.heads[i];
            if (!head) {
              misplaced.push(`${row.row} 第 ${i + 1} 格没有对应的列头`);
              return;
            }
            const cx = (td.left + td.right) / 2;
            if (cx < head.left || cx > head.right) {
              misplaced.push(`${row.row}×${head.label} 的 x=${cx.toFixed(0)} 不在列头 [${head.left.toFixed(0)},${head.right.toFixed(0)}]`);
            }
            if (rowHead && (td.bottom <= rowHead.top || td.top >= rowHead.bottom)) {
              misplaced.push(`${row.row}×${head.label} 的 y 不在本行行头里`);
            }
            const want = rowTruth.get(head.label);
            const got = want === undefined ? '·' : nf.format(want);
            if (td.text !== got) wrongValue.push(`${row.row}×${head.label} 界面=${td.text} 真值=${got}`);
          });
        });
        return { misplaced, wrongValue };
      };

      // 行级真值现读：前面的内联编辑、看板拖拽、表单新建早把数据改过了
      const listRows = async () => (await fetch(
        `${API}/api/lc/runtime/list?entityCode=task&appCode=${appCode}&tenantCode=default`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({ page: 1, size: 200 }),
        },
      ).then((r) => r.json()))?.data?.records ?? [];
      const pvRecs = await listRows();
      check('交叉表用例读到了行级数据', pvRecs.length >= 3, `records=${pvRecs.length}`);
      // 后端的空值档 label 就是 NULL，pivot 拿值当键 → 界面上那一档叫「（未填写）」。
      // 两维各自一个函数：10e 用 优先级×标题，10e-2 反过来用 标题×优先级（让空档落到列维度上）。
      const prioBucket = (r) => r.prio_label || r.prio || UNFILLED;
      const titleBucket = (r) => (r.title === null || r.title === undefined || r.title === '' ? UNFILLED : r.title);
      const truthByRow = (recs, rowOf, colOf) => {
        const map = new Map();
        for (const r of recs) {
          const rk = rowOf(r);
          const ck = colOf(r);
          if (!map.has(rk)) map.set(rk, new Map());
          map.get(rk).set(ck, (map.get(rk).get(ck) ?? 0) + 1);
        }
        return map;
      };
      const byRow = truthByRow(pvRecs, prioBucket, titleBucket);
      const wantCols = [...new Set(pvRecs.map(titleBucket))].sort();

      await page.goto(`${BASE}/${appCode}/task/PIVOT`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.waitForSelector('.zlc-pivot-toolbar', { timeout: 15000 });
      const incompleteText = await page.locator('body').innerText();
      check('只选了一个维度时不发整形请求，并说清还缺什么',
        shapeReqs.length === 0 && incompleteText.includes('还没选列维度'),
        `reqs=${shapeReqs.length} text=${incompleteText.slice(0, 120).replace(/\n/g, ' | ')}`);

      await pivotPick('.zlc-pivot-row', '优先级');
      await pivotPick('.zlc-pivot-col', '标题');
      await pivotWaitCount('.zlc-pivot-table tbody tr', byRow.size);

      const geo = await readPivotGeometry();
      if (geo.error) throw new Error(geo.error);

      check('列头就是第二维在数据里真实出现过的取值（透视出来的列，不是配置里写死的）',
        JSON.stringify(geo.heads.map((h) => h.label).sort()) === JSON.stringify(wantCols),
        `want=${JSON.stringify(wantCols)} got=${JSON.stringify(geo.heads.map((h) => h.label))}`);
      check('行数等于第一维的取值数（既没折行也没多出一行）',
        geo.rowHeads.length === byRow.size && geo.cells.length === byRow.size,
        `rows=${geo.cells.length} want=${byRow.size}`);

      // 后端直接拿维度的值当行键/列键，该维度为 NULL 时那个键就是空串（18090 实测）。
      // 原样画出去是一根没有标题的列 / 一行没有名字的行 —— 所以两头都断：
      // 不许出现空白表头，且"未填写"那一档带的是后端那个空键（证明改的是显示、不是键）。
      // ⚠ 这一条是数据条件式的：hasUnfilled 为 false 时（标题是必填列，正常跑不出空档）它只断到
      // "不许空白表头"。把「未填写」那一列真的画出来的正例在下面 10e-2 —— 那里往库里清一条记录。
      const blankHeads = [...geo.heads, ...geo.rowHeads].filter((h) => h.label === '');
      const hasUnfilled = pvRecs.some((r) => titleBucket(r) === UNFILLED);
      const unfilledHeads = geo.heads.filter((h) => h.label === UNFILLED);
      check('空值那一档在界面上报得出名字，且它认领的仍是后端给的那个空键',
        blankHeads.length === 0
        && unfilledHeads.length === (hasUnfilled ? 1 : 0)
        && (!hasUnfilled || unfilledHeads[0]?.key === ''),
        `blank=${blankHeads.length} 界面里的未填写列=${unfilledHeads.length}`
        + ` key=${JSON.stringify(unfilledHeads[0]?.key)} 数据里有未填写列=${hasUnfilled}`);

      const { misplaced, wrongValue } = verifyPivotCells(geo, byRow);
      check('每一格都落在自己那一列的列头下面、自己那一行的行头右边',
        misplaced.length === 0, misplaced.slice(0, 3).join(' ; '));
      check('交叉表每一格都等于行级真值（没有记录的组合是 ·，不是 0）',
        wrongValue.length === 0, wrongValue.slice(0, 3).join(' ; '));

      const dots = geo.cells.reduce((n, r) => n + r.tds.filter((td) => td.text === '·').length, 0);
      const wantDots = geo.cells.reduce(
        (n, r) => n + (geo.heads.length - (byRow.get(r.row)?.size ?? 0)), 0,
      );
      // 分母守卫：这份数据里要是根本没有空组合，上一条"缺格是 ·"就是条空断言
      check('用例真的含空组合（否则上一条缺格显示 · 是条空断言）',
        wantDots > 0 && dots === wantDots, `dots=${dots} want=${wantDots}`);

      const wantFoot = geo.heads.map((h) => nf.format(pvRecs.filter((r) => titleBucket(r) === h.label).length));
      check('页脚列合计等于行级真值按列数出来的记录数',
        JSON.stringify(geo.foot) === JSON.stringify(wantFoot),
        `want=${JSON.stringify(wantFoot)} got=${JSON.stringify(geo.foot)}`);
      check('总计是全部分组的记录数，不是"屏上可见格子之和"',
        geo.grand === nf.format(pvRecs.length), `grand=${geo.grand} recs=${pvRecs.length}`);

      const body = shapeReqs[0] ?? {};
      const [step] = body.shape ?? [];
      check('整形请求带的是两个维度（行在前）+ SUM(group_count) 的透视程序',
        JSON.stringify(body.groupFields) === JSON.stringify(['prio', 'title'])
        && step?.op === 'pivot' && step?.agg === 'SUM'
        && step?.value === 'group_count' && step?.on === 'group_label_2',
        JSON.stringify(body).slice(0, 220));
      check('选齐两个维度后只发一次 /runtime/shape',
        shapeReqs.length === 1, `reqs=${shapeReqs.length}`);
      await shot(page, `r${runNum}-10e-pivot`);

      const shapeBefore = shapeReqs.length;
      await page.waitForTimeout(2000);
      check('交叉表安静下来后不再自己重查（2s 内不该再有 /runtime/shape）',
        shapeReqs.length === shapeBefore, `churn=${shapeReqs.length - shapeBefore}`);

      /* ---- 10e-2. 「未填写」那一列的正例：把空档造在列维度上 ---- */
      // 18090 实测两件事才敢这么写：fieldValues 里给 null 是"这一列不动"（记录原样留着 P2），
      // 要给空串才真的清空；清空后多维聚合回来的 group_key_2 / group_label_2 都是空串，
      // /runtime/shape 于是产出一个键为 "" 的列 —— 界面上那一根就是 pivotColumnLabel 唯一走到的分支。
      const clearTarget = pvRecs.find((r) => prioBucket(r) !== UNFILLED);
      const cleared = await fetch(
        `${API}/api/lc/runtime/update?entityCode=task&appCode=${appCode}&tenantCode=default`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({
            appCode, tenantCode: 'default', fieldValues: { id: clearTarget?.id, prio: '' },
          }),
        },
      ).then((r) => r.json());
      check('用例真的把一条记录的优先级清掉了（空档不是假设出来的）',
        cleared?.success === true, `id=${clearTarget?.id} resp=${JSON.stringify(cleared).slice(0, 160)}`);

      const pvRecs2 = await listRows();
      const unfilledRecs = pvRecs2.filter((r) => prioBucket(r) === UNFILLED);
      // 分母守卫：清空没生效 / 把全部记录都清了，下面三条就都退化成空断言
      check('库里现在既有"没有优先级"的记录也有有优先级的（否则下面是空断言）',
        unfilledRecs.length >= 1 && unfilledRecs.length < pvRecs2.length,
        `unfilled=${unfilledRecs.length} recs=${pvRecs2.length}`);

      await pivotPick('.zlc-pivot-row', '标题');
      await pivotPick('.zlc-pivot-col', '优先级');
      const byRow2 = truthByRow(pvRecs2, titleBucket, prioBucket);
      // 只数行头会等出一张过期的表：这份数据里"优先级的档数"和"标题的个数"都是 2。
      // 所以要等的是列键 —— 后端给的那个空串键不会因为界面换了叫法而变，
      // 于是"等到真到位"和"下面那条断言标签"是两件事，后者仍然可以单独红。
      const wantKeys2 = [...new Set(pvRecs2.map((r) => r.prio_label || r.prio || ''))].sort();
      await page.waitForFunction(
        ([sel, want]) => {
          const got = [...document.querySelectorAll(`${sel} thead th.zlc-pivot-colhead`)]
            .map((n) => n.getAttribute('data-col-key') ?? '').sort();
          return JSON.stringify(got) === want;
        },
        ['.zlc-pivot-table', JSON.stringify(wantKeys2)],
        { timeout: 15000 },
      );

      const geo2 = await readPivotGeometry();
      if (geo2.error) throw new Error(geo2.error);
      check('换维度组合（标题×优先级）之后行数仍等于行维度的档数',
        geo2.rowHeads.length === byRow2.size && geo2.cells.length === byRow2.size,
        `rows=${geo2.cells.length} want=${byRow2.size}`);

      const heads2 = geo2.heads.map((h) => h.label);
      const unfilledCol = geo2.heads.find((h) => h.label === UNFILLED);
      check('未填写那一列画得出来：没有任何一根列是空白标题，且它认领的还是后端那个空键',
        geo2.heads.every((h) => h.label !== '') && !!unfilledCol && unfilledCol.key === '',
        `heads=${JSON.stringify(heads2)} key=${JSON.stringify(unfilledCol?.key)}`);
      // 只画出一根表头不算数：那一列里的格子要按行级真值落位（键没被标签替换掉才可能对得上）
      const { misplaced: misplaced2, wrongValue: wrongValue2 } = verifyPivotCells(geo2, byRow2);
      check('未填写那一列的格子按行级真值落位，不是摆在最右边的装饰列',
        misplaced2.length === 0 && wrongValue2.length === 0,
        [...misplaced2, ...wrongValue2].slice(0, 3).join(' ; '));
      const blankIndex = heads2.indexOf(UNFILLED);
      check('未填写那一列的列合计等于没有优先级的记录数，总计仍是全部记录数',
        geo2.foot[blankIndex] === nf.format(unfilledRecs.length)
        && geo2.grand === nf.format(pvRecs2.length),
        `foot=${JSON.stringify(geo2.foot)} idx=${blankIndex} unfilled=${unfilledRecs.length} grand=${geo2.grand}`);
      check('这一回的行/列维度没有写反：groupFields 是 标题在前、优先级在后',
        JSON.stringify((shapeReqs[shapeReqs.length - 1] ?? {}).groupFields)
        === JSON.stringify(['title', 'prio']),
        JSON.stringify((shapeReqs[shapeReqs.length - 1] ?? {}).groupFields));
      await shot(page, `r${runNum}-10e2-pivot-unfilled`);
    } catch (e) {
      check('交叉表视图', false, e?.message);
      await shot(page, `r${runNum}-10e-pivot-FAIL`);
    }

    /* ---- 11. 管理/设计页 ---- */
    try {
      for (const [route, label] of [
        ['/admin/dicts', '数据字典'],
        ['/admin/views', '视图配置'],
        ['/admin/pipelines', '处理流水线'],
        ['/db-import', '从数据库导入'],
      ]) {
        await page.goto(`${BASE}${route}`, { waitUntil: 'networkidle', timeout: 15000 });
        await page.waitForTimeout(1200);
        const text = await page.locator('body').innerText();
        check(`${label} 页面渲染正常`, text.includes(label), text.slice(0, 60));
      }
      await shot(page, `r${runNum}-08-admin`);
    } catch (e) {
      check('管理/设计页', false, e?.message);
      await shot(page, `r${runNum}-08-admin-FAIL`);
    }

    /* ---- 11a. 处理流水线页：画出来的顺序就是引擎要跑的顺序（#41）---- */
    // 这一节问的是"界面说的话后端兑不兑现"。种一份**合法但数组位置与 order 不一致**的配置:
    // 幽灵阶段 / AFTER_* / 空链现在都在写入口就被拒了, 浏览器里再也造不出那种脏行 ——
    // 还能造出来的错位只剩这一种, 而它正好问得出"表格按哪个排"。
    try {
      const pipeSeed = await fetch(`${API}/api/lc/pipeline-config/create`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
        body: JSON.stringify({
          tenantCode: 'default', appCode, entityCode: 'task', triggerEvent: 'BEFORE_CREATE', enabled: 1,
          // 数组第一项是 DICT_RESOLVE 而它的 order 是 3: 界面上它必须排在最后
          stages: JSON.stringify([
            { type: 'DICT_RESOLVE', config: {}, order: 3 },
            { type: 'VALUE_VALIDATE', config: {}, order: 2 },
            { type: 'REQUIRED_CHECK', config: {}, order: 0 },
            { type: 'TYPE_CONVERT', config: {}, order: 1 },
          ]),
        }),
      }).then((r) => r.json());
      check('种下一份 order 与数组位置不一致的配置（这一节的前提）',
        pipeSeed?.success === true, JSON.stringify(pipeSeed).slice(0, 180));

      if (pipeSeed?.success) {
        await page.goto(`${BASE}/admin/pipelines?appCode=${appCode}`, { waitUntil: 'networkidle', timeout: 20000 });
        await page.locator('[data-testid^="pipeline-stage-"]').first().waitFor({ state: 'visible', timeout: 15000 });
        const stageTags = (await page.locator('[data-testid^="pipeline-stage-"]').allInnerTexts())
          .map((t) => t.replace(/\s+/g, ' ').trim());
        check('阶段链按 order 画，不是照抄 JSON 数组的位置（数组第一项是字典解析，画出来它排最后）',
          stageTags.length === 4 && stageTags[0].includes('必填校验') && stageTags[1].includes('类型转换')
          && stageTags[2].includes('值域校验') && stageTags[3].includes('字典解析'),
          JSON.stringify(stageTags));
        check('编号就是 1..N 的执行位数',
          stageTags.every((t, i) => t.startsWith(`${i + 1}.`)), JSON.stringify(stageTags));
        check('后端认得的阶段编码原样写在每一格里（只有中文名的话，词表漂了没人看得出来）',
          ['REQUIRED_CHECK', 'TYPE_CONVERT', 'VALUE_VALIDATE', 'DICT_RESOLVE']
            .every((c, i) => (stageTags[i] ?? '').includes(c)), JSON.stringify(stageTags));
        // 这一条**不按行号读**：它断言的是"哪一档带这个牌子"，与行序无关（行序归上面那条管）。
        // 原来写成 `stageTags[3]`，S1（不按 order 排）一注入就把 DICT_RESOLVE 挪出第 4 格，
        // 于是这一条跟着红 —— 一条检查红在别人的缺陷上，它就再也不是自己那句保证的证据了。
        const noopRows = stageTags.filter((t) => t.includes('写路径暂不做事'));
        const gateRows = stageTags.filter((t) => /REQUIRED_CHECK|TYPE_CONVERT|VALUE_VALIDATE/.test(t));
        check('只有今天什么都不做的那一档带「写路径暂不做事」，三道闸门都不带',
          noopRows.length === 1 && noopRows[0].includes('DICT_RESOLVE')
          && gateRows.length === 3 && !gateRows.some((t) => t.includes('写路径暂不做事')),
          JSON.stringify(stageTags));
        const trigTag = (await page.locator('[data-testid^="pipeline-trigger-"]').first().innerText())
          .replace(/\s+/g, ' ').trim();
        check('真有挂接点的那一行给中文挂接点名', trigTag === '创建前', trigTag);

        // 草稿里的选择项：这是"能选出什么"的唯一出口，也是幽灵阶段唯一还可能回来的地方
        await page.getByRole('button', { name: /新\s*建\s*流\s*水\s*线/ }).click();
        await page.locator('.ant-modal .ant-select-selector').nth(2).waitFor({ state: 'visible', timeout: 15000 });

        // 读数前必须确认**只有一个**下拉框开着：上一支的隐藏动画没跑完时这里会数到 2，
        // 于是一次读数会把两个选择项混成一份（第二轮实测：阶段清单凭空多出两条 <没有阶段编码>，
        // 而那两条其实是实体下拉框的选项）。去竞态而不是拉长 timeout —— 两半要落在同一次读取上。
        const openBoxes = () => page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden)');
        const settleTo = async (want) => {
          for (let i = 0; i < 80; i += 1) {
            if (await openBoxes().count() === want) break;
            await page.waitForTimeout(100);
          }
          return openBoxes().count();
        };
        const readDropdown = async (label) => {
          const got = await settleTo(1);
          check(`${label}：读的时候只有一个下拉框开着（两个同时开着的读数会把两份选择项混成一份）`,
            got === 1, `开着的下拉框数=${got}`);
          return (await openBoxes().first().locator('.ant-select-item-option').allInnerTexts())
            .map((t) => t.trim());
        };
        const closeDropdown = async () => {
          // 不能用 Escape 收下拉框: 焦点不在 Select 里时那一下会落到 antd Modal 自己身上，
          // 把**整个弹窗**关掉（实测: `.anticon-delete` 等 30s 等不到，因为弹窗已经没了）。
          // 点弹窗标题是一次场内点击 —— antd 的 Select 认 mousedown 的"点外面"，关掉下拉框而不碰弹窗。
          if (await openBoxes().count() > 0) await page.locator('.ant-modal-title').click();
          await settleTo(0);
        };

        await page.locator('.ant-modal .ant-select-selector').nth(1).click();
        await openBoxes().first().locator('.ant-select-item-option').first()
          .waitFor({ state: 'visible', timeout: 10000 });
        const triggerOptions = await readDropdown('触发事件选择项');
        check('触发事件只能选到引擎真有挂接点的那两个（写后事件一个都不许出现在选择项里）',
          triggerOptions.join('|') === '创建前|更新前', JSON.stringify(triggerOptions));

        await closeDropdown();
        await page.locator('.ant-modal .ant-select-selector').nth(2).click();
        await openBoxes().first().locator('.ant-select-item-option').first()
          .waitFor({ state: 'visible', timeout: 10000 });
        const stageOptions = await readDropdown('阶段选择项');
        const codes = stageOptions.map((t) => (t.match(/[A-Z_]+/) ?? ['<没有阶段编码>'])[0]);
        check('阶段选择项与后端那份词表同源：正好这 5 个，多一个少一个都算漂',
          codes.join('|') === 'DICT_RESOLVE|REF_CHECK|REQUIRED_CHECK|TYPE_CONVERT|VALUE_VALIDATE',
          JSON.stringify(codes));
        // 反向断言要有猎物：上面那一次读取真的数出了 5 个选项，这里说"没有幽灵"才不是空跑
        check('没有执行器的幽灵阶段不能再被选中（同一格读数里刚数满 5 项）',
          codes.length === 5 && !stageOptions.join('|').includes('WEBHOOK')
          && !stageOptions.join('|').includes('SCRIPT'), JSON.stringify(codes));
        check('摘不得的三道闸门与"什么都不做"在选择项里就说出来（用户是在这里决定摘不摘的）',
          stageOptions.join('|').includes('（必填）') && stageOptions.join('|').includes('（写路径暂不做事）'),
          JSON.stringify(stageOptions).slice(0, 260));

        await closeDropdown();
        // 草稿默认不带实体，而 save() 第一步就在前端 `message.warning('请选择实体')` 后 return ——
        // 不选实体就永远发不出那个请求，下面那两条"后端真的拒了空链"的检查会退化成"界面自己拦了一下"，
        // 而这正是这一节要否证的那类谎。所以先选实体，并把它当成一条前提检查。
        await page.locator('.ant-modal .ant-select-selector').nth(0).click();
        await openBoxes().first().locator('.ant-select-item-option').first()
          .waitFor({ state: 'visible', timeout: 10000 });
        const entityOpts = await readDropdown('实体选择项');
        const wanted = entityOpts.findIndex((t) => t.includes('任务'));
        check('草稿里选得到被测实体（选不到则下面那两条"后端拒了"根本打不到服务端）',
          wanted >= 0, JSON.stringify(entityOpts).slice(0, 160));
        if (wanted >= 0) {
          await openBoxes().first().locator('.ant-select-item-option').nth(wanted).click();
        }
        await closeDropdown();

        // 收下拉框那几下点击有可能把弹窗一起带走（焦点位置一变就换形状），而弹窗一没，
        // 下面三条关于"保存被后端拒"的检查会全部空跑成"读不到东西"。先把前提钉住。
        const modalOpen = await page.locator('.ant-modal-title').isVisible();
        check('草稿弹窗还开着（一关掉，下面那三条关于"保存被拒"的检查就全是空跑）', modalOpen,
          `标题可见=${modalOpen}`);
        const deleteBtns = await page.locator('.ant-modal .anticon-delete').count();
        check('草稿默认就带着三道闸门可删（删不到按钮 = 默认链本身空了，"删空"那一路走不到）',
          deleteBtns === 3, `删除按钮数=${deleteBtns}`);

        for (let i = 0; i < 3; i += 1) {
          await page.locator('.ant-modal .anticon-delete').first().click();
        }
        const modalText = (await page.locator('.ant-modal').innerText()).replace(/\s/g, '');
        check('阶段删空时界面当场说清这一份保存会被后端拒绝',
          modalText.includes('还没有阶段——空链保存会被后端拒绝'), modalText.slice(0, 200));

        const respPromise = page.waitForResponse(
          (res) => res.url().includes('/pipeline-config/create') && res.request().method() === 'POST',
          { timeout: 15000 },
        );
        // toast 三秒就自己收掉，而"没等到响应"那一条路要把 15s 等完 —— 那时再去找
        // `.ant-message-notice` 必然找不到，一次 waitFor 超时会把整节带走（S5 实测：
        // N_TOAST 连跑都没跑到，红的是那句兜底的 catch）。改成**边出现边抄**：
        // 这一条想说的是"界面说过什么"，不是"此刻还挂着什么"。
        await page.evaluate(() => {
          const w = window;
          w.__toasts = [];
          const scan = () => {
            document.querySelectorAll('.ant-message-notice').forEach((node) => {
              const t = (node.textContent || '').replace(/\s+/g, ' ').trim();
              if (t && w.__toasts.indexOf(t) < 0) w.__toasts.push(t);
            });
          };
          new MutationObserver(scan).observe(document.body, { childList: true, subtree: true });
          setInterval(scan, 120);
          scan();
        });
        await page.locator('.ant-modal-footer').getByRole('button', { name: /保\s*存/ }).click();
        let pipeResp = null;
        try {
          pipeResp = await respPromise;
        } catch { /* 没等到响应: 下面那条检查会红, 不让它把整节带走 */ }
        const pipeStatus = pipeResp ? pipeResp.status() : null;
        if (pipeResp) sanctionedRejections.push(`${pipeStatus} ${pipeResp.url()}`);
        check('点保存真的发出了请求（这一节测的是服务端那道闸，不是界面在前端偷偷拦）',
          pipeStatus === 400, `http=${pipeStatus}`);
        // 上一条的账只认"我等到的那一次"，这一条补齐"整轮就这一次"：被登记放行的拒绝
        // 若能被无限次套用，哨兵就成了摆设。
        check('整轮 pipeline-config/create 恰好一个 POST（放行的登记不许多于实际发生的那一次）',
          pipelineCreateReqs.length === 1, `reqs=${pipelineCreateReqs.length}`);
        let toastAll = '';
        for (let i = 0; i < 100; i += 1) {
          toastAll = (await page.evaluate(() => (window.__toasts || []).join('|'))).replace(/\s/g, '');
          if (toastAll) break;
          await page.waitForTimeout(120);
        }
        check('空链真的被后端拒了，而界面没有把它报成「已保存」（UI 不许替后端说好话）',
          !toastAll.includes('已保存') && /非空JSON数组|阶段链/.test(toastAll), toastAll.slice(0, 240));
        const rowCount = await page.locator('.ant-table-row[data-row-key]').count();
        check('那次被拒的保存一行都没进表（表里还是原来那一条）', rowCount === 1, `rows=${rowCount}`);
        await shot(page, `r${runNum}-08c-pipelines`);
        await page.locator('.ant-modal-footer').getByRole('button', { name: /取\s*消/ }).click().catch(() => {});
      }
    } catch (e) {
      check('处理流水线页的顺序与拒绝', false, e?.message);
      await shot(page, `r${runNum}-08c-pipelines-FAIL`);
    }

    /* ---- 11b. 设计器侧栏：断言的是"读到的是哪些实体"，不是页面里出现过某个词 ---- */
    // 原来这里只有一遍 `body.innerText.includes('字段')`：侧栏就算写着「还没有实体」也能过，
    // 因为页面别处（字段表格的表头、右侧编辑区的输入框标签）到处都在说"字段"。
    try {
      const sidebar = page.locator('.ant-card[style*="248px"]').first();
      // 之前几步访问过 /db-import，它自己也会拉实体清单 —— 必须按"这次导航新增了几条"算，
      // 不能拿整场的累计值当"挂载只查一次"的证据。
      const beforeLoad = entityListReqs.length;
      await page.goto(`${BASE}/designer/${appCode}`, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await sidebar.locator('.ant-list-item').first().waitFor({ state: 'visible', timeout: 15000 });
      const items = await sidebar.locator('.ant-list-item').allInnerTexts();
      check('设计器侧栏列出的是真的那一个实体', items.length === 1 && items[0].includes('任务'),
        JSON.stringify(items));
      const side = (await sidebar.innerText()).replace(/\s/g, '');
      check('读到实体时侧栏不说任何"没读到/还没有"', !side.includes('没有读到') && !side.includes('还没有实体'),
        side.slice(0, 120));
      await page.waitForTimeout(600);
      check('设计器挂载只查一次实体清单', entityListReqs.length - beforeLoad === 1,
        `reqs=${entityListReqs.length - beforeLoad}`);
      const churnAt = entityListReqs.length;
      await page.waitForTimeout(2000);
      check('设计器安静下来后不再自己重查（2s 内不该再有 entity/list）',
        entityListReqs.length - churnAt === 0, `churn=${entityListReqs.length - churnAt}`);
      await shot(page, `r${runNum}-08b-designer`);
    } catch (e) {
      check('设计器侧栏', false, e?.message);
      await shot(page, `r${runNum}-08b-designer-FAIL`);
    }

    /* ---- 11c. 字段编码那道闸：界面真的按得住保存，而不只是显示了红字 ---- */
    // #34 的界面半边此前在浏览器里零覆盖：`id` 是**合法标识符**，旧的那道正则放它过，
    // 所以要钉住的不是"有没有报错"，而是"报的是哪一条 + 保存按钮有没有真的 disable + 一次写请求都没发"。
    try {
      const sidebar = page.locator('.ant-card[style*="248px"]').first();
      await sidebar.locator('.ant-list-item').first().click();
      const codeInput = page.locator('.ant-table-tbody .ant-table-row').first().locator('input').first();
      await codeInput.waitFor({ state: 'visible', timeout: 15000 });
      const original = await codeInput.inputValue();
      const saveBtn = page.getByRole('button', { name: /保\s*存/ }).first();
      const writesBefore = entityWriteReqs.length;

      await codeInput.fill('id');
      await page.waitForTimeout(400);
      const issueText = await page.locator('.ant-alert').filter({ hasText: '无法保存' }).innerText()
        .catch(() => '');
      check('撞自建列时说的是「撞了引擎自建列」而不是「不合法」（id 本身是合法标识符）',
        issueText.includes('撞了引擎自建列') && !issueText.includes('不合法'), issueText.slice(0, 180));
      check('文案里点名是哪一列，且带出那五个自建列',
        issueText.includes('id') && issueText.includes('tenant_code'), issueText.slice(0, 180));
      check('那一栏自己标红（不是只有页面底部一句泛泛的警告）',
        (await codeInput.getAttribute('class') || '').includes('ant-input-status-error'),
        await codeInput.getAttribute('class'));
      check('保存按钮被按住了', await saveBtn.isDisabled(), 'enabled');
      check('拦住 = 一次写请求都没发出去',
        entityWriteReqs.length - writesBefore === 0, `reqs=${entityWriteReqs.length - writesBefore}`);

      await codeInput.fill(original);
      await page.waitForTimeout(400);
      check('改回干净编码后闸立刻松开：警告消失、保存可点（闸不是粘住的）',
        !(await saveBtn.isDisabled())
        && !(await page.locator('.ant-alert').filter({ hasText: '无法保存' }).count()),
        `disabled=${await saveBtn.isDisabled()}`);
      check('松开之后仍然没有偷发写请求（闸的松开不等于替用户保存）',
        entityWriteReqs.length - writesBefore === 0, `reqs=${entityWriteReqs.length - writesBefore}`);
      await shot(page, `r${runNum}-08c-field-code`);
    } catch (e) {
      check('字段编码闸', false, e?.message);
      await shot(page, `r${runNum}-08c-field-code-FAIL`);
    }

    /* ---- 12. 批量导入：界面说的行数必须等于库里多出来的行数 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/LIST`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.getByRole('button', { name: '导入 CSV' }).first().waitFor({ timeout: 15000 });

      const writeCsv = (name, content) => {
        const file = path.join(SHOTS, name);
        fs.writeFileSync(file, content);
        return file;
      };

      /** 走完向导前三步：点「导入 CSV」→ 真点「选择 CSV 文件」接住文件选择框 → 点「导入 N 行」。 */
      async function runImportWizard(file) {
        await page.getByRole('button', { name: '导入 CSV' }).first().click();
        const [chooser] = await Promise.all([
          page.waitForEvent('filechooser', { timeout: 8000 }),
          page.getByRole('button', { name: '选择 CSV 文件' }).click(),
        ]);
        await chooser.setFiles(file);
        await page.getByText('实体字段').first().waitFor({ timeout: 8000 });
        await page.getByRole('button', { name: /导入 \d+ 行/ }).click();
        await page.locator('[data-testid="zlc-import-summary"]').waitFor({ timeout: 20000 });
        const summary = await page.locator('[data-testid="zlc-import-summary"]').innerText();
        const body = await page.locator('.ant-modal-body').innerText();
        // 逐行结果表按单元格取，不按整页文字找词：表头会先把「数据行 结果 详情」连成一片，
        // 在整页文字里搜 "数据行 2" 永远搜不到，而搜 "2" 又会撞上别的数字。
        // 必须在点「完成」之前读 —— 弹窗关掉后元素是隐藏的，innerText 会变成空串。
        const rows = await page.$$eval('.ant-modal-body .ant-table-row', (nodes) => nodes.map((row) =>
          Array.from(row.querySelectorAll('td')).map((td) => td.innerText.replace(/\s+/g, ' ').trim())));
        // 两个汉字的按钮 antd 会在中间插一个空格（渲染成「完 成」），所以 name 必须用容忍空格的
        // 正则；写字面量 '完成' 会永远等不到元素，30 秒后抛超时。
        await page.getByRole('button', { name: /完\s*成/ }).click();
        await page.waitForTimeout(400);
        return {
          summary: summary.replace(/\s+/g, ' '),
          body: body.replace(/\s+/g, ' '),
          rows,
        };
      }

      const before = await listRows();
      const beforeIds = new Set(before.map((r) => Number(r.id)));

      // 坏批：第 2 条记录缺必填的 title；第 3 条的 prio 不在字典里（只该警告，不该算错误）
      importReqs.length = 0;
      const bad = await runImportWizard(writeCsv('import-bad.csv', 'title,prio,hours\n导入甲,P0,3\n,P1,1\n导入丙,P9,2\n'));
      const afterBad = await listRows();
      check('预检没过：库里一行都没多出来',
        afterBad.length === before.length && afterBad.every((r) => beforeIds.has(Number(r.id))),
        `before=${before.length} after=${afterBad.length}`);
      check('预检没过：根本不发 commit 请求',
        importReqs.join(',') === 'preview', importReqs.join(','));
      check('坏批计数用服务端的 total-validCount，不是列出的 errors 条数',
        /3 行里有 1 行不合法，整批一行都没写入/.test(bad.summary) && !/成功/.test(bad.body),
        bad.summary);
      check('不合法的行被点名：第 2 条记录、标失败、说的是必填',
        bad.rows.length === 1 && bad.rows[0][0] === '2' && bad.rows[0][1] === '失败'
        && /必填/.test(bad.rows[0][2]) && /标题/.test(bad.rows[0][2]),
        JSON.stringify(bad.rows));
      check('字典外的值只警告，不冒充错误',
        /P9/.test(bad.body) && /不在字典/.test(bad.body) && !/2 行不合法/.test(bad.summary),
        bad.body.slice(0, 200));
      await shot(page, `r${runNum}-12-import-bad`);

      // 好批：界面说 2 行，库里就必须正好多 2 行，而且值带得对
      importReqs.length = 0;
      const ok = await runImportWizard(writeCsv('import-ok.csv', 'title,prio,hours\n导入甲,P0,3\n导入乙,P1,1.5\n'));
      const afterOk = await listRows();
      const added = afterOk.filter((r) => !beforeIds.has(Number(r.id)));
      check('导入全绿：走的是 preview + commit 两段',
        importReqs.join(',') === 'preview,commit' && /已导入 2 行/.test(ok.summary),
        `${importReqs.join(',')} | ${ok.summary}`);
      check('界面说导入几行，库里就正好多几行',
        added.length === 2 && afterOk.length === before.length + 2,
        `added=${added.length} before=${before.length} after=${afterOk.length}`);
      check('导入进来的行带的是 CSV 里的值，不是空壳',
        added.map((r) => r.title).sort().join('|') === '导入乙|导入甲'
        && added.map((r) => Number(r.hours)).sort((a, b) => a - b).join('|') === '1.5|3',
        added.map((r) => `${r.title}/${r.hours}/${r.prio}`).join(' | '));
      check('合法批不再出现字典警告（P9 那行没混进这批）',
        !/不在字典/.test(ok.body), ok.body.slice(0, 160));
      check('好批逐行都标了成功，行号是记录序号',
        ok.rows.length === 2 && ok.rows.every((row, i) => row[0] === String(i + 1) && row[1] === '成功'),
        JSON.stringify(ok.rows));
      await shot(page, `r${runNum}-12-import-ok`);

      // 收尾：删掉本步导入的行，让每轮的数据规模保持用例假设的 3 条
      for (const row of added) {
        await fetch(`${API}/api/lc/runtime/delete?entityCode=task`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'default' },
          body: JSON.stringify({ id: Number(row.id), appCode, tenantCode: 'default' }),
        }).then((r) => r.json());
      }
      const cleaned = await listRows();
      check('用例自己导入的行已清干净，不污染后面的步骤',
        cleaned.length === before.length, `left=${cleaned.length} want=${before.length}`);
    } catch (e) {
      check('批量导入', false, e?.message);
      await shot(page, `r${runNum}-12-import-FAIL`);
    }

    /* ---- 13. 批量删除：一个请求、整批原子、界面报的条数 = 库里少的条数 ---- */
    try {
      await page.goto(`${BASE}/${appCode}/task/LIST`, { waitUntil: 'networkidle', timeout: 20000 });
      await page.getByText('写迁移脚本').first().waitFor({ timeout: 15000 });

      /*
       * 这一节验的是"只有真浏览器才验得了"的那半条链路：
       * jsdom 里 userEvent 点 Popconfirm 的触发按钮会挂住不返回，所以 vitest 那层是
       * fireEvent 快进；真实的"点两下 → 弹确认 → 发一个请求 → 表格和库里同步"只能在这儿验。
       */

      /** 混进一个不存在的 id：界面上没有这种操作，但"提交之后别人先删了"有，所以在网络层造。 */
      let ghostId = null;
      const batchPayloads = [];
      await page.route('**/runtime/delete-batch*', async (route) => {
        const req = route.request();
        let payload;
        try {
          payload = req.postDataJSON() ?? {};
        } catch {
          payload = {};
        }
        if (ghostId !== null) {
          payload = { ...payload, ids: [...(payload.ids ?? []), ghostId] };
        }
        batchPayloads.push(payload);
        await route.continue({ postData: JSON.stringify(payload) });
      });

      /** 勾选前 n 行。点 label 而不是 input：antd 的 checkbox input 是 0 尺寸、透明盖着的。 */
      async function selectRows(n) {
        const rows = page.locator('tr[data-row-key]');
        await rows.first().waitFor({ timeout: 15000 });
        for (let i = 0; i < n; i++) {
          await rows.nth(i).locator('label.ant-checkbox-wrapper').click({ timeout: 8000 });
        }
        return page.$$eval('tr[data-row-key]', (nodes, count) =>
          nodes.slice(0, count).map((node) => Number(node.getAttribute('data-row-key'))), n);
      }

      /** 上一条吐司要等它自己散掉，否则读到的是上一次的文案，看着像断言稳如老狗其实是串了。 */
      async function waitToastGone() {
        await page.locator('.ant-message-notice-content').first()
          .waitFor({ state: 'detached', timeout: 6000 }).catch(() => {});
      }

      /** 点「删除 n 条」→ Popconfirm 确认 → 收 Popconfirm 的问句和吐司。 */
      async function confirmBulkDelete(n) {
        await page.getByRole('button', { name: new RegExp(`删除\\s*${n}\\s*条`) }).click({ timeout: 8000 });
        // 关掉之后 rc-trigger 会把容器标成 ant-popover-hidden 并留在 DOM 里；
        // 不收窄的话 .last() 可能点到已经藏起来的旧弹层。
        const pop = page.locator('.ant-popover:not(.ant-popover-hidden)').last();
        await pop.waitFor({ timeout: 8000 });
        const asked = (await pop.innerText()).replace(/\s+/g, ' ');
        await pop.getByRole('button', { name: /^(OK|确\s*定)$/ }).click({ timeout: 8000 });
        await page.locator('.ant-message-notice-content').last().waitFor({ timeout: 20000 });
        const toast = (await page.locator('.ant-message-notice-content').last().innerText()).replace(/\s+/g, ' ');
        return { asked, toast };
      }

      const before = await listRows();
      const beforeIds = before.map((r) => Number(r.id)).sort((a, b) => a - b);
      ghostId = Math.max(...beforeIds) + 987654;

      // 坏批：勾 2 行 + 混 1 条不存在的 id
      await waitToastGone();
      deleteReqs.length = 0;
      batchPayloads.length = 0;
      const picked = await selectRows(2);
      const bad = await confirmBulkDelete(2);
      const afterBad = await listRows();
      const afterBadIds = afterBad.map((r) => Number(r.id)).sort((a, b) => a - b);
      check('批量删除真的先弹 Popconfirm 问一句',
        new RegExp(`确认删除选中的\\s*2\\s*条`).test(bad.asked), bad.asked);
      check('一个请求带走所有 id：一次 delete-batch，零次逐行单删',
        deleteReqs.join(',') === 'batch' && batchPayloads.length === 1
        && JSON.stringify(batchPayloads[0]?.ids) === JSON.stringify([...picked, ghostId]),
        `${deleteReqs.join(',')} | ${JSON.stringify(batchPayloads[0]?.ids)}`);
      check('请求带的是 entityCode/appCode/tenantCode，不是前端猜的作用域',
        batchPayloads[0]?.appCode === appCode && batchPayloads[0]?.tenantCode === 'default'
        && String(batchPayloads[0]?.entityCode) === 'task',
        JSON.stringify(batchPayloads[0]));
      check('混进一条不存在的 id：界面说"整批未删除"，不许出现"已删除"',
        /整批未删除/.test(bad.toast) && !/已删除/.test(bad.toast), bad.toast);
      check('坏批还点名是哪一条：那条不存在的 id',
        bad.toast.includes(`记录 ${ghostId}`), bad.toast);
      check('坏批：库里一条都没少（不是删了一半）',
        JSON.stringify(afterBadIds) === JSON.stringify(beforeIds),
        `before=${beforeIds.join(',')} after=${afterBadIds.join(',')}`);
      const stillSelected = await page.getByRole('button', { name: /删除\s*2\s*条/ }).count();
      check('坏批后勾选保持原样，用户修好问题能直接重来', stillSelected === 1, `按钮数=${stillSelected}`);
      await shot(page, `r${runNum}-13-bulk-delete-bad`);

      // 好批：沿用还在的勾选，把 ghost 撤掉再点一次
      await waitToastGone();
      ghostId = null;
      deleteReqs.length = 0;
      batchPayloads.length = 0;
      const good = await confirmBulkDelete(2);
      const afterGood = await listRows();
      const afterGoodIds = afterGood.map((r) => Number(r.id)).sort((a, b) => a - b);
      const gone = beforeIds.filter((id) => !afterGoodIds.includes(id));
      check('好批：一个请求删完，报的条数来自服务端',
        deleteReqs.join(',') === 'batch' && /已删除 2 条/.test(good.toast),
        `${deleteReqs.join(',')} | ${good.toast}`);
      check('界面说"已删除 2 条"，库里就正好少这 2 条',
        gone.length === 2
        && JSON.stringify(gone.sort((a, b) => a - b)) === JSON.stringify(picked.sort((a, b) => a - b)),
        `gone=${gone.join(',')} picked=${picked.join(',')}`);
      await page.waitForTimeout(600);
      const visibleRows = await page.locator('tr[data-row-key]').count();
      const visibleIds = await page.$$eval('tr[data-row-key]', (nodes) =>
        nodes.map((node) => node.getAttribute('data-row-key')));
      check('删完的表格不留幽灵行：行数与 id 集合都跟库里一致',
        visibleRows === afterGood.length && visibleIds.every((id) => afterGoodIds.includes(Number(id))),
        `table=${visibleRows} db=${afterGood.length} | ${visibleIds.join(',')} vs ${afterGoodIds.join(',')}`);
      const badgeLeft = await page.getByRole('button', { name: /删除\s*\d+\s*条/ }).count();
      check('删成功后勾选清空，批量删除按钮收起', badgeLeft === 0, `还在的按钮数=${badgeLeft}`);
      await shot(page, `r${runNum}-13-bulk-delete-ok`);
    } catch (e) {
      check('批量删除全链路', false, e?.message);
      await shot(page, `r${runNum}-13-bulk-delete-FAIL`);
    }

    /* ---- 9. 全局异常 ---- */
    try {
      check('全程没有未捕获的 JS 异常', pageErrors.length === 0, pageErrors.slice(0, 3).join(' ;; '));
      const api404 = (() => {
        // 按多重集扣减，不是按 URL 放行: 某一节**登记**了一次被拒的请求（它要验的就是那道闸），
        // 那一节的第二次撞上来仍然要红。
        const rest = badResponses.filter((u) => !/favicon|\.ico|apple-touch/.test(u));
        for (const sanctioned of sanctionedRejections) {
          const at = rest.indexOf(sanctioned);
          if (at >= 0) rest.splice(at, 1);
        }
        return rest;
      })();
      runNotes.push(`  INFO  非 2xx 响应 ${badResponses.length} 个：${badResponses.slice(0, 6).join(' | ') || '无'}`);
      check('没有意外的 404/5xx 接口', api404.length === 0, api404.slice(0, 4).join(' | '));
    } catch (e) {
      check('全局异常检查', false, e?.message);
    }
  } catch (e) {
    if (e instanceof skipRemaining) {
      runNotes.push(`  SKIP  ${e.message}`);
    } else {
      runFailures.push(`脚本自身抛错: ${e?.message}`);
      runNotes.push(`  ERROR ${e?.stack?.split('\n').slice(0, 3).join('\n') ?? e}`);
      await shot(page, `r${runNum}-99-crash`).catch(() => {});
    }
  } finally {
    tl('run-end');
    await browser.close();
  }

  return { runFailures: [...runFailures], runNotes: [...runNotes], timeline: [...timeline] };
}

class skipRemaining extends Error {
  constructor(msg) { super(msg); }
}

/* ------------------------------------------------------------------ */
/*  主入口：REPEATS 轮                                                  */
/* ------------------------------------------------------------------ */
function newestMtimeMs(dir) {
  let newest = 0;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    newest = Math.max(newest, entry.isDirectory() ? newestMtimeMs(full) : fs.statSync(full).mtimeMs);
  }
  return newest;
}

/**
 * 门禁打的是哪一份产物 —— 不查这一条，"全绿"完全可能测的是上一轮的 bundle：
 * 改了 src 忘了 build，preview 照样伺服旧件、页面照样全绿，而且没有任何一条检查会告诉你。
 * 两道都要：名字对得上（浏览器加载的就是磁盘这份）+ 产物不比源码旧（磁盘这份是当前源码构建的）。
 */
async function assertServingFreshBuild() {
  const root = path.join(HERE, '..');
  const distDir = path.join(root, 'dist');
  const problems = [];
  const servedHtml = await fetch(`${BASE}/`).then((r) => r.text()).catch(() => '');
  const servedBundle = (servedHtml.match(/assets\/(index-[^"']+\.js)/) ?? [])[1] ?? '';
  const indexPath = path.join(distDir, 'index.html');
  const diskBundle = fs.existsSync(indexPath)
    ? ((fs.readFileSync(indexPath, 'utf8').match(/assets\/(index-[^"']+\.js)/) ?? [])[1] ?? '')
    : '';
  if (!diskBundle) problems.push(`${indexPath} 里没有 index-*.js（先 npm run build）`);
  if (!servedBundle) problems.push(`没从 ${BASE}/ 的 HTML 里读到 assets/index-*.js（preview 没起？）`);
  if (servedBundle && diskBundle && servedBundle !== diskBundle) {
    problems.push(`浏览器要加载的是 ${servedBundle}，磁盘上构建出来的是 ${diskBundle}（preview 指向的不是这份 dist）`);
  }
  let ageSeconds = 0;
  if (servedBundle === diskBundle && diskBundle) {
    const bundlePath = path.join(distDir, 'assets', diskBundle);
    const newestSource = newestMtimeMs(path.join(root, 'src'));
    ageSeconds = (fs.statSync(bundlePath).mtimeMs - newestSource) / 1000;
    if (ageSeconds < 0) {
      problems.push(`src 里最新的文件比 ${diskBundle} 新 ${(-ageSeconds).toFixed(0)}s —— 这份 bundle 不是当前源码构建出来的，重新 build`);
    }
  }
  if (problems.length) {
    console.error(`门禁拒绝开跑（测的必须是本轮构建的产物）：\n  - ${problems.join('\n  - ')}`);
    process.exit(2);
  }
  console.log(`  产物指纹：${diskBundle}，比 src 里最新的文件新 ${ageSeconds.toFixed(0)}s`);
}

async function main() {
  fs.mkdirSync(SHOTS, { recursive: true });
  if (!fs.existsSync(CHROME)) {
    console.error(`找不到 Chrome：${CHROME}`);
    process.exit(2);
  }

  console.log(`\n=== 真浏览器 E2E · ${BASE} · REPEATS=${REPEATS} ===\n`);
  await assertServingFreshBuild();

  const allTimelines = [];
  const results = [];
  for (let i = 1; i <= REPEATS; i++) {
    // 每轮新建一个应用，避免数据积累
    const appCode = await seedTestData();
    console.log(`--- 第 ${i}/${REPEATS} 轮 (app=${appCode}) ---`);
    const r = await runOnce(i, appCode);
    results.push(r);
    allTimelines.push(...r.timeline.map((t) => ({ ...t, run: i })));
    console.log(r.runNotes.join('\n'));
    const passCount = r.runNotes.filter((n) => n.startsWith('  PASS')).length;
    const failCount = r.runFailures.length;
    console.log(`  => PASS ${passCount} / FAIL ${failCount}\n`);
  }

  // 汇总
  const totalRuns = results.length;
  const fullyGreen = results.filter((r) => r.runFailures.length === 0).length;
  const allFailures = [...new Set(results.flatMap((r) => r.runFailures))];

  console.log(`\n=== 汇总 ${totalRuns} 轮 ===`);
  console.log(`  全绿轮次: ${fullyGreen}/${totalRuns}`);
  if (allFailures.length) {
    console.log(`  累计失败项: ${allFailures.length}`);
    allFailures.forEach((f) => console.log(`    - ${f}`));
  }

  // 写 timeline（只有失败时才写，节省空间）
  if (allFailures.length || REPEATS > 1) {
    const tlPath = path.join(SHOTS, 'timeline.json');
    fs.writeFileSync(tlPath, JSON.stringify(allTimelines, null, 2));
    console.log(`\n  时间线已写入: ${tlPath}`);
  }

  if (fullyGreen < totalRuns) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
