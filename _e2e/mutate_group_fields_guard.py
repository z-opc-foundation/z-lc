#!/usr/bin/env python3
"""同轮注入缺陷自证：`groupFields`（N 维分组）那一半的后端守卫。

跑法（在 z-lc 下）：

    python3 _e2e/mutate_group_fields_guard.py

多维分组是交叉表能成立的前提，而它新引入的不是"功能面"而是**注入面**：
`GROUP BY` 的列名不能走占位符，只能拼字符串，所以每多一个维度就是多一个必须过白名单的
入口。这一支钉的七条，每条都是这段代码最可能写错的一种样子（不是稻草人）：
GROUP BY 只按第一维、multi 阈值 off-by-one、重复维度静默去重、第二维漏过白名单、
timeGroup 与多维的互斥守卫被搬走、行序少了第二维的 tie-break、把空维度当错误。

判据分母 = `DynamicSqlBuilderAggregateTest` 这一个类的 16 条（钉子见 COLLECTED）。
为什么只跑这一个类而不是整个 reactor：一轮 27s，7 个注入 × 2 遍 + 基线 + 恢复 = 16 轮，
全量跑会把这一支变成半小时而不会多证明任何事。**跑不完不等于通过，等于没测。**

⚠ 每轮 mvn 之前都会删掉这个类的 surefire 报告：编译不过的注入不会写新报告，
留着旧报告就会把"上一轮的红"当成本轮的结论 —— 那是假绿里最省事的一种。
报告缺失或零条 testcase 一律 FATAL，不返回空集合。

锁用的是前端那一支同一个 `_mutlock`（按仓库而不是按脚本）：这一支改的是 Java 源文件，
和 vitest 那几支并不撞文件，但撞的是同一个"顺手再开一支"的错。
"""

import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))

BUILDER = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/executor/DynamicSqlBuilder.java"
TEST_CLASS = "com.zifang.z.lc.core.executor.DynamicSqlBuilderAggregateTest"
REPORT = ROOT / "z-lc-core/target/surefire-reports" / f"TEST-{TEST_CLASS}.xml"

BASELINE_TOTAL = 16
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_group_fields_logs"
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


def run_tests(label: str = "run") -> tuple[list[str], int, list[str]]:
    """返回 (变红的用例方法名, 收集到的用例数, 全部用例方法名)。"""
    if REPORT.exists():
        REPORT.unlink()
    started = time.time()
    proc = subprocess.run(
        ["mvn", "-o", "-B", "-pl", "z-lc-core", "-am", f"-Dtest={TEST_CLASS.split('.')[-1]}",
         "-Dsurefire.failIfNoSpecifiedTests=false", "test"],
        cwd=str(ROOT), capture_output=True, text=True, timeout=900,
    )
    out = proc.stdout + proc.stderr
    if not REPORT.exists():
        keep = LOG_DIR / f"{slug(label)}_noreport.log"
        keep.write_text(out, encoding="utf-8")
        compile_err = "COMPILATION ERROR" in out
        print(
            f"    !! mvn 没有产出这一类的报告（exit={proc.returncode}, "
            f"编译失败={compile_err}）—— 没有样本就没有结论，不返回空集合；留档: {keep}"
        )
        return [f"<no-report:{slug(label)}>"], -1, []
    root = ET.parse(REPORT).getroot()
    failed, titles = [], []
    for case in root.iter("testcase"):
        name = (case.get("name") or "?").split("(")[0]
        titles.append(name)
        if case.find("failure") is not None or case.find("error") is not None:
            failed.append(name)
    if not titles:
        print("    !! 报告里有零条 testcase，这一轮什么也没测到（不返回空集合作为通过）")
        return [f"<empty-report:{slug(label)}>"], 0, []
    print(f"    mvn: {len(titles)} tests, {len(failed)} failed ({time.time() - started:.0f}s)")
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(out, encoding="utf-8")
        print(f"    原始输出留档: {keep}")
    return failed, len(titles), titles


# 这个类该有哪 16 条，钉死在这里：谁改了名或删了一条，注入结果就不能再被相信 ——
# 否则"没有连带红"可能只是因为邻居已经不在了。
COLLECTED = [
    "monthBucketEmitsYearAndMonthInSelectGroupByAndOrderBy",
    "dayAndYearBucketsScaleTheParts",
    "timeBucketsDropRowsWithoutADate",
    "timeGroupOnANonDateFieldIsRejected",
    "timeGroupWithoutGroupFieldIsRejected",
    "unknownTimeGroupIsRejected",
    "plainGroupByShapeIsUntouched",
    "timeGroupKeepsDictJoinPlaceholderOrder",
    "twoDimensionsAreBothSelectedAndBothGrouped",
    "singleDimensionKeepsTheUntouchedLabelShape",
    "oneDimensionInGroupFieldsIsByteIdenticalToGroupField",
    "duplicateDimensionFailsInsteadOfSilentlyDeduping",
    "unknownSecondDimensionFailsInsteadOfBeingDropped",
    "secondDimensionCarriesTheSameInjectionGuard",
    "blankSecondDimensionMeansOneDimension",
    "timeGroupRefusesToGuessWhichDimensionToBucket",
]

T_TWO = "twoDimensionsAreBothSelectedAndBothGrouped"
T_DUP = "duplicateDimensionFailsInsteadOfSilentlyDeduping"
T_UNKNOWN = "unknownSecondDimensionFailsInsteadOfBeingDropped"
T_INJECT = "secondDimensionCarriesTheSameInjectionGuard"
T_BLANK = "blankSecondDimensionMeansOneDimension"
T_TIMEGROUP = "timeGroupRefusesToGuessWhichDimensionToBucket"

GROUP_BY_BLOCK = (
    '            sql.append(" GROUP BY");\n'
    "            for (int i = 0; i < groups.size(); i++) {\n"
    '                sql.append(i == 0 ? " " : ", ").append("t.").append(quote(groups.get(i).getFieldCode()));\n'
    "            }"
)
ORDER_2_BLOCK = (
    "            if (groups.size() > 1) {\n"
    '                sql.append(", group_key_2 ASC");\n'
    "            }"
)
TIMEGROUP_GUARD = (
    "        if (resolved.size() > 1 && query.getTimeGroup() != null && !query.getTimeGroup().trim().isEmpty()) {\n"
    '            throw new IllegalArgumentException("timeGroup 只能作用于单个分组字段, 不能与 groupFields ("\n'
    '                    + resolved.size() + " 维) 同时使用");\n'
    "        }"
)
RESOLVE_CALL = "            FieldDefDTO field = resolveGroupField(entity, fieldIndex, code);"
BLANK_SKIP = (
    "            if (code == null || code.trim().isEmpty()) {\n"
    "                // 空位 = 这一维还没选 (透视表的列维度常常先空着), 与 groupField 留空同口径\n"
    "                continue;\n"
    "            }"
)
DUP_THROW = (
    "            for (FieldDefDTO picked : resolved) {\n"
    "                if (picked.getFieldCode().equals(field.getFieldCode())) {\n"
    '                    throw new IllegalArgumentException("Duplicate groupField: " + field.getFieldCode()\n'
    '                            + " (entity=" + entity.getEntityCode() + ")");\n'
    "                }\n"
    "            }"
)

# (label, [(file, anchor, mutant)], 预期必须变红的用例)
RUNS = [
    (
        "G1 GROUP BY 只按第一维（第二维进了 SELECT 却没进 GROUP BY → 数据库要么报错要么并行）",
        [(BUILDER, GROUP_BY_BLOCK,
          '            sql.append(" GROUP BY t.").append(quote(groups.get(0).getFieldCode()));')],
        [T_TWO],
    ),
    (
        "G2 multi 阈值写成 > 2（两维时走了单维分支：第二维没有标签列，透视程序引用不到列）",
        [(BUILDER, "            boolean multi = groups.size() > 1;",
          "            boolean multi = groups.size() > 2;")],
        [T_TWO],
    ),
    (
        "G3 重复维度改成静默去重（GROUP BY city, city 变成一张对角线假表）",
        [(BUILDER, DUP_THROW,
          "            for (FieldDefDTO picked : resolved) {\n"
          "                if (picked.getFieldCode().equals(field.getFieldCode())) {\n"
          "                    field = picked;\n"
          "                }\n"
          "            }")],
        [T_DUP],
    ),
    (
        "G4 只校验查得到的字段（第二维绕过标识符白名单 = 多维新开的注入面）",
        [(BUILDER, RESOLVE_CALL,
          "            FieldDefDTO field = fieldIndex.get(code);\n"
          "            if (field == null) {\n"
          "                field = new FieldDefDTO();\n"
          "                field.setFieldCode(code);\n"
          "            }")],
        [T_UNKNOWN, T_INJECT],
    ),
    (
        "G5 timeGroup 与多维的互斥守卫被搬走（引擎默默只对第一维分桶，另一维其实没参与）",
        [(BUILDER, TIMEGROUP_GUARD, "        // 注入：守卫随重构一起丢了")],
        [T_TIMEGROUP],
    ),
    (
        "G6 行序少了第二维的 tie-break（同一档行合计下, 列序跟着数据库的哈希序漂）",
        [(BUILDER, ORDER_2_BLOCK, "")],
        [T_TWO],
    ),
    (
        "G7 把空维度当错误（透视表只选行维度是常态，这里报错等于不让用户先选一半）",
        [(BUILDER, BLANK_SKIP,
          "            if (code == null || code.trim().isEmpty()) {\n"
          '                throw new IllegalArgumentException("Blank groupField");\n'
          "            }")],
        [T_BLANK],
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
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无一条连带红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(
            f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
            "先拿留档证明这一处改动换掉了哪一支断言，再改预期"
        )
    return 1


def main() -> int:
    ORIG[BUILDER] = BUILDER.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（修复态）===")
        failed, total, titles = run_tests("00_baseline")
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
                    failed, total, titles = run_tests(f"{label}_r{rep}")
                    bad += judge(label, failed, total, titles, expected)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path.name} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        failed, total, titles = run_tests("99_restored")
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
