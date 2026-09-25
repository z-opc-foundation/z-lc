#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #46 的注入自证（「补测试要同轮注入缺陷自证」）。

改动有两处，各自钉在两层:
  A) SchemaAdminBizService.updateEntity —— 一栏被墓碑化时补一条 field 级 DELETE 事件;
  B) EventReplayService.replay —— 整实体 DELETE（payload 没有 fieldCode）把实体从折叠里摘掉,
     并且不再在分发之前 computeIfAbsent 立空桩。

预期红集是**先写下再去量**的:
  M1 撤掉 emitFieldRemoved 的调用          -> 只红 web 层第一例（drop_me 还端在列表里）
  M2 撤掉整实体摘除分支                    -> 红 core 两例 + web 第二例（删掉的实体还供记录 / 凭空立桩）
  M3 摘除分支不看 fieldCode（任何 DELETE 都 wipes 整实体）
                                          -> 只红 core 的 replayDeleteShouldRemoveField。
     注意 M3 在 HTTP 流程里是**等价变异**: updateEntity 紧接着会发一条带全量字段的 UPDATE,
     把整实体抹掉后又原样重建 —— 所以这一支只能由折叠层的单测捉住, 这正是"词汇表"(DELETE 带不带
     fieldCode) 需要单独钉一层的原因。

用法: python3 _e2e/mutate_replay_guard.py
日志与备份一律在 ~/.cache（/tmp 会被别的会话扫掉）。
"""
import hashlib
import os
import shutil
import subprocess
import sys
import time

REPO = "/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc"
# 与前端及 provision 那两支注入脚本共用同一把**按仓库**的锁：这一支改的
# `SchemaAdminBizService.java` 正是它们也在就地改写的文件，A 的"按字节还原"会把 B 正在
# 判定的那份源码换掉 —— 那既能让 B 假绿（注入被悄悄抹掉），也能让 B 报出一条不属于它的红。
sys.path.insert(0, os.path.join(REPO, "z-lc-admin-ui", "e2e"))
import _mutlock  # noqa: E402
SVC = "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
RPY = "z-lc-core/src/main/java/com/zifang/z/lc/core/event/EventReplayService.java"
CACHE = os.path.expanduser("~/.cache/zlc_replay_guard")

# 只认这些**具名**的红；其余的按"预期之外"记账。
NAMED = [
    "entityDeleteWithoutFieldCodeShouldDropTheWholeEntityFromTheFold",
    "deletingAnUnknownEntityShouldNotConjureAStub",
    "replayDeleteShouldRemoveField",
    "removedFieldBecomesInvisibleToRuntimeReads",
    "deletedEntityStopsServingRecords",
    "savingAnEntityThatAlreadyHasFieldsSucceeds",
    "reAddingADroppedFieldRevivesItsRowInsteadOfInsertingAnother",
    "fieldCodeCaseVariantStaysOneColumn",
]

SUITES = [
    ("core", ["mvn", "-o", "-B", "test", "-pl", "z-lc-core",
              "-Dtest=EventReplayServiceTest", "-Dsurefire.failIfNoSpecifiedTests=false"]),
    ("web", ["mvn", "-o", "-B", "test", "-pl", "z-lc-web",
             "-Dtest=com.zifang.z.lc.web.it.LcHttpContractTest"]),
]


def sh(cmd, cwd=REPO, timeout=1200):
    return subprocess.run(cmd, cwd=cwd, shell=False, capture_output=True, text=True, timeout=timeout)


def read(rel):
    with open(os.path.join(REPO, rel), encoding="utf-8") as f:
        return f.read()


def write(rel, text):
    with open(os.path.join(REPO, rel), "w", encoding="utf-8") as f:
        f.write(text)


def digest(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def run_suites():
    """跑两层，返回 (具名红集合, 每层汇总行, 原始输出)。"""
    reds, summaries, raw = set(), [], ""
    build_failed = False
    for tag, cmd in SUITES:
        r = sh(cmd)
        out = r.stdout + r.stderr
        raw += "\n===== %s =====\n" % tag + out
        lines = [l for l in out.splitlines() if "Tests run" in l and "in com.zifang" in l]
        summaries.append("%s: %s" % (tag, lines[-1].strip() if lines else "(没有汇总行)"))
        if r.returncode != 0 and not lines:
            build_failed = True
            summaries[-1] += "  << 这一层没跑起来(rc=%d)" % r.returncode
        for line in out.splitlines():
            if "<<<" in line:
                for name in NAMED:
                    if name in line:
                        reds.add(name)
    return reds, summaries, raw, build_failed


def build_core():
    b = sh(["mvn", "-o", "-B", "-q", "install", "-DskipTests", "-pl", "z-lc-core"])
    return b.returncode == 0, (b.stdout + b.stderr)[-800:]


MUTANTS = [
    ("M1 撤掉 field 级 DELETE（回到「只墓碑化、不吭声」）",
     [(SVC, """                    fieldMapper.updateById(row);
                    // 运行时权威源是事件链，不是这张表: 只墓碑化不吭声，折叠出来的定义里那一栏永远还在。
                    emitFieldRemoved(e.getTenantCode(), e.getAppCode(), e.getEntityCode(),
                            row.getFieldCode());""",
       """                    fieldMapper.updateById(row);""")],
     {"removedFieldBecomesInvisibleToRuntimeReads"}),
    ("M2 撤掉整实体摘除分支（回到「DELETE 是空操作」）",
     [(RPY, """                if ("DELETE".equals(type) && !data.hasNonNull("fieldCode")) {
                    byEntity.remove(entityCode);
                    continue;
                }""", """                if (false) {
                    continue;
                }""")],
     {"entityDeleteWithoutFieldCodeShouldDropTheWholeEntityFromTheFold",
      "deletingAnUnknownEntityShouldNotConjureAStub",
      "deletedEntityStopsServingRecords"}),
    ("M3 摘除分支不看 fieldCode（任何 DELETE 都抹掉整实体）",
     [(RPY, """                if ("DELETE".equals(type) && !data.hasNonNull("fieldCode")) {""",
       """                if ("DELETE".equals(type)) {""")],
     {"replayDeleteShouldRemoveField"}),
]


def main():
    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = os.path.join(CACHE, stamp)
    os.makedirs(backup, exist_ok=True)
    originals = {}
    for rel in {p for _, edits, _ in MUTANTS for p, _, _ in edits}:
        originals[rel] = read(rel)
        shutil.copyfile(os.path.join(REPO, rel), os.path.join(backup, rel.replace("/", "__")))
    baseline_digest = {rel: digest(os.path.join(REPO, rel)) for rel in originals}
    log = open(os.path.join(backup, "guard.log"), "w", encoding="utf-8", buffering=1)

    def out(msg):
        print(msg)
        log.write(msg + "\n")

    out("=== 缺陷#46 注入自证 run=%s" % stamp)

    ok, tail = build_core()
    if not ok:
        out("FATAL 基线构建没通过，没脸谈变异: " + tail)
        return 2
    reds, summaries, _, broken = run_suites()
    for s in summaries:
        out("  " + s)
    out("基线: 具名红=%s" % sorted(reds))
    if reds or broken:
        out("FATAL 基线不干净（%s），后面的「谁红了」没有参照。" % sorted(reds))
        return 2

    problems = []
    for tag, edits, expected in MUTANTS:
        for rel, old, new in edits:
            text = read(rel)
            if text.count(old) != 1:
                out("FATAL %s: 锚点在 %s 里出现 %d 次（要 1 次），不敢改。" % (tag, rel, text.count(old)))
                return 2
            write(rel, text.replace(old, new))
        ok, tail = build_core()
        if not ok:
            out("FATAL %s 编译就没过: %s" % (tag, tail))
            for rel, old, new in edits:
                write(rel, read(rel).replace(new, old))
            return 2
        reds, summaries, raw, broken = run_suites()
        out("%s" % tag)
        for s in summaries:
            out("    " + s)
        out("    预期红: %s" % sorted(expected))
        out("    实测红: %s%s" % (sorted(reds), "（这一层没跑起来）" if broken else ""))
        missing = sorted(expected - reds)
        extra = sorted(reds - expected)
        if missing:
            problems.append("%s: 这几支注入没被捉住 %s" % (tag, missing))
            out("    !! 逃过: %s" % missing)
        if extra:
            problems.append("%s: 红了预期之外的 %s（预期按实测改，断言不许改软）" % (tag, extra))
            out("    !! 预期之外: %s" % extra)
        with open(os.path.join(backup, tag.split(" ")[0] + ".log"), "w", encoding="utf-8") as f:
            f.write(raw[-300000:])
        for rel, old, new in edits:
            write(rel, read(rel).replace(new, old))

    # 还原必须是字节级相同，否则下一轮量的不是同一棵树。
    for rel in originals:
        now = digest(os.path.join(REPO, rel))
        if now != baseline_digest[rel]:
            write(rel, originals[rel])
            now = digest(os.path.join(REPO, rel))
        same = now == baseline_digest[rel]
        out("还原 %s: %s" % (rel, "字节相同" if same else "!! 不同"))
        if not same:
            problems.append("还原失败 " + rel)

    ok, _ = build_core()
    reds, summaries, _, broken = run_suites()
    out("还原后复测: 构建 %s / 具名红=%s" % ("过" if ok else "不过", sorted(reds)))
    for s in summaries:
        out("  " + s)
    if not ok or broken or reds:
        problems.append("还原后仍不干净: red=%s" % sorted(reds))

    if problems:
        out("RESULT: %d problem(s)" % len(problems))
        for p in problems:
            out("  - " + p)
        return 1
    out("RESULT: 缺陷#46 的三支注入逐支按预期点名，产物已还原到基线字节")
    return 0


if __name__ == "__main__":
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
