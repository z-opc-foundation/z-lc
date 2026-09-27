#!/usr/bin/env python3
"""#61 流程绑定的**浏览器层**注入自证（`browser-e2e.mjs` 第 11w 段）。

跑法（在 z-lc-admin-ui 下；后端 18090 要在跑，5274 要让出来给这一支自己起的 preview）：

    python3 e2e/mutate_workflow_browser_guard.py

为什么单开一支：这一族前面已经有四层各自的守卫 —— 契约层（`WorkflowTriggerContractTest`）、
vitest（`WorkflowsPage.test.tsx`）、java 侧对"发出去的那个 jar"（`_e2e/mutate_workflow_deployed_guard.py`
W1–W6）、以及接口层量具（`_e2e/e2e_api_test.py` 第 [15w] 节）。vitest 能证明"词表长成这样时组件画成
这样"，证明不了这份词表是真服务端给的、也证明不了界面上点出去的那一句真的到了 z-wf；deployed 那层管
的是服务端，界面话术它看不见。这一支管中间那段：**事件清单 → 下拉/红标 → 写出去的草稿 → 抽屉里的账**。

十八支注入，每支只摘一句保证，红集合互不相同：

  W1  下拉自己抄一份清单（#61 的原形状）→ 「个数 = 词表个数」+「兑现不了的进不了下拉」+「说的是人话」
  W2  被拒清单整块不渲染            → 「摆在窗里」+「理由由引擎给出」+「点名事件码」
  W3  摘掉「时机未校对」那一支        → 只红「词表形状读坏时标的是未校对而不是引擎不兑现」
  W4  抽屉读失败改画表格             → 只红「报的是没有读到，而不是还没有发起记录」
  W5  = W4 + 关抽屉不清空旧行         → 上面那条 + 「读失败那一屏也不留下三行账的假象」
  W6  写入口的原因换成固定「保存失败」 → 「原因原样摆出来」+「带空白的重复也算重复」（两条读同一句 toast）
  W7  「流程实例」那一格画记录号       → 「实例号就是 z-wf 回的那一格」+「FAILED 那一格必须是空的」
                                       +「502 带号的那一个不许进账」（这三条读的都是那一格，摘掉 render 三条一起塌）
  W8  摘掉「新建绑定」的 canDraft 闸   → 只红「词表读失败时新建绑定收住」
  W9  成功那一下不吭声               → 只红「保存成功界面说已保存」（另一条"没有失败文案"仍绿：空 toast 也会绿）
  W10 流程 KEY 那一格画成空白         → 「表里那一行同时给出三格」+「词表读失败时那一行照常给出 KEY」
  W11 兑现判断写反（!includes）       → 「那一行不带引擎不兑现的红标」+ W3 那条（同一格两种坏法，红集不同）
  W12 结果那一格画「—」              → 抽屉里读 status 字样的**六**条账（一行 STARTED / 多出来是 FAILED /
                                       FAILED 不覆盖 STARTED / 三行两成一败 / 502 那一行 / (7b) 拒绝那一行 ——
                                       后两条的合取里都写着"这一行得自称 FAILED"）
  W13 「记录」那一列画事件码          → 「那一行记着是哪条记录的账」+「三行各自指着三条不同的记录」
                                       + (7b) 那三条（它们靠「记录」那一格认领是哪一行的账 —— 认不出行
                                       就没有"哪一行的理由"，这不是替身红：断言的主语本就是"那一格指着的那一行"）
  W14 解绑之后不刷新                 → 只红「表里那一行真的下去了」（「库里也读不到」仍绿：它读的是接口）
  W15 词表读失败的横幅整块摘掉        → 只红「页面报的是触发时机词表没有读到并给重试的入口」
  W16 「时机未校对」挂到了已兑现那一行  → 只红「把词表换回来：同一行立刻不再报未校对」（W3 的反方向：一支管失败态、一支管恢复态）
  W17 解绑成功那一下不吭声            → 只红「界面说已删除」
  W18 「为什么」那一列整列画空        → 「多出来那一行是 FAILED 且说得出为什么」+「引擎那句拒绝理由原样落在这一格」
                                       +「502 那一行的为什么说的是这一单没成」

第一轮实测（03:1x，见 `~/.cache/zlc61/browser_guard/run1.out`）红出来的三条都是**我这一支自己的**问题，
没有一条是产品坏了，账按实测改成现在这样：
  1) W13 预期 2 条却红 0 条 —— 那两条当时写的是"整行 blob 里 includes(String(recordId))"，而时间那一格
     里全是数字（记录号是 1、2、3），一蹭就中 ⇒ 检查是空的。现在按格读第一格（`wfFiresCells`）。
  2) W12 红了一条没预期的「FAILED 那一行不许留下流程实例号」—— 当时靠行里的 'FAILED' 字样认领那一行，
     摘掉字样就"找不到那一行"而红：那是替身红，指不到"FAILED 行长出了实例号"这件事上。现在按
     `data-testid="fire-status-FAILED"` 认行、再按格读第三格 ⇒ W12 不再红它，而它归到 W7 名下（那一格
     画记录号才是它的猎物）。服务端写没写 instanceId 那一半仍在 deployed 层，浏览器层只管画没画。
  3) W16 第一版（`!== 'ready'` → `!== 'loading'`）红 0 条：ready 态下上面 `implemented.includes(event)`
     先命中，这一支结构上走不到 —— 等价变异。换成"未校对挂到了已兑现那一行"才打得到恢复态那条。
顺带把桩里 `mode` 的两个分支接进判定（(7b)）：此前没有任何一处翻过旗，于是"FAILED 那一格的实例号是空的"
只在"桩不可达"这一种成因下测过，而那种成因结构上收不到 body —— 那句断言里的 `ghost-` 是一个没有猎物的
字样。502 带一个成功样的 body 才是它的猎物，界面层这一条钉"那一个号没画进账"。

第二轮实测（04:4x，`~/.cache/zlc61/browser_guard/run2.out`）：18 支里 **17 支逐字对上**，
剩下的两处都不是产品的账，一处改预期、一处修量具：
  4) W12 多红「引擎那句拒绝理由原样落在「为什么」那一格」—— **预期漏记，不是替身红**：那条断言的合取里
     确实有 `wfRow4[1] === 'FAILED'`（browser-e2e.mjs:3137），而 W12 摘的就是那一格的字样；读数里第三格
     「为什么」是完好的。把它记进 W12 名下（同一条断言同时挂在 W12/W13/W18 三支名下 = 它读了三格，
     而三支的**整集**各不相同 6/5/3，判据分得开）。没有摘掉那个合取：它是"引擎答 200 而 success=false
     的那一行也得自称 FAILED"唯一的落点。
  5) 恢复轮不是"还原没还原对"，是**环境把下一轮拦停**：那一轮分母只有 2、红的是「表格渲染」。
     机理量出来了 —— `seedTestData` 的 appCode 用 `Date.now().toString().slice(-6)`（= epoch ms mod 1e6，
     **每 1000s 回一次**），物理表名又只取后 4 位（**每 100s 回一次**），而 seed 建的应用**一个都没删过**
     （04:4x 实测盘上 92 个 uitest/uiprov）⇒ 本轮基线轮的 `uitest239924` 与 1000s 后恢复轮的
     `uitest429924` 尾巴同为 `9924`，抢同一张 `ui_task9924`，服务端拒建实体（那句原话点着占用方），
     界面上塌下来的是 15s 之后的超时红。跑满 20 轮（>3000s）必然横跨这个回绕 ⇒ **这一支每跑一次都在掷骰子**。
     修法在量具侧：appCode 尾随 4 位随机数、表名取后 8 位（不再与旧残留同形），且 seed 实体建不成时
     **当场抛错具名拒跑**（"环境残留不是产品缺陷"），不许让它穿着「表格渲染」的红进台账。
     还原本身是干净的：`WorkflowsPage.tsx` 盘上 md5 `87b30e33161f27089623d260151e12fd` == `git show HEAD:`
     同一个值，恢复轮产物名也回到了基线的 `index-ruBdvAdn.js`。

W4/W5 这一对是**成对**设计的，不是两支重复：`setFires([])` 那一句在只摘 W4 时永远红不了 ——
错误分支整块换掉了表格，"旧行还亮着"这个形状在结构上不可达；它的猎物只在"错误分支也没盖住表格"时
才露出来。于是判据落在**差集**上：W5 的红集比 W4 恰好多「三行账」那一条 ⇒ 那一条确实钉在
`setFires([])` 上，而 W4 单独证明那一条不是它的替身。少了 W4，W5 的两条红说不清是谁的功劳。

⚠ 有一批检查这一支**不**注入，按未覆盖记账（写成"全都能抓到"是谎）。三类理由，逐条列在
`NOT_COVERED` 里、由 validate() 对着源码核名字：
  (a) 夹具与桥的自证（页面渲染出来了、词表接口读得到、桩收到了第一句、按钮在正常态是 enabled）——
      它们红了说明环境坏了而不是守卫坏了，设计上就不该有注入能红它们。
  (b) 会让整节级联的形状（摘掉写调用、把草稿的 appCode/时机/autoSubmit/默认实体改错）——写入口现在
      **拒**这些形态，于是一次注入连着红掉"保存答 200""表里那一行""桥"以及后面三十多条并带走整节，
      那是造一个新缺陷而不是摘一句守卫。它们的牙在 vitest（`WorkflowsPage.test.tsx` 第 267—285 行
      就是这份草稿的字节）与 deployed 层。
  (c) 数据在服务端那一侧（库里读回什么、fire 行里写没写 instanceId/detail、删没删掉）—— 改
      `WorkflowsPage.tsx` 动不到，牙在 `_e2e/mutate_workflow_deployed_guard.py` 的 W 系列。

判据同 `mutate_provision_browser_guard.py`：`FAIL <名字>   << <读数>` 必须先剥掉读数再比集合。
另加这一支自己的三条：
  1) 每轮 PASS+FAIL 总数 == 基线那轮（少了 = 某一节中途没跑完，"没红"是跑不完不是通过）；
  2) 每轮的产物指纹（vite 的 `index-<hash>.js` 文件名 + 字节 md5）必须与基线**不同**，恢复轮必须
     **回到基线那个名字** —— 内容哈希同名 = 注入根本没进 bundle，那一轮的"全绿"就是基线的回声；
  3) 11w 的检查名由**源码扫**出来（不抄清单）：整节之内不许重名，且每一条要么被某支注入覆盖、
     要么出现在记账名单里 —— 新增一条检查而两边都没登记，validate() 当场拒绝开跑。
"""

import hashlib
import os
import re
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

import _mutlock

UI = Path(__file__).resolve().parents[1]
PAGE = UI / "src/views/admin/WorkflowsPage.tsx"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
LOG_DIR = Path.home() / ".cache/zlc61/browser_guard"
LOG_DIR.mkdir(parents=True, exist_ok=True)

BUILD_TIMEOUT = 900
# 上限而不是预期：一轮跑的是完整的 browser-e2e（分母由基线轮现量并钉住，低负载 ~4min，机器忙时翻倍）。
# 超时 == 无结论（收线并说明），不 == 全绿。
ROUND_TIMEOUT = 2400
FAIL_DETAIL_SEP = "   << "

SECTION_HEAD = "/* ---- 11w."
SECTION_TAIL = "/* ---- 9. 全局异常"
CHECK_NAME = re.compile(r"^\s*(?:check|wfCheck)\(\s*'([^']+)'", flags=re.M)

# 整节 catch 的兜底标题：它红了就说明这一节中途抛错，后面的检查一条没跑 —— 归因到此为止。
T_SECTION_CRASH = '流程绑定的发起账（#61 浏览器层）'


def section_text() -> str:
    src = E2E.read_text(encoding="utf-8")
    at = src.find(SECTION_HEAD)
    tail = src.find(SECTION_TAIL)
    if at < 0 or tail <= at:
        raise SystemExit(f"!! 找不到 11w 那一段的边界（head={at} tail={tail}）—— 段名改过？分母就取不出来")
    return src[at:tail]


def section_names() -> list[str]:
    return CHECK_NAME.findall(section_text())


def _name(fragment: str) -> str:
    """在 11w 那一段里按子串找唯一的名字；不唯一或找不到都拒绝继续（手敲会臆造）。"""
    hits = [n for n in section_names() if fragment in n]
    if len(hits) != 1:
        raise SystemExit(f"!! 检查名子串 {fragment!r} 命中 {len(hits)} 条（应为 1）: {hits}")
    return hits[0]


# ---- anchors: each must occur exactly once in the pristine file; the script refuses to guess ----
A1 = "  const triggerSelectOptions = implemented.map((event) => ({ value: event, label: triggerLabel(event) }));\n"
R1 = ("  const triggerSelectOptions = ['AFTER_CREATE', 'AFTER_UPDATE', 'AFTER_DELETE']"
      ".map((event) => ({ value: event, label: triggerLabel(event) }));\n")

A2 = "                {triggers.vocabulary.rejected.map((item) => (\n"
R2 = "                {triggers.vocabulary.rejected.slice(0, 0).map((item) => (\n"

A3 = ('        // 词表读失败时不许把每一行都标成"引擎不兑现" —— 那是把量具的故障说成数据的问题。\n'
      "        if (triggers.status !== 'ready') {\n"
      "          return <Tag data-testid={`workflow-trigger-${row.id ?? event}`}>{triggerLabel(event)} 时机未校对</Tag>;\n"
      "        }\n")
R3 = "        // (注入：摘掉「未校对」这一支，于是「词表读失败」与「引擎不兑现」混成一格)\n"

A4 = ("      {status === 'error' ? (\n"
      '        <div data-testid="fire-load-error">发起记录没有读到：{reason}</div>\n'
      "      ) : (\n")
# `fires.length < 0` 是恒假但类型合法的写法：换成 `false &&` 会被 tsc 判"条件恒假"，
# 那时 build 红指不回守卫（这一支的每一轮都要真构建）。
# ⚠ 09-27 10:4x 跟着 #65 改过一次：抽屉的数据从 `fires: WorkflowFireEntity[]` 换成了
# 分页信封 `ledger: FireWindow | null`，旧式子里那个 `fires` 已经不在作用域里 ——
# 留着它，W4/W5 这两支会红在 tsc 上而不是红在那句守卫上（validate() 抓不到，只有 build 抓得到）。
R4 = ("      {status === 'error' && (ledger?.total ?? 0) < 0 ? (\n"
      '        <div data-testid="fire-load-error">发起记录没有读到：{reason}</div>\n'
      "      ) : (\n")

A5 = ("    if (!binding) {\n"
      "      setStatus('idle');\n"
      "      setLedger(null);\n"
      "      setPage(1);\n"
      "      return;\n"
      "    }\n")
R5 = ("    if (!binding) {\n"
      "      setStatus('idle');\n"
      "      setPage(1);\n"
      "      return;\n"
      "    }\n")
# ⚠ 09-27 11:1x/12:1x 两次实测：(A5,R5) 与 (A19,R19) **各自单独**挂到 W4 上都抓不到
# '读失败那一屏也不留下"三行账"的假象' —— 那条性质有两个互不相干的守点（关抽屉那句 + catch 那句），
# 摘掉任意一个，另一个仍然把旧账清干净，界面上没有可观察差别 = 等价变异。
# 于是 W5 一次摘两个（见 W5 那段注释）。这一支不是"守卫没用"，是
# **单站点摘除在这条性质上结构上不可观察**；要把它拆回一支一站点，得加一条
# "重新打开的那 1.2s 里旧行不许还亮着"的采样（loading 窗口内读数），本窗没加，按覆盖缺口记账。

# `err` 必须仍然被读一次：`noUnusedLocals` 会把没用的 catch 变量判成编译错，那时 build 红指不回守卫。
A6 = "      message.error(err instanceof Error && err.message ? err.message : '保存失败');\n"
R6 = "      message.error(err instanceof Error ? '保存失败' : '保存失败');\n"

A7 = ("    { title: '流程实例', dataIndex: 'instanceId', width: 150, "
      "render: (v: string | null | undefined) => v || '—' },\n")
R7 = ("    { title: '流程实例', dataIndex: 'instanceId', width: 150, "
      "render: (_v: string | null | undefined, row) => String(row.recordId ?? '—') },\n")

A8 = "            disabled={!appCode || !canDraft}\n"
R8 = "            disabled={!appCode}\n"

A9 = "      message.success('已保存');\n"
R9 = ""

A10 = "      render: (value: string) => <Text code>{value}</Text>,\n"
R10 = "      render: () => <Text code>—</Text>,\n"

A11 = "        if (implemented.includes(event)) {\n"
R11 = "        if (!implemented.includes(event)) {\n"

A12 = ("      render: (value: string) => <Tag color={value === 'STARTED' ? 'green' : 'red'} "
       "data-testid={`fire-status-${value}`}>{value}</Tag>,\n")
R12 = ("      render: (value: string) => <Tag data-testid={`fire-status-${value}`}>—</Tag>,\n")

A13 = "{ title: '记录', dataIndex: 'recordId', width: 110 },\n"
R13 = "{ title: '记录', dataIndex: 'triggerEvent', width: 110 },\n"

A14 = ("              await deleteWorkflowBinding(row.id);\n"
       "              message.success('已删除');\n"
       "              reload();\n")
R14 = ("              await deleteWorkflowBinding(row.id);\n"
       "              message.success('已删除');\n")

# 词表读失败的那一条横幅整块摘掉：只剩"表格里那一格说没说清"，用户没有任何出口。
A15 = ("      {triggers.status === 'error' ? (\n"
       "        <div style={{ marginBottom: 12 }}>\n"
       "          <ListBanner\n"
       "            state=\"error\"\n"
       "            error={triggers.error}\n"
       "            onRetry={triggers.reload}\n"
       "            label=\"触发时机词表\"\n"
       "          />\n"
       "        </div>\n"
       "      ) : null}\n")
R15 = ""

# 第一版这里打的是"未校对的哨兵值写错成一直成立"（`!== 'ready'` → `!== 'loading'`），实测**等价**
# （03:1x 那一轮红 0）：ready 态下上面那一支 `implemented.includes(event)` 先命中，根本走不到这一支。
# 于是换成"未校对这个标记挂到了引擎确实兑现的那一行上" —— 这一支才打得到"换回词表要立刻恢复正常"。
A16 = ("          return <Tag data-testid={`workflow-trigger-${row.id ?? event}`}>"
       "{triggerLabel(event)}</Tag>;\n")
R16 = ("          return <Tag data-testid={`workflow-trigger-${row.id ?? event}`}>"
       "{triggerLabel(event)} 时机未校对</Tag>;\n")

A17 = "              message.success('已删除');\n"
R17 = ""

# 「为什么」那一格整列画空：FAILED 行还在、状态也还写着 FAILED，只是再说不出"为什么"。
# 这一支是 11w 里 detail 那一列唯一的注入 —— 摘之前它一次都没红过，等于那一列没人守。
A18 = ("      render: (v: string | null | undefined) => (v ? <Text style={{ fontSize: 12 }}>{v}</Text> : '—'),\n")
R18 = "      render: () => '—',\n"

# ---- W19–W22：缺陷 #65 那一族（服务器分页）在浏览器层的牙，09-27 11:2x 补的 ----
# A19 摘的是 catch 里那句清空，和 A5（关抽屉那句）一起挂在 W5 上 —— 两处任摘一处都是等价变异，
# 实测见 A5 上方那段注释。
A19 = ("        if (stale) return;\n"
       "        setLedger(null);\n"
       "        setStatus('error');\n")
R19 = "        if (stale) return;\n        setStatus('error');\n"

A20 = "              这个实体一共有 <Text strong>{ledger.total}</Text> 条发起记录"
R20 = "              这个实体一共有 <Text strong>{rows.length}</Text> 条发起记录"

# 恒假但类型合法（`false &&` 会被 tsc 判"条件恒假"，那一轮 build 红指不回守卫）。
A21 = "          {hiddenRows > 0 ? (\n"
R21 = "          {hiddenRows < 0 ? (\n"

A22 = "  }, [binding, page]);\n"
R22 = "  }, [binding]);\n"

A23 = "const FIRE_PAGE_SIZE = 20;\n"
R23 = "const FIRE_PAGE_SIZE = 200;\n"


def runs() -> list[tuple[str, list[tuple[str, str]], list[str]]]:
    return [
        ("W1 下拉自己抄一份清单", [(A1, R1)], [
            _name('下拉里可选项的个数 = 词表里'),
            _name('兑现不了的时机一个都进不了下拉'),
            _name('下拉里那一项说的是人话'),
        ]),
        ("W2 被拒清单整块不渲染", [(A2, R2)], [
            _name('被拒的时机连同「为什么兑现不了」摆在窗里'),
            _name('每一条被拒理由都由引擎给出'),
            _name('被拒清单里的每一条都点名了自己的事件码'),
        ]),
        ("W3 摘掉「时机未校对」那一支", [(A3, R3)], [
            _name('词表形状读坏时，那一行标的是「时机未校对」'),
        ]),
        ("W4 抽屉读失败改画表格", [(A4, R4)], [
            _name('读不到发起记录时，抽屉报的是"没有读到"'),
            _name('没有总数那一栏的裸数组'),
        ]),
        # 三支补丁一起下：A4 让"没有读到"那一格不再画，A5+A19 把两处清账一起摘 —— 第三条检查
        # 要的是"旧行还亮着"这个可观察形状，而它有两个守点（实测见 A5 上方那段注释：
        # family_rerun_0927_1059.out 用 (A4,A5) 红 2、narrow_w5w22_0927_1159.out 用 (A4,A19) 红 2，
        # 两跑都逐字 `!! W5 …: 预期变红却没红 … ['读失败那一屏也不留下"三行账"的假象…']`）。
        ("W5 W4 + 两处清账一起摘，旧行在「读不到」那一屏还亮着", [(A4, R4), (A5, R5), (A19, R19)], [
            _name('读不到发起记录时，抽屉报的是"没有读到"'),
            _name('没有总数那一栏的裸数组'),
            _name('读失败那一屏也不留下"三行账"的假象'),
        ]),
        ("W6 写入口的原因换成固定「保存失败」", [(A6, R6)], [
            _name('重复登记时，界面把接口那句原因原样摆出来'),
            _name('带空白的重复也算重复'),
        ]),
        ("W7 流程实例那一格画记录号", [(A7, R7)], [
            _name('那一行给出的流程实例号就是 z-wf 回的那一格'),
            _name('FAILED 那一行不许留下流程实例号'),
            _name('502 带一个成功样的 body：那一个号不许进账'),
        ]),
        ("W8 摘掉「新建绑定」的 canDraft 闸", [(A8, R8)], [
            _name('词表读失败时「新建绑定」收住'),
        ]),
        ("W9 成功那一下不吭声", [(A9, R9)], [
            _name('保存成功那一下界面说「已保存」'),
        ]),
        ("W10 流程 KEY 那一格画成空白", [(A10, R10)], [
            _name('表里那一行同时给出实体、时机与流程 KEY'),
            _name('词表读失败时那一行仍然照常给出流程 KEY'),
        ]),
        ("W11 兑现判断写反", [(A11, R11)], [
            _name('那一行不带「引擎不兑现」的红标'),
            _name('词表形状读坏时，那一行标的是「时机未校对」'),
        ]),
        ("W12 结果那一格画「—」", [(A12, R12)], [
            _name('「发起记录」抽屉里正好一行，且状态是 STARTED'),
            _name('抽屉里多出来的那一行是 FAILED'),
            _name('FAILED 不覆盖上一条 STARTED'),
            _name('账上三行两成一败'),
            _name('502 带一个成功样的 body：那一个号不许进账'),
            # 09-27 04:2x 第二轮实测：W12 多红了这一条，读数写着
            # `["4","—","—","z-wf 拒绝发起: 流程启动失败: business key 已存在","2026-09-27 04:26"]`
            # —— 第三格「为什么」是对的，红在第二格（结果）上：那条断言的合取里写着 `wfRow4[1] === 'FAILED'`
            # （browser-e2e.mjs:3137），而 W12 摘的正是那一格的字样。所以这一条**确实是** W12 的猎物，
            # 改账不改软：留下那个合取（它是"拒绝的那一行还得自称 FAILED"唯一的落点），把它记进 W12 名下。
            _name('引擎那句拒绝理由原样落在「为什么」那一格'),
        ]),
        ("W13 「记录」那一列画事件码", [(A13, R13)], [
            _name('那一行记着是哪条记录的账'),
            _name('三行各自指着三条不同的记录'),
            _name('引擎那句拒绝理由原样落在「为什么」那一格'),
            _name('502 带一个成功样的 body：那一个号不许进账'),
            _name('502 那一行的「为什么」说的是这一单没成'),
        ]),
        ("W14 解绑之后不刷新", [(A14, R14)], [
            _name('从界面上解绑之后，表里那一行真的下去了'),
        ]),
        ("W15 词表读失败的横幅整块摘掉", [(A15, R15)], [
            _name('词表读失败时页面报的是「触发时机词表没有读到」'),
        ]),
        ("W16 已兑现的那一行也挂上「时机未校对」", [(A16, R16)], [
            _name('把词表换回来：同一行立刻不再报「未校对」'),
        ]),
        ("W17 解绑成功那一下不吭声", [(A17, R17)], [
            _name('解绑那一下界面说「已删除」'),
        ]),
        ("W18 「为什么」那一列整列画空", [(A18, R18)], [
            _name('抽屉里多出来的那一行是 FAILED'),
            _name('引擎那句拒绝理由原样落在「为什么」那一格'),
            _name('502 那一行的「为什么」说的是这一单没成'),
        ]),
        # ---- W19–W22：缺陷 #65（服务器分页）在浏览器层的四支，09-27 11:2x 补 ----
        # 预期红集是**推出来的**（每支摘掉的是哪一格、那一格被哪几条断言读），
        # 由 `--only W19..W22` 那一跑逐字核；实测不符按实测改这本账，不改断言。
        # 11:59 那一跑实测（narrow_w5w22_0927_1159.out）：W19 红 2 / W20 红 1 / W21 红 2，
        # 三跑逐字 `OK … 预期 N 条全红，无一条连带红` ⇒ 上面这三行预期与盘面一致，不动。
        # W22 那一条预期当时是**推漏了一格**：把每页 20 抬成 200 之后 25 条全落在第一页，
        # 「第 2 页」那颗按钮根本不存在，`.click()` 超时抛出把整节带走（实测分母 285→273、
        # 逐字 `!! W22 …: 整节中途抛错 …「没红」是「没跑到」`）。先修套件那一处点击
        # （browser-e2e.mjs 8b 段，点不到就留给下面的检查自己红），再把预期红集补成
        # 这一支真的动到的**五格**：每页几条那句、差额那句、请求那一格、翻页那次请求、末页行数。
        ("W19 总数那一格拿这一页的行数顶", [(A20, R20)], [
            _name('总数来自服务器，不是这一页的行数'),
            _name('第 2 页画剩下那 5 行'),
        ]),
        ("W20 「另外 N 条没读在这一页里」那句的开关翻成恒假", [(A21, R21)], [
            _name('那 5 行的差额必须被点名'),
        ]),
        ("W21 翻页不再重新问服务器（依赖里去掉 page）", [(A22, R22)], [
            _name('带上 page=2'),
            _name('第 2 页画剩下那 5 行'),
        ]),
        ("W22 每页条数从 20 抬成 200（界面与请求不再是同一个数）", [(A23, R23)], [
            _name('问的是 page=1&size=20'),
            _name('总数来自服务器，不是这一页的行数'),
            _name('那 5 行的差额必须被点名'),
            _name('带上 page=2'),
            _name('第 2 页画剩下那 5 行'),
        ]),
    ]


# ---- 未覆盖记账：名字逐字取自套件，三类理由分开写 ----------------------------------------------
def not_covered() -> dict[str, list[str]]:
    return {
        "(a) 夹具/桥/阳性对照：红了说明环境坏了而不是守卫坏了，设计上不许注入红它": [
            _name('流程绑定页在真浏览器里渲染出来'),
            _name('词表接口给得出「引擎真兑现」的时机清单'),
            _name('词表读得到的时候，「新建绑定」这颗按钮是 enabled'),
            _name('界面上写一条记录 ⇒ 桩正好收到一句'),
            _name('把桩换回来（换不回来就没有'),
            _name('把桩换回来：同一条绑定立刻又发得出去'),
            _name('桩不可达时记录照样写成功'),
            _name('引擎答 200 而 success=false 时记录照写'),
            _name('引擎答 502 而 body 里带号时记录照写'),
        ],
        "(b) 会让整节级联：写入口现在拒这些形态，一次注入要连着红三十多条并带走整节 —— 造缺陷不是摘守卫。"
        "它的牙在 vitest（WorkflowsPage.test.tsx 钉的就是这份草稿的字节）与 deployed 层": [
            _name('在界面上保存绑定真的发出了一次写请求'),
            _name('送出去的那一份带着本节的 appCode'),
            _name('界面送出去的草稿里 autoSubmit 真的是 1'),
            _name('草稿里的触发时机就是下拉里那一个'),
            _name('新建那张窗默认选中这个应用的第一个实体'),
        ],
        "(c) 数据在服务端那一侧：界面无条件画服务端给的那一格，改 .tsx 动不到它写没写。"
        "牙在 _e2e/mutate_workflow_deployed_guard.py 的 W 系列": [
            _name('成功那一行不写失败原因'),
            _name('打的必须是 z-wf 真映射的那条路径'),
            _name('businessKey 能定位回'),
            _name('initiator 用的是这个浏览器自己的那个人'),
            _name('流程变量里带着低代码这一侧的坐标'),
            _name('界面上打的那一句标题作为流程变量带走了'),
            _name('流程 KEY 是界面上填的那一格'),
            _name('桩不可达时一句都不许发出去'),
            _name('界面那一行与库里读回的那一条逐字相同'),
            _name('被拒之后表里仍然只有那一条绑定'),
            _name('库里也读不到这一条'),
            _name('这一节的应用收掉了'),
            _name('解绑与删应用本身不发流程'),
        ],
        # (d) 那一格（09-27 10:4x 跟着 #65 新添的 5 条"只加了检查没加注入"）已在 11:2x 由
        # W19–W22 清空 —— 总数/差额/页码请求/翻页真的再问一次/末页那一格各有各的注入。
        # 同一轮量具自己撞出来的**残余缺口**记在这儿（缺口在注入这一侧，不在检查名那一侧，
        # 所以不进这张表）：'读失败那一屏也不留下"三行账"的假象' 这条性质有两个守点
        # （关抽屉 A5 + catch A19），摘任意一个都是等价变异 —— 两跑实测各红 2 条、逐字
        # `!! W5 …: 预期变红却没红`。W5 于是改成一次摘两个。这意味着
        # 于是 W5 改成一次摘两个。这意味着
        # **浏览器层现在只守得住"两处一起摘"，单点回归（比如日后有人"简化"掉 catch 那句）
        # 抓不到**：要拆回一支一站点，得先给套件加一条"重新打开的那 1.2s 里旧行不许还亮着"
        # 的 loading 窗口采样。本窗没加 ⇒ 按覆盖缺口记账，不许把它当成已经在守卫的东西。
    }


def bundle() -> tuple[str, str]:
    html = DIST_INDEX.read_text(encoding="utf-8") if DIST_INDEX.exists() else ""
    match = re.search(r'assets/(index-[^"\']+\.js)', html)
    if not match:
        return ("<dist/index.html 里没有 index-*.js>", "")
    name = match.group(1)
    target = UI / "dist" / "assets" / name
    digest = hashlib.md5(target.read_bytes()).hexdigest() if target.exists() else "<缺文件>"
    return name, digest


def src_digest() -> str:
    """整棵 `src/` 的字节指纹 —— "还原了没有"要钉的是源码，不是产物长得好不对。

    为什么不能拿产物当判据（缺陷 #69）：同一棵 src 树在本机会构建出两套 `index-*.js`。
    这一句的每一个数都是 09-27 现量、日志还在盘上，别背：
      - 整族第三轮（`browser_guard/run3.out`）：基线（05:02:10 那次 build）出 `index-DBJrrqb7.js`，
        恢复轮（06:00:02）出 `index-ruBdvAdn.js` —— 而 `WorkflowsPage.tsx` 的 md5 前后逐字节都是
        `87b30e33161f27089623d260151e12fd`。这一对差值就是那一轮 `RESULT: 1 problem(s)` 的全部内容。
      - 收窄跑（`browser_guard/run4_narrow.out`，带下面这条 src 指纹判据）：基线（06:09:21）出
        `index-ruBdvAdn.js`，恢复轮（06:15:11）出 `index-DBJrrqb7.js` —— 而这一轮的判据现量了两边的
        src 指纹**相等**（否则它会打印 `!! 恢复后 src 指纹 …` 并计数）。方向还和上一轮相反。
      - 紧接着 06:19:52 `npm run check`（`~/.cache/zlc61/check_0927_0617.log`）同一份 src 又出
        `index-ruBdvAdn.js`。
    ⇒ 六次干净 src 构建、两个名字、交替出现。两支 chunk 表逐行只在名字上不同，`antd-*.js` 甚至差 0.04 kB
    ⇒ rollup 在本机不逐字节可复现，产物名在这一层不是身份，连"两次同名 ⇒ 同一份源码"这种反向推都不成立。
    仓库里另一支守卫（`mutate_permission_browser_guard.py`）早就因此改用 src 指纹，这一支却拿"产物相等"判还原，
    于是第三轮那一条红**结构上抓不到任何猎物**：它想防的"变异没还原"一直由 PAGE 文本相等看着（那一句三轮里 0 次触发），
    而它对纯粹的产物漂移必红。
    """
    h = hashlib.md5()
    for p in sorted((UI / "src").rglob("*")):
        if p.is_file():
            h.update(str(p.relative_to(UI)).encode("utf-8"))
            h.update(p.read_bytes())
    return h.hexdigest()


def validate() -> int:
    """静态体检：锚点唯一、红集互不相同、名字在套件里逐字一次、分母全被登记。

    全在这一支的代价（一轮 ~5 分钟 × 19 轮：基线 + 17 支注入 + 恢复轮）之前做，否则一次改名要等整轮
    build + 整轮浏览器才发现。
    """
    src = PAGE.read_text(encoding="utf-8")
    for anchor in (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17):
        n = src.count(anchor)
        if n != 1:
            print(f"!! anchor 在 WorkflowsPage.tsx 里出现 {n} 次（应为 1）: {anchor.strip()[:70]}")
            return 1

    names = section_names()
    if len(names) != len(set(names)):
        dup = sorted({n for n in names if names.count(n) > 1})
        print(f"!! 11w 段内有重名检查（红集合无法归因）: {dup}")
        return 1
    suite = E2E.read_text(encoding="utf-8")
    for name in names:
        n = suite.count(name)
        if n != 1:
            print(f"!! 检查名在 browser-e2e.mjs 里出现 {n} 次（应为 1）: {name[:70]}")
            return 1
    if T_SECTION_CRASH not in names:
        print("!! 兜底标题不在扫出来的分母里（段内 catch 改过形状？）")
        return 1

    sets: dict[tuple[str, ...], str] = {}
    for tag, edits, expected in runs():
        if not expected:
            print(f"!! {tag}: 预期红集为空 —— 一支谁都不红的注入不是证据，别把它算进覆盖")
            return 1
        for anchor, _ in edits:
            if src.count(anchor) != 1:
                print(f"!! {tag}: 锚点不唯一，注入会打到别处")
                return 1
        for name in expected:
            if name not in names:
                print(f"!! {tag}: 预期红集里的名字不在 11w 分母里: {name}")
                return 1
            if name == T_SECTION_CRASH:
                print(f"!! {tag}: 把「整节中途抛错」写成了预期红 —— 那一轮的其余读数全部作废")
                return 1
        key = tuple(sorted(expected))
        if key in sets:
            print(f"!! {tag} 与 {sets[key]} 的预期红集合相同 —— 那要么是不同的断言红在同一个标题下，"
                  "要么其中一支是重复检查")
            return 1
        sets[key] = tag

    booked: dict[str, str] = {}
    for reason, listed in not_covered().items():
        for name in listed:
            if name not in names:
                print(f"!! 记账的名字不在 11w 分母里: {name}")
                return 1
            if name in booked:
                print(f"!! {name} 被记了两笔（{booked[name]} / {reason[:24]}）")
                return 1
            booked[name] = reason

    covered = {n for _, _, expected in runs() for n in expected}
    both = covered & set(booked)
    if both:
        print(f"!! 这些名字既被注入覆盖又被记为未覆盖（账本自相矛盾）: {sorted(both)}")
        return 1
    orphans = [n for n in names if n not in covered and n not in booked and n != T_SECTION_CRASH]
    if orphans:
        print(f"!! {len(orphans)} 条检查既没被注入覆盖、也没记在未覆盖账上（新增检查要两边都登记）:")
        for one in orphans:
            print(f"   - {one}")
        return 1
    print(f"  静态体检通过：11w 分母 {len(names)} 条（扫自源码）、{len(runs())} 支注入、"
          f"覆盖 {len(covered)} 条、按未覆盖记账 {len(booked)} 条、兜底标题 1 条")
    return 0


def port_busy() -> bool:
    for host in ("127.0.0.1", "::1"):
        family = socket.AF_INET if host == "127.0.0.1" else socket.AF_INET6
        with socket.socket(family, socket.SOCK_STREAM) as sock:
            sock.settimeout(0.4)
            if sock.connect_ex((host, PORT)) == 0:
                return True
    return False


def e2e_already_running() -> list[str]:
    """另一轮 browser-e2e 正在跑的时候绝对不能开火：它不持锁（只是读产物），
    而我这一支会把 .tsx 改成带缺陷的样子 —— 它的"全绿"就会变成我的注入的读数。"""
    proc = subprocess.run(["pgrep", "-fl", "browser-e2e.mjs"], capture_output=True, text=True)
    return [line for line in proc.stdout.splitlines() if line.strip()]


def build(label: str) -> str:
    proc = subprocess.run(["npm", "run", "build"], cwd=UI, capture_output=True, text=True,
                          timeout=BUILD_TIMEOUT)
    (LOG_DIR / f"{label}_build.log").write_text(proc.stdout + proc.stderr, encoding="utf-8")
    if proc.returncode != 0:
        raise SystemExit(f"build 失败（{label}），见 {LOG_DIR / (label + '_build.log')}\n"
                         + (proc.stdout + proc.stderr)[-1800:])
    name, digest = bundle()
    print(f"    built: {name} md5={digest[:8]}")
    return f"{name}|{digest}"


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)], cwd=UI,
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    for _ in range(480):
        if port_busy():
            return proc
        time.sleep(0.5)
    stop_preview(proc)
    raise SystemExit(f"preview 起不来（:{PORT} 240s 内没在听）")


def stop_preview(proc: subprocess.Popen) -> None:
    try:
        os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
        proc.wait(timeout=20)
    except (ProcessLookupError, subprocess.TimeoutExpired):
        try:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
        except ProcessLookupError:
            pass


def run_round(label: str) -> tuple[list[str], int]:
    """返回 (红名单, 本轮检查总数)。总数用来对分母 —— 见文档判据 1。"""
    try:
        proc = subprocess.run(["node", str(E2E)], cwd=UI, capture_output=True, text=True,
                              timeout=ROUND_TIMEOUT, env={**os.environ, "E2E_REPEATS": "1"})
    except subprocess.TimeoutExpired:
        raise SystemExit(f"{label}: browser-e2e 一轮超过 {ROUND_TIMEOUT}s 没收线 → "
                         "这一轮没有结论（不许当成通过），先等机器空闲再重跑")
    text = proc.stdout + proc.stderr
    (LOG_DIR / f"{label}.log").write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {LOG_DIR / (label + '.log')}")
        return [f"<build-guard:{label}>"], 0
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论")
    if "ERROR" in text:
        print(f"  !! {label}: 套件里有脚本级 ERROR（不是检查红），这一轮没有结论，见 "
              f"{LOG_DIR / (label + '.log')}")
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    summary = re.search(r"=> PASS (\d+) / FAIL (\d+)", text)
    total = (int(summary.group(1)) + int(summary.group(2))) if summary else 0
    print(f"  {label}: PASS+FAIL {total} / 红 {len(failed)}")
    return failed, total


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if T_SECTION_CRASH in failed:
        print(f"  !! {label}: 整节中途抛错（{T_SECTION_CRASH} 红了）—— 这一轮的归因不成立，"
              "后面那些「没红」是「没跑到」")
        return 1
    if not hard and not extra:
        print(f"  OK  {label}: 预期 {len(expected)} 条全红，无一条连带红")
        return 0
    if hard:
        print(f"  !! {label}: 预期变红却没红（这几条浏览器检查是空的）: {hard}")
    if extra:
        print(f"  !! {label}: 出现预期之外的红 {extra} —— 不许加白名单了事，"
              "先拿样本证明这一处注入换掉了哪一支断言，再改预期")
    return 1


def main(only: str | None = None) -> int:
    if (code := validate()):
        return code
    # `--only` 收逗号分隔的题号（09-27 11:5x 加的：一整族 20 轮 ~50 分钟，只为核 5 支重跑整族不划算）。
    # 点不到名的题号要**当场拒**，不许退化成"只跑到能跑到的那几支"—— 那等于把打错的题号读成"这一支已经核过了"。
    wanted = None if only is None else {t.strip() for t in only.split(",") if t.strip()}
    tags = {r[0].split()[0] for r in runs()}
    if wanted is not None:
        unknown = sorted(wanted - tags)
        if unknown:
            print(f"!! --only {only} 里有整族不存在的题号 {unknown}"
                  f"（这一族现有 {len(tags)} 支：{sorted(tags, key=lambda t: int(t[1:]))}）—— 收窄跑不许空跑")
            return 2
    picked = [(i, r) for i, r in enumerate(runs(), start=1)
              if wanted is None or r[0].split()[0] in wanted]
    if only is not None and not picked:
        print(f"!! --only {only} 在一整族里点不到名 —— 收窄跑不许空跑")
        return 2
    busy = e2e_already_running()
    if busy:
        print("!! 已经有一轮 browser-e2e 在跑，它不持锁而我会改 .tsx —— 那轮的读数会变成我的注入：")
        for line in busy:
            print(f"   {line}")
        return 2
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              f"\n   先确认那是谁（`lsof -nP -iTCP:{PORT} -sTCP:LISTEN`），是自己这一支的量具再停。")
        return 2

    original = PAGE.read_text(encoding="utf-8")
    base_src = src_digest()
    bad = 0
    preview = None
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===")
        base_bundle = build("00_baseline")
        preview = start_preview()
        failed, total = run_round("00_baseline")
        if failed:
            print(f"  !! 基线就有红，注入结果无法归因；先修基线: {failed}")
            return 2
        if not total:
            print("  !! 基线一轮数不出分母（套件没跑完？）")
            return 2
        print(f"  基线分母钉住: {total} 条检查、产物 {base_bundle.split('|')[0]}")
        if only is not None:
            print(f"⚠ 收窄跑：只点名的那一支（{only}）+ 恢复轮。这一跑的 RESULT 只证明"
                  "改过的判据在真 build/真浏览器轮次里跑得通，**不能**当「W1–W18 整族自证」的账。")

        for i, (tag, edits, expected) in picked:
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} ===")
            mutated = original
            for anchor, repl in edits:
                mutated = mutated.replace(anchor, repl, 1)
            PAGE.write_text(mutated, encoding="utf-8")
            try:
                # 判"注入到底进没进这棵树"用 src 指纹：它是源字节的确定函数，而产物会漂（见 src_digest）。
                if src_digest() == base_src:
                    print(f"  !! {tag}: 写完变异而 src 指纹没动 —— 锚点没落进被测的树，"
                          "这一轮的读数就是基线的回声，不算证据")
                    bad += 1
                this_bundle = build(label)
                print(f"  （信息）产物 {this_bundle} —— 只记不判：本机 rollup 不逐字节可复现")
                failed, this_total = run_round(label)
                if total and this_total != total:
                    print(f"  !! {tag}: 本轮分母 {this_total} != 基线 {total} —— "
                          "某一节中途没跑完，「没红」不能算通过")
                    bad += 1
                bad += judge(tag, expected, failed)
            finally:
                PAGE.write_text(original, encoding="utf-8")
                if PAGE.read_text(encoding="utf-8") != original:
                    print("  !! WorkflowsPage.tsx 未恢复到原始内容")
                    bad += 1
                if src_digest() != base_src:
                    print("  !! 这一支收完 src 指纹没回到基线 —— 有 PAGE 之外的文件被留在变异态"
                          "（上面那句 PAGE 文本相等看不见这种事）")
                    bad += 1

        print("\n=== 恢复后复跑 ===")
        restored_src = src_digest()
        restored_bundle = build("99_restored")
        if restored_src != base_src:
            print(f"  !! 恢复后 src 指纹 {restored_src} != 基线 {base_src} —— 源码真的没回来")
            bad += 1
        if restored_bundle != base_bundle:
            # 只打印，不判红：判"还原"的活由上面那条 src 指纹干。这一条从前是判红的，
            # 而它在源码逐字节回来了的时候照样红（缺陷 #69），一条永远抓不到猎物的红等于把
            # "量具有病"记成"产品坏了"。产物名照旧进日志，漂移要看得见，只是不许当判据。
            print(f"  （信息）产物 {restored_bundle} != 基线 {base_bundle} —— 产物漂移，不作判据")
        failed, this_total = run_round("99_restored")
        if failed or this_total != total:
            print(f"  !! 恢复后仍有红或分母不符: {failed} ({this_total} vs {total})")
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    n_covered = len({n for _, _, expected in runs() for n in expected})
    n_booked = sum(len(v) for v in not_covered().values())
    if bad:
        print(f"\nRESULT: {bad} problem(s)")
        return 1
    print(f"\nRESULT: 11w 的 {n_covered} 条各自钉住一件事（{len(runs())} 支注入）；"
          f"{n_booked} 条按未覆盖记账（三类理由在文档与 NOT_COVERED 里）")
    return 0


def selftest() -> int:
    """`--selftest`：证明上面那条 src 指纹判据**有猎物**，且不靠 19 轮整族重跑（那一跑 58 分钟）。

    两支成对，都不 build、不抢 preview 端口：
      A 动一个 `WorkflowsPage.tsx` **之外**的 src 文件 —— 新判据必须变；同时 PAGE 文本必须逐字不变，
        这就把旧判据（只看 PAGE）的瞎处演出来了。
      B 撤掉探针 —— 指纹必须回到基线。回不去就是还原步没做完，判据本身就是空的。
    它证不了的那半也记在这：「产物漂移会不会被新判据漏掉」不需要证 —— 新判据根本不读产物。
    反过来「注入进了源码但没进产物（等价变异）」这一格从前的产物相等判据名义上管、实际管不住
    （产物本来就会漂，相等几乎不发生 ⇒ 一条几乎不触发的判据是空跑），现在由每轮"预期那几条必须红"
     behavioral 地管：改动没落到界面上，那几条就不会红，`judge()` 当场报「预期变红却没红」。
    """
    base = src_digest()
    page_before = PAGE.read_text(encoding="utf-8")
    probe = UI / "src" / "__guard_probe__.ts"
    bad = 0
    try:
        probe.write_text("export const GUARD_PROBE = 1;\n", encoding="utf-8")
        moved = src_digest() != base
        blind = PAGE.read_text(encoding="utf-8") == page_before
        if not moved:
            print("  !! A 往 src 里加了一个文件而指纹没动 —— 这把尺没有猎物")
            bad += 1
        elif not blind:
            print("  !! A 探针不该动到 WorkflowsPage.tsx，PAGE 却变了 —— 这一支的对照不成立")
            bad += 1
        else:
            print("  OK A 动 PAGE 之外的 src 文件：src 指纹跟着动，而 PAGE 文本逐字不变"
                  "（旧判据只看 PAGE，这一处它是瞎的）")
    finally:
        probe.unlink(missing_ok=True)
    if src_digest() != base:
        print("  !! B 撤掉探针后指纹没回到基线 —— 还原步没做完")
        bad += 1
    else:
        print("  OK B 同一棵树读两次逐字节相同：判据量的是源码，不受 rollup 产物漂移影响")
    # C：把"锚点没命中"这一格演一遍 —— 注入落空时 main() 里那句 `src_digest() == base_src` 必须为真，
    # 也就是那条红**打得出得来**（否则新判据和它替掉的那条一样，是条没有猎物的尺）。
    # 这里用的是真注入的锚点，只是先给它接一个不存在的尾巴，让 `replace(..., 1)` 静默落空。
    try:
        _tag, edits, _exp = runs()[0]
        anchor, repl = edits[0]
        missed = page_before.replace(anchor + "\n/* 这行源码里没有 */", repl, 1)
        if missed != page_before:
            print("  !! C 造不出「锚点落空」的样本（replace 竟然命中了）—— 这一支对照不成立")
            bad += 1
        else:
            PAGE.write_text(missed, encoding="utf-8")
            if src_digest() != base:
                print("  !! C 写回的是一份逐字节没变的源码，指纹却动了 —— 判据在量别的东西")
                bad += 1
            else:
                print(f"  OK C 锚点落空的变异（`{_tag}` 那一支的 replace 静默 no-op）：写盘后指纹不动"
                      "⇒ main() 那条「写完变异而 src 指纹没动」的红打得出，注入没落地不会混过去")
    finally:
        PAGE.write_text(page_before, encoding="utf-8")
    if PAGE.read_text(encoding="utf-8") != page_before or src_digest() != base:
        print("  !! C 收场后 PAGE/指纹没回到基线")
        bad += 1
    print(f"SELFTEST RESULT: {bad} problem(s)")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(Path(__file__).name)
    try:
        if "--selftest" in sys.argv:
            sys.exit(selftest())
        only = None
        if "--only" in sys.argv:
            only = sys.argv[sys.argv.index("--only") + 1]
        sys.exit(main(only))
    finally:
        _mutlock.release()
