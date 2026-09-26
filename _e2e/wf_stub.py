#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""z-wf 引擎的**桩**，给 250 那条部署腿用（`_e2e/deploy_250_remote.sh` 的 `stub` / `fire` 两步）。

为什么要有这一支：`z-lc.adapter.wf.base-url` 的属性默认值是 `http://localhost:8888`，而 250 上那一格
是**别人的**进程（09-27 04:1x 实测：`ss -ltnp` 显示 `*:8888` 由 pid 1622 的 `z-opc-main-starter` 持有，
对它 POST `/api/approval-center/processes/start` 回 404 + 一段 Tomcat HTML）。部署腿要么点名一个真引擎，
要么点名自己的桩；留默认值 = 每写一条低代码记录就往别人的在跑服务发一次 POST。

**桩的三种失败形状里有两种是从真 z-wf 抄来的**，不是编的：
  `reject`  200 + `success:false` + `流程启动失败: ...`
            = `z-wf/z-wf-web/.../ApprovalCenterController.java:672-673`
              `catch (Exception e) { return Result.fail("流程启动失败: " + e.getMessage()); }`
            ⇒ 这一支证的是**真引擎会给的结局**（Camunda 撞 business key 就走这条路）。
  `http5xx` 502 + 一个成功样的 body
            ≠ z-wf 这段代码能产出的形状（它任何结局都回 200 信封）。它证的是我们适配器的判据
            "看状态码还是看信封"，账要记清：这条不冒充引擎行为。
  `ok`      200 + `data.processInstanceId`，字段名同样取自 `ApprovalCenterController.java:664`。

只在 127.0.0.1 上听（不往局域网暴露一个能发起流程的端口），每条请求按 JSON 行落到 `--hit-file`，
`/__hits` 让调用方能拿"桩到底收到几句"这个数去对账。

⚠ 这里用的是**单线程** `HTTPServer`（3.6 上还没有 `ThreadingHTTPServer`，而 250 实测只有 python 3.6.9）
⇒ `hang` 模式会把后续请求排在 sleep 之后。250 这一腿不用 `hang`（"有界等待"那一格在部署件层
`_e2e/mutate_workflow_deployed_guard.py` 的 W5 已经钉过），留着它只是为了这个桩与本机那个 JS 桩同形。
"""
import argparse
import json
import os
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

STATE = {"mode": "ok", "hits": [], "delay": 7.0}
LOCK = threading.Lock()
START = "/api/approval-center/processes/start"
HIT_FILE = "/tmp/zlc_wf_stub_hits.jsonl"


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):  # 别把每条请求刷进部署日志
        pass

    def _send(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json;charset=UTF-8")
        self.send_header("Content-Length", str(len(body)))
        # 每条都掐连接：真引擎那边是 Tomcat，而低代码侧的连接池若把长连接攥到"桩换过一次模式"之后，
        # 复现超时那一类判据时会撞上一个自己造出来的 stale socket，那种红指不回任何结论。
        self.send_header("Connection", "close")
        # 少这一句，客户端只会等到超时（本机冒烟实测：curl 拿到 http=000，而桩那边"答过了"）。
        # 头没写完就送 body = 应答永远不完整，`send_response`/`send_header` 都不负责收尾。
        self.end_headers()
        self.close_connection = True
        self.wfile.write(body)

    def do_POST(self):
        raw = ""
        n = int(self.headers.get("Content-Length") or 0)
        if n:
            raw = self.rfile.read(n).decode("utf-8", "replace")
        with LOCK:
            STATE["hits"].append({"path": self.path, "body": raw, "at": time.time()})
            seq = len(STATE["hits"])
            mode = STATE["mode"]
            with open(HIT_FILE, "a") as f:
                f.write(json.dumps({"seq": seq, "path": self.path, "body": raw, "mode": mode},
                                   ensure_ascii=False) + "\n")
        try:
            biz = json.loads(raw or "{}").get("businessKey")
        except ValueError:
            biz = None
        if mode == "hang":
            time.sleep(STATE["delay"])
            self._send(200, {"success": True, "code": 200, "message": "ok",
                             "data": {"processInstanceId": "wf250-hang-%d" % seq}})
            return
        if mode == "reject":
            self._send(200, {"success": False, "code": 500,
                             "message": "流程启动失败: business key 已存在"})
            return
        if mode == "http5xx":
            # 一个"看着像成功"的 body 配 5xx：适配器不许把里面那个号当账收下。
            self._send(502, {"success": True, "code": 200, "message": "ok",
                             "data": {"processInstanceId": "ghost-should-not-be-kept"}})
            return
        self._send(200, {"success": True, "code": 200, "message": "ok",
                         "data": {"processInstanceId": "wf250-%d" % seq,
                                  "businessKey": biz, "message": "流程启动成功"}})

    def do_GET(self):
        if self.path.startswith("/__hits"):
            with LOCK:
                out = {"count": len(STATE["hits"]),
                       "last": STATE["hits"][-1] if STATE["hits"] else None,
                       "mode": STATE["mode"]}
            self._send(200, out)
            return
        if self.path.startswith("/__mode"):
            q = self.path.split("?", 1)
            if len(q) == 2:
                for kv in q[1].split("&"):
                    if kv.startswith("mode="):
                        m = kv.split("=", 1)[1].strip()
                        if m not in ("ok", "reject", "http5xx", "hang"):
                            self._send(400, {"success": False, "message": "unknown mode " + m})
                            return
                        with LOCK:
                            STATE["mode"] = m
            with LOCK:
                self._send(200, {"mode": STATE["mode"]})
            return
        self._send(404, {"success": False, "message": "only %s / __mode / __hits" % START})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=18888)
    ap.add_argument("--hit-file", default="/tmp/zlc_wf_stub_hits.jsonl")
    ap.add_argument("--pid-file", default="")
    args = ap.parse_args()
    global HIT_FILE
    HIT_FILE = args.hit_file
    if args.pid_file:
        with open(args.pid_file, "w") as f:
            f.write(str(os.getpid()))
    srv = HTTPServer(("127.0.0.1", args.port), Handler)
    print("wf_stub listening 127.0.0.1:%d mode=%s" % (args.port, STATE["mode"]), flush=True)
    srv.serve_forever()


if __name__ == "__main__":
    import os
    main()
