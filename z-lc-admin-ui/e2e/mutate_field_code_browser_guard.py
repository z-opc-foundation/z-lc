#!/usr/bin/env python3
"""字段编码那道闸的**浏览器层**注入自证（`browser-e2e.mjs` 第 11c 段那 7 条）。

跑法（在 z-lc-admin-ui 下，需要 18090 的后端在跑；5274 要让出来）：

    python3 e2e/mutate_field_code_browser_guard.py

为什么单开一支：`mutate_designer_field_code.py` 判的是 vitest（`DesignerPage.test.tsx` 那些用例），
vitest 层能证明 `fieldCodeProblem('id')` 返回那句话，证明不了那句话走到了真渲染、
也证明不了保存按钮真的 disabled、更证明不了没有请求偷发出去。这一支的分母是**真构建 + 真浏览器**
里 11c 那 7 条检查的名字，一条不多一条不少。

五个注入，每个只摘掉一句保证，红集合互不相同 —— 这才是"每条检查各自钉住一件事"的证据：

  B1 摘掉自建列那条分支        → `id` 变成完全合法：告警、标红、按住按钮一起没（4 条红）
  B2 那一栏不再自己标红        → 只有"那一栏自己标红"红（其余照常，页面底部那句泛泛警告仍然在）
  B3 保存按钮不再被闸按住      → 只有"保存按钮被按住了"红
  B4 文案不再点名编码与自建列  → 只有"文案里点名是哪一列，且带出那五个自建列"红
  B5 撞自建列说成"不合法"      → 红在 c1（**这是 c1 唯一能被证伪的形状**：`id` 是合法标识符，
                                  把它报成"不合法"是把两种错混成一种，用户会去改一个没毛病的名字）

⚠ 三条**没有**注入、按未覆盖记账，不写成"全都能抓到"：
  「拦住 = 一次写请求都没发出去」「松开之后仍然没有偷发写请求」—— 这两条数的是请求条数，
  要红必须**新增**一个自动提交的行为（往组件里加 effect），那不是"摘掉一道守卫"的形状，
  而是造一个新缺陷；机制本身开火是实测过的（同一套计数监听抓到过日历 43 次/300ms 的重查）。
  「改回干净编码后闸立刻松开」也同理：它的失效形状是"输入不受控 / memo 粘住"，
  单点摘源码里的哪一行都不会让它红成它自己 —— 把 memo 依赖摘空会连带让前 4 条一起红，
  那就退回 B1 了。这三条留在这里，不假装证过。

判据同 `mutate_pivot_browser_guard.py`：`FAIL <名字>   << <读数>` 必须先剥掉读数再比集合，
不剥的话同一批名字会**同时**被判成"预期却没红"和"预期外的红"。
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
RULES = UI / "src/fields/columnRules.ts"
DESIGNER = UI / "src/views/designer/DesignerPage.tsx"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
# 上限而不是预期：这一支每轮跑完整的 browser-e2e（137 条真浏览器检查），
# 低负载时一轮 ~660s。今天机器 load 74+，一轮能翻一倍，所以把天花板抬到能容纳慢，
# 但**不把"跑不完"当成通过** —— 超时会抛 TimeoutExpired，那一轮记为无结论。
BUILD_TIMEOUT = 900
ROUND_TIMEOUT = 3000
FAIL_DETAIL_SEP = "   << "

C_MSG_KIND = '撞自建列时说的是「撞了引擎自建列」而不是「不合法」（id 本身是合法标识符）'
C_NAMED = '文案里点名是哪一列，且带出那五个自建列'
C_CELL_RED = '那一栏自己标红（不是只有页面底部一句泛泛的警告）'
C_BTN = '保存按钮被按住了'

RESERVED_BRANCH = (
    "  if (SYSTEM_COLUMN_CODES.includes(value.toLowerCase())) {\n"
    "    return `字段编码「${value}」撞了引擎自建列（${SYSTEM_COLUMN_CODES.join(' / ')}），"
    "建表会直接 Duplicate column name`;\n"
    "  }\n"
)
GENERIC_SENTENCE = (
    "    return `字段编码「${value}」撞了引擎自建列（${SYSTEM_COLUMN_CODES.join(' / ')}），"
    "建表会直接 Duplicate column name`;"
)
STATUS_ANCHOR = ("status={defs[index]?.fieldCode && fieldCodeProblem(defs[index].fieldCode) "
                 "? 'error' : undefined}")
DISABLED_ANCHOR = 'disabled={!dirty || codeIssues.length > 0}'

# tag -> (file, anchor, replacement, 预期红集合, 这一支摘掉的是哪句保证)
RUNS = [
    ("B1 摘掉自建列那条分支", RULES, RESERVED_BRANCH, "",
     [C_MSG_KIND, C_NAMED, C_CELL_RED, C_BTN],
     "`id` 完全合法：告警、标红、按住按钮一起没有"),
    ("B2 那一栏不再自己标红", DESIGNER, STATUS_ANCHOR, "status={undefined}",
     [C_CELL_RED],
     "只有页面底部那句泛泛警告，用户看不出是哪一栏"),
    ("B3 保存按钮不再被闸按住", DESIGNER, DISABLED_ANCHOR, "disabled={!dirty}",
     [C_BTN],
     "文案照说、栏照红，但按钮点得动"),
    ("B4 文案不再点名编码与自建列", RULES, GENERIC_SENTENCE,
     "    return `字段编码撞了引擎自建列，建表会直接 Duplicate column name`;",
     [C_NAMED],
     "报对了类型，却不说是哪一列、也不给那五个名字"),
    ("B5 撞自建列说成「不合法」", RULES, GENERIC_SENTENCE,
     "    return `字段编码「${value}」不合法（保留列，建表会撞名）`;",
     [C_MSG_KIND, C_NAMED],
     "两种错混成一种：用户会去改一个本身没毛病的名字"),
]

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_fieldcode_browser_logs"
LOG_DIR.mkdir(exist_ok=True)


def _expected_of(run):
    """取一支注入的预期红集合。RUNS 的槽序是 (tag, path, anchor, repl, expected, note)。

    必须**逐个具名解包**而不是 `_, _, _, e, _, _` 这样的简写：我第一版就是那么写的，`e`
    静默落到了 repl（替换文本）上，后果有两条 ——「两支注入不许有相同预期红集合」这条守卫
    改成拿替换文本互比，而每支的替换文本天生不同 ⇒ **守卫空转、永远通过**；而新加的
    「预期红的名字必须在门禁里出现一次」会把 repl 当字符串逐字符 iterate，第一次真跑就会
    以一条乱码理由（`' ' 出现 18860 次`）拒跑。位置绑定错了不报错，这是它最贵的地方。
    """
    tag, path, anchor, repl, expected, note = run  # noqa: F841 — 具名解包就是这条守卫本身
    return tuple(sorted(expected))


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
    print(f"    built: {bundle_name()}")


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)],
                            cwd=UI, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    # 30s 是低负载时定的。今天这台机器 load 74+，后端 JVM 冷启动就要 80s，
    # 把 preview 的等待卡在 30s 只会把"慢"误报成"起不来"，然后一整轮注入白跑。
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
        # 跑不完 == 无结论，不是 == 全绿：整支收线并让退出码非零。
        # 上一行注释说了要做的事，这里必须真做，否则"超时"会以 Python 回溯的样子混进日志，
        # 看的人得先猜它死在哪一层。
        raise SystemExit(f"{label}: browser-e2e 一轮超过 {ROUND_TIMEOUT}s 没收线 → "
                         "这一轮没有结论（不许当成通过），先等机器空闲再重跑")
    text = proc.stdout + proc.stderr
    keep = LOG_DIR / f"{label}.log"
    keep.write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {keep}")
        return [f"<build-guard:{label}>"]
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，见 {keep}")
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    passed = len(re.findall(r"^  PASS  ", text, flags=re.M))
    print(f"  {label}: PASS {passed} / FAIL {len(failed)}")
    return failed


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if not hard and not extra:
        print(f"  OK  {label}: 预期 {len(expected)} 条全红，无一条连带红")
        return 0
    if hard:
        print(f"  !! {label}: 预期变红却没红（这几条浏览器检查是空的）: {hard}")
    if extra:
        print(f"  !! {label}: 出现预期之外的红 {extra} —— 不许加白名单了事，"
              "先拿样本证明这一处注入换掉了哪一支断言，再改预期")
    return 1


def main() -> int:
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              "\n   先确认那是谁（`lsof -nP -iTCP:{PORT} -sTCP:LISTEN`），是自己这一支的量具再停。")
        return 2

    originals = {f: f.read_text(encoding="utf-8") for f in (RULES, DESIGNER)}
    for tag, path, anchor, _, _, _ in RUNS:
        n = originals[path].count(anchor)
        if n != 1:
            print(f"!! anchor 在 {path.name} 里出现 {n} 次（{tag}），注入无法定位")
            return 2
    sets = [_expected_of(r) for r in RUNS]
    if len(set(sets)) != len(sets):
        print("!! 有两支注入的预期红集合相同 —— 那要么是不同的断言红在同一个标题下（要去核文本），"
              "要么其中一支是重复检查，先别跑")
        return 2

    # 预期红的名字必须真的在那 137 条里，而且要唯一。
    # 不查这一条的话，一次改名要等整轮 build + 整轮浏览器跑完（今天这台机器 ~20 分钟）
    # 才被报成"预期变红却没红"，而且那句话会把"检查被改名了"误读成"我的检查是空的"。
    suite = E2E.read_text(encoding="utf-8")
    for name in sorted({n for r in RUNS for n in _expected_of(r)}):
        n = suite.count(name)
        if n != 1:
            print(f"!! 预期红的名字在 browser-e2e.mjs 里出现 {n} 次（应为 1）: {name}")
            return 2

    bad = 0
    preview = None
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===")
        build("00_baseline")
        preview = start_preview()
        if run_round("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for i, (tag, path, anchor, repl, expected, note) in enumerate(RUNS, start=1):
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} —— 摘掉的是：{note} ===")
            path.write_text(originals[path].replace(anchor, repl, 1), encoding="utf-8")
            try:
                build(label)
                bad += judge(tag, expected, run_round(label))
            finally:
                path.write_text(originals[path], encoding="utf-8")
                if path.read_text(encoding="utf-8") != originals[path]:
                    print(f"  !! {path.name} 未恢复到原始内容")
                    bad += 1

        print("\n=== 恢复后复跑 ===")
        build("99_restored")
        if (failed := run_round("99_restored")):
            print(f"  !! 恢复后仍有红: {failed}")
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    print(f"\n{'FAILED: ' + str(bad) if bad else '11c 那 4 条各自钉住一件事（另 3 条见文档里的未覆盖记账）'}")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
