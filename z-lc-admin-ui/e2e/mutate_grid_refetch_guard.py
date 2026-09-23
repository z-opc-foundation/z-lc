#!/usr/bin/env python3
"""同轮注入缺陷自证：`GridView.load` 的"一次挂载只查一次"（#26）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_grid_refetch_guard.py

这一支补的不是"说哪句话"，而是"页面自己在那儿转圈重查"那一类（缺陷 #27 由日历带出来）：
全仓库只剩 `GridView.load` 还是手写的 `useCallback(...) + useEffect(...)` 取数 —— 看板、图表、
仪表盘、页脚统计、引用字段候选都走 react-query，queryKey 按**内容** hash，数组每轮渲染换身份
并不会多打一次请求，给它们补计数用例是在测 react-query 而不是测我们。
而 `load` 的依赖里正好躺着两个"每轮渲染可能换新身份"的东西：`state.sorts` 与父组件传进来的
`conditions`。写错一次就是这个文件里最贵的一类静默回归：每次拿回同一份数据，
`GridView.test.tsx` 原有那 8 条断言（表头、字典标签、请求体形状、内联编辑提交内容）**一条都不会红**。

两个注入都是最常见的两种写错方式（不是稻草人）：把自己想"响应变化"的那个数组在依赖里摊开一次。

⚠ 为什么这一支只跑 `GridView.test.tsx` 一个文件，而记录侧那 12 个注入跑全量：
第一次尝试按全量跑，V1 挂上去之后 vitest **13 分钟没跑完**（我 kill 掉，手工把 GridView.tsx
恢复成原始字节并核对了 md5，另外杀了残留的 worker）。同一个注入按文件跑，几秒就红给你看：
`AssertionError: 挂载之后不该有第二次查询: expected 3 to be 1`。原因是重查循环会喂不饱
别的应用（antd 的 rc-* 计时器 + 每个文件自己的 effect），全量跑把这种饿死放大成"没有结论"。
**跑不完一整轮不等于通过，等于没测** —— 所以判据的分母从"全仓库 171 条"换成"这个文件的 9 条"，
并把这一条写进 `_e2e/README.md`：churn 类注入必须按文件跑。

连带红：一个都不许。理由是这个文件特有的 —— `StateBlock` 的 `isLoading={loading && rows.length === 0}`
加上 `load` 里的 `requestId` token 守卫，让循环期间**已经画出来的行一直留在 DOM 里**（新结果覆盖旧结果、
失败的迟到结果被丢弃），所以那 8 条 UI/请求体断言没有一支会因为重查而换分支。
这和记录侧那两处（日历 42 格空格、画廊空态）不同，那边我只能允许连带红。
为了让这句话可证伪，每个注入跑两遍；哪天冒出连带红，就必须拿样本证明它换了哪一支，
而不是往白名单里加一条了事。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
GRID = UI / "src/views/workspace/GridView.tsx"
SUITE = "src/views/workspace/GridView.test.tsx"

# 按文件跑（理由见 docstring）。基线分母 = 这个文件自己的 9 条。
BASELINE_TOTAL = 9
# 每个注入重复几遍：一遍只能证明"能红"，两遍才勉强支撑"红的恰好是这一条"。
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_grid_logs"
LOG_DIR.mkdir(exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> tuple[list[str], int, list[str]]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", SUITE, "--reporter=json", f"--outputFile={out}"],
            cwd=UI,
            capture_output=True,
            text=True,
            # 按文件跑本该几秒结束；给 600s 已经宽松。超时必须当成"无结论"而不是通过。
            timeout=600,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(
            stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
            encoding="utf-8",
        )
        print(f"    vitest 超过 600s 未结束（视为该注入让测试挂住 → 无结论）；输出留档: {keep}")
        return [f"<timeout:{slug(label)}>"], -1, []
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed, titles = [], []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            title = case.get("title") or case.get("fullName") or "?"
            titles.append(title)
            if case.get("status") == "failed":
                failed.append(title)
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        keep_json = LOG_DIR / f"{slug(label)}.vitest.json"
        keep_json.write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep} / {keep_json}")
    return failed, total, titles


T_LOOP = "画出行之后安静期不再自己重查"

# 这个文件该有哪 9 条，钉死在这里：谁改了名或删了一条，注入结果就不能再被相信 ——
# 否则"没有连带红"可能只是因为那 8 条邻居已经不在了。
COLLECTED = [
    "列由 schema 生成，标题用字段的中文名",
    "字典列直接显示后端 JOIN 出的标签，不显示原始码",
    "列表请求带得上 entityCode/appCode/tenantCode（后端少了 appCode 会直接 400）",
    "筛选条件与 conjunction 会原样进请求体，且变化后立刻重新拉数",
    "页脚统计按持久化的配置发聚合请求，并把服务端结果渲染出来",
    '统计口径写的是"命中多少行"，不是"这一页有几行"',
    "每个统计值落在它自己那一列下面，不能整体错位",
    "内联编辑只提交被改的那一列，并把 id 放进 fieldValues",
    T_LOOP,
]


def judge(name: str, failed: list[str], total: int, titles: list[str], expected: list[str]) -> int:
    missing_tests = [t for t in COLLECTED if t not in titles]
    extra_tests = [t for t in titles if t not in COLLECTED]
    if total != BASELINE_TOTAL or missing_tests or extra_tests:
        print(
            f"  !! {name}: 收集到的用例和钉子不符（分母 {total} != {BASELINE_TOTAL}"
            f"，缺 {missing_tests}，多 {extra_tests}），这一轮结论作废"
        )
        return 1
    hard = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for title in failed:
        print(f"    RED ({'预期' if any(e in title for e in expected) else '未预期'}) {title}")
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无一条连带红（共 {len(failed)} 条红）")
        return 0
    # 两件事一起报：只报 missing 会把 extra 藏起来（记录侧那轮差点因此记错结论）。
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(
            f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
            "先拿样本证明重查把哪一支断言换掉了（本文件的既有解释是 rows 不会消失）"
        )
    return 1


# (label, [(file, anchor, mutant)], 预期必须变红的用例)
DEPS = "  }, [entityCode, appCode, tenantCode, page, pageSize, conditions, conjunction, state.sorts]);"
RUNS = [
    (
        "V1 把 state.sorts 在依赖里摊开（每轮渲染新身份 → load 换 → useEffect 自触发）",
        [(GRID, DEPS, DEPS.replace("state.sorts", "[...state.sorts]"))],
        [T_LOOP],
    ),
    (
        "V2 把父组件传进来的 conditions 摊开（同一类写错，与记录侧 G5 一模一样的形状）",
        [(GRID, DEPS, DEPS.replace("conditions,", "[...conditions],"))],
        [T_LOOP],
    ),
]


def main() -> int:
    ORIG[GRID] = GRID.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（修复态）===")
        failed, total, titles = run_vitest("00_baseline")
        if failed or total != BASELINE_TOTAL or set(titles) != set(COLLECTED):
            print("  !! 基线就有红、分母不对或收集到的用例和钉子不符，注入结果无法归因；先修基线")
            return 2

        for run in RUNS:
            if len(run) != 3:
                raise SystemExit(f"RUNS 条目形状不对（要 3 元）: {run[0]}")
            label, edits, expected = run
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                for rep in range(1, REPEATS + 1):
                    print(f"\n=== {label} (第 {rep}/{REPEATS} 遍) ===")
                    failed, total, titles = run_vitest(f"{label}_r{rep}")
                    bad += judge(label, failed, total, titles, expected)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        failed, total, titles = run_vitest("99_restored")
        if failed or total != BASELINE_TOTAL or set(titles) != set(COLLECTED):
            print("  !! 恢复后仍有红、分母不对或用例集合不符")
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
