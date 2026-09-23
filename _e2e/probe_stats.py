#!/usr/bin/env python3
"""Ad-hoc probe for non-numeric aggregates and dict-range warnings."""
import json
import random
import sys
import urllib.error
import urllib.request

B = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:18090").rstrip("/")


def post(path, body, params=None):
    url = B + path
    if params:
        url += "?" + "&".join(f"{k}={v}" for k, v in params.items())
    # 空 body 会被 Spring 判成 400 并回一个空响应，这里一律发个 {}
    data = json.dumps(body if body is not None else {}).encode()
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Tenant-Code", "default")
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            text = r.read().decode()
    except urllib.error.HTTPError as e:
        text = e.read().decode()
    except Exception as e:  # noqa: BLE001
        return {"_transport": str(e)}
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"_nonjson": text[:160]}


A = "fn%05d" % random.randrange(100000)
def step(label, payload):
    body = payload if isinstance(payload, dict) else {}
    if body.get("success") is False or "_transport" in body or "_nonjson" in body:
        print("!! %-14s -> %s" % (label, json.dumps(payload, ensure_ascii=False)[:220]))
    return payload


step("app", post("/api/lc/app/create", {"tenantCode": "default", "appCode": A, "appName": "统计"}))
step("dict", post("/api/lc/dict/create", {"tenantCode": "default", "dictCode": "c" + A, "dictName": "城市", "status": "ENABLED"}))
for i, (code, label) in enumerate([("BJ", "北京"), ("SH", "上海")]):
    step("dict-item", post("/api/lc/dict/items/create",
         {"tenantCode": "default", "dictCode": "c" + A, "itemCode": code,
          "itemLabel": label, "itemValue": code, "sortOrder": i + 1},
         params={"dictCode": "c" + A}))
step("entity", post("/api/lc/admin/app/entity/create",
     {"tenantCode": "default", "appCode": A, "entityCode": "p", "entityName": "人员",
      "tableName": "fn_p" + A,
      "fields": [
          {"fieldCode": "name", "fieldName": "姓名", "fieldType": "STRING",
           "required": True, "fieldLength": 32, "sortOrder": 1},
          {"fieldCode": "city", "fieldName": "城市", "fieldType": "STRING",
           "dictCode": "c" + A, "fieldLength": 16, "sortOrder": 2},
          {"fieldCode": "note", "fieldName": "备注", "fieldType": "TEXT", "sortOrder": 3}]},
     params={"appCode": A, "tenantCode": "default"}))
step("provision", post("/api/lc/admin/app/provision-all", {}, params={"appCode": A, "tenantCode": "default"}))
for name, city, note in [("甲", "BJ", "x"), ("乙", "SH", None), ("丙", "BJ", None)]:
    step("create", post("/api/lc/runtime/create",
         {"entityCode": "p", "appCode": A, "tenantCode": "default",
          "fieldValues": {"name": name, "city": city, "note": note}},
         params={"entityCode": "p"}))

print("app =", A)
r = post("/api/lc/runtime/aggregate",
         {"appCode": A, "tenantCode": "default",
          "aggregations": {"city": ["DISTINCT", "FILLED"],
                           "note": ["FILLED", "DISTINCT"],
                           "name": ["DISTINCT"]}},
         params={"entityCode": "p"})
print("DISTINCT/FILLED:", json.dumps(r.get("data"), ensure_ascii=False))

bad = post("/api/lc/runtime/aggregate",
           {"appCode": A, "tenantCode": "default", "aggregations": {"city": ["SUM"]}},
           params={"entityCode": "p"})
print("文本列 SUM 被拒:", bad.get("success"), "|", (bad.get("message") or "")[:70])

fn = post("/api/lc/runtime/aggregate",
          {"appCode": A, "tenantCode": "default", "aggregations": {"city": ["STDDEV"]}},
          params={"entityCode": "p"})
print("未知函数被拒:", fn.get("success"), "|", (fn.get("message") or "")[:60])

pv = post("/api/lc/runtime/import/preview",
          {"appCode": A, "tenantCode": "default",
           "records": [{"name": "丁", "city": "SZ"}, {"name": "戊", "city": "BJ"}]},
          params={"entityCode": "p"})
d = pv.get("data") or {}
print("preview: valid=%s errors=%s warnings=%s" % (d.get("validCount"), len(d.get("errors") or []),
                                                    len(d.get("warnings") or [])))
print("  message:", d.get("message"))
warn = (d.get("warnings") or [{}])[0]
print("  warning:", (warn.get("message") or "")[:90], "| field:", warn.get("fieldCode"), "| index:", warn.get("index"))

cm = post("/api/lc/runtime/import/commit",
          {"appCode": A, "tenantCode": "default", "records": [{"name": "己", "city": "SZ"}]},
          params={"entityCode": "p"})
cd = cm.get("data") or {}
print("commit 带越界值: applied=%s warnings=%s" % (cd.get("applied"), len(cd.get("warnings") or [])))
print("  message:", cd.get("message"))
