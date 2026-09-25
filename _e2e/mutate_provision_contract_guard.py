#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
#47 落在 **java 契约层** 的那一半反证 —— 改产品源码，看 LcHttpContractTest 那两支逐条红。

被测件: `LcHttpContractTest` 里的 provision 两支 (单支 `provisionTable` + 整批 `provisionAllTables`)。
基线 (实测): `mvn -o -B -pl z-lc-web -am test` → LcHttpContractTest **55 条全绿**，
             整条 reactor 1251+112+57 全绿，Total time 8.8 s。所以这一支可以每轮跑**整个类**
             (不像前端那几支只能挑目标文件跑) —— 分母大、代价小，连带红就骗不过去。

为什么这一支脚本要存在 (本轮 java 门禁就是这么红的):
    那两条断言是 #43 写的，写的时候「旧表比定义少一栏」是**永远修不好**的 FAILED。
    #47 把这一族改成按定义只补列 ⇒ 同一份夹具现在返回 ALTERED 且那一栏真的进了库，
    于是两条断言红。红的是**断言过期**，不是产品坏了 —— 这两件事只能靠注入分开:
    往产品源码里注入缺陷还能把它们打红的，才算这两支现在仍然各自钉着一件事；
    注入不打红的断言，就是要按「未覆盖」记账的空断言 (见下面 NOT_COVERED)。

判红口径 (与前几支同源):
    * 只认**具名**红 —— 每条预期红是 (测试方法, 该条断言的文案前缀) 一对，
      文案对应的**行号从被测文件的当前字节里现搜**，改名或挪位置都会被发现；
    * 一支 JUnit 方法只报**第一条**红 (抛出来就停了)，所以每支注入每个方法最多认领一条；
      同一因的下游红写在注释里当说明，不混进预期集；
    * 分母每轮必须相等，少一条就是「套件没真跑」，不当绿；
    * 编译不过的变异体不给结论 (它不测任何东西)；跑之前先删掉上一轮的 surefire XML，
      否则「构建挂了」会被读成「上一轮的全绿」。

RESULT 行: 全对时打印 `contract-layer falsification done`。
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
SVC = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/schema/SchemaAdminBizService.java"
TEST = REPO / "z-lc-web/src/test/java/com/zifang/z/lc/web/it/LcHttpContractTest.java"
CLASS = "com.zifang.z.lc.web.it.LcHttpContractTest"
XML = REPO / f"z-lc-web/target/surefire-reports/TEST-{CLASS}.xml"

sys.path.insert(0, str(REPO / "z-lc-admin-ui" / "e2e"))
import _mutlock  # noqa: E402

LOGS = pathlib.Path(os.path.expanduser("~/.cache/zlc47/contract_guard/logs"))

T1 = "provisionVerifiesColumnsInsteadOfClaimingSuccess"
T2 = "provisionAllIsolatesOneBadEntityFromItsNeighbours"

# ---- 断言文案前缀: 每条在被测文件里逐字唯一，且就是 assert 的 message 参数 ---------------------
A_CREATED = "第一支建表之后的状态"
A_ALTER_STATUS = "补列那一支报的状态"
A_ALTER_IN_DB = "说补上了，库里却没有"
A_FOREIGN_FAILED = "不归这份定义管的表"
A_FOREIGN_KIND = "没说是哪一类缺列"
A_FOREIGN_MISSING = "缺哪几列必须点名"
A_BATCH_500 = "部分成功不该是 500"
A_BATCH_CREATED = "这次真建出来的那张表"
A_BATCH_ALTERED = "这次真补过列的那张表"
A_BATCH_ALLKW = "坏的那支必须让汇总看得见"
A_BATCH_HANDS_OFF = "坏的那支把手伸进了别人的表"
A_BATCH_UNCHANGED = "建成过的两支都该回到"

# ---- 产品源码锚点 (每个必须逐字出现恰好一次，脚本先数再动) ---------------------------------
ALTERED_LINE = '        ProvisionReport.Item ok = item(def, ProvisionReport.ALTERED, text,'
ALTER_COUNT = '                report.setAltered(report.getAltered() + 1);'
UNCHANGED_COUNT = '                report.setUnchanged(report.getUnchanged() + 1);'
MISSING_SYS = '            bad.setMissingColumns(missingSystem);'
SYS_GUARD = '        if (!missingSystem.isEmpty()) {'
SYS_FAILED_ITEM = '            ProvisionReport.Item bad = item(def, ProvisionReport.FAILED, ddl,'
BATCH_ITEM = '            ProvisionReport.Item item = provisionOne(def);'
STATUS_TERNARY = '        return before.isEmpty()'

RUNS = [
    ("K1 把「这次真的补了一列」说成「这张表是这次建出来的」",
     [(SVC, ALTERED_LINE, ALTERED_LINE.replace("ProvisionReport.ALTERED", "ProvisionReport.CREATED"))],
     {T1: A_ALTER_STATUS, T2: A_BATCH_CREATED},
     "T2 红在 created 而不是 altered: 状态一改成 CREATED，汇总里 created 就变成 2 —— 先撞上的是那条，"
     "altered 那条是同一因的下游 (一支方法只报第一条)。"),

    ("K2 补了列但不计进汇总",
     [(SVC, ALTER_COUNT, '                report.setAltered(report.getAltered());')],
     {T2: A_BATCH_ALTERED},
     "只有批的那支红: 单支 provision 不读汇总。接口层 D9 红的是同一处判定的另一层探针。"),

    ("K3 FAILED 不点名缺的是哪几列",
     [(SVC, MISSING_SYS, '            bad.setMissingColumns(new ArrayList<String>());')],
     {T1: A_FOREIGN_MISSING},
     "状态仍是 FAILED，只把「缺谁」抹掉 —— 与「点名」那条互补。"),

    ("K4 摘掉「这张表不是引擎建的」那道闸 (拿别人的表按自己的定义补列)",
     [(SVC, SYS_GUARD, '        if (missingSystem.size() < 0) {')],
     {T1: A_FOREIGN_KIND, T2: A_BATCH_HANDS_OFF},
     "(实测推翻了我第一版的因果) 我以为闸门一摘坏的那支会变成 ALTERED、allOk 变 true —— 不是: "
     "摘掉之后它落进补列那一族，用户列补得上、引擎自建那四列永远补不上，于是仍然判 FAILED，"
     "汇总看上去一模一样。真正的差别只在**库里**: 别人的表被多加了一列 (T2 红在「坏的那支把手伸进了"
     "别人的表」那一条)，而文案从「缺引擎自建列」换成了「补列没补上」(T1 红在「没说是哪一类缺列」那一条)。"
     "这一支因此比预想的更值钱: 它说明报告的一致性不足以守住这张表，只有物理证据守得住。"),

    ("K5 一支坏实体把整批 provision 变回 500 (#43 的原始形状)",
     [(SVC, BATCH_ITEM, BATCH_ITEM + '\n'
       '            if (ProvisionReport.FAILED.equals(item.getStatus())) {\n'
       '                throw new IllegalStateException("provision failed: " + item.getMessage());\n'
       '            }')],
     {T2: A_BATCH_500},
     "单支那一支不碰批接口，所以只有 T2 红 —— 这一支证明「部分成功不该是 500」与「不连坐」是两条独立的账。"),

    ("K6 这次真建出来的表也说成「表本来就在」",
     [(SVC, STATUS_TERNARY, '        return before.isEmpty() && false')],
     {T1: A_CREATED, T2: A_BATCH_CREATED},
     "K1 的镜像: 那边把补列说成建表，这边把建表说成跳过。库里什么都有，只有状态在撒谎。"),

    ("K7 跳过的那一支不计数 (第二次点看不出它其实什么都没做)",
     [(SVC, UNCHANGED_COUNT, '                report.setUnchanged(report.getUnchanged());')],
     {T2: A_BATCH_UNCHANGED},
     "红在**第二次** provision-all: 第一次两支都在建/补，unchanged 本来就是 0，只有整批幂等那一句"
     "读得到这个计数器 —— 这也是这一支比接口层多出来的一条探针。"),

    ("K8 不归它管的表也说成「表本来就在，一列不缺」",
     [(SVC, SYS_FAILED_ITEM, SYS_FAILED_ITEM.replace("ProvisionReport.FAILED", "ProvisionReport.EXISTS_INTACT"))],
     {T1: A_FOREIGN_FAILED, T2: A_BATCH_ALLKW},
     "与 K4 互补: 那边真的去改别人的表 (只有库看得见)，这边一个字都不动、只是把状态说圆 —— "
     "同一张残缺的表，一边红在物理列，一边红在状态。"),
]

# 这一条在 java 契约层**注入不出来**，所以不许把它算进「已证伪」:
# 「ALTER 没报错」与「那一栏真进了库」要分开红，得造一次**不抛异常却没补出列**的 ALTER —— H2 里造不出
# (库拒的就是抛异常那一类)。已证的层: 接口层 mutate_provision_deployed_guard.py 的 D3
# (摘掉 `if (!still.isEmpty())` → Q_DB_REFUSES / R_ALTERDB 红) 与 D4。
NOT_COVERED = [A_ALTER_IN_DB]


def sh(cmd, cwd=REPO, timeout=1800):
    return subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout)


def md5(p: pathlib.Path) -> str:
    return hashlib.md5(p.read_bytes()).hexdigest()


def snapshot(targets):
    """备份目录**按本次运行**分名 —— 固定名会被上一支脚本的旧快照复用 (那是真事故过的坑)。"""
    run_dir = LOGS / f"backup-run-{os.getpid()}-{time.strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    for f in targets:
        shutil.copyfile(f, run_dir / (f.name + ".orig"))
    return run_dir


def statement_start(src_lines, idx):
    """把「文案所在行」归到「那条 assert 调用的首行」。

    JUnit 报的是**调用**那一行，而长消息参数常被写到续行上 —— 不归这一口，一条正当的红会被
    读成"红在另一条"(本轮实测 K3/K6 就是这么假的两次偏差，差的正是那一行)。
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
    """开跑前把地基钉三件事: 锚点唯一、预期文案真在被测文件里、预期红集两两不同。"""
    src = {f: f.read_text(encoding="utf-8") for f in (SVC, TEST)}
    problems = []
    for _, edits, _, _ in RUNS:
        for path, anchor, _ in edits:
            n = src[path].count(anchor)
            if n != 1:
                problems.append(f"锚点出现 {n} 次（要 1 次）: {anchor.strip()}")
    test_lines = src[TEST].splitlines()
    lines = {}
    titles = sorted({t for _, _, exp, _ in RUNS for t in exp.values()})
    for t in titles:
        hits = [i for i, line in enumerate(test_lines) if t in line]
        if not hits:
            problems.append(f"预期红的文案在被测文件里找不到: {t}")
        elif len(hits) != 1:
            problems.append(f"预期红的文案在被测文件里出现 {len(hits)} 次（要 1 次），"
                            "它就不再是一条断言的名字了")
        else:
            i = hits[0]
            # 调用行与文案行都收: 具体哪一行由 JUnit 说了算，但只能是这两行之一。
            lines[t] = {i + 1, statement_start(test_lines, i)}
    sets = {frozenset(exp.items()) for _, _, exp, _ in RUNS}
    if len(sets) != len(RUNS):
        problems.append("有两支注入的预期红集完全相同 —— 要么其中一支是重复检查，先去核文案")
    for _, _, exp, _ in RUNS:
        for m in exp:
            if m not in (T1, T2):
                problems.append(f"预期红点名了一个不存在的方法: {m}")
    return problems, lines


def run_suite(label):
    """跑整个 LcHttpContractTest，返回 (分母, [(方法, 断言行号, 消息), ...])。"""
    XML.unlink(missing_ok=True)
    proc = sh(["mvn", "-o", "-B", "-pl", "z-lc-web", "-am", "test",
               f"-Dtest={CLASS.split('.')[-1]}", "-Dsurefire.failIfNoSpecifiedTests=false"])
    (LOGS / f"{label}.log").write_text(proc.stdout + "\n--- stderr ---\n" + proc.stderr, encoding="utf-8")
    if "COMPILATION ERROR" in proc.stdout or "cannot find symbol" in proc.stdout:
        raise SystemExit(f"变异体编译不过（它不测任何东西，这一轮没有结论），见 {LOGS / (label + '.log')}")
    if not XML.exists():
        raise SystemExit(f"没有 surefire XML —— 套件根本没跑到，见 {LOGS / (label + '.log')}")
    root = ET.parse(XML).getroot()
    cases = root.findall("testcase")
    reds = []
    for tc in cases:
        node = tc.find("failure")
        if node is None:
            node = tc.find("error")
        if node is None:
            continue
        trace = (node.text or "") + " " + (node.get("message") or "")
        m = re.search(re.escape(TEST.name) + r":(\d+)", trace)
        reds.append((tc.get("name"), int(m.group(1)) if m else -1, node.get("message") or ""))
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
            print(f"  OK  预期那条真的红了: {method}:{hit[0]} 「{title}」")
        else:
            got = by_method.get(method)
            print(f"  !!  预期变红却没红: {method} 「{title}」"
                  + (f"（它红在另一条，行 {got[0]} —— 那就是这一支注入实际打掉的东西）" if got
                     else "（这一支整个是绿的）"))
            bad += 1
    for method, hits in sorted(by_method.items()):
        if method not in expected:
            print(f"  !!  未预期的红: {method} 行 {hits[0]} —— 不许加白名单了事，"
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
        print("=== 基线（整个 LcHttpContractTest 必须全绿）===")
        denom, reds = run_suite("00_baseline")
        if reds:
            for name, line, _ in reds:
                print(f"  !! 基线就红: {name}:{line}")
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
            print(f"  !! 还原后仍红: {[r[0] for r in reds]}")
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
          + ("contract-layer falsification done" if bad == 0 else f"{bad} problem(s)")
          + f" | {len(RUNS)} 支注入 / 认领 {owned} 条断言 / 按未覆盖记账 {len(NOT_COVERED)} 条")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
