#!/usr/bin/env python3
"""同轮注入缺陷自证：元数据降级口径（read / unreadReason / bundledOr / 两处页面门禁）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_degradation_guards.py

为什么必须有这个脚本：#19/#21 这批改动全是"界面不许把加载故障说成被删除"，
而它的新测试是**我自己写的**。没有注入缺陷，就无法区分"这条测试钉住了那处门禁"
和"这条测试只是跟着代码一起变绿"。

每轮的做法：只改一个守卫（其余保持修复态）→ 跑全量 vitest → 要求红的测试
**恰好**是预期的那几条。多红 = 守卫被别处依赖（要写进文档），少红 = 测试是空的。

M2 是这轮最值得看的一条：它注入的是"bundle 里有 key 就信 bundle"，正是修复前的
真实代码形状（不是为测试编的稻草人），结果红在两个测试上而不是零个 —— 说明
`treatEmptyAsMissing` 这个看起来可有可无的参数是承重的。
"""

import json
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
SRC = UI / "src"
META = SRC / "api/meta.ts"
DASH = SRC / "views/workspace/DashboardPage.tsx"
WSVP = SRC / "views/workspace/WorkspaceViewPage.tsx"

ORIG = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor}")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest() -> list[str]:
    out = Path(tempfile.mkdtemp()) / "vitest.json"
    proc = subprocess.run(
        ["npx", "vitest", "run", "--reporter=json", f"--outputFile={out}"],
        cwd=UI,
        capture_output=True,
        text=True,
    )
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    failed = []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            if case.get("status") == "failed":
                failed.append(case.get("title") or case.get("fullName") or "?")
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    return failed


def judge(name: str, failed: list[str], expected: list[str]) -> int:
    missing = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for t in failed:
        print(f"    RED   {t}")
    if missing:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {missing}")
        return 1
    if extra:
        print(f"  !! {name}: 出现了预期之外的红（守卫被别处依赖？口径要重算）: {extra}")
        return 1
    print(f"  OK  {name}: 红了恰好 {len(failed)} 条，与预期一致")
    return 0


# (label, [(file, anchor, mutant)], expected red test-title substrings)
RUNS = [
    (
        "M1 拆掉 DashboardPage 的整页 views 门禁（退回逐块说「视图已被删除」）",
        [(DASH, "if (meta && !meta.read.views) {", "if (false && meta) {")],
        ["views 没读到整页报加载故障"],
    ),
    (
        "M2 bundledOr 不再把 bundle 里的空列表当成缺失（= 修复前的真实代码形状）",
        [(META, "        degraded,\n        true,\n      ),", "        degraded,\n      ),")],
        ["bundle 可用时只补一次 field-types", "bundle 路径里补拉 field-types 失败也要记进 degraded"],
    ),
    (
        "M3 wasRead 只看 degraded，不看接口到底回了个列表（success:true + data:null 被当成读到）",
        [
            (
                META,
                "  return !degraded.includes(DEGRADED_LABEL[resource]) && Array.isArray(value);",
                "  return !degraded.includes(DEGRADED_LABEL[resource]);",
            )
        ],
        [
            "接口回 success:true 但 data:null 时，出参已是数组且不算降级项",
            "data:null 的实体列表同样是",
            "接口返回 success:true 但 data 为 null 时同样算没读到",
        ],
    ),
    (
        "M4 拆掉 WorkspaceViewPage 的 read.entities 门禁（谎话「实体不存在，可能已被删除」回来了）",
        [(WSVP, "if (!meta?.read.entities) {", "if (false) {")],
        [
            "读不到实体列表时报",
            "接口返回 success:true 但 data 为 null 时同样算没读到",
            "重试按钮会重新拉元数据",
        ],
    ),
    (
        "M5 组件占位块无条件按「实体已被删除」说话（不再区分读不到与真删了）",
        [(DASH, "const entityGone = meta?.read.entities;", "const entityGone = true;")],
        ["entities 没读到、views 读到时"],
    ),
]


def main() -> int:
    for p in (META, DASH, WSVP):
        ORIG[p] = p.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（全部修复态）===")
        if run_vitest():
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for label, edits, expected in RUNS:
            print(f"\n=== {label} ===")
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                bad += judge(label, run_vitest(), expected)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        if run_vitest():
            print("  !! 恢复后仍有红")
            bad += 1
    finally:
        for path, text in ORIG.items():
            path.write_text(text, encoding="utf-8")

    print(f"\n{'FAILED: ' + str(bad) if bad else 'ALL MUTANTS BEHAVED AS CLAIMED'}")
    return 1 if bad else 0


if __name__ == "__main__":
    from _mutlock import acquire, release

    acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        release()
