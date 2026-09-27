#!/usr/bin/env python3
"""Falsify the `[15j] 字段编码撞引擎自建列` section of _e2e/e2e_api_test.py against the DEPLOYED fat jar.

Field-code guards have a unit net (SchemaAdminBizServiceTest) and a front-end net
(e2e/mutate_designer_field_code.py), but the thing that ships is the jar, and the jar is where the
HTTP envelope shape (200 + success:false + code:400 vs a real 400) is decided. This layer owns
exactly that question.

Each mutant re-breaks *one* guard site, so each maps to one derivable red set. The sets are built
here from the same strings the suite prints rather than copied by hand, because a hand-copied
expectation that is wrong reads as "MISMATCH" only if someone compares — and the whole point of
this file is that nobody has to.

What the run is expected to show, and why each part is worth the minute it costs:

  J1 createEntity's call removed   -> every create-path probe goes red, nothing else
  J2 case-insensitive compare      -> the two 大写 ID probes *plus* the "nothing landed" probe.
                                      First run predicted only the two, and the third red is not
                                      noise: with the compare made case-sensitive the `ID`
                                      submission is accepted, so an entity really does land — the
                                      probe that counts rows must go red for a red that is
                                      causally required, not be explained away afterwards. (The
                                      lowercase probes are deliberately insensitive, which is what
                                      makes this pair the only carrier of that branch.)
  J3 updateEntity's call removed   -> only the two update probes; that is why the update probe
                                      sits *last* in [15j]: placed before provision it would
                                      poison the fixture and 6 downstream checks would go red for
                                      one bug, which is a signal that cannot name its cause
  J4 buildCreateTableDdl's call    -> INVISIBLE here on purpose: both entry points already reject,
                                      so no HTTP path in the suite can hand the DDL builder a bad
                                      code. Saying so out loud is the point; J4's only net is
                                      SchemaAdminBizServiceTest, which this script therefore also
                                      runs (a deployed-layer "not caught" claim is worth nothing
                                      unless you can say where it *is* caught).
  J5 reverse mapper stops skipping -> the import probe + the three mapper probes. Note what the
                                      createEntity check adds: with J5, /table/import is rejected
                                      by the *entry point*, i.e. without the skip the whole
                                      "map a real table" feature dies. That is the pair the two
                                      guards form, and it is only visible if the skip is mutated
                                      on its own.
  J6 the skip stops leaving a trace-> exactly 1 probe (the description), because silently dropping
                                      a column and reporting it are separate guarantees
  J7 FIELD_CODE_RE loosened so a leading digit is legal -> only the `2bad` pair + the "nothing
                                      landed" probe. `我的字段` / `has space` still get rejected,
                                      which is what keeps this pair from being J1's clone.

Hard guards inherited from mutate_duplicate_guard_deployed.py, all of them earned:
  * the artifact is fingerprinted over *class file* digests inside the nested z-lc-*.jar entries
    (jar-level md5 differs for byte-identical sources, so a jar-level digest always "changes" and
    therefore guards nothing); an empty fingerprint is a hard error
  * the per-run denominator must equal the baseline's: a run that dies at section 3 has zero reds
    for a reason that has nothing to do with the guard
  * sources are restored from a byte-compare snapshot, and the pristine rebuild is itself re-run
  * this script restarts the 18090 JVM, so nothing else may be talking to it while it runs
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
# 和前端子脚本共用同一把按仓库的锁：这一支改的是 Java 源文件，另一支改的是 .ts/.tsx，
# 看似不打架，但两支同时改写源文件时，A 的字节还原会把 B 正在判定的那份换掉。
# 上一轮实测过一次这种假红。
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402
SCHEMA = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
MAPPER = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/mapper/DbTableMapperService.java"
# 备份放 ~/.cache: /tmp 会被同机其他会话扫空, 备份中途消失 = 还原失败
BAK = Path.home() / ".cache/zlc42/deployed_bak" / "tmp/fcguard_deployed_bak"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
UNIT_LOG = "/tmp/fcguard_unit.log"

# ---- the [15j] names, derived from the same constants the suite prints ------------------------
RESERVED = ["id", "tenant_code", "deleted", "create_time", "update_time"]
ILLEGAL = ["2bad", "我的字段", "has space", ""]

COLLIDE_REFUSED = [f"撞自建列 {c} 在 entity/create 就被拒(不是等建表才炸)" for c in RESERVED]
COLLIDE_MSG = [f"撞自建列 {c} 的文案列出保留列，且不带 SQL/物理表名细节" for c in RESERVED]
UPPER = ["大写 ID 同样被拒（撞名比较不分大小写）", "文案回显的是用户实际写进去的那个大小写"]
ILLEGAL_REFUSED = [f"非法列名 {c!r} 被拒且说的是列名规则" for c in ILLEGAL]
ILLEGAL_NAMED = [f"非法列名 {c!r} 的文案指名是哪一列（不是笼统一句失败）" for c in ILLEGAL]
NOTHING_LANDED = "上面那些被拒的提交一行都没落库（闸在写入之前，不是事后回滚）"
B2BAD_REFUSED, B2BAD_NAMED = ILLEGAL_REFUSED[0], ILLEGAL_NAMED[0]
# #43 之后 [15j] 的映射宿主换成了"没有活主人的表"（软删掉宿主实体再映射），标题里那句
# "刚建好的物理表"已经不是套件打印的那句了。名字对不上时这一支会被判成"没红"，
# 而实际是整套的预期集里少了一条 —— 必须抄套件的字面，不能凭印象。
IMPORT_OK = "POST /admin/db/table/import (逆向映射一张没有活主人的物理表)"
MAP_SKIP = "逆向映射跳过引擎自建列（一律拒绝会把 /table/import 打死：真表的 id/deleted 是合法物理列）"
MAP_ALL = "用户列一列不少地映射回来"
MAP_DESC = "跳过的清单写进 description，不是静默丢掉"
UPDATE_REFUSED = "updateEntity 是第二个写入口：改成撞名列同样被拒"
UPDATE_UNTOUCHED = "被拒的 update 一行都没改（字段还是原来那三列、原顺序）"

CREATE_PATH = COLLIDE_REFUSED + COLLIDE_MSG + UPPER + ILLEGAL_REFUSED + ILLEGAL_NAMED + [NOTHING_LANDED]

# ---- anchors (each must occur exactly once; the script refuses to guess) -----------------------
CREATE_ANCHOR = ('        validateFieldCodes(req.getFields());\n\n'
                 '        EntityEntity entity = new EntityEntity();')
CREATE_REPL = '        EntityEntity entity = new EntityEntity();'

UPDATE_ANCHOR = ('        // 同样在任何写入之前: 全量替换会先把旧字段软删掉，坏编码进来后再想退回去就没退路了。\n'
                 '        validateFieldCodes(req.getFields());\n')
UPDATE_REPL = ''

DDL_ANCHOR = ('        // 这里以前对撞名/非法编码是 `continue` 静默跳过: 建表照样返回"成功", 少几列没人知道。\n'
              '        validateFieldCodes(def.getFields());\n')
DDL_REPL = ''

CASE_ANCHOR = 'SYSTEM_COLUMN_CODES.contains(code.toLowerCase(Locale.ROOT))'
CASE_REPL = 'SYSTEM_COLUMN_CODES.contains(code)'

REGEX_ANCHOR = 'Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$")'
REGEX_REPL = 'Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_]*$")'

SKIP_ANCHOR = ('                if (colName != null\n'
               '                        && SchemaAdminBizService.SYSTEM_COLUMN_CODES'
               '.contains(colName.toLowerCase(Locale.ROOT))) {')
SKIP_REPL = '                if (false) {'

DESC_ANCHOR = ('            entity.setDescription("逆向映射自: " + tableName\n'
               '                    + "; 已跳过引擎自建列: " + String.join(", ", skippedSystemColumns));')
DESC_REPL = '            entity.setDescription("逆向映射自: " + tableName);'

# tag -> (edits, expected [15j] reds (None = must stay invisible), expected unit reds (None = skip))
RUNS = [
    ("J1", [(SCHEMA, CREATE_ANCHOR, CREATE_REPL)], CREATE_PATH,
     ["createEntityShouldRejectEveryEngineOwnedColumnCode",
      "createEntityShouldRejectSystemColumnCodeIgnoringCase",
      "createEntityShouldRejectFieldCodeThatIsNotALegalColumnName"]),
    ("J2", [(SCHEMA, CASE_ANCHOR, CASE_REPL)], UPPER + [NOTHING_LANDED],
     ["createEntityShouldRejectSystemColumnCodeIgnoringCase"]),
    ("J3", [(SCHEMA, UPDATE_ANCHOR, UPDATE_REPL)], [UPDATE_REFUSED, UPDATE_UNTOUCHED],
     ["updateEntityShouldRejectCollidingCodeBeforeAnyWrite"]),
    ("J4", [(SCHEMA, DDL_ANCHOR, DDL_REPL)], [],
     ["buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns"]),
    ("J5", [(MAPPER, SKIP_ANCHOR, SKIP_REPL)], [IMPORT_OK, MAP_SKIP, MAP_ALL, MAP_DESC], None),
    ("J6", [(MAPPER, DESC_ANCHOR, DESC_REPL)], [MAP_DESC], None),
    ("J7", [(SCHEMA, REGEX_ANCHOR, REGEX_REPL)], [B2BAD_REFUSED, B2BAD_NAMED, NOTHING_LANDED],
     ["createEntityShouldRejectFieldCodeThatIsNotALegalColumnName",
      "buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns"]),
]

FILES = sorted({SCHEMA, MAPPER}, key=lambda p: str(p))
DDL_UNIT_METHOD = "buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns"


def sh(cmd, cwd=ROOT, timeout=1200):
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


# 本机在同一时间还跑着别人的 mvn/javac：19:20 那一轮 J1 的 restart 用 150s 等不到 UP 就抛了
# （其实 8 分钟后起来过，health=UP），一轮反证就这么废掉。等待拉长不是放宽判据 —— 判据是
# "这一轮注入有没有被抓到"，等久一点只让证据落下来；真等不到 420s 才叫没起来。
def wait_health(timeout=420):
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
    # start_new_session：不起新会话的话这个 jar 属于本量具的进程组，量具一退出它跟着收 SIGTERM（缺陷 #74）
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log, start_new_session=True)
    if not wait_health():
        # 抛之前把现场留下：谁在听 18090、boot 日志最后几行是什么。
        # 上一轮只留了一句 "did not come up"，于是"起不来"和"起来了但健康检查打的是另一个地址"
        # 两种完全不同的成因在日志里长得一模一样。
        listening = subprocess.run(["lsof", "-nP", "-iTCP:18090", "-sTCP:LISTEN"],
                                   capture_output=True, text=True).stdout.strip()
        tail = subprocess.run(["tail", "-15", "/tmp/lc_deployed_mut_boot.log"],
                              capture_output=True, text=True).stdout.strip()
        raise RuntimeError(
            "server did not come up; see /tmp/lc_deployed_mut_boot.log"
            f"\n   18090 listeners: {listening or '(none)'}"
            f"\n   pids alive: {lc_pids()}"
            f"\n   boot log tail:\n{tail}")


def build():
    p = sh(["mvn", "-o", "-B", "install", "-DskipTests", "-pl", "z-lc-core,z-lc-admin", "-am"])
    if "BUILD SUCCESS" not in (p.stdout + p.stderr):
        raise RuntimeError("jar build failed, tail of log:\n"
                           + "\n".join((p.stdout + p.stderr).splitlines()[-25:]))


def run_e2e(label):
    """Returns ((passed, total), all_fail_names, [15i/15j]-agnostic section split).

    Section is taken from the harness's own `[NNx]` banner lines, not keyword matching, so a
    failure in some other section can never be laundered into this section's tally.
    """
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    Path(f"/tmp/e2e_fcguard_{label}.log").write_text(p.stdout + p.stderr)
    summary = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    section, fails, mine = None, [], []
    for line in p.stdout.splitlines():
        m = re.match(r"^\[(\d+\w*)\]", line)
        if m:
            section = m.group(1)
            continue
        if line.startswith("  FAIL  "):
            name = line[8:].split("   <<")[0]
            fails.append(name)
            if section == "15j":
                mine.append(name)
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    return parsed, fails, mine


def run_unit():
    """(total, red-method-names) for SchemaAdminBizServiceTest, or (None, None) if it never ran."""
    p = sh(["mvn", "-o", "-B", "test", "-pl", "z-lc-core", "-Dtest=SchemaAdminBizServiceTest"])
    out = p.stdout + p.stderr
    Path(UNIT_LOG).write_text(out)
    totals = [int(x) for x in re.findall(r"Tests run: (\d+), Failures: \d+, Errors: \d+"
                                         r"[^\n]*in com\.zifang\.z\.lc\.core\.schema", out)]
    if not totals:
        raise RuntimeError("surefire printed no summary for SchemaAdminBizServiceTest; tail:\n"
                           + "\n".join(out.splitlines()[-20:]))
    red = sorted({m for line in out.splitlines() if "<<<" in line
                  for m in re.findall(r"SchemaAdminBizServiceTest\.([A-Za-z0-9_]+)", line)})
    return sum(totals), red


def main():
    acquire_lock("mutate_field_code_deployed_guard.py")
    try:
        return run_all()
    finally:
        release_lock()


def run_all():
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
        unit_total, unit_red = run_unit()
        print(f"  unit baseline SchemaAdminBizServiceTest: {unit_total} tests, red={unit_red}")
        if unit_red:
            print("UNIT BASELINE NOT GREEN, aborting")
            return 1

        for tag, edits, expect, unit_expect in RUNS:
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
            (passed, this_total), fails, mine = run_e2e(f"mut{tag}")
            print(f"\n[{tag}] expect red on: {expect if expect is not None else 'nothing here'}")
            print(f"  artifact fp = {fp_id(fp)}  changed={changed}")
            print(f"  e2e = {passed}/{this_total}  (baseline denominator {total})")
            print(f"  [15j] failures ({len(mine)}): {mine or '(none)'}")
            print(f"  failures outside [15j]: {[f for f in fails if f not in mine] or '(none)'}")
            if not changed:
                print("  !! artifact did not change -> this run proves nothing")
                bad += 1
            if this_total != total:
                print(f"  !! 这一轮只跑了 {this_total} 项，基线是 {total} 项 —— 分母变了，"
                      "「没红」的结论不成立")
                bad += 1
            if sorted(mine) != sorted(expect):
                print(f"  !! MISMATCH: wanted exactly {expect}, got {mine}")
                bad += 1
            if expect == []:
                print(f"  这一支在本层不可见是预期内的：J4 的唯一网是 "
                      f"SchemaAdminBizServiceTest.{DDL_UNIT_METHOD}")
            if unit_expect is not None:
                u_total, u_red = run_unit()
                print(f"  unit = {u_total} tests, red={u_red}")
                if u_total != unit_total:
                    print(f"  !! 单测分母变了（{unit_total} → {u_total}），「红了几条」无从谈起")
                    bad += 1
                if u_red != sorted(unit_expect):
                    print(f"  !! UNIT MISMATCH: wanted exactly {sorted(unit_expect)}, got {u_red}")
                    bad += 1
            restore_all()

        print("\n=== restore: rebuild pristine, restart, re-run ===")
        build()
        fp = artifact_fingerprint()
        restart()
        (passed, last_total), fails, _ = run_e2e("restored")
        u_total, u_red = run_unit()
        print(f"  artifact back to baseline bytes: {fp == base_fp}")
        print(f"  e2e = {passed}/{last_total}  (baseline {total})  failures={fails or '(none)'}")
        print(f"  unit = {u_total} tests, red={u_red or '(none)'}")
        if fp != base_fp or passed != total or last_total != total or u_red or u_total != unit_total:
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
