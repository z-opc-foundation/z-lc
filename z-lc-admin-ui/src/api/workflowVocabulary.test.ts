import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import { readVocabulary } from './workflowBinding';
import { triggerLabel } from '@/views/admin/_workflow';

/**
 * 缺陷 #61 的第四层：界面上那份"有哪些触发时机"的清单，来源必须是引擎自己说的那一份。
 *
 * 为什么还要在 vitest 里读一遍 java 源码：`/vocabulary` 与服务类之间的同源关系由
 * `WorkflowTriggerContractTest`（java 契约层）钉住，而**前端有没有真的去用它**没有任何 java
 * 尺能看见。这一族此前的形状就是：后端拒得死死的，界面照样摆着 AFTER_UPDATE / AFTER_DELETE，
 * 还默认送 autoSubmit=0 —— 于是用户每点一次"新建绑定"都白挨一次 400。
 * 所以这里两头都钉：引擎的清单逐字对表（含拒绝原因不许是空话），页面源码里不许再有手抄的清单。
 */

const here = dirname(fileURLToPath(import.meta.url));
const TRIGGERS_JAVA = resolve(
  here,
  '../../../z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowTriggers.java',
);
const PAGE_TSX = resolve(here, '../views/admin/WorkflowsPage.tsx');
const HOOK_TS = resolve(here, '../views/admin/_workflow.ts');
const API_TS = resolve(here, './workflowBinding.ts');

function read(path: string, what: string): string {
  let text: string;
  try {
    text = readFileSync(path, 'utf8');
  } catch (err) {
    throw new Error(`读不到${what} (${path}) —— 参照集为空时下面的断言会全部空转: ${err}`);
  }
  expect(text.length, `${what} 是空文件`).toBeGreaterThan(0);
  return text;
}

/** 引擎真兑现的那几个事件（`IMPLEMENTED` 那条声明，singletonList / asList 两种写法都认）。 */
function javaImplemented(java: string): string[] {
  const codes = new Map<string, string>();
  for (const m of java.matchAll(/public static final String (\w+) = "([^"]+)"/g)) {
    if (m[1] && m[2]) codes.set(m[1], m[2]);
  }
  expect(codes.size, '没解析出任何 String 常量, 说明正则或被读的文件不对').toBeGreaterThan(0);

  const decl = java.match(/List<String> IMPLEMENTED\s*=\s*([^;]+);/);
  expect(decl, 'WorkflowTriggers.java 里找不到 IMPLEMENTED 那条声明').toBeTruthy();
  const body = decl?.[1] ?? '';
  const names = [...body.matchAll(/\b([A-Z][A-Z0-9_]*)\b/g)].map((m) => m[1] ?? '');
  const values = names
    .filter((name) => codes.has(name))
    .map((name) => codes.get(name) as string);
  expect(values.length, `IMPLEMENTED 解析出 0 项（原文=${body.trim()}）—— 空参照集不能算通过`).toBeGreaterThan(0);
  return values;
}

/** 兑现不了的那几个，以及各自的原因（写入口 400 消息的取材处）。 */
function javaRejected(java: string): { event: string; reason: string }[] {
  const start = java.search(/private static Map<String, String> unimplemented\(\)/);
  expect(start, '找不到 unimplemented() —— 拒绝原因的出处搬走了').toBeGreaterThanOrEqual(0);
  const body = java.slice(start);
  const end = body.indexOf('return Collections.unmodifiableMap');
  expect(end, 'unimplemented() 的方法体不完整').toBeGreaterThan(0);
  const puts = [...body.slice(0, end).matchAll(/m\.put\(\s*"([^"]+)"\s*,\s*([\s\S]*?)\);/g)];
  expect(puts.length, '一个 m.put 都没解析出来, 拒绝面清单在别处').toBeGreaterThan(0);
  return puts.map((m) => {
    const event = m[1] ?? '';
    // java 里长原因是几段字符串相加 ⇒ 把每段的字面量拼起来看整句
    const reason = [...(m[2] ?? '').matchAll(/"([^"]*)"/g)].map((p) => p[1] ?? '').join('').trim();
    return { event, reason };
  });
}

/** 中文名那张表的键（它只能给引擎兑现得了的事件起名，多一个键就是又抄了一份清单）。 */
function labelKeys(hook: string): string[] {
  const block = hook.match(/const TRIGGER_LABELS: Record<string, string> = \{([\s\S]*?)\};/);
  expect(block, '解析不到 TRIGGER_LABELS —— 那份中文显示名表搬走了').toBeTruthy();
  const keys = [...(block?.[1] ?? '').matchAll(/^\s*([A-Z_]+):/gm)].map((m) => m[1] ?? '');
  expect(keys.length, 'TRIGGER_LABELS 解析出 0 项 —— 空参照集不能算通过').toBeGreaterThan(0);
  return keys.sort();
}

describe('流程触发时机的口径与引擎同源', () => {
  it('前端能给出的时机就是引擎 IMPLEMENTED 那份，且每个都有中文名', () => {
    const implemented = javaImplemented(read(TRIGGERS_JAVA, '引擎事件口径 WorkflowTriggers.java'));
    expect(implemented, '今天只该有一个真有挂接点的事件').toEqual(['AFTER_CREATE']);
    for (const event of implemented) {
      const label = triggerLabel(event);
      expect(label, `${event} 在界面上还是裸编码, 用户读不出它是什么意思`).not.toBe(event);
      expect(label).not.toBe('—');
    }
    // 两个方向都要对：少一个键=新事件在界面上是裸编码；多一个键=那份表又变成一份手抄清单。
    expect(labelKeys(read(HOOK_TS, '词表 hook _workflow.ts'))).toEqual([...implemented].sort());
    // 词表里出现新事件时，界面不许把它悄悄显示成裸编码：要么补中文名，要么这条红把它盯住。
    expect(triggerLabel('AFTER_ARCHIVE'), '没登记的事件码应当原样显示（不猜、不编一个中文名）').toBe('AFTER_ARCHIVE');
  });

  it('拒绝面逐条带着"为什么兑现不了"，不许是一句空的「不支持」', () => {
    const rejected = javaRejected(read(TRIGGERS_JAVA, '引擎事件口径 WorkflowTriggers.java'));
    expect(rejected.length).toBeGreaterThan(0);
    for (const item of rejected) {
      expect(item.reason.length, `${item.event} 的原因短得不像一句解释: ${item.reason}`).toBeGreaterThan(12);
      expect(item.reason, `${item.event} 的原因把事件码复述了一遍就当解释`).not.toBe(item.event);
    }
    // 页面上的老脏数据要靠这份原因说话：源码里必须真的读 rejected 的 reason，而不是自己编一句。
    const page = read(PAGE_TSX, '流程绑定页');
    expect(page.includes('rejectedReasons'), '页面没有把引擎那句原因接过来').toBe(true);
    expect(page.includes('.reason'), '原因这一格没上界面').toBe(true);
  });

  it('页面的选择项来自 /vocabulary，源码里不许再有手抄的时机清单', () => {
    const page = read(PAGE_TSX, '流程绑定页');
    const hook = read(HOOK_TS, '词表 hook');
    const api = read(API_TS, '流程接口层');
    // 先钉"确实接上了那条接口"，再说"没有手抄" —— 只留后一半的话，把整个下拉删掉也能全绿。
    expect(api.includes('/workflow-binding/vocabulary'), '接口层没有去读引擎那份词表').toBe(true);
    expect(hook.includes('getWorkflowVocabulary'), '词表 hook 没有真的调接口').toBe(true);
    expect(page.includes('useWorkflowVocabulary'), '页面没有把词表接进来').toBe(true);
    expect(page.includes('triggerSelectOptions'), '下拉框的选项不是从词表长出来的').toBe(true);
    for (const ghost of ['AFTER_UPDATE', 'AFTER_DELETE', 'status_change', 'BEFORE_CREATE']) {
      expect(page.includes(`'${ghost}'`), `${ghost} 又回到了页面源码里（引擎不兑现的事件不该能选）`).toBe(false);
      expect(hook.includes(`'${ghost}'`), `${ghost} 回到了词表 hook 里`).toBe(false);
    }
    expect(page.includes('const TRIGGERS'), '手抄清单回来了: 页面自己列了一份引擎清单').toBe(false);
    expect(page.includes('TRIGGER_EVENTS'), '选择项不许从 types.ts 那份手抄联合类型取').toBe(false);
    // autoSubmit 那个开关在运行期是"关掉就永远不发"，界面上不许再给一个能填但会被写入口拒掉的开关。
    expect(page.includes('<Switch'), '自动提单开关回来了: 关掉之后这条绑定没有任何运行时行为').toBe(false);
  });

  it('词表形状不对时判读失败，不许降级成「引擎没有可兑现的事件」', () => {
    const java = read(TRIGGERS_JAVA, '引擎事件口径 WorkflowTriggers.java');
    const good = { implemented: javaImplemented(java), rejected: javaRejected(java) };
    expect(readVocabulary(good)).toEqual(good);

    // 逐条喂"接口漂了"的形状：每一条都必须抛，而不是回一份空清单。
    for (const broken of [null, undefined, 'oops', {}, { implemented: null }, { implemented: 'AFTER_CREATE' }]) {
      expect(() => readVocabulary(broken), `这个形状被判成了"读到了": ${JSON.stringify(broken)}`).toThrow();
    }
    // implemented 里混进空值 ⇒ 剔除；rejected 整格缺席 ⇒ 只当没有拒绝面，不当读失败。
    expect(readVocabulary({ implemented: [' AFTER_CREATE ', '', null, 7], rejected: 'x' }).implemented).toEqual([
      'AFTER_CREATE',
    ]);
    const noRejected = readVocabulary({ implemented: ['AFTER_CREATE'] });
    expect(noRejected.implemented).toEqual(['AFTER_CREATE']);
    expect(noRejected.rejected).toEqual([]);
    // 拒绝项少了 reason 也不能显示成空白一句：那等于对用户说"不支持"而没说为什么。
    expect(readVocabulary({ implemented: [], rejected: [{ event: 'AFTER_UPDATE' }] }).rejected).toEqual([
      { event: 'AFTER_UPDATE', reason: '引擎没有说原因' },
    ]);
  });
});
