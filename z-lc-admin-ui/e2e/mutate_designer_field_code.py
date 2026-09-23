#!/usr/bin/env python3
"""同轮注入缺陷自证：设计器保存前的字段编码闸（缺陷 #34 / #35 的前端半边）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_designer_field_code.py

这一支守的是保存前那两道：规则本体在 `src/fields/columnRules.ts`（`fieldCodeProblem`：
空 / 非法列名 / 撞引擎自建列），接线在 `DesignerPage.tsx`（`codeIssues`、保存按钮的
disabled、撞名那一格标红）。判定口径同 `mutate_field_code_guard.py`（后端那一支）：
**只有 vitest JSON 报告里出现预期用例的 failed 才算"注入被抓"**，多红算"守卫被别处依赖"
要写进文档，少红算"那条测试是空的"。构建/转译失败本身不算红。

跑的范围只到 `src/views/designer`，理由有两条，都不是偷懒：
① 这一支改的锚点只被 DesignerPage.tsx 用到，而 import DesignerPage 的测试只有 designer
   目录下那两份（grep 过：DesignerFieldCode.test.tsx + DesignerEntityStates.test.tsx）——
   别的文件不可能"连带红"，全量跑只是把已知那支没复现过的 ImportDialog 间歇红请进来当噪声。
② 一轮十来个用例，REPEATS=2 × 7 个注入 = 28 轮，全量跑要跑到天荒地老。
DesignerEntityStates 那一份也在跑的范围里：任何注入都不该动它，动了就是未预期红。

每个注入都写成本轮改动前**真实存在过**的形状或最常见的写法错误：
F7 就是改动前 `codeIssues` 的原样（只有正则那一句，没有空值话术、没有自建列清单）——
它让 T_ILLEGAL 保持绿，这不是漏判：改之前设计器确实拦得住中文/空格/数字开头，
真正漏的是撞 `id` / `deleted` 这类"合法但保留"的名字，那正是 #34。

F8/F9 是 #36 那一对（点"新建实体"根本没有草稿 + 新草稿预置引擎自建列），它们**故意**共用
同一个预期红集合 {T_NEW}：这一条用例只测"新草稿能不能保存"这一件事，两条机制都体现在它身上。
所以末尾那行"预期红集合相同"的告警对这两支是**已知的、有理由的**，不是重复检查；
F8 与 F9 各自拆的是不同行的代码，任一被改坏都会红。除此之外 7 支的集合两两不同。
⚠ 标题粒度的代价：集合相同不代表断言相同，F8 红在"编辑器没出现"、F9 红在"字段表里有 3 行引擎列"。
这两句要分别在归档的 failureMessages 里核一次，光看"1 条红"分不出是哪一支（本轮就是这么核的）。
"""

import json
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
DESIGNER = UI / "src/views/designer/DesignerPage.tsx"
RULES = UI / "src/fields/columnRules.ts"
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_designer_fieldcode_logs"
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


def run_vitest(label: str = "run") -> list[str]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", "src/views/designer",
             "--reporter=json", f"--outputFile={out}"],
            cwd=UI,
            capture_output=True,
            text=True,
            timeout=900,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(
            stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
            encoding="utf-8",
        )
        print(f"    vitest 超过 900s 未结束（视为该注入让测试挂住）；输出留档: {keep}")
        return [f"<timeout:{slug(label)}>"]
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    total = report.get("numTotalTests", 0)
    if not report.get("testResults") or not total:
        # 零个用例的一轮"全绿"是最容易骗人的一种绿。
        raise SystemExit(
            f"vitest report is empty (total={total}) — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed = []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            if case.get("status") == "failed":
                failed.append(case.get("title") or case.get("fullName") or "?")
    print(f"    vitest: {total} tests, {len(failed)} failed")
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        (LOG_DIR / f"{slug(label)}.vitest.json").write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep}")
    return failed


def judge(name: str, failed: list[str], expected: list[str]) -> int:
    hard = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for title in failed:
        print(f"    RED ({'预期' if any(e in title for e in expected) else '未预期'}) {title}")
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无未预期红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红（守卫被别处依赖？口径要重算）: {extra}")
    return 1


# 用例标题（与 DesignerFieldCode.test.tsx 一一对应）
T_ID = '把字段编码改成 id：要说清撞了引擎自建列，并且保存要按住'
T_OWNED = '五个引擎自建列逐个试，一个都不许放行'
T_UPPER = '大写 ID 一样要拦（MySQL 列名不分大小写，它照样撞 id）'
T_LEGAL = '合法编码不该被误伤：改了名要能真的保存出去'
T_RECOVER = '撞名修好之后，闸门要跟着松开（不能一旦拦过就永远按住）'
T_ILLEGAL = '中文、空格、数字开头三种都拦在保存之前'
T_EMPTY = '清空编码不算改好'
T_NEW = '新建一份干净草稿：填完编码就该能保存，字段表里不该预置引擎自建列'

RESERVED_BRANCH = "  if (SYSTEM_COLUMN_CODES.includes(value.toLowerCase())) {"
EMPTY_BRANCH = "  if (!value.trim()) return '字段编码不能为空';"
REGEX_DEF = "export const FIELD_CODE_RE = /^[A-Za-z][A-Za-z0-9_]*$/;"
SAVE_DISABLED = "disabled={!dirty || codeIssues.length > 0}"
CELL_STATUS = "status={defs[index]?.fieldCode && fieldCodeProblem(defs[index].fieldCode) ? 'error' : undefined}"
CODEISSUES_WIRING = """      const problem = fieldCodeProblem(field.fieldCode);
      if (problem) problems.push(problem);"""
NEW_DRAFT_ANCHOR = """    if (creating) return;"""
NEW_SEED = """                    // 不预置 systemFieldDefs()：id / create_time / update_time 是引擎自建列，
                    // 进不了用户字段表（后端会 400，见 fields/columnRules.ts），视图那边本来就自己合成它们。
                    fields: [],"""

# (label, [(file, anchor, mutant)], 预期必须变红的用例)
RUNS = [
    (
        "F1 拆掉撞引擎自建列那一支（#34 的原样：正则放得住这五个名字）",
        [(RULES, RESERVED_BRANCH, "  if (SYSTEM_COLUMN_CODES.length < 0) {")],
        [T_ID, T_OWNED, T_UPPER, T_RECOVER],
    ),
    (
        "F2 丢掉 toLowerCase（大写 ID 就漏了，而 MySQL 列名不分大小写照样撞）",
        [(RULES, RESERVED_BRANCH, "  if (SYSTEM_COLUMN_CODES.includes(value)) {")],
        [T_UPPER],
    ),
    (
        "F3 正则松成前缀匹配（最常见的'看着像正则'写法）",
        [(RULES, REGEX_DEF, "export const FIELD_CODE_RE = /^[A-Za-z]/;")],
        [T_ILLEGAL],
    ),
    (
        "F4 空值话术只管 undefined、不管空串（清空后不再说\"不能为空\"）",
        [(RULES, EMPTY_BRANCH, "  if (value === undefined) return '字段编码不能为空';")],
        [T_EMPTY],
    ),
    (
        "F5 提示还在、按钮不拦（界面说了不算，用户照样能点保存）",
        [(DESIGNER, SAVE_DISABLED, "disabled={!dirty}")],
        [T_ID, T_OWNED, T_UPPER, T_RECOVER, T_ILLEGAL, T_EMPTY],
    ),
    (
        "F6 撞名那一格不标红（一屏字段里只有一句横幅，找不出是哪个坏了）",
        [(DESIGNER, CELL_STATUS, "status={undefined}")],
        [T_ID],
    ),
    (
        "F7 退回改动前的 codeIssues（只有那一句正则，本轮修复前的真实形状）",
        [(DESIGNER, CODEISSUES_WIRING,
          "      if (field.fieldCode && !/^[A-Za-z][A-Za-z0-9_]*$/.test(field.fieldCode))"
          " problems.push(`字段编码「${field.fieldCode}」不合法`);")],
        [T_ID, T_OWNED, T_UPPER, T_RECOVER, T_EMPTY],
    ),
    (
        "F8 派生草稿的 effect 不再让位给新建草稿（#36 的原样：点新建实体等于空操作）",
        [(DESIGNER, NEW_DRAFT_ANCHOR, "    if (creating === null) return;")],
        [T_NEW],
    ),
    (
        "F9 新草稿重新预置引擎自建列（#34 那道闸于是把新建这条路按住）",
        [(DESIGNER, NEW_SEED,
          # 字面量而不是 SYSTEM_COLUMN_CODES.slice(...)：源码里已经没有那个 import 了
          # （tsc 的 TS6133 会拦"导入了没用"），注入若引用它就是**崩红**而不是**闸红** ——
          # 两者都会让 T_NEW 变红，但只有前者证明不了"预置引擎列会把新建这条路按住"。
          # 跑完要看 failureMessages 确认红的是 fieldRowCount 那句，不是 ReferenceError。
          "                    fields: (['id', 'create_time', 'update_time'] as string[]).map((code) => ({"
          " fieldCode: code, fieldName: code, fieldType: 'STRING' })) as FieldDefDTO[],")],
        [T_NEW],
    ),
]


def main() -> int:
    for path in (DESIGNER, RULES):
        ORIG[path] = path.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（全部修复态）===")
        if run_vitest("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for label, edits, expected in RUNS:
            print(f"\n=== {label} ===")
            for attempt in range(1, REPEATS + 1):
                tag = f"{label} (第{attempt}/{REPEATS}遍)"
                try:
                    for path, anchor, mutated in edits:
                        sub(path, anchor, mutated)
                    bad += judge(tag, run_vitest(tag), expected)
                finally:
                    for path, _, _ in edits:
                        path.write_text(ORIG[path], encoding="utf-8")
                        if path.read_text(encoding="utf-8") != ORIG[path]:
                            print(f"  !! {path} 未恢复到原始字节")
                            bad += 1
                time.sleep(0.2)

        print("\n=== 恢复后复跑 ===")
        if run_vitest("99_restored"):
            print("  !! 恢复后仍有红")
            bad += 1
    finally:
        for path, text in ORIG.items():
            path.write_text(text, encoding="utf-8")

    sets = {label: tuple(sorted(expected)) for label, _, expected in RUNS}
    seen: dict[tuple, str] = {}
    for label, s in sets.items():
        if s in seen:
            print(f"⚠ 预期红集合与「{seen[s]}」完全相同，这一支不新增证据: {label}")
        seen[s] = label

    print(f"\n{'FAILED: ' + str(bad) if bad else 'ALL MUTANTS BEHAVED AS CLAIMED'}")
    return 1 if bad else 0


if __name__ == "__main__":
    from _mutlock import acquire, release

    acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        release()
