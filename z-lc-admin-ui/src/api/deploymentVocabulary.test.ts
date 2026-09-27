import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import { readVocabulary } from './deployment';
import { deployTypeLabel } from '@/views/admin/_deployment';

/**
 * 缺陷 #70 的第四层：界面上那份"有哪几种部署方式"的清单，来源必须是服务器自己说的那一份。
 *
 * 与 #61 同法（见 `workflowVocabulary.test.ts`）：`/deployment/create` 与 `DeploymentTypes` 之间的
 * 同源关系由 `DeploymentContractTest`（java 契约层）钉住，而**前端有没有真的去用它**没有任何 java
 * 尺能看见。这一族此前的形状就是：页面自己抄了 `['HOT_LOAD','DOCKER','GIT_PUSH']` 三种，
 * 而服务器一种都不执行 —— 点任何一种都只留下一行永远 PENDING 的账，再弹一句「部署已创建」。
 * 所以这里两头都钉：服务器的清单逐字对表（含"为什么不执行"不许是空话），页面源码里不许再有手抄的清单。
 */

const here = dirname(fileURLToPath(import.meta.url));
const TYPES_JAVA = resolve(
  here,
  '../../../z-lc-core/src/main/java/com/zifang/z/lc/core/deployment/DeploymentTypes.java',
);
const PAGE_TSX = resolve(here, '../views/admin/DeploymentsPage.tsx');
const HOOK_TS = resolve(here, '../views/admin/_deployment.ts');
const API_TS = resolve(here, './deployment.ts');
const TYPES_TS = resolve(here, './types.ts');

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

/** 服务器真执行得了的那几种（`IMPLEMENTED` 那条声明，singletonList / asList 两种写法都认）。 */
function javaImplemented(java: string): string[] {
  const codes = new Map<string, string>();
  for (const m of java.matchAll(/public static final String (\w+) = "([^"]+)"/g)) {
    if (m[1] && m[2]) codes.set(m[1], m[2]);
  }
  expect(codes.size, '没解析出任何 String 常量, 说明正则或被读的文件不对').toBeGreaterThan(0);

  const decl = java.match(/List<String> IMPLEMENTED\s*=\s*([^;]+);/);
  expect(decl, 'DeploymentTypes.java 里找不到 IMPLEMENTED 那条声明').toBeTruthy();
  const body = decl?.[1] ?? '';
  const names = [...body.matchAll(/\b([A-Z][A-Z0-9_]*)\b/g)].map((m) => m[1] ?? '');
  const values = names.filter((name) => codes.has(name)).map((name) => codes.get(name) as string);
  expect(values.length, `IMPLEMENTED 解析出 0 项（原文=${body.trim()}）—— 空参照集不能算通过`).toBeGreaterThan(0);
  return values;
}

/** 执行不了的那几种，以及各自的原因（写入口 400 消息与界面那一格的取材处）。 */
function javaRejected(java: string): { type: string; reason: string }[] {
  const start = java.search(/private static Map<String, String> unimplemented\(\)/);
  expect(start, '找不到 unimplemented() —— 拒绝原因的出处搬走了').toBeGreaterThanOrEqual(0);
  const body = java.slice(start);
  const end = body.indexOf('return Collections.unmodifiableMap');
  expect(end, 'unimplemented() 的方法体不完整').toBeGreaterThan(0);
  const puts = [...body.slice(0, end).matchAll(/m\.put\(\s*"([^"]+)"\s*,\s*([\s\S]*?)\);/g)];
  expect(puts.length, '一个 m.put 都没解析出来, 不执行清单在别处').toBeGreaterThan(0);
  return puts.map((m) => {
    const type = m[1] ?? '';
    // java 里长原因是几段字符串相加 ⇒ 把每段的字面量拼起来看整句
    const reason = [...(m[2] ?? '').matchAll(/"([^"]*)"/g)].map((p) => p[1] ?? '').join('').trim();
    return { type, reason };
  });
}

/** 中文名那张表的键（它是显示名，不是清单：键必须恰好等于服务器提到过的编码）。 */
function labelKeys(hook: string): string[] {
  const block = hook.match(/const TYPE_LABELS: Record<string, string> = \{([\s\S]*?)\};/);
  expect(block, '解析不到 TYPE_LABELS —— 那份中文显示名表搬走了').toBeTruthy();
  const keys = [...(block?.[1] ?? '').matchAll(/^\s*([A-Z_]+):/gm)].map((m) => m[1] ?? '');
  expect(keys.length, 'TYPE_LABELS 解析出 0 项 —— 空参照集不能算通过').toBeGreaterThan(0);
  return keys.sort();
}

describe('部署方式的口径与服务器同源', () => {
  it('前端能给出的方式就是服务器 IMPLEMENTED 那份，且每种都有中文名', () => {
    const java = read(TYPES_JAVA, '服务器部署方式口径 DeploymentTypes.java');
    const implemented = javaImplemented(java);
    const rejected = javaRejected(java);
    expect(implemented, '今天只该有一种真有执行器的方式').toEqual(['HOT_LOAD']);

    for (const type of implemented) {
      const label = deployTypeLabel(type);
      expect(label, `${type} 在界面上还是裸编码, 用户读不出它是什么意思`).not.toBe(type);
      expect(label).not.toBe('—');
    }
    // 显示名表既不许漏（漏 = 界面上是裸编码），也不许多（多 = 它又变成一份手抄清单）。
    const known = [...new Set([...implemented, ...rejected.map((item) => item.type)])].sort();
    expect(labelKeys(read(HOOK_TS, '显示名 hook'))).toEqual(known);
    // 服务器报告了一个新编码时，界面不许猜一个名字：原样显示，要么补中文名，要么这条红盯着它。
    expect(deployTypeLabel('BLUE_GREEN'), '没登记的编码应当原样显示（不猜、不编一个中文名）').toBe('BLUE_GREEN');
  });

  it('不执行面逐条带着"为什么执行不了"，不许是一句空的「不支持」', () => {
    const rejected = javaRejected(read(TYPES_JAVA, '服务器部署方式口径 DeploymentTypes.java'));
    expect(rejected.length).toBeGreaterThan(0);
    for (const item of rejected) {
      expect(item.reason.length, `${item.type} 的原因短得不像一句解释: ${item.reason}`).toBeGreaterThan(12);
      expect(item.reason, `${item.type} 的原因把编码复述了一遍就当解释`).not.toBe(item.type);
    }
    // 页面上那几种"能不能选"是服务器说了算：源码里必须真的读 rejected 的 reason，而不是自己编一句。
    const page = read(PAGE_TSX, '部署页');
    expect(page.includes('rejectedReasons'), '页面没有把服务器那句原因接过来').toBe(true);
    expect(page.includes('.reason'), '原因这一格没上界面').toBe(true);
  });

  it('页面的选择项来自 /vocabulary，源码里不许再有手抄的方式清单', () => {
    const page = read(PAGE_TSX, '部署页');
    const hook = read(HOOK_TS, '显示名与词表 hook _deployment.ts');
    const api = read(API_TS, '部署接口层');
    const types = read(TYPES_TS, '类型定义');
    // 先钉"确实接上了那条接口"，再说"没有手抄" —— 只留后一半的话，把整个下拉删掉也能全绿。
    expect(api.includes('/deployment/vocabulary'), '接口层没有去读服务器那份词表').toBe(true);
    expect(hook.includes('getDeploymentVocabulary'), '词表 hook 没有真的调接口').toBe(true);
    expect(page.includes('useDeploymentVocabulary'), '页面没有把词表接进来').toBe(true);
    expect(page.includes('selectOptions'), '下拉框的选项不是从词表长出来的').toBe(true);
    // 结局这一句是 #70 的本体：状态必须参与提示，不许无条件报"已创建"。
    expect(page.includes("done.status === 'SUCCESS'"), '页面没有按部署的真结局分叉').toBe(true);
    expect(page.includes('部署已创建'), '老那句谎话回来了: 服务器没做成时也报创建成功').toBe(false);

    for (const ghost of ['DOCKER', 'GIT_PUSH', 'SQL']) {
      expect(page.includes(`'${ghost}'`), `${ghost} 又回到了页面源码里（服务器执行不了的方式不该能选）`).toBe(false);
      expect(types.includes(`'${ghost}'`), `${ghost} 回到了类型定义里（那份清单的唯一来源在服务器）`).toBe(false);
    }
    expect(page.includes('const DEPLOY_TYPES'), '手抄清单回来了: 页面自己列了一份方式清单').toBe(false);
    expect(types.includes('export const DEPLOY_TYPES'), 'types.ts 里又抄了一份可选项清单').toBe(false);
  });

  it('词表形状不对时判读失败，不许降级成「服务器没有可执行的方式」', () => {
    const java = read(TYPES_JAVA, '服务器部署方式口径 DeploymentTypes.java');
    const good = { executable: javaImplemented(java), rejected: javaRejected(java) };
    expect(readVocabulary(good)).toEqual(good);

    // 逐条喂"接口漂了"的形状：每一条都必须抛，而不是回一份空清单。
    for (const broken of [null, undefined, 'oops', {}, { executable: null }, { executable: 'HOT_LOAD' }]) {
      expect(() => readVocabulary(broken), `这个形状被判成了"读到了": ${JSON.stringify(broken)}`).toThrow();
    }
    // executable 里混进空值 ⇒ 剔除；rejected 整格缺席 ⇒ 只当没有不执行面，不当读失败。
    expect(readVocabulary({ executable: [' HOT_LOAD ', '', null, 7], rejected: 'x' }).executable).toEqual(['HOT_LOAD']);
    const noRejected = readVocabulary({ executable: ['HOT_LOAD'] });
    expect(noRejected.executable).toEqual(['HOT_LOAD']);
    expect(noRejected.rejected).toEqual([]);
    // 不执行项少了 reason 也不能显示成空白一句：那等于对用户说"不支持"而没说为什么。
    expect(readVocabulary({ executable: [], rejected: [{ type: 'DOCKER' }] }).rejected).toEqual([
      { type: 'DOCKER', reason: '服务器没有说原因' },
    ]);
  });
});
