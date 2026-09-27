#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #65 的注入自证：`/fires` 的分页信封（core service + web controller）。

#65 的原始形状：这一格一次最多给 200 条、而**不回总数**。界面把它切成"客户端分页"，
账长到 201 条那天，用户看到的是"这个实体还没有发起记录"——读不出来被说成没有。
修法分三层：core 的 `pageFires` 出 `{records,total,pageNum,pageSize}`、controller 回这个信封、
前端 `readFireWindow` 认这个形状并把它分开说。三层各自都有名字下的注入，一支不许含糊。

两条量具层（每支注入按 `tiers` 跑对应那层）：
  reactor   两个模块**各跑各的**（`mvn -o -B -pl z-lc-core -Dtest=WorkflowBindingServiceTest test`
            与 `-pl z-lc-web -Dtest=WorkflowTriggerContractTest test`，**故意不带 -am**，见 run_tests 注释）
            —— core 单测（替身真的按谓词数、按 LIMIT 切窗）+ 契约层（真 Spring + 真 H2 + 真 SQL）
            契约层从 ~/.m2 取 core，所以每支注入先 `install` 再跑这两条腿，两条量的是同一份变异。
  deployed  用变异后的 fat jar 重启 18090 → 跑整份接口层，只看 `[15w]` 那一节
            —— 证明"部署件里那个 class 真的换了、HTTP 上真的回这个形状"

注入清单（名字与预期红集一律抄自实测，见每支后面那条注释；对不上就 MISMATCH，不白名单）：
  F1 摘掉 COUNT，总数拿**这一页读出来的行数**顶 ⇒ 截断又变得不可见（就是 #65 那句谎）
  F2 摘掉 LIMIT 的 offset（只写条数）⇒ 第 2 页永远等于第 1 页
  F3 摘掉每页上限收口（size=5000 就真读 5000 并回显 5000）
  F4 COUNT 与行读**两套谓词**（数的时候不看 tenant/entity/record）⇒ 「共 N 条」数的是别人的账
  F5 排序翻成最旧在前 ⇒ 倒序那一格失去兑现
  F6 controller 回**裸数组**（#65 原样）⇒ 三层读形状的地方一起红

量具规则（每一条都是这个仓里付过代价的）：
  * 注入前把每个文件字节读进**本次运行独占**的备份目录，还原只从这份副本 cp + md5 对账；
    **不许**用 `git checkout --` 当还原步 —— git 基线是 HEAD，不是开局测量那一刻。
  * 每支注入前确认 anchor 在文件里**恰好出现一次**，否则当场 SKIPPED 记坏账（不猜）。
  * 分母钉死：reactor 两模块各自的 Tests run 数、deployed 的全量与 `[15w]` 本节条数，
    任何一轮漂了就不许说"没红"。
  * deployed 轮必须证明 fat jar 里**恰好一个** `.class` 变了；没变 ⇒ 这一轮什么都证明不了。
  * 一支注入在它该红的层里一条都不红 ⇒ 记坏账（要么补断言，要么写进 README 当覆盖缺口）。
  * 复跑期间不改被测源码；收尾"还原原始字节 → 重新 build → 重启 → 两层再各跑一遍"回绿。
  * 台账记 ran_by。
"""
import hashlib
import io
import json
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

SERVICE = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowBindingService.java"
CTRL = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/WorkflowBindingController.java"
FILES = sorted({SERVICE, CTRL}, key=lambda p: str(p))

LOGS = Path.home() / ".cache/zlc65mut"
BAK = LOGS / "bak"
LEDGER = LOGS / "ledger.json"
BOOT_LOG = LOGS / "mut_boot.log"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
SECTION = "15w"

# ---- anchors ---------------------------------------------------------------------------
F1_COUNT_ANCHOR = ('        Long counted = workflowFireMapper.selectCount(q);\n'
                   '        long total = counted == null ? 0L : counted;\n')
F1_COUNT_REPL = '        long total = -1L; // 注入 F1：不再问库要总数\n'
F1_RETURN_ANCHOR = ('        return new PageResult<WorkflowFireEntity>(\n'
                    '                workflowFireMapper.selectList(q), total, (long) p, (long) s);\n')
F1_RETURN_REPL = ('        java.util.List<WorkflowFireEntity> pageRows = workflowFireMapper.selectList(q);\n'
                  '        return new PageResult<WorkflowFireEntity>(\n'
                  '                pageRows, pageRows.size(), (long) p, (long) s);\n')

F2_ANCHOR = '        q.last("LIMIT " + (long) (p - 1) * s + "," + s);'
F2_REPL = '        q.last("LIMIT " + s);'

F3_ANCHOR = '        int s = size == null || size < 1 ? FIRE_PAGE_DEFAULT : Math.min(size, FIRE_PAGE_MAX);'
F3_REPL = '        int s = size == null || size < 1 ? FIRE_PAGE_DEFAULT : size;'

F4_ANCHOR = '        Long counted = workflowFireMapper.selectCount(q);'
F4_REPL = ('        Long counted = workflowFireMapper.selectCount(\n'
           '                new QueryWrapper<WorkflowFireEntity>().eq("app_code", appCode));')

F5_ANCHOR = '        q.orderByDesc("id");'
F5_REPL = '        q.orderByAsc("id");'

F6_SIG_ANCHOR = '    public Result<PageResult<WorkflowFireEntity>> fires(@RequestParam String appCode,'
F6_SIG_REPL = '    public Result<Object> fires(@RequestParam String appCode,'
F6_BODY_ANCHOR = ('        return Result.success(workflowBindingService.pageFires(\n'
                  '                DEFAULT_TENANT, appCode, entityCode, recordId, page, size));\n')
F6_BODY_REPL = ('        return Result.success((Object) workflowBindingService.pageFires(\n'
                '                DEFAULT_TENANT, appCode, entityCode, recordId, page, size).getRecords());\n')

EDITS = {
    "F1": [(SERVICE, F1_COUNT_ANCHOR, F1_COUNT_REPL), (SERVICE, F1_RETURN_ANCHOR, F1_RETURN_REPL)],
    "F2": [(SERVICE, F2_ANCHOR, F2_REPL)],
    "F3": [(SERVICE, F3_ANCHOR, F3_REPL)],
    "F4": [(SERVICE, F4_ANCHOR, F4_REPL)],
    "F5": [(SERVICE, F5_ANCHOR, F5_REPL)],
    "F6": [(CTRL, F6_SIG_ANCHOR, F6_SIG_REPL), (CTRL, F6_BODY_ANCHOR, F6_BODY_REPL)],
}
TIERS = {
    # F2 与 F6 额外跑部署件层：HTTP 那一侧的"翻页真的换一次读"与"回的是信封不是裸数组"
    # 只有对着真 jar 跑过才算钉住（reactor 那两层量的是同一份字节，但走的是 MockMvc / 替身）。
    "F1": ["reactor"], "F2": ["reactor", "deployed"], "F3": ["reactor"],
    "F4": ["reactor"], "F5": ["reactor"], "F6": ["reactor", "deployed"],
}
# 预期红集：**逐字抄自 09-27 10:3x 那一轮 `--only F1..F6` 的实测读数**
# （transcript `~/.cache/zlc65mut/guard-0927-103651.log`，六支全部红、恢复轮回基线字节）。
# 名字按 surefire 的简单类名归一（同一支红它会打带包名和不带包名两回）。
EXPECT_REACTOR = {
    "F1": [
        "WorkflowBindingServiceTest#pageFiresShouldCapThePageButEchoTheWholeLedger",
        "WorkflowBindingServiceTest#pageFiresWindowShouldWalkTheWholeLedgerWithoutGaps",
        "WorkflowTriggerContractTest#fireLedgerReportsItsWholeWindow",
    ],
    "F2": [
        "WorkflowBindingServiceTest#pageFiresWindowShouldWalkTheWholeLedgerWithoutGaps",
        "WorkflowTriggerContractTest#fireLedgerReportsItsWholeWindow",
    ],
    "F3": [
        "WorkflowBindingServiceTest#pageFiresShouldCapThePageButEchoTheWholeLedger",
        "WorkflowTriggerContractTest#fireLedgerReportsItsWholeWindow",
    ],
    "F4": [
        "WorkflowBindingServiceTest#fixtureReallyAppliesWrapperPredicates",
        "WorkflowBindingServiceTest#pageFiresShouldBeTenantPinnedAndNewestFirst",
        "WorkflowTriggerContractTest#fireLedgerReportsItsWholeWindow",
    ],
    # F5 只有 1 条：排序这一格在契约层那一条 (fireLedgerReportsItsWholeWindow) 里不读顺序，
    # 而在部署件层 [15w] 那 72 条里也没有一条按位比顺序 —— 这一族的顺序只钉在 core 单测一处。
    # （不是"没红就是没网"：真红了，只是只有一处能红。）
    "F5": ["WorkflowBindingServiceTest#pageFiresShouldBeTenantPinnedAndNewestFirst"],
    "F6": [
        "WorkflowTriggerContractTest#bindingActuallyFiresAndTheLedgerReadsBackStarted",
        "WorkflowTriggerContractTest#duplicateBindingIsRefusedWhileTheFirstOneStillFires",
        "WorkflowTriggerContractTest#engineRefusalKeepsTheUsersWriteAndLeavesAFailedRow",
        "WorkflowTriggerContractTest#fireLedgerReportsItsWholeWindow",
        "WorkflowTriggerContractTest#foreignTenantBindingNeverFires",
        "WorkflowTriggerContractTest#http5xxWithASuccessfulLookingBodyIsNotAFire",
        "WorkflowTriggerContractTest#importAndRedoDoNotFire",
        "WorkflowTriggerContractTest#slowEngineIsCutOffAtTheConfiguredDeadline",
        "WorkflowTriggerContractTest#timeoutDoesNotPoisonTheNextFires",
        "WorkflowTriggerContractTest#unreachableEngineBecomesAFailedRowNotAnError",
        "WorkflowTriggerContractTest#unrealizableBindingsAreRefusedAndFireNothing",
        "WorkflowTriggerContractTest#updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice",
    ],
}
# deployed 层只有 F2/F6 跑（见 TIERS）。F6 那 21 条是设计如此：`/fires` 一旦回裸数组，
# 这一节里凡是"从信封读"的账一起塌 —— 红集大不是噪声，是那一格被多少条账当唯一出处。
EXPECT_API = {
    "F1": [], "F3": [], "F4": [], "F5": [],
    "F2": [
        "走完再往前一页：这一页没有行，而 total 仍然写着整张账那么多（空页 ≠ 这条链没发过单）",
        "逐页拼回来的行与一次读全的那批逐位相同、且不重号（翻页漏行/重行就是「没读出来」）",
    ],
    "F6": [
        "/fires 回的是分页信封：records/total/pageNum/pageSize 四栏齐（裸数组=这一格没有总数）",
        "/fires 读回这一条：正好一行",
        "5xx 时 body 里那个实例号一个字都不许留下，且失败原因说清是 http 几",
        "page/size 传成负数或 0：回到首页、回到默认每页条数，而不是报错也不是空集",
        "second 的账上有两行，且 recordId 各指各的记录",
        "size 超上限要收口，并且回显**收口之后**的那个值（回显请求值=界面上那格每页条数是假的）",
        "一次超时不许传染下一次：紧接着的一条又发得出去（2 槽池的槽位要在有上限之后自己回来）",
        "一页一条走到第 None 页：每页正好 1 行、页码回显对、每页都带着同一份总数",
        "一页装得下整张账时 total 就等于读出来的行数（>=2 是要有猎物，不是空跑）",
        "到点判 FAILED，而且原因点名的是那个默认预算本身",
        "发出去那一句在账上留下一行 STARTED",
        "带 recordId 时 total 也只数这一条（COUNT 用另一套谓词=界面写着「共 9 条」而只有一行）",
        "并且账上是一行 FAILED 带引擎那句原话",
        "引擎不可达：账上留一行 FAILED 并说得出为什么",
        "引擎回 5xx 而 body 写着成功：仍是 FAILED（这一格形状是 §2.3 的 M5 逼出来的）",
        "成功行不带失败原因，但带上它是哪条绑定的兑现",
        "没发单的实体在账上也没有行",
        "没登记绑定的实体 plain 读回 total=0 —— 这一条是上一条的猎物：COUNT 若不看 entityCode，这里就会是 None",
        "账上累积的行数与发出去的句子一样多（每一次尝试都留痕，成功的也不例外）",
        "走完再往前一页：这一页没有行，而 total 仍然写着整张账那么多（空页 ≠ 这条链没发过单）",
        "那一行是 STARTED，实例 id 就是 z-wf data.processInstanceId 那一格（不是整个 data 的 toString）",
    ],
}
ONLY = {a for a in sys.argv[1:] if a.startswith("F")}
UNKNOWN = [a for a in sys.argv[1:] if not a.startswith("F")]
if UNKNOWN:
    raise SystemExit(f"只认 F1..F6，收到 {UNKNOWN}")


def sh(cmd, timeout=2400):
    return subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True, timeout=timeout)


def snapshot_sources():
    run_dir = BAK / f"run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=True)
    originals = {}
    for f in FILES:
        if not f.exists():
            raise RuntimeError(f"源文件不在，连基线都量不了: {f}")
        disk = f.read_bytes()
        (run_dir / (str(f).replace("/", "_").lstrip("_") + ".orig")).write_bytes(disk)
        originals[f] = disk
    print(f"sources snapshotted -> {run_dir}")
    return originals


def artifact_map():
    out = {}
    with zipfile.ZipFile(JAR) as fat:
        libs = [n for n in sorted(fat.namelist()) if n.startswith("BOOT-INF/lib/z-lc-")]
        if not libs:
            raise RuntimeError(f"{JAR} 里没有嵌套的 z-lc-*.jar：零条目的指纹是常数，没有价值")
        for lib in libs:
            with zipfile.ZipFile(io.BytesIO(fat.read(lib))) as inner:
                for e in sorted(inner.namelist()):
                    if e.endswith(".class"):
                        out[f"{lib}!{e}"] = hashlib.md5(inner.read(e)).hexdigest()
    if not out:
        raise RuntimeError("class 指纹表是空的")
    return out


def fp_id(m):
    return hashlib.md5("|".join(f"{k}:{v}" for k, v in sorted(m.items())).encode()).hexdigest()[:12]


def changed_classes(base, cur):
    keys = set(base) | set(cur)
    return sorted(k for k in keys if base.get(k) != cur.get(k))


def port_listeners():
    out = subprocess.run(["lsof", "-nP", "-tiTCP:18090", "-sTCP:LISTEN"],
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
    pids = port_listeners()
    if len(pids) > 1:
        raise RuntimeError(f"18090 上有 {len(pids)} 个监听者 {pids}，不敢猜停哪一个")
    if pids:
        subprocess.run(["kill", str(pids[0])])
    waited = 0
    while port_listeners() and waited < 60:
        time.sleep(1)
        waited += 1
    if pids and port_listeners() == pids:
        # SIGTERM 收不掉就升级到 SIGKILL —— 只杀这一支已归因的 pid（argv 里就是本次这份 jar）。
        # 09-27 10:47 实测：正常退出时那个 JVM 停在 shutdown hook 里 8 分钟不退，端口一直被
        # 一个"类加载器已经关掉、还在答 HTTP"的半死件占着 —— 那种状态下"没红"是"没跑到"。
        print(f"  pid={pids[0]} 收到 SIGTERM 后 {waited}s 仍占着 18090，升级 SIGKILL")
        subprocess.run(["kill", "-9", str(pids[0])])
        waited = 0
        while port_listeners() and waited < 30:
            time.sleep(1)
            waited += 1
    if port_listeners():
        raise RuntimeError(f"端口 18090 在 {waited}s 之后仍被 {port_listeners()} 占着，不敢起新件")
    print(f"  旧进程已收干净（等了 {waited}s, 端口 18090 空）")
    BOOT_LOG.parent.mkdir(parents=True, exist_ok=True)
    log = open(BOOT_LOG, "ab")
    # start_new_session：不起新会话的话这个 JVM 属于本量具的进程组，量具一退出就跟着收 SIGTERM，
    # 于是"跑完了"与"部署件还在伺服"永远只能有一个成立（下面那条 kill 分支就是这么被触发的）。
    proc = subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                            cwd=str(ROOT), stdout=log, stderr=log, start_new_session=True)
    if not wait_health():
        tail = subprocess.run(["tail", "-20", str(BOOT_LOG)], capture_output=True, text=True).stdout.strip()
        raise RuntimeError(f"新件没起来 (launcher pid={proc.pid})；{BOOT_LOG} 末尾:\n{tail}")
    now = port_listeners()
    if len(now) != 1:
        raise RuntimeError(f"health 起来了但 18090 上有 {len(now)} 个监听者 {now}：归因不了")
    argv = subprocess.run(["ps", "-o", "command=", "-p", str(now[0])], capture_output=True, text=True).stdout
    if str(JAR.name) not in argv:
        raise RuntimeError(f"18090 的监听者 pid={now[0]} 伺服的不是本次这份 jar:\n{argv.strip()[:200]}")
    print(f"  归因成立：18090 的监听者 pid={now[0]}，argv 里就是 {JAR.name}")
    return now[0]


def build(skip_tests=True):
    cmd = ["mvn", "-o", "-B", "install", "-pl", "z-lc-core,z-lc-web,z-lc-admin", "-am"]
    if skip_tests:
        cmd.append("-DskipTests")
    p = sh(cmd)
    out = p.stdout + p.stderr
    if "BUILD SUCCESS" not in out:
        raise RuntimeError("jar build failed, tail:\n" + "\n".join(out.splitlines()[-30:]))


def run_tests(label, module, test_class):
    """单模块跑一支测试类。**故意不用 -am**：core 一红，reactor 就中止，下游模块一条不跑 ——
    那时"契约层没红"其实是"契约层没跑"。（09-27 10:2x 第一次冒烟就撞在这个形状上：
    F1 在 core 红了 2 条，整条 `[INFO] Tests run:` 尺读不到，被我的尺报成"尺坏了"。）
    所以 core 的红要先 install 进 ~/.m2，契约层那一条腿才量得到同一份变异。
    """
    p = sh(["mvn", "-o", "-B", "-pl", module, "-Dtest=" + test_class,
            "-Dsurefire.failIfNoSpecifiedTests=false", "test"])
    out = p.stdout + p.stderr
    (LOGS / f"tests_{label}.log").write_text(out)
    counts = [int(m.group(1)) for m in re.finditer(
        r"^\[(?:INFO|ERROR)\] Tests run: (\d+), Failures: \d+, Errors: \d+, Skipped: \d+$", out, re.M)]
    # surefire 同一支红会打两回：一次带全限定类名，一次只打简单名。按简单名归一，
    # 否则台账里每个洞都记两笔，包名一挪红集就对不上（而这与产品无关）。
    reds = sorted({f"{m.group(1).split('.')[-1]}#{m.group(2)}" for m in re.finditer(
        r"^\[ERROR\]\s+([\w.]+)\.([\w]+)(?::\d+)?\s", out, re.M)})
    if len(counts) != 1:
        raise RuntimeError(f"{module}/{test_class} 数出 {len(counts)} 条 Tests run（应为 1）—— "
                           f"多半是编译没过，这一支变异不算覆盖也不算网；"
                           f"见 {LOGS / f'tests_{label}.log'}")
    return counts[0], reds


def run_java_tiers(label):
    """core 单测 + web 契约测试，两条腿各跑各的，红集合合并记账。"""
    n_core, core_reds = run_tests(f"{label}-core", "z-lc-core", "WorkflowBindingServiceTest")
    n_web, web_reds = run_tests(f"{label}-web", "z-lc-web", "WorkflowTriggerContractTest")
    return (n_core, n_web), sorted(set(core_reds) | set(web_reds))


def run_api(label):
    t0 = time.time()
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py", "http://localhost:18090"],
                       cwd=str(ROOT), capture_output=True, text=True)
    (LOGS / f"api_{label}.log").write_text(p.stdout + p.stderr)
    summary = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    section, mine, sec_rows, n = None, [], 0, 0
    for line in p.stdout.splitlines():
        m = re.match(r"^\[(\d+\w*)\]", line)
        if m:
            if section == SECTION:
                sec_rows = n
            section, n = m.group(1), 0
            continue
        if line.startswith("  FAIL  "):
            name = line[8:].split("   <<")[0]
            if section == SECTION:
                mine.append(name)
            n += 1
        elif section == SECTION and line.startswith("  PASS  "):
            n += 1
    if section == SECTION:
        sec_rows = n
    # 分母 = 这一节的**行数**（PASS 与 FAIL 都算一行）。上面那个 `n += 1` 故意写在
    # FAIL 分支的外面：09-27 10:3x 我先只数 PASS（于是 F2 正常红 2 条被读成"分母 72→70"），
    # 又把 FAIL 加了一回（F2 变成 74、F6 变成 93）—— 两回都是尺坏，不是产品坏。
    sec_n = sec_rows
    if not summary:
        raise RuntimeError(f"接口层没打出 E2E RESULT 那一行，见 {LOGS / f'api_{label}.log'}")
    if "没跑到结尾" in p.stdout:
        # 量具自己的卫兵打了旗：这一轮的分母是残缺的，"本节没红"一句不成立。
        raise RuntimeError(f"接口层这一轮半途死（分母残缺），见 {LOGS / f'api_{label}.log'}")
    return int(summary.group(1)), int(summary.group(2)), sorted(mine), sec_n, round(time.time() - t0, 1)


def apply(tag):
    applied = []
    for path, anchor, repl in EDITS[tag]:
        text = path.read_text(encoding="utf-8")
        if text.count(anchor) != 1:
            print(f"[SKIPPED] {tag}: anchor 在 {path.name} 里出现 {text.count(anchor)} 次")
            return None
        path.write_text(text.replace(anchor, repl, 1))
        applied.append(path)
    return applied


def main():
    originals = snapshot_sources()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    restore_all()
    bad = 0
    crashed = []
    tags = [t for t in EDITS if not ONLY or t in ONLY]
    book = {"ran_by": f"{os.environ.get('USER','?')}@{time.strftime('%m%d-%H%M%S')}",
            "script": os.path.basename(__file__), "runs": []}
    try:
        LOGS.mkdir(parents=True, exist_ok=True)
        # 基线先 build 一次：`-pl z-lc-web` 那条腿从 ~/.m2 取 core，
        # 缓存里的 core 若不是这份源字节的，契约层量的就不是被测变异。
        build()
        base_fp = artifact_map()
        base_counts, base_reds = run_java_tiers("baseline")
        if base_reds:
            print(f"BASELINE reactor 不绿，中止。reds={base_reds}")
            return 1
        restart()
        ok_n, total, api_reds, sec_n, secs = run_api("baseline")
        print(f"\n=== baseline: reactor 每模块 Tests run={list(base_counts)} 全绿；"
              f"deployed {ok_n}/{total} 全量、本节 {sec_n} 条，artifact fp={fp_id(base_fp)}，一轮 {secs}s")
        if ok_n != total or api_reds or not sec_n:
            print(f"BASELINE deployed 不绿，中止。reds={api_reds}")
            return 1
        denoms = {"java_counts": list(base_counts), "e2e_total": total, "section": sec_n}
        book["denominators"] = denoms

        for tag in tags:
            rec = {"tag": tag, "tiers": TIERS[tag]}
            applied = apply(tag)
            if applied is None:
                # anchor 数不对就当场收手：apply() 可能已经改了前一条，必须还原，
                # 否则下一支注入判的是"上一支留下的一半"。
                restore_all()
                bad += 1
                continue
            print(f"\n[{tag}] 注入 {[p.name for p in applied]}（跑 {TIERS[tag]}）")
            # 先 install 再跑两条腿：install 出的 core 才是契约层要看的那份，
            # 顺便产出的 fat jar 给 deployed 层用（一次构建，两层同源）。
            build()
            fp = artifact_map()
            diff = changed_classes(base_fp, fp)
            counts, reds = run_java_tiers(f"mut{tag}")
            print(f"  reactor Tests run={list(counts)} 具名红 {len(reds)} 条:")
            for r_ in reds:
                print(f"     - {r_}")
            rec["java_counts"] = list(counts)
            rec["java_reds"] = reds
            rec["changed_classes"] = diff
            if tuple(counts) != tuple(base_counts):
                print(f"  !! reactor 分母漂了：{base_counts} → {counts}（编译没过的模块会被跳过，"
                      "那一条红都没有就是假绿）")
                bad += 1
            expect = EXPECT_REACTOR[tag]
            if not reds:
                print("  !! reactor 层一条红都没有：这一族的性质在单测/契约层没有网")
                bad += 1
            elif expect and sorted(reds) != sorted(expect):
                print(f"  !! MISMATCH(reactor): 预期 {len(expect)} 实跑 {len(reds)}")
                print(f"     只在预期里: {sorted(set(expect) - set(reds))}")
                print(f"     只在实跑里: {sorted(set(reds) - set(expect))}")
                bad += 1
            if ONLY:
                print(f"  EXPECT_REACTOR_{tag} = {json.dumps(reds, ensure_ascii=False, indent=1)}")

            if "deployed" in TIERS[tag]:
                restart()
                passed, this_total, mine, this_sec, s_secs = run_api(f"mut{tag}")
                print(f"  artifact fp={fp_id(fp)} changed={len(diff)} class: {diff[:4]}")
                print(f"  deployed = {passed}/{this_total} 本节 {this_sec}/{sec_n} 条（一轮 {s_secs}s）")
                print(f"  [{SECTION}] 红 ({len(mine)}):")
                for n_ in mine:
                    print(f"     - {n_}")
                rec["e2e"] = f"{passed}/{this_total}"
                rec["section"] = this_sec
                rec["api_reds"] = mine
                if not diff:
                    print("  !! 构件一个 class 都没变 -> 这一轮什么都证明不了")
                    bad += 1
                if this_total != total or this_sec != sec_n:
                    print(f"  !! 分母漂了：全量 {total}→{this_total}，本节 {sec_n}→{this_sec}")
                    bad += 1
                if not mine:
                    print("  !! 部署件层一条红都没有：HTTP 上这一格没有网")
                    bad += 1
                elif EXPECT_API[tag] and sorted(mine) != sorted(EXPECT_API[tag]):
                    print(f"  !! MISMATCH(api): 预期 {len(EXPECT_API[tag])} 实跑 {len(mine)}")
                    print(f"     只在预期里: {sorted(set(EXPECT_API[tag]) - set(mine))}")
                    print(f"     只在实跑里: {sorted(set(mine) - set(EXPECT_API[tag]))}")
                    bad += 1
                if ONLY:
                    print(f"  EXPECT_API_{tag} = {json.dumps(mine, ensure_ascii=False, indent=1)}")
            book["runs"].append(rec)
            restore_all()

        print("\n=== restore: 还原字节 → 重新 build → 重启 → 两层各再跑一遍 ===")
        build()
        fp = artifact_map()
        restart()
        counts, reds = run_java_tiers("restored")
        passed, last_total, mine, last_sec, _ = run_api("restored")
        print(f"  artifact 回到基线字节: {fp == base_fp}")
        print(f"  reactor Tests run={list(counts)} 红={reds or '(none)'}")
        print(f"  deployed = {passed}/{last_total} 本节 {last_sec} 红={mine or '(none)'}")
        if fp != base_fp or passed != total or last_total != total or last_sec != sec_n or reds or mine:
            bad += 1
        for f in FILES:
            if f.read_bytes() != originals[f]:
                print(f"  !! NOT RESTORED: {f}")
                bad += 1
        book["restored"] = all(f.read_bytes() == originals[f] for f in FILES)
        book["rerun_green"] = (passed == total and tuple(counts) == tuple(base_counts)
                               and not reds and not mine)
        book["bad"] = bad
    except BaseException as ex:  # noqa: BLE001
        crashed.append(ex)
        book["crashed"] = repr(ex)
        book["bad"] = bad + 1
        raise
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("restored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))
        # 半路崩了也不能把变异留在 ~/.m2 与 fat jar 里：下一位（包括下一次基线）
        # 从缓存取的就是这一份被改过的 core，而那看起来完全像"代码没动"。
        try:
            build()
            print(f"  ~/.m2 与 fat jar 已重装原始字节 (fp={fp_id(artifact_map())})")
        except BaseException as ex:  # noqa: BLE001
            print(f"  !! 原始字节重装失败，~/.m2 里可能还留着变异: {ex!r}")
        LOGS.mkdir(parents=True, exist_ok=True)
        LEDGER.write_text(json.dumps(book, ensure_ascii=False, indent=1), encoding="utf-8")
        verdict = ("CRASHED: " + repr(crashed[0])) if crashed else (
            "fire-window falsification done" if bad == 0 else f"{bad} problem(s)")
        if ONLY and not crashed:
            verdict += f" (PARTIAL: 只跑了 --only {','.join(sorted(ONLY))})"
        print(f"台账: {LEDGER}")
        print(f"RESULT: {verdict}")
    return 1 if (bad or crashed) else 0


class Tee:
    def __init__(self, stream, handle):
        self.stream = stream
        self.handle = handle

    def write(self, text):
        self.stream.write(text)
        self.handle.write(text)
        if text.endswith("\n"):
            self.handle.flush()
        return len(text)

    def flush(self):
        self.stream.flush()
        self.handle.flush()


if __name__ == "__main__":
    LOGS.mkdir(parents=True, exist_ok=True)
    transcript = LOGS / ("guard-" + time.strftime("%m%d-%H%M%S") + ".log")
    sys.stdout = Tee(sys.stdout, open(transcript, "w", encoding="utf-8"))
    print(f"transcript -> {transcript}")
    acquire_lock(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        sys.stdout = sys.stdout.stream
        release_lock()
