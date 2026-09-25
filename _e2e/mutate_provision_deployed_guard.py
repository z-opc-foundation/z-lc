#!/usr/bin/env python3
"""Falsify the `[15q] 一张物理表只属于一个活实体` / `[15j] 逆向映射` / `[15r] 改定义再建` /
`[15s] 缺陷 #47 读写两侧` probes against the DEPLOYED fat jar.

缺陷 #43 的形状是"HTTP 200 + 一句'已 provision N 张表'，而那份定义声明的列在库里一个都没有"。
上层已经各有一张网：单测 (SchemaAdminBizServiceTest / SchemaAdminServiceTest)、前端
(z-lc-admin-ui/src/views/designer/DesignerProvision.test.tsx + e2e/mutate_provision_report_guard.py)。
这一层只管一件事：**发出去的那个 jar** 会不会把没建成说成建成。前端那张网管的是"报告说 FAILED 时 UI
不许说已建成"，它验不出报告本身有没有撒谎 —— 少这一层，两边一起把同一个谎讲得很一致也能全绿。

每支 mutant 只破一处判定，所以预期红集是能推出来的；但推出来的东西要认账：跑第一遍时若实际红集和
预期不同，先判"这条红是不是这一支因果必然的后果"，是就改预期并写下为什么，不是就说明注入点选错了。
下面每条注释里的 (实测) 就是这么来的，不是先验的。

⚠ #47 把这一层的判据换了一批（reuse 那一步从"永远 FAILED"变成"ALTERED + 回读库"），所以
D1–D7 的预期集是按新夹具重推的，D8–D13 是 #47 自己的。

  D1  摘掉 createEntity 里的表名闸门    -> 撞表那几支 + "被拒的两支不留元数据" + reuse 的 ALTERED 一族
                                          (补列被占用者闸挡回 FAILED) + 汇总里 failedCount/total/点名
                                          + 整条 [15j] 逆向映射链（抢到的那两支占住了 entityCode）
  D2  表名比较改成单边大小写            -> 大写那支溜过去之后**反过来**把小写的 reuse 挡住了
  D3  补完不再回读物理列                -> 只红"读报告"的那六条；读库的一条都不红（见下）
  D4  FAILED 不点名缺了哪几列            -> 只应打掉点名那一栏的一条（判"缺列"和"报缺列"是两件事）
  D5  把"本来就在"说成"这次建出来的"      -> 两条 EXISTS_INTACT 幂等 + 汇总里的 created/unchanged
  D6  整批在第一个坏实体上抛（#43 旧行为）-> 整份汇总 + 邻居 + 批里 ALTERED 是否真落地 + 尾部 allOk
  D7  把软删实体也算成占用者            -> "软删之后表名可以复用" + ALTERED 一族 + 逆向映射整条链
  D8  摘掉 #47 的补列（回到"缺列一律 FAILED"）-> ALTERED 一族 + 库给的原因 + 汇总三条 + [15r]/[15s] 全塌
  D9  补了列但不计进汇总                -> 四态各占各的格那一条（altered 永远是 0）
  D10 读侧退回裸 500（摘掉 400 分支）     -> [15s] 的"不再裸 500"与"点名那一栏"
  D11 400 给但不说是哪一栏              -> 只应打掉"点名是哪一栏、并指向 provision"一条
  D12 400 把驱动原文拼进文案             -> 只应打掉"不带 SQL 文本与物理表名"那一条（与 D11 互补）
  D13 新建的表也说成"表本来就在"          -> CREATED 那一条 + 汇总里的 created（D5 的镜像）

三轮实测（第 1 轮 11 支跑出 8 处偏差，第 2 轮 13 支只剩 1 处，第 3 轮 `RESULT: deployed-layer
falsification done`、13/13 逐字相同、还原干净、恢复后 434/434）：D4/D9/D11 从第一遍就逐字相同，
其余都有偏差，全部按"这条红是不是这一支因果必然的后果"认账，写进了各自注释里的 (实测)。
归口成三条规律：
  1) **读库的判据不会因为"报告撒谎"而红**。D3 我只红六条却预计十条：库里确实没有那一列 /
     补上了且真进了库 / 补列之后读得通 / 尾部整批 allOk 全是读库的，注入改的是报告不是库。
     反过来凡是读报告的全红 —— 这正是 D3 该有的形状。（第一版是按主题数的，所以错了四条。）
  2) **一支注入挤掉了夹具，红集就跟着动**。D2/D7 的红与不红都由"reuse 建不出来"这一条因果链决定：
     多的三条来自"批里多出一支活的占用者"，少的来自"那几条压根不读报告"。
  3) **两处偏差是断言本身空转，改的是断言不是预期集**：
     D10 只红一条 —— 探针栏名 `never_provisioned` 自己含 "provision" 子串，驱动原文
     (`Column "t.never_provisioned" not found`) 里既有栏名又有这个子串，于是"点名并指向 provision"
     对着 500 的驱动原文打绿灯。改成先把栏名摘掉再查指路、并钉住同一个响应是 400，现在两条都红。
     S_LEAKFREE/D12、S_CREATED/D13 则是"从来没有注入能让它红"—— 补两支才敢记"已证伪"。
D13 那一支还教了一条归口的规矩：红集**按标题清单认领**（OWNED），不按 `[15x]` 节标签 ——
`[15j]` 里同时住着别人的探针和我自己的逆向映射链，按节归口会把"一支注入搅了两节"这道闸变成摆设。
仍记为**本层未覆盖**（纯读库，要动 DDL 才能红，那是 java 单测层 C 系列的活）：
`库里确实没有那一列`、`补列只加不删`。

历史实测（#43 那一版，03:31/03:39 两轮）留下的两条规矩还在生效：
"被拒的文案不带 DDL 文本与索引名" 原来只有负向半边，D1 把拒绝整个摘掉时它对着空消息打了绿灯 ——
负向断言必须同一条里钉住猎物，补上之后才重新进红集。

Hard guards inherited from mutate_field_code_deployed_guard.py, all of them earned:
  * artifact fingerprinted over *class file* digests inside the nested z-lc-*.jar entries (jar-level
    md5 changes for byte-identical sources, so it would guard nothing); empty fingerprint = hard error
  * per-run denominator must equal the baseline's, or "没红" means the run died early
  * sources restored from a byte-compare snapshot, per-run backup dir (a shared `.orig` that never
    expires overwrites the working tree with an older round's sources — that already destroyed
    uncommitted work once), and the pristine rebuild is re-run at the end
  * 备份/日志一律写 ~/.cache，不写 /tmp：同机其他会话会扫 /tmp
  * build 一律带 clean（见上面那段 ⚠：不带 clean 时基线可能吃 ~/.m2 里的旧构件而"全绿"）
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
# 与前端/其他层的注入脚本共用同一把按仓库的锁：它们改 .tsx，这里改 .java，看着不打架，
# 但同时改写源文件时 A 的字节还原会把 B 正在判定的那份换掉（上一轮实测出过一次假红）。
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

SCHEMA = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
HANDLER = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/LcExceptionHandler.java"
SOURCES = (SCHEMA, HANDLER)
BAK = Path.home() / ".cache/zlc43/deployed_bak"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
LOGS = Path.home() / ".cache/zlc43/deployed_logs"
BOOT_LOG = LOGS / "boot.log"

# 本层" owns 哪些探针"按**标题清单**判定，不按 `[15x]` 节标签: [15j] 里同时住着两支探针 ——
# 字段编码撞自建列 (mutate_field_code_deployed_guard.py 的) 和逆向映射撞活实体 (这一层的)。
# 按节标签收会把别人的探针算进"我的红集"，于是那一条真的红了也不会被认成"一支注入搅了两节"。
# OWNED 在下面标题常量定义完之后从同一批常量里取，不再抄第二份清单。

# ---- the names this layer owns, taken verbatim from the suite's own printed titles -------------
Q_SQUATTER = "第二个活实体不许抢同一张物理表（旧口径照收，等建表才发现是空操作）"
Q_NAMED = "撞表被拒时要点名是哪一方占着（app/entity，不点名只能一个个试）"
Q_NOLEAK = "被拒的文案不带 DDL 文本与索引名（不给浏览器看物理结构）"
Q_CASE = "表名大小写变体同样抢不到（MySQL/H2 的表名不分大小写）"
Q_NOTHING_LANDED = "被拒的两支都不许留下元数据（留下就是「存下了但永远建不出来」的坏定义）"
Q_REUSE = "软删实体之后表名可以复用（不许把表名永久占死）"
Q_ALTERED_NAMED = "旧表缺的那一栏按定义补上：ALTERED 且点名 addedColumns（旧口径是永远修不好的 FAILED）"
Q_DB_HAS = "库里真的多了那一列（判据来自库，不来自报告自述）"
Q_ONLY_ADD = "补列只加不删：上一支实体留下的那一栏还在（动到别人的列就是 #43）"   # 见下面的 NOT_COVERED
Q_ALTER_MSG = "成功文案说的是这次真的补了列，不许退回「列一列不缺」"
Q_DB_REFUSES = "库自己拒的 DDL 不许说成 ALTERED：补不上就是 FAILED"
Q_FAILED_NAMED = "FAILED 点名没补上的那一栏"
Q_FAILED_REASON = "FAILED 带上库给的原因（H2 实测 NULL not allowed），但不带 DDL 文本"
Q_DB_LACKS = "库里确实没有那一列（FAILED 不是报告撒的谎）"   # 见下面的 NOT_COVERED
Q_BATCH_NOT_500 = "一支坏实体不再把整批 provision 变成 500（旧口径抛异常，调用方只知道「失败了」）"
Q_SIX_KEYS = "汇总带 total/created/unchanged/altered/failedCount/allOk 六个数（#47 多了 altered 这一格）"
Q_IN_REPORT = "坏的那一支必须体现在汇总里（allOk=false、failedCount=1）"
Q_STATUS_GRID = "四种状态在汇总里各占各的格（neighbour 是这次新建的 created，drift 是补列的 altered）"
Q_TOTAL = "total 数的是实体数（reuse + bad + neighbour + drift = 4，被拒的那两支不算）"
Q_POINTS_AT_BAD = "批里坏的那一支点的就是那支坏的"
Q_NEIGHBOUR = "邻居没被连坐：它的表真在库里"
Q_ALTER_LANDED = "批里的 ALTERED 不是自述：drift 的那一栏真进了库"
Q_NEIGHBOUR_WRITE = "坏实体在批里时，邻居照样能写数据"
Q_REPAIR_OK = "补上了：ALTERED，且那一列真进了库"
Q_ROW_INTACT = "补列之后读得通，旧行还在（补列不许把已有数据弄丢，也不许让运行时继续 400）"
Q_ALL_OK_AFTER = "全都补上之后整批才报 allOk（四态这时候只剩 unchanged 一格是 4）"
Q_INTACT = "同一支再 provision 一次 → EXISTS_INTACT（幂等，且不谎称新建）"
Q_HOLDER_REAL = "宿主的表这次是真建出来的（CREATED，不是 IF NOT EXISTS 的空操作）"
J_MAP_REFUSED = "正被活实体占用的表，逆向映射在入口就拒（不许映射出第二份定义）"
J_MAP_NAMED = "逆向映射被拒时点名是哪一方占着这张表（不命名只能一个个试）"
J_MAP_NOTHING = "被拒的逆向映射一行元数据都不留（清单里还是只有 fc_clean）"
J_IMPORT_OK = "POST /admin/db/table/import (逆向映射一张没有活主人的物理表)"
J_MAP_SKIP = "逆向映射跳过引擎自建列（一律拒绝会把 /table/import 打死：真表的 id/deleted 是合法物理列）"
J_MAP_ALL = "用户列一列不少地映射回来"
J_MAP_DESC = "跳过的清单写进 description，不是静默丢掉"
R_REEDIT = "改完定义再 provision：多出来的那一栏按定义补进表里，并点名补了哪一列（#47；旧口径在这里永远 FAILED）"
R_ALTERDB = "「补上了」的判据来自库，不来自报告自述"
S_CREATED = "表按第一版定义建出来: CREATED，库里就是那一栏"
S_INTACT = "同一份定义再 provision 一次: EXISTS_INTACT，一列都不缺就不该说补过"
S_NO500 = "漂移期间的列表不再是一个裸 500（旧行为: 驱动原文 + 500，调用方只能猜）"
S_NAMED = "400 要点名是哪一栏、并指向 provision（不点名=用户不知道该动哪一栏）"
S_LEAKFREE = "这条 400 只点用户自己定义里的栏名，不带 SQL 文本与物理表名"
S_REPAIRED = "一次 provision 就把漂移清掉了: ALTERED 且点名补了哪一栏"
S_DB_AGREES = "库里真的多了那一栏（「补好了」只有库能作证）"
S_READ_OK = "补列之后列表回到 200，旧数据一个字都不少（新栏对旧行是空，不是把行抹了）"
S_IDEMPOTENT = "修好之后再 provision 一次不再谎报补过列（幂等，状态回到 EXISTS_INTACT）"

# [4] 那一节最基础的一支探针。认领它不是因为这一层管得宽，而是因为 D13 改的是**所有 provision
# 共用**的那句状态判定 —— 它必然连带把 [4] 的 CREATED 也改掉。不认领的话，一次因果正当的注入
# 会被"其他节红"这条报警当成串节污染，而那条报警唯一值钱的地方就是只报真正意外的红。
P_TRISTATE = "provision 回三态之一 (status=CREATED)"

# 记账：这两条在本层**没有任何注入能让它们红** —— 它们只读 JDBC 元数据里的列清单，不读报告，
# 所以要动 DDL 才碰得到（把 ALTER 换成 DROP、或给 NOT NULL 补默认值绕过库的拒绝）。那是 java
# 单测层 mutate_provision_reconcile_guard.py (C 系列) 的活，本层不许把它们算进"已证伪"。
NOT_COVERED = [Q_DB_LACKS, Q_ONLY_ADD]

# 按命名约定收 (Q_/J_/R_/S_ = 本层那四个节的探针标题，P_ = 被本层连带认领的其他节探针，
# 见上面 P_TRISTATE 的注释)；锚点与注入替换串是 *_ANCHOR / H_* / *_MUT，不会被收进来。verify_owned() 在开跑前钉两件事: 每条标题都要能在被测套件里逐字找到，
# 每条预期红集里的标题都必须属于 OWNED —— 少收一条就会把一个本该"我的红"的探针漏成"其他节红"。
OWNED = {v for k, v in globals().items()
         if isinstance(v, str) and re.fullmatch(r"[QJSRP]_[A-Z0-9_]+", k)}

# ---- anchors (each must occur exactly once; the script refuses to guess) -----------------------
GATE_ANCHOR = ('        // 物理表名在租户内是一份命名空间。不在这道闸上拦，坏消息要等 provision 才出现，\n'
               '        // 而且那时 `CREATE TABLE IF NOT EXISTS` 对第二个实体是空操作 —— 接口照样回"建好了"。\n'
               '        validateTableNameAvailable(tenantCode, req.getTableName());\n')
CASE_ANCHOR = '        String wanted = tableName.toLowerCase(Locale.ROOT);'
RECHECK_ANCHOR = '        if (!still.isEmpty()) {'
MISSING_ANCHOR = ('            bad.setMissingColumns(still);\n'
                  '            bad.setAddedColumns(added);')
STATUS_ANCHOR = ('        return before.isEmpty()\n'
                 '                ? item(def, ProvisionReport.CREATED, ddl, "已按这份定义建表")\n'
                 '                : item(def, ProvisionReport.EXISTS_INTACT, ddl, "表本来就在，列一列不缺，跳过建表");')
BATCH_ANCHOR = ('            ProvisionReport.Item item = provisionOne(def);\n'
                '            report.getItems().add(item);')
SOFT_ANCHOR = ('        List<EntityEntity> live = entityMapper.selectList(\n'
               '                new QueryWrapper<EntityEntity>()\n'
               '                        .eq("tenant_code", tenantCode)\n'
               '                        .eq("deleted", 0));')
RECONCILE_CALL = '            return reconcileColumns(def, ddl, missingUser);'
RECONCILE_MUT = ('            ProvisionReport.Item legacy = item(def, ProvisionReport.FAILED, ddl,\n'
                 '                    "表 " + def.getTableName() + " 缺 " + String.join(", ", missingUser));\n'
                 '            legacy.setMissingColumns(missingUser);\n'
                 '            return legacy;')
ALTER_COUNT = '                report.setAltered(report.getAltered() + 1);'
# 读侧: 缺列的 400 是 #47 的另一半，它整个住在 z-lc-web 的那个 advice 里。
H_400 = ('        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(\n'
         '                "这一栏在物理表里还不存在: " + column + "。定义已经改到表前面了，去设计器点一次「provision」补列"\n'
         '                        + "（没补之前这张表的读都会失败）").code(400));')
H_AS_500 = ('        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)\n'
            '                .body(Result.fail(clientSafe(ex)).code(500));')
H_NO_NAME = ('        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(\n'
             '                "这一栏在物理表里还不存在。定义已经改到表前面了，去设计器点一次「provision」补列"\n'
             '                        + "（没补之前这张表的读都会失败）").code(400));')
# 把驱动原文拼进 400 的文案里 —— 这一支测的是"点名那一栏"和"不把 SQL 文本/物理表名发出去"
# 能不能分开红 (D11 只动栏名，S_LEAKFREE 照旧绿；这一支反过来: 400 与栏名都对，只有泄密那半红)。
H_LEAK = ('        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(\n'
          '                "这一栏在物理表里还不存在: " + column + " | " + ex.getMessage() + "。"\n'
          '                        + "定义已经改到表前面了，去设计器点一次「provision」补列"\n'
          '                        + "（没补之前这张表的读都会失败）").code(400));')
# 新建的表也说成"表本来就在"（D5 是它的镜像: 那边把跳过说成成果，这边把成果说成跳过）。
STATUS_AS_INTACT = ('        return item(def, ProvisionReport.EXISTS_INTACT, ddl, "表本来就在，列一列不缺，跳过建表");')

RUNS = [
    ("D1", [(SCHEMA, GATE_ANCHOR, '')],
     # 摘掉闸门后不只是"撞表那几支没人拦"：抢到的那两支会**占住实体编码和表名**，于是
     # reuse 补列时被 reconcileColumns 的占用者闸挡回 FAILED (ALTERED 那一族三条一起红)，
     # 批里多出坏定义 (failedCount/total/点名那三条一起红)，后面 allOk 再也回不来；
     # 加上 [15j] 整条逆向映射链 —— 都是这一支的因果，不是注入点选错。
     # (实测) 唯一没红的是 `批里的 ALTERED 不是自述`：drift 用的是自己那张表，闸门摘不摘它
     # 都照样补得动 —— 这一条读的是"库认不认这次 ALTER"，与建表闸没有因果关系，是我预期过宽。
     [Q_SQUATTER, Q_NAMED, Q_NOLEAK, Q_CASE, Q_NOTHING_LANDED,
      Q_ALTERED_NAMED, Q_DB_HAS, Q_ALTER_MSG, Q_IN_REPORT, Q_STATUS_GRID, Q_TOTAL, Q_POINTS_AT_BAD,
      Q_ALL_OK_AFTER,
      J_MAP_REFUSED, J_MAP_NAMED, J_MAP_NOTHING, J_IMPORT_OK, J_MAP_SKIP, J_MAP_ALL, J_MAP_DESC]),
    ("D2", [(SCHEMA, CASE_ANCHOR, '        String wanted = tableName;')],
     # 只改 `wanted` 那一侧 = 单边归一化：大写那支溜过去了（它占的仍是同一张表），
     # 于是**下一个用小写表名的实体反而被它挡住** —— reuse 拿不到 id，后面所有读它的判据
     # 一起塌；这不是噪声，是"两边都得比"这句话的证据。
     # (实测) 比第一版多三条汇总的：混进来的那支大写表名实体是**活的**，批里于是有两支坏的
     # (failedCount=2)、状态格子错位、点名点不准 —— 都在这条因果链上，不是串节。
     # (实测) 少一条 `补列只加不删`：它只读库里的列清单，reuse 被挡住时那一栏根本没被碰过，
     # 断言对着"没动过"打绿灯 —— 它不在这一支的因果链上。
     [Q_CASE, Q_NOTHING_LANDED, Q_REUSE, Q_ALTERED_NAMED, Q_DB_HAS, Q_ALTER_MSG,
      Q_IN_REPORT, Q_STATUS_GRID, Q_POINTS_AT_BAD, Q_ALL_OK_AFTER]),
    ("D3", [(SCHEMA, RECHECK_ANCHOR, '        if (false) {')],
     # 补列之后不再回读物理列: "ALTER 没报错" 被当成 "那一栏在表里了"。
     # (实测) 只红六条，且全部是**读报告**的那几条；凡是读库的 (库里确实没有那一列 / 补上了
     # 且真进了库 / 补列之后读得通 / 尾部整批 allOk) 一条没红 —— 库的行为压根没变，变的只是
     # 报不报告撒谎。这正好是这一支的因果形状，也说明我第一版那十条是按主题数的。
     [Q_DB_REFUSES, Q_FAILED_NAMED, Q_FAILED_REASON, Q_IN_REPORT, Q_STATUS_GRID,
      Q_POINTS_AT_BAD]),
    ("D4", [(SCHEMA, MISSING_ANCHOR, '            bad.setAddedColumns(added);')],
     [Q_FAILED_NAMED]),
    ("D5", [(SCHEMA, STATUS_ANCHOR,
             '        return item(def, ProvisionReport.CREATED, ddl, "已按这份定义建表");')],
     # 宿主第一次本来就是 CREATED，D5 改不动它 —— 这一支只管"跳过被记成成果"。
     # (实测) 多一条 [15s] 的"修好之后再 provision 一次不再谎报补过列"：它和 [15q] 那条幂等读
     # 的是同一个状态字段，红是必然的 —— 我第一版只写了一半。
     [Q_INTACT, S_INTACT, S_IDEMPOTENT, Q_STATUS_GRID, Q_ALL_OK_AFTER]),
    ("D6", [(SCHEMA, BATCH_ANCHOR,
             '            ProvisionReport.Item item = provisionOne(def);\n'
             '            if (ProvisionReport.FAILED.equals(item.getStatus())) {\n'
             '                throw new IllegalStateException("provision failed for "\n'
             '                        + item.getEntityCode());\n'
             '            }\n'
             '            report.getItems().add(item);')],
     # 整批一抛：坏实体之后的 neighbour/drift 根本没被 provision（表不在、ALTERED 没落地）。
     # (实测) 第一版我断言"尾部整批 allOk 照旧绿"，实测它红了：drift 的 ALTER 原本就是在这一批
     # 里第一次执行的，那次被抛掉之后尾部的汇总变成 altered=1、created=1 而不是"四格 unchanged"
     # —— 是这一支的因果。
     [Q_BATCH_NOT_500, Q_SIX_KEYS, Q_IN_REPORT, Q_STATUS_GRID, Q_TOTAL, Q_POINTS_AT_BAD,
      Q_NEIGHBOUR, Q_ALTER_LANDED, Q_NEIGHBOUR_WRITE, Q_ALL_OK_AFTER]),
    ("D7", [(SCHEMA, SOFT_ANCHOR, SOFT_ANCHOR.replace('.eq("deleted", 0))', '.ge("deleted", 0))'))],
     # 把软删的也算成占用者：宿主实体是软删过的，于是那张表的表名被永久占死 ——
     # reuse 建不出来，逆向映射整条链一起没（宿主自己建表那两支不受影响，见下）。
     # (实测) 比第一版少三条：`补列只加不删` 只读列清单（同 D2 的机制）；
     # `坏的那一支必须体现在汇总里` 与 `批里坏的那一支点的就是那支坏的` 说的是 pq_bad，
     # 它用的是自己那张表，占用者闸挡不到它，批里 failedCount 仍然是 1。
     [Q_REUSE, J_IMPORT_OK, J_MAP_SKIP, J_MAP_ALL, J_MAP_DESC,
      Q_ALTERED_NAMED, Q_DB_HAS, Q_ALTER_MSG, Q_TOTAL, Q_STATUS_GRID, Q_ALL_OK_AFTER]),
    # ---- #47 自己的四支 ----
    ("D8", [(SCHEMA, RECONCILE_CALL, RECONCILE_MUT)],
     # 回到 #43 那一版: 缺列一律 FAILED、一列都不补。"未建成"因此变成谁都清不掉的死状态。
     # (实测) 17 条 = 第一版 12 条 + 5 条没数到的因果：摘掉补列后 FAILED 的文案里不再有
     # 库给的原因那一句、批里三支一起 FAILED (failedCount 与点名那两条跟着红)、
     # [15r] 那两条读同一份报告的也一起红。
     [Q_ALTERED_NAMED, Q_DB_HAS, Q_ALTER_MSG, Q_FAILED_REASON, Q_IN_REPORT, Q_STATUS_GRID,
      Q_POINTS_AT_BAD, Q_ALTER_LANDED, Q_REPAIR_OK, Q_ROW_INTACT, Q_ALL_OK_AFTER,
      R_REEDIT, R_ALTERDB, S_REPAIRED, S_DB_AGREES, S_READ_OK, S_IDEMPOTENT]),
    ("D9", [(SCHEMA, ALTER_COUNT, '                report.setAltered(report.getAltered());')],
     # 补了列但不计进汇总: 界面拿到的 altered 永远是 0，"N 张表按定义补了列"这句没人能证。
     [Q_STATUS_GRID]),
    ("D10", [(HANDLER, H_400, H_AS_500)],
     # 读侧退回裸 500: 定义跑在表前面时，调用方只拿到一句数据库原文。
     # (实测) 第一版这里只红一条 —— 探针那一栏的名字 `never_provisioned` 自己就含 "provision"
     # 子串，而驱动原文 `Column "t.never_provisioned" not found` 里既有栏名又有这个子串，
     # 于是"点名并指向 provision"成了一句空断言（对着 500 的驱动原文打绿灯）。
     # 修的是**断言**不是预期集：先把栏名从文案里摘掉再查指路，并钉住同一个响应是 400。
     # 现在两条都红。
     [S_NO500, S_NAMED]),
    ("D11", [(HANDLER, H_400, H_NO_NAME)],
     # 400 仍然给，但不说是哪一栏 —— 用户不知道该动哪一栏，等于没指路。
     [S_NAMED]),
    ("D12", [(HANDLER, H_400, H_LEAK)],
     # 第一版没有这一支: S_LEAKFREE 那句"不带 SQL 文本与物理表名"在 D10/D11 里都不红，
     # 也就是说它从没被证明过会咬。这一支把驱动原文拼进 400 文案 (400 对、栏名也点名了)，
     # 只剩泄密那半红 —— 与 D11 互补，两条合起来才说明"点名"和"不泄密"是两件事。
     # (实测 1 红) 正是预期那一条：D12 里 400 与栏名都还在，只有"不带 SQL 文本/表名"被打掉。
     [S_LEAKFREE]),
    ("D13", [(SCHEMA, STATUS_ANCHOR, STATUS_AS_INTACT)],
     # D5 的镜像: 那边把"跳过"说成"这次建出来的"，这一支把"这次真的建出来了"说成"表本来就在"。
     # 汇总里的 created 因此永远是 0，[15s] 那句"表按第一版定义建出来: CREATED"没人证过。
     # (实测 4 红) 除预期两条外，还连带红两条，都是这一支因果必然的下游：
     #   `宿主的表这次是真建出来的` —— [15q] 里另一条读同一个 status 字段的探针，我第一版漏了它；
     #   `provision 回三态之一 (status=CREATED)` —— 住在 [4]，改的是所有 provision 共用的那句判定，
     #     所以它必红；不认领它就会让一次正当注入看起来像"一支注入搅了两节"（见 P_TRISTATE 注释）。
     [S_CREATED, Q_STATUS_GRID, Q_HOLDER_REAL, P_TRISTATE]),
]


def sh(cmd, cwd=ROOT, timeout=1800):
    return subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout)


def snapshot_sources():
    run_dir = BAK / f"run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    originals = {}
    for f in SOURCES:
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
            raise RuntimeError(f"no nested z-lc-*.jar inside {JAR}: a fingerprint over zero entries "
                               "is constant and therefore worthless")
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
    LOGS.mkdir(parents=True, exist_ok=True)
    log = open(BOOT_LOG, "ab")
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log)
    if not wait_health():
        listening = subprocess.run(["lsof", "-nP", "-iTCP:18090", "-sTCP:LISTEN"],
                                   capture_output=True, text=True).stdout.strip()
        raise RuntimeError(f"server did not come up; see {BOOT_LOG}"
                           f"\n   18090 listeners: {listening or '(none)'}"
                           f"\n   pids alive: {lc_pids()}"
                           f"\n   boot tail:\n"
                           f"{subprocess.run(['tail','-15',str(BOOT_LOG)],capture_output=True,text=True).stdout}")


def build():
    # clean 是必须的，不是保险: 这一批改过 z-lc-common 的 ProvisionReport，而 javac 的增量跳过
    # 让"基线构建"用过 ~/.m2 里那份旧构件 —— 基线全绿、任何一支注入强制重编译就死在
    # `cannot find symbol: ALTERED` 上 (上一轮实测)。带 clean 才是每次真编译。
    p = sh(["mvn", "-o", "-B", "clean", "install", "-DskipTests"], timeout=2400)
    if "BUILD SUCCESS" not in (p.stdout + p.stderr):
        raise RuntimeError("jar build failed, tail of log:\n"
                           + "\n".join((p.stdout + p.stderr).splitlines()[-25:]))


def run_e2e(label):
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    LOGS.mkdir(parents=True, exist_ok=True)
    (LOGS / f"e2e_{label}.log").write_text(p.stdout + p.stderr)
    summary = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    fails, mine = [], []
    for line in p.stdout.splitlines():
        if line.startswith("  FAIL  "):
            name = line[8:].split("   <<")[0]
            fails.append(name)
            if name in OWNED:
                mine.append(name)
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    return parsed, fails, mine


def run_unit():
    LOGS.mkdir(parents=True, exist_ok=True)
    # 不能带 -DskipTests：那是"跑测试的同时把测试关掉"，surefire 一行汇总都不打，
    # 下面的解析只会抛"没跑起来"。参照 mutate_field_code_deployed_guard.py 的写法。
    p = sh(["mvn", "-o", "-B", "test", "-pl", "z-lc-core",
            "-Dtest=SchemaAdminBizServiceTest,SchemaAdminServiceTest", "-DfailIfNoTests=false"])
    out = p.stdout + p.stderr
    (LOGS / "unit.log").write_text(out)
    totals = [int(x) for x in re.findall(r"Tests run: (\d+), Failures: \d+, Errors: \d+"
                                         r"[^\n]*in com\.zifang\.z\.lc\.core\.schema", out)]
    if not totals:
        raise RuntimeError("surefire printed no summary for the schema tests; tail:\n"
                           + "\n".join(out.splitlines()[-20:]))
    red = sorted({m for line in out.splitlines() if "<<<" in line
                  for m in re.findall(r"(SchemaAdmin\w*Test)\.([A-Za-z0-9_]+)", line)})
    return sum(totals), red


def main():
    acquire_lock("mutate_provision_deployed_guard.py")
    try:
        return run_all()
    finally:
        release_lock()


def verify_owned():
    """开跑前把"我认领了哪些探针"钉死：标题必须逐字存在于被测套件，预期红集必须都属于 OWNED。

    这一层是按标题归属判红的，所以一个错字的表现是"预期红却永远不红"—— 那会被读成注入没咬住，
    而真正坏的是清单。宁可在这里就拒绝启动。
    """
    src = (ROOT / "_e2e/e2e_api_test.py").read_text(encoding="utf-8")
    if not OWNED:
        raise RuntimeError("OWNED 是空的 —— 命名约定漂了，红集归属形同虚设")
    not_in_suite = sorted(t for t in OWNED
                          if f'"{t}"' not in src and f"'{t}'" not in src)
    if not_in_suite:
        raise RuntimeError(f"{len(not_in_suite)} 条标题在套件里逐字找不到: {not_in_suite}")
    for tag, _, expect in RUNS:
        stray = [t for t in expect if t not in OWNED]
        if stray:
            raise RuntimeError(f"{tag} 的预期红集里有 OWNED 之外的标题（永远不可能被判为我的红）: {stray}")
    print(f"owned probes: {len(OWNED)} titles, all verbatim in the suite; "
          f"{len(RUNS)} injections, expectations all owned")


def run_all():
    verify_owned()
    originals = snapshot_sources()

    def restore_all():
        for f, disk in originals.items():
            f.write_bytes(disk)

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
        print(f"  unit baseline (schema tests): {unit_total} tests, red={unit_red}")
        if unit_red:
            print("UNIT BASELINE NOT GREEN, aborting")
            return 1

        for tag, edits, expect in RUNS:
            for path, anchor, repl in edits:
                text = path.read_text(encoding="utf-8")
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
            u_total, u_red = run_unit()
            print(f"\n[{tag}] expect red on ({len(expect)}): {expect}")
            print(f"  artifact fp = {fp_id(fp)}  changed={changed}")
            print(f"  e2e = {passed}/{this_total}  (baseline denominator {total})")
            print(f"  本节 failures ({len(mine)}): {mine or '(none)'}")
            print(f"  其他节 failures (必须为空，否则一支注入搅了两节) : "
                  f"{[f for f in fails if f not in mine] or '(none)'}")
            print(f"  unit = {u_total} tests, red={u_red or '(none)'} (单测红集只作参照，"
                  "这一层的判据是发出去的 jar)")
            if not changed:
                print("  !! artifact did not change -> this run proves nothing")
                bad += 1
            if this_total != total:
                print(f"  !! 这一轮只跑了 {this_total} 项，基线是 {total} 项 —— 分母变了，「没红」的结论不成立")
                bad += 1
            if sorted(mine) != sorted(expect):
                print(f"  !! MISMATCH: wanted exactly {sorted(expect)}, got {sorted(mine)}")
                print(f"     少了 {sorted(set(expect) - set(mine))}  多了 {sorted(set(mine) - set(expect))}")
                bad += 1
            if u_total != unit_total:
                print(f"  !! 单测分母变了（{unit_total} → {u_total}），「红了几条」无从谈起")
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
        for f, disk in originals.items():
            if f.read_bytes() != disk:
                print(f"  !! NOT RESTORED: {f}")
                bad += 1
    finally:
        restore_all()
        leaked = [str(f) for f, disk in originals.items() if f.read_bytes() != disk]
        print("\nrestored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))
        print(f"serving pid(s) now: {lc_pids()}  health={wait_health(10)}")
    print("RESULT: " + ("deployed-layer falsification done" if bad == 0 else f"{bad} problem(s)"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 裸 sys.exit(main()) 会让"只是 import 看一下"的调用方直接开跑战役
    sys.exit(main())
