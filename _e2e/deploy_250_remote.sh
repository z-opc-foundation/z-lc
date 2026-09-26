#!/usr/bin/env bash
# z-lc 部署演练 —— 250 那一侧真正执行的脚本。
#
# 为什么拆成两个文件：原先所有远端命令都是本地双引号里套远端双引号再套 docker exec 的引号,
# 三级嵌套没法读也没法 bash -n。这一份是普通 bash 文件, 能单独检查, 由 _e2e/deploy_250.sh
# 用 `ssh 250 bash -s -- <step>` 喂进去执行。
#
# 凭证只在 ~/.config/z-lc-deploy/mysql.env (0600), 这个文件里一个字都不写。
#
# 三道硬闸（今天真撞上的三个坑, 都只有对着真 MySQL 8 部署才会撞）：
#   闸 1 require_app_env —— 配置文件的值必须 %q 写死再校验非空。含 `&` 的 JDBC url 不加引号
#        时, `source` 会在 `&` 处把命令切到后台, 变量**静默变成空串**、不报错；带着空 url 起
#        服务 /health 照样 UP, 于是"部署成功"是假的（实测见日志里的 7.1 GB NPE）。
#   闸 2 step_verify —— "health UP" 不算数：先要把这份 UP **归因**到本次启动的那个 pid
#        (app.pid vs 端口上的监听者), 再要求 API 写进去 + 让 MySQL 自己承认那一行在
#        （z_lc_app 与 z_lc_event 两张表分别核对主池和 LC 模块池）。归因这一半是缺陷 #55 补的：
#        旧进程没让出端口时, 新 JVM 以 "already in use" 自杀, 而 health 照样 UP —— 答的是上一版
#        构件。不认这一点, 抬版本的那一次部署可以"全绿"地什么都没换上去（250 实测 15:49）。
#   闸 3 step_collate —— 运行时表和元数据层必须同一套校对：dev 的 H2 没有校对这一层, 只有对着
#        真 MySQL 8 才看得见"表建成了但读不出来"（缺陷 #51, 详见函数上方注释）。
#        step_repair 是它的兑现面: 闸 3 只**给** ALTER（动已有表要运维决定）, repair 把那句真的
#        执行掉并立刻用同一把尺复测。
set -euo pipefail

DIR="${ZLC_DIR:-$HOME/zlc-deploy}"
CONF="${ZLC_CONF:-$HOME/.config/z-lc-deploy}"
DB_CONTAINER="${ZLC_DB_CONTAINER:-z-lc-deploy-mysql}"
DB_IMAGE="${ZLC_DB_IMAGE:-mysql:8.0.26}"
DB_PORT="${ZLC_DB_PORT:-33061}"
APP_PORT="${ZLC_APP_PORT:-18090}"
JAR="$DIR/lib/z-lc-admin-1.0.0-SNAPSHOT.jar"
LOG="$DIR/logs/boot-mysql.log"
TABLES_MIN=14

die() { echo "!! $*" >&2; exit "${2:-1}"; }
root_pw() { . "$CONF/mysql.env"; echo "$MYSQL_ROOT_PASSWORD"; }

require_app_env() {
  [ -f "$CONF/app.env" ] || die "$CONF/app.env 不存在 —— 先跑 env"
  # shellcheck disable=SC1090
  set -a; . "$CONF/app.env"; set +a
  [ -n "${SPRING_DATASOURCE_URL:-}" ] \
    || die "闸1: SPRING_DATASOURCE_URL source 之后是空的 —— 配置文件里的值没加引号"
  [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ] \
    || die "闸1: SPRING_DATASOURCE_PASSWORD source 之后是空的"
  case "$SPRING_DATASOURCE_URL" in
    jdbc:mysql://*|jdbc:h2:*) ;;
    *) die "闸1: url 不像 JDBC 串（前 24 字符：${SPRING_DATASOURCE_URL:0:24}）—— 若是 HIDE_IN_REPO 则环境变量没设上" ;;
  esac
}

# 端口上真正的监听者 pid（问不到回空串）。用 ss 而不是 lsof：250 上没装 lsof。
# 有了它才能把"health UP"归因到**某一次启动** —— 否则旧进程还在服务时, 部署脚本会把上一版
# 构件的应答报成自己刚部署的那个（缺陷 #55 的实测形状）。
port_pid() {
  ss -ltnp 2>/dev/null \
    | awk -v p=":$APP_PORT" '$4 ~ (p "$") {print; exit}' \
    | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' || true
}
port_busy() { [ -n "$(port_pid)" ]; }

# health_aggregate_up —— 只看**聚合**判定，两半落在同一次读取里。
# 缺陷 #52 之前 data.status 是 HealthController 里写死的一句 "UP"，`grep '"status":"UP"'` 恰好
# 与它同义；现在 data 是 {status:<逐池真探的聚合>, sources:[{status:UP},…]}，每一池各带一个
# "status":"UP"，于是那句裸 grep 从「总体好」悄悄退化成「至少一个池好」——主池接得上、模块池
# Connection refused 的那一版部署（正是 #52 的现场）照样过闸。所以判"含 UP 且不含 DOWN"。
health_aggregate_up() {
  local body
  body=$(curl -s -m 3 "http://127.0.0.1:$APP_PORT/api/lc/health" || true)
  case "$body" in
    *'"status":"DOWN"'*) return 1 ;;
    *'"status":"UP"'*) return 0 ;;
    *) return 1 ;;
  esac
}

health_body() {
  curl -s -m 3 "http://127.0.0.1:$APP_PORT/api/lc/health" || true
}

count_sql() {
  # 只回一个整数；拿不到整数就当场 fatal（尺子读空会报"0 张表/0 行", 那是假绿）
  local out
  out=$(docker exec "$DB_CONTAINER" mysql -uroot -p"$(root_pw)" -N -B -e "$1" </dev/null 2>&1 \
        | grep -v 'Using a password' | tr -dc '0-9')
  [ -n "$out" ] || die "读数不是整数：$1 → '$out'"
  echo "$out"
}

step_db() {
  [ -f "$CONF/mysql.env" ] || die "$CONF/mysql.env 不存在"
  # shellcheck disable=SC1090
  . "$CONF/mysql.env"
  if ! docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    docker run -d --name "$DB_CONTAINER" -p "127.0.0.1:$DB_PORT:3306" \
      -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" -e MYSQL_DATABASE=z_lc \
      -e MYSQL_USER=zlc -e MYSQL_PASSWORD="$LC_DB_PASSWORD" \
      "$DB_IMAGE" \
      --character-set-server=utf8mb4 \
      --collation-server=utf8mb4_general_ci \
      --default-authentication-plugin=mysql_native_password >/dev/null \
      || die "docker run 失败（mysqld 服务端参数必须写在镜像名之后）"
  fi
  for _ in $(seq 1 90); do
    docker exec "$DB_CONTAINER" mysqladmin -uroot -p"$MYSQL_ROOT_PASSWORD" ping >/dev/null 2>&1 && {
      echo "    mysql 就绪（$DB_CONTAINER / 127.0.0.1:$DB_PORT）"; return 0; }
    sleep 2
  done
  die "mysql 90*2s 内没就绪"
}

step_schema() {
  local db="${1:-z_lc}"
  [ -f "$DIR/schema-h2.sql" ] || die "$DIR/schema-h2.sql 不存在"
  if [ "$db" != "z_lc" ]; then
    docker exec "$DB_CONTAINER" mysql -uroot -p"$(root_pw)" </dev/null \
      -e "CREATE DATABASE IF NOT EXISTS \`$db\` CHARACTER SET utf8mb4;
          GRANT ALL PRIVILEGES ON \`$db\`.* TO 'zlc'@'%'" 2>&1 \
      | grep -v 'Using a password' || true
  fi
  docker exec -i "$DB_CONTAINER" mysql -uroot -p"$(root_pw)" \
    --default-character-set=utf8mb4 "$db" < "$DIR/schema-h2.sql" 2>&1 \
    | grep -v 'Using a password' || true
  local n
  n=$(count_sql "select count(*) from information_schema.tables where table_schema='$db'")
  echo "    库 $db 里的表数：$n"
  [ "$n" -ge "$TABLES_MIN" ] || die "表不足 $TABLES_MIN 张 —— schema 没装全"
}

step_env() {
  [ -f "$CONF/mysql.env" ] || die "$CONF/mysql.env 不存在"
  # shellcheck disable=SC1090
  . "$CONF/mysql.env"
  umask 077
  local url="jdbc:mysql://127.0.0.1:$DB_PORT/z_lc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
  { printf 'SPRING_DATASOURCE_URL=%q\n' "$url"
    printf 'SPRING_DATASOURCE_USERNAME=%q\n' zlc
    printf 'SPRING_DATASOURCE_PASSWORD=%q\n' "$LC_DB_PASSWORD"; } > "$CONF/app.env"
  echo "    已写 $CONF/app.env（值一律 %q）"
  require_app_env
}

step_start() {
  require_app_env
  # LC 模块池 (z.base.db.lc.*) 单独传：z-boot 的 ModuleDataSourceTemplate 只读 host/port
  # 并硬编码 jdbc:mysql://, 所以显式给 jdbc-url。ZLC_LC_URL 是给负控注入用的（把 LC 池
  # 指向另一个库, 看闸 2 认不认得出来）。
  local lc_url="${ZLC_LC_URL:-$SPRING_DATASOURCE_URL}"
  [ -f "$JAR" ] || die "$JAR 不存在 —— 先跑 sync"
  mkdir -p "$DIR/logs"
  local pidfile="$DIR/app.pid" now
  # 停旧进程之后要等端口**真的**让出来。老 JVM 收 Druid/Tomcat 得好几秒, 只 sleep 3 就启动的话
  # 新进程会 "Port 18090 was already in use" 当场自杀, 而 health UP 由那个还没死透的旧进程给出
  # —— "部署自证通过"于是报的是上一版构件。250 上实测：抬 #54 那一次 all 就是这么死的。
  pkill -f 'java -jar .*z-lc-admin-1\.0\.0' 2>/dev/null || true
  local waited=0
  while port_busy && [ "$waited" -lt 60 ]; do sleep 1; waited=$((waited + 1)); done
  if port_busy; then
    now=$(port_pid)
    die "端口 $APP_PORT 60s 内没让出来（现在占着的是 pid=$now, 我的 pkill 匹配不到它）—— 不停掉它就没法判在跑哪一版"
  fi
  echo "    旧进程已收干净（等了 ${waited}s, 端口 $APP_PORT 空）"
  nohup /usr/bin/java -jar "$JAR" \
    --server.port="$APP_PORT" \
    --spring.datasource.url="$SPRING_DATASOURCE_URL" \
    --spring.datasource.username="$SPRING_DATASOURCE_USERNAME" \
    --spring.datasource.password="$SPRING_DATASOURCE_PASSWORD" \
    --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
    --z.base.db.lc.jdbc-url="$lc_url" \
    --z.base.db.lc.username="$SPRING_DATASOURCE_USERNAME" \
    --z.base.db.lc.password="$SPRING_DATASOURCE_PASSWORD" \
    --z.base.db.lc.driver-class-name=com.mysql.cj.jdbc.Driver \
    --z-lc.adapter.ctc.base-url="http://localhost:$APP_PORT" \
    --z-lc.adapter.meta.base-url="http://localhost:$APP_PORT" \
    --z-lc.adapter.script.base-url="http://localhost:$APP_PORT" \
    > "$LOG" 2>&1 &
  local pid=$!
  echo "$pid" > "$pidfile"
  echo "    pid=$pid  log=$LOG（pid 记进 $pidfile —— 下面和闸 2 都按它归因）"
  # 盯到自己起来为止：Spring Boot 起不来**不会**让这条 ssh 非零退出, 所以"进程还在"不等于
  # "应用起来了", 而 health UP 更不等于"UP 的是我这个 pid"。
  local i=0 have
  while [ "$i" -lt 90 ]; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "      --- 日志尾 ---" >&2
      tail -6 "$LOG" | sed 's/^/      /' >&2
      die "我起的 pid=$pid 已经退出（上面是日志尾）—— 这不是部署成功"
    fi
    if grep -q 'APPLICATION FAILED TO START\|already in use' "$LOG"; then
      grep -m1 -A4 'APPLICATION FAILED TO START\|already in use' "$LOG" | sed 's/^/      /' >&2
      die "pid=$pid 还活着但应用没起来（端口或上下文冲突, 完整原因在 $LOG）"
    fi
    have=$(port_pid)
    if [ "$have" = "$pid" ] && health_aggregate_up; then
      echo "  ✓ 端口 $APP_PORT 上的监听者正是这次起的 pid=$pid, health UP"
      return 0
    fi
    sleep 2; i=$((i + 1))
  done
  have=$(port_pid)
  die "180s 内端口 $APP_PORT 没被 pid=$pid 占住（现在占着的是 ${have:-none}）—— 不能按'起来了'往下走"
}

step_verify() {
  local db="${ZLC_VERIFY_DB:-z_lc}"
  local probe="deploy_probe_$(date +%N | tail -c 7)"

  local up=""
  for _ in $(seq 1 45); do
    health_aggregate_up && { up=1; break; }
    sleep 2
  done
  [ -n "$up" ] || { echo "--- 日志尾 20 行 ---"; tail -20 "$LOG" || true; die "45*2s 内 /api/lc/health 没回 UP"; }
  # 缺陷 #52 之后 health 的 UP 是**逐池真探**的结果，所以这一道闸第一次能问出"这个进程到底
  # 接没接上 250 上那台 MySQL 8"。修复前这里是问不出的：health 硬编码 UP，同一进程第一条业务
  # 查询回 500 Connection refused（实测 Case A）。既然现在问得出，就必须问 —— 否则部署自证
  # 只到"有个进程在应答"，接不上库的进程也算部署成功。
  local hb
  hb=$(health_body)
  printf '%s' "$hb" | head -c 900 | sed 's/^/      health: /'; echo
  printf '%s' "$hb" | grep -q '"name":"dataSourceLc"' \
    || die "health 的 sources 里没有 dataSourceLc —— 模块池压根没被探到，这份 UP 不能算部署自证"
  printf '%s' "$hb" | grep -q '"database":"MySQL 8' \
    || die "health 探出的库不是 MySQL 8（真实响应: $(printf '%s' "$hb" | head -c 300)）"
  # health UP 只说明"有个进程在应答", 不说明应答的是**这次部署的那个构件**。先归因再往下：
  # 250 上实测过一次 all —— 旧进程没让出端口, 新进程 "already in use" 自杀, health 却照样 UP
  # (那是上一版构件答的), 再晚一步这个函数就会把它写成"部署自证通过"。
  local want have
  want=$(cat "$DIR/app.pid" 2>/dev/null || true)
  [ -n "$want" ] \
    || die "闸2: 没有 $DIR/app.pid —— 先跑 start, 否则这份 health UP 归不到任何一次部署"
  have=$(port_pid)
  [ "$have" = "$want" ] \
    || die "闸2: 端口 $APP_PORT 上是 pid=${have:-none}, 而 app.pid 记的是 $want —— 应答的不是这次部署的构件（旧进程还在服务）"
  echo "    health UP，且端口上的监听者就是 app.pid 记的 pid=$want（下面才是真判据）"

  local created
  created=$(curl -s -m 20 -X POST "http://127.0.0.1:$APP_PORT/api/lc/app/create" \
    -H 'Content-Type: application/json' \
    -d "{\"tenantCode\":\"default\",\"appCode\":\"$probe\",\"appName\":\"deploy gate\",\"icon\":\"appstore\"}")
  echo "$created" | grep -q '"success":true' \
    || die "API 写入就失败：$(echo "$created" | tr -d '\n' | head -c 200)"
  echo "    API 写入 ok（appCode=$probe）"

  curl -s -m 10 "http://127.0.0.1:$APP_PORT/api/lc/app/detail?appCode=$probe" \
    | grep -q "$probe" || die "API 读不回刚写的那一条"
  echo "    API 读回 ok"

  local rows
  rows=$(count_sql "select count(*) from \`$db\`.z_lc_app where app_code='$probe'")
  echo "    MySQL $db.z_lc_app 里这一条：rows=$rows"
  [ "$rows" -ge 1 ] || die "闸2: API 说写成功了, 但 MySQL $db 里没有这行 —— 进程连的根本不是这个库"

  local events
  events=$(count_sql "select count(*) from \`$db\`.z_lc_event where app_code='$probe'")
  echo "    MySQL $db.z_lc_event 里这一条：rows=$events"
  [ "$events" -ge 1 ] || die "闸2: $db.z_lc_event 没有 $probe 的事件 —— LC 模块池没落进这个库"

  [ -f "$LOG" ] || die "$LOG 不在 —— 没日志就没法判有没有建连风暴"
  local spew
  spew=$(grep -c 'create connection RuntimeException' "$LOG" || true)
  spew=${spew:-0}
  [ "$spew" = "0" ] || die "日志里有 $spew 条建连失败 —— 有一个池连不上, 这不是部署成功"
  echo "  ✓ 部署自证通过：API 写得进、MySQL 认账（z_lc_app 走主池视图、z_lc_event 走 LC 池）"
}

step_stop() {
  pkill -f 'java -jar .*z-lc-admin-1\.0\.0' 2>/dev/null || true
  # 归因凭证跟着这一次部署一起收掉：留着它, 下一次 verify 会把一个不是我起的监听者认成
  # "这次部署的那个"。
  rm -f "$DIR/app.pid"
  local waited=0
  while port_busy && [ "$waited" -lt 60 ]; do sleep 1; waited=$((waited + 1)); done
  if port_busy; then
    die "端口 $APP_PORT 还被 pid=$(port_pid) 占着（等了 ${waited}s）—— 不说'已停'"
  fi
  echo "    app 已停：端口 $APP_PORT 空了（等了 ${waited}s；MySQL 容器保留, 删数据要显式 docker rm -f $DB_CONTAINER）"
}

# ---- 两道闸各自的负控：闸只有"坏样本能红"才算存在 ----
# 只跑正向 = 永远不知道它是不是空跑（这是这一路踩过的坑）。

# 闸 1 负控：把 app.env 改成不加引号写（真出过事的写法）, require_app_env 必须当场拒绝。
# 正向对照就是 step_env 自己（同一道校验, 值 %q 写 → 必须过）。
step_gate1() {
  local rc=0 out
  # 前置（必须先证）：起点要是干净的。今天实测踩过两回——
  #   ① 上一次负控中途 die 退出脚本, 把 app.env 留在坏的形态; 这一轮"备份→注入→还原"
  #      还原的就是那份坏备份, 于是"闸咬住了"咬的是上一轮的残留。所以恢复改用 step_env 重新生成。
  #   ② require_app_env 里 `set -a; . app.env` 把好值漏进脚本自己的环境; 而
  #      `A=x&cmd` 这种坏行在子 shell 里只是"这一句没赋值", 变量沿用父进程那个好值 ——
  #      注入后校验照样绿, 看着像"闸是空的"。所以两个分支都从不带这三个变量的起点跑同一个闸。
  good_url_check() { ( unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
                       require_app_env ); }
  good_url_check || die "闸1 负控的起点就不干净：app.env 现在就过不了校验 —— 先跑 env"
  # shellcheck disable=SC1090
  . "$CONF/mysql.env"
  umask 077
  { printf 'SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:%s/z_lc?useUnicode=true&characterEncoding=utf8\n' "$DB_PORT"
    printf 'SPRING_DATASOURCE_USERNAME=%q\n' zlc
    printf 'SPRING_DATASOURCE_PASSWORD=%q\n' "$LC_DB_PASSWORD"; } > "$CONF/app.env"
  good_url_check || rc=$?
  out=$( good_url_check 2>&1 ) || rc=$?
  echo "    [注入后校验说] $(echo "$out" | tail -1)"
  step_env >/dev/null
  if [ "$rc" = "0" ]; then
    die "闸1 负控失败：不加引号的 url 照样过了校验 —— 这道闸是空的"
  fi
  echo "$out" | grep -q "闸1" \
    || die "闸1 负控是红了，但红不是这道闸报的（退出码 $rc）—— 上面那行是它实际报的什么"
  echo "  ✓ 闸 1 咬得住：同一份校验，%q 写的过、不加引号写的拒（具名报闸1，退出码 $rc）；已重新生成 app.env 并复检"
}

# 闸 2 负控：让 LC 池连到另一个库（表齐全, 所以应用自己一切正常）, 但自证按 z_lc 读回 ——
# 必须红。三段式：正向绿 → 注入红 → 恢复绿，少一段这口红就不可归因。
step_gate2() {
  local mis=z_lc_misdeploy
  echo "    [起点] 两个池都指 z_lc, 先证正向是绿的"
  step_start
  step_verify
  step_schema "$mis"
  echo "    [负控] 把 LC 模块池指向 $mis（主池仍在 z_lc）"
  ZLC_LC_URL="jdbc:mysql://127.0.0.1:$DB_PORT/$mis?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" \
    step_start
  local rc=0 out
  out=$( ( ZLC_VERIFY_DB=z_lc step_verify ) 2>&1 ) || rc=$?
  echo "$out" | tail -6
  if [ "$rc" = "0" ]; then
    die "闸2 负控失败：LC 池写到别的库, 自证仍说通过 —— 这道闸是空的"
  fi
  # 只认具名判红：红在别处（比如权限不够、写就失败了）不能算"读回判据咬住了"
  echo "$out" | grep -q "闸2" \
    || die "闸2 负控是红了，但红不在读回判据上（退出码 $rc）—— 上面那段就是它实际红在哪"
  echo "  ✓ 闸 2 咬得住：API 写得进、读回也认账，但行落在 $mis 而不是 z_lc → 读回判据具名报错（退出码 $rc）"
  echo "    [恢复] 两个池都指回 z_lc, 正向必须绿"
  step_start
  step_verify
  # 负控库是这一支自己造的, 用完就收掉（留着会变成下一个人眼里的"z-lc 好像有两个库"）
  docker exec "$DB_CONTAINER" mysql -uroot -p"$(root_pw)" \
    -e "DROP DATABASE IF EXISTS \`$mis\`; REVOKE ALL PRIVILEGES ON \`$mis\`.* FROM 'zlc'@'%'" 2>&1 | grep -v 'Using a password' || true
  echo "    已回收负控库 $mis"
}

# ---- 闸 3：运行时表和元数据层必须用同一套校对（缺陷 #51） ----
# 250 上真 MySQL 8 实测：建表语句只写 `DEFAULT CHARSET=utf8mb4` 时，MySQL 8 取的是**该 charset 的
# 默认校对** utf8mb4_0900_ai_ci，而按 schema-h2.sql 建出来的元数据表沿用库默认 utf8mb4_general_ci
# （15 张 z_lc_* 全是后者，引擎自己建的 e2e_customer_404937 是前者）。差一个校对级别不会在 DDL 时
# 报错，它等的是第一个跨表字符串比较 —— 列表页 JOIN 字典取 label 当场 500
# `Illegal mix of collations ... for operation '='`（一轮 API 门禁 28 条红里 8 条是这个）。
#
# 判据只问 information_schema：那是这张表**实际是什么**，而不是应用自己说它建成了什么。
# 因此这一道闸同时是"部署的构件真带上了修复"的证据（mtime 新不等于内容新）。
DB_NAME="${ZLC_DB_NAME:-z_lc}"
REF_TABLE=z_lc_dict_item      # 参照就取元数据层自己的一栏字符串列
REF_COLUMN=item_code
COLLATE_PROBE=z_lc_collate_probe   # 闸 3 负控自己造的表, 用完就收

sql_out() {
  # `|| true` 是必需的：mysql 成功但零行时 grep -v 没有输出会回 1, 而这套脚本跑在
  # `set -euo pipefail` 下 —— 不加就会在"库里确实干干净净"这种最好的结果上中止。
  # 真正的失败由各判据自己兜（读不到参照/读数不是整数一律 fatal）。
  #
  # **不许用 `docker exec -i`**：这一支脚本是 `ssh 250 bash -s < deploy_250_remote.sh` 从 stdin
  # 喂进来的，`-i` 让子进程继续挂在同一根管道上，第一次调用就把后面还没执行的脚本吃掉。
  # 实测：三条查询的 heredoc 只印出第一条（16:3x 在本机排查闸 4 留脏时撞上）。SQL 走 `-e`，
  # stdin 一律 /dev/null；只有 step_schema 灌 SQL 文件那一处该留 `-i`（它自己重定向了 stdin）。
  docker exec "$DB_CONTAINER" mysql -uroot -p"$(root_pw)" -N -B -e "$1" </dev/null 2>&1 \
    | grep -v 'Using a password' || true
}

# 参照校对；读不到、或读回来的不是一个合法标识符就 fatal —— 闸 3 没有参照就不判，
# 而不是"当作没有不一致"。
meta_collation() {
  local want
  want=$(sql_out "select collation_name from information_schema.columns
        where table_schema='$DB_NAME' and table_name='$REF_TABLE' and column_name='$REF_COLUMN'" \
        | tr -d '[:space:]')
  [ -n "$want" ] || die "闸 3 没有参照：问不到 $DB_NAME.$REF_TABLE.$REF_COLUMN 的校对"
  [ "$want" = "${want//[^A-Za-z0-9_]/}" ] \
    || die "闸 3 的参照不是一个标识符形状：'$want' —— 不去拼 SQL"
  echo "$want"
}

step_collate() {
  local want offenders bad n
  want=$(meta_collation)
  echo "    参照（$DB_NAME.$REF_TABLE.$REF_COLUMN）= $want"
  offenders=$(sql_out "select concat(table_name, ' ', column_name, ' ', collation_name)
      from information_schema.columns
      where table_schema = '$DB_NAME' and collation_name is not null
        and collation_name <> '$want'
      order by table_name, column_name")
  offenders=$(printf '%s\n' "$offenders" | grep -v '^[[:space:]]*$' || true)
  [ -n "$offenders" ] || { echo "  ✓ 闸 3：库里每一栏字符串列都在 $want 上，跨表比较不会 mix"; return 0; }
  # 修法按**表**给（一个表里可能好几栏），去重后一人一句。
  bad=$(printf '%s\n' "$offenders" | awk '{print $1}' | sort -u \
        | sed "s#.*#ALTER TABLE \`&\` CONVERT TO CHARACTER SET utf8mb4 COLLATE $want;#")
  n=$(printf '%s\n' "$bad" | grep -c 'ALTER TABLE')
  printf '%s\n' "$offenders" | head -8 | sed 's/^/      /' >&2
  printf '%s\n' "$bad" | sed 's/^/      /'
  die "闸 3: $n 张表的校对不在元数据层那一套（$want）上 —— 上面按表给了可直接执行的 ALTER" "41"
}

# 闸 3 只**给**修法（缺陷 #51 的口径：引擎不替运维决定要不要动已有表）。这一条把那句 ALTER
# 真的执行掉，好让"部署"这一路能在一条命令里走完。名单与校对一律现问 information_schema，
# 不在这另数一遍 —— 否则"修完转绿"可能只是两处口径碰巧一致。
step_repair() {
  local want tables t out rc=0 n=0
  want=$(meta_collation)
  tables=$(sql_out "select distinct table_name from information_schema.columns
      where table_schema = '$DB_NAME' and collation_name is not null
        and collation_name <> '$want' order by table_name" \
    | grep -v '^[[:space:]]*$' || true)
  if [ -z "$tables" ]; then
    echo "  ✓ 没有要修的表：库里每一栏字符串列本来就在 $want 上"
    return 0
  fi
  for t in $tables; do
    # COLLATE 必须跟着钉死：CONVERT TO 只写 charset 的话，MySQL 8 会把列搬到该 charset 的
    # **默认**校对（= utf8mb4_0900_ai_ci），修法和病同形，一道闸都白跑。
    [ "$t" = "${t//[^A-Za-z0-9_]/}" ] \
      || die "表名 '$t' 不是标识符形状 —— 拒绝拼进 SQL"
    out=$(sql_out "ALTER TABLE \`$DB_NAME\`.\`$t\` CONVERT TO CHARACTER SET utf8mb4 COLLATE $want")
    case "$out" in
      *ERROR*) printf '%s\n' "$out" | sed 's/^/      /' >&2; rc=1 ;;
      *) n=$((n + 1)); echo "    ✓ $t -> $want" ;;
    esac
  done
  echo "    已修 $n 张"
  [ "$rc" = "0" ] || die "有表的 CONVERT TO 被库拒了（上面是原文）—— 不装作修完"
  # 修完立刻用同一把尺复测：这里不复用上面那份名单，因为"执行过"不等于"改到位"。
  step_collate
}

# 闸 3 负控：三段式（正向绿 → 注入红 → 恢复绿），少一段这口红就不可归因。
# 注入用的是**旧版代码自己会写出来的那句 DDL**（只给 charset 不给 collation）, 所以这一支
# 证的不是"我能造一张怪表", 而是"这道闸认得那个真实形状"。
step_gate3() {
  local rc=0 out bad
  # 起点/收尾这两次检查必须把 step_collate 关进子 shell: 它自己用的是 die（= exit）, 直接
  # `step_collate || die "起点不干净"` 会在 die 之前就整只脚本退出, 那句归因永远印不出来。
  # （实测: 上一轮负控中途判红、探针表没被 DROP 就留下脏, 这一轮只看到闸 3 的 41, 看不到"起点不干净"）
  ( step_collate ) || die "闸 3 负控的起点不干净：库里现在就有不一致的表 —— 先跑 repair 收掉它"
  # 探针表中途判红也要收掉：留着它, 下一轮的"起点不干净"会常红, 而且报的是"库里不一致"这种
  # 看着像别人的账的错。必须挂 **EXIT** 而不是 RETURN：die 走的是 exit，函数根本没"返回"过,
  # 而判红恰恰就是 die 那一条路 —— 实测 RETURN 版留脏（250 上 16:3x 捞到一张 e2e_g4_* 与 2 条探针应用）。
  trap 'sql_out "DROP TABLE IF EXISTS \`$DB_NAME\`.\`$COLLATE_PROBE\`" >/dev/null; true' EXIT
  sql_out "CREATE TABLE IF NOT EXISTS \`$DB_NAME\`.\`$COLLATE_PROBE\` (
             id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
             note VARCHAR(64)) DEFAULT CHARSET=utf8mb4" >/dev/null
  # information_schema.tables 那一层没有 collation_name（那是 columns 的列名）—— MySQL 8 里
  # 表级的那一列叫 table_collation。写成 collation_name 时这一句每次都打一行
  # `ERROR 1054 ... Unknown column`（sql_out 吞退出码, 所以只有红日志里看得见）—— 缺陷 #56。
  sql_out "select concat('    探针表实际校对: ', table_collation) from information_schema.tables
           where table_schema='$DB_NAME' and table_name='$COLLATE_PROBE'"
  out=$(step_collate 2>&1) || rc=$?
  if [ "$rc" = "0" ]; then
    die "闸 3 负控失败：一张 utf8mb4_0900_ai_ci 的表混在 general_ci 的元数据层里，闸照样说通过"
  fi
  printf '%s\n' "$out" | tail -6
  printf '%s\n' "$out" | grep -q "闸 3" \
    || die "闸 3 负控是红了，但红不在这道闸的判据上（退出码 $rc）—— 上面那段就是它实际红在哪"
  # 只认"探针在判红集合里"那一支：红在别处（比如参照问不到）不能算这道闸咬住了。
  printf '%s\n' "$out" | grep -q "$COLLATE_PROBE" \
    || die "闸 3 报了红，但点名里没有 $COLLATE_PROBE —— 咬的不是我注入的那一张"
  # 缩进不许参与判定：step_collate 把 ALTER 印成 `      ALTER TABLE ...`（六个空格），
  # 而这里要问的是"有没有一句指向探针表的可执行 ALTER"。用 `^ALTER` 就是一条结构上永不成立的
  # 判据 —— 实测它把这一支负控从第一天就没绿过（缺陷 #56）。
  bad=$(printf '%s\n' "$out" | grep -E '^[[:space:]]*ALTER TABLE' | grep "$COLLATE_PROBE" || true)
  [ -n "$bad" ] || die "闸 3 的红要带着**可执行且指向探针表**的 ALTER 语句"
  echo "    注入被点名，给出的修法：$bad"
  sql_out "DROP TABLE IF EXISTS \`$DB_NAME\`.\`$COLLATE_PROBE\`" >/dev/null
  ( step_collate ) || die "闸 3 负控收不回去：探针表已 DROP, 正向却仍然红"
  echo "  ✓ 闸 3 咬得住：同一套判据，探针表在时具名报错并给 ALTER、DROP 之后转绿"
}

# ---- 闸 4：java 侧那道校对闸在真 MySQL 8 上到底答不答得出（缺陷 #57）----
# 为什么这一道**只能**在 250 上跑：H2 里没有校对这个概念，metadataCharsetCollation() 问不到
# 就返回 null，provisionOne 里 `if (meta != null)` 那一整块被跳过 —— 于是那句查表级校对的 SQL
# 写对没有，java/vitest/接口/浏览器四层全绿也一个字都看不见（正是"单测层与接线层不是一回事"）。
# 250 实测：tableCollation() 问的是 information_schema.tables 的 collation_name（那是 columns
# 的列名，表级那一列叫 table_collation）→ 每次必抛 ERROR 1054 → 被 catch 成 null →
# collationRepairMessage 一律"不判"：一道看着在、实际从不咬的闸。
BAD_COLLATE=utf8mb4_0900_ai_ci    # utf8mb4 在 MySQL 8 的**默认**校对 = #51 那个真实形状
G4_APP=""; G4_TBL=""              # 探针留下的元数据与物理表，两个都要收（全局：RETURN trap 取值）

g4_post() {
  # 三次 provision / 两次建元数据走同一个出口，免得四处引号各自漂。
  curl -s -m 30 -X POST "http://127.0.0.1:$APP_PORT$1" \
    -H 'Content-Type: application/json' -H 'X-Tenant-Code: default' ${2:+-d "$2"}
}

g4_table_collation() {
  # 表级校对只在 table_collation 上有（写成 collation_name 会 1054，而 sql_out 吞退出码）。
  sql_out "select table_collation from information_schema.tables
           where table_schema='$DB_NAME' and table_name='$1'" | tr -d '[:space:]'
}

g4_clean_meta() {
  # 只按那一个一次性 appCode 删；表名是库里读回来的，仍然逐个过标识符校验再拼进 SQL。
  local t
  for t in $(sql_out "select distinct table_name from information_schema.columns
      where table_schema='$DB_NAME' and column_name='app_code'"); do
    [ "$t" = "${t//[^A-Za-z0-9_]/}" ] || continue
    sql_out "delete from \`$DB_NAME\`.\`$t\` where app_code='$1'" >/dev/null
  done
}

step_gate4() {
  local want have id out got msg
  want=$(meta_collation)
  # 归因先于判定（与闸 2 同一把尺）：这一闸测的是**这次部署的那个构件**里的 SQL，答错了人的构件
  # 就等于拿上一版的红绿给这一版记账。
  have=$(port_pid)
  [ -n "$have" ] && [ "$have" = "$(cat "$DIR/app.pid" 2>/dev/null || true)" ] \
    || die "闸4: 端口 $APP_PORT 上是 pid=${have:-none}，而 app.pid 记的是 $(cat "$DIR/app.pid" 2>/dev/null || echo '<无>') —— 先跑 start"
  G4_APP="deploy_g4_$(date +%N | tail -c 7)"
  G4_TBL="e2e_g4_$(date +%N | tail -c 7)"
  # 与闸 3 同一把尺：挂 EXIT 而不是 RETURN（die = exit，RETURN 那条路一次都不走，实测留过脏）。
  trap 'if [ -n "$G4_TBL" ]; then sql_out "DROP TABLE IF EXISTS \`$DB_NAME\`.\`$G4_TBL\`" >/dev/null; fi
        if [ -n "$G4_APP" ]; then g4_clean_meta "$G4_APP"; fi
        true' EXIT

  out=$(g4_post "/api/lc/app/create" \
    "{\"tenantCode\":\"default\",\"appCode\":\"$G4_APP\",\"appName\":\"闸4校对探针\",\"icon\":\"appstore\"}")
  printf '%s\n' "$out" | grep -q '"success":true' \
    || die "闸4: 探针应用就没建起来（app 侧那道 SQL 都没跑到）：$(printf '%s' "$out" | head -c 200)"
  out=$(g4_post "/api/lc/admin/app/entity/create?appCode=$G4_APP&tenantCode=default" \
    "{\"tenantCode\":\"default\",\"appCode\":\"$G4_APP\",\"entityCode\":\"g4_col\",\"entityName\":\"校对闸探针\",\"tableName\":\"$G4_TBL\",\"fields\":[{\"fieldCode\":\"note\",\"fieldName\":\"备注\",\"fieldType\":\"STRING\",\"fieldLength\":32,\"sortOrder\":1}]}")
  # id 不解析应答文本（`createEntity` 回来的 DTO 里第一个 "id" 是**字段**的 id，实测 provision
  # 拿到它只会报 "Entity not found: 117"）—— 实体的主键只认库：这一句同时也是"探针真落进 MySQL"的前提。
  id=$(sql_out "select id from \`$DB_NAME\`.z_lc_entity where table_name='$G4_TBL' and deleted = 0" \
    | tr -d '[:space:]')
  case "$id" in
    '' | *[!0-9]*) die "闸4: 库里问不到探针实体 $G4_TBL 的主键（读到的是 '$id'）—— 前置不成立，后面红绿都不算数" ;;
  esac
  echo "    探针实体落库：id=$id  表=$G4_TBL"

  # 正向：引擎自己钉了 COLLATE 的建表子句，建出来的表必须就在参照校对上。这一句是后面那口红
  # 可归因的前提 —— 表本来就不一致，"注入之后 FAILED"可能只是本来就该 FAILED。
  out=$(g4_post "/api/lc/admin/entity/provision?id=$id")
  printf '%s\n' "$out" | grep -q '"status":"CREATED"' \
    || die "闸4: 探针表没按定义建出来（正向前提不成立）：$(printf '%s' "$out" | head -c 240)"
  got=$(g4_table_collation "$G4_TBL")
  [ "$got" = "$want" ] \
    || die "闸4 正向不成立：#51 钉死 COLLATE 之后建出来的表校对是 $got，参照是 $want —— 先修这个"
  echo "    正向：新建的表就在 $want 上（引擎钉的那句 COLLATE 真落到了库里）"

  sql_out "ALTER TABLE \`$DB_NAME\`.\`$G4_TBL\` CONVERT TO CHARACTER SET utf8mb4 COLLATE $BAD_COLLATE" >/dev/null
  got=$(g4_table_collation "$G4_TBL")
  [ "$got" = "$BAD_COLLATE" ] \
    || die "闸4 的注入没进去（表级校对读到的是 $got，要的是 $BAD_COLLATE）—— 这种红绿都不可归因"
  # 应答只截 700 字看，但**不能** `curl | head -c`：head 提前关管道会给 curl SIGPIPE，而这套脚本
  # 跑在 pipefail 下 —— 一次正常的读取会以"命令失败"收场。先整份收下再切。
  msg=$(g4_post "/api/lc/admin/entity/provision?id=$id"); msg=${msg:0:700}
  printf '%s\n' "$msg" | grep -q '"status":"FAILED"' \
    || die "闸 4 咬不住：一栏都不缺、表却漂到 $BAD_COLLATE，provision 照样报表好 —— java 侧那道校对闸在真 MySQL 8 上从不咬（缺陷 #57，多半是那句 SQL 问不到）。应答原文：$msg"
  printf '%s\n' "$msg" | grep -q "CONVERT TO CHARACTER SET utf8mb4 COLLATE $want" \
    || die "闸 4 是红了，但没给出把这张表搬回 $want 的可执行修法：$msg"
  echo "    注入被点名，给的修法就在应答里（$BAD_COLLATE -> $want）"

  sql_out "ALTER TABLE \`$DB_NAME\`.\`$G4_TBL\` CONVERT TO CHARACTER SET utf8mb4 COLLATE $want" >/dev/null
  msg=$(g4_post "/api/lc/admin/entity/provision?id=$id"); msg=${msg:0:700}
  printf '%s\n' "$msg" | grep -q '"status":"EXISTS_INTACT"' \
    || die "闸 4 收不回去：表已经修回 $want，provision 却还报表有问题：$msg"
  if printf '%s\n' "$msg" | grep -q "$BAD_COLLATE"; then
    die "闸 4 恢复态的应答里还留着 $BAD_COLLATE —— 读的是旧状态还是别的表？：$msg"
  fi
  echo "  ✓ 闸 4 咬得住：同一支实体、同一把尺，漂到 $BAD_COLLATE 时 FAILED 并给 CONVERT TO，搬回 $want 后转绿"
}

# ---------------------------------------------------------------------------
# 缺陷 #52 的部署层自证：三档**真实启动**，钉住"配置形状坏 ⇒ 拒起；库暂时不可达 ⇒ 起得来但
# 如实报 DOWN"。为什么要在这一层再量一次：#52 的三种坏形状全是进程级行为（启动期受不受理、
# 探活超不超时、聚合算不算数），java 单测里我拿的是替身 Environment 和 Proxy DataSource，
# "端口上到底有没有起来一个会答话的进程"这个量在单测层结构上拿不到；dev 那套 H2 更是三支都
# 撞不着（Case A/B/C 全是真 MySQL 现场量出来的）。
#   D1  环境变量没设上 ⇒ 主池 url 落成 ${SPRING_DATASOURCE_URL:HIDE_IN_REPO} 的字面值（Case A
#       原样）。修复前实测：6.656s 起来、health 200 "status":"UP"、同进程第一条业务查询
#       http=500 Connection refused。现在必须**自己退出**，且端口不监听。
#   D2  两池都配好（dev H2）⇒ 起得来，sources 里两池各一条、dataSourceLc 在列、库产品看得见。
#   D3  模块池 url 形好而对面的端口没人听 ⇒ **照样起得来**（暂时连不上的库不该拖死进程），
#       而 health 聚合必须 DOWN 且 detail 点名 dataSourceLc —— D1/D3 这一左一右就是
#       "拒起"与"如实报"的分界线本身：只修一头（比如一律拒起）都能让另一头的场景变错。
# 三档一律起在自己的端口（现挑，见 hp_pick_port）+ 自己的 pid/log，绝不 pkill（那会打到 18090 那个实例）。
# 每一档带日志体积保险丝：Case C 实测过"驱动对不上 url ⇒ Druid 建连线程无退避死循环，
# 8 分钟刷 13 GB"，所以"日志没爆"本身就是这一族的一条断言，不只是防身。
HP_PORT="${ZLC_HP_PORT:-18095}"
HP_LOG_CAP=31457280   # 30 MB
HP_ALIVE=""

hp_pid() {
  ss -ltnp 2>/dev/null \
    | awk -v p=":$HP_PORT" '$4 ~ (p "$") {print; exit}' \
    | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' || true
}

# hp_busy <port> → 该端口上的监听者 pid（没有则空）
hp_busy() {
  ss -ltnp 2>/dev/null \
    | awk -v p=":$1" '$4 ~ (p "$") {print; exit}' \
    | sed -n 's/.*pid=\([0-9][0-9]*\).*/\1/p' || true
}

# 端口现挑，不许照抄"我记得没人用的那个号"：18095 实测在 09-26 18:23 起被 z-mcp-server
# (pid=6636) 占着，本轮 healthproof 一上来就被自己那道"不抢端口"的检查挡停 —— 检查是对的，
# 写死端口是我编的假设。显式给了 ZLC_HP_PORT 时尊重它（那是人要复现某一档）。
hp_pick_port() {
  local p
  if [ -n "${ZLC_HP_PORT:-}" ]; then
    echo "  healthproof 用端口 $HP_PORT（ZLC_HP_PORT 指定）"
    return 0
  fi
  for p in 18095 18096 18097 18098 18101 18102 18103 18104; do
    if [ -z "$(hp_busy "$p")" ]; then
      if [ "$p" != "$HP_PORT" ]; then
        echo "  端口 $HP_PORT 已被 pid=$(hp_busy "$HP_PORT") 占着，改用 $p"
      fi
      HP_PORT="$p"
      echo "  healthproof 用端口 $HP_PORT"
      return 0
    fi
  done
  die "18095~18104 全被占着 —— 这一族的三档没法跑，不许借用 18090（那是被验收的实例）"
}

hp_cleanup() {
  # 这里原先写成 `[ -n "$HP_ALIVE" ] && kill "$HP_ALIVE" 2>/dev/null`：D2/D3 分支已经自己 kill
  # 过、也把端口等空了，走到这一步那个 pid 早已退出，kill 回来就是 1 —— 脚本开着 set -e，整个
  # trap 就在这一句中中止，下面的 `return 0` 根本没跑到。而 **bash 在脚本自然结束时拿 EXIT trap
  # 的最后一条状态当退出码**（本机实测：`trap 'false' EXIT; echo hi` 的退出码是 1），于是三档
  # 全部 ✓ 的一轮以 rc=1 收场（09-26 18:5x 跑 gates 整批复现；单独跑 healthproof 又常是 0，
  # 取决于那个 pid 有没有被回收成僵尸 —— "结论对、退出码随机"的闸比直接红更坏）。
  if [ -n "$HP_ALIVE" ]; then
    kill "$HP_ALIVE" 2>/dev/null || true
    wait "$HP_ALIVE" 2>/dev/null || true
  fi
  # 等端口真让出来再走：留着这一档端口上的监听者会让下一次 healthproof 误判"别人占着端口"
  local i=0
  while [ -n "$(hp_pid)" ] && [ "$i" -lt 30 ]; do sleep 1; i=$((i + 1)); done
  return 0
}

hp_boot() {
  local log="$1"; shift
  local have
  have=$(hp_pid)
  [ -z "$have" ] || die "端口 $HP_PORT 已被 pid=$have 占着 —— 不跟别人的进程抢端口，先清干净再跑"
  : > "$log"
  # env -u：这一族的现场就是"部署时环境变量没设上"，所以 D1 必须真的把 SPRING_DATASOURCE_*
  # 从环境里拿掉，而不是"我以为它没设"。
  env -u SPRING_DATASOURCE_URL -u SPRING_DATASOURCE_USERNAME -u SPRING_DATASOURCE_PASSWORD \
    java -jar "$JAR" --server.port="$HP_PORT" "$@" > "$log" 2>&1 &
  # pid 走全局 HP_ALIVE，**不走 stdout**：调用方一律 `hp_boot … > /dev/null`，因为
  # `pid=$(hp_boot)` 会把整段扔进子 shell，那里的 HP_ALIVE 赋值回不到当前 shell（#58 那一族
  # "在子 shell 里改状态"的另一种现形），后面的 kill/hp_wait 就会拿着空 pid 去操作。
  HP_ALIVE=$!
}

# hp_wait <秒> → 打印 exited|blown|up|timeout 四种状态之一
hp_wait() {
  local max="$1" i=0 sz
  while [ "$i" -lt "$max" ]; do
    if ! kill -0 "$HP_ALIVE" 2>/dev/null; then echo exited; return; fi
    sz=$(stat -c %s "$HP_LOG_CUR" 2>/dev/null || echo 0)
    if [ "$sz" -gt "$HP_LOG_CAP" ]; then
      kill "$HP_ALIVE" 2>/dev/null
      echo "blown(${sz}B)"
      return
    fi
    if [ "$(hp_pid)" = "$HP_ALIVE" ]; then echo up; return; fi
    sleep 1; i=$((i + 1))
  done
  echo timeout
}

hp_health() {
  curl -s -m 8 "http://127.0.0.1:$HP_PORT/api/lc/health" || true
}

step_healthproof() {
  [ -f "$JAR" ] || die "$JAR 不存在 —— 先跑 sync"
  hp_pick_port
  trap hp_cleanup EXIT
  local tag state body log
  for tag in D1 D2 D3; do
    log="$DIR/logs/healthproof-$(echo "$tag" | tr 'A-Z' 'a-z').log"
    HP_LOG_CUR="$log"
    echo "  --- $tag ($log) ---"
    case "$tag" in
      D1) # 默认 profile + 环境里没有 SPRING_DATASOURCE_* ⇒ url 解析成 HIDE_IN_REPO
        hp_boot "$log" > /dev/null
        state=$(hp_wait 70)
        ;;
      D2) hp_boot "$log" --spring.profiles.active=dev > /dev/null
        state=$(hp_wait 90)
        ;;
      D3) hp_boot "$log" --spring.profiles.active=dev \
              --z.base.db.lc.jdbc-url='jdbc:mysql://127.0.0.1:1/dead?connectTimeout=1000' \
              --z.base.db.lc.driver-class-name=com.mysql.cj.jdbc.Driver \
              --z.base.db.lc.username=dead --z.base.db.lc.password=dead > /dev/null
        state=$(hp_wait 90)
        ;;
    esac
    echo "    状态: $state  pid=${HP_ALIVE:-none}"
    case "$tag" in
      D1)
        [ "$state" = "exited" ] || die "D1: 坏 url 下进程没有拒起（状态=$state）—— #52 的 fail-fast 没生效"
        grep -q 'z-lc 拒绝启动' "$log" \
          || die "D1: 进程退了，但日志里没有那句拒绝启动（那不是被闸拦的）：$(tail -5 "$log")"
        grep -Eq 'SPRING_DATASOURCE_URL|z\.base\.db\.lc\.jdbc-url' "$log" \
          || die "D1: 拒绝启动却没点名是哪个配置项 —— 运维照着改不了"
        [ -z "$(hp_pid)" ] || die "D1: 拒起了却还在 $HP_PORT 上监听"
        echo "    ✓ D1 拒起并点名配置项（修复前这一档是 200 UP + 第一条业务查询 500）"
        ;;
      D2)
        [ "$state" = "up" ] || die "D2: 两池都配好却没起来（状态=$state）：$(tail -8 "$log")"
        body=$(hp_health)
        printf '    health: %s\n' "$(printf '%s' "$body" | head -c 700)"
        if ! printf '%s' "$body" | grep -q '"status":"UP"'; then die "D2: 聚合不是 UP"; fi
        # 注意这些"含 X 就 die"的每一支都必须写成 if…then，不能写 `grep && die`：
        # 脚本开着 set -e，`cmd && die` 在 cmd 不成立的**好情况**下整条返回非零，
        # 会在绿的那一刻静默中止（#58 的 EXIT-vs-RETURN 是同一族的另一种死法）。
        if printf '%s' "$body" | grep -q '"status":"DOWN"'; then die "D2: 全配好了却有池 DOWN：$body"; fi
        if ! printf '%s' "$body" | grep -q '"name":"dataSourceLc"'; then die "D2: sources 里没有 dataSourceLc"; fi
        if ! printf '%s' "$body" | grep -q '"database":"H2'; then die "D2: 没探出库产品（database 不是 H2）"; fi
        if printf '%s' "$body" | grep -qi 'password'; then die "D2: health 应答里出现了 password"; fi
        echo "    ✓ D2 两池各被真探一次，sources 里点名 + 库产品 + latencyMs 都在"
        kill "$HP_ALIVE" 2>/dev/null || true
        while [ -n "$(hp_pid)" ]; do sleep 1; done
        ;;
      D3)
        [ "$state" = "up" ] || die "D3: 库不可达却把进程一起拖死了（状态=$state）—— 闸管的是配置形状，不是连通性: $(tail -8 "$log")"
        body=$(hp_health)
        printf '    health: %s\n' "$(printf '%s' "$body" | head -c 700)"
        if ! printf '%s' "$body" | grep -q '"status":"DOWN"'; then
          die "D3: 模块池连的是没人的端口，health 却整个 UP：$body"
        fi
        if ! printf '%s' "$body" | grep -q '"name":"dataSourceLc"'; then die "D3: DOWN 了却没点名是哪个池"; fi
        if ! printf '%s' "$body" | grep -q '"status":"UP"'; then
          die "D3: 主池明明是好的，也该如实报 UP（逐池可见才对）：$body"
        fi
        echo "    ✓ D3 起得来、聚合 DOWN、病句里点名叫出 dataSourceLc（修复前是 200 UP 骗过所有闸）"
        kill "$HP_ALIVE" 2>/dev/null || true
        while [ -n "$(hp_pid)" ]; do sleep 1; done
        ;;
    esac
  done
  echo "  healthproof: D1 拒起 / D2 真探两池 / D3 不可达如实报 DOWN —— 三档都在 $HP_PORT 上真起过进程"
}

step_status() {
  # 只认 `java -jar …z-lc-admin`：pgrep -af 的 -f 会把我自己这条含字面量的命令行也算进去。
  # awk 无匹配也回 0，所以"有没有 app"要看输出空不空，不能看退出码。
  local out
  out=$(ps -eo pid,etime,args | awk '/[j]ava -jar/ && /z-lc-admin/' \
        | sed 's/password=[^ ]*/password=<redacted>/')
  [ -n "$out" ] && echo "$out" || echo "    no app"
  # 归因两行一起看: "有 java 进程"和"端口上就是它"是两件事（缺陷 #55 就是这么骗过 verify 的）。
  echo "    app.pid=$(cat "$DIR/app.pid" 2>/dev/null || echo '<无>')  端口 $APP_PORT 上=$(port_pid || true)<无>" \
    | sed "s/\$/APP_PORT/$APP_PORT/"
  docker ps --format '{{.Names}} {{.Status}} {{.Ports}}' | grep "$DB_CONTAINER" || echo "    no container"
  ls -la "$DIR/logs" 2>/dev/null | tail -4 || true
}

case "${1:-status}" in
  db|schema|env|start|verify|stop|status|gate1|gate2|gate3|gate4|collate|repair|healthproof)
    step="step_$1"; shift; "$step" "$@" ;;
  *) echo "用法: bash -s -- [db|schema [库名]|env|start|verify|stop|status|gate1|gate2|gate3|gate4|collate|repair|healthproof]"; exit 2 ;;
esac
