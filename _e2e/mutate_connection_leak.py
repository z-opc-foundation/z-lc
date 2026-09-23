#!/usr/bin/env python3
"""同轮注入缺陷自证：逆向映射的连接生命周期（scanTables + mapTable）。

背景：这个泄漏不是读代码读出来的，是 Druid 抓现行的 —— dev 日志里
`abandon connection, owner thread: http-nio-18090-exec-1`，借用栈停在
DbTableMapperService.scanTables，一个连接被持有 66 秒。HEAD 里的写法是
`getDataSource().getConnection().getMetaData()`，借了不还。

跑法（在 z-lc 下）：

    python3 _e2e/mutate_connection_leak.py

判定口径（第一版在这里撒过谎，所以写死在代码里）：
**只有 surefire 报告里出现预期测试的 failure/error 才算"注入被抓"**。
构建失败（编译错、插件错、依赖缺）一律判为「没证明任何事」，不算红。
第一版把 `mvn != 0` 当成红，结果注入的写法 `Connection c = ds.getConnection()`
在 try 外头根本编译不过（SQLException 未声明），脚本却报
"ALL MUTANTS BEHAVED AS CLAIMED" —— 一个只会因编译失败而红的守卫，
钉不住任何东西。
"""

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/mapper/DbTableMapperService.java"
REPORT = ROOT / "z-lc-web/target/surefire-reports/TEST-com.zifang.z.lc.web.it.LcHttpContractTest.xml"

EXPECTED = {"reverseMappingReturnsEveryConnectionItBorrows", "reverseMappingEndpointsDoNotGrowThePool"}

# 把连接的获取挪进 try 体内：语法合法（SQLException 被 catch (Exception) 接住），
# 唯一的差别是它不再被自动归还 —— 这正是 HEAD 里那份写法的行为。
SCANS = [
    (
        "        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {\n"
        "            DatabaseMetaData meta = conn.getMetaData();",
        "        try {\n"
        "            Connection conn = jdbcTemplate.getDataSource().getConnection();\n"
        "            DatabaseMetaData meta = conn.getMetaData();",
    ),
    (
        "        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {\n"
        "            return mapTable(conn.getMetaData(), tableName, null);",
        "        try {\n"
        "            Connection conn = jdbcTemplate.getDataSource().getConnection();\n"
        "            return mapTable(conn.getMetaData(), tableName, null);",
    ),
]

MVN = ["mvn", "-o", "-B", "test", "-pl", "z-lc-web", "-am", "-q",
       "-Dtest=LcHttpContractTest#reverseMapping*", "-DfailIfNoTests=false",
       "-Dsurefire.failIfNoSpecifiedTests=false"]


def surefire_results() -> dict[str, str]:
    """Read the report freshly: name → 'passed' | 'failed'. Empty dict = nothing ran."""
    if not REPORT.exists():
        return {}
    root = ET.parse(REPORT).getroot()
    out = {}
    for case in root.findall("testcase"):
        name = (case.get("name") or "").split("(")[0]
        bad = case.find("failure") is not None or case.find("error") is not None
        out[name] = "failed" if bad else "passed"
    return out


def run(label: str) -> tuple[int, dict[str, str]]:
    print(f"--- {label} ---", flush=True)
    REPORT.unlink(missing_ok=True)
    proc = subprocess.run(MVN, cwd=ROOT, capture_output=True, text=True)
    results = surefire_results()
    if not results:
        print("    surefire 报告里一条测试都没有 —— 这次运行证明不了任何事")
        for line in [l for l in proc.stdout.splitlines() if "ERROR" in l][-6:]:
            print("   ", line[:180])
    for name, status in sorted(results.items()):
        print(f"    {status.upper():7} {name}")
    print(f"    mvn exit={proc.returncode}")
    return proc.returncode, results


def main() -> int:
    original = SRC.read_text(encoding="utf-8")
    for anchor, _ in SCANS:
        if original.count(anchor) != 1:
            print(f"anchor found {original.count(anchor)}× in {SRC}, expected 1:\n{anchor}")
            return 2

    rc = 0
    try:
        code, results = run("基线：连接归还写法")
        if code != 0 or set(results) != EXPECTED or any(v != "passed" for v in results.values()):
            print("  !! 基线不干净（要恰好这两条且全过），注入结果无法归因")
            return 2

        mutated = original
        for anchor, leaking in SCANS:
            mutated = mutated.replace(anchor, leaking)
        SRC.write_text(mutated, encoding="utf-8")

        code, results = run("注入：把 getConnection 挪出 try-with-resources（连接不再归还）")
        caught = {n for n, s in results.items() if s == "failed"}
        if code == 0 and not caught:
            print("  !! 注入泄漏后两条测试仍然全绿 —— 它们钉不住这个缺陷（空断言）")
            rc = 1
        elif not caught:
            print("  !! 构建非零但没有测试判红：多半又是编译不过，等于没证明任何事")
            rc = 1
        else:
            print(f"  OK  注入泄漏即变红：{sorted(caught)}")
            if not caught & EXPECTED:
                print("  !! 红的不是预期的那两条，归因不成立")
                rc = 1
    finally:
        SRC.write_text(original, encoding="utf-8")
        back = SRC.read_text(encoding="utf-8") == original
        print(f"    source restored byte-exact: {back}")
        if not back:
            rc = 1
        code, results = run("恢复后复跑")
        if code != 0 or any(v != "passed" for v in results.values()) or set(results) != EXPECTED:
            print("  !! 恢复后不干净")
            rc = 1

    print("FAILED" if rc else "ALL MUTANTS BEHAVED AS CLAIMED")
    return rc


if __name__ == "__main__":
    sys.exit(main())
