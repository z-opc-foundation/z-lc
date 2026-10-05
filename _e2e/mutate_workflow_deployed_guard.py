#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Falsify the `[15w]` section of `_e2e/e2e_api_test.py` against the **deployed fat jar**.

为什么还要这一支（§2.3 那一支不是已经打过六个洞了吗）：
`mutate_workflow_trigger_guard.py` 量的是 mvn 起的那两个测试进程 —— 它们证明"这些类自己会这么做"。
证明不了**部署件里有一个人调它**。#61 的原始形状恰恰是 `listByEvent` + `startProcess` 在生产代码里
零调用者而 core 1326 例全绿：那条链在测试语境里是活的、在 jar 里是死的，两层都测不到。
这一支每次注入都重新 build fat jar、用**它**重启 18090、再跑一遍接口层，所以"接线"这件事
被打掉时这里必须红，而 §2.3 那一支根本不需要动。

注入清单（红的名字一律抄自实测，见 RUNS 里每支后面的读数；期望与实跑不符 = MISMATCH，不白名单）：
  W1 摘掉 `RuntimeCrudController` 里 `afterCreate(...)` 那一行 ⇒ 派发调用点没了
  W2 `START_PATH` 改回 #61 之前的 `/approval-center/process/start` ⇒ 路径那一条红
  W3 `if (!CtcAdapter.httpAccepted(res))` 改回 `if (!res.isSuccess())` ⇒ 5xx 当成功
  W4 `data.getString("processInstanceId")` 改回 `data.toString()` ⇒ 实例 id 那一格
  W5 派发侧的等待预算放大 60 倍（无上限那一支编不过，见 W5_REPL 上面那段）⇒ 挂死到不了点，
     "在默认预算内返回"与"到点判 FAILED 并点名预算"两支红
  W6 摘掉 `autoSubmit != 1` 那道闸 ⇒ autoSubmit=0 那两支红

量具规则（与 §2.3 那一致，逐条都有过一次代价）：
  * 注入前把每个文件的字节读进**本次运行独占**的备份目录，还原只从这份内存/磁盘副本 cp + md5 对账；
    **不许**用 `git checkout --` 当还原步 —— git 的基线是 HEAD，不是我开局测量那一刻，共享工作树里
    那会把别人未提交的改动一起抹掉。
  * 每支注入前先确认 anchor 在文件里**恰好出现一次**，否则当场 SKIPPED 并记一笔坏账（不猜）。
  * 产物指纹：注入前后对 fat jar 里每一个 `BOOT-INF/lib/z-lc-*.jar` 的每一个 `.class` 取 md5，
    差集就是"这次变异真的进了构件"的书证；指纹没变 ⇒ 这一轮什么都证明不了，直接记坏账。
  * 分母钉死：每轮 `[15w]` 的条目数必须等于基线那一份，不等就不许说"没红"。
  * 复跑期间不改被测源码；收尾一定"还原成原始字节 → 重新 build → 重启 → 整份再跑一遍"回绿。
  * 台账记 `ran_by`（谁跑的）。
  * 端口归因（#55 的教训落在这一层）：新 JVM 起起来之后，必须**现读 18090 的监听者**并证明它的
    argv 里就是本次那份 jar，否则"这次部署起来了"这句话没有任何一层能归因。
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

CTRL = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/RuntimeCrudController.java"
CAMUDA_ADAPTER = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/adapter/CamudaAdapter.java"
DISPATCHER = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowTriggerDispatcher.java"
TRIGGERS = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowTriggers.java"
FILES = sorted({CTRL, CAMUDA_ADAPTER, DISPATCHER, TRIGGERS}, key=lambda p: str(p))

BAK = Path.home() / ".cache/zlc61/deployed_bak"
LEDGER = Path.home() / ".cache/zlc61/deployed_ledger.json"
LOGS = Path.home() / ".cache/zlc61"
BOOT_LOG = LOGS / "deployed_mut_boot.log"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
SECTION = "15w"

# ---- anchors（每支都必须恰好出现一次）----------------------------------------------------------
W1_ANCHOR = ('        int fired = workflowTriggerDispatcher.afterCreate(body.getTenantCode(), body.getAppCode(),\n'
             '                def.getEntityCode(), id, body.getFieldValues(), actor);\n')
W1_REPL = '        int fired = 0;\n'

W2_ANCHOR = 'static final String START_PATH = "/api/approval-center/processes/start";'
W2_REPL = 'static final String START_PATH = "/approval-center/process/start";'

W3_ANCHOR = '        if (!CtcAdapter.httpAccepted(res)) {'
W3_REPL = '        if (!res.isSuccess()) {'

W4_ANCHOR = '        String instanceId = data.getString("processInstanceId");'
W4_REPL = '        String instanceId = data.toString();'

W5_ANCHOR = '                return future.get(timeoutMs, TimeUnit.MILLISECONDS);'
# 一支**编不过**的变异不算覆盖（也不算网）：把它摘成无上限的 `future.get()`，javac 当场拒绝
# —— 下面那句 `catch (TimeoutException)` 变成不可达（"exception ... is never thrown in body of
# corresponding try statement"，实测于 09-27 01:32 那一轮，整支战役就此崩在此处）。
# 语言替这个洞上了一道闸，但闸不证明**行为**被钉住。所以这一支改打同一个性质、可编译的形状：
# 预算放大 60 倍（180s），桩挂死 7s 于是永远到不了点 —— "在默认预算内返回"与"到点判 FAILED 并点名
# 那个预算"两支必须红，否则本节对"等待有上限"就没有网。
W5_REPL = '                return future.get(timeoutMs * 60, TimeUnit.MILLISECONDS);'

W6_ANCHOR = '        if (autoSubmit != null && autoSubmit.intValue() != 1) {'
W6_REPL = '        if (false) {'

# 每支的预期红集：先按"谁读这个值"推出来写在这里，第一轮实测若与推理不符，**按实测改这一份账**
# （断言本身一个字都不改软），并把差异写进注释。
RUNS = [
    ("W1", [(CTRL, W1_ANCHOR, W1_REPL)], [
                                           "写一条记录 ⇒ 桩正好收到一句（这一条是整节的桥：它不成立，本节其余没有判定）",
                                           "打的必须是 z-camuda 真映射的那条路径（少 /api 或 process 少个 s 都是 404）",
                                           "body 里是 DTO 真读的 processKey（不是 processDefKey），且空白剪掉",
                                           "businessKey 能定位回这条记录",
                                           "title 在（缺席时审批中心里那一单没有名字）",
                                           "initiator 用的是这次请求的那个人，不是让 z-camuda 兜底成常量 \"1\"",
                                           "字段值整份当流程变量带走，并且留了低代码这一侧的坐标",
                                           "/fires 读回这一条：正好一行",
                                           "那一行是 STARTED，实例 id 就是 z-camuda data.processInstanceId 那一格（不是整个 data 的 toString）",
                                           "成功行不带失败原因，但带上它是哪条绑定的兑现",
                                           "登记绑定这件事本身不发单（只有写记录才发）",
                                           "second 上第一条记录发一句",
                                           "同一实体第二条记录又发一句：逐条新建各自发起，不复用上一条",
                                           "second 的账上有两行，且 recordId 各指各的记录",
                                           "复证链还通：写 case 仍然正好一句",
                                           "没有登记的实体一句都不发（否则「绑定决定发不发」这句是假的）",
                                           "没发单的实体在账上也没有行",
                                           "复证：被拒 6 次之后写 case 仍然正好一句",
                                           "发出去那一句在账上留下一行 STARTED",
                                           "引擎说不了（success=false）：记录照样落地",
                                           "并且账上是一行 FAILED 带引擎那句原话",
                                           "引擎回 5xx 而 body 写着成功：仍是 FAILED（这一格形状是 §2.3 的 M5 逼出来的）",
                                           "5xx 时 body 里那个实例号一个字都不许留下，且失败原因说清是 http 几",
                                           "引擎不可达：账上留一行 FAILED 并说得出为什么",
                                           "把桩换回来：同一条绑定立刻发得出去（阳性对照，case 正好一句）",
                                           "账上累积的行数与发出去的句子一样多（每一次尝试都留痕，成功的也不例外）",
                                           "引擎挂死 7s：写入口在默认预算内就返回了（不是等它自己回话）",
                                           "到点判 FAILED，而且原因点名的是那个默认预算本身",
                                           "一次超时不许传染下一次：紧接着的一条又发得出去（2 槽池的槽位要在有上限之后自己回来）",
                                           "复证链还通：批量导入之前 case 仍然正好一句",
                                           "批量导入今天不发单（发就是 N 条记录一次外部调用，且没有回滚路径）"
                                          ]),  # 实测 31 条（09-27 01:2x --only W1），抄自日志不是推的。派发调用点没了 => 本节所有依赖那条链的断言一起红
    ("W2", [(CAMUDA_ADAPTER, W2_ANCHOR, W2_REPL)], [
                                                 "打的必须是 z-camuda 真映射的那条路径（少 /api 或 process 少个 s 都是 404）"
                                                ]),  # 实测 1 条（09-27 01:2x --only W2），抄自日志不是推的。只红路径那一条：形状对、只有门牌错
    ("W3", [(CAMUDA_ADAPTER, W3_ANCHOR, W3_REPL)], [
                                                 "引擎回 5xx 而 body 写着成功：仍是 FAILED（这一格形状是 §2.3 的 M5 逼出来的）",
                                                 "5xx 时 body 里那个实例号一个字都不许留下，且失败原因说清是 http 几"
                                                ]),  # 实测 2 条（09-27 01:2x --only W3），抄自日志不是推的。5xx 被当成成功 => 只有那两支红
    ("W4", [(CAMUDA_ADAPTER, W4_ANCHOR, W4_REPL)], [
                                                 "那一行是 STARTED，实例 id 就是 z-camuda data.processInstanceId 那一格（不是整个 data 的 toString）"
                                                ]),  # 实测 1 条（09-27 01:2x --only W4），抄自日志不是推的。实例 id 那一格漂成整个 data
    ("W5", [(DISPATCHER, W5_ANCHOR, W5_REPL)], [
                                                 "到点判 FAILED，而且原因点名的是那个默认预算本身",
                                                 "一次超时不许传染下一次：紧接着的一条又发得出去（2 槽池的槽位要在有上限之后自己回来）",
                                                 "复证链还通：批量导入之前 case 仍然正好一句",
                                                 "批量导入今天不发单（发就是 N 条记录一次外部调用，且没有回滚路径）"
                                                ]),
# 实测 4 条（09-27 01:3x --only W5）。两支「到点」检查红在派发侧那一句：预算点名（等待 z-camuda 响应超过
# 3000ms）出自 future.get(timeoutMs) 的 TimeoutException 分支，放大预算后换成了传输层抛错那句。
# 而「写入口在默认预算内返回」这一支**没红** —— 它另有一根独立的桩：CamudaAdapter.newTransport 的 socket
# 超时是 timeoutMs + SOCKET_GRACE_MS（同一个字段，W5 碰不到它），3.5s 就切掉那次挂死。两道界各自
# 名下自己那一支检查：注入摘掉其中一道，只红那一道名下的。这不是量具漏判，是这一族本来有两层。
    ("W6", [(TRIGGERS, W6_ANCHOR, W6_REPL)], [
                                               "autoSubmit=0 的绑定被拒（没有任何运行时行为的开关就是装饰）",
                                               "autoSubmit=0 的绑定被拒（没有任何运行时行为的开关就是装饰）：点名为什么兑现不了",
                                               "上面这 6 次被拒的提交一行都没落库（闸在写入之前）"
                                              ]),
# 实测 3 条（09-27 01:3x --only W6，整轮 529/532、还原轮 532/532 回绿）。第三支红「一行都没落库」的
# 机制是照 §15w 那 6 支的形状读出来的，不是猜的：第 5 支（autoSubmit=0，KEY=p_off）在闸被摘之后真的
# 落了一行，于是 wf_bindings(case) 读出两行。而它**没有**跟着让「写 case 仍然正好一句」红 —— 因为落的
# 那行 auto_submit=0，被 listByEvent 的 auto_submit = 1 挡在发起之外（服务里那段 javadoc 说的就是这个）。
# ⇒ 结论：写入口那道闸管「别让装饰进库」，读侧那道过滤管「别让它发单」，两层各自名下不同的检查。
]
ONLY = set(sys.argv[1:])


def sh(cmd, cwd=ROOT, timeout=1800):
    return subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout)


def snapshot_sources():
    run_dir = BAK / f"run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    originals = {}
    for f in FILES:
        if not f.exists():
            raise RuntimeError(f"source file missing, cannot even baseline: {f}")
        disk = f.read_bytes()
        (run_dir / (str(f).replace("/", "_").lstrip("_") + ".orig")).write_bytes(disk)
        originals[f] = disk
    print(f"sources snapshotted -> {run_dir}")
    return originals


def artifact_map():
    """"lib!class" -> md5。整个 fat jar 的每一条 class 都进这张表。"""
    out = {}
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
                for e in classes:
                    out[f"{lib}!{e}"] = hashlib.md5(inner.read(e)).hexdigest()
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
    """收旧进程 → **等端口真的空下来** → 起本次这份 jar → 等 health → 现读监听者做归因。

    归因这一段是 #55 的形状：pkill + sleep 3 之后新 JVM 死于 "already in use"，而 /health 照样回
    UP —— 那一轮的"部署好了"其实属于上一支进程。这里不比较 pid 数值（`Popen` 拿到的是 shell 的
    子壳 pid，与 java 的真实 pid 差一号，实测会误判），而是读监听者自己的 argv。
    """
    pids = port_listeners()
    if len(pids) > 1:
        raise RuntimeError(f"refusing to guess which JVM to stop: {pids}")
    if pids:
        subprocess.run(["kill", str(pids[0])])
    waited = 0
    while port_listeners() and waited < 60:
        time.sleep(1)
        waited += 1
    if port_listeners():
        raise RuntimeError(f"端口 18090 在 {waited}s 之后仍被 {port_listeners()} 占着，不敢起新件")
    print(f"  旧进程已收干净（等了 {waited}s, 端口 18090 空）")
    BOOT_LOG.parent.mkdir(parents=True, exist_ok=True)
    log = open(BOOT_LOG, "ab")
    # start_new_session：不起新会话的话这个 jar 属于本量具的进程组，量具一退出它跟着收 SIGTERM（缺陷 #74）
    proc = subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                            cwd=str(ROOT), stdout=log, stderr=log, start_new_session=True)
    if not wait_health():
        tail = subprocess.run(["tail", "-20", str(BOOT_LOG)], capture_output=True,
                              text=True).stdout.strip()
        raise RuntimeError(f"新件没起来 (launcher pid={proc.pid})；{BOOT_LOG} 末尾:\n{tail}")
    now = port_listeners()
    if len(now) != 1:
        raise RuntimeError(f"health 起来了但 18090 上有 {len(now)} 个监听者 {now}：归因不了")
    argv = subprocess.run(["ps", "-o", "command=", "-p", str(now[0])],
                          capture_output=True, text=True).stdout
    if str(JAR.name) not in argv:
        raise RuntimeError(f"18090 的监听者 pid={now[0]} 伺服的不是本次这份 jar:\n{argv.strip()[:200]}")
    print(f"  归因成立：18090 的监听者 pid={now[0]}，argv 里就是 {JAR.name}")
    return now[0]


def build():
    p = sh(["mvn", "-o", "-B", "install", "-DskipTests",
            "-pl", "z-lc-core,z-lc-web,z-lc-admin", "-am"])
    out = p.stdout + p.stderr
    if "BUILD SUCCESS" not in out:
        raise RuntimeError("jar build failed, tail:\n" + "\n".join(out.splitlines()[-25:]))


def run_e2e(label):
    """跑**整份**接口层（不给它开后门只跑一节），按 harness 自己的 `[NNx]` 横幅切出本节。

    别按关键字过滤 —— 那样别人的红能被洗进本节，本节的红也能被洗到别处。
    """
    t0 = time.time()
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py", "http://localhost:18090"],
                       cwd=str(ROOT), capture_output=True, text=True)
    (LOGS / f"deployed_api_{label}.log").write_text(p.stdout + p.stderr)
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
            if section == SECTION:
                mine.append(name)
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    # 本节自己的条目数从**同一份输出**里数：一条注入把本节打断（抛异常退出）时，
    # "本节一条红都没有"必须是坏账而不是"没红"。
    lines = p.stdout.splitlines()
    section, n, sec_n = None, 0, None
    for line in lines:
        m = re.match(r"^\[(\d+\w*)\]", line)
        if m:
            if section == SECTION:
                sec_n = n
            section, n = m.group(1), 0
            continue
        if section == SECTION and line.startswith(("  PASS  ", "  FAIL  ")):
            n += 1
    if section == SECTION:
        sec_n = n
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    return parsed, fails, mine, sec_n, round(time.time() - t0, 1)


def main():
    global RUNS
    RUNS = [r for r in RUNS if not ONLY or r[0] in ONLY]
    originals = snapshot_sources()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    restore_all()
    bad = 0
    crashed = []
    book = {"ran_by": f"{os.environ.get('USER','?')}@{time.strftime('%m%d-%H%M%S')}",
            "script": os.path.basename(__file__), "runs": []}
    try:
        build()
        restart()
        base_fp = artifact_map()
        (ok_n, total), base_fails, _, sec_n, secs = run_e2e("baseline")
        print(f"\n=== baseline deployed: {ok_n}/{total} 全量、本节 {sec_n} 条，"
              f"artifact fp={fp_id(base_fp)}，一轮 {secs}s")
        if ok_n != total or not sec_n:
            print(f"BASELINE NOT GREEN on the deployed jar, aborting. failures={base_fails}")
            return 1
        denoms = {"e2e_total": total, "section": sec_n}

        for tag, edits, expect in RUNS:
            applied = []
            for path, anchor, repl in edits:
                text = path.read_text(encoding="utf-8")
                if text.count(anchor) != 1:
                    print(f"[SKIPPED] {tag}: anchor occurs {text.count(anchor)}x in {path.name}")
                    bad += 1
                    continue
                path.write_text(text.replace(anchor, repl, 1))
                applied.append(path)
            if not applied:
                continue
            build()
            fp = artifact_map()
            diff = changed_classes(base_fp, fp)
            restart()
            (passed, this_total), fails, mine, this_sec, secs = run_e2e(f"mut{tag}")
            print(f"\n[{tag}] 注入 {[p.name for p in applied]}")
            print(f"  artifact fp = {fp_id(fp)}  changed={len(diff)} class: {diff[:4]}")
            print(f"  e2e = {passed}/{this_total} 本节 {this_sec}/{sec_n} 条（一轮 {secs}s）")
            print(f"  [{SECTION}] 红 ({len(mine)}):")
            for n_ in mine:
                print(f"     - {n_}")
            outside = [f for f in fails if f not in mine]
            print(f"  本节之外的红: {outside or '(none)'}")
            rec = {"tag": tag, "files": [p.name for p in applied], "changed_classes": diff,
                   "e2e": f"{passed}/{this_total}", "section": this_sec,
                   "red": mine, "outside": outside}
            book["runs"].append(rec)
            if not diff:
                print("  !! 构件一个 class 都没变 -> 这一轮什么都证明不了")
                bad += 1
            if this_total != total:
                print(f"  !! 这一轮只跑了 {this_total} 项，基线是 {total} 项 —— 分母变了，"
                      "「没红」的结论不成立")
                bad += 1
            if this_sec != sec_n:
                print(f"  !! 本节条目数漂了：{sec_n} → {this_sec}")
                bad += 1
            if expect == [] and not mine:
                print("  !! 一支注入一条红都没有：这条链在部署件上没有网（要么补断言，要么记未覆盖）")
                bad += 1
            elif expect:
                if sorted(mine) != sorted(expect):
                    print(f"  !! MISMATCH: 预期 {len(expect)} 条，实跑 {len(mine)} 条")
                    print(f"     只在预期里: {sorted(set(expect) - set(mine))}")
                    print(f"     只在实跑里: {sorted(set(mine) - set(expect))}")
                    bad += 1
            # 连带红必须**每一轮**都判 —— 早先它挂在同一串 elif 的最后一支上，于是"预期非空"的
            # 那一轮（也就是抄完实测之后的全部轮次）根本走不到这一句：注入打到本节之外也不会红。
            if outside:
                print("  !! 连带红打到了本节之外 —— 注入没这么宽的影响面，先怀疑量具")
                bad += 1
            if ONLY:
                print(f"  （--only {tag} 的实测红集，可直接抄进 RUNS 的 expect）")
                print("  EXPECT_" + tag + " = " + json.dumps(mine, ensure_ascii=False, indent=1))
            restore_all()

        print("\n=== restore: rebuild pristine, restart, re-run ===")
        build()
        fp = artifact_map()
        restart()
        (passed, last_total), fails, _, last_sec, _ = run_e2e("restored")
        print(f"  artifact 回到基线字节: {fp == base_fp}")
        print(f"  e2e = {passed}/{last_total} 本节 {last_sec}  failures={fails or '(none)'}")
        if fp != base_fp or passed != total or last_total != total or last_sec != sec_n:
            bad += 1
        for f in FILES:
            if f.read_bytes() != originals[f]:
                print(f"  !! NOT RESTORED: {f}")
                bad += 1
        book["denominators"] = denoms
        book["restored"] = all(f.read_bytes() == originals[f] for f in FILES)
        book["rerun_green"] = passed == total and last_total == total
        book["bad"] = bad
    except BaseException as ex:  # noqa: BLE001
        # 崩溃必须记一笔：`finally` 里有 return 的写法会把异常洗成 "RESULT: done"，
        # 于是"重启失败/构建失败"这种什么都没判定的轮次看起来像跑完了。
        crashed.append(ex)
        book["crashed"] = repr(ex)
        book["bad"] = bad + 1
        raise
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("restored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))
        LOGS.mkdir(parents=True, exist_ok=True)
        LEDGER.write_text(json.dumps(book, ensure_ascii=False, indent=1), encoding="utf-8")
        verdict = ("CRASHED: " + repr(crashed[0])) if crashed else (
            "deployed-layer falsification done" if bad == 0 else f"{bad} problem(s)")
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
    transcript = LOGS / ("deployed-guard-" + time.strftime("%m%d-%H%M%S") + ".log")
    sys.stdout = Tee(sys.stdout, open(transcript, "w", encoding="utf-8"))
    print(f"transcript -> {transcript}")
    acquire_lock(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        sys.stdout = sys.stdout.stream
        release_lock()
