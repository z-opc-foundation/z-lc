#!/usr/bin/env python3
"""Falsify the `[15i] 唯一编码撞车` section of _e2e/e2e_api_test.py against the DEPLOYED fat jar.

This layer is the one LcHttpContractTest cannot substitute for: it hits the artifact that
actually ships.

The four "blind prefilter" mutants (A1..A4) each re-instate `.eq("deleted", 0)` on *one*
resource only, so every mutant maps to exactly one assertion — which is how a passing run
proves that specific prefilter is load-bearing. Mutating all four at once (as the first
version did) collapses four independent signals into one and cannot tell you which service
the suite actually covers.

The B mutant (advice returns raw driver text, prefilters intact) is expected to be INVISIBLE
here, because the prefilters shadow the advice on every path the suite drives. Saying so out
loud is the point: it is why LcExceptionHandlerTest exists — that test calls the handler
directly, which is the only way to reach this branch.

Every run rebuilds the jar and fingerprints every nested z-lc-* jar inside it, so a mutant can
never "pass" because the build silently shipped the old classes. The first run of this script
shipped a fingerprint built from `unzip -Z` output, whose lines start with file permissions,
not paths — so zero entries matched, the md5 was of the empty string, and *both* mutants
reported "changed=False". A vacuous check is worse than no check, so an empty fingerprint is
now a hard error.
"""
import hashlib
import io
import os
import re
import subprocess
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
SRC = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core"
WEB = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller"
# 备份放 ~/.cache: /tmp 会被同机其他会话扫空, 备份中途消失 = 还原失败
BAK = Path.home() / ".cache/zlc42/deployed_bak" / "tmp/dupfix_deployed_bak"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"

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
ADVICE_MUTATED = '.body(Result.fail(ex.getMessage()).code(400));'

# tag -> (edits, the one [15i] assertion that must go red)
RUNS = [
    ("A1", [(SCHEMA, APP_ANCHOR, APP_MUTATED)], "文案要说清设计器路由此前已被删除"),
    ("A2", [(APP, APP_ANCHOR, APP_MUTATED)], "文案要解释是「此前已被删除」"),
    ("A3", [(DICT, DICT_ANCHOR, DICT_MUTATED)], "文案要说清字典此前已被删除"),
    ("A4", [(REL, REL_ANCHOR, REL_MUTATED)], "文案要说清关系此前已被删除"),
    ("B", [(HANDLER, ADVICE_ANCHOR, ADVICE_MUTATED)], None),
]

FILES = sorted({SCHEMA, APP, DICT, REL, HANDLER}, key=lambda p: str(p))


def sh(cmd, cwd=ROOT, timeout=900):
    return subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout)


def snapshot_sources():
    """一次运行一份独占备份。

    原来是 `BAK` 跨运行复用 + `if not b.exists(): copyfile` + `originals = b.read_bytes()`:
    上一轮战役留下的 .orig 永不过期, 于是每次开局都把**更早一轮**的源码盖回工作树。
    同一写法在 mutate_pipeline_wiring_guard.py 上实测抹掉了 09-26 未提交的 #42 改动, 所以这里
    换成不存在共享路径的形状; 真被 SIGKILL 留下注入残局, 下一次会在基线那句红给我看。
    """
    run_dir = BAK / f"run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    originals = {}
    for f in FILES:
        if not f.exists():
            raise RuntimeError(f"source file missing, cannot even baseline: {f}")
        disk = f.read_bytes()
        b = run_dir / (str(f).replace("/", "_").lstrip("_") + ".orig")
        b.write_bytes(disk)
        if b.read_bytes() != disk:
            raise RuntimeError(f"backup of {f} did not stick")
        originals[f] = disk
    print(f"sources snapshotted -> {run_dir}")
    return originals


def artifact_fingerprint():
    """md5 over every *class file* inside every nested z-lc-* jar of the fat jar.

    Deliberately not the nested jars themselves: those are rewritten with fresh entry
    timestamps on every build, so a jar-level digest differs even for byte-identical sources
    (measured: restoring pristine sources and rebuilding gave a different jar-level md5). That
    makes "changed=True" fire for a mutant that never reached the artifact — a guard that
    always passes guards nothing. Class file contents are stable across rebuilds, so this
    digest is equal for equal sources and different when an edit really shipped.
    Covers core *and* web, because the two kinds of mutant live in different modules.
    """
    parts = []
    with zipfile.ZipFile(JAR) as fat:
        libs = [n for n in sorted(fat.namelist()) if n.startswith("BOOT-INF/lib/z-lc-")]
        if not libs:
            raise RuntimeError(f"no nested z-lc-*.jar inside {JAR}: a fingerprint over zero "
                               "entries is constant and therefore worthless")
        for lib in libs:
            with zipfile.ZipFile(io.BytesIO(fat.read(lib))) as inner:
                classes = [e for e in sorted(inner.namelist()) if e.endswith(".class")]
                if not classes:
                    raise RuntimeError(f"{lib} carries no .class entries")
                parts += [f"{lib}!{e}:{hashlib.md5(inner.read(e)).hexdigest()}" for e in classes]
    return "|".join(parts)


def fp_id(fingerprint):
    return hashlib.md5(fingerprint.encode()).hexdigest()[:12]


def lc_pids():
    out = subprocess.run(["pgrep", "-f", "z-lc-admin-1.0.0-SNAPSHOT.jar"],
                         capture_output=True, text=True).stdout.split()
    return [int(x) for x in out]


def wait_health(timeout=150):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(HEALTH, timeout=3) as r:
                if b'"status":"UP"' in r.read():
                    return True
        except Exception:  # noqa: BLE001
            pass
        time.sleep(2)
    return False


def restart():
    pids = lc_pids()
    if len(pids) > 1:
        raise RuntimeError(f"refusing to guess which JVM to stop: {pids}")
    if pids:
        subprocess.run(["kill", "-9", str(pids[0])])
        time.sleep(2)
    log = open("/tmp/lc_deployed_mut_boot.log", "ab")
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log)
    if not wait_health():
        raise RuntimeError("server did not come up; see /tmp/lc_deployed_mut_boot.log")


def build():
    p = sh(["mvn", "-o", "-B", "install", "-DskipTests", "-pl", "z-lc-core,z-lc-admin", "-am"])
    if "BUILD SUCCESS" not in (p.stdout + p.stderr):
        raise RuntimeError("jar build failed, tail of log:\n"
                           + "\n".join((p.stdout + p.stderr).splitlines()[-25:]))


def run_e2e(label):
    """Returns ((passed, total), all_fail_names, [15i]-only fail names).
    The section split is by position in the harness output, not by keyword matching."""
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    Path(f"/tmp/e2e_deployed_{label}.log").write_text(p.stdout + p.stderr)
    summary = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    section = None
    fails, i15 = [], []
    for line in p.stdout.splitlines():
        m = re.match(r"^\[(\d+\w*)\]", line)
        if m:
            section = m.group(1)
            continue
        if line.startswith("  FAIL  "):
            name = line[8:].split("   <<")[0]
            fails.append(name)
            if section == "15i":
                i15.append(name)
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    return parsed, fails, i15


def main():
    originals = snapshot_sources()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    restore_all()
    bad = 0
    try:
        build()
        restart()
        base_fp = artifact_fingerprint()
        (ok_n, total), base_fails, _ = run_e2e("baseline")
        print(f"=== baseline deployed: {ok_n}/{total} artifact fp={fp_id(base_fp)}")
        if ok_n != total:
            print(f"BASELINE NOT GREEN on the deployed jar, aborting. failures={base_fails}")
            return 1

        for tag, edits, expect in RUNS:
            for path, anchor, repl in edits:
                text = path.read_text()
                if text.count(anchor) != 1:
                    print(f"[SKIPPED] {tag}: anchor occurs {text.count(anchor)}x in {path.name}")
                    bad += 1
                    continue
                path.write_text(text.replace(anchor, repl, 1))
            build()
            fp = artifact_fingerprint()
            changed = fp != base_fp
            restart()
            (passed, this_total), fails, i15 = run_e2e(f"mut{tag}")
            print(f"\n[{tag}] expect red on: {expect or 'nothing (prefilters shadow the advice)'}")
            print(f"  artifact fp = {fp_id(fp)}  changed={changed}")
            print(f"  e2e = {passed}/{this_total}  (baseline denominator {total})")
            print(f"  [15i] failures ({len(i15)}): {i15 or '(none)'}")
            print(f"  failures outside [15i]: {[f for f in fails if f not in i15] or '(none)'}")
            if not changed:
                print("  !! artifact did not change -> this run proves nothing")
                bad += 1
            # 分母必须和基线一样。原来这里打印的是基线的 total（拿别人的分母报自己的数），
            # 而 B 那一支只问"有没有红"：如果 server 起来就崩、E2E 在第 3 节就退出，
            # "零条红"会被当成"预检确实遮住了 advice"。跑不完一整轮不等于什么都没红。
            if this_total != total:
                print(f"  !! 这一轮只跑了 {this_total} 项，基线是 {total} 项 —— 分母变了，"
                      "「没红」的结论不成立")
                bad += 1
            if expect is None:
                if i15:
                    print(f"  !! B was caught at this layer after all -> {i15}"
                          " (so the prefilters do NOT fully shadow the advice)")
                    bad += 1
            elif i15 != [expect]:
                print(f"  !! MISMATCH: wanted exactly [{expect}], got [{', '.join(i15) or 'nothing'}]")
                bad += 1
            restore_all()

        print("\n=== restore: rebuild pristine, restart, re-run ===")
        build()
        fp = artifact_fingerprint()
        restart()
        (passed, last_total), fails, _ = run_e2e("restored")
        print(f"  artifact back to baseline bytes: {fp == base_fp}")
        print(f"  e2e = {passed}/{last_total}  (baseline {total})  failures={fails or '(none)'}")
        if fp != base_fp or passed != total or last_total != total:
            bad += 1
        for f in FILES:
            if f.read_bytes() != originals[f]:
                print(f"  !! NOT RESTORED: {f}")
                bad += 1
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("\nrestored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))
        print(f"serving pid(s) now: {lc_pids()}  health={wait_health(10)}")
    print("RESULT: " + ("deployed-layer falsification done" if bad == 0 else f"{bad} problem(s)"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 裸 sys.exit(main()) 会让"只是 import 看一下"的调用方直接开跑战役
    sys.exit(main())
