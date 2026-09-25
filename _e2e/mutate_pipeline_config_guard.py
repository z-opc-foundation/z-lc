#!/usr/bin/env python3
"""#42 的**单测层**注入自证: 让"阶段参数没人读"那道闸的十条新检查各自证明它有牙。

为什么单独一支脚本, 而不是加进 _e2e/mutate_pipeline_wiring_guard.py:
那一支每支注入都要重新打包 + 重启 18090 的 JVM (12 支约 25 分钟), 而它跑单测只跑
`PipelineWriteChainTest` 一个类。#42 新补的 10 条里只有 1 条住在那个类 —— 剩下 9 条住在
PipelineStagesTest / PipelineConfigServiceTest, 在那一支里根本没有载体。这里三层一起跑,
不碰 jar、不碰 JVM, 一支约 30 秒。
部署件层能看见的那两条 (`阶段参数没人读 -> 拒` / `阶段参数不是对象 -> 拒`) 走那一支的 P13/P14。

六支注入, 各自打掉闸的一条保证 (预期红集是**动手前**按调用点推出来的, 不是跑完回填的):
  U1  写入口不再拒"没人读的参数" (validateAndResolve 直接回阶段链)
  U2  未知参数只点名第一个                        -> 只有"两个键都该出现"那条红
  U3  config 形状不对时干脆不记账                  -> 只有"不是对象"那条红
  U4  把 trimStrings 登记成 TYPE_CONVERT 的参数, 但处理器一个字都不读
      —— 这是**第一个真参数落地时最可能犯**的错: 词表加了, 读的人没加。
  U5  运行期也照写入口硬拒 (Pipeline 改调 validateAndResolve)
      -> 唯一载体是 PipelineWriteChainTest: "不对称"这件事在 PipelineStagesTest 里看不见,
         因为那里的 resolve() 是直接被调的。这正是那一支要单独存在的理由。
  U6  "凡是带 config 就拒" (连 config:{} 一起拒)
      —— 反向注入: 摘掉接受侧的口子, 界面默认写的那份 config:{} 就存不进去了。
      只测拒绝的网, 写成"永远抛异常"也能全绿; 这一支就是拦那件事的。

判据 (一条都不放宽):
  * 基线必须全绿, 且**每支注入的那一轮分母恒等于基线** —— 少跑几条不是"更少的红", 是没有判定
  * 每个预期红的名字必须先出现在基线的绿清单里 (名字打错 ≠ 注入没抓到)
  * 每支注入都要证明 `PipelineStages.class` 的字节真的变了 (没变 = 编译没吃到这处改动 = 假绿)
  * 逐名比对: 多红/少红/红得不是那几条, 一律 MISMATCH 并非零退出
  * 源文件按字节快照还原, 还原后再跑一遍整份
"""
import hashlib
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

STAGES = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/config/PipelineStages.java"
PIPELINE = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/pipeline/Pipeline.java"
CLASSES = [
    ROOT / "z-lc-core/target/classes/com/zifang/z/lc/core/pipeline/config/PipelineStages.class",
    ROOT / "z-lc-core/target/classes/com/zifang/z/lc/core/pipeline/Pipeline.class",
]
TESTS = "PipelineStagesTest,PipelineConfigServiceTest,PipelineWriteChainTest"
LOG_DIR = Path(os.path.expanduser("~/.cache/zlc42/unit_mut"))
LOG_DIR.mkdir(parents=True, exist_ok=True)

MIN_BASELINE_TOTAL = 55  # 三层实测 23+26+14=63; 低于这条就是有一个类没跑起来


def sh(cmd, timeout=1800):
    return subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True, timeout=timeout)


# ---- 注入点: anchor 必须在文件里**恰好出现一次**, 否则这一支直接 SKIPPED (不猜) -----------------
U1_A = '        if (!unread.isEmpty()) {'
# 第一版把替换写成了"另一段代码"(结构被拆, javac 直接报 statements not expected outside of methods),
# 于是这一支 SKIP。死分支才是这处闸的正确摘法: 语法完整、unread 照算、只有拒的那一下没了。
U1_B = '        if (false) {'

U2_A = ('                        unread.add("阶段 [" + type + "] 里的参数 " + join(unknown)\n'
        '                                + " 没有任何处理器读取" + acceptedConfigHint(type));')
U2_B = ('                        unread.add("阶段 [" + type + "] 里的参数 " + unknown.get(0)\n'
        '                                + " 没有任何处理器读取" + acceptedConfigHint(type));')

U3_A = ('                    unread.add("阶段 [" + type + "] 的 config 不是对象 (实际: "\n'
        '                            + shorten(configNode.asText()) + ")" + acceptedConfigHint(type));')
U3_B = '                    // mutant: 形状不对也不记账'

U4_A = ('        for (String type : PROCESSOR_BY_TYPE.keySet()) {\n'
        '            m.put(type, Collections.<String>emptyList());\n'
        '        }')
U4_B = ('        for (String type : PROCESSOR_BY_TYPE.keySet()) {\n'
        '            m.put(type, "TYPE_CONVERT".equals(type)\n'
        '                    ? java.util.Arrays.<String>asList("trimStrings")\n'
        '                    : Collections.<String>emptyList());\n'
        '        }')

# U5 打在**挂接处**而不是 resolve() 里: "不对称"是 Pipeline 这个调用方做的选择,
# 改共享函数会把写入口那一半一起改掉 (两支注入就分不开谁打的是哪条保证了)。
# 第一版只替换了调用表达式, LHS 还是 Resolution => incompatible types, 整支 SKIP。
U5_A = ('            PipelineStages.Resolution parsed =\n'
        '                    PipelineStages.resolve(config.getTriggerEvent(), config.getStages());')
U5_B = ('            PipelineStages.Resolution parsed =\n'
        '                    PipelineStages.resolve(config.getTriggerEvent(), config.getStages());\n'
        '            if (!parsed.unreadConfig().isEmpty()) {\n'
        '                throw new IllegalArgumentException("mutant: 运行期也拒装饰参数");\n'
        '            }')

U6_A = '                } else if (configNode.size() > 0) {'
# 第一版写成 `else if (true)`, 里面那个"逐个键对词表"的循环照旧 —— 空 config 一个未知键都数不出来,
# 于是这一支**行为上什么都没改**, 两条预期红稳稳是绿的。"凡是带 config 就拒"必须自己记这一笔。
U6_B = ('                } else if (configNode.isObject()) {\n'
        '                    if (configNode.size() == 0) {\n'
        '                        unread.add("阶段 [" + type + "] 带了 config (mutant: 空的那个也算)");\n'
        '                    }')

# 写入口那半边是**两处**调用 (create / update); U1 打在它们共同调用的 validateAndResolve 里,
# 所以一处注入同时打掉两道门 —— 预期红集里 create/update 两条一起红就是这件事的证据。
# tag -> (说明, [(文件, anchor, 替换)...], 预期红 (Class.method))
RUNS = [
    ("U1", "写入口不再拒没人读的阶段参数",
     [(STAGES, U1_A, U1_B)],
     # 第五红 (rejectsNonObject...) 是第一轮实测补进来的, 不是连带噪声放白: "config 不是对象"和
     # "键没人读"记在**同一个** unreadConfig 清单里, 摘掉那道 throw 就同时放过了两种形状。
     # 少预期这一条等于承认"U1 只影响未知键", 而那与代码结构不符。
     ["PipelineStagesTest.rejectsStageParamsTheEngineDoesNotReadAndNamesStageAndEachKey",
      "PipelineStagesTest.namesEveryUnknownKeyInsteadOfJustTheFirst",
      "PipelineStagesTest.rejectsNonObjectStageConfigInsteadOfIgnoringIt",
      "PipelineConfigServiceTest.createRejectsStageParamsTheEngineDoesNotRead",
      "PipelineConfigServiceTest.updateRejectsStageParamsToo"]),
    ("U2", "未知参数只点第一个名",
     [(STAGES, U2_A, U2_B)],
     ["PipelineStagesTest.namesEveryUnknownKeyInsteadOfJustTheFirst"]),
    ("U3", "config 不是对象时不记账",
     [(STAGES, U3_A, U3_B)],
     ["PipelineStagesTest.rejectsNonObjectStageConfigInsteadOfIgnoringIt"]),
    ("U4", "词表登记了参数但处理器不读",
     [(STAGES, U4_A, U4_B)],
     ["PipelineStagesTest.noStageReadsAnyParameterYet",
      "PipelineStagesTest.rejectsStageParamsTheEngineDoesNotReadAndNamesStageAndEachKey",
      "PipelineConfigServiceTest.createRejectsStageParamsTheEngineDoesNotRead"]),
    ("U5", "运行期不再容忍装饰参数 (不对称被抹平)",
     [(PIPELINE, U5_A, U5_B)],
     ["PipelineWriteChainTest.unreadStageParamsWarnButDoNotBlockWrites"]),
    ("U6", "连 config:{} 一起拒 (闸按住正常路径)",
     [(STAGES, U6_A, U6_B)],
     ["PipelineStagesTest.emptyOrDefaultStageConfigIsAccepted",
      "PipelineConfigServiceTest.emptyStageConfigIsAcceptedOnBothWriteEntrances"]),
]

FILES = [STAGES, PIPELINE]
ONLY = {x.strip() for x in (sys.argv[2].split(",") if sys.argv[1:2] == ["--only"] else []) if x.strip()}
if sys.argv[1:2] == ["--only"] and not ONLY:
    raise SystemExit("--only 后面是空的: 一支都不跑不等于「全跑」, 那是「没有判定」")


def snap(path):
    return path.read_bytes()


def fp():
    return {p.name: (hashlib.sha256(p.read_bytes()).hexdigest()[:16] if p.exists() else None)
            for p in CLASSES}


def run_tests(label):
    """(分母, 红的 Class.method 集合, 基线绿名集合)。跑不起来就抛 —— 不返回"没有红"。"""
    p = sh(["mvn", "-o", "-B", "test", "-pl", "z-lc-core", f"-Dtest={TESTS}"])
    out = p.stdout + p.stderr
    (LOG_DIR / f"{label}.log").write_text(out)
    totals = []
    green = set()
    for cls in TESTS.split(","):
        m = re.findall(rf"Tests run: (\d+), Failures: \d+, Errors: \d+[^\n]*in [\w.]*{cls}\b", out)
        if not m:
            # 实测教训: 第一轮 U1/U5 就是这一支 —— 表面是"类没跑起来", 真因是注入把源码改得 javac
            # 都过不去。两种原因的处置完全不同 (改注入 vs 查 -Dtest 名字), 所以必须当场说清。
            comp = re.search(r"COMPILATION ERROR[\s\S]{0,600}", out)
            why = ("注入编不过: " + " / ".join(re.findall(r"\.java:\[\d+,\d+\] [^\n]+", comp.group(0))[:3])
                   if comp else f"-Dtest 里没有 {cls} 这个类 (名字打错?)")
            raise RuntimeError(f"{label}: surefire 没打印 {cls} 的汇总行 -> {why}; tail:\n"
                               + "\n".join(out.splitlines()[-25:]))
        totals.append(sum(int(x) for x in m))
    red = sorted({f"{c}.{mm}" for line in out.splitlines() if "<<<" in line
                  for c in ("PipelineStagesTest", "PipelineConfigServiceTest", "PipelineWriteChainTest")
                  for mm in re.findall(rf"{c}\.([A-Za-z0-9_]+)", line)})
    # 基线绿清单: surefire 不逐个列绿名, 用 @Test 方法名当参照系 (红的名字必须是一个真的测试方法)
    for src in (ROOT / "z-lc-core/src/test/java/com/zifang/z/lc/core/pipeline/config/PipelineStagesTest.java",
                ROOT / "z-lc-core/src/test/java/com/zifang/z/lc/core/pipeline/config/PipelineConfigServiceTest.java",
                ROOT / "z-lc-core/src/test/java/com/zifang/z/lc/core/pipeline/PipelineWriteChainTest.java"):
        cls = src.stem
        green |= {f"{cls}.{m}" for m in re.findall(r"public void ([A-Za-z0-9_]+)\(", src.read_text())}
    return sum(totals), set(red), green


def apply(edits):
    for path, anchor, repl in edits:
        text = path.read_text()
        n = text.count(anchor)
        if n != 1:
            raise RuntimeError(f"anchor 在 {path.name} 里出现 {n} 次 (要恰好 1 次), 这一支不猜: \n{anchor[:90]}")
        path.write_text(text.replace(anchor, repl))


def main():
    acquire_lock("mutate_pipeline_config_guard.py")
    originals = {p: snap(p) for p in FILES}
    try:
        return run_all(originals)
    finally:
        for p, blob in originals.items():
            if p.read_bytes() != blob:
                print(f"!! 还原 {p.name}", flush=True)
                p.write_bytes(blob)
        release_lock()


def run_all(originals):
    ok = True
    if ONLY:
        print(f"  !! 本轮只跑 {len(ONLY)}/{len(RUNS)} 支（--only {','.join(sorted(ONLY))}）: "
              "其余各支的判定**没有**在这一轮做过, 别把这份日志当整族绿")
    print("=== 基线（修复态）===", flush=True)
    total, red, green = run_tests("00_baseline")
    if red:
        raise SystemExit(f"基线不干净, {len(red)} 条红: {sorted(red)[:5]} -> 先修基线再谈注入")
    if total < MIN_BASELINE_TOTAL:
        raise SystemExit(f"基线只跑出 {total} 条 (<{MIN_BASELINE_TOTAL}) -> 有测试类没跑起来, "
                         "空参照集会让每一条预期红判定假绿")
    print(f"    mvn: {total} tests, 0 failed", flush=True)

    for tag, why, edits, expect in RUNS:
        if ONLY and tag not in ONLY:
            continue
        print(f"\n=== {tag}: {why} ===", flush=True)
        for p in FILES:
            blob = originals[p]
            if p.read_bytes() != blob:
                print(f"  !! 进入 {tag} 前发现 {p.name} 不是原始内容, 先写回", flush=True)
                p.write_bytes(blob)
        before = fp()
        apply(edits)
        try:
            this_total, this_red, _ = run_tests(tag)
        except Exception as ex:
            print(f"  SKIP {tag}: {ex}", flush=True)
            ok = False
            continue
        after = fp()
        changed = sorted(k for k in before if before[k] != after[k])
        if not changed:
            print(f"  FAIL {tag}: 产物字节没变 ({before}) -> 这处注入压根没进 classpath, "
                  "下面的红/绿都与之无关", flush=True)
            ok = False
        else:
            print(f"  产物已变: {', '.join(changed)}", flush=True)
        if this_total != total:
            print(f"  FAIL {tag}: 分母从 {total} 变成 {this_total} -> 有测试类中途没跑完, "
                  "这一轮没有判定", flush=True)
            ok = False
            continue
        miss = sorted(set(expect) - this_red)
        extra = sorted(this_red - set(expect))
        for name in sorted(this_red):
            print(f"  FAIL {name}", flush=True)
        if miss or extra:
            ok = False
            print(f"  MISMATCH {tag}", flush=True)
            if miss:
                print("    预期红但仍是绿的: " + ", ".join(miss), flush=True)
            if extra:
                print("    没预期到却红了 (连带?): " + ", ".join(extra), flush=True)
        else:
            print(f"  OK {tag}: 预期 {len(expect)} 条全红, 无一条连带红", flush=True)
        phantom = sorted(n for n in expect if n not in green)
        if phantom:
            ok = False
            print(f"  FAIL {tag}: 预期红的名字不是真的测试方法: {phantom}", flush=True)
        for p in FILES:
            p.write_bytes(originals[p])

    print("\n=== 恢复后复跑 ===", flush=True)
    this_total, this_red, _ = run_tests("99_restored")
    print(f"    mvn: {this_total} tests, {len(this_red)} failed", flush=True)
    for p in FILES:
        if p.read_bytes() != originals[p]:
            ok = False
            print(f"  FAIL {p.name} 还原后字节不一致", flush=True)
    if not ok:
        raise SystemExit("RESULT: #42 单测层注入自证**没有**通过 (见上面 MISMATCH / SKIP / FAIL)")
    print(f"\nRESULT: #42 那六支各自打掉一句保证；恢复态 {this_total} 条全绿")
    return True


if __name__ == "__main__":
    sys.exit(0 if main() else 1)
