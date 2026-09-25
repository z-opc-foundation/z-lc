#!/usr/bin/env python3
"""同轮注入缺陷自证：交叉表（PIVOT）的前端那一半。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_pivot_guards.py

交叉表这一轮的代码有一半是"口径"而不是"流程"：合并算子选哪个、空格子是 null 还是 0、
被折掉的行算不算进合计、两个维度撞同一列。这类代码写错了**页面照样画得出来**，
而且画出来的东西看着比报错更像正确答案 —— 所以这一支测的不是"有没有红"，
而是"我声称钉住的那五条口径，是不是真的每条都只钉住自己那一句"。

七个注入是这段代码最可能写错的样子（不是稻草人）：
`agg` 写成 COUNT（进来的行已经是聚合行，COUNT(*) 恒为 1，整张表变成一片 1）、
空单元格显示成 0、忘截断、只把行维度发给后端、给平均数加总、`on` 少写一位下标、
去掉一维两用的守卫。

P8–P10 是本轮新加的口径（第二维为 NULL 时后端给的列键就是空串，18090 实测）：
P8 把空键的翻译摘掉（画出一根没有标题的列）、P9 让行标签和图表各说各话、
P10 把标签**写进键本身**（看着标题对了，格子却全取不到）—— 正是注释里那句"只改显示、不改键"。

判据分母 = 这两个文件自己的 26 条（pivotModel 16 + PivotView 10）。
按文件跑而不是全量：与 mutate_grid_refetch_guard 同一理由 —— 全量跑会把一轮变成十几分钟，
而**跑不完不等于通过，等于没测**。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
MODEL = UI / "src/views/workspace/pivotModel.ts"
VIEW = UI / "src/views/workspace/PivotView.tsx"
SUITES = ["src/views/workspace/pivotModel.test.ts", "src/views/workspace/PivotView.test.tsx"]

BASELINE_TOTAL = 26
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_pivot_logs"
LOG_DIR.mkdir(exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path.name}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> tuple[list[str], int, list[str]]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", *SUITES, "--reporter=json", f"--outputFile={out}"],
            cwd=UI,
            capture_output=True,
            text=True,
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


# 这两个文件该有哪 26 条，钉死在这里：谁改了名或删了一条，注入结果就不能再被相信 ——
# 否则"没有连带红"可能只是因为邻居已经不在了。
COLLECTED = [
    "标量显示列都能当维度，只有数值列能做度量",
    "行维度与列维度撞同一列时被错开，而不是画出一张对角线假表",
    "维度列被删掉后回落到可用列，而不是把未知列发给后端吃 400",
    "列维度没选时保持没选，不替用户猜一列",
    "非 COUNT 指标必须有度量列，缺了取第一个数值列",
    "COUNT 走 group_count 这一列，而不是给聚合行再数一次",
    "合并算子恒为 SUM：进来的每行已经是一个桶，COUNT(*) 会恒为 1",
    "列序自己排定，不跟着后端\"第一次出现\"的顺序漂",
    "行按合计从大到小排，标签只做兜底平序",
    "空格子是 null：不加进合计，也不冒充 0",
    "MIN / MAX 的合计取小取大，不是把格子加起来的 30",
    "平均数不参与横向加总：合计口径缺失时宁可不显示",
    "折叠掉的行仍进总计：可见格 + 其余行 == 列合计 == 总计",
    "分组值为 NULL 的那一档：行和列都要报出名字，但取格子仍用原始空键",
    "交叉表与柱状图对同一个空档用同一个字",
    "程序产出不是矩阵时抛结构错误，不回退成一张空表",
    "只选了行维度时不发请求，并说清还缺什么",
    "选齐两个维度后发一次请求：行维度在前、程序按 group_count 数记录",
    "空格子显示成 ·，不让它冒充 0",
    "行头列显示字典标签而不是裸编码",
    "第二维为空的那一列有标题，且它的数落在自己那一列下面",
    '读失败时显示错误，不把故障说成"没有可透视的记录"',
    '后端结构不合预期时说"结构"问题，不说成空表',
    '折叠掉的行仍在合计里：出现"其余 N 行"，总计不缩水',
    "平均数不显示合计，并说明为什么",
    "切指标到 SUM 时度量列进了请求，页面副标题跟着口径走",
]

T_SUM = "合并算子恒为 SUM：进来的每行已经是一个桶，COUNT(*) 会恒为 1"
T_REQ = "选齐两个维度后发一次请求：行维度在前、程序按 group_count 数记录"
T_DOT = "空格子显示成 ·，不让它冒充 0"
T_REST_MODEL = "折叠掉的行仍进总计：可见格 + 其余行 == 列合计 == 总计"
T_REST_VIEW = '折叠掉的行仍在合计里：出现"其余 N 行"，总计不缩水'
T_AVG_MODEL = "平均数不参与横向加总：合计口径缺失时宁可不显示"
T_AVG_VIEW = "平均数不显示合计，并说明为什么"
T_DIAG = "行维度与列维度撞同一列时被错开，而不是画出一张对角线假表"
T_NULLCOL = "分组值为 NULL 的那一档：行和列都要报出名字，但取格子仍用原始空键"
T_SAME = "交叉表与柱状图对同一个空档用同一个字"
T_HEAD = "第二维为空的那一列有标题，且它的数落在自己那一列下面"

# (label, [(file, anchor, mutant)], 预期必须变红的用例)
RUNS = [
    (
        "P1 pivot 的合并算子写成 COUNT（进来的行已是聚合行 → 整张表变成一片 1）",
        [(MODEL, "      agg: 'SUM',", "      agg: 'COUNT',")],
        [T_SUM, T_REQ],
    ),
    (
        "P2 空格子显示成 0（把'这个组合没有记录'说成'这个组合的记录数是 0'）",
        [(VIEW, "value === null ? '·'", "value === null ? '0'")],
        [T_DOT],
    ),
    (
        "P3 忘了截断：visible 直接用全量（rest 行还在，于是同一批数被加两遍）",
        [(MODEL, "const visible = all.slice(0, config.maxRows);", "const visible = all;")],
        [T_REST_MODEL, T_REST_VIEW],
    ),
    (
        "P4 请求里只带行维度（后端退化成单维分组，交叉表其实没在透视）",
        [(VIEW, "groupFields: [query.rowField ?? '', query.colField ?? ''],",
          "groupFields: [query.rowField ?? ''],")],
        [T_REQ],
    ),
    (
        "P5 给平均数也加总（totalsMeaningful 写死 true → 出一个数学上不存在的合计）",
        [(MODEL, "const totalsMeaningful = config.metricFn !== 'AVG';",
          "const totalsMeaningful = true;")],
        [T_AVG_MODEL, T_AVG_VIEW],
    ),
    (
        "P6 透视的 on 少写一位下标（引用行维度的标签列，表变成对角线）",
        [(MODEL, "      on: 'group_label_2',", "      on: 'group_label',")],
        [T_SUM, T_REQ],
    ),
    (
        "P7 去掉一维两用的守卫（行列同列时画出一张每个格子只有自己的对角线假表）",
        [(MODEL, "  if (colField && colField === rowField) {\n"
                 "    colField = dimensionCodes.find((item) => item !== rowField);\n"
                 "  }",
          "  // 注入：漏了一维两用的守卫")],
        [T_DIAG],
    ),
    (
        "P8 空列键不做翻译（画出一根没有任何标题的列，用户只能猜它属于哪一档）",
        [(MODEL,
          "export const pivotColumnLabel = (rawKey: string): string => (rawKey.trim() === '' ? UNFILLED_GROUP_LABEL : rawKey);",
          "export const pivotColumnLabel = (rawKey: string): string => rawKey;")],
        [T_NULLCOL, T_SAME, T_HEAD],
    ),
    (
        "P9 行标签自说自话（同一个 NULL 档在交叉表叫「(空)」、在柱状图叫「（未填写）」）",
        [(MODEL, "      ? (key === '' ? UNFILLED_GROUP_LABEL : key)",
          "      ? (key === '' ? '(空)' : key)")],
        [T_NULLCOL],
    ),
    (
        "P10 把标签写进列键本身（标题看着对了，格子却按错键去取 → 整列变成空）",
        [(MODEL, "      columns.add(field);", "      columns.add(pivotColumnLabel(field));")],
        [T_NULLCOL, T_HEAD],
    ),
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
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(
            f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
            "先拿样本证明这一处口径换掉了哪一支断言，再改预期"
        )
    return 1


def main() -> int:
    for path in (MODEL, VIEW):
        ORIG[path] = path.read_text(encoding="utf-8")

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
                        print(f"  !! {path.name} 未恢复到原始字节")
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
