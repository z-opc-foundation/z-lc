#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #47 的 java 单测层注入自证（「补测试要同轮注入缺陷自证」）。

#47 的修复分两半：读侧把 `Column "x" not found` 换成 400 并指向 provision（由部署层那几支
战役负责），写侧让 provision **真的能把缺的那几列补上**（`ALTER TABLE ADD COLUMN`，
状态 `ALTERED`），而且补之前要先问"这张表归谁"。本战役钉的是写侧的纯函数部分：

  columnClause            —— CREATE 与 ALTER 共用同一份列定义
  buildAddColumnDdl       —— 只 ADD，不 MODIFY/DROP/CHANGE
  otherLiveEntityOnSameTable —— 占表判定：谓词齐不齐（deleted / tenant_code）、自己算不算、
                                 表名大小写算不算

为什么单开一层：`provisionOne` 要真库才能跑（那一层由 mutate_provision_deployed_guard.py 打），
但上面三个是**纯字符串/纯查询**判定，在单测层就能钉死；而且"补出来的列和建出来的列不一致"
这种缺陷在集成层根本看不出来 —— 表建成功了、列也在、只是宽度不一样，要等下一次数据写歪才炸。

判据同 `mutate_replay_guard.py`：只认 surefire 输出里**具名测试的红**；编译不过 = 崩红，不算证据；
一整轮没红过的受跟踪测试按空跑记账。

⚠ SUITE 里那个 `clean` 不是浪费时间，是这一层的**前提**（本轮实测）：`-pl z-lc-core` 的
z-lc-common 是从 ~/.m2 解析的，改了 `ProvisionReport` 而不 install 的话 m2 里躺着旧 jar ——
基线那一次 javac 因为"源码没变"直接跳过，于是"基线 30 条全绿"是假的，而七支注入每支都强制重编译，
全部红成 `cannot find symbol: ALTERED`。先 `mvn -o -B install -DskipTests -pl z-lc-common,z-lc-core`，
再带 `clean` 跑，基线才真的量过一次全量编译。

用法: python3 _e2e/mutate_provision_reconcile_guard.py
      python3 _e2e/mutate_provision_reconcile_guard.py --only C1 C3
日志与备份一律在 ~/.cache（/tmp 会被别的会话扫掉）。
"""
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

SVC = "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
CACHE = Path(os.path.expanduser("~/.cache/zlc47/reconcile_guard"))
LOGS = CACHE / "logs"
BAK = CACHE / "bak"

SUITE = ["mvn", "-o", "-B", "clean", "test", "-pl", "z-lc-core",
         "-Dtest=SchemaAdminBizServiceTest", "-Dsurefire.failIfNoSpecifiedTests=false"]

# 本战役要钉的具名测试（分母）。每条都必须在至少一支注入里红过，否则按空跑记账。
T_PARITY = "addColumnShouldUseTheSameClauseAsCreateTable"
T_ONLY_ADD = "addColumnShouldOnlyAddAndNeverRewriteOrDrop"
T_REFUSE = "reconcileShouldRefuseTableHeldByAnotherLiveEntity"
T_OWN = "ownRowShouldNotCountAsSquatter"
T_SOFT = "softDeletedEntityShouldNotBlockReconcile"
T_TENANT = "otherTenantShouldNotBlockReconcile"
T_CASE = "squatterMatchShouldIgnoreTableNameCase"
T_FIXTURE = "entityRowFixtureReallyAppliesTheWrapperPredicates"

TRACKED = {T_PARITY, T_ONLY_ADD, T_REFUSE, T_OWN, T_SOFT, T_TENANT, T_CASE, T_FIXTURE}

# 一支注入都没能让它红、但**确实有别的层负责红它**的，写在这里点名；不写就等于按空跑处理。
WHY_NEVER_RED = {}

SQUATTER_IF = ('            if (held != null && wanted.equals(held.toLowerCase(Locale.ROOT))\n'
               '                    && (def.getId() == null || !def.getId().equals(other.getId()))) {')

MUTANTS = [
    (
        "C1 补列自己写一份列定义（丢掉 NOT NULL/默认值/注释）",
        [(SVC,
          '    private String buildAddColumnDdl(String tableName, FieldDefDTO f) {\n'
          '        return "ALTER TABLE `" + tableName + "` ADD COLUMN " + columnClause(f);\n'
          '    }',
          '    private String buildAddColumnDdl(String tableName, FieldDefDTO f) {\n'
          '        return "ALTER TABLE `" + tableName + "` ADD COLUMN `" + f.getFieldCode() + "` "\n'
          '                + com.zifang.z.lc.core.executor.DynamicSqlBuilder.jdbcType(\n'
          '                        f.getFieldType(), f.getFieldLength(), f.getScale());\n'
          '    }')],
        {T_PARITY},
    ),
    (
        "C2 把 ADD COLUMN 写成 MODIFY COLUMN（补列顺手改别人的列）",
        [(SVC,
          '        return "ALTER TABLE `" + tableName + "` ADD COLUMN " + columnClause(f);',
          '        return "ALTER TABLE `" + tableName + "` MODIFY COLUMN " + columnClause(f);')],
        # 两句都红不是串线：语句换了关键字之后，"ADD COLUMN 后面那段"这个取法本身就不成立了，
        # 而它正是第一条断言用来比对的两段文本之一。
        {T_PARITY, T_ONLY_ADD},
    ),
    (
        "C3 占表判定漏加 deleted 谓词（软删墓碑把表永久占死）",
        [(SVC,
          '                .eq("tenant_code", def.getTenantCode())\n'
          '                .eq("deleted", 0))) {',
          '                .eq("tenant_code", def.getTenantCode()))) {')],
        # 夹具那条也红是**它的工作**：它存在的理由就是"替身要是没按谓词过滤，下面全都不算数"。
        {T_SOFT, T_FIXTURE},
    ),
    (
        "C4 占表判定漏加 tenant_code 谓词（跨租户误判抢占）",
        [(SVC,
          '        for (EntityEntity other : entityMapper.selectList(new QueryWrapper<EntityEntity>()\n'
          '                .eq("tenant_code", def.getTenantCode())',
          '        for (EntityEntity other : entityMapper.selectList(new QueryWrapper<EntityEntity>()')],
        {T_TENANT, T_FIXTURE},
    ),
    (
        "C5 占表判定不排自己（每次 provision 把自己判死）",
        # 锚点必须带上第二行: `validateTableNameAvailable` 里有一行**前缀完全相同**的判断
        # （那边是 `...toLowerCase(Locale.ROOT))) {`），只写第一行会数出 2 次。
        [(SVC,
          SQUATTER_IF,
          '            if (held != null && wanted.equals(held.toLowerCase(Locale.ROOT))) {')],
        {T_OWN},
    ),
    (
        "C6 占表判定改成大小写敏感（MySQL 表名不分大小写，TBL 和 tbl 是同一张表）",
        [(SVC,
          SQUATTER_IF,
          '            if (held != null && wanted.equals(held)\n'
          '                    && (def.getId() == null || !def.getId().equals(other.getId()))) {')],
        {T_CASE},
    ),
    (
        "C7 占表判定整个反了（该拦的不拦、不该拦的全拦）",
        [(SVC,
          SQUATTER_IF,
          '            if (held != null && !wanted.equals(held.toLowerCase(Locale.ROOT))\n'
          '                    && (def.getId() == null || !def.getId().equals(other.getId()))) {')],
        {T_REFUSE, T_CASE, T_FIXTURE},
    ),
]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    (ROOT / rel).write_text(text, encoding="utf-8")


def digest(abs_path):
    h = hashlib.sha256()
    with open(abs_path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def run_suite(out):
    """跑一层，返回 (具名红集合, 汇总行, 是否崩掉, 用例分母)。"""
    proc = subprocess.run(SUITE, cwd=str(ROOT), shell=False, capture_output=True, text=True, timeout=1800)
    text = proc.stdout + proc.stderr
    out.write("\n===== mvn =====\n" + text)
    out.flush()
    lines = [l for l in text.splitlines() if "Tests run" in l and "in com.zifang" in l]
    summary = lines[-1].strip() if lines else "(没有汇总行)"
    broken = proc.returncode != 0 and not lines
    total = int(re.search(r"Tests run: (\d+)", summary).group(1)) if re.search(r"Tests run: (\d+)", summary) else 0
    reds = set()
    if not broken:
        # 只认 surefire 点名的那条测试。`Tests run: 30, Failures: 1` 只说明"有红"，
        # 不说明红在谁身上 —— 而判定要的是"这一支注入让**它声称要捉的那条**变红了吗"。
        for line in text.splitlines():
            if "<<<" in line or ("[ERROR]" in line and "SchemaAdminBizServiceTest." in line):
                for name in TRACKED:
                    if re.search(r"\b" + name + r"\b", line):
                        reds.add(name)
        counted = re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+)", text)
        if counted:
            total_fail = sum(int(a) + int(b) for _, a, b in counted[-1:])
            if total_fail and not reds:
                broken = True
                summary += "  << 有失败但没解析出具名测试, 按崩掉处理"
    return reds, summary, broken, proc.returncode, total


def main(argv):
    only = [a for a in argv if not a.startswith("--")] if "--only" in argv else []
    if "--only" in argv:
        wanted = set(argv[argv.index("--only") + 1:])
        runs = [m for m in MUTANTS if m[0].split()[0] in wanted]
        if len(runs) != len(wanted):
            print("!! --only 里有不存在的编号: %s（现有 %s）"
                  % (sorted(wanted - {r[0].split()[0] for r in runs}), [m[0].split()[0] for m in MUTANTS]))
            return 2
    else:
        runs = MUTANTS

    stamp = "%s-%s" % (os.getpid(), time.strftime("%H%M%S"))
    backup = BAK / ("run-" + stamp)
    backup.mkdir(parents=True, exist_ok=False)  # 绝不复用：复用会把上一轮的"原始源码"当参照
    LOGS.mkdir(parents=True, exist_ok=True)
    log_path = LOGS / ("guard-%s.log" % stamp)

    originals = {rel: read(rel) for _, edits, _ in MUTANTS for rel, _, _ in edits}
    for rel, text in originals.items():
        (backup / rel.replace("/", "__")).write_text(text, encoding="utf-8")
    baseline_digest = {rel: digest(ROOT / rel) for rel in originals}

    def out(msg):
        print(msg, flush=True)
        with open(log_path, "a", encoding="utf-8") as f:
            f.write(msg + "\n")

    with open(log_path, "w", encoding="utf-8") as f:
        f.write("# mutate_provision_reconcile_guard run=%s\n" % stamp)
    out("=== 缺陷#47 java 单测层注入自证 run=%s  日志=%s" % (stamp, log_path))

    # 预检：锚点先一次性数过（改一处话术往往同时挪好几支的锚点）。
    broken_anchors = []
    for label, edits, _ in runs:
        for rel, old, new in edits:
            if old == new:
                broken_anchors.append("%s: 注入是空操作" % label.split()[0])
            elif originals[rel].count(old) != 1:
                broken_anchors.append("%s: 锚点在 %s 里出现 %d 次（要 1 次）"
                                      % (label.split()[0], os.path.basename(rel), originals[rel].count(old)))
    if broken_anchors:
        out("FATAL 锚点不合格，一个源文件都没碰:")
        for line in broken_anchors:
            out("   " + line)
        return 2

    problems = []
    ever_red = set()
    try:
        with open(log_path, "a", encoding="utf-8") as f:
            reds, summary, broken, rc, baseline_n = run_suite(f)
        out("基线: %s" % summary)
        if reds or broken:
            out("FATAL 基线不干净（红=%s / 崩=%s），后面的「谁红了」没有参照。" % (sorted(reds), broken))
            return 2
        out("基线分母: %d 条用例" % baseline_n)

        for label, edits, expected in runs:
            tag = label.split()[0]
            out("\n=== %s ===" % label)
            for rel, old, new in edits:
                write(rel, read(rel).replace(old, new))
            try:
                with open(log_path, "a", encoding="utf-8") as f:
                    reds, summary, broken, rc, total = run_suite(f)
                out("    %s" % summary)
                out("    预期红: %s" % sorted(expected))
                out("    实测红: %s%s" % (sorted(reds), "（这一层没跑起来/崩红，不算证据）" if broken else ""))
                if broken:
                    problems.append("%s: 这一层崩了，注入结果作废 (rc=%d)" % (tag, rc))
                # 分母漂了就是"有用例没被跑到"，那些"没红"全部作废。
                if total != baseline_n:
                    problems.append("%s: 分母从 %d 变成 %d（用例被跑掉了？先怀疑量具）" % (tag, baseline_n, total))
                missing = sorted(expected - reds)
                extra = sorted(reds - expected)
                if missing:
                    problems.append("%s: 这几支注入没被捉住 %s" % (tag, missing))
                    out("    !! 逃过: %s" % missing)
                if extra:
                    problems.append("%s: 红了预期之外的 %s（预期按实测的因果改，断言不许改软）" % (tag, extra))
                    out("    !! 预期之外: %s" % extra)
                ever_red |= reds
            finally:
                for rel, _, _ in edits:
                    write(rel, originals[rel])

        # 还原必须回到**本轮读进去的那一份**（sha256 逐文件核，别只看测试绿）。
        for rel, want in baseline_digest.items():
            now = digest(ROOT / rel)
            if now != want:
                write(rel, originals[rel])
                now = digest(ROOT / rel)
            out("\n还原 %s: %s" % (rel, "字节相同" if now == want else "!! 不同"))
            if now != want:
                problems.append("还原失败 " + rel)

        with open(log_path, "a", encoding="utf-8") as f:
            reds, summary, broken, rc, total = run_suite(f)
        out("还原后复测: %s / 具名红=%s" % (summary, sorted(reds)))
        if broken or reds:
            problems.append("还原后仍不干净: red=%s broken=%s" % (sorted(reds), broken))

        never = sorted(TRACKED - ever_red - set(WHY_NEVER_RED))
        if never:
            problems.append("一整轮没红过、也没写明由谁负责红的受跟踪用例（＝空跑）: %s" % never)
        out("\n一整轮没红过、也没写明由谁负责红的受跟踪用例: %s" % never)
    finally:
        for rel, text in originals.items():
            write(rel, text)

    if only:
        out("(PARTIAL: 只跑了 --only 指定的那几支，空跑记账不成立)")

    if problems:
        out("RESULT: %d problem(s)" % len(problems))
        for p in problems:
            out("  - " + p)
        return 1
    out("RESULT: 缺陷#47 的 %d 支注入逐支按预期点名，产物已还原到基线字节" % len(runs))
    return 0


if __name__ == "__main__":
    acquire_lock(os.path.basename(__file__))
    try:
        sys.exit(main(sys.argv[1:]))
    finally:
        release_lock()
