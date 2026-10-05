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
  # 缺陷 #52 之后 health 的 data.status 是**逐池真探**的聚合，响应体里每个池各带一个
  # "status":"UP"。于是"body 里含 \"status\":\"UP\""从「总体好」退化成「至少一个池好」——
  # 一个池 DOWN、一个池 UP 也能过这道闸。要求"含 UP 且不含 DOWN"，两半落在同一次读取里。
  # head -c 也要放宽：sources 会让响应从 ~80 字节涨到几百字节，切太早会连总体判定都看不见。
  body=$(head -c 4000 /tmp/zlc_tunnel_probe.json 2>/dev/null || true)
  rm -f /tmp/zlc_tunnel_probe.json
  echo "    GET /api/lc/health → $code $body"
  [ "$code" = "200" ] || { echo "!! 隧道对不上 z-lc（$code）—— 别写'连上了'"; exit 5; }
  case "$body" in
    *'"status":"UP"'*'"status":"DOWN"'*|*'"status":"DOWN"'*)
      echo "!! 有数据源探活是 DOWN（隧道对上的这个进程接不上某个池），不算连上：$(echo "$body" | head -c 600)"
      exit 5 ;;
    *'"status":"UP"'*) ;;
    *) echo "!! 200 但不是 UP：$body"; exit 5 ;;
  esac
  echo "    隧道自证：http://localhost:$LOCAL_TUNNEL → $HOST:$APP_PORT，health UP"
}

# 缺陷 #82：接口层 [15w] 那 40 条读的是**本进程内存里**那本桩账（e2e_api_test.py 自己起的
# _WfStub），而 jar 在 250 上只会打它自己那一侧 app.env 里点名的地址。09-27 16:5x 第一次把
# `api` 打到 250：桩在本地 8888 绑上了（那条 check 是绿的），jar 一句都没打过来 ⇒ 40 条判
# "桥没通"。那 40 条不是产品红，是量具缺一条腿 —— 反向隧道把本机的桩搬到 250 那一侧去。
BRIDGE_PORT="${ZLC_BRIDGE_PORT:-18899}"   # 250 的 8888 是别人的 java（pid 1655），这里不抢
_api_pin_saved=""

bridge_up() {
  if ssh -o ConnectTimeout=8 -o BatchMode=yes "$HOST" "ss -lnt 2>/dev/null | grep -qE ':$BRIDGE_PORT[[:space:]]'"; then
    echo "!! 250 上 127.0.0.1:$BRIDGE_PORT 已经有人在听 —— 别把 jar 指到一个不是我的洞上"; exit 6
  fi
  ssh -f -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=15 -o ConnectTimeout=8 \
    -R "127.0.0.1:$BRIDGE_PORT:127.0.0.1:$BRIDGE_PORT" "$HOST"
  local i
  for i in 1 2 3 4 5; do
    if ssh -o ConnectTimeout=8 -o BatchMode=yes "$HOST" "ss -lnt 2>/dev/null | grep -qE ':$BRIDGE_PORT[[:space:]]'"; then
      echo "    反向隧道自证：$HOST:127.0.0.1:$BRIDGE_PORT 在听，洞的另一头是本机 127.0.0.1:$BRIDGE_PORT"
      return 0
    fi
    sleep 1
  done
  echo "!! 反向隧道没能在 250 那侧开出 127.0.0.1:$BRIDGE_PORT —— 别往下跑（jar 会打给一个不存在的洞）"; exit 6
}

bridge_down() {
  local pid args
  pid=$(pgrep -f "127.0.0.1:$BRIDGE_PORT:127.0.0.1:$BRIDGE_PORT" 2>/dev/null | head -1 || true)
  [ -n "${pid:-}" ] || { echo "    反向隧道进程不在了，无需收"; return 0; }
  args=$(ps -o args= -p "$pid" 2>/dev/null || true)
  case "$args" in
    ssh*-R*"$BRIDGE_PORT"*) kill "$pid" 2>/dev/null || true
      echo "    已收反向隧道 ssh（pid $pid）" ;;
    *) echo "!! pid $pid 的命令行不是这条反向隧道（$args）—— 不动它" ;;
  esac
}

api_teardown() {
  # 还原顺序不能反：先把 app.env 指回原来那格并重启，再拆洞 —— 反过来的话中间那几秒里
  # 线上实例的 wf 目标是一个已经没人听的洞，而这一格之后没人会去重启它。
  if [ -n "$_api_pin_saved" ]; then
    echo "    [还原] app.env 的 wf 目标指回：$_api_pin_saved"
    ZLC_EXTRA_ENV="$_api_pin_saved" rssh env && rssh start \
      || echo "!! 还原这一步没跑成 —— 线上那格的 wf 目标可能还指着洞"
    local want hole
    want=$(printf '%s' "$_api_pin_saved" | sed -n 's#^ZLC_WF_BASE_URL=.*:\([0-9]\{1,\}\)$#\1#p')
    hole=$(ssh -o ConnectTimeout=8 -o BatchMode=yes "$HOST" \
      "ss -lnt 2>/dev/null | grep -cE ':${want:-0}[[:space:]]'" || echo "?")
    echo "    [还原后] 250 上 :${want:-?} 的监听数 = $hole（0 ⇒ 那个桩没在跑，线上这格的 wf 现在是发不出去的，别说'恢复原状'）"
  fi
  bridge_down
  _api_pin_saved=""
}

api_with_bridge() {
  bridge_up
  trap 'api_teardown' EXIT
  _api_pin_saved=$(ssh -o ConnectTimeout=8 -o BatchMode=yes "$HOST" \
    "grep -m1 '^ZLC_WF_BASE_URL=' ~/.config/z-lc-deploy/app.env" || true)
  [ -n "$_api_pin_saved" ] \
    || { echo "!! 读不到 250 上 app.env 现有的 wf 目标 —— 没有退路就不做注入"; exit 6; }
  echo "=== 把部署件的 wf 目标指到反向隧道（原来是：$_api_pin_saved）==="
  ZLC_EXTRA_ENV="ZLC_WF_BASE_URL=http://127.0.0.1:$BRIDGE_PORT" rssh env && rssh start
  tunnel
  echo "=== API 层门禁打远程（桩 = 本机 :$BRIDGE_PORT，jar 经反向隧道打过来）==="
  LC_CAMUDA_STUB_PORT="$BRIDGE_PORT" python3 -u "$REPO/_e2e/e2e_api_test.py" "http://localhost:$LOCAL_TUNNEL"
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
  gates)  rssh gate1; rssh gate2; rssh gate3; rssh gate4; rssh healthproof ;;
  # 单道闸也要能单独跑：改了一道的判据只重跑那一道（整批 gates 会把 app.env 换来换去并重启三次）。
  gate1)  rssh gate1 ;;
  gate2)  rssh gate2 ;;
  gate3)  rssh gate3 ;;
  gate4)  rssh gate4 ;;
  # 闸 5（缺陷 #52）：三档真启动自证——坏 url 拒起 / 两池真探 / 库不可达如实报 DOWN。
  # 只在 18095 上起自己的进程，不碰 18090 那个线上实例。
  healthproof) rssh healthproof ;;
  # 闸 6（缺陷 #61 §2.6）：流程发起那本账在真 MySQL 8 上写不写得进、读不读得出。
  # 判据读的是库（information_schema + z_lc_workflow_fire），不是应用的转述；桩要先在 250 本机起来：
  #   scp _e2e/camuda_stub.py 250:~/zlc-deploy/ && ssh 250 'cd ~/zlc-deploy && nohup python3 camuda_stub.py \
  #     --port 18888 --hit-file ~/zlc-deploy/logs/wf_hits.jsonl --pid-file ~/zlc-deploy/camuda_stub.pid &'
  # 且 env/start 两步要带 ZLC_EXTRA_ENV='ZLC_WF_BASE_URL=http://localhost:18888'（闸 1 现在不给默认值）。
  fireprobe) rssh fireprobe ;;
  collate) rssh collate ;;
  repair)  rssh repair ;;
  # 缺陷 #82：`api` 现在默认接反向隧道。不带隧道那一跑（09-27 16:5x 实测）是 515/555，40 条红
  # **全部**落在 [15w] 那一节且逐条写着"桥没通"—— 那是量具的拓扑，不是产品的行为。
  # 要复刻旧那一跑（只打线上实例、不重启它）用 `api_plain`。
  api)    api_with_bridge ;;
  api_plain) tunnel; echo "=== API 层门禁打远程 ==="; \
          python3 -u "$REPO/_e2e/e2e_api_test.py" "http://localhost:$LOCAL_TUNNEL" ;;
  all)    sync; rssh db; rssh schema; rssh env; rssh start; rssh verify; rssh collate; tunnel ;;
  *) echo "用法: $0 [all|sync|db|schema|env|start|verify|collate|repair|gates|gate1|gate2|gate3|gate4|healthproof|fireprobe|api|api_plain|tunnel|status|stop]"; exit 2 ;;
esac
