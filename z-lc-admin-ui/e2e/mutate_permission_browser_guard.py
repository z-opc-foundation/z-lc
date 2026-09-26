#!/usr/bin/env python3
"""权限矩阵那一页的**浏览器层**注入自证（`browser-e2e.mjs` 第 11e 段那 39 条具名检查）。

跑法（在 z-lc-admin-ui 下，需要 18090 的后端在跑；5274 要让出来给它自己起 preview）：

    python3 e2e/mutate_permission_browser_guard.py

为什么单开一支：#49 这条缺陷的名字就是"权限矩阵页在真浏览器层零断言"。vitest 那 12 条
（`mutate_permission_matrix_guard.py` 的 B 族）钉的是"给定这份行清单，格子该亮哪一盏"，
而它看不见真后端到底答允许还是拒绝 —— jsdom 里 `/permission/check` 是一个恒真 mock。
两边各说一套时只有这一节能发现。所以这一支的分母是**真构建 + 真浏览器 + 真后端**里 11e 那 39 条检查的名字
（其中一条是整节的兜底哨兵），一条不多一条不少 —— 分母是从 browser-e2e.mjs 里扫出来的，不是抄的。

九支注入，每支摘掉一句不同的保证，预期红集合互不相同（脚本开局会核这一条）：

  M1 格子口径退化成"出现过就算有"  → 应用级那一档被实体级授权点亮（4 条红）
  M2 前端词表换了顺序            → 矩阵列序与后端那份 ALL 不再一致（1 条红）
  M3 「来自整个应用」没有证据就写  → 应用级那一格的来源话说没了（1 条红）
  M4 手动加的角色不并进矩阵(#50)  → 新角色那一行压根不出现（3 条红，后面整节没跑到）
  M5 回收之后不重读              → 界面说"已回收"而矩阵与清单都还亮着（5 条红）
  M6 data-granted 恒为 1         → 钩子与真值脱钩，整节逐格对账一起塌（大面红）
  M7 NULL 那一档不说成 APP_WIDE  → 清单里"整个应用"的身份钩子少了 NULL 那一支（1 条红）
  M8 实体过滤器换了不生效         → 换档读数全部落在上一档（4 条红）
  M9 「加入矩阵」在没名字时点得动  → 一行空角色塞进矩阵（1 条红，B18 在浏览器层的孪生）

⚠ 有**十八条**这一族没有注入，按未覆盖记账 —— 而且这条账是**机器核的**，不是文档里的一句话：
`collected_names()` 从 browser-e2e.mjs 里把 11e 那一段的 `check('…')` 名字全扫出来，
开局要求"被某支注入认领的 ∪ NOT_COVERED 认领的 == 扫到的全集"，
两边都占上的（重复记账）和两边都不占的（漏记）各点名拒跑。
所以这一节新增一条检查而没人记账时，这一支会**拒绝开跑**而不是悄悄少测一条。
每条为什么记成未覆盖，理由写在 NOT_COVERED 的 value 里（六类：真值源在后端 / 前置清场与种子
是自己写的 / 驱动自己的守卫 / 要打红只能造稻草人 / 那一刀已由 vitest 层的 B 族或别的守卫支插过 /
失效形状是"新增一个行为"而不是"摘掉一句保证"）。

每支注入都要重新 build 并**核对产物 hash 真的变了**：hash 没变说明这一支改动根本没进包
（改的是注释、或被 memo 依赖挡掉），那一轮读到的"没红"就不是产品的事。上一支 B 族里
B13 就是这么一支等价变异（`useMemo` 的依赖还写着 `allRows`），在这里同样要挡。

⚠ 还有一类失败长的是另一副样子：**注入让产品编译不过**（M8 第一版 `onChange={() => undefined}`
被 `tsc --noEmit` 的 TS6133 打掉，那一轮的 M8/M9/还原轮全没跑）。现在一支没能构建只记它自己
一条失败并继续下一支，而"没构建成功"永远不会被读成"没红"。

⚠ 第二类：被测介质半路没了。run2 的 M7 跑到一半，18090 那个 JVM 死了（H2 是 `jdbc:h2:mem`，
进程一死库就没了），M8/M9/99_restored 三轮各留下一句 `[ECONNREFUSED]` 和 **0 条具名检查**，
而 node 的退出码仍是 0 —— 判据当时说的是"预期红没红 → 注入是等价变异"，把三轮环境事故
读成了三轮产品缺陷。现在每轮开跑前 `backend_up()`（200 + `"status":"UP"` 两半都要），
每轮收尾核 `ROUND_MIN_CHECKS` 地板：**空输入 FATAL，不许读成满分。**
"""

import hashlib
import os
import re
import signal
import socket
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

import _mutlock

UI = Path(__file__).resolve().parents[1]
PAGE = UI / "src/views/admin/PermissionsPage.tsx"
KEYS_TS = UI / "src/api/permission.ts"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
SRC = UI / "src"
PORT = 5274
API_PORT = 18090
HEALTH_URL = f"http://127.0.0.1:{API_PORT}/api/lc/health"
# 一轮的地板：整套门禁实测 222 条具名检查。地板放这么低（而不是 222）只为一件事 ——
# 后端死了/页面没起来时会留下 0~几十条，那必须 FATAL，而不是"零红 = 全绿"。
ROUND_MIN_CHECKS = 200
BUILD_TIMEOUT = 900
# 一轮完整的 browser-e2e（今天 load ~2 时实测 ~600s/轮）；上限不是预期，超时会抛，
# 那一轮记为"无结论"而不是"通过"。
ROUND_TIMEOUT = 2400
FAIL_DETAIL_SEP = "   << "

# 11e 那 39 条里被这九支认领的名字（其余在 NOT_COVERED 里按未覆盖记账，开局机器核账）。
A_APP_WIDE_ALIGN = '未选实体那一档：界面点亮的那几格 == /permission/check 答允许的那几格（一格不符就点名）'
A_BTN_TRACKS = '已授予的格子按钮是「已授予」且点亮状态与它一致（按钮与状态各说一套 = 再点一次会重复授）'
A_HEADERS = '矩阵的列就是后端那份动词表，且顺序一致（列序是 PermissionKeys 的序，不是 Object.keys 的运气）'
A_NOT_APP_WIDE = 'task 上的单实体授权不把「整个应用」那格点亮（真后端那里答的也是拒绝）'
A_OTHER_ENTITIES = '那一格同时说清「另有 1 个实体单独授予」（不点亮又不说，用户会以为这条授权丢了）'
A_FROM_APP_WIDE = '应用级那一格说出它的来源是「来自整个应用」（与单实体授予是两种口径，混说就没法核对）'
A_FRESH_ROW = '新角色进了矩阵，且行身份是 trim 过的名字（带空格的那一行和它不是同一个角色）'
A_FRESH_CLICKABLE = '新那一行五格全是"能点的授予" —— 有行却一格都点不动，入口就是画的'
A_FRESH_CHECK = '新角色的那一格从此与 /check 同答（界面亮 = 后端允许，两边任一处不跟都是红）'
A_SELF_REVOKE = '界面授的那一条，界面也收得回来：库回到种下的那两条、格子灭了、/check 对这个新角色答拒绝'
A_SCOPE_TOOK = '换档真的生效了：组件状态和过滤框上印的都是那个实体（只换显示不换状态，后面读的就是上一档的格子）'
A_TASK_ALIGN = '选 task 之后：应用级授权覆盖它（Auditor/VIEW 亮），而 task 的那条只点亮 DELETE'
A_NOT_ECHO = '格子是真值口径变化的受害者而不是文案的复读（同一格在两档下状态不同，说明它读的是行清单不是提示）'
A_SCOPED_HINT = 'task 这一档里那条单实体授权说「该实体单独授予」（与「来自整个应用」是两句话）'
A_SCOPE_CELL = '应用级那一行的作用范围格渲染成「整个应用」，且 data-scope 钉的是 APP_WIDE（NULL 那一档）'
A_ROW_GONE = '回收之后那一行从清单里真的消失（不是只弹了一句"已回收"）'
A_LIST_ROWS = '清单里两行都在，且行身份就是库里那两个 id（只数个数不比对 id 的话，按角色当 key 也照样绿）'
A_NO_STALE = '回收掉唯一一条授权后那一行不再冒充"还授着"（矩阵里读不到 Auditor 这一行）'
A_REDO_ENTRY = '但这一页给得出再授的入口：把 Auditor 加回矩阵，那一格是「授予」并且点得动'
A_EMPTY_LOCKED = '「加入矩阵」在角色名还没填的时候是 disabled（点得动就会往矩阵里塞一行空角色）'
SENTINEL = '权限矩阵与 /check 对齐（#49 浏览器层）'

GRANTED_ATTR = "                      data-granted={granted ? '1' : '0'}"
PENDING_LINE = "    const pending = addedRoles.filter((role) => !seen.includes(role));"
REVOKE_RELOAD = "            reload();"
DISABLED_ANCHOR = "          disabled={!newRole.trim()}"

# tag -> (path, anchor, repl, 预期红集合, 这一支摘掉的是哪句保证)
RUNS = [
    ("M1 格子退化成「出现过就算有」", PAGE,
     "      granted: mine.some((row) => (scope ? coversScope(row, scope) : isAppWide(row))),",
     "      granted: mine.length > 0,",
     [A_APP_WIDE_ALIGN, A_NOT_APP_WIDE, A_OTHER_ENTITIES, A_NOT_ECHO],
     "一个实体的授权把「整个应用」那一格点亮，而真后端对应用级答的是拒绝"),
    ("M2 前端词表换序", KEYS_TS,
     "export const PERMISSION_KEYS = ['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT'] as const;",
     "export const PERMISSION_KEYS = ['VIEW', 'CREATE', 'UPDATE', 'EXPORT', 'DELETE'] as const;",
     [A_HEADERS],
     "列序不再等于后端 PermissionKeys.ALL 的序，两边一漂就是两副面孔"),
    ("M3 「来自整个应用」没有证据就写", PAGE,
     "      fromAppWide: mine.some(isAppWide),",
     "      fromAppWide: false,",
     [A_FROM_APP_WIDE],
     "亮着的格子说不清它是应用级授的还是这个实体单独授的"),
    ("M4 手动加的角色不并进矩阵（#50 复发）", PAGE,
     PENDING_LINE, "    const pending: string[] = [];",
     [A_FRESH_ROW, A_FRESH_CLICKABLE, SENTINEL],
     "有输入框、没有入口：新角色进不了矩阵，第一条权限还是授不出去"),
    ("M5 回收之后不重读", PAGE,
     REVOKE_RELOAD, "            void 0;",
     # 实测比第一版预测多红两条：不 reload 留下的是**整个清单与整张矩阵**的上一秒，
     # 凡是读这两处的都跟着塌 —— `清单里两行都在…`(读数 ["39","41","40"]，多出的 41 就是
     # 刚被收掉那一行) 与 `但这一页给得出再授的入口…`(读数是 `已授予来自整个应用`，
     # 而库里 Auditor 那条已经没有了)。两条都有据，改的是账不是断言。
     [A_SELF_REVOKE, A_LIST_ROWS, A_ROW_GONE, A_NO_STALE, A_REDO_ENTRY],
     "弹了「已回收」，矩阵和清单却还停在回收前那一秒"),
    ("M6 data-granted 恒亮", PAGE,
     GRANTED_ATTR, "                      data-granted=\"1\"",
     [A_APP_WIDE_ALIGN, A_BTN_TRACKS, A_NOT_APP_WIDE, A_FRESH_CLICKABLE, A_FRESH_CHECK,
      A_SELF_REVOKE, A_TASK_ALIGN, A_NOT_ECHO, A_REDO_ENTRY],
     "钩子与真值脱钩 —— 这一族全部读数都建立在它上面，所以是大面红"),
    ("M7 NULL 那一档不带身份", PAGE,
     "        <span data-testid=\"perm-scope\" data-scope={value || 'APP_WIDE'}>",
     "        <span data-testid=\"perm-scope\" data-scope={value || ''}>",
     [A_SCOPE_CELL],
     "清单里那一句「整个应用」没有库里的 NULL 作依据"),
    ("M8 实体过滤器换了不生效", PAGE,
     "            onChange={(value) => setEntityCode(value ?? '')}",
     # 第一版写的是 `onChange={() => undefined}`（最直白的"忘了接线"），它压根没跑到浏览器：
     # `npm run build` 的 `tsc --noEmit` 先报 TS6133 `'setEntityCode' is declared but its value
     # is never read` 把整支守卫打断。换成"写回同一个值"—— 同样是不生效，且是唯一被
     # noUnusedLocals 放过的形状（stale closure 里把上一个 entityCode 又塞回去，
     # React 见到同一个值直接跳过重渲染，界面就是"换档只换个显示"）。
     # 第二版跑出来这一条预期红却没红（run3 11:35）：读的是过滤框上那行字，而那行字在
     # `value` 为 undefined 时归 rc-select 内部状态管，状态没换它照样换显示 —— 注入穿着
     # 绿跑过去了（另三条靠格子抓到了）。所以产品侧补了 data-entity 这个状态读数面、
     # 断言改成"状态与显示同一次读数里都要对"，这一支重新有据。
     "            onChange={() => setEntityCode(entityCode)}",
     [A_SCOPE_TOOK, A_TASK_ALIGN, A_NOT_ECHO, A_SCOPED_HINT],
     "换档只换了下拉框的显示，组件状态还留在上一档"),
    ("M9 「加入矩阵」在没名字时点得动", PAGE,
     DISABLED_ANCHOR, "          disabled={false}",
     [A_EMPTY_LOCKED],
     "点得动就会往矩阵里塞一行空角色 —— 一行没有身份的格子，B18 在浏览器层的孪生"),
]

# 11e 那 39 条里**没有**注入认领的，逐条记为未覆盖，并写下为什么摘不掉。
# 这份账由 `account_check()` 机器核：每条名字必须真的在 11e 里、且不与某支注入的预期红重叠。
NOT_COVERED = {
    # —— 前置清场与种子：这一段的数据是自己用 fetch 直接写库的，摘产品源码红不到它 ——
    "这个应用里确实有 task 那一栏可授（矩阵要按实体分档，空实体会整张不渲染）":
        "读的是 /admin/app/entity/list 本身，那一刀在 `mutate_workspace_entity_guards.py`（同一接口的侧栏那层）",
    '开跑之前这个应用的授权清单是空的（不清零就没法说"下面那两行是我种的"）':
        "本节自己的清场账：失效形状是上一轮留了数据，不是产品少写一行",
    "种下两条授权：一条整个应用（entity_code 为 NULL）、一条只在 task 上":
        "写入走 grantApi（直连后端的 fetch），前端源码摘哪一行都不会让它不落地",
    '库里那两行确实是那个形状（应用级那行的 entityCode 真是 NULL，不是空串也不是 "null"）':
        "读回来的形状，真值源在后端：那一刀由 #48 的 `mutate_permission_deployed_guard.py` 插",
    "后端这一轮确实答的是一真一真一假一真一假一假（对照集若全是 null，下面的对齐就是空转）":
        "对照集来自 /permission/check 本身，能红它的是后端判定，不是这一页",
    # —— 驱动自己的守卫：失效形状是这段测试写错，不是产品写错 ——
    "换档前只有一个下拉框开着（两个同时开着，这一次读数会把两份选择项混成一份）":
        "数的是本文件自己开着的下拉层；产品里没有一行能摘它",
    "换档的选择项里点名了那个实体（找不到名字就该红，而不是按位置猜一项）":
        "驱动的选择器守卫（按名不用按位）；要红它得让实体名不再进下拉，那是 meta 读取那一族",
    # —— 与已认领的那几条同因，再插一刀只是换个写法 ——
    '矩阵的行按角色身份定位，两行都在（读不到行就是"整张表空着也绿"的那个形状）':
        "要打红只能把行集整个清空，那是一根稻草人（矩阵连渲染都不渲染，M4 已用真实形状钉过行集）",
    "填了名字（还带着两端空格）就能提交":
        "它的反面（按钮永远点不动）红的是 M4 那一片（新行不出现 + 哨兵），单独立一支只是给同一个因换个写法",
    "点下去真的授出去了：库里有这个角色的一行、且是应用级（entityCode 为 NULL）":
        "与 A_FRESH_CHECK 同一支 payload 的两种读法；摘 `entityCode: undefined` 那把刀在 B3",
    "同一个角色不许多加一行（两行会给出同一格互相矛盾的答案）":
        "去重那一行在 vitest 层由 B16 插；这一层要红它得跳过 setAddedRoles 的判重，M4 已覆盖同一条路径",
    "task 那一行点名 task，不被上面那句「整个应用」吞掉":
        "与 M7 互补但同栏：M7 摘的是 NULL 那一支，非 NULL 那一支要红得把 value 整个不吃掉，那会连带让 A_SCOPE_TOOK 一起塌",
    # —— 真值源不在这一层 ——
    "库里也确实少了那一条，剩下的是 task 那一行（界面少了而库没少 = 谎报成功）":
        "读的是 /list 的返回，摘前端任何一行都不会让库少删；那是 API 层 464 条的地盘",
    "回收之后 /check 改口答拒绝（矩阵那格之前亮着，判定的权威源必须跟着变）":
        "同上：/check 的口径由后端判定守卫插刀",
    "回收那一下界面说的是「已回收」，且没有一句失败文案（只断言\"没报错\"对着空 toast 也会打绿灯）":
        "它的失效形状是\"后端回 400 也报成功\"（B4 用 mock 插的刀）；真后端这一轮收的是自己刚种下的行，必然收得动",
    "读失败时页面报的是读不到，而不是「还没有权限配置」（前者让人去修接口）":
        "摘的是 `_scope.ts` 的 ListState 与各家文案，那是 `mutate_degradation_guards.py` 的地盘",
    '读失败也不许留下"格子全没授过"的假矩阵（那种页面点一下就是真的写一次授权）':
        "同上：要红它得让读失败不发生（或让矩阵不吃 error），后者是新增行为而不是摘保证",
    # —— 收的是本节的账，不是产品的账 ——
    "这一节自己种的授权全部回收干净（留着会让下一轮的两行前提红，而且没人知道是谁留的）":
        "它不算空跑：第 5 轮就是它抓到本节漏收 Clerk/task（真写进库了）。只是它的\"注入者\"只能是这段测试自己",
}

# 与 `~/.cache` 同例：/tmp 会被同一台机器上别的会话扫掉（本项目已复现两次"日志失踪"）。
LOG_DIR = Path.home() / ".cache/zlc49/mut_browser_logs"
LOG_DIR.mkdir(parents=True, exist_ok=True)


def _expected_of(run):
    """取一支注入的预期红集合 —— 必须逐个具名解包（位置绑定错了不报错，只会让守卫空转）。"""
    tag, path, anchor, repl, expected, note = run  # noqa: F841 — 具名解包就是这条守卫本身
    return tuple(sorted(expected))


SECTION_HEAD = "/* ---- 11e."
SECTION_TAIL = "/* ---- 12."
CHECK_NAME = re.compile(r"^\s*check\(\s*'([^']+)'")


def collected_names() -> list[str]:
    """从 browser-e2e.mjs 里**扫出** 11e 那一段具名检查的全集，而不是抄一个数字。

    为什么扫而不写死：写死的分母（我第一版文档里的"31 条"）会在下一次加检查之后变成一句
    假话，而且是一句**朝着"覆盖更多"的方向**假的话。扫出来的分母只会让记账在新增检查时
    当场拒跑（"这条既没人认领也没记账"）。
    """
    lines = E2E.read_text(encoding="utf-8").split("\n")
    head = [i for i, ln in enumerate(lines) if ln.strip().startswith(SECTION_HEAD)]
    tail = [i for i, ln in enumerate(lines) if ln.strip().startswith(SECTION_TAIL)]
    if len(head) != 1 or len(tail) != 1 or tail[0] <= head[0]:
        raise SystemExit(f"11e 那一段的边界扫不出来（head={head} tail={tail}）—— "
                         "分母是扫出来的，边界一旦不唯一就没有分母可言，这一支不能跑")
    names = [m.group(1) for ln in lines[head[0]:tail[0]] if (m := CHECK_NAME.match(ln))]
    if len(names) < 20:
        raise SystemExit(f"11e 只扫到 {len(names)} 条具名检查（<< 预期的量级）→ "
                         "要么是 `check(` 的写法变了（名字不再在同一行的单引号里），要么整段被搬走；"
                         "两种都不能让注入自证继续跑")
    return names


def account_check(names: list[str]) -> int:
    """核账：认领的 ∪ 记账未覆盖的 == 扫到的全集，且两边不重叠。返回 0/1（1 = 拒跑）。"""
    claimed = {n for r in RUNS for n in _expected_of(r)}
    seen = set(names)
    dup = sorted({n for n in names if names.count(n) > 1})
    phantom = sorted(claimed - seen)
    unaccounted = sorted(seen - claimed - set(NOT_COVERED))
    double = sorted(claimed & set(NOT_COVERED))
    stale = sorted(set(NOT_COVERED) - seen)
    bad = 0
    for label, items in (("11e 里有重名检查", dup), ("某支注入认领的名字在 11e 里根本不存在", phantom),
                         ("既没被注入认领、也没记成未覆盖", unaccounted),
                         ("两边都占上（重复记账）", double),
                         ("NOT_COVERED 记的名字在 11e 里找不到（大概是改过名）", stale)):
        if items:
            bad = 1
            print(f"!! {label}: {len(items)} 条")
            for name in items:
                print(f"     - {name}")
    if bad:
        print("!! 账不平就别跑：分母是扫出来的，任何一处对不上都意味着接下来那 9 轮读到的"
              "\"没红\"不一定等于\"这条检查没事\"")
        return 1
    print(f"  账平：11e 扫到 {len(names)} 条 = 注入认领 {len(claimed)} 条 + 未覆盖记账 {len(NOT_COVERED)} 条")
    return 0


def port_busy() -> bool:
    for host in ("127.0.0.1", "::1"):
        family = socket.AF_INET if ":" not in host else socket.AF_INET6
        with socket.socket(family, socket.SOCK_STREAM) as sock:
            sock.settimeout(0.4)
            if sock.connect_ex((host, PORT)) == 0:
                return True
    return False


def backend_up() -> bool:
    """后端活着 = HTTP 200 **且** body 里有 `"status":"UP"`。

    两半都要：`curl`/urllib 对 404 也照样连得上（本项目在 18099 上把**别的服务**的响应当成
    自己的过），而"连得上但没有 health"同样不能证明 18090 上是那个 fat jar。
    """
    try:
        with urllib.request.urlopen(HEALTH_URL, timeout=5) as resp:
            return resp.status == 200 and b'"status":"UP"' in resp.read()
    except Exception:  # noqa: BLE001 — 连不上/超时/非 200 全都是"不在"，不区分
        return False


def bundle_name() -> str:
    html = DIST_INDEX.read_text(encoding="utf-8") if DIST_INDEX.exists() else ""
    match = re.search(r'assets/(index-[^"\']+\.js)', html)
    return match.group(1) if match else "<dist/index.html 里没有 index-*.js>"


def src_digest() -> dict[str, str]:
    """src 下每个文件的字节 md5（按相对路径排序）。这是"还原干净"唯一可靠的尺。

    产物文件名 hash 不能当判据 —— 今天实测（~/.cache/zlc49/hashset{A,B}.txt）：同一棵树
    （前后 md5 钉死为 4029d264…／70e41dd6…）连跑两次 `npm run build`，六个 chunk 的
    **内容字节**全部不同（antd 差 41 字节、pro 差 2 字节），三次跑出两套 index-*.js 名字。
    也就是说 rollup 的输出在本机不逐字节可复现：名字不同推不出源码不同（run3 就是被这一条
    冤枉成"还原没还原干净"），而名字相同能推出内容相同（单向，仍留着当"注入没进包"的判据）。
    """
    return {str(p.relative_to(SRC)): hashlib.md5(p.read_bytes()).hexdigest()
            for p in sorted(SRC.rglob("*")) if p.is_file()}


def build(label: str, strict: bool = True) -> str:
    proc = subprocess.run(["npm", "run", "build"], cwd=UI,
                          capture_output=True, text=True, timeout=BUILD_TIMEOUT)
    (LOG_DIR / f"{label}_build.log").write_text(proc.stdout + proc.stderr, encoding="utf-8")
    if proc.returncode != 0:
        if strict:
            raise SystemExit(f"build 失败（{label}），见 {LOG_DIR / (label + '_build.log')}")
        # 注入把产品写成编译不过，是这一支自己的事，不该让后面几支 + 还原轮（~20 分钟）陪葬 ——
        # M8 的第一版就死在这里（`tsc --noEmit` 报 TS6133），整支守卫当轮作废。
        # 所以：记一条失败、继续下一支，但**绝不**把"没能构建"算成"没红也没事"。
        print(f"  !! {label}: 注入没能通过 build（这一支没有结论，不是通过），"
              f"见 {LOG_DIR / (label + '_build.log')}")
        return ""
    return bundle_name()


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)],
                            cwd=UI, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    for _ in range(240):
        if port_busy():
            return proc
        time.sleep(0.5)
    os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
    raise SystemExit(f"preview 起不来（:{PORT} 120s 内没在听）")


def stop_preview(proc: subprocess.Popen) -> None:
    try:
        os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
        proc.wait(timeout=20)
    except (ProcessLookupError, subprocess.TimeoutExpired):
        try:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
        except ProcessLookupError:
            pass


def run_round(label: str) -> list[str]:
    # 后端不在了 ⇒ 一轮跑下来 0 条具名检查、退出码却是 0/1。上一轮 M8/M9/99_restored 就是这样
    # 死掉的（18090 的 JVM 在 M7 中途没了）：判据把"预期红没红"归因成"注入是等价变异"，
    # 而真相是那三轮压根没跑到页面。**空输入必须 FATAL，不能读成满分。**
    if not backend_up():
        raise SystemExit(f"{label}: 开跑前 {HEALTH_URL} 没有答 200 + \"status\":\"UP\" —— "
                         "后端不在了，这一轮和后面所有轮都没有结论；先把 18090 起回来再跑")
    try:
        proc = subprocess.run(["node", str(E2E)], cwd=UI, capture_output=True, text=True,
                              timeout=ROUND_TIMEOUT, env={**os.environ, "E2E_REPEATS": "1"})
    except subprocess.TimeoutExpired:
        raise SystemExit(f"{label}: browser-e2e 一轮超过 {ROUND_TIMEOUT}s 没收线 → "
                         "这一轮没有结论（不许当成通过），先看机器是否被占满")
    text = proc.stdout + proc.stderr
    keep = LOG_DIR / f"{label}.log"
    keep.write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {keep}")
        return [f"<build-guard:{label}>"]
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，见 {keep}")
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    passed = len(re.findall(r"^  PASS  ", text, flags=re.M))
    total = passed + len(failed)
    if total < ROUND_MIN_CHECKS:
        raise SystemExit(f"{label}: 这一轮只留下 {total} 条具名检查（地板 {ROUND_MIN_CHECKS}）→ "
                         f"一轮没跑到东西不等于一轮全绿，见 {keep}")
    print(f"  {label}: PASS {passed} / FAIL {len(failed)}（共 {total} 条，log: {keep}）")
    return failed


def judge(label: str, expected: list[str], failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    if not hard and not extra:
        print(f"  OK  {label}: 预期 {len(expected)} 条全红，无一条连带红")
        return 0
    if hard:
        print(f"  !! {label}: 预期变红却没红（这几条浏览器检查是空的，或注入是等价变异）: {hard}")
    if extra:
        print(f"  !! {label}: 出现预期之外的红 {extra} —— 不许加白名单了事，先拿 "
              f"{LOG_DIR}/<这一支的编号_Mx>.log 证明这一处注入换掉了哪一支断言，再改预期")
    return 1


def main() -> int:
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              "\n   先确认那是谁（`lsof -nP -iTCP:{PORT} -sTCP:LISTEN`），是自己这一支的量具再停。")
        return 2

    originals = {path: path.read_text(encoding="utf-8") for path in (PAGE, KEYS_TS)}
    for run in RUNS:
        tag, path, anchor, repl, expected, note = run
        n = originals[path].count(anchor)
        if n != 1:
            print(f"!! anchor 在 {path.name} 里出现 {n} 次（{tag}），注入无法定位")
            return 2
        if anchor == repl:
            print(f"!! {tag}: 注入是空操作")
            return 2

    sets = [_expected_of(r) for r in RUNS]
    if len(set(sets)) != len(sets):
        print("!! 有两支注入的预期红集合相同 —— 那要么是不同的断言红在同一个标题下（要去核文本），"
              "要么其中一支是重复检查，先别跑")
        return 2

    # 分母是**扫**出来的：11e 里每一条具名检查要么被某支注入认领、要么记在 NOT_COVERED 里，
    # 两边都占或两边都不占都拒跑。不查这一条的话，一次改名要等整轮 build + 整轮浏览器
    # （今天这台机器十几分钟）才被报成"预期变红却没红"，而那句话会把"检查被改名了"
    # 误读成"我的检查是空的"。
    section_names = collected_names()
    if account_check(section_names):
        return 2

    bad = 0
    preview = None
    # MUT_FILTER=M8 只跑那一支：改一条断言之后要验"它到底咬不咬"，等不到整轮二十分钟。
    # 部分轮的覆盖账不作数（下面收口那句话会点名它是筛过的），别把它当成整轮绿。
    wanted = os.environ.get("MUT_FILTER", "").strip()
    runs = RUNS
    if wanted:
        runs = [r for r in RUNS if wanted in r[0]]
        if not runs:
            print(f"!! MUT_FILTER={wanted} 没点到任何一支注入（现有：{[r[0].split()[0] for r in RUNS]}）")
            return 2
        print(f"!! 部分轮：只跑 {wanted}（{len(runs)}/{len(RUNS)} 支），这一轮的覆盖账不作数")
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===")
        baseline_src = src_digest()
        print(f"    src 指纹：{len(baseline_src)} 个文件，"
              f"{hashlib.md5(''.join(f'{k}:{v}' for k, v in sorted(baseline_src.items())).encode()).hexdigest()[:12]}")
        baseline_hash = build("00_baseline")
        print(f"    built: {baseline_hash}")
        preview = start_preview()
        if run_round("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for i, (tag, path, anchor, repl, expected, note) in enumerate(RUNS, start=1):
            if tag not in [r[0] for r in runs]:
                continue  # 编号按整轮算，别因为筛过就把 08_M8 印成 01_M8
            label = f"{i:02d}_{tag.split()[0]}"
            print(f"\n=== {tag} —— 摘掉的是：{note} ===")
            mutated = originals[path].replace(anchor, repl, 1)
            path.write_text(mutated, encoding="utf-8")
            try:
                got_hash = build(label, strict=False)
                if not got_hash:
                    bad += 1
                    continue
                print(f"    built: {got_hash}")
                if got_hash == baseline_hash:
                    # 单向判据：名字相同 => 内容相同 => 这一刀没进包。名字不同推不出任何东西
                    # （见 src_digest 的 docstring），所以"进了包"这句话由下面的预期红集来说，
                    # 不由这一行说。
                    print(f"  !! {tag}: 产物 hash 与基线相同 —— 这一支注入根本没进包"
                          "（等价变异 / 只改了注释），它测不到任何东西")
                    bad += 1
                    continue
                bad += judge(tag, expected, run_round(label))
            finally:
                if path.read_text(encoding="utf-8") != mutated:
                    # 我写进去的字节不是我读回来的字节 => 这一轮中间有人（另一个会话/另一个代理）
                    # 改了同一个文件。这一支的红不能归因给注入，而且下面 `originals` 那一句
                    # 会把别人的改动覆盖掉 —— 先把话说明白，别让它变成一次无声的丢工。
                    print(f"  !! {tag}: {path.name} 在本轮中间被外部改过（不是我写的那份）—— "
                          f"这一支不能归因，且还原会覆盖那次的改动，先去查是谁写的")
                    bad += 1
                path.write_text(originals[path], encoding="utf-8")
                if path.read_text(encoding="utf-8") != originals[path]:
                    print(f"  !! {path.name} 未恢复到原始内容")
                    bad += 1

        print("\n=== 恢复后复跑 ===")
        after = src_digest()
        drift = sorted(k for k in set(baseline_src) | set(after) if baseline_src.get(k) != after.get(k))
        restored_hash = build("99_restored")
        print(f"    built: {restored_hash}")
        if drift:
            mine = [k for k in drift if (SRC / k) in (PAGE, KEYS_TS)]
            print(f"  !! 恢复后 src 指纹与基线不同 —— 变的是 {drift}；"
                  + ("其中 " + ", ".join(mine) + " 是我注入的那两个文件，是我没还原干净"
                     if mine else
                     "都不是我注入的文件 ⇒ 别人在这二十分钟里改了这棵树，"
                     "本轮其余结论建立在两份不同的源码上，要重跑"))
            bad += 1
        if (failed := run_round("99_restored")):
            print(f"  !! 恢复后仍有红: {failed}")
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    # 结尾那三个数一律算出来，不手写：手写的"31 条"在我加了一条检查之后就是一句朝
    # "覆盖更多"方向假的话（这一支的整条理由就是不许有那种话）。
    claimed = {n for run in runs for n in _expected_of(run)}
    verdict = (f"{'⚠ 部分轮（MUT_FILTER=' + wanted + '，只 ' + str(len(runs)) + '/' + str(len(RUNS)) + ' 支，整轮覆盖账不作数）：' if wanted else ''}"
               f"11e 扫到 {len(section_names)} 条：{len(claimed)} 条各有注入钉住自己那一句、"
               f"无连带红；另 {len(NOT_COVERED)} 条按未覆盖记账（理由在 NOT_COVERED 里逐条具名）")
    print(f"\n{'FAILED: ' + str(bad) if bad else verdict}")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
