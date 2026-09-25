#!/usr/bin/env python3
"""provision 四态的**浏览器层**注入自证（`browser-e2e.mjs` 第 11d 段）。

跑法（在 z-lc-admin-ui 下；后端 18090 要在跑，5274 要让出来给这一支自己起的 preview）：

    python3 e2e/mutate_provision_browser_guard.py

为什么单开一支：`mutate_provision_report_guard.py` 判的是 vitest（`DesignerProvision.test.tsx`
那十几例），vitest 能证明"报告这么说时界面画成这样"，证明不了这份报告是真服务端给的、
也证明不了那句 toast 真的出现在真浏览器里、横幅真的挂上了 DOM。发出去的 jar 那一层
(`_e2e/mutate_provision_deployed_guard.py` D1–D13) 反过来管的是服务端，界面话术它看不见。
这一支管的是中间那段没人看的路：**状态 → 文案 → 渲染**。

六个注入，每个只摘掉一句保证，红集合互不相同 —— 这才是"每条检查各自钉住一件事"的证据：

  P1 ALTERED 说成无事发生   → 「补了 1 列」那条 + (d) 修好那条（它读的也是这半句）
  P2 EXISTS_INTACT 冒充成果 → 只红"幂等：再点一次不许宣称补了列"（把空操作记成成果就是 #43）
  P3 横幅不点名缺了哪一栏   → 只红"横幅写清缺了哪一栏"
  P4 横幅点名点错（把状态当实体名）→ 只红"横幅…点名是哪一份实体定义"—— 这一支是 17 那条的**唯一**
                              可证伪形状：第一版的断言写的是 `includes('case')`，而服务端 message 里
                              本来就带表名 ui_caseXXXXXX，任何一条泛泛红横幅都能撞出来
  P5 FAILED 不进 problems    → 横幅整个不出现：红 16/17/18/20 四条（**含 20 那条负向断言**：它同句里
                              先钉"那句红色横幅真的在页上"，摘掉横幅时它跟着红，而不是对着空页面打绿灯）
  P6 成功也不清 problems     → 横幅赖着不走：红 11（补成功后不许还挂着）+ 23（修好了横幅要清掉）

⚠ 四条**没有**注入，按未覆盖记账，不写成"全都能抓到"：
  「点了按钮真的打到 provision 接口且 HTTP 200」—— 它唯一的失效形状是把 `onClick` 摘掉/让按钮
  永久 disabled，那之后每一次 clickProvision 都要空等 20s+12s，且 11d 的后续状态一个也建不起来，
  于是一起红十几条并带走整节 —— 那是"造一个新缺陷"而不是"摘掉一句保证"。它要防的那半（服务端
  谎报）由 P1/P2 各自单独红过，"HTTP 200 不等于建成"这半由 P5 红。
  「设计器里有且只有一颗单实体 Provision 按钮」—— 让它红只能再多造一颗以 Provision 结尾的按钮，
  而 Playwright 的 locator 一到 2 个匹配就 strict-mode 抛错、整节进 catch，同上面一样不是"摘守卫"。
  「库里真的多了这一栏 / 库里确实没有这一栏 / 再点一次库里一列不多不少 / 补列不许把已有那一行弄丢」
  —— 这四条读的是 `/admin/db/table` 与 `/runtime/list`，改 .tsx 动不到库。它们该由谁证伪已经记在
  deployed 那层的 D 系列（D3/D8 的"读库的不会因报告撒谎而红"这条规律）与 java 单测 C 系列里；
  这一层把它们摆在 11d 里是为了**串起因果**（界面说补上了 → 库里真有），不是因为这一层能打断它们。
  照旧不在这一层：所有 `provision 探针*` / `加一栏的定义收下` / `先写一行进去` 那几条 —— 它们是
  **夹具自检**（量具红了说明环境坏了，不是被测的守卫），设计上就不许有注入能让它们红。

判据同 `mutate_field_code_browser_guard.py`：`FAIL <名字>   << <读数>` 必须先剥掉读数再比集合，
不剥的话同一批名字会**同时**被判成"预期却没红"和"预期外的红"。另加一条这一支自己的：
每轮的 PASS+FAIL 总数必须等于基线那一轮 —— 少了就是某一节中途抛错没跑完，"没红"是跑不完不是通过
（deployed 那层的分母守卫搬到浏览器层）。
"""

import os
import re
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

import _mutlock

UI = Path(__file__).resolve().parents[1]
DESIGNER = UI / "src/views/designer/DesignerPage.tsx"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
LOG_DIR = Path.home() / ".cache/zlc43/browser_guard"
LOG_DIR.mkdir(parents=True, exist_ok=True)

BUILD_TIMEOUT = 900
# 上限而不是预期：一轮跑的是完整的 browser-e2e（184 条真浏览器检查），低负载 ~7min。
# 今天这台机器 load 高时能翻倍。超时 == 无结论（抛 TimeoutExpired 收线），不 == 全绿。
ROUND_TIMEOUT = 3000
FAIL_DETAIL_SEP = "   << "

# ---- 11d 里被这一层 owns 的检查名，逐字抄自 browser-e2e.mjs ------------------------------------
T_ALTER_TOAST = '#47 写侧：界面按新状态说「补了 1 列」并点名是哪一栏（旧口径在这里永远报未建成）'
T_IDEMPOTENT = '幂等：再点一次说「这次没有执行 DDL」，不许继续宣称补了列（把空操作记成成果就是 #43）'
T_BANNER_MISSING = '横幅写清缺了哪一栏（光说"没建成"，用户不知道该动哪一栏）'
T_BANNER_NAMED = '「未建成」横幅常驻，并点名是哪一份实体定义（不点名只能一个个猜）'
T_BANNER_HTTP200 = '库拒的 DDL：HTTP 仍 200，而界面必须报「未建成」（200 不等于建成）'
T_BODY_NEGATIVE = '整页确实报了未建成、且没有一句"已建成/列一列不缺/补了列"（200 不等于建成）'
T_BANNER_CLEAR = '补成功之后「未建成」横幅不许留在页上（挂着就是谎报失败）'
T_REPAIRED = '补上默认值后同一个按钮真的修好了：ALTERED 点名那一栏，横幅清掉'
# 整节 catch 的兜底标题：它红了就说明这一节中途抛错，后面的检查一条没跑 —— 归因到此为止。
T_SECTION_CRASH = 'provision 结论走真后端（#43/#47 浏览器层）'

COVERED = [T_ALTER_TOAST, T_IDEMPOTENT, T_BANNER_MISSING, T_BANNER_NAMED,
           T_BANNER_HTTP200, T_BODY_NEGATIVE, T_BANNER_CLEAR, T_REPAIRED]

# 见文档里"未覆盖记账"那一段：这四条不是"忘了注入"，是这一层的形状动不到它们。
NOT_COVERED_UI = ['点了按钮真的打到 provision 接口且 HTTP 200（这一节测的是服务端结论，不是前端偷偷拦）',
                  '设计器里有且只有一颗单实体 Provision 按钮（点错成"全部实体"这一节就白测）']
NOT_COVERED_DB = ['库里真的多了这一栏（界面说补上了，判据得来自库而不是这句 toast）',
                  'FAILED 不是报告撒的谎：库里确实没有这一栏',
                  '再点一次库里一列不多不少（"没执行 DDL" 这句是库认账的，不是自述）',
                  '补列不许把已有那一行弄丢：旧行的 ref 还在，新栏按默认值落上（#47 只加不改不删）']

# ---- anchors: each must occur exactly once in the pristine file; the script refuses to guess ----
ALTER_BRANCH = (
    "        const added = item.addedColumns ?? [];\n"
    "        message.success(\n"
    "          `表本来就在，按这份定义补了 ${added.length} 列${added.length ? `：${added.join('、')}` : ''}`,\n"
    "        );\n"
)
INTACT_TOAST = "        message.success('表本来就在，列一列不缺，这次没有执行 DDL');\n"
MISSING_FRAGMENT = "{item.missingColumns?.length ? `（缺列 ${item.missingColumns.join(', ')}）` : ''}"
CODE_CHIP = "<Text code>{item.entityCode}</Text>"
PROBLEMS_ANCHOR = "setProvisionProblems(bad ? [item] : []);"

# 四条注释里说的"整行都不画"，用空 JSX 表达式而不是删掉那一行：删行会让缩进对不上，
# 而下一次跑的时候没人会发现文件已经被改坏。
RUNS = [
    ("P1 ALTERED 说成无事发生", ALTER_BRANCH,
     "        message.success('表本来就在，列一列不缺，这次没有执行 DDL');\n",
     [T_ALTER_TOAST, T_REPAIRED]),
    ("P2 EXISTS_INTACT 冒充补了列", INTACT_TOAST,
     "        message.success('表本来就在，按这份定义补了 0 列');\n",
     [T_IDEMPOTENT]),
    ("P3 横幅不点名缺了哪一栏", MISSING_FRAGMENT, "{''}",
     [T_BANNER_MISSING]),
    ("P4 横幅点名点错成状态", CODE_CHIP, "<Text code>{item.status}</Text>",
     [T_BANNER_NAMED]),
    ("P5 FAILED 不进 problems（横幅整个不出现）", PROBLEMS_ANCHOR, "setProvisionProblems([]);",
     [T_BANNER_HTTP200, T_BANNER_NAMED, T_BANNER_MISSING, T_BODY_NEGATIVE]),
    ("P6 成功也不清 problems（横幅赖着不走）", PROBLEMS_ANCHOR, "setProvisionProblems([item]);",
     [T_BANNER_CLEAR, T_REPAIRED]),
]


def bundle_name() -> str:
    html = DIST_INDEX.read_text(encoding="utf-8") if DIST_INDEX.exists() else ""
    match = re.search(r'assets/(index-[^"\']+\.js)', html)
    return match.group(1) if match else "<dist/index.html 里没有 index-*.js>"


def validate() -> int:
    """静态体检：锚点唯一、预期红集互不相同、标题在套件里逐字出现一次。

    全在这一支的代价（一轮 ~20 分钟）之前做，否则一次改名要等整轮 build + 整轮浏览器才发现。
    """
    src = DESIGNER.read_text(encoding="utf-8")
    for tag, anchor, _, _ in RUNS:
        n = src.count(anchor)
        if n != 1:
            print(f"!! anchor 在 DesignerPage.tsx 里出现 {n} 次（应为 1）: {tag}")
            return 1
    sets = [tuple(sorted(expected)) for _, _, _, expected in RUNS]
    if len(set(sets)) != len(sets):
        print("!! 两支注入的预期红集合相同 —— 那要么是不同的断言红在同一个标题下，要么其中一支是重复检查")
        return 1
    suite = E2E.read_text(encoding="utf-8")
    all_names = sorted({t for _, _, _, expected in RUNS for t in expected} | set(COVERED))
    for name in all_names:
        n = suite.count(name)
        if n != 1:
            print(f"!! 检查名在 browser-e2e.mjs 里出现 {n} 次（应为 1）: {name}")
            return 1
    for name in NOT_COVERED_UI + NOT_COVERED_DB:
        if suite.count(name) != 1:
            print(f"!! 记账的标题在套件里找不到（改过名？账要跟着改）: {name}")
            return 1
    print(f"  静态体检通过：{len(RUNS)} 支注入、{len(COVERED)} 条已覆盖、"
          f"{len(NOT_COVERED_UI) + len(NOT_COVERED_DB)} 条按未覆盖记账")
    return 0


def port_busy() -> bool:
    for host in ("127.0.0.1", "::1"):
        family = socket.AF_INET if ":" not in host else socket.AF_INET6
        with socket.socket(family, socket.SOCK_STREAM) as sock:
            sock.settimeout(0.4)
            if sock.connect_ex((host, PORT)) == 0:
                return True
    return False


def e2e_already_running() -> list[str]:
    """另一轮 browser-e2e 正在跑的时候绝对不能开火：它不持锁（只是读产物），
    而我这一支会把 .tsx 改成带缺陷的样子 —— 它的"全绿"就会变成我的注入的读数。"""
    proc = subprocess.run(["pgrep", "-fl", "browser-e2e.mjs"], capture_output=True, text=True)
    return [line for line in proc.stdout.splitlines() if line.strip()]


def build(label: str) -> None:
    proc = subprocess.run(["npm", "run", "build"], cwd=UI, capture_output=True, text=True,
                          timeout=BUILD_TIMEOUT)
    (LOG_DIR / f"{label}_build.log").write_text(proc.stdout + proc.stderr, encoding="utf-8")
    if proc.returncode != 0:
        raise SystemExit(f"build 失败（{label}），见 {LOG_DIR / (label + '_build.log')}")
    print(f"    built: {bundle_name()}")


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)], cwd=UI,
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    for _ in range(480):
        if port_busy():
            return proc
        time.sleep(0.5)
    stop_preview(proc)
    raise SystemExit(f"preview 起不来（:{PORT} 240s 内没在听）")


def stop_preview(proc: subprocess.Popen) -> None:
    try:
        os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
        proc.wait(timeout=20)
    except (ProcessLookupError, subprocess.TimeoutExpired):
        try:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
        except ProcessLookupError:
            pass


def run_round(label: str) -> tuple[list[str], int]:
    """返回 (红名单, 本轮检查总数)。总数用来对分母 —— 见文档最后一段。"""
    try:
        proc = subprocess.run(["node", str(E2E)], cwd=UI, capture_output=True, text=True,
                              timeout=ROUND_TIMEOUT, env={**os.environ, "E2E_REPEATS": "1"})
    except subprocess.TimeoutExpired:
        raise SystemExit(f"{label}: browser-e2e 一轮超过 {ROUND_TIMEOUT}s 没收线 → "
                         "这一轮没有结论（不许当成通过），先等机器空闲再重跑")
    text = proc.stdout + proc.stderr
    (LOG_DIR / f"{label}.log").write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {LOG_DIR / (label + '.log')}")
        return [f"<build-guard:{label}>"], 0
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论")
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    summary = re.search(r"=> PASS (\d+) / FAIL (\d+)", text)
    total = (int(summary.group(1)) + int(summary.group(2))) if summary else 0
    print(f"  {label}: PASS+FAIL {total} / 红 {len(failed)}")
    return failed, total


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if T_SECTION_CRASH in failed:
        print(f"  !! {label}: 整节中途抛错（{T_SECTION_CRASH} 红了）—— 这一轮的归因不成立，"
              "后面那些「没红」是「没跑到」")
        return 1
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
    if (code := validate()):
        return code
    busy = e2e_already_running()
    if busy:
        print("!! 已经有一轮 browser-e2e 在跑，它不持锁而我会改 .tsx —— 那轮的读数会变成我的注入：")
        for line in busy:
            print(f"   {line}")
        return 2
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              f"\n   先确认那是谁（`lsof -nP -iTCP:{PORT} -sTCP:LISTEN`），是自己这一支的量具再停。")
        return 2

    original = DESIGNER.read_text(encoding="utf-8")
    bad = 0
    preview = None
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===")
        build("00_baseline")
        preview = start_preview()
        failed, total = run_round("00_baseline")
        if failed:
            print(f"  !! 基线就有红，注入结果无法归因；先修基线: {failed}")
            return 2
        if not total:
            print("  !! 基线一轮数不出分母（套件没跑完？）")
            return 2
        print(f"  基线分母钉住: {total} 条检查")

        for i, (tag, anchor, repl, expected) in enumerate(RUNS, start=1):
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} ===")
            DESIGNER.write_text(original.replace(anchor, repl, 1), encoding="utf-8")
            try:
                build(label)
                failed, this_total = run_round(label)
                if total and this_total != total:
                    print(f"  !! {tag}: 本轮分母 {this_total} != 基线 {total} —— "
                          "某一节中途没跑完，「没红」不能算通过")
                    bad += 1
                bad += judge(tag, expected, failed)
            finally:
                DESIGNER.write_text(original, encoding="utf-8")
                if DESIGNER.read_text(encoding="utf-8") != original:
                    print(f"  !! DesignerPage.tsx 未恢复到原始内容")
                    bad += 1

        print("\n=== 恢复后复跑 ===")
        build("99_restored")
        failed, this_total = run_round("99_restored")
        if failed or this_total != total:
            print(f"  !! 恢复后仍有红或分母不符: {failed} ({this_total} vs {total})")
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    if bad:
        print(f"\nRESULT: {bad} problem(s)")
        return 1
    print(f"\nRESULT: 11d 的 {len(COVERED)} 条各自钉住一件事；"
          f"{len(NOT_COVERED_UI) + len(NOT_COVERED_DB)} 条按未覆盖记账（理由在文档里）")
    return 0


if __name__ == "__main__":
    _mutlock.acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
