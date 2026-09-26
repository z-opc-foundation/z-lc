#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #51 + #54 的注入自证（「补测试要同轮注入缺陷自证」）。

250 上真 MySQL 8 撞出的四支产品缺陷，各自的修法与判红面（全都只在真库上才撞得到；dev 的 H2 一支都不红）:

  A) 校对 (M1–M7): buildCreateTableDdl 只写 `DEFAULT CHARSET=utf8mb4`，MySQL 8 于是取该 charset 的
     **默认校对** utf8mb4_0900_ai_ci，而元数据层 15 张 z_lc_* 沿用库默认 utf8mb4_general_ci
     (实测 information_schema.tables)。跨表字符串比较当场 500 (Illegal mix of collations)，
     一轮 API 门禁 28 条红里 8 条是这个。修法: 问出元数据层那一套并钉进建表语句；已经错的那张
     表在 provision 报告里判 FAILED 并给出 CONVERT TO 的修法。
  B) 快照形态 (M8–M12): DATETIME 列 H2 给 java.sql.Timestamp、MySQL 8 的 Connector/J 给
     java.time.LocalDateTime —— 修复前只认 Date/String 两支，于是日志里存进带 T 的 ISO 串，
     撤销/重做当场 "Field requires date"。修法: 写快照时归一 (persistable)，读回旧日志时也归一
     (applyInverted/applyForward 都过 persistable)。
  C) 补列的库间分叉 (N1–N5): 给**有行**的表补"必填且无默认值"的列，H2 当场拒，mysql:8.0.26
     (@@sql_mode 含 STRICT_TRANS_TABLES) 却**接受**并把已有行那一栏静默填成空串 —— 报告因此
     报 ALTERED"成功"，库里已经躺着一批违反"这一栏必填"的行。修法: 补之前先问库里有没有行，
     有行且没配默认值就一列都不许多发，并把可操作的修法带回（问不到行数按"有行"fail-safe）。
  D) 问句本身的列名 (M13): 表级校对那一问写成了 `information_schema.tables.collation_name`，
     而 MySQL 8 的 tables 视图里这一列叫 TABLE_COLLATION（COLLATION_NAME 是 columns 的列名）——
     真库上当场 `ERROR 1054 Unknown column`，被生产的 catch 吞成"问不到"，于是**整道校对闸一次都不咬**
     (#57，250 实测: 漂到 utf8mb4_0900_ai_ci 的表 provision 照样报 EXISTS_INTACT)。这一支在 H2 上
     结构上看不见（那边压根没有 information_schema，那一整块 if 不走），修完也是靠 250 的闸 4 才证明咬住。

⚠ 层的边界要写清: N 族钉的是 java 单测层。**只有 MySQL 才会红的那一半**（库答应了、数据被改了）
在本地这一层拿不到猎物 —— 它由 `_e2e/deploy_250.sh api` 那一轮真库门禁量，不由本脚本冒充。
⚠ M13 能在单测层红，前提是从这一窗起 FakeJdbc **按 MySQL 8 实测的目录形状答话**（问错列名就抛，
等价于真库拒绝）；替身不读 SQL 的那些年，这一支是"注了也不红"的空账，只能记未覆盖。

预期红集是**先写下再去量**的。三支"只碰装配顺序/两个参数换序"的注入 (M2/M3/M7) 编译器管不着，
是这一族最可能悄悄复发的形状；M13 更狠一层 —— 它改的是一句 SQL 的**字符串内容**，
编译器、替身、H2 三者都不读它，只有按真库目录形状编排的替身和 250 的闸 4 会咬。

用法: python3 _e2e/mutate_collation_guard.py
日志与备份一律在 ~/.cache（/tmp 会被别的会话扫掉）。
"""
import hashlib
import os
import shutil
import subprocess
import sys
import time

REPO = "/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc"
# 与 replay/provision 那几支共用**按仓库**的锁: 这一支也在就地改写 SchemaAdminBizService.java。
sys.path.insert(0, os.path.join(REPO, "z-lc-admin-ui", "e2e"))
import _mutlock  # noqa: E402

SVC = "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
UNDO = "z-lc-core/src/main/java/com/zifang/z/lc/core/undo/UndoService.java"
CACHE = os.path.expanduser("~/.cache/zlc51_collation_guard")

NAMED = [
    # A 族 (core 单测)
    "createTableDdlShouldPinTheMetadataLayerCollation",
    "createTableDdlShouldNotInventACollationWhenMetadataIsUnaskable",
    "nonIdentifierCollationReadingMustNotReachDdl",
    "metadataCollationShouldBeReaskedUntilItResolves",
    "collationRepairMessageShouldNameTheFixOnlyOnARealMismatch",
    "provisionShouldFailWhenTableCollationDiffersFromMetadataLayer",
    "provisionShouldStayGreenWhenCollationMatchesOrIsUnaskable",
    # B 族 (core 单测 + web 契约层)
    "localDateTimeSnapshotSurvivesTheWritePath",
    "localDateSnapshotSurvivesTheWritePath",
    "localTimeSnapshotPadsSeconds",
    "isoStringFromAnOlderSnapshotIsReplayable",
    "offsetAwareIsoStringKeepsTheSameInstant",
    "undoOfLegacyIsoDateTimeSnapshotLands",
    # N 族 (core 单测, 缺陷 #54)
    "requiredColumnWithoutDefaultIsNotAddedToAPopulatedTable",
    "requiredColumnOnAnEmptyTableIsStillAdded",
    "nullableColumnOnAPopulatedTableIsStillAdded",
    "requiredColumnWithDefaultCarriesItIntoTheAddColumnDdl",
    "unaskableRowCountFailsSafeToRefusal",
    "blankDefaultDoesNotCountAsADeclaredDefault",
]

SUITES = [
    ("core", ["mvn", "-o", "-B", "test", "-pl", "z-lc-core",
              "-Dtest=SchemaAdminBizServiceTest,UndoServiceSnapshotFormatTest",
              "-Dsurefire.failIfNoSpecifiedTests=false"]),
    ("web", ["mvn", "-o", "-B", "test", "-pl", "z-lc-web",
             "-Dtest=LcHttpContractTest#undoOfLegacyIsoDateTimeSnapshotLands",
             "-Dsurefire.failIfNoSpecifiedTests=false"]),
]

CLAUSE = """        String[] meta = metadataCharsetCollation();
        if (meta != null) {
            return "DEFAULT CHARSET=" + meta[0] + " COLLATE=" + meta[1];
        }
        return "DEFAULT CHARSET=utf8mb4";"""

GATE = """        String[] meta = metadataCharsetCollation();
        if (meta != null) {
            String repair = collationRepairMessage(def.getTableName(), meta[0], meta[1],
                    tableCollation(def.getTableName()));
            if (repair != null) {
                return item(def, ProvisionReport.FAILED, ddl, repair);
            }
        }"""

REPAIR_HEAD = """        if (want == null || got == null || want.equals(got)) {
            return null;
        }"""

IDENT = "        return v.matches(\"[A-Za-z0-9_]+\") ? v.toLowerCase(Locale.ROOT) : null;"

CATCH = """        } catch (Exception ex) {
            log.debug("No metadata charset/collation to inherit ({}): {}",
                    "z_lc_dict_item", ex.getMessage());
        }"""

# N 族 (#54) 的两个锚点: 拒发那一句的头，和"问不到行数"的那个 catch。
REFUSE_HEAD = "            if (f.getRequired() != null && f.getRequired() && !hasDeclaredDefault(f)) {"

ROWS_CATCH = """        } catch (Exception ex) {
            log.debug("Cannot tell whether {} has rows ({}), treating it as populated",
                    safe, ex.getMessage());
            return true;
        }"""

MUTANTS = [
    ("M1 建表子句退回只写 charset（修复前的写法）",

     [(SVC, CLAUSE, '        return "DEFAULT CHARSET=utf8mb4";')],
     # 少掉那一支子句的同时也少掉了那一次"去问" —— 计数那支跟着归零，说明两支绑在同一句代码上。
     {"createTableDdlShouldPinTheMetadataLayerCollation",
      "metadataCollationShouldBeReaskedUntilItResolves"}),
    ("M2 provisionOne 里摘掉校对那道闸（装配顺序回归：零件全绿而闸不在路上）",
     [(SVC, GATE, "        // 闸被摘掉")],
     {"provisionShouldFailWhenTableCollationDiffersFromMetadataLayer"}),
    ("M3 把 charset 和 collation 两个参数换序（编译器抓不到，界面上的 ALTER 会把表改歪）",
     [(SVC, "collationRepairMessage(def.getTableName(), meta[0], meta[1],",
       "collationRepairMessage(def.getTableName(), meta[1], meta[0],")],
     # 换序后 want 成了 charset，于是**每一张健康的表**都会被判红 —— 这正是换序的代价。
     {"provisionShouldFailWhenTableCollationDiffersFromMetadataLayer",
      "provisionShouldStayGreenWhenCollationMatchesOrIsUnaskable"}),
    ("M4 判据空跑：collationRepairMessage 一律不判",
     [(SVC, REPAIR_HEAD, "        if (true) {\n            return null;\n        }\n" + REPAIR_HEAD)],
     {"collationRepairMessageShouldNameTheFixOnlyOnARealMismatch",
      "provisionShouldFailWhenTableCollationDiffersFromMetadataLayer"}),
    ("M5 去掉标识符校验（数据库返回的内容直接拼进 DDL 尾巴）",
     [(SVC, IDENT, "        return v.isEmpty() ? null : v;")],
     {"nonIdentifierCollationReadingMustNotReachDdl"}),
    ("M6 问不到就凭猜测钉一个「主流」校对",
     [(SVC, '        return "DEFAULT CHARSET=utf8mb4";',
       '        return "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci";')],
     # 凭猜测钉的那一支同时是"坏读数当问不到"的落点：猜测值会从这条路径漏进 DDL。
     {"createTableDdlShouldNotInventACollationWhenMetadataIsUnaskable",
      "nonIdentifierCollationReadingMustNotReachDdl"}),
    ("M7 把「问不到」缓存下来（元数据表还没建时就永久关掉这道闸）",
     [(SVC, CATCH,
       '        } catch (Exception ex) {\n'
       '            metaCharsetCache = new String[]{"utf8mb4", "utf8mb4_general_ci"};\n'
       '            log.debug("No metadata charset/collation to inherit ({}): {}",\n'
       '                    "z_lc_dict_item", ex.getMessage());\n'
       '        }')],
     {"metadataCollationShouldBeReaskedUntilItResolves",
      "createTableDdlShouldNotInventACollationWhenMetadataIsUnaskable"}),
    ("M8 撤销不再归一前像（回到读回原始 ISO 串）",
     [(UNDO, "        Map<String, Object> before = persistable(entity, read(target.getBeforeImage()));",
       "        Map<String, Object> before = read(target.getBeforeImage());")],
     {"undoOfLegacyIsoDateTimeSnapshotLands"}),
    ("M9 重做不再归一后像（只修 undo 那一头的半修）",
     [(UNDO, "        Map<String, Object> after = persistable(entity, read(target.getAfterImage()));",
       "        Map<String, Object> after = read(target.getAfterImage());")],
     {"undoOfLegacyIsoDateTimeSnapshotLands"}),
    ("M10 LocalDateTime 分支形同虚设（type 永不匹配）",
     [(UNDO, '        if (value instanceof java.time.LocalDateTime && "DATETIME".equals(type)) {',
       '        if (value instanceof java.time.LocalDateTime && "NEVER".equals(type)) {')],
     {"localDateTimeSnapshotSurvivesTheWritePath"}),
    ("M11 LocalDate 分支形同虚设",
     [(UNDO,
       '        if (value instanceof java.time.LocalDate && ("DATE".equals(type) || "DATETIME".equals(type))) {',
       '        if (value instanceof java.time.LocalDate && "NEVER".equals(type)) {')],
     {"localDateSnapshotSurvivesTheWritePath"}),
    ("M12 LocalTime 退回 toString（整分时秒被削掉：12:00）",
     [(UNDO,
       '            return ((java.time.LocalTime) value).format(\n'
       '                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss", java.util.Locale.ROOT));',
       "            return ((java.time.LocalTime) value).toString();")],
     {"localTimeSnapshotPadsSeconds"}),
    # ---- N 族: 缺陷 #54（给有行的表补「必填且无默认值」的列）----
    ("N1 摘掉整道必填闸（回到「先发 DDL，看库给什么脸色」）",
     [(SVC, REFUSE_HEAD, "            if (false) {")],
     {"requiredColumnWithoutDefaultIsNotAddedToAPopulatedTable",
      "blankDefaultDoesNotCountAsADeclaredDefault",
      "unaskableRowCountFailsSafeToRefusal"}),
    ("N2 行数探针读不按时按「空表」处理（fail-open）",
     [(SVC, ROWS_CATCH, '        } catch (Exception ex) {\n'
                         '            log.debug("rows probe failed: {}", ex.getMessage());\n'
                         '            return false;\n'
                         '        }')],
     {"unaskableRowCountFailsSafeToRefusal"}),
    ("N3 分叉的依据换成「必不必填」而不是「有没有行」（有行那一句恒真）",
     [(SVC, "                if (hasRows) {", "                if (true) {")],
     {"requiredColumnOnAnEmptyTableIsStillAdded"}),
    ("N4 连可空列一起拒掉（拿 #47 修好的能力换一个假安全）",
     [(SVC, REFUSE_HEAD, "            if (!hasDeclaredDefault(f)) {")],
     {"nullableColumnOnAPopulatedTableIsStillAdded"}),
    ("N5「配了默认值」的口径漂成「非 null 就算」—— 空串进不了 DEFAULT 子句却算作有默认值",
     [(SVC, '        return f.getDefaultValue() != null && !f.getDefaultValue().isEmpty();',
       "        return f.getDefaultValue() != null;")],
     {"blankDefaultDoesNotCountAsADeclaredDefault"}),
    # ---- D 族: 缺陷 #57（问句里的列名在真库上不存在）----
    ("M13 表级校对那一问退回 `collation_name`（MySQL 8 的 tables 视图里没有这一列，缺陷 #57 的原样）",
     [(SVC, '"select table_collation from information_schema.tables"',
       '"select collation_name from information_schema.tables"')],
     # 替身按 250 实测的目录形状答话（问错列名就抛），于是产品那一头的 catch 把它吞成"问不到"，
     # 漂了的表就此报好 —— 红的是"闸在不在路上"那一条，不是某个零件。
     {"provisionShouldFailWhenTableCollationDiffersFromMetadataLayer"}),
]


def sh(cmd, cwd=REPO, timeout=1800):
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


def build_core():
    b = sh(["mvn", "-o", "-B", "-q", "install", "-pl", "z-lc-core", "-am", "-DskipTests"])
    return b.returncode == 0, (b.stdout + b.stderr)[-800:]


def run_suites():
    """两层各跑一次; 返回 (具名红集合, 每层汇总行, 原始输出, 有没有哪层没跑起来)。"""
    reds, summaries, raw = set(), [], ""
    broken = False
    for tag, cmd in SUITES:
        r = sh(cmd)
        out = r.stdout + r.stderr
        raw += "\n===== %s =====\n" % tag + out
        lines = [l for l in out.splitlines() if "Tests run" in l and "in com.zifang" in l]
        summaries.append("%s: %s" % (tag, lines[-1].strip() if lines else "(没有汇总行)"))
        if not lines:
            broken = True
            summaries[-1] += "  << 这一层没跑起来(rc=%d)" % r.returncode
        for line in out.splitlines():
            if "<<<" in line:
                for name in NAMED:
                    if name in line:
                        reds.add(name)
    return reds, summaries, raw, broken


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

    out("=== 缺陷#51 注入自证 run=%s（参照集=本次运行开始时的字节，不是 git HEAD）" % stamp)

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
                for rel2 in originals:
                    write(rel2, originals[rel2])
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

    out("备份/日志: %s" % backup)
    if problems:
        out("RESULT: %d problem(s)" % len(problems))
        for p in problems:
            out("  - " + p)
        return 1
    out("RESULT: 缺陷#51+#54+#57 的 %d 支注入逐支按预期点名，产物已还原到基线字节" % len(MUTANTS))
    return 0


if __name__ == "__main__":
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
