#!/usr/bin/env python3
"""同轮注入缺陷自证：权限矩阵那一半（前端 + 词表同源）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_permission_matrix_guard.py

#48 修的是"三份口径互不相符"：词表(后端注释 READ/WRITE/ADMIN vs 矩阵列头)、查重、
作用范围。这类错法有个共同点 —— **页面照样画得出来，而且画出来的比正确更像正确**：
一个实体的授权把「整个应用」那格点亮，看着完全合理，只有 `/permission/check` 知道答案
是拒绝。所以这一支测的不是"有没有红"，而是"我声称钉住的那十四条，是不是每条只钉自己那一句"。

十八支注入都是这段代码最可能写错的样子（不是稻草人）：
B1 格子退化成"出现过就算有"、B2 应用级行不再覆盖实体、B3 授予时不带当前档位、
B4 回收的"已回收"抢在请求前面、B5 校验框顺手改掉整页筛选、B6 把筛选搬回后端、
B7/B8/B9 词表从前端/后端顺序/后端内容三个方向漂移、B10 页面自己抄一份列头、
B11/B12 空态的两半各说错一次。B12 这一支换了两次写法才测到东西：先补了 T5b 才有夹具，
再发现"摘掉 `&& allRows.length > 0`"是**等价变异**（ListState 的 'ready' 本身就由 rows.length
算出来），于是改成 `state !== 'error'` —— 一支永远打不红的注入不是测试在保护什么，
是守卫自己骗过自己，见它下面那条注释。

B13–B18 是 #50 那一半的新代码（矩阵行集 = 授权里的角色 ∪ 手动加进来的角色）：
B13 行集退回筛过的行、B14 手动加的角色根本不并进矩阵（有输入框没入口）、B15 名字不 trim、
B16 不查重、B17 换应用不清、B18 空名字也能提交。这一族里 B14 一支红三条是设计如此 ——
"授出第一条"这件事在界面上只有一个入口，入口没了三条各钉一面的用例一起塌。

B7/B8/B9 三支故意分别从 TS、Java 顺序、Java 内容三个面进来 —— 只钉一面时另一面会漂。

判据分母 = 这两个文件自己的 14 条（PermissionsPage 12 + permissionVocabulary 2）。
按文件跑而不是全量：与 mutate_pivot_guards 同一理由 —— 全量跑会把一轮变成十几分钟，
而**跑不完不等于通过，等于没测**。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
PAGE = UI / "src/views/admin/PermissionsPage.tsx"
KEYS_TS = UI / "src/api/permission.ts"
KEYS_JAVA = UI.parent / "z-lc-core/src/main/java/com/zifang/z/lc/core/permission/PermissionKeys.java"
SUITES = ["src/views/admin/PermissionsPage.test.tsx", "src/api/permissionVocabulary.test.ts"]

BASELINE_TOTAL = 14
REPEATS = 2

# 留档不能用 /tmp：同一台机器上别的会话会扫它（本项目已复现过两次"日志失踪"）。
LOG_DIR = Path.home() / ".cache/zlc48/mut_logs"
LOG_DIR.mkdir(parents=True, exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path.name}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> tuple[list[str], int, list[str]]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", *SUITES, "--reporter=json", f"--outputFile={out}"],
            cwd=UI,
            capture_output=True,
            text=True,
            timeout=600,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(
            stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
            encoding="utf-8",
        )
        print(f"    vitest 超过 600s 未结束（视为该注入让测试挂住 → 无结论）；输出留档: {keep}")
        return [f"<timeout:{slug(label)}>"], -1, []
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed, titles = [], []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            title = case.get("title") or case.get("fullName") or "?"
            titles.append(title)
            if case.get("status") == "failed":
                failed.append(title)
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        keep_json = LOG_DIR / f"{slug(label)}.vitest.json"
        keep_json.write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep} / {keep_json}")
    return failed, total, titles


# 这两个文件该有哪 10 条，钉死在这里：谁改了名或删了一条，注入结果就不能再被相信 ——
# 否则"没有连带红"可能只是因为邻居已经不在了。
COLLECTED = [
    "未选实体时，某个实体的单独授权不许点亮「整个应用」那一格",
    "选了实体：应用级授权覆盖它，别的实体的授权不覆盖",
    "格子授的是当前档位，payload 与提示都要说清范围",
    "两个筛选要真的叠加（后端 roleCode 优先于 entityCode，所以筛选放在客户端）",
    '筛完没有匹配行时不许说"该应用还没有权限配置"',
    '库里真的没有授权时说"还没有权限配置"，不套用「筛选后没有匹配」那句',
    "回收失败要报后端那句 400，不许报「已回收」",
    "校验框自己选实体，不该顺手改掉整页筛选",
    "一条授权都没有的角色，也能从这一页授出第一条",
    "同一个角色加两次只多一行（两行会给出同一格互相矛盾的答案）",
    '按实体过滤不许把"只有别的实体有授权"的角色整行藏掉',
    "换应用要把上一轮手动加进来的角色带走（它不属于这个应用）",
    "矩阵列头就是 PermissionKeys.ALL，一项不多一项不少，顺序也一致",
    "页面里不再有第二份抄出来的权限词",
]

T1, T2, T3, T4, T5, T5b, T6, T7, T8, T9, T10, T11, V1, V2 = COLLECTED

JAVA_ALL = "Arrays.asList(VIEW, CREATE, UPDATE, DELETE, EXPORT)"

# (label, [(file, anchor, mutant)], 预期必须变红的用例)
RUNS = [
    (
        "B1 格子退化成「这个角色这一项出现过就算有」（一个实体的授权点亮整个应用那一格）",
        [(PAGE,
          "      granted: mine.some((row) => (scope ? coversScope(row, scope) : isAppWide(row))),",
          "      granted: mine.length > 0,")],
        # T10 是 #50 补的那一支（同一格在别实体的档位下该灭着并点名）：B1 摘的正是这个口径，
        # 它连带红是同一个因的另一层，取证 `expected '已授予该实体单独授予' to be '授予另有1个实体单独授予'`。
        [T1, T2, T10],
    ),
    (
        "B2 coversScope 丢掉应用级那一支（选了实体后，覆盖它的授权反而不点亮）",
        [(PAGE,
          "const coversScope = (row: PermissionEntity, scope: string) =>\n"
          "  !row.entityCode || row.entityCode === scope;",
          "const coversScope = (row: PermissionEntity, scope: string) =>\n"
          "  row.entityCode === scope;")],
        # T10 里那句「清单按实体筛时应用级那一行也算覆盖」(listRowCount == 2) 与 T2/T4 同因，
        # 取证 `expected 1 to be 2`。
        [T2, T4, T10],
    ),
    (
        "B3 授予时不带当前档位（payload 永远授给整个应用，界面却写着（task））",
        [(PAGE, "          entityCode: scope || undefined,", "          entityCode: undefined,")],
        [T3],
    ),
    (
        "B4 「已回收」抢在请求前面（后端回 400 也照样报成功）",
        [(PAGE,
          "            try {\n"
          "              await revokePermission(row.id);\n"
          "              message.success('已回收');",
          "            try {\n"
          "              message.success('已回收');\n"
          "              await revokePermission(row.id);")],
        [T6],
    ),
    (
        "B5 校验框的实体选择器顺手改掉整页筛选（问一条权限把清单也筛了）",
        [(PAGE,
          "              onChange={(value) => setProbeEntity(value ?? '')}",
          "              onChange={(value) => {\n"
          "                setProbeEntity(value ?? '');\n"
          "                setEntityCode(value ?? '');\n"
          "              }}")],
        [T7],
    ),
    (
        "B6 把筛选搬回后端（/list 的 roleCode 优先于 entityCode，两个一起给会丢应用级行）",
        [(PAGE,
          "    () => listPermissions({ appCode }),",
          "    () =>\n"
          "      listPermissions({\n"
          "        appCode,\n"
          "        entityCode: entityCode || undefined,\n"
          "        roleCode: roleCode || undefined,\n"
          "      }),"),
         (PAGE, "    appCode || null,", "    (appCode ? `${appCode}|${entityCode}|${roleCode}` : null),")],
        [T4],
    ),
    (
        "B7 前端词表少了一项（矩阵从此不再等于后端那份 ALL）",
        [(KEYS_TS,
          "export const PERMISSION_KEYS = ['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT'] as const;",
          "export const PERMISSION_KEYS = ['VIEW', 'CREATE', 'UPDATE', 'DELETE'] as const;")],
        # T8 点名了五个动词（新那一行五格全得能点），少一项它就先撞在"列头里没有 EXPORT"上 ——
        # 与 T3 同一因的两层，取证 `Error: 矩阵列头里没有 EXPORT，实际: ...`。
        [T3, T8, V1],
    ),
    (
        "B8 后端 ALL 换了顺序（列顺序是词表顺序，两边一漂就是两副面孔）",
        [(KEYS_JAVA, JAVA_ALL, "Arrays.asList(VIEW, CREATE, UPDATE, EXPORT, DELETE)")],
        [V1],
    ),
    (
        "B9 后端 ALL 少了一项而前端没跟（页面渲染得出来，check 却不认这个字）",
        [(KEYS_JAVA, JAVA_ALL, "Arrays.asList(VIEW, CREATE, UPDATE, DELETE)")],
        [V1],
    ),
    (
        "B10 页面自己抄一份列头，还抄了个后端没登记的 READ",
        [(PAGE,
          "              ...PERMISSION_KEYS.map((key) => ({",
          "              ...(['VIEW', 'CREATE', 'UPDATE', 'DELETE', 'READ'] as PermissionKey[]).map((key) => ({")],
        [T3, T8, V2],
        # 实测打红的第三条不是白名单: 手抄的那份没有 EXPORT，T3 要点 (SALES, EXPORT) 那一格、
        # T8 要逐格看新那一行的五格，两处都先撞在"列头里没有 EXPORT"上（取证同上）。
    ),
    (
        "B11 空态不做筛选感知（#48 修之前的原样：筛完 0 行就说这应用没配过权限）",
        [(PAGE,
          "        locale={{\n"
          "          emptyText:\n"
          "            // 'ready' 只在「读成功且库里非空」时成立（读成功但零行是 'empty'，见 _scope.ts 的 ListState），\n"
          "            // 所以这一支说的是「有授权，只是被筛掉了」—— 与「这个应用压根没配过权限」是两句话。\n"
          "            state === 'ready' && allRows.length > 0\n"
          "              ? '当前筛选下没有匹配的授权（应用级授权覆盖所有实体，所以按实体筛时不会被排除）'\n"
          "              : listEmptyText(state, '权限配置'),\n"
          "        }}",
          "        locale={{ emptyText: listEmptyText(state, '权限配置') }}")],
        [T5],
    ),
    (
        "B12 反过来: 库里真的一条都没有，也套「当前筛选下没有匹配」那句",
        [(PAGE,
          "            state === 'ready' && allRows.length > 0",
          "            state !== 'error'")],
        [T5b],
        # 第一版注入的是「摘掉 && allRows.length > 0」，跑出来 10 条全绿 —— 但那不是测试空跑，
        # 是**等价变异**：`ListState` 的 'ready' 就是由 `rows.length` 算出来的（_scope.ts:90-99，
        # done + 有行才 ready，done + 零行是 'empty'），所以那个合取项永远改不了结果。
        # 换成 `state !== 'error'` 才是人真会写错的样子（把"读成功"简化成"没坏"），
        # 它让 'empty'/'idle'/'loading' 三档都掉进筛选那一句，T5b 才有牙。
    ),
    # ---- #50：矩阵的行集。每一支新写的分支各进来一次注入 ----
    (
        "B13 矩阵行退回到只从筛过的行里推（#50 原样：选了实体就把别实体的角色整行藏掉）",
        # 两支一起改，少一支就是**等价变异**：第一版只把 `allRows` 换成 `visible`，14 条全绿 ——
        # 因为 useMemo 的依赖还写着 allRows，entityCode 变了 memo 根本不重算，那一行换没换都一样。
        # （#50 之前的老代码是 `const roles = Array.from(new Set(visible...))`，每次渲染都重算，
        # 所以"退回原样"这件事必须连依赖一起退，否则注入的是一把不存在的刀。）
        [(PAGE,
          "      new Set(allRows.map((row) => row.roleCode).filter(Boolean) as string[]),",
          "      new Set(visible.map((row) => row.roleCode).filter(Boolean) as string[]),"),
         (PAGE,
          "  }, [allRows, addedRoles, roleCode]);",
          "  }, [visible, addedRoles, roleCode]);")],
        [T10],
    ),
    (
        "B14 手动加进来的角色不进矩阵（有输入框、没有入口，第一条权限还是授不出去）",
        [(PAGE,
          "    const pending = addedRoles.filter((role) => !seen.includes(role));",
          "    const pending: string[] = [];")],
        [T8, T9, T11],
    ),
    (
        "B15 角色名不 trim（库里会同时有 \"Manager\" 和 \" Manager \" 两个角色）",
        [(PAGE,
          "    const role = newRole.trim();\n    if (!role) return;",
          "    const role = newRole;\n    if (!role) return;")],
        [T8],
    ),
    (
        "B16 「加入矩阵」不查重（同一角色两行，同一格给出两个互相矛盾的答案）",
        [(PAGE,
          "    setAddedRoles((prev) => (prev.includes(role) ? prev : [...prev, role]));",
          "    setAddedRoles((prev) => [...prev, role]);")],
        [T9],
    ),
    (
        "B17 换应用不清掉手动加的角色（上个应用填的名字，在这个应用里还是个能点的格子）",
        [(PAGE,
          "        setAddedRoles([]);\n        setNewRole('');\n        setAppCode(value);",
          "        setAppCode(value);")],
        [T11],
    ),
    (
        "B18 名字没填也能提交（矩阵里多出一行空角色，那一行五格全都能点）",
        [(PAGE, "          disabled={!newRole.trim()}", "          disabled={false}")],
        [T8],
    ),
]


def judge(name: str, failed: list[str], total: int, titles: list[str], expected: list[str]) -> int:
    missing_tests = [t for t in COLLECTED if t not in titles]
    extra_tests = [t for t in titles if t not in COLLECTED]
    if total != BASELINE_TOTAL or missing_tests or extra_tests:
        print(
            f"  !! {name}: 收集到的用例和钉子不符（分母 {total} != {BASELINE_TOTAL}"
            f"，缺 {missing_tests}，多 {extra_tests}），这一轮结论作废"
        )
        return 1
    hard = [e for e in expected if not any(e in f for f in failed)]
    extra = [f for f in failed if not any(e in f for e in expected)]
    for title in failed:
        print(f"    RED ({'预期' if any(e in title for e in expected) else '未预期'}) {title}")
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无一条连带红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(
            f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
            "先拿样本证明这一处口径换掉了哪一支断言，再改预期"
        )
    return 1


def main() -> int:
    for path in (PAGE, KEYS_TS, KEYS_JAVA):
        if not path.exists():
            print(f"  !! 被测文件不存在，注入会写成新文件: {path}")
            return 2
        ORIG[path] = path.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（修复态）===")
        failed, total, titles = run_vitest("00_baseline")
        if failed or total != BASELINE_TOTAL or set(titles) != set(COLLECTED):
            print("  !! 基线就有红、分母不对或收集到的用例和钉子不符，注入结果无法归因；先修基线")
            return 2

        for run in RUNS:
            if len(run) != 3:
                raise SystemExit(f"RUNS 条目形状不对（要 3 元）: {run[0]}")
            label, edits, expected = run
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                for rep in range(1, REPEATS + 1):
                    print(f"\n=== {label} (第 {rep}/{REPEATS} 遍) ===")
                    failed, total, titles = run_vitest(f"{label}_r{rep}")
                    bad += judge(label, failed, total, titles, expected)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path.name} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        failed, total, titles = run_vitest("99_restored")
        if failed or total != BASELINE_TOTAL or set(titles) != set(COLLECTED):
            print("  !! 恢复后仍有红、分母不对或用例集合不符")
            bad += 1
    finally:
        for path, text in ORIG.items():
            path.write_text(text, encoding="utf-8")

    print(f"\n{'FAILED: ' + str(bad) if bad else 'ALL MUTANTS BEHAVED AS CLAIMED'}")
    return 1 if bad else 0


if __name__ == "__main__":
    from _mutlock import acquire, release

    acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        release()
