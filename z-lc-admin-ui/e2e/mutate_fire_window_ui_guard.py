#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #65 的**界面层**注入自证：`/fires` 那一页的读法与说法（vitest 层）。

跑法（在 z-lc-admin-ui 下；不需要后端，桩在测试里）：

    python3 e2e/mutate_fire_window_ui_guard.py           # 全量（基线 + 8 支 + 恢复轮）
    python3 e2e/mutate_fire_window_ui_guard.py U1 U2     # 只跑这几支，并打印实测红集（抄回 EXPECT）

#65 的形状是"接口一次最多回 200 条而不回总数"，界面把 200 条当全部、再客户端切 20 行。
修完之后这一页有四句话各自有各自的出处，每句都可能被写回成"看起来一样"的谎：
  · 「一共有 N 条」只能来自服务器那一格 total；
  · 「另外 N 条没读在这一页里」= 服务器 total − 这一页行数；
  · 「这一页没有行，但一共有 N 条」与「还没有发起记录」是两种结局，各看 total；
  · 点第 2 页必须真的换一次请求（带页码）。
读的那一层（`readFireWindow`）也在这支里：它决定"形状漂移"是走"没有读到"还是被演成"没有账"。

八支注入（名字与预期红集抄自实测；两支红集相同 = 判据分不开，见 PAIRS）：
  U1 `readFireWindow` 把裸数组也收下（#65 原样）
  U2 total 缺席时补一个 `records.length`（比裸数组更难发现：信封在、形状对，只有总数是编的）
  U3 「一共有 N 条」画成这一页的行数
  U4 摘掉「另外 N 条没有读在这一页里」那一句
  U5 空态不看 total 而看这一页有没有行 ⇒ 空页被说成"还没有发起记录"
  U6 分页控件 onChange 不 setPage（点了第 2 页什么也没发生）
  U7 effect deps 去掉 page（换了页码不重读）
  U8 把"读不出形状"的异常吞成 done ⇒ 演成"没有账"

量具规则（与 java 那支 `_e2e/mutate_fire_window_guard.py` 同一套）：
  * 注入前把两个文件字节读进本次运行独占的备份，还原只从这份副本写回 + 逐字节对账；
    **不许**用 `git checkout --` 当还原步。
  * 每个 anchor 必须在文件里**恰好出现一次**，否则当场 SKIPPED 记坏账（不猜）。
  * 分母钉死：基线必须是这 8 条用例且全绿；每支注入后用例数仍为 8（少一条=编译没过=假绿）。
  * 一支注入在它该红的地方一条都不红 ⇒ 记坏账（等价变异或被别处掩盖，都要写清楚）。
  * 收尾"还原字节 → 再跑一遍基线"回绿，并把还原结果写进台账。
"""
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(UI / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

API = UI / "src/api/workflowBinding.ts"
PAGE = UI / "src/views/admin/WorkflowsPage.tsx"
FILES = sorted({API, PAGE}, key=lambda p: str(p))
SUITES = ["src/views/admin/WorkflowsPage.test.tsx"]

LOGS = Path.home() / ".cache/zlc65ui"
BAK = LOGS / "bak"
LEDGER = LOGS / "ledger.json"
VITEST_TIMEOUT = 600

# ---- anchors ---------------------------------------------------------------------------
U1_A = "  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {"
U1_R = "  if (!raw || typeof raw !== 'object') {"

U2_A = (
    "  if (typeof value.total !== 'number' || !Number.isFinite(value.total) || value.total < 0) {\n"
    "    throw new Error(\n"
    "      `/fires 没有如实回总数（total=${String(value.total)}）—— 少了这一格，一页读不满就等于\"没有账\"，` +\n"
    "        '那正是缺陷 #65',\n"
    "    );\n"
    "  }\n"
)
U2_R = (
    "  const total =\n"
    "    typeof value.total === 'number' && Number.isFinite(value.total) && value.total >= 0\n"
    "      ? value.total\n"
    "      : value.records.length;\n"
)
U2B_A = "    total: value.total,"
U2B_R = "    total,"

U3_A = "              这个实体一共有 <Text strong>{ledger.total}</Text> 条发起记录，这里按 id 倒序读第{' '}\n"
U3_R = "              这个实体一共有 <Text strong>{rows.length}</Text> 条发起记录，这里按 id 倒序读第{' '}\n"

U4_A = "          {hiddenRows > 0 ? ("
U4_R = "          {hiddenRows < 0 ? ("

U5_A = "                ledger && ledger.total > 0 ? ("
U5_R = "                ledger && rows.length > 0 ? ("

U6_A = "              onChange: (next) => setPage(next),"
U6_R = "              onChange: () => undefined,"

U7_A = "  }, [binding, page]);"
U7_R = "  }, [binding]);"

U8_A = "        setLedger(null);\n        setStatus('error');"
U8_R = "        setLedger(null);\n        setStatus('done');"

EDITS = {
    "U1": [(API, U1_A, U1_R)],
    "U2": [(API, U2_A, U2_R), (API, U2B_A, U2B_R)],
    "U3": [(PAGE, U3_A, U3_R)],
    "U4": [(PAGE, U4_A, U4_R)],
    "U5": [(PAGE, U5_A, U5_R)],
    "U6": [(PAGE, U6_A, U6_R)],
    "U7": [(PAGE, U7_A, U7_R)],
    "U8": [(PAGE, U8_A, U8_R)],
}
# 允许"红集逐字相同"的成对注入 —— 这两支打的是同一条性质的两个代码站点，
# 界面上没有任何可观察差别（点了第 2 页：既不多发请求，也不换行、也不换页码）。
# 逐字相同就记成**一条**守卫，不许按两支记分（这是账面口径，不是"覆盖了两处"）。
PAIRS = [{"U6", "U7"}]

# 预期红集：抄自实测（transcript `~/.cache/zlc65ui/guard-0927-104801.log`，
# 由 `--only U1..U8` 那一轮打印的 EXPECT_* 机械提取，不是推的）。
EXPECT = {
    "U1": ['接口退回 #65 之前那个裸数组 ⇒ 判"没有读到"，不许把行数当总数'],
    "U2": ['total 那一格缺席 ⇒ 判"没有如实回总数"，不许拿这一页的行数补一个总数'],
    "U3": [
        '发起记录的第一页：「一共有多少」与「这一页读出来几条」是两句话（缺陷 #65）',
        '空页 ≠ 没有账；真的没有账才配说「还没有发起记录」',
        '翻页要真的换一次请求：点第 2 页得带着页码重读，而不是把第 1 页再画一遍（缺陷 #65）',
    ],
    "U4": ['发起记录的第一页：「一共有多少」与「这一页读出来几条」是两句话（缺陷 #65）'],
    "U5": ['空页 ≠ 没有账；真的没有账才配说「还没有发起记录」'],
    "U6": ['翻页要真的换一次请求：点第 2 页得带着页码重读，而不是把第 1 页再画一遍（缺陷 #65）'],
    "U7": ['翻页要真的换一次请求：点第 2 页得带着页码重读，而不是把第 1 页再画一遍（缺陷 #65）'],
    "U8": [
        'total 那一格缺席 ⇒ 判"没有如实回总数"，不许拿这一页的行数补一个总数',
        '发起记录：绑定存在不等于流程发出去了，这一页要能翻到那张账',
        '接口退回 #65 之前那个裸数组 ⇒ 判"没有读到"，不许把行数当总数',
    ],
}

ONLY = {a for a in sys.argv[1:] if a.startswith("U")}
UNKNOWN = [a for a in sys.argv[1:] if not a.startswith("U")]
if UNKNOWN:
    raise SystemExit(f"只认 U1..U8，收到 {UNKNOWN}")


def snapshot_sources():
    run_dir = BAK / f"run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=True)
    originals = {}
    for f in FILES:
        if not f.exists():
            raise RuntimeError(f"源文件不在，连基线都量不了: {f}")
        disk = f.read_bytes()
        (run_dir / (str(f).replace("/", "_").lstrip("_") + ".orig")).write_bytes(disk)
        originals[f] = disk
    print(f"sources snapshotted -> {run_dir}")
    return originals


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


def run_vitest(label: str) -> tuple[list[str], int]:
    """跑这一页的用例，返回 (具名红集, 用例总数)。**跑不满分母就当坏**：编译不过时
    vitest 报的是 0 条用例，那一轮"没有红"完全不能信。"""
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    t0 = time.time()
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", *SUITES, "--reporter=json", f"--outputFile={out}"],
            cwd=str(UI), capture_output=True, text=True, timeout=VITEST_TIMEOUT,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOGS / f"{slug(label)}_timeout.log"
        keep.write_text((exc.stdout or "") if isinstance(exc.stdout, str) else
                        (exc.stdout or b"").decode("utf-8", "replace"), encoding="utf-8")
        raise RuntimeError(f"{label}: vitest 超过 {VITEST_TIMEOUT}s 没结束（多半是被注入挂住了某条用例），"
                           f"留档 {keep}") from exc
    raw = proc.stdout + proc.stderr
    if not out.exists():
        (LOGS / f"{slug(label)}.log").write_text(raw, encoding="utf-8")
        raise RuntimeError(f"{label}: vitest 没写出报告 (exit={proc.returncode})，见 {LOGS / f'{slug(label)}.log'}")
    report = json.loads(out.read_text(encoding="utf-8"))
    results = report.get("testResults") or []
    if not results:
        (LOGS / f"{slug(label)}.log").write_text(raw, encoding="utf-8")
        raise RuntimeError(f"{label}: 报告里零条用例 —— 编译没过或文件没匹配上，这一轮不能算"
                           f"「没红」，见 {LOGS / f'{slug(label)}.log'}")
    failed, titles = [], []
    for file_result in results:
        for case in file_result.get("assertionResults") or []:
            title = case.get("title") or "?"
            titles.append(title)
            if case.get("status") == "failed":
                failed.append(title)
    total = int(report.get("numTotalTests", len(titles)))
    (LOGS / f"{slug(label)}.log").write_text(raw + "\n=== titles ===\n" + "\n".join(titles), encoding="utf-8")
    return sorted(set(failed)), total


def apply(tag):
    applied = []
    for path, anchor, repl in EDITS[tag]:
        text = path.read_text(encoding="utf-8")
        if text.count(anchor) != 1:
            print(f"[SKIPPED] {tag}: anchor 在 {path.name} 里出现 {text.count(anchor)} 次")
            return None
        if anchor == repl:
            print(f"[SKIPPED] {tag}: 注入是空操作")
            return None
        path.write_text(text.replace(anchor, repl, 1))
        applied.append(path.name)
    return applied


def main():
    originals = snapshot_sources()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    restore_all()
    bad = 0
    crashed = []
    tags = [t for t in EDITS if not ONLY or t in ONLY]
    book = {"ran_by": f"{os.environ.get('USER','?')}@{time.strftime('%m%d-%H%M%S')}",
            "script": os.path.basename(__file__), "runs": []}
    try:
        LOGS.mkdir(parents=True, exist_ok=True)
        base_reds, base_total = run_vitest("baseline")
        print(f"\n=== baseline: 用例 {base_total} 条，红 {len(base_reds)} 条")
        if base_reds:
            print(f"BASELINE 不绿，中止。reds={base_reds}")
            return 1
        denoms = {"tests": base_total}
        book["denominators"] = denoms
        seen = {}

        for tag in tags:
            rec = {"tag": tag}
            applied = apply(tag)
            if applied is None:
                restore_all()
                bad += 1
                continue
            print(f"\n[{tag}] 注入 {applied}")
            reds, total = run_vitest(f"mut{tag}")
            rec["tests"] = total
            rec["reds"] = reds
            print(f"  用例 {total} 条，具名红 {len(reds)} 条:")
            for r_ in reds:
                print(f"     - {r_}")
            if total != base_total:
                print(f"  !! 分母漂了：{base_total} → {total}"
                      "（用例少一条就是编译没过，那「没红」是假的）")
                bad += 1
            if not reds:
                print("  !! 一条都不红：这一族的性质在 vitest 层没有网（等价变异或被别处掩盖）")
                bad += 1
            key = tuple(reds)
            if key in seen:
                pair = {seen[key], tag} in PAIRS
                print(f"  !! 与 {seen[key]} 红集逐字相同 —— {'已声明的成对注入，按一条守卫记账' if pair else '判据分不开两种修法'}")
                if not pair:
                    bad += 1
            else:
                seen[key] = tag
            if EXPECT[tag] and sorted(reds) != sorted(EXPECT[tag]):
                print(f"  !! MISMATCH: 预期 {len(EXPECT[tag])} 实跑 {len(reds)}")
                print(f"     只在预期里: {sorted(set(EXPECT[tag]) - set(reds))}")
                print(f"     只在实跑里: {sorted(set(reds) - set(EXPECT[tag]))}")
                bad += 1
            if ONLY:
                print(f"  EXPECT_{tag} = {json.dumps(reds, ensure_ascii=False, indent=1)}")
            book["runs"].append(rec)
            restore_all()

        print("\n=== restore: 还原字节 → 再跑一遍基线 ===")
        reds, total = run_vitest("restored")
        print(f"  用例 {total} 条，红={reds or '(none)'}")
        if reds or total != base_total:
            bad += 1
        leaked = []
        for f in FILES:
            disk = f.read_bytes()
            if disk != originals[f]:
                leaked.append(f"{f} md5={hashlib.md5(disk).hexdigest()} != 基线 "
                              f"{hashlib.md5(originals[f]).hexdigest()}")
        print("restored sources: " + ("clean" if not leaked else "NO -> " + "; ".join(leaked)))
        book["restored"] = not leaked
        book["rerun_green"] = (not reds and total == base_total)
        book["bad"] = bad
        bad += len(leaked)
    except BaseException as ex:  # noqa: BLE001
        crashed.append(ex)
        book["crashed"] = repr(ex)
        book["bad"] = bad + 1
        raise
    finally:
        restore_all()
        LOGS.mkdir(parents=True, exist_ok=True)
        LEDGER.write_text(json.dumps(book, ensure_ascii=False, indent=1), encoding="utf-8")
        verdict = ("CRASHED: " + repr(crashed[0])) if crashed else (
            "fire-window UI falsification done" if bad == 0 else f"{bad} problem(s)")
        if ONLY and not crashed:
            verdict += f" (PARTIAL: 只跑了 {','.join(sorted(ONLY))})"
        print(f"台账: {LEDGER}")
        print(f"RESULT: {verdict}")
    return 1 if (bad or crashed) else 0


class Tee:
    def __init__(self, stream, handle):
        self.stream = stream
        self.handle = handle

    def write(self, text):
        self.stream.write(text)
        self.handle.write(text)
        if text.endswith("\n"):
            self.handle.flush()
        return len(text)

    def flush(self):
        self.stream.flush()
        self.handle.flush()


if __name__ == "__main__":
    LOGS.mkdir(parents=True, exist_ok=True)
    transcript = LOGS / ("guard-" + time.strftime("%m%d-%H%M%S") + ".log")
    sys.stdout = Tee(sys.stdout, open(transcript, "w", encoding="utf-8"))
    print(f"transcript -> {transcript}")
    acquire_lock(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        sys.stdout = sys.stdout.stream
        release_lock()
