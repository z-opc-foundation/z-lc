#!/usr/bin/env python3
"""同轮注入缺陷自证：画廊 / 日历 / 变更历史的「记录没读到」（#24）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_record_fetch_guards.py

这一批补的是同一类谎话最后三个已定位的出口。表格、看板、图表早就把"读失败"和
"读到空"分成两句话了（StateBlock），这三处没有：接口 500 之后画廊亮"暂无数据"、
日历摆一整月空格子、变更历史说"这个实体还没有数据变更"。

新测试是我自己写的，所以每条都要能因为**改坏守卫**而红 —— 否则就是跟着代码一起变绿的装饰。
每个注入都取"修复前的真实代码形状"：G2/C2 就是本轮改动前那行 `message.error('加载数据失败')`
（toast 三秒自己收走，之后页面照样是空态）；G3/C1/U2 是改动前无条件渲染空态/网格的形状；
C0 就是改动前日历的真实写法（monthStart 不缓存），G5 把同一类身份不稳定搬到画廊；
不是为测试编的稻草人。

C0/G5 这两条守的不是"说哪句话"，而是"一次挂载只发一次查询"：日历因为依赖身份每轮渲染都变，
useEffect 无限重发当月查询，写这轮测试时实测挂载后 300ms 内 43 次 /runtime/list。
这个缺陷四道门禁此前一个都没抓到（浏览器层只数点击结果，不数请求条数）。

每轮只改一个守卫，跑全量 vitest，要求红的用例**恰好**是预期那几条：
多红 = 守卫被别处依赖（要写进文档），少红 = 那条测试是空的。

唯一的例外是 C0/G5 这两条"重查"注入：循环让画面钉在哪一支，取决于断言落在第几次请求
的间隙上（同一注入两轮分别发了 83 / 110 次查询，第二轮就多红了"记录没有读到"那条）。
把这种偶然写进硬预期，下一轮就会以"你的测试是空的"的名义误报。所以这两条只硬要求
LOOP_* 红（纯计数、对循环稳定），其余列在第四项里按"连带红"报告、不计入判定。
测试本身一条都没因此放宽 —— 放宽的是判定口径，而且理由写在条目里。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
SRC = UI / "src"
GALLERY = SRC / "views/workspace/GalleryView.tsx"
CALENDAR = SRC / "views/workspace/CalendarView.tsx"
DRAWER = SRC / "views/workspace/UndoHistoryDrawer.tsx"

# 留档放在系统临时目录而不是仓库里：一份失败输出几百 KB，不该混进交付物。
LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_rec_fetch_logs"
LOG_DIR.mkdir(exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor}")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> list[str]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", "--reporter=json", f"--outputFile={out}"],
            cwd=UI,
            capture_output=True,
            text=True,
            timeout=1500,
        )
    except subprocess.TimeoutExpired as exc:
        # 注入把测试跑挂住也是一种结果（例如依赖身份不稳定 → 微任务自旋饿死计时器）。
        # 不能让它把后面几轮一起带走：留档 + 记一条红，继续下一轮。
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(
            stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
            encoding="utf-8",
        )
        print(f"    vitest 超过 1500s 未结束（视为该注入让测试挂住）；输出留档: {keep}")
        return [f"<timeout:{slug(label)}>"]
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    failed = []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            if case.get("status") == "failed":
                failed.append(case.get("title") or case.get("fullName") or "?")
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    # 只要见到红就把原始输出留住：没有样本就只能猜，这是本仓库写过的教训。
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
        # 两份都留：stdout 在 --reporter=json 下只有几十字节，失败样本（failureMessages）
        # 只在 JSON 报告里。上一轮 G5 的"多红"就因为没有样本只能猜分支。
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        keep_json = LOG_DIR / f"{slug(label)}.vitest.json"
        keep_json.write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep} / {keep_json}")
    return failed


def judge(name: str, failed: list[str], expected: list[str], collateral: list[str] = ()) -> int:
    hard = [e for e in expected if not any(e in f for f in failed)]
    allowed = list(expected) + list(collateral)
    extra = [f for f in failed if not any(e in f for e in allowed)]
    for title in failed:
        kind = "预期" if any(e in title for e in expected) else "连带"
        print(f"    RED ({kind}) {title}")
    if not hard and not extra:
        hits = len([c for c in collateral if any(c in f for f in failed)])
        note = f"，{hits} 条连带红按理由不计入" if hits else ""
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无未预期红（共 {len(failed)} 条红{note}）")
        return 0
    # 两件事要一起报：只报 missing 就把 extra 藏了，上一轮就是因此差点记错结论。
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红（守卫被别处依赖？口径要重算）: {extra}")
    return 1


G_ALL = 'import { Button, Card, Empty, Pagination, Tag, Typography } from \'antd\';'
G_TOAST = 'import { Button, Card, Empty, Pagination, Tag, Typography, message } from \'antd\';'
G_CATCH = """    } catch (err) {
      // 只弹一条会自己消失的 toast 等于没说的：toast 收走之后画廊会亮出
      // "暂无数据"，把"这次没读到"讲成"库里没有"。错误要留在原地并能重试。
      setLoadError(err);
    } finally {"""
G_CATCH_MUT = """    } catch {
      message.error('加载数据失败');
    } finally {"""

C_ALL = 'import { Button, Empty, Pagination, Typography } from \'antd\';'
C_TOAST = 'import { Button, Empty, Pagination, Typography, message } from \'antd\';'
C_CATCH = """    } catch (err) {
      // 挂掉的月份和真的空的月份长得一样: 42 个空格子。所以错误必须挡住网格,
      // 而不是只弹一条三秒就消失的 toast。
      setLoadError(err);
    } finally {"""
C_CATCH_MUT = """    } catch {
      message.error('加载数据失败');
    } finally {"""

G_FAIL = '记录接口失败时说"记录没有读到"，不许说"还没有记录"'
G_CARDS = '读到记录就画卡片，一句谎都不说'
G_RETRY = '横幅里的重试要真的再发一次 /runtime/list 并把卡片画回来'
G_STALE = '换筛选后读失败，旧卡片不许留在页面上装作还在'
C_FAIL = '记录接口失败时说"本月记录没有读到"，整月空格子不许留在这里'
C_MONTH = '换到读不到的月份，旧月的贴片不许继续摆在上面'
C_RETRY = '横幅里的重试要真的再发一次当月查询'
U_FAIL = '历史接口失败时说"变更记录没有读到"，不许说"这个实体还没有数据变更"'
U_RETRY = '面板里的重试要真的再发一次 /undo/history'
LOOP_CAL = '日历安静下来后不再自己重查，换月只补一次'
LOOP_GAL = '画廊安静下来后不再自己重查，换筛选只补一次'

# (label, [(file, anchor, mutant)], 预期必须变红的用例, [可选：允许连带红、不计入判定的用例])
RUNS = [
    (
        "C0 日历的月份边界不再缓存（改动前的真实缺陷：每轮渲染换新身份 → 无限重查）",
        [
            (
                CALENDAR,
                """  const monthStart = useMemo(() => currentMonth.startOf('month'), [currentMonth]);
  const monthEnd = useMemo(() => currentMonth.endOf('month'), [currentMonth]);""",
                """  const monthStart = currentMonth.startOf('month');
  const monthEnd = currentMonth.endOf('month');""",
            )
        ],
        [LOOP_CAL],
        # 重查注入是唯一一类"红哪些条"不稳定的：循环把画面钉在哪一支，取决于断言落在
        # 第几次请求的间隙上。取证：同一注入两轮分别发了 83 / 110 次查询，第二轮的
        # "记录没有读到"那条因此也红了（第一轮没有）。所以这里只硬要求 LOOP_CAL 红 ——
        # 那条是纯计数的、对循环稳定的；其余按连带红报告，不计入判定。
        # 测试本身一条都没为这个放宽。
        [C_FAIL, C_MONTH, C_RETRY],
    ),
    (
        "G5 画廊的 loadPage 依赖塞进一个新数组（同一类身份不稳定 → 无限重查）",
        [
            (
                GALLERY,
                "  }, [entity.entityCode, appCode, tenantCode, page, conditions, conjunction]);",
                "  }, [entity.entityCode, appCode, tenantCode, page, [...conditions], conjunction]);",
            )
        ],
        [LOOP_GAL],
        [G_FAIL, G_RETRY, G_STALE],
    ),
    (
        "G1 拆掉画廊的错误门禁（= 改动前失败只走空态那一支）",
        [(GALLERY, "        isError={Boolean(loadError)}", "        isError={false}")],
        [G_FAIL, G_RETRY, G_STALE],
    ),
    (
        "G2 失败只弹一条三秒就消失的 toast（改动前原样）",
        [
            (GALLERY, G_ALL, G_TOAST),
            (GALLERY, G_CATCH, G_CATCH_MUT),
        ],
        [G_FAIL, G_RETRY, G_STALE],
    ),
    (
        "G3 画廊空态无条件成立（读到记录也说\"还没有记录\"）",
        [(GALLERY, "        isEmpty={rows.length === 0}", "        isEmpty={true}")],
        # StateBlock 的优先级是 loading > error > empty > children，isEmpty 一旦恒真，
        # children 那一支永远不渲染 —— 所以三条"要看卡片"的用例（画卡片、重试后画回来、
        # 换筛选后旧卡片必须消失）全红。LOOP_GAL 也在其中不是因为它数请求：它的第一句是
        # waitFor(3 张 .ant-card)，先确认真的画出了东西才开始数（只数请求会放过"发对了但没画"），
        # 于是同样被 isEmpty 掐住。两轮取证都是这稳定的 4 条。
        [G_CARDS, G_RETRY, G_STALE, LOOP_GAL],
    ),
    (
        "G4 画廊的重试变成空操作（看着能修，点了什么也不发）",
        [(GALLERY, "        onRetry={() => void loadPage()}", "        onRetry={() => undefined}")],
        [G_RETRY],
    ),
    (
        "C1 日历整个状态层不接（改动前无条件画网格）",
        [
            (CALENDAR, "        isLoading={loading}", "        isLoading={false}"),
            (CALENDAR, "        isError={Boolean(loadError)}", "        isError={false}"),
        ],
        [C_FAIL, C_MONTH, C_RETRY],
    ),
    (
        "C2 日历失败只弹 toast（改动前原样）",
        [
            (CALENDAR, C_ALL, C_TOAST),
            (CALENDAR, C_CATCH, C_CATCH_MUT),
        ],
        [C_FAIL, C_MONTH, C_RETRY],
    ),
    (
        "C3 日历的重试变成空操作",
        [(CALENDAR, "        onRetry={() => void loadPage()}", "        onRetry={() => undefined}")],
        [C_RETRY],
    ),
    (
        "U1 拆掉变更历史的 isError 门禁",
        [(DRAWER, "      {query.isError ? (", "      {false ? (")],
        [U_FAIL, U_RETRY],
    ),
    (
        "U2 空态不看读取结果（改动前原样：读失败也说\"还没有数据变更\"）",
        [
            (
                DRAWER,
                "      {!query.isLoading && !query.isError && entries.length === 0 ? (",
                "      {!query.isLoading && entries.length === 0 ? (",
            )
        ],
        [U_FAIL],
    ),
    (
        "U3 面板里的重试变成空操作",
        [(DRAWER, "onRetry={() => void query.refetch()}", "onRetry={() => undefined}")],
        [U_RETRY],
    ),
]


def main() -> int:
    for p in (GALLERY, CALENDAR, DRAWER):
        ORIG[p] = p.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（全部修复态）===")
        if run_vitest("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        for run in RUNS:
            if len(run) not in (3, 4):
                raise SystemExit(f"RUNS 条目形状不对（要 3 或 4 元）: {run[0]}")
            label, edits, expected = run[:3]
            collateral = run[3] if len(run) > 3 else []
            print(f"\n=== {label} ===")
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                bad += judge(label, run_vitest(label), expected, collateral)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        if run_vitest("99_restored"):
            print("  !! 恢复后仍有红")
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
