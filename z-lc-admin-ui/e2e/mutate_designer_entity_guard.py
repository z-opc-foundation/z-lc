#!/usr/bin/env python3
"""同轮注入缺陷自证：设计器侧栏的「实体列表没读到」（#25）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_designer_entity_guard.py

收尾扫描找到的最后一处出口：`/designer/:appCode` 左边那一列实体清单。改动前它是
`<List loading={loading} locale={{ emptyText: '还没有实体' }}>`，拉取失败只
`message.error(err instanceof Error ? err.message : '加载实体失败')` 弹一条三秒就消失的
toast，然后那台 List 就永久挂着"还没有实体"。比管理页那几处更糟的地方在于：**设计器正是
判断这句话真假的地方** —— 用户照着它去新建一个其实已经存在的实体，就会撞唯一编码（#21 那一族）。

每个注入都是本轮改动前工作区里的真实代码形状（DesignerPage 还没入库，git 里没有对照，
所以旧形状是从本轮那次 Edit 的 old_string 里逐字抄回来的，不是为测试编的稻草人）：
D1 = 改动前那行 toast、D2 = 改动前"失败只走空态那一支"（当时的 List 根本没有错误出口）、
D6 = 改动前的"在读不挡住空态"（当时是 `<List loading={loading}>`，spinner 只是盖在
`emptyText: '还没有实体'` 上面一层）。D3/D4/D5 是守卫本身被拆掉的三种形状。
D7 守的不是"说哪句话"而是"一次挂载只发一次查询"（日历栽过的那一类，#27）：
把 `entities` 写进 `reload` 自己的依赖，就是这个组件里最自然的写法错误。

⚠ D6 最初写的是"把 `loading` 初值改回改动前的 false"，跑出来**没有一条测试变红** —— 也就是
那条测试当时是空的。原因不是测试写少了断言，而是这件事在 jsdom 里根本看不见：RTL 的 act()
会先把挂载 effect 跑完，`reload()` 一进来就 setLoading(true)，等测试拿到 DOM 时"首帧"早过去了
（真浏览器里 useEffect 在 paint 之后才跑，所以初值 false 确实会闪一下"还没有实体"，但那是
sub-frame 竞态，测不出来也别假装测得出来）。所以 D6 换成了它真正守得住的那一挡：拆掉
StateBlock 的 isLoading。`useState(true)` 这个初值仍按代码约定保留，**不计入"有测试守着"**。

每轮只改一个守卫，跑全量 vitest，要求红的用例**恰好**是预期那几条：
多红 = 守卫被别处依赖（要写进文档），少红 = 那条测试是空的。

D7 是唯一一条判定口径不同的：无限重查时画面钉在哪一支，取决于断言落在第几次请求的间隙上
（#24 那轮同一注入两轮分别发了 83 / 110 次查询）。所以它只硬要求 T_LOOP 红 —— 那条是纯计数、
对循环稳定的 —— 其余列在第四项里按"连带红"报告、不计入判定。测试本身一条都没因此放宽。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
DESIGNER = UI / "src/views/designer/DesignerPage.tsx"

# 留档放在系统临时目录而不是仓库里：一份失败输出几百 KB，不该混进交付物。
LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_designer_logs"
LOG_DIR.mkdir(exist_ok=True)


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
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
        # 注入把测试跑挂住也是一种结果（D7 就是专门制造这种形状的）；
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
    if not report.get("testResults"):
        raise SystemExit(
            "vitest report has zero test results — a run over zero tests proves nothing:\n"
            + proc.stdout[-3000:]
        )
    failed = []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            if case.get("status") == "failed":
                failed.append(case.get("title") or case.get("fullName") or "?")
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    # 只要见到红就把两份证据都留住：--reporter=json 下 stdout 只有几十字节，
    # 失败样本（failureMessages）只在 JSON 报告里。上一轮就是靠样本才没把 G3 的理由写错。
    if failed:
        keep = LOG_DIR / f"{slug(label)}.log"
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


T_FAIL = '实体接口失败时说"实体列表没有读到"，不许说"还没有实体"'
T_LIST = '读到实体就列出来，一句谎都不说'
T_EMPTY = '读到空数组才说"还没有实体"'
T_FRAME = '挂载到响应回来之间那一帧说的是"在读"，不是"还没有实体"'
T_RETRY = '横幅里的重试要真的再发一次实体请求并把清单画回来'
T_SWITCH = '切到另一个应用时读失败，上一个应用的实体清单不许留在侧栏'
T_LOOP = '设计器安静下来后不再自己重查'

CATCH = """    } catch (err) {
      // 只弹一条三秒就消失的 toast，之后侧栏会一直挂着"还没有实体" —— 而设计器正是
      // 判断这句话真假的地方，用户会照着它去新建一个其实已经存在的实体（撞唯一编码那族）。
      setLoadError(err);
    } finally {"""
CATCH_MUT = """    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载实体失败');
    } finally {"""

# (label, [(file, anchor, mutant)], 预期必须变红的用例, [可选：允许连带红、不计入判定的用例])
RUNS = [
    (
        "D1 失败只弹一条三秒就消失的 toast（改动前原样）",
        [(DESIGNER, CATCH, CATCH_MUT)],
        [T_FAIL, T_RETRY, T_SWITCH],
    ),
    (
        "D2 拆掉错误门禁（改动前根本没有这一支：失败只能走空态）",
        [(DESIGNER, "          isError={Boolean(loadError)}", "          isError={false}")],
        [T_FAIL, T_RETRY, T_SWITCH],
    ),
    (
        "D3 空态无条件成立（读到实体也说\"还没有实体\"）",
        [(DESIGNER, "          isEmpty={entities.length === 0}", "          isEmpty={true}")],
        # StateBlock 的优先级是 loading > error > empty > children，isEmpty 恒真时 children
        # 那一支永远不渲染：画清单的、重试后画回来的、切实体后旧行必须消失的，全红。
        # T_LOOP 也在其中不是因为它数请求 —— 它第一句是 waitFor(2 个 .ant-list-item)，
        # 先确认真的画出了东西才开始数（只数请求会放过"发对了但没画"），同样被掐住。
        [T_LIST, T_RETRY, T_SWITCH, T_LOOP],
    ),
    (
        "D4 拆掉空态那一支（真·0 实体时也什么都不说）",
        [(DESIGNER, "          isEmpty={entities.length === 0}", "          isEmpty={false}")],
        [T_EMPTY],
    ),
    (
        "D5 横幅里的重试变成空操作（看着能修，点了什么也不发）",
        [(DESIGNER, "          onRetry={() => void reload()}", "          onRetry={() => undefined}")],
        [T_RETRY],
    ),
    (
        "D6 拆掉在读那一挡（改动前的形状：List 的 loading 只是盖一层 spinner，空态照旧渲染）",
        [(DESIGNER, "          isLoading={loading}", "          isLoading={false}")],
        [T_FRAME],
    ),
    (
        "D7 把 entities 写进 reload 自己的依赖（一轮自激重查）",
        [
            (
                DESIGNER,
                """      setLoading(false);
    }
  }, [appCode]);""",
                """      setLoading(false);
    }
  }, [appCode, entities]);""",
            )
        ],
        [T_LOOP],
        [T_FAIL, T_LIST, T_RETRY, T_SWITCH],
    ),
]


def main() -> int:
    ORIG[DESIGNER] = DESIGNER.read_text(encoding="utf-8")

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
