#!/usr/bin/env python3
"""#41/#42 处理流水线那 23 条浏览器检查的**浏览器层**注入自证（`browser-e2e.mjs` 第 11a 段）。

跑法（在 z-lc-admin-ui 下，需要 18090 的后端在跑，且 5274 空着 —— 这一支自己起 preview）：

    python3 e2e/mutate_pipeline_browser_guard.py
    python3 e2e/mutate_pipeline_browser_guard.py --only S1,S5   # 只重跑两支，收线会打 PARTIAL

为什么单开一支（vitest 那十三支 F1–F13 不够）：`mutate_pipeline_wiring_guard.py` 判的是 jsdom 里
`PipelinesPage.test.tsx` 那两条用例，它能证明 `parseStages` 排了序、选择项里没有幽灵，
但证明不了**真构建出来的那份 bundle** 在真浏览器里把 order 画成了执行顺序，也证明不了
"空链被拒"那一次真的是服务端拒的而不是界面在前端偷偷拦下来。第 11a 段这一节自己就带了一条
"真的发出了请求"的网络断言，那一条如果没有注入给它当猎物，它和一句永远为真的话没有区别。

九支注入，每支各自摘掉一句保证，预期红集合互不相同：

  S1 parseStages 不再按 order 排       → 画出来的是数组位置（2 条红：顺序 + 每格里的阶段编码）
  S2 DICT_RESOLVE 的 noOpOnWrite 翻假  → 那一档不再说"今天什么都不做"（1 条红）
  S3 词表里塞回幽灵 WEBHOOK            → 选择项多出第 6 项，幽灵又能选了（2 条红）
  S4 触发事件塞回 AFTER_CREATE         → 下拉框多出"更新后"（1 条红）
  S5 save() 在前端就把空链拦下来       → 那一枪根本打不到服务端（3 条红：请求 / 只此一枪 / 后端文案）

⚠ S5 这一支先后打掉过**量具自己**的两个缺陷，两处都已改在 `browser-e2e.mjs` 里，不是改预期：
  1. 原来 N_NOOP 那条按 `stageTags[3]` 读 —— S1 只是把行序换了、牌子仍跟着 DICT_RESOLVE 走，
     它却跟着红。行序归 N_ORDER 管，所以改成按"code↔badge 配对"读（改完双向实测：S1 形状留绿、
     S2 形状必红）。**坐标耦合不是因果，别把它写进预期红集合。**
  2. 原来点保存后 `waitFor('.ant-message-notice')` —— 这一条的 3s 自动收起赶不上"没等到响应"
     那条路的 15s，于是 S5 轮里整节被一次超时带走，N_TOAST 连跑都没跑到（报成"预期变红却没红"），
     红反倒在兜底 catch 那句上。改成**点保存前挂 toast 收集器**，读的是"曾经出现过的那些 toast"，
     这正是 N_TOAST 那句话的本意（界面**说过**什么，不是此刻还挂着什么）。
  S6 挂接点列不再给中文名              → 「创建前」变回裸码 BEFORE_CREATE（1 条红）
  S7 阶段删空时不再说会被后端拒        → 那句红字消失（1 条红）
  S8 选择项里不再说必填/不做事         → 用户在这里点删除前得不到任何提示（1 条红）
  S9 (#42) 那一格换回能打的参数框      → 两条一起红：每档还在不在说"有没有参数" +
                                        那一格还是不是一个能填的框

⚠ 三条**没有**注入、按未覆盖记账（写在这里而不是假装全都能抓到）：
  「种下一份 order 与数组位置不一致的配置」是这一节的前提，它的失效形状是服务端写入口被改回
    放行脏配置 —— 那归部署件层 P1–P12 与 `[15p]` 打，浏览器层不该也没法自己造。
  「编号就是 1..N 的执行位数」在这一节是**过定**的：种下去的 order 恰好是 0..3 连续，
    于是 `index + 1` 与 `order + 1` 永远同值，把编号改成按 order 显示也不会红。
    要让它可证伪得再种一份 order 有空洞的配置，那会同时改掉 S1 的猎物 —— 不如就在这里记成过定。
  「那次被拒的保存一行都没进表」的失效形状是"后端收了空链"或"前端不重查表"，
    前者由部署件层 P10 打（它红的是"被拒"本身），后者要在页面里摘掉 reload，那不是这一支持 claim 的那句保证。

判据同 `mutate_field_code_browser_guard.py`：`FAIL <名字>   << <读数>` 先剥读数再比集合；
预期红的名字必须在 `browser-e2e.mjs` 里逐字出现一次（改名要在这里当场拒跑，不要等 4 分钟后
被报成"预期变红却没红"）；跑不完 == 无结论，不是 == 通过。
"""

import os
import re
import signal
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import _mutlock

UI = Path(__file__).resolve().parents[1]
API = UI / "src/api/pipeline.ts"
PAGE = UI / "src/views/admin/PipelinesPage.tsx"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
BUILD_TIMEOUT = 900
ROUND_TIMEOUT = 3000
FAIL_DETAIL_SEP = "   << "

# 一轮"跑完了"的最小条数。这一节 23 条、全门禁 160 条 —— 低于此值一定是**有段落中途退出**
# （浏览器崩 / 前置条件断 / 超时），而不是"检查变少了"。中途退出的一轮里"零条红"没有意义：
# 2026-09-26 实测过一支，浏览器在还原轮崩掉，报出 `PASS 4 / FAIL 1`，那 1 条红是
# `page.waitForTimeout: Target page, context or browser has been closed` —— 崩红不是判据。
MIN_BASELINE_TOTAL = 150
STATE = {"total": None}

N_ORDER = '阶段链按 order 画，不是照抄 JSON 数组的位置（数组第一项是字典解析，画出来它排最后）'
N_CODES = '后端认得的阶段编码原样写在每一格里（只有中文名的话，词表漂了没人看得出来）'
N_NOOP = '只有今天什么都不做的那一档带「写路径暂不做事」，三道闸门都不带'
N_TRIGNAME = '真有挂接点的那一行给中文挂接点名'
N_TRIGOPT = '触发事件只能选到引擎真有挂接点的那两个（写后事件一个都不许出现在选择项里）'
N_STAGEOPT = '阶段选择项与后端那份词表同源：正好这 5 个，多一个少一个都算漂'
N_GHOST = '没有执行器的幽灵阶段不能再被选中（同一格读数里刚数满 5 项）'
N_NOOPOPT = '摘不得的三道闸门与"什么都不做"在选择项里就说出来（用户是在这里决定摘不摘的）'
N_MODAL = '阶段删空时界面当场说清这一份保存会被后端拒绝'
N_SENT = '点保存真的发出了请求（这一节测的是服务端那道闸，不是界面在前端偷偷拦）'
N_ONESHOT = '整轮 pipeline-config/create 恰好一个 POST（放行的登记不许多于实际发生的那一次）'
N_TOAST = '空链真的被后端拒了，而界面没有把它报成「已保存」（UI 不许替后端说好话）'
# #42 那两条 (每一档那一格说了什么 / 它不是一个能填的框)
N_CELLS = '每一档都还在说这一档有没有参数（格子整个消失也算"没有输入框"，那是删证据不是修谎）'
N_NOBOX = '阶段参数不再是一个能填的框（填了也不生效的配置，给个输入框就是骗人）'

SORT_LINE = "      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));"
# 词表每一行末尾多了 `configKeys: []` (#42) —— 锚是逐字比对的, 前端清单一改这里就得跟着改,
# 否则这几支会在 anchor 检查上抛"锚出现 0 次"。这比悄悄少跑一支好。
DICT_ROW = ("  { type: 'DICT_RESOLVE', label: '字典解析', mandatory: false, noOpOnWrite: true,"
            " configKeys: [] },")
VALUE_ROW = ("  { type: 'VALUE_VALIDATE', label: '值域校验', mandatory: true, noOpOnWrite: false,"
             " configKeys: [] },")
TRIG_ROW = ("export const PIPELINE_SUPPORTED_TRIGGERS = ['BEFORE_CREATE', 'BEFORE_UPDATE'] as const;")
SAVE_HEAD = ("    if (!editing?.entityCode?.trim()) {\n"
             "      message.warning('请选择实体');\n"
             "      return;\n"
             "    }")
TRIGGER_TAG = ("<Tag data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>"
               "{PIPELINE_TRIGGER_LABELS[trigger] ?? trigger}</Tag>")
MODAL_WARN = ('<Text type="danger">还没有阶段 —— 空链保存会被后端拒绝，因为它绕过必填/类型/值域三道闸门</Text>')
OPTION_LABEL = ("label: `${item.label} ${item.type}${item.mandatory ? '（必填）' : ''}"
                "${item.noOpOnWrite ? '（写路径暂不做事）' : ''}`,")

# S9 (#42): 那一格现在是一句陈述; 注入把它换回 #42 修掉之前那个"能打的参数框"。
PARAM_TEXT = """<Text
                      type={Object.keys(stage.config ?? {}).length > 0 ? 'danger' : 'secondary'}
                      data-testid={`pipeline-stage-config-${index}`}
                      style={{ display: 'inline-block', width: 260 }}
                    >
                      {Object.keys(stage.config ?? {}).length > 0
                        ? `参数 ${Object.keys(stage.config ?? {}).join(' / ')} 引擎不读取，保存会被拒`
                        : '这一档没有可配参数（引擎不读取阶段参数）'}
                    </Text>"""
PARAM_BOX = """<Input.TextArea
                      size="small"
                      style={{ width: 260 }}
                      rows={1}
                      value={JSON.stringify(stage.config ?? {})}
                      placeholder="阶段参数（当前引擎不读取，保留给后续实现）"
                      onChange={(event) => {
                        try {
                          patchStage(index, { config: JSON.parse(event.target.value || '{}') as Record<string, unknown> });
                        } catch {
                          /* keep editing an invalid JSON without clobbering the draft */
                        }
                      }}
                    />"""
PAGE_IMPORT = "  Card,\n  Modal,"
PAGE_IMPORT_WITH_INPUT = "  Card,\n  Input,\n  Modal,"

# (tag, 摘掉的是哪句保证, [(file, anchor, repl)...], 预期红集合)
RUNS = [
    ("S1", "parseStages 不再按 order 排：看到的是数组位置，引擎跑的是 order",
     [(API, SORT_LINE, "      .sort(() => 0);")], [N_ORDER, N_CODES]),
    ("S2", "DICT_RESOLVE 不再承认自己今天不改变数据",
     [(API, DICT_ROW, DICT_ROW.replace("noOpOnWrite: true", "noOpOnWrite: false"))], [N_NOOP]),
    ("S3", "词表里塞回没有执行器的幽灵阶段",
     [(API, VALUE_ROW, VALUE_ROW + "\n  { type: 'WEBHOOK', label: '外部通知', mandatory: false, noOpOnWrite: false },")],
     [N_STAGEOPT, N_GHOST]),
    ("S4", "触发事件塞回引擎没有挂接点的 AFTER_CREATE",
     [(API, TRIG_ROW, TRIG_ROW.replace("'BEFORE_UPDATE'] as const;", "'BEFORE_UPDATE', 'AFTER_CREATE'] as const;"))],
     [N_TRIGOPT]),
    ("S5", "空链被前端偷偷拦下：那一枪根本打不到服务端",
     [(PAGE, SAVE_HEAD, "    if (stages.length === 0) {\n"
                        "      message.warning('还没有阶段，前端先拦住（不发给后端）');\n"
                        "      return;\n"
                        "    }\n" + SAVE_HEAD)],
     [N_SENT, N_ONESHOT, N_TOAST]),
    ("S6", "挂接点那一格不再说中文名，只回显原始事件码",
     [(PAGE, TRIGGER_TAG, "<Tag data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>{trigger}</Tag>")],
     [N_TRIGNAME]),
    ("S7", "阶段删空时那句「会被后端拒绝」的红字撤掉",
     [(PAGE, MODAL_WARN, "<Text>—</Text>")], [N_MODAL]),
    ("S8", "选择项里不再说这一档摘不得 / 今天什么都不做",
     [(PAGE, OPTION_LABEL, "label: `${item.label} ${item.type}`,")], [N_NOOPOPT]),
    # S9 (#42): 把那一格换回"能打的参数框"。两支同时改: 撤掉陈述 + 请回 Input。
    # 这正是 #42 修掉之前那个状态 —— 五个处理器没有一个读 config, 而界面摆着一个输入框,
    # 填什么都改变不了任何行为 (后端现在会把这种配置 400 拒掉)。
    ("S9", "阶段参数又变回一个能填、却不会被读的输入框",
     [(PAGE, PARAM_TEXT, PARAM_BOX), (PAGE, PAGE_IMPORT, PAGE_IMPORT_WITH_INPUT)],
     [N_CELLS, N_NOBOX]),
]

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_pipe41_browser_logs"
LOG_DIR.mkdir(exist_ok=True)

FILES = [API, PAGE]
ONLY = {x.strip() for x in (sys.argv[2].split(",") if sys.argv[1:2] == ["--only"] else []) if x.strip()}
if sys.argv[1:2] == ["--only"] and not ONLY:
    raise SystemExit("--only 后面是空的: 一支都不跑不等于「全跑」, 那是「没有判定」")
if ONLY:
    unknown = sorted(ONLY - {tag for tag, _, _, _ in RUNS})
    if unknown:
        raise SystemExit(f"--only 里有不认识的注入编号: {unknown}（可选 {[r[0] for r in RUNS]}）")


def port_busy() -> bool:
    for host in ("127.0.0.1", "::1"):
        family = socket.AF_INET if ":" not in host else socket.AF_INET6
        with socket.socket(family, socket.SOCK_STREAM) as sock:
            sock.settimeout(0.4)
            if sock.connect_ex((host, PORT)) == 0:
                return True
    return False


def bundle_name() -> str:
    html = DIST_INDEX.read_text(encoding="utf-8") if DIST_INDEX.exists() else ""
    match = re.search(r'assets/(index-[^"\']+\.js)', html)
    return match.group(1) if match else "<dist/index.html 里没有 index-*.js>"


def build(label: str) -> None:
    proc = subprocess.run(["npm", "run", "build"], cwd=UI,
                          capture_output=True, text=True, timeout=BUILD_TIMEOUT)
    (LOG_DIR / f"{label}_build.log").write_text(proc.stdout + proc.stderr, encoding="utf-8")
    if proc.returncode != 0:
        raise SystemExit(f"build 失败（{label}），见 {LOG_DIR / (label + '_build.log')}")
    print(f"    built: {bundle_name()}", flush=True)


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)],
                            cwd=UI, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    for _ in range(240):
        if port_busy():
            return proc
        time.sleep(0.5)
    os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
    raise SystemExit(f"preview 起不来（:{PORT} 120s 内没在听）")


def stop_preview(proc: subprocess.Popen) -> None:
    try:
        os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
        proc.wait(timeout=20)
    except (ProcessLookupError, subprocess.TimeoutExpired):
        try:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
        except ProcessLookupError:
            pass


def run_round(label: str) -> list[str]:
    try:
        proc = subprocess.run(["node", str(E2E)], cwd=UI, capture_output=True, text=True,
                              timeout=ROUND_TIMEOUT, env={**os.environ, "E2E_REPEATS": "1"})
    except subprocess.TimeoutExpired:
        raise SystemExit(f"{label}: browser-e2e 一轮超过 {ROUND_TIMEOUT}s 没收线 → "
                         "这一轮没有结论（不许当成通过），先等机器空闲再重跑")
    text = proc.stdout + proc.stderr
    keep = LOG_DIR / f"{label}.log"
    keep.write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {keep}", flush=True)
        return [f"<build-guard:{label}>"]
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，见 {keep}")
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    passed = len(re.findall(r"^  PASS  ", text, flags=re.M))
    total = passed + len(failed)
    print(f"  {label}: PASS {passed} / FAIL {len(failed)}（本轮共 {total} 条）", flush=True)
    if STATE["total"] is None:
        if total < MIN_BASELINE_TOTAL:
            raise SystemExit(f"基线一轮只跑出 {total} 条（<{MIN_BASELINE_TOTAL}）→ 有段落中途退出，"
                             f"这样的基线定不了分母，一切实验作废，见 {keep}")
        STATE["total"] = total
    elif total != STATE["total"]:
        raise SystemExit(f"{label}: 本轮只有 {total} 条，基线 {STATE['total']} 条 → "
                         "有段落中途退出（超时/浏览器崩/前置条件断），注入结果无法归因；"
                         f"这一轮不算结论，见 {keep}")
    return failed


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}", flush=True)
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if not hard and not extra:
        print(f"  OK  {label}: 预期 {len(expected)} 条全红，无一条连带红", flush=True)
        return 0
    if hard:
        print(f"  !! {label}: 预期变红却没红（这条浏览器检查是空的）: {hard}", flush=True)
    if extra:
        print(f"  !! {label}: 出现预期之外的红 {extra} —— 不许加白名单了事，"
              "先拿样本证明这一处注入换掉了哪一支断言，再改预期", flush=True)
    return 1


def main() -> int:
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              "\n   先停掉自己那一支（`ps -eo pid,command | grep 'vite preview'`），再来。")
        return 2

    originals = {f: f.read_text(encoding="utf-8") for f in FILES}
    for tag, _, edits, _ in RUNS:
        if not edits:
            print(f"!! {tag} 没有任何 edit")
            return 2
        for path, anchor, repl in edits:
            n = originals[path].count(anchor)
            if n != 1:
                print(f"!! anchor 在 {path.name} 里出现 {n} 次（{tag}），注入无法定位:\n{anchor[:120]}")
                return 2
            if anchor == repl:
                print(f"!! {tag} 的注入是空操作")
                return 2

    # 两支注入的预期红集合不得相同 —— 相同就意味着其中一支的猎物其实是另一处口径给的
    sets = [tuple(sorted(expected)) for _, _, _, expected in RUNS]
    if len(set(sets)) != len(sets):
        print("!! 有两支注入的预期红集合相同，先分开再跑")
        return 2

    # 预期红的名字必须逐字出现在门禁里且只出现一次
    suite = E2E.read_text(encoding="utf-8")
    for name in sorted({n for _, _, _, expected in RUNS for n in expected}):
        n = suite.count(name)
        if n != 1:
            print(f"!! 预期红的名字在 browser-e2e.mjs 里出现 {n} 次（应为 1）: {name}")
            return 2

    bad = 0
    preview = None
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===", flush=True)
        build("00_baseline")
        preview = start_preview()
        if run_round("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线", flush=True)
            return 2

        for tag, note, edits, expected in RUNS:
            if ONLY and tag not in ONLY:
                continue
            print(f"\n=== {tag} —— 摘掉的是：{note} ===", flush=True)
            try:
                # 每支开头无条件把**在册文件全部**写回原始字节：上一支的还原若没成功，
                # 它的变异就会跟着后面每一支跑（读数会多出别支的红，被误记成"守卫漏检"）。
                for path, content in originals.items():
                    if path.read_text(encoding="utf-8") != content:
                        print(f"  !! 进入 {tag} 前发现 {path.name} 不是原始内容，先写回", flush=True)
                        path.write_text(content, encoding="utf-8")
                for path, anchor, repl in edits:
                    path.write_text(path.read_text(encoding="utf-8").replace(anchor, repl, 1),
                                    encoding="utf-8")
                build(tag)
                bad += judge(tag, expected, run_round(tag))
            finally:
                for path, _, _ in edits:
                    path.write_text(originals[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != originals[path]:
                        print(f"  !! {path.name} 未恢复到原始内容", flush=True)
                        bad += 1

        print("\n=== 恢复后复跑 ===", flush=True)
        build("99_restored")
        if (failed := run_round("99_restored")):
            print(f"  !! 恢复后仍有红: {failed}", flush=True)
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    ran = [r[0] for r in RUNS if not ONLY or r[0] in ONLY]
    tail = f"（PARTIAL: 只跑了 {len(ran)}/{len(RUNS)} 支 = {ran}，这份日志不能当整族绿）" if ONLY else ""
    print(f"\nRESULT: {'11a 那九支各自打掉一句保证' if bad == 0 else f'{bad} problem(s)'}{tail}",
          flush=True)
    return 1 if bad else 0


if __name__ == "__main__":  # 战役脚本不许被 import 就跑起来
    _mutlock.acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
