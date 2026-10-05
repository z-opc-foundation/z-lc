#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #61 的 java 侧注入自证：改产品源码，看那 68（core 四类）+ 13（契约层）条断言逐条红下来.

TASK §2.3 点名的六支都在这里。为什么必须有这一份（而不是"契约层 13 条绿着"就算完）：
#61 的原始形状恰恰是**全套测试绿而运行期什么都不做** —— {@code listByEvent} 与
{@code startProcess} 在生产代码里零调用者的那段时间，单测与 DTO 测试一条都没红过。
"绿"本身不是证据，"我故意把它改坏，它红在哪一条"才是。

量的是两层，每支注入两层都跑（另一层**预期 0 红**也是一条判定，不是没跑）：
  core = CamudaAdapterTest + WorkflowTriggersTest + WorkflowBindingServiceTest + WorkflowTriggerDispatcherTest
  web  = WorkflowTriggerContractTest（真起上下文、真打 HTTP、真发一句到桩、真回读账）

六支（M1..M6）与各自打掉的东西：
  M1 摘掉 {@code RuntimeCrudController} 的写后派发 ⇒ web 10 条同时红。这一支同时是"接线还在"的复证：
     10 条里有 5 条落在 helper {@code fireRow} 的同一条断言上（账上读不到那一行），另外 5 条各自
     红在自己的桩计数断言或阳性对照上。core 层 0 红 —— 派发点在 web，单测层结构上看不见它，这正是契约层
     必须单独存在的那件事（#51/#54/#57 同轴）。
  M2 把 {@code AFTER_UPDATE} 塞进引擎的 implemented 清单 ⇒ 词表/写入口/拒绝面三处一致性一起红：
     core 5 条（清单本身、"两份名单不许混"、拒绝原因点名不了 —— 兑现得了的事件没有拒绝理由、
     写入口该拒的没拒 **两个门一起漏**），web 2 条（/vocabulary 必须就是引擎那份、400 那一道）。
     第 5 条（update 那道门）是第一跑实测补进来的，不是设想出来的：create 与 update 共用
     {@code validateForWrite}，名单一混，"先建一条合法的、再把它改成合法的以外"那条绕过创建闸的
     路跟着一起漏 —— 少写这一条就是把一条真实的连带红当成"注入不干净"。
     这一支也顺带证明"拒绝原因必须逐条带着为什么"：名单一混，最先坏掉的是一句 NPE。
  M3 摘掉 {@code listByEvent} 的租户条件 ⇒ 跨租户越界 1 条 + 查重把别人的行算进来 1 条
     + **替身自证**那 1 条（夹具自己承认它按谓词过滤，条件一摘它先红）+ 契约层外租户 1 条。
  M4 把 URL 改回 #61 修复前那句 {@code /approval-center/process/start} ⇒ core 2 条 + web 1 条。
     桩对路径无感（任何路径都照脚本回话），所以这一支不会连带出一堆 404 假红：三条红的都是
     "打出去的路径对不对"这同一件事。
  M5 把状态码闸摘回 {@code !res.isSuccess()} ⇒ core 2 条 + web 1 条。这一支跑出来三件事：
     ① {@code HttpExecutor} 对 4xx/5xx 一样走 {@code HttpExecutionResult.ok(...)}（字节码里
        {@code iconst_1 → putfield success}），所以 {@code isSuccess()} 恒真 —— 摘掉 2xx 那道闸
        之后连 404 都被当成受理；
     ② 404 那一条里"要说清是 http 几"**假绿**（消息把整个 body 抄进文案，而那个 body 里正好有
        "404"），真红的是下一条"要带上打的是哪条路径"。这是文案里混入回显内容把断言写软的形状，
        已按实测记进 _e2e/README；
     ③ 第一跑这一支 **web 0 红**。那一条 0 当时按"未覆盖"记账（契约层的桩没有"引擎回非 2xx"这一格
        形状 ⇒ 摘闸在真进程边界上无人看见），随后把那一格补进了契约层
        （{@code http5xxWithASuccessfulLookingBodyIsNotAFire}：502 + 成功 body ⇒ 必须是一行 FAILED
        且 detail 里带 {@code http=502}，另配 200 同 body 的阳性对照），复测才有上面那条 web 1 红。
        这是这一族少见的反向收获：量具找出的不是"断言太软"，而是"被测面缺一整格形状"。
  M6 把 {@code data.processInstanceId} 读回成整个 data 的 {@code toString()} ⇒ core 2 条 + web 1 条。
     整份 data 的文本既"不等于 wf-42"又"永远非空"⇒ 连 {@code data:{}} 都会被当成一次成功发起，
     "没有实例 id 就不算成功"那半边一起漏。

量具自己的规矩（与前几支同源）：
  * 注入前把原始字节读进内存，还原只从内存写回 + 逐文件 md5 对账（**不用** {@code git checkout --}：
    共享工作树里那一句会把别人未提交的改动一起抹掉）；
  * 写回一律 {@code write_text}（mtime = 现在）而不是 {@code copy2}（连 mtime 一起倒回过去 ⇒
    maven 增量编译跳过重编 ⇒ 下一支量的还是上一支的变异字节）。这一条另有正面证据：每支注入都
    要求被改文件的 {@code .class} 指纹相对基线**变了**，没变就判"这一轮没有结论"；
  * 锚点必须逐字出现恰好一次，否则 SKIPPED 而不猜；
  * 只认具名红：一条预期红 = (测试类, 测试方法, 那条断言的文案)，文案在其作用域内逐字唯一，
    行号区间从被测文件的当前字节现搜（改名或挪位置都会被发现）；一支 JUnit 方法只报第一条红，
    所以同一支里同一方法只许认领一次；
  * 分母每轮必须相等；XML 缺失或 0 条用例直接抛（空参照集会打印"满分"）；没被点名的红一律
    MISMATCH，不加白名单；
  * 复跑期间不改被测源码；台账与日志落 ~/.cache（/tmp 会被别的会话扫），并记"谁跑的"。

跑法：{@code python3 _e2e/mutate_workflow_trigger_guard.py}（全 6 支，约 1.5 分钟，
      整轮 stdout 另落 {@code ~/.cache/zlc61/mut/logs/guard-<时间>.log} —— README 引的那一句从那儿回读）
      {@code ... --only M2,M5}（只跑这两支，判据一条不减，但 RESULT 行会写明另几支本轮没有判定）
      {@code ... --recompute}（只从台账 JSON 复算读数，不重新注入）
"""
import hashlib
import json
import os
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

REPO = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO / "z-lc-admin-ui" / "e2e"))
import _mutlock  # noqa: E402

# ---- 被测产品源码 ---------------------------------------------------------------------------
ADAPTER = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/adapter/CamudaAdapter.java"
TRIGGERS = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowTriggers.java"
BINDING_SVC = REPO / "z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowBindingService.java"
CRUD = REPO / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/RuntimeCrudController.java"

# ---- 尺（测试文件只用来现搜断言文案与行号） --------------------------------------------------
T_ADAPTER = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/adapter/CamudaAdapterTest.java"
T_TRIGGERS = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/workflow/WorkflowTriggersTest.java"
T_BINDING = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/workflow/WorkflowBindingServiceTest.java"
T_DISPATCH = REPO / "z-lc-core/src/test/java/com/zifang/z/lc/core/workflow/WorkflowTriggerDispatcherTest.java"
T_CONTRACT = REPO / "z-lc-web/src/test/java/com/zifang/z/lc/web/it/WorkflowTriggerContractTest.java"

LOGS = pathlib.Path(os.path.expanduser("~/.cache/zlc61/mut/logs"))
LEDGER = pathlib.Path(os.path.expanduser("~/.cache/zlc61/mut/ledger.json"))

FIRE_ROW = "这条记录在账上应该正好一行"  # helper fireRow 的那一条，M1 有四支落在它上面

LAYERS = {
    "core": {
        "module": "z-lc-core",
        "classes": {
            "CamudaAdapterTest": T_ADAPTER,
            "WorkflowTriggersTest": T_TRIGGERS,
            "WorkflowBindingServiceTest": T_BINDING,
            "WorkflowTriggerDispatcherTest": T_DISPATCH,
        },
        "xml": REPO / "z-lc-core/target/surefire-reports",
    },
    "web": {
        "module": "z-lc-web",
        "classes": {"WorkflowTriggerContractTest": T_CONTRACT},
        "xml": REPO / "z-lc-web/target/surefire-reports",
    },
}


def fqn(test_file):
    """测试文件的 FQN —— surefire 的报告文件名就是它。按目录现推，不写死包名：
    这四个类分属 core.adapter / core.workflow / web.it 三个包，写死一个就把另外两个判成"没跑到"。"""
    tail = str(test_file).split("/src/test/java/", 1)[1]
    return tail[:-len(".java")].replace("/", ".")


def claim(cls, method, needle, host=None):
    """一条预期红。host=None ⇒ 文案在全文件唯一；host="方法名" ⇒ 只要求在那个体内唯一。"""
    return (cls, method, needle, host)


# ---- 注入锚点 -------------------------------------------------------------------------------
A_M1 = ('        int fired = workflowTriggerDispatcher.afterCreate(body.getTenantCode(), body.getAppCode(),\n'
        '                def.getEntityCode(), id, body.getFieldValues(), actor);')
# 分支里那句**不带** `int fired =`：照抄整句会让局部变量重名、变异体连编译都过不去
# （编译不过的变异体不测任何东西，这一支就没有结论了 —— 第一次跑正是这样暴露的）。
R_M1 = ('        int fired = 0;\n'
        '        if (false) {\n'
        '            // mutant M1: 摘掉写后派发 —— 绑定照存，运行期一个字都不执行（#61 的原始形状）\n'
        '            workflowTriggerDispatcher.afterCreate(body.getTenantCode(), body.getAppCode(),\n'
        '                    def.getEntityCode(), id, body.getFieldValues(), actor);\n'
        '        }')

A_M2 = '    private static final List<String> IMPLEMENTED = Collections.singletonList(AFTER_CREATE);'
R_M2 = '    private static final List<String> IMPLEMENTED = Arrays.asList(AFTER_CREATE, "AFTER_UPDATE");'

A_M3 = ('        return workflowBindingMapper.selectList(\n'
        '                new QueryWrapper<WorkflowBindingEntity>()\n'
        '                        .eq("tenant_code", tenantCode)\n'
        '                        .eq("app_code", appCode)\n'
        '                        .eq("entity_code", entityCode)\n'
        '                        .eq("trigger_event", triggerEvent)')
R_M3 = ('        return workflowBindingMapper.selectList(\n'
        '                new QueryWrapper<WorkflowBindingEntity>()\n'
        '                        .eq("app_code", appCode)\n'
        '                        .eq("entity_code", entityCode)\n'
        '                        .eq("trigger_event", triggerEvent)')

A_M4 = '    static final String START_PATH = "/api/approval-center/processes/start";'
R_M4 = '    static final String START_PATH = "/approval-center/process/start";'

A_M5 = '        if (!CtcAdapter.httpAccepted(res)) {'
R_M5 = '        if (!res.isSuccess()) {'

A_M6 = '        String instanceId = data.getString("processInstanceId");'
R_M6 = '        String instanceId = data.toString();'

RUNS = [
    {
        "tag": "M1",
        "what": "摘掉 RuntimeCrudController 的写后派发",
        "edits": [(CRUD, A_M1, R_M1)],
        "core": [],
        "web": [
            claim("WorkflowTriggerContractTest", "bindingActuallyFiresAndTheLedgerReadsBackStarted",
                  "桩应该正好收到一句"),
            claim("WorkflowTriggerContractTest", "unrealizableBindingsAreRefusedAndFireNothing",
                  "登记成功的那一条要真发得出去"),
            claim("WorkflowTriggerContractTest", "duplicateBindingIsRefusedWhileTheFirstOneStillFires",
                  "重复绑定没能进去"),
            claim("WorkflowTriggerContractTest", "engineRefusalKeepsTheUsersWriteAndLeavesAFailedRow", FIRE_ROW),
            claim("WorkflowTriggerContractTest", "slowEngineIsCutOffAtTheConfiguredDeadline", FIRE_ROW),
            claim("WorkflowTriggerContractTest", "timeoutDoesNotPoisonTheNextFires", FIRE_ROW),
            claim("WorkflowTriggerContractTest", "unreachableEngineBecomesAFailedRowNotAnError", FIRE_ROW),
            claim("WorkflowTriggerContractTest", "foreignTenantBindingNeverFires",
                  "改掉租户之后发得出去"),
            claim("WorkflowTriggerContractTest", "importAndRedoDoNotFire", "逐条新建要发一句"),
            # 这一条跟着"契约层补了 5xx+成功 body 那一格"一起进来（不是预先设想，是补完那一格
            # 之后重测得到的）：派发点没了 ⇒ 那一条也在同一个 helper 的账回读上红。
            claim("WorkflowTriggerContractTest", "http5xxWithASuccessfulLookingBodyIsNotAFire", FIRE_ROW),
        ],
        "why": "派发点没了 ⇒ 桩一句都没收到、账上一行都没有。红的分布本身就是结论：桩计数、账回读、"
               "阳性对照三种角度各自指着同一个挂接点；core 层 0 红说明这一层结构上看不见它。",
    },
    {
        "tag": "M2",
        "what": "把 AFTER_UPDATE 塞进引擎的 implemented 清单",
        "edits": [(TRIGGERS, A_M2, R_M2)],
        "core": [
            claim("WorkflowTriggersTest", "afterCreateIsTheOnlyHookTheEngineReallyHas",
                  "今天真有挂接点的只有写后发起这一档"),
            claim("WorkflowTriggersTest", "vocabularyIsConsistentWithTheWriteGate",
                  "既在 implemented 又在拒绝清单里"),
            claim("WorkflowTriggersTest", "rejectionReasonsSayWhyNotJustNo", "这条记录对应哪个流程实例"),
            claim("WorkflowBindingServiceTest", "createShouldRefuseEveryTriggerTheEngineCannotHonor",
                  "写入口应当拒掉，好让一份兑现不了的绑定不出生"),
            # 这一条是第一跑**实测补上来的**（原本以为只有 create 那一族会红）：update 走的是同一个
            # validateForWrite ⇒ "先建一条合法的、再改成合法的以外" 那条绕过创建闸的路一起漏。
            # 它是同一个缺陷的第二批证据（写入口的两个门），不是连带噪声，所以按名字钉住。
            claim("WorkflowBindingServiceTest", "updateShouldNotLetAValidRowBeTurnedIntoAnUnhonorableOne",
                  "先建一条合法的、再把它改成合法的以外"),
        ],
        "web": [
            claim("WorkflowTriggerContractTest", "vocabularyMirrorsTheEngine",
                  'assertEquals(Arrays.asList("AFTER_CREATE"), implemented);'),
            claim("WorkflowTriggerContractTest", "unrealizableBindingsAreRefusedAndFireNothing",
                  "HTTP 状态应当是 400"),
        ],
        "why": "两份名单一混，先坏的是"
               "「说能登记的必须放行、说不兑现的必须拒」那一句一致性断言；reasonUnavailable 一返回 "
               "null，拒绝原因那一条就以 NPE 的形态红（同一个缺陷的第二批证据）。接口层红在"
               "「词表必须就是引擎那份」与「兑现不了的形态在写入口就 400」两格。",
    },
    {
        "tag": "M3",
        "what": "摘掉 listByEvent 的租户条件",
        "edits": [(BINDING_SVC, A_M3, R_M3)],
        "core": [
            claim("WorkflowBindingServiceTest", "fixtureReallyAppliesWrapperPredicates",
                  "夹具: 挂接点上只该留下同租户/同应用/同实体/同事件、未删除且 auto_submit=1 的那一行"),
            claim("WorkflowBindingServiceTest", "listByEventShouldSeeNothingForAnotherTenant",
                  "跨租户的绑定不能出现在这个租户的挂接点上"),
            claim("WorkflowBindingServiceTest", "createShouldAllowTheSameProcessOnADifferentEntityOrTenant",
                  'service.create(binding("other", APP, ENTITY, WorkflowTriggers.AFTER_CREATE, PROCESS));'),
        ],
        "web": [
            claim("WorkflowTriggerContractTest", "foreignTenantBindingNeverFires",
                  "外租户的绑定被本租户的记录写触发"),
        ],
        "why": "替身自证那条先红 ⇒ 这一支不是「夹具忽略谓词所以看不见」。查重那条红是因为 "
               "requireNotDuplicate 走的就是 listByEvent：租户一摘，别的租户登记的同一个流程 KEY 会"
               "顶掉本租户的正常写入 —— 方向与越界相反，两条都得在账上。契约层那条才是用户看得见的"
               "那件事：替别人提单。",
    },
    {
        "tag": "M4",
        "what": "把发起 URL 改回 #61 修复前那句 /approval-center/process/start",
        "edits": [(ADAPTER, A_M4, R_M4)],
        "core": [
            claim("CamudaAdapterTest", "postsToThePathZwfActuallyMaps",
                  '"http://127.0.0.1:1/api/approval-center/processes/start"'),
            claim("CamudaAdapterTest", "httpFailureCarriesStatusAndPath", "要带上打的是哪条路径"),
        ],
        "web": [
            claim("WorkflowTriggerContractTest", "bindingActuallyFiresAndTheLedgerReadsBackStarted",
                  "打的必须是 z-camuda 真映射的那条路径"),
        ],
        "why": "桩对路径无感 ⇒ 三条红的都是同一件事（打出去的路径是不是 z-camuda 真映射的那一条），"
               "第一红落在 startUrl() 的逐字比对上，它早于桩那一发。",
    },
    {
        "tag": "M5",
        "what": "把状态码闸摘回 !res.isSuccess()",
        "edits": [(ADAPTER, A_M5, R_M5)],
        "core": [
            claim("CamudaAdapterTest", "non2xxIsRefusedEvenWhenTheBodyLooksLikeASuccessEnvelope",
                  "500 就是没受理"),
            claim("CamudaAdapterTest", "httpFailureCarriesStatusAndPath", "要带上打的是哪条路径"),
        ],
        "web": [
            # 契约层原来**没有**"引擎回非 2xx"这一格形状 ⇒ 这一支 web 0 红（第一跑如实登记成"未覆盖"）。
            # 补了 `http5xxWithASuccessfulLookingBodyIsNotAFire` 之后重测：这一支在真进程边界上也有牙了
            # —— 摘掉状态码闸，5xx+成功信封会直接写出一行 STARTED 带实例号，那一条先红。
            claim("WorkflowTriggerContractTest", "http5xxWithASuccessfulLookingBodyIsNotAFire",
                  "5xx 时 body 里那个 processInstanceId 一个字都不能信"),
        ],
        "why": "HttpExecutor 对 4xx/5xx 一样调 HttpExecutionResult.ok(...) ⇒ isSuccess() 恒真，"
               "500+成功信封被当成受理。顺带抓出一条软处：404 那支里「要说清是 http 几」因为消息抄了"
               "整个 body、而 body 里正好有 404 而假绿，真红的是「要带上打的是哪条路径」。"
               "web 层那一条是量具反过来把被测面补宽的证据：先前它 0 红不是"
               "「运行时不受影响」，而是这一层压根没有那个形状。",
    },
    {
        "tag": "M6",
        "what": "实例 id 读回成整个 data 的 toString()",
        "edits": [(ADAPTER, A_M6, R_M6)],
        "core": [
            claim("CamudaAdapterTest", "readsInstanceIdFromDataProcessInstanceId",
                  'assertEquals("wf-42", start.getInstanceId())'),
            claim("CamudaAdapterTest", "successWithoutInstanceIdIsNotSuccess",
                  "assertFalse(start.getFailure(), start.isStarted());", "successWithoutInstanceIdIsNotSuccess"),
        ],
        "web": [
            claim("WorkflowTriggerContractTest", "bindingActuallyFiresAndTheLedgerReadsBackStarted",
                  "实例 id 必须是 z-camuda data.processInstanceId 那一格"),
        ],
        "why": "整份 data 的文本既「不等于 wf-42」又「永远非空」⇒ 连 data:{} 都会被当成一次成功发起，"
               "「没有实例 id 就不算成功」那半边一起漏；账上那一格则是用户在界面上读到的东西。",
    },
]

SOURCES = sorted({p for run in RUNS for p, _, _ in run["edits"]})
SCALE = sorted({f for layer in LAYERS.values() for f in layer["classes"].values()})
FILES = sorted(set(SOURCES) | set(SCALE), key=str)

ONLY = {x.strip() for x in (sys.argv[2].split(",") if sys.argv[1:2] == ["--only"] else []) if x.strip()}
if sys.argv[1:2] == ["--only"] and not ONLY:
    raise SystemExit("--only 后面是空的：一支都不跑不等于「全跑」，那是「没有判定」")


def sh(cmd, timeout=1800):
    return subprocess.run(cmd, cwd=str(REPO), capture_output=True, text=True, timeout=timeout)


def md5(path):
    return hashlib.md5(path.read_bytes()).hexdigest()


def class_file(source):
    """src/main/java/x/Y.java -> <module>/target/classes/x/Y.class（编译器有没有量到这份变异看它）。"""
    parts = list(source.relative_to(REPO).parts)
    module = parts[0]
    tail = "/".join(parts[1:]).split("src/main/java/", 1)[1]
    return REPO / module / "target/classes" / tail.replace(".java", ".class")


def module_of(source):
    return source.relative_to(REPO).parts[0]


def method_span(lines, host):
    """{@code void host(...)} 那一行到与之配对的右括号，返回 0 基 [start, end)。"""
    for i, line in enumerate(lines):
        if re.search(r"\b" + re.escape(host) + r"\s*\([^()]*\)\s*(throws [\w, ]+)?\s*\{", line):
            depth = 0
            for j in range(i, len(lines)):
                depth += lines[j].count("{") - lines[j].count("}")
                if depth == 0 and j > i:
                    return i, j + 1
            return i, len(lines)
    return None


def statement_extent(lines, idx):
    """把「文案所在行」归到它所属 assert 调用的首行，并延伸到该语句结束行。

    JUnit 报的行号由 javac 说了算：长消息参数的续行、跨行的 assertEquals(...) 可能被算到首行或末行。
    只锁一行会把一条正当的红读成"红在另一条"（#47 那一族实测过），所以锁的是**区间**。
    """
    start = idx
    while start > 0:
        prev = lines[start - 1].rstrip()
        stripped = prev.lstrip()
        if not stripped or stripped.startswith(("//", "*", "/*")):
            break
        if prev.endswith((";", "{", "}", "*/")):
            break
        start -= 1
    end = idx
    while end < len(lines) - 1 and not lines[end].rstrip().endswith(";"):
        end += 1
    return start, end


def resolve(layer_key, cls, method, needle, host, cache):
    """一条预期红 → (行号区间[1 基], 出处说明)；定位不了就返回原因，让 preflight 当场 FATAL。"""
    path = LAYERS[layer_key]["classes"][cls]
    if path not in cache:
        cache[path] = path.read_text(encoding="utf-8").splitlines()
    lines = cache[path]
    span = None
    scope = "全文件"
    if host:
        span = method_span(lines, host)
        if span is None:
            return None, f"{path.name} 里找不到方法 {host}（host 作用域失效）"
        scope = f"方法 {host} 体内"
    hits = [i for i, line in enumerate(lines)
            if needle in line and (span is None or span[0] <= i < span[1])]
    if len(hits) != 1:
        return None, (f"文案 {needle!r} 在 {path.name} 的{scope}出现 {len(hits)} 次（要 1 次），"
                      "它就不是一条断言的名字了")
    start, end = statement_extent(lines, hits[0])
    return (start + 1, end + 1), f"{path.name}:{start + 1}-{end + 1}（{scope}唯一）"


def run_layer(key, label):
    """跑一层的指定测试类 → (分母, [(类, 方法, 报出行)], {类: {方法名}})。跑不起来就抛。"""
    cfg = LAYERS[key]
    for cls, path in cfg["classes"].items():
        (cfg["xml"] / f"TEST-{fqn(path)}.xml").unlink(missing_ok=True)
    proc = sh(["mvn", "-o", "-B", "-pl", cfg["module"], "-am", "test",
               "-Dtest=" + ",".join(sorted(cfg["classes"])), "-Dsurefire.failIfNoSpecifiedTests=false"])
    out = proc.stdout + "\n--- stderr ---\n" + proc.stderr
    LOGS.mkdir(parents=True, exist_ok=True)
    (LOGS / f"{label}.log").write_text(out, encoding="utf-8")
    if "COMPILATION ERROR" in out or "cannot find symbol" in out:
        raise RuntimeError(f"变异体编译不过（它不测任何东西，这一轮没有结论），见 {LOGS / (label + '.log')}")
    reds, total, names = [], 0, {}
    for cls, path in cfg["classes"].items():
        xml = cfg["xml"] / f"TEST-{fqn(path)}.xml"
        if not xml.exists():
            raise RuntimeError(f"{key} 层没有 {cls} 的 surefire XML —— 这一类根本没跑到，"
                               f"「没红」不成立，见 {LOGS / (label + '.log')}")
        cases = ET.parse(xml).getroot().findall("testcase")
        if not cases:
            raise RuntimeError(f"{cls} 的 XML 里 0 条用例 —— 空参照集会打印「满分」，不能算通过")
        total += len(cases)
        names[cls] = {tc.get("name") for tc in cases}
        for tc in cases:
            node = tc.find("failure")
            if node is None:
                node = tc.find("error")
            if node is None:
                continue
            trace = (node.text or "") + " " + (node.get("message") or "")
            m = re.search(re.escape(path.name) + r":(\d+)", trace)
            reds.append((cls, tc.get("name"), int(m.group(1)) if m else -1))
    if proc.returncode not in (0, 1):
        raise RuntimeError(f"mvn 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，"
                           f"见 {LOGS / (label + '.log')}")
    print(f"  {key} {label}: 分母 {total} 条 / 红 {len(reds)} 条")
    return total, reds, names


def judge(layer_key, tag, claims, reds, located):
    """预期红按 (类, 方法) 认，且报出行要落在那条断言的区间里；没点名的红一律算意外。"""
    bad = 0
    want = {(c, m) for c, m, _, _ in claims}
    got = {}
    for cls, method, line in reds:
        got.setdefault((cls, method), []).append(line)
    for cls, method in sorted(want):
        where, span = located[(tag, layer_key, cls, method)]
        hits = got.get((cls, method))
        if hits and any(span[0] <= ln <= span[1] for ln in hits):
            print(f"  OK  预期那条真的红了: {cls}.{method}:{hits[0]}  ({where})")
        elif hits:
            print(f"  !!  它红了但不在我认领的那条断言上: {cls}.{method} 报在 {hits}，"
                  f"预期区间 {span}（{where}）")
            bad += 1
        else:
            print(f"  !!  预期变红却没红: {cls}.{method}（{where}）—— 这一支是空的，或被别处替它挡了")
            bad += 1
    for (cls, method), hits in sorted(got.items()):
        if (cls, method) not in want:
            print(f"  !!  未预期的红: {cls}.{method} 行 {hits[0]} —— 不许加白名单了事，"
                  "先证明这一支注入换掉了哪一条断言，再按实测改预期")
            bad += 1
    return bad


def preflight():
    """开跑前钉三件事：锚点逐字唯一、每条预期红的文案唯一且定位得到、同一支里同一方法不被点两次。"""
    problems = []
    cache = {}
    src = {p: p.read_text(encoding="utf-8") for p in FILES}
    located = {}
    for run in RUNS:
        for path, anchor, _ in run["edits"]:
            n = src[path].count(anchor)
            if n != 1:
                problems.append(f"{run['tag']} 锚点出现 {n} 次（要 1 次）: {path.name}")
        if not run["core"] and not run["web"]:
            problems.append(f"{run['tag']} 一支预期红都没有 —— 那它不测任何东西")
        for key in ("core", "web"):
            seen = set()
            for cls, method, needle, host in run[key]:
                span, where = resolve(key, cls, method, needle, host, cache)
                if span is None:
                    problems.append(f"{run['tag']} 认领不了的一条预期红: {where}")
                    continue
                if (cls, method) in seen:
                    problems.append(f"{run['tag']}: {method} 被点了两次 —— 一支 JUnit 方法只报第一条红，"
                                    "两次认领里必有一条是编的")
                seen.add((cls, method))
                located[(run["tag"], key, cls, method)] = (where, span)
    tags = {r["tag"] for r in RUNS}
    if ONLY - tags:
        problems.append(f"--only 里有不存在的注入: {sorted(ONLY - tags)}（可选 {sorted(tags)}）")
    if problems:
        for p in problems:
            print(f"!! {p}")
        raise SystemExit(2)
    owned = sum(len(r["core"]) + len(r["web"]) for r in RUNS)
    print(f"preflight: {len(RUNS)} 支注入 / {owned} 条预期红的文案与行号全部现搜定位到唯一断言")
    return located


def recompute():
    book = json.loads(LEDGER.read_text(encoding="utf-8"))
    bad = book.get("bad", 0)
    print(f"台账 {LEDGER}\n  谁跑的: {book['ran_by']}  起于 {book['started_at']}  HEAD={book['head']}")
    print(f"  基线分母 {book['denominators']}")
    for run in book["runs"]:
        core = run.get("core", {})
        web = run.get("web", {})
        print(f"  {run['tag']:3} 注入于 {run['injected_at']}: core 红 {len(core.get('red_names', []))} "
              f"{core.get('red_names', [])} | web 红 {len(web.get('red_names', []))} "
              f"{[x.split(':')[0] for x in web.get('red_names', [])]} | 本轮判定问题 {run['problems']}")
        bad += run["problems"]
    print(f"  还原: {book['restored']}  收尾复跑全绿: {book['rerun_green']}")
    print("RESULT: " + ("ledger 复算：全对" if bad == 0 else f"ledger 有 {bad} 处问题"))
    return 1 if bad else 0


def main():
    if "--recompute" in sys.argv:
        return recompute()

    located = preflight()
    head = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=str(REPO),
                          capture_output=True, text=True).stdout.strip()
    book = {"ran_by": f"{os.environ.get('USER', '?')}@{os.uname().nodename} pid={os.getpid()}",
            "started_at": time.strftime("%Y-%m-%d %H:%M:%S"), "head": head, "runs": []}

    originals = {path: path.read_text(encoding="utf-8") for path in FILES}
    digest0 = {path: md5(path) for path in FILES}

    def restore():
        for path, text in originals.items():
            if path.read_text(encoding="utf-8") != text:
                # write_text ⇒ mtime 落在现在。用 copy2 还原会把 mtime 一起倒回过去，
                # 于是 maven 增量编译跳过重编、下一支量的还是上一支的变异字节。
                path.write_text(text, encoding="utf-8")

    bad = 0
    denoms = {}
    try:
        restore()
        print("\n=== 基线（两层都必须全绿，否则注入结果无法归因）===")
        ran = {}
        for key in ("core", "web"):
            denoms[key], reds, names = run_layer(key, "00_baseline_" + key)
            ran[key] = names
            if reds:
                for cls, method, line in reds:
                    print(f"  !! 基线就红: {cls}.{method}:{line}")
                print("  !! 基线有红，先修基线 —— 带着红的基线上做出的「没红」一句都不算")
                return 2
        for run in RUNS:
            for key in ("core", "web"):
                for cls, method, _needle, _host in run[key]:
                    if method not in ran[key].get(cls, set()):
                        print(f"!! {run['tag']} 点名的 {method} 不在 {cls} 基线真跑过的那一批里"
                              "（打错字不等于没红，是这条检查不存在）")
                        bad += 1
        baseline_class = {}
        for path in SOURCES:
            cf = class_file(path)
            if not cf.exists():
                print(f"!! {path.name} 的产物找不到（{cf}）—— 指纹没有基线，这一支不能给结论")
                bad += 1
            else:
                baseline_class[path] = md5(cf)
        if bad:
            return 2

        for i, run in enumerate(RUNS, start=1):
            if ONLY and run["tag"] not in ONLY:
                print(f"\n[{run['tag']}] 本轮未选（--only {','.join(sorted(ONLY))}），不记账也不判")
                continue
            print(f"\n=== {i}. {run['tag']} {run['what']} ===\n    因果: {run['why']}")
            skip = False
            for path, anchor, repl in run["edits"]:
                if originals[path].count(anchor) != 1:
                    print(f"  SKIPPED: 锚点在 {path.name} 里出现 {originals[path].count(anchor)} 次")
                    bad += 1
                    skip = True
                    break
                path.write_text(originals[path].replace(anchor, repl, 1), encoding="utf-8")
            if skip:
                restore()
                continue
            entry = {"tag": run["tag"], "what": run["what"], "problems": 0,
                     "injected_at": time.strftime("%H:%M:%S"),
                     "mutant_md5": {path.name: md5(path) for path, _, _ in run["edits"]}}
            try:
                for key in ("core", "web"):
                    n, reds, _names = run_layer(key, f"{i:02d}_{run['tag']}_{key}")
                    entry[key] = {"denominator": n, "red_names": [f"{c}.{m}:{l}" for c, m, l in reds],
                                  "class_changed": {}}
                    if n != denoms[key]:
                        print(f"  !! {key} 层分母从 {denoms[key]} 变成 {n} —— 套件没按同一批测试跑，不算")
                        bad += 1
                    for path, _, _ in run["edits"]:
                        if module_of(path) != LAYERS[key]["module"]:
                            continue
                        cf = class_file(path)
                        changed = cf.exists() and md5(cf) != baseline_class[path]
                        entry[key]["class_changed"][path.name] = changed
                        if not changed:
                            print(f"  !! {key} 层：{path.name} 的 .class 指纹没动 —— 编译器没量到这份变异，"
                                  "这一轮的「没红」不成立")
                            bad += 1
                    if entry[key]["class_changed"]:
                        print(f"  .class 指纹: {entry[key]['class_changed']}")
                    problems = judge(key, run["tag"], run[key], reds, located)
                    bad += problems
                    entry["problems"] += problems
            finally:
                restore()
            book["runs"].append(entry)
            for path in FILES:
                if md5(path) != digest0[path]:
                    print(f"  !! {path.name} 没还原到基线字节")
                    bad += 1

        print("\n=== 全部还原后两层复跑 ===")
        rerun_green = True
        for key in ("core", "web"):
            n, reds, _names = run_layer(key, "99_restored_" + key)
            if n != denoms[key] or reds:
                print(f"  !! 还原后 {key} 层不干净: 分母 {n} vs {denoms[key]}, 红 {reds}")
                rerun_green = False
                bad += 1
            for path in SOURCES:
                if module_of(path) != LAYERS[key]["module"]:
                    continue
                cf = class_file(path)
                if cf.exists() and md5(cf) != baseline_class[path]:
                    print(f"  !! {cf} 还是变异体的字节（源码还原了而产物没跟上，下一轮会量到脏 class）")
                    bad += 1
        book["denominators"] = denoms
        book["restored"] = all(md5(p) == digest0[p] for p in FILES)
        book["rerun_green"] = rerun_green
        book["bad"] = bad
        LEDGER.parent.mkdir(parents=True, exist_ok=True)
        LEDGER.write_text(json.dumps(book, ensure_ascii=False, indent=1), encoding="utf-8")
        owned = sum(len(r["core"]) + len(r["web"]) for r in RUNS if not ONLY or r["tag"] in ONLY)
        verdict = "java-layer falsification done" if bad == 0 else f"{bad} problem(s)"
        if ONLY:
            verdict += f" (PARTIAL: 只跑了 --only {','.join(sorted(ONLY))}, " \
                       f"{len(RUNS) - len(ONLY)} 支本轮没有判定)"
        print(f"\n台账: {LEDGER}")
        print(f"RESULT: {verdict} | 本轮 {owned} 条具名红 / 分母 {denoms}")
        return 1 if bad else 0
    finally:
        restore()
        leaked = [p.name for p in FILES if md5(p) != digest0[p]]
        print("restored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))


class Tee:
    """整轮 stdout 另落一份日志。

    为什么不省：README 与工单里那句"现读日志"要真的有一支日志可回读 —— `RESULT:` 那一行原本只活在
    当时那次终端输出里，台账 JSON 只有结构化读数，没有"哪一条被判成 MISMATCH"的现场。
    """

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
    _mutlock.acquire(os.path.basename(__file__))
    try:
        sys.exit(main())
    finally:
        sys.stdout = sys.stdout.stream
        _mutlock.release()
