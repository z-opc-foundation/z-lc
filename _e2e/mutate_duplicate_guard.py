#!/usr/bin/env python3
"""Falsify the duplicate unique-code prefilters: re-introduce the exact production bug
(.eq("deleted", 0) against a unique index that does not include `deleted`) and prove
which tests go red. Files are restored in a finally-block and byte-verified, so an
exception can never leave a mutant on disk."""
import re
import shutil
import subprocess
import sys
from pathlib import Path

CORE = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc/z-lc-core")
SRC = CORE / "src/main/java/com/zifang/z/lc/core"
BAK = Path("/tmp/dupfix_bak")
CLASSES = "SchemaAdminBizServiceTest,AppAdminBizServiceTest,RelationServiceImplTest,DictAdminServiceImplTest"

SCHEMA = SRC / "schema/SchemaAdminBizService.java"
APP = SRC / "app/AppAdminBizService.java"
DICT = SRC / "dict/DictAdminServiceImpl.java"
REL = SRC / "relation/RelationServiceImpl.java"

APP_ANCHOR = '.eq("app_code", req.getAppCode()));'
APP_MUTATED = '.eq("app_code", req.getAppCode())\n                        .eq("deleted", 0));'
DICT_ANCHOR = '.eq("dict_code", dictCode));\n        if (hits.isEmpty()) {'
DICT_MUTATED = '.eq("dict_code", dictCode)\n                .eq("deleted", 0));\n        if (hits.isEmpty()) {'
REL_ANCHOR = '.eq("relation_code", entity.getRelationCode()));'
REL_MUTATED = '.eq("relation_code", entity.getRelationCode())\n                .eq("deleted", 0));'
DICT_CALL = "        requireDictCodeFree(entity.getTenantCode(), entity.getDictCode());\n"

MUTANTS = [
    ("M1 schema: app_code prefilter goes deleted=0-only", [(SCHEMA, APP_ANCHOR, APP_MUTATED)],
     {"createAppShouldRejectCodeHeldBySoftDeletedApp"}),
    ("M2 app: app_code prefilter goes deleted=0-only", [(APP, APP_ANCHOR, APP_MUTATED)],
     {"createAppShouldRejectCodeHeldBySoftDeletedApp"}),
    ("M3 dict: dict_code prefilter goes deleted=0-only", [(DICT, DICT_ANCHOR, DICT_MUTATED)],
     {"createDictShouldRejectCodeHeldBySoftDeletedDict"}),
    ("M4 relation: relation_code prefilter goes deleted=0-only", [(REL, REL_ANCHOR, REL_MUTATED)],
     {"createRelationShouldRejectCodeHeldBySoftDeletedRelation"}),
    ("M5 dict: prefilter call removed entirely", [(DICT, DICT_CALL, "")],
     {"createDictShouldRejectDuplicateDictCode", "createDictShouldRejectCodeHeldBySoftDeletedDict"}),
]

FILES = sorted({f for _, edits, _ in MUTANTS for f, _, _ in edits})
FAIL_RE = re.compile(r"\[ERROR\]\s+(\w+Test)\.(\w+):(\d+)")


def bak_name(path):
    return BAK / (path.name.replace(".", "_") + ".orig")


def run_tests():
    p = subprocess.run(
        ["mvn", "-o", "-B", "test", "-pl", "z-lc-core", "-Dtest=" + CLASSES,
         "-Dsurefire.failIfNoSpecifiedTests=false"],
        cwd=str(CORE.parent), capture_output=True, text=True)
    out = p.stdout + p.stderr
    Path("/tmp/mvn_mut.log").write_text(out)
    red = {name for _, name, _ in FAIL_RE.findall(out)}
    totals = re.findall(r"^\[INFO\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)$",
                        out, re.M)
    compile_err = "COMPILATION ERROR" in out
    return p.returncode, red, totals[-1] if totals else None, compile_err


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

    restore_all()  # idempotent: start from the known-good pristine copy every time
    bad = 0
    try:
        print("=== baseline (unmutated) ===")
        code, red, total, cerr = run_tests()
        print(f"  exit={code} red={sorted(red)} totals={total}")
        if code != 0 or red:
            print("BASELINE NOT GREEN -- mutation results would be meaningless, aborting")
            return 1

        for name, edits, expected in MUTANTS:
            for path, anchor, repl in edits:
                text = path.read_text()
                n = text.count(anchor)
                if n != 1:
                    print(f"\n[SKIPPED] {name}: anchor occurs {n} times in {path.name}")
                    bad += 1
                    continue
                path.write_text(text.replace(anchor, repl, 1))
            code, red, total, cerr = run_tests()
            verdict = "OK" if red == expected and not cerr else "MISMATCH"
            if verdict != "OK":
                bad += 1
            print(f"\n[{verdict}] {name}")
            print(f"  exit={code} compile_error={cerr} totals={total}")
            print(f"  red      = {sorted(red)}")
            print(f"  expected = {sorted(expected)}")
            restore_all()
            for f in FILES:
                if f.read_bytes() != originals[f]:
                    print(f"  !! NOT RESTORED: {f}")
                    bad += 1

        print("\n=== re-verify green after restore ===")
        code, red, total, cerr = run_tests()
        print(f"  exit={code} red={sorted(red)} totals={total}")
        if code != 0:
            bad += 1
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("\nrestored clean: " + ("yes" if not leaked else "NO -> " + ", ".join(leaked)))
    print("RESULT: " + ("every mutant killed exactly the intended tests"
                        if bad == 0 else f"{bad} problem(s), see /tmp/mvn_mut.log"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 裸 sys.exit(main()) 会让"只是 import 看一下"的调用方直接开跑战役
    sys.exit(main())
