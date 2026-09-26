#!/usr/bin/env bash
# z-lc 部署演练：把 admin 胖 jar 装到 250 上，对着**真 MySQL 8** 起服务并自证真的连着它。
#
# 为什么要有这一支：dev profile 用 `jdbc:h2:mem:zlc`，进程一死库就没了 —— 于是
# "能在别的机器上、对着真的 MySQL 部署起来" 这件事从来没被实测过。第一次真跑就撞上
# 三个只有对着真 MySQL 8 部署才会撞的坑（细节见 deploy_250_remote.sh 头部），三处都成了硬闸，
# 而且各自带一个负控（闸只有"坏样本能红"才算存在）：
#   bash _e2e/deploy_250.sh gates      # 闸1 + 闸2 + 闸3 各咬一次，正向再绿一遍
#
# 用法（都在本机敲）：
#   bash _e2e/deploy_250.sh all        # sync → db → schema → env → start → verify
#   bash _e2e/deploy_250.sh sync       # 只传包 + 字节对账（81MB 传坏了会以"行为古怪"出现）
#   bash _e2e/deploy_250.sh verify     # 只跑闸 2 的写-读回自证
#   bash _e2e/deploy_250.sh collate      # 只问物理表实际校对（闸 3, 不打应用）
#   bash _e2e/deploy_250.sh repair       # 把闸 3 给的 CONVERT TO 真的执行掉并立刻复测那道闸
#                                        # （all 里**不**放这一条：动已有表要运维自己按一次）
#   bash _e2e/deploy_250.sh status|stop|gates|tunnel|api
#
# 凭证只在 250 的 ~/.config/z-lc-deploy/（0600），仓库里一个字都不留。
set -euo pipefail

HOST="${ZLC_DEPLOY_HOST:-250}"
DIR="${ZLC_DEPLOY_DIR:-~/zlc-deploy}"
APP_PORT="${ZLC_APP_PORT:-18090}"
LOCAL_TUNNEL="${ZLC_TUNNEL_PORT:-18099}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$REPO/z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar"
SCHEMA="$REPO/z-lc-admin/src/main/resources/db/schema-h2.sql"
REMOTE_SH="$REPO/_e2e/deploy_250_remote.sh"

# 远端只吃 stdin（不在远端落一个会被下次改动漏掉的副本）：变量、引号都只有一层。
# 要往远端塞变量用 ZLC_EXTRA_ENV='K=V K2=V2'（值里别带空格），参数走 step 后面。
rssh() {
  local step="$1"; shift
  ssh -o ConnectTimeout=8 -o BatchMode=yes "$HOST" \
    "ZLC_DIR=$DIR ZLC_APP_PORT=$APP_PORT ${ZLC_EXTRA_ENV:-} bash -s -- $step $*" < "$REMOTE_SH"
}

sync() {
  LOCAL_MD5=$(md5 -q "$JAR" 2>/dev/null || md5sum "$JAR" | cut -d' ' -f1)
  ssh -o ConnectTimeout=8 "$HOST" "mkdir -p $DIR/lib $DIR/logs"
  scp -q "$JAR" "$HOST:$DIR/lib/"
  scp -q "$SCHEMA" "$HOST:$DIR/schema-h2.sql"
  REMOTE_MD5=$(ssh -o ConnectTimeout=8 "$HOST" "md5sum $DIR/lib/$(basename "$JAR") | cut -d' ' -f1")
  [ "$LOCAL_MD5" = "$REMOTE_MD5" ] || { echo "!! jar 字节不一致 $LOCAL_MD5 != $REMOTE_MD5"; exit 4; }
  echo "    jar 字节一致：$LOCAL_MD5（$(basename "$JAR")）"
  REMOTE_MD5=$(ssh -o ConnectTimeout=8 "$HOST" "md5sum $DIR/schema-h2.sql | cut -d' ' -f1")
  LOCAL_MD5=$(md5 -q "$SCHEMA" 2>/dev/null || md5sum "$SCHEMA" | cut -d' ' -f1)
  [ "$LOCAL_MD5" = "$REMOTE_MD5" ] || { echo "!! schema 字节不一致"; exit 4; }
  echo "    schema 字节一致：$LOCAL_MD5"
}

tunnel() {
  # 端口在监听只说明有个 ssh 占着那个洞，不说明洞对面是 z-lc —— 复用也一样要自证，
  # 否则对面换了进程（我这台机上就撞过：隧道还在，后端已经被换成半坏的旧进程）。
  if ! lsof -nP -iTCP:"$LOCAL_TUNNEL" -sTCP:LISTEN >/dev/null 2>&1; then
    ssh -f -N -o ExitOnForwardFailure=yes -o ConnectTimeout=8 \
      -L "$LOCAL_TUNNEL:127.0.0.1:$APP_PORT" "$HOST"
    echo "    已建隧道 $LOCAL_TUNNEL → $HOST:$APP_PORT"
  else
    echo "    本机 $LOCAL_TUNNEL 已在监听，直接验它对的是不是 z-lc"
  fi
  local code body
  code=$(curl -s -m 8 -o /tmp/zlc_tunnel_probe.json -w '%{http_code}' \
    "http://localhost:$LOCAL_TUNNEL/api/lc/health" || echo "000")
  body=$(head -c 120 /tmp/zlc_tunnel_probe.json 2>/dev/null || true)
  rm -f /tmp/zlc_tunnel_probe.json
  echo "    GET /api/lc/health → $code $body"
  [ "$code" = "200" ] || { echo "!! 隧道对不上 z-lc（$code）—— 别写'连上了'"; exit 5; }
  case "$body" in *'"status":"UP"'*) ;; *) echo "!! 200 但不是 UP：$body"; exit 5 ;; esac
  echo "    隧道自证：http://localhost:$LOCAL_TUNNEL → $HOST:$APP_PORT，health UP"
}

case "${1:-all}" in
  sync)   sync ;;
  tunnel) tunnel ;;
  status) rssh status ;;
  stop)   rssh stop ;;
  db)     rssh db ;;
  schema) rssh schema "${2:-z_lc}" ;;
  env)    rssh env ;;
  start)  rssh start ;;
  verify) rssh verify ;;
  gates)  rssh gate1; rssh gate2; rssh gate3; rssh gate4 ;;
  # 单道闸也要能单独跑：改了一道的判据只重跑那一道（整批 gates 会把 app.env 换来换去并重启三次）。
  gate1)  rssh gate1 ;;
  gate2)  rssh gate2 ;;
  gate3)  rssh gate3 ;;
  gate4)  rssh gate4 ;;
  collate) rssh collate ;;
  repair)  rssh repair ;;
  api)    tunnel; echo "=== API 层门禁打远程 ==="; \
          python3 -u "$REPO/_e2e/e2e_api_test.py" "http://localhost:$LOCAL_TUNNEL" ;;
  all)    sync; rssh db; rssh schema; rssh env; rssh start; rssh verify; rssh collate; tunnel ;;
  *) echo "用法: $0 [all|sync|db|schema|env|start|verify|collate|repair|gates|gate1|gate2|gate3|gate4|api|tunnel|status|stop]"; exit 2 ;;
esac
