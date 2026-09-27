#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""缺陷 #73：部署中心在**真浏览器层**的注入自证（`browser-e2e.mjs` 第 11x 段）。

跑法（在 z-lc-admin-ui 下；后端 18090 要在跑，5274 让出来给这一支自己起的 preview）：

    python3 e2e/mutate_deployment_browser_guard.py              # 整族（基线 + 21 支 + 恢复轮）
    python3 e2e/mutate_deployment_browser_guard.py --only D7,D20   # 收窄跑（只为核改过的判据）
    python3 e2e/mutate_deployment_browser_guard.py --selftest   # 证 src 指纹那条判据有猎物（不 build）

#70 把部署中心从"登记一行做不成的账"改成了"当场执行并把结局写回来"，改动面在
`DeploymentsPage.tsx` + `_deployment.ts`，而 `grep -c DeploymentsPage e2e/browser-e2e.mjs` 当时是 **0**：
vitest 那份喂的是它自己 stub 的词表（`DeploymentsPage.test.tsx:28` 的 `executable` 是两种，
服务端只报一种），所以"这份清单是真服务端给的""点下去之后**运行时库里**真长出那张表"
"状态/版本/日志三格抄的是服务器原话"这三件事，在浏览器层一条都没量过。这一支就是那一段：
**词表 → 下拉/被拒理由 → 点一次真执行 → 表里那一行 → 库里那张表 → 抽屉原话 → 两处"读不到"**。

21 支注入，每支只摘一句保证，红集合互不相同（预期红集在跑第一遍时是**推**出来的，
按实测改这本账，不改断言）：

  D1  下拉里不摆服务器不执行的那几种      → 「个数 = 词表个数」+「灰的那几种个数 = rejected」
  D2  不执行的那几种改成可选             → 「可选项个数 = executable」+ 上一条（灰的集合空了）
  D3  灰标签里的「（服务器不执行）」拿掉   → 只红「灰的那几种标签自己写着不执行」
  D4  窗里那句理由换成页面编的            → 「理由原样摆在窗里且点名方式码」
  D5  界面自己抄一份可执行清单            → 「个数 = 词表个数」+「可选项说的是人话」
  D6  状态格盖自己的章「已执行」          → 「那一行的状态格是服务器给的结局」
  D7  物化批次那一格画空白                → 「没挂批次的那格画的是破折号」
  D8  部署完不重读列表                    → 「读回来不是乐观追加」+ 底下五格各自一条（{14,15,16,17,19,20,21}）
  D9  成功那一下说「部署已创建」          → 「说的是部署完成并抄服务器那句第一行」
  D10 摘掉「新建部署」的词表闸            → 「词表读不到时按钮收住」
  D11 词表闸写反（ready 才禁用）          → 粗筛不判别：整节 19 条一起塌（见 runs() 里的注释）
  D12 词表读失败的横幅整块摘掉            → 「报的是部署方式词表没有读到并给重试入口」
  D13 空态退回通用「暂无数据」            → 「一条都没有时说的是该应用还没有部署记录」
  D14 空态不分读失败，恒说「还没有」      → 「读不到那一屏报的是没有读到」（这一条要读占位话，见套件里那句注释）
  D15 「物化批次」整列摘掉                → 「表头逐格按列序」+ 破折号那条（列一少，按位读的格子全体错位）
  D16 抽屉标题不认领是哪一行的账          → 「标题里有 #id 与状态」
  D17 日志格换成「成功」两个字            → 「那一格里是服务器原话」
  D18 「日志」那颗按钮永远禁用            → 标题 + 原话两条（详情根本读不回来）
  D19 「方式」列画裸编码                  → 「那一格画的是中文标签，ID 就是服务器那行的 id」
  D20 挂载时多读一次列表                  → 「一次挂载只发一次部署列表请求」
  D21 点一次发两个 create                 → 「只发一个 create」+「界面行数 = 服务器行数」+ 收尾那一句（实测 3 条）

上面每支箭头后面是**这一支想摘掉哪一句保证**；每支的**实测红集**以 `runs()` 里那份名单为准 ——
run1 跑完按实测改过两处（D11 从 3 条改 19 条、D21 从 2 条改 3 条），断言一个字没动。

⚠ 有七条检查这一支**没有专属猎物注入**，按未覆盖记账（写成"全都能抓到"是谎），逐条在
`not_covered()` 里由 validate() 对着源码核名字。理由各不一样，都留了可复核的凭据：
  · 「节前对账 / 节前的词表读得到 / 应用收掉了」是夹具与收尾，实测在 run1 的 22 轮里一条都没红过；
  · 「默认落在服务器清单的第一种」在当前词表下**结构上打不出专属注入** —— `executable` 只有一种，
    于是 `executable[0]` ↔ 钉死 `'HOT_LOAD'` ↔ 取最后一种，三种写法画出来逐字节相同。
    它的牙在 vitest（那边的 fixture 是两种，钉的是原序）；
  · 「点了部署之后库里真的长出那张表」要红它只能改**送出去**的那个 deployType，而写入口现在
    把非 executable 的拒成 `success:false / code:400`（09-27 13:2x 实测 curl：
    「部署方式 [DOCKER] 服务器执行不了…当前可登记的只有 [HOT_LOAD]」）⇒ 一次注入连着塌
    「toast」「状态格」「服务器行数」等九条并带走整节，那是造一个新缺陷不是摘一句守卫；
  · 「读不到那一屏不留下上一份账的行」的清账那一句在共享的 `_scope.ts`（六个管理页共用，
    别的注入族的基线也读它），不在本支的被测文件里 —— 摘它是跨族改动，另开一族量；
  · 「恢复之后再读一次：那一行回来了」同样没有专属注入，但它 D11、D21 两支里都连带红过。
    「没有专属猎物」≠「从来没红过」：这四格混过一次，所以 validate() 现在要求 —— 既进某支预期红集
    又留在记账名单上的名字，理由里必须原样写「连带红过」，少了这四个字当场拒绝开火。

判据同 `mutate_workflow_browser_guard.py`（`FAIL <名字>   << <读数>` 先剥读数再比集合），
外加这一支自己的五条：
  1) 每轮 PASS+FAIL 总数 == 基线那轮（少一条 = 某一节中途没跑完，"没红"是"没跑到"）；
  2) 每轮开始前 `src/` 指纹必须与基线**不同**（相同 = 锚点静默落空，那一轮是基线的回声）；
     产物名只记不判 —— 本机 rollup 不逐字节可复现（缺陷 #69 量过：同一棵 src 树出过两个名字）；
  3) 恢复轮必须回到基线的 src 指纹；
  4) 11x 的检查名由**源码扫**出来（不抄清单）：整节之内不许重名，且每一条要么被某支注入覆盖、
     要么出现在记账名单里 —— 新增一条检查而两边都没登记，validate() 当场拒绝开跑；
  5) 红在 29 条分母之外（run1 实测 3/22 轮被 11w 的桩计数波及）⇒ **这一轮归因不成立，点名重跑**，
     既不写成预期也不加白名单 —— 我的注入只改 `DeploymentsPage.tsx`，别的节的红不该由这一轮的账承担。
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
PAGE = UI / "src/views/admin/DeploymentsPage.tsx"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
# 每轮日志按 `NN_<tag>.log` 落这里，重跑同一族会逐字覆盖上一轮的日志 —— 而票面/README 要引某一轮
# 的原始日志当出处，所以给每一跑单独开目录：`ZLC73_GUARD_RUN=run2` ⇒ browser_guard_run2/。
LOG_DIR = Path(str(Path.home() / ".cache/zlc73/browser_guard") + (
    f"_{os.environ['ZLC73_GUARD_RUN']}" if os.environ.get("ZLC73_GUARD_RUN") else ""))
LOG_DIR.mkdir(parents=True, exist_ok=True)

BUILD_TIMEOUT = 900
# 上限而不是预期：一轮跑的是完整 browser-e2e（分母由基线轮现量并钉住；09-27 13:1x 空载实测一轮 ~3.5min）。
# 超时 == 无结论（收线并说明），不 == 全绿。
ROUND_TIMEOUT = 2400
FAIL_DETAIL_SEP = "   << "

SECTION_HEAD = "/* ---- 11x."
SECTION_TAIL = "/* ---- 9. 全局异常"
CHECK_NAME = re.compile(r"^\s*check\(\s*'([^']+)'", flags=re.M)

# 整节 catch 的兜底标题：红了就说明这一节中途抛错，后面那些「没红」全是「没跑到」。
T_SECTION_CRASH = '部署中心的真浏览器层（#70 / 缺陷 #73）'


def section_text() -> str:
    src = E2E.read_text(encoding="utf-8")
    at = src.find(SECTION_HEAD)
    tail = src.find(SECTION_TAIL)
    if at < 0 or tail <= at:
        raise SystemExit(f"!! 找不到 11x 那一段的边界（head={at} tail={tail}）—— 段名改过？分母就取不出来")
    return src[at:tail]


def section_names() -> list[str]:
    return CHECK_NAME.findall(section_text())


def _name(fragment: str) -> str:
    """在 11x 那一段里按子串找唯一的名字；不唯一或找不到都拒绝继续（手敲会臆造）。"""
    hits = [n for n in section_names() if fragment in n]
    if len(hits) != 1:
        raise SystemExit(f"!! 检查名子串 {fragment!r} 命中 {len(hits)} 条（应为 1）: {hits}")
    return hits[0]


# ---- anchors：每一段都必须在原始文件里恰好出现一次，否则注入会打到别处（validate() 先查一遍）----
D1_A = ("      ...rejectedReasons.map((item) => ({\n"
        "        value: item.type,\n"
        "        label: `${deployTypeLabel(item.type)}（服务器不执行）`,\n"
        "        disabled: true,\n"
        "      })),\n")
D1_R = ("      ...rejectedReasons.filter(() => false).map((item) => ({\n"
        "        value: item.type,\n"
        "        label: `${deployTypeLabel(item.type)}（服务器不执行）`,\n"
        "        disabled: true,\n"
        "      })),\n")

D2_A = "        disabled: true,\n"
D2_R = "        disabled: false,\n"

D3_A = "        label: `${deployTypeLabel(item.type)}（服务器不执行）`,\n"
D3_R = "        label: deployTypeLabel(item.type),\n"

D4_A = "                  <Text code>{item.type}</Text> —— {item.reason}\n"
D4_R = "                  <Text code>{item.type}</Text> —— 这一种服务器跑不了\n"

D5_A = "  const executable = kinds.vocabulary.executable;\n"
D5_R = "  const executable = ['HOT_LOAD', 'SQL_SYNC'];\n"

# `STATUS_COLOR` 必须仍然被读一次：摘掉那一句会撞 TS6133（noUnusedLocals），
# 那时 build 红在 tsc 上而不是红在那句守卫上（同 W6 那句 `err` 的教训）—— 预跑 tsc 实测过。
D6_A = "      render: (value: string) => <Tag color={STATUS_COLOR[String(value)] ?? 'default'}>{value}</Tag>,\n"
D6_R = "      render: (value: string) => <Tag color={STATUS_COLOR[String(value)] ?? 'default'}>已执行</Tag>,\n"

D7_A = "      render: (value?: number | null) => value ?? '—',\n"
D7_R = "      render: () => '',\n"

D8_A = "      reload();\n      if (done.status === 'SUCCESS') {\n"
D8_R = "      if (done.status === 'SUCCESS') {\n"

D9_A = "        message.success(`部署完成 · ${firstLine(done.deployLog)}`);\n"
D9_R = "        message.success('部署已创建');\n"

D10_A = "          disabled={!appCode || kinds.status !== 'ready'}\n"
D10_R = "          disabled={!appCode}\n"

D11_R = "          disabled={!appCode || kinds.status === 'ready'}\n"

D12_A = ("      {kinds.status === 'error' ? (\n"
         "        <div style={{ marginBottom: 12 }} data-testid=\"deployment-vocabulary-error\">\n"
         "          <ListBanner state=\"error\" error={kinds.error} onRetry={kinds.reload} label=\"部署方式词表\" />\n"
         "        </div>\n"
         "      ) : null}\n")
D12_R = ("      {kinds.status === 'loading' ? (\n"
         "        <div style={{ marginBottom: 12 }} data-testid=\"deployment-vocabulary-error\">\n"
         "          <ListBanner state=\"error\" error={kinds.error} onRetry={kinds.reload} label=\"部署方式词表\" />\n"
         "        </div>\n"
         "      ) : null}\n")

# 空态那一格：D13 退回通用文案（只有读失败那一屏还分家），D14 干脆不分家。
# 两支都必须让 `listEmptyText` 仍被引用一次 —— 否则 `noUnusedLocals` 把 import 判成编译错，
# 那一轮 build 红指不回守卫（同 W6 那句 `err` 的教训）。
DP_EMPTY_A = "        locale={{ emptyText: listEmptyText(state, '部署记录') }}\n"
D13_R = ("        locale={{ emptyText: state === 'error'\n"
         "          ? listEmptyText(state, '部署记录') : '暂无数据' }}\n")
D14_R = "        locale={{ emptyText: listEmptyText('ready', '部署记录') }}\n"

D15_A = ("    {\n"
         "      title: '物化批次',\n"
         "      dataIndex: 'materializationId',\n"
         "      width: 110,\n"
         "      // 这一格现在是\"指得回一批真产物\"的 id：写入口会拒掉不存在或属于别的应用的批次。\n"
         "      render: (value?: number | null) => value ?? '—',\n"
         "    },\n")
D15_R = ""

D16_A = "        title={detail ? `部署 #${detail.id} · ${detail.status}` : ''}\n"
D16_R = "        title=\"部署详情\"\n"

D17_A = ("            <pre className=\"zlc-ddl\" data-testid=\"deployment-log\">\n"
         "              {detail.deployLog || '（这一行没有日志：它是在部署真的会执行之前登记的老记录）'}\n"
         "            </pre>\n")
D17_R = ("            <pre className=\"zlc-ddl\" data-testid=\"deployment-log\">\n"
         "              {'成功'}\n"
         "            </pre>\n")

D18_A = "          disabled={!row.id}\n"
D18_R = "          disabled={Boolean(row.id)}\n"

D19_A = "      render: (value: string) => deployTypeLabel(value),\n"
D19_R = "      render: (value: string) => value,\n"

D20_A = "    () => listDeployments(appCode),\n    appCode || null,\n  );\n"
D20_R = ("    () => listDeployments(appCode),\n    appCode || null,\n  );\n"
         "  useEffect(() => {\n    reload();\n  }, []);\n")
D20_IMPORT_A = "import { useCallback, useMemo, useState } from 'react';\n"
D20_IMPORT_R = "import { useCallback, useEffect, useMemo, useState } from 'react';\n"

D21_A = "        onOk={() => void create()}\n"
D21_R = "        onOk={() => { void create(); void create(); }}\n"


def runs() -> list[tuple[str, list[tuple[str, str]], list[str]]]:
    return [
        ("D1 下拉里不摆服务器不执行的那几种", [(D1_A, D1_R)], [
            _name('下拉里的选项个数 = 词表里的个数'),
            _name('兑现不了的那几种在列表里是灰的'),
        ]),
        ("D2 不执行的那几种改成可选", [(D2_A, D2_R)], [
            _name('兑现得了的那几种是可选中的'),
            _name('兑现不了的那几种在列表里是灰的'),
        ]),
        ("D3 灰标签里的「（服务器不执行）」拿掉", [(D3_A, D3_R)], [
            _name('兑现不了的那几种在列表里是灰的'),
        ]),
        ("D4 窗里那句理由换成页面编的", [(D4_A, D4_R)], [
            _name('每一种不执行的理由原样摆在这扇窗里'),
        ]),
        ("D5 界面自己抄一份可执行清单", [(D5_A, D5_R)], [
            _name('下拉里的选项个数 = 词表里的个数'),
            _name('兑现得了的那几种是可选中的'),
        ]),
        ("D6 状态格盖自己的章「已执行」", [(D6_A, D6_R)], [
            _name('表里那一行的状态格写的是服务器给的那个结局'),
        ]),
        ("D7 物化批次那一格画空白", [(D7_A, D7_R)], [
            _name('没记版本、没挂物化批次的那两格画的是破折号'),
        ]),
        ("D8 部署完不重读列表", [(D8_A, D8_R)], [
            _name('表里那一行的状态格写的是服务器给的那个结局'),
            _name('方式那一格画的是中文标签'),
            _name('没记版本、没挂物化批次的那两格画的是破折号'),
            _name('列表里那一行是读回来的'),
            _name('抽屉标题认领的是哪一行的账'),
            _name('日志那一格里是服务器原话'),
            _name('关掉抽屉不带走那一行'),
        ]),
        ("D9 成功那一下说「部署已创建」", [(D9_A, D9_R)], [
            _name('成功那一下界面说的是「部署完成」'),
        ]),
        ("D10 摘掉「新建部署」的词表闸", [(D10_A, D10_R)], [
            _name('词表读不到时「新建部署」收住'),
        ]),
        # 实测（run1 的 11_D11.log）：这一支一红就是 19 条 —— 闸写反把「新建部署」永久按住，
        # 本节后面每一条"点一次部署"的动作都做不了 ⇒ 整节塌下去。它是**粗筛**，不是判别：
        # 真正把这一处闸分开的还是 D10（只摘闸 ⇒ 恰好 1 条红）。记账照实测，不许为了"看着精确"
        # 把连带红删出预期 —— 那等于把 19 条真红当成噪声。
        ("D11 词表闸写反（ready 才禁用）", [(D10_A, D11_R)], [
            _name('词表读得到的时候「新建部署」'),
            _name('下拉里的选项个数 = 词表里'),
            _name('兑现得了的那几种是可选中的，'),
            _name('兑现不了的那几种在列表里是灰'),
            _name('每一种不执行的理由原样摆在这'),
            _name('没选过的时候默认落在服务器清'),
            _name('点一次「开始部署」只发一个 '),
            _name('成功那一下界面说的是「部署完'),
            _name('表里那一行的状态格写的是服务'),
            _name('方式那一格画的是中文标签（不'),
            _name('没记版本、没挂物化批次的那两'),
            _name('列表里那一行是读回来的，不是'),
            _name('点了部署之后运行时库里真的长'),
            _name('抽屉标题认领的是哪一行的账（'),
            _name('日志那一格里是服务器原话（界'),
            _name('关掉抽屉不带走那一行（表格还'),
            _name('词表读不到时「新建部署」收住'),
            _name('把词表换回来：同一颗按钮立刻'),
            _name('恢复之后再读一次：那一行回来'),
        ]),
        ("D12 词表读失败的横幅整块摘掉", [(D12_A, D12_R)], [
            _name('词表读不到时页面报的是「部署方式词表没有读到」'),
        ]),
        ("D13 空态退回通用「暂无数据」", [(DP_EMPTY_A, D13_R)], [
            _name('一条部署都没有时说的是'),
        ]),
        ("D14 空态不分读失败，恒说「还没有」", [(DP_EMPTY_A, D14_R)], [
            _name('部署记录读不到那一屏报的是'),
        ]),
        ("D15 「物化批次」整列摘掉", [(D15_A, D15_R)], [
            _name('部署页的表头逐格按列序画出来'),
            _name('没记版本、没挂物化批次的那两格画的是破折号'),
        ]),
        ("D16 抽屉标题不认领是哪一行的账", [(D16_A, D16_R)], [
            _name('抽屉标题认领的是哪一行的账'),
        ]),
        ("D17 日志格换成「成功」两个字", [(D17_A, D17_R)], [
            _name('日志那一格里是服务器原话'),
        ]),
        ("D18 「日志」那颗按钮永远禁用", [(D18_A, D18_R)], [
            _name('抽屉标题认领的是哪一行的账'),
            _name('日志那一格里是服务器原话'),
        ]),
        ("D19 「方式」列画裸编码", [(D19_A, D19_R)], [
            _name('方式那一格画的是中文标签'),
        ]),
        ("D20 挂载时多读一次列表", [(D20_IMPORT_A, D20_IMPORT_R), (D20_A, D20_R)], [
            _name('一次挂载只发一次部署列表请求'),
        ]),
        # 实测（run1 的 21_D21.log）三条：除了"只发一个 create""列表是读回来的"，
        # 「恢复之后再读一次」也跟着塌 —— 双发在库里多留了一行，收尾那一句读的已经不是同一份账。
        ("D21 点一次发两个 create", [(D21_A, D21_R)], [
            _name('点一次「开始部署」只发一个 '),
            _name('列表里那一行是读回来的，不是'),
            _name('恢复之后再读一次：那一行回来'),
        ]),
    ]


# ---- 未覆盖记账：名字逐字取自套件（由 _name 现扫），理由各自可复核 -------------------------------
# 「没有专属猎物注入」与「在注入轮里一条都没红过」是两件事，之前被我混成一格：run1 实测
# D11（把词表闸写反）连带红 16 条、D21 连带红 1 条，其中三条正是这里记过账的名字。
# 于是记账口径改成可机械查的一条：凡既进某支预期红集、又留在未覆盖账上的名字，理由里必须原样
# 写「连带红过」—— 少这四个字就是矛盾，体检不许过（也不许拿这句话把没牙的格子悄悄混进覆盖）。
def not_covered() -> dict[str, list[str]]:
    return {
        "(a) 夹具/收尾的读点：没有一支注入以它们为猎物，红了说明环境坏了而不是守卫坏了。"
        "实测这三条在 run1 的 22 轮里一条都没红过 —— 复算："
        "`for t in 节前对账 节前的词表 这一节的应用收掉了; do grep -c \"FAIL.*$t\" "
        "~/.cache/zlc73/browser_guard/*.log; done` 三跑全 0 命中": [
            _name('节前对账：这张物理表还不存在'),
            _name('节前的词表读得到'),
            _name('这一节的应用收掉了'),
        ],
        "(b) 结构上无专属猎物可打：当前词表的 executable 只有一种，`executable[0]` / 钉死 'HOT_LOAD' / "
        "取最后一种 画出来逐字节相同（等价变异）。牙在 vitest —— 那边的 fixture 是两种"
        "（复算：`grep -n 'BLUE_GREEN' src/views/admin/DeploymentsPage.test.tsx`）"
        "且钉的是原序。但它没有专属注入：现在它在 D11（词表闸写反、整屏塌）里连带红过，实测": [
            _name('没选过的时候默认落在服务器清单的第一种'),
        ],
        "(c) 要红它只能改**送出去**的那个 deployType，而写入口现在把非 executable 的拒成 "
        "success:false/400（09-27 13:2x 实测 curl：部署方式 [DOCKER] 服务器执行不了…当前可登记的只有 "
        "[HOT_LOAD]）⇒ 一次注入连着塌 toast/状态格/服务器行数等九条并带走整节，那是造新缺陷不是摘守卫。"
        "它同样在 D11 里连带红过，实测": [
            _name('点了部署之后运行时库里真的长出了那张表'),
        ],
        "(d) 清账那一句在共享的 `_scope.ts`（六个管理页共用、别的注入族的基线也读它），不在本支的"
        "被测文件里 —— 摘它是跨族改动，本支不许动别人的尺": [
            _name('读不到那一屏不留下上一份账的行'),
        ],
        "(e) 收尾那一句读的是「恢复之后再看一眼」，本族没有一支的猎物是它 —— 但它 D11、D21 两支里都"
        "连带红过（实测），说明它对「库里的账被写脏」确有反应。这份证据记在那两支的账上，"
        "不许再按它单开一格记分": [
            _name('恢复之后再读一次：那一行回来了'),
        ],
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
    """整棵 `src/` 的字节指纹 —— 判"注入进没进被测的树""还原回没回来"都只能读源码。

    不能拿产物当判据：同一棵 src 树在本机会构建出两套 `index-*.js`（缺陷 #69 量过，
    六次干净构建、两个名字、交替出现）⇒ 产物名在这一层不是身份，连"两次同名 ⇒ 同一份源码"
    这种反向推都不成立。这里直接沿用 `_mutlock` 那族里已被证过的写法，不再复述取证。
    """
    h = hashlib.md5()
    for p in sorted((UI / "src").rglob("*")):
        if p.is_file():
            h.update(str(p.relative_to(UI)).encode("utf-8"))
            h.update(p.read_bytes())
    return h.hexdigest()


def validate() -> int:
    """静态体检：锚点唯一、红集互不相同、名字逐字在分母里、分母全被登记。

    全在这一支的代价（21 轮 × ~4.5min）之前做 —— 一次改名/一个不唯一的锚点要等整轮
    build + 整轮浏览器才发现，那已经是十几分钟之后。
    """
    src = PAGE.read_text(encoding="utf-8")
    for anchor in (D1_A, D2_A, D3_A, D4_A, D5_A, D6_A, D7_A, D8_A, D9_A, D10_A, D12_A,
                   DP_EMPTY_A, D15_A, D16_A, D17_A, D18_A, D19_A, D20_A, D20_IMPORT_A, D21_A):
        n = src.count(anchor)
        if n != 1:
            print(f"!! anchor 在 DeploymentsPage.tsx 里出现 {n} 次（应为 1）: {anchor.strip()[:70]}")
            return 1

    names = section_names()
    if len(names) != len(set(names)):
        dup = sorted({n for n in names if names.count(n) > 1})
        print(f"!! 11x 段内有重名检查（红集合无法归因）: {dup}")
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
        for anchor, repl in edits:
            if src.count(anchor) != 1:
                print(f"!! {tag}: 锚点不唯一，注入会打到别处")
                return 1
            if anchor == repl:
                print(f"!! {tag}: 注入是空操作")
                return 1
        for name in expected:
            if name not in names:
                print(f"!! {tag}: 预期红集里的名字不在 11x 分母里: {name}")
                return 1
            if name == T_SECTION_CRASH:
                print(f"!! {tag}: 把「整节中途抛错」写成了预期红 —— 那一轮的其余读数全部作废")
                return 1
        key = tuple(sorted(expected))
        if key in sets:
            print(f"!! {tag} 与 {sets[key]} 的预期红集合相同 —— 那要么是不同的断言红在同一个标题下，"
                  "要么其中一支是重复检查（不许按两支记分）")
            return 1
        sets[key] = tag

    booked: dict[str, str] = {}
    for reason, listed in not_covered().items():
        for name in listed:
            if name not in names:
                print(f"!! 记账的名字不在 11x 分母里: {name}")
                return 1
            if name in booked:
                print(f"!! {name} 被记了两笔（{booked[name]} / {reason[:24]}）")
                return 1
            booked[name] = reason

    covered = {n for _, _, expected in runs() for n in expected}
    # 一个名字既可以「没有专属猎物注入」（记在未覆盖账上），又可以在别人的粗暴注入里连带红。
    # 这不是矛盾，但必须被写明 —— 没写明的就是账本自相矛盾，不许悄悄拿连带红当覆盖。
    for name in sorted(covered & set(booked)):
        reason = booked[name]
        if "连带红过" not in reason:
            print(f"!! {name} 既进了某支的预期红集、又被记为未覆盖，而理由里没写「连带红过」"
                  f"（{reason[:36]}…）—— 要么它真有专属猎物（从没覆盖账上划掉），"
                  "要么它只是连带红（理由里点明是哪一支）")
            return 1
    prey = covered - set(booked)
    orphans = [n for n in names if n not in covered and n not in booked and n != T_SECTION_CRASH]
    if orphans:
        print(f"!! {len(orphans)} 条检查既没被注入覆盖、也没记在未覆盖账上（新增检查要两边都登记）:")
        for one in orphans:
            print(f"   - {one}")
        return 1
    print(f"  静态体检通过：11x 分母 {len(names)} 条（扫自源码）、{len(runs())} 支注入、"
          f"有专属猎物的覆盖 {len(prey)} 条、无专属猎物但被别的注入连带红过 {len(covered & set(booked))} 条、"
          f"纯记账 {len(booked) - len(covered & set(booked))} 条、兜底标题 1 条")
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
    """另一轮 browser-e2e 正在跑时绝对不能开火：它不持锁（只是读产物），而我会把 .tsx 改成
    带缺陷的样子 —— 它的"全绿"就会变成我的注入的读数。"""
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
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    summary = re.search(r"=> PASS (\d+) / FAIL (\d+)", text)
    total = (int(summary.group(1)) + int(summary.group(2))) if summary else 0
    print(f"  {label}: PASS+FAIL {total} / 红 {len(failed)}")
    return failed, total


def foreign_titles(failed: list[str]) -> list[str]:
    """红在 11x 这一节之外 —— 我的注入只改 DeploymentsPage.tsx，而分母是扫 browser-e2e.mjs 得到的，
    所以「不属于这 29 条」是机械可判的。它说明这一轮被别的节（11w 的桩计数）污染了，
    而不是别的节有了缺陷 —— 别把它写成白名单，也别把它当成通过。"""
    mine = set(section_names())
    return [t for t in failed if t not in mine]


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if T_SECTION_CRASH in failed:
        print(f"  !! {label}: 整节中途抛错（{T_SECTION_CRASH} 红了）—— 这一轮的归因不成立，"
              "后面那些「没红」是「没跑到」")
        return 1
    foreign = foreign_titles(failed)
    if foreign:
        tag = label.split("_", 1)[1]
        print(f"  !! {label}: 这 {len(foreign)} 条红不属于 11x 这一节（它们来自别的节，逐条见上）—— "
              f"这一轮的归因不成立（不是那一节有缺陷，也不是这里加了白名单），"
              f"重跑：--only {tag}")
        for one in foreign:
            print(f"     · 外来红 {one}")
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
            print(f"⚠ 收窄跑：只点名的那几支（{only}）+ 恢复轮。这一跑的 RESULT 只证明改过的判据"
                  "在真 build/真浏览器轮次里跑得通，**不能**当「D1–D21 整族自证」的账。")

        for i, (tag, edits, expected) in picked:
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} ===")
            mutated = original
            for anchor, repl in edits:
                mutated = mutated.replace(anchor, repl, 1)
            PAGE.write_text(mutated, encoding="utf-8")
            try:
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
                    print("  !! DeploymentsPage.tsx 未恢复到原始内容")
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
            # 只打印，不判红：判"还原"的活由 src 指纹干（产物本来就会漂，相等几乎不发生 ⇒
            # 一条几乎不触发的判据是空跑，见文档判据 2）。漂移要看得见，只是不许当判据。
            print(f"  （信息）产物 {restored_bundle} != 基线 {base_bundle} —— 产物漂移，不作判据")
        failed, this_total = run_round("99_restored")
        if failed or this_total != total:
            print(f"  !! 恢复后仍有红或分母不符: {failed} ({this_total} vs {total})")
            bad += 1
    finally:
        PAGE.write_text(original, encoding="utf-8")
        if preview is not None:
            stop_preview(preview)

    covered = {n for _, _, expected in runs() for n in expected}
    booked = {n for v in not_covered().values() for n in v}
    prey = covered - booked
    collateral = covered & booked
    # 三堆必须正好铺满分母（外加整节兜底那一条）。这条加和是量具自己的账 ——
    # run2 之前它印的是「24 条覆盖 + 7 条记账」= 31，比 29 多出来的正是被重复计的那三条连带红。
    n_names = len(section_names())
    if len(prey) + len(collateral) + len(booked - covered) + 1 != n_names:
        print(f"\nRESULT: 记账的加和对不上分母（专属 {len(prey)} + 连带 {len(collateral)} + "
              f"纯记账 {len(booked - covered)} + 兜底 1 ≠ {n_names}）—— 有名字被重复计或漏计")
        return 1
    if bad:
        print(f"\nRESULT: {bad} problem(s)")
        return 1
    print(f"\nRESULT: {len(runs())} 支注入把 11x 的 {len(prey)} 条各自钉住一件事；"
          f"另 {len(collateral)} 条没有专属猎物、只在别人的粗暴注入里连带红过；"
          f"{len(booked - covered)} 条从没红过的格按未覆盖记账"
          f"（合计 {len(booked)} 条、{len(not_covered())} 类理由在文档与 not_covered() 里）")
    return 0


def selftest() -> int:
    """证「写完变异而 src 指纹没动」那条判据**有猎物**，不 build、不抢 preview 端口。
    与 `mutate_workflow_browser_guard.py --selftest` 同形（那支已把 A/B/C 三格证过），
    这一支只补一件事：D20 是**两个**锚点，少 import 那一个必须仍然让指纹动 ——
    否则两处编辑里有一处静默落空，那一轮就是基线的回声。
    """
    base = src_digest()
    page_before = PAGE.read_text(encoding="utf-8")
    bad = 0
    try:
        _tag, edits, _exp = [r for r in runs() if r[0].startswith("D20")][0]
        if len(edits) != 2:
            print(f"  !! D20 应当有两处编辑，实际 {len(edits)} —— 这一支的对照不成立")
            return 1
        half = page_before
        for anchor, repl in edits[:1]:
            half = half.replace(anchor, repl, 1)
        if half == page_before:
            print("  !! D20 的第一处编辑（import）没命中 —— 锚点已经过期")
            return 1
        PAGE.write_text(half, encoding="utf-8")
        if src_digest() == base:
            print("  !! 改了一处 src 而指纹没动 —— 这把尺没有猎物")
            bad += 1
        else:
            print("  OK 只下 D20 的一半（只补 import）：指纹跟着动 ⇒ 多锚点注入里"
                  "「有一处静默落空」不会被读成「这一支已经量过」")
        missed = page_before.replace("/* 源码里没有这一行 */", "", 1)
        PAGE.write_text(missed, encoding="utf-8")
        if src_digest() != base:
            print("  !! 写回一份逐字节没变的源码，指纹却动了 —— 判据在量别的东西")
            bad += 1
        else:
            print("  OK 锚点落空的变异（replace 静默 no-op）：写盘后指纹不动 ⇒ main() 那条红打得出")
    finally:
        PAGE.write_text(page_before, encoding="utf-8")
    if PAGE.read_text(encoding="utf-8") != page_before or src_digest() != base:
        print("  !! selftest 收场后 PAGE/指纹没回到基线")
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
