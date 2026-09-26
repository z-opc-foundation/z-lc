#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #48 落在 **java 层** 的注入自证 —— 改产品源码，看 PermissionServiceTest 与
LcHttpContractTest 那两层逐条红。

跑法（在 z-lc 下）：

    python3 _e2e/mutate_permission_service_guard.py

为什么这一支必须存在：#48 的六条缺陷里，"查重写 entity_code = NULL 恒不匹配""判定只比
entity_code = ?""三条列表与判定都不比租户""回收无条件说已成功""写入口让调用方自报租户"
—— 没有一条会让页面崩，也没有一条会让接口报错。它们只让**策略数据与判定互相说不清**。
测试如果只测"能授出去、能读回来"，这五条全都能绿。所以判据不是"有没有红"，而是
"我声称钉住的那几条，每条是不是只钉住自己那一句"。

判红口径（与前几支同源）：
    * 只认**具名**红 —— 每条预期红是 (测试方法, 该条断言的文案前缀) 一对，文案对应的
      **行号从被测文件的当前字节里现搜**，改名或挪位置都会被发现；
    * 一个 JUnit 方法只报**第一条**红，所以每支注入每个方法最多认领一条；同一因的下游红
      写在注释里当说明，不混进预期集；
    * 分母每轮必须相等，少一条就是"套件没真跑"，不当绿；
    * 编译不过的变异体不给结论（它不测任何东西）；跑之前先删掉上一轮的 surefire XML，
      否则"构建挂了"会被读成"上一轮的全绿"；
    * 预期之外的红不许加白名单了事，要先证明这一处注入换掉了哪一支断言再改预期。

RESULT 行: 全对时打印 `permission java-layer falsification done`。
"""
import hashlib
import os
import pathlib
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

REPO = pathlib.Path(__file__).resolve().parent.parent
SVC = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/permission/PermissionService.java"
KEYS = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/permission/PermissionKeys.java"
CTL = REPO / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/PermissionController.java"
UTEST = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/permission/PermissionServiceTest.java"
CTEST = REPO / "z-lc-web/src/test/java/com/zifang/z/lc/web/it/LcHttpContractTest.java"
UXML = REPO / "z-lc-core/target/surefire-reports/TEST-com.zifang.z.lc.core.permission.PermissionServiceTest.xml"
CXML = REPO / "z-lc-web/target/surefire-reports/TEST-com.zifang.z.lc.web.it.LcHttpContractTest.xml"

sys.path.insert(0, str(REPO / "z-lc-admin-ui" / "e2e"))
import _mutlock  # noqa: E402

# 日志与备份一律放 ~/.cache，不放 /tmp：同一台机器上别的会话会扫 /tmp（已复现两次）。
LOGS = pathlib.Path(os.path.expanduser("~/.cache/zlc48/permission_java_guard"))

U = "com.zifang.z.lc.core.permission.PermissionServiceTest"
C = "com.zifang.z.lc.web.it.LcHttpContractTest"

# ---- 断言文案前缀：每条在两个被测文件里逐字唯一，且就是 assert 的 message 参数 ----------------
S_IDEMPOTENT = "com.zifang.z.lc.core.permission.PermissionServiceTest.appWideGrantDedupeUsesIsNullNotEqualsNull"
S_ENTITYDEDUPE = U + ".entityScopedGrantComparesTheEntityColumn"
S_CANONICAL = U + ".grantStoresTheCanonicalFormSoTheCheckCanMatchIt"
S_REJECT = U + ".grantRejectsUnknownPermissionAndNamesWhatIsAllowed"
S_EXISTING = U + ".grantReturnsTheExistingRowWithoutInsertingAgain"
S_TENANTREAD = U + ".everyReadPathFiltersOnTenant"
S_BLANK = U + ".blankTenantOrKeyIsRejectedRatherThanSilentlyDroppingTheFilter"
S_SCOPECHECK = U + ".checkForAConcreteEntityAcceptsTheAppWideRowToo"
S_WIDECHECK = U + ".checkWithoutAnEntityAsksOnlyTheAppWideRow"
S_SCOPEUNKNOWN = U + ".checkRejectsAnUnknownPermissionInsteadOfAnsweringFalse"
S_REVOKE = U + ".revokeReportsHowManyRowsItTouchedAndScopedItsDelete"
S_REGISTRY = U + ".registryIsTheSingleSourceAndCoversTheMatrixColumns"

H_IDEM = "com.zifang.z.lc.web.it.LcHttpContractTest.appWideGrantIsIdempotent"
H_VERB = "com.zifang.z.lc.web.it.LcHttpContractTest.unknownPermissionVerbIsRejectedAndCaseIsNormalized"
H_SCOPE = "com.zifang.z.lc.web.it.LcHttpContractTest.grantScopeIsHonouredByTheCheckEndpoint"
H_FOREIGN = "com.zifang.z.lc.web.it.LcHttpContractTest.foreignTenantRowsNeitherGrantNorGetRevoked"
H_TENANT = "com.zifang.z.lc.web.it.LcHttpContractTest.grantIgnoresTheTenantTheCallerSupplies"

A = {
    S_IDEMPOTENT: "应用级查重必须用 IS NULL",
    S_ENTITYDEDUPE: "实体级不能退化成 IS NULL",
    S_CANONICAL: "库里存的必须是规范形态",
    S_REJECT: "不在词表里的权限项必须被写入口拒掉",
    S_EXISTING: "命中查重时必须回原来那一行",
    S_TENANTREAD: "listByApp 少了租户条件",
    S_BLANK: "空租户不能被当成",
    S_SCOPECHECK: "具体实体必须同时认应用级那一行",
    S_WIDECHECK: "整个应用那一档必须只比 ENTITY_CODE IS NULL",
    S_SCOPEUNKNOWN: "未知权限项应当抛，而不是回一句拒绝",
    S_REVOKE: "回收必须按租户圈定",
    S_REGISTRY: "词表就是矩阵的列顺序，一项不能少",
    H_IDEM: "同一份「整个应用」的授权每点一次多一行",
    "H_IDEM_ENTITY": "实体级授权也在长重复行",
    H_VERB: "任意思字符串都当权限项收下了",
    "H_VERB_ALLOWED": "文案没点名允许哪些项",
    "H_VERB_CHECKMATCH": "归一之后 check 必须认得这一行",
    "H_SCOPE_WIDE": "应用级授权对具体实体答了拒绝",
    "H_SCOPE_FAKE_WIDE": "拿某个实体的授权冒充「整个应用都可以」",
    "H_FOREIGN_LIST": "/list 把别的租户的行混进了本租户的清单",
    "H_FOREIGN_REVOKE": "跨租户回收被报成成功",
    "H_TENANT_PINNED": "这一族里 grant 原本是唯一让调用方自报租户的写入口",
    # /check 的 HTTP 状态由 checkAllows 这个夹具自己判 —— 必填参数一恢复，红落在那一行。
    "C_HELPER_STATUS": "/check 的坏消息不该以 ",
    # 但"少了必填参数"这一种坏消息连信封都没有（MockMvc 的 400 空体），红先落在传输守卫上。
    'C_TRANSPORT_EMPTY': 'assertFalse(body == null || body.isEmpty()',
    # 词表少一项时，"点名允许哪些词"这句在两层同时变短 —— 与"有没有拒"是两条不同的账。
    'S_REJECT_LISTS_EXPORT': 'ex.getMessage().contains("EXPORT")',
}

# ---- 产品源码锚点（每个必须逐字出现恰好一次，脚本先数再动）---------------------------------
GRANT_ISNULL = '            same.isNull("entity_code");'
GRANT_EQ = '            same.eq("entity_code", entityCode);'
CHECK_OR = '            q.and(w -> w.eq("entity_code", scope).or().isNull("entity_code"));'
LIST_APP_TENANT = '                        .eq("tenant_code", require(tenantCode, "tenantCode"))\n' \
                  '                        .eq("app_code", require(appCode, "appCode"))\n' \
                  '                        .orderByAsc("entity_code", "role_code"));'
GRANT_CANONICAL = '        String permission = PermissionKeys.canonical(entity.getPermission());'
REVOKE_TENANT = '        return permissionMapper.delete(new QueryWrapper<PermissionEntity>()\n' \
                '                .eq("tenant_code", require(tenantCode, "tenantCode"))\n' \
                '                .eq("id", id));'
CTL_ZERO_ROWS = "        if (removed == 0) {"
CTL_ENTITY_REQUIRED = '    public Result<Boolean> check(@RequestParam String appCode,\n' \
                      '                                @RequestParam(required = false) String entityCode,'
CHECK_WIDE_ISNULL = '        if (scope == null) {\n            q.isNull("entity_code");\n        } else {'
KEYS_GATE = "        if (!ALL.contains(wanted)) {"
REQUIRE_BLANK = "        if (value == null || value.trim().isEmpty()) {"
CTL_TENANT_PIN = "            entity.setTenantCode(DEFAULT_TENANT);"
DEDUPE_HIT = '        PermissionEntity existing = permissionMapper.selectOne(same);\n' \
             '        if (existing != null) {\n            return existing;\n        }'
KEYS_ALL = "            Arrays.asList(VIEW, CREATE, UPDATE, DELETE, EXPORT));"

RUNS = [
    ("J1 应用级查重退回 `entity_code = ?`（#48 的第一现场：每点一次多一行）",
     [(SVC, GRANT_ISNULL, '            same.eq("entity_code", entityCode);')],
     {S_IDEMPOTENT: A[S_IDEMPOTENT], H_IDEM: A[H_IDEM]},
     "SQL 里 `entity_code = NULL` 恒为 unknown，一行也匹配不上。单元那支红在条件形状，"
     "契约那支红在库里的行数 —— 同一处写错的两层证据，缺一层就只是巧合。"),

    ("J2 判定丢掉「或应用级」那一支（矩阵亮着，/check 却答拒绝）",
     [(SVC, CHECK_OR, '            q.eq("entity_code", scope);')],
     {S_SCOPECHECK: A[S_SCOPECHECK], H_SCOPE: A["H_SCOPE_WIDE"],
      H_VERB: A["H_VERB_CHECKMATCH"]},
     "三条都红是同一因：任何拿应用级授权去问具体实体的地方都会答拒绝。第三支（大小写归一那支）"
     "是意外之喜 —— 它顺手证明了「归一之后 check 真认得这一行」不是靠两边都写歪碰上。"),

    ("J3 listByApp 丢掉租户条件（别的租户授的行混进本租户清单）",
     [(SVC, LIST_APP_TENANT, LIST_APP_TENANT.replace(
         '                        .eq("tenant_code", require(tenantCode, "tenantCode"))\n', '', 1))],
     {S_TENANTREAD: A[S_TENANTREAD], H_FOREIGN: A["H_FOREIGN_LIST"], S_BLANK: A[S_BLANK]},
     "契约那支先用直接 SQL 播一行 foreign 租户的合法授权（写入口种不出这种行，见 J11 的注释），"
     "所以这一支在 API 层**注入不出来** —— 只有 service 层丢条件才看得见。"
     "第三条连带是实测拿到的、也是最值钱的一条: 被摘掉的那一行 `.eq(\"tenant_code\", require(tenantCode, "
     "...))` 同时负着两件事 —— 过滤，以及\"空租户不许当成不过滤\"。摘掉它，`listByApp(\"  \", ...)` 也不再"
     "抛（实测红在 blankTenantOrKey...:221 那句 fail）。一行守两条性质，所以这一支不许只算一条红。"),

    ("J4 grant 不做归一也不查词表（任何字符串都存得进去）",
     [(SVC, GRANT_CANONICAL, "        String permission = entity.getPermission();")],
     {S_REJECT: A[S_REJECT], S_CANONICAL: A[S_CANONICAL], H_VERB: A[H_VERB]},
     "单元那支先撞上「必须被写入口拒掉」；契约那支的红落在同一句的接口形态上（success 仍为 true）。"
     "规范形态那支单独红是因为大小写：存 'view' 而 check 问 'VIEW' 会永远答拒绝。"),

    ("J5 回收不按租户圈定（谁的 id 都删得掉）",
     [(SVC, REVOKE_TENANT, REVOKE_TENANT.replace(
         '                .eq("tenant_code", require(tenantCode, "tenantCode"))\n', '', 1))],
     {S_REVOKE: A[S_REVOKE], H_FOREIGN: A["H_FOREIGN_REVOKE"]},
     "契约那支在这里红的是「报成成功」而不是「别人的行被删了」：回收成功删掉了 foreign 那一行，"
     "于是 success=true —— 后面的 `assertEquals(1, permRowCount(app, other))` 是同一因的下游，"
     "一支方法只报第一条。"),

    ("J6 控制器把 0 行也报成「已回收」（#48 里界面那句假话的源头）",
     [(CTL, CTL_ZERO_ROWS, "        if (removed < 0) {")],
     {H_FOREIGN: A["H_FOREIGN_REVOKE"]},
     "只有契约层红：service 的返回值（受影响行数）这一支是对的，被改掉的是「0 行要不要说成功」。"
     "与 J5 互补 —— 那边是删错了对象，这边是什么都没删却报成功。"
     "「回收一个从没存在过的 id」那一形状由部署层 [15t] 钉，见 D3。"),

    ("J7 /check 的 entityCode 恢复成必填（问「整个应用」那一档直接 400）",
     [(CTL, CTL_ENTITY_REQUIRED,
       "    public Result<Boolean> check(@RequestParam String appCode,\n"
       '                                @RequestParam String entityCode,')],
     {H_SCOPE: A['C_TRANSPORT_EMPTY']},
     "实测推翻了我对这一支的因果猜测: 我预期红在 checkAllows 里那句状态断言（2139 行），"
     "实际红在**更外一层** ——夹具 `call()` 的\"响应不许是空体\"（246 行）。MockMvc 对缺必填参数的 "
     "400 根本不发 body，所以那句\"坏消息不该以 400 回来\"永远轮不到。这既是这一支该钉的东西"
     "（接口层留空能不能问，由 HTTP 参数形状决定，与判定逻辑无关），也顺手记下了一件别的事: "
     "MockMvc 的 400 空体与真容器的 200+code:400 两种坏消息不同形 —— 见 README 里 #34 那条未收口项。"),

    ("J8 「整个应用」那一档不再要求 IS NULL（某个实体的授权就能冒充全应用可用）",
     [(SVC, CHECK_WIDE_ISNULL,
       '        if (scope == null) {\n'
       '            // 注入：整个应用那一档不再要求 IS NULL\n'
       '        } else {')],
     {S_WIDECHECK: A[S_WIDECHECK], H_SCOPE: A["H_SCOPE_FAKE_WIDE"]},
     "与 J2 正好相反的一支：J2 让应用级行不参与实体判定，这一支让任何行都参与「整个应用」判定。"
     "两条凑在一起才钉住「entityCode 给没给」这两种语义各是一支条件，而不是一个 if 的开关。"),

    ("J9 词表那道闸摘掉（canonical 只 trim + 大写，什么词都放行）",
     [(KEYS, KEYS_GATE, "        if (false && !ALL.contains(wanted)) {")],
     {S_REJECT: A[S_REJECT], S_SCOPEUNKNOWN: A[S_SCOPEUNKNOWN], H_VERB: A[H_VERB]},
     "比 J4 更靠里一层：J4 是 service 不调 canonical，这一支是 canonical 自己放行。"
     "红的是「问不出口的问题不能被答成拒绝」那一条 —— 判定的未知项也一起漏了闸。"),

    ("J10 require() 放过空白（空租户被当成「不过滤」）",
     [(SVC, REQUIRE_BLANK, "        if (value == null) {")],
     {S_BLANK: A[S_BLANK]},
     "只有单元层红：HTTP 层永远带 appCode/租户（控制器钉死），空串这一族只有 service 直接调用"
     "才碰得到 —— 也就是网关/其它 service 复用它的那条路。"),

    ("J11 控制器不再覆写调用方自报的租户（写入口能往别的租户种行）",
     [(CTL, CTL_TENANT_PIN, "            // 注入：不再覆写调用方自报的租户")],
     {H_TENANT: A["H_TENANT_PINNED"]},
     "这一支与 J3 是一对：J3 拆读侧的租户条件，J11 拆写侧的钉死。只修一边都还剩一条通道 —— "
     "读侧钉死而写侧放开，等于让人自己往别的租户种行；反过来则种不出 foreign 行，"
     "所以 J3 只能靠直接 SQL 的夹具才看得见。"),

    ("J12 实体级查重退回 IS NULL（同一份实体授权永远查不中，且认错了应用级那一条）",
     [(SVC, GRANT_EQ, '            same.isNull("entity_code");')],
     {S_ENTITYDEDUPE: A[S_ENTITYDEDUPE], H_IDEM: A["H_IDEM_ENTITY"]},
     "J1 的镜像：那边应用级那一支错，这边实体级那一支错。两边都只测一边的话，"
     "「范围两种语义」就只有一半有牙。"),

    ("J13 命中查重也再插一行（幂等只剩注释）",
     [(SVC, DEDUPE_HIT,
       '        PermissionEntity existing = permissionMapper.selectOne(same);\n'
       '        if (existing != null && false) {\n            return existing;\n        }')],
     {S_EXISTING: A[S_EXISTING], H_IDEM: A[H_IDEM]},
     "查重条件本身是对的，只是不再短路 —— 这一支补的是「IS NULL 写对了但结果没用」的形状，"
     "J1 打不到它（J1 改的是条件，不是分支）。"),

    ("J14 词表少一项 EXPORT（矩阵的列与判定能认的词从此不一致）",
     [(KEYS, KEYS_ALL, "            Arrays.asList(VIEW, CREATE, UPDATE, DELETE));")],
     {S_REGISTRY: A[S_REGISTRY], H_VERB: A["H_VERB_ALLOWED"], H_SCOPE: A["C_HELPER_STATUS"],
      S_REJECT: A['S_REJECT_LISTS_EXPORT']},
     "第三支是算得到的连带：EXPORT 从此不在词表里，`/check ... permission=EXPORT` 就成了"
     "问不出口的问题（400），红在夹具的状态断言那一行。它说明这张表的每一项都真的有人用 —— "
     "删一项不会只是「少一列」，而是把那一列上的所有问题变成非法问题。"
     "第四支也是实测拿到的: 单元那句\"文案必须点齐每一项\"（`ex.getMessage().contains(\"EXPORT\")`）"
     "跟着红 —— 词表与\"允许哪些词\"这句文案是同一张表的两个面，少一项时两边一起塌，"
     "所以这一支在两层四处都留了证据。"),
]


def sh(cmd, timeout=2400):
    return subprocess.run(cmd, cwd=str(REPO), capture_output=True, text=True, timeout=timeout)


def md5(p: pathlib.Path) -> str:
    return hashlib.md5(p.read_bytes()).hexdigest()


def snapshot(targets):
    """备份目录**按本次运行**分名 —— 固定名会被上一支脚本的旧快照复用（那是真事故过的坑）。"""
    run_dir = LOGS / f"backup-run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    for f in targets:
        shutil.copyfile(f, run_dir / (f.name + ".orig"))
    return run_dir


def statement_start(src_lines, idx):
    """把「文案所在行」归到「那条 assert 调用的首行」。

    JUnit 报的是**调用**那一行，而长消息参数常被写到续行上 —— 不归这一口，一条正当的红会被
    读成"红在另一条"。
    """
    while idx > 0:
        prev = src_lines[idx - 1].rstrip()
        stripped = prev.lstrip()
        if not stripped or stripped.startswith(("//", "*", "/*")):
            break
        if prev.endswith((";", "{", "}", "*/")):
            break
        idx -= 1
    return idx + 1


def assert_anchors():
    """开跑前把地基钉三件事：锚点唯一、预期文案真在被测文件里且逐字唯一、预期红集两两不同。"""
    src = {f: f.read_text(encoding="utf-8") for f in (SVC, KEYS, CTL, UTEST, CTEST)}
    problems = []
    for path in (SVC, KEYS, CTL):
        if not src[path].strip():
            problems.append(f"产品源码是空的，注入会变成写文件: {path}")
    for _, edits, _, _ in RUNS:
        for path, anchor, repl in edits:
            n = src[path].count(anchor)
            if n != 1:
                problems.append(f"锚点出现 {n} 次（要 1 次）: {anchor.strip()[:60]}")
            if anchor == repl:
                problems.append(f"注入是空操作: {_[:40]}")
    lines = {}
    test_src = {UTEST.name: src[UTEST], CTEST.name: src[CTEST]}
    for t in sorted({x for _, _, exp, _ in RUNS for x in exp.values()}):
        hits = []
        for kind, text in test_src.items():
            for i, line in enumerate(text.splitlines()):
                if t in line:
                    hits.append((kind, i))
        if not hits:
            problems.append(f"预期红的文案在两个被测文件里都找不到: {t}")
        elif len(hits) != 1:
            problems.append(f"预期红的文案出现 {len(hits)} 次（要 1 次），它就不再是一条断言的名字: {t}")
        else:
            kind, i = hits[0]
            src_lines = src[UTEST if kind == UTEST.name else CTEST].splitlines()
            # 调用行与文案行都收：具体哪一行由 JUnit 说了算，但只能是这两行之一。
            lines[t] = {i + 1, statement_start(src_lines, i)}
    sets = {frozenset(exp.items()) for _, _, exp, _ in RUNS}
    if len(sets) != len(RUNS):
        problems.append("有两支注入的预期红集完全相同 —— 要么其中一支是重复检查，先去核文案")
    known = {m for _, _, exp, _ in RUNS for m in exp}
    allowed = {S_IDEMPOTENT, S_ENTITYDEDUPE, S_CANONICAL, S_REJECT, S_EXISTING, S_TENANTREAD,
               S_BLANK, S_SCOPECHECK, S_WIDECHECK, S_SCOPEUNKNOWN, S_REVOKE, S_REGISTRY,
               H_IDEM, H_VERB, H_SCOPE, H_FOREIGN, H_TENANT}
    for m in known:
        if m not in allowed:
            problems.append(f"预期红点名了一个不存在的方法: {m}")
    return problems, lines


def run_suite(label):
    """跑 PermissionServiceTest + LcHttpContractTest，返回 (分母, [(方法, 断言行号, 消息), ...])。

    ⚠ `-Dmaven.test.failure.ignore=true` 是必须的，不是便利：两个类分属两个模块，而 z-lc-core
    在 reactor 里排在 z-lc-web 前面 —— 单元层一红，maven 就 SKIPPED 掉整个 z-lc-web，契约层的
    XML 根本不会生成（实测 J1 第一版就是这么"没有 surefire XML"中止的）。到没到位一律只看两份
    XML，不看 mvn 的退出码。
    """
    for xml in (UXML, CXML):
        xml.unlink(missing_ok=True)
    proc = sh(["mvn", "-o", "-B", "-pl", "z-lc-core,z-lc-web", "-am", "test",
               "-Dtest=PermissionServiceTest,LcHttpContractTest",
               "-Dsurefire.failIfNoSpecifiedTests=false",
               "-Dmaven.test.failure.ignore=true"])
    (LOGS / f"{label}.log").write_text(proc.stdout + "\n--- stderr ---\n" + proc.stderr, encoding="utf-8")
    if "COMPILATION ERROR" in proc.stdout or "cannot find symbol" in proc.stdout:
        raise SystemExit(f"变异体编译不过（它不测任何东西，这一轮没有结论），见 {LOGS / (label + '.log')}")
    missing = [str(x) for x in (UXML, CXML) if not x.exists()]
    if missing:
        raise SystemExit(f"没有 surefire XML —— 套件根本没跑到 {missing}，见 {LOGS / (label + '.log')}")
    cases = []
    for xml in (UXML, CXML):
        cases.extend(ET.parse(xml).getroot().findall("testcase"))
    reds = []
    for tc in cases:
        node = tc.find("failure")
        if node is None:
            node = tc.find("error")
        if node is None:
            continue
        cls = (tc.get("classname") or "").split(".")[-1]
        trace = (node.text or "") + " " + (node.get("message") or "")
        m = re.search(re.escape(cls + ".java") + r":(\d+)", trace)
        reds.append((f"{tc.get('classname')}.{tc.get('name')}", int(m.group(1)) if m else -1,
                     node.get("message") or ""))
    if proc.returncode not in (0, 1):
        raise SystemExit(f"mvn 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，见 "
                         f"{LOGS / (label + '.log')}")
    print(f"  {label}: 分母 {len(cases)} 条 / 红 {len(reds)} 条")
    return len(cases), reds


def judge(expected, reds, lines):
    """预期红按 (方法, 该文案所在的源码行) 认；没点名的红一律算意外，且要逐条给因果。"""
    by_method = {}
    for name, line, _msg in reds:
        by_method.setdefault(name, []).append(line)
    bad = 0
    for method, title in sorted(expected.items()):
        hit = [ln for ln in by_method.get(method, []) if ln in lines[title]]
        if hit:
            print(f"  OK  预期那条真的红了: {method.split('.')[-1]}:{hit[0]} 「{title}」")
        else:
            got = by_method.get(method)
            print(f"  !!  预期变红却没红: {method.split('.')[-1]} 「{title}」"
                  + (f"（它红在另一条，行 {got[0]} —— 那就是这一支注入实际打掉的东西）" if got
                     else "（这一支整个是绿的）"))
            bad += 1
    for method, hits in sorted(by_method.items()):
        if method not in expected:
            print(f"  !!  未预期的红: {method.split('.')[-1]} 行 {hits[0]} —— 不许加白名单了事，"
                  "先拿样本证明这一处注入换掉了哪一支断言，再改预期")
            bad += 1
    return bad


def main() -> int:
    problems, lines = assert_anchors()
    if problems:
        for p in problems:
            print(f"!! {p}")
        return 2

    LOGS.mkdir(parents=True, exist_ok=True)
    targets = sorted({path for _, edits, _, _ in RUNS for path, _, _ in edits}, key=str)
    originals = {path: path.read_text(encoding="utf-8") for path in targets}
    digest0 = {path: md5(path) for path in targets}
    run_dir = snapshot(targets)
    print(f"备份: {run_dir}")

    bad = 0
    denom = None
    try:
        print("=== 基线（两层必须全绿）===")
        denom, reds = run_suite("00_baseline")
        if reds:
            for name, line, _ in reds:
                print(f"  !! 基线就红: {name.split('.')[-1]}:{line}")
            print("  !! 基线有红，注入结果无法归因；先修基线")
            return 2

        for i, (tag, edits, expected, note) in enumerate(RUNS, start=1):
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} ===\n    因果: {note}")
            for path, anchor, repl in edits:
                path.write_text(originals[path].replace(anchor, repl, 1), encoding="utf-8")
            try:
                n, reds = run_suite(label)
                if n != denom:
                    print(f"  !! 分母从 {denom} 变成 {n} —— 套件没按同一批测试跑，这一轮不算")
                    bad += 1
                bad += judge(expected, reds, lines)
            finally:
                for path in targets:
                    path.write_text(originals[path], encoding="utf-8")
                    if md5(path) != digest0[path]:
                        print(f"  !! {path.name} 没还原到基线字节")
                        bad += 1

        print("\n=== 全部还原后复跑 ===")
        n, reds = run_suite("99_restored")
        if n != denom:
            print(f"  !! 复跑分母 {n} != {denom}")
            bad += 1
        if reds:
            print(f"  !! 还原后仍红: {[r[0].split('.')[-1] for r in reds]}")
            bad += 1
    finally:
        for path in targets:
            if path.read_text(encoding="utf-8") != originals[path]:
                path.write_text(originals[path], encoding="utf-8")
                print(f"  兜底还原 {path.name}")
            if md5(path) != digest0[path]:
                print(f"  !! 收尾校验失败: {path.name} 的字节和开跑前不一样")
                bad += 1

    owned = len({(m, t) for _, _, exp, _ in RUNS for m, t in exp.items()})
    print(f"\nRESULT: "
          + ("permission java-layer falsification done" if bad == 0 else f"{bad} problem(s)")
          + f" | {len(RUNS)} 支注入 / 认领 {owned} 条断言 / 基线分母 {denom}")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
