#!/usr/bin/env python3
"""同轮注入缺陷自证：字段编码那道闸（缺陷 #34 / #35）。

背景（全是实测打出来的，不是读代码读出来的）：
- `POST /admin/app/entity/create` 对 fieldCode = id / tenant_code / deleted / create_time /
  update_time 一律 HTTP 200 收下，要等 provision 建表才炸出裸 500 `Duplicate column name`；
  而 provision-all 在第一个坏实体上抛，同应用其他实体的表一起被挡住（实测：干净实体 ctl
  的表因此不存在，`runtime/list ctl` 回 500 Table not found，单独 provision 它才成功）。
- 非法编码更阴：`buildCreateTableDdl` 对不合规的 fieldCode 是 `continue` 静默跳过，
  建表**照样返回成功**，元数据说有三列、物理表一列都没有（实测 DDL 里只剩 title）。

判定口径沿用 _e2e/mutate_connection_leak.py 那条教训：
**只有 surefire 报告里出现预期测试的 failure 才算"注入被抓"**；
编译失败、构建失败一律判为「没证明任何事」，不算红。

跑法（在 z-lc 下）：

    python3 _e2e/mutate_field_code_guard.py
"""

import hashlib
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
REPORT = (ROOT / "z-lc-core/target/surefire-reports"
          / "TEST-com.zifang.z.lc.core.schema.SchemaAdminBizServiceTest.xml")
LOG_DIR = Path("/tmp/zlc_mut_fieldcode_logs")

REPEATS = 2

# 分母：这一支关心的 7 条（新增的），以及它们各自该在哪些注入下红。
COLLECTED = [
    "fieldCodeFixtureActuallySeedsAnApp",
    "createEntityShouldRejectEveryEngineOwnedColumnCode",
    "createEntityShouldRejectSystemColumnCodeIgnoringCase",
    "createEntityShouldRejectFieldCodeThatIsNotALegalColumnName",
    "updateEntityShouldRejectCollidingCodeBeforeAnyWrite",
    "buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns",
    "buildCreateTableDdlShouldKeepAllLegalColumns",
]
FIXTURE = "fieldCodeFixtureActuallySeedsAnApp"
POSITIVE = "buildCreateTableDdlShouldKeepAllLegalColumns"
A = "createEntityShouldRejectEveryEngineOwnedColumnCode"
B = "createEntityShouldRejectSystemColumnCodeIgnoringCase"
C = "createEntityShouldRejectFieldCodeThatIsNotALegalColumnName"
D = "updateEntityShouldRejectCollidingCodeBeforeAnyWrite"
E = "buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns"

VALIDATOR_HEAD = "    private void validateFieldCodes(List<FieldDefDTO> fields) {\n        if (fields == null) {\n            return;\n        }\n"
VALIDATOR_NEUTERED = "    private void validateFieldCodes(List<FieldDefDTO> fields) {\n        if (true) {\n            return;\n        }\n        if (fields == null) {\n            return;\n        }\n"

CREATE_CALL = ("        // 先校验再落库: 建表要等 provision 才发生，元数据一旦写进去就是永久一份坏定义，\n"
               "        // 而且 provision-all 会在第一个坏实体上抛，把同应用其他实体的表一起挡住。\n"
               "        validateFieldCodes(req.getFields());\n")
UPDATE_CALL = ("        // 同样在任何写入之前: 全量替换会先把旧字段软删掉，坏编码进来后再想退回去就没退路了。\n"
               "        validateFieldCodes(req.getFields());\n")
DDL_CALL = ("        // 这里以前对撞名/非法编码是 `continue` 静默跳过: 建表照样返回\"成功\", 少几列没人知道。\n"
            "        validateFieldCodes(def.getFields());\n")

MUTANTS = {
    # I1: 整个闸失效 —— create / update / 建表三处一起松开
    "I1_validator_neutered": [(VALIDATOR_HEAD, VALIDATOR_NEUTERED)],
    # I2: 只松开 createEntity 那一处
    "I2_create_call_removed": [(CREATE_CALL, "")],
    # I3: 只松开 updateEntity 那一处
    "I3_update_call_removed": [(UPDATE_CALL, "")],
    # I4: 只松开建表那一处（= 退回"静默少列还说成功"）
    "I4_ddl_call_removed": [(DDL_CALL, "")],
}

PREDICTED = {
    "I1_validator_neutered": {A, B, C, D, E},
    "I2_create_call_removed": {A, B, C},
    "I3_update_call_removed": {D},
    "I4_ddl_call_removed": {E},
}

MVN = ["mvn", "-o", "-B", "test", "-pl", "z-lc-core", "-q",
       "-Dtest=SchemaAdminBizServiceTest", "-DfailIfNoTests=false",
       "-Dsurefire.failIfNoSpecifiedTests=false"]


def md5(path: Path) -> str:
    return hashlib.md5(path.read_bytes()).hexdigest()


def surefire_results() -> dict:
    """name → 'failed' | 'passed'. 空 dict = 什么都没跑起来（≠ 全绿）。"""
    if not REPORT.exists():
        return {}
    out = {}
    for case in ET.parse(REPORT).getroot().findall("testcase"):
        name = (case.get("name") or "").split("(")[0]
        bad = case.find("failure") is not None or case.find("error") is not None
        out[name] = "failed" if bad else "passed"
    return out


def run_and_collect(label: str) -> tuple:
    if REPORT.exists():
        REPORT.unlink()
    proc = subprocess.run(MVN, cwd=ROOT, capture_output=True, text=True)
    results = surefire_results()
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    (LOG_DIR / f"{label}.out").write_text(proc.stdout + "\n--- stderr ---\n" + proc.stderr)
    if REPORT.exists():
        (LOG_DIR / f"{label}.xml").write_bytes(REPORT.read_bytes())
    return proc.returncode, results


def judge(label: str, expected_red: set, code: int, results: dict) -> list:
    problems = []
    if not results:
        return [f"{label}: 没有任何 surefire 结果（exit={code}）—— 构建/编译失败，没证明任何事"]
    ran = set(results)
    missing = [t for t in COLLECTED if t not in ran]
    if missing:
        problems.append(f"{label}: 这些用例根本没跑: {missing}")
    for t in COLLECTED:
        status = results.get(t)
        if status is None:
            continue
        red = status == "failed"
        if red != (t in expected_red):
            verb = "本该红却绿了" if t in expected_red else "不该红"
            problems.append(f"{label}: {t} {verb} (exit={code})")
    collateral = sorted(t for t in ran
                        if t not in COLLECTED and results[t] == "failed")
    if collateral:
        problems.append(f"{label}: 预期外的红（连带？）: {collateral}")
    return problems


def main() -> int:
    original = SRC.read_text()
    if md5(SRC) != hashlib.md5(original.encode()).hexdigest():
        print("读文件即校验不过，停")
        return 2

    print("=== 基线（未注入）===")
    code, results = run_and_collect("baseline")
    problems = []
    if not results:
        print(f"基线跑不起来 (exit={code})，看 {LOG_DIR}/baseline.out")
        return 2
    bad = [t for t, s in results.items() if s == "failed"]
    if bad:
        problems.append(f"基线就有红: {bad}")
    print(f"  跑了 {len(results)} 条，红 {len(bad)} 条，exit={code}")

    for name, patches in MUTANTS.items():
        text = original
        for old, new in patches:
            if text.count(old) != 1:
                print(f"  跳过 {name}: 锚点出现 {text.count(old)} 次（要恰好 1 次），注入没打进去")
                problems.append(f"{name}: 锚点不唯一，注入未生效")
                text = None
                break
            text = text.replace(old, new)
        if text is None:
            continue
        SRC.write_text(text)
        broken_restore = False
        try:
            for rep in range(1, REPEATS + 1):
                label = f"{name}_r{rep}"
                code, results = run_and_collect(label)
                got_red = {t for t, s in results.items() if s == "failed"}
                p = judge(label, PREDICTED[name], code, results)
                status = "OK" if not p else "问题"
                print(f"  {label}: {status}  预期红 {sorted(PREDICTED[name])} / 实测红 {sorted(got_red)}")
                problems.extend(p)
        finally:
            SRC.write_text(original)
            if md5(SRC) != hashlib.md5(original.encode()).hexdigest():
                print("  !! 恢复后字节不一致，立刻停")
                broken_restore = True
        if broken_restore:
            return 3

    print("=== 恢复后复跑 ===")
    code, results = run_and_collect("restored")
    bad = [t for t, s in results.items() if s == "failed"]
    if bad or not results:
        problems.append(f"恢复后有红或没跑起来: {bad} exit={code}")
    print(f"  跑了 {len(results)} 条，红 {len(bad)} 条，exit={code}")

    # 闸必须"各测一半"：红集合两两不同，否则有两条是重复检查。
    sets = {n: frozenset(s) for n, s in PREDICTED.items()}
    seen = {}
    for n, s in sets.items():
        if s in seen:
            problems.append(f"{n} 与 {seen[s]} 的预期红集合相同 —— 其中一条是重复检查")
        seen[s] = n

    if problems:
        print("\n--- 有问题 ---")
        for p in problems:
            print("  " + p)
        print(f"\n日志在 {LOG_DIR}")
        return 1
    print(f"\nALL MUTANTS BEHAVED AS CLAIMED  （{len(MUTANTS)} 个注入 × {REPEATS} 遍，"
          f"日志 {LOG_DIR}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
