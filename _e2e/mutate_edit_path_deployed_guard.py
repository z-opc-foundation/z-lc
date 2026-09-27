#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #45/#46 在**部署层**的注入自证 —— 打的是 18090 上真正跑着的那支 jar。

为什么这一层还要再来一遍: 契约层 (`LcHttpContractTest`) 量的是 MockMvc 里的服务装配,
而 `[15r]`/`[15s]` 那些探针量的是"用户 curl 一下会不会看见"。同一句改动在两层都要有人作证,
否则"部署产物里到底有没有这个修复"就只是一句口头承诺 —— 上一轮就吃过一次:
`mvn install` 0.9 秒"成功", fat jar 里的 z-lc-core 还是 03:59 那份旧的,
所有 #46 探针对着 stale jar 打了一轮红。所以这里先做**字节级 marker 预检**, 预检不过就 FATAL。

六支变异，预期红集是**先写下再去量**的（对不上分两种走法: 预期漏了就按实测补上并把因果写进注释;
预期里那一支**从没红过**、且查明是探针自己看不见猎物 ⇒ 改的是探针不是预期，见 N_ONE 那段）:
  E1 复活的行不再 setDeleted(0)        -> 加回来的那一列带着新属性回不到清单
  E2 不发 field 级 DELETE 事件         -> 折叠/列表把删掉的两栏都端回来, 且 extra_col 拖垮整个列表
  E3 被移除的编码不墓碑化              -> 元数据凭空多一列, 连带一串
  E4 按编码对齐时不看大小写            -> 大小写变体插出第二行
  E5 更新路径不 copyFieldAttrs         -> 改名/改属性全都看不见
  E6 新编码永远不 insert               -> 后加的列根本没进元数据, provision 也就不再报缺它

用法: python3 _e2e/mutate_edit_path_deployed_guard.py   (需要 18090 由它自己重启)
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
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

SCHEMA = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
LOGS = Path.home() / ".cache/zlc45/deployed_logs"
BAK = Path.home() / ".cache/zlc45/deployed_bak"
BOOT_LOG = LOGS / "boot.log"
# 预检 marker: 修复没进 jar 就别开始量, 否则"谁红了"读出来的是 stale 产物的行为。
MARKERS = {("com/zifang/z/lc/core/schema/SchemaAdminBizService.class"): ["emitFieldRemoved", "copyFieldAttrs"]}

# 探针名一律从 _e2e/e2e_api_test.py 里抄, 不凭印象。
N_REVIVE = "加回来的那一列带着新属性回到清单里"
N_READD = "删掉又加回来的那一列能用（这一步只能复活原行，插新行会撞同码约束）"
N_FOLD = "折叠出的运行时定义和库里的清单一致（少了这一句, 上面删掉的栏在运行时还活着）"
N_NOSERVE = "运行时的每一次列表都不再端出被删掉的两栏（drop_me 的旧值在物理表里还留着，只有「不再引用」才算修好）"
N_OLDDATA = "改过一次大小写，那一列上的旧数据还在读得回来"
N_GONE = "被移除的那一列从清单里消失（留着就是元数据凭空多一列）"
N_RENAME = "保留下来的那一列按新定义改名（只认新增、旧列属性不动等于没改）"
N_NAME = "留着的那一列 fieldName 真的落库"
N_FAILED = "改完定义再 provision：没建出来的那一列要报 FAILED 并点名（#43 的回读还生效）"
# 这一支第一轮是**空跑**: 旧写法把 fieldCode 全 lower() 后再数名字，于是 E4 把 ref 那行
# 删掉、插一行 REF(id 26 -> 29) 出来，它读到的仍然是"一个名字 ref" ⇒ 永远绿。
# 现在量的是行 id: 原地改 ⇒ id 不动，删一行插一行 ⇒ id 跳号 (视图/引用认 id)。
N_ONE = "改大小写落在同一行上（行 id 原地不动，不是删一行插一行）"
N_IDENTITY = "编码身份保持用户先写进去的那个大小写"
TRACKED = {N_REVIVE, N_READD, N_FOLD, N_NOSERVE, N_OLDDATA, N_GONE, N_RENAME, N_NAME,
           N_FAILED, N_ONE, N_IDENTITY}

# 上一轮 N_ONE 就是这么混过去的: 它把 fieldCode 全 lower() 再数名字, 六支注入没有一支能让它红,
# 而"预期红集"里本来也没写它 —— 于是一整套流程走完，没人发现它其实什么都没在看。
# 现在补一道账: 受跟踪的探针若一整轮都没红过, 必须在这里点名"谁红它"(另一支闸/另一个战役),
# 没写理由 = problem。理由不能是"它不重要", 否则就不该放进 TRACKED。
WHY_NEVER_RED = {
    N_READD: "量的是「保存别被 uk_field_entity_code 打回 400」 —— 那是 #45 的病灶, "
              "由 mutate_duplicate_guard*.py 那几支战役负责红; 本战役六支都不碰唯一约束",
}

A_REVIVE = ('                row.setDeleted(0);\n'
            '                row.setUpdateTime(new Date());')
A_EMIT = ('                    // 运行时权威源是事件链，不是这张表: 只墓碑化不吭声，折叠出来的定义里那一栏永远还在。\n'
          '                    emitFieldRemoved(e.getTenantCode(), e.getAppCode(), e.getEntityCode(),\n'
          '                            row.getFieldCode());')
A_STALE = '                if (!kept.contains(stale.getKey()) && stale.getValue().getDeleted() == 0) {'
A_KEY = '                String key = fd.getFieldCode().toLowerCase(Locale.ROOT);'
A_COPY = '                copyFieldAttrs(fd, row);'
A_INSERT = ('                    fieldMapper.insert(row);\n'
            '                    continue;')

RUNS = [
    ("E1 复活的行不改回未删", [(SCHEMA, A_REVIVE, '                row.setUpdateTime(new Date());')],
     {N_REVIVE}),
    ("E2 不发 field 级 DELETE", [(SCHEMA, A_EMIT, '')],
     {N_FOLD, N_NOSERVE, N_OLDDATA}),
    ("E3 被移除的编码不墓碑化", [(SCHEMA, A_STALE, '                if (false) {')],
     # 不动墓碑那一步, 后面那支 emitFieldRemoved 也就没机会跑 (它在同一个 if 里) ——
     # 所以这一支同时是"#45 的移除"和"#46 的折叠"两层一起塌: 清单多一列、折叠多两列、
     # 列表被 extra_col 拖成 500。N_REVIVE 不在预期里: 复活那一行的 copyFieldAttrs 没被碰,
     # 名字照样落得下去。
     # N_ONE 也不在: 它量的是 ref 那一行的 id, E3 没有碰对齐键, 那一行始终是同一行。
     # N_IDENTITY 在: 名字是"整份清单恰好等于 ['ref']"的探针, 不墓碑化 ⇒ drop_me/extra_col
     # 全都活着留在清单里, 它按整份清单判红是正当的 (不是靠大小写那一支漏掉的猎物)。
     {N_GONE, N_RENAME, N_NAME, N_FOLD, N_NOSERVE, N_OLDDATA, N_IDENTITY}),
    ("E4 对齐编码时看大小写", [(SCHEMA, A_KEY, '                String key = fd.getFieldCode();')],
     {N_ONE, N_IDENTITY, N_FOLD}),
    ("E5 更新路径不 copyFieldAttrs", [(SCHEMA, A_COPY, '')],
     {N_NAME, N_REVIVE}),
    ("E6 新编码永远不 insert", [(SCHEMA, A_INSERT, '                    continue;')],
     {N_RENAME, N_NAME, N_FAILED}),
]


def sh(cmd, timeout=1800):
    return subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True, timeout=timeout)


def core_class_bytes(entry):
    with zipfile.ZipFile(JAR) as fat:
        lib = [n for n in fat.namelist() if n.startswith("BOOT-INF/lib/z-lc-core")]
        if not lib:
            raise RuntimeError("fat jar 里没有嵌套的 z-lc-core.jar: 零条目的指纹是常量, 等于没有")
        with zipfile.ZipFile(io.BytesIO(fat.read(lib[0]))) as inner:
            return inner.read(entry)


def fingerprint():
    parts = []
    with zipfile.ZipFile(JAR) as fat:
        libs = [n for n in sorted(fat.namelist()) if n.startswith("BOOT-INF/lib/z-lc-")]
        if not libs:
            raise RuntimeError("no nested z-lc-*.jar: 指纹覆盖零条目就是自欺")
        for lib in libs:
            with zipfile.ZipFile(io.BytesIO(fat.read(lib))) as inner:
                classes = [e for e in sorted(inner.namelist()) if e.endswith(".class")]
                parts += [f"{lib}!{e}:{hashlib.md5(inner.read(e)).hexdigest()}" for e in classes]
    return "|".join(parts)


def preflight_markers():
    missing = []
    for entry, marks in MARKERS.items():
        blob = core_class_bytes(entry).decode("latin1")
        missing += [entry + " 少了 " + m for m in marks if m not in blob]
    if missing:
        raise RuntimeError("部署产物里没有这次的修复(或 marker 改名了), 别再量: " + "; ".join(missing))


def build():
    # 必须**两步**: 只 install z-lc-core 进 m2, 再 clean 重打 admin 的 fat jar。
    # 单跑 `mvn install` 会把 admin 当"没变化"跳过 repackage —— 实测 stale 过一次。
    for cmd in (["mvn", "-o", "-B", "-q", "install", "-DskipTests", "-pl", "z-lc-core"],
                ["mvn", "-o", "-B", "clean", "install", "-DskipTests", "-pl", "z-lc-admin"]):
        p = sh(cmd)
        if p.returncode != 0 and ("BUILD SUCCESS" not in (p.stdout + p.stderr)):
            raise RuntimeError("build failed: %s\n%s" % (cmd, (p.stdout + p.stderr)[-1500:]))
    preflight_markers()


def lc_pids():
    return [int(x) for x in subprocess.run(["pgrep", "-f", "z-lc-admin-1.0.0-SNAPSHOT.jar"],
                                           capture_output=True, text=True).stdout.split()]


def restart():
    pids = lc_pids()
    if len(pids) > 1:
        raise RuntimeError(f"refusing to guess which JVM to stop: {pids}")
    if pids:
        subprocess.run(["kill", "-9", str(pids[0])])
        time.sleep(2)
    LOGS.mkdir(parents=True, exist_ok=True)
    log = open(BOOT_LOG, "ab")
    # start_new_session：不起新会话的话这个 jar 属于本量具的进程组，量具一退出它跟着收 SIGTERM（缺陷 #74）
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log, start_new_session=True)
    deadline = time.time() + 420
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(HEALTH, timeout=3) as r:
                if b'"status":"UP"' in r.read():
                    return
        except Exception:  # noqa: BLE001
            pass
        time.sleep(2)
    raise RuntimeError("server did not come up; see %s" % BOOT_LOG)


def run_e2e(label):
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    LOGS.mkdir(parents=True, exist_ok=True)
    (LOGS / f"e2e_{label}.log").write_text(p.stdout + p.stderr)
    m = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    reds = {l[8:].split("   <<")[0] for l in p.stdout.splitlines() if l.startswith("  FAIL  ")}
    if not m:
        # 套件连汇总都没打出来 = 它自己崩了。崩在注入下是** findings，不是噪声**:
        # 说明某条探针假设"这一步一定读得动"。把红集带回去, 让这一支照样记账。
        crash = [l for l in (p.stdout + p.stderr).splitlines() if l.startswith(("Traceback", "  File", "IndexError", "KeyError", "TypeError"))]
        return (None, None), reds, "套件崩溃(没有汇总行): %s | 崩前红=%s" % (
            " / ".join(crash[-3:]) or p.stdout.splitlines()[-1], sorted(reds))
    return (int(m.group(1)), int(m.group(2))), reds, None


def main():
    acquire_lock("mutate_edit_path_deployed_guard.py")
    try:
        return run_all()
    finally:
        release_lock()


def run_all():
    # 记账表本身也要过一遍: 名字写错(或被改名的探针留下孤儿条目)时, 那条豁免就什么也不豁免。
    orphans = sorted(set(WHY_NEVER_RED) - TRACKED)
    if orphans:
        raise RuntimeError("WHY_NEVER_RED 里的名字不在 TRACKED 里(拼错/探针改名?): %s" % orphans)
    stamp = time.strftime("%m%d-%H%M%S")
    run_dir = BAK / f"run-{os.getpid()}-{stamp}"
    run_dir.mkdir(parents=True, exist_ok=False)
    original = SCHEMA.read_bytes()
    (run_dir / "SchemaAdminBizService.java.orig").write_bytes(original)
    if (run_dir / "SchemaAdminBizService.java.orig").read_bytes() != original:
        raise RuntimeError("backup did not stick")
    base_fp = None
    problems = []
    ever_red = set()
    try:
        build()
        base_fp = fingerprint()
        restart()
        parsed, reds, crash = run_e2e("baseline")
        print(f"基线: {parsed[0]}/{parsed[1]} passed, 红={sorted(reds)}")
        if crash:
            problems.append("基线就崩了: " + crash)
            return 2
        if reds:
            problems.append(f"基线不干净: {sorted(reds)}")
            return 2

        for tag, edits, expected in RUNS:
            text = SCHEMA.read_text(encoding="utf-8")
            for _, old, _ in edits:
                if text.count(old) != 1:
                    raise RuntimeError(f"{tag}: 锚点出现 {text.count(old)} 次(要 1 次), 不敢改")
            for rel, old, new in edits:
                text = text.replace(old, new)
            SCHEMA.write_text(text, encoding="utf-8")
            try:
                build()
                restart()
                parsed2, reds2, crash2 = run_e2e(tag.split(" ")[0])
            finally:
                SCHEMA.write_bytes(original)
            mine = reds2 & TRACKED
            ever_red |= mine
            print(f"{tag}: {parsed2[0]}/{parsed2[1]} passed\n    预期红: {sorted(expected)}"
                  f"\n    实测红(受跟踪的): {sorted(mine)}\n    其它红: {sorted(reds2 - TRACKED)}")
            if crash2:
                problems.append(f"{tag}: {crash2}")
            missing = sorted(expected - mine)
            extra = sorted(mine - expected)
            if missing:
                problems.append(f"{tag}: 注入没被捉住 {missing}")
            if extra:
                problems.append(f"{tag}: 红了预期之外 {extra}")
            if parsed2[1] is not None and parsed2[1] != parsed[1]:
                problems.append(f"{tag}: 探针分母从 {parsed[1]} 变成 {parsed2[1]}")
            if sorted(reds2 - TRACKED):
                print("    (注: 未跟踪的红按因果记账, 不默认豁免)")
            fp2 = fingerprint()
            if fp2 == base_fp:
                problems.append(f"{tag}: 指纹没变 —— 这一支根本没被编进产物, 实测的红不算它的成绩")

        never = sorted(TRACKED - ever_red - set(WHY_NEVER_RED))
        print("一整轮没红过、也没写明由谁负责红的受跟踪探针: %s" % never)
        for name in never:
            problems.append(f"探针空跑: {name} 六支注入没有一支能让它红, 也没写 WHY_NEVER_RED")

        if SCHEMA.read_bytes() != original:
            SCHEMA.write_bytes(original)
            problems.append("还原时发现字节不同, 已按备份强行还原")
        build()
        if fingerprint() != base_fp:
            problems.append("还原后指纹与基线不一致")
        restart()
        parsed3, reds3, crash3 = run_e2e("restored")
        print(f"还原后复测: {parsed3[0]}/{parsed3[1]} passed, 红={sorted(reds3)}")
        if reds3 or crash3:
            problems.append(f"还原后仍有红/崩溃: {sorted(reds3)} {crash3 or ''}")
    finally:
        SCHEMA.write_bytes(original)

    if problems:
        print("RESULT: %d problem(s)" % len(problems))
        for p in problems:
            print("  - " + p)
        return 1
    print("RESULT: 部署层六支注入逐支按预期点名, 产物已还原到基线指纹")
    return 0


if __name__ == "__main__":
    sys.exit(main())
