#!/usr/bin/env python3
"""同轮注入缺陷自证：流水线配置页那一半的前端口径（#41）。

跑法（在 z-lc-admin-ui 下）：

    python3 e2e/mutate_pipeline_wiring_guard.py

#41 的前端这一半不是"流程"而是**口径**：界面上那份阶段清单、那几个触发事件、以及"移动一行会不会
改到执行顺序"。这类代码写错了页面照样画得出来，而且画出来的东西看着比报错更像正确答案 ——
上一轮的原状就是：页面自己写了一份带 WEBHOOK / SCRIPT 的清单（引擎里连执行器都没有），
`stringifyStages` 用 `stage.order ?? index` 把旧 order 原样写回（界面顺序动了、执行链没动），
历史行里的 AFTER_CREATE 被渲染成一个体面的中文标签（说"更新后"，其实引擎根本没有写后挂接点）。

十支注入就是这段代码最可能写错的十种样子（不是稻草人，每种各自打掉一处口径）。
判红只看两件事：**预期的那条用例必须红**，**不该红的必须不红**。F7/F8/F9 都落在列表那一条用例里
（同一次渲染的三个不同断言），所以这三支额外钉了失败消息的关键字 —— 光看用例名分不出它们。

分母 = 这两个文件自己的 10 条（pipelineVocabulary 8 + PipelinesPage 2）。
按文件跑而不是全量：与 mutate_pivot_guards 同一理由 —— 全量跑会把一轮变成十几分钟，
而**跑不完不等于通过，等于没测**。

注入列表：
  F1  `order: index` 退回 `stage.order ?? index` —— 界面动了、落库的顺序没动
  F2  parseStages 不再按 order 排 —— 看到的是数组位置，引擎跑的是 order
  F3  前端清单里塞回幽灵阶段 WEBHOOK —— 配置页重新能选出"没有执行器"的阶段
  F4  触发事件塞回 AFTER_CREATE —— 又能创建一份永远不会被执行的配置
  F5  REQUIRED_CHECK 的 mandatory 翻成 false —— 默认草稿变成一份会被写入口拒掉的配置
  F6  DICT_RESOLVE 的 noOpOnWrite 翻成 false —— 对用户谎称这一档会改变数据
  F7  列表不再标注"无执行器" —— 脏数据显示成一个正常阶段
  F8  触发事件列改回只出中文标签 —— AFTER_CREATE 看着像"更新后"
  F9  列表不再标注空链 —— 空链这一行看起来像"没配阶段"而不是"保存会被拒"
  F10 默认草稿顺序反过来 —— 值校验排到类型转换之前（后端会拒，而这是新建时的默认值）

F3/F4 各有一处**已知的漏网**并如实记在这里：幽灵检查那一条用例读的是 PipelinesPage.tsx 的源码，
往 PIPELINE_STAGE_TYPES 里塞 WEBHOOK 不会让页面源文件出现这个词，所以它抓的是另外三条
（清单对表 + 草稿的选择项 + 列表那一列的「无执行器」标记 —— 后两者读的都是同一份词汇表，
这一条不是"没抓到"，是"被另一处口径抓到"）。
"""

import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

UI = Path(__file__).resolve().parents[1]
API = UI / "src/api/pipeline.ts"
PAGE = UI / "src/views/admin/PipelinesPage.tsx"
SUITES = ["src/api/pipelineVocabulary.test.ts", "src/views/admin/PipelinesPage.test.tsx"]

BASELINE_TOTAL = 10
LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_pipe41_logs"
LOG_DIR.mkdir(exist_ok=True)

T_TYPES = "阶段清单与后端 PROCESSOR_BY_TYPE 一字不差（含顺序，那就是执行顺序）"
T_FLAGS = "必填闸门与不做事标记按后端的两个集合标注，不是前端自己猜"
T_TRIG = "触发事件只给引擎真有挂接点的那几个"
T_GHOST = '页面上再也没有"没有执行器"的幽灵阶段'
T_SORT = "parseStages 按 order 排，与后端 parseTypes 的排序键一致"
T_MOVE = "上下移动真的改变将要执行的顺序：order 按数组位置重写"
T_DEFAULT = "新建的默认链就是引擎今天跑的那三道闸门，且满足值校验在转换之后"
T_JSON = "坏 JSON 不会把阶段清单悄悄变成空"
T_LIST = "列表把引擎兑现不了的东西逐个标出来, 且不按数组位置假装是执行顺序"
T_DRAFT = "草稿里只给后端真支持的选择项, 且移动阶段会改写落库的 order"

COLLECTED = [T_TYPES, T_FLAGS, T_TRIG, T_GHOST, T_SORT, T_MOVE, T_DEFAULT, T_JSON, T_LIST, T_DRAFT]

# (label, [(file, anchor, mutant)], 预期红, 失败消息里必须出现的关键字或 None, 额外稳定复跑次数)
RUNS = [
    ("F1 order 退回 stage.order ?? index（界面动了、执行链没动）",
     [(API, "      order: index,", "      order: stage.order ?? index,")],
     [T_MOVE, T_DRAFT], "旧 order 被原样写回"),
    ("F2 parseStages 不按 order 排（看到的是数组位置，引擎跑的是 order）",
     # 注释掉的 sort 不是删掉整行: 上一版写成 `"      ;)"` 是一个语法错误, vitest 直接收集到 0 条,
     # 整轮按"跑不完 = 没测"作废。比较器恒返回 0 = 稳定排序原样保留数组位置, 那才是这一支的错法。
     [(API, "      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));", "      .sort(() => 0);")],
     [T_SORT, T_LIST], None),
    ("F3 清单里塞回幽灵阶段 WEBHOOK（又能选出没有执行器的阶段）",
     # 连带红是**同一份词汇表的第三处读者**: 列表那一列靠 `PIPELINE_STAGE_TYPES` 查 meta 来判
     # "这个阶段没有执行器"，词汇表里一旦有了 WEBHOOK，脏数据那行就被渲染成一个体面的正常阶段。
     [(API, "  { type: 'VALUE_VALIDATE', label: '值域校验', mandatory: true, noOpOnWrite: false },",
       "  { type: 'VALUE_VALIDATE', label: '值域校验', mandatory: true, noOpOnWrite: false },\n"
       "  { type: 'WEBHOOK', label: '外部通知', mandatory: false, noOpOnWrite: false },")],
     [T_TYPES, T_DRAFT, T_LIST], None),
    ("F4 触发事件塞回 AFTER_CREATE（又能存一份永远不会被执行的配置）",
     # 同 F3: 挂接点那一格也是拿 `isPipelineTriggerSupported` 判的，词汇表一放宽，
     # 历史 AFTER_CREATE 那行就从「无挂接点」的红标签变回一个像样的「更新后」。
     [(API, "export const PIPELINE_SUPPORTED_TRIGGERS = ['BEFORE_CREATE', 'BEFORE_UPDATE'] as const;",
       "export const PIPELINE_SUPPORTED_TRIGGERS = ['BEFORE_CREATE', 'BEFORE_UPDATE', 'AFTER_CREATE'] as const;")],
     [T_TRIG, T_DRAFT, T_LIST], "写后没有回调落点"),
    ("F5 REQUIRED_CHECK 的 mandatory 翻成 false（默认草稿会被后端拒）",
     [(API, "  { type: 'REQUIRED_CHECK', label: '必填校验', mandatory: true, noOpOnWrite: false },",
       "  { type: 'REQUIRED_CHECK', label: '必填校验', mandatory: false, noOpOnWrite: false },")],
     [T_FLAGS, T_DEFAULT, T_DRAFT], None),
    ("F6 DICT_RESOLVE 的 noOpOnWrite 翻成 false（对用户谎称它会改变数据）",
     [(API, "  { type: 'DICT_RESOLVE', label: '字典解析', mandatory: false, noOpOnWrite: true },",
       "  { type: 'DICT_RESOLVE', label: '字典解析', mandatory: false, noOpOnWrite: false },")],
     [T_FLAGS, T_LIST], None),
    ("F7 列表不再标注「无执行器」（脏数据显示成一个正常阶段）",
     [(PAGE, "{index + 1}. {meta ? `${stageTypeLabel(stage.type)} ${stage.type}` : `${stage.type} 无执行器`}",
       "{index + 1}. {stageTypeLabel(stage.type)}")],
     [T_LIST], "无执行器"),
    ("F8 触发事件列改回只出中文标签（AFTER_CREATE 看着像「更新后」）",
     [(PAGE, "        return isPipelineTriggerSupported(trigger) ? (\n"
             "          <Tag data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>"
             "{PIPELINE_TRIGGER_LABELS[trigger] ?? trigger}</Tag>\n"
             "        ) : (\n"
             "          <Tag color=\"red\" data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>\n"
             "            {trigger} 无挂接点\n"
             "          </Tag>\n"
             "        );",
       "        return (\n"
       "          <Tag data-testid={`pipeline-trigger-${row.id ?? 'draft'}`}>\n"
       "            {PIPELINE_TRIGGER_LABELS[trigger] ?? trigger}\n"
       "          </Tag>\n"
       "        );")],
     [T_LIST], "无挂接点"),
    ("F9 列表不再标注空链（看着像「没配阶段」而不是「保存会被拒」）",
     [(PAGE, "            <Text type=\"danger\" data-testid={`pipeline-empty-chain-${row.id ?? 'draft'}`}>\n"
             "              空链（保存会被拒）\n"
             "            </Text>",
       "            <Text data-testid={`pipeline-empty-chain-${row.id ?? 'draft'}`}>—</Text>")],
     [T_LIST], "空链（保存会被拒）"),
    ("F10 默认草稿反过来（值校验排到类型转换之前，新建即被拒）",
     [(API, "  return PIPELINE_STAGE_TYPES.filter((item) => item.mandatory).map((item, index) => ({",
       "  return PIPELINE_STAGE_TYPES.filter((item) => item.mandatory).reverse().map((item, index) => ({")],
     [T_DEFAULT, T_DRAFT], "值校验排到转换之前"),
]

ORIG: dict[Path, str] = {}


def slug(label: str) -> str:
    return re.sub(r"[^0-9A-Za-z_.-]+", "_", label)[:60] or "run"


def sub(path: Path, anchor: str, mutated: str) -> None:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        raise SystemExit(f"anchor found {n}× in {path.name}, expected exactly 1:\n{anchor}")
    if anchor == mutated:
        raise SystemExit("注入是空操作（anchor 与 mutant 相同），这一轮什么都没被测到")
    path.write_text(text.replace(anchor, mutated), encoding="utf-8")


def run_vitest(label: str = "run"):
    """(failed_titles, total, titles, all_failure_messages) —— 报告为空一律硬错，不当"没有红"."""
    tmp = Path(tempfile.mkdtemp())
    out = tmp / "vitest.json"
    try:
        proc = subprocess.run(
            ["npx", "vitest", "run", *SUITES, "--reporter=json", f"--outputFile={out}"],
            cwd=UI, capture_output=True, text=True, timeout=900,
        )
    except subprocess.TimeoutExpired as exc:
        keep = LOG_DIR / f"{slug(label)}_timeout.log"
        stdout = exc.stdout or ""
        keep.write_text(stdout if isinstance(stdout, str) else stdout.decode("utf-8", "replace"),
                        encoding="utf-8")
        print(f"    vitest 超过 900s 未结束（视为无结论，不是通过）；留档: {keep}")
        return [f"<timeout:{slug(label)}>"], -1, [], []
    if not out.exists():
        raise SystemExit(f"vitest wrote no report (exit={proc.returncode}):\n{proc.stdout[-3000:]}")
    report = json.loads(out.read_text(encoding="utf-8"))
    if not report.get("testResults"):
        raise SystemExit("vitest report has zero test results — a run over zero tests proves nothing:\n"
                         + proc.stdout[-3000:])
    failed, titles, messages = [], [], []
    for file_result in report.get("testResults", []):
        for case in file_result.get("assertionResults", []):
            title = case.get("title") or case.get("fullName") or "?"
            titles.append(title)
            if case.get("status") == "failed":
                failed.append(title)
                messages.extend(case.get("failureMessages") or [])
    total = report.get("numTotalTests", 0)
    print(f"    vitest: {total} tests, {len(failed)} failed")
    if failed or proc.returncode != 0:
        keep = LOG_DIR / f"{slug(label)}.log"
        keep.write_text(proc.stdout + proc.stderr, encoding="utf-8")
        (LOG_DIR / f"{slug(label)}.vitest.json").write_bytes(out.read_bytes())
        print(f"    原始输出留档: {keep}")
    return failed, total, titles, messages


def judge(name: str, failed: list, total: int, titles: list, expected: list, msg: str,
          messages: list) -> int:
    missing_tests = [t for t in COLLECTED if t not in titles]
    extra_tests = [t for t in titles if t not in COLLECTED]
    if total != BASELINE_TOTAL or missing_tests or extra_tests:
        print(f"  !! {name}: 收集到的用例和钉子不符（分母 {total} != {BASELINE_TOTAL}，"
              f"缺 {missing_tests}，多 {extra_tests}），这一轮结论作废")
        return 1
    hard = [e for e in expected if e not in failed]
    extra = [f for f in failed if f not in expected]
    for title in failed:
        print(f"    RED ({'预期' if title in expected else '未预期'}) {title}")
    if msg is not None and not any(msg in m for m in messages):
        print(f"  !! {name}: 红是红了，但失败消息里没有「{msg}」——"
              "红的是同一格里的另一句断言，这一支指不回它声称打掉的那处口径")
        return 1
    if not hard and not extra:
        print(f"  OK  {name}: 预期 {len(expected)} 条全红，无一条连带红（共 {len(failed)} 条红）")
        return 0
    if hard:
        print(f"  !! {name}: 预期变红却没红（这条测试是空的）: {hard}")
    if extra:
        print(f"  !! {name}: 出现了预期之外的红 {extra} —— 不许加白名单了事，"
              "先拿样本证明这一处口径换掉了哪一支断言，再改预期")
    return 1


def main() -> int:
    from _mutlock import acquire, release
    acquire("mutate_pipeline_wiring_guard.py (frontend)")
    try:
        return run_all()
    finally:
        release()


def run_all() -> int:
    for path in (API, PAGE):
        ORIG[path] = path.read_text(encoding="utf-8")

    bad = 0
    try:
        print("=== 基线（修复态）===")
        failed, total, titles, _ = run_vitest("00_baseline")
        if failed or total != BASELINE_TOTAL or sorted(titles) != sorted(COLLECTED):
            print("  !! 基线就有红、分母不对或收集到的用例和钉子不符，注入结果无法归因；先修基线")
            return 2
        # 绿态本身也要稳定：整页 mount 在本机高负载下是慢操作，跑两遍都绿才叫基线
        failed2, total2, _, _ = run_vitest("00_baseline_repeat")
        if failed2 or total2 != BASELINE_TOTAL:
            print(f"  !! 基线第二遍不绿（{failed2}）：绿色本身不稳定，后面的红无法归因")
            return 2

        for label, edits, expected, msg in RUNS:
            try:
                for path, anchor, mutated in edits:
                    sub(path, anchor, mutated)
                print(f"\n=== {label} ===")
                failed, total, titles, messages = run_vitest(label)
                bad += judge(label, failed, total, titles, expected, msg, messages)
            finally:
                for path, _, _ in edits:
                    path.write_text(ORIG[path], encoding="utf-8")
                    if path.read_text(encoding="utf-8") != ORIG[path]:
                        print(f"  !! {path.name} 未恢复到原始字节")
                        bad += 1

        print("\n=== 恢复后复跑 ===")
        failed, total, titles, _ = run_vitest("99_restored")
        if failed or total != BASELINE_TOTAL or sorted(titles) != sorted(COLLECTED):
            print(f"  !! 恢复后仍有红或分母不对: {failed}")
            bad += 1
        for path in (API, PAGE):
            if path.read_text(encoding="utf-8") != ORIG[path]:
                print(f"  !! NOT RESTORED: {path}")
                bad += 1
    finally:
        for path, text in ORIG.items():
            path.write_text(text, encoding="utf-8")
    print("RESULT: " + ("前端口径注入自证完成" if bad == 0 else f"{bad} problem(s)"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":  # 战役脚本不许被 import 就跑起来
    sys.exit(main())
