#!/usr/bin/env python3
"""同轮注入缺陷自证：管理页列表的「读到 / 没读到 / 真没有」口径。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_admin_list_guards.py

`AdminListStates.test.tsx` 这批测试是跟着 `useResourceList` 一起由我写的，
没有注入就无法区分"测试钉住了那处口径"和"测试只是跟着代码一起变绿"。
每条 mutant 都是**修复前的真实代码形状**，不是为测试编的稻草人：

* A 就是 DictsPage 改之前的字面量 `isError={false}`；
* B/C 就是七个管理页改之前的 `catch { message.error(...) }`（行不清、错误不外露）；
* E 就是"故障只活在一句 toast 里"；
* F 就是 antd Table 的默认 `暂无数据`。

判定只认**具名的那条测试变红**，且红的集合要**恰好**等于预期：多红说明口径被别处
依赖（要写进文档），少红说明这条测试是空的。跑完在 finally 里按字节还原并复跑。
"""

import json
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
SRC = UI / "src"
SCOPE = SRC / "views/admin/_scope.ts"
SHARED = SRC / "views/admin/_shared.tsx"
DICTS = SRC / "views/admin/DictsPage.tsx"
RELATIONS = SRC / "views/admin/RelationsPage.tsx"

ORIG = {}

KEEP_ROWS = """      if (failure || !Array.isArray(next)) {
        setRows([]);
"""
TRUST_ANYTHING = """      if (failure || !Array.isArray(next)) {
"""
BANNER_LINE = """      <ListBanner state={state} error={error} onRetry={reload} label="实体关系" />
"""
ERROR_SENTENCE = """  if (state === 'error') return '接口没有读到数据，无法判断有没有';
"""


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor!r}")
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
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed = [
        case.get("title") or case.get("fullName") or "?"
        for file_result in report.get("testResults", [])
        for case in file_result.get("assertionResults", [])
        if case.get("status") == "failed"
    ]
    print(f"    vitest: {report.get('numTotalTests', 0)} tests, {len(failed)} failed")
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
        print(f"  !! {name}: 出现了预期之外的红（口径被别处依赖？要写进文档）: {extra}")
        return 1
    print(f"  OK  {name}: 红了恰好 {len(failed)} 条，与预期一致")
    return 0


# (label, [(file, anchor, mutant)], expected red test-title substrings)
RUNS = [
    (
        "A DictsPage 的 isError 退回字面 false（修复前的原样）",
        [(DICTS, "isError={dictState === 'error'}", "isError={false}")],
        ["字典接口失败时显示"],
    ),
    (
        "B 失败的刷新不清行（= 修复前 catch 里什么都不做）",
        [(SCOPE, KEEP_ROWS, TRUST_ANYTHING)],
        ["失败要清空 rows", "读到数据就正常列行"],
    ),
    (
        "C 接口回什么都当成列表（data:null 被静默变成空列表）",
        [
            (
                SCOPE,
                """      if (failure || !Array.isArray(next)) {
        setRows([]);
        setError(failure ?? new Error('接口没有返回列表'));
        setStatus('error');
        return;
      }
      setRows(next);
      setStatus('done');
""",
                """      if (failure) {
        setRows([]);
        setError(failure);
        setStatus('error');
        return;
      }
      setRows(Array.isArray(next) ? next : []);
      setStatus('done');
""",
            )
        ],
        ["success:true 但 data:null 是没读到"],
    ),
    (
        "D 去掉过期响应守卫（慢的旧响应盖掉新应用）",
        [(SCOPE, "      if (mine !== seq.current) return;\n", "")],
        ["换应用后，慢一步到达的旧响应"],
    ),
    (
        "E 撤掉故障横幅（故障只剩一句三秒就消失的 toast）",
        [(RELATIONS, BANNER_LINE, "")],
        ["接口失败时说", "读到数据就正常列行", "横幅里的重试"],
    ),
    (
        "F 空态不再区分故障（退回 antd 默认的「暂无数据」）",
        [(SCOPE, ERROR_SENTENCE, "")],
        ["接口失败时说", "读到数据就正常列行"],
    ),
    (
        "G 没选应用当成「读完了，什么都没有」",
        [(SCOPE, "      setStatus('idle');", "      setStatus('done');")],
        # 三条都靠 idle 这个口径，不是重复检查：hook 层直接钉状态；RelationsPage 那两条
        # 钉的是"没作用域时不许说还没有"，其中应用列表故障那条是**级联**（picker 读不到
        # → appCode 为空 → 列表本来就是 idle），少了它就看不出应用故障页也在用这句话。
        [
            "没有作用域时是 idle",
            "一个应用都没读到时",
            "应用列表读不到时",
        ],
    ),
    (
        "H 应用列表的故障不外露（picker 只是空着）",
        [(SCOPE, "    error: apps.error,", "    error: null,")],
        ["应用列表读不到时"],
    ),
]


def main() -> int:
    for p in (SCOPE, SHARED, DICTS, RELATIONS):
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
