import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

import {
  PIPELINE_STAGE_TYPES,
  PIPELINE_SUPPORTED_TRIGGERS,
  defaultPipelineStages,
  isPipelineTriggerSupported,
  newPipelineDraft,
  parseStages,
  stringifyStages,
} from './pipeline';

/**
 * 这一层的存在理由: 配置面(PipelinesPage)和执行面(PipelineStages.java + Pipeline)此前各写一套清单,
 * 于是页面上摆着 WEBHOOK / SCRIPT 两个"没有执行器"的阶段、AFTER_* 三个"没有挂接点"的触发事件,
 * 保存成功而什么都不跑。前端的清单不再由人抄一遍 —— 直接从 java 源码对表。
 */

const here = dirname(fileURLToPath(import.meta.url));
const STAGES_JAVA = resolve(here, '../../../z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/config/PipelineStages.java');
const PAGE_TSX = resolve(here, '../views/admin/PipelinesPage.tsx');

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

/** `public static final String BEFORE_CREATE = "BEFORE_CREATE";` → 常量名到字面量。 */
function constants(text: string): Map<string, string> {
  const map = new Map<string, string>();
  for (const m of text.matchAll(/public static final String (\w+) = "([^"]+)"/g)) {
    if (m[1] && m[2]) map.set(m[1], m[2]);
  }
  expect(map.size, '没解析出任何 String 常量, 说明正则或被读的文件不对').toBeGreaterThan(0);
  return map;
}

function region(text: string, anchorRe: RegExp, terminator: string, label: string): string {
  const start = text.search(anchorRe);
  expect(start, `在 PipelineStages.java 里找不到 ${label}`).toBeGreaterThanOrEqual(0);
  const rest = text.slice(start);
  const end = rest.indexOf(terminator);
  expect(end, `${label} 声明体不完整 (找不到结束符 "${terminator}")`).toBeGreaterThan(0);
  // 结束符本身要留在切片里: MANDATORY 的最后一项紧跟 `);`, 切在它之前会让末项凭空消失
  return rest.slice(0, end + terminator.length);
}

function resolveNames(names: string[], consts: Map<string, string>, label: string): string[] {
  expect(names.length, `${label} 解析出 0 项 —— 空参照集不能算通过`).toBeGreaterThan(0);
  return names.map((name) => {
    const value = consts.get(name);
    expect(value, `${name} 在 PipelineStages.java 里没有对应的 String 常量 (${label})`).toBeTruthy();
    return value as string;
  });
}

/** 后端那份口径, 每次现读: 解析失败要让具体用例红, 而不是整个文件悄悄没跑。 */
function engine() {
  const java = read(STAGES_JAVA, '后端阶段口径 PipelineStages.java');
  const consts = constants(java);
  const fromRegion = (anchor: RegExp, terminator: string, label: string, extract: RegExp) =>
    resolveNames(
      [...region(java, anchor, terminator, label).matchAll(extract)].map((m) => m[1] ?? '<未捕获>'),
      consts,
      label,
    );

  return {
    // PROCESSOR_BY_TYPE 的迭代顺序就是没有配置时那条默认链的执行顺序
    types: fromRegion(
      /private static Map<String, String> buildProcessorByType/,
      'return Collections.unmodifiableMap',
      'PROCESSOR_BY_TYPE',
      /m\.put\((\w+),\s*"\w+"\)/g,
    ),
    mandatory: fromRegion(/Set<String> MANDATORY/, ');', 'MANDATORY', /([A-Z_]+)\s*(?=[,)])/g),
    noOpOnWrite: fromRegion(/Set<String> NO_OP_ON_WRITE/, ');', 'NO_OP_ON_WRITE', /([A-Z_]+)\s*(?=[,)])/g),
    triggers: fromRegion(/List<String> SUPPORTED_TRIGGERS/, '}})', 'SUPPORTED_TRIGGERS', /add\((\w+)\)/g),
  };
}

/**
 * 后端 `buildConfigKeysByType` 里登记的参数键 (今天一个都没有)。
 * 只解析"哪些键被登记", 不解析"登记在哪一档" —— 那个 map 是按 PROCESSOR_BY_TYPE 循环填的,
 * 逐档的键清单在源码里根本不存在, 硬要按行号读会读出一份并不存在的对应关系。
 */
function javaConfigKeys(): string[] {
  const java = read(STAGES_JAVA, '后端阶段口径 PipelineStages.java');
  const body = region(java, /private static Map<String, List<String>> buildConfigKeysByType/,
    'return Collections.unmodifiableMap', 'CONFIG_KEYS_BY_TYPE');
  const puts = [...body.matchAll(/m\.put\([\s\S]*?\);/g)].map((m) => m[0] ?? '');
  // 空参照集不能算通过: 解析不到任何 m.put 就是被那个方法改了名/搬了家
  expect(puts.length, 'CONFIG_KEYS_BY_TYPE 里一个 m.put 都没解析出来, 词表在别处').toBeGreaterThan(0);
  return [...new Set(puts.flatMap((one) => [...one.matchAll(/"([^"]+)"/g)].map((m) => m[1] ?? '')))].sort();
}

function uiConfigKeys(): string[] {
  return [...new Set(PIPELINE_STAGE_TYPES.flatMap((item) => item.configKeys))].sort();
}

describe('流水线前端口径与引擎同源', () => {
  it('阶段清单与后端 PROCESSOR_BY_TYPE 一字不差（含顺序，那就是执行顺序）', () => {
    expect(PIPELINE_STAGE_TYPES.map((item) => item.type)).toEqual(engine().types);
  });

  it('必填闸门与不做事标记按后端的两个集合标注，不是前端自己猜', () => {
    const { mandatory, noOpOnWrite } = engine();
    expect(PIPELINE_STAGE_TYPES.filter((item) => item.mandatory).map((item) => item.type)).toEqual(mandatory);
    expect(PIPELINE_STAGE_TYPES.filter((item) => item.noOpOnWrite).map((item) => item.type)).toEqual(noOpOnWrite);
    for (const item of PIPELINE_STAGE_TYPES) {
      expect(item.mandatory && item.noOpOnWrite, `${item.type} 不可能既是闸门又什么都不做`).toBe(false);
      expect(item.label, `${item.type} 少了中文名`).not.toBe(item.type);
    }
  });

  it('触发事件只给引擎真有挂接点的那几个', () => {
    // 这一句排在对照后端之前: 一个多出来的写后事件会同时打掉两句，而只有这一句说得出"为什么不行"。
    expect(isPipelineTriggerSupported('AFTER_CREATE'), '写后没有回调落点，不能算支持').toBe(false);
    expect([...PIPELINE_SUPPORTED_TRIGGERS]).toEqual(engine().triggers);
    expect(isPipelineTriggerSupported('BEFORE_CREATE')).toBe(true);
  });

  it('阶段参数词表与后端 CONFIG_KEYS_BY_TYPE 同源（两边今天都是空的）', () => {
    // ⚠ 空清单相等是**真空为真**: 它证明的是"两边都没有登记", 不是"有参数时被读到了"。
    // 第一个真参数落地时, 这一句仍然会绿, 所以那一句"处理器确实读了它"必须跟着一起补,
    // 否则 #42 只是换了个字段名重新长回来 —— 记进 _e2e/README.md 的待办。
    // 先问"后端有没有多出键", 再问"两边对不对得上": 两句的失败消息指向两种相反的漂法,
    // 反过来写会让"后端登记了参数"也报成"前端不许声明", 红消息就把人往错的方向带。
    expect(javaConfigKeys(), `后端登记的参数键变了 (${javaConfigKeys()}), 配置页的"没有可配参数"这句就该改`).toEqual([]);
    expect(uiConfigKeys(), '前端不许声明后端没登记的参数').toEqual(javaConfigKeys());
    for (const item of PIPELINE_STAGE_TYPES) {
      expect(Array.isArray(item.configKeys), `${item.type} 少了 configKeys 这一栏`);
    }
  });

  it('配置页不再提供一个能填、但引擎不会读的参数框', () => {
    const page = read(PAGE_TSX, '流水线配置页');
    // 先钉"这一格真的换了句实话", 再说"没有输入框" —— 只留后面那半句的话,
    // 把整格删掉也能全绿 (负向断言要有猎物)。
    expect(page.includes('引擎不读取'), '老配置行带着没人读的参数时, 页面必须说出来').toBe(true);
    expect(page.includes('pipeline-stage-config-'), '每一档那一格的位置还在 (换成了一句陈述)').toBe(true);
    expect(page.includes('Input.TextArea'), '参数框回来了: 填进去的东西后端一个字都不读').toBe(false);
    expect(page.includes('patchStage(index, { config'), '界面不许再往 config 里写任何东西').toBe(false);
    expect(page.includes('保留给后续实现'), '不许再拿"以后会实现"暗示现在填了有用').toBe(false);
  });

  it('页面上再也没有"没有执行器"的幽灵阶段', () => {
    const page = read(PAGE_TSX, '流水线配置页');
    for (const ghost of ['WEBHOOK', 'SCRIPT']) {
      expect(page.includes(ghost), `${ghost} 在引擎里没有执行器，页面不该再出现它`).toBe(false);
    }
    // 没有挂接点的事件同样不许出现在选择项里（列表列里作为"这行不会执行"的告警是另一回事）
    const optionsLine = page.slice(page.indexOf('PIPELINE_SUPPORTED_TRIGGERS.map'), page.indexOf('PIPELINE_SUPPORTED_TRIGGERS.map') + 200);
    expect(optionsLine.includes('AFTER_'), '触发事件选择项里又混进了写后事件').toBe(false);
    expect(page.includes('"url":"https'), '阶段参数提示不能继续暗示 webhook 已经能配').toBe(false);
    expect(page.includes('外部通知'), '描述里不许再承诺引擎兑现不了的能力').toBe(false);
  });
});

describe('阶段链的顺序是真的顺序', () => {
  it('parseStages 按 order 排，与后端 parseTypes 的排序键一致', () => {
    const parsed = parseStages(
      '[{"type":"VALUE_VALIDATE","order":3},{"type":"REQUIRED_CHECK","order":1},{"type":"TYPE_CONVERT","order":2}]',
    );
    expect(parsed.map((item) => item.type)).toEqual(['REQUIRED_CHECK', 'TYPE_CONVERT', 'VALUE_VALIDATE']);
  });

  it('上下移动真的改变将要执行的顺序：order 按数组位置重写', () => {
    // 这份 fixture 的 order 必须和数组位置**不一致** —— 那才是用户在编辑器里点了上移之后
    // 真实拿到的形状 (数组位置动了, 每项还带着从库里解析出来的旧 order)。
    // 如果 order 恰好等于下标, `stage.order ?? index` 这个旧写法会原样通过, 注入也照抓不住。
    const moved = [
      { type: 'VALUE_VALIDATE', config: {}, order: 2 },
      { type: 'TYPE_CONVERT', config: {}, order: 0 },
      { type: 'REQUIRED_CHECK', config: {}, order: 1 },
    ];
    const raw = JSON.parse(stringifyStages(moved)) as { type: string; order: number }[];
    expect(raw.map((item) => item.order), '旧 order 被原样写回 = 界面动了、执行链没动').toEqual([0, 1, 2]);
    expect(raw.map((item) => item.type)).toEqual(['VALUE_VALIDATE', 'TYPE_CONVERT', 'REQUIRED_CHECK']);
    // 写回去的顺序再解析出来, 必须还是数组里这个顺序 (后端按 order 排)
    expect(parseStages(stringifyStages(moved)).map((item) => item.type)).toEqual([
      'VALUE_VALIDATE',
      'TYPE_CONVERT',
      'REQUIRED_CHECK',
    ]);
  });

  it('新建的默认链就是引擎今天跑的那三道闸门，且满足值校验在转换之后', () => {
    const { mandatory } = engine();
    const draft = newPipelineDraft('crm');
    expect(isPipelineTriggerSupported(draft.triggerEvent), '草稿默认就得是一份会被拒的配置').toBe(true);
    const types = parseStages(draft.stages).map((item) => item.type);
    expect([...types].sort()).toEqual([...mandatory].sort());
    expect(types.indexOf('VALUE_VALIDATE'), '值校验排到转换之前会把合法数字按字符长度拒掉').toBeGreaterThan(
      types.indexOf('TYPE_CONVERT'),
    );
    expect(types).toEqual(['REQUIRED_CHECK', 'TYPE_CONVERT', 'VALUE_VALIDATE']);
    expect(defaultPipelineStages().map((item) => item.type)).toEqual(types);
    expect(draft.enabled).toBe(1);
  });

  it('坏 JSON 不会把阶段清单悄悄变成空', () => {
    expect(parseStages('not json')).toEqual([]);
    expect(parseStages('{"type":"TYPE_CONVERT"}')).toEqual([]);
    expect(parseStages('[]')).toEqual([]);
    expect(parseStages(null)).toEqual([]);
  });
});
