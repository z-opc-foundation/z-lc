#!/usr/bin/env python3
"""同轮注入缺陷自证：设计器 provision 四个状态的前端半边（缺陷 #43 + #47）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_provision_report_guard.py
    python3 e2e/mutate_provision_report_guard.py --only M1,M13   # 只重跑两支，收线会打 PARTIAL

守的是 `src/views/designer/DesignerProvision.test.tsx` 那 15 条用例，被改的接线全在
`DesignerPage.tsx` 的 `provision()` / `provisionAll()` / 「未建成」横幅 / DDL 面板 Tag。

为什么这一支必须单独开：#43 的后端半边已经改成逐项回 status（`provisionVerifiesColumns…`
那四条打在真 H2 + 真 HTTP 上），但**前端拿到三态之后仍然可以把它读成一句成功**。
旧界面就是这么干的：`provision()` 不抛异常就弹「物理表已 provision」，
`provisionAll()` 弹 `已 provision ${Object.keys(result).length} 张表` —— 那个 Map 每个实体
一个键、坏的那支也有键，于是"有实体没建成"永远被算进"已 provision"。M13 就是**原样退回
那一版**，它必须一次打掉四条；M1–M12 各拆一句，用来证明那四句话不是一句空话。

#47 之后多出来的第四态是 `ALTERED`（表本来就在，但这一次真的执行了 ALTER 补列），
M15–M18 钉的是它：摘分支（M15）、Tag 说成空操作（M16）、汇总少一格（M17）、
汇总取错数（M18）。M17/M18 是**有意留着的等价对**（见 KNOWN_TITLE_EQUIV）：
"补了列的表数"在界面上只有那一句 toast 可观察，删掉它和把它读成 created 红在同一条用例上，
但动的源码行不同 —— 删任何一支，那一行就没人钉。

判据同 `mutate_designer_field_code.py`：只有 vitest JSON 报告里**预期用例真的 failed** 才算
"注入被抓"；少红 == 那条测试是空的，多红 == 口径要重算。构建/转译失败本身不算红，
崩红（ReferenceError）与闸红要分开看 —— 所以每支的红消息都另外核过一遍（见归档日志目录）。

⚠ 加新用例会把**旧注入的波及面**改掉（本轮实测：新增 T14 让 M5/M14 各多红一条，
新增 T15 让 M9/M12/M13 各多红一条）。所以改完 DesignerPage 的话术要**整族重跑**，
不能只跑与本次主题相关的那几支。

一条**没有**注入、按未覆盖记账：T12（批量按钮打的是 provision-all，一次点击只发一次请求）。
把它改红的形状是"批量按钮改去逐支调 /admin/entity/provision"，那一动同时会打掉 T7/T8/T9/T11
（响应根本不再是 ProvisionReport），红集合失去归因；而它真正守的那件事——**只有 N 次网络往返**
——在别处更根本：`DesignerPage` 里没有别的循环 provision，这一条是调用形状检查，
注入面比"接口被前端偷偷替代"更窄。留在这里当已知空洞，比硬凑一支"多红"诚实。
"""

import json
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
DESIGNER = UI / "src/views/designer/DesignerPage.tsx"
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_provision_report_logs"
LOG_DIR.mkdir(exist_ok=True)

ORIG: dict[Path, str] = {}


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path.name}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> list[str]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", "src/views/designer",
             "--reporter=json", f"--outputFile={out}"],
            cwd=UI, capture_output=True, text=True, timeout=900,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(
            stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
            encoding="utf-8",
        )
        print(f"    vitest 超过 900s 未结束（视为该注入让测试挂住）；输出留档: {keep}")
        return [f"<timeout:{slug(label)}>"]
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    total = report.get("numTotalTests", 0)
    if not report.get("testResults") or not total:
        # 零个用例的一轮"全绿"是最容易骗人的一种绿。
        raise SystemExit(
            f"vitest report is empty (total={total}) — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed = []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            if case.get("status") == "failed":
                failed.append(case.get("title") or case.get("fullName") or "?")
    print(f"    vitest: {total} tests, {len(failed)} failed")
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        (LOG_DIR / f"{slug(label)}.vitest.json").write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep}")
    return failed


def judge(name: str, failed: list[str], expected: list[str]) -> int:
    hard = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for title in failed:
        print(f"    RED ({'预期' if any(e in title for e in expected) else '未预期'}) {title}")
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无未预期红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红（守卫被别处依赖？口径要重算）: {extra}")
    return 1


# 用例标题：与 DesignerProvision.test.tsx 一一对应（这一份就是分母）
T1 = 'HTTP 200 但 status=FAILED：一句"已建成"的话都不许出现'
T2 = '没建成的那个实体要被点名（不点名就没人知道该改哪一份定义）'
T3 = '缺列清单要写出来：光说"没建成"，用户不知道该动哪一栏'
T4 = 'DDL 面板不许把没建成说成"这是服务端实际执行的 DDL"'
T5 = 'CREATED：说清是按这份定义建出来的哪张表，并且不留「未建成」横幅'
T6 = 'EXISTS_INTACT：要说"表本来就在、没执行建表"，不许混成"已建出"'
T7 = '三个实体一个没建成：要说清"有 1 个没建成"，不许报整体成功'
T8 = '好的那两支不许被连坐进「未建成」横幅'
T9 = '全部建成：新表数取服务端的 created，不许拿实体数凑'
T10 = '"本来就在"要单独数（unchanged），不能合成一个更大的成功数'
T11 = '应用一个实体都没有：那是"没有要建的表"，不是"provision 完成"'
T12 = '批量按钮打的是 provision-all，一次点击只发一次请求'
T13 = 'ALTERED：要说清补了哪几列，不许退回"列一列不缺"'
T14 = 'ALTERED 的 DDL 面板要说这次真的执行过，不许说成"空操作"'
T15 = '补过列的表要单独数（altered），不许并进新表或"本来就在"'

# ---- 锚点（逐字取自 DesignerPage.tsx，改源码要同步改这里，否则当场拒跑）----
BAD_SINGLE = "      const bad = item.status === 'FAILED';"
PROBLEMS_SINGLE = "      setProvisionProblems(bad ? [item] : []);"
CODE_TEXT = "                    <Text code>{item.entityCode}</Text>"
MISSING_TEXT = "                    {item.missingColumns?.length ? `（缺列 ${item.missingColumns.join(', ')}）` : ''}"
CREATED_TOAST = "        message.success(`已按这份定义建出物理表 ${item.tableName ?? ''}`);"
TAG_BLOCK = """                <Tag
                  color={
                    shownVerdict.status === 'FAILED'
                      ? 'red'
                      : shownVerdict.status === 'ALTERED'
                        ? 'blue'
                        : 'green'
                  }
                >
                  {shownVerdict.status === 'FAILED'
                    ? '这张表没有建成，上面是服务端尝试执行的 DDL'
                    : shownVerdict.status === 'EXISTS_INTACT'
                      ? '表本来就在，这条 DDL 是空操作（列已核对齐全）'
                      : shownVerdict.status === 'ALTERED'
                        ? '表本来就在，上面末尾的 ALTER 是这次真的执行过的（只加列）'
                        : '这是服务端实际执行的 DDL'}
                </Tag>"""
TAG_FAILED_LINE = "                    ? '这张表没有建成，上面是服务端尝试执行的 DDL'"
TAG_ALTERED_LINE = "                        ? '表本来就在，上面末尾的 ALTER 是这次真的执行过的（只加列）'"
INTACT_TOAST = "        message.success('表本来就在，列一列不缺，这次没有执行 DDL');"
ALTERED_BRANCH = """      } else if (item.status === 'ALTERED') {
        const added = item.addedColumns ?? [];
        message.success(
          `表本来就在，按这份定义补了 ${added.length} 列${added.length ? `：${added.join('、')}` : ''}`,
        );
      } else {"""
BAD_FILTER = "      const bad = items.filter((item) => item.status === 'FAILED');"
ZERO_BRANCH = "      if (report.total === 0) {"
PARTIAL_BRANCH = "      } else if (bad.length) {"
CREATED_COUNT = """        const parts = [
          `已 provision ${report.created} 张新表`,
          `${report.unchanged} 张表本来就在且列一列不缺`,
        ];"""
CREATED_COUNT_MUT = """        const parts = [
          `已 provision ${items.length} 张新表`,
          `${items.length} 张表本来就在且列一列不缺`,
        ];"""
ALTERED_PUSH = "        if (report.altered > 0) parts.push(`${report.altered} 张表按定义补了列`);"
THREE_BRANCHES = """      if (report.total === 0) {
        message.info('这个应用还没有实体，没有要建的表');
      } else if (bad.length) {
        // 旧口径是 Object.keys(返回 Map).length —— 那个 Map 每个实体一个键，
        // 坏的那支也在里面，于是"有实体没建成"永远被算进"已 provision"。
        message.error(`有 ${bad.length} 个实体没建成，详见下方「未建成」`);
      } else {
        // 少了 altered 这一格，"3 张新表 + 2 张补了列"会被说成"3 张新表 + 0 张无事发生"，
        // 而那 2 张表刚刚真的被执行过 ALTER —— 这是这句文案唯一能被证伪的地方。
        const parts = [
          `已 provision ${report.created} 张新表`,
          `${report.unchanged} 张表本来就在且列一列不缺`,
        ];
        if (report.altered > 0) parts.push(`${report.altered} 张表按定义补了列`);
        message.success(parts.join('，'));
      }"""
OLD_BATCH_SHAPE = """      message.success(`已 provision ${Object.keys(report).length} 张表`);"""
# 清单刷新那一句：锚点必须带上下面那行注释才唯一（`setDirty(false);` 在文件里有两处）。
DIRTY_ANCHOR = "    setDirty(false);\n    // provision 的结论**不在这里**清"

# 两支 mutant 的预期红标题**完全相同**时，收线会打 ⚠"不新增证据"。下面这一对是有意留着的：
#  · "补了列的表数"在界面上只有汇总那一句 toast 可观察，所以
#    M17（那一格整个漏掉）与 M18（那一格读成 created）红在同一条用例上。
# 新增这种等价对要写进这个集合，否则 ⚠ 每轮都响、响久了就等于没有。
#
# M5 与 M14 以前也在这本账上（都是 {T4,T6}）。#47 之后 M5 多红一条 T14 而 M14 没有 ——
# 实测原因：T14 的三句断言全落在 `waitFor` 那一次读取上，而 M14 的冲掉发生在**其后**的
# reload 里；"结论要活过 reload"这一句由 T4 单独钉（它带了 settle 之后的复查）。
# 不是 T14 空跑：M16/M5 都能把它打红。
KNOWN_TITLE_EQUIV = {"M17", "M18"}

RUNS = [
    (
        "M14 清单刷新时把 provision 结论一并冲掉（本轮实测真踩到过的形状）",
        [(DESIGNER, DIRTY_ANCHOR,
          "    setDirty(false);\n    setProvisionVerdict(null);\n    // provision 的结论**在这里**清")],
        # 冲掉之后 DDL 面板退回本地 previewDdl：T4 的"没建成"那句和 T6 的"空操作"那句一起没。
        # （T14 为什么不在这里 —— 见上面 KNOWN_TITLE_EQUIV 旁边那段实测记录。）
        [T4, T6],
    ),
    (
        "M1 单实体不再按 status 判坏（FAILED 被当成'跳过建表'）",
        [(DESIGNER, BAD_SINGLE, "      const bad = item.status === 'NOPE';")],
        [T1, T2, T3],
    ),
    (
        "M2 摘掉单实体那条横幅接线（结论只剩 toast）",
        [(DESIGNER, PROBLEMS_SINGLE, "      setProvisionProblems([]);")],
        [T2, T3],
    ),
    (
        "M3 横幅不点名实体，只写状态（用户找不出是哪一份定义）",
        [(DESIGNER, CODE_TEXT, "                    <Text code>{item.status}</Text>")],
        # 单支与批量共用同一段 <li> 渲染，所以这一支同时打掉三条"要点名实体"的断言。
        [T2, T7, T8],
    ),
    (
        "M4 横幅丢掉缺列清单（'没建成'变成一句没法行动的话）",
        [(DESIGNER, MISSING_TEXT, "                    {''}")],
        [T3],
    ),
    (
        "M5 DDL 面板退回旧的那只恒绿色 Tag",
        # 替换体必须是**一个 JSX 元素**：外层已经是 `{shownVerdict ? ( … ) : null}`，
        # 再塞一个 `{… ? … : null}` 进去是语法错 —— 表现是"整份报告 0 个用例"（转译崩掉），
        # 那不是闸红，实测就是这样被 SystemExit 中断过一次。
        [(DESIGNER, TAG_BLOCK,
          "                <Tag color=\"green\">这是服务端实际执行的 DDL</Tag>")],
        [T4, T6, T14],
    ),
    (
        "M6 没建成时 Tag 仍说'这是服务端实际执行的 DDL'（只改那一句）",
        [(DESIGNER, TAG_FAILED_LINE, "                    ? '这是服务端实际执行的 DDL'")],
        [T4],
    ),
    (
        "M7 真建表成功也只弹旧的泛化话术（说不清建了哪张表）",
        [(DESIGNER, CREATED_TOAST, "        message.success('物理表已 provision');")],
        [T5],
    ),
    (
        "M8 把'表本来就在、没执行建表'说成'已按这份定义建出'（跳过被记成成果）",
        [(DESIGNER, INTACT_TOAST, "        message.success(`已按这份定义建出物理表 ${item.tableName ?? ''}`);")],
        [T6],
    ),
    (
        "M9 批量成功数取 items.length（实体数冒充新表数）",
        [(DESIGNER, CREATED_COUNT, CREATED_COUNT_MUT)],
        # T15 也红：那句里的两个数一起被换成 4，"1 张新表"这句就不成立了。
        [T9, T10, T15],
    ),
    (
        "M10 部分失败不再走'有 N 个没建成'那支（落到成功话术）",
        [(DESIGNER, PARTIAL_BRANCH, "      } else if (false) {")],
        [T7],
    ),
    (
        "M11 零实体也报'provision 完成'",
        [(DESIGNER, ZERO_BRANCH, "      if (false) {")],
        [T11],
    ),
    (
        "M12 横幅把好的那两支也连坐进来（'整应用都没建成'的镜像）",
        [(DESIGNER, BAD_FILTER, "      const bad = items.filter(() => true);")],
        # 这一支不只脏横幅：`bad.length` 同时喂给那句"有 N 个实体没建成"，所以
        # T7 的"1 个"变成"3 个"；而在**全部建成**的那四例里 bad 由空变成非空，
        # 成功话术整个走不到 —— T9/T10/T15 一起红。都是这句话本来的后果，不是断言串了线。
        [T7, T8, T9, T10, T15],
    ),
    (
        "M13 整批退回本轮修复前的真实形状：Object.keys(报告).length 张表",
        [(DESIGNER, THREE_BRANCHES, OLD_BATCH_SHAPE)],
        # 三个分支一起没：T15 那条"补了列的要单独数"也红 —— 整句成功话术都被换成一个数了。
        [T7, T9, T10, T11, T15],
    ),
    (
        "M15 ALTERED 那一支整个摘掉（补过列的表被说成'列一列不缺'）",
        [(DESIGNER, ALTERED_BRANCH, "      } else {")],
        [T13],
    ),
    (
        "M16 ALTERED 的 DDL 面板说成'空操作'（刚执行过的 ALTER 被抹掉）",
        [(DESIGNER, TAG_ALTERED_LINE, "                        ? '表本来就在，这条 DDL 是空操作（列已核对齐全）'")],
        # 只红 T14：T6 的夹具走的是 EXISTS_INTACT 那一行，没被碰。
        [T14],
    ),
    (
        "M17 批量汇总漏掉'补了列'那一格（两张表被说成什么都没动）",
        [(DESIGNER, ALTERED_PUSH, "        // (mutant) 这一格整个漏了")],
        [T15],
    ),
    (
        "M18 批量汇总把补列数读成 created（数字看着齐全，指错了表）",
        [(DESIGNER, ALTERED_PUSH, "        if (report.altered > 0) parts.push(`${report.created} 张表按定义补了列`);")],
        # T15 的夹具把三个数做成 1/1/2 就是为了这一支：读错数的 toast 与正确形状只差那个数字。
        [T15],
    ),
]


def main(argv: list[str]) -> int:
    only = []
    if "--only" in argv:
        only = argv[argv.index("--only") + 1].split(",")
    runs = [r for r in RUNS if not only or any(r[0].startswith(f"{m.strip()} ") for m in only)]
    if only and len(runs) != len(only):
        matched = [r[0].split()[0] for r in runs]
        missing = [m for m in only if m.strip() not in matched]
        print(f"!! --only 里这些编号不存在: {missing}（现有 {[r[0].split()[0] for r in RUNS]}）")
        return 2

    for path in (DESIGNER,):
        ORIG[path] = path.read_text(encoding="utf-8")

    # 预检：所有锚点一次性数过。`sub()` 会在注入当场拒跑，但那时基线已经跑完三分钟，
    # 而且只报第一支 —— 改一处话术往往同时挪好几支的锚点，一次全报出来才不用反复试。
    broken_anchors = []
    for label, edits, _ in runs:
        for path, anchor, _mut in edits:
            n = ORIG[path].count(anchor)
            if n != 1:
                broken_anchors.append(f"{label.split()[0]}: {n}× {anchor.strip()[:60]!r}")
    if broken_anchors:
        print("!! 锚点不唯一，先对源码再改这些行（未跑任何一轮）:")
        for line in broken_anchors:
            print(f"   {line}")
        return 2

    bad = 0
    try:
        print("=== 基线（全部修复态）===")
        if run_vitest("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for label, edits, expected in runs:
            print(f"\n=== {label} ===")
            for attempt in range(1, REPEATS + 1):
                tag = f"{label} (第{attempt}/{REPEATS}遍)"
                try:
                    for path, anchor, mutated in edits:
                        sub(path, anchor, mutated)
                    bad += judge(tag, run_vitest(tag), expected)
                finally:
                    for path, _, _ in edits:
                        path.write_text(ORIG[path], encoding="utf-8")
                        if path.read_text(encoding="utf-8") != ORIG[path]:
                            print(f"  !! {path} 未恢复到原始字节")
                            bad += 1
                time.sleep(0.2)

        print("\n=== 恢复后复跑 ===")
        if run_vitest("99_restored"):
            print("  !! 恢复后仍有红")
            bad += 1
    finally:
        for path, text in ORIG.items():
            path.write_text(text, encoding="utf-8")

    seen: dict[tuple, str] = {}
    for label, _, expected in RUNS:
        key = tuple(sorted(expected))
        mid = label.split(" ", 1)[0]
        if key in seen:
            prev = seen[key].split(" ", 1)[0]
            if {mid, prev} <= KNOWN_TITLE_EQUIV:
                print(f"    已知等价: {mid} 与 {prev} 打的是同一组 claim（动的行不同，两支都留着）")
            else:
                print(f"⚠ 预期红集合与「{seen[key]}」完全相同，这一支不新增证据: {label}")
        seen[key] = label

    if only:
        print("\n(PARTIAL: 只跑了 --only 指定的那几支，收线口径不成立)")
    print(f"\n{'FAILED: ' + str(bad) if bad else 'ALL MUTANTS BEHAVED AS CLAIMED'}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from _mutlock import acquire, release

    acquire(Path(__file__).name)
    try:
        sys.exit(main(sys.argv[1:]))
    finally:
        release()
