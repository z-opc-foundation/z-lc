#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缺陷 #48 在**部署层**的注入自证 —— 打的是 18090 上真正跑着的那支 fat jar。

为什么这一层还要再来一遍: `_e2e/mutate_permission_service_guard.py` 那 14 支量的是
QueryWrapper 拼出来的**条件串**(以及 MockMvc 里的装配)，而 `[15t]` 那 30 支探针量的是
"H2 里真的落了几行、`/check` 真的答什么"。同一句查重写法在条件串那一层看着是对的、到了
真库上才见分晓 (`entity_code = NULL` 在 SQL 里恒为 unknown 这件事只有真库会替你说一遍)。
少了这一层，"部署产物里带着这个修复"就还是口头承诺。

十四支变异，预期红集是**先写下再去量**的 (对不上分两种走法: 预期漏了就把因果补进注释，
预期里那一支从没红过、且查明探针看不见猎物 ⇒ 改的是探针不是预期)。
D1/D2/D3/D6 直接抄 #48 修之前的原码，其余是邻近的、人真会写出来的形状:
  D1  应用级查重退回 eq(entity_code, null)      -> 不选实体时每点一次多一行
  D2  /check 丢掉「或应用级」那一支              -> 矩阵亮着而 /check 答拒绝
  D3  grant 不走词表闸                          -> 任何字符串落库，下游一串跟着塌
  D4  /check 不走词表闸                         -> 问不出口的问题被答成"没权限"
  D5  实体不做归一 ("" 与空白原样落库)            -> 多出一行"看着像应用级、谁也查不到"
  D6  控制器不再钉租户                          -> body 里写什么租户就存什么
  D7  归一把 null 落成空串                       -> 与查重/判定的 IS NULL 各认一种
  D8  应用级判定连范围都不筛                     -> 某实体的授权冒充"整个应用都能干"
  D9  「或应用级」写成 isNotNull                 -> 任一实体的授权对任一实体都算数
  D10 两个分支写反 (实体那一支只认应用级)          -> deal 的单独授权查不中
  D11 控制器只看返回值、不看"删了几行"             -> 后端 400 也报"已回收"
  D12 回收丢掉 id 条件                          -> 一句回收把本租户全部授权删光
  D13 回收的租户条件写成 IS NULL                -> 本租户那一条谁也回收不掉
  D14 「整个应用」那一档查成空串范围              -> 应用级授权对任何问法都答拒绝

六支 `ok(...)` 夹具探针 (建 app、四次"授得进去"、一次"再授一次") 不进判红记账: 它们问的是
"合法输入回不回 success"，这十四支改的都是查重/归一/判定/租户口径，没有一支动受理路径 ——
把它们算进账会让"预期红集"退化成"什么都不红也算过"。

首轮实测改了三处账，每一处都写清了因果: D3 (我以为大小写漂移会让下游判定跟着比不中 —— 恰恰因为
库分大小写，'view' 与 'VIEW' 各成一行，按值过滤的探针反倒看不见破坏，只有"整份清单等于什么"那支
看得见)、D7 (写读两侧共用同一个归一函数，null → 空串 会一起漂，判定那三支因此测不到它)、D12
(删过头时"回收成功/清单里没有"两支读到它们期望的结果，多删由 /list 那两支红)。两支"留空问应用级"
的探针一整轮没红，也不是它们空跑: 十三支里没有一支把"整个应用"写成另一种范围形状，所以补了 D14。

用法: python3 _e2e/mutate_permission_deployed_guard.py   (18090 由它自己重启)
"""
import hashlib
import io
import os
import re
import subprocess
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path("/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-lc")
sys.path.insert(0, str(ROOT / "z-lc-admin-ui" / "e2e"))
from _mutlock import acquire as acquire_lock, release as release_lock  # noqa: E402

SVC = ROOT / "z-lc-core/src/main/java/com/zifang/z/lc/core/permission/PermissionService.java"
CTL = ROOT / "z-lc-web/src/main/java/com/zifang/z/lc/web/controller/PermissionController.java"
JAR = ROOT / "z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
HEALTH = "http://localhost:18090/api/lc/health"
LOGS = Path.home() / ".cache/zlc48/deployed_logs"
BAK = Path.home() / ".cache/zlc48/deployed_bak"
BOOT_LOG = LOGS / "boot.log"
# 预检 marker: 修复没进 jar 就别开始量，否则读到的"谁红了"是 stale 产物的行为。
MARKERS = {
    "BOOT-INF/lib/": {
        "com/zifang/z/lc/core/permission/PermissionService.class":
            ["canonical", "trimmedToNull", "isNull", "hasPermission"],
        "com/zifang/z/lc/core/permission/PermissionKeys.class": ["EXPORT", "canonical"],
    },
}

# 探针名一律从 _e2e/e2e_api_test.py 的 [15t] 那一段里抄，不凭印象。
N_WIBBLE_REJECT = "词表外的权限项在写入口就被拒（以前任何字符串都存得进去，矩阵却把它显示成已授予）"
N_WIBBLE_MSG = "拒的时候点名允许哪几个词、并复述被拒的那个（不点名=没人知道该改成什么）"
N_WIBBLE_NOEFFECT = "被拒的那一条一行都没留下（没写进去才是唯一的拒）"
N_CANONICAL = "存进去的是规范形态（trim + 大写），否则那个大小写敏感的 = 比不中、库里成并排两行、界面却亮着"
N_APPWIDE_NULL = "未指定实体时授的是整个应用，库里落成 NULL 而不是空串"
N_WIDE_ONE = "应用级授权反复点只有一行（旧查重写 entity_code = NULL 恒不匹配，不选实体时每点一次多一行）"
N_BLANK_NULL = "空串实体归一成 NULL 并与应用级那一条查重合并（不许多出一行「看着像应用级、谁也查不到」的）"
N_COVERS_DEAL = "应用级那条 VIEW 覆盖具体实体 deal（旧口径只比 entity_code = ?，矩阵亮着而 /check 答拒绝）"
N_CHECK_APPWIDE = "/check 的 entityCode 留空 = 问整个应用那一档，同样成立（旧接口把这个参数写成必填）"
N_CHECK_400 = "/check 对词表外的项回 400，不静默答拒绝（那是把配置错误读成没权限）"
N_ENTITY_ONLY = "某个实体的单独授权不许冒充整个应用都能删"
N_DEAL_OK = "而对 deal 本身它是允许的"
N_DEAL_OTHER = "而对别的实体它不算数"
N_TENANT_PIN = "落库的租户是钉死的那个，调用方自报的被覆写（以前 body 里写什么就存什么）"
N_TENANT_READ = "钉租户必须是写读两侧的同一口径: 刚授的那条在本租户清单里读得回来（不是被悄悄丢掉）"
N_TENANT_DEDUPE = "查重也在同一租户口径内: 自报租户没有多骗出一行（多一行=同一条授权有两个说法）"
N_TENANT_CHECK = "刚授的那条在 /check 里答允许（钉的是 default 这一侧，读写对得上）"
N_REVOKE_OK = "回收一条真实存在的授权"
N_REVOKE_GONE = "回收之后清单里真的没有它（回读清单，不是看接口回了个 true）"
N_REVOKE_AGAIN = "再回收同一个 id 是 400 而不是成功（旧实现无条件回 success，界面照着报已回收）"
N_REVOKE_FAKE = "回收一个从没存在过的 id 同样 400"
N_LIST_ENTITY = "/list 按实体筛时不给应用级那些行（与 /check 的「实体或应用级」不同口径）"
N_LIST_PRIORITY = "/list 的 roleCode 优先于 entityCode: 两个一起给时实体那个被静默丢掉（页面把筛选搬到客户端的依据）"
N_CLEANUP = "收尾: 本租户这一侧的探针行全部回收干净（留在库里会让下一轮的分母漂）"

TRACKED = {
    N_WIBBLE_REJECT, N_WIBBLE_MSG, N_WIBBLE_NOEFFECT, N_CANONICAL, N_APPWIDE_NULL, N_WIDE_ONE,
    N_BLANK_NULL, N_COVERS_DEAL, N_CHECK_APPWIDE, N_CHECK_400, N_ENTITY_ONLY, N_DEAL_OK,
    N_DEAL_OTHER, N_TENANT_PIN, N_TENANT_READ, N_TENANT_DEDUPE, N_TENANT_CHECK, N_REVOKE_OK,
    N_REVOKE_GONE, N_REVOKE_AGAIN, N_REVOKE_FAKE, N_LIST_ENTITY, N_LIST_PRIORITY, N_CLEANUP,
}

# 一整轮都没红过的受跟踪探针，必须写明"谁红它"，否则就是没人看见的守卫。
WHY_NEVER_RED = {}

A_DEDUPE = ('        if (entityCode == null) {\n'
            '            same.isNull("entity_code");\n'
            '        } else {\n'
            '            same.eq("entity_code", entityCode);\n'
            '        }')
A_CHECK_AND = '            q.and(w -> w.eq("entity_code", scope).or().isNull("entity_code"));'
A_CHECK_ISNULL = ('        if (scope == null) {\n'
                  '            q.isNull("entity_code");\n'
                  '        } else {')
A_GRANT_CANON = '        String permission = PermissionKeys.canonical(entity.getPermission());'
A_CHECK_CANON = '        String wanted = PermissionKeys.canonical(permission);'
A_ENTITY_TRIM = '        String entityCode = trimmedToNull(entity.getEntityCode());'
A_HELPER_NULL = ('    private static String trimmedToNull(String value) {\n'
                 '        if (value == null) {\n'
                 '            return null;\n'
                 '        }')
A_PIN_TENANT = ('        if (entity != null) {\n'
                '            entity.setTenantCode(DEFAULT_TENANT);\n'
                '        }')
A_REVOKE_ZERO = '        if (removed == 0) {'
A_REVOKE_DELETE = ('        return permissionMapper.delete(new QueryWrapper<PermissionEntity>()\n'
                   '                .eq("tenant_code", require(tenantCode, "tenantCode"))\n'
                   '                .eq("id", id));')

RUNS = [
    ("D1 应用级查重退回 entity_code = NULL", [(SVC, A_DEDUPE, '        same.eq("entity_code", entityCode);')],
     {N_WIDE_ONE, N_BLANK_NULL, N_TENANT_DEDUPE, N_LIST_PRIORITY}),
    ("D2 /check 只比实体那一支", [(SVC, A_CHECK_AND, '            q.eq("entity_code", scope);')],
     {N_COVERS_DEAL}),
    ("D3 grant 不走词表闸", [(SVC, A_GRANT_CANON, '        String permission = entity.getPermission().trim();')],
     # 首轮我把"下游判定/查重会跟着比不中"写进预期，实测那三支全绿，原因量出来了:
     # dev 这套 H2 的 `=` **确实**分大小写，所以 ` view ` 落成的 'view' 与后来 `VIEW` 那三次授权
     # 各自成行 —— 库里最终是 'WIBBLE…' + 'view' + 'VIEW' 三行，"数一数等于 1" 的那三支
     # 按 permission == 'VIEW' 过滤，恰好仍只看到一行，于是它们**看不见这一支的破坏**。
     # 看得见的是"整份清单必须恰好等于 ['VIEW']"那一支 (N_CANONICAL) 与按 role 筛出来的那一支。
     # 换句话说: 按值过滤的探针天然放过形态漂移，形态要按整份读回来对账才测得出。
     {N_WIBBLE_REJECT, N_WIBBLE_MSG, N_WIBBLE_NOEFFECT, N_CANONICAL, N_LIST_PRIORITY}),
    ("D4 /check 不走词表闸", [(SVC, A_CHECK_CANON, '        String wanted = permission.trim();')],
     {N_CHECK_400}),
    ("D5 实体不做归一（空串原样落库）", [(SVC, A_ENTITY_TRIM, '        String entityCode = entity.getEntityCode();')],
     {N_BLANK_NULL, N_LIST_PRIORITY}),
    ("D6 控制器不再钉租户", [(CTL, A_PIN_TENANT, '')],
     # 只红两支是**对的**: 自报那一条落到别的租户后，本租户清单读不到它；紧接着
     # "同一个键在默认租户下再授一次"确实授进来了，所以查重/判定/回收三步看到的仍是正常形状。
     # 这一支不给"跨租户行会不会漏进清单"作证 —— 接口层种不出那样的行，见 [15t] 的覆盖边界注释。
     {N_TENANT_PIN, N_TENANT_READ}),
    ("D7 归一把 null 落成空串", [(SVC, A_HELPER_NULL,
                                 '    private static String trimmedToNull(String value) {\n'
                                 '        if (value == null) {\n'
                                 '            return "";\n'
                                 '        }')],
     # 判定那三支没红，而且原因很干净: grant 与 hasPermission 用的是**同一个** trimmedToNull，
     # 所以 null → "" 在写读两侧同时漂移 —— /check 留空时 scope 也变成 ""，走的不再是
     # `isNull` 那一支而是"具体实体"那一支 (`eq('') OR IS NULL`)，正好命中库里那行 ""。
     # 于是这一支真正测到的是"库里落成什么形态"与"空串那一条与它不成同一行"，不是判定。
     # 红出来的 N_LIST_PRIORITY 是连带: 空串探针授出的那一行与应用级那行不成同一行，
     # 按 role 筛就多出第二行 VIEW。
     {N_APPWIDE_NULL, N_BLANK_NULL, N_LIST_PRIORITY}),
    ("D8 应用级那一档连范围都不筛", [(SVC, A_CHECK_ISNULL,
                                     '        if (scope == null) {\n'
                                     '            // 变异 D8: 忘了加范围条件\n'
                                     '        } else {')],
     {N_ENTITY_ONLY}),
    ("D9 「或应用级」写成 isNotNull", [(SVC, A_CHECK_AND,
                                       '            q.and(w -> w.eq("entity_code", scope).or().isNotNull("entity_code"));')],
     {N_DEAL_OTHER, N_COVERS_DEAL}),
    ("D10 两个分支写反（实体那一支只认应用级）", [(SVC, A_CHECK_AND, '            q.isNull("entity_code");')],
     {N_DEAL_OK}),
    ("D11 控制器只看返回值不看删了几行", [(CTL, A_REVOKE_ZERO, '        if (removed < 0) {')],
     {N_REVOKE_AGAIN, N_REVOKE_FAKE}),
    ("D12 回收丢掉 id 条件", [(SVC, A_REVOKE_DELETE,
                              '        return permissionMapper.delete(new QueryWrapper<PermissionEntity>()\n'
                              '                .eq("tenant_code", require(tenantCode, "tenantCode")));')],
     # 删过头时那一条 AUDITOR 行也被一起删掉了，于是"回收成功""清单里真的没有它"两支反倒读到
     # 它们期望的结果 —— 它们钉的是"删不掉时不许报成功"，不钉"不许多删"。"多删"这一侧由 /list
     # 那两支红出来: 表被清空之后，"按实体筛恰好一行"与"roleCode 优先"两句都不成立了。
     {N_LIST_ENTITY, N_LIST_PRIORITY}),
    ("D13 回收的租户条件写成 IS NULL", [(SVC, A_REVOKE_DELETE,
                                        '        return permissionMapper.delete(new QueryWrapper<PermissionEntity>()\n'
                                        '                .isNull("tenant_code")\n'
                                        '                .eq("id", id));')],
     {N_REVOKE_OK, N_REVOKE_GONE, N_CLEANUP}),
    ("D14 应用级那一档拿空串当范围", [(SVC, A_CHECK_ISNULL,
                                      '        if (scope == null) {\n'
                                      '            q.eq("entity_code", "");\n'
                                      '        } else {')],
     # 首轮有两支"留空问应用级"的探针一整轮没红过 —— 不是它们空跑，是十三支里没有一支把
     # "整个应用"写成另一种范围形状: D8 是"干脆不筛范围"(它们反而更成立), D7 让两侧一起漂。
     # 这一支才是它们的否证: 把 NULL 范围查成 '' —— 一行也匹配不上, "答允许"的两支当场翻。
     # 反面同时看清: N_ENTITY_ONLY 那句"不许冒充"在本支仍为绿, 因为它问的是同一档的 false。
     {N_CHECK_APPWIDE, N_TENANT_CHECK}),
]

EXPECTED_ANY = set()
for _, _, exp in RUNS:
    EXPECTED_ANY |= exp
ORPHANS = sorted(EXPECTED_ANY - TRACKED)


def sh(cmd, timeout=2400):
    return subprocess.run(cmd, cwd=str(ROOT), capture_output=True, text=True, timeout=timeout)


def class_bytes(entry):
    with zipfile.ZipFile(JAR) as fat:
        libs = [n for n in fat.namelist() if n.startswith("BOOT-INF/lib/z-lc-")]
        if not libs:
            raise RuntimeError("fat jar 里没有嵌套的 z-lc-*.jar: 零条目的指纹是常量, 等于没有")
        for lib in libs:
            with zipfile.ZipFile(io.BytesIO(fat.read(lib))) as inner:
                if entry in inner.namelist():
                    return inner.read(entry)
    raise RuntimeError("jar 里没有 " + entry + " 这个条目 (类改名? 那 marker 预检就是空的)")


def fingerprint():
    parts = []
    with zipfile.ZipFile(JAR) as fat:
        libs = [n for n in sorted(fat.namelist()) if n.startswith("BOOT-INF/lib/z-lc-")]
        if not libs:
            raise RuntimeError("no nested z-lc-*.jar: 指纹覆盖零条目就是自欺")
        for lib in libs:
            with zipfile.ZipFile(io.BytesIO(fat.read(lib))) as inner:
                classes = [e for e in sorted(inner.namelist()) if e.endswith(".class")]
                parts += [f"{lib}!{e}:{hashlib.md5(inner.read(e)).hexdigest()}" for e in classes]
    return "|".join(parts)


def preflight_markers():
    missing = []
    for _, wanted in MARKERS.items():
        for entry, marks in wanted.items():
            blob = class_bytes(entry)
            missing += [entry + " 少了 " + m for m in marks if m.encode("utf-8") not in blob]
    if missing:
        raise RuntimeError("部署产物里没有这次的修复(或 marker 改名了), 别再量: " + "; ".join(missing))


def build():
    # 两步, 与 mutate_edit_path_deployed_guard 同一理由: 只 install 改动的那两个模块,
    # 再 clean 重打 admin 的 fat jar —— 单跑 install 会把 repackage 当"没变化"跳过, 实测 stale 过。
    for cmd in (["mvn", "-o", "-B", "-q", "install", "-DskipTests", "-pl", "z-lc-core,z-lc-web"],
                ["mvn", "-o", "-B", "clean", "install", "-DskipTests", "-pl", "z-lc-admin"]):
        p = sh(cmd)
        if p.returncode != 0 and ("BUILD SUCCESS" not in (p.stdout + p.stderr)):
            raise RuntimeError("build failed: %s\n%s" % (cmd, (p.stdout + p.stderr)[-2500:]))
    preflight_markers()


def lc_pids():
    return [int(x) for x in subprocess.run(["pgrep", "-f", "z-lc-admin-1.0.0-SNAPSHOT.jar"],
                                           capture_output=True, text=True).stdout.split()]


def restart():
    pids = lc_pids()
    if len(pids) > 1:
        raise RuntimeError(f"refusing to guess which JVM to stop: {pids}")
    if pids:
        subprocess.run(["kill", "-9", str(pids[0])])
        time.sleep(2)
    LOGS.mkdir(parents=True, exist_ok=True)
    log = open(BOOT_LOG, "ab")
    subprocess.Popen(["java", "-jar", str(JAR), "--spring.profiles.active=dev"],
                     cwd=str(ROOT), stdout=log, stderr=log)
    deadline = time.time() + 420
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(HEALTH, timeout=3) as r:
                if b'"status":"UP"' in r.read():
                    return
        except Exception:  # noqa: BLE001
            pass
        time.sleep(2)
    raise RuntimeError("server did not come up; see %s" % BOOT_LOG)


def run_e2e(label):
    p = subprocess.run(["python3", "_e2e/e2e_api_test.py"], cwd=str(ROOT),
                       capture_output=True, text=True)
    LOGS.mkdir(parents=True, exist_ok=True)
    (LOGS / f"e2e_{label}.log").write_text(p.stdout + p.stderr)
    m = re.search(r"E2E RESULT: (\d+)/(\d+) passed", p.stdout)
    reds = {l[8:].split("   <<")[0] for l in p.stdout.splitlines() if l.startswith("  FAIL  ")}
    if not m:
        crash = [l for l in (p.stdout + p.stderr).splitlines()
                 if l.startswith(("Traceback", "  File", "IndexError", "KeyError", "TypeError"))]
        return (None, None), reds, "套件崩溃(没有汇总行): %s | 崩前红=%s" % (
            " / ".join(crash[-3:]) or (p.stdout.splitlines() or [""])[-1], sorted(reds))
    return (int(m.group(1)), int(m.group(2))), reds, None


def main():
    acquire_lock("mutate_permission_deployed_guard.py")
    try:
        return run_all()
    finally:
        release_lock()


def run_all():
    if ORPHANS:
        raise RuntimeError("预期红集里有名字不在 TRACKED 里(探针改名或写错): %s" % ORPHANS)
    orphans2 = sorted(set(WHY_NEVER_RED) - TRACKED)
    if orphans2:
        raise RuntimeError("WHY_NEVER_RED 里的名字不在 TRACKED 里: %s" % orphans2)
    stamp = time.strftime("%m%d-%H%M%S")
    run_dir = BAK / f"run-{os.getpid()}-{stamp}"
    run_dir.mkdir(parents=True, exist_ok=False)
    originals = {p: p.read_bytes() for p in (SVC, CTL)}
    for p, b in originals.items():
        (run_dir / (p.name + ".orig")).write_bytes(b)
        if (run_dir / (p.name + ".orig")).read_bytes() != b:
            raise RuntimeError("backup did not stick: " + p.name)
    problems = []
    ever_red = set()
    base = None
    try:
        build()
        base = fingerprint()
        restart()
        parsed, reds, crash = run_e2e("baseline")
        print(f"基线: {parsed[0]}/{parsed[1]} passed, 红={sorted(reds)}")
        if crash:
            print("RESULT: 基线就崩了: " + crash)
            return 2
        if reds:
            print("RESULT: 基线不干净: %s" % sorted(reds))
            return 2

        for tag, edits, expected in RUNS:
            for path, old, _ in edits:
                text = path.read_text(encoding="utf-8")
                if text.count(old) != 1:
                    raise RuntimeError(f"{tag}: {path.name} 的锚点出现 {text.count(old)} 次(要 1 次), 不敢改")
            for path, old, new in edits:
                path.write_text(path.read_text(encoding="utf-8").replace(old, new), encoding="utf-8")
            try:
                build()
                restart()
                parsed2, reds2, crash2 = run_e2e(tag.split(" ")[0])
            finally:
                for path, b in originals.items():
                    path.write_bytes(b)
            mine = reds2 & TRACKED
            ever_red |= mine
            print(f"{tag}: {parsed2[0]}/{parsed2[1]} passed"
                  f"\n    预期红: {sorted(expected)}"
                  f"\n    实测红(受跟踪的): {sorted(mine)}"
                  f"\n    其它红: {sorted(reds2 - TRACKED)}")
            if crash2:
                problems.append(f"{tag}: {crash2}")
            missing = sorted(expected - mine)
            extra = sorted(mine - expected)
            if missing:
                problems.append(f"{tag}: 注入没被捉住 {missing}")
            if extra:
                problems.append(f"{tag}: 红了预期之外 {extra}")
            if parsed2[1] and parsed2[1] != parsed[1]:
                problems.append(f"{tag}: 探针分母从 {parsed[1]} 变成 {parsed2[1]}")
            fp2 = fingerprint()
            if fp2 == base:
                problems.append(f"{tag}: 指纹没变 —— 这一支根本没被编进产物, 实测的红不算它的成绩")
            if sorted(reds2 - TRACKED):
                # 未跟踪的红不当噪声丢掉: 它是 [11] 那两支冒烟探针或别的战役在这一支下的连带,
                # 每一支都要能说出因果 (跑完逐条回看 e2e_<tag>.log)。
                print("    (未跟踪的红: %s)" % sorted(reds2 - TRACKED))

        never = sorted(TRACKED - ever_red - set(WHY_NEVER_RED))
        print("一整轮没红过、也没写明由谁负责红的受跟踪探针: %s" % never)
        for name in never:
            problems.append(f"探针空跑: {name} 十四支注入没有一支能让它红, 也没写 WHY_NEVER_RED")

        for path, b in originals.items():
            if path.read_bytes() != b:
                path.write_bytes(b)
                problems.append(f"还原时发现 {path.name} 字节不同, 已按备份强行还原")
        build()
        if fingerprint() != base:
            problems.append("还原后指纹与基线不一致")
        restart()
        parsed3, reds3, crash3 = run_e2e("restored")
        print(f"还原后复测: {parsed3[0]}/{parsed3[1]} passed, 红={sorted(reds3)}")
        if reds3 or crash3:
            problems.append(f"还原后仍有红/崩溃: {sorted(reds3)} {crash3 or ''}")
    finally:
        for path, b in originals.items():
            path.write_bytes(b)

    if problems:
        print("RESULT: %d problem(s)" % len(problems))
        for p in problems:
            print("  - " + p)
        return 1
    print("RESULT: permission deployed-layer falsification done | "
          f"{len(RUNS)} 支注入 / 认领 {len(TRACKED)} 条 [15t] 探针")
    return 0


if __name__ == "__main__":
    sys.exit(main())
