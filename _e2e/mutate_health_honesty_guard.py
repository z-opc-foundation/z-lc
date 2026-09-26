#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #52 的注入自证（「补测试要同轮注入缺陷自证」）。

修的是什么（全都 09-26 在**部署件**上量出来的，不是推理）：
  1) `/api/lc/health` 的 `status` 是硬编码的 `"UP"`，控制器里连 DataSource 都没有注入 ——
     Case A（不设任何 SPRING_DATASOURCE_*）：6.656 s 起来、health 回 200 `"status":"UP"`，
     而同一个进程 `/api/lc/app/list` 回 `http=500 处理失败|Connection refused`，
     日志里 `{dataSource-2} init error, url: jdbc:mysql://localhost:3306/?…`（库名为空）。
  2) url 坏得一眼可见时不 fail-fast：`HIDE_IN_REPO` 字面量照样 init（Druid 默认 initial-size=0
     一条连接都不试）；设成空串则 Boot 见 classpath 有 h2 就**自己造**一个 `jdbc:h2:mem:<随机>`
     （Case C 只把驱动换成 org.h2.Driver 就干净启动 2.937 s），数据写进一个重启即蒸发的库。
  3) 把不接这个 URL 的驱动交给**活的** Druid：`driver.connect()` 返回 null ⇒ create-connection
     线程 NPE 且**无退避**死循环 —— 实测 8 分钟刷出 13 GB 日志。这条是本族量具的保险丝存在的理由。

四支文件、六个闸点：探活聚合(H1/N1/N2)、被探的池集合(H2)、连接归还(H3)、校验 SQL 要有回话(H4)、
超时上限(H5)、凭证抹除(H6)、主池闸的判据与装配(H7/H8/H11/H13/H14/H18/N3)、模块池不再偷偷编串(H9/H10)、
驱动与 url 的配套性(H15/H16)和它"认得才判"的让步(H17)。
H13（闸还在、启动路径上没人调它）是这一族最要紧的一支：单测直接调方法照绿，只有装配层的用例看得见它。
H14/H17 是一对方向相反的：前者证明"闸自己没接到配置"会红而不是静默放行，后者证明"没见过就不判"
那条让步真的有东西在守 —— fail-fast 的判据两头都可能漂，只钉一头等于没钉。

预期红集是**先写下再去量**的；对不上就当场诊断，改的是这本账，不是断言。

用法: python3 _e2e/mutate_health_honesty_guard.py
日志与备份一律在 ~/.cache（/tmp 会被别的会话扫掉）。
"""
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time

REPO = "/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc"
sys.path.insert(0, os.path.join(REPO, "z-lc-admin-ui", "e2e"))
import _mutlock  # noqa: E402

PROBER = "z-lc-web/src/main/java/com/zifang/z/lc/web/health/DataSourceHealthProber.java"
GUARD = "z-lc-web/src/main/java/com/zifang/z/lc/web/config/DataSourceConfigGuard.java"
LCDS = "z-lc-web/src/main/java/com/zifang/z/lc/web/config/LcModuleDataSource.java"
CTRL = "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/HealthController.java"

CACHE = os.path.expanduser("~/.cache/zlc52_health_guard")
FUSE_LINES = 600000          # 保险丝：一支变异把日志刷过这个行数就判跑飞（见开头第 3 条）
CMD = ["mvn", "-o", "-B", "-pl", "z-lc-web", "test",
       "-Dtest=DataSourceHealthProberTest,DataSourceConfigGuardTest,LcStartupRefusalTest,LcHttpContractTest",
       "-Dsurefire.failIfNoSpecifiedTests=false"]

NAMED = [
    # 探活层
    "reportsUpOnlyFromRealProbes", "reportsDownAndNamesTheFailingPool", "probeIsBoundedWhenPoolStarves",
    "probeReleasesEveryConnectionItBorrowed", "emptyResultIsNotSuccess", "zeroPoolsIsNotHealthy",
    "neverLeaksCredentialsInHealthPayload",
    # 配置层（单测口径）
    "rejectsPlaceholderUrl", "rejectsBlankUrl", "leavesAbsentPropertyAlone", "acceptsRealJdbcUrls",
    "usablePredicateIsPinned", "describeNeverEchoesCredentials", "lcPoolRefusesToFabricateLocalhostUrl",
    "lcPoolRejectsPlaceholderWithoutLeakingPassword", "lcPoolFallsBackToMainDataSourceUrl",
    "lcPoolPrefersExplicitModuleUrl", "explicitHostAndDatabaseStillUsesTemplate",
    # 驱动↔url 配套性（13 GB 日志那一支的判据本身）
    "mainPoolRefusesDriverUrlMismatch", "lcPoolRefusesDriverUrlMismatchTheOtherWay",
    "mismatchViaFallbackChainIsAlsoRefused", "unknownDriverOrSchemeIsLeftAlone",
    "consistentPairsAreLetThrough", "refusesToRunBlindWithoutEnvironment",
    # 装配层
    "guardRunsAsPartOfTheBeanLifecycle", "guardRunsBeforeAnyDataSourceIsCreated",
    "refusesPlaceholderLcUrlAtStartup", "bootsWhenBothPoolsAreConfigured",
    # 契约层（真 HTTP）
    "healthCarriesOneRealProbePerDataSource",
]

MASK_HEAD = """    public static String mask(String url) {
        if (url == null) {
            return "unknown";
        }"""
MASK_BODY = """    public static String mask(String url) {
        return url == null ? "unknown" : url;
    }

    private static String mask_disabled(String url) {
        if (url == null) {
            return "unknown";
        }"""

MUTANTS = [
    ("H1 总体状态改回写死 UP（缺陷#52 的原样）",
     [(PROBER, '        out.put("status", allUp ? "UP" : "DOWN");', '        out.put("status", "UP");')],
     {"reportsDownAndNamesTheFailingPool", "probeIsBoundedWhenPoolStarves",
      "emptyResultIsNotSuccess", "zeroPoolsIsNotHealthy"}),

    ("H2 健康检查只探主池（z-lc 所有业务查询走的模块池不在探活集合里）",
     [(CTRL, "Map<String, Object> data = new LinkedHashMap<>(healthProber.report(dataSources));",
       "Map<String, Object> data = new LinkedHashMap<>(healthProber.report(\n"
       "                java.util.Collections.singletonMap(\"dataSource\", dataSources.get(\"dataSource\"))));")],
     {"healthCarriesOneRealProbePerDataSource"}),

    ("H3 探活借走的连接不还（缺陷#18 那一族在健康检查里复现）",
     # 上一版只把 `try (Connection conn = …) {` 换成 `try { …`，留下一只没有 catch/finally 的
     # try —— javac 当场 "'try' without 'catch', 'finally' or resource declarations"，这一支
     # 读成"没跑起来"而不是"红了"。编不过的变异体不是证据，是量具坏了：这里改成"取连接挪到
     # try 外面 + finally 里什么都不做"，语法完整而语义正是"借了不还"。
     [(PROBER, "        try (Connection conn = ds.getConnection()) {",
       "        final Connection conn = ds.getConnection();\n        try {"),
      (PROBER, '            out.put("latencyMs", Long.valueOf(latencyMs));\n            return out;\n        }\n    }',
       '            out.put("latencyMs", Long.valueOf(latencyMs));\n            return out;\n'
       '        } finally {\n            // conn.close() 被摘掉：借走的连接从此不再还\n        }\n    }')],
     {"probeReleasesEveryConnectionItBorrowed"}),

    ("H4 「SQL 跑完了」当「SQL 有回话」（SELECT 1 不返回行也算好）",
     [(PROBER, '                if (!rs.next()) {\n'
               '                    throw new IllegalStateException("SELECT 1 没有返回任何行");\n'
               '                }',
       '                rs.next();')],
     {"emptyResultIsNotSuccess"}),

    ("H5 去掉探活超时上限（池借干时健康检查自己一起挂死）",
     [(PROBER, "            return future.get(timeoutMs, TimeUnit.MILLISECONDS);",
       "            return future.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);")],
     {"probeIsBoundedWhenPoolStarves"}),

    ("H6 抹凭证那一步被摘掉（公开端点回显带密码的 JDBC 串）",
     [(GUARD, MASK_HEAD, MASK_BODY)],
     # 预期集补 mainPoolRefusesDriverUrlMismatch（18:39 本轮实测）：驱动↔url 那条报错里也带了
     # mask(url)，mask 一停就多出第三个漏点。这支测试本来就写着"异常也是对外面"，红得对，是我漏记。
     {"neverLeaksCredentialsInHealthPayload", "describeNeverEchoesCredentials",
      "lcPoolRejectsPlaceholderWithoutLeakingPassword", "mainPoolRefusesDriverUrlMismatch"}),

    ("H7 主池那道闸认得 HIDE_IN_REPO（占位符本来就是给自己兜底的，等于没闸）",
     [(GUARD, "        if (!isUsable(url)) {", "        if (!isUsable(url) && !PLACEHOLDER.equals(url)) {")],
     # 预期集补 guardRunsBeforeAnyDataSourceIsCreated（18:39 本轮实测）：那支顺序断言是双向的 ——
     # 既要求"池没在闸之前出生"，也要求 refresh 真的抛。闸认得占位符 ⇒ 上下文起来 ⇒ assertThrows 先红。
     {"rejectsPlaceholderUrl", "guardRunsAsPartOfTheBeanLifecycle",
      "guardRunsBeforeAnyDataSourceIsCreated"}),

    ("H8 「像个串」的口径漂成「非 null 就算」",
     [(GUARD, '        return url != null && url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:");',
       "        return url != null;")],
     # 预期集补 rejectsPlaceholderUrl（250 本轮实测）：口径放宽后**第一个漏网的就是占位符本身**
     # —— 我当初把账花在 blank/absent 那几个边角上，反倒漏了这一族的主猎物。
     # 再补 guardRunsBeforeAnyDataSourceIsCreated（18:39 本轮实测）：同 H7，那支也要求 refresh 真抛。
     {"usablePredicateIsPinned", "rejectsBlankUrl", "rejectsPlaceholderUrl",
      "lcPoolRejectsPlaceholderWithoutLeakingPassword",
      "guardRunsAsPartOfTheBeanLifecycle", "guardRunsBeforeAnyDataSourceIsCreated",
      "refusesPlaceholderLcUrlAtStartup"}),

    ("H9 模块池退回「没配就偷偷编一个 localhost:3306」（缺陷#52 的成因）",
     [(LCDS, "        if (!DataSourceConfigGuard.isUsable(jdbcUrl)) {", "        if (false) {")],
     # 预期集补 explicitHostAndDatabaseStillUsesTemplate（250 本轮实测）：那一句 if 同时挂着两条
     # 承诺 —— 不编造 localhost，和"显式给了 host+database 就仍走 z-boot 模板"。buildDataSource
     # 在这段里面，摘掉整段等于两条一起没。一条 if 担两件事，注入就是一支打两根。
     {"lcPoolRefusesToFabricateLocalhostUrl", "lcPoolRejectsPlaceholderWithoutLeakingPassword",
      "explicitHostAndDatabaseStillUsesTemplate",
      "refusesPlaceholderLcUrlAtStartup"}),

    ("H10 单库部署那一档 fallback 摘掉（只写 spring.datasource.* 的部署从此接不上库）",
     [(LCDS, '                "z.base.db.lc.jdbc-url", "z.base.db.default.jdbc-url", "spring.datasource.url");',
       '                "z.base.db.lc.jdbc-url", "z.base.db.default.jdbc-url");')],
     {"lcPoolFallsBackToMainDataSourceUrl"}),

    ("H11 「属性没出现就不拦」那条让步被摘掉（只靠 classpath 起内嵌库的上下文一起挂）",
     # 注入必须**语法完整**：上一版把整段换成 `if (false) {` 留下一只孤括号，编译就没过，
     # 于是这一支读成"没跑起来"而不是"红了/逃了" —— 编不过的变异体不是证据，只是量具坏了。
     [(GUARD, "        if (!env.containsProperty(\"spring.datasource.url\")) {\n            return;\n        }",
       "        if (false) {\n            return;\n        }")],
     {"leavesAbsentPropertyAlone"}),

    ("H12 「一个池都没有」算健康（分母为空给满分）",
     [(PROBER, "        boolean allUp = pools != null && !pools.isEmpty();", "        boolean allUp = true;")],
     {"zeroPoolsIsNotHealthy"}),

    ("H13 闸还在、启动路径上没有人调它（早先那支 @PostConstruct 的等价体）",
     # 校验从 @PostConstruct 搬到了 BeanFactoryPostProcessor（见 H18：那条接线本身也有一支），
     # 所以"摘注解"这个动作已经不存在了 —— 同形的缺陷是 postProcessBeanFactory 的函数体变空。
     [(GUARD, "        validateMainDataSourceUrl();",
       "        // 方法还在，但启动路径上再没有人调它")],
     {"guardRunsAsPartOfTheBeanLifecycle", "guardRunsBeforeAnyDataSourceIsCreated"}),

    ("H14 闸自己没接到 Environment 时静默放行（fail-open）",
     [(GUARD, "        if (env == null) {", "        if (false) {")],
     {"refusesToRunBlindWithoutEnvironment"}),

    ("H15 主池不再判驱动与 url 的配套性（13 GB 日志那一支的判据被摘）",
     [(GUARD, "        String mismatch = driverUrlMismatch(driver, url);\n        if (mismatch != null) {",
       "        String mismatch = driverUrlMismatch(driver, url);\n        if (false && mismatch != null) {")],
     {"mainPoolRefusesDriverUrlMismatch"}),

    ("H16 模块池不再判驱动与 url 的配套性（fallback 链凑出来的那一对也放过）",
     [(LCDS, "        String mismatch = DataSourceConfigGuard.driverUrlMismatch(driver, jdbcUrl);\n        if (mismatch != null) {",
       "        String mismatch = DataSourceConfigGuard.driverUrlMismatch(driver, jdbcUrl);\n"
       "        if (false && mismatch != null) {")],
     {"lcPoolRefusesDriverUrlMismatchTheOtherWay", "mismatchViaFallbackChainIsAlsoRefused"}),

    ("H17 「认得才判」的让步被摘掉（没见过的驱动一律当成对不上）",
     # 这一支和 H14~H16 方向相反：那条让步是有意的（误拦会把一个本来能跑的部署挡在门外），
     # 所以"更严格"同样是缺陷，也得有一支注入能红 —— 否则让步只是注释里的一句话。
     [(GUARD, "        if (family == null || scheme == null || family.equals(scheme)) {",
       "        if (family != null && scheme != null && family.equals(scheme)) {")],
     {"unknownDriverOrSchemeIsLeftAlone", "acceptsRealJdbcUrls"}),

    ("N1 明细都探了，但 DOWN 不参与总体判定（只丢聚合那一步）",
     [(PROBER, '                if (!"UP".equals(one.get("status"))) {\n                    allUp = false;\n                }',
       '                // 聚合被摘掉')],
     {"reportsDownAndNamesTheFailingPool", "probeIsBoundedWhenPoolStarves", "emptyResultIsNotSuccess"}),

    ("N2 探活照跑但明细不落账（响应里只有一句总括，运维查不到是哪个池）",
     [(PROBER, "            sources.add(one);", "            // 明细不落账")],
     # 预期集补 neverLeaksCredentialsInHealthPayload（250 本轮实测）：那支测试是**双向**的 ——
     # 既要求响应体里没有 password，也要求带凭证的那条 jdbc:mysql://… 以**抹过**的形式真的出现
     # （否则"响应里根本没有 url"也能让前一半空跑）。明细不落账把后一半的猎物一起带走了，
     # 红得对，是我漏记。
     {"reportsUpOnlyFromRealProbes", "reportsDownAndNamesTheFailingPool", "probeIsBoundedWhenPoolStarves",
      "neverLeaksCredentialsInHealthPayload",
      "healthCarriesOneRealProbePerDataSource"}),
]


def sh(cmd, timeout=3600):
    return subprocess.run(cmd, cwd=REPO, shell=False, capture_output=True, text=True, timeout=timeout)


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


KEEP = re.compile(r"Tests run:|<<<|\[ERROR\]|BUILD ")


def run_suite():
    """跑一次四把尺；返回 (具名红, 汇总行, 摘录, 有没有跑飞/没跑起来)。

    逐行读，只留得下的那些行 —— 这一族有过「无退避死循环刷 13 GB 日志」的先例，
    所以行数过保险丝就当这一支没跑成，绝不拿半截读数当证据。
    """
    p = subprocess.Popen(CMD, cwd=REPO, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                         text=True, bufsize=1)
    kept, reds, summaries, total, fused = [], set(), [], 0, False
    for line in p.stdout:
        total += 1
        if total % 50 == 0 and time.time() - run_suite.deadline > 900:
            p.kill()
            fused = True
            kept.append("!! 这一支超过 900s，判跑飞")
            break
        if total > FUSE_LINES:
            p.kill()
            fused = True
            kept.append("!! 日志行数过保险丝 %d，判跑飞" % FUSE_LINES)
            break
        if KEEP.search(line):
            if len(kept) < 4000:
                kept.append(line.rstrip())
            if "Tests run" in line and "in com.zifang" in line:
                summaries.append(line.strip())
            if "<<<" in line:
                for name in NAMED:
                    if name in line:
                        reds.add(name)
    rc = p.wait()
    broken = fused or not summaries
    if not summaries:
        kept.append("!! 没有任何 `Tests run ... in com.zifang` 汇总行（这一层没跑起来，rc=%d）" % rc)
    return reds, summaries, "\n".join(kept), broken, total, rc


def main():
    run_suite.deadline = time.time()
    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = os.path.join(CACHE, stamp)
    os.makedirs(backup, exist_ok=True)
    files = sorted({p for _, edits, _ in MUTANTS for p, _, _ in edits})
    originals = {rel: read(rel) for rel in files}
    baseline_digest = {rel: digest(os.path.join(REPO, rel)) for rel in files}
    for rel in files:
        shutil.copyfile(os.path.join(REPO, rel), os.path.join(backup, rel.replace("/", "__")))
    log = open(os.path.join(backup, "guard.log"), "w", encoding="utf-8", buffering=1)

    def out(msg):
        print(msg, flush=True)
        log.write(msg + "\n")
        log.flush()

    out("=== 缺陷#52 注入自证 run=%s（参照集=本次运行开始时的字节，不是 git HEAD）" % stamp)

    reds, summaries, raw, broken, total, rc = run_suite()
    for s in summaries:
        out("  " + s)
    out("基线: 行数=%d 具名红=%s" % (total, sorted(reds)))
    if reds or broken:
        out("FATAL 基线不干净（%s），后面的「谁红了」没有参照。\n%s" % (sorted(reds), raw[-6000:]))
        return 2

    problems = []
    for tag, edits, expected in MUTANTS:
        # 每一支都从**本次运行开始时的原始字节**出发改，落盘时只写这一支的那一处。
        # 反例是"改完再用 replace(new, old) 换回去": H1 注入出的那句
        # `out.put("status", "UP");` 在 connect() 里**本来就有一模一样的**，于是还原时
        # 两处一起被换成了 `allUp ? "UP" : "DOWN"`，`allUp` 在那个方法里不在作用域内 ——
        # 编译不过，H2~N2 十几支全部读成"这一层没跑起来"（实测：整轮只有 H1 有读数）。
        # 一支的还原动作碰了另一支的文本，后面的账就全废了。从原字节重写没有这个问题。
        staged = {}
        for rel, old, new in edits:
            base = staged.get(rel, originals[rel])
            if base.count(old) != 1:
                out("FATAL %s: 锚点在 %s 里出现 %d 次（要 1 次），不敢改。" % (tag, rel, base.count(old)))
                for r2 in originals:
                    write(r2, originals[r2])
                return 2
            staged[rel] = base.replace(old, new)
        for rel, text in staged.items():
            write(rel, text)
        reds, summaries, raw, broken, total, rc = run_suite()
        out("%s  (日志行数 %d)" % (tag, total))
        for s in summaries:
            out("    " + s)
        out("    预期红: %s" % sorted(expected))
        out("    实测红: %s%s" % (sorted(reds), "（这一层没跑起来）" if broken else ""))
        missing = sorted(expected - reds)
        extra = sorted(reds - expected)
        if broken:
            problems.append("%s: 量具没跑成（保险丝/无汇总行），这一支的读数不算数" % tag)
        if missing:
            problems.append("%s: 这几支注入没被捉住 %s" % (tag, missing))
            out("    !! 逃过: %s" % missing)
        if extra:
            problems.append("%s: 红了预期之外的 %s（预期按实测改，断言不许改软）" % (tag, extra))
            out("    !! 预期之外: %s" % extra)
        with open(os.path.join(backup, tag.split(" ")[0] + ".log"), "w", encoding="utf-8") as f:
            f.write(raw[-300000:])
        for rel in staged:
            write(rel, originals[rel])

    for rel in files:
        now = digest(os.path.join(REPO, rel))
        if now != baseline_digest[rel]:
            write(rel, originals[rel])
            now = digest(os.path.join(REPO, rel))
        same = now == baseline_digest[rel]
        out("还原 %s: %s" % (rel, "字节相同" if same else "!! 不同"))
        if not same:
            problems.append("还原失败 " + rel)

    reds, summaries, raw, broken, total, rc = run_suite()
    out("还原后复测: %s / 具名红=%s" % ("构建并跑齐" if not broken else "没跑起来", sorted(reds)))
    for s in summaries:
        out("  " + s)
    if broken or reds:
        problems.append("还原后仍不干净: red=%s broken=%s" % (sorted(reds), broken))

    out("备份/日志: %s" % backup)
    if problems:
        out("RESULT: %d problem(s)" % len(problems))
        for p_ in problems:
            out("  - " + p_)
        return 1
    out("RESULT: 缺陷#52 的 %d 支注入逐支按预期点名，产物已还原到基线字节" % len(MUTANTS))
    return 0


if __name__ == "__main__":
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
