#!/usr/bin/env python3
"""Falsify the HTTP-layer guards for duplicate unique codes.

Three mutants, each answering a different question:
  M6  prefilters go deleted=0-only        -> do the IT cases actually see the soft-delete hole?
  M7  advice returns the raw driver text  -> does the handler unit test pin the leak?
  M8  both (the actual pre-fix state)     -> does the whole defence collapse together?

M6 is expected to leave the handler unit test green and M7 is expected to leave the ITs
green: the two layers cover different holes (prefilters give readable messages, the advice
only has to stop leaks on paths nobody prefiltered -- concurrent creates, other tables).

z-lc-web resolves z-lc-core from ~/.m2, so a mutated core is invisible unless the
reactor rebuilds it -> always run with -pl z-lc-core,z-lc-web -am.
Files are restored in a finally-block and byte-verified.
"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
SRC = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core"
WEB = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller"
BAK = Path("/tmp/dupfix_http_bak")

SCHEMA = SRC / "schema/SchemaAdminBizService.java"
APP = SRC / "app/AppAdminBizService.java"
DICT = SRC / "dict/DictAdminServiceImpl.java"
REL = SRC / "relation/RelationServiceImpl.java"
HANDLER = WEB / "LcExceptionHandler.java"

APP_ANCHOR = '.eq("app_code", req.getAppCode()));'
APP_MUTATED = '.eq("app_code", req.getAppCode())\n                        .eq("deleted", 0));'
DICT_ANCHOR = '.eq("dict_code", dictCode));\n        if (hits.isEmpty()) {'
DICT_MUTATED = '.eq("dict_code", dictCode)\n                .eq("deleted", 0));\n        if (hits.isEmpty()) {'
REL_ANCHOR = '.eq("relation_code", entity.getRelationCode()));'
REL_MUTATED = '.eq("relation_code", entity.getRelationCode())\n                .eq("deleted", 0));'
ADVICE_ANCHOR = '.body(Result.fail("记录已存在：同一租户下的编码必须唯一，请换一个编码").code(400));'
# 修复前的形态: 兜底直接把驱动原文回给前端, 索引名/表名/列全泄漏。
ADVICE_MUTATED = '.body(Result.fail(ex.getMessage()).code(400));'

PREFILTER_EDITS = [(SCHEMA, APP_ANCHOR, APP_MUTATED), (APP, APP_ANCHOR, APP_MUTATED),
                   (DICT, DICT_ANCHOR, DICT_MUTATED), (REL, REL_ANCHOR, REL_MUTATED)]
ADVICE_EDIT = (HANDLER, ADVICE_ANCHOR, ADVICE_MUTATED)

DICT_IT = "duplicateDictCreateIsABusinessError"
CODES_IT = "duplicateUniqueCodesAreBusinessErrorsWithoutLeakingSchema"
ADVICE_UNIT = "duplicateKeyBecomesReadableBadRequest"

MUTANTS = [
    ("M6 all four prefilters go deleted=0-only (advice still sanitises)", PREFILTER_EDITS,
     {DICT_IT, CODES_IT}),
    ("M7 advice leaks the raw driver message (prefilters intact)", [ADVICE_EDIT],
     {ADVICE_UNIT}),
    ("M8 pre-fix production state: blind prefilters AND leaking advice",
     PREFILTER_EDITS + [ADVICE_EDIT],
     {DICT_IT, CODES_IT, ADVICE_UNIT}),
]

FILES = sorted({f for _, edits, _ in MUTANTS for f, _, _ in edits} |
               {f for _, edits, _ in MUTANTS for f, _, _ in edits})
FAIL_RE = re.compile(r"\[ERROR\]\s+(\w+Test)\.(\w+):(\d+)")
EXPECTED_NAMES = {n for _, _, s in MUTANTS for n in s}


def bak_name(path):
    return BAK / (str(path.relative_to(ROOT)).replace("/", "_") + ".orig")


def run_tests():
    p = subprocess.run(
        ["mvn", "-o", "-B", "test", "-pl", "z-lc-core,z-lc-web", "-am",
         "-Dtest=LcHttpContractTest,LcExceptionHandlerTest",
         "-Dsurefire.failIfNoSpecifiedTests=false"],
        cwd=str(ROOT), capture_output=True, text=True)
    out = p.stdout + p.stderr
    Path("/tmp/mvn_mut_http.log").write_text(out)
    red = {name for _, name, _ in FAIL_RE.findall(out)}
    totals = re.findall(r"^\[INFO\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)$",
                        out, re.M)
    return p.returncode, red, totals[-1] if totals else None


def describe_expected_names():
    """Guard against a typo in the expected-red set: every name must exist somewhere.
    Both target files are JUnit 5, so the methods are package-private (`void name()`)."""
    src = (ROOT / "z-lc-web/src/test/java/com/zifang/z/lc/web/it/LcHttpContractTest.java").read_text()
    unit = (ROOT / "z-lc-web/src/test/java/com/zifang/z/lc/web/controller/LcExceptionHandlerTest.java").read_text()
    missing = [n for n in EXPECTED_NAMES if f"void {n}(" not in src + unit]
    return missing


def main():
    BAK.mkdir(exist_ok=True)
    originals = {}
    for f in FILES:
        b = bak_name(f)
        if not b.exists():
            shutil.copyfile(str(f), str(b))
        originals[f] = b.read_bytes()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    missing = describe_expected_names()
    if missing:
        print("expected-red names not found in any test file: " + ", ".join(missing))
        return 1

    restore_all()
    bad = 0
    try:
        print("=== baseline (unmutated) ===")
        code, red, total = run_tests()
        print(f"  exit={code} red={sorted(red)} totals={total}")
        if code != 0 or red:
            print("BASELINE NOT GREEN -- mutation results would be meaningless, aborting")
            return 1

        for name, edits, expected in MUTANTS:
            for path, anchor, repl in edits:
                text = path.read_text()
                n = text.count(anchor)
                if n != 1:
                    print(f"\n[SKIPPED] {name}: anchor occurs {n}× in {path.name}")
                    bad += 1
                    continue
                path.write_text(text.replace(anchor, repl, 1))
            code, red, total = run_tests()
            verdict = "OK" if red == expected else "MISMATCH"
            if verdict != "OK":
                bad += 1
            print(f"\n[{verdict}] {name}")
            print(f"  exit={code} totals={total}")
            print(f"  red      = {sorted(red)}")
            print(f"  expected = {sorted(expected)}")
            if red - expected:
                print(f"  extra red (check not scoped to this defect): {sorted(red - expected)}")
            restore_all()
            for f in FILES:
                if f.read_bytes() != originals[f]:
                    print(f"  !! NOT RESTORED: {f}")
                    bad += 1

        print("\n=== re-verify green after restore ===")
        code, red, total = run_tests()
        print(f"  exit={code} red={sorted(red)} totals={total}")
        if code != 0:
            bad += 1
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("\nrestored clean: " + ("yes" if not leaked else "NO -> " + ", ".join(leaked)))
    print("RESULT: " + ("every HTTP-layer mutant killed exactly the intended tests"
                        if bad == 0 else f"{bad} problem(s), see /tmp/mvn_mut_http.log"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 裸 sys.exit(main()) 会让"只是 import 看一下"的调用方直接开跑战役
    sys.exit(main())
