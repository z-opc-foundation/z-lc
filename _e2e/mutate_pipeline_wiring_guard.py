#!/usr/bin/env python3
"""Falsify the `[15p] 流水线配置决定执行链` section against the DEPLOYED fat jar, in the round it was written.

缺陷 #41 的原状是: z_lc_pipeline_config 的 order/enabled/stages 对运行时一个都不影响 (Pipeline 跑的
是按类名字典序排好的全部 Spring bean), 而配置页宣传"按 order 顺序执行" —— WEBHOOK / SCRIPT 连执行器都
没有, AFTER_* 连挂接点都没有。这一轮补的网有三层 (单测 PipelineStagesTest / PipelineWriteChainTest、
IT LcHttpContractTest、这里的 [15p]), 本脚本只管**部署件层**: 真正 shipped 的那个 jar 有没有把配置当回事。

为什么每支注入都要单独一支, 而不是"把校验整个摘掉看红多少":
一道闸红了另一道闸会替它挡, 一条检查绿着的原因可能根本不是它声称的那个。P4 就是这个形状 ——
摘掉"没有执行器"之后, `required_check` 那行不是被"没有执行器"拒掉的、而是被"缺少必填阶段"拒掉的,
于是 "未知阶段类型大小写不放过 -> 拒" **照样绿**, 只有它的文案半边红。这正是把每条拒绝写成
"拒 + 文案点名"两半的理由, 脚本把这个残留如实记成 P4 的预期红集合而不是替它解释。

  P1  执行面完全不查配置 (enabledConfigFor 恒 null) -> 3 红, 全是"配置里的顺序"那三条。
      注意 "停用后退回默认链" 与 "更新走 REQUIRED_CHECK 在前" 在 P1 下**是绿的**: 它们期望的正好就是
      默认链的输出。这两条不是漏网, 而是 P1/P2 的分工 —— 顺序轴由 P1 负责, 挂接点轴由 P2 负责。
  P2  按挂接点取配置退化成永远取 BEFORE_CREATE -> 恰好 1 红, 就是那条更新链的探针;
      它在单测层的同名载体 (beforeUpdateAndBeforeCreateAreDifferentChains) 也恰好 1 红, 两层同轴。
  P3  处理器抛错被 Chain.run 吞掉 -> 8 红。这是最坏的一种坏: 写入静默成功、界面全绿,
      "配置在跑"这件事的唯一外部证据 (错误消息点名的处理器) 没了, 所以红的不是 1 条而是整个证据面。
  P4  "阶段没有执行器" 摘掉 -> webhook/script 两对 + 大小写的文案半边 + 计数 = 6 (见上面的遮蔽)
  P5  "触发事件没有挂接点" 摘掉 -> AFTER_* 三对 + 计数 = 7
  P6  三道闸门不再必填 -> 1 对 + 计数 = 3
  P7  VALUE_VALIDATE 必须在 TYPE_CONVERT 之后 摘掉 -> 1 对 + 计数 = 3
  P8  阶段去重摘掉 -> 1 对 + 计数 = 3
  P9  order 必须是整数摘掉 -> 1 对 + 计数 = 3 (1.5 走 intValue() 塌成 1)
  P10 "非空 JSON 数组" 的 size==0 半边摘掉 -> 2 红: `[]` 被收下、随后被必填闸拒,
      所以 "空阶段链 -> 拒" 半边仍然绿。P10 是这一族里唯一能证明文案半边不是冗余的注入。
  P11 Controller 的 enabled 只认 0/1 摘掉 -> 2 红: 99 那次请求把流水线**停用**, 于是紧接着的顺序探针
      也跟着换消息。第二条红是因果必需的, 不是连带噪声 —— 它证明 99 那条检查真的有牙。
  P12 Service 写入口的 enabled 只认 0/1 摘掉 -> enabled=2 那对 + 计数 = 3 (P11 打开关, P12 打存库)
  P13 #42: 写入口不再拒"没人读的阶段参数" -> create 那对 + update 那对 + 两条计数 + "被拒的 update
      一次都没改到那一行" = 7。update 那三条里只有 config 这一条会真的落库 (另外两条闸还在),
      所以 suite 里那条 config 用的链序**照抄被接受的那份** —— 否则落库顺手改了执行顺序,
      顺序探针会一起红, 这一支就不再是它自己那句保证的证据 (同 P4 的遮蔽, 方向相反)。
  P14 #42: "config 不是对象"那一笔不记账 -> 那对 + 两条计数 = 4 (未知键那道闸照旧, 所以只有这一族红)

前置体检 (空参照集会打印"满分", 所以必须先 FATAL):
  * 每个预期红的名字都必须真的出现在基线那一轮 [15p] 的 PASS 清单里 —— 名字写错了不是"没红",
    而是"这条检查不存在"
  * 每支注入都要证明产物字节变了 (class 级指纹), 分母恒等于基线 (353), [15p] 恒为 69 条
  * 源文件按字节快照还原, 还原后重新构建、重启、再跑一遍整份
本脚本会重启 18090 那个 JVM, 所以跑的时候不能有别的注入进程或人在打它。
"""
import hashlib
import io
import os
import re
import socket
import subprocess
import sys
import time
import urllib.request
import zipfile
from datetime import datetime
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
# 与前端那几支共用同一把按仓库的锁: 它们改 .ts/.tsx, 这里改 .java, 看着不打架,
# 但同时改写源文件时 A 的字节还原会把 B 正在判定的那份换掉 —— 上一轮实测出过这种假红。
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

STAGES = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/config/PipelineStages.java"
SERVICE = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/config/PipelineConfigService.java"
PIPELINE = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/Pipeline.java"
CTRL = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/PipelineConfigController.java"
# 备份放在 ~/.cache 而不是 /tmp: 同一台机器上别的会话会扫 /tmp (实测连别人的日志都扫掉过),
# 备份在战役中途消失 = 还原失败, 那正是这一支最不能出的错。
BAK = Path.home() / ".cache/zlc42/deployed_bak/pipeline"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
UNIT_CLASS = "PipelineWriteChainTest"
#  scratch 一律写 ~/.cache: 这台机器上有多个会话会扫 /tmp, 日志在半路消失过。
UNIT_LOG = str(Path.home() / ".cache/zlc42/unit_mut/pipe41_unit.log")
SECTION = "15p"

GATES = ["REQUIRED_CHECK", "TYPE_CONVERT", "VALUE_VALIDATE"]
TWIN = "：文案点名要什么"


def pair(name):
    return [name, name + TWIN]


WEBHOOK = "阶段 WEBHOOK 没有执行器 -> 拒"
SCRIPT = "阶段 SCRIPT 没有执行器 -> 拒"
LOWER = "未知阶段类型大小写不放过 -> 拒"
AFTER_CREATE = "触发事件 AFTER_CREATE 没有挂接点 -> 拒"
AFTER_UPDATE = "触发事件 AFTER_UPDATE 没有挂接点 -> 拒"
AFTER_DELETE = "触发事件 AFTER_DELETE 没有挂接点 -> 拒"
NO_GATES = "摘掉三道闸门 -> 拒（等于让该实体的写入绕过校验）"
INVERTED = "值校验排在类型转换之前 -> 拒"
DUP_STAGE = "同一阶段重复配置 -> 拒"
EMPTY_CHAIN = "空阶段链 -> 拒"
NOT_ARRAY = "阶段链不是数组 -> 拒"
FRAC = "order 不是整数 -> 拒"
ENABLED2 = "enabled=2 -> 拒（这一行在 listByEvent 里永远查不到，等于存了条死数据）"

# 名字里没有条数 (01:2x 实测教训): 这条曾被写成 f"上面 {len(REJECTS) + 2} 次...", 于是 #42 加了
# 两条被拒提交就把它换成 "17 次", 下面 6 支注入的预期红集当场指向一条不存在的检查。
# suite 那边把条数挪进了失败详情, 这里跟着改名。
COUNT = "上面被拒的提交一行都没落库（闸在写入之前）"
# 同一批"该拒的没拒"在 suite 里有**两个**互不依赖的读数: 一个是 15 次提交一行都没落库,
# 另一个是"被拒的第二份没有把第一份顶掉"。实测(P4..P9/P12)证实: 摘掉任一道写入前的
# 配置校验闸, 这两条**一起**变红 —— 少写一条就是把真实的连带红当成"注入不干净"。
COUNT2 = "被拒的第二份没有把第一份顶掉"
TC_FIRST = "配置链里 TYPE_CONVERT 在前 -> 空串先撞类型转换"
ENVELOPE = "runtime 的业务级失败是 HTTP 200 + 信封 code 400（前端必须读信封）"
AFTER_TOGGLE = "停用后退回默认链（字典序 RequiredCheck 在前）：同一请求换一条消息"
REENABLE = "再启用又换回 [TypeConvert]：enabled 与 order 都是真的在起作用"
OTHER_ENTITY = "同一应用的另一个实体没配流水线，走的还是默认链（配置不串）"
VV_LIMIT = "三道闸门都在配置链里：值校验报出上限"
UPD_CHAIN = "两个写前挂接点各自生效：更新走 REQUIRED_CHECK 在前的那条链"
CREATE_STILL_TC = "而同一条空值在创建挂接点上仍是 TypeConvert 先报（两条链互不顶替）"
NO_99 = "enabled 只认 0/1，给个 99 不能当成开启"

# #42: 阶段参数 config。这两条在写入口拒, 与"没有执行器的阶段"是同一类谎 (收下不执行的东西)。
UNREAD_CREATE = "阶段参数没人读 -> 拒（填了不生效的配置比没有配置更坏）"
UNREAD_UPDATE = "阶段参数没人读 -> update 也拒"
NOT_OBJECT = "阶段参数不是对象 -> 拒"
UPD_UNTOUCHED = "上面 3 次被拒的 update 一次都没改到那一行（后面还要拿这一行验执行顺序）"

# suite 这一窗给"该拒的 update"补了(update 也拒)那一对探针之后, 创建闸失效的注入**波及面变大**了:
# 以前只有 create 路径的坏配置被挡住, 现在 update 路径也会把一条坏配置真写进行里,
# 于是后面那几条"按配置链执行"的检查读到的就是那条坏配置。这两条名字是为那批连带红准备的锚。
NO_EXEC_UPDATE = "没有执行器的阶段 -> update 也拒"
NO_GATES_UPDATE = "摘掉三道闸门 -> update 也拒"
LEGIT_PASS = "合法写入照常通过（这道闸没把正常路径一起按住）"

# ---- anchors (每个都必须**恰好出现一次**, 否则这一支 SKIPPED 而不是猜) --------------------------
P1_A = ('        if (configService == null || entity == null || appCode == null || triggerEvent == null) {\n'
        '            return null;\n'
        '        }')
P1_R = '        if (true) {\n            return null;\n        }'

P2_A = '        PipelineConfigEntity config = enabledConfigFor(appCode, entity, triggerEvent);'
P2_R = '        PipelineConfigEntity config = enabledConfigFor(appCode, entity, BEFORE_CREATE);'

P3_A = ('                } catch (RuntimeException ex) {\n'
        '                    throw new PipelineException(p.name(), ex);\n'
        '                }')
P3_R = ('                } catch (RuntimeException ex) {\n'
        '                    log.warn("mutant: chain swallowed [{}] {}", p.name(), ex.getMessage());\n'
        '                }')

P4_A = '            if (!PROCESSOR_BY_TYPE.containsKey(type)) {'
P4_R = '            if (false) {'

P5_A = '        if (!isSupportedTrigger(trigger)) {'
P5_R = '        if (false) {'

P6_A = ('        if (!missing.isEmpty()) {\n'
        '            throw new IllegalArgumentException("阶段链缺少必填阶段 " + join(missing)\n'
        '                    + " —— 摘掉它们等于让写入绕过必填校验/类型转换/值校验");\n'
        '        }')
P6_R = '        // mutant'

P7_A = '        if (convert > validate) {'
P7_R = '        if (false && convert > validate) {'

P8_A = ('        Set<String> seen = new LinkedHashSet<String>();\n'
        '        for (String type : types) {\n'
        '            if (!seen.add(type)) {\n'
        '                throw new IllegalArgumentException("阶段 [" + type + "] 重复配置, '
        '同一个阶段在一次写入里只会执行一次");\n'
        '            }\n'
        '        }')
P8_R = ('        Set<String> seen = new LinkedHashSet<String>();\n'
        '        for (String type : types) {\n'
        '            seen.add(type);\n'
        '        }')

P9_A = ('                if (!orderNode.isIntegralNumber()) {\n'
        '                    throw new IllegalArgumentException("阶段 [" + type + "] 的 order 必须是整数, 实际: "\n'
        '                            + shorten(orderNode.asText()));\n'
        '                }')
P9_R = '                // mutant'

P10_A = '        if (root == null || !root.isArray() || root.size() == 0) {'
P10_R = '        if (root == null || !root.isArray()) {'

P11_A = ('        if (enabled != 0 && enabled != 1) {\n'
         '            // enabled == 1 才开, 其余一律关: 传 2/-1 的请求会"成功"停用一条流水线,\n'
         '            // 而调用方以为自己在启用。开关语义只认 0/1, 别的值直接拒。\n'
         '            throw new IllegalArgumentException("enabled 只能是 1(启用) 或 0(停用), 实际: " + enabled);\n'
         '        }')
P11_R = '        // mutant'

P12_A = '        } else if (entity.getEnabled() != 0 && entity.getEnabled() != 1) {'
P12_R = '        } else if (false) {'

# P13/P14 (#42): 写入口那道"参数没人读就拒"的闸。摘法是死分支而不是删掉整段 ——
# unread 照算 (运行期那条 warn 还指着它), 只有"拒"这一下没了; 删整段会连累 resolve 的返回值。
P13_A = '        if (!unread.isEmpty()) {'
P13_R = '        if (false) {'
P14_A = ('                    unread.add("阶段 [" + type + "] 的 config 不是对象 (实际: "\n'
         '                            + shorten(configNode.asText()) + ")" + acceptedConfigHint(type));')
P14_R = '                    // mutant: 形状不对也不记账'

# tag -> (edits, 预期 [15p] 红, 预期单测层红或 None=不跑, 预期**别的节**的红)
#
# 第四项不是"放宽": 别的节红一条都不许有, 除了**明写在这里**的那几条。P3 挖的是 `Chain.run`
# 里那一处 `throw` —— 整条写前链的拒绝从此蒸发, 波及面天然是全 suite, 这四条红是**同一个缺陷
# 的第二批证据**（逐条读过的结论写在 P3 那一行下面）, 所以按名字钉住, 名字漂了照样 MISMATCH。
RUNS = [
    # P1 摘的是 `Pipeline.run` 开头那句"没有 configService/entity/appCode/trigger 就走默认链"的
    # 短路 —— 换件之后**任何**一次执行都不再查配置。单测层这一支原来预期 7 条红, #42 之后是 8 条:
    # 多出来那条 `unreadStageParamsWarnButDoNotBlockWrites` 断的是"库里那份带没人读参数的老配置
    # 跑起来要 warn 但仍写得进", 而 warn 是 resolve 出来之后才打的 —— P1 让 resolve 根本不被调用,
    # 那句 warn 就没了。它是**同一个缺陷的第二批证据**, 不是白名单。
    ("P1", [(PIPELINE, P1_A, P1_R)], [TC_FIRST, REENABLE, CREATE_STILL_TC],
     ["configuredSubsetRunsExactlyThoseStages", "configuredOrderIsTheExecutionOrder",
      "configIsLookedUpPerTriggerPointNotPerRow", "beforeUpdateAndBeforeCreateAreDifferentChains",
      "legacyConfigMissingMandatoryStagesFailsLoudInsteadOfFallingBack",
      "stageWithoutABeanInContainerFailsLoudInsteadOfBeingSkipped",
      "multipleEnabledConfigsPickTheNewestWithoutMerging",
      "unreadStageParamsWarnButDoNotBlockWrites"], []),
    ("P2", [(PIPELINE, P2_A, P2_R)], [UPD_CHAIN],
     ["beforeUpdateAndBeforeCreateAreDifferentChains"], []),
    # P3 的连带红（实测 + 逐条读源码）: 这四条都在断言"该被写前链挡下来的写入没写成"——
    #   显式清空必填列仍被拒绝 / 伪造的前像不会写库  —— runtime/update 走 preWrite
    #   preview 统计 total/validCount / 行级错误带回行号 —— import/preview 每行也走 preWrite
    # 吞掉 throw 之后这四条**必须**红, 不红才说明"链跑的是别的代码"。
    ("P3", [(PIPELINE, P3_A, P3_R)],
     [TC_FIRST, ENVELOPE, AFTER_TOGGLE, REENABLE, OTHER_ENTITY, VV_LIMIT, UPD_CHAIN, CREATE_STILL_TC],
     ["processorNamesAreWrappedIntoPipelineExceptionWithTheStageThatThrew"],
     ["显式清空必填列仍被拒绝", "伪造的前像不会写库",
      "preview 统计 total/validCount", "行级错误带回行号"]),
    # P4/P6 的连带红 09-26 整族重跑时才补齐（原来各少 9 条 / 8 条, 报的正是 MISMATCH 而不是"通过"）。
    # 机理是同一条: 这两支把"写入口那道闸"变成死分支, 于是**update 路径**这一窗新加的那一对探针
    # (NO_EXEC_UPDATE / NO_GATES_UPDATE) 从"被拒"变成"写进去了" —— 一行坏配置落地之后,
    # 后面所有"按配置链执行"的检查读到的就是它, 于是 TC_FIRST / REENABLE / UPD_CHAIN / VV_LIMIT /
    # CREATE_STILL_TC / UPD_UNTOUCHED 一起换读数。P4 还多两条: LEGIT_PASS(坏配置里引用的阶段在
    # 容器里没有 bean, 链按 #41 的口径** fail loud**, 所以一次合法写入被它顶掉) 与 ENVELOPE(同理,
    # 报出来的信封换了内容)。这一批红不是"注入不干净", 恰恰是"闸为什么必须在写入之前"的书证。
    ("P4", [(STAGES, P4_A, P4_R)],
     pair(WEBHOOK) + pair(SCRIPT) + [LOWER + TWIN, COUNT, COUNT2]
     + pair(NO_EXEC_UPDATE)
     + [TC_FIRST, REENABLE, UPD_CHAIN, CREATE_STILL_TC, VV_LIMIT, UPD_UNTOUCHED, LEGIT_PASS],
     None, []),
    ("P5", [(STAGES, P5_A, P5_R)],
     pair(AFTER_CREATE) + pair(AFTER_UPDATE) + pair(AFTER_DELETE) + [COUNT, COUNT2], None, []),
    # P6 的连带比 P4 少一条 LEGIT_PASS: 摘掉必填闸门之后**没有**跑不起来的阶段, 链照旧执行,
    # 一次合法写入仍然通过 —— 它少的是校验而不是多了个不存在的处理器。差别就在这儿。
    ("P6", [(STAGES, P6_A, P6_R)],
     pair(NO_GATES) + [COUNT, COUNT2] + pair(NO_GATES_UPDATE)
     + [TC_FIRST, REENABLE, CREATE_STILL_TC, VV_LIMIT, UPD_UNTOUCHED, ENVELOPE], None, []),
    ("P7", [(STAGES, P7_A, P7_R)], pair(INVERTED) + [COUNT, COUNT2], None, []),
    ("P8", [(STAGES, P8_A, P8_R)], pair(DUP_STAGE) + [COUNT, COUNT2], None, []),
    ("P9", [(STAGES, P9_A, P9_R)], pair(FRAC) + [COUNT, COUNT2], None, []),
    # P10 只有"文案"那一半红: 摘掉 `root.size() == 0` 之后, 空链仍被必填那道闸拒在同一个 400 上
    # ——「拒」这一半是**过定**的, 两条计数检查也照旧绿(什么都没落库)。
    # 这正好反过来证明"文案点名要什么"那 13 条孪生断言不是凑数的。
    ("P10", [(STAGES, P10_A, P10_R)], [EMPTY_CHAIN + TWIN], None, []),
    ("P11", [(CTRL, P11_A, P11_R)], [NO_99, TC_FIRST], None, []),
    ("P12", [(SERVICE, P12_A, P12_R)], pair(ENABLED2) + [COUNT, COUNT2], None, []),
    # P13/P14 打的是 #42 那道"阶段参数没人读"的闸。单测层填 None 不是"忘了填", 而是这一支的
    # UNIT_CLASS 看不见它: 那一层跑的是 PipelineWriteChainTest, 而运行期走的是 resolve (照旧容忍),
    # 真正会红的 5 条住在 PipelineStagesTest / PipelineConfigServiceTest —— 那边由
    # _e2e/mutate_pipeline_config_guard.py 的 U1/U3 负责, 两层各管各的载体。
    #
    # P13 的预期红集里有 NOT_OBJECT 那一对, 是 09-26 实测补上的 (我原先只写了 UNREAD 那一族四条
    # + 三条计数, 于是这一支 MISMATCH)。理由不是"顺手多红两条": parseTypes 把**两种**坏形状
    # ("值是对象但键没人读" 和 "压根不是对象") 记进**同一个** `unread` 清单, P13 摘的是
    # `if (!unread.isEmpty())` 这唯一的消费点 —— 清单照旧被填, 只是没人再看它, 所以两条一起漏。
    # 这恰好是那一处合并刻意的代价被量出来的样子: 少一个分支, 两种谎一起放出去。
    ("P13", [(STAGES, P13_A, P13_R)],
     pair(UNREAD_CREATE) + pair(UNREAD_UPDATE) + pair(NOT_OBJECT)
     + [COUNT, COUNT2, UPD_UNTOUCHED], None, []),
    ("P14", [(STAGES, P14_A, P14_R)], pair(NOT_OBJECT) + [COUNT, COUNT2], None, []),
]

FILES = sorted({STAGES, SERVICE, PIPELINE, CTRL}, key=lambda p: str(p))

# `--only P3,P10` 只决定**跑哪几支**, 判据一条不减: 基线照跑、每支照样重新 build + 重启 JVM +
# 逐名对红集 + 分母不变。改完预期红集后复验两支约 4 分钟, 整族 12 支约 25 分钟 —— 慢不是放宽
# 理由, 但也没必要为此把同一支注入再跑十遍。不带 --only 就是全跑。
ONLY = {x.strip() for x in (sys.argv[2].split(",") if sys.argv[1:2] == ["--only"] else []) if x.strip()}
if sys.argv[1:2] == ["--only"] and not ONLY:
    raise SystemExit("--only 后面是空的: 一支都不跑不等于「全跑」, 那是「没有判定」")


def sh(cmd, cwd=ROOT, timeout=1800):
    return subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout)


def snapshot_sources():
    """给工作树取一份**这一次运行专属**的备份, 并把这份备份当作唯一要还原回去的状态。

    原来这里是 `BAK = /tmp/pipe41_deployed_bak` + `if not b.exists(): copyfile` +
    `originals = b.read_bytes()`: 备份目录跨运行复用、且永不过期, 于是每次开局都把**更早一轮**
    存的四份源码盖回工作树。实测后果不是假红, 是把 #42 在 PipelineStages.java / Pipeline.java
    里未提交的改动整段抹掉 (09-26 01:47:56, 盘上四份与 HEAD 逐字节相同), 而日志只留下一句
    "restored sources: clean"。改成每次运行一个独占目录: 串味在没有路径可走这一步就没了。
    真被 SIGKILL 打断而留下注入残局时, 下一次会在基线那一句红给我看, 而不是悄悄"修好"。
    """
    run_dir = BAK / f"run-{os.getpid()}-{datetime.now().strftime('%m%d-%H%M%S')}"
    run_dir.mkdir(parents=True, exist_ok=False)
    originals = {}
    for f in FILES:
        if not f.exists():
            raise RuntimeError(f"source file missing, cannot even baseline: {f}")
        disk = f.read_bytes()
        b = run_dir / (str(f.relative_to(ROOT)).replace("/", "_") + ".orig")
        b.write_bytes(disk)
        if b.read_bytes() != disk:
            raise RuntimeError(f"backup of {f.name} did not stick")
        originals[f] = disk
    print(f"sources snapshotted -> {run_dir}")
    return originals


def artifact_fingerprint():
    parts = []
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
                parts += [f"{lib}!{e}:{hashlib.md5(inner.read(e)).hexdigest()}" for e in classes]
    if not parts:
        raise RuntimeError("empty artifact fingerprint")
    return "|".join(parts)


def fp_id(fingerprint):
    return hashlib.md5(fingerprint.encode()).hexdigest()[:12]


def lc_pids():
    out = subprocess.run(["pgrep", "-f", "z-lc-admin-1.0.0-SNAPSHOT.jar"],
                         capture_output=True, text=True).stdout.split()
    return [int(x) for x in out]


def wait_health(timeout=420):
    # 等待拉长不是放宽判据 —— 判据是"这一轮注入有没有被抓到"; 本机同一时间还有别人的 mvn/javac,
    # 上一轮 150s 就判过"起不来", 实际是 8 分钟后起来了。真等不到 420s 才叫没起来。
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


def ensure_fresh_jvm():
    """重启前先证明"我读到的 UP 就是这个 jar 给的": 起来一个新 JVM, 用新代码独有的行为当指纹。

    只看 /api/lc/health 不够 —— 上一轮旧进程吃了 SIGTERM 却没死, 健康检查秒回 200, 差点把
    "旧 jar 的 342/342" 记成这一轮注入的读数。所以这里以 P11 的 enabled=2 拒绝为指纹:
    那条消息只在打了本轮补丁的字节码里存在。
    """
    pids = lc_pids()
    if len(pids) > 1:
        raise RuntimeError(f"refusing to guess which JVM to stop: {pids}")
    if pids:
        subprocess.run(["kill", "-9", str(pids[0])])
        if not _wait_port_free(120):
            raise RuntimeError("old JVM still answering after kill -9")
    log = open("/tmp/lc_deployed_mut_boot.log", "ab")
    # start_new_session：不起新会话的话这个 jar 属于本量具的进程组，量具一退出它跟着收 SIGTERM（缺陷 #74）
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log, start_new_session=True)
    if not wait_health():
        # 失败路径上更不能挂: 本机 lsof 在 load 高的时候直接不返回（这轮已经踩过四次）,
        # 而这里挂住 = 整场战役卡死在"报不出为什么起不来"。用 connect 探一下就够了。
        probe = "18090 没在监听"
        try:
            with socket.create_connection(("127.0.0.1", 18090), timeout=3):
                probe = "127.0.0.1:18090 能连上但 health 不 UP（大概是别人的进程在答）"
        except OSError as ex:
            probe = f"127.0.0.1:18090 连不上: {ex}"
        tail = subprocess.run(["tail", "-15", "/tmp/lc_deployed_mut_boot.log"],
                              capture_output=True, text=True).stdout.strip()
        raise RuntimeError("server did not come up; see /tmp/lc_deployed_mut_boot.log"
                           f"\n   {probe}"
                           f"\n   pids alive: {lc_pids()}\n   boot log tail:\n{tail}")


def _wait_port_free(timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not lc_pids():
            try:
                urllib.request.urlopen(HEALTH, timeout=2).read()
            except Exception:  # noqa: BLE001
                return True
        time.sleep(2)
    return False


def build():
    p = sh(["mvn", "-o", "-B", "install", "-DskipTests", "-pl", "z-lc-core,z-lc-web,z-lc-admin", "-am"])
    if "BUILD SUCCESS" not in (p.stdout + p.stderr):
        raise RuntimeError("jar build failed, tail of log:\n"
                           + "\n".join((p.stdout + p.stderr).splitlines()[-25:]))


def run_e2e(label):
    """((passed, total), all_fail_names, [15p] fail names, [15p] pass names, 全 suite pass 名).

    归属取自 harness 自己打的 `[NNx]` 横幅, 不做关键字匹配 —— 别的小节红不会被洗进这一节。
    全 suite 的 PASS 名是**别的节**那份预期红集的参照系: 没有它, P3 的四条连带红写错了名字
    也没人发现（"预期一条不存在的红"和"注入没抓到"在输出上长得一模一样）。
    """
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    e2e_dir = Path.home() / ".cache/zlc42/deploy_mut"
    e2e_dir.mkdir(parents=True, exist_ok=True)
    (e2e_dir / f"e2e_pipe41_{label}.log").write_text(p.stdout + p.stderr)
    summary = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    section, fails, mine, passed_in_section, all_pass = None, [], [], [], []
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
        elif line.startswith("  PASS  "):
            name = line[8:].split("   <<")[0]
            all_pass.append(name)
            if section == SECTION:
                passed_in_section.append(name)
    parsed = (int(summary.group(1)), int(summary.group(2))) if summary else (None, None)
    if not summary:
        raise RuntimeError(f"no E2E RESULT line in {label} run; tail:\n"
                           + "\n".join((p.stdout + p.stderr).splitlines()[-20:]))
    return parsed, fails, mine, passed_in_section, all_pass


def run_unit():
    """(total, red-method-names) for PipelineWriteChainTest; 跑不起来就抛, 不返回"没有红"."""
    p = sh(["mvn", "-o", "-B", "test", "-pl", "z-lc-core", f"-Dtest={UNIT_CLASS}"])
    out = p.stdout + p.stderr
    Path(UNIT_LOG).parent.mkdir(parents=True, exist_ok=True)
    Path(UNIT_LOG).write_text(out)
    totals = [int(x) for x in re.findall(
        rf"Tests run: (\d+), Failures: \d+, Errors: \d+[^\n]*in [\w.]*{UNIT_CLASS}", out)]
    if not totals:
        raise RuntimeError(f"surefire printed no summary for {UNIT_CLASS}; tail:\n"
                           + "\n".join(out.splitlines()[-20:]))
    red = sorted({m for line in out.splitlines() if "<<<" in line
                  for m in re.findall(rf"{UNIT_CLASS}\.([A-Za-z0-9_]+)", line)})
    return sum(totals), red


def check_expectations(baseline_pass_names, baseline_all_pass):
    """预期红的名字必须是基线里真的绿过的检查名; 名字打错不等于"注入没抓到"."""
    if not baseline_pass_names:
        raise RuntimeError(f"baseline [15p] produced no PASS names at all; an empty reference set "
                           "would make every expectation trivially satisfied")
    known = set(baseline_pass_names)
    unknown = sorted({n for _, _, expect, _, _ in RUNS for n in expect if n not in known})
    if unknown:
        raise RuntimeError("expected-red names that are NOT in the baseline [15p] list (typo or "
                           "renamed check -> the guard would silently assert nothing):\n  "
                           + "\n  ".join(unknown))
    all_known = set(baseline_all_pass)
    if not all_known:
        raise RuntimeError("baseline produced no PASS names outside [15p] either; 别的节那份预期红集"
                           "就没有参照系, 写错名字也不会红")
    stray = sorted({n for _, _, _, _, other in RUNS for n in other if n not in all_known})
    if stray:
        raise RuntimeError("expected collateral-red names that are NOT a green check anywhere in the "
                           "baseline suite (typo or renamed check):\n  " + "\n  ".join(stray))
    tags = {tag for tag, _, _, _, _ in RUNS}
    if ONLY - tags:
        # 打错一支的标签会安静地少跑一支, 而输出看着像"全跑过了"
        raise RuntimeError(f"--only names that are not injections in RUNS: {sorted(ONLY - tags)} "
                           f"(available: {sorted(tags)})")
    if ONLY:
        print(f"  !! 本轮只跑 {len(ONLY)}/{len(RUNS)} 支（--only {','.join(sorted(ONLY))}）: "
              "其余各支的判定**没有**在这一轮做过, 别把这份日志当整族绿")
    print(f"  preflight: {len(known)} [15p] check names, "
          f"{sum(len(e) for _, _, e, _, _ in RUNS)} expected reds + "
          f"{sum(len(o) for _, _, _, _, o in RUNS)} expected collateral reds across {len(RUNS)} "
          "injections, every name resolves to a real green check")


def main():
    acquire_lock("mutate_pipeline_wiring_guard.py")
    try:
        return run_all()
    finally:
        release_lock()


def run_all():
    originals = snapshot_sources()

    def restore_all():
        for f in FILES:
            f.write_bytes(originals[f])

    restore_all()
    bad = 0
    try:
        build()
        ensure_fresh_jvm()
        base_fp = artifact_fingerprint()
        (ok_n, total), base_fails, _, base_pass, base_all_pass = run_e2e("baseline")
        print(f"=== baseline deployed: {ok_n}/{total}  [15p] checks={len(base_pass)}  "
              f"artifact fp={fp_id(base_fp)}")
        check_expectations(base_pass, base_all_pass)
        if ok_n != total:
            print(f"BASELINE NOT GREEN on the deployed jar, aborting. failures={base_fails}")
            return 1
        unit_total, unit_red = run_unit()
        print(f"  unit baseline {UNIT_CLASS}: {unit_total} tests, red={unit_red}")
        if unit_red:
            print("UNIT BASELINE NOT GREEN, aborting")
            return 1

        only = ONLY
        for tag, edits, expect, unit_expect, other_expect in RUNS:
            if only and tag not in only:
                print(f"\n[{tag}] 本轮未选（--only {','.join(sorted(only))}），不记账也不判")
                continue
            applied = True
            for path, anchor, repl in edits:
                text = path.read_text()
                if text.count(anchor) != 1:
                    print(f"\n[{tag}] SKIPPED: anchor occurs {text.count(anchor)}x in {path.name} "
                          "-> the mutant would not be the one described above")
                    bad += 1
                    applied = False
                    break
                path.write_text(text.replace(anchor, repl, 1))
            if not applied:
                restore_all()
                continue
            build()
            fp = artifact_fingerprint()
            changed = fp != base_fp
            ensure_fresh_jvm()
            (passed, this_total), fails, mine, _, _ = run_e2e(f"mut{tag}")
            outside = sorted({f for f in fails if f not in mine})
            print(f"\n[{tag}] expect red on: {expect}")
            print(f"  artifact fp = {fp_id(fp)}  changed={changed}")
            print(f"  e2e = {passed}/{this_total}  (baseline denominator {total}, "
                  f"[15p] count {len(mine)} red)")
            print(f"  [15p] failures ({len(mine)}): {mine or '(none)'}")
            print(f"  failures outside [15p] ({len(outside)}): {outside or '(none)'}  "
                  f"expected: {sorted(other_expect) or '(none)'}")
            if not changed:
                print("  !! artifact did not change -> this run proves nothing")
                bad += 1
            if this_total != total:
                print(f"  !! 分母变了（{total} → {this_total}）：这一轮跑不完，「没红」不成立")
                bad += 1
            if sorted(mine) != sorted(expect):
                print(f"  !! MISMATCH: wanted exactly {sorted(expect)}, got {sorted(mine)}")
                bad += 1
            if outside != sorted(other_expect):
                print(f"  !! 别的节的红对不上（注入不干净，或连带红没被抓到）: "
                      f"wanted {sorted(other_expect)}, got {outside}")
                bad += 1
            if unit_expect is not None:
                u_total, u_red = run_unit()
                print(f"  unit {UNIT_CLASS} = {u_total} tests, red={u_red}")
                if u_total != unit_total:
                    print(f"  !! 单测分母变了（{unit_total} → {u_total}），「红了几条」无从谈起")
                    bad += 1
                if u_red != sorted(unit_expect):
                    print(f"  !! UNIT MISMATCH: wanted exactly {sorted(unit_expect)}, got {u_red}")
                    bad += 1
            restore_all()

        print("\n=== restore: rebuild pristine, restart, re-run ===")
        build()
        fp = artifact_fingerprint()
        ensure_fresh_jvm()
        (passed, last_total), fails, last_mine, last_pass, _ = run_e2e("restored")
        u_total, u_red = run_unit()
        print(f"  artifact back to baseline bytes: {fp == base_fp}")
        print(f"  e2e = {passed}/{last_total}  (baseline {total})  failures={fails or '(none)'}")
        print(f"  restored [15p] count = {len(last_pass)} (baseline {len(base_pass)})")
        print(f"  unit = {u_total} tests, red={u_red or '(none)'}")
        if (fp != base_fp or passed != total or last_total != total or u_red
                or u_total != unit_total or len(last_pass) != len(base_pass) or last_mine):
            bad += 1
        for f in FILES:
            if f.read_bytes() != originals[f]:
                print(f"  !! NOT RESTORED: {f}")
                bad += 1
    finally:
        restore_all()
        leaked = [str(f) for f in FILES if f.read_bytes() != originals[f]]
        print("\nrestored sources: " + ("clean" if not leaked else "NO -> " + ", ".join(leaked)))
        print(f"serving pid(s) now: {lc_pids()}  health={wait_health(10)}")
    verdict = "deployed-layer falsification done" if bad == 0 else f"{bad} problem(s)"
    if ONLY:
        # 跑了一半却打印"done"是最坏的一种谎: 让人以为整族 12 支都过了
        verdict += f" (PARTIAL: 只跑了 --only {','.join(sorted(ONLY))}, " \
                   f"{len(RUNS) - len(ONLY)} 支本轮没有判定)"
    print("RESULT: " + verdict)
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 裸 sys.exit(main()) 会让"只是 import 看一下"的调用方直接开跑战役
    sys.exit(main())
