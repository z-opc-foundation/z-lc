#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #70 的注入自证：故意把产品源码改坏，看那 19（core）+ 7（契约层）+ 7（vitest）条断言红在哪一条.

为什么必须有这一份（而不是"两层绿着"就算完）：#70 的原始形状恰恰是**全套测试绿而运行期什么都不做** ——
`createDeployment` insert 一行 PENDING 就 return，`updateDeploymentStatus` 在生产代码里零调用者，
而前端抄了一份三种方式都不能选的清单；那时单测只测"字段拷进 DTO"，一条都没红过。
"绿"本身不是证据，"我故意把它改坏，它红在哪一条"才是。

量的是三层，每支只跑它结构上够得着的那层（够不着的层不跑这件事在 RESULT 行里明写）：
  core = DeploymentServiceImplTest（替身 mapper/服务，量的是服务类的分叉与回写）
  web  = DeploymentContractTest（真起上下文、真打 HTTP、真建表、真回读那一行）
  ui   = deploymentVocabulary.test.ts（逐字对表 java 源码）+ DeploymentsPage.test.tsx（真渲染 jsdom）

七支（J1a/J1b/J2/J3 改 java，F1/F2/F3 改前端）与各自打掉的东西：
  J1a 摘掉写入口那道"方式"门 ⇒ core 2 条 + web 3 条。三条契约层红全部落在 helper
      {@code refused} 的第一句（"HTTP 状态应当是 400"）—— 这不是含糊：被拒的形态一旦被收下，
      后面每一条断言都失去了对象，所以红的形状就是"400 没等到"。
  J1b 摘掉"物化批次必须存在且属于这个应用"那道门 ⇒ core 1 条 + web 1 条。与 J1a 的红集不同，
      所以这两支分得开"漏了哪一种门"。
  J2 让 {@code execute} 忽略 {@code report.isAllOk()} 恒报 SUCCESS ⇒ core 1 条 + web 1 条。
      这一支打掉的是"部署说成了而表没落"：计数与逐支点名还在，只是结局被洗白。
  J3 摘掉 {@code execute} 末尾那句回写 ⇒ core 2 条 + web 2 条。返回值是对的而库里那行永远 PENDING ——
      界面的状态列与日志抽屉读的是库里那行，所以两层各两条都必须红。
      core 那条正向（{@code createShouldRecordTheRowThenExecuteAndReturnTheRealOutcome}）是本轮补出来的：
      原先这一层只在 FAILED 路径上读回库，SUCCESS 路径全靠返回值自证 ⇒ 注入当场暴露了这个洞。
  F1 页面不看结局、无条件报成功 ⇒ ui 2 条（源码扫描那条 + jsdom 那条真渲染）。
  F2 页面回到手抄的三种方式 ⇒ ui 2 条（源码扫描 + 下拉项逐字比对）。
  F3 词表形状漂移时降级成空清单而不抛 ⇒ ui 2 条（api 那条 + 界面那条：读失败被说成"没有可执行的方式"）。

量具自己的规矩（与前几支同源）：
  * 注入前把原始字节读进内存，还原只从内存写回 + 逐文件 md5 对账（**不用** {@code git checkout --}：
    共享工作树里那一句会把别人未提交的改动一起抹掉）；
  * 写回一律 {@code write_text}（mtime = 现在）而不是 {@code copy2}（连 mtime 一起倒回过去 ⇒
    maven 增量编译跳过重编 ⇒ 下一支量的还是上一支的变异字节）。java 层另有正面证据：**跑完那一层之后**
    取 {@code .class} 的 mtime，早于本轮注入写入的时刻 ⇒ 判"maven 没重编，这一轮没有结论"。
    ⚠ 这条检查曾经写反过（08:30 那一跑）：拿**跑之前**的 {@code .class} md5 去比"上一次运行的 md5"，
    而那一刻 maven 根本还没编译 ⇒ 恒等于旧字节 ⇒ J1a 一开场就 FATAL。改成"跑完之后比 mtime"，
    并且不用 md5 当判据 —— 等价变异（字节本来就一样）会被 md5 尺误判成"没重编"；
  * 七支全还原后再要一次"三层 0 红"（收尾复跑基线）：这一遍红说明还原不彻底，前面的 MATCH 一律不记账；
  * 只认具名红：一条预期红 = (测试类/文件, 测试方法/标题, 那条断言的文案)，文案从测试源码里**复制**
    而不是凭记忆重写；一个方法只报第一条红，所以同一支里同一方法只许认领一次；
  * 分母每轮必须相等（core 19 / web 7 / ui 7）；XML 缺失或 0 条用例直接抛（空参照集会打印"满分"）；
    没被点名的红一律 MISMATCH，不加白名单；
  * 尺文件（三个测试文件）本轮只读：它们的 md5 在一支之内变了 ⇒ FATAL，因为红集就不再属于这一支；
  * 复跑期间不改被测源码；台账与日志落 ~/.cache（/tmp 会被别的会话扫），并记"谁跑的"。

跑法：{@code python3 _e2e/mutate_deployment_guard.py}（全 7 支 + 基线，约 8 分钟）
      {@code ... --only J2,F1}（只跑这两支，判据一条不减，但 RESULT 行会写明另几支本轮没有判定）
      {@code ... --baseline}（只跑基线三层，不注入）
整轮 stdout 另落 {@code ~/.cache/zlc70/mut/logs/guard-<时间>.log}，台账 JSON 落 {@code ~/.cache/zlc70/mut/ledger.json}。
"""
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

REPO = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO / "z-lc-admin-ui" / "e2e"))
import _mutlock  # noqa: E402

# ---- 被测产品源码 ---------------------------------------------------------------------------
SVC = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/deployment/DeploymentServiceImpl.java"
PAGE = REPO / "z-lc-admin-ui/src/views/admin/DeploymentsPage.tsx"
APID = REPO / "z-lc-admin-ui/src/api/deployment.ts"

# ---- 尺（测试文件只用来现搜断言文案，注入期间一个字不许动）------------------------------------
T_CORE = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/deployment/DeploymentServiceImplTest.java"
T_WEB = REPO / "z-lc-web/src/test/java/com/zifang/z/lc/web/it/DeploymentContractTest.java"
UI_FILES = [
    REPO / "z-lc-admin-ui/src/api/deploymentVocabulary.test.ts",
    REPO / "z-lc-admin-ui/src/views/admin/DeploymentsPage.test.tsx",
]

CACHE = pathlib.Path(os.path.expanduser("~/.cache/zlc70/mut"))
LOGS = CACHE / "logs"
LEDGER = CACHE / "ledger.json"

GAUGE = "deployment70"

LAYERS = {
    "core": {
        "cmd": ["mvn", "-o", "-B", "-pl", "z-lc-common,z-lc-core",
                "-Dtest=DeploymentServiceImplTest", "-Dsurefire.failIfNoSpecifiedTests=false", "test"],
        "cwd": REPO,
        "classes": {"DeploymentServiceImplTest": T_CORE},
        "xml": REPO / "z-lc-core/target/surefire-reports",
        "class_file": REPO / "z-lc-core/target/classes/com/zifang/z/lc/core/deployment/DeploymentServiceImpl.class",
    },
    "web": {
        "cmd": ["mvn", "-o", "-B", "-pl", "z-lc-common,z-lc-core,z-lc-web",
                "-Dtest=DeploymentContractTest", "-Dsurefire.failIfNoSpecifiedTests=false", "test"],
        "cwd": REPO,
        "classes": {"DeploymentContractTest": T_WEB},
        "xml": REPO / "z-lc-web/target/surefire-reports",
        "class_file": REPO / "z-lc-core/target/classes/com/zifang/z/lc/core/deployment/DeploymentServiceImpl.class",
    },
    "ui": {
        "cmd": ["npx", "vitest", "run", "src/api/deploymentVocabulary.test.ts",
                "src/views/admin/DeploymentsPage.test.tsx", "--reporter=json"],
        "cwd": REPO / "z-lc-admin-ui",
        "files": {str(p): p for p in UI_FILES},
    },
}


def md5(path):
    return hashlib.md5(path.read_bytes()).hexdigest() if path.exists() else None


def claim(cls, method, needle):
    """一条预期红。needle 从测试源码里复制，且必须在该方法的第一条真红里出现。"""
    return {"class": cls, "method": method, "needle": needle}


# ---- 注入锚点 -------------------------------------------------------------------------------
A_J1A = "        DeploymentTypes.validateForWrite(target.getDeployType());"
R_J1A = ("        // mutant J1a: 摘掉方式那道门 —— 服务器执行不了的方式照样留下一行账\n"
         "        if (System.nanoTime() < 0) {\n"
         "            DeploymentTypes.validateForWrite(target.getDeployType());\n"
         "        }")

A_J1B = "            requireBatchOfThisApp(target);"
R_J1B = ("            // mutant J1b: 摘掉批次那道门 —— 不存在或属于别的应用的批次照样收下\n"
         "            if (System.nanoTime() < 0) {\n"
         "                requireBatchOfThisApp(target);\n"
         "            }")

A_J2 = ("            row.setStatus(report.isAllOk() ? DeploymentEntity.STATUS_SUCCESS "
        ": DeploymentEntity.STATUS_FAILED);")
R_J2 = "            row.setStatus(DeploymentEntity.STATUS_SUCCESS); // mutant J2: 忽略 report 的结局，恒报成功"

A_J3 = "        deploymentMapper.updateById(row);"
R_J3 = ("        // mutant J3: 执行完不回写 —— 库里那行永远停在 PENDING（界面状态列读的就是它）\n"
        "        if (System.nanoTime() < 0) {\n"
        "            deploymentMapper.updateById(row);\n"
        "        }")

A_F1 = "      if (done.status === 'SUCCESS') {"
R_F1 = ("      // mutant F1: 不看结局 —— 老形状，服务器判 FAILED 也报「部署完成」\n"
        "      if (done.status !== 'NEVER_MATCHES') {")

A_F2 = """  const selectOptions = useMemo(
    () => [
      ...executable.map((value) => ({ value, label: deployTypeLabel(value), disabled: false })),
      ...rejectedReasons.map((item) => ({
        value: item.type,
        label: `${deployTypeLabel(item.type)}（服务器不执行）`,
        disabled: true,
      })),
    ],
    [executable, rejectedReasons],
  );"""
R_F2 = """  const selectOptions = useMemo(
    () => [
      { value: 'HOT_LOAD', label: '热加载', disabled: false },
      { value: 'DOCKER', label: 'Docker 镜像', disabled: false },
      { value: 'GIT_PUSH', label: 'Git 推送', disabled: false },
    ],
    // mutant F2: 手抄清单回来了 —— 页面对接口报告了什么完全无感（#70 的原始形状）
    [],
  );"""

A_F3 = """  if (!Array.isArray(value.executable)) {
    throw new Error("""
R_F3 = """  if (!Array.isArray(value.executable)) {
    // mutant F3: 形状漂移降级成空清单 —— "接口漂了"被读成"这个能力下线了"
    return { executable: [], rejected: [] };
  }
  if (false) {
    throw new Error("""

MUTANTS = [
    {
        "id": "J1a", "file": SVC, "anchor": A_J1A, "replace": R_J1A, "layers": ["core", "web"],
        "expect": {
            "core": [
                claim("DeploymentServiceImplTest", "createShouldRefuseADeployTypeTheServerCannotExecute",
                      "兑现不了的部署方式不该出生"),
                claim("DeploymentServiceImplTest", "createShouldRefuseAMissingDeployTypeInsteadOfLeakingANullConstraint",
                      "deploy_type 是 NOT NULL"),
            ],
            "web": [
                claim("DeploymentContractTest", "typesWithoutAnExecutorAreRefusedAndLeaveNothingBehind",
                      "HTTP 状态应当是 400"),
                claim("DeploymentContractTest", "missingDeployTypeIsRefusedWithoutLeakingTheDatabaseError",
                      "HTTP 状态应当是 400"),
                claim("DeploymentContractTest", "vocabularyMatchesWhatTheDoorActuallyDoes",
                      "HTTP 状态应当是 400"),
            ],
        },
    },
    {
        "id": "J1b", "file": SVC, "anchor": A_J1B, "replace": R_J1B, "layers": ["core", "web"],
        "expect": {
            "core": [
                claim("DeploymentServiceImplTest", "createShouldRefuseAPhantomOrForeignMaterializationBatch",
                      "指向不存在的批次：修前实测原样进账并回显 999999"),
            ],
            "web": [
                claim("DeploymentContractTest", "materializationBatchMustExistAndBelongToThisApp",
                      "HTTP 状态应当是 400"),
            ],
        },
    },
    {
        "id": "J2", "file": SVC, "anchor": A_J2, "replace": R_J2, "layers": ["core", "web"],
        "expect": {
            "core": [
                claim("DeploymentServiceImplTest", "createShouldRecordFailureAndNameTheEntityThatDidNotLand",
                      "有一支没建成就不能把返回值判成 SUCCESS"),
            ],
            "web": [
                claim("DeploymentContractTest", "aDefinitionThatCannotLandIsRecordedAsFailedAndNamesTheEntity",
                      "有一支没建成就不能报 SUCCESS"),
            ],
        },
    },
    {
        "id": "J3", "file": SVC, "anchor": A_J3, "replace": R_J3, "layers": ["core", "web"],
        "expect": {
            "core": [
                claim("DeploymentServiceImplTest", "createShouldRecordTheRowThenExecuteAndReturnTheRealOutcome",
                      "库里那一行必须是执行后的状态"),
                claim("DeploymentServiceImplTest", "createShouldRecordFailureAndNameTheEntityThatDidNotLand",
                      "账要留在库里，不能只在返回值里红一下"),
            ],
            "web": [
                claim("DeploymentContractTest", "deploymentActuallyLandsTheDefinitionAndRecordsTheOutcome",
                      "详情读回来的状态要跟写进去的一致"),
                claim("DeploymentContractTest", "aDefinitionThatCannotLandIsRecordedAsFailedAndNamesTheEntity",
                      "失败也要在库里读得回，不能只在响应里红一下"),
            ],
        },
    },
    {
        "id": "F1", "file": PAGE, "anchor": A_F1, "replace": R_F1, "layers": ["ui"],
        "expect": {
            "ui": [
                claim("deploymentVocabulary", "页面的选择项来自 /vocabulary", "页面没有按部署的真结局分叉"),
                claim("DeploymentsPage", "没做成时报的是失败与服务器那句原因", "部署没做成（FAILED）"),
            ],
        },
    },
    {
        "id": "F2", "file": PAGE, "anchor": A_F2, "replace": R_F2, "layers": ["ui"],
        "expect": {
            "ui": [
                claim("deploymentVocabulary", "页面的选择项来自 /vocabulary", "又回到了页面源码里"),
                claim("DeploymentsPage", "部署方式只有接口报告真执行的那几种",
                      "下拉项必须逐字等于从接口长出来的那份"),
            ],
        },
    },
    {
        "id": "F3", "file": APID, "anchor": A_F3, "replace": R_F3, "layers": ["ui"],
        "expect": {
            "ui": [
                claim("deploymentVocabulary", "词表形状不对时判读失败", "这个形状被判成了"),
                claim("DeploymentsPage", "词表读不出来时不猜", "executable 不是数组"),
            ],
        },
    },
]

DENOM = {"core": 19, "web": 7, "ui": 7}


def fqn(test_file):
    tail = str(test_file).split("/src/test/java/", 1)[1]
    return tail[: -len(".java")].replace("/", ".")


def clean_reports(layer):
    """只删本轮要看的那几个报告文件：留着上一支的 XML 会把陈旧红抬进分母。"""
    cfg = LAYERS[layer]
    removed = 0
    if layer == "ui":
        return removed
    for name, path in cfg["classes"].items():
        f = cfg["xml"] / ("TEST-%s.xml" % fqn(path))
        if f.exists():
            f.unlink()
            removed += 1
    return removed


def norm_status(raw):
    """把两侧跑测器的状态词归一：surefire 用 failure/error 节点，vitest 写 passed/failed/pending/skipped/todo。"""
    s = str(raw or "").strip().lower()
    return "fail" if s in ("fail", "failed") else "pass"


def run_layer(layer, tag):
    """跑一层，返回 (读数列表, 分母, 日志路径)。读数 = 每条用例的 (类/文件, 方法/标题, 全文本)。"""
    cfg = LAYERS[layer]
    clean_reports(layer)
    started = time.time()
    proc = subprocess.run(cfg["cmd"], cwd=str(cfg["cwd"]), capture_output=True, text=True)
    log = LOGS / ("%s-%s.log" % (tag, layer))
    log.write_text("$ %s\nrc=%d  %.1fs\n\n===== STDOUT =====\n%s\n\n===== STDERR =====\n%s\n"
                   % (" ".join(cfg["cmd"]), proc.returncode, time.time() - started,
                      proc.stdout, proc.stderr), encoding="utf-8")
    rows = []
    if layer == "ui":
        raw = proc.stdout
        start = raw.find("{")
        if start < 0:
            raise RuntimeError("vitest 没有吐 JSON（rc=%d）—— 见 %s" % (proc.returncode, log))
        data = json.loads(raw[start:])
        total = data.get("numTotalTests", 0)
        for res in data.get("testResults", []):
            key = pathlib.Path(res.get("name", "")).name
            for a in res.get("assertionResults", []):
                text = " | ".join(a.get("failureMessages") or [])
                # vitest 写的是 "failed"，surefire 那一侧我映射成了 "fail"。
                # ⚠ 08:41 那一跑就是栽在这里：reds_of 只认 "fail" ⇒ ui 层结构上永远数不出红，
                # 于是"基线 ui 0 红"是恒真读数，而 F1/F2/F3 各 2 条**真红**被报成 MISMATCH。
                # 状态一律归一化到 fail/pass，不放过任何第三种写法（"skipped"/"todo" 都归 pass）。
                rows.append((key, a.get("fullName", ""), norm_status(a.get("status", "")), text))
        # 量具自证：我自己数出的红条数必须等于 vitest 报的 numFailedTests。
        # 这一句就是本支尺的阳性对照 —— 08:41 那一跑（状态词没归一）有它当场就会 FATAL，
        # 而不是把"ui 恒 0 红"当成基线干净、把三支前端变异的真红报成 MISMATCH。
        declared = int(data.get("numFailedTests", 0))
        if declared != len(reds_of(rows)):
            raise RuntimeError("ui 层读数不自洽: vitest 报 numFailedTests=%d 而我数出 %d 条红 ⇒ 解析层坏了，本轮没有结论"
                               % (declared, len(reds_of(rows))))
    else:
        total = 0
        for name, path in cfg["classes"].items():
            f = cfg["xml"] / ("TEST-%s.xml" % fqn(path))
            if not f.exists():
                raise RuntimeError("找不到 %s（rc=%d）—— 这一层没跑到，见 %s" % (f, proc.returncode, log))
            root = ET.parse(str(f)).getroot()
            cases = root.findall("testcase")
            if not cases:
                raise RuntimeError("%s 里 0 条用例（空参照集不能算通过），见 %s" % (f, log))
            total += int(root.get("tests", len(cases)))
            for c in cases:
                msg = ""
                status = "pass"
                for kind in ("failure", "error"):
                    node = c.find(kind)
                    if node is not None:
                        status = "fail"
                        msg = (node.get("message") or "") + " ⏎ " + (node.text or "")
                        break
                rows.append((name, c.get("name", "").split("[")[0], status, msg))
    if total != DENOM[layer]:
        raise RuntimeError("%s 层分母漂了: 期望 %d 实际 %d（见 %s）" % (layer, DENOM[layer], total, log))
    return rows, total, str(log)


def reds_of(rows):
    return [r for r in rows if r[2] == "fail"]


def match(layer, rows, expected):
    """只认具名红：每条预期 (类, 方法, 文案) 必须在**对应那条红**的全文里找到；多出来的红一律算 MISMATCH。"""
    reds = reds_of(rows)
    seen = set()
    hit, miss = [], []
    for exp in expected:
        found = None
        for idx, (cls, method, _status, text) in enumerate(reds):
            if idx in seen:
                continue
            if exp["class"] not in cls and exp["class"] not in method:
                continue
            if exp["method"] not in method and exp["method"] not in cls:
                continue
            if exp["needle"] not in text:
                continue
            found = idx
            break
        if found is None:
            miss.append(exp)
        else:
            seen.add(found)
            hit.append(exp)
    extra = [reds[i] for i in range(len(reds)) if i not in seen]
    return hit, miss, extra


def judge(mutant, layer, rows):
    hit, miss, extra = match(layer, rows, mutant["expect"].get(layer, []))
    ok = not miss and not extra
    print("  [%s/%s] 红 %d 条 / 预期 %d 条 ⇒ %s"
          % (mutant["id"], layer, len(reds_of(rows)), len(mutant["expect"].get(layer, [])),
             "MATCH" if ok else "MISMATCH"))
    for e in hit:
        print("     ✓ %s#%s ←「%s」" % (e["class"], e["method"], e["needle"]))
    for e in miss:
        print("     ✗ 没打出这一条: %s#%s ←「%s」" % (e["class"], e["method"], e["needle"]))
    for cls, method, _status, text in extra:
        print("     ! 没被点名的红: %s#%s :: %s" % (cls, method, text[:400].replace("\n", " ⏎ ")))
    return ok, {"hit": len(hit), "miss": [m["method"] for m in miss],
               "extra": ["%s#%s" % (c, m) for c, m, _s, _t in extra]}


def apply_mutation(m):
    text = m["file"].read_text(encoding="utf-8")
    n = text.count(m["anchor"])
    if n != 1:
        raise RuntimeError("%s 的锚点在 %s 里出现 %d 次（必须正好 1 次）" % (m["id"], m["file"].name, n))
    m["_before"] = text
    m["file"].write_text(text.replace(m["anchor"], m["replace"], 1), encoding="utf-8")


def restore(m):
    m["file"].write_text(m["_before"], encoding="utf-8")


def class_state():
    """.class 的 (md5, mtime) —— 用来证明这一层量的真是注入后重编出来的字节，不是上一轮的旧壳子。"""
    f = LAYERS["core"]["class_file"]
    if not f.exists():
        raise RuntimeError("%s 不存在 ⇒ 这一层根本没编译，读数无从归属" % f)
    return md5(f), f.stat().st_mtime


def main():
    LOGS.mkdir(parents=True, exist_ok=True)
    _mutlock.acquire(GAUGE)
    only = [x.strip() for x in sys.argv[sys.argv.index("--only") + 1].split(",")] if "--only" in sys.argv else None
    baseline_only = "--baseline" in sys.argv

    gauges_before = {p: md5(p) for p in [T_CORE, T_WEB] + UI_FILES}
    originals = {p: md5(p) for p in [SVC, PAGE, APID]}
    results = {}

    def baseline_pass(tag, label):
        """三层都必须 0 红，并把分母钉死。收尾再跑一遍：还原不彻底的话这一遍会红，而注入的红就不归属了。"""
        print("=== %s（不注入）：三层都必须 0 红，并把分母钉死 ===" % label)
        for layer in ("core", "web", "ui"):
            rows, total, log = run_layer(layer, tag)
            reds = reds_of(rows)
            print("  [%s] 分母 %d，红 %d 条，日志 %s" % (layer, total, len(reds), log))
            if reds:
                for cls, method, _s, text in reds:
                    print("     ! %s就红: %s#%s :: %s" % (label, cls, method, text[:300].replace("\n", " ⏎ ")))
                raise RuntimeError("%s不干净 ⇒ 这一轮没有结论（先修产品或尺，别往下走）" % label)

    try:
        baseline_pass("baseline", "基线")
        if baseline_only:
            print("RESULT BASELINE-ONLY ok")
            return

        for m in MUTANTS:
            if only and m["id"] not in only:
                print("=== %s 本轮没有判定（--only 没点它）===" % m["id"])
                continue
            print("=== %s 注入 %s ===" % (m["id"], m["file"].name))
            apply_mutation(m)
            src_mtime = m["file"].stat().st_mtime
            after = md5(m["file"])
            if after == originals[m["file"]]:
                restore(m)
                raise RuntimeError("%s 写完字节没变 ⇒ 这一支什么都没测" % m["id"])
            verdicts = {}
            try:
                for layer in m["layers"]:
                    rows, _total, _log = run_layer(layer, m["id"])
                    if layer != "ui":
                        # 归属检查只能放在跑完之后：跑之前 .class 必然是旧的（maven 还没编译）。
                        # 判"有没有重编"要拿 .class 的 mtime 跟**注入写入的时刻**比，
                        # 不能拿 md5 比 —— 等价变异（字节本来就一样）会被 md5 尺误判成"没重编"。
                        cm, ct = class_state()
                        if ct < src_mtime - 1.0:
                            raise RuntimeError(
                                "%s/%s：.class 的时刻 %s 早于注入写入 %s ⇒ maven 没重编，"
                                "这一层量的还是注入前的字节，本轮没有结论"
                                % (m["id"], layer, time.strftime("%H:%M:%S", time.localtime(ct)),
                                   time.strftime("%H:%M:%S", time.localtime(src_mtime))))
                        print("     .class 已重编（md5 %s…，时刻 %s）"
                              % (cm[:10], time.strftime("%H:%M:%S", time.localtime(ct))))
                    ok, detail = judge(m, layer, rows)
                    verdicts[layer] = detail
            finally:
                restore(m)
                back = md5(m["file"])
                if back != originals[m["file"]]:
                    raise RuntimeError("还原后 md5 对不上: %s %s != %s" % (m["file"].name, back, originals[m["file"]]))
            for p, h in gauges_before.items():
                if md5(p) != h:
                    raise RuntimeError("尺文件 %s 在这一支里被改过 ⇒ 红集不再属于这一支" % p.name)
            skipped = [l for l in ("core", "web", "ui") if l not in m["layers"]]
            print("  RESULT %s %s | 未跑的层: %s（改的是%s，那几层结构上读不到）"
                  % (m["id"], "OK" if all(v["miss"] == [] and v["extra"] == [] for v in verdicts.values()) else "MISMATCH",
                     ",".join(skipped) or "无",
                     "前端源码" if m["file"] in (PAGE, APID) else "java 服务类"))
            results[m["id"]] = verdicts

        # 七支跑完、字节全数还原 ⇒ 再要一次"三层 0 红"。这一遍红就说明还原不彻底
        # （或者某一层的红根本不是注入带来的），前面那些 MATCH 都不能记账。
        baseline_pass("after", "收尾复跑基线")

        LEDGER.write_text(json.dumps({"ts": time.strftime("%Y-%m-%d %H:%M:%S"),
                                      "denom": DENOM, "runners": os.environ.get("USER", "?"),
                                      "mutants": results}, ensure_ascii=False, indent=2), encoding="utf-8")
        bad = [k for k, v in results.items() if any(x["miss"] or x["extra"] for x in v.values())]
        print("RESULT %s 支判定，其中 MISMATCH: %s" % (len(results), ",".join(bad) or "无"))
        print("台账: %s" % LEDGER)
        if bad:
            sys.exit(1)
    finally:
        _mutlock.release()


if __name__ == "__main__":
    main()
