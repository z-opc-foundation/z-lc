#!/usr/bin/env python3
"""同轮注入缺陷自证：列设置抽屉的"顺序契约"（缺陷 #33）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_column_order_guards.py

这一支守的东西和记录侧那几支不同：不是"页面自己转圈重查"，也不是"读失败画成库里真没有"，
而是**同一个抽屉里"看到的顺序"和"写出去的顺序"是两套**。它有三个实测出来的症状
（2026-09-23 同一轮，先真浏览器读数、再落单测）：

  1. 干净的 columnMeta（每个新视图的默认态）里只给"工时"输一个宽度，表头从
     `标题|优先级|工时|截止日期` 变成 `工时|标题|优先级|截止日期` —— 改宽度把整列挪到最前面；
  2. 键盘排序成功之后（表头确实变了），抽屉里那几行纹丝不动，还是 schema 顺序；
  3. 再排第二次，表头和第一次**一模一样** —— 第二次是从 schema 顺序起算的，第一次的结果被整份覆盖。

三个症状背后只有两处代码：`rows` 走没走 `projectColumns`，`patch` 以什么为基底写回。
所以三个注入就是这三种写错法，而且**每个注入预期变红的集合互不相同**（见下表）——
集合相同就说明有一层是重复检查，那一条该删掉而不是留着凑数。

| 注入 | 写回的是什么 | 预期红 |
|---|---|---|
| I1 `rows` 退回 `resolvedFields`（症状 2、3 的原样） | 抽屉按 schema 排行 | 顺序契约 + 隐藏那一整份 |
| I2 `patch` 退回"缺就追加到尾巴"（症状 1 的原样） | 追加=插到表格最前 | 只有宽度那条 |
| I3 `patch` 整份重写但丢掉邻居的配置 | 宽度/显隐被抹平 | 只有隐藏那条 |

I1 与 I3 都会红"隐藏"那一条，区别在 I1 还红"顺序"那一条：隐藏那条钉的是**整份数组逐个比**
（顺序 + 槽位 + 邻居的 width/hidden），顺序那条只比列出的顺序。两个注入落在同一处的不同半边。

⚠ 为什么这一支只跑 `ColumnManagerDrawer.test.tsx` 一个文件：沿用记录侧那轮的结论
（churn 类注入按文件跑，见 `mutate_grid_refetch_guard.py`），而且这个文件本来就只该有 3 条。
分母钉成标题清单，不是"跑绿就行"。

⚠ 拖拽本身（含界面上那句「空格 + 方向键可键盘排序」）在这一层**注入不出来**：
jsdom 没实现 `Element.prototype.scrollIntoView`，dnd-kit 的 KeyboardSensor 一按 Space 就抛，
PointerSensor 又依赖 `document.elementFromPoint`。所以"真能排序""两次会叠加"归浏览器门禁
（`browser-e2e.mjs` §4b 的 6 条），这里按"未覆盖"记账，不在单测里声称验过拖拽。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
DRAWER = UI / "src/views/grid/ColumnManagerDrawer.tsx"
SUITE = "src/views/grid/ColumnManagerDrawer.test.tsx"

# 按文件跑：分母 = 这个文件自己的 3 条。
BASELINE_TOTAL = 3
# 每个注入重复几遍：一遍只能证明"能红"，两遍才勉强支撑"红的恰好是这几条"。
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_col_logs"
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
            # 一个文件本该几秒结束；给 600s 已经宽松。超时必须当成"无结论"而不是通过。
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
    # 绿的也要留档：只留红的日志＝挑选过的证据。
    keep = LOG_DIR / f"{slug(label)}_{'red' if failed else 'green'}.log"
    keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
    (LOG_DIR / f"{slug(label)}.vitest.json").write_bytes(out.read_bytes())
    if failed:
        print(f"    原始输出留档: {keep}")
    return failed, total, titles


T_ORDER = "抽屉列出的顺序就是表格将要用的顺序，不是 schema 顺序"
T_WIDTH = "给一个还没持久化的列改宽度，不许把该列挪到表格最前面"
T_HIDE = "隐藏一列：写回的必须是\"当前顺序 + 这一项加 hidden\"，邻居的宽度要原样带着"

# 这个文件该有哪 3 条，钉死在这里：谁改了名或删了一条，注入结果就不能再被相信 ——
# 否则"没有连带红"可能只是因为邻居已经不在了。
COLLECTED = [T_ORDER, T_WIDTH, T_HIDE]


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
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，先拿样本证明它换了哪一支")
    return 1


# (label, [(file, anchor, mutant)], 预期必须变红的用例)
ROWS_ANCHOR = """    const projected = projectColumns(resolvedFields, columnMeta);
    return projected.map(({ resolved, meta }) => {"""
ROWS_SCHEMA = """    const byCode = new Map(columnMeta.map((item) => [item.fieldCode, item]));
    const projected = resolvedFields.map((resolved) => ({
      resolved,
      meta: byCode.get(resolved.ctx.field.fieldCode) ?? { fieldCode: resolved.ctx.field.fieldCode },
    }));
    return projected.map(({ resolved, meta }) => {"""

PATCH_ANCHOR = "    onChange(rows.map((row) => (row.fieldCode === fieldCode ? { ...row.meta, ...next } : row.meta)));"
PATCH_APPEND = """    onChange(columnMeta.some((item) => item.fieldCode === fieldCode)
      ? columnMeta.map((item) => (item.fieldCode === fieldCode ? { ...item, ...next } : item))
      : [...columnMeta, { fieldCode, ...next }]);"""
PATCH_BLANK = "    onChange(rows.map((row) => (row.fieldCode === fieldCode ? { fieldCode, ...next } : { fieldCode: row.fieldCode })));";

RUNS = [
    (
        "I1 rows 退回 resolvedFields（症状 2、3 的原样写法）",
        [(DRAWER, ROWS_ANCHOR, ROWS_SCHEMA)],
        [T_ORDER, T_HIDE],
    ),
    (
        "I2 patch 退回\"缺就追加到尾巴\"（症状 1 的原样写法）",
        [(DRAWER, PATCH_ANCHOR, PATCH_APPEND)],
        [T_WIDTH],
    ),
    (
        "I3 patch 整份重写但抹掉邻居的配置",
        [(DRAWER, PATCH_ANCHOR, PATCH_BLANK)],
        [T_HIDE],
    ),
]


def main() -> int:
    ORIG[DRAWER] = DRAWER.read_text(encoding="utf-8")

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
