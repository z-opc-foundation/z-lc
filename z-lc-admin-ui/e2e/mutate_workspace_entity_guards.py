#!/usr/bin/env python3
"""同轮注入缺陷自证：workspace 侧「实体没读到」的四处门禁（#23）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_workspace_entity_guards.py

这批和 #22 是同一类谎话的另外三个出口：侧边栏、应用概览的统计位、实体候选下拉。
新测试是我自己写的，所以每条都要能因为**改坏守卫**而红 —— 否则就是跟着代码一起变绿的装饰。

每个注入都取"修复前的真实代码形状"：
A1/A2 就是本轮改动前的 `entities.length === 0` 一支和"计数永远是数字"；
B2 是改动前那个无条件空态；C1 是 `useEntityOptions` 改动前"entities 里有就信"的形状；
D1 是 AiModelingPage 改动前那两行写死的 `message.warning('该应用还没有实体')`。
不是为测试编的稻草人。

每轮只改一个守卫，跑全量 vitest，要求红的用例**恰好**是预期那几条：
多红 = 守卫被别处依赖（要写进文档），少红 = 那条测试是空的。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
SRC = UI / "src"
LAYOUT = SRC / "layouts/WorkspaceLayout.tsx"
OVER = SRC / "views/apps/AppOverviewPage.tsx"
SCOPE = SRC / "views/admin/_scope.ts"
AI = SRC / "views/admin/AiModelingPage.tsx"

# 留档放在系统临时目录而不是仓库里：一份失败输出几百 KB，不该混进交付物。
LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_ws_ent_logs"
LOG_DIR.mkdir(exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"

ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor}")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> list[str]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
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
    # 只要见到红就把原始输出留住。上一轮有条**不在预期里**的红（ImportDialog 的口径用例）只在
    # 注入轮出现，脚本当时只打印标题，失败样本没拿到 —— 没有样本就只能猜，这是本仓库写过的教训。
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        print(f"    原始输出留档: {keep}")
    return failed


def judge(name: str, failed: list[str], expected: list[str]) -> int:
    missing = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for title in failed:
        print(f"    RED   {title}")
    if not missing and not extra:
        print(f"  OK  {name}: 红了恰好 {len(failed)} 条，与预期一致")
        return 0
    # 两件事要一起报：先报 missing 就把 extra 藏了，上一轮 A1 的"多红"其实是我的测试
    # 在负载下点到了 disabled 按钮 —— 少看一半就会把结论记错。
    if missing:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {missing}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红（守卫被别处依赖？口径要重算）: {extra}")
    return 1


SIDEBAR_FAIL = '实体接口失败时说"实体列表没有读到"，不许说"该应用还没有实体"'
SIDEBAR_NULL = "接口回 success:true 但 data 为 null 时同样算没读到"
SIDEBAR_RETRY = "侧边栏的重试要真的再拉一次 schema"
OVER_DASH = '实体没读到时报"没有读到"，统计位是破折号'
HOOK_READ = "元数据降级时 read 是 false，reason 点名是哪个接口"
HOOK_HELPER = '降级时 helper 给的是"没有读到"这句话，不是「暂无数据」'
AI_TOAST = 'AI 建模点"数据分析"时，元数据降级要说没读到而不是说没实体'

# (label, [(file, anchor, mutant)], expected red test-title substrings)
RUNS = [
    (
        "A1 拆掉侧边栏的 read 门禁（= 改动前只分「空数组就说没有实体」）",
        [(LAYOUT, "            ) : !entitiesRead ? (", "            ) : false ? (")],
        [SIDEBAR_FAIL, SIDEBAR_NULL, SIDEBAR_RETRY],
    ),
    (
        "A2 计数位不再让位（没读到也写「实体 (0)」）",
        [
            (
                LAYOUT,
                "实体 ({entitiesRead ? entities.length : '—'})",
                "实体 ({entities.length})",
            )
        ],
        [SIDEBAR_FAIL],
    ),
    (
        "A3 横幅里的重试变成空操作（看着能修，点了什么也不发）",
        [(LAYOUT, "onClick={() => void refetch()}", "onClick={() => undefined}")],
        [SIDEBAR_RETRY],
    ),
    (
        "B1 统计位无条件报数字（「没读到」被报成 0）",
        [
            (
                OVER,
                "  const count = (key: 'entities' | 'dicts' | 'views', value: number) => (read?.[key] ? value : '—');",
                "  const count = (key: 'entities' | 'dicts' | 'views', value: number) => value;",
            )
        ],
        [OVER_DASH],
    ),
    (
        "B2 概览空态无条件说「还没有实体」（改动前原样）",
        [(OVER, "emptyText: read?.entities ? (", "emptyText: true ? (")],
        [OVER_DASH],
    ),
    (
        "C1 useEntityOptions 无条件认为实体读到了（改动前\"entities 里有就信\"的形状）",
        [(SCOPE, "    read: Boolean(meta?.read.entities),", "    read: true,")],
        [HOOK_READ, HOOK_HELPER, AI_TOAST],
    ),
    (
        "C2 helper 永远交回 antd 默认空态（下拉里又变成「暂无数据」）",
        [
            (
                SCOPE,
                "  return source.read ? undefined : `实体列表没有读到：${source.reason}`;",
                "  return undefined;",
            )
        ],
        [HOOK_HELPER],
    ),
    (
        "D1 AI 建模的提示写死「该应用还没有实体」（改动前那两行的形状）",
        [
            (
                AI,
                "    entitiesSource.read ? fallback : `实体列表没有读到：${entitiesSource.reason}`;",
                "    fallback;",
            )
        ],
        [AI_TOAST],
    ),
]


def main() -> int:
    for p in (LAYOUT, OVER, SCOPE, AI):
        ORIG[p] = p.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（全部修复态）===")
        if run_vitest("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for label, edits, expected in RUNS:
            print(f"\n=== {label} ===")
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                bad += judge(label, run_vitest(label), expected)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        if run_vitest("99_restored"):
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
