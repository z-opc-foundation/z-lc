#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""卡 TASK-20261025-042 的仓内回归钉：z-lc-admin 部署清单**两处**探针的 timeoutSeconds 预算。

背景（只读复验见卡面 §一）：
  `deploy/k8s/02-deployment.yaml` 的 `readinessProbe` / `livenessProbe` **两处都没写 timeoutSeconds**
  ⇒ K8s 落到**默认 1 秒**；而被探端点 `/api/lc/health` 每次请求要**串行**真探 2 个连接池
  （`DataSourceHealthProber.DEFAULT_TIMEOUT_MS = 2000ms` / 池，线上 `sources` 实读 2 个池）
  ⇒ 端点最坏 ~4s ⇒ **探针预算(1s) < 端点最坏耗时(~4s)** ⇒ DB 一抖动即假 liveness 失败
  （线上 2026-10-25 06:01:44 CST 已发生 1 次 `Liveness probe failed: context deadline exceeded`）。

本钉钉的是这条因果链的**最后一环（配置面）**：两处探针的 `timeoutSeconds` 必须 >= 5
（= 2000ms × 池数 2 + 余量；与同族已修的 `z-ops-admin` / `z-config-web` 同值）。

三条硬规矩（照抄本仓 `_e2e/mutate_*_guard.py` 那一族的老规矩）：

  1. **只读**：本脚本**不写仓库里的任何文件**。注入的变异体全部写在系统临时目录里
     （`tempfile.TemporaryDirectory` 上下文管理器 ⇒ 退出即清，不留残留），
     被测的 `deploy/k8s/02-deployment.yaml` **一个字节都不改**。
     ⇒ 因此它可以随时在共享工作区里跑，不怕把别人的未提交改动冲掉。
  2. **量具版本化自检**：跑之前先核「本地清单 blob == `origin/main` 同路径 blob」，
     不一致 ⇒ 显式 FATAL 退出（rc=3）⇒ 钉测的一定是**入库真相源**，不是工作区里的半成品。
     **没有环境变量可以绕过这条**（不许静默降级成"读工作区就行"）。
  3. **接线状态**：本钉**故意不挂进 maven 测试套件** —— 卡第 1 项（往清单补 `timeoutSeconds`）
     属"需人工确认后执行"，清单没补之前挂进主套件会让 **z-lc 仓整体构建变红** ⇒ 不可接受
     ⇒ 退化为**仓内独立脚本式钉**（照本仓 `_e2e/mutate_*_guard.py` 那一族的跑法）。
     **接线条件**：卡第 1 项落地（清单两处都有 `timeoutSeconds: 5`）之后，再把本脚本挂到
     `mvn verify` 的前置闸。取舍与理由写在卡 TASK-20261025-042 的证据里。

定位方式：**用 YAML 解析按字段名找**，**不按行号、不用 grep**（清单近期动过，行号会漂）。
变异体是**按字段名注入/删除** `timeoutSeconds` 这一行（注入点由 YAML 节点的标记得出），
不是拿行号硬插。

跑法：
    python3 _e2e/mutate_probe_timeout_budget_guard.py            # 全跑：版本自检 + 变异矩阵 + 现网清单核对
    python3 _e2e/mutate_probe_timeout_budget_guard.py --self-test # 只跑变异矩阵（量具灵敏度自证）
    python3 _e2e/mutate_probe_timeout_budget_guard.py --check     # 只跑现网清单核对（守缺陷）

退出码：
    0 = 全绿（变异矩阵全对 + 现网清单两处都达标）
    1 = 现网清单**红**（缺陷仍在：两处缺 `timeoutSeconds` 或低于下限）—— 变异矩阵本身是绿的
    2 = 变异矩阵红（量具自己失灵：阳性对照没过，或某个变异体没被判红）
    3 = FATAL：版本自检不过（本地清单 blob != origin/main blob）/ 清单缺失 / 不是合法 YAML
    4 = 用法错
"""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import time

import yaml

# ---- 判据常量（改这里 = 改判据；改判据必须同步改卡面口径）----
MIN_TIMEOUT_SECONDS = 5
TARGET_DEPLOYMENT = "z-lc-admin"
TARGET_NAMESPACE = "z-lc"
MANIFEST_REL = "deploy/k8s/02-deployment.yaml"
PROBE_KEYS = ("readinessProbe", "livenessProbe")

EXIT_OK = 0
EXIT_CHECK_RED = 1
EXIT_SELF_TEST_RED = 2
EXIT_FATAL = 3
EXIT_USAGE = 4


# ---------------------------------------------------------------- 仓库定位 / git 读数
def repo_root() -> pathlib.Path:
    """仓根 = 本脚本的上上级（_e2e/xxx.py -> 仓根）。不依赖 cwd。"""
    return pathlib.Path(__file__).resolve().parent.parent


def _git(args):
    return subprocess.run(["git", "-C", str(repo_root())] + args,
                          capture_output=True, text=True)


def _git_out(args):
    p = _git(args)
    if p.returncode != 0:
        raise RuntimeError("git %s rc=%d: %s" % (" ".join(args), p.returncode,
                                                (p.stderr or "").strip()))
    return p.stdout.strip()


# ---------------------------------------------------------------- YAML 结构定位
def _map_get(node, key):
    """在 MappingNode 里按**字段名**取值；取不到返回 None。全程不碰行号。"""
    if not isinstance(node, yaml.MappingNode):
        return None
    for k, v in node.value:
        if isinstance(k, yaml.ScalarNode) and k.value == key:
            return v
    return None


def _container_nodes(root):
    """spec.template.spec.containers 里的每个容器节点。"""
    spec = _map_get(root, "spec")
    tmpl = _map_get(spec, "template")
    tspec = _map_get(tmpl, "spec")
    containers = _map_get(tspec, "containers")
    if not isinstance(containers, yaml.SequenceNode):
        return []
    return [c for c in containers.value if isinstance(c, yaml.MappingNode)]


def _find_probe_node(root, probe_key):
    """返回 (容器节点, 探针节点)；找不到抛异常。"""
    for c in _container_nodes(root):
        p = _map_get(c, probe_key)
        if isinstance(p, yaml.MappingNode):
            return c, p
    raise RuntimeError("清单里找不到 %s 节点" % probe_key)


def _timeout_key_node(probe_node):
    """探针节点里 timeoutSeconds 的 key 节点（没有则 None）。"""
    for k, v in probe_node.value:
        if isinstance(k, yaml.ScalarNode) and k.value == "timeoutSeconds":
            return k, v
    return None, None


def _render_timeout_value(value):
    """把判定用的值渲染成 YAML 标量。字符串必须带引号（否则渲染成整数，变异体就造错了）。"""
    if isinstance(value, str):
        return '"%s"' % value
    return str(value)


def set_probe_timeout(text, probe_key, value):
    """按字段名改/加/删某一处探针的 timeoutSeconds，返回新文本。

    value=None  => 删掉该行（本来就没有就是空操作）
    value=<int>  => 有则改值、无则在探针映射末尾插入一行
    插入点由 YAML 节点标记得出（不是写死行号）。
    """
    root = yaml.compose(text)
    if root is None:
        raise RuntimeError("清单为空")
    _container, probe_node = _find_probe_node(root, probe_key)
    key_node, val_node = _timeout_key_node(probe_node)

    lines = text.splitlines(keepends=True)

    if value is None:
        if key_node is None:
            return text  # 本来就没有 => 空操作
        if val_node is not None and val_node.start_mark.line != key_node.start_mark.line:
            raise RuntimeError("%s 的 timeoutSeconds 不是单行标量，删不掉" % probe_key)
        idx = key_node.start_mark.line
        if idx >= len(lines):
            raise RuntimeError("timeoutSeconds 所在行号越界")
        del lines[idx]
        return "".join(lines)

    if key_node is not None:
        idx = key_node.start_mark.line
        if val_node is not None and val_node.start_mark.line != key_node.start_mark.line:
            raise RuntimeError("%s 的 timeoutSeconds 不是单行标量，改不了" % probe_key)
        indent = " " * key_node.start_mark.column
        lines[idx] = "%stimeoutSeconds: %s\n" % (indent, _render_timeout_value(value))
        return "".join(lines)

    # 没有这一行 => 插到探针映射的末尾（子节点最后一行之后）
    at = probe_node.end_mark.line
    if at > len(lines):
        at = len(lines)
    if at == len(lines) and lines and not lines[-1].endswith("\n"):
        lines[-1] = lines[-1] + "\n"
    # 缩进取探针映射**子键**所在列（block mapping 的 start_mark 指向第一个子键，
    # 不是探针键本身那一列）—— 取错列会让 YAML 变成非法缩进。
    if probe_node.value:
        indent = " " * probe_node.value[0][0].start_mark.column
    else:
        indent = " " * (probe_node.start_mark.column + 2)
    lines.insert(at, "%stimeoutSeconds: %s\n" % (indent, _render_timeout_value(value)))
    return "".join(lines)


# ---------------------------------------------------------------- 判据本体
def detect(text):
    """读清单正文，判「两处探针的 timeoutSeconds 是否都 >= 5」。

    返回 (ok, findings[])。findings 是**逐条具名**的失败原因（要能点名是哪一处、为什么）。
    """
    findings = []
    try:
        doc = yaml.safe_load(text)
    except yaml.YAMLError as e:
        return False, ["清单不是合法 YAML: %s" % e]
    if not isinstance(doc, dict):
        return False, ["清单顶层不是映射"]

    meta = doc.get("metadata") or {}
    if meta.get("name") != TARGET_DEPLOYMENT:
        findings.append("清单 metadata.name=%r，不是本钉的目标 %s"
                        % (meta.get("name"), TARGET_DEPLOYMENT))
    if meta.get("namespace") != TARGET_NAMESPACE:
        findings.append("清单 metadata.namespace=%r，不是本钉的目标 %s"
                        % (meta.get("namespace"), TARGET_NAMESPACE))

    spec = doc.get("spec") or {}
    tmpl = spec.get("template") or {}
    tspec = tmpl.get("spec") or {}
    containers = tspec.get("containers") or []
    if not isinstance(containers, list):
        return False, findings + ["spec.template.spec.containers 不是列表"]

    probed = [c for c in containers
              if isinstance(c, dict) and any(k in c for k in PROBE_KEYS)]
    if not probed:
        findings.append("清单里没有任何容器带探针（readinessProbe/livenessProbe）")

    for c in probed:
        name = c.get("name", "?")
        for pk in PROBE_KEYS:
            p = c.get(pk)
            if not isinstance(p, dict):
                findings.append("容器 %s 缺 %s" % (name, pk))
                continue
            if "timeoutSeconds" not in p:
                findings.append("容器 %s 的 %s **没有 timeoutSeconds**（K8s 会落到默认 1s）"
                                % (name, pk))
                continue
            v = p["timeoutSeconds"]
            if isinstance(v, bool) or not isinstance(v, int):
                findings.append("容器 %s 的 %s.timeoutSeconds 不是整数（读到 %r）" % (name, pk, v))
                continue
            if v < MIN_TIMEOUT_SECONDS:
                findings.append("容器 %s 的 %s.timeoutSeconds=%d < %d"
                                % (name, pk, v, MIN_TIMEOUT_SECONDS))
    return (not findings), findings


def detect_file(path):
    return detect(pathlib.Path(path).read_text(encoding="utf-8"))


# ---------------------------------------------------------------- 量具版本化自检
def version_self_check():
    """核「本地清单 blob == origin/main 同路径 blob」。不一致 => FATAL（rc=3）。

    不提供任何环境变量/开关绕过 —— 量具必须测入库真相源。
    """
    manifest = repo_root() / MANIFEST_REL
    if not manifest.is_file():
        raise RuntimeError("清单不存在: %s" % manifest)
    try:
        local = _git_out(["hash-object", MANIFEST_REL])
    except RuntimeError as e:
        raise RuntimeError("读本地 blob 失败: %s" % e)
    try:
        remote = _git_out(["rev-parse", "origin/main:" + MANIFEST_REL])
    except RuntimeError as e:
        raise RuntimeError("读 origin/main 同路径 blob 失败（远端引用不在？）: %s" % e)
    if local != remote:
        raise RuntimeError("清单本地 blob(%s) != origin/main blob(%s) —— "
                           "工作区清单不是入库真相源，量具拒绝在此形态下下结论"
                           % (local[:8], remote[:8]))
    return local, remote


# ---------------------------------------------------------------- 变异矩阵（量具自证）
# 每支都是**相对原始清单文本**构造出来的 ⇒ 清单将来被修好了，这些支的期望**不变**。
MUTANTS = [
    ("CTRL-OK", "两处都补 timeoutSeconds: 5", {"readinessProbe": 5, "livenessProbe": 5}, True),
    ("M1", "两处都写回 timeoutSeconds: 1（缺陷原形）",
     {"readinessProbe": 1, "livenessProbe": 1}, False),
    ("M2", "只修 readiness，liveness 仍是 1",
     {"readinessProbe": 5, "livenessProbe": 1}, False),
    ("M3", "只修 liveness，readiness 仍是 1",
     {"readinessProbe": 1, "livenessProbe": 5}, False),
    ("M4", "readiness 缺 timeoutSeconds（只补了 liveness）",
     {"readinessProbe": None, "livenessProbe": 5}, False),
    ("M5", "liveness 缺 timeoutSeconds（只补了 readiness）",
     {"readinessProbe": 5, "livenessProbe": None}, False),
    ("M6", "两处都缺 timeoutSeconds（清单原形）",
     {"readinessProbe": None, "livenessProbe": None}, False),
    ("M7", "两处 timeoutSeconds: 4（低于下限）",
     {"readinessProbe": 4, "livenessProbe": 4}, False),
    ("M8", "两处 timeoutSeconds: \"5\"（写成字符串，不是整数）",
     {"readinessProbe": "5", "livenessProbe": "5"}, False),
]


def run_mutation_matrix(manifest_text):
    """跑变异矩阵：阳性对照必须绿，每个变异体必须红。返回 (ok, rows[])。"""
    rows = []
    ok = True
    with tempfile.TemporaryDirectory(prefix="zlc-probe-budget-") as td:
        for cid, desc, values, expect_pass in MUTANTS:
            try:
                text = manifest_text
                for pk in PROBE_KEYS:
                    text = set_probe_timeout(text, pk, values.get(pk))
                fx = pathlib.Path(td) / ("%s.yaml" % cid)
                fx.write_text(text, encoding="utf-8")
                passed, findings = detect_file(fx)
            except Exception as e:  # noqa: BLE001 —— 构造失败本身就是"判据没跑起来"
                rows.append({"id": cid, "desc": desc, "expect": "PASS" if expect_pass else "RED",
                             "got": "ERROR", "findings": [str(e)], "ok": False})
                ok = False
                continue
            good = (passed == expect_pass)
            if not good:
                ok = False
            rows.append({"id": cid, "desc": desc, "expect": "PASS" if expect_pass else "RED",
                         "got": "PASS" if passed else "RED",
                         "findings": findings, "ok": good})
    return ok, rows


# ---------------------------------------------------------------- 输出 / 台账
def _cache_dir():
    d = pathlib.Path(os.path.expanduser("~")) / ".cache" / "zlc-probe-budget"
    return d


def emit(lines):
    for ln in lines:
        print(ln)


def main(argv=None):
    ap = argparse.ArgumentParser(description="z-lc-admin 探针 timeoutSeconds 预算钉（卡 TASK-20261025-042）")
    ap.add_argument("--self-test", action="store_true", help="只跑变异矩阵（量具灵敏度自证）")
    ap.add_argument("--check", action="store_true", help="只跑现网清单核对（守缺陷）")
    ap.add_argument("--ledger", default=None, help="台账 JSON 落点（默认 ~/.cache/zlc-probe-budget/ledger.json）")
    args = ap.parse_args(argv)

    if args.self_test and args.check:
        emit(["FATAL: --self-test 与 --check 不能同时给（一次只测一层）"])
        return EXIT_USAGE

    lines = []
    t0 = time.time()

    # ---- 0. 量具版本化自检 ----
    try:
        local_blob, remote_blob = version_self_check()
        lines.append("VERSION-SELFCHECK: PASS  local_blob=%s == origin/main blob=%s"
                     % (local_blob[:8], remote_blob[:8]))
    except RuntimeError as e:
        emit(lines + ["FATAL: %s" % e, "RESULT: FATAL (rc=%d) —— 量具拒绝在非真相源形态下下结论"
                      % EXIT_FATAL])
        return EXIT_FATAL

    manifest_path = repo_root() / MANIFEST_REL
    manifest_text = manifest_path.read_text(encoding="utf-8")
    lines.append("MANIFEST: %s (%d bytes)" % (MANIFEST_REL, len(manifest_text.encode("utf-8"))))
    lines.append("JUDGE: 两处探针 timeoutSeconds 均须 >= %d（> 2000ms/池 × 2 池 + 余量）"
                 % MIN_TIMEOUT_SECONDS)

    self_ok = None
    rows = []
    if not args.check:
        self_ok, rows = run_mutation_matrix(manifest_text)
        lines.append("")
        lines.append("--- 变异矩阵（量具自证：故意改坏，它必须红在哪一条）---")
        for r in rows:
            lines.append("  [%s] %-8s expect=%-4s got=%-5s %s"
                         % ("PASS" if r["ok"] else "FAIL", r["id"], r["expect"], r["got"], r["desc"]))
            for f in r["findings"]:
                lines.append("        · %s" % f)
        lines.append("SELF-TEST: %s（阳性对照 %d 支 + 变异体 %d 支）"
                     % ("PASS" if self_ok else "RED", 1, len(MUTANTS) - 1))

    live_ok = None
    findings = []
    if not args.self_test:
        live_ok, findings = detect(manifest_text)
        lines.append("")
        lines.append("--- 现网清单核对（守缺陷）---")
        if live_ok:
            lines.append("  [PASS] %s 两处探针 timeoutSeconds 均 >= %d" % (MANIFEST_REL, MIN_TIMEOUT_SECONDS))
        else:
            for f in findings:
                lines.append("  [RED ] %s" % f)
            lines.append("  => 该缺陷仍在仓内清单里（卡 TASK-20261025-042 第 1 项"
                         "「往清单补 timeoutSeconds」待人工批准后落地）")

    lines.append("")
    lines.append("RESULT: local=%s  self-test=%s  live-check=%s  (%.1fs)"
                 % (MANIFEST_REL,
                    "-" if self_ok is None else ("PASS" if self_ok else "RED"),
                    "-" if live_ok is None else ("PASS" if live_ok else "RED"),
                    time.time() - t0))

    if self_ok is False:
        rc = EXIT_SELF_TEST_RED
        lines.append("VERDICT: 量具自身失灵（变异矩阵红）⇒ 结论作废")
    elif live_ok is False:
        rc = EXIT_CHECK_RED
        lines.append("VERDICT: 缺陷仍在（现网清单红）⇒ 需人工批准卡第 1 项后补 timeoutSeconds")
    else:
        rc = EXIT_OK
        lines.append("VERDICT: 全绿")
    lines.append("EXIT: rc=%d" % rc)

    emit(lines)

    # ---- 台账落 ~/.cache（/tmp 会被别的会话扫）----
    try:
        ld = pathlib.Path(args.ledger) if args.ledger else _cache_dir() / "ledger.json"
        ld.parent.mkdir(parents=True, exist_ok=True)
        logdir = ld.parent / "logs"
        logdir.mkdir(parents=True, exist_ok=True)
        logfile = logdir / ("guard-%s.log" % time.strftime("%Y%m%d-%H%M%S"))
        logfile.write_text("\n".join(lines) + "\n", encoding="utf-8")
        ld.write_text(json.dumps({
            "card": "TASK-20261025-042",
            "at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
            "repo_root": str(repo_root()),
            "manifest": MANIFEST_REL,
            "manifest_local_blob": local_blob,
            "manifest_origin_main_blob": remote_blob,
            "min_timeout_seconds": MIN_TIMEOUT_SECONDS,
            "self_test_ok": self_ok,
            "self_test_rows": rows,
            "live_check_ok": live_ok,
            "live_findings": findings,
            "rc": rc,
            "log": str(logfile),
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print("LEDGER: %s" % ld)
        print("LOG: %s" % logfile)
    except Exception as e:  # noqa: BLE001 —— 台账写不进去不该改判据结论，只报
        print("WARN: 台账/日志落盘失败（不影响判据结论）: %s" % e)

    return rc


if __name__ == "__main__":
    sys.exit(main())
