#!/usr/bin/env python3
"""z-lc end-to-end API test harness.

Drives a running z-lc instance through its real HTTP API and asserts on the
JSON contract the frontend depends on. Exits non-zero on any failure.

Usage: python3 e2e_api_test.py [base_url]
"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:18090").rstrip("/")
TENANT = "default"
APP = "e2e_crm_" + uuid.uuid4().hex[:6]

PASS, FAIL = [], []


def D(j):
    return (j or {}).get("data") or {}



def call(method, path, body=None, params=None, raw=False, expect_http=200):
    url = BASE + path
    if params:
        url += "?" + "&".join(f"{k}={urllib.parse.quote(str(v))}" for k, v in params.items())
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Tenant-Code", TENANT)
    actor = globals().get("_ACTOR")
    if actor:
        req.add_header("X-User-Code", actor)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            text, status = r.read().decode(), r.status
    except urllib.error.HTTPError as e:
        text, status = e.read().decode(), e.code
    except Exception as e:  # noqa: BLE001
        return None, str(e), 0
    if raw:
        return text, status, status
    try:
        return json.loads(text), status, status
    except json.JSONDecodeError:
        return None, f"non-JSON: {text[:200]}", status


def call_as(actor, method, path, body=None, params=None):
    """Same as call() but pins X-User-Code, for per-user undo-stack assertions."""
    global _ACTOR
    previous = globals().get("_ACTOR")
    globals()["_ACTOR"] = actor
    try:
        return call(method, path, body=body, params=params)[0]
    finally:
        globals()["_ACTOR"] = previous


def check(name, cond, detail=""):
    (PASS if cond else FAIL).append(name)
    print(("  PASS  " if cond else "  FAIL  ") + name + ("" if cond else f"   << {detail}"))
    return cond


def ok(name, resp, resp_name="Result"):
    """Assert the Result envelope says success:true."""
    if not isinstance(resp, dict):
        return check(name, False, f"no JSON body: {str(resp)[:160]}")
    return check(name, resp.get("success") is True, f"{resp_name}{{code:{resp.get('code')},message:{resp.get('message')}}}")


SUF = str(int(time.time()))[-6:]

print(f"\n=== z-lc E2E against {BASE} (app={APP}) ===\n")

# ---------------------------------------------------------------- health
print("[1] health + reachability")
j, s, _ = call("GET", "/api/lc/health")
ok("health returns Result envelope", j)
check("health data.status == UP", isinstance(j, dict) and D(j).get("status") == "UP", j)

# ---------------------------------------------------------------- app lifecycle
print("\n[2] app lifecycle: create -> detail -> list -> update -> publish")
j, _, _ = call("POST", "/api/lc/app/create", {
    "tenantCode": TENANT, "appCode": APP, "appName": "E2E CRM",
    "description": "created by e2e harness", "icon": "appstore"})
ok("POST /app/create", j)
app = (j or {}).get("data") or {}
check("created app echoes appCode", app.get("appCode") == APP, app)

j, _, _ = call("GET", "/api/lc/app/detail", params={"appCode": APP})
ok("GET /app/detail", j)
check("detail returns the app", D(j).get("appName") == "E2E CRM", j)

j, _, _ = call("GET", "/api/lc/app/list")
ok("GET /app/list", j)
check("app/list is a bare array (not paged)", isinstance((j or {}).get("data"), list), type((j or {}).get("data")))

j, _, _ = call("POST", "/api/lc/app/update", {"id": app.get("id"), "appName": "E2E CRM v2"})
ok("POST /app/update", j)

j, _, _ = call("POST", "/api/lc/app/publish", {"appCode": APP})
ok("POST /app/publish", j)

j, _, _ = call("POST", "/api/lc/admin/app/list", params={"page": 1, "size": 20})
ok("POST /admin/app/list returns paged", j)
page = (j or {}).get("data") or {}
check("PageResult shape records/total/pageNum/pageSize",
      all(k in page for k in ("records", "total", "pageNum", "pageSize")), list(page.keys()) if isinstance(page, dict) else page)

# ---------------------------------------------------------------- schema / entity
print("\n[3] entity + field schema authoring")
FIELDS = [
    {"fieldCode": "customer_name", "fieldName": "客户名称", "fieldType": "STRING",
     "required": True, "fieldLength": 128, "sortOrder": 1, "description": "客户名"},
    {"fieldCode": "phone", "fieldName": "手机号", "fieldType": "STRING",
     "required": False, "fieldLength": 32, "sortOrder": 2},
    {"fieldCode": "age", "fieldName": "年龄", "fieldType": "INT", "sortOrder": 3},
    {"fieldCode": "balance", "fieldName": "余额", "fieldType": "DECIMAL",
     "fieldLength": 18, "scale": 2, "sortOrder": 4},
    {"fieldCode": "is_vip", "fieldName": "VIP", "fieldType": "BOOLEAN", "sortOrder": 5},
    {"fieldCode": "birthday", "fieldName": "生日", "fieldType": "DATE", "sortOrder": 6},
    {"fieldCode": "last_visit", "fieldName": "最近联系", "fieldType": "DATETIME", "sortOrder": 7},
    {"fieldCode": "level", "fieldName": "客户等级", "fieldType": "STRING",
     "dictCode": "e2e_dict_" + SUF, "fieldLength": 32, "sortOrder": 8},
    {"fieldCode": "remark", "fieldName": "备注", "fieldType": "TEXT", "sortOrder": 9},
]
j, _, _ = call("POST", "/api/lc/admin/app/entity/create",
               {"tenantCode": TENANT, "appCode": APP, "entityCode": "customer",
                "entityName": "客户", "tableName": "e2e_customer_" + SUF, "description": "e2e",
                "fields": FIELDS},
               params={"appCode": APP, "tenantCode": TENANT})
ok("POST /admin/app/entity/create", j)
ent = (j or {}).get("data") or {}
ent_id = ent.get("id")
check("entity returned with id", bool(ent_id), ent)
check("entity carries 9 persisted fields", len(ent.get("fields") or []) == 9, len(ent.get("fields") or []))

j, _, _ = call("GET", "/api/lc/admin/entity", params={"id": ent_id})
ok("GET /admin/entity?id=", j)

j, _, _ = call("GET", "/api/lc/app/schema", params={"appCode": APP, "tenantCode": TENANT})
ok("GET /app/schema (event-replayed schema)", j)
schema = D(j) if isinstance(D(j), list) else []
cust = next((e for e in schema if e.get("entityCode") == "customer"), None)
if check("schema contains the customer entity", cust is not None, [e.get("entityCode") for e in schema]):
    check("schema folds all 9 fields through the event log", len(cust.get("fields") or []) == 9,
          len(cust.get("fields") or []))
    byc = {f["fieldCode"]: f for f in cust.get("fields") or []}
    check("field metadata survives the round-trip", byc.get("balance", {}).get("scale") == 2, byc.get("balance"))
    check("dictCode binding survives", byc.get("level", {}).get("dictCode") == "e2e_dict_" + SUF, byc.get("level"))
    check("required flag survives", byc.get("customer_name", {}).get("required") is True, byc.get("customer_name"))

# ---------------------------------------------------------------- provision
print("\n[4] DDL provisioning (metadata -> physical table)")
j, _, _ = call("POST", "/api/lc/admin/entity/provision", params={"id": ent_id})
if ok("POST /admin/entity/provision", j):
    check("provision returns executable DDL", "CREATE TABLE" in str((j or {}).get("data")), str((j or {}).get("data"))[:200])

# ---------------------------------------------------------------- dict
print("\n[5] dictionary + items")
j, _, _ = call("POST", "/api/lc/dict/create",
               {"tenantCode": TENANT, "dictCode": "e2e_dict_" + SUF, "dictName": "客户等级",
                "description": "e2e", "status": "ENABLED"})
ok("POST /dict/create", j)
for i, (code, label) in enumerate([("A", "普通"), ("B", "银卡"), ("C", "金卡"), ("D", "钻石")]):
    call("POST", "/api/lc/dict/items/create",
         {"tenantCode": TENANT, "dictCode": "e2e_dict_" + SUF, "itemCode": code,
          "itemLabel": label, "itemValue": code, "sortOrder": i + 1},
         params={"dictCode": "e2e_dict_" + SUF})
j, _, _ = call("GET", "/api/lc/dict/items", params={"dictCode": "e2e_dict_" + SUF})
ok("GET /dict/items", j)
items = D(j) if isinstance(D(j), list) else []
check("4 dict items persisted", len(items) == 4, len(items))

# ---------------------------------------------------------------- runtime CRUD
print("\n[6] runtime CRUD golden path")
j, _, _ = call("POST", "/api/lc/runtime/create",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                "fieldValues": {"customer_name": "张三", "phone": "13800000001", "age": 30,
                                "balance": 1234.56, "is_vip": True, "birthday": "1994-02-03",
                                "last_visit": "2026-09-20 10:30:00",
                                "level": "A", "remark": "首个客户"}},
               params={"entityCode": "customer"})
if ok("POST /runtime/create", j):
    rid = (j or {}).get("data")
    check("create returns a numeric id", isinstance(rid, int) and rid > 0, rid)
else:
    rid = None

ids = [rid] if rid else []
for n, (nm, age, lvl, bal) in enumerate([("李四", 41, "B", 88.5), ("王五", 25, "C", 9999.99), ("赵六", 52, "A", 0.0)]):
    j, _, _ = call("POST", "/api/lc/runtime/create",
                   {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                    "fieldValues": {"customer_name": nm, "age": age, "balance": bal,
                                    "level": lvl, "is_vip": n % 2 == 0}},
                   params={"entityCode": "customer"})
    if (j or {}).get("success"):
        ids.append(j.get("data"))

j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 20, "orderBy": "id desc"},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
ok("POST /runtime/list", j)
lr = (j or {}).get("data") or {}
rows = lr.get("records") or []
check("list returns all inserted rows", lr.get("total") == len(ids), (lr.get("total"), len(ids)))
if check("rows are snake_case column maps", rows and "customer_name" in rows[0], list(rows[0].keys()) if rows else None):
    check("dict field is enriched with *_label for grid rendering",
          rows[0].get("level_label") in ("普通", "银卡", "金卡", "钻石"),
          f"value={rows[0].get('level')} label={rows[0].get('level_label')}")

j, _, _ = call("POST", "/api/lc/runtime/get",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT, "id": rid},
               params={"entityCode": "customer"})
ok("POST /runtime/get", j)
check("get returns the row", D(j).get("customer_name") == "张三", j)

j, _, _ = call("POST", "/api/lc/runtime/update",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                "fieldValues": {"id": rid, "customer_name": "张三丰", "age": 31}},
               params={"entityCode": "customer"})
ok("POST /runtime/update", j)
j, _, _ = call("POST", "/api/lc/runtime/get",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT, "id": rid},
               params={"entityCode": "customer"})
check("update actually persisted", D(j).get("customer_name") == "张三丰", j)

# ---------------------------------------------------------------- query operators
print("\n[7] filter / operator / pagination semantics")
j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 10, "filters": {"age:gt": 35}},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
d = (j or {}).get("data") or {}
check("filters 'age:gt' works", d.get("total") == 2, d.get("total"))

j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 10, "filters": {"customer_name:like": "张"}},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
check("filters ':like' works", D(j).get("total") == 1, (j or {}).get("data"))

j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 10, "filters": {"level:in": ["A", "C"]}},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
check("filters ':in' works", D(j).get("total") == 3, D(j).get("total"))

j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 2, "orderBy": "age asc"},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
d = (j or {}).get("data") or {}
check("size clamps page length", len(d.get("records") or []) == 2, len(d.get("records") or []))
check("orderBy asc sorts", [r.get("age") for r in (d.get("records") or [])] == [25, 31],
      [r.get("age") for r in (d.get("records") or [])])

# ---------------------------------------------------------------- security
print("\n[8] SQL-injection surface (ORDER BY / filter key)")
for label, payload in [
    ("ORDER BY stacked query", {"page": 1, "size": 5, "orderBy": "id; DROP TABLE z_lc_app"}),
    ("ORDER BY subselect", {"page": 1, "size": 5, "orderBy": "(SELECT 1) UNION SELECT table_name FROM information_schema.tables"}),
    ("filter key injection", {"page": 1, "size": 5, "filters": {"1=1": "x"}}),
]:
    j, s, _ = call("POST", "/api/lc/runtime/list", payload,
                   params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
    poisoned = isinstance(j, dict) and j.get("success") is True and \
        len(D(j).get("records") or []) > 0 and "information_schema" in json.dumps(j)[:2000]
    check(f"{label} is not executed", not poisoned, str(j)[:200])
j, _, _ = call("GET", "/api/lc/app/list")
ok("z_lc_app survived the injection attempts", j)

# ---------------------------------------------------------------- validation
print("\n[9] field validation")
j, _, _ = call("POST", "/api/lc/runtime/create",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                "fieldValues": {"phone": "138000"}}, params={"entityCode": "customer"})
check("missing required field is rejected", not (isinstance(j, dict) and j.get("success") is True), j)

# ---------------------------------------------------------------- view config
print("\n[10] view-config persistence (saved grid state)")
cfg = json.dumps({"columnMeta": {"customer_name": {"order": 1, "width": 180},
                                 "age": {"order": 2, "hidden": False}},
                  "rowHeight": "compact"})
j, _, _ = call("POST", "/api/lc/view-config/create",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                "viewType": "LIST", "config": cfg})
ok("POST /view-config/create", j)
j, _, _ = call("GET", "/api/lc/view-config/list", params={"appCode": APP, "entityCode": "customer"})
ok("GET /view-config/list", j)
vc = D(j) if isinstance(D(j), list) else []
if check("view config persisted", len(vc) >= 1, vc):
    check("config round-trips as a JSON string", isinstance(vc[0].get("config"), str), type(vc[0].get("config")))

# ---------------------------------------------------------------- relations
print("\n[11] relations, pipelines, permissions, deployment")
j, _, _ = call("POST", "/api/lc/relation/create",
               {"relationCode": "cust_orders", "relationName": "客户订单",
                "sourceEntityCode": "customer", "targetEntityCode": "order",
                "relationType": "ONE_TO_MANY", "sourceFieldCode": "id",
                "tenantCode": TENANT, "appCode": APP})
ok("POST /relation/create", j)
j, _, _ = call("GET", "/api/lc/relation/list", params={"appCode": APP})
ok("GET /relation/list", j)

# 这一格原本是 AFTER_CREATE + 只配 DICT_RESOLVE —— 引擎里既没有写后挂接点也没有那道闸,
# 而当时只要求"别 500"就算过。#41 之后这种配置在写入口就会被拒, 所以这里换成引擎兑现得了的一份,
# 并把 enabled 必须是 0/1 这件事钉住 (完整的开关/顺序断言在 [15p])。
j, _, _ = call("POST", "/api/lc/pipeline-config/create",
               {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
                "triggerEvent": "BEFORE_CREATE",
                "stages": json.dumps([{"type": "REQUIRED_CHECK", "config": {}, "order": 0},
                                      {"type": "TYPE_CONVERT", "config": {}, "order": 1},
                                      {"type": "VALUE_VALIDATE", "config": {}, "order": 2}]),
                "enabled": 1})
ok("POST /pipeline-config/create (引擎兑现得了的配置)", j)
pipe_cfg_id = D(j).get("id")
j, s, _ = call("POST", "/api/lc/pipeline-config/toggle",
               {"entityCode": "customer", "appCode": APP, "enabled": 0})
check("toggle 少了 id 不能报成切换成功", isinstance(j, dict) and j.get("success") is False and s == 400,
      f"http={s} body={str(j)[:160]}")
j, _, _ = call("POST", "/api/lc/pipeline-config/toggle",
               {"id": pipe_cfg_id, "appCode": APP, "tenantCode": TENANT, "enabled": 0})
check("带 id 的停用按整数 0/1 接受", isinstance(j, dict) and j.get("success") is True, str(j)[:160])
if pipe_cfg_id:
    call("POST", "/api/lc/pipeline-config/delete", {"id": pipe_cfg_id})

j, _, _ = call("POST", "/api/lc/permission/grant",
               {"appCode": APP, "entityCode": "customer", "roleCode": "editor",
                "permission": "READ", "tenantCode": TENANT})
ok("POST /permission/grant", j)
j, _, _ = call("GET", "/api/lc/permission/check",
               params={"appCode": APP, "entityCode": "customer", "roleCode": "editor", "permission": "READ"})
ok("GET /permission/check", j)

j, _, _ = call("POST", "/api/lc/deployment/create",
               {"appCode": APP, "tenantCode": TENANT, "deployType": "HOT_LOAD", "status": "PENDING"})
ok("POST /deployment/create", j)
j, _, _ = call("GET", "/api/lc/deployment/list", params={"appCode": APP})
ok("GET /deployment/list", j)

head = call("GET", "/api/lc/app/event/last", params={"appCode": APP, "tenantCode": TENANT})[0]
parent = D(head).get("eventId")
if check("GET /app/event/last gives the chain head", bool(parent), head):
    # 用正确的 parentEventId 续链 -> 应该成功
    j, _, _ = call("POST", "/api/lc/app/event",
                   {"tenantCode": TENANT, "entityCode": "customer", "eventType": "UPDATE",
                    "eventData": json.dumps({"entityName": "客户", "tableName": "e2e_customer_" + SUF,
                                            "description": "e2e updated"}),
                    "source": "E2E", "parentEventId": parent},
                   params={"appCode": APP})
    ok("POST /app/event append (chained)", j)
    j, _, _ = call("GET", "/api/lc/app/schema", params={"appCode": APP, "tenantCode": TENANT})
    sc2 = next((e for e in (D(j) if isinstance(D(j), list) else []) if e.get("entityCode") == "customer"), {})
    check("event replay applies last-write-wins UPDATE",
          sc2.get("description") == "e2e updated", sc2.get("description"))

    # 因果冲突必须被诚实上报: 不能外层 success:true 把失败包装成成功
    stale = call("POST", "/api/lc/app/event",
                 {"tenantCode": TENANT, "entityCode": "customer", "eventType": "UPDATE",
                  "eventData": json.dumps({"description": "should-not-land"}),
                  "source": "E2E", "parentEventId": parent},
                 params={"appCode": APP})[0]
    check("stale parentEventId is reported as a failure, not a wrapped success",
          isinstance(stale, dict) and stale.get("success") is False and stale.get("code") == 409,
          str(stale)[:200])

# ---------------------------------------------------------------- error contract
print("\n[12] error contract consistency")
j, s, _ = call("POST", "/api/lc/runtime/list", {"page": 1, "size": 5}, params={"entityCode": "nope_xyz"})
check("unknown entity does not leak a stack trace as HTML", s == 200 or isinstance(j, dict), f"http={s}")
check("unknown entity reports failure", not (isinstance(j, dict) and j.get("success") is True), str(j)[:160])
j, s, _ = call("GET", "/api/lc/app/detail", params={"appCode": "does_not_exist"})
check("missing app -> success:false or null data",
      not (isinstance(j, dict) and j.get("success") is True and j.get("data")), str(j)[:160])

# ---------------------------------------------------------------- new meta API
print("\n[13] meta bundle + field-type registry (new low-code platform APIs)")
j, s, _ = call("GET", "/api/lc/meta/field-types")
if ok("GET /meta/field-types", j):
    fts = D(j) if isinstance(D(j), list) else []
    names = {f.get("fieldType") for f in fts}
    need = {"STRING", "INT", "LONG", "DECIMAL", "BOOLEAN", "DATE", "DATETIME", "TEXT", "JSON", "REF"}
    check("covers all 10 runtime field types", need <= names, sorted(need - names))
    probe = next((f for f in fts if f.get("fieldType") == "DECIMAL"), {})
    check("descriptor exposes the 3-axis model + capabilities",
          all(k in probe for k in ("cellValueType", "dbType", "widget", "operators",
                                   "sortable", "groupable", "filterable", "inlineEditable")),
          list(probe.keys()))
    check("DECIMAL maps to a Number cellValueType", probe.get("cellValueType") == "Number", probe)
else:
    print("  .. /meta/field-types not deployed yet")

j, s, _ = call("GET", "/api/lc/meta/bundle", params={"appCode": APP, "tenantCode": TENANT})
if ok("GET /meta/bundle", j):
    b = (j or {}).get("data") or {}
    check("bundle carries app", (b.get("app") or {}).get("appCode") == APP, b.get("app"))
    check("bundle carries entities", len(b.get("entities") or []) >= 1, len(b.get("entities") or []))
    check("bundle carries dicts", len(b.get("dicts") or []) >= 1, len(b.get("dicts") or []))
    check("bundle carries views", len(b.get("views") or []) >= 1, len(b.get("views") or []))
    check("bundle carries relations", len(b.get("relations") or []) >= 1, len(b.get("relations") or []))
    check("bundle carries fieldTypes", len(b.get("fieldTypes") or []) >= 10, len(b.get("fieldTypes") or []))
else:
    print("  .. /meta/bundle not deployed yet")

j, _, _ = call("GET", "/api/lc/meta/bundle")
check("bundle without appCode degrades gracefully", isinstance(j, dict) and j.get("success") is True, str(j)[:160])

print("\n[14] structured query API (injection-safe multi-filter/sort)")
j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 10, "conjunction": "AND",
                "conditions": [{"fieldCode": "age", "operator": "gte", "value": 30},
                               {"fieldCode": "level", "operator": "in", "value": ["A", "B"]}],
                "sorts": [{"fieldCode": "age", "dir": "desc"}, {"fieldCode": "id", "dir": "asc"}]},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
d = (j or {}).get("data") or {}
if check("structured conditions+sorts accepted", isinstance(j, dict) and j.get("success") is True, str(j)[:200]):
    check("structured filter narrows result set", d.get("total") in (2, 3), d.get("total"))
j, _, _ = call("POST", "/api/lc/runtime/list",
               {"page": 1, "size": 10,
                "conditions": [{"fieldCode": "1=1", "operator": "eq", "value": "x"}]},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
check("unknown/invalid fieldCode in conditions rejected", not (isinstance(j, dict) and j.get("success") is True), str(j)[:200])

# ---------------------------------------------------------------- delete
print("\n[15] delete + soft-delete semantics")
if rid:
    j, _, _ = call("POST", "/api/lc/runtime/delete",
                   {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT, "id": rid},
                   params={"entityCode": "customer"})
    ok("POST /runtime/delete", j)
    j, _, _ = call("POST", "/api/lc/runtime/get",
                   {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT, "id": rid},
                   params={"entityCode": "customer"})
    check("deleted row no longer readable", not (j or {}).get("data"), j)
    j, _, _ = call("POST", "/api/lc/runtime/list", {"page": 1, "size": 50},
                   params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})
    check("soft delete removes from list", D(j).get("total") == len(ids) - 1,
           (j or {}).get("data"))

# ---------------------------------------------------------------- undo/redo
print("\n[15b] 服务端 undo / redo / 变更历史 / 布尔写入保真")
UNDO_APP_HDR = {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT}

# 布尔保真: 历史实现 Boolean.valueOf("1")==false, 会把合法的 1 静默存成 0
j, _, _ = call("POST", "/api/lc/runtime/create", dict(UNDO_APP_HDR, fieldValues={
    "customer_name": "布尔保真", "is_vip": 1, "age": 40}), params={"entityCode": "customer"})
uid = D(j) if isinstance(j, dict) and isinstance(D(j), int) else None
if check("create with is_vip=1 (整数布尔)", isinstance(j, dict) and j.get("success") is True, str(j)[:160]):
    j, _, _ = call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid), params={"entityCode": "customer"})
    stored = D(j).get("is_vip")
    check("整数 1 落库为真值, 没有被 coerce 成 0", str(stored) in ("1", "True", "true"),
          f"is_vip={stored!r}")

before = D(call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid),
                params={"entityCode": "customer"})[0]).get("customer_name")
upd = call("POST", "/api/lc/runtime/update",
           dict(UNDO_APP_HDR, fieldValues={"id": uid, "customer_name": "改坏的名字"}),
           params={"entityCode": "customer"})[0]
ok("update before undo", upd)
mid = D(call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid),
             params={"entityCode": "customer"})[0]).get("customer_name")
check("改动已生效", mid == "改坏的名字", mid)

u = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
         params={"entityCode": "customer"})[0]
ok("POST /undo/undo", u)
check("undo 报的是被撤销的那次 UPDATE", isinstance(u, dict) and D(u).get("operation") == "UPDATE", u)
back = D(call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid),
              params={"entityCode": "customer"})[0]).get("customer_name")
check("undo 精确回到前像", back == before, f"expected {before!r}, got {back!r}")

r = call("POST", "/api/lc/undo/redo", {"appCode": APP, "tenantCode": TENANT},
         params={"entityCode": "customer"})[0]
ok("POST /undo/redo", r)
again = D(call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid),
               params={"entityCode": "customer"})[0]).get("customer_name")
check("redo 重新应用了后像", again == "改坏的名字", again)

# 再 undo 一次应当退到 CREATE 那条 —— 撤销"新建"的合法结果就是这条记录消失
call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT}, params={"entityCode": "customer"})
third = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
             params={"entityCode": "customer"})[0]
ok("undo down to the CREATE entry", third)
gone = call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=uid),
            params={"entityCode": "customer"})[0]
check("撤销 CREATE 后记录不再可见 (undo CREATE = 软删)", not D(gone).get("id"), gone)

hist = call("GET", "/api/lc/undo/history",
            params={"appCode": APP, "entityCode": "customer", "tenantCode": TENANT})[0]
ok("GET /undo/history", hist)
entries = D(hist) if isinstance(D(hist), list) else []
check("history 有内容", len(entries) >= 1, len(entries))
dirty = [e for e in entries if "_label" in str(e.get("beforeImage")) + str(e.get("afterImage"))]
check("快照不含 *_label 派生列", not dirty, dirty[:1])

# 撤销删除: 用一条新记录, 且实体带 DATE / DATETIME 字段 —— 快照日期格式必须能重放
j, _, _ = call("POST", "/api/lc/runtime/create", dict(UNDO_APP_HDR, fieldValues={
    "customer_name": "待撤销删除", "birthday": "1990-05-06",
    "last_visit": "2026-01-02 03:04:05", "age": 33}), params={"entityCode": "customer"})
did = D(j) if isinstance(D(j), int) else None
check("create a dated record for delete-undo", did is not None, j)
snapshot = D(call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=did),
                  params={"entityCode": "customer"})[0])
call("POST", "/api/lc/runtime/delete", dict(UNDO_APP_HDR, id=did), params={"entityCode": "customer"})
ud = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
          params={"entityCode": "customer"})[0]
ok("undo a DELETE on a row with DATE/DATETIME columns", ud)
check("undo 报的是 DELETE", isinstance(ud, dict) and D(ud).get("operation") == "DELETE", ud)
rev = call("POST", "/api/lc/runtime/get", dict(UNDO_APP_HDR, id=did),
           params={"entityCode": "customer"})[0]
check("被删记录已复活", D(rev).get("id") == did, rev)
# 断言"删除前 == 复活后"的快照一致性, 而不是和我手敲的字面量比:
# API 把 DATETIME 统一序列化成 UTC ISO 串, 比字面量只会测到 JSON 表示而不是保真度.
for col in ("birthday", "last_visit"):
    check(f"复活后 {col} 与删除前完全一致", str(snapshot.get(col)) == str(D(rev).get(col)),
          f"before={snapshot.get(col)!r} after={D(rev).get(col)!r}")

# ---------------------------------------------------------------- per-user undo
print("\n[15c] undo 栈按人隔离（多人同时编辑不能互撤）")
# 自带一条新记录: 上一节把 uid 的 CREATE 都撤销掉了, 复用它只会得到"记录不存在"
resp = call("POST", "/api/lc/runtime/create",
            {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT,
             "fieldValues": {"customer_name": "并发基线", "age": 20}},
            params={"entityCode": "customer"})[0]
cid = D(resp)
check("per-user 用例自备记录", isinstance(cid, int) and cid > 0, resp)
SCOPE = {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT}


def name_of(actor_id):
    body, _s, _h = call("POST", "/api/lc/runtime/get", dict(SCOPE, id=actor_id),
                        params={"entityCode": "customer"})
    return D(body).get("customer_name")


call_as("alice", "POST", "/api/lc/runtime/update",
        dict(SCOPE, fieldValues={"id": cid, "customer_name": "Alice 写的"}),
        params={"entityCode": "customer"})
check("alice 的改动已落库", name_of(cid) == "Alice 写的", name_of(cid))

_bob, _s2, _h2 = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
                      params={"entityCode": "customer"})
bob = _bob if isinstance(_bob, dict) else {}
globals()["_ACTOR"] = "bob"
bob = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
           params={"entityCode": "customer"})[0]
check("bob 的 undo 请求本身不报错", isinstance(bob, dict) and bob.get("code") != 500, str(bob)[:150])
check("bob 撤不动 alice 的改动", D(bob).get("applied") is False, bob)
check("alice 的数据没被 bob 回滚", name_of(cid) == "Alice 写的", name_of(cid))

globals()["_ACTOR"] = "alice"
alice = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
             params={"entityCode": "customer"})[0]
check("alice 撤得动自己的", D(alice).get("applied") is True, alice)
check("撤完回到基线", name_of(cid) == "并发基线", name_of(cid))

globals()["_ACTOR"] = "carol"
hist = call("GET", "/api/lc/undo/history",
            params={"appCode": APP, "entityCode": "customer", "tenantCode": TENANT,
                    "owner": "mine"})[0]
owners = {e.get("actor") for e in (D(hist) if isinstance(D(hist), list) else [])}
check("owner=mine 的历史只含本人", owners <= {"carol"}, owners)

globals()["_ACTOR"] = None
hist_all = call("GET", "/api/lc/undo/history",
                params={"appCode": APP, "entityCode": "customer", "tenantCode": TENANT,
                        "owner": "all"})[0]
all_owners = {e.get("actor") for e in (D(hist_all) if isinstance(D(hist_all), list) else [])}
check("owner=all 能看到 alice 的改动", "alice" in all_owners, all_owners)
# 被拒绝的撤销不该留下日志: bob 从没写过数据, 他不该出现在历史里
check("被拒绝的 undo 不写变更日志", "bob" not in all_owners, all_owners)

call("POST", "/api/lc/runtime/delete", dict(SCOPE, id=cid), params={"entityCode": "customer"})

# ---------------------------------------------------------------- aggregation
print("\n[15d] 分组聚合 / 数值聚合 / 列白名单守卫")
AGG_SCOPE = {"appCode": APP, "tenantCode": TENANT}
agg = call("POST", "/api/lc/runtime/aggregate",
           dict(AGG_SCOPE, groupField="level", aggregations={"balance": ["SUM", "MAX", "AVG"]}),
           params={"entityCode": "customer"})[0]
ok("POST /runtime/aggregate group by dict field", agg)
buckets = D(agg) if isinstance(D(agg), list) else []
check("聚合返回分组行", len(buckets) >= 2, buckets)
if buckets:
    row = buckets[0]
    check("分组行含 group_key/group_count", "group_key" in row and "group_count" in row, row)
    check("字典分组带可读标签", row.get("group_label") in ("普通", "银卡", "金卡", "钻石", None), row)
    check("数值聚合列回传", "sum_balance" in row and "max_balance" in row, row)
    check("分组按 count 倒序", [b.get("group_count") for b in buckets] == sorted(
        [b.get("group_count") for b in buckets], reverse=True), buckets)

total = call("POST", "/api/lc/runtime/aggregate", dict(AGG_SCOPE),
             params={"entityCode": "customer"})[0]
td = D(total) if isinstance(D(total), list) else []
check("不带 groupField 返回一行总计", len(td) == 1 and td[0].get("group_count", 0) >= 1, total)

filtered = call("POST", "/api/lc/runtime/aggregate",
                dict(AGG_SCOPE, groupField="level", filters={"age:gte": 30}),
                params={"entityCode": "customer"})[0]
frows = D(filtered) if isinstance(D(filtered), list) else []
check("聚合尊重筛选条件", all(r.get("group_count", 0) >= 1 for r in frows) and len(frows) >= 1, filtered)

for label, payload in [
    ("注入型 groupField", {"groupField": "level; DROP TABLE z_lc_app"}),
    ("未知 groupField", {"groupField": "nope"}),
    ("文本列做 SUM", {"aggregations": {"customer_name": ["SUM"]}}),
    ("非白名单函数", {"aggregations": {"balance": ["STDDEV"]}}),
]:
    bad = call("POST", "/api/lc/runtime/aggregate", dict(AGG_SCOPE, **payload),
               params={"entityCode": "customer"})[0]
    check(f"聚合拒绝 {label}", isinstance(bad, dict) and bad.get("success") is False
          and bad.get("code") == 400, str(bad)[:150])
ok("聚合守卫后 z_lc_app 仍在", call("GET", "/api/lc/app/list")[0])

# ------------------------------------------- multi-dim grouping + pivot shape
print("\n[15d1] 多维分组与交叉表整形 (groupFields + /runtime/shape 的 pivot 步)")
# 行=level(字典), 列=is_vip(布尔)。第二维刻意选布尔而不是客户名: 列的基数必须是常数,
# 否则这张表的宽度会随着导入的数据长, 下面那几条断言的口径也就跟着漂。
PIVOT_PROGRAM = [{"op": "pivot", "by": ["group_key", "group_label"],
                  "on": "group_label_2", "agg": "SUM", "value": "group_count"}]

single = call("POST", "/api/lc/runtime/aggregate", dict(AGG_SCOPE, groupField="level"),
              params={"entityCode": "customer"})[0]
by_key = {r.get("group_key"): r.get("group_count") for r in (D(single) or []) if isinstance(r, dict)}
check("交叉表用例的基准: 单维分组有至少两档", len(by_key) >= 2, single)

piv = call("POST", "/api/lc/runtime/shape",
           dict(AGG_SCOPE, groupFields=["level", "is_vip"], aggregations={}, shape=PIVOT_PROGRAM),
           params={"entityCode": "customer"})[0]
raw2 = D(call("POST", "/api/lc/runtime/aggregate", dict(AGG_SCOPE, groupFields=["level", "is_vip"]),
              params={"entityCode": "customer"})[0])
raw2 = raw2 if isinstance(raw2, list) else []
ok("POST /runtime/shape 两维 + pivot", piv)
grid = D(piv) if isinstance(D(piv), list) else []
check("整形产出是行数组", len(grid) >= 2, str(piv)[:220])
if grid:
    DIMS = {"group_key", "group_label"}
    cols = sorted({k for row in grid for k in row if k not in DIMS})
    check("每一行都带行键与行标签",
          all("group_key" in row and "group_label" in row for row in grid), str(grid[:2])[:220])

    # 列键就是第二维的值本身：第二维为 NULL 时那个键是空串（18090 实测的形状）。这不是引擎坏掉，
    # 而是"这一档没有值"本来就该有一列。引擎要保证的是列集合 == 第二维出现过的值集合；
    # 把空键翻译成人话是显示层的事（pivotModel.pivotColumnLabel，由 UI 用例钉住）。
    def _col_of(row):
        v = row.get("group_label_2")
        return "" if v is None else str(v)

    observed = {_col_of(r) for r in raw2 if isinstance(r, dict)}
    check("列集合 == 第二维在原始行里取到的值 (NULL 折成空键，不是整列不见)",
          set(cols) == observed, f"cols={cols} observed={sorted(observed)}")
    unfilled = [r for r in raw2 if isinstance(r, dict) and _col_of(r) == ""]
    if check("第二维确有未填写的记录 (否则下面那条是空断言)", bool(unfilled), str(raw2)[:200]):
        want = {}
        for r in unfilled:
            want[r.get("group_key")] = want.get(r.get("group_key"), 0) + (r.get("group_count") or 0)
        misfiled = [row for row in grid
                    if (row.get("") or 0) != want.get(row.get("group_key"), 0)]
        check("「未填写」那一列数的是各自行自己的空值记录", not misfiled, str(misfiled[:2])[:220])
    check("每行的列数一致 (缺的那一格是 null, 不是整列不见)",
          {len([k for k in row if k not in DIMS]) for row in grid} == {len(cols)},
          [(r.get("group_key"), sorted(k for k in r if k not in DIMS)) for r in grid][:3])

    def _cells(row):
        return sum(v for k, v in row.items()
                   if k not in DIMS and isinstance(v, int) and not isinstance(v, bool))

    broken = [row for row in grid if _cells(row) != by_key.get(row.get("group_key"))]
    # 这条守恒是整个交叉表的地基: pivot 里再数一次 COUNT(*) 恒为 1, GROUP BY 少一维会把
    # 两档并成一档 —— 两种写法画出来都是"一张像模像样的表", 只有守恒能抓住。
    check("守恒: 每行格子相加 == 同一档在单维分组里的记录数", not broken, str(broken[:2])[:300])

for label, payload in [
    ("未知第二维", {"groupFields": ["level", "nope"]}),
    ("重复维度", {"groupFields": ["level", "level"]}),
    ("注入型第二维", {"groupFields": ["level", "is_vip) FROM t_customer t2 WHERE 1=1 --"]}),
    ("timeGroup 与多维同时", {"groupFields": ["level", "is_vip"], "timeGroup": "MONTH"}),
]:
    bad = call("POST", "/api/lc/runtime/shape",
               dict(AGG_SCOPE, aggregations={}, shape=PIVOT_PROGRAM, **payload),
               params={"entityCode": "customer"})[0]
    check(f"shape 拒绝 {label}", isinstance(bad, dict) and bad.get("success") is False
          and bad.get("code") == 400, str(bad)[:160])

modern = call("POST", "/api/lc/runtime/aggregate", dict(AGG_SCOPE, groupFields=["level"]),
              params={"entityCode": "customer"})[0]
check("一维 groupFields 与沿用 groupField 返回同一批分组 (老调用方不换口径)",
      D(modern) == D(single), f"{str(D(modern))[:140]} != {str(D(single))[:140]}")
ok("多维守卫后 z_lc_app 仍在", call("GET", "/api/lc/app/list")[0])

# ---------------------------------------------------------------- time buckets
print("\n[15d2] 时间分桶 timeGroup（图表时间轴的地基）")
# 图表时间轴不能靠"拉一页记录前端自己数"：数据一多就被分页静默截断，画出来的趋势是半张图。
# 锚点用 1937 年 —— 全库不会有人撞它，所以桶计数能精确断言，而不是"结果非空"这种废话。
bucket_ids = []
for nm, bd, lv in [("分桶甲", "1937-04-02", "1937-04-02 08:15:00"),
                   ("分桶乙", "1937-04-19", "1937-04-19 21:40:00"),
                   ("分桶丙", "1937-11-05", "1937-11-05 12:00:00")]:
    r = call("POST", "/api/lc/runtime/create",
             dict(AGG_SCOPE, fieldValues={"customer_name": nm, "birthday": bd, "last_visit": lv}),
             params={"entityCode": "customer"})[0]
    if isinstance(D(r), int):
        bucket_ids.append(D(r))
check("分桶用例的三条记录建起来了", len(bucket_ids) == 3, bucket_ids)

BUCKET = dict(AGG_SCOPE, filters={"customer_name:like": "分桶"})
by_year = D(call("POST", "/api/lc/runtime/aggregate",
                 dict(BUCKET, groupField="birthday", timeGroup="YEAR"),
                 params={"entityCode": "customer"})[0])
yrows = by_year if isinstance(by_year, list) else []
check("YEAR 分桶把同年的记录并成一桶",
      len(yrows) == 1 and yrows[0].get("bucket_year") == 1937 and yrows[0].get("group_count") == 3,
      by_year)
if yrows:
    check("YEAR 粒度不多返回 bucket_month",
          "bucket_month" not in yrows[0] and "bucket_day" not in yrows[0], yrows[0])

by_month = D(call("POST", "/api/lc/runtime/aggregate",
                  dict(BUCKET, groupField="birthday", timeGroup="MONTH",
                       aggregations={"age": ["MAX"]}),
                  params={"entityCode": "customer"})[0])
mrows = by_month if isinstance(by_month, list) else []
check("MONTH 分桶按月份拆开且时间正序",
      [(r.get("bucket_year"), r.get("bucket_month"), r.get("group_count")) for r in mrows]
      == [(1937, 4, 2), (1937, 11, 1)], by_month)
if mrows:
    check("分桶与数值聚合可以同时要", "max_age" in mrows[0], mrows[0])

# DATETIME 直接 group by 会一记录一桶（时间戳几乎不重复），分桶才画得出"按天"
by_day = D(call("POST", "/api/lc/runtime/aggregate",
                dict(BUCKET, groupField="last_visit", timeGroup="DAY"),
                params={"entityCode": "customer"})[0])
drows = by_day if isinstance(by_day, list) else []
check("DATETIME 按天分桶收进 3 个日历日",
      len(drows) == 3 and all(r.get("group_count") == 1 and r.get("bucket_day") for r in drows),
      by_day)

undated = D(call("POST", "/api/lc/runtime/create",
                 dict(AGG_SCOPE, fieldValues={"customer_name": "分桶无日期"}),
                 params={"entityCode": "customer"})[0])
uid = undated if isinstance(undated, int) else None
check("无日期记录建起来了（它同样落在「分桶」like 范围内）", uid is not None, undated)
after_null = D(call("POST", "/api/lc/runtime/aggregate",
                    dict(BUCKET, groupField="birthday", timeGroup="YEAR"),
                    params={"entityCode": "customer"})[0])
arows = after_null if isinstance(after_null, list) else []
check("未填日期的记录不进入时间轴，也不会冒出一个空桶",
      len(arows) == 1 and arows[0].get("group_count") == 3, after_null)

for label, payload in [
    ("非日期列按时间分桶", dict(groupField="level", timeGroup="MONTH")),
    ("没有时间列可分桶", dict(timeGroup="MONTH")),
    ("WEEK（各库方言不同，宁可不做）", dict(groupField="birthday", timeGroup="WEEK")),
]:
    bad = call("POST", "/api/lc/runtime/aggregate", dict(BUCKET, **payload),
               params={"entityCode": "customer"})[0]
    check(f"分桶拒绝 {label}", isinstance(bad, dict) and bad.get("success") is False
          and bad.get("code") == 400, str(bad)[:150])

still_ok = D(call("POST", "/api/lc/runtime/aggregate",
                  dict(BUCKET, groupField="birthday", timeGroup="MONTH"),
                  params={"entityCode": "customer"})[0])
check("被拒绝的请求不污染后续聚合",
      isinstance(still_ok, list) and len(still_ok) == 2, still_ok)

for rid in bucket_ids + ([uid] if uid else []):
    call("POST", "/api/lc/runtime/delete", dict(AGG_SCOPE, id=rid), params={"entityCode": "customer"})

# ---------------------------------------------------------------- partial update
print("\n[15e] 部分更新 vs 必填校验（内联编辑/看板拖拽的地基）")
# [15c] 收尾时把 cid 删掉了, 这里必须自带一条新记录, 不能复用别人的残骸
seed = call("POST", "/api/lc/runtime/create",
            dict(AGG_SCOPE, fieldValues={"customer_name": "部分更新用例", "age": 30, "level": "A"}),
            params={"entityCode": "customer"})[0]
cid = D(seed) if isinstance(D(seed), int) else None
check("部分更新用例自备记录", cid is not None, seed)
before_title = D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
                       params={"entityCode": "customer"})[0]).get("customer_name")
partial = call("POST", "/api/lc/runtime/update",
               dict(AGG_SCOPE, fieldValues={"id": cid, "age": 45}),
               params={"entityCode": "customer"})[0]
ok("只提交 age（未提交必填 customer_name）", partial)
after_row = D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
                   params={"entityCode": "customer"})[0])
check("未提交的必填列保持原值", after_row.get("customer_name") == before_title, after_row)
check("提交的列确实改了", after_row.get("age") == 45, after_row)

blank = call("POST", "/api/lc/runtime/update",
             dict(AGG_SCOPE, fieldValues={"id": cid, "customer_name": ""}),
             params={"entityCode": "customer"})[0]
check("显式清空必填列仍被拒绝", isinstance(blank, dict) and blank.get("success") is False
      and blank.get("code") == 400, str(blank)[:150])

spoof = call("POST", "/api/lc/runtime/update",
             dict(AGG_SCOPE, fieldValues={"id": cid, "age": 46},
                  existingValues={"customer_name": "客户端伪造"}),
             params={"entityCode": "customer"})[0]
ok("客户端塞 existingValues 不能左右校验", spoof)
check("伪造的前像不会写库",
      D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
             params={"entityCode": "customer"})[0]).get("customer_name") == before_title,
      "spoofed pre-image leaked into the row")

# 看板拖拽 = 一次部分更新，端到端确认这条路通
kanban_move = call("POST", "/api/lc/runtime/update",
                   dict(AGG_SCOPE, fieldValues={"id": cid, "level": "B"}),
                   params={"entityCode": "customer"})[0]
ok("看板拖拽式单列更新", kanban_move)
check("分组字段确实换了", D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
                                params={"entityCode": "customer"})[0]).get("level") == "B",
      "stage move did not persist")

# 部分更新里 null 与"根本没提交这一列"是同义的（18090 实测：给 null 之后记录原样留着 B）。
# 这条口径要是不钉住，交叉表那个"未填写"档就说不清是哪来的 —— 前端清空一格走的是空串，
# 而库里空串存的就是空串（不是 NULL），两件事都得是实测的而不是猜的。
null_update = call("POST", "/api/lc/runtime/update",
                   dict(AGG_SCOPE, fieldValues={"id": cid, "level": None}),
                   params={"entityCode": "customer"})[0]
ok("提交 level:null 不报错", null_update)
check("null 与「没提交这一列」同义：值保持原样",
      D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
             params={"entityCode": "customer"})[0]).get("level") == "B",
      str(D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
                 params={"entityCode": "customer"})[0]))[:160])
empty_update = call("POST", "/api/lc/runtime/update",
                    dict(AGG_SCOPE, fieldValues={"id": cid, "level": ""}),
                    params={"entityCode": "customer"})[0]
ok("提交 level:\"\" 是一次数值写入（不是空操作）", empty_update)
cleared_row = D(call("POST", "/api/lc/runtime/get", dict(AGG_SCOPE, id=cid),
                     params={"entityCode": "customer"})[0])
check("空串才真的清空这一列，且库里存的是空串不是 NULL",
      cleared_row.get("level") == "", str(cleared_row)[:160])
call("POST", "/api/lc/runtime/update", dict(AGG_SCOPE, fieldValues={"id": cid, "level": "B"}),
     params={"entityCode": "customer"})

# ---------------------------------------------------------------- batch import
print("\n[15f] 服务端批量导入（preview 零写入 / commit 整批 all-or-nothing）")
IMP = {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT}
before_count = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 1},
                      params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]).get("total", 0)

pv = call("POST", "/api/lc/runtime/import/preview",
          {"records": [{"customer_name": "导入甲", "age": 20},
                       {"age": 30},
                       {"customer_name": "导入乙", "age": "不是数字"}]},
          params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
ok("POST /runtime/import/preview", pv)
pvd = D(pv)
check("preview 统计 total/validCount", pvd.get("total") == 3 and pvd.get("validCount") == 1, pvd)
check("preview 不落库", pvd.get("applied") is False and pvd.get("insertedCount") == 0, pvd)
idxs = sorted(e.get("index") for e in pvd.get("errors", []))
check("行级错误带回行号", idxs == [1, 2], pvd.get("errors"))
after_preview = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 1},
                      params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]).get("total")
check("preview 之后行数没变", after_preview == before_count, (before_count, after_preview))

bad_commit = call("POST", "/api/lc/runtime/import/commit",
                  {"records": [{"customer_name": "能过"}, {"age": "x"}]},
                  params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
bcd = D(bad_commit)
check("有一行不合法则整批不写", bcd.get("applied") is False and bcd.get("insertedCount") == 0, bcd)
after_bad = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 1},
                   params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]).get("total")
check("失败的批次没留下半截数据", after_bad == before_count, (before_count, after_bad))

good = call("POST", "/api/lc/runtime/import/commit",
            {"records": [{"customer_name": "导入甲", "age": 20, "level": "A"},
                         {"customer_name": "导入乙", "age": 30, "level": "C"},
                         {"customer_name": "导入丙", "age": 40}]},
            params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
gd = D(good)
ok("POST /runtime/import/commit 全合法批次", good)
check("一次请求写入 3 行并回传 id", gd.get("applied") is True and gd.get("insertedCount") == 3
      and len(gd.get("ids", [])) == 3, gd)
after_good = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 1},
                    params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]).get("total")
check("行数确实 +3", after_good == before_count + 3, (before_count, after_good))

# 批量导入不是撤销的黑洞
globals()["_ACTOR"] = "imp"
undo_one = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
                params={"entityCode": "customer"})[0]
check("导入的行不属于自己的栈时不该被撤走",
      D(undo_one).get("applied") is False, undo_one)
globals()["_ACTOR"] = None
undo_anon = call("POST", "/api/lc/undo/undo", {"appCode": APP, "tenantCode": TENANT},
                 params={"entityCode": "customer"})[0]
ok("anonymous 撤销自己导入的行", undo_anon)
check("撤销的是导入产生的 CREATE", D(undo_anon).get("operation") == "CREATE", undo_anon)
after_undo = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 1},
                    params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]).get("total")
check("撤销后少一行", after_undo == before_count + 2, after_undo)

empty = call("POST", "/api/lc/runtime/import/commit", {"records": []},
             params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
check("空 records 直接 400", isinstance(empty, dict) and empty.get("success") is False, empty)

# ---------------------------------------------------------------- batch delete
print("\n[15h] 服务端批量删除（整批预检 / 一个请求删完 / 逐条 undo / 上限）")
# 这一节对着的是**独立启动的 jar**（不是测试语境里的 MockMvc），所以它抓的是
# "打包出来的服务真的会这么做"，而不是"某个测试上下文里恰好通过"。
BD_SCOPE = {"entityCode": "customer", "appCode": APP, "tenantCode": TENANT}


def bd_ids():
    """现拉一次全量 id 集合 —— 所有"删没删掉"的结论都以它为准，不看界面也不看回包。"""
    d = D(call("POST", "/api/lc/runtime/list", {"page": 1, "size": 500},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0])
    return sorted(int(r["id"]) for r in (d.get("records") or [])), d.get("total")


def bd_new(tag):
    j, _, _ = call("POST", "/api/lc/runtime/create",
                   dict(BD_SCOPE, fieldValues={"customer_name": f"批量删除{tag}", "age": 33, "level": "A"}),
                   params={"entityCode": "customer"})
    v = D(j)
    return v if isinstance(v, int) else None


def bd_delete(ids, actor=None):
    body = dict(BD_SCOPE, ids=ids)
    if actor is None:
        return call("POST", "/api/lc/runtime/delete-batch", body, params={"entityCode": "customer"})[0]
    return call_as(actor, "POST", "/api/lc/runtime/delete-batch", body, params={"entityCode": "customer"})


def bd_undo(actor):
    # 口径不对称：call() 回三元组、call_as() 只回响应体，混用时别再 [0]
    return call_as(actor, "POST", "/api/lc/undo/undo",
                   {"appCode": APP, "tenantCode": TENANT}, params={"entityCode": "customer"})


ids_before, total_before = bd_ids()
made = [bd_new(n) for n in ("甲", "乙", "丙", "丁", "戊")]
made = [m for m in made if m]
if check("为批量删除准备了 5 条数据", len(made) == 5, made):
    ids_now, total_now = bd_ids()
    check("准备阶段行数 +5", total_now == total_before + 5, (total_before, total_now))

    # 1) 混进一条不存在的 id：整批不动，而且要点名是哪一条
    ghost = max(ids_now or [0]) + 999999
    bad = bd_delete([made[0], made[1], ghost])
    bdd = D(bad)
    ok("POST /runtime/delete-batch（含幽灵 id）", bad)
    check("有一行不能删则整批不删",
          bdd.get("applied") is False and bdd.get("deletedCount") == 0 and not bdd.get("ids"), bdd)
    check("服务端报的是「整批未删除」", "整批未删除" in str(bdd.get("message")), bdd.get("message"))
    check("还点名是哪一条挡住", any(ghost == e.get("id") for e in bdd.get("errors") or []), bdd.get("errors"))
    ids_after_bad, total_after_bad = bd_ids()
    check("坏批之后库里 id 集合一条都没少", ids_after_bad == ids_now, (len(ids_now), len(ids_after_bad)))

    # 2) 好批：一个请求删两条，报的条数与库里少的条数一致
    good = bd_delete([made[0], made[1]], actor="bd_api")
    gd = D(good)
    ok("POST /runtime/delete-batch（全合法两条）", good)
    check("回包说删了 2 条", gd.get("applied") is True and gd.get("deletedCount") == 2
          and sorted(gd.get("ids") or []) == sorted([made[0], made[1]]), gd)
    check("回包文案与计数一致", "已删除 2 条" == str(gd.get("message")), gd.get("message"))
    ids_after_good, total_after_good = bd_ids()
    check("库里确实少了这两条", set([made[0], made[1]]).isdisjoint(ids_after_good)
          and total_after_good == total_now - 2, (total_now, total_after_good))
    got = call("POST", "/api/lc/runtime/get", dict(BD_SCOPE, id=made[0]),
               params={"entityCode": "customer"})[0]
    check("删掉的 id 再也读不到（软删对读不可见）", not D(got).get("id"), got)

    # 3) 重复 id 只算一条：否则第二条必然命中 0 行，把整批无辜拖进回滚
    dup = bd_delete([made[2], made[2], made[2]], actor="bd_api")
    dd = D(dup)
    check("重复 id 去重后按一条处理", dd.get("total") == 1 and dd.get("deletedCount") == 1
          and dd.get("applied") is True, dd)
    check("去重后不回滚", dd.get("rolledBack") is False, dd)

    # 4) 请求本身不合法的两种：空批次、超上限 —— 都不许动数据
    base_ids, base_total = bd_ids()
    empty_bd = bd_delete([])
    check("空 ids 被拒而不是假装成功", isinstance(empty_bd, dict) and empty_bd.get("success") is False,
          str(empty_bd)[:160])
    oversize = bd_delete([10_000_000 + i for i in range(2001)])
    check("超过 2000 条上限被拒", isinstance(oversize, dict) and oversize.get("success") is False,
          str(oversize)[:120])
    check("拒的时候给的是可操作提示（分批）", "分批" in str((oversize or {}).get("message")),
          str(oversize)[:160])
    ids_rejected, total_rejected = bd_ids()
    check("被拒的请求一行都没删", ids_rejected == base_ids and total_rejected == base_total,
          (base_total, total_rejected))

    # 5) 撤销栈是逐条登记的：3 条删除要 3 次 undo，每次只回来 1 行
    #    （如果服务端把整批写成一条日志，这里第一次 undo 就会一次回来 2 行）
    #    ⚠ 必须一次撤销、一次读数地写：`u1, u2, u3 = undo(), undo(), undo()` 会把三次
    #      请求全跑完才读第一次的行数，中间态根本量不到（我第一版就这么错过了两次）。
    u1 = bd_undo("bd_api")
    ok("撤销批量删除（第 1 次）", u1)
    check("撤销的是 DELETE", D(u1).get("operation") == "DELETE", u1)
    t1 = bd_ids()[1]
    check("一次 undo 只回来一行", t1 == base_total + 1, (base_total, t1))
    u2 = bd_undo("bd_api")
    ok("撤销批量删除（第 2 次）", u2)
    # 空栈的 undo 也是 HTTP 200 + success:true，只是 applied:false —— 光看信封看不出来撤销真发生过
    check("第 2 次撤销的确实是一条 DELETE", D(u2).get("operation") == "DELETE", u2)
    t2 = bd_ids()[1]
    check("第二次 undo 再回来一行", t2 == base_total + 2, (base_total, t2))
    u3 = bd_undo("bd_api")
    ok("撤销批量删除（第 3 次）", u3)
    check("第 3 次撤销的也确实是一条 DELETE", D(u3).get("operation") == "DELETE", u3)
    ids_final, total_final = bd_ids()
    check("三次撤销之后 5 条数据一条不少地回来了",
          set(made).issubset(set(ids_final)) and total_final == total_now, (total_now, total_final))
    # 别人撤不动我的栈（撤销栈按 actor 分）
    stranger = bd_undo("bd_stranger")
    check("别人的栈里没有这批删除", D(stranger).get("applied") is False, stranger)

# ---------------------------------------------------------------- stats & 值域提示
print("\n[15g] 非数值列统计（去重数/填充率）与字典值域 warning")
st = call("POST", "/api/lc/runtime/aggregate",
          {"aggregations": {"customer_name": ["DISTINCT", "FILLED"],
                            "phone": ["DISTINCT", "FILLED"],
                            "level": ["DISTINCT"]}},
          params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
ok("文本列可以算 DISTINCT / FILLED", st)
srow = (D(st) or [{}])[0]
check("去重数与行数同时回传", "distinct_customer_name" in srow and "group_count" in srow, srow)
check("未填的列 FILLED 应小于总数", srow.get("filled_phone", 0) <= srow.get("group_count", 0), srow)
check("去重数不超过非空数", srow.get("distinct_phone", 0) <= max(srow.get("filled_phone", 0), 0), srow)

bad_sum = call("POST", "/api/lc/runtime/aggregate", {"aggregations": {"customer_name": ["SUM"]}},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
check("文本列 SUM 仍然被拒", isinstance(bad_sum, dict) and bad_sum.get("success") is False, bad_sum)
bad_fn = call("POST", "/api/lc/runtime/aggregate", {"aggregations": {"customer_name": ["STDDEV"]}},
              params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
check("未知函数被拒且报的是函数问题", isinstance(bad_fn, dict)
      and "Unsupported aggregation function" in str(bad_fn.get("message")), str(bad_fn)[:140])

warn_pv = call("POST", "/api/lc/runtime/import/preview",
               {"records": [{"customer_name": "越界值", "level": "ZZZ_NOT_IN_DICT"}]},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
wd = D(warn_pv)
check("越界字典值算校验通过（不阻断）", wd.get("validCount") == 1 and not wd.get("errors"), wd)
ws = wd.get("warnings") or []
check("但会回一条值域 warning", len(ws) >= 1, wd)
if ws:
    check("warning 指到行与列", ws[0].get("index") == 0 and ws[0].get("fieldCode") == "level", ws[0])
    check("warning 文案里带出真实值", "ZZZ_NOT_IN_DICT" in ws[0].get("message", ""), ws[0])
check("message 会说明有 N 处需要注意", "不在字典" in str(wd.get("message")), wd.get("message"))

warn_cm = call("POST", "/api/lc/runtime/import/commit",
               {"records": [{"customer_name": "越界写入", "level": "QQQ"}]},
               params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
cd = D(warn_cm)
check("warning 不阻止写入", cd.get("applied") is True, cd)
check("commit 也带回 warnings", len(cd.get("warnings") or []) >= 1, cd)
clean = call("POST", "/api/lc/runtime/import/commit",
             {"records": [{"customer_name": "正常写入", "level": "A"}]},
             params={"entityCode": "customer", "appCode": APP, "tenantCode": TENANT})[0]
check("值在字典里时不该有 warning", len(D(clean).get("warnings") or []) == 0, D(clean))

# ------------------------------------------------- unique code collisions
print("\n[15i] 唯一编码撞车: 可读的 400，不是 500 + 数据库细节")

def leak_free(env):
    """唯一键冲突的响应里不许出现索引名/物理表名/SQL 片段。"""
    msg = str((env or {}).get("message") or "").upper()
    return not any(tok in msg for tok in ("UNIQUE INDEX", "UK_", "Z_LC_", "PRIMARY KEY", "SELECT"))

DUP_DICT = f"e2e_dup_{SUF}"
dup_dict_body = {"tenantCode": TENANT, "dictCode": DUP_DICT, "dictName": "重复探针", "status": "ENABLED"}
dj, _, _ = call("POST", "/api/lc/dict/create", dup_dict_body)
ok("POST /dict/create (探针字典)", dj)
probe_dict_id = D(dj).get("id")
j, s, _ = call("POST", "/api/lc/dict/create", dup_dict_body)
check("重复建同 code 字典被拒", isinstance(j, dict) and j.get("success") is False, str(j)[:160])
check("重复建字典是 400 而不是 500", s == 400 and (j or {}).get("code") == 400, f"http={s} body={j}")
check("400 文案说清是编码重复", "已存在" in str((j or {}).get("message")), str(j)[:160])
check("字典撞车的响应不带数据库细节", leak_free(j), str(j)[:160])

# 软删之后拿同一个 code 重建 —— 这才是"预检漏掉软删行"唯一的暴露形状：活着的重复任何预检都拦得住。
dj, _, _ = call("POST", "/api/lc/dict/delete", {"id": probe_dict_id})
ok("POST /dict/delete (软删探针字典)", dj)
j, s, _ = call("POST", "/api/lc/dict/create", dup_dict_body)
check("复用已删除的 dictCode 仍被拒", s == 400 and (j or {}).get("success") is False, f"http={s} body={str(j)[:160]}")
check("文案要说清字典此前已被删除", "已被删除" in str((j or {}).get("message")), str(j)[:200])

TMP_APP = f"e2edupapp{SUF}"
tmp_app_body = {"tenantCode": TENANT, "appCode": TMP_APP, "appName": "重复探针"}
ok("POST /app/create (探针应用)", call("POST", "/api/lc/app/create", tmp_app_body)[0])
ok("POST /app/delete (软删探针应用)", call("POST", "/api/lc/app/delete", {"appCode": TMP_APP})[0])
j, s, _ = call("POST", "/api/lc/app/create", tmp_app_body)
check("复用已删除的 appCode 仍被拒(唯一索引不含 deleted)", s == 400 and (j or {}).get("success") is False, f"http={s} body={str(j)[:160]}")
check("文案要解释是「此前已被删除」", "已被删除" in str((j or {}).get("message")), str(j)[:200])
check("应用撞车不带索引名/表名", leak_free(j), str(j)[:160])

REL = {"tenantCode": TENANT, "appCode": APP, "relationCode": f"e2eduprel{SUF}",
       "relationName": "重复探针", "sourceEntityCode": "customer", "targetEntityCode": "customer",
       "relationType": "ONE_TO_MANY"}
rj, _, _ = call("POST", "/api/lc/relation/create", REL)
ok("POST /relation/create (探针关系)", rj)
probe_rel_id = D(rj).get("id")
j, s, _ = call("POST", "/api/lc/relation/create", REL)
check("重复建关系是 400 而不是 500", s == 400 and (j or {}).get("code") == 400, f"http={s} body={str(j)[:200]}")
check("关系撞车的文案指名 relationCode", "关系已存在" in str((j or {}).get("message")), str(j)[:160])
check("关系撞车不带 uk_ 与列名", leak_free(j), str(j)[:160])

rj, _, _ = call("POST", "/api/lc/relation/delete", {"id": probe_rel_id})
ok("POST /relation/delete (软删探针关系)", rj)
j, s, _ = call("POST", "/api/lc/relation/create", REL)
check("复用已删除的 relationCode 仍被拒", s == 400 and (j or {}).get("success") is False, f"http={s} body={str(j)[:200]}")
check("文案要说清关系此前已被删除", "已被删除" in str((j or {}).get("message")), str(j)[:200])

# SchemaAdminController 有一条独立的建应用入口（设计器用），它自己也有唯一性预检 ——
# 两条路由共用 z_lc_app 表和同一个唯一索引，所以漏掉软删行会各自独立地退化。
SA_APP = f"e2edupsa{SUF}"
sa_body = {"tenantCode": TENANT, "appCode": SA_APP, "appName": "设计器探针"}
j, s, _ = call("POST", "/api/lc/admin/app/create", sa_body)
ok("POST /admin/app/create (设计器探针应用)", j)
ok("POST /app/delete (软删设计器探针应用)", call("POST", "/api/lc/app/delete", {"appCode": SA_APP})[0])
j, s, _ = call("POST", "/api/lc/admin/app/create", sa_body)
check("设计器路由复用已删除 appCode 也被拒", isinstance(j, dict) and j.get("success") is False, f"http={s} body={str(j)[:200]}")
check("文案要说清设计器路由此前已被删除", "已被删除" in str((j or {}).get("message")), str(j)[:200])

# ------------------------------------------------- field-code guard (#34/#35)
# 引擎给每张受管表自建 id/tenant_code/deleted/create_time/update_time 这 5 列，
# 而这 5 个全是"合法标识符" —— 旧的那道正则拦不住：entity/create HTTP 200 收下元数据，
# 要等 provision 建表才炸裸 500，且 provision-all 在第一个坏实体上抛、同应用干净实体一起没建成。
# 这一节盯的是**写入口**：create 与 update 两个调用点各自一组断言（注入摘掉任一个都会红）。
print("\n[15j] 字段编码撞引擎自建列 / 非法列名：在写入口拒，不到建表才炸")

FC_APP = f"e2efc{SUF}"
FC_TABLE = f"e2e_fc_{SUF}"
RESERVED = ["id", "tenant_code", "deleted", "create_time", "update_time"]
LEGAL_COLS = ["order_no", "amount", "note"]


def fc_field(code, order=1):
    return {"fieldCode": code, "fieldName": "探针列", "fieldType": "STRING",
            "fieldLength": 32, "sortOrder": order}


def fc_entity(code, entity_code):
    return {"tenantCode": TENANT, "appCode": FC_APP, "entityCode": entity_code,
            "entityName": "编码探针", "tableName": FC_TABLE, "description": "e2e",
            "fields": [fc_field(code)]}


def create_entity(req):
    return call("POST", "/api/lc/admin/app/entity/create", req,
                params={"appCode": FC_APP, "tenantCode": TENANT})


ok("POST /admin/app/create (字段编码探针应用)",
   call("POST", "/api/lc/admin/app/create",
        {"tenantCode": TENANT, "appCode": FC_APP, "appName": "编码探针"})[0])

for code in RESERVED:
    j, s, _ = create_entity(fc_entity(code, f"fc_{code}"))
    msg = str((j or {}).get("message") or "")
    check(f"撞自建列 {code} 在 entity/create 就被拒(不是等建表才炸)",
          isinstance(j, dict) and j.get("success") is False and j.get("code") == 400,
          f"http={s} body={str(j)[:200]}")
    check(f"撞自建列 {code} 的文案列出保留列，且不带 SQL/物理表名细节",
          "撞了引擎自建列" in msg and "保留列: id, tenant_code, deleted, create_time, update_time" in msg
          and leak_free(j), msg[:200])

# MySQL/H2 列名不分大小写，所以 `ID` 照样撞 —— 这条专门盯 toLowerCase。
j, s, _ = create_entity(fc_entity("ID", "fc_upper_id"))
msg = str((j or {}).get("message") or "")
check("大写 ID 同样被拒（撞名比较不分大小写）",
      isinstance(j, dict) and j.get("success") is False and "撞了引擎自建列" in msg,
      f"http={s} body={msg[:200]}")
check("文案回显的是用户实际写进去的那个大小写", "ID" in msg, msg[:200])

for i, (bad, want) in enumerate((("2bad", "不是合法列名"), ("我的字段", "不是合法列名"),
                                 ("has space", "不是合法列名"), ("", "fieldCode is required"))):
    j, s, _ = create_entity(fc_entity(bad, f"fc_bad_{i}"))
    msg = str((j or {}).get("message") or "")
    check(f"非法列名 {bad!r} 被拒且说的是列名规则",
          isinstance(j, dict) and j.get("success") is False and want in msg,
          f"http={s} body={msg[:200]}")
    # #35 的另一半：非法编码在 buildCreateTableDdl 里原本是 continue 静默跳过 ——
    # 建表照样返回成功，于是元数据说有这一列、物理表里没有。所以文案必须指名**哪一列**。
    check(f"非法列名 {bad!r} 的文案指名是哪一列（不是笼统一句失败）",
          (bad in msg) if bad else ("required" in msg), msg[:200])

j, _, _ = call("GET", "/api/lc/admin/app/entity/list", params={"appCode": FC_APP, "tenantCode": TENANT})
ok("GET /admin/app/entity/list (探针应用)", j)
listed = D(j) if isinstance(D(j), list) else []
check("上面那些被拒的提交一行都没落库（闸在写入之前，不是事后回滚）",
      listed == [], [e.get("entityCode") for e in listed])

j, _, _ = create_entity({"tenantCode": TENANT, "appCode": FC_APP, "entityCode": "fc_clean",
                         "entityName": "干净实体", "tableName": FC_TABLE, "description": "e2e",
                         "fields": [fc_field(c, i + 1) for i, c in enumerate(LEGAL_COLS)]})
ok("合法编码的实体照常建成（这道闸没把正常路径一起按住）", j)
fc_ent_id = D(j).get("id")

j, s, _ = call("POST", "/api/lc/admin/entity/provision", params={"id": fc_ent_id})
ok("POST /admin/entity/provision (干净实体建表不被闸误伤)", j)
j, s, _ = call("POST", "/api/lc/runtime/list", {"page": 1, "size": 5, "conditions": []},
               params={"appCode": FC_APP, "tenantCode": TENANT, "entityCode": "fc_clean"})
ok("新表随即可查（元数据与物理表一致，不是静默少列）", j)
check("空表读到 total=0", D(j).get("total") == 0, str(j)[:200])

j, s, _ = call("POST", "/api/lc/admin/db/table/import",
               params={"tableName": FC_TABLE, "tenantCode": TENANT, "appCode": FC_APP,
                       "entityCode": f"fc_mapped_{SUF}", "autoProvision": "false"})
ok("POST /admin/db/table/import (逆向映射刚建好的物理表)", j)
# mapped 可能是 fail 信封（注入实验里就会这样），D() 会塌成 {}；这里不兜住的话
# 后面 (None).get 会让整个 [15j] 抛异常退出，那一轮就变成"跑不完"而不是"哪条红"。
mapped = D(j) or {}
map_codes = [f.get("fieldCode") for f in (mapped.get("fields") or [])]
check("逆向映射跳过引擎自建列（一律拒绝会把 /table/import 打死：真表的 id/deleted 是合法物理列）",
      bool(map_codes) and all(c not in RESERVED for c in map_codes), map_codes)
check("用户列一列不少地映射回来", map_codes == LEGAL_COLS, map_codes)
check("跳过的清单写进 description，不是静默丢掉",
      "已跳过引擎自建列" in str(mapped.get("description") or ""), str(mapped.get("description"))[:200])

# update 探针放在最后，而不是紧跟 fc_clean 的创建：它一旦被放过，fc_clean 的元数据就变成
# [deleted]，后面 provision 会撞 Duplicate column name、/runtime/list 与 /table/import 一起翻车。
# 那时"哪个入口漏了"这个问题就有 6 种答案，注入实验再也指不回 updateEntity 这一支。
j, s, _ = call("PUT", "/api/lc/admin/entity", {"id": fc_ent_id, "tenantCode": TENANT,
                                               "appCode": FC_APP, "entityCode": "fc_clean",
                                               "entityName": "干净实体", "tableName": FC_TABLE,
                                               "fields": [fc_field("deleted")]},
               params={"id": fc_ent_id})
check("updateEntity 是第二个写入口：改成撞名列同样被拒",
      isinstance(j, dict) and j.get("success") is False and "撞了引擎自建列" in str(j.get("message") or ""),
      f"http={s} body={str(j)[:200]}")
j, _, _ = call("GET", "/api/lc/admin/entity", params={"id": fc_ent_id})
kept = [f.get("fieldCode") for f in (D(j).get("fields") or [])]
check("被拒的 update 一行都没改（字段还是原来那三列、原顺序）", kept == LEGAL_COLS, kept)

call("DELETE", "/api/lc/admin/entity", params={"id": fc_ent_id})
if mapped.get("id"):
    call("DELETE", "/api/lc/admin/entity", params={"id": mapped.get("id")})
call("POST", "/api/lc/app/delete", {"appCode": FC_APP})

# ------------------------------------------------- pipeline wiring (#41)
# 这一轮之前, z_lc_pipeline_config 里的 order/enabled/stages 对运行时一个都不影响: Pipeline 跑的是
# 按类名字典序排好的全部 Spring bean, 而配置页宣传"校验、字典解析、外部通知按 order 执行" ——
# WEBHOOK / SCRIPT 连执行器都没有, AFTER_* 连挂接点都没有, 保存成功和真的在跑是两件事。
# 这一节盯两层: 写入口拒掉引擎兑现不了的配置; 收下的配置真的决定跑哪几段、按什么顺序跑。
# 顺序的可观察证据是错误消息里点名的处理器 —— [RequiredCheck] 为必填 vs [TypeConvert] 类型转换失败:
# 同一份请求体, 只改配置, 消息跟着换。别的都能骗过这一格, 执行顺序骗不过。
print("\n[15p] 流水线配置决定执行链：没接线的在写入口拒，收下的按 order 真跑")

PP_APP = f"e2epp{SUF}"
PP_TABLE = f"e2e_pipe_{SUF}"
GATES = ["REQUIRED_CHECK", "TYPE_CONVERT", "VALUE_VALIDATE"]
BLANK_AMOUNT = {"amount": "", "note": "ok"}


def pp_stages(*types):
    return json.dumps([{"type": t, "config": {}, "order": i} for i, t in enumerate(types)])


def pp_config(entity="pipe_probe", trigger="BEFORE_CREATE", st=None, enabled=1, app=None):
    return {"tenantCode": TENANT, "appCode": PP_APP if app is None else app, "entityCode": entity,
            "triggerEvent": trigger, "stages": pp_stages(*GATES) if st is None else st,
            "enabled": enabled}


def cfg_rejected(name, body, want):
    j, s, _ = call("POST", "/api/lc/pipeline-config/create", body)
    msg = str((j or {}).get("message") or "")
    check(name, isinstance(j, dict) and j.get("success") is False and j.get("code") == 400 and s == 400,
          f"http={s} body={str(j)[:180]}")
    # 光拒不够, 还得说清要什么 —— 否则界面只能显示一句"不合法"
    check(f"{name}：文案点名要什么", want in msg and leak_free(j), msg[:200])


def pp_write(entity, values):
    return call("POST", "/api/lc/runtime/create",
                {"entityCode": entity, "appCode": PP_APP, "tenantCode": TENANT, "fieldValues": values},
                params={"entityCode": entity})


def env_msg(env):
    return str((env or {}).get("message") or "")


def pp_list(entity="pipe_probe"):
    j, _, _ = call("GET", "/api/lc/pipeline-config/list", params={"appCode": PP_APP, "entityCode": entity})
    return D(j) if isinstance(D(j), list) else []


# 被拒探针现在各占一个实体, 所以"一行都没落库"必须按**应用**数, 按实体数只会数到那个干净的。
def pp_list_app():
    j, _, _ = call("GET", "/api/lc/pipeline-config/list", params={"appCode": PP_APP})
    return D(j) if isinstance(D(j), list) else []


ok("POST /admin/app/create (流水线探针应用)",
   call("POST", "/api/lc/admin/app/create",
        {"tenantCode": TENANT, "appCode": PP_APP, "appName": "流水线探针"})[0])

PP_FIELDS = [
    {"fieldCode": "amount", "fieldName": "金额", "fieldType": "INT", "required": True, "sortOrder": 1},
    {"fieldCode": "note", "fieldName": "备注", "fieldType": "STRING", "fieldLength": 5, "sortOrder": 2},
]
for ent, table in (("pipe_probe", PP_TABLE), ("pipe_other", PP_TABLE + "_2")):
    je, _, _ = call("POST", "/api/lc/admin/app/entity/create",
                    {"tenantCode": TENANT, "appCode": PP_APP, "entityCode": ent,
                     "entityName": "流水线探针表", "tableName": table, "description": "e2e",
                     "fields": PP_FIELDS},
                    params={"appCode": PP_APP, "tenantCode": TENANT})
    ok(f"探针实体 {ent} 建成", je)
    ok(f"POST /admin/entity/provision ({ent})",
       call("POST", "/api/lc/admin/entity/provision", params={"id": D(je).get("id")})[0])

# ---- 写入口: 引擎兑现不了的配置一律不收
# 每条被拒的提交各占一个实体, 这不是随手起的名字: 同一个 (实体, 挂接点) 上, 前一行落库会让后一行
# 被**另一道闸**拒掉, 于是那条检查照样绿。实测过这种遮蔽 —— 摘掉 "没有执行器" 那道闸之后
# WEBHOOK 那行落地, SCRIPT 那行紧接着被 "同一挂接点已经有一份启用" 顶回来, "阶段 SCRIPT -> 拒"
# 一条都不红。一行只测一道规则, 注入才指得回它打掉的到底是哪一道。
REJECTS = [
    ("webhook", dict(st=pp_stages(*GATES, "WEBHOOK")), "没有执行器", "阶段 WEBHOOK 没有执行器 -> 拒"),
    ("script", dict(st=pp_stages(*GATES, "SCRIPT")), "没有执行器", "阶段 SCRIPT 没有执行器 -> 拒"),
    ("after_create", dict(trigger="AFTER_CREATE"), "没有挂接点", "触发事件 AFTER_CREATE 没有挂接点 -> 拒"),
    ("after_update", dict(trigger="AFTER_UPDATE"), "没有挂接点", "触发事件 AFTER_UPDATE 没有挂接点 -> 拒"),
    ("after_delete", dict(trigger="AFTER_DELETE"), "没有挂接点", "触发事件 AFTER_DELETE 没有挂接点 -> 拒"),
    ("no_gates", dict(st=pp_stages("DICT_RESOLVE")), "REQUIRED_CHECK",
     "摘掉三道闸门 -> 拒（等于让该实体的写入绕过校验）"),
    ("inverted", dict(st=pp_stages("VALUE_VALIDATE", "TYPE_CONVERT", "REQUIRED_CHECK")), "之后",
     "值校验排在类型转换之前 -> 拒"),
    ("dup_stage", dict(st=pp_stages("REQUIRED_CHECK", "TYPE_CONVERT", "VALUE_VALIDATE", "TYPE_CONVERT")),
     "重复配置", "同一阶段重复配置 -> 拒"),
    ("empty_chain", dict(st="[]"), "非空 JSON 数组", "空阶段链 -> 拒"),
    ("not_array", dict(st='{"type":"TYPE_CONVERT"}'), "非空 JSON 数组", "阶段链不是数组 -> 拒"),
    ("frac_order", dict(st='[{"type":"REQUIRED_CHECK","order":1.5},{"type":"TYPE_CONVERT"},'
                          '{"type":"VALUE_VALIDATE"}]'), "整数", "order 不是整数 -> 拒"),
    ("lower_type", dict(st=pp_stages("required_check", *GATES[1:])), "required_check",
     "未知阶段类型大小写不放过 -> 拒"),
    ("enabled_bogus", dict(enabled=2), "enabled",
     "enabled=2 -> 拒（这一行在 listByEvent 里永远查不到，等于存了条死数据）"),
]
for _slug, _kw, _want, _name in REJECTS:
    cfg_rejected(_name, pp_config(entity=f"pipe_r_{_slug}", **_kw), _want)
cfg_rejected("缺 appCode 的配置永远不会被执行 -> 拒", pp_config(entity="pipe_r_no_app", app=""), "appCode")
cfg_rejected("缺 entityCode 的配置永远不会被执行 -> 拒", pp_config(entity="  "), "entityCode")

# ---- 反向: 兑现得了的必须收, 而且只落这一行
j, s, _ = call("POST", "/api/lc/pipeline-config/create",
               pp_config(st=pp_stages("TYPE_CONVERT", "REQUIRED_CHECK", "VALUE_VALIDATE")))
if ok("引擎兑现得了的配置照收（只测拒绝会把校验写成永远抛异常也能全绿）", j):
    pp_cfg_id = D(j).get("id")
    check("创建回带 id", bool(pp_cfg_id), str(j)[:160])
else:
    pp_cfg_id = None
listed = pp_list_app()
check("上面 15 次被拒的提交一行都没落库（闸在写入之前）", len(listed) == 1,
      [(c.get("entityCode"), c.get("triggerEvent")) for c in listed])
cfg_rejected("同一挂接点上第二份启用配置 -> 拒（一个挂接点只跑一条链）", pp_config(), "已经有一份启用")
check("被拒的第二份没有把第一份顶掉", len(pp_list_app()) == 1,
      [(c.get("entityCode"), c.get("enabled")) for c in pp_list_app()])

# ---- toggle 的含糊请求
j, s, _ = call("POST", "/api/lc/pipeline-config/toggle", {"appCode": PP_APP, "enabled": 0})
check("toggle 缺 id 不能报成切换成功", isinstance(j, dict) and j.get("success") is False and s == 400,
      f"http={s} body={str(j)[:160]}")
j, s, _ = call("POST", "/api/lc/pipeline-config/toggle", {"id": pp_cfg_id, "appCode": PP_APP})
check("toggle 缺 enabled 不能按停用处理", isinstance(j, dict) and j.get("success") is False and s == 400,
      f"http={s} body={str(j)[:160]}")
rows_now = pp_list()
check("两次被拒的 toggle 一次都没改到那一行", len(rows_now) == 1 and rows_now[0].get("enabled") == 1,
      [c.get("enabled") for c in rows_now])
j, s, _ = call("POST", "/api/lc/pipeline-config/toggle",
               {"id": (rows_now[0] or {}).get("id"), "appCode": PP_APP, "enabled": 99})
check("enabled 只认 0/1，给个 99 不能当成开启", isinstance(j, dict) and j.get("success") is False, str(j)[:160])

# ---- 收下的配置真的决定执行链
j, s, _ = pp_write("pipe_probe", BLANK_AMOUNT)
check("配置链里 TYPE_CONVERT 在前 -> 空串先撞类型转换", isinstance(j, dict) and "[TypeConvert]" in env_msg(j),
      str(j)[:200])
check("runtime 的业务级失败是 HTTP 200 + 信封 code 400（前端必须读信封）", s == 200 and (j or {}).get("code") == 400,
      f"http={s} body={str(j)[:160]}")

j, _, _ = call("POST", "/api/lc/pipeline-config/toggle",
               {"id": pp_cfg_id, "appCode": PP_APP, "tenantCode": TENANT, "enabled": 0})
ok("停用这份配置", j)
j, _, _ = pp_write("pipe_probe", BLANK_AMOUNT)
check("停用后退回默认链（字典序 RequiredCheck 在前）：同一请求换一条消息",
      "[RequiredCheck]" in env_msg(j) and "为必填" in env_msg(j), str(j)[:200])
j, _, _ = call("POST", "/api/lc/pipeline-config/toggle",
               {"id": pp_cfg_id, "appCode": PP_APP, "tenantCode": TENANT, "enabled": 1})
ok("再启用这份配置", j)
j, _, _ = pp_write("pipe_probe", BLANK_AMOUNT)
check("再启用又换回 [TypeConvert]：enabled 与 order 都是真的在起作用", "[TypeConvert]" in env_msg(j), str(j)[:200])

j, _, _ = pp_write("pipe_other", BLANK_AMOUNT)
check("同一应用的另一个实体没配流水线，走的还是默认链（配置不串）", "[RequiredCheck]" in env_msg(j), str(j)[:200])

j, _, _ = pp_write("pipe_probe", {"amount": 7, "note": "备注长得放不下"})
check("三道闸门都在配置链里：值校验报出上限", "[ValueValidate]" in env_msg(j) and "上限 5" in env_msg(j),
      str(j)[:200])
j, _, _ = pp_write("pipe_probe", {"amount": 7, "note": "短"})
ok("合法写入照常通过（这道闸没把正常路径一起按住）", j)
pp_row_id = D(j)

j, _, _ = call("POST", "/api/lc/pipeline-config/create", pp_config(trigger="BEFORE_UPDATE", st=pp_stages(*GATES)))
ok("同一实体的 BEFORE_UPDATE 另有一份启用配置（挂接点不同，不算重复）", j)
pp_upd_id = D(j).get("id")
j, s, _ = call("POST", "/api/lc/runtime/update",
               {"entityCode": "pipe_probe", "appCode": PP_APP, "tenantCode": TENANT,
                "fieldValues": {"id": pp_row_id, "amount": ""}},
               params={"entityCode": "pipe_probe"})
check("两个写前挂接点各自生效：更新走 REQUIRED_CHECK 在前的那条链",
      isinstance(j, dict) and "[RequiredCheck]" in env_msg(j), str(j)[:200])
j, _, _ = pp_write("pipe_probe", BLANK_AMOUNT)
check("而同一条空值在创建挂接点上仍是 TypeConvert 先报（两条链互不顶替）", "[TypeConvert]" in env_msg(j),
      str(j)[:200])

for cfg_id in (pp_cfg_id, pp_upd_id):
    if cfg_id:
        call("POST", "/api/lc/pipeline-config/delete", {"id": cfg_id})
check("配置删掉后列表清空（delete 是真删这一行）", pp_list() == [], pp_list())
call("POST", "/api/lc/app/delete", {"appCode": PP_APP})

# ---------------------------------------------------------------- cleanup

print("\n[16] teardown")
j, _, _ = call("POST", "/api/lc/dict/items/delete", {"id": (items[0] or {}).get("id") if items else None})
check("dict item delete responds", isinstance(j, dict), str(j)[:120])
j, _, _ = call("POST", "/api/lc/app/archive", {"appCode": APP})
ok("POST /app/archive", j)

n = len(PASS) + len(FAIL)
print(f"\n=== E2E RESULT: {len(PASS)}/{n} passed ===")
if FAIL:
    print("FAILED:")
    for f in FAIL:
        print("   - " + f)
sys.exit(1 if FAIL else 0)
