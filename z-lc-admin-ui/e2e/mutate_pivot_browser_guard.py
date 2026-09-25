#!/usr/bin/env python3
"""交叉表「未填写」那一列的**浏览器层**注入自证。

跑法（在 z-lc-admin-ui 下，需要 18090 的后端在跑；5274 要让出来）：

    python3 e2e/mutate_pivot_browser_guard.py

为什么单开一支而不是塞进 `mutate_pivot_guards.py`：那一支判的是 vitest，分母是那两个文件
里的 26 条用例；这一支判的是**真构建 + 真浏览器**，分母是 `browser-e2e.mjs` 的 130 条检查。
两层钉的是同一句口径的不同半边 —— 单测层能证明 `pivotColumnLabel('')` 返回那句话，
证明不了这句话走到了渲染、也证明不了那一列的格子没被挪位（`data-col-key` 还在不在）。

只做 P8 一个注入（把列标签的翻译摘掉：空键 `''` 原样上屏），因为这一支要回答的是
**浏览器层那 7 条新检查咬不咬**，不是把十个注入再跑一遍（那十条已由 vitest 层负责）。
判据是"预期红的集合"而不是"至少红一条"：
`…画得出来` 红在列头是空白，`…按行级真值落位` 红在真值的列名对不上，
`…列合计等于没有优先级的记录数` 红在 `indexOf(UNFILLED)` 找不到那一列。
少红一条 = 那条检查是空的；多红一条 = 这一处改动串了别的口径，先查清再改预期。

⚠ 这一支自己 build、自己起 preview：5274 上已经有东西在伺服就直接拒绝启动 —— 挂在别人的
preview 上等于测一份不是我构建的产物。（`browser-e2e.mjs` 里那条产物指纹守卫管的是另一半：
`src` 里最新文件不许比 bundle 新。两道都不是摆设，各自被反证过。）
"""

import os
import re
import signal
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import _mutlock

UI = Path(__file__).resolve().parents[1]
MODEL = UI / "src/views/workspace/pivotModel.ts"
E2E = UI / "e2e/browser-e2e.mjs"
DIST_INDEX = UI / "dist/index.html"
PORT = 5274
BUILD_TIMEOUT = 420
ROUND_TIMEOUT = 900
# browser-e2e.mjs 把读数挂在名字后面：`  FAIL  名字   << 读数`（见其 runNotes.push）。
FAIL_DETAIL_SEP = "   << "

ANCHOR = ("export const pivotColumnLabel = (rawKey: string): string =>"
          " (rawKey.trim() === '' ? UNFILLED_GROUP_LABEL : rawKey);")
MUTANT = "export const pivotColumnLabel = (rawKey: string): string => rawKey;"

EXPECTED_RED = [
    "未填写那一列画得出来：没有任何一根列是空白标题，且它认领的还是后端那个空键",
    "未填写那一列的格子按行级真值落位，不是摆在最右边的装饰列",
    "未填写那一列的列合计等于没有优先级的记录数，总计仍是全部记录数",
]

LOG_DIR = Path(tempfile.gettempdir()) / "zlc_mut_pivot_browser_logs"
LOG_DIR.mkdir(exist_ok=True)


def port_busy() -> bool:
    for host in ("127.0.0.1", "::1"):
        family = socket.AF_INET if ":" not in host else socket.AF_INET6
        with socket.socket(family, socket.SOCK_STREAM) as sock:
            sock.settimeout(0.4)
            if sock.connect_ex((host, PORT)) == 0:
                return True
    return False


def bundle_name() -> str:
    html = DIST_INDEX.read_text(encoding="utf-8") if DIST_INDEX.exists() else ""
    match = re.search(r'assets/(index-[^"\']+\.js)', html)
    return match.group(1) if match else "<dist/index.html 里没有 index-*.js>"


def build(label: str) -> None:
    proc = subprocess.run(["npm", "run", "build"], cwd=UI,
                          capture_output=True, text=True, timeout=BUILD_TIMEOUT)
    (LOG_DIR / f"{label}_build.log").write_text(proc.stdout + proc.stderr, encoding="utf-8")
    if proc.returncode != 0:
        raise SystemExit(f"build 失败（{label}），见 {LOG_DIR / (label + '_build.log')}")
    print(f"    built: {bundle_name()}")


def start_preview() -> subprocess.Popen:
    proc = subprocess.Popen(["npx", "vite", "preview", "--port", str(PORT)],
                            cwd=UI, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            start_new_session=True)
    for _ in range(60):
        if port_busy():
            return proc
        time.sleep(0.5)
    os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
    raise SystemExit(f"preview 起不来（:{PORT} 30s 内没在听）")


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
    proc = subprocess.run(["node", str(E2E)], cwd=UI, capture_output=True, text=True,
                          timeout=ROUND_TIMEOUT, env={**os.environ, "E2E_REPEATS": "1"})
    text = proc.stdout + proc.stderr
    keep = LOG_DIR / f"{label}.log"
    keep.write_text(text, encoding="utf-8")
    if "门禁拒绝开跑" in text:
        print(f"  !! {label}: 被产物指纹守卫拦下（构建没跟上源码），见 {keep}")
        return [f"<build-guard:{label}>"]
    if proc.returncode not in (0, 1):
        raise SystemExit(f"browser-e2e 退出码 {proc.returncode}（不是 0/1）→ 这一轮没有结论，见 {keep}")
    # ⚠ 门禁打印的是 `  FAIL  <名字>   << <读数>`，必须先把读数剥掉再和 EXPECTED_RED 比。
    # 不剥的话两侧集合恒为空交集：同一批名字会**同时**被判成"预期却没红"和"预期外的红"
    # （第一次跑就是这么报的，而它注入掉的那 3 条其实条条红对了）。
    failed = [line.split(FAIL_DETAIL_SEP)[0].strip()
              for line in re.findall(r"^  FAIL  (.+?)$", text, flags=re.M)]
    passed = len(re.findall(r"^  PASS  ", text, flags=re.M))
    print(f"  {label}: PASS {passed} / FAIL {len(failed)}")
    return failed


def judge(label: str, failed: list[str]) -> int:
    for title in failed:
        print(f"    RED ({'预期' if title in EXPECTED_RED else '未预期'}) {title}")
    hard = [e for e in EXPECTED_RED if e not in failed]
    extra = [f for f in failed if f not in EXPECTED_RED]
    if not hard and not extra:
        print(f"  OK  {label}: 预期 {len(EXPECTED_RED)} 条全红，无一条连带红")
        return 0
    if hard:
        print(f"  !! {label}: 预期变红却没红（这几条浏览器检查是空的）: {hard}")
    if extra:
        print(f"  !! {label}: 出现预期之外的红 {extra} —— 不许加白名单了事，"
              "先拿样本证明这一处注入换掉了哪一支断言，再改预期")
    return 1


def main() -> int:
    if port_busy():
        print(f"!! :{PORT} 已经有东西在伺服 —— 不是我起的 preview 不能当门禁介质。"
              "\n   先确认那是谁（`lsof -nP -iTCP:{PORT} -sTCP:LISTEN`），是自己这一支的量具再停。")
        return 2

    original = MODEL.read_text(encoding="utf-8")
    if original.count(ANCHOR) != 1:
        print(f"!! anchor 在 pivotModel.ts 里出现 {original.count(ANCHOR)} 次，注入无法定位")
        return 2

    bad = 0
    preview = None
    try:
        print("=== 基线（修复态：build + 起 preview + 跑一轮）===")
        build("00_baseline")
        preview = start_preview()
        if run_round("00_baseline"):
            print("  !! 基线就有红，注入结果无法归因；先修基线")
            return 2

        print("\n=== P8 摘掉列标签翻译（空键 '' 原样上屏）===")
        MODEL.write_text(original.replace(ANCHOR, MUTANT), encoding="utf-8")
        try:
            build("01_p8")
            bad += judge("P8 浏览器层", run_round("01_p8"))
        finally:
            MODEL.write_text(original, encoding="utf-8")
            if MODEL.read_text(encoding="utf-8") != original:
                print("  !! pivotModel.ts 未恢复到原始内容")
                bad += 1

        print("\n=== 恢复后复跑 ===")
        build("02_restored")
        if (failed := run_round("02_restored")):
            print(f"  !! 恢复后仍有红: {failed}")
            bad += 1
    finally:
        if preview is not None:
            stop_preview(preview)

    print(f"\n{'FAILED: ' + str(bad) if bad else '浏览器层那几条新检查咬得住 P8'}")
    return 1 if bad else 0


if __name__ == "__main__":
    _mutlock.acquire(Path(__file__).name)
    try:
        sys.exit(main())
    finally:
        _mutlock.release()
