#!/usr/bin/env python3
"""同轮注入缺陷自证：看板「移到」的焦点桥（缺陷 #32）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_kanban_keyboard_guards.py

#32 的症状是**界面在撒谎**：卡片上的 tooltip 写着"移到其他列（键盘可用）"，而 antd 的 Dropdown
打开时不把焦点交给菜单（它的 `autoFocus` 只实现于 `Dropdown.Button`），方向键/Enter 那套处理
全挂在 rc-menu 的根节点上 —— 焦点还停在触发按钮时一条都收不到。浏览器门禁实测过：Enter 打得开
菜单，之后 ArrowDown + Enter 什么也不做，卡片留在原列。

修法是 `KanbanView.tsx` 里那三层，一层一个注入，每层的预期红各不相同（这才说明它们不是重复检查）：

| 注入 | 改掉的行 | 预期变红的用例 |
| --- | --- | --- |
| M1 | effect 里那句 `menu.focus()` 换成空回调 | 焦点交给菜单 / 关掉还焦点 / 第二次打开 |
| M2 | 关闭时 `moveTriggerRef.current?.focus()` | 只有"关掉还焦点" |
| M3 | `destroyOnHidden` → `destroyOnHidden={false}` | 关掉还焦点 / 第二次打开 |
| M4 | 整个 `popupRender` 桥摘掉（= 修复前的原状） | 焦点交给菜单 / 关掉还焦点 / 第二次打开 |

M3 会连带红"关掉还焦点"是**机制上的必然**，不是巧合：那条用例先等 `.ant-dropdown-menu` 从 DOM 消失，
而"消失"本身就是 `destroyOnHidden` 的行为。这个预期在写脚本时是推出来的，跑完要对样本核一遍
（`/tmp/zlc_mut_kb_logs/*.vitest.json` 里有 failureMessages），推错就以样本为准改预期。

⚠ 按文件跑（`KanbanView.test.tsx` 自己那 7 条），不跑全量：这条链路上有 raf 和焦点转移，
全量跑会把 worker 拖成"跑不完"，而**跑不完一整轮不等于通过，等于没测**。
分母钉成标题清单，缺一条多一条都作废这一轮。

⚠ 已知测不到的那段：ArrowDown 在 jsdom 里走不动 —— rc-menu 挑可聚焦项要过 `isVisible`，
它读 `offsetParent` / `getBoundingClientRect`，jsdom 没有布局，恒为 null/0，菜单项一律"不可见"
（真量过：焦点钉在 ul 上）。完整的 Tab → Enter → ArrowDown → Enter 归浏览器门禁
（`browser-e2e.mjs` 看板一节，可信按键注入，实测 2/2 轮绿）。这一条按"未覆盖"记账，不许说成有单测。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
BOARD = UI / "src/views/workspace/KanbanView.tsx"
SUITE = "src/views/workspace/KanbanView.test.tsx"

BASELINE_TOTAL = 7
# 每个注入跑两遍：一遍只证明"能红"，两遍才勉强支撑"红的恰好是这几条、且每次都这几条"。
REPEATS = 2

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_kb_logs"
LOG_DIR.mkdir(exist_ok=True)

T_FOCUS = "「移到」菜单打开后焦点交给菜单，Enter 选中发出与鼠标同形的部分更新"
T_RESTORE = "菜单关掉后焦点回到「移到」按钮，键盘用户不会在一次移动之后丢了位置"
T_REOPEN = "第二次打开仍然把焦点交给菜单（destroyOnHidden 不是装饰）"

# 这个文件该有哪 7 条，钉死在这里：改名或删一条都会让"没有连带红"失去意义。
COLLECTED = [
    "列走服务端 group-by：列名用字典标签，计数是全量口径",
    "默认按字典字段分组，不会退化成一条记录一列",
    "卡片按自己的分组值落列，不是按顺序占位",
    "「移到」菜单和拖拽走同一条部分更新：只提交分组字段和 id",
    T_FOCUS,
    T_RESTORE,
    T_REOPEN,
]

FOCUS_EFFECT = "    const frame = requestAnimationFrame(() => menu.focus());"
RESTORE_LINE = "                                  moveTriggerRef.current?.focus();"
DESTROY_PROP = "                              destroyOnHidden\n"
POPUP_RENDER = "                              popupRender={(node) => <MoveMenuPortal>{node}</MoveMenuPortal>}\n"


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:48] or "run"


ORIG: dict[Path, str] = {}


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path}, expected exactly 1:\n{anchor!r}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run") -> tuple[list[str], int, list[str]]:
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", SUITE, "--reporter=json", f"--outputFile={out}"],
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
        print(f"    vitest 超过 600s 未结束（视为该注入把测试挂住了 → 无结论）；留档: {keep}")
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
    # 无论红不红都留档：判"预期之外的红"要读 failureMessages，只留红的样本会像挑证据。
    keep = LOG_DIR / f"{slug(label)}.log"
    keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
    (LOG_DIR / f"{slug(label)}.vitest.json").write_bytes(out.read_bytes())
    return failed, total, titles


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
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无一条预期外的红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(
            f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
            f"先读 {LOG_DIR} 里的 failureMessages 说清它换了哪一支断言"
        )
    return 1


RUNS = [
    (
        "M1 effect 找到了菜单却不给焦点（回到 #32 的现场）",
        [(BOARD, FOCUS_EFFECT, "    const frame = requestAnimationFrame(() => { void menu; });")],
        [T_FOCUS, T_RESTORE, T_REOPEN],
    ),
    (
        "M2 关掉菜单不还焦点（键盘用户丢了位置）",
        [(BOARD, RESTORE_LINE, "")],
        [T_RESTORE],
    ),
    (
        "M3 弹层关掉后不销毁（第二次打开不再跑桥）",
        [(BOARD, DESTROY_PROP, "                              destroyOnHidden={false}\n")],
        [T_RESTORE, T_REOPEN],
    ),
    (
        "M4 整个焦点桥摘掉（= 修复前的原状）",
        [(BOARD, POPUP_RENDER, "")],
        [T_FOCUS, T_RESTORE, T_REOPEN],
    ),
]


def main() -> int:
    ORIG[BOARD] = BOARD.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（修复态）===")
        failed, total, titles = run_vitest("00_baseline")
        if failed or total != BASELINE_TOTAL or set(titles) != set(COLLECTED):
            print("  !! 基线就有红、分母不对或收集到的用例和钉子不符，注入结果无法归因；先修基线")
            return 2

        for label, edits, expected in RUNS:
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
                        print(f"  !! {path} 未恢复到原始字节")
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
