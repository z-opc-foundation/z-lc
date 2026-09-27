# z-lc 端到端验证 / 续跑清单

## 一、怎么把后端跑起来（零外部依赖）

不需要 MySQL、不需要 Docker。dev profile 直接吃 H2 内存库。

```bash
cd z-lc
mvn -B -DskipTests install -pl z-lc-admin -am
java -jar z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
# -> http://localhost:18090   健康检查: /api/lc/health   API 文档: /doc.html
```

三层测试，当前全是绿的：

```bash
mvn -o -B clean install          # 4677 个 Java 测试（surefire 模块汇总行现加 = 2701+525+1276+112+63；z-lc-web 那 63 个里 61 个是 LcHttpContractTest 的真 HTTP 集成测试）
python3 _e2e/e2e_api_test.py     # 464/464 项断言，打真在跑的 server
python3 _e2e/probe_stats.py      # 非数值统计与字典值域 warning 的即席探针（要 server 在跑）
cd z-lc-admin-ui && npm run check   # tsc + eslint --max-warnings 0 + vitest 28 文件/252 用例 + vite build（09-27 06:1x 实测产物 index-ruBdvAdn.js。⚠ 这个名**不是身份**：同一份 src 本机 rollup 会给出不同名，见下文"#69：产物名不能当还原判据"）
E2E_REPEATS=3 node e2e/browser-e2e.mjs  # 真浏览器门禁 279 项/轮（先 build，preview 见下文。⚠ 这一行原写 222，是 09-26 那窗的数；#61 §2.5 给流程绑定页补了断言，09-27 05:0x 实测 `~/.cache/zlc61/browser_guard/00_baseline.log` 逐字 `=> PASS 279 / FAIL 0`，两跑（第二轮基线 / 第三轮基线）都是 279）
bash _e2e/deploy_250.sh gates    # 部署层四道闸，各自带负控，都要能红（见「部署演练怎么跑」一节）
```

（以上是 2026-09-26 16:4x – 16:5x 这一窗**同轮**实跑的数，不是抄上一轮 —— 上一轮（08:3x – 08:4x）记的是 4656 / 464 / 248 / 184。
这一窗是把同一套东西**打到 250 上的真 MySQL 8 上**那一轮（部署演练），撞出 #51/#54/#55/#56/#57/#58 六件事，
其中 #57 改了 java、#58 改了部署量具，所以每一层都重测了一遍；日志与退出码逐层落在 `~/.cache/zlc57/gates/`：
`java57b.log`（`JAVA_EXIT=0`）、`api_h2_57b.log`（`API_EXIT=0`）、`check57b.log`（`CHECK_EXIT=0`）、
`browser57b.log`（`BROWSER_EXIT=0`，三轮各自 222/0）、`deploy_all57b.log`（`ALL_EXIT=0`）、
`gates57b.log`（`GATES_EXIT=0`，四道闸逐条具名负控红过又绿回来）、`api_mysql57b.log`（`API250_EXIT=0`）。
⚠ 这一窗浏览器那一格**第一次跑是拒跑的**（`~/.cache/zlc49/gates/browser55.log` 里逐字写着
`门禁拒绝开跑（测的必须是本轮构建的产物）：- 没从 http://localhost:5274/ 的 HTML 里读到 assets/index-*.js（preview 没起？）`）——
产物指纹守卫拦下"没人起 preview 就去打 5274"，报的是 2 而不是 0。这一族里"跑不起来"和"跑过了"从来是两个读数，别混着记。
⚠ 上一窗（08:3x – 08:4x）那条链一次跑绿，五个退出码没有第二个值；它的账仍在下文「交接状态」最前面第二格。）

⚠ **本窗浏览器那一格差点测的不是本窗的件**：`npm run preview:e2e` 起在 5274 时撞上"端口已被占用"，
它自己退到 5275（`preview48.log` 里写着 `Port 5274 is in use, trying another one...`），
而 `browser-e2e.mjs` 打的是 **5274** —— 占着 5274 的是 06:27:52 起的一支 vite preview（本仓同一路径，不是本窗这条链起的）。
这一格因此没有直接采信"退出码 0"，而是把三方对齐现量了一次：5274 返回的 HTML 里引用的产物 =
5275 返回的 = 盘上 `dist/index.html` 引用的 = `index-CQtLNKSU.js`，且 `dist/assets/` 那一份的 mtime 是 `08:33:41`
（= 本窗 `npm run check` 里 build 出来的那一个）。vite preview 每次请求现读磁盘，所以旧进程伺服的是**新件** ——
但这条结论只有量过才敢写：**"preview 还在跑"不等于"它在测旧件"，也不等于"它在测新件"**。
（收这条链时 5274/5275 都空了：脚本末尾按名字 `pgrep -f 'vite preview --port 5274'` + `kill -9`，把 06:27 那一支一起带走了 ——
这一族脚本按端口/进程名清理时**会连别人那一支的 preview 一起杀**，用之前先想清楚这一点。）

注入缺陷自证（"补的测试到底钉不钉得住"唯一的答案，见下文各节）。**40 支，后端 21 + 前端 19**
（这个数不是敲出来的，09-27 09:2x 现敲：`ls _e2e/mutate_*.py | wc -l` = 21、`ls z-lc-admin-ui/e2e/mutate_*.py | wc -l` = 19。
上一版（01:4x）记的 38 之后又长了两支：`z-lc-admin-ui/e2e/mutate_workflow_browser_guard.py`（#61 的浏览器层，18 支 W1–W18，
它的账在「缺陷 #69」那一节里 —— 那一节的量具就是它）与 `mutate_deployment_guard.py`（#70 部署中心，见下文「缺陷 #70」那一节））：

```bash
python3 _e2e/mutate_duplicate_guard.py            # 单测层：预检回到 deleted=0 口径
python3 _e2e/mutate_duplicate_guard_http.py       # IT 层：预检 / advice 分别注入
python3 _e2e/mutate_duplicate_guard_deployed.py   # 部署件层：A1..A4 + B，带 class 级指纹
python3 _e2e/mutate_connection_leak.py            # 逆向映射的连接归还
python3 _e2e/mutate_field_code_guard.py           # 后端字段编码闸：I1..I4（单测层，surefire 判定）
python3 _e2e/mutate_field_code_deployed_guard.py  # 同一道闸打到 fat jar：J1..J7 走真 HTTP + 同步核单测层
python3 _e2e/mutate_group_fields_guard.py         # 后端多维分组：7 个注入，分母钉在 AggregateTest 16 条
python3 _e2e/mutate_pipeline_wiring_guard.py      # 流水线接线：P1..P14 打 fat jar + 同步核 PipelineWriteChainTest
python3 _e2e/mutate_pipeline_config_guard.py      # 流水线阶段参数 #42 单测层：U1..U6（跑 PipelineStagesTest + PipelineConfigServiceTest + PipelineWriteChainTest = 63 条）
python3 _e2e/mutate_replay_guard.py               # 事件折叠删栏/删实体 #46 单测层：M1..M3（M3 在 HTTP 流程里是等价变异，只有这一层捉得住）
python3 _e2e/mutate_edit_path_deployed_guard.py   # #45/#46 打 fat jar：E1..E6，带字节级 marker 预检（预检不过 FATAL，不拿 stale jar 读数）
python3 _e2e/mutate_provision_reconcile_guard.py  # 定义与库对账 #47 java 层：C1..C7（补列/回读/幂等/只加不删）
python3 _e2e/mutate_provision_deployed_guard.py   # 同一件事打到发出去的 jar：D1..D13，含"读库的判据不会因为报告撒谎而红"这条归口
python3 _e2e/mutate_provision_contract_guard.py   # #47 的 java 契约层：K1..K8 打在 LcHttpContractTest 那两支上（8 支注入跑一整轮 8×mvn 只要 ~2 分钟，所以每轮跑**整个类**钉分母）
python3 _e2e/mutate_permission_service_guard.py    # 权限 #48 java 层：J1..J14（PermissionServiceTest + LcHttpContractTest，分母每轮钉 75 条，认领 25 条具名断言）
python3 _e2e/mutate_permission_deployed_guard.py   # #48 打发出去的 fat jar：D1..D14，认领 `[15t]` 那 30 支探针里的 24 支（另 6 支是 ok() 夹具，不进判红账，理由写在该支开头）
python3 _e2e/mutate_collation_guard.py             # 250 真库撞出的那一族 #51/#54/#57：M1..M13 + N1..N5（共 18 支，跑 SchemaAdminBizServiceTest + UndoServiceSnapshotFormatTest + LcHttpContractTest 那两支）
python3 _e2e/mutate_health_honesty_guard.py        # #52 的 java 层：H1..H17 + N1..N2（账见「健康探针」那一节）
python3 _e2e/mutate_workflow_trigger_guard.py      # #61 的 java 层：M1..M6 打 core 四类 + 契约层（分母每轮钉 68 + 13，认领 30 条具名断言）
python3 _e2e/mutate_workflow_deployed_guard.py     # #61 的**部署件层**：W1..W6 各重新 build fat jar、重启 18090 再跑 `[15w]`（分母每轮钉 63/546，09-27 09:1x 现读；这一节自身仍 63 条）
python3 _e2e/mutate_deployment_guard.py            # #70 部署中心：J1a/J1b/J2/J3（java 三层）+ F1..F3（vitest 层），每轮核分母必须 = core 19 / web 7 / ui 7（漂了直接抛），账见下文「缺陷 #70」那一节
cd z-lc-admin-ui && python3 e2e/mutate_provision_report_guard.py # #47 的 vitest 层 M1..M18（DesignerProvision.test.tsx 15 例）
cd z-lc-admin-ui && python3 e2e/mutate_provision_browser_guard.py # #47 的**浏览器层** P1..P6（自带 build + preview，11d 那 24 条）
cd z-lc-admin-ui && python3 e2e/mutate_permission_browser_guard.py # #49/#50 的**浏览器层** M1..M9（自带 build + preview，11e 那 39 条静态 check 的账是机器核的：认领 ∪ NOT_COVERED == 扫到的全集）
cd z-lc-admin-ui && python3 e2e/mutate_degradation_guards.py     # 元数据降级口径 M1..M5
cd z-lc-admin-ui && python3 e2e/mutate_admin_list_guards.py      # 管理页列表五态 A..H
cd z-lc-admin-ui && python3 e2e/mutate_workspace_entity_guards.py# workspace 侧出口 A1..D1
cd z-lc-admin-ui && python3 e2e/mutate_record_fetch_guards.py    # 记录侧三出口 12 个注入
cd z-lc-admin-ui && python3 e2e/mutate_designer_entity_guard.py  # 设计器侧栏 D1..D7
cd z-lc-admin-ui && python3 e2e/mutate_grid_refetch_guard.py     # 表格计数守卫（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_kanban_keyboard_guards.py # 看板焦点桥 M1..M4（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_column_order_guards.py    # 列设置抽屉顺序契约 I1..I3（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_designer_field_code.py    # 设计器字段编码闸 + 新建实体通路 F1..F9（按目录跑）
cd z-lc-admin-ui && python3 e2e/mutate_pivot_guards.py           # 交叉表单测层 P1..P10（按两个文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_pivot_browser_guard.py    # 交叉表**浏览器层** P8（自带 build + preview）
cd z-lc-admin-ui && python3 e2e/mutate_field_code_browser_guard.py  # 字段编码闸**浏览器层** B1..B5（自带 build + preview）
cd z-lc-admin-ui && python3 e2e/mutate_pipeline_wiring_guard.py  # 流水线词表/顺序/参数 F1..F13（按三个文件跑，含 java 参照集）
cd z-lc-admin-ui && python3 e2e/mutate_pipeline_browser_guard.py # 流水线 11a 那 23 条**浏览器层** S1..S9（自带 build + preview）
cd z-lc-admin-ui && python3 e2e/mutate_permission_matrix_guard.py # 权限矩阵页 B1..B12（vitest 层，分母钉 10 条，跑在两支新文件上）
cd z-lc-admin-ui && python3 e2e/mutate_workflow_browser_guard.py # #61 的**浏览器层** W1..W18（自带 build + preview，11w 那一段；分母由源码扫出来、不许重名，判据修过的账见「缺陷 #69」那一节）
```

⚠ 每一支都自己报 `ALL MUTANTS BEHAVED AS CLAIMED` 才算数；退出码 0 而没跑完一整轮不等于通过。
⚠ 只有**产品源码**里注入的缺陷才算反证；改测试让它变红是在测测试，两件事别混着报。
⚠ **两支不能同时在飞**：它们都就地改写源文件，A 的"按字节还原"会把 B 正在判定的那份源码换掉。
本轮实测踩到 —— 后台那支还没收线就前台再开一支，基线报出 1 条红
（`字段表里不该预置引擎自建列: expected 3 to be 0`），那是**另一支的注入形状**，不是产品坏了。
假红还能回头查，假绿更糟（期待的注入被对方悄悄还原）。锁现在是 **35 支共用** `e2e/_mutlock.py`
（**19 支前端全接**，后端接了 **16** 支：`mutate_group_fields_guard.py`、`mutate_field_code_deployed_guard.py`、
`mutate_pipeline_wiring_guard.py`、`mutate_pipeline_config_guard.py`、`mutate_replay_guard.py`（本窗补上：它改的
`SchemaAdminBizService.java` 正是 provision 那几支也在就地改写的文件）、`mutate_provision_reconcile_guard.py`、
`mutate_provision_deployed_guard.py`、`mutate_provision_contract_guard.py`、`mutate_edit_path_deployed_guard.py`，
`mutate_permission_service_guard.py` / `mutate_permission_deployed_guard.py`
（后者与前者的 D/J 编号虽不同战役，**改的是同一份 `PermissionService.java`**，不同锁就等于没有），
以及部署演练这一族新加的 `mutate_collation_guard.py`（它同样就地改 `SchemaAdminBizService.java` + `UndoService.java`），
#52 的 `mutate_health_honesty_guard.py`，和 #61 这一窗新加的 `mutate_workflow_trigger_guard.py`
（它就地改 `RuntimeCrudController.java` —— 那正是流水线、权限、provision 那几支也要动的同一个文件）
与 `mutate_workflow_deployed_guard.py`（改的是同一个 `RuntimeCrudController.java` + `WfAdapter.java` +
`WorkflowTriggerDispatcher.java` + `WorkflowTriggers.java`，而且它还要**重启 18090**，两支同时在飞一定互相抹），
#70 这一窗的 `mutate_deployment_guard.py`（就地改 `DeploymentServiceImpl.java` / `DeploymentController.java`，
而它的 F 族跑的是 vitest —— 与前端那 19 支抢的是同一个报告目录）与 #61 浏览器层的 `mutate_workflow_browser_guard.py`
（自带 build + preview，`PORT = 5274`，和另外五支浏览器量具抢的是同一个端口）
—— 它们和前端撞的是同一个 mvn/vitest 缓存与报告目录；
这个 19/16 是 `grep -l _mutlock z-lc-admin-ui/e2e/mutate_*.py | wc -l` = 19 与
`grep -l _mutlock _e2e/mutate_*.py | wc -l` = 16 数出来的（09-27 09:2x 现敲），不是点的）；
仍未接锁的 5 支后端脚本（三个 duplicate_guard + connection_leak + field_code）**还没接锁**，
它们两两之间同样会互相抹源码，同时开两支得自己盯着。锁拿不到直接 `exit 2` 并且
**一个源文件都不碰**（已实测这一条）。

⚠ **锁挡不住的那一类：备份目录被上一支脚本复用。** 本轮最贵的一课，写在这里而不是藏在某一节里：
`_e2e/` 下五支"改的是 src/main"的注入脚本，原本把还原参照写成固定名 ` bak/<name>.orig`，
且只在**不存在**时才快照当前源码。后果是实打实的 —— 09-26 01:47 那一次 `mutate_pipeline_wiring_guard.py`
收线时，从 `/tmp/pipe41_deployed_bak/` 里读出的是**前一晚 23:00:34** 那份快照，
它把工作区里未提交的 #42 java 改动**整份覆盖回 HEAD**（`PipelineStages.java` 从 +140 行变回 0 行）。
症状还不是"测试红了"，而是 `mvn testCompile` **cannot find symbol**（测试引用了刚被抹掉的 `Resolution`/`resolve`），
所以四层门禁里没有任何一层会把它报成"缺陷"。已修的形态：
① 五支一起换成 `snapshot_sources()`，每次运行**无条件**重新快照，落到带 pid 与时间戳的独立目录
（`BAK = ~/.cache/zlc42/deployed_bak/<脚本名>/run-<pid>-<时间>`，`mkdir(exist_ok=False)`，撞不上第二次）；
② 备份不再放 `/tmp`（本机有多个会话会扫 `/tmp`，本轮已经丢过一次日志）；
③ 还原后除了逐字节比对，再核一次 sha256 —— 前端那支 F 战役对 java 参照集就是这么钉的。
教训口径：**"按字节还原"的参照集必须是这一次运行自己读的，不能是盘上恰好还在的那份。**

⚠ **第三种"两支队伍同时在飞"的形状本轮刚踩到，而且比并发开两支隐蔽得多**：注入脚本结尾若写成
裸的 `sys.exit(main())`（而不是 `if __name__ == "__main__":`），那么**任何一次 import 都会把
整支战役跑起来** —— 我只是想用 `importlib` 取一下它的 `artifact_fingerprint()` 函数，
结果那条命令变成一支**在飞的注入进程**：它抢了锁、改源码、重新 build fat jar、又起了一台 18090。
后果不是"多跑了一轮"而是**我的判定被污染**：同一时间我手跑的 `e2e_api_test.py` 报了 283/283，
而当时在伺服的那个 jar 的 class 指纹是 `ebde0d86533e`（= 注入态），盘上那份也是它注入后重建的 ——
那句"283/283"因此**不可归因**，只能作废重跑（重跑前先把盘上件重建回基线指纹 `bb52bb4ce511`）。
已把 4 支（`mutate_duplicate_guard{,_http,_deployed}.py` + `mutate_field_code_deployed_guard.py`）
改成 `if __name__ == "__main__":`，并**实测这个修法**：同一条 import 现在只打指纹、
不拿锁（`/tmp/zlc_mutate_harness.lock` 全程不存在）、不派生 JVM。
推论：**量具的"取一个函数来复用"必须走真正的库导入，不能走脚本文件**；
反过来说，任何注入脚本都该假定"会有人 import 我"。

> `_e2e/e2e_api_test.py` 需要 server 已起且**是本次运行新起的**：H2 是内存库，重启即空，
> 脚本自己会建 app/entity/字典并 provision，所以可以直接反复跑。若指向一个已被重启过的
> appCode 会看到「实体不存在」的空态。

### dev 环境为什么能起来（四处非显然的坑）

1. `ModuleDataSourceTemplate`（在 z-boot，发布件 1.0.9，改不到）只读 `host/port` 且
   **硬编码 `jdbc:mysql://`**，`z.base.db.*.jdbc-url` / `driver-class-name` 会被静默丢弃。
   `LcModuleDataSource` 现在自己识别 `jdbc-url`，没配才回落到模板。
2. `spring.sql.init` 在 z-lc 里**永远不生效**：该自动配置带 `@ConditionalOnSingleCandidate(DataSource.class)`，
   而 z-lc 同时有 `spring.datasource`(Hikari) 与 `dataSourceLc`(Druid) 两个 DataSource。
   改由 `z-lc-admin/.../DevSchemaInitializer.java`（运行时）和
   `z-lc-web/src/test/.../TestDataInitializer.java`（测试）显式灌 schema。
   注意 `@SpringBootTest` **不会**执行 `ApplicationRunner`，测试侧必须用 `@PostConstruct`。
3. `LcAutoConfiguration` 原先逐个枚举 16 个 `core.*` 子包，新增子包不会被扫，
   表现为「编译通过、bean 注入失败、启动直接挂」，且报错完全不指向根因。已改成整包扫 `core`。
   副作用（都已处理）：`core.lifecycle.ScriptLifecycleService` 硬依赖 z-script 的
   `DynamicApiExecutor` → 改 `required=false` + 空值放行；`core.ai.AiModelingService` 依赖
   `<optional>true</optional>` 的 `z-agent-llm-gateway-core` → z-lc-web 补了 test scope。
   也就是说 lifecycle 钩子在整个历史上从未生效过。
4. **`z-util` 抬到 1.0.12 是"本机装过"才有的前提**（`pom.xml` 的 `util.version`；父 pom `z-opc`
   仍是 1.0.9，其它仓不受影响）。数据源注册/探活/方言/动态查询/内存 SQL 引擎收口在 `z-util-jdbc`，
   z-lc 不再自建造池代码 —— 代价是这个版本**只存在于本机 m2**：
   `~/.m2/repository/io/github/yuku123/z-util-jdbc/1.0.12/_remote.repositories` 里三行都是
   `...=>`（等号后面为空 = 本地 `mvn install` 进去的，不是从远端拉的），同目录还留着
   一次早先失败尝试的 `.jar.lastUpdated`。换台机器 / 清过 m2 就会先吃一串"依赖解析不到"，
   而报错指向的是 z-util 不是 z-lc。⚠ 另一条同源教训：**改 z-util 的 L3 制品必须 `mvn install`
   才生效，且会让 m2 与源码分叉** —— 分叉之后"代码改了行为没变"是必然现象，不是灵异事件。

`z-lc-admin/src/main/resources/db/schema-h2.sql` 是 dev 权威 schema，按 MyBatis-Plus Entity
的真实字段重写。生产 MySQL 仍以 `z-opc/_doc/004_sql/` 为准，但那份有缺陷（见下表 #7 #8），
且排版被打散（列名和类型跨多行）、含 `AFTER` 等 H2 不认的语法，**别试图机读生成**。
实测 H2 2.2.224 `MODE=MySQL` 能吃 MySQL 的 `ENGINE=InnoDB` / 内联 `KEY` / `COMMENT` / 反引号，
但**跨表重名的 constraint name 会撞**（`IF NOT EXISTS` 不保护约束），要按表加后缀。

## 二、本轮修掉的存量缺陷（全部是实测打出来的，不是重构偏好）

| # | 缺陷 | 影响 | 有没有回归测试 |
|---|------|------|----------------|
| 1 | `POST /dict/items/create`、`/items/update` 走的是 `saveItems`（整表替换语义：先软删该字典全部项再插一条） | **新增一个字典项会删光同字典其它字典项**，数据丢失级 | `LcHttpContractTest.createDictItemIsNotDestructive` |
| 2 | `buildListSql` / `buildCountSql` 里 JOIN 的 `dict_code = ?` 占位符排在 WHERE 之前，但参数先 `add(tenantCode)` 再 `add(dictCode)` | 租户值与字典码**互相绑错**：含字典字段的实体分页恒为 0 条，且租户过滤实际失效 | `runtimeListWithDictFieldReturnsRowsAndLabels`（已实测：把 bug 改回去立刻红，报 `total:0`） |
| 3 | `joinColumns` 给业务列加了 `t.` 前缀，系统列 `id/create_time/update_time` 没加 | list SQL 一旦 JOIN 字典表即 `Ambiguous column name "id"` | 同上 |
| 4 | `selectCount(qw)` 复用了带 `ORDER BY` 的 wrapper（`AppAdminBizService` 与 `SchemaAdminBizService` 各一份） | `SELECT COUNT(*) … ORDER BY id DESC`，MySQL 宽容所以从没暴露 | `pagedAppListSurvivesStrictSql` |
| 5 | 结构化 `sorts` 白名单只认 `fields[]` 里的列 | `id` / `create_time` 不能排序，前端点表头就 400 | `structuredSortWhitelist` |
| 6 | `POST /app/event` 返回 `Result.success(Result.fail(...).code(409))` 双层信封 | 外层 `success:true`，**任何只看外层的客户端会把因果冲突的失败写入当成成功** | `eventChainHeadAndHonestConflict` |
| 7 | `parentEventId` 是因果校验必填，但没有任何接口能读到链头 | 除内部服务外，**外部调用者追加事件必然撞 409** | 已补 `GET /api/lc/app/event/last` |
| 8 | `z_lc_relation.relation_type VARCHAR(8)` | 存不下 `ONE_TO_MANY`(11)/`MANY_TO_MANY`(12)：H2 报错，**MySQL 非严格模式静默截断成 `ONE_TO_MA`**，关系类型从此永久错乱 | 仅 dev schema 已修，**生产 MySQL 还缺 migration** |
| 9 | `z_lc_app.icon` 只存在于一条 `ALTER`，基础建表没有 | 只跑建表就少列 | dev schema 已含 |
| 10 | `BooleanTypeHandler.coerce` 用 `Boolean.valueOf(str)`，而 `checkValue` 认 `1/0/Y/N` 合法 | **校验放行、写入篡改**：`is_vip:1` 静默存成 0（CSV 导入、整数式布尔全中招） | `BooleanTypeHandlerCoerceTest` + E2E「整数 1 落库为真值」 |
| 11 | undo 快照把 `java.sql.Timestamp` 序列化成 UTC ISO 串，而引擎口径是 `yyyy-MM-dd HH:mm:ss` | **带 DATE/DATETIME 字段的实体，撤销删除必失败**（`Field requires date: ...`）；而"直接截掉 +08:00"又会把时刻平移 8 小时 | E2E「复活后 birthday / last_visit 与删除前完全一致」 |
| 12 | redo 用 `updateById` 清 `undone_by=null` | MyBatis-Plus 默认 `FieldStrategy.NOT_NULL` **跳过 null 字段**，redo 表面成功但记录仍标"已撤销"，下次 undo 直接跳过它 | `undoAndRedoRestoreRowImages` |
| 19 | **写入链路从来没调用过注册表的 `validateValue`** —— 长度、数值范围、日期可解析性都白写了 | `preview` 会放行一个 300 字符的值，然后 `commit` 才被数据库以 "Value too long" 打回来。正是我上一轮亲口说过"不可能出现"的**预览说能过、真写却失败**。新增 `ValueValidateProcessor` 补上 | `previewCatchesLengthViolationsBeforeTheDatabaseDoes`（把处理器摘掉即红） |
| 20 | 批量导入**逐行**记变更日志，而日志写在另一个数据源、不跟主写入一起回滚 | 中途失败的批次会留下指向已被回滚记录的"幽灵日志"，undo 会去撤不存在的行 | 改成整批成功后再记；并修了"两个 list 按下标配对、某行没拿到 id 就整体错位"的自伤 | `failedImportLeavesNoOrphanJournalEntries` |
| 13 | **`RequiredCheckProcessor` 只看提交的 `fieldValues`** | 部分更新（内联编辑一格、看板拖一卡，只提交一列）会因为**其它必填列没出现在请求里**而被拒 —— 等于"只要有必填列，单列编辑全线不可用"。改成按"改完之后的整行"判定：未提交的列取库里现值，提交了但为空的仍算清空并拒绝 | `partialUpdatePassesRequiredCheckAgainstExistingRow` + E2E [15e] |
| 16 | 前端 `asBoolean` 只认 `'true'`，而字典/CSV/接口常给 1/0/Y/N | 与后端同一个"校验认它合法、转换判它 false"的坑，前端版；布尔列勾选后提交 '1' 会被写成 false | `registry.contract.test` 布尔用例 |
| 17 | 前端 `pickDefinition` 里 `widget:*` 覆盖**连值转换一起覆盖**，`widget:datePicker` 是从 DATETIME 定义 spread 来的 | DATE 列被按 `YYYY-MM-DD HH:mm:ss` 序列化写出去，依赖数据库宽松解析才没出事。类型身份必须跟 `ctx.field.fieldType` 走，widget 只能换控件 | 同上（日期用例） |
| 18 | `getFieldDefinition` 是精确匹配，而 REF/DICT 是注册表里的合成小写别名 | 调用方直接把后端 `fieldType` 传进来会拿到 undefined，然后在页面上炸成 undefined 属性访问。已改成大小写 + 别名收口 | `registry.contract.test` 别名断言 |

> 16/17/18 都是**先有测试、后被发现**的：没有这轮补的前端测试集，它们要靠用户点出来。

| 15 | 前端 `GridView` 的列布局/排序/统计是 `useState(() => readStoredState(...))` 懒加载，而路由 `/:appCode/:entityCode/:viewType` 切实体时 React **复用同一组件实例** | A 实体的列顺序与页脚统计会直接套到 B 实体上（实测：切到 invoice 后 footer 仍带着 customer 的统计配置） | 已用 `key={entityCode}` 强制重挂，并实测双向验证隔离 |
| 14 | 更新一条不存在/已软删的记录 | 报的是 `[RequiredCheck] 字段 X 为必填`，把用户引到完全错误的方向；现在直接 404 `record not found` | E2E [15e] |
| 21 | 唯一编码撞车走 `DuplicateKeyException`：HTTP 500 + 消息里带**索引名、表名、列名** | 软删的行仍占着唯一索引（`uk_*_tenant_code` 不含 `deleted`），而预检写了 `.eq("deleted",0)` 自查通过 → 客户端拿到 500 和一份库结构。四实体撞了六次 | 四条路由各自的预检 + `LcExceptionHandlerTest`（兜未预检路径）；E2E [15i] 24 项，A1–A4 逐条反证 |
| 22 | 元数据**读不到**时界面说"**已被删除**" | `DashboardPage` 逐块报「图表视图 #id 已被删除」、`WorkspaceViewPage` 报「实体可能已被删除」。用户会去找一次自己根本没做过的删除，甚至照着提示把组件从仪表盘里移除 | `meta.read` 逐资源口径 + `unreadReason`；vitest 新增 4 条，M1/M3/M4/M5 四个注入各自变红 |
| 23 | 逆向映射（设计器「从数据库导入」）借连接不还：`getDataSource().getConnection().getMetaData()` | 每点一次「扫描」永久吃掉池里一个连接（Druid 现场：持有 66 秒 + `abandon connection`），池一空整个服务无声挂起 | `reverseMappingReturnsEveryConnectionItBorrows`（borrow/close 计数）+ `reverseMappingEndpointsDoNotGrowThePool`（真实池 active），`mutate_connection_leak.py` 注入即双红 |
| 24 | 7 个管理列表页把**接口失败**画成"该应用还没有 X"，且失败的刷新**不清行** | 两类谎话同页：空态把人支去新建数据而不是查接口；刷新失败后旧行还在、`isError={false}`，表看着比故障前更"健康"（字典页那行字面写着 `isError={false}`）。选不到应用时又说"该应用还没有关系定义" | `useResourceList` 五态口径 + `AdminListStates.test.tsx` 13 条；`mutate_admin_list_guards.py` 八个注入，见下文「管理页列表」一节 |
| 25 | 同一类谎话在 workspace 侧的三个出口：侧边栏 / 应用概览统计位 / 实体候选下拉 | `fetchWorkspaceMeta` 从不抛，所以 `isError` 永远不红：schema 读不到时侧边栏输出「该应用还没有实体。」+ 一个建实体的链接，概览把三个数报成 0，五个管理页的实体候选**静默变空**，`AiModelingPage` 顺着 `entityOptions[0]` 从 toast 里说出同一句谎 | `meta.read.entities` 门禁 + `useEntityOptions` 改出 `EntityOptionSource{options,read,reason,retry}` + `entityNotFoundContent()`；`WorkspaceEntityStates.test.tsx` 12 条，`mutate_workspace_entity_guards.py` 八个注入（A1–D1），见下文「workspace 侧」一节 |
| 26 | 记录侧最后三个出口仍在把"这次没读到"说成"库里没有"：画廊 / 日历 / 变更历史 | 同一批记录、同一次故障：表格说"记录没有读到 + 重试"，切到画廊变「暂无数据」、切到日历摆一整月**画满格子、一个事件都没有**的月历（比"暂无数据"更像"这个月确实没安排"）、变更历史说「这个实体还没有数据变更」。日历那句更短命 —— 只有一句三秒就消失的 `message.error` | 三处接上 `StateBlock`/`ErrorBlock`；`WorkspaceRecordStates.test.tsx` 16 条 + `mutate_record_fetch_guards.py` 十二个注入，见下文「记录侧」一节 |
| 27 | **`CalendarView` 无限重查**：`monthStart/monthEnd` 每次渲染现算，依赖身份每轮都变 → `loadPage` 换 → `useEffect` 自触发 | 挂载后 300ms 内 43 次 `/runtime/list`（实测），每次都是同样的数据所以**画面完全正常** —— 四类 UI 断言一条都看不见，只有数请求条数才暴露。同一写法在画廊只要把 `[...conditions]` 塞进依赖就会复发 | `一次挂载只查一次` 2 条（含换月/换筛选各只补一次）+ 浏览器侧 `churn` 检查（安静 2s 内不该再有 `/runtime/list`）；C0/G5 两个注入即红 |
| 28 | 设计器侧栏（`/designer/:appCode` 左列）把**实体清单读失败**说成"还没有实体"：`catch` 里只 `message.error('加载实体失败')`，`loading` 初值 false，底下无条件是 `<List locale={{emptyText:'还没有实体'}}>` | 收尾扫描找到的最后一处同形态出口，也是**后果最重**的一处：设计器正是"这句话是真是假"由它决定的地方 —— 用户照着"还没有实体"去点「+」新建一个其实已经存在的实体，就会撞唯一编码（#21 那一族）；慢接口期间第一帧就已经在说这句话 | `StateBlock` 五态 + `DesignerEntityStates.test.tsx` 7 条 + `mutate_designer_entity_guard.py` 七个注入（D1–D7），见下文「设计器侧栏」一节 |
| 29 | 浏览器层对设计器的唯一断言是 `body.innerText.includes('字段')` | 整页找词：字段表格表头、右侧编辑区的输入框标签到处都在说"字段"，侧栏就算写着「还没有实体」也照样绿。这条谎在单测层被七个注入堵住之前，浏览器层是**看不见**的 | 换成按卡片定位断言"列出的是真的那一个实体"+ 侧栏不许出现两句谎 + 挂载只查一次 + 安静 2s 无重查（4 条）|
| 30 | 计数守卫（#27 那一族）只覆盖了日历与画廊，`GridView.load` 这个**最后一个手写取数出口**没有 | `load` 的依赖里躺着 `state.sorts` 与父组件传进来的 `conditions`，两个都是"每轮渲染可能换新身份"的东西；改错一次的后果和日历那条一样：画面完全正常、同文件那 8 条 UI/请求体断言一条都不红 | `GridView.test.tsx` 第 9 条 `画出行之后安静期不再自己重查` + 浏览器侧 2 条计数检查；`mutate_grid_refetch_guard.py` 两个注入（V1/V2）各跑两遍即红且零连带 |
| 31 | **注入脚本自己按全量跑 churn 缺陷**：V1 挂上去之后 vitest 13 分钟没跑完 | 一支"反证"脚本跑不完就等于**没有结论**，而它的退出码看起来只是在忙（我 kill 之后要手工核对源码 md5 才敢确认树是干净的）。同一注入按**文件**跑，几秒就给出 `expected 3 to be 1` | 脚本改为只跑 `GridView.test.tsx`（分母 9），并把"收集到的 9 条用例标题"钉成静态清单 —— 邻居被改名/删除时判"作废"而不是静默吸收 |
| 32 | 看板卡片上的 tooltip 写着**「移到其他列（键盘可用）」**，而那条路径是死的：Enter 打得开菜单，之后 ArrowDown + Enter 什么也不做，卡片留在原列 | antd 的 `Dropdown` 打开时**不把焦点交给菜单**（`autoFocus` 只实现于 `Dropdown.Button`，`rc-dropdown` 自己没有任何键盘处理），而方向键/Enter 全挂在 `rc-menu` 根节点（`tabIndex=0` + `useAccessibility`）上 —— 焦点还在触发按钮时一条都收不到。「能打开」被当成了「能用」，检查就停在了菜单渲染出来 | `mutate_kanban_keyboard_guards.py` 四个注入（M1 不给焦点 / M2 不还焦点 / M3 不销毁弹层 / M4 摘掉整座桥）各跑两遍，预期红集合与实测**逐一吻合且零预期外的红**；`KanbanView.test.tsx` 3 条焦点用例；浏览器门禁 1 条全程键盘检查（2/2 轮绿） |
| 33 | **列设置抽屉"看到的顺序"和"写出去的顺序"是两套**：`rows` 直接 `map(resolvedFields)`（schema 序），而表格读的是 `projectColumns(resolvedFields, columnMeta)`；`patch` 对还没持久化的列是往 `columnMeta` **尾巴追加** | 三个症状都在同一轮真浏览器里读出来的数：① 干净的视图（`columnMeta` 默认空）里只给"工时"输一个宽度，表头就从 `标题\|优先级\|工时\|截止日期` 变成 `工时\|标题\|优先级\|截止日期` —— **改个宽度把整列挪到第一列**；② 排序成功之后抽屉里那几行纹丝不动，用户以为没生效；③ 再排第二次得到和第一次**完全相同**的表头，上一次的结果被整份覆盖（"移到第三列"这种布局根本表达不出来） | `ColumnManagerDrawer.test.tsx` 3 条 + `mutate_column_order_guards.py` 三个注入（I1 退回 schema 序 / I2 退回追加 / I3 抹掉邻居配置）**预期红集合互不相同**、各两遍零连带；浏览器门禁 §4b 6 条（含真键盘 `Tab→Space→↓→Space` 连排两次验证叠加）。注意界面上那句「空格 + 方向键可键盘排序」**是真的**（和 #32 不同，实测能排序），谎的是顺序本身 |
| 34 | **撞引擎自建列的字段编码照收**：`id / tenant_code / deleted / create_time / update_time` 全是合法标识符，`FIELD_CODE_RE` 放不住，而它们会和引擎自己那几行一起被写进建表 DDL | 保存元数据成功、`previewDdl` 也照画，到 provision 建表才炸 `Duplicate column name`；而 `provision-all` 在**第一个**坏实体上抛，同一个应用里其他实体的表被一起挡住。两侧同口径：后端 `SchemaAdminBizService.validateFieldCodes` 在 create/update/建表三处拒 400（`_e2e/mutate_field_code_guard.py` I1–I4），前端 `src/fields/columnRules.ts` 在点保存前就拦 + 把坏的那一格标红（F1–F6） |
| 35 | #34 那道闸的**前身是一个 `continue`**：建表时遇到撞名/非法编码就静默跳过那一列 | 「报成功而实际做得更少」比报失败难查一个量级 —— 表建"成功"了、少几列没人知道，元数据里有而物理表里没有，之后 runtime 一查才 500 | 改成直接抛；注入 I3/I4（把三处调用点逐个摘掉）各红自己那一条，摘一处红一处＝三处都不是重复检查 |
| 36 | 「新建实体」按钮是**空操作**：`onClick` 里 `setDraft(新草稿)`，而"从选中实体派生草稿"的 `useEffect([selectedId, entities])` 随即用 `found === undefined` 把它冲回 null。同一条路上还埋着第二处：新草稿 `fields: systemFieldDefs()` 预置 3 行引擎自建列，于是 #34 那道**新闸把新建这条路按住了** | 设计器只能改已有实体、一个都新建不了；而症状长得像"闸写错了"，最容易的"修法"是去放宽闸 —— 那是把两个缺陷合成一个更坏的 | 加 `creating` 让 effect 认新建、字段表清空（引擎列由视图侧自己合成，见 `registry.systemFieldDefs`）。`DesignerFieldCode.test.tsx` 的 `闸不许把新建实体这条路堵死` + 注入 F8/F9：**同一测试标题、不同断言**（F8 红在「没有「实体编码」这一栏」＝编辑器根本没出现，F9 红在「expected 3 to be 0」＝闸被预置列触发），本轮在归档的 `failureMessages` 里逐条核过 |
| 37 | 交叉表里**空值那一档的表头是一个空格**：某维值为空时后端给的列键是 `""`（不是 NULL、不是缺列），`PivotView` 原样把这个键当标题画出去 | 一根带真数字的**无标题列**摆在最右边，看着像装饰列；合计行、总计、格子数字全都对，所以 Java 单测、vitest、接口 E2E 三层**一条都不红**，只有真浏览器量表头才看得见。用户读不出那是"没填写"，也没有任何提示告诉它缺了什么 | `pivotModel.pivotColumnLabel` 把空白键映射成 `chartModel.UNFILLED_GROUP_LABEL`（`（未填写）`，与图表同源），表头另存原始键进 `data-col-key` 让"认领的是后端那个空键"可证。`pivotModel`/`PivotView` 26 条单测（注入 P1–P10）+ 浏览器 13 条（`10e`）+ 7 条空档正例（`10e-2`，先真清一条记录的 `prio` 再画）+ 接口层 `[15d1]` 15 项，见下文「交叉表（PIVOT）」一节 |
| 41 | **处理流水线页与引擎是两套事实**：`Pipeline` 永远跑全部处理器并按类名排序，`PipelineConfigService.listByEvent` 在生产代码里**零调用者**；页面上还摆着 `WEBHOOK`/`SCRIPT` 两个没有执行器的阶段和 `AFTER_*` 三个没有写后挂接点的事件 | 删阶段、调顺序、换事件全都"保存成功而运行时什么都不变"，这比报错坏得多 —— 它看着像配好了。用记录调用顺序的桩一量就露：配置里点名的那几个阶段一个都没跑 | `Pipeline.resolve` 按 (appCode, entityCode, triggerEvent) 查启用配置并**按 `order` 执行**，配置兑现不了就抛错而不是悄悄退回默认链；`PipelineStages` 成唯一词表，写入口拒掉幽灵阶段/写后事件/缺闸门/值校验倒序/非整数 order；前端清单从 Java 源码机械抽取。四层反证见「处理流水线」一节 |
| 42 | 流水线**阶段参数 `config` 是下一个"存了但一个字都不执行"**：`#41` 让顺序真的生效之后，`PipelineStages` 仍然只把 config 当 JSON 存进行，没有任何处理器回头读它 | 给 `TRIM_STRINGS` 配 `{mode:'both'}`、给根本不存在的参数配值，全都"保存成功而运行时什么都不变"—— 症状和 #41 一模一样，而它藏在已经修好的那一层底下，更难看见。现在写入口把 config 里每个键拿去对**该阶段登记在 `PipelineStages.CONFIG_KEYS_BY_TYPE` 里、处理器真读的键集合**：没人读的直接 400 并点名是哪几个键，config 不是对象也 400；运行期同一份内容只 `log.warn` 不按住业务写入 —— 这个不对称是故意的（写入口拒是为了不让一份假参数出生，运行期拒会让一份历史配置把用户的写入一起带走），U5 专门钉这一条 | `_e2e/mutate_pipeline_config_guard.py` U1–U6 单测层（含反向注入 U6「凡是带 config 就拒」—— 只测拒绝的网写成"永远抛异常"也能全绿，那一支就是拦这个的）+ 部署件层 P13/P14 |
| 43 | **两个实体可以抢同一张物理表，而 `provision-all` 说"建好了"而列根本没建**：表名在租户内是一份命名空间，但三个写入口都不查；`CREATE TABLE IF NOT EXISTS` 对第二个实体是空操作，而接口从不回头看库里到底有什么 | 撞上的那支元数据照收、provision 报成功，实际一列都没建 —— 之后 runtime 每次读都是 `Column ... not found`，而"有实体没建成"在旧汇总里被算进"已 provision"（那个 Map 每个实体一个键，坏的那支也在里面）。现在 `validateTableNameAvailable` 在 `createEntity` 这一处入口就把抢名拒掉、点名是哪一方占着（软删的不占，表名可复用；比较不分大小写）—— 逆向映射的出口（`DbTableController` 那两处 `createEntity` 调用）走的是同一个入口，所以一并挡住；`updateEntity` 路径上表名根本不可改，没有第二个口子。另一侧还有 `otherLiveEntityOnSameTable`：**抢不出来不等于不存在**（历史上已经落在同一张表上的两份定义、软删实体留下的旧表），补列是有副作用的 ALTER，不做这一问就是拿 B 的定义去改 A 的表。provision 之后**回读 JDBC 元数据**，报告长出 `missingColumns/addedColumns` | `_e2e/mutate_provision_deployed_guard.py` D1–D13（十三支注入逐支按预期点名、零连带红）+ `SchemaAdminBizServiceTest`。⚠ **#47 之后这两条自己的契约断言过期了**：`LcHttpContractTest` 那两支把"旧表比定义少一栏"钉成"必须永远 FAILED"，而 #47 恰恰把这一族修成了 ALTERED —— java 门禁红两条，**红的是断言不是产品**。已按实测改成仍可否证的形式（ALTERED + 库里真有那一栏；另配一张真正修不好的"缺引擎自建列"的 foreign table），并由 K1–K8 八支注入逐条重新证牙（见 #47 那一格）|
| 45 | `updateEntity` 是**全删再全插**：删是软删（留下墓碑行），插走唯一索引 `uk_field_def_tenant_code`（不含 `deleted`） | 同一份定义第二次保存必然撞自己刚留下的那行墓碑 → **带字段的实体建好之后就再也改不动**（PUT 一律 400，错误消息还是 #21 那一族索引名）。现在按 `fieldCode` 对齐：同名的原地更新并 copy 属性，多出来的墓碑化，只有新编码才 insert；大小写变体算同一个编码 | `_e2e/mutate_edit_path_deployed_guard.py` E1–E6 六支（打 18090 上真跑着的那支 jar，字节级 marker 预检不过就 FATAL —— 上一轮 `mvn install` 0.9 秒"成功"而 fat jar 里还是旧件）+ `[15r]`/`[15s]` 探针 |
| 46 | **运行时权威源是事件链，但折叠从不删字段**：一栏被移除时只墓碑化元数据、不发 field 级 DELETE 事件；`EventReplayService.replay` 收到整实体 DELETE 也只是 `computeIfAbsent` 立个空桩 | 墓碑是表的口径，不是运行时的：删掉的栏在折叠出来的定义里永远活着（多出来的 `extra_col` 直接把整个列表打成 400，而不是少一列），删掉的实体照样供记录、还凭空多一个空桩 | 一栏移除时 `emitFieldRemoved` 补一条 field 级 DELETE；replay 里整实体 DELETE（payload 无 `fieldCode`）把实体从折叠里摘掉、不再预立空桩。`_e2e/mutate_replay_guard.py` M1–M3 —— M3（任何 DELETE 都抹整实体）在 HTTP 流程里是**等价变异**，只有折叠层单测捉得住，这正是"DELETE 带不带 fieldCode"这套词汇表要单独钉一层的原因 |
| 47 | **定义跑在物理表前面之后，没有任何一条路能把它修回来**：`provisionOne` 见表已存在就一律 `FAILED`，运行时每次读都是裸 500（`Column "t.xxx" not found`），而设计器那句「未建成」横幅谁都清不掉 | 这是 #43 修好之后剩下的那半边：闸挡住了抢表，却没回答"漂移了怎么办"。用户的自助路径本来是现成的 —— 设计器里点一次 provision —— 但那次点击永远得到同一句谎。现在 provision 比对定义与 JDBC 元数据，缺的列 `ALTER TABLE` 补上并**回读库**作证（第四态 `ALTERED`，只加不改不删，幂等）；库自己拒的（如有行的表加 NOT NULL 无默认值）才 `FAILED` 并点名缺哪几列；读侧认得出列名的 `BadSqlGrammarException` 转 400、点名那一栏并指向 provision，认不出照旧 500（把真·库故障包装成"去点一下 provision"比 500 更坏）；界面上四态各说各的话、批量汇总多出 `altered` 一格、补成功即清横幅 | 五层各有牙，各自逐支实测吻合：`mutate_provision_reconcile_guard.py` C1–C7（java 单测：占表判定那五个谓词）→ `mutate_provision_contract_guard.py` K1–K8（java 契约层：`LcHttpContractTest` 那两支，8 支注入认领 **11** 条具名红、分母每轮钉 55 条）→ `mutate_provision_report_guard.py` M1–M18（vitest，`DesignerProvision.test.tsx` **15** 例 —— 本轮 `npm run check` 现读到的数，不是记的）→ `mutate_provision_deployed_guard.py` D1–D13（发出去的 jar）→ `mutate_provision_browser_guard.py` P1–P6（真浏览器 `11d` 那 **24** 条（静态 `check(` 25 个，末支在 catch 分支里只在失败时打）：8 条各有牙、6 条按未覆盖记账），`11d` 这一节自己从 `FAIL 3` 收到 `FAIL 0`、全绿 3/3 轮 |
| 48 | **权限这一族六个写入口/读出口各说一套**：① `grant` 的查重写 `eq("entity_code", entity.getEntityCode())`，而"整个应用"的授权那一列是 NULL —— SQL 里 `entity_code = NULL` **恒为 unknown**，一行也匹配不上；② `hasPermission` 只比 `entity_code = ?`，丢掉了「或应用级」；③ 三个列表查询加一个判定**一个字都不比 `tenant_code`**，而 `grant` 把 body 里的租户原样落库；④ `revoke` 是 `deleteById(id)`，删 0 行与删掉别人的行都无条件回"已回收"；⑤ 权限项没有任何词表，任何字符串都收；⑥ 前端矩阵按"筛过的行"算格子真值，而 `/list` 的 `roleCode` 优先级高于 `entityCode`，两个筛选一起给时实体那个被后端静默丢掉 | 六条都不崩、都不报错，只让**策略数据与判定互相说不清**：①恰恰是矩阵页默认那一支（不选实体点格子），每点一次多一行"查重成功"的重复行；②让矩阵里明明白白渲染成「整个应用」的那些行对任何一个实体都答"拒绝"（反过来，某个实体的单独授权在旧页面上会把「整个应用」那一格点亮）；③两半合起来等于没有租户这一列 —— 任何人都能把别的租户的授权写进这张表，然后在本租户的判定里读到它（`/check` 直接答 true）；⑤最隐蔽，因为**实测这套 dev H2（`MODE=MySQL`）的 `=` 分大小写**（D3 取证时 curl 读到 `permission: "view"` 与 `"VIEW"` 在库里并排两行）：同一个逻辑授权成了两行、各自都"查重成功"，而矩阵与网关只认大写那一行 —— 存下了、查不到、界面上却亮着 | `PermissionKeys` 成唯一词表（VIEW/CREATE/UPDATE/DELETE/EXPORT，`ALL` 的顺序**就是**矩阵的列序），写入口 trim + 大写归一、不在词表内 400 并点名，`trimmedToNull` 把空范围统一成 NULL、查重按 `IS NULL`；判定分两支（给了实体→该实体 `OR` 应用级；留空→只问应用级，不拿某个实体的授权冒充"整个应用"）；四个端点全部钉 `DEFAULT_TENANT`（与 `AppAdminController`/`DictAdminController` 同口径）且服务层每个读方法都收 `tenantCode`；`revoke` 返回受影响行数、0 行抛 `IllegalArgumentException`→400；前端矩阵与 `/check` 用同一个 `coversScope`/`isAppWide` 口径，格子真值对**整张表**算（只按 `appCode` 拉一次全量、筛选放客户端），回收失败不再吞成"已回收"，空态两句话分开（"被筛掉了" vs "这个应用压根没配过权限"）。**三层各有牙，各自本窗实测**：`_e2e/mutate_permission_service_guard.py` J1–J14（java 单测+契约层，分母每轮钉 **75** 条、认领 **25** 条具名断言）→ `z-lc-admin-ui/e2e/mutate_permission_matrix_guard.py` B1–B12（vitest 层，分母钉 **10** 条、零连带红）→ `_e2e/mutate_permission_deployed_guard.py` D1–D14（发出去的 fat jar，认领 `[15t]` 那 **30** 支探针里的 **24** 支，另 6 支是 `ok()` 夹具、按理由不进判红账）。首轮**六处对不上的账全部按实测改**（D3/D7/D12 三处预期 + 两处从没红过 + 补出 D14 才有猎物），没有一条断言被改软 |
| 51 | **引擎自己建的表与元数据层不在同一套校对上**：`buildCreateTableDdl` 只写 `DEFAULT CHARSET=utf8mb4`，不给 `COLLATE` | MySQL 8 于是取该 charset 的**默认校对** `utf8mb4_0900_ai_ci`，而 15 张 `z_lc_*` 元数据表沿用库默认 `utf8mb4_general_ci`（实测 `information_schema.tables`）—— 跨表字符串比较当场 `Illegal mix of collations`。250 上第一轮真库门禁 **28 条红里 8 条是这个**。dev 的 H2 压根没有校对这一层，所以这四支缺陷**一支都不会在本地红** | 问出元数据层那一套并钉进建表语句；已错的那张表在 provision 报告里判 `FAILED` 并给出可直接执行的 `ALTER TABLE … CONVERT TO CHARACTER SET … COLLATE …`。`_e2e/mutate_collation_guard.py` **M1–M13**（13 支注入，M2/M3/M7 那三支"装配顺序/两个参数换序/把问不到缓存下来"编译器都管不着）+ 部署层闸 3（三段式负控：正向绿 → 注入旧版代码自己会写出的那句 DDL → 恢复绿）|
| 53 | 接口层量具的 `atexit` 分母卫兵**注册在崩溃点之后**：脚本前半段就挂时，"这一轮到底跑了多少项"一句都不说 | 半途死的量具打得没打红看不出来，退出码之外没有第二个证据 | `_e2e/e2e_api_test.py` 把卫兵前移，空输入必 FATAL（不再打印"满分"）|
| 54 | **给有行的表补"必填且无默认值"的列，结局由库决定**：`ADD COLUMN x VARCHAR(32) NOT NULL` 在 H2 当场拒，在 `mysql:8.0.26`（`@@sql_mode` 含 `STRICT_TRANS_TABLES`）却**接受**并把已有行那一栏静默填成 `''` | provision 报 `ALTERED`"成功"，而库里已经躺着一批违反"这一栏必填"的行 —— 这是"报告说得对而数据是坏的"那一类，界面上没有任何一处会显示 | 补列之前先问库里有没有行（`queryForList("select 1 from … limit 1")`），有行且没配默认值就一列都不许多发并带回可操作修法；问不到行数按"有行"fail-safe。注入 **N1–N5**（N2 专把 fail-safe 翻成 fail-open）；只有 MySQL 才红的那一半由 `deploy_250.sh api` 在真库上量，不由单测冒充 |
| 55 | 部署脚本用 `pkill` + `sleep 3` 收旧进程，**不等端口**：旧 jar 还在服务 18090 时新 JVM 死于 "already in use"，而 `/health` 照样回 UP | "这次部署起来了"这件事当时没有任何一层能归因 —— health 探针读到的 UP 属于上一支进程 | `app.pid` 记账 + `port_pid()` 逐次把端口上的监听者与本次 pid 对齐才敢报 UP + 起进程后盯日志 fail-fast（实测：`旧进程已收干净（等了 29s, 端口 18090 空）`）。闸 2 的负控就是这个形状（把 LC 池指向 `z_lc_misdeploy`：API 写得进、读回也认账，只有读 MySQL 才判红）|
| 56 | **闸 3 的负控里四把尺各自坏在量具上**（缩进敏感的 `grep '^ALTER'`、`tables` 上问 `collation_name`、`step_collate \|\| die` 的死代码、探针表判红后没被收掉） | 一道只会红在别处的闸，看起来像"咬住了" —— 而它一次都没咬过自己该咬的那一支。这一支缺陷是**测量它的那一轮**发现的 | `grep -E '^[[:space:]]*ALTER TABLE'`、`table_collation`、`( step_collate ) || die …`（`die` 走的是 `exit`，子 shell 才拦得住）、清场改挂 `trap … EXIT`（见 #58）。负控三段现在逐段验：起点必须绿、红必须具名报"闸 3"、判红集合里必须有探针那一行 |
| 57 | **java 侧那道校对闸在真 MySQL 8 上是死代码**：`tableCollation()` 问的是 `information_schema.tables.collation_name`，而 MySQL 8 的 `tables` 视图里表级那一列叫 `TABLE_COLLATION`（`COLLATION_NAME` 是 `columns` 的列名） | 这一句每次都抛 `ERROR 1054 Unknown column`，被生产的 `catch` 吞成"问不到"，`collationRepairMessage` 因此不判 —— **漂到 `utf8mb4_0900_ai_ci` 的表 provision 照样报 `EXISTS_INTACT`**（闸 4 实测到的就是这个读数）。它坏得最安静：接口全绿、单测全绿、界面也全绿 | ① SQL 改 `table_collation`；② 那一侧的失败不再算合法的"问不到"，日志 `debug` → `warn`（能走到这里说明元数据层问得到参照，那就是这句问坏了）；③ **单测层的替身原来压根不看 SQL**，所以 `FakeJdbc.queryForObject` 改成按 250 实测的目录形状答话（问错列名就抛，等价于真库拒绝）—— 于是 M13 这一支从"注了也不红、只能记未覆盖"变成红在 `provisionShouldFailWhenTableCollationDiffersFromMetadataLayer` 这一条具名断言上。真库那一半由部署层**闸 4** 证（三段：新建的表就在参照校对上 → `CONVERT TO` 漂到 `utf8mb4_0900_ai_ci` 并验注入落地 → provision 必须 `FAILED` 且应答里带着指向正确校对的那句 → 搬回后 `EXISTS_INTACT`）|
| 58 | **部署量具自己两把坏尺**：① 清场挂的是 `trap … RETURN`，而红路径走的是 `die`（= `exit`）；② 脚本经 ssh **stdin** 喂给远端，脚本内部的 `docker exec -i` 把 stdin 剩下的部分吃掉了 | ① 探针表与探针应用在判红后留脏（实测残留 `表=1 应用=2`），下一轮"起点不干净"会常红，且报的是"库里不一致"这种看着像别人账的错；② 三条查询的 heredoc 只印出第一条（本机三次复现，包括我手工清扫时第二遍循环读不到东西） | ① 清场改挂 `trap … EXIT`；② 除 `step_schema` 那处真要灌文件的地方以外一律去掉 `-i` 并补 `</dev/null`。两处都验到残留归零（`表=0 应用=0 实体=0`）。⚠ 这一支的机制我只写实测到的相关性，不写"为什么"的理论 |

## 三、前端现状（`z-lc-admin-ui/`）

Vite 5 + React 18 + TS strict + antd 5 + pro-components + react-query v5 + zustand + dnd-kit。
5273 端口，proxy 指向 18090（`VITE_LC_API_TARGET` 可覆盖）。

架构核心是 **meta 驱动的字段注册表** `src/fields/`（对标 Baserow 的 fieldType 契约）：
一个 fieldType 一个条目，声明单元格渲染 / 内联编辑 / 表单控件 / 可用算子 / CSV 格式化 /
设计器 config 子表单（`configFields` 是数据不是 JSX）。**任何页面里都不许 `switch(fieldType)`**，
新增字段类型不需要改任何视图代码。字段类型能力元数据优先取服务端 `GET /api/lc/meta/field-types`，
拿不到回落 `src/fields/defaults.ts`，所以 UI 不会因为后端版本而崩。

新增能力（对标 Teable 的 server-side undo/redo，NocoDB 把它锁在企业版）：
`z_lc_data_change` 变更日志 + `POST /api/lc/undo/{undo,redo}` + `GET /api/lc/undo/history`，
grid 工具栏「变更历史」抽屉里可直接看逐字段 diff 并一键撤销。
三点语义要记住：① 撤销的是**前像回放**，不在浏览器重放；② 一条记录若在这之后又被改过，撤销会被
拒绝而不是悄悄覆盖；③ 重做「新建」时自增主键会变（原 recordId 仍在日志里可追溯）。

**撤销栈是 per-user 的**：写路径把身份记进 `z_lc_data_change.actor`，`/undo/{undo,redo}` 默认只回溯
调用者自己的栈，`/undo/history?owner=mine|all` 同口径。身份来源是请求头 `X-User-Code`
（网关注入为准），控制台在 `src/api/actor.ts` 里生成一个 localStorage 持久化的 `web-<uuid>` 兜底，
所以两个人开两个浏览器不会互相回滚。`ActorResolver` 只做归属，**不参与鉴权**（z-lc 直连模式，
鉴权统一在 z-ctc）。前端「变更历史」抽屉有 只看我的/全部成员 切换 + 当前身份回显。

页面：应用列表 → 实体工作区（表格 / 表单 / 详情，URL 携带 `/:appCode/:entityCode/:viewType`，
状态可分享）；表格有 schema 生成列、服务端分页、算子白名单筛选条（AND/OR）、列管理抽屉、
命名视图（存 `z_lc_view_config`）、CSV 导出、**CSV 导入向导**（解析 → 列映射 → 前 10 行预览
含类型转换结果 → 提交 → 逐行成功/失败报告）；设计器有实体/字段增删改、必填与重复编码校验、
拖拽排序、客户端 DDL 预览（会按 `fieldLength`/`scale` 还原成 `VARCHAR(128)`/`DECIMAL(18,2)`）、
provision 后展示服务端真实 DDL；`从数据库导入` 走 JDBC 元数据反向认领物理表。
另有 8 个管理页：字典（含字典项主从编辑）、关系、视图配置（结构化 + JSON 双模式）、流水线
（阶段链编辑器）、流程绑定、权限（角色×权限矩阵 + 实时 check）、部署（含日志抽屉）、AI 建模
（草案可逐格改再落库，不自动 provision）。

**视图一共 8 类 + 仪表盘**：表格 / 表单 / 详情 / 画廊 / 日历 / 看板 / 图表 / **交叉表**，
外加把已存图表拼成一页的仪表盘。取数一律走服务端（看板的列、图表的点、交叉表的格子都是
`/runtime/aggregate` 或 `/runtime/shape` 的结果，前端不做二次聚合；`RuntimeCrudController`
只有 list/get/create/aggregate/shape/import/{preview,commit}/update/delete/delete-batch，
**没有** `/runtime/group` 这种东西），交叉表那条链路和它的空值档口径见下文「交叉表（PIVOT）」一节。

已验证（浏览器实测，非静态检查）：网格按 schema 出列、字典列显示 `金卡/银卡` 而非码值、
字典项被软删后**优雅回落成原始码值**、表单按类型生成控件（数字/开关/日期/字典下拉/多行文本）、
UI 建单 → `DETAIL/5` → 回读一致、CSV 导入 2 行全部落库且 `B` 正确解析成 `银卡`、
9 条路由全部渲染真数据、无 JS 报错（只有 antd 的 `destroyOnClose`/`findDOMNode` 弃用警告）。

## 四、还没做的（按优先级）

### ~~P0 视觉 QA 没做~~ → 已解决（2026-09-22）
`z-lc-admin-ui/e2e/browser-e2e.mjs` 用 Playwright + 系统 Chrome、1440×900 真实视口跑，
能截图（`z-lc-admin-ui/e2e/shots/r*-*.png`）也能断言 `boundingBox()`。
上面那句"in-app browser viewport 0×0、截图被拒"只适用于那个内置浏览器，不适用于这条链路。
**这一轮就是靠看图抓到三个真缺陷**（轴标签被 viewBox 裁掉、甜甜圈被 `width:100%` 撑成 940 巨饼、
轴标签把 `<title>` 文本重复进 textContent），只看 DOM 断言是发现不了的。

### P1 生产 MySQL migration
`z_lc_relation.relation_type` 放宽到 `VARCHAR(32)`；确认目标库 `z_lc_app.icon` 已存在。
dev(H2) 侧已经是对的，生产还没有。

### P2 对标优秀平台还差的能力
- 看板、**画廊**、**日历**、**图表**都已落地（列=服务端 group-by，卡=当前筛选下的记录，
  拖卡=一次部分更新，自动进 undo 日志）。看板还没接 saved view 的 `columnMeta` 与卡片字段自选。
- 聚合接口 `POST /api/lc/runtime/aggregate` 已可用（group by + count/sum/avg/min/max，
  列白名单 + 函数白名单，绑字典的分组列自动带 `group_label`），
  并且**时间分桶在 SQL 里做**（`timeGroup` = DAY/MONTH/YEAR，可移植 `EXTRACT`；WEEK 故意不支持）。
- `POST /api/lc/runtime/shape` 把这条链走完：库里拿原始行 → `/aggregate` 那套入参粗糙产出二维分组结果
  → **对象整形语言**（`z-util-expr-obj`）把二维抬成视图要的高维结构（keyBy 成文档、order/limit 取 Top-N、
  group+map 挂组内明细）。形状属于视图不属于实体，所以既不建表也不在前端拼。`shape` 就是一段 JSON 程序，
  入参直接继承 `AggregateQueryDTO`（`ShapeQueryDTO` 只多一个 `shape` 字段），口径与 `/aggregate` 同一个。
  程序写错（未知步骤/槽位取不到）一律 400 原样带回引擎的自解释报错，不退化成空结构。
- **图表视图**（柱/折线/饼/指标卡，手写 SVG 不引图表库）已接，且命名视图**存进服务端**
  （`z_lc_view_config`，`viewType=CHART`，config 里带图表口径 + 当时的筛选条件 + `name`），
  工具栏可「保存为命名图表视图」并从切换器还原。**仪表盘也已落地（2026-09-22）**：
  一个应用可以有多份仪表盘，每份由若干组件拼成，组件存的是命名视图的 **id 而不是配置副本** ——
  改一次口径，所有引用它的仪表盘一起跟着改；视图被删掉时组件显式报「图表视图 #id 已被删除」，
  而不是悄悄少一块（少一块比报错更难解释）。复用 `z_lc_view_config` 且**不加迁移**：
  `viewType=DASHBOARD`、`entity_code='*'`（该列 NOT NULL，而仪表盘是应用级的）。
  这只在两个前提下安全：`namedViews()` 按 `(entityCode, viewType)` **双条件**过滤，
  且 `MetaController.safeViews` 走的是应用级 `listViewConfigsByApp`（所以 `'*'` 行能到前端）。
  少任何一个条件都会串台，浏览器门禁为此专门有一条：应用级的仪表盘行绝不能出现在实体的图表下拉里。
  ⚠ 顺带修掉一个存量缺陷：**没有任何写路径失效 workspace 缓存**，所以表格「保存为视图」之后
  下拉框里根本没有那条视图（`useWorkspace` staleTime 20s + 关了窗口聚焦重拉）。
  现在失效统一放在 `api/viewConfig.ts` 的 `afterWrite`（三个写入口共用，漏一个就是一个假绿灯）。
  **反证做过**：把那行失效注释掉重新构建，真浏览器 E2E 的「保存后不用刷新，命名视图就出现在切换器里」
  立刻变红（`selection=` 为空），另外三条（落库、重载后是本机配置、从服务端还原）照常绿 ——
  说明这 4 条各测各的事，没有互相顶包。
- 表格页脚统计行已接：每列可单选 合计/平均/最小/最大，配置跟着视图一起存（`WorkspaceViewState.stats`，
  进 localStorage 也进 `z_lc_view_config.config`），口径是**当前筛选命中的全部行**而不是当前这一页
  —— 拿分页数据前端加总会让人读出错误结论。实测 footer 合计 197 / 平均 3362.83 与服务端一致。
  **非数值列统计已补**：聚合函数分两档 —— SUM/AVG/MIN/MAX 仍限数值列（文本列 SUM 照旧被拒），
  任意列可算 `DISTINCT`(去重数) 与 `FILLED`(非空数)；填充率由前端拿 `FILLED / group_count` 现算，
  服务端不偷偷做除法。前后端共用同一份 `STAT_FUNCTIONS / NUMERIC_STATS / UNIVERSAL_STATS` 口径。
  高基数分组仍靠 `limit≤500` 兜着，没有"其它"归并桶。
- **交叉表（PIVOT）已落地，是第 8 类视图**：行维度 × 列维度 + 一个指标（`COUNT/SUM/AVG/MIN/MAX`），
  格子、行合计、列合计、总计**全部由服务端出数**（`/runtime/aggregate` 的多维 `groupFields` →
  `/runtime/shape` 的 `pivot` 步骤），前端不做二次聚合。链路口径、那个"无标题列"缺陷和三层反证
  见下文「交叉表（PIVOT）」一节。**这一族还没做的**：① 一次透视多个指标（现在 `metricFn` 只有一个，
  要多指标得存多份视图并排看）；② 行维度分页/虚拟滚动（现在靠 `maxRows`，默认 20、夹到 1–100，
  折掉的行合进一条"其余 N 行"并**照旧进合计**，所以总计不缩水但看不到明细）；③ 交叉表导出 CSV。
- undo/redo 的 per-user 栈已落地；还差 **栈深上限/裁剪策略**（日志只增不删，长期会涨）和
  **选择性撤销**（现在只能按栈回溯，做不到"撤销中间某一条"——需要 Teable 那种 operation 链 +
  invert 注册表）；`assertNotSuperseded` 目前是"目标记录有更新变更就直接拒绝"
- 身份是真·归属不是鉴权：`X-User-Code` 目前谁都能填。等 z-ctc 网关接进来后要改成信任网关头、
  拒绝客户端自报，否则"只撤自己的"这个保证会被绕过
- `view_config.config` 还没收敛成带 JSON Schema 校验的
  `columnMeta{order:float,width,hidden,statisticFunc}`（float 顺序让拖拽重排只写一个数）
- 字段级 / 视图级权限（现在只有 app/entity/role/permission）
- Webhook 订阅（列粒度 + 调用审计 + 失败自动停用）
- 两段式服务端批量导入已接：`POST /runtime/import/preview`（只校验零写入）+
  `/runtime/import/commit`（落库）。校验复用**单条 create 的同一条 pipeline**，所以不会出现
  "预览说能过、真写却失败"。前端向导改成一次 preview + 一次 commit，浏览器实测
  `perRowCreateReqs = 0`（以前 3 行就是 3 个请求）。行数上限 2000，超限直接 400 并提示分批。
  ⚠ 关于原子性要说清楚：commit 的保证是"有一行校验不过就整批不写"（校验级 all-or-nothing，
  确实成立）；它**不是数据库事务** —— `RuntimeCrudExecutor` 自己 `new JdbcTemplate(dataSource)`，
  不受 `@Transactional` 管辖，所以插入中途意外失败走的是**补偿回滚**（软删本批已插入 id），
  结果里用 `rolledBack` 标出来（实测 `applied=false / rolledBack=true / 剩余行数=0`）。
  **这一轮评估过要不要上真事务，结论是先不做**：数据写在 `spring.datasource`、变更日志写在
  `dataSourceLc`，两套事务管理器；只给执行器套 `TransactionAwareDataSourceProxy` 会让
  "数据回滚了、日志没回滚" 这种半提交状态更隐蔽。要上真事务，得连带把日志写入纳入同一个
  事务语境（或统一数据源），那是独立一轮的事，不该顺手半做。
- 导入的行同样进 `z_lc_data_change`，所以批量写入不是撤销的黑洞；同样按人归栈
  （不带 `X-User-Code` 的导入归到 `anonymous`，别人撤不动是预期行为）。
- **批量删除也已接服务端批量端点**：`POST /api/lc/runtime/delete-batch`（整批预检 + 补偿回滚 +
  逐条 undo），前端一次请求带走所有 id。口径、反证与那两条防线各自证明了什么，见下文
  「批量删除改成服务端一条请求」一节。
- 看板拖拽仍是原生 HTML5 DnD（无拖拽预览图、移动端不便），但**移动这件事已经不再只有鼠标一条路**：
  每张卡片加了「移到」下拉，走的是与拖拽**同一个 `move()`**，所以两条路径发出的请求形状一致
  （`KanbanView.test.tsx` 把它钉成断言：`fieldValues` 只能是 `{stage, id}` 两个键）。
  ✅ **整条键盘路径现在是真的能走完了**（缺陷 #32 已修）：浏览器门禁用可信按键注入跑
  `Tab 聚焦 → Enter 打开 → ArrowDown → Enter 选中`，并回读那张卡**当前所在列的 key** 与
  菜单上点的那个名字比对，实测 2/2 轮绿。修的是什么、为什么以前是"看着能用其实不能"，
  见下文「看板「移到」的焦点桥」一节。
- 手动排序列（用户自定义行序）

### 已知语义空洞：字典列不校验值域（**批量导入那半边已经做完了，单条写入口仍未做**）
实测 `stage="NOPE"`（不在字典里）能通过校验并写进去。
后果：网格只能回落显示原始码、看板/统计会冒出幽灵分组。
**故意没有直接改成拒绝** —— 字典项后来被删会让存量数据瞬间"非法"，存量库导入也常带未登记的码。

⚠ 这一节原来写的是"单条 create 与批量导入口径一致（都不说）"，**那句已经过期**：
`RuntimeImportService.collectDictWarnings` 早就把"值不在字典中"作为 **warning** 回传了
（`ImportDto.RowWarning`，preview 与 commit 都带，`MAX_ERRORS_RETURNED` 同级截断，
查不到码表时只 `log.warn` 不拿假 warning 骚扰），界面也接了（`ImportDialog.applyWarnings`）。
量具三处实测在案：API `[15g]`（"但会回一条值域 warning"、"warning 指到行与列"、
"文案里带出真实值"、"warning 不阻止写入"、"commit 也带回 warnings"、"值在字典里时不该有 warning"）、
浏览器 11 段（`/不在字典/` 出现在坏批摘要里、且好批摘要里**没有**）、
vitest `ImportDialog.test.tsx`。

**现在真正的空洞是"两套口径"**：`/runtime/create` 与 `/runtime/update`（含表格内联编辑）**什么都不回**，
所以同一件"值不在字典里"的事，走 CSV 会被警告、走接口不会。界面这一侧够不到那条路
（字典列渲染成受限的 `Select`，全仓 `mode="tags"` 实测 0 命中，人打不出字典外的值），
所以这是**API 契约不对称**，不是"用户能看到的错"。要不要把它拉平，取决于有没有人拿裸接口写数据；
别顺手改成硬校验（理由同上）。

### 这轮被测试/实测抓到的两个"实现了但没做完"的坑（教训型，值得记住）
1. **后端支持 ≠ 前端可用**：DISTINCT/FILLED 后端上线后，页脚选择器仍只对数值列开放，
   文本列看不到入口 —— 接口门禁全绿也发现不了，因为它测的是 API。补了浏览器实测才发现。
2. **`colSpan={2}` + `slice(2)` 把所有统计整体左移一列**：值会显示在错误的列名下
   （看起来"有数"，但归属是错的，比不显示更有害）。现在有一条测试专门钉住它：
   统计行里带值的那个格子的下标，必须等于表头里该列的下标；把 bug 改回去会报
   `expected 1 to be 3`。

### 真浏览器 E2E（重要：它默认打生产构建，不是 dev server）

`z-lc-admin-ui/e2e/browser-e2e.mjs` —— 用系统 Chrome（playwright-core，不下浏览器）+ 1440×900
真实视口跑 **110 项/轮**（这个数是每轮实测出来的，别照抄上一轮；下列举的是各批落地时的形状，
列设置抽屉那批把 104 抬到 110）：schema 出列、字典出标签、内联编辑、页脚统计（真实鼠标点开 antd 下拉）、
看板分组（含卡片归属列的几何检查）、画廊、日历（含星期表头与事件落格的几何检查）、
图表四种画法（含服务端聚合、命名视图落库、环图几何采样、
柱与折线点的"用轴刻度量回来"几何检查，共 20 项）、
**仪表盘**（新建/放组件/落库 `entity_code='*'`/重开还原/引用断掉/应用级行不串进实体下拉，17 项）、
**批量导入**（真点文件选择框上传，11 项：坏批前后库里 id 集合不变、不发 commit、
好批"界面说几行库里就多几行"且值带得对、逐行结果表按单元格判定、字典外的值只警告，
跑完自己清干净），
**批量删除**（11 项：必须先弹 Popconfirm、正好一个 `delete-batch` 请求且一个单删都不发、
作用域字段来自服务端不前端猜、坏批界面说"整批未删除"且库里 id 集合不变、点名是哪条、勾选保留、
好批"界面说删 2 条库里就少这 2 条"、表格里不留幽灵行、成功后清空勾选与徽标），
键盘 Tab+Enter 走「移到」、真实鼠标拖拽、表单填写→详情、管理/设计页渲染、
**5 条请求计数检查**（表格挂载只发一次 + 安静 2s 无重查、日历安静 2s 无重查、
设计器挂载只查一次 + 安静 2s 无重查 —— 这一族是"画面完全正常但自己在转圈"唯一的捕手，见上文 #27/#30）、
无未捕获 JS 异常、无非预期 4xx/5xx。跑法：

    cd z-lc-admin-ui && npm run build && npm run preview:e2e   # 另一个终端
    node e2e/browser-e2e.mjs                                    # 默认 http://localhost:5274

**为什么不用 dev server 跑它**：踩过两次坑。① 我反复改代码后 HMR 会残留多个已卸载的 React 根，
同一页里数出 5 份表格 DOM、48 个本应只有 5 个的可编辑单元格，点上去打到的是死实例；
② 更要紧的是下面这条内联编辑缺陷只在生产构建里复现。

### ✅ 曾经的内联编辑 P1（生产构建里打不开编辑框）：真因是字典 join fan-out

过程值得记，因为它骗了我两轮：连跑 4 次真浏览器门禁 **4/4 第一次点击都打不开编辑框**
（重试第二次才出输入框），数值列 4/4 完全打不开；中间有一次 22/22 全过，差点据此判"修好了"。

第一轮我给出的成因（点击抢焦点 → 编辑框立刻 blur → `onBlur: commit` → 秒关）**被自己的实测否证**：
给挂载/卸载与 blur/commit 打时间戳后，成功样本的时间线是干净的（`mount@1014 activeElement=INPUT`，
没有 blur 没有 unmount）。基于该假设的三次改动（rAF 再聚焦 / `onBlur` 150ms 宽限 /
`onMouseDown`+`preventDefault`）方向本身就是错的，已全部回退，仓库没留半修好的改动。

真因在**后端 SQL**：`DynamicSqlBuilder` 用 `LEFT JOIN z_lc_dict_item … ON item_code = t.field`
取字典 label，而 `z_lc_dict_item` 允许同一 `(dict_code, item_code)` 存在多行（软删后重新添加会撞
唯一索引，所以当初没加 DB 级唯一约束）。重复项把每条业务记录 fan-out 成 N 行 —— 于是**同一行数据
在表格里出现多个 DOM 实例**，点上去打到的是那份不被 React 状态机认领的实例，编辑框自然"打不开"；
而第二次点击之所以有效，是因为中间那次重渲染把行 key 收敛了。修复两处：
- `DynamicSqlBuilder` 字典列改 JOIN 去重派生表（`NOT EXISTS` 取同 code 最小 id 行），
  单测钉在 `DynamicSqlBuilderExtendedTest`（断言 `LEFT JOIN (SELECT`）；
- `DictAdminServiceImpl.assertItemCodeFree` 在写入侧守住唯一性（仅统计 `deleted = 0`），
  注释里写明了违反的后果不是"脏数据"而是分页 total 与页脚统计一起虚高。

修完 `E2E_REPEATS` 连跑多轮均 **PASS 48 / FAIL 0**，脚本里的 `KNOWN_FLAKY` 豁免机制已删除：
留着豁免，就等于亲手造一批"永远绿的检查"。

### ✅ 环图几何 P1：41 条图表单测 + 64 项浏览器检查全绿，只有截图看得见（2026-09-22）

`chartModel.arcPath(radius, inner, start, end)` 画一块扇区是「外弧 → 径向线 → 内弧回程 → Z」，
内弧回程那条写成了 `A ${radius} ${radius} …`（应为 `inner`）。SVG 的 `A` 命令不校验半径能不能
真的过它的两个端点：半径对不上时它**另找一个圆心**把弧补上，于是圆心被挪走、扇区甩到环外面。
图照样画满、照样有 3 块、数值照样对，所以：

- 单测当时只查了「有没有 NaN」「几条 `M`」「扇区数 == 分组数」—— 全部免疫；
- 浏览器门禁只查了扇区个数与 `boundingBox()` 尺寸 —— 也全部免疫；
- 发现方式是**看仪表盘那张截图**（同一份 `PieChart` 在组件里缩略后更明显）。

现在两层各补了一道，且都做过反证（把 bug 改回去跑，确认变红）：

1. `chartModel.test.ts` 在测试里把 `d` 重新解析成命令序列（`arcsOf`），核对每条弧的
   **半径 == 两端点到原点的距离**（`arcGeometryErrors`）以及 `large-arc-flag`。
   反证两次：内弧改回 `${radius}` → 2 条用例红；`large` 恒为 0 → 1 条红（报
   `过了半圆，两条弧都得翻 large-arc-flag: expected [ 0, 0 ] to deeply equal [ 1, 1 ]`）。
2. `browser-e2e.mjs` 的 `checkRing()` 沿扇区描边用 `getPointAtLength()` 采样 240 个点，
   要求量到的半径全部落在 `62–108` 环带内 —— 这是让**浏览器自己的几何引擎**算，
   不是字符串比对。反证：内弧改回 `${radius}` 重新 `npm run build` 后跑一轮，
   单图页与仪表盘那两条各自变红（`radii=62.4~169.0 46.9~108.0 62.0~108.0`，
   最远探到半径 169），**其余 64 项照常绿** —— 这就是"计数型检查看不见几何"的实测证据。

教训写进基线：**看图不是可选项，`boundingBox`/`getPointAtLength` 这类渲染端量出来的数才是视觉 QA 的断言**。
数个数、查 NaN 只能证明"画了"，不能证明"画对了"。

### ✅ 把几何检查推到柱与折线：尺子是图自己画出来的那套网格线（2026-09-22）

上一节那条教训不能只补环图 —— 柱状图原来只查根数和标签，折线只查点的个数与 x 递增，
"高度对不对"整类错误仍然免疫。这轮补了两道，并且**这道检查用的尺子是被测对象自己画出来的**：

- 尺子 = 渲染出来的 `.zlc-chart-grid` 那些网格线：读出每条线 `y1` 与它旁边的刻度值，
  0 那条和最大那条定出一条线性映射 `valueAt(y)`；
- 断言 = 每根柱子顶（`rect` 的 `y`）/ 每个折线点（`circle` 的 `cy`）用 `valueAt` 量回来的值，
  必须等于图上**印出来**的那个数（柱子上方裸数字、点的 `<title>` 末尾数字），容差 `max(0.25, 2%)`；
  画 `—` 的那几行必须量回 ≈0（NULL≠0 那条规矩的几何侧）。

刻意**没有**在测试里重跑一遍组件的 `yOf()`：拿组件自己的公式验组件，等于把同一个 bug
抄进期望值，永远绿。让浏览器/JSX 量、让独立的网格线当尺子，才是外部真值。

**接力关系**：先有的那些检查证明「行级真值 == 图上印的数」（`runtime/list` 现拉的真值），
这一道证明「图上印的数 == 画出来的形」。两段接起来才等于"这张图没有说谎"，缺一段都不算。

反证做三次（每次都恢复后复绿）：
1. 单测（`ChartView.test.tsx`）里把柱高公式换成 `(value / dataMax) * plotH`（用数据最大值代替
   轴最大值，一个"看着很像"的错）→ 对应用例红并报 `printed: 37, measured: 40`，
   同文件**其余 12 条全绿**；
2. 把轴标签与刻度错开一格 → 3 条红（这条顺手证明了尺子真的挂在标签上）；
3. 浏览器侧带着同一个 1 跑生产构建 → `柱状图(合计工时)` 红：
   `ticks=0,2,4,6,8 rows=7.5~8 3.5~3.733 —~0`（比值正好 `axisMax/dataMax=8/7.5`），
   **其余 68 项照常绿**。

⚠ 第 3 次第一次跑出来是 **69 全绿的假绿**，原因值得单独记：我用的命令是
`npm run build 2>&1 | tail -2 && node e2e/...`，**流水线的退出码是最后一个命令（`tail`）的**，
build 挂了整条也照样往下跑，`dist/` 没换 → E2E 打的是上一份正确的 bundle。
从这轮起固定成 `npm run build >日志 2>&1; echo "build exit=$?"`，并且核对新 bundle 的 hash 变了才跑。
同类坑：`mvn test | tail -25` 会把所有 `Tests run:` 行切掉（本轮也踩过一次）。

⚠ 数据集本身的盲区也要说清：那次假浏览器反证里，`柱状图(计数)` 与折线**仍然是绿的** ——
不是检查坏了，是当前那份数据下计数柱的 `dataMax` 正好等于轴最大值，缩放误差为 1。
所以"这条检查在哪些数据上有效"是要报出来的结论，不能只报"它抓到了"。


### ✅ 几何检查推到看板与日历：量的是"谁拥有这块像素"（2026-09-22）

柱与折线那节的道理对"位置类"视图同样成立：看板以前只数列数和列名、日历只数格子数和 chip 数，
**"这张卡/这条事件到底归谁"整类错误仍然免疫**。这轮补了 7 项（69→76/轮），尺子全部来自组件外部：

- **看板**：卡片自带 `data-record-id`，列自带 `data-group-key` + `.zlc-kb-column-label`。三问：
  ① 每张卡所在列的 `data-group-key`，必须等于**从 `runtime/list` 现拉的那条记录**的分组字段值
  （不是从页面别处读回来的，那等于让组件自证）；② 页面卡片的 id 集合 == 真值记录的 id 集合，
  既不丢也不重复挂；③ 用 `getBoundingClientRect()` 要求卡片矩形真的装在所属列的矩形里（±0.5px）。
- **日历**：① 每个日期格**正上方**那个星期字（按 x 找最近的 `.zlc-cal-weekday`），必须等于
  `new Date(Date.UTC(y,m-1,d)).getUTCDay()` 换算成周一起头的字 —— 组件自己手算 `firstDay`，
  而表头是另一份 `WEEKDAYS` 常量，两者对不上就是"整月顶偏一天"；② 每条 chip 所在格的
  `data-date` 必须等于该记录 `due` 的前 10 位，且 chip 矩形落在这一格内；③ 当月带 `due` 的
  记录一条都不能少（防止"只画了前 N 条"被 chip 计数掩盖）。
- **`XLabels` 抽稀**（单测侧，`ChartView.test.tsx` 第 14 条）：15 个点 > 12 时轴标签按密度抽样，
  现在逐个标签要求"x 上存在唯一的点，且那个点的 `<title>` 前半就是标签文字"，并强制最后一个点必须有标签。
  浏览器侧对应改成 `isOrderedSubset` + 逐标签配对，不再按下标一一相等（抽稀本来就会跳着画）。

**接力关系**延续上一节：老检查管"真值 == 印出来的数"，这轮管"印出来的数 == 摆在谁的地盘上"。

反证四次，每次只红该红的那几条（其余照常绿），恢复后复绿：
1. 日历 `firstDay` 改成周日开头 → **只有星期表头那条红**（`错格=2026-08-30:日≠一 …`），
   "每条事件在自己那一格"仍然绿 —— 证明后者量的是记录↔日期，不被表头偏移污染，两者不是重复检查；
2. 看板改成"按下标把卡塞进列"（位置占位，一个看着能跑的错）→ 单测 1 红 + 浏览器 3 红
   （`cards=3:P0 2:P1 1:P2 truth=3:P2 2:P1 1:P0`），而"没丢卡也没重复挂卡"**照常绿** ——
   计数型检查看不见归属，这正是补几何检查的理由；
3. 在卡片上加 `marginLeft:-300` 让它溢到隔壁列 → 归属/矩形两条各自红（`溢出=3,2,1`），
   丢卡检查仍绿；
4. 折线轴标签 x 整体 `+24` → 抽稀那条与浏览器轴标签那条同时红
   （`标签「2026-09-01」没对到任何一个点 x=82`），**同文件其余 12 条绿**。

⚠ 这轮还抓到一条**一直在绿的空断言**：老的拖拽检查写的是 `allText.includes(fromText)`（拿
`body.innerText` 判断），卡片根本没动也照样过；而且 `nth(2)` 可能选中卡片自己那一列，
拖了等于没拖。现在改成"拖完后回读这张卡当前所属列的 `data-group-key`，必须等于目标列且 ≠ 拖之前的值"，
键盘「移到」菜单那条同样强化。**凡是"从整页文字里找一下有没有某个词"的检查，默认按不可信处理**，
它证明的只是那个词在页面上出现过。

### ✅ 导入向导的账要对得上库里的真值（2026-09-22）

老代码有一句会伤到数据判断的谎：`commit` 返回 `applied=false`（写入中断、补偿回滚还没做完）时，
界面照样渲染一条绿色「共 3 行，成功 3 行」——**最难看的那种偏差**：人说没写，界面说写了。
同一个函数里还有三处口径不严：
① 「多少行不合法」取的是 `errors.length`，而服务端 `ImportDto.MAX_ERRORS_RETURNED=50` 会截断，
500 行坏数据只报 50 行；② 只看 `applied`，不看 `insertedCount`/`rolledBack`；
③「CSV 行号」按 `index+2` 猜物理行号，但解析器是 `skipEmptyLines:'greedy'`，中间有空行就错位。

改法（`ImportDialog.tsx`）：**判定只认服务端的计数**（`total - validCount` 说不合法多少行、
`insertedCount` 说真写了多少行、`rolledBack` 说批是否干净），拿不到逐行对应关系时
宁可整批标「未确认」（灰 Tag）也不按顺序猜哪几行进去了；行号统一叫**「数据行」**并明确写
"是第几条记录，不是文件里的物理行号"；被截断的条数直接写成「另有 N 行未列出」。
后端配套一处：`RuntimeImportService.commit` 那句「已导入 N 行」以前用 `validCount`，
改成用真正拿到 id 的行数（校验全过但某行没回 id 时，`validCount` 就是虚的）。

**浏览器侧补了 11 项**（76→87/轮），尺子在组件外面：
- 数据真值：坏批导入前后，`runtime/list` 现拉的**行数与 id 集合一条都不变**；
  好批必须"界面说 2 行、库里正好多 2 行"，而且新行的 `title`/`hours` 就是 CSV 里那些值；
- 网络真值：监听 `POST`，坏批必须只有 `preview`（`importReqs.join(',')==='preview'`），
  好批必须是 `preview,commit` —— 界面文案能编，请求发没发编不了；
- 单元格真值：逐行结果表按 `<td>` 数组读（`[["2","失败","[RequiredCheck] 字段 [标题] (title) 为必填"]]`），
  不在整页文字里找词；字典外的值（`P9`）只进 warning，不许冒充错误行。

反证（注入 `if (false && bad > 0)`，即把预检的早退守卫旁路掉）：**精确红 4 条**
（`不发 commit 请求` 报 `preview,commit`、`坏批计数` 报服务端那句「存在 1 行校验失败，整批未写入」、
`不合法的行被点名` 三行全变成 `未确认`、`字典外的值` 那条），
而 **「库里一行都没多出来」照常绿** —— 这条正是设计成兜底的：服务端 all-or-nothing 还在，
前端的谎只有这一类检查抓不到，两者不是重复检查。恢复文件 + 重建 bundle（核对 hash 从
`B5zWKAMV` 变回 `Cr6HMEvq`）后 87/87 复绿。
另外 3 处口径各自在单测里反证过（`ImportDialog.test.tsx` 5 条）：M1 把 `applied` 当唯一依据
→ 2 红 3 绿；M2 把 `bad` 换成 `errors.length` → 1 红并报出 `50` 与期望 `60` 不符；
M3 把成功条件放宽成 `applied` → 1 红并报 `已导入 2 行`。**50 条截断那条只能靠单测**：
浏览器 fixture 只有 1 个坏行，凑不出 60 个坏行，别声称 E2E 覆盖了这个区间。

⚠ 过程里撞了两个环境坑，都是"生产代码没错、测试环境缺东西"那类：
① jsdom 25 的 `Blob.prototype.text`/`arrayBuffer` 不存在（`node -e` 实测 `undefined`），
生产代码用 `file.text()` 是对的，所以在 `src/test/setup.ts` 里用 `FileReader` 补，不削生产代码；
② antd 给**两个汉字**的按钮中间插空格（渲染成「完 成」），Playwright 的
`getByRole('button', {name:'完成'})` 永远等不到 → 必须写 `/完\s*成/`。
③ 关掉弹窗后再读表格 `innerText` 会得到空串（元素隐藏），要在点「完成」之前读完。

### ✅ 批量删除改成服务端一条请求，并且整批原子（2026-09-22）

上一节末尾那条"真正剩下的事"落地了。以前 `GridView.bulkDelete` → `deleteRecords` 是
**for 循环发 N 个串行 `/runtime/delete`**（代码注释自己写着 "No bulk endpoint exists server-side"），
中途失败就留下"删了一半"，而且没有导入那样的补偿回滚。现在新增
`POST /api/lc/runtime/delete-batch`（`RuntimeBatchDeleteService`，z-lc-core），口径照抄 `/import/commit`：

- **整批预检**：逐条读前像，任一条读不到（`记录 N 不存在或已被删除`）或读取抛异常，就**一行都不删**，
  回 `applied:false, deletedCount:0` + 逐条 `errors`。
  ⚠ 别把这里写成"权限/引用检查"：预检查的是**存在性与可读性**，权限仍在 controller/`resolveEntity`
  那一层（app/entity 粒度），字段级权限还没做（见 P3 能力清单）。
- **写入中途失败 → 补偿回滚**：用 `crudExecutor.restore` 把已删的那几条按预检时读到的前像写回去，
  回 `rolledBack:true, deletedCount:0`。这里**不是数据库事务** —— `RuntimeCrudExecutor` 自建
  `JdbcTemplate`、坐在 `@Transactional` 外面（就是"基础设施两笔未定位账"里的第二条），
  所以补偿是唯一可用的手段；回滚**只有全部还原成功才报 `rolledBack:true`**，
  部分失败时文案是"回滚未完成，请人工核对"（这条不许粉饰）；
- **同一套语义还包括并发窗口**：预检读到过、真删时却命中 0 行（被人抢先删了）→ 抛出去走回滚，
  绝不让 `deletedCount` 比实际多；入参先**按顺序去重**，因为重复 id 的第二条必然命中 0 行，
  会把整批无辜地拖进回滚；
- **undo 日志只在整批成功之后逐条写**（变更日志在另一个数据源 `dataSourceLc`，不跟主写入一起回滚，
  先写就会留下指向"没删成的记录"的幽灵日志），撤销栈里因此不会出现"半批"这种没法还原的条目；
- 上限 `MAX_IDS = 2000`，回传错误 `MAX_ERRORS_RETURNED = 50`（同导入的截断口径，`total` 说真数）；
- ⚠ **坏消息有两种载体，别混**：请求本身不合法（body 为 null / `ids` 空 / 超 2000）走
  `Result.fail(...).code(400)`（HTTP 仍是 200，信封里 `success:false`，前端 `client.ts` 会抛）；
  业务级失败（整批预检没过、写入中断）走 **HTTP 200 + `Result.success(envelope)`**，
  靠 `applied:false / deletedCount / errors / rolledBack` 表达。传输层状态码不承载业务结论。
- ⚠ 契约不对称：`/runtime/delete` 的 `appCode`/`tenantCode` **只认 body**，
  而 `/list`、`/aggregate`、`/import/*`、`/delete-batch` 都接受 query 参数（query 优先）。别按单删的习惯给 batch 只塞 body 以外的地方。

前端一侧：一个请求带走所有 id，**报几条只看服务端回包**（`deletedCount`），不看"我发了几个请求"；
`applied:false` 时保留勾选并点名是哪几条挡住（"有 1 条记录不能删除，整批未删除"）。

**四层各自的数量**：Java 4533→**4536**（z-lc-web 33→**36**，含真 HTTP 集成测试：整批原子、
补偿回滚、cap 拒 2001 个 id（并且空批次也拒、不许假装成功，被拒的请求一行都不动）、undo 逐条登记）；vitest 105→**108**（新文件
`GridViewBulkDelete.test.tsx` 3 条：只发一个 batch 且一发单删都不发、条数来自服务端、
预检没过时不许出现成功文案且保留勾选）；浏览器 87→**98**（+11 项，尺子在组件外面）。
`_e2e/e2e_api_test.py` **179→207（+28 项，`[15h]` 一节）**：这一层是**打在真实部署件（18090 那个 fat jar）上**
的口径，`LcHttpContractTest` 顶替不了（它跑在测试语境自己启动的服务上）。覆盖：整批预检（坏批 `applied:false`
+ `deletedCount:0` + 空 `ids`、文案「整批未删除」且**点名是哪一条挡住**、坏批前后**现拉的 id 集合逐字节不变**）、
一个请求删完两条、重复 id 去重后按一条算**且不回滚**、空 `ids` 与超 `MAX_IDS` 走 400 信封且一行都没删、
软删对 `/runtime/get` 不可见、**逐条 undo**（每次断言 `operation === "DELETE"` 且行数只回来一行）、
别人的栈里没有这批删除（按人归栈）。
- ⚠ 写这一节自己踩的坑：`u1, u2, u3 = undo(), undo(), undo()` 会**把三次请求全跑完**才去读第一次的量数，
  中间态（"一次 undo 只回来一行"）根本量不到 → 必须一次撤销、一次读数地写。第一版就是这么漏掉两次量数的。
- 顺带删掉了 `call2`：`return` 在第二行、底下 23 行是死代码，且 `actor` 参数被静默忽略 —— 挂着比没有更危险。
  ⚠ 同族小坑：`call()` 回三元组、`call_as()` 只回响应体，混用时别再 `[0]`（我第一次就 `KeyError: 0`）。

浏览器那 11 项里最值钱的三条：**网络真值**（监听 `POST`，`deleteReqs` 必须正好是 `['batch']`，
出现任何 `single` 即红）、**数据真值**（现拉 `runtime/list` 的 id 集合，坏批前后**逐字节不变**；
好批必须"少的就是勾上的那两条"，两边都排序比）和**表格真值**（删完 `tr[data-row-key]` 的个数
必须等于刚拉到的 `listRows.length`，幽灵行不许留在表里）。

反证是**两处注入、各自一层抓到**（不写清"在哪些数据上有效"就等于把检查的灵敏度说宽了）：
- **后端 MUTANT-C**：把 `if (!errors.isEmpty()) return` 改成"坏行只跳过、其余照常删"，
  也就是经典的"删了一半"。→ Java 集成测试红在契约那条（`整批未删除` 没了、`deletedCount` 变成部分数）；
  浏览器侧红的是**界面那条**（界面报"已删除"而服务端没答应），而**「坏批：库里一条都没少」照常绿** ——
  补偿回滚把已删的写回去了（`rolledBack:true, deletedCount:0`），库里确实一条都没少，
  这条**本来就该绿**。预检和补偿不是重复检查：预检让失败便宜且诚实，补偿是最后一道防线。
- **前端 M-A**：把 `bulkDelete` 换回 for 循环逐个 `/runtime/delete`，再自己拼一个信封回来
  （"我发了 2 个请求，所以删了 2 条"）。→ `GridViewBulkDelete.test.tsx` **3 条里红 2 条**。
  没红的那条是**已知不敏感**，别当成它也防住了：它断言"条数来自服务端回包"，
  而 mock 的 `deletedCount` 恰好等于请求个数时，拆成几个请求它看不出来 ——
  真正抓到拆请求的是隔壁"只发一个 batch、零个单删"那条（浏览器侧同口径另有一条）。
- **同一处预检，在 API 层（MUTANT-1）**：→ `e2e_api_test.py` 红 **8 条**（197/205），从"整批未删除"文案
  一直到"第二次 undo 再回来一行"，说明这 8 条各自盯着不同的下游后果，不是同一条断言复读。
- **补偿回滚只恢复 `deleted` 的第一条（MUTANT-2，API 层）**：第一次跑**只红 1 条**（204/205）——
  那三条「撤销批量删除」的 `ok()` 只看信封，而**空栈的 `/undo` 也是 HTTP 200 + `success:true`（只是 `applied:false`）**，
  光看信封根本看不出撤销真发生过。补上「这次撤销的确实是一条 DELETE」之后 → 205/207、红 2 条，才咬住。
  ⚠ 同一次反证里「库里确实少了这两条」**照常绿** —— 前面那批坏批已经把这两条删掉了，集合差"对得上"纯属巧合，
  抓到它的是「回包说删了 2 条」和「回包文案与计数一致」。**一条断言的灵敏度是分缺陷的，别把一次红当成"这套都灵"。**
恢复文件 + 重建产物（jar 与 bundle hash 都核对过）后 98/98、2/2 轮全绿，`e2e_api_test.py` **207/207**（exit 0）。

⚠ **这一轮最贵的三个教训，都不是产品缺陷**：
1. **反证的假绿来自构建管道，不来自检查太弱**：`mvn -o -B test -pl z-lc-web` 的 `z-lc-core`
   是从 `~/.m2` 拿的**旧 jar**，源码里的 bug 根本没进测试类路径 → 注入 bug 后 36/36 全绿，
   差点得出"新检查抓不到"的错误结论。必须 `-pl z-lc-core,z-lc-web -am install`。
   前端同族：mutant 先撞上 tsc 错误（`errors: [] as never[]` 让 `.message` 报 TS2339），
   `npm run build` 失败但 **`dist` 保持上一份正确产物**，浏览器门禁用的是干净代码。
   → 反证前必须回读**产物真的换了**（比对 bundle hash / class 里那句中文的 UTF-8 字节）。
2. **我新增的 vitest 文件把整个门禁拖红了**（ImportDialog ×3 + KanbanView ×1 在并发下红）。
   真因不是"antd 弹层容器累积"（我第一版写的就是这个，错的），实测元凶是**我自己用了
   O(document) 的查询**：`getAllByRole('button')` 在这份 GridView 文档上、**每次 DOM 变更之后**
   要 1372–1472ms（jsdom 重算 `getComputedStyle`，stderr 那句 "Not implemented" 就是这条路径），
   `getByRole(name)` 首查 642ms、命中缓存后 8ms，`within(小容器)` 1–36ms，
   裸 `querySelectorAll`+`textContent` 扫 0–1ms。单条用例 12–17 秒 → 饿死并发的 vitest worker
   → 把**本来就存在**的负载竞态暴露成红门。改成收窄查询后每条 ~1 秒。
   顺带修掉 `ImportDialog.runImport` 里那条真竞态（`openAndMap` 等的 `'实体字段'` 是 Steps 的
   静态标题，CSV 还没解析完就点了「导入 N 行」，现在先 `waitFor(/导入 \d+ 行/)`）。
   → 上面 P2.5 里那句"vitest 曾 1 红未复现"**现在有机制解释了**，不再是悬案。
3. **antd 的两个渲染陷阱**：`.ant-table-tbody` 里有一行 `ant-table-measure-row`，
   自带一模一样的 `ant-checkbox-input`（2 行数据能数出 3 个勾选框）→ 必须按
   `tr[data-row-key] input[type="checkbox"]` 收窄；`message` 是挂在 `document.body` 上的
   **单例**，testing-library 的 cleanup 收不掉，上一条用例的吐司会串到下一条
   → `afterEach(() => message.destroy())`。关掉的 Popconfirm 会以 `.ant-popover-hidden`
   留在 DOM 里，定位当前弹层要过滤掉它。

### ✅ 唯一编码撞车：三层防线各自挡住了什么（2026-09-23）

四条 create 路由（`/dict/create`、`/dict/item/create`、`/relation/create`、`/schema/entity/create`
与设计器侧 `/admin/app/create`）撞同一个坑：唯一索引 `uk_*_tenant_code` **不含 `deleted`**，
软删过的行仍占着编码，而预检写成 `.eq("deleted", 0)` —— 自查通过、插入撞索引，
客户端拿到 HTTP 500，消息里带着索引名/表名/列名（等于白送一份库结构）。
现在四条预检都统计**全部行**，命中软删行时明说「此前已被删除」，口径与 house convention
（400 + `IllegalArgumentException`）一致。

三层不是重复检查，这一点**是量出来的**：
- **单测层**（`mutate_duplicate_guard.py`）与 **IT 层**（`mutate_duplicate_guard_http.py`）：
  预检回到 `deleted=0` → 各自点名的用例红；advice 改回吐原始驱动文本 → IT 层**不红**，
  因为它只被 `LcExceptionHandlerTest`（直接调 handler）钉住。
- **部署件层**（`mutate_duplicate_guard_deployed.py`，打 18090 那个 fat jar）：A1–A4 每个只挖掉
  **一个**资源的预检，各自**恰好**翻掉 `[15i]` 里属于它的那一条断言（类级指纹
  `ad64aba25804 / ea9e0ee9e13c / b7c38b87c546 / 75ec7762d1f3`，基线 `c47cb3005a2c`）；
  B（advice 回到裸文本、预检完好）在这一层**零红** —— 这就是"预检遮住了 advice"的实测形状，
  也是 `LcExceptionHandlerTest` 存在的理由。
  `[15i]` 为此从 14 项扩到 **24 项**：每个资源都有一条"软删之后拿同一个 code 重建"的探针
  （活着的重复任何预检都拦得住，只有软删复用时才暴露预检漏没漏）。

### ⚠ 注入脚本自己也会撒谎：这轮抓到四条 vacuous guard（2026-09-23）

"补的测试要同轮注入缺陷自证"这条规矩，前提是**注入脚本本身可信**。这一轮它四次在
"没有任何东西被证明"的情况下报绿：

1. **`unzip -Z` 的详细输出里没有一条匹配项** → 对空串求 md5 → 常量。于是每一轮都报
   `changed=False`，"产物换了没换"这条守卫永远通过。改法是 `zipfile` 逐项匹配，
   **匹配到零项就直接抛错**，宁可炸也不要给出一个常量指纹。
2. **对嵌套 jar 整体求 md5**：那些 jar 每次构建都带新的 entry 时间戳 → 永远"变了"。
   最坏的一次是 B 那一轮：它的结论是**否定式**（"这层看不见"），而 `changed=True` 意味着
   那次注入可能压根没进产物，于是"看不见"这个结论什么也没说。改法：只对 nested jar 里的
   `.class` **内容**求摘要。并且这次先做了一次标定（同一份源码重构建 → 必须相同；
   改一处 → 必须不同）才敢用它。
3. **`mvn` 非零 == "测试抓到了"**：`mutate_connection_leak.py` 第一版把连接的获取挪到
   try 外面，`SQLException` 未声明 → **编译失败**，脚本却报
   `ALL MUTANTS BEHAVED AS CLAIMED`。一个只会因编译失败而红的守卫钉不住任何东西。
   现在判定只认 surefire 报告里"预期那条 testcase 出现 failure/error"，
   构建非零但没有测试判红 = 直接报「等于没证明任何事」。
4. **拿别人的分母报自己的数**：同一支脚本打印的是**基线**的 total，而 B 那一支只问"有没有红"。
   如果 server 起来就崩、E2E 在第 3 节就退出，"零条红"会被读成"预检确实遮住了 advice"。
   现在每轮的分母必须等于基线分母，否则判红。

顺带一条同族的**假红**（这次是我的测试写错，不是产品缺陷）：新加的两条断言用
`queryByText(/已被删除/)`、`getByText(/先别移除这个组件/)` 全文扫，结果
① 匹配到我自己那句澄清「这是加载故障，不代表视图已被删除」，
② 两块引用同一实体的组件各说一句 → `getByText` 命中两个直接抛错。
现在按具体句式匹配（`/图表视图 #\d+ 已被删除/`）或改成结构计数（`.zlc-dash-tile` 个数、
`getAllByText(...).toHaveLength(2)`）。**"扫整页文字找词"式断言在测试里和在生产里一样不可信。**

### ✅ 元数据"读不到"不等于"已被删除"：`meta.read` 逐资源口径（2026-09-23）

`fetchWorkspaceMeta` **从不抛出**：单个端点失败只被记进 `degraded`，所以 react-query 的
`isError` 永远抓不到这类故障，出参里的 `entities`/`views` 就是一支空数组。页面把它当"没有数据"，
于是连接池被吃空的那次故障在界面上长成了「实体「task」不存在，可能已被删除」。
`DashboardPage` 更狠：它按组件逐块说「图表视图 #id 已被删除」，还挂着"移除这个组件"的按钮 ——
**用户会因为一次接口故障亲手删掉自己仪表盘里的图。**

三处改动：
- `WorkspaceMeta` 从"一个 `entitiesRead`"扩成**逐资源**的 `read: Record<MetaResource, boolean>`，
  口径是 `wasRead()`：rejection（记进 `degraded`）和"回了 `success:true` 但 `data:null`"
  都算没读到 —— 后者不是失败，但拿 `.length` 就是崩，所以出参一律先归一成数组。
- 新增 `unreadReason(meta, resource)`：页面据此区分"哪个接口挂了"和"接口回了个不是列表的东西"。
- bundle 路径改成**逐资源补拉**（`bundledOr`）：`MetaController#bundle` 给每个子资源各套一层
  try/catch + `log.warn`，所以部分失败的 bundle 照样回 `success:true`，只是**那个 key 直接不在**。
  "bundle 成功 == 什么都读到了"正是那条谎的来源。`fieldTypes` 额外把空数组也当缺失
  （`treatEmptyAsMissing`）：bundle 里的 `fieldTypes: []` 是"没提供"，不是"这库没有字段类型"。
- 两个页面各加门禁；`ChartTile` 的占位牌多一个 `description`，加载故障时换成
  「图和视图都还在…先别移除这个组件」，真删了才说"已被删除"。

`e2e/mutate_degradation_guards.py` 的 M1–M5（基线 122 全绿，恢复后复绿，源码字节校验）：
M1 拆仪表盘整页门禁 → 红 **1**；M2 去掉 `treatEmptyAsMissing` → 红 **2**（这一条注入的是
**修复前的真实代码形状**，不是为测试编的稻草人，而那个"看着可有可无"的参数是承重的）；
M3 `wasRead` 不再看返回的是不是列表 → 红 **3**（跨 `meta.test.ts` 与 `WorkspaceViewPage.test.tsx`）；
M4 拆 workspace 门禁 → 红 **3**；M5 占位块无条件说"实体已被删除" → 红 **1**。

### ✅ 管理页列表：一次失败被画成"这个应用本来就没有"（2026-09-23）

上一节只修了 workspace/dashboard 两处，优先级 9 里那句"8 个管理页还没逐页查过"这轮查完了，
结论是**每一页都在撒同一种谎**，而且比元数据那两处多一类：

- **空态吞掉故障**：`RelationsPage` 之类是 `catch { message.error(...) }` + 一支空数组，
  于是表格里写着「该应用还没有关系定义」—— 提示用户去新建数据，而真因是接口挂了。
- **失败的刷新不清行**：`DictsPage` 的 `StateBlock` 那行字面写着 `isError={false}`，
  读失败后旧数据原样留着。**表比故障前更"健康"**，这一类比空态更糟：它让人继续相信屏幕上的行。
- **三秒就消失**：`utils/errorBus.ts` → `ErrorBridge` 是全局 toast，故障提示活 ~3s，
  而那支误导性空表格会挂在屏幕上几分钟。故障必须有一个**不消失的**位置。
- **没选应用也说"没有"**：应用列表自己读失败时 `appCode` 为空，各页于是齐声说
  「该应用还没有 X」，实际是"我连有哪些应用都不知道"。

`_scope.ts` 里的 `useResourceList` 把一次读取拆成**五种结果**：
`idle`（还没选作用域）/ `loading` / `error` / `empty`（读到的是**数组**且长度为 0）/ `ready`。
关键口径三条：
① `client.ts` 是 `return parsed.data as T`，所以 `success:true` + `data:null` 在类型上"是数组"、
运行时会崩在 `.length` 上 —— 因此 fetch 签名收 `T[] | null | undefined`，
**只有真拿到数组才算读到**，`data:null` 归到 `error`（「接口没有返回列表」）；
② `state` 是**派生**的（存储的读取状态 × `rows.length`），因为调用方会乐观改行
（流水线启停、字典项增删），存死一个 state 就会和 `setRows` 打架；
③ 每次读取一个 `seq`，`mine !== seq.current` 的结果直接丢弃 —— 换应用时慢一步到达的旧响应
会把新应用的列表盖回去，并且**旧应用的 error 会挂在新生面上**。
`state` 派生 + 失败清空 `rows`，两件事合起来才让"看着健康"无处可藏。

页面侧：每页表格上方一个 `ListBanner`（复用 `ErrorBlock`，带真重试），
空态文案走 `listEmptyText(state, label)`，`AdminScaffold` 多接一个 `appError` 并在
`onRefresh` 里同时重拉应用列表和本页。`useAppSelection` 也换成跑在 hook 上，
应用选择器自己的故障不再是**一个空下拉框**。

`e2e/mutate_admin_list_guards.py` 八个注入（基线 135 全绿 → 注入 → 恢复后 135 全绿，源码字节校验）。
每个注入的都是**修复前的真实代码形状**，不是为测试编的稻草人：

| 注入 | 改回的样子 | 红的用例数 |
|---|---|---|
| A | `isError={dictState === 'error'}` → 字面 `isError={false}`（修复前原样） | **1** |
| B | 失败分支不清 `rows`（= 修复前 `catch` 里什么都不做） | **2** |
| C | 接口回什么都当列表（`data:null` 静默变空列表） | **1** |
| D | 去掉过期响应守卫（慢的旧响应盖掉新应用） | **1** |
| E | 撤掉故障横幅（故障只剩三秒就消失的 toast） | **3** |
| F | 空态不再区分故障（退回 antd 默认「暂无数据」） | **2** |
| G | 没选应用当成「读完了，什么都没有」 | **3** |
| H | 应用列表的故障不外露（picker 只是空着） | **1** |

G 的红是 3 条而不是 1 条，**这不是重复检查**：那三条都靠 `idle` 这个口径，其中
「应用列表读不到时要说"没有读到"」是**级联**（picker 读不到 → `appCode` 为空 → 本页本来就是
`idle`），所以拆掉 idle 会连带它红。第一条跑出来就多红两条时，改的是**预期集合**并写下理由，
不是把测试删掉或把守卫放宽。

⚠ 同一轮第四次犯"vitest 全绿但类型是坏的"：我把 `listEmptyText` 从 `_shared.tsx` 挪进
`_scope.ts` 的脚本顺手删掉了 `import type { ListState }`，而 `ListBanner` 还在用它 →
TS2304。此时**注入脚本已经报过"135 tests, 0 failed"**（运行时不受影响，judgment 基于同一份坏树），
是 `npm run check` 第一步（`CHECK_EXIT=2`）才抓到的。写脚本改源码时，"删掉一行 import"必须单独核对，
别指望测试兜住。

### ✅ workspace 侧的同一类谎：侧边栏 / 概览统计位 / 实体候选（2026-09-23）

上一节把管理页接上五态口径，这一节是 `fetchWorkspaceMeta` 这条链**剩下的三个出口**（就是优先级 9 里
定位未修的那三处）。共同点仍然是它**从不抛**：单资源失败只记进 `degraded`，react-query 的 `isError`
永远抓不到，出参是一支空数组，于是页面替用户宣布"这个应用什么都没有"。

- **侧边栏**：`isLoading` / `isError` 两支之后直接 `entities.length === 0` →「该应用还没有实体。」
  外加一个"打开 Schema 设计器"的链接 —— 把人支去建实体而不是去查接口。计数位写死 `实体 (0)`。
- **应用概览**：四个 `Statistic` 无条件报数字，「没读到」被报成"这个应用有 0 个实体、0 个字段"。
- **实体候选**：`useEntityOptions` 拿 `meta?.entities ?? []`，元数据降级时**五个管理页**的下拉静默变空
  （正是上一节在列表上刚修掉的那类，换了个出口）；`AiModelingPage` 顺着 `entityOptions[0]`，
  取不到就 `message.warning('该应用还没有实体')` —— 同一句谎从 toast 里说出来。

口径收成一个形状：`useEntityOptions(appCode): EntityOptionSource{options, read, reason, retry}`。
`read` 直接取上一节已有的 `meta.read.entities`，`reason` 走 `unreadReason(meta, 'entities')`，
下拉空态由 `entityNotFoundContent(source)` 生成 —— **读到但确实为空**交回 antd 默认「暂无数据」，
**没读到**则说"实体列表没有读到：失败接口: schema"。侧边栏与概览共用同一个 `read` 门禁，
没读到的统计位写**破折号而不是 0**，并各留一个真会 `refetch` 的重试。

`e2e/mutate_workspace_entity_guards.py` 八个注入（基线 147 全绿 → 注入 → 恢复后 147 全绿，
四份源码字节校验）。每个注入的都是**修复前的真实代码形状**，不是为测试编的稻草人：

| 注入 | 改回的样子 | 红的用例数 |
|---|---|---|
| A1 | 侧边栏 `!entitiesRead ? (` → `false ? (`（只分"空数组就说没有实体"） | **3** |
| A2 | 计数位 `实体 ({entitiesRead ? entities.length : '—'})` → `实体 ({entities.length})` | **1** |
| A3 | 横幅里的重试 → `onClick={() => undefined}`（看着能修，点了什么也不发） | **1** |
| B1 | 概览 `count()` 门禁 → 无条件 `=> value` | **1** |
| B2 | 概览 `emptyText: read?.entities ? (` → `emptyText: true ? (`（改动前原样） | **1** |
| C1 | `read: Boolean(meta?.read.entities)` → `read: true` | **3** |
| C2 | helper 永远 `return undefined`（下拉又变回「暂无数据」） | **1** |
| D1 | `entityGateText` 的三元 → 直接 `fallback`（改动前那两行写死的提示） | **1** |

C1 红 3 条不是重复检查：`read` 同时被 hook 用例、helper 用例和 AI 建模的 toast 用例吃 ——
拆掉一处门禁让三个消费点各自变红，恰好说明它们吃的是同一个口径。

⚠ **这一轮的新测试先骗了我三次，全部记下来，因为测试的假绿比产品缺陷更难被发现**：

1. **不装 fetch 也能绿**：`stubBackend()` 一开始没进 `beforeEach`，Node 真实 fetch 打到相对 URL 上
   直接 reject → 所有接口一起"降级"，第一条用例照样绿。它测的是"网络全挂"，不是我要的那个故障。
2. **`waitFor(read === false)` 是恒真起点**：hook 的 `read` 在加载完成前本来就是 `false`，
   断言在什么都没发生时就是通过。改成等 `reason` 里出现 `失败接口: schema` / `不是列表` ——
   等的是**这个具体原因**，不是一个恰好相同的布尔值。
3. **等 A 却断言 B（同一个毛病犯两次）**：AI 建模那条先等 `'数据分析'` 出现在 DOM 就点击，
   而 `useAppSelection` 的 `appCode` 是从 `/app/list` 兜底填进来的 —— 此时按钮是 `disabled`，
   点击落空；全量跑负载高时它以"预期外的红"暴露。修法不是拉长超时，是**去掉竞态**：
   路由带 `?appCode=crm`，首帧即选中态。同一用例的两半断言还必须落在**同一次读取**上
   （toast 3 秒自收，分开读会让反断言在空串上"通过"），且只读 `.ant-message-notice-content`
   —— 整页 `textContent` 会被侧边栏那种"也写着没读到"的元素喂饱。

⚠ **注入脚本自己也漏了一类证据，一并补了**：`judge()` 原先命中 `missing` 就返回，把同轮的 `extra`
藏起来（A1 第一次跑因此少看一半）；而且它只打印标题、不留原始输出，于是"`ImportDialog` 冒出一条
预期外的红"这件事**没有失败样本**可循 —— 本仓库已经写过"没有样本就是瞎试"。现在 missing/extra
一起报，且见红就把 stdout+stderr 留档到 `$TMPDIR/zlc_mut_ws_ent_logs/<注入名>.log`。
那条 ImportDialog 的红此后 **29 次全量跑未复现**（同一注入轮剩下的 6 次 + 无注入 3 次 + 带 A3 注入
2 次 + 连跑 8 次 + 完整注入轮 10 次），按"未复现的一次性异常"记录：**不当作已修，也不加豁免**，
下次出现时留档会给出那句失败本身。

### ✅ 记录侧的同一类谎：画廊 / 日历 / 变更历史（外加一个四道门禁全瞎的重查缺陷，2026-09-23）

前两节把管理页与 workspace 侧接上了五态口径，这一节是**已定位的最后三个出口**（交接清单里那条 🔴）。
它们吃的是同一个 `/runtime/list`：接口 500 之后

- **画廊**：`<Empty description="暂无数据" />` 无条件渲染 —— 同一批记录，切到表格看是"记录没有读到 + 重试"，
  切到画廊就是"这个实体没有记录"。
- **日历**：整页没有状态层，失败只有一句 `message.error('加载数据失败')`，三秒收走；
  收走之后留在屏幕上的是**画满 42 个格子、一个事件都没有**的月历。比"暂无数据"更坏，
  因为它长得和"这个月确实没安排"一模一样，而后者是一个**看起来像读到的结论**。
- **变更历史**：`entries.length === 0` 无条件说「这个实体还没有数据变更」—— 把"我没查到"说成
  "没人动过数据"，会让人放心地继续改（撤销面板正是查"谁动过"的地方）。

三处都接上已有的 `StateBlock`（`isLoading / isError / isEmpty / children` 一支），
措辞沿用同一口径：**没读到 = "X 没有读到"，读到且为空才 = "还没有 X"**，重试必须真发一次请求。
另加一条容易忽略的：`loading` 初值是 `true` —— 组件挂载即发请求，首帧该是"在读"而不是"还没有记录"，
否则接口慢的时候第一帧就在撒谎。日历的空态是**故意不给**的（`isEmpty={false}`）：
一个月真的没排期时，42 个空格子就是正确答案，不该被一句"还没有"替换掉。

`mutate_record_fetch_guards.py` 十二个注入（基线 163 全绿 → 逐个注入 → 恢复后 163 全绿，三份源码字节校验）：

| 注入 | 改回的样子 | 红的用例数 |
|---|---|---|
| C0 | 日历 `monthStart/monthEnd` 不再 `useMemo`（**改动前的真实写法**） | **1** |
| G5 | 画廊依赖数组写成 `[...conditions]`（同一类身份不稳定） | **1** 硬 + 0–3 连带 |
| G1 | 画廊 `isError={false}` | **3** |
| G2 | 失败只 `message.error('加载数据失败')`（改动前原样） | **3** |
| G3 | 画廊 `isEmpty={true}`（读到记录也说"还没有记录"） | **4** |
| G4 | 横幅重试 → `() => undefined` | **1** |
| C1 | 日历 `isLoading`/`isError` 双双写死 false（改动前无条件画网格） | **3** |
| C2 | 日历失败只弹 toast（改动前原样） | **3** |
| C3 | 日历横幅重试 → `() => undefined` | **1** |
| U1 | 变更历史 `{query.isError ? (` → `{false ? (` | **2** |
| U2 | 空态不看读取结果（改动前原样） | **1** |
| U3 | 面板重试 → `() => undefined` | **1** |

#### 写这轮测试时打出来的第二个缺陷：页面自己在那儿转圈重查

新测试文件第一次跑**挂了 12 分钟不退出**。不是测试写错了：20 行探针一量，
`CalendarView` 挂载后 300ms 内发了 **43 次** `/runtime/list`，同窗口内画廊是 1 次。
真因就在读代码时觉得"没问题"的那两行 —— `monthStart/monthEnd` 每次渲染现算，
`monthConditions` 跟着换身份，`loadPage` 的 `useCallback` 跟着换，`useEffect` 跟着再跑一次：

    const monthStart = currentMonth.startOf('month');   // 每轮渲染都是新对象

修法是 `useMemo(..., [currentMonth])`。真正的教训是**这类缺陷对界面断言完全隐形**：
每次重查返回的是同一份数据，格子照在、贴片照在、点击照跳转，所以同文件里另外 14 条 UI 断言
和浏览器门禁那 98 条检查**一条都不会红**。只有数请求条数才看得见 —— 于是加了
`一次挂载只查一次` 2 条（挂载后不再补、换月/换筛选各只补一次），并在浏览器侧的日历步骤里
`waitForTimeout(2000)` 数 `/runtime/list` 的增量（分母 98 → **99**）。

⚠ **jsdom 里还埋着一个坑**：fetch stub 若只交微任务，配上真循环会把计时器饿死 —— 测试是**挂住**
而不是变红，12 分钟才被我手动中断。stub 现在固定 `await new Promise(r => setTimeout(r, 0))`
让出一拍，将来同类回归会以"红"而不是"卡住"的形式出现。

⚠ **重查注入的"红哪些条"本身是不稳定的**，这一条是被取证逼出来的，不是想出来的：G5 两轮分别
发了 83 / 110 次查询，第二轮比第一年多红了"记录没有读到"那条 —— 循环把画面钉在哪一支，
取决于断言落在第几次请求的间隙上。把偶然写进硬预期，下一轮就会以"你的测试是空的"的名义误报。
所以 C0/G5 只硬要求 `LOOP_*`（纯计数、对循环稳定）红，其余按**连带红**报告、不计入判定，
理由写在条目里；**测试本身一条都没为这个放宽**。反过来 G3 的 4 条是稳定的，
因为 `isEmpty={true}` 永远掐掉 children 那一支 —— 连 `LOOP_GAL` 也被掐（它第一句是
`waitFor(3 张 .ant-card)`：先确认真画出了东西才开始数请求，只数请求会放过"发对了但没画"）。

⚠ **注入脚本的证据也补了两处**：① `--reporter=json` 下 stdout 只有 96 字节，
失败样本（`failureMessages`）只在 JSON 报告里，上一轮 G5 的"多红"因此只能猜分支 —— 现在两份都留档；
② `judge()` 支持第四项 collateral，并区分打印 `RED (预期)` / `RED (连带)`。

### ✅ 设计器侧栏的同一类谎（外加一条"测不出来的守卫"的取证，2026-09-23）

上一节收尾扫描找到的最后一处出口（当时的判断依据：`grep` 出所有 `catch → message.error` 而不接
状态层的读点）。`/designer/:appCode` 左边那一列实体清单改动前是三句谎叠在一起：

1. `catch (err) { message.error(err instanceof Error ? err.message : '加载实体失败') }` ——
   三秒后 toast 自己收走，底下那台 `<List locale={{ emptyText: '还没有实体' }}>` 永久挂着"还没有实体"；
2. `loading` 初值 `false` —— 慢接口期间**第一帧**就在说"还没有实体"；
3. `<List loading={loading}>` 的 spinner 只是**盖在**空态上面一层，不挡住那句谎。

这处比管理页那几处后果重：**设计器正是决定"这个应用有没有实体"这句话真假的地方**。用户照着
"还没有实体"去点「+」新建一个其实已经存在的实体，就会撞唯一编码（#21 那一族：500 + 一份库结构）。

`mutate_designer_entity_guard.py` 七个注入（基线 170 全绿 → 逐个注入 → 恢复后 170 全绿，源码 md5 字节校验）：

| 注入 | 改回的样子 | 红的用例数 |
|---|---|---|
| D1 | 失败只弹一条 toast（**改动前原样**，逐字从那次 Edit 的 old_string 抄回） | **3** |
| D2 | `isError={Boolean(loadError)}` → `isError={false}`（改动前根本没有错误这一支） | **3** |
| D3 | `isEmpty={true}`（读到实体也说"还没有实体"） | **4** |
| D4 | `isEmpty={false}`（真·0 实体时也什么都不说） | **1** |
| D5 | 横幅重试 → `() => undefined` | **1** |
| D6 | `isLoading={loading}` → `isLoading={false}`（改动前 spinner 不挡空态） | **1** |
| D7 | `useCallback(…, [appCode])` → `[appCode, entities]`（把自己写的 state 写进自己的依赖 → 自激重查） | **1** |

DesignerPage 还没入库，git 里没有"改动前"可对照，所以 D1/D2/D6 的旧形状是从本轮那次 Edit 的
`old_string` 里**逐字**抄回来的，并在注入脚本顶部写明了这一点 —— 免得下一轮把它当成替测试编的稻草人。

#### ⚠ D6 第一次跑出来的是一条**空的测试**，而且修不好

D6 原本注入的是"把 `loading` 初值改回 `false`"（改动前真正的写法），跑完全量 **0 条变红** ——
也就是说我那条"挂载到响应回来之间那一帧说的是在读"**没在钉它声称的东西**。原因不是断言写少了，
是这件事在 jsdom 里根本看不见：RTL 的 `act()` 会先把挂载 effect 跑完，`reload()` 一进来就
`setLoading(true)`，等测试拿到 DOM 时"首帧"早就过去了。真浏览器里 `useEffect` 在 paint 之后才跑，
初值 `false` 确实会闪一下"还没有实体"，但那是 sub-frame 竞态 —— 测不出来就别按"有测试守着"记账。
于是：初值 `true` 作为代码约定保留（注释里写着为什么），测试的断言与注释改成只声称它真正钉得住的东西
（`StateBlock` 的 `isLoading` 那一挡，D6 换注入后即红）。**这一条按"未覆盖"记在文档里，而不是抹掉。**

顺带把设计器在浏览器层的唯一断言也修了：原来是一遍 `body.innerText.includes('字段')` ——
字段表格表头和右侧输入框标签到处都在说"字段"，侧栏写着「还没有实体」也照样绿。现在是四条打在侧栏自己身上：
按 `248px` 那张卡片定位、断言列出的是**真的那一个实体**（`任务`，长度恰为 1）、侧栏文字里两句谎都不许出现、
本次导航只新增 1 次 `entity/list` 请求 + 安静 2s 增量为 0（`beforeLoad` 快照是必要的：前一步 `/db-import`
自己也会拉实体清单，拿整场累计值当证据会假红）。

### ✅ 表格视图的计数守卫，以及一支"跑不完所以没结论"的注入脚本（2026-09-23）

记录侧那一轮留下一个收尾判断（缺陷 #27 的适用面）：**只有手写的 `useCallback + useEffect` 取数
会自己把自己点起**，react-query 那几处不受影响，因为 queryKey 按**内容** hash，数组换身份不会多打一次。
按这个判断扫了一遍还剩谁：看板行、图表 `chartQuery`、页脚统计 `grid-stats`、引用字段候选、变更历史
全部走 react-query —— 全仓库只剩 `GridView.load` 一处手写，而它的依赖里正好躺着两个易变身份
（`state.sorts`、父组件传进来的 `conditions`）。所以这一轮只补这一处，没给那五处补计数用例
（**给它们补是在测 react-query，不是测我们**）。

新增第 9 条用例：先 `waitFor` 真的画出行，再断言 `/runtime/list` 的调用数 == 1，然后安静 200ms 再断一次
（只数请求会放过"发对了但没画"；先确认画出来才开始数）。浏览器侧同口径 2 条：挂载 delta === 1、
安静 2s delta === 0（分母 102 → **104**，两轮都绿）。

`mutate_grid_refetch_guard.py` 两个注入，每个跑两遍，**零连带红**（基线 9 全绿 → 恢复后 9 全绿，
`GridView.tsx` md5 字节校验 `31c33a2b…`）：

| 注入 | 改回的样子 | 红的用例 | 实测样本 |
|---|---|---|---|
| V1 | `state.sorts` 在依赖里摊开 `[...state.sorts]` | 只有第 9 条 | `挂载之后不该有第二次查询: expected 3 to be 1`（四遍都是 3） |
| V2 | 父组件的 `conditions` 摊开（和记录侧 G5 同形） | 只有第 9 条 | 同上 |

为什么这一支**不许**任何连带红（记录侧那两处只能允许）：`StateBlock` 这里写的是
`isLoading={loading && rows.length === 0}`，加上 `load` 里的 `requestId` token 守卫（迟到的旧响应直接丢弃），
重查期间**已经画出来的行一直留在 DOM 里**，那 8 条邻居断言没有一支会因此换分支。
日历/画廊不一样，循环能把画面钉在空态那一支上，所以那边只能按连带红报告。
"两遍"是为了让这句话可证伪：将来真冒出连带红，脚本会直接判失败并写明"不许加白名单了事"。

#### ⚠ 脚本第一次是按全量跑的，结果不是红而是没有结论

第一版脚本沿用记录侧的写法（跑全量、分母 171）。V1 挂上去之后 **vitest 13 分钟没跑完**，我只能 kill；
kill 之后必须手工把 `GridView.tsx` 写回原始字节并核对 md5、再杀掉残留的 vitest worker ——
一支反证脚本把树留在"不知道改没改回来"的状态，比不跑更糟。
同一个注入按**文件**跑，几秒给出 `expected 3 to be 1`。**跑不完一整轮不等于通过，等于没测**，
所以 churn 类注入的口径定成：按测试文件跑，分母用该文件自己的用例数。
顺带把"分母"升级成"分母 + 用例标题清单"（脚本里的 `COLLECTED` 那 9 条）：
只核对数量的话，邻居被删掉也会凑出正确的 9，而"没有连带红"这句话就变成假的。
这条也记进缺陷表 #31。

### ✅ 看板「移到」的焦点桥：界面写着"键盘可用"，而那条路径是死的（2026-09-23，缺陷 #32）

**怎么撞上的**：把浏览器门禁里那条键盘检查从"能打开菜单"加强成**全程键盘**（打开之后
`ArrowDown + Enter` 选中，不让鼠标代劳），第一次跑就 2/2 轮稳定红：

    FAIL  键盘全程把卡片移到了目标列（打开与选中都不碰鼠标）
         << id=1 from=P0 to=P0(紧急) expect=常规 focusAfterOpen=tag=BUTTON inDropdown=false

`to=P0` 就是"什么都没发生"。旧检查只断言"按 Enter 能打开菜单"，所以这条谎已经绿了很久 ——
而卡片上的 tooltip 一直写着「移到其他列（键盘可用）」。**"能打开"被当成了"能用"**。

**根因是读库源码定下来的，不是猜的**（三处互证）：
- `rc-dropdown` 里 grep 不到任何 keydown 处理 —— 它只管开关与定位；
- antd 5.29.3 的 `autoFocus` **只实现于 `dropdown-button.js`**，普通 `Dropdown` 打开时
  焦点原地不动（还在触发按钮上）；
- 方向键/Enter 那套全在 `rc-menu`：根节点是 `<ul role="menu" tabIndex=0 data-menu-list
  onKeyDown=useAccessibility(...)>`（垂直菜单 `DOWN→下一项`），`MenuItem.js` 里
  `onKeyDown` 判 `e.which===ENTER` 才调 `onClick`。**焦点不进这棵子树，一条都收不到。**

**修法是 `KanbanView.tsx` 里那座三层小桥**（`popupRender` 包一层 `MoveMenuPortal`）：
① 弹层挂上来把焦点交给菜单根节点（让 rc-menu 自己的无障碍路径生效，不重造方向键）；
② `destroyOnHidden` —— 否则弹层关着也留在 DOM，第二次打开不再跑那个 effect；
③ `onOpenChange` 记下触发按钮，关闭时把焦点还回去（不然一次移动之后键盘用户丢了位置）。

⚠ **第一版桥在真浏览器里是绿的、在 jsdom 里是红的**，红得很有迷惑性："打开后焦点没进菜单，
而在 button[移动 甲 到其他分组]"。我差点据此判定桥无效。逐帧量完才发现机制：
effect 跑的那一刻弹层子树 `isConnected === false`（还没接进 document），
而**对未连接的节点 `focus()` 是空操作**；下一帧 `isConnected=true`，`focus()` 立刻把
activeElement 从 BUTTON 换成 UL。所以那句 focus 要推到 `requestAnimationFrame` 里
（rc-menu 自己挪焦点也走 raf，同一帧抢在它前面反而会互相覆盖）。
这条只有单测层能暴露 —— 两层的时序不一样，谁也别替谁背书。

⚠ **一段明确按"未覆盖"记账的**：ArrowDown 在 jsdom 里根本走不动。`rc-menu` 挑可聚焦项要过
`rc-util/Dom/isVisible`，它读 `offsetParent` / `getBoundingClientRect`，jsdom 没有布局引擎，
两者恒为 `null` / `0`，于是**任何菜单项在 jsdom 里都"不可见"**（真量过：按 Down 之后焦点钉在 ul 上）。
所以"方向键换项"这一步只有浏览器门禁那一条在守（可信按键注入）。
单测里那句 `item.focus()` 是直接把手指放在第一项上，绕开了不可用的那段，不算证明方向键。

**反证：`e2e/mutate_kanban_keyboard_guards.py`，四个注入 × 各两遍**，预期红集合与实测逐一吻合，
**零条预期外的红**，恢复后 7/7 绿：

| 注入 | 摘掉的那一层 | 实测红（两遍一致） |
| --- | --- | --- |
| M1 | effect 找到了菜单却不给焦点 | 焦点交给菜单 / 关掉还焦点 / 第二次打开（3 条） |
| M2 | 关闭时不还焦点 | 只有"关掉还焦点"（1 条） |
| M3 | `destroyOnHidden={false}` | 关掉还焦点 / 第二次打开（2 条） |
| M4 | 整座桥摘掉（= 修复前原状） | 三条焦点用例全红 |

四个注入的红集合**互不相同**，这正是"三层不是重复检查"的证据。
M3 连带红"关掉还焦点"是机制上的必然，写脚本时先按推理写成预期、跑完再对
`/tmp/zlc_mut_kb_logs/*.vitest.json` 里的 failureMessages 核过：那条用例先等
`.ant-dropdown-menu` 从 DOM 消失，而"消失"本身就是 `destroyOnHidden` 的行为。

⚠ 顺带一条自己的测试 bug：想用 `fireEvent.click(卡片文本)` 关掉菜单，结果关不掉 ——
`rc-trigger` 的 outside-click 听的是 **mousedown**。第一次因此把"关掉后焦点在哪儿"读成了假现象。
改成 `fireEvent.mouseDown` 后行为才对得上。

### ✅ 列设置抽屉：改一个宽度把整列挪到第一列，排两次互相覆盖（2026-09-23，缺陷 #33）

找它的手法沿用 #32：**扫界面上的"承诺语"**，命中 `ColumnManagerDrawer.tsx` 里那句
「拖动排序，使用 `空格` + 方向键可键盘排序」。这一次我先入为主的假设是"键盘那句又是谎"，
**结果被实测证伪**：真浏览器里 `Tab → Space → ↓ → Space` 确实把两列换了位置（`B2`）。
谎不在键盘上，在**顺序**上——所以下面记录的是量到的东西，不是猜到的东西。

**三个症状，全是同一轮真浏览器读数（每轮现建应用，`columnMeta` 从默认的 `[]` 起步）**

| 操作 | 修复前的表头 | 修复前的抽屉 |
| --- | --- | --- |
| 只给"工时"输一个宽度 233 | `工时\|标题\|优先级\|截止日期` ← **它跳到第一列了** | 不动 |
| 键盘排序一次（prio↓） | `标题\|工时\|优先级\|截止日期`（排序本身是对的） | `title,prio,hours,due` ← **不反映** |
| 再键盘排序一次 | 与第一次**完全相同** ← 上一次的被整份覆盖 | 同上 |
| 修复后同样的三步 | `标题\|优先级\|工时\|截止日期`（宽度 233 落在第 3 列自己身上，实测 `th` 宽 233）/ `标题\|工时\|优先级` / `工时\|标题\|优先级\|截止日期` | `title,hours,prio,due` ← 跟着变 |

**根因只有两处，且都在抽屉这一个文件里**

1. `rows` 是 `resolvedFields.map(...)` —— 抽屉永远按 **schema 顺序**列自己；
   而表格读的是 `projectColumns(resolvedFields, columnMeta)`，按 **columnMeta 顺序**列。
   同一个抽屉里"看到的顺序"和"写出去的顺序"是两套，于是：
   `handleDragEnd` 里的 `order = rows.map(...)` 每次都从 schema 顺序起算，
   第二下不是叠在第一下之上，而是把第一下**重算一遍**（症状 ③）。
2. `patch`（宽度/显隐走的就是它）遇到 `columnMeta` 里没有的列，是 `[...columnMeta, {fieldCode, ...next}]`
   **追加到尾巴**。而 `projectColumns` 的规则是"`columnMeta` 里的条目按它自己的顺序排在前面，
   schema 剩下的补在后面"—— 所以"追加到尾巴"= **插到表格最前面**。
   每个没配过列的视图都是这个默认态，也就是说不需要任何前置操作就能撞上门。

修法就是把两处都收到同一份投影上：`rows = projectColumns(resolvedFields, columnMeta).map(...)`，
`patch = onChange(rows.map(row => row.fieldCode === fieldCode ? {...row.meta, ...next} : row.meta))`。
`handleDragEnd` 一行没改 —— 它本来就写的是"按当前渲染出的顺序 arrayMove"，
只是"当前渲染出的顺序"以前是假的。

**三层各钉什么**

- 单测（`ColumnManagerDrawer.test.tsx` 3 条）：抽屉列出的顺序 == 表格投影顺序；
  给未持久化的列改宽度不换顺序（且宽度写进了那一列）；隐藏一列时**整份数组逐个比**
  （顺序 + 槽位 + 邻居的 `width`/`hidden` 都要原样带着）。
  修复前 3 条全红，其中宽度那条报的就是
  `表格列顺序不该因为一个宽度而变: expected ['hours','title','prio','due'] to equal ['title','prio','hours','due']`
  —— 和浏览器里 `A2` 同一件事，两个层各自独立打出来。
- 浏览器门禁（§4b 6 条）：抽屉顺序=表头顺序（未排序时）、改宽度不挪位且宽度落在该列自己身上
  （量 `th` 的实际矩形，不是查文本）、真键盘 `Tab→Space→↓→Space` 交换相邻两列、
  排完一次抽屉跟着变、**第二次排序叠加在第一次之上**、「恢复默认」还得回 schema。
  键盘那两条走的是真按键注入，`tabToHandle` 只用 Tab 走焦点 —— 走不到就报"Tab 走不到该句柄"，
  不用 `element.focus()` 蒙过去（那正是 #32 那类谎能活下来的原因）。
- ⚠ **拖拽本身在单测层注入不出来，按"未覆盖"记账**：jsdom 没实现 `Element.prototype.scrollIntoView`，
  dnd-kit 的 `KeyboardSensor` 一按 Space 就在 drag start 抛 `TypeError`（实测：`KB_EMIT []`），
  `PointerSensor` 又依赖 `document.elementFromPoint`。所以"真能排序""两次会叠加"只有浏览器那层算验过。
  这是 jsdom 边界的第五条，与前四条（Dropdown 不交焦点 / 弹层 effect 时未接进文档 /
  `isVisible` 要真布局 / outside-click 听 mousedown）同记在用户侧记忆里。

**反证：`z-lc-admin-ui/e2e/mutate_column_order_guards.py`，三个注入 × 各两遍**，
恢复后 3/3 绿，**零条预期外的红**：

| 注入 | 改回去的那一处 | 预期红 = 实测红（两遍一致） |
| --- | --- | --- |
| I1 | `rows` 退回 `resolvedFields`（= 症状 ②③ 的原样写法） | 顺序契约 + 隐藏那一整份（2 条） |
| I2 | `patch` 退回"缺就追加到尾巴"（= 症状 ① 的原样写法） | 只有宽度那条（1 条） |
| I3 | `patch` 整份重写但把邻居的配置抹平 | 只有隐藏那条（1 条） |

三个红集合**互不相同**，这是"三条不是重复检查"的证据。I1 与 I3 都红隐藏那条，区别在 I1 多红一条顺序：
隐藏那条比的是整份数组（顺序 + 槽位 + 邻居配置），顺序那条只比列出的顺序 —— 两个注入打中的是同一处的不同半边。

顺带记两条：

- ⚠ 一支只跑 `npx vitest run` 的门禁会漏掉类型：这轮的测试文件里 `emitted[emitted.length-1]`
  在 TS 严格下是 `ColumnMeta[] | undefined`，还漏了必填的 `onClose`，vitest 三条全绿而
  `npm run check` 第一步直接 `TS18048 / TS2741 / TS2345`。老规矩仍然成立。
- ⚠ 我自己第一次写的浏览器检查就是条假检查：拿 `th` 的**中文标题**去和抽屉的 **fieldCode** 比，
  两条比顺序的断言永远不可能相等 —— 它红了，但红的原因是检查写错，不是产品写错。
  修正前一条都没绿过，所以它当时既没证明任何东西、还差点被当成"缺陷仍在"的证据。
  判据：**新写的检查先问"它要怎样才能红"，再问"它现在为什么红"**。

### ✅ 字段编码 = 物理列名：一道闸拦住建表 500，同一道闸差一点把「新建实体」按死（2026-09-23，缺陷 #34/#35/#36）

**#34 的形状**：`id / tenant_code / deleted / create_time / update_time` 是引擎给每张受管表自建的列，
而这五个名字**全是合法标识符** —— 原来那道 `^[A-Za-z][A-Za-z0-9_]*$` 正则放不住它们。
后果不在"命名不优雅"：元数据照收、`previewDdl` 照画，到 provision 才 `Duplicate column name`；
而 `provision-all` 在**第一个**坏实体上抛，同一个应用里其他实体的表被一起挡住 —— 一个手滑的字段名变成整个应用不可用。

**#35 比 #34 更坏**：`buildCreateTableDdl` 遇到撞名/非法编码是 `continue` 静默跳列。
表建"成功"、少几列没人知道，之后 runtime 一查才 500。这类"报成功而实际做得更少"比报失败难查一个量级，
因为它不会把用户引向任何一处代码。修法不是"多一个 if"，是**把 continue 换成抛错**（三处调用点都过闸）。

**#36 是这道新闸的自伤**（本轮唯一一个我自己造出来的 P1）：点「新建实体」看着没反应，两层原因叠在一起 ——
① `onClick` 里 `setDraft(新草稿)`，而"从选中实体派生草稿"的 `useEffect([selectedId, entities])`
随即用 `found === undefined` 把它冲回 null（**这个按钮从有以来就是空操作**）；
② 新草稿 `fields: systemFieldDefs()` 预置了 3 行引擎自建列，于是刚加的闸让这份草稿**永远保存不了**。
症状长得像"闸写错了"，最省事的"修法"是去放宽闸 —— 那会把两个缺陷合成一个更坏的。
真正的修法是把两件事分开：effect 认 `creating` 标志（新建期间不让位）、字段表清空
（引擎列本来就由视图侧 `registry.systemFieldDefs()` 合成，**只用于渲染，永远不该进可编辑字段表**）。

**取证过程里最值钱的三条**：
- 一开始我按"闸误判"去查，探针读到的报错是 `没有「实体编码」这一栏，页面上的 addon 只有 []`。
  这句话**有歧义**：既可能是"编辑器没渲染"，也可能是"我定位器写错"。加了一句"实际存在的 addon 列表"
  才分清 —— 列表是空的，说明编辑器真的不在，**不是测试找不到它**。
  教训：**报错信息要带"我当时看见什么"，不然下一步会在两个假说之间掷硬币**。
- 两条 jsdom 边界（第 5、6 条，已进用户侧记忆）：`<Input>` 的 value **不在 `textContent` 里**
  （害我第一次 7/7 全红），以及 `fireEvent.change` 连发两次（清空再输入）**只落第一次**
  （害我 5/7 红、另外 2 条**空过** —— 它们其实在验空值那一支）。
  后者是本轮最阴的一次：绿灯不等于测到了东西，得**断言消息文本**而不是只断言 `disabled`。
- `react-refresh/only-export-components` 不允许组件文件导出纯函数，所以规则本体落在
  `src/fields/columnRules.ts`（和后端 `SchemaAdminBizService` 一一对应，注释里写明"两边必须同一个口径"）。
  这个约束顺手把"规则"和"页面"分开了，是好事，不是绕路。

**上一轮（唯一编码那一轮）新加的两支**（都跑两遍、判定只看具名结果）：
- 后端 `_e2e/mutate_field_code_guard.py`：I1 摘整闸 / I2 丢 `toLowerCase`（大写 `ID` 漏，而 MySQL 列名不分大小写）
  / I3、I4 逐个摘调用点。**8/8 与预期吻合**，摘哪处红哪处 —— 三处调用点不是重复检查的证据。
- 前端 `z-lc-admin-ui/e2e/mutate_designer_field_code.py`：F1 拆保留列那一支、F2 丢 `toLowerCase`、
  F3 正则松成前缀匹配、F4 空值只管 `undefined` 不管空串、F5「提示在但按钮不拦」、F6 那一格不标红、
  F7 退回改动前的 `codeIssues`、F8/F9 = #36 的两层。**F1–F9 各两遍 = 18 条 OK，零预期外的红**。
  ⚠ F8/F9 的预期红集合**故意相同**（同一个测试标题），但**红在不同断言**：
  F8 `没有「实体编码」这一栏`（编辑器根本没出现）vs F9 `expected 3 to be 0`（闸被预置列触发）。
  标题粒度的集合相同**不等于**重复检查，这句要去归档的 `failureMessages` 里核 —— 本轮核了。
  ⚠ F9 的 mutant 现在自带字面量 `['id','create_time','update_time']` 而不是引用 `SYSTEM_COLUMN_CODES`：
  源码里那个 import 已被 tsc 的 TS6133 拿掉，注入若引用不存在的符号会红成**崩红**，崩红证明不了闸。

**这一族的三层覆盖（2026-09-25 补齐，上面那段"仍未覆盖"就此销账）**

上一版记的是"接口层没有一条打『保留列 → 400』、浏览器层没有一条『输入 `id` 就拦保存』"。
两句都已经是过去式：接口层新增 `[15j]` **33 条**（这个数是从归档的那一轮 `e2e_fcguard_restored.log`
里按 `[NNx]` 横幅分段数出来的，不是从源码里手数 `check(` —— 源码只有 19 处调用点，
两个 `for` 展开成 5×2 与 4×2 才到 33；顺带这条分段计数把 27 个段的条数加回 **283**，
与脚本自己报的分母对上，等于给分母做了一次独立复核），
浏览器层新增 11c **7 条**。覆盖不等于有效，所以两道注入自证各自跑在**它自己那一层**：

- 部署件层 `z-lc/_e2e/mutate_field_code_deployed_guard.py`（J1–J7，每支都重新 build fat jar、
  重新起 18090，判据分母钉死 283）：**七个注入全部与预期吻合**，`RESULT: deployed-layer
  falsification done`。逐支的红数：J1（摘掉 `createEntity` 那道调用）21 条、J2（丢
  `toLowerCase`）3 条、J3（摘掉 `updateEntity` 的调用）2 条、J4（摘掉 `buildCreateTableDdl`
  的调用）**0 条 —— 这一支在本层按设计不可见**，它的网是单测
  `buildCreateTableDdlShouldFailLoudlyInsteadOfDroppingColumns`，脚本同一轮里实测到该条确实红，
  所以记为"有网，只是不在这层"，不冒充本层抓到；J5（逆向映射改成一律拒绝）4 条、
  J6（静默跳过不写 description）1 条、J7（正则松掉）3 条。**每一支 `failures outside [15j]`
  都是 (none)**，即零连带红；恢复轮重建产物**回到基线指纹**（`artifact back to baseline
  bytes: True`）、283/283 复跑、单测红归零。
  ⚠ J2 的预期一开始我写少了一条：它只列了"大写 ID"两条，实测多出"一行都没落库"。
  那不是连带红而是**因果必需**（大小写比较坏掉 → `ID` 被放过 → 那一行真的落库），
  所以改的是预期而不是断言，且**整支重跑**取证，没有拿旧日志离线重判。
- 浏览器层 `z-lc-admin-ui/e2e/mutate_field_code_browser_guard.py`（B1–B5，自己 build、
  自己起 preview、每轮跑完整 137 条）：基线 **137/137** → B1（摘掉整个自建列分支）133/**4**、
  B2（那一栏不再自己标红）136/**1**、B3（保存按钮不再被按住）136/**1**、
  B4（文案不再点名）136/**1**、B5（撞自建列说成"不合法"）135/**2** → 恢复后 **137/137**。
  五支红集合两两不同、**零一条预期外的红**，且**每支 bundle hash 都不同**
  （`Bhd3Gz8F` → `sXza_L-k` → `rgzSiCpu` → `BemD70ss` → `Cimo1oim` → `CYqzpOi5` → 回到 `Bhd3Gz8F`），
  这既证明注入走到了真产物，也证明还原是逐字节的。`columnRules.ts` 与 `DesignerPage.tsx`
  跑完后 `git status` 不再列出 = 与 HEAD 逐字节一致。
  ⚠ **三条按未覆盖记账**（写在脚本 docstring 里，不冒充证过）：「拦住 = 一次写请求都没发出去」
  「松开之后仍然没有偷发写请求」数的是请求条数，要让它红必须**新增**一个自动提交的行为
  （往组件里塞 effect），那是"造一个新缺陷"而不是"摘掉一道守卫"；
  「改回干净编码后闸立刻松开」的失效形状是 memo 粘住/输入不受控，摘源码里任何一行都不会
  让它单独红（把 memo 依赖摘空会连带前 4 条一起红 = 退回 B1）。计数监听这一层本身是开火的 ——
  同一套监听抓到过日历 43 次/300ms 的重查。

### ✅ 交叉表（PIVOT）：空值那一档在库里是 `''`，在表头上差点是一个空格（2026-09-25）

后端两件事（`5dad2a0`）：`/runtime/aggregate` 支持多维 `groupFields`，`/runtime/shape` 吃这些行、
按 `pivot` 步骤产出高维视图结构。前端 `PivotView` + `pivotModel`（第 8 类视图）。

**引擎口径是量出来的，不是推的**（`_e2e` 里 `[15d1]` 15 项钉着）：
- 两个分组字段 → 列名是 `group_key/group_label/group_key_2/group_label_2/group_count`；
  哪一维挂了 `timeGroup` 也不改列名形状。
- 某一维**值为空**（NULL 或空串）时，后端给的键是 `""` —— 不是 `null`、不是缺列。
- `pivot` 之后的行形如 `{"group_key":"写迁移脚本","group_label":"写迁移脚本","常规":1,"":null}`：
  这一行在这一档**没有记录**，那是 `null` 而不是 0。
- ⚠ 喂进 `pivot` 的行**已经是聚合结果**了，所以"多少条记录"必须是 `SUM(group_count)`；
  按 `COUNT(*)` 数出来的是格子数。这一条是同类聚合最容易犯的错，行维度一多就虚高。

**缺陷（本轮抓到并修掉的那一条）**：那个 `""` 键若原样上屏，表头就出现一根**没有标题的列** ——
它带着真实数字，看着像一列装饰，而且合计行、总计都算得对，所以四道门禁里**只有浏览器层**能看见它。
修法在 `pivotModel.pivotColumnLabel`：空白键 → `chartModel.UNFILLED_GROUP_LABEL`（`（未填写）`），
和图表那边共用一个词，不再各写各的。表头同时挂 `data-col-key` 存**后端那个原始键**，
这样"这一列认领的是空键"是可证的，而不是"标题写了（未填写）"这句自说自话。
⚠ 一处**已知不一致没有抹平**：看板那一列空档仍写 `（空）`（`KanbanView`）。它不影响数字，
但同一平台里同一件事有两个词，改哪边都要连带改它的用例 —— 按记账留着，不是忘了。

**空值是从库里真的清出来一条才测的**：我第一版 10e 里那条"空值那一档要有标题"**是空断言** ——
数据里根本没有空档（`title` 是必填列），它照样绿。这就是我自己记过的那类"永远绿的检查"。
所以加了 10e-2：先 `POST /runtime/update` 把一条记录的 `prio` 真清掉，回读确认库里那格是空串、
并且**同时**存在"有优先级"和"没优先级"两种记录（非空性自己先断一道），再把维度组合换成 标题×优先级
去画。这一族一共 7 条，包括"换组合之后行数仍等于行维度档数"和"行/列维度没写反"。
顺带把部分更新的语义量清楚并钉进接口层（`[15e]` 新增 4 项）：
`fieldValues:{col:null}` 与"这一列没提交"**同义**（值不变），要清空必须传 `""`，
且库里存的是空串而不是 NULL —— 也就是说表格里那个 `（未填写）` 档，数据源头就是 `''`。

**反证（补齐"只有能变绿的门禁才算门禁"这条规矩到浏览器层）**：
- 单测层：`z-lc-admin-ui/e2e/mutate_pivot_guards.py` 十个注入 P1–P10，分母钉成脚本里的
  `COLLECTED` 26 条标题（`pivotModel` 16 + `PivotView` 10），只看具名红、零容忍连带红；
- 后端：`_e2e/mutate_group_fields_guard.py` —— 七个注入钉的是 `DynamicSqlBuilder` 里 `groupFields`
  这段**只能拼字符串、不能走占位符**的新注入面（GROUP BY 只按第一维 / multi 阈值 off-by-one /
  重复维度静默去重 / 第二维漏过白名单 / timeGroup 与多维的互斥守卫被搬走 / 行序少第二维 tie-break /
  把空维度当错误），分母钉成 `DynamicSqlBuilderAggregateTest` 一个类的 16 条。
  ⚠ 它每轮 mvn 前先删这个类的 surefire 报告 —— 编译不过的注入不写新报告，留着旧报告就是
  把"上一轮的红"当本轮结论，那是假绿里最省事的一种；报告缺失或零条 testcase 一律 FATAL。
- **浏览器层：`z-lc-admin-ui/e2e/mutate_pivot_browser_guard.py`（本轮新）** —— 它必须自带
  `npm run build` + 自己起 `vite preview`，因为门禁打的是生产产物；改完源码不重新构建，
  测的就不是"改之后的那一份"。P8 把 `pivotColumnLabel` 退化成原样返回 `rawKey`，
  实测：基线 `PASS 130 / FAIL 0` → P8 `PASS 127 / FAIL 3`（红的正是那 3 条 `未填写…`，
  一条连带红都没有）→ 还原后 `PASS 130 / FAIL 0`，`pivotModel.ts` 按字节还原（md5 校验）。

**这一轮同时给门禁加了两件它此前没有的东西**：
1. **产物指纹守卫**（`assertServingFreshBuild`）：preview 伺服的那份 bundle 必须就是
   `dist/index.html` 引用的那份，且不比 `src/` 里最新的文件旧，否则**拒绝开跑**并退 2
   （已实测会拦：touch 一个 src 文件后 `GUARD_EXIT=2`，报"src 里最新的文件比 index-xxx.js 新 Ns"）。
   动因就是上面那支注入脚本 —— 它跑完把源码换回去但不会再构建，我差点拿一份旧产物去判"全绿"。
2. ⚠ **注入脚本自己也会算错账**（又一次）：`run_round` 抓 `  FAIL  ` 行时没剥掉
   `   << 读数` 后缀，于是那 3 个名字**同时**被判成"预期红却没红"和"预期外的红"，
   脚本对着一次真实的成功报了 FAILED。修完之后我拿归档的 `01_p8.log` 复判验过 `hard=[]、extra=[]`，
   再把脚本整支重跑一遍才算数 —— **离线复判只能证明判定逻辑，不能代替脚本自己跑绿**（同一条规矩）。

还有一个测试侧的竞态是这一轮顺手收掉的：仪表盘"丢一块不连带"那条原来只等两块组件挂上，
而挂上 ≠ 数据回来（实测 4 轮输过一次 `slices=0`，请求明明还在飞）。改成等
`.zlc-chart-slice > 0`，且**只等 `>0` 不等 `==真值`** —— 等到真值就等于把断言搬进了等待里，
那条检查会永远绿。

### ✅ 处理流水线：配置页写着"按 order 执行"，而引擎永远跑全部处理器（2026-09-25，缺陷 #41）

缺陷本身有两半，第二半比第一半难看见：

1. **配置不执行。** `Pipeline` 拿到的是 Spring 注入的全部 `FieldProcessor`，自己按**类名排序**跑完，
   `triggerEvent` 只用来决定"写前还是写后"。`PipelineConfigService.listByEvent` 在生产代码里
   **零调用者** —— 这个"没人读"就是配置页与引擎脱节的硬证据，界面上删掉一个阶段、把顺序调过来，
   库里存了、运行时什么都没变。
2. **配置页在卖引擎兑现不了的东西。** 阶段清单里有 `WEBHOOK`、`SCRIPT` 两个**没有任何执行器**的编码，
   触发事件里有 `AFTER_CREATE/UPDATE/DELETE` 三个**没有任何写后回调落点**的值。选它们能存成功，
   而那一行配置永远不会跑 —— 比"报错"坏得多，因为它看着像配好了。

修完之后每一层都有钉子，也各自有反证：

- **执行面（Java 单测）** `PipelineWriteChainTest` 13 条，全部用**记录调用顺序的桩处理器**直接量
  "哪几个跑了、按什么顺序跑"，不量错误消息：配置里的子集就是跑的那几个、`order` 就是执行顺序、
  整批 2000 行只查**一次**配置表、停用/别的应用/别的实体都不许顶掉默认链，以及三条"兑现不了就抛错"
  —— 缺必填阶段的存量配置、词表里有而容器里没有的 bean、处理器抛错要包成 `PipelineException` 并带上阶段名。
- **写入口（API E2E `[15p]` 58 项，#42 之后是 **69** 项** —— 这一节的账记的是 #41 那一窗，段号没变、条目涨了）
  一份配置必须同时过"必填/类型/值域"三道闸门、阶段编码必须撞得上
  执行器、触发事件必须真有挂接点、`order` 必须是整数、值校验必须排在类型转换之后（引擎的不变式：
  `maxLength` 只对 String 生效，反过来配会把合法数字按字符长度拒掉）。
  **13 次该拒的提交逐个一探**，每次只让它违反**一条**规则，并各配一条孪生断言「文案点名要什么」。
- **同源（前端单测）** `pipelineVocabulary.test.ts` 8 条，直接从 `PipelineStages.java` 机械抽取
  `PROCESSOR_BY_TYPE / MANDATORY / NO_OP_ON_WRITE / SUPPORTED_TRIGGERS` 与前端清单逐字节对（含顺序），
  词表漂了就红 —— 前端不许自己猜"哪三个阶段摘不得"。
- **界面（浏览器 `[11a]`）** 种一份**合法但数组位置与 `order` 不一致**的配置：
  表格必须按 `order` 画、编号就是 1..N 的执行位、阶段编码原样显示（只有中文名则词表漂了看不出来）、
  「写路径暂不做事」只挂在 `DICT_RESOLVE` 那一档、草稿里只给得出引擎认得的选择项、
  删空阶段时当场说"这一份保存会被后端拒绝"、保存之后 toast 里**没有**「已保存」且表里仍只有原来那一条。

**四份预期红集是推出来的，实测校正了三处**（部署件层第一轮的 9 个 problem 全是这个原因，不是产品坏）：

- 摘掉任意一道写入前的配置校验闸，`[15p]` 里**两条**落库计数检查会**一起**红
  （「一行都没落库」和「被拒的第二份没有把第一份顶掉」）—— 我原来只预期了前者。
- 摘掉 `root.size() == 0`（空链）之后，**「拒」这一半仍然绿**：空链被必填那道闸拒在同一个 400 上，
  只有「文案点名要什么」这一半红。这是"过定"的实测样本，也反过来说明那 13 条孪生断言不是凑数的。
- `Chain.run` 吞掉 `PipelineException` 的那一支（P3）本来就该波及全 suite：
  「显式清空必填列仍被拒绝」「伪造的前像不会写库」「preview 统计 total/validCount」「行级错误带回行号」
  四条红是**同一个缺陷的第二批证据**（逐条读过快照里那两处调用点：`runtime/update` 与 `import/preview`
  每行都走 `preWrite`），现在按名字钉进预期，名字漂了照样 MISMATCH。

⚠ 两条量具自己的坑，都已改掉：判"新代码在不在伺服"不能只看 `/api/lc/health` —— 旧进程吃了 SIGTERM
没死时健康检查照样 200，要用**新字节码独有的行为**当指纹（`enabled=2` 必须 400）；后台跑的注入脚本
不加 `-u` 会把 stdout 全缓冲成 0 字节，跑完才吐出来，中途无法审进度。

**上面那 21 条浏览器检查自己是被三处量具缺陷打出来的**，三处都不是产品坏了，而且第一处**两轮全绿也照不出来**：

1. 这一节一开始**根本没选实体** —— `save()` 在「请选择实体」那句就返回了，于是"后端拒了"那两条检查
   打的是界面而不是服务端。这条是**读代码读出来的**，不是跑出来的（那两轮确实全绿）。补了一条前置检查
   「草稿里选得到被测实体」，选不到就当场红，不许下面三条空跑。
2. 全局 `.ant-select-dropdown:not(.ant-select-dropdown-hidden)` 在**两个下拉框同时开着**时会把两份选择项
   读成一份 —— 第 2 轮实测读出 7 项，其中 2 项是 `<没有阶段编码>` 幽灵。修法不是把 timeout 拉长而是
   **去竞态**：读之前先等"开着的下拉框数 == 1"，并把这件事本身写成一条具名检查（三个调用点各自一条）。
3. 收下拉框那一下原本用 `Escape`：焦点不在 `Select` 里时那一下会落到 antd `Modal` 自己身上，
   把**整个弹窗**关掉 —— 第 3 轮因此卡在 `.ant-modal .anticon-delete` 等 30s 超时、整节中止。
   改成点 `.ant-modal-title`，并新增「草稿弹窗还开着」这条前置检查，让"弹窗被误关"必红而不是变成一段空跑。

这一节**故意**造一次 400（空链被服务端拒），于是全局那条「没有意外的 404/5xx 接口」两轮都报 `FAIL 1`。
没有把 URL 加进白名单，而是把被许可的那一次按**多重集**从 `badResponses` 里扣掉（`sanctionedRejections`
只登记这一节真的看到的那条 `400 <url>`），并配一条等重的反证「整轮 `pipeline-config/create` 恰好一个 POST」
—— 白名单会让"多放一次"隐身，多重集 + 计数不会。收线时日志里是 `INFO 非 2xx 响应 1 个：400 …/pipeline-config/create`
紧跟 `PASS 没有意外的 404/5xx 接口`，那一条 INFO 就是"扣掉的那一次仍然看得见"。

**注入自证分两层，各判各的分母**：

- jsdom 层 `mutate_pipeline_wiring_guard.py`：F1–F10 十支，基线 10 条用例全绿 ×2 → 十支**各自 `OK … 无一条连带红`**
  → 恢复后复跑仍 10 全绿（09-25 23:56–23:57 那一轮；分母钉在 `pipelineVocabulary.test.ts` + `PipelinesPage.test.tsx`
  这 10 条用例的**标题**上）。两处返工值得记：F2 的第一版 mutant 写成 `"      ;)"`，那是个 TS 语法错误，
  vitest 收集到 **0 条**，整轮按"跑不完 = 没测"作废 —— 换成恒等比较器 `.sort(() => 0)` 之后**先单独跑一次**验过
  （10 条收集、红恰好是预期那 2 条），再进整轮；F3/F4 各多红一条列表列的检查，因为**同一份词表还被读了一次**
  （列表列的"无执行器/无挂接点"警告与选择项同源），按名字钉进预期而不是放宽判据，同时把
  `pipelineVocabulary.test.ts` 里触发事件那一条的断言顺序调了个位，让**带理由的那句先红**。
- 浏览器层 `mutate_pipeline_browser_guard.py`：见下一段（S1–S9，#42 又加了 S9 一支）。
  ⚠ 上面这两行的分母是**当时**的：#42 之后 jsdom 那支的基线从 10 条用例涨到 **12** 条、注入从 F1–F10 涨到
  **F1–F13**（并且它现在改的是**三个**文件，含 java 参照集，见下一节）；浏览器那支从 S1–S8 涨到 **S1–S9**，
  11a 那一节 21 → **23** 条、全门禁 158 → **160**。同一支脚本的分母变了，不等于它原来的判定作废 ——
  但**记账要写清是哪一窗的数**，下一节里那两张表就是 09-26 02:2x 重跑的。

### ✅ 流水线阶段参数 `config`：填了能存、运行期一个字都不执行（2026-09-26，缺陷 #42）

`PipelineStages` 一直把 `[{type, config, order}]` 里的 `config` 原样落库，配置页还给了一个能打的输入框
（框下面诚实写着"当前引擎不读取，保留给后续实现"）。诚实的那半句是**界面向用户认错**，
但用户那边没有任何变化：填进去的东西存成功了，跑起来什么也不发生。这正是 #41 第二半那一类。

选的是"两条出路"里那条更窄、也更容易守的：**先让口径只有一份，再让写入口替用户挡住空话**。

- 新增 `CONFIG_KEYS_BY_TYPE`（由 `buildConfigKeysByType()` 建，`configKeysOf(type)` 读）：
  "这一档今天有哪几个参数真被读"的**唯一**登记表。今天五档全是空清单。
- 解析分成两种用途，一个函数两种严格度：
  `validateAndResolve()` 走**写入口**，`resolve()` 走**运行期**。
  同一条 `stages` JSON，前者对"没人读的参数"抛 `IllegalArgumentException`
  （消息点名引擎支持什么：`—— 该阶段今天不读取任何参数 (引擎里参数只有一份口径: PipelineStages.CONFIG_KEYS_BY_TYPE)`），
  后者只 **warn** 并照旧执行 —— 库里已经躺着的老配置不能因为一次代码收紧就跑不起来（`Pipeline` 里逐条
  `log.warn("流水线配置 [id={}, entity={}] {}")`）。**不对称是设计**，所以它有专门的注入（U5）。
- 两种坏形状记进**同一个** `unread` 清单：`config` 不是对象（`阶段参数不是对象`）、
  对象里有没登记的键（`… 的 … 没有任何处理器读取`）。这不是省事，它直接决定了部署层那支注入的形态（见下）。
- 配置页把输入框**撤掉**了，换成一句读数：老配置行里真带着参数时写
  `参数 xxx 引擎不读取，保存会被拒`，否则写 `这一档没有可配参数（引擎不读取阶段参数）`。
  删掉格子不算修谎 —— 所以浏览器层那条检查钉的是"每一档都还在说这一档有没有参数"。
- 前端词表 `pipeline.ts` 每一档多了 `configKeys: string[]`，并由
  `pipelineVocabulary.test.ts` 用**两句方向相反**的断言和 java 那份登记表对齐：
  先问"后端有没有多出前端没跟上的键"，再问"前端有没有声明后端没登记的参数"。

六层各自钉一层，全部同轮注入自证（09-26 01:0x–02:1x 这一窗，跑在**恢复后**的那份源码上）：

| 层 | 载体 | 注入 | 读数 |
| --- | --- | --- | --- |
| Java 单测 | `PipelineStagesTest` 23 + `PipelineConfigServiceTest` 26 + `PipelineWriteChainTest` 14 = **63** | `_e2e/mutate_pipeline_config_guard.py` **U1–U6** | 六支各自打掉一句保证，恢复态 63 条全绿（`RESULT: #42 那六支各自打掉一句保证；恢复态 63 条全绿`，`U_EXIT=0`） |
| 真 HTTP（源码层） | `_e2e/e2e_api_test.py` `[15p]` 新增 3 条：`阶段参数没人读 -> 拒`、`阶段参数不是对象 -> 拒`、`… -> update 也拒` | 同上（P 战役每支都重跑整份 `[15p]`） | 全门禁 **353/353**，`[15p] checks=69` |
| 部署件（18090 那台 jar） | 同一份 353 | `_e2e/mutate_pipeline_wiring_guard.py` **P13/P14**，并把**整族 P1–P14** 在本窗重跑 | P13 打掉 `if (!unread.isEmpty())` 这唯一的消费点 → **9 条具名红**；P14 打掉"不是对象也记账" → 4 条；整族第二轮 `exit=0`、14 个注入块每个都 `changed=True`（产物真换过）、**0 处 MISMATCH**、收线"重建成原始字节再跑"回到 353/353 且 `[15p]` 69、`restored sources: clean` |
| 前端 jsdom | `pipelineVocabulary.test.ts` + `PipelinesPage.test.tsx` 共 **12** 条 | 同一支 F 战役里的 **F11/F12/F13** | F11 前端多报一个键 / F12 把输入框装回来 / F13 后端登记的键变了 —— 三支各红一条且互不相同；整支 **F1–F13 十三支全 OK**，`RESULT: 前端口径注入自证完成` |
| 真浏览器 | `browser-e2e.mjs` 11a 段（这一节 **23** 条，全门禁 **160**/轮） | `mutate_pipeline_browser_guard.py` **S9** | 基线 160/0 → 注入后 `PASS 158 / FAIL 2`，两条红恰好是新增的那两句 → 恢复后 160/0；三次构建的产物 `index-DiIAeQ4C.js` → `index-DED1Bq_b.js` → `index-DiIAeQ4C.js`（回到原 hash 才叫"被测件换回来过"） |
| 交叉引用 | 上面五层 | — | 四道门禁在**同一次运行**里串跑全绿（`JAVA_EXIT / API_EXIT / CHECK_EXIT / BROWSER_EXIT` 四个 0） |

⚠ S9 那一份归档日志（`~/.cache/zlc42/browser_mut/s9_r2.log`）末行打的是 `RESULT: 11a 那八支…（PARTIAL: 只跑了 1/9 支 = ['S9']…）` ——
"八支"是**当时脚本里还没改的措辞**，判定本身按 `bad == 0` 走退出码，跑的确实是 S9 那一支；
脚本里的措辞在那之后已改成"九支"，但**没有为改一句 print 再重跑一次**（重跑只会产出一份字节不同的日志，
不会给那一轮多任何证据）。这条属于"数字对、措辞旧"，别拿它当"S9 没测"。
同理那份日志是 `--only S9` 的部分跑，**不能当整族绿** —— 九支一起跑的那一轮记在 #41 一节里。

**这一族最容易被自己骗过去的地方：词表为空，"两边都是空的"这个断言天然为真。**
所以 F11/F13 是一对**方向互补**的猎物 —— 只在 `pipeline.ts` 里凭空加一个键（F11）和只在 java 里加一个键（F13）
必须红在**不同**的那一句上；只注入其中一支，就等于没测"反向"。
同理，第一个真参数落地那天，`CONFIG_KEYS_BY_TYPE` 加一行之外还必须补一条
"处理器确实读了它"的用例 —— 否则这道闸会退化成"登记了就等于生效"，那正是 #42 原来的样子。

⚠ **P13 的预期红集我第一次写错了**（少写了"不是对象"那一对），跑出来 MISMATCH。
这条不是量具噪音，是我的预测不全：两种坏形状进同一个 `unread` 清单，所以摘掉那一个消费点会**同时**放跑两类。
补上以后按名字钉死，并在脚本注释里写明了归因 —— 预期红集要按"谁读了这个值"估，不按主题估。

⚠ **同一族里新增探针会改写旧注入的预期红集，所以"只跑新增那两支"是不够的**（本窗实测）：
#42 在 `[15p]` 段尾加了 3 条 update 探针、又给 `PipelineWriteChainTest` 加了 1 条 warn 用例之后，
**P1–P14 整族**第一次重跑报了 3 处 MISMATCH —— P1 少 1 条（`resolve()` 在那支注入下压根不被调用，
那句 warn 也随之消失，所以它属于 P1 的红集），P4/P6 各多红 9 与 8 条（预期从 7→**16**、4→**12**，
原因是 update 探针让创建闸失效的**波及面变宽**：
坏配置真写进行里，后面所有"按配置链执行"的检查读到的就是它）。
三处全是"我的账目过期"，不是产品坏了（同一轮基线 353/353、恢复后 353/353）。
**How to apply:** 改了一段共享探针之后，把打那一段的**整族**重跑一遍并逐条给新红写出机理；
把预期红集改宽是最容易做错的动作（改完就绿 = 自证失效），所以每一处补账都得配上"为什么这一句被这处注入打掉了"。

⚠ 这一族的 java 改动**被自家量具抹过一次**：09-26 01:47 那支 P 战役收线时拿前一晚 23:00 的旧备份
覆盖回 HEAD（详见上文"备份目录被上一支脚本复用"那条）。症状是 `mvn testCompile` 报
`cannot find symbol: Resolution / resolve / configKeysOf` —— 四层门禁里**没有任何一层**会把这个报成缺陷。
从会话记录里逐段重放恢复后，用三件独立的事确认恢复对了：63 条单测全绿、U/P 两支注入的锚点仍然各自唯一
（两支都能整族跑完并按预期红收线）、`git diff --stat` 回到 +140/+9 的形状。

### 真浏览器 E2E 怎么跑

    cd z-lc-admin-ui
    npm run build && (npm run preview:e2e &)   # 生产构建 + preview，端口 5274
    node e2e/browser-e2e.mjs                   # 默认打 http://localhost:5274
    E2E_REPEATS=3 node e2e/browser-e2e.mjs     # 同一份产物连跑 N 轮，要的是通过率不是"绿一次"

两个只在这条链路上的坑：
- **`vite preview` 只绑 IPv6**（实测 `[::1]:5274` LISTEN，`127.0.0.1:5274` 连不上、curl 退 7）。
  探活/取证一律写 `localhost` 或 `[::1]`，两支都要试（注入脚本 `port_busy()` 就是双栈都探）。
- **开跑前先验产物**：`assertServingFreshBuild` 会比对"preview 正在伺服的那份 bundle"
  与"`dist/index.html` 引用的那份"，并要求它不比 `src/` 里最新的文件旧；不满足直接退 2，
  打印 `门禁拒绝开跑（测的必须是本轮构建的产物）`。满足时打印
  `产物指纹：index-xxx.js，比 src 里最新的文件新 Ns` —— 报数时把这一行一起报。

默认打生产构建而不是 dev：dev 下热更新期间会残留旧的已卸载 React 实例，点上去打到死实例，
既会假绿也会假红 —— 上面那条"HMR 结论"就是这么来的（而且证伪了我自己第一次的解释）。

### P2.5 测试自身的质量问题（2026-09-23 已改）
`DataModelConvertMysqlSqlStrategy` 早期的用例连不上真实 MySQL 时只打一行
`ERROR ... Communications link failure` 然后**照样算通过** —— 这类"依赖不可达就静默 pass"的测试
等于没有测试。现已按本条建议收口：建池/探活交给 z-util-jdbc `DataSourceRegistry`，
同一段逻辑改由 `DataModelConvertMysqlSqlStrategyH2Test` 在 H2 内存库上真跑
（复用同一个池、换绑关旧池、探活不过不发布、close 释放池），
不可达地址则显式断言"必须失败"，不再是静默通过。
日志里仍会看到 Druid 的 `init datasource error` ERROR —— 那是这些"必须连不上"的用例在按预期报错。
另：`npm run check` 里 vitest 曾出现过一次 1 红、随后 5 次连跑全绿的未复现失败。
**同一类现象在批量删除那一轮真的复现并定位了**（见上文「批量删除」一节的教训 2：
O(document) 的 accessible-name 查询把单条用例拖到 12–17 秒，饿死并发 worker，
把负载下的竞态暴露成红门；顺带修掉 `ImportDialog.runImport` 一条真竞态）。
那条历史记录**没有取到当时的失败样本**，所以只能说"很可能同源"，不能宣称已经结案；
但结论可用：**别按"没复现=稳定"或"红一次=测试废"下判断，先让并发负载可控**。

### 前端测试基线与 lint 现状
`z-lc-admin-ui` 现在有 **22 个测试文件 211 个用例**（`npm run check` 里 vitest 那一步实测打印的数，
2026-09-25 重跑；上一轮记的是 20/185），已进 `npm run check`
（typecheck + `eslint --max-warnings 0` + test + build，四步全绿才算过）。
⚠ 别用 `npx vitest run` 的结果声称"typecheck 干净"：**vitest 不做类型检查**，本轮就有两条
测试文件里的 tsc 错误（`matched[2]` 可能 undefined、展开 `unknown`）在 vitest 全绿的情况下溜过去，
只有 `npm run check` 第一步才报出来。**下一轮又犯一次**（`GridViewBulkDelete.test.tsx` 里
对 `closest('button')` 用类型谓词 → TS2677/TS2322，vitest 依然全绿）—— 这条不是偶发，是习惯问题。
**第三次是本轮**：`WorkspaceViewPage.tsx` 用了新加的 `unreadReason` 却没 import → TS2304，
由 `npm run check` 第一步报出（`CHECK_EXIT=1`，vitest 那一步压根没跑到）。同一个毛病从测试文件换到了产品文件上。
**第四次（管理页列表那轮）**又是 TS2304，但成因不同：脚本搬函数时顺手删掉了一行还被人用的
`import type { ListState}`。⚠ 那一次**注入脚本已经先报过 135 全绿**，也就是"反证跑完了"并不保证
树是类型干净的 —— 顺序必须是 check 与注入各自跑，不能拿注入当 typecheck。
**第五次就是本轮，而且比第四次更典型**：`DesignerEntityStates.test.tsx` 里 `errorAlert(container, …)`
根本没用到那个参数 → TS6133，我是**在七个注入全部报完 170 全绿之后**才第一次 typecheck 才发现的
（新测试文件只跑了 `npx vitest run` 就往下走了）。同一轮的另一个后果更值得记：那个没用的 `container`
其实是断言写松的痕迹（当时用的是整页 `screen.getByText`），把它改成"在侧栏那张卡片里找 alert"之后
参数才真的被用上 —— 参数没用 == 断言没收窄，两件事是同一件事。
`_shared.tsx` 已拆成组件文件 + `_scope.ts`（hook 与工具函数），lint 因此降到 0 warning，
没有靠关规则糊过去。守的都是真实踩过的坑：
Result 信封（200 + success:false 必须抛）、`X-User-Code`/租户头、actor 单例、
字段注册表 10 类契约与值转换口径、视图状态归一化（脏 stats/坏 JSON 不崩）、看板分组排序、
`GridView` 组件级 9 条（schema 生成列、字典出标签、list 请求必带 appCode/tenantCode、
筛选与 conjunction 原样进请求体、页脚统计按持久化配置发聚合请求并渲染服务端结果、
统计口径是命中数而不是当前页条数、统计值落在自己那一列下面、
**内联编辑只提交被改的那一列且 fieldValues 带 id**、
**画出行之后安静期不再自己重查** —— 第 9 条是 `GridView.load` 这个全仓库最后一个手写取数出口的
计数守卫，见上文「表格视图的计数守卫」一节），
`KanbanView` 组件级 7 条（列走服务端 group-by、默认分组字段不许选成标题列而退化成一记录一列、
卡片按自己的分组值落列、「移到」与拖拽发出同形的部分更新，以及 **焦点交给菜单 / 关掉后还回触发按钮 /
第二次打开仍然给** 这三条 —— 它们是缺陷 #32 那座桥的三层，一层一个注入，见上文「看板「移到」的焦点桥」），
`ChartView` + `chartModel` 43 条（默认分组轴不选名称列、timeGroup 真下发、
NULL 不画成 0、命名视图落库与还原、扇区弧的半径/`large-arc-flag` 几何不变式、
柱高与折线点用图自己的网格线量回来 == 印出来的数），
以及 `pivotModel` + `PivotView` 26 条（16 + 10，标题逐条钉在 `mutate_pivot_guards.py` 的
`COLLECTED` 里）：能当维度的只有标量列、行列撞同一列要错开（否则画出一张对角线假表）、
维度列被删后回落而不是裸发吃 400、列维度没选就不替用户猜；聚合口径 ——
`COUNT` 走 `group_count`、**合并算子恒为 SUM（进来的每行已经是一个桶，`COUNT(*)` 会恒为 1）**、
`MIN/MAX` 的合计取小取大而不是把格子加起来、平均数不参与横向加总；
空值 —— 空格子是 `null`（不加进合计也不冒充 0）、NULL 那一档**行列都要报出名字但取格子仍用原始空键**、
交叉表与柱状图对同一个空档**用同一个字**；顺序与折叠 —— 列序不跟后端"第一次出现"漂、
折叠掉的行仍进总计（可见格 + 其余行 == 列合计 == 总计）；请求 —— 只选行维度时不发请求并说清缺什么、
选齐后一次请求且行维度在前、切指标时度量列进请求；谎话 —— 程序产出不合矩阵结构要抛结构错误而非空表、
读失败不许说成"没有可透视的记录"、后端结构不合预期时说的是"结构"不是空。
另：行头列显示字典标签而不是裸编码、第二维为空那一列有标题且数落在自己那一列下面。
全在「交叉表（PIVOT）」一节里对着读。
以及 `DashboardPage` + `dashboardModel` 17 条（每个组件按自己那份落库口径发请求、
切仪表盘要丢掉未保存的草稿、URL 不带编号时开最新一份而不是数组里恰好第一个、
新建后必须重拉 workspace（否则地址栏说 #60、内容还是 #50）、引用断掉点名说是谁丢了、
认不出组件清单的 DASHBOARD 行要说清坏在哪且不给一个能覆盖它的空仪表盘），
以及 `ImportDialog` 5 条（写入中断且回滚没做完时不许出现一个绿色「成功」、回滚做完了也说"未写入"、
错误列表被服务端截到 50 条时用 total-validCount 报 60 条、服务端只登记部分 id 时整批标未确认不许按顺序猜、
全绿才报成功且提交的是映射后的 fieldCode），
以及 `GridViewBulkDelete` 3 条（整批删除只发**一个** `delete-batch`、body 里带
`{appCode, tenantCode, ids}` 且一个 `/runtime/delete` 都不发、报的条数来自服务端 `deletedCount`、
预检没过时不许出现成功文案但要点名是哪条挡住并保留勾选让人重来）。
⚠ 这个文件独立存在且查询写法有讲究（`within` 收窄、按 `tr[data-row-key]` 取勾选框、
`afterEach` 里 `message.destroy()`）—— 原因和实测数字写在文件头注释里，别改回 O(document) 的
`getAllByRole('button')`，那会把整条链路的并发负载拖垮。
以及 `AdminListStates` 13 条（`useResourceList` 五态各一条 + `RelationsPage` 6 条 +
`DictsPage` 2 条：失败要说"没有读到"且**不许出现「暂无数据」**、读到空数组才说"还没有"、
读到后再刷新失败要清行、一个应用都没读到时不许说"该应用还没有关系定义"、
应用 picker 自己失败时不能只留一个空下拉框、横幅里的重试要真的再打一次接口，
外加 hook 那条"换应用后慢一步的旧响应不能落地"）。
以及 `WorkspaceEntityStates` 12 条（侧边栏五种读法各一条：接口失败 / `data:null` / 读到空数组 /
读到实体 / 横幅里的重试要真的再打一次 schema；概览三条：没读到时报"没有读到"且统计位是破折号、
读到才出真实条数、读到但确实为空才提"还没有"；实体候选四条：hook 的 `read:false` 且 `reason`
点名接口、`data:null` 同样算没读到、读到时候 helper 不接管 antd 默认空态、
AI 建模的 toast 在降级时说没读到）。
以及 `WorkspaceRecordStates` 16 条（画廊 5 条：失败说"记录没有读到"且**不许出现「暂无数据」**、
读到空数组才说"还没有记录"、读到就画卡片、横幅重试真发一次 `/runtime/list` 并把卡片画回来、
换筛选后失败要清掉旧卡片；日历 5 条：失败说"本月记录没有读到"且整月空格子不许留、
**诚实的空月照画 42 格且不算失败**、贴片摆进各自当天那一格、换到读不到的月份旧月贴片不许留、
重试真发当月查询；变更历史 4 条：失败说"变更记录没有读到"、读到空历史才说"还没有数据变更"、
读到条目就列出来、面板重试真发 `/undo/history`；外加"一次挂载只查一次"2 条 —— 见上文「记录侧」一节，
那 2 条是日历无限重查（43 次/300ms）唯一的捕手）。
以及 `DesignerEntityStates` 7 条（实体接口失败说"实体列表没有读到"且**不许出现「还没有实体」**、
读到就列出真实那两条、读到空数组才说"还没有实体"、请求在途那一帧侧栏说的是在读、
横幅重试真发一次 `/admin/app/entity/list` 并把清单画回来、
**用真实 `<Link>` 导航换应用**时读失败不许把上一个应用的实体留在侧栏、挂载只查一次且安静期不再补）。
错误断言一律打在**侧栏自己那张 248px 卡片**的 `.ant-alert` 上（`errorAlert(container, …)` 会先取侧栏再找）：
整页 `screen.getByText` 式找词会被页面别处的同一句话喂饱 —— 页面里本来就到处在说"字段""实体"。
mock 的口径也记一下：`respond()` 必须给 `text()`（`client.ts` 读的是 `response.text()` 不是 `json()`），
行数只按 `.ant-table-tbody tr[data-row-key]` 数，点按钮要先 `.replace(/\s/g,'')`（antd 会给
两个汉字的按钮名中间插空格，「重 试」）。

**反证做过**：把内联编辑改成整行提交 → 对应用例立刻红并报
`expected {id:1, customer_name:'张三丰', …} to not have property "balance"`；
把统计查询 `enabled` 改成 false → 页脚那条立刻红；把 workspace 缓存失效那行注释掉 →
「保存后不用刷新，命名视图就出现在切换器里」立刻红；仪表盘那 4 处各自注入缺陷
（草稿不重置 / 新建不 refetch / 组件读的是当前工具栏配置而不是落库那份 / 引用断掉静默少一块）
→ 各自主导的用例红，其中"新建不 refetch"被 3 条用例同时抓到；环图与柱/折线那几条见上面两节
（各自 2 次与 3 次注入，全部单点红、其余绿）；导入那 4 处注入见上面「导入向导的账」一节；
批量删除那两处（后端"坏行只跳过、其余照删" + 前端"逐个单删再自己拼信封"）见上面
「批量删除」一节 —— 那节还记了一条**已知不敏感**（"条数来自服务端"那条看不出请求被拆开），
别把"抓到了"写成"这类错误全都能抓到"。恢复后全绿。
（这条很重要：只有能变绿的门禁不算门禁。）

### P3 零碎
- **待办 #38：`/admin/app/entity/create` 那道闸的 HTTP 形状没有任何一层钉住。** 实测的形状是不对称的：
  `SchemaAdminController` 自己 catch 了 `IllegalArgumentException`，所以这条路由回 **HTTP 200 + `success:false` + `code:400`**；
  只有让异常逃出去的路由才真到 `LcExceptionHandler` 拿到 HTTP 400（`[15i]` 里那批 `s == 400` 断言钉的是后者）。
  `[15j]` **刻意不断言状态码**，只断言信封里的 `success/code` —— 因为那个 200 是我们并不想钉死的产品形状。
  代价是：谁把它改成真 400、或把 200 改成 500，现存的三层都不会红（`grep 撞了引擎自建列 z-lc-web/src/test/` = 0 命中，
  IT 层 `LcHttpContractTest` 压根没有这一族用例）。要么在 IT 层补一条并明确选一种形状，要么在 API 层加一条
  "形状就是 200+code:400"的钉子并写明这是刻意的 —— 别留着"两层都以为对方钉了"。
- **待办 #39：逆向映射那一族只有部署层一张网。** `z-lc-core/src/test/.../mapper/DbTableMapperServiceTest.java`
  存在，但 `grep -l "mapTable" z-lc-core/src/test/` = **0 命中** —— 那个类只测 `toLcType` 的 JDBC→lc 类型映射，
  压根不碰 `mapTable`。也就是说 J5（不再跳过自建列）与 J6（跳过但不留痕）
  只在真 HTTP 那层有覆盖，单测层是空的。注入实验把这件事测出来了，就顺手补两条单测（映射一张含 `id/deleted` 的表，
  断言字段清单里没有它们、description 里有那句）。
- ~~**待办 #42：阶段参数 `config` 是下一个"存了但一个字都不执行"。~~ **已闭（09-26）** ——
  走的是"② 写入口对没人读的参数直接 400"，并且把"这一档有哪几个参数真被读"收成一份登记表
  （`PipelineStages.CONFIG_KEYS_BY_TYPE`）；六层注入自证与两条口径（运行期只 warn 的不对称、
  空词表必须双向钉）见「流水线阶段参数 `config`」一节。
  **留一条给下一个动这里的人**：第一个真参数落地时，除了往登记表里加一行，必须同时补
  "处理器确实读了它"的用例 —— 登记表不是生效的证据。
- 目录命名：`z-opc/AGENTS.md` 约定子模块前端叫 `_frontend/`（如 `z-task/_frontend/`），
  当前是 `z-lc-admin-ui/`。要么改名，要么在 AGENTS.md 记一笔。
- 前端 `antd` chunk 1.26MB 已配 `chunkSizeWarningLimit: 1400` 并接受（应用本体只有 ~130kB，
  vendor 单独缓存），真要再小就得按组件引 antd。
- **提交状态（09-26 02:1x 现数，`git status --porcelain` 与 `git log` 当场量的，不是回忆）**：
  上一记那"44 行全在工作区"的现场已经收敛成一笔 —— HEAD = **`e5504ce`**
  （`feat: 让处理流水线配置真的执行，并把交叉表/字段编码/流水线三层量具补齐`，
  装的就是 #41 那一整族 + 交叉表/字段编码那两层量具 + 上一记那 25 个 ` M` 的绝大多数），
  **且已推送**：`git rev-parse HEAD origin/main` 两个 sha 相同（`e5504ce218ad…`），不是"提交了但还在本地"。
  这一窗（#42）的改动**全部未提交**，全在工作区：`git status --porcelain` = **21 行**
  = **18** 个 ` M` + **3** 个 `??` + **0** 个 `M `（暂存区是空的，三个数各自 `grep -c` 现数，09-26 02:3x 量的）。
  18 个 ` M` 逐个点名（照 `git status` 现列分四堆）：
  java 本体 2（`core/pipeline/Pipeline.java`、`core/pipeline/config/PipelineStages.java`）+
  对应单测 3（`PipelineWriteChainTest.java`、`config/PipelineConfigServiceTest.java`、`config/PipelineStagesTest.java`）+
  前端 4（`src/api/pipeline.ts`、`src/api/pipelineVocabulary.test.ts`、`src/views/admin/PipelinesPage.tsx`、
  `z-lc-admin-ui/e2e/browser-e2e.mjs`）+ 量具与文档 9（`_e2e/e2e_api_test.py`、`_e2e/README.md`、
  `_e2e/mutate_pipeline_wiring_guard.py`、`z-lc-admin-ui/e2e/mutate_pipeline_browser_guard.py`、
  `z-lc-admin-ui/e2e/mutate_pipeline_wiring_guard.py`，以及四支补"备份目录被复用"那个修法的
  `mutate_duplicate_guard{,_deployed,_http}.py` + `mutate_field_code_deployed_guard.py`）。
  2+3+4+9 = 18 对得上；`git diff --stat` = **18 files changed, 912 insertions(+), 165 deletions(-)**。
  ⚠ 上一记这一格写的是"20 行 = **17** 个 ` M`"、加法写 `2+3+4+8 = 17`，
  而**同一句话的第四堆把名字逐个列了 9 个** —— 清单与加数在当时就差 1，不是这一窗多写了一个文件才漂的；
  两行数（+725 −105 → +912 −165）的差才是这一窗的真实增量（收文档 + 改 P1/P4/P6 三处预期集合）。
  ⚠ **别拿 mtime 判"哪些文件是本窗改的"**：变异量具按字节还原过一批源文件，被它还过的 mtime 全部作废。
  ⚠ 3 个 `??` 里 **2 个不是我的**：`z-lc-admin-ui/pnpm-lock.yaml` 与 `pnpm-workspace.yaml`
  是共享工作树里别人在飞的活，**永远别顺手带上**；剩下那个是本轮新量具
  `_e2e/mutate_pipeline_config_guard.py`。
  ⚠ 这一段还是"别人随时会来 commit"的现场：真要提交只能**按路径点名** `git commit -- <路径>`，不能 `git add -A`。
  ⚠ 上一记里那条"字段编码那道闸的产品源码改动呢"依然成立，值得再抄一遍免得有人又去找：
  `src/fields/columnRules.ts` 与 `src/views/designer/DesignerPage.tsx` 与 HEAD 逐字节相同
  （`git ls-files --error-unmatch` 都命中 = 已被跟踪，`git status` 不列 = 没有改动，
  `git log -1 -- DesignerPage.tsx` = `c15788a` 09-23 08:11）—— 那道闸的**产品实现是 09-23 就提交过的既有代码**，
  后两轮动的是它周围的三层量具（API `[15j]`、浏览器 11c、两支注入自证），不是闸本身。
  别把"没改闸"读成"闸没在做"。

### ✅ 部署演练怎么跑：第一次把 z-lc 打到**真 MySQL 8** 上（250），一次撞出六个缺陷（2026-09-26，#51/#54/#55/#56/#57/#58）

**为什么要练这一条**：dev profile 吃 `jdbc:h2:mem:zlc`，进程一死库就没了 —— 于是"能在另一台机器上、
对着真的 MySQL 部署起来并且自证连的是它"这件事从来没被实测过。四层门禁全绿证的都只是 H2。
第一次真跑就撞出坑，每一个都**只在真 MySQL 8 / 真远程机上才会出现**；其中 #51/#54/#57 是产品的，
#55 是部署脚本的，#56/#58 是**量具自己的** —— 后两类都要写进账，因为"闸红过"这件事本身也是被测的。

    bash _e2e/deploy_250.sh all       # sync → db → schema → env → start → verify → collate → tunnel
    bash _e2e/deploy_250.sh gates     # 闸1 + 闸2 + 闸3 + 闸4 各自"正向绿 → 注入红 → 恢复绿"跑三段
    bash _e2e/deploy_250.sh gate2     # 单道闸也能单独跑（改了哪一道的判据只重跑那一道；整批 gates 会把
                                      # app.env 换来换去并重启三次）
    bash _e2e/deploy_250.sh collate   # 只问物理表实际校对（闸 3 的判据，不打应用）
    bash _e2e/deploy_250.sh repair    # 把闸 3 给的 CONVERT TO 真的执行掉，然后立刻用同一把尺复测
    bash _e2e/deploy_250.sh api       # 建隧道并让接口层门禁打远程：http://localhost:18099 → 250:18090

形状上的四条约定（都被踩过才定下来的）：
- **远端只吃 stdin**（`ssh 250 bash -s -- <step> < deploy_250_remote.sh`）：远端不落一份副本，否则下次改脚本
  改的是本机这份、跑的是那份。参数走 step 后面，往远端塞变量用 `ZLC_EXTRA_ENV='K=V'`（值里别带空格）。
  ⚠ 这条约定反过来咬了量具一口（#58）：**脚本内部不能再出现 `docker exec -i`** —— `-i` 会跟脚本抢同一份 stdin，
  实测三条查询的 heredoc 只印出第一条（本机三次复现）。除 `step_schema` 那处真要灌文件的地方以外全部去掉 `-i`
  并补 `</dev/null`。这一支只写实测到的相关性，不写"为什么"的理论。
- **凭证只在 `250:~/.config/z-lc-deploy/{mysql.env,app.env}`（0600）**，仓库里一个字都不留。
  ⚠ 但 `app.env` 的值会出现在 java 进程的 argv 里（`--spring.datasource...password=…`）—— 取证命令一律带
  `sed 's/password=[^ ]*/password=<redacted>/'`，别把 `ps` 原文贴进日志。
- **`all` 里刻意不放 `repair`**：`CONVERT TO` 是动已有表的 DDL，得运维自己按一次，不能藏在"部署"里顺手执行掉。
- **81MB 的 jar 传完要做字节对账**（`sync` 打印 `jar 字节一致：<md5>`）：传坏了的症状是"行为古怪"，不是报错。

四道闸与各自的负控（每一道都是"注入的缺陷形状能红"才算存在，见下面 #55/#56/#58 那三格——闸自己也坏过）：

| 闸 | 判据 | 负控形状（实测注入） |
|---|---|---|
| 闸 1 `gate1` | 写 `app.env` 时逐行过 `%q` 校验：值不带引号写进去会被 shell 吃掉 | 同一份校验，`%q` 写的过、裸写的当场拒（退出码 1）；随后重新生成 app.env 并复检 |
| 闸 2 `verify`/`gate2` | **先归因再判读写**：端口上的监听者必须 == `app.pid`；然后 API 写一条、读回，再直接查 `z_lc.z_lc_app`（主池）与 `z_lc.z_lc_event`（LC 池）各 `rows=1` | 把数据源临时指向 `z_lc_misdeploy`：API 照样"写成功"、health 照样 UP，而 `z_lc` 里 `rows=0` → 具名报「进程连的根本不是这个库」（退出码 1）；恢复后绿，并回收负控库 |
| 闸 3 `collate`/`gate3` | 参照 = `information_schema.columns` 里多数派校对（实测 `z_lc.z_lc_dict_item.item_code` = `utf8mb4_general_ci`）；库里每一张表的每一个字符串列都必须在它上面，否则按表给出**可直接执行**的 `ALTER TABLE … CONVERT TO CHARACTER SET utf8mb4 COLLATE …` | 建一张只写 `DEFAULT CHARSET=utf8mb4`（不写 COLLATE）的探针表 —— 用的就是**旧版代码自己会写出来的那句 DDL**，所以这一支证的是"这道闸认得那个真实形状"，不是"我能造一张怪表"。三段式：起点绿 → 注入红（且红必须落在闸 3 的判据上、点名里必须有探针表）→ `trap` DROP 之后绿 |
| 闸 4 `gate4` | **打应用而不是打库**：走真 HTTP 建应用 + 建实体（字段一栏不缺），provision 之后拿 `information_schema.tables.table_collation` 对表实际校对 —— 建出来的表必须在参照上（这是 #51 的修法在真库上兑现了），再把那张表 `CONVERT TO` 漂到 `utf8mb4_0900_ai_ci`（**utf8mb4 在 MySQL 8 的默认校对**，也就是修复前引擎自己会写出来的那个形状），provision 必须判 `FAILED` 且应答里带着指向正确校对的那句 `CONVERT TO`，搬回之后必须 `EXISTS_INTACT` | 注入的就是"表一栏不缺而校对漂了"这一支 —— 它正是**缺陷 #57 的原始读数**：修之前 provision 回的是 `EXISTS_INTACT`，闸 4 当场报 `!! 闸 4 咬不住`。三段之外还钉了一条"注入必须真的落地"（漂完当场回读 `table_collation`，没漂成功这一轮的结论不作数）。实体 id 是**从 MySQL 里读**的而不是从应答 JSON 里 sed 的（`createEntity` 回来的 DTO 第一个 `"id"` 是**字段**的 id，拿它去 provision 只会 `Entity not found: 117`）。清场挂 `trap … EXIT`（不是 `RETURN`，因为红路径走的是 `die` = `exit`），残留实测 `表=0 应用=0 实体=0` |

⚠ **这一族的层界（写清楚，否则下一窗会当"全绿 = 都证过"读）**：#57 在 java 单测、vitest、接口 E2E **三层都结构上打不到**
—— dev 的 H2 压根没有 `information_schema`，`metadataCharsetCollation()` 问不到参照，那一整块 `if` 不走。
本窗把 `FakeJdbc.queryForObject` 改成按 250 实测的目录形状答话（问错列名就抛），M13 才有牙（43 例里恰好红那一条
`provisionShouldFailWhenTableCollationDiffersFromMetadataLayer`）；但**"MySQL 8 的 tables 视图里那一列到底叫什么"
这一半不在单测层**，它只由闸 4 在真库上证。替身能模仿真库，不能代替真库。

`mutate_collation_guard.py`（**18 支**注入：M1–M12 打 #51 那一族、N1–N5 打 #54 那一族、M13 打 #57 那一支）的写法值得抄：
注入用的是**旧版代码自己会写出来的那段字节**（`M1 建表子句退回只写 charset`、`M13 表级那一问退回 collation_name`），
不是随手造的怪代码；每支跑完必比"产物指纹变了 / 还原后回到基线指纹"，否则那一轮的结论不作数。

⚠ **`api` 那一轮在共享库上是会留东西的**：接口门禁每跑一次，`z_lc` 里就多一批 `e2e_*_<随机后缀>` 的实体表
（本窗实测 `z_lc` 从 39 张涨到 **49** 张，逐名列出后确认新增的都是 `e2e_pq_*` / `e2e_pipe_*` / `e2e_fc*` 这几族探针表，
`z_lc_*` 元数据 14 张一张没多）。这不是脏数据（每一张都是那一轮真打出来的表），但**别把"表数"当判据**——
它只说明"这里跑过几轮"，闸 3 的判据是校对而不是表数。

---

### ⚠ 缺陷 #69：本机 rollup **不逐字节可复现**，"产物回到基线字节"这一条判据没有猎物（2026-09-27，浏览器注入量具）

出处是 `z-lc-admin-ui/e2e/mutate_workflow_browser_guard.py`（18 支 W1–W18，打 `WorkflowsPage.tsx`）的第三轮整族跑，
尾行 `RESULT: 1 problem(s)`，而那一整轮 18 支注入**逐支 OK**、无一条连带红 —— 唯一那条红出在恢复轮，逐字（`~/.cache/zlc61/browser_guard/run3.out:138`）：

```
  !! 恢复后产物 index-ruBdvAdn.js|d6d15714… != 基线 index-DBJrrqb7.js|5f2afecc… —— 源码回来了而产物没回来
```

它想防的是"变异没还原"。但它抓不到那一轮真正要防的东西，反而是**必然红**：`PAGE` 的 md5 在恢复轮前后是同一个值
（`87b30e33…`，本窗再次实测仍是这个），
也就是说**源码逐字节回来了它照样红**。一条"几乎必然红"的判据和一条"几乎必然不触发"的判据是同一枚硬币 ——
前者会把"量具有病"记成"产品坏了"，本窗就是这么撞上的。

漂移不是罕见事件，是这一族的常态。六次"干净 src"构建，两个名字，交替出现（每行的日志现在还在盘上）：

| 时刻 | 谁构建的 | 产物 | 那时 src 是不是同一份 |
| --- | --- | --- | --- |
| 03:44:25 | `build_7b.log`（本窗早先的一次 build） | `index-DBJrrqb7.js` | — |
| 05:02:10 | 整族第三轮基线 | `index-DBJrrqb7.js` | — |
| 06:00:02 | 整族第三轮恢复轮 | `index-ruBdvAdn.js` | `WorkflowsPage.tsx` md5 与基线前后逐字节同为 `87b30e33…` |
| 06:09:21 | 收窄跑基线 | `index-ruBdvAdn.js` | — |
| 06:15:11 | 收窄跑恢复轮 | `index-DBJrrqb7.js`（← 与本轮基线不同名） | 新判据现量：恢复轮 src 指纹 **==** 基线指纹 |
| 06:19:52 | `npm run check` | `index-ruBdvAdn.js`（← 与上一行同 src） | 中间只改过 `_e2e/README.md`（不在 `src/` 里） |

看第 3–5 行：**恢复轮和紧随其后的 `npm run check` 拿同一份 src 得出两个不同名**，方向和第三轮还相反。
所以产物名在这一层不能当身份，连"两次同名 ⇒ 同一份源码"这种反方向推都不成立。

修法（判据换尺，不是把红改成不红）：
- 恢复判据改为 `src_digest()` —— `z-lc-admin-ui/src/**` 逐文件（相对路径 + 字节）的 md5。它管得住旧尺管不住的那一半
  （`PAGE` 之外的文件被留在变异态），也不管产物名。
- 产物名**照旧打印**，只是降级成信息：漂移要看得见，但不许当判据。
- 新增 `--only <Wxx>` 收窄跑，并强制它在日志头上写「⚠ 收窄跑：…**不能**当『W1–W18 整族自证』的账」——
  收窄跑的 `RESULT` 只证明改过的判据在真 build/真浏览器轮次里跑得通。
- 新增 `--selftest`（不 build、不抢 5274）：A 动 `WorkflowsPage.tsx` **之外**的一个 src 文件 ⇒ 指纹必须变而 `PAGE` 文本逐字不变
  （把旧尺的瞎处演出来）；B 撤掉探针 ⇒ 指纹必须回到基线（回不去就是还原步没做完，新判据自己就是空的）；
  C 造一支"锚点落空"的变异 ⇒ 指纹不变，证明 `main()` 里那条新红支可达。06:0x 实测 `SELFTEST RESULT: 0 problem(s)` / `SELFTEST_EXIT=0`。

收窄跑（`--only W12`，06:09:16 起，`run4_narrow.out`，`NARROW_EXIT=0`）的实际读数：基线 `=> PASS 279 / FAIL 0`、
W12 红 6 条（与整族第三轮那 6 条**逐字相同** —— 把行尾墙上时钟归一后 `diff` 为空）、恢复轮 `PASS+FAIL 279 / 红 0`，
而这一轮照样发生了上表那次产物漂移 ⇒ **它没有再红**。这是新尺的第一次实战：旧尺在这一轮会给出第二条假红。

⚠ 别把这条结论外推到 java / 部署层：`_e2e/mutate_collation_guard.py` 那类量具写的
"每支跑完必比『产物指纹变了 / 还原后回到基线指纹』"（见上一节）在它们那一层是**有牙的** —— 编译件逐字节可复现。
坏的不是那条判据，是把它搬到一台 rollup 不确定的机器上。

---

### ⚠ 缺陷 #66：`/update` 的查重跑在租户归一化之前 ⇒ `tenant_code = NULL` 恒筛不到行，同 KEY 的第二条照样落库（2026-09-27，java 层）

**机制（这一条不是"忘了判"，是结构上判不到）**：`WorkflowBindingController` 的 `/update` 照 #48 那批的口径把请求带来的租户钉成 `null`
（写入口不许自带租户，归属只能由库里那一行决定），而 `WorkflowBindingService.update()` 里
`entity.setTenantCode(existing.getTenantCode())` 写在 `requireNotDuplicate(...)` **之后** ⇒ 查重那条 wrapper 的 `tenant_code` 绑的是 **null**。
MyBatis-Plus 的 `eq(col, null)` **不是**"这一列不加条件"，它照样生成 `tenant_code = #{...}` 并把 NULL 绑上去；
而 SQL 里 `col = NULL` 对任何行都是 UNKNOWN ⇒ 那一列**把全表筛光，也把自己那条筛掉** ⇒ 恒 0 行 ⇒ **恒不拒，而它自己不知道**。
落库的后果是同一 `(tenant, app, entity, triggerEvent, processDefinitionKey)` 真的有两行，
而 `WorkflowTriggerDispatcher.afterCreate(...)` 是**按绑定逐条发单**的 ⇒ 界面上"一条流程"实际对外发两句。

**修法**（`z-lc-core/.../WorkflowBindingService.java`，三处，缺一不成立）：
1. `normalizeBeforeJudgement(entity)`（只剪 `triggerEvent` / `processDefinitionKey` 两端空白）在 `create()` 与 `update()` 里都提到
   `WorkflowTriggers.validateForWrite` + `requireNotDuplicate` **之前** —— 判定必须用归一之后的值，这是 #61 那一族的同一课；
2. `entity.setTenantCode(existing.getTenantCode())` 挪到查重**之前**；
3. `requireNotDuplicate` 开口一条哨兵：`candidate.getTenantCode() == null` ⇒ **拒判**（抛「查重前必须先确定这条绑定属于哪个租户」），
   而不是"查不到就当没重复"。⚠ 这支哨兵钉的是"以后再接一个不带租户的调用方时它必须响"，**不是**"线上现在会走到这里"
   （现在两个调用方都给非 null、DDL 也是 `NOT NULL`）—— 这个区别写在那支测试的注释里，别让下一窗把它读成线上事故复现。

**它为什么能从四道闸里全绿走出来（三层原因，比缺陷本身值钱）**：
1. **HTTP 门没打过这条路**：`git show HEAD:` 那份 `WorkflowTriggerContractTest` 里 `binding/update` 出现 **0** 次（工作树 = 1 次，就是这一支加的门）
   —— #61 那一族测的是 `create` 查重，`/update` 改成同 KEY 这条路径一次都没请求过。
2. **core 那几行夹具没有猎物**：历史代码在 update 这条路上查不到行 ⇒ "应当拒"的断言红不了，不是因为断言软，是因为没有一条用例把
   "调用方不带租户 + 撞已有 KEY" 这个形状种出来（`updateShouldRefuseDuplicateEvenWhenTheCallerSendsNoTenant` 就是那个猎物）。
3. **测试替身自己也有病**（07:2x 现挖出来的，也是我对 I1 的预测被实测否掉的原因）：
   `WorkflowBindingServiceTest` 里模拟 MyBatis-Plus wrapper 的 `eq()` 写的是 `wanted == null || String.valueOf(wanted).equals(...)`，
   即"绑的是 null 就算这一列不筛" —— 而真 SQL 的语义**正好相反**（绑 null ⇒ 全筛掉）。
   后果是"跨租户的行照抄出来"，症状是 `expected:\<[]> but was:\<[keep, foreign-tenant]>`。
   修法是**把尺改严**并让它自己带猎物/反对照，两条都写进 `fixtureReallyAppliesWrapperPredicates`：
   `predicateKey()` 先问"这一列到底有没有谓词"，有就必须取到的值相等；
   - 正向：`listByEvent(null, APP, ENTITY, AFTER_CREATE)` 必须回 **空**（绑 null = 筛掉一切）；
   - 反向：`listFires(TENANT, APP, null, null)` 必须回 **1 行**（没有谓词的那一列不许被当成"筛掉一切"）。
   两支方向相反，只改一头都会红 —— 这条尺同时是 `#61`/`#48` 那两族以后所有 wrapper 断言的地基，
   ⚠ 另外 5 个用 `getSqlSegment()` 的测试类（relation/app/schema/permission/dict）当时都还是"只看列名不看绑值"的宽松写法，
   只有 workflow 这一族真在值上绑 `eq()`，所以那 5 族目前**没有**这个病，但也**没有**这把尺。

**成对注入自证（七跑矩阵：07:1x–07:2x 六跑 + 07:5x 补 I5；备份 `cp` 到 `~/.cache/zlc66/`、还原也只 `cp`，没拿 `git checkout --` 当还原步）**：

| 跑 | 生产码 | 测试替身 | core 读数（`~/.cache/zlc66/`） | web 读数 |
|---|---|---|---|---|
| b | 修好的（工作树那份） | 宽松 `eq()` + 本批新增断言 | **29/0** BUILD SUCCESS（`core_66_b.log:125`）| — |
| I1 | 历史的（`git show HEAD:` 那份字节 `e510218…`） | 同上 | **Failures: 2**（`core_66_injected.log:195`）= `requireNotDuplicateRefusesToJudgeWithoutATenant`、`updateShouldRefuseDuplicateAcrossWhitespaceInTheEventAndKey` | **Failures: 1**（`web_66_injected.log:816`）= `updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice`，逐字 `expected: <400> but was: <200>`，而 body 里 `"processDefinitionKey":"p_first"` —— **缺陷本体在 HTTP 层的形状**（同 KEY 的第二条被收下了，且行已被改写） |
| a | 修好的 | faithful（绑 null = 全筛掉） | **29/0** BUILD SUCCESS（`core_66_faithful.log:125`）| **14/0**（`web_66_a.log:475`）|
| I3 | 历史的 | faithful | **Failures: 3**（`core_66_injected_faithful.log:229`）= I1 那两支 + `updateShouldRefuseDuplicateEvenWhenTheCallerSendsNoTenant` | — |
| I2 | 修好的 | 只把 `eq()` 换回宽松写法 | **Failures: 1**（`core_66_loose_eq.log:162`）= `fixtureReallyAppliesWrapperPredicates`，`expected:\<[]> but was:\<[keep, foreign-tenant]>` | — |
| I5 | 修好的，**只摘 `create()` 里那一处 `requireNotDuplicate` 调用**（`update()` 那句不动） | 同 a | **Failures: 2**（`core_66_no_dedupe.log:197`）= `createShouldRefuseADuplicateOfTheSameEventAndProcessKey`、`createShouldRefuseADuplicateWhoseKeyOnlyDiffersByWhitespace` | **Failures: 1**（`web_66_no_dedupe.log:726`）= `duplicateBindingIsRefusedWhileTheFirstOneStillFires` 撞 `HTTP 状态应当是 400`（应答 body 里 `"id":2` 正是那条本该被拒的重复绑定） |
| 还原 | 从 `~/.cache/zlc66/WBS.java.fixed` `cp` 回工作树（逐字节对账 `e2936a…`） | 同 a | **29/0** BUILD SUCCESS（`core_66_restored.log:118`）；I5 之后又 `cp` 还原一次并复跑：**29/0、BUILD SUCCESS、rc=0**（`core_66_restored_i5.log:125`），`grep -c "MUT(I5)"` = **0** | — |

⚠ 四格要如实记的账：
- **预测被实测否掉了一半**：工单上写的是"两步一起摘 ⇒ `requireNotDuplicateRefusesToJudgeWithoutATenant` +
  `…EvenWhenTheCallerSendsNoTenant` 这两支 + HTTP 那一条同时红"。实测 HTTP 与第一支如约，
  但 **I1 红的第二支是 `…AcrossWhitespaceInTheEventAndKey`，而 `…EvenWhenTheCallerSendsNoTenant` 在历史代码上照样绿**
  —— 支数对上了（core 2 + web 1），**是哪两支对不上**，这种差只有真跑一遍才暴露。
  查下去不是测试写错，是上面第 3 层那把病尺把 `tenant_code = NULL` 当成了"不筛租户"，于是跨租户那一行被"查"了出来、
  查重"成功"拒了一次 —— **病尺把这个缺陷反向掩盖成了正常行为**（真库里恒 0 行、恒不拒）。挖出它才有 I3/I2 这两跑。
- `createShouldRefuseADuplicateWhoseKeyOnlyDiffersByWhitespace` 在 **I1/I3 下都是绿的**：历史那份 `requireNotDuplicate` 自己在比较 KEY 时对两边都调了 `.trim()`
  （现读命令：`git show HEAD:z-lc-core/src/main/java/com/zifang/z/lc/core/workflow/WorkflowBindingService.java | grep -n "\.trim()"` —— 别在账里钉静态行号，
  这一族改一次代码那些号就位移一次），所以 create 那条路的空白早就被处理了。这一支**不是 #66 的猎物**，不许算进"具名红"，也不算"逃过 0 支"里的那一支。
  ⚠ 但"不是猎物"不等于"空桩"：**I5**（只摘 `create()` 那一处查重调用）把它**测成红**了 ⇒ 它对 create 这一路真有牙，
  只是那支牙不咬 update 这一族的缺陷。这一支是 07:5x 补跑的，因为原工单 ② 点名的就是"摘掉 `requireNotDuplicate`"，
  而先头那六跑一支都没打过它 —— **我先把那格勾成"已跑"，回读时才发这里少一支，于是补跑而不是改账**。
- **数总句数抓不到这个缺陷**：`STUB.count()` 在修法前后都是 **2**（两条各不重复的绑定各发一句本来就是 2）。
  所以那一支 HTTP 断言钉的是"每个 KEY 各出现 1 次 + 账上 2 行各指各的记录"，不是总数。
- **命令本身有坑，工单那两条当时跑不出来**：`mvn -o -pl z-lc-core -am -Dtest=WorkflowBindingServiceTest test` 会在 `z-lc-common` 上
  `BUILD FAILURE: No tests matching pattern "WorkflowBindingServiceTest" were executed!`（`core_66_a.log:46` 逐字留着）。
  `-am` 带上游模块就必须加 `-Dsurefire.failIfNoSpecifiedTests=false`，TASK.md 里那两条已按实测改写。

⚠ **覆盖缺口（认下来，不当已闭）**：#66 这一族**没有仓内永久注入器**。`_e2e/` 里打流程绑定的注入器只有
`mutate_workflow_trigger_guard.py`（M1–M6，摘的是 #61"存了但一个字都不执行"那几个写入口），本窗这七跑是手工成对自证。
也就是说：#66 现在靠 3 支 core 断言 + 1 支 HTTP 断言钉住，回归由全量门禁（本窗 4774）兜，**不由注入兜**。
下一支要补的话，形状照 `mutate_health_honesty_guard.py`：注入"把 `setTenantCode(...)` 挪回查重之后"与"摘掉 null 哨兵"两支，
预期红集就是上表 I1/I3 那三支（并把 faithful 的 `eq()` 钉进量具，否则第二层原因会复发）。

---

### ✅ 缺陷 #70：部署中心是下一个「存了但一个字都不执行」（2026-09-27，四层 + 一跑注入）

**盘面（修之前，全部实测）**：`DeploymentController.create` insert 一行 `PENDING` 然后
`// TODO: 异步执行物化/部署逻辑` 直接返回 200 —— 全仓没有执行器，`updateDeploymentStatus` 在生产代码里
**零调用者**，所以 `PENDING` 是**终态**：界面弹「部署已创建」、状态列永远灰、`deploy_log` 抽屉永远写「（暂无日志）」。
同一趟还照收不存在的 `appCode`、不存在的 `materializationId`，以及不给 `deployType` 时 NOT NULL 撞出的裸 500。

**修法（口径与本仓 #41/#42/#61/#66 同族）**：新增 `DeploymentTypes` 作唯一词表（`HOT_LOAD` 一种有执行器；
DOCKER / GIT_PUSH / SQL 各带一句"服务器为什么做不了"），写入口 `validateForWrite` 拒掉兑现不了的形态，
能兑现那一种**真的同步执行**（`schemaAdminService.provisionAllTables`）并把结局 + 四计数汇总写回那一行，
`trigger` 返回**重读后**的那一行；`/deployment/vocabulary` 成为界面下拉项的唯一来源
（`_deployment.ts` + `DeploymentsPage` 从它长清单，被拒的那几种摆在窗里并点名原因）。
全貌与裁定记在 `_doc/003_待办事项/feature001_workflow_binding_fires/TASK.md` §2.12。

**注入自证（永久量具 `_e2e/mutate_deployment_guard.py`，7 支 / 19 条具名红）**：
J1a 摘写入口闸、J1b 摘"执行完把结局写回"、J2 摘 vocabulary 的可执行面、J3 让 `trigger` 返回伪造行、
F1..F3 打前端（下拉抄一份清单 / 失败也说"部署已创建" / 词表读不出照开草稿）。逐支读数、预期红集与
"哪两条读同一句 toast"这类归因都在 `~/.cache/zlc70/mut/ledger.json`（`ts 2026-09-27 09:15:02`，
`RESULT 7 支判定，其中 MISMATCH: 无`，分母 core 19 / web 7 / ui 7 每轮核等值、漂了就抛）。

⚠ **同一窗在量具身上撞出的、另立缺陷 #72 的那一支**（其余几条量具账：TASK §2.12 末「这一窗在量具自己身上抓到的四件事」，
交接表末行还有一条 mtime 假红）：
接口层那一节的 z-wf 桩原先只绑 `127.0.0.1`，而 jar 里的 okhttp 连 `localhost` 优先解析到 `::1`
⇒ `[15w]` 整节 30 条红、报的却是"桩没起来或 jar 没打过来"（方向不给）。修 = 两族 loopback 都绑 +
红消息里带上 jar 自己记账那句 `detail`；**没解释清的那一半照实留着**：同一份只绑 v4 的桩 08:50:51 还是
532/532 全绿，08:54:13 起在同一支 JVM 里恒红，我没有量"中间翻了什么"，所以这一格不写成因。

⚠ **覆盖缺口（认下来，不当已闭）**：
1. **浏览器层对 `DeploymentsPage` 零断言** —— 本窗现读：`grep -c "deploy" z-lc-admin-ui/e2e/browser-e2e.mjs` = **0**
   （那支量具里连"部署"这个字都只出现 1 次，且是注释里"部署件"那三个字）；`279 → 279`、新增 0 / 消失 0（两套 PASS 标题唯一集现算）。
   这正是 #49 当年在权限页上的同一形状：vitest 那 3 条只证明"词表长成这样时组件画成这样"，
   证明不了真服务端给的词表长这样、也证明不了点下去那一下真到了服务器。
2. **250 那半条腿这一窗仍然没量**（09:32:37 现读：22 端口 TCP 握手 rc=0 而 `ssh` rc=255 `kex_exchange_identification: read: Connection reset by peer`，见下面交接表那一格），部署这件事在真 MySQL 8 上的结局没有读数。


---

## 交接状态（本轮收尾时实测，不是回忆）

四层门禁当前状态（最前面那张 **09-27 09:1x–09:2x** 的表是**当前数**；后面那几张是历窗的账，逐格保留作历史与教训出处，
**不是当前数**。
⚠ 上一版这行指的是 07:2x 那张。09-27 09:1x 这一窗 java / 接口(H2) / 前端 / 真浏览器 **本地四层**同轮重测过，
而**打真 MySQL 8 的那两格这一窗量不了**（250 的 sshd 建不起会话，rc 现读在下面表里，别把 09-26 那两个 469/469 当当前数）。
表里每一行的时间戳才是证据，标题里的窗口只是"这一批数是哪一窗的"，别把它当成"下面都是老数"）：

**09-27 09:1x – 09:2x 这一窗（#70 收线：部署中心从「insert 一行 PENDING + TODO」变成"能兑现的那种真的执行、兑现不了的那种写入口就拒"；
顺带在量具身上撞出 #72：接口层的 z-wf 桩只听 IPv4，而 jar 里的 okhttp 连 `localhost` 优先打到 `::1`）本地四层全部同轮重测，日志逐层落在 `~/.cache/zlc70/`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**（`gate1-recheck.log`，`Total time: 17.972 s`、`Finished at 2026-09-27T09:15:59`）、**4788** 个用例 0 红 0 错 0 跳（5 条不带 `-- in` 的模块汇总行现加 = 2701+525+**1337**+112+**113**）。**4774 → 4788 的 +14 两把尺互证**：surefire 模块级差是 core +7 / web +7，而 `git ls-tree`+`git show` 与盘上各数一次 `@Test` 的差**逐文件落在两支上** —— `DeploymentServiceImplTest` **12 → 19**（+7）与全新的 `DeploymentContractTest` **0 → 7**（+7），加总正好 14，其余测试文件 0 变动。产物 `z-lc-admin/target/z-lc-admin.jar` md5 `37402c05baa6b1e53c0435f110ac55f9`（inode 141692193，09:15:58）|
| 接口 E2E（本机 H2） | `python3 -u _e2e/e2e_api_test.py http://localhost:18090` | **546/546**（`gate2-final.log:616` 逐字 `=== E2E RESULT: 546/546 passed ===`，`grep -c "^  PASS"` 也 = **546**、`grep -c "^  FAIL"` = 0）。532 → 546 的 **+14 / 消失 0** 是 PASS 标题唯一集现算的（与 `zlc66/api_66.log` 逐条求差），14 条全在 `[11]` 那一节（词表两栏 / 三种被拒方式各 400 且一行不进账 / 可执行那种走完不再是 PENDING / 库里读回 = 返回的那一行 / 不给 deployType、不存在的物化批次、不存在的应用）；**分母 33 段、`[15w]` 自身仍 63 条没动**。⚠ 打的是本窗 09:15:58 那份件：`lsof` 现读 18090 监听者 = pid **57406**（`ps -o lstart` = `Sun Sep 27 09:17:53 2026`），它的 `NODE` = **141692193** = 上面那个 jar 的 inode ⇒ 起的就是这一窗新装的件 |
| 接口 E2E（**真 MySQL 8**，隧道 18099） | `bash _e2e/deploy_250.sh api` | **这一格这一窗没重测 —— 不是绿，是没量。** 09:32:37 现读：`nc -z -w 4 192.168.31.250 22` **rc=0**（`port 22 [tcp/ssh] succeeded!`），而 `ssh -o ConnectTimeout=6 -o BatchMode=yes 250 'echo alive'` **rc=255** + `kex_exchange_identification: read: Connection reset by peer` ⇒ **端口在听 ≠ 会话建得起来**。这与本机记忆里 250 那台从 09-27 06:40 起的失联签名**同形**（TCP 开而 sshd banner 被 RST，判的是 userland 卡住），所以这里**不再重试第二遍**（那条账的纪律就是"判完就停，别敲 ssh"）。别把 09-26 那两个 469/469 当当前数 |
| 前端 | `cd z-lc-admin-ui && npm run check`（tsc + `eslint --max-warnings 0` + vitest + build） | **rc=0**（`gate3-final.log`，09:21:50–09:22:19）：tsc 0 / eslint 无输出 / **vitest `Tests 259 passed (259)`、`Test Files 30 passed (30)`** / build `✓ built in 2.65s`。252 → 259 的 **+7 机械核过**（与 `zlc66/check_0727.log` 的逐文件计数求差：新增 `DeploymentsPage.test.tsx` **3** + `deploymentVocabulary.test.ts` **4**，`removed` = 空集、其余 28 个文件计数**逐一相同**）。本窗为修 tsc 报的 `TS2532` 动过 `DeploymentsPage.tsx` 一行（`firstLine` 里对下标取值补 `?? ''`）|
| 真浏览器 | `E2E_BASE=http://localhost:5274 E2E_API=http://localhost:18090 node e2e/browser-e2e.mjs` | **`=> PASS 279 / FAIL 0`**、`全绿轮次: 1/1`（`gate4-final.log`，09:18:50–09:21:38，`REPEATS=1`）。**279 → 279、新增 0 / 消失 0**（两套 PASS 标题唯一集现算）⇒ 这一窗 #70 在浏览器层**一条断言都没加**，`deploy` 那支量具里出现 **0** 次 —— 记成覆盖缺口，不算"已被上面三层兜住"（见上一节末）|
| 注入自证（#70 族，三层一跑） | `python3 _e2e/mutate_deployment_guard.py`（**7 支**：J1a/J1b/J2/J3 + F1..F3） | 末行逐字 `RESULT 7 支判定，其中 MISMATCH: 无`（`mut/sweep_after_ui_fix.log`，09:14:03–09:15:10），台账 `mut/ledger.json` `ts 2026-09-27 09:15:02`；分母每轮核等值 **core 19 / web 7 / ui 7**；逐支红 = J1a **core 2 + web 3**、J1b **1/1**、J2 **1/1**、J3 **2/2**、F1/F2/F3 各 **ui 2**；起跑与收尾各有一轮"不注入"基线，三层各自 **0 红**；收线后 09:3x 再核一次残留：`grep -rn "MUT(" --include='*.java' --include='*.ts' --include='*.tsx' z-lc-core/src z-lc-web/src z-lc-admin-ui/src` = **0 处**，同一条命令带阳性对照（`grep -rn deployType z-lc-core/src/main/java` = **19** 命中）⇒ 那个 0 是"尺跑了且没找到"，不是"尺没跑"|
| ⚠ 量具层（#72，同一窗） | `_e2e/e2e_api_test.py` 里那一族 z-wf 桩 | 症状：**30 条 `[15w]` 红、总盘 516/546**，红消息只说"桩没起来或 jar 没打过来"。归因三步：`/fires` 里 49/50/51 三行的 `detail` 逐字写着 `Failed to connect to localhost/[0:0:0:0:0:0:0:1]:8888` ⇒ 桩只听 `127.0.0.1` 而 okhttp 连 `localhost` 先解析到 `::1`；换一个双族监听的桩立刻 `STARTED`（id 51）。修法 = **两族 loopback 各起一支**（不是 `::` + `IPV6_V6ONLY=0` —— 那等于把进程启动期的端口开到局域网）+ 红消息里带上 jar 自己那句 `detail`。**没解释清的那一半照实记**：同一份只绑 v4 的桩在 08:50:51 还是 532/532、08:54:13 起在同一支 JVM 里恒红，"中间翻了什么"我没量，所以这一格不写成因 |
| ⚠ 时序与另一处同形病 | 注入扫完 ⇒ `dist/` mtime 被"按字节还原"顶新 ⇒ 闸 4 直接 `rc=2` | 闸 4 的新鲜度判据读的是 **mtime**，于是报出 274s 的"产物过期"假红（`npm run build` 一次即解）。这与缺陷 #69 是同一把病尺（名字/mtime 不是身份）；**修法应当换成内容哈希**，本窗只记录不改 |

**09-27 07:1x – 07:3x 这一窗（#66 收线：`/update` 的查重跑在租户归一化之前 ⇒ `tenant_code = NULL` 恒筛不到行、同 KEY 的第二条照样落库；
顺带挖出 core 那个测试替身把"绑 null"当成"不筛租户"——那是 #66 能从四道闸里全绿走出来的第三层原因）本地四层全部同轮重测，日志逐层落在 `~/.cache/zlc66/`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**（`install_full.log:4386`，`:4388` `Total time: 16.894 s`）、**4774** 个用例 0 红 0 错 0 跳（模块汇总行现加 = 2701+525+**1330**+112+**106**）。⚠ 16.9s 这个耗时不像"跑了 4774"，所以数**不是从体感推的**：`[INFO] Building z-lc-*` 现数 7 个模块、5 条不带 `-- in` 的汇总行逐条取（`:706/:1052/:2660/:2983/:4326`），另有 07:24:32 落盘的 81 MB `z-lc-admin` jar 作证这局真走完了。**4706 → 4774 的 +68 拆到文件级，且两把尺互证**：`git ls-tree` + `git show` 逐文件数 `@Test` ⇒ `4fee620`（上一窗 18:3x 那批件所在的提交）树 core **1276** / web **92**，与上一窗 surefire 的模块数**逐字同**（分母可信）；`HEAD` 树 core 1326 / web 105（= **+63**：`WorkflowTriggerDispatcherTest` +17、`WorkflowBindingServiceTest` +15、`WfAdapterTest` +10、`WorkflowTriggersTest` +8、09-26 23:28 `3b05b73` 新增的 `WorkflowTriggerContractTest` +13）；工作树 1330 / 106 = 本批 #66 只贡献 `WorkflowBindingServiceTest` **25 → 29**（+4）与 `WorkflowTriggerContractTest` **13 → 14**（+1）。**剩下那 +63 一条都不许记到 #66 这一支账上** |
| 接口 E2E（本机 H2） | `python3 _e2e/e2e_api_test.py http://localhost:18090` | **532/532**（`api_66.log` 末行逐字 `=== E2E RESULT: 532/532 passed ===`），分母双向校验：`^  PASS ` 行数也 = **532**。469 → 532 的 **+63 / 消失 0** 是标题唯一集量出来的（与 `~/.cache/zlc52_health_guard/api_h2_fresh.log` 逐条求差，469 条distinct对 532 条 distinct）；新增那 63 条全在流程绑定那一节（`/fires` 回读、`/vocabulary` 两栏、z-wf 桩绑 8888、引擎不可达 / 回 5xx / 挂死 7s …），**本批 #66 在接口层一条没加**。打的是本窗 07:24:32 那份件（`md5 -q` = `8f268420f01b1e8933f5f2a2d2ebdaf8`；`lsof` 现读 18090 监听者 = pid 64499、`ps -o lstart` = `Sun Sep 27 07:26:08 2026` ⇒ 起的是这一窗新装的件，不是早先那支）|
| 接口 E2E（**真 MySQL 8**，隧道 18099） | `bash _e2e/deploy_250.sh api` | **这一格这一窗没重测 —— 不是绿，是没量。** 上一窗（09-26 19:0x）那个 469/469 是老数。07:3x 现读：`ssh 250` ⇒ `kex_exchange_identification: read: Connection reset by peer` / `Connection reset by 192.168.31.250 port 22`、**rc=255**；本机 18099 上还挂着一条 ssh 隧道（pid 70952）而 `curl --max-time 8 http://localhost:18099/api/lc/health` ⇒ `HTTP_CODE=000`、rc=56 ⇒ **本地端口在听 ≠ 对面活着**，要复测得先重起隧道。⚠ 这一窗只敲了这一次 ssh，没反复重试 |
| 前端 | `cd z-lc-admin-ui && npm run typecheck && npm run lint && npm run test && npm run build` | tsc 0 / `eslint --max-warnings 0` 无输出 / **vitest `Tests 252 passed (252)`、`Test Files 28 passed (28)`**（`check_0727.log:9609/:9610`）/ build `✓ built in 2.74s`、产物 `dist/assets/index-ruBdvAdn.js`。⚠ **252 这个总数与上一窗逐字同，但集合换了**，"同数"不等于"没动"：逐文件对账（`✓ <file> (N tests)` 行现取，机械加总 = 252）对 `zlc52_health_guard/ui_gates3.log` 求差 = `WorkflowsPage.test.tsx` **+3**、`workflowVocabulary.test.ts` **+4**、`src/fields/registry.contract.test.ts` **−7** —— 那 7 条所在的 `src/fields/` 整棵子树（13 个文件）是 09-24 `e3a80c8`「字段类型注册表改用 @yuku123/render 正式版」删的，经 01:5x 那次 rebase 才进到 `HEAD`，所以上一窗跑的那棵树里它还在。**+7 与 −7 恰好抵消**，纯巧合。本批 #66 前端一条断言没加 |
| 真浏览器 | `E2E_BASE=http://localhost:5274 E2E_API=http://localhost:18090 node e2e/browser-e2e.mjs` | **`=> PASS 279 / FAIL 0`**、`全绿轮次: 1/1`（`browser_66.log`）。⚠ **这一窗跑的是 1 轮，不是上一窗那 3 轮**（`REPEATS=1`）。222 → 279 的 **+57 / 消失 0** 同样是 PASS 标题唯一集量出来的；新增那 57 条全是 #61 §2.5 那一族（发起账抽屉、词表长出来的下拉、FAILED 不许留实例号…），`git log` 现读 `e2e/browser-e2e.mjs` 最后一次改动 = `1ff9e83`（07:18 那批），与本批 #66 无关。产物指纹 `index-ruBdvAdn.js`（与本窗 `npm run check` 那个同名）—— 按 #69 那条**名字不是身份**，这一格只能说门禁自己报的是"比 src 里最新的文件新 767s" |
| 部署（250 真 MySQL 8） | `bash _e2e/deploy_250.sh sync` → `start` → `verify` → `gates` | **这一格这一窗同样没重测**（250 不可达，rc 见上面那行）。上一窗那批 `GATES_RC=0` / 14 条 ✓ / jar 字节对账 `bb4865…` / healthproof 三档全是**老数**。⚠ 另有一格从来没有在任何窗跑成过：闸 6 `fireprobe` 的**正向 23 条读数**（250 一挂就再没打过），这一窗仍然只能是"没量" |
| 注入自证（#66 族，java 层） | 手工七跑矩阵（跑法与逐支读数见上面 `### ⚠ 缺陷 #66` 那一节） | b / a / 还原三跑 **BUILD SUCCESS、core 29/0**，web a **14/0**；I1 **core Failures: 2** + **web Failures: 1**、I3 **core Failures: 3**、I2 **core Failures: 1**、I5 **core 2 + web 1** —— 每支的具名红逐字抄在上一节那张表里。⚠ 我对 I1 的**预测**（"两步一起摘 ⇒ 那两支 + HTTP 同时红"）被实测否掉一半（红的第二支换了一支），根因是量具有病而不是断言软，那条发现就是 I2 那一跑。**这一族没有永久量具**：`_e2e/` 里覆盖流程绑定的注入器只有 `mutate_workflow_trigger_guard.py`（M1–M6，打的是 #61"存了不执行"那一族），按**覆盖缺口**记账，见上一节末 |
| ⚠ 时序（这一窗真发生过，别当成脏数据） | I5 是在上面 java/接口/前端/浏览器 那四个数**量完之后**才补跑的 | 四个数（4774 / 532 / 252 / 279）量的都是 `md5 = e2936acb…` 那棵树；I5 注入完 `cp` 还原后现读 md5 仍是 `e2936acb…`、`grep -c "MUT(I5)"` = 0、复跑 `core_66_restored_i5.log` **29/0 BUILD SUCCESS** ⇒ 那四个数的有效性靠这一次对账兜住。下一窗要引用它们，先 `md5 -q` 比这个值 |

**09-26 18:3x – 19:1x 这一窗（#52 收线：`/api/lc/health` 的 UP 从此每池真探一次、配置不成串就拒起；
顺带在部署量具自己身上撞出 #59/#60 两支）六层全部同轮重测，日志逐层落在 `~/.cache/zlc52_health_guard/`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**、**4706** 个用例 0 红 0 错 0 跳（`full_install3.log`，surefire 模块汇总行现加 = 2701+525+1276+112+**92**）。4677 → 4706 的 **+29 拆到文件级**：`git show HEAD:<文件>` 与盘上各数一次 `@Test` —— 新支 `DataSourceConfigGuardTest` **0 → 17**、`DataSourceHealthProberTest` **0 → 7**、`LcStartupRefusalTest` **0 → 4**、`LcHttpContractTest` **61 → 62**，加总正好 29，与模块级那条（z-lc-web 63 → 92）逐一对上 |
| 接口 E2E（本机 H2） | `python3 _e2e/e2e_api_test.py http://localhost:18090` | **469/469**（`api_h2_fresh.log` 末行逐字 `=== E2E RESULT: 469/469 passed ===`），分母双向校验：`^  PASS ` 行数也 = **469**。464 → 469 的 **+5 是标题唯一集量出来的**（与 16:4x 那轮 `~/.cache/zlc57/gates/api_h2_57b.log` 逐条求差：新增 **5**、消失 **0**；5 条全在 health 那一节 —— 每池一条探活、每池 `UP`+`database`+`latencyMs`+`jdbc:` url、`dataSourceLc` 在列、`sources` ≥ 2、payload 不含 password）。打的是本窗 `clean install` 的那份件（`lsof` 现读 18090 的监听者 = pid 22098，那支 16:42 起的旧件已换成它）|
| 接口 E2E（**真 MySQL 8**，隧道 18099） | `bash _e2e/deploy_250.sh api` | **469/469**（`api_mysql3.log`，`API_RC=0`）—— 同一份门禁脚本、同一个 469 分母，一次打 H2 一次打 MySQL 8；health 那五条在真库上读回的 `database` 是 `MySQL 8.0.26` |
| 前端 | `cd z-lc-admin-ui && npm run typecheck && npm run lint && npm run test && npm run build` | tsc 0 / `eslint --max-warnings 0` 无输出 / **vitest 252/252（27 个文件）** / build 绿、产物 `index-C4lf5SiW.js`（`ui_gates3.log`）。四步是 `&&` 串的，末步 build 出了产物即前三步全过；252/27 与 16:4x 那轮 `check57b.log` 的 `Test Files 27 passed (27)` / `Tests 252 passed (252)` **逐字同**，#52 这一窗前端一条断言都没加 |
| 真浏览器 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **三轮各自 `PASS 222 / FAIL 0`**、`全绿轮次: 3/3`、`BROWSER_EXIT=0`，产物指纹 `index-C4lf5SiW.js`（门禁自己那行写的是"比 src 里最新的文件新 6432s"，即测的确实是本轮 build 的产物）。⚠ 222 也是**量出来没动的**：与 16:5x 那轮 `browser57b.log` 各自的 PASS 标题唯一集求差 = 新增 **0** / 消失 **0** |
| 部署（250 真 MySQL 8） | `bash _e2e/deploy_250.sh sync` → `start` → `verify` → `gates` | sync 回读 `jar 字节一致：bb48651152a723dc2651ecd3aeb28ba4`，另有第三次独立对账（`ssh 250 md5sum` 与本机 `md5 -q` 同值）；`start` 把旧进程收干净（`等了 29s, 端口 18090 空`）才起新件，且"端口上的监听者正是这次起的 pid=8045"；`verify` 读回的 health 是**两池各一条**：`dataSource` 与 `dataSourceLc` 都 `UP`、`database":"MySQL 8.0.26`、`url` 已抹成 `jdbc:mysql://127.0.0.1:33061/z_lc`、`latencyMs` 在。**`gates` 整批 `GATES_RC=0`、14 条 ✓**（`gates4.log`）：闸 1（`%q` / 裸写）、闸 2（`z_lc_misdeploy` 负控）、闸 3（探针表三段式）、闸 4（校对漂移）、healthproof 三档 |
| 部署层自证（本窗新增，`gates` 的第五道） | `bash _e2e/deploy_250.sh healthproof` | 三档都在 250 上**真起过进程**（自带 pid/log/30 MB 体积保险丝，绝不 pkill）：**D1** 把 `SPRING_DATASOURCE_*` 从环境里真的 `env -u` 掉 ⇒ 进程**自己退出**、日志具名「z-lc 拒绝启动」并点名 `SPRING_DATASOURCE_URL`、端口不监听（修复前这一档实测是 6.656s 起来 + 200 UP + 第一条业务查询 500）；**D2** dev H2 ⇒ 聚合 UP、`sources` 里 `dataSourceLc` 在列、`"database":"H2`；**D3** url 形好而对面没人听 ⇒ **照样起得来**、聚合 `DOWN`、病句点名 `dataSourceLc`、主池仍如实 `UP`。D1/D3 一左一右就是"拒起"与"如实报"的分界线本身，只修一头都会让另一头的场景变错。修复后的写法连跑 **3/3 轮 `rc=0` 且 `✓ D` 计数=3**（`hp_fix_{1,2,3}.log`）|
| 注入自证（#52 族，java 层） | `_e2e/mutate_health_honesty_guard.py`（**19 支**：H1–H17 + N1–N2） | `RESULT: 缺陷#52 的 19 支注入逐支按预期点名，产物已还原到基线字节`（`~/.cache/zlc52_health_guard/20260926-184508/guard.log`）；基线 `行数=646 具名红=[]`，四支被测文件还原行逐条 `字节相同`，还原后复测 `构建并跑齐 / 具名红=[]`。⚠ **18:39 的第一轮（`mut20.out`）报了 3 处"预期之外"，改的是这本账、不是断言**：H6 摘掉 `mask()` ⇒ `mainPoolRefusesDriverUrlMismatch` 也红（驱动↔url 那条报错里同样带 `mask(url)`，那是第三个漏点，测试写着"异常也是对外面"，红得对是我漏记）；H7/H8 放宽主池判据 ⇒ `guardRunsBeforeAnyDataSourceIsCreated` 也红（那支顺序断言是**双向**的，既要求"池没在闸之前出生"也要求 refresh 真抛）。**逃过 0 支**（两轮日志里 `!! 逃过` 各 0 次）|
| 注入自证（本窗新加的三支结构性判据各自有牙） | 同上 | `guardRunsBeforeAnyDataSourceIsCreated`（校验挂在 `BeanFactoryPostProcessor` 而不是 `@PostConstruct`：BFPP 在**任何**单例实例化之前跑完 ⇒ "闸在池之前"是结构保证而不是运气；摘掉调用 = H13 红）、`refusesToRunBlindWithoutEnvironment`（`Environment` 走 `EnvironmentAware` 那条接线，因为 BFPP 的实例化发生在 `@Autowired` 基础设施就绪之前，实测 `No default constructor found`；env 为 null 时**拒起**而不是当成"没什么要查的" = H14 红）、`unknownDriverOrSchemeIsLeftAlone`（"认得才判"的让步：误拦会把一个本来能跑的部署挡在门外，所以 shaded 驱动/未知子协议一律不瞎猜 = H17 红，`acceptsRealJdbcUrls` 同红）|

**09-27 00:5x–01:0x 追加：#61（流程绑定「存了但运行期一个字都不执行」）的 java 层注入自证收线**

| 层 | 命令 | 实测（同一窗现读，`~/.cache/zlc61/mut/logs/guard-0927-010646.log` + 台账 `ledger.json`） |
|---|---|---|
| 注入自证（#61 族，java 层） | `python3 _e2e/mutate_workflow_trigger_guard.py`（**6 支** M1–M6，每支 core + web 两层都跑） | 末行逐字 `RESULT: java-layer falsification done \| 本轮 30 条具名红 / 分母 {'core': 68, 'web': 13}` + `restored sources: clean`。基线两层 0 红；逐支 core/web = **M1 0/10、M2 5/2、M3 3/1、M4 2/1、M5 2/1、M6 2/1**；还原后复跑 68+13 全绿；每支的 `.class` 指纹相对基线**都变了**（编译器确实量了这份变异，不是拿旧字节码复述上一轮）。未预期红 0 条、逃过 0 支 |
| ⇒ 这一支反过来把被测面补宽了一格 | `WorkflowTriggerContractTest.http5xxWithASuccessfulLookingBodyIsNotAFire`（新增，12 → **13** 条） | M5 第一跑 **web 0 红**。查下来不是"运行时不受影响"而是**契约层没有"引擎回非 2xx"这一格形状**（桩只有"200 + `success=false`"，而 `HttpExecutionResult.isSuccess()` 对任何完成的响应恒真，字节码 `iconst_1 → putfield success`）⇒ "判成功看状态码还是看信封"这个决定在真进程边界上无处被检验。补了 502+成功 body（必须是一行 `FAILED`、`detail` 带 `http=502`、body 里那个实例号一个字都不许留下）再加 200 同 body 的阳性对照，M5 才有了第四层的牙 |
| ⇒ 顺带抓出一条**软断言**（记账，未改） | `WfAdapterTest.httpFailureCarriesStatusAndPath` | M5 下"要说清是 http 几"**假绿** —— 失败消息把整个 body 抄进文案，而那个 body 里正好有 `"404"`；真红的是同一条方法里的"要带上打的是哪条路径"。**断言的文案里混入被回显的内容 = 把它写软了**，这种绿只有注入看得见 |
| ⇒ 一处**预期红集漏记**（改账不改软） | `WorkflowBindingServiceTest.updateShouldNotLetAValidRowBeTurnedIntoAnUnhonorableOne` | M2 第一跑多红这一条。读下来是真连带：create 与 update 共用 `validateForWrite` ⇒ 名单一混，"先建一条合法的、再把它改成合法的以外"那条绕过创建闸的路一起漏。按名字认领进 M2 的账，**没有**加白名单、也没有把断言改软 |

**09-27 01:0x–01:4x 追加：#61 的第六层 —— 打进「发出去的那个 jar」，不是测试进程**

| 层 | 命令 | 实测（同一窗现读，`~/.cache/zlc61/deployed-guard-0927-013839.log` + 台账 `deployed_ledger.json`） |
|---|---|---|
| 部署件接口层（新增一节 `[15w]`） | `python3 _e2e/e2e_api_test.py`（对着 18090 上真跑的 fat jar） | 逐字 `=== baseline deployed: 532/532 全量、本节 63 条，artifact fp=5a0564136514，一轮 4.5s`。分母 63 = 532−469，469 正是加这一节之前的全量。**这一节自己起 z-wf 桩**（`http.server` 指 8888，模式 ok/reject/http5xx/hang），断言的是报文形状（路径 / 剪过空白的 `processKey` / `businessKey` 能回指这条记录 / `title` / `initiator` 是这次请求的人 / variables 带 `lc*` 坐标）+ `/fires` 回读 STARTED 与实例号 + 登记本身不发单 + 未登记实体一句不发 + 6 次写入口被拒且**一行都没落库** + 引擎说不了/5xx 装成功/不可达 三种结局各落一行 FAILED 带原话 + 挂死 7s 撞默认 3000ms 预算（实测 3003ms）+ 一次超时不传染下一次 + 批量导入不发单。⚠ **开局量的第一件事是"18090 上跑的是哪一个件"**：nested `z-lc-core` 时间戳 09-26 18:51、`WfAdapter.class` 4969B、**根本没有 `WorkflowTriggerDispatcher`** —— 那是 #61 之前的 jar。从 HEAD 重打（`78aeba1b…`、`WfAdapter.class` 7794B）并**先按字节比对 fat jar 里那个 class 与 `target/classes/` 的**，之后才信任何读数 |
| 注入自证（#61 族，部署件层） | `python3 _e2e/mutate_workflow_deployed_guard.py`（**6 支** W1–W6，每支重新 build fat jar、用它重启 18090、再跑整份接口层） | 末行逐字 `RESULT: deployed-layer falsification done`，台账 `bad=0 / restored=true / rerun_green=true / ran_by zifang@0927-013839`。基线 532/532；逐支 = **W1 501（红 31）、W2 531（1）、W3 530（2）、W4 531（1）、W5 528（4）、W6 529（3）**，六支的"本节之外的红"全是 `(none)`；每轮证明 fat jar 里**恰好一个 `.class` 变了**（`changed=1` + 点名是哪个，六支分别是 `RuntimeCrudController` / `WfAdapter`×3 / `WorkflowTriggerDispatcher` / `WorkflowTriggers`）；还原轮 `artifact 回到基线字节: True` + `e2e = 532/532 本节 63 failures=(none)` + `restored sources: clean` |
| ⇒ 一支**编译器替洞上闸**的读法（记账，非缺陷） | W5 原样打"无上限的 `future.get()`" | **javac 直接拒**：`exception java.util.concurrent.TimeoutException is never thrown in body of corresponding try statement`（01:32 那一轮整场崩在这里）⇒ "摘掉超时"这种变异在 java 层根本编不出来，**编译不过不等于没覆盖**，换一个编得过的形状。改成"预算放大 60 倍（180s）"后："到点判 FAILED 并点名预算"红，而"写入口在默认预算内返回"**没红** —— 因为它另有独立的一根桩（`WfAdapter` 传输层 socket = `timeoutMs + 500ms`，W5 碰不到）。"有界等待"这一族本来就有**两道界**，各名下各的检查，不是量具漏判 |
| ⇒ 两层闸各管一件事（同一支注入读出来的） | W6 摘 `WorkflowTriggers` 的 `autoSubmit != 1` | 红的是"autoSubmit=0 的绑定被拒"两条 + "上面这 6 次被拒的提交一行都没落库"；而"写 case 仍然正好一句"**没红** —— 落库那行 `auto_submit=0` 被 `listByEvent` 的 `auto_submit = 1` 挡在发起之外。⇒ 写入口那道管"别让装饰进库"、读侧那道管"别让它发单"，两层各有名，摘一层另一层还在 |

⚠ **这一窗量具层面得到的一条通则：检查的名字必须是稳定身份。** 头两轮（`deployed-guard-0927-012438` / `-012821`）有两条名字里插了本轮才有的值（一个时间后缀、一个实测毫秒数），
"预期红集"就没法逐字比对 —— 每跑一轮名字就变，账永远对不上，而这看起来像"注入逃过了"。修法是把值挪进 `detail`、名字常量化，
**不是**把比对放宽成前缀匹配。同窗另两条量具层面的账：① 连带红（打到本节之外的红）早先挂在同一串 `elif` 的最后一支上，
于是"预期非空"的那些轮（= 抄完实测之后的全部轮次）**根本走不到那一句**，注入打到别处也不会红 ⇒ 改成无条件判定；
② 每一支注入的"预期红集"是从 Transcript 里 `json.dumps` 机械抄进来的，不是手敲的（手敲必臆造，这一族已经错过三次）。

⚠ **接口层这一节不托管的东西，写清楚免得读成"已证"**：跨租户绑定那支造不出来 —— `WorkflowBindingController`
把租户归一成 `default`，从这个口根本写不进 foreign 行（已写进代码注释而不是假造一条）；`latencyMs` 类的话题与本节无关；
以及本节测的是 **18090 这台上的 jar**，250 那条腿另有 §2.6 的未闭缺口（`z_lc_workflow_fire` 在 250 真库里还不存在）。

⚠ **这一窗最值钱的一条：闸自己也会撒谎，而且是在"结论对"的时候撒**。#60 —— `healthproof` 三档全 ✓，
`gates` 整批退出码却是 **1**（`gates3.log`）。机理：`hp_cleanup` 写的是 `[ -n "$HP_ALIVE" ] && kill …`，
D2/D3 分支已经自己 kill 过并把端口等空了，走到 trap 时那个 pid 早已退出 ⇒ `kill` 回 1 ⇒ 脚本开着
`set -e`，整个 trap 中止在 `return 0` **之前**；而 bash 在脚本自然结束时**拿 EXIT trap 的最后一条状态
当退出码**（本机确定性复现：`~/.cache/zlc52_health_guard/hp_trap_repro.sh`，旧写法 `rc=1` / 新写法 `rc=0`，
两句都照样打印"三档全部 ✓"）。同一条链单独跑 `healthproof` 又常是 0（取决于那个 pid 有没有被回收成僵尸），
把旧写法注回真实链路跑一轮也读到 rc=0 —— 所以这一支的账是"两轮里红过一轮"，不是"每次都红"。
**"结论对、退出码随机"的闸比直接红更坏**：它在 CI 里既误报又稀释信任。同窗另一支是同一族：#59 ——
`healthproof` 的端口我照抄成"我记得没人用的 18095"，实测它被 z-mcp-server（pid=6636）占着、18096/18097
也是别人的，那道"不跟别人抢端口"的检查一上来就把这一族拦停（检查是对的，写死端口是我编的假设）⇒
改成 `hp_pick_port` 在 18095–18104 里现挑并打印改用了哪一个（实测改用 18098）。

⚠ **这一窗的边界，别读成"已证"**：闸只管**配置形状**，不管连通性 —— 库暂时起不来时进程要能起来并由
`DataSourceHealthProber` 如实报 DOWN（D3 就是这条边界的负控）；驱动↔url 那条判据对**没见过的驱动**
一律不判（H17 钉住的就是这条让步本身），所以拿一个 shaded 驱动配错 url 仍会走到 Druid 去撞；
`/api/lc/health` 里的 `latencyMs` 是单条 `SELECT 1` 的耗时，不是业务查询的尾延迟。


**09-26 14:2x – 16:5x 这一窗（部署演练：把 z-lc 打到 250 的真 MySQL 8 上，撞出 #51/#54/#55/#56/#57/#58；
本窗动了 java 与部署量具，所以六层全部同轮重测）日志与退出码逐层落在 `~/.cache/zlc57/gates/`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**、**4677** 个用例 0 红 0 错 0 跳（surefire 模块汇总行现加 = 2701+525+**1276**+112+**63**；`JAVA_EXIT=0`）。08:3x 那一记的 4656 → 4677 的 **+21 现量到出处**：`git show HEAD:<文件>` 与盘上各数一次 `@Test` —— `SchemaAdminBizServiceTest` **30 → 43**（+13，#51/#54 那一族）、新文件 `UndoServiceSnapshotFormatTest` **0 → 7**、`LcHttpContractTest` **60 → 61**；两条尺对得上：模块级 z-lc-core +20 = 13+7、z-lc-web +1，加总正好 21。⚠ 这一格是本窗**第二次**跑：第一次（`~/.cache/zlc49/gates/java57.log`，16:28）是 #57 修完、`FakeJdbc` 还没改严之前的数，分母同样是 4677 —— 因为 #57 这一支**没加用例，改的是替身答话的规则**（写清楚，免得下一窗以为 43 条里有一条是新的）|
| 接口 E2E（本机 H2） | `python3 _e2e/e2e_api_test.py` | **464/464**，`api_h2_57b.log` 里逐字有 `=== E2E RESULT: 464/464 passed ===`，`API_EXIT=0` 落在同一份日志末行；分母双向校验：`^  PASS ` 行数也 = **464**。打的是本窗 `clean install` 出来的那份件（pid 90731，`lsof` 现读端口 18090 的监听者就是它）|
| 前端 | `cd z-lc-admin-ui && npm run check` | tsc 0 / `eslint --max-warnings 0` 无输出 / **vitest 252/252（27 个文件）** / build 绿，产物 `index-C4lf5SiW.js`，`CHECK_EXIT=0`。08:3x 那记的 248 → **252** 是本窗之前那两窗（#49/#50 的权限页前端）累积的未提交改动，本窗只重测不改数 |
| 真浏览器 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **三轮各自 `PASS 222 / FAIL 0`**、`全绿轮次: 3/3`、`BROWSER_EXIT=0`，产物指纹 `index-C4lf5SiW.js`（本窗 `npm run check` 里 build 的那一份）。184 → 222 的 **+38 是名字集合量出来的**（把本轮与 08:3x 那轮 `browser48.log` 各自的 `PASS` 行标题取唯一集再求差：新增 **38** 条、消失 **0** 条，38 条逐条都落在权限矩阵（11e）那一节 —— 从"这个应用里确实有 task 那一栏可授"到"这一节自己种的授权全部回收干净"），不是拿 222−184 减出来的。⚠ **本窗第一次跑这一格是拒跑的**（`~/.cache/zlc49/gates/browser55.log` 逐字：`门禁拒绝开跑（测的必须是本轮构建的产物）：- 没从 http://localhost:5274/ 的 HTML 里读到 assets/index-*.js（preview 没起？）`）—— 那一轮的 222 一个都不存在，是**量具把"没人起 preview"报成 2 而不是 0** 才让这一格后来真的有结论；这一条留在账里，因为"跑过了"与"跑起来了"是两个读数 |
| 部署（250 真 MySQL 8） | `bash _e2e/deploy_250.sh all` → `gates` | `ALL_EXIT=0`、`GATES_EXIT=0`。sync 回读 `jar 字节一致：c78f57facee27ab6423d60ac40406b90`（与本机 `md5 -q` 同一个值）、`schema 字节一致：ffaa1084…`；`库 z_lc 里的表数：49`（39 → 49 的账见「部署演练怎么跑」那一节末尾：是 `api` 那几轮各自留下的 `e2e_*_<后缀>` 实体表，`z_lc_*` 元数据 14 张一张没多）；旧进程收干净后才起新件（`旧进程已收干净（等了 0s, 端口 18090 空）`→ `pid=22579` 与端口监听者对上）。**四道闸同一次运行里逐条绿，且每一条都先具名红过一次**：闸 1（`%q` / 裸写）、闸 2（`z_lc_misdeploy` 负控，`z_lc` 里 `rows=0` 时报「进程连的根本不是这个库」）、闸 3（探针表三段式）、闸 4（`探针实体落库：id=36 表=e2e_g4_362476` → 漂到 `utf8mb4_0900_ai_ci` 判 `FAILED` 给 `CONVERT TO` → 搬回 `EXISTS_INTACT`）|
| 接口 E2E（**真 MySQL 8**，隧道） | `bash _e2e/deploy_250.sh api` | **464/464**，`api_mysql57b.log` 里逐字有 `=== E2E RESULT: 464/464 passed ===`，`API250_EXIT=0`，`^  PASS ` 行数 = 464。这一格是本窗新加的第六层：同一份门禁脚本、同一个 464 分母，一次打 H2 一次打 MySQL 8 —— **#51/#54/#57 三支缺陷全都是"只在 MySQL 才红"的，所以这一层不是 H2 那一层的复读** |
| 注入自证 | `_e2e/mutate_collation_guard.py`（**18 支**：M1–M13 + N1–N5） | 本窗把 M13（表级那一问退回 `collation_name`，= #57 修复前的原样）加进族里；**先手工单支验过**：注入后 `mvn test -pl z-lc-core -Dtest=SchemaAdminBizServiceTest` 报 `Tests run: 43, Failures: 1`，红的正是 `provisionShouldFailWhenTableCollationDiffersFromMetadataLayer` 那一条具名断言，还原后 md5 与开跑前逐字节相同（`RESTORE_OK md5=2fa93939ffc47fb03949afec1330a4cc`）。整族 18 支已在同一窗跑完（`~/.cache/zlc57/gates/coll18_run1.log`，`COLL_GUARD_EXIT=0`）：`RESULT: 缺陷#51+#54+#57 的 18 支注入逐支按预期点名，产物已还原到基线字节`；18/18 的「预期红 == 实测红」逐支等集（日志里 `!! 逃过` / `!! 预期之外` 各 0 次），两支文件还原行均打 `字节相同`，还原后复测 `构建 过 / 具名红=[]` |
| 注入自证（浏览器层） | `cd z-lc-admin-ui && python3 e2e/mutate_permission_browser_guard.py`（**M1–M9** 整族，#49/#50 那一页） | `PERM_GUARD_EXIT=0`（`~/.cache/zlc57/gates/perm_full57.log`）。开局机器核账：`账平：11e 扫到 39 条 = 注入认领 21 条 + 未覆盖记账 18 条`；基线 `PASS 222 / FAIL 0`，九支**逐支 `OK … 预期 N 条全红，无一条连带红`**（4/1/1/3/5/9/1/4/1 条，红集各不相同；M4 那一支因为新角色进不了矩阵，整节后续 21 条压根没跑到，分母当场只剩 201 —— 这条写在日志的 `共 201 条` 里，不是我把它算绿的）；还原轮 `src 指纹` 与基线逐文件 md5 一致（脚本不用产物名当判据：本机 rollup 输出不逐字节可复现，同一棵树三次构建跑出两套 `index-*.js` 名字，见 `src_digest` 的 docstring）且 `99_restored: PASS 222 / FAIL 0` |

⚠ **这一窗最值钱的一条是层界，不是绿**：`#57` 那一支在 java/vitest/接口三层**结构上打不到**（H2 没有 `information_schema`，
那一整块 `if` 不走），修法也分两头 —— 单测层的替身改成按真库目录形状答话只是让"问错列名"不再隐形，
"MySQL 8 的 `tables` 视图里那一列到底叫什么"只有 250 上的闸 4 说得清。**替身能模仿真库，不能代替真库**；
反过来也一样：这一窗四道闸全绿 ≠ 那 464 条断言证过校对这件事（它们里只在真库上跑的那一层才第一次撞到 #51）。

⚠ **提交状态（17:3x 现数，`git status --porcelain` / `git ls-remote` / `git show --stat` 当场量的，不是回忆）**：
本窗这一批 **16 个路径**已提交并推送 —— HEAD = **`e68104be0ad1ed2a8780b5cb9cf25e40ad36a992`**，
`git ls-remote origin refs/heads/main` 读回**同一个 sha**（不是"提交了但还在本地"），
`git show --stat HEAD` = **16 files / +3290 −73**。提交后 `git status --porcelain` = **2 行**，
逐字是 `?? z-lc-admin-ui/pnpm-lock.yaml` 与 `?? z-lc-admin-ui/pnpm-workspace.yaml` —— 那两份**不是我的**
（别的会话在飞的改动），所以这个仓**永远禁 `git add -A`，只按路径点名 commit**（本批 16 个路径逐个 `git add --`，
提交前后各数一次 `git diff --cached --name-only | wc -l` = 16）。
本格自身是提交**之后**改的，由紧随其后那一笔只动 `_e2e/README.md` 的 docs 提交带走。

**09-26 08:3x – 08:4x 这一窗（#48 权限这一族收线：三层注入自证同轮跑齐）五道闸串行同轮实跑，
五个退出码一起落在 `~/.cache/zlc48/gates/chain48.status`，日志各自在 `~/.cache/zlc48/gates/{java_full48,boot48,run_api48,npm_check48,browser48}.log`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**、**4656** 个用例 0 红 0 错 0 跳（surefire 模块汇总行现加 = 2701+525+**1256**+112+**62**；`JAVA_EXIT=0`。06:4x 那一记的 4646 → 4656 的 +10 **拆到模块级再拆到文件级**：z-lc-core 1251 → 1256、z-lc-web 57 → 62（两轮的四条汇总行逐字对过，只有这两条不同），再用 `git show HEAD:<文件> \| grep -c '@Test'` 与盘上现数各量一次对上 —— `PermissionServiceTest` **10 → 15**、`LcHttpContractTest` **55 → 60**。⚠ 这一窗**第一次**跑这条链就是绿的，五个退出码没有第二个值 |
| 部署件 | 用刚构建的件重启 18090 | `BOOT_EXIT=0`（`boot48.log`；健康检查读到 `"status":"UP"` 之后才放行接口层） |
| 接口 E2E | `python3 _e2e/e2e_api_test.py` | **464/464**：`run_api48.log` 第 **532** 行逐字是 `=== E2E RESULT: 464/464 passed ===`，`API_EXIT=0` 在同一轮的 marker 文件里。434 → 464 的 **+30 一段不差全在新增的 `[15t]`（权限这一族）**：拿两轮日志各自的 `[NNx]` 横幅 + 段内 PASS 行现算并逐段对比，结果是 `changed: {'15t': (None, 30)}` —— **其余 31 段一条都没动**。分母双向校验：32 段求和 = 464 = 标题那个数，且 `^  PASS ` 行数也 = 464。⚠ 上一窗那句"日志里共 **29** 段"用的是 `grep '\[[0-9]+[a-z]?\]'`，那个正则**不认** `[15d1]`/`[15d2]` 这种带尾数的段名 —— 同一条 grep 本窗得 30，仍然只有 `[15t]` 是新的；段数分母按解析器数（32）还是按那条 grep 数（30）得写明是哪个 |
| 前端 | `cd z-lc-admin-ui && npm run check` | tsc 0 / `eslint --max-warnings 0` 无输出 / **vitest 248/248（27 个文件）** / build 绿，产物 `index-CQtLNKSU.js`，`CHECK_EXIT=0`。238 → 248 的 +10 **整份住在两支新文件里**（`npm_check48.log` 逐文件行：`PermissionsPage.test.tsx (8 tests)` + `permissionVocabulary.test.ts (2 tests)`，`git status` 现读两支都是 `??`），25 个老文件**逐文件计数与上一窗一条不差**（`changed: {}`、`gone: []`） |
| 真浏览器 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **三轮各自 `PASS 184 / FAIL 0`**、`全绿轮次: 3/3`、`BROWSER_EXIT=0`。分母与上一窗**相同**：#48 没有新增任何浏览器层断言（`grep -c '权限\|permission' e2e/browser-e2e.mjs` = **0** —— 权限矩阵页在真浏览器层连一句"渲染正常"都没有，见下面那格边界）。唯一的非 2xx 仍是那条被登记放行的 400（`/api/lc/pipeline-config/create`）。⚠ 起 preview 时 5274 被一支 **06:27:52** 起的旧 vite preview 占着，这条链自己的那个退到了 5275，而门禁打的是 5274 —— "测的是本窗的件"这一句是**量**出来的（5274 / 5275 / 盘上 `dist/index.html` 三处引用的产物都是 `index-CQtLNKSU.js`，`dist/assets/` mtime `08:33:41` = 本窗 build 那一次），细节与"这条链收尾会按名字杀掉 5274 上别人的 preview"都在文首那一格 |
| 注入自证 | 33 支（`ls` 现数：`_e2e/` 16 + `z-lc-admin-ui/e2e/` 17），带锁 28 支（`grep -l _mutlock` 现数：前端 17 全接 + 后端 11） | 本窗新跑并收线的是**同一族三层**：**J1–J14** `mutate_permission_service_guard.py`（java 层：分母每轮钉 **75** 条、认领 **25** 条具名断言、99_restored 复跑 75 条全绿）→ **B1–B12** `mutate_permission_matrix_guard.py`（vitest 层：分母钉 **10** 条，十二支逐支打印"无一条连带红"，恢复后 `10 tests, 0 failed`）→ **D1–D14** `mutate_permission_deployed_guard.py`（发出去的 fat jar：`RESULT: permission deployed-layer falsification done \| 14 支注入 / 认领 24 条 [15t] 探针`，十四支的"其它红"逐支为 `[]`，基线与还原后各一次 **464/464**，每支注入后产物指纹都必变、还原后必回基线指纹） |

⚠ **本窗最值钱的三处读数，都是"预期红集"与实测对不上之后量出来的**（每一处改的都是账，没有一处把断言改软）：
1. **D3（`grant` 不走词表闸）**：我预期"大小写漂移会让下游判定跟着比不中"，实测那三支按值过滤的探针**反倒看不见破坏** ——
   恰恰因为库分大小写，`'view'` 与 `'VIEW'` 各成一行，`permission == 'VIEW'` 的过滤器读到的仍是那一条它期望的。
   只有"整份清单恰好等于什么"那一支看得见。**后果**：判"归一有没有生效"不能用同一条归一后的值去筛。
2. **D7（归一把 NULL 落成空串）**：写侧与读侧**共用同一个归一函数**，于是两侧一起漂，判定那三支因此测不到它 ——
   等价变异在部署层的另一个方向。这一支真正的猎物是"整份清单"与"查重多出一行"那两支。
3. **D12（回收丢掉 id 条件）**：删过头时"回收成功""清单里真的没有它"两支反倒读到它们期望的结果
   （它们钉的是"删不掉时不许报成功"，不钉"不许多删"）；"多删"这一侧由 `/list` 那两支红出来。
   **预期红集要按"谁读了这个值"估，不是按主题估** —— 这一条在 #47 的 K4 也踩过一次。
另有两支"留空问应用级"的探针一整轮没红过：不是它们空跑，是十四支里原本没有一支把"整个应用"写成另一种范围形状，
所以**补了 D14**（把那一档查成空串范围）才让它们有猎物 —— 补完之后 `TRACKED - ever_red` 为空，`WHY_NEVER_RED` 保持 `{}`。

⚠ **这一窗量到的一条库事实（写进代码注释也写在这里）**：dev 的 H2（`jdbc:h2:mem:zlc;MODE=MySQL;
DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`）里 **`=` 对 VARCHAR 分大小写**。
`CASE_INSENSITIVE_IDENTIFIERS` 只管**标识符**，不管**值** —— 照着名字里那个 "CASE_INSENSITIVE" 推断"值也不分"是本轮
一处写错的 javadoc 的来源（已改成实测口径）。取证方式：对 D3 注入态的那支 jar 直接 curl `/grant` 两次
（`view` / `VIEW`），`/list` 读回来是**并排两行**、各自的查重都报"成功"。

⚠ **三层各自证不到的地方，逐条写明（不写 = 下一窗当"全绿"读）**：
- **跨租户读隔离在接口层不可证，但在契约层证过**：四个端点都把租户钉成 `DEFAULT_TENANT`，所以 HTTP 面上**没有能插入
  foreign 行的入口** —— D6（控制器不再钉租户）只能红掉那两支**写侧**探针。真正的反证在 java 契约层：
  `LcHttpContractTest.foreignTenantRowsNeitherGrantNorGetRevoked` 用直接 SQL 播一行另一租户的合法授权，再断言
  `/list` 混不进来、`/check` 答 false、那个 id 回收回 400（并各配一句反向证据：那一行确实在库里、本租户那一条确实删得掉），
  由 J3 与 J5 两支注入**分别认领它的两句**（`H_FOREIGN_LIST` / `H_FOREIGN_REVOKE`）。
  ⚠ 本窗我先把这条边界写成了"只有 `PermissionServiceTest` 覆盖到" —— **那是错的归属**：那一支用的是 mock mapper，
  钉的是"条件串里有 `TENANT_CODE` 这个列名"，从没往库里放过一行 foreign 数据；真放过数据的是契约层那一支。
  两者不是一回事（"条件在"≠"条件有效"），写错文件名的代价就是下一窗可能有人去改错的那一层。
- **同一句 `IllegalArgumentException` 在 MockMvc 层与部署层的读数形状不同**：契约层拿到的 400 可以没有 body，
  fat jar 后面有 `LcExceptionHandler` 统一转成带 `message` 的信封。所以 D 战役不能套 K/J 战役的预期红集，
  两支各钉各的（`[15t]` 里那三支"消息要说清是哪一栏/哪一项"的探针只在部署层存在）。
- **权限矩阵页在真浏览器层零断言**（连 §11 那句 `text.includes(label)` 的浅检查都没有覆盖它 —— 顺带说，那种整页找词的断言
  正是缺陷 #29 点名过不要写的东西，补一条同形态的没有意义）。要补的话形态是 §11d 那种：种一条应用级授权 + 一条实体级授权，
  在真页面上读格子的亮/暗并与 `/check`、与库里的 `entity_code` 三方对齐。**这是 #48 剩下的唯一一层，已登记为 #49 候选。**

**09-26 06:4x – 06:5x 这一窗（#47 收线 + 两条过期契约断言修成仍可否证的形式）四道闸串行同轮实跑，
五个退出码一起落在 `~/.cache/zlc47/gates.status`，日志各自在 `~/.cache/zlc47/gates/{java,boot,api,check,browser}.log`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B clean install` | **BUILD SUCCESS**、**4646** 个用例 0 红 0 错 0 跳（surefire 模块汇总行现加 = 2701+525+**1251**+112+**57**；`JAVA_EXIT=0`。02:1x 那一记的 4627 → 4646 = z-lc-core +10（#46/#47 的折叠与对账单测）+ z-lc-web +9（#43/#47 的契约探针）。⚠ **同一条链第一次跑时这一格是红的**（`JAVA_EXIT=1`，`LcHttpContractTest` 两条）：`provision` 现在回 `ALTERED` 且 `addedColumns:["cc"]`、那一栏真进了库 —— 是 #43 那两条断言的**前提**被 #47 换掉了。改成"ALTERED + 库里真有那一栏 + 另配一张真修不好的 foreign table"之后，由 K1–K8 八支注入逐条重新证牙（`RESULT: contract-layer falsification done`） |
| 部署件 | 用刚构建的件重启 18090 | `BOOT_EXIT=0`（pid 74843），健康检查 200 之后才放行接口层 —— 这一格单列出来是因为上一窗踩过"改了 src 忘了 install，接口层在测旧件" |
| 接口 E2E | `python3 _e2e/e2e_api_test.py` | **434/434**：`api.log` 里逐字有 `=== E2E RESULT: 434/434 passed ===` 这一行，`API_EXIT=0` 在同一轮的 marker 文件里。353 → 434 的 **+81 按段现数**（拿两轮日志各自的
`[NNx]` 横幅 + 段内 PASS 行现算，加法与总账 81 对得上）：新增三段 `[15q]` **39**（#46 事件链折叠）、
`[15r]` **22**、`[15s]` **12**（#45/#47 的编辑路径与补列），加上 `[15j]` 33 → **39**（+6）与
`[4]` 2 → **4**（+2）；日志里共 **29** 段（`[15j]` 之后直接跳到 `[15p]`，没有 15k–15o），其余 **24** 段一条没动。
⚠ 上一版这一格写的是"`[15q]/[15r]/[15s]/**[15t]**` 那几段"
—— **`[15t]` 这一段不存在**，是我照着前三个的编号形状**编**的第四个（"+81 涨在几段里"这种句子
不含量具，写的人只会数到手边那几段）。段名与段内计数一律以 `grep '\[[0-9]+[a-z]?\]' api.log` 为准 |
| 前端 | `cd z-lc-admin-ui && npm run check` | tsc 0 / lint 0 warnings / **vitest 238/238（25 个文件）** / build 绿，产物 `index-CAT5sfEl.js`，`CHECK_EXIT=0`（223 → 238 的 +15 **整份住在新增的那一个文件里**：`check.log` 逐文件行数出来 `DesignerProvision.test.tsx (15 tests)`，其余 24 个文件一条没动 —— 这个归属是现读的，⚠ 我手上另一处记录把它写成"30 例"，那是没量过的数） |
| 真浏览器 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **三轮各自 `PASS 184 / FAIL 0`**、`全绿轮次: 3/3`、`BROWSER_EXIT=0`。160 → 184 的 **+24 整段来自新增的 11d**（"provision 的四种结论在浏览器里各说各的话，且每一句都有库作证"）：归因是拿段内 25 个静态 `check(` 的标题逐个去一轮日志里前缀反查的 —— **24 个各命中恰好 1 行**，第 25 个（`provision 结论走真后端（#43/#47 浏览器层）`）挂在 catch 分支里，只在整段抛异常时才打，绿轮次里它不存在。**这是"静态数不等于分母"的第二个反向样本**（11a 那一次是 +2 与 −1 混着来，这一次是干净的 −1），所以这一格的分母仍然只从 `=> PASS 184 / FAIL 0` 现读。唯一的非 2xx 是那条**被登记放行的 400**（`/api/lc/pipeline-config/create`，它正是被某一条检查点名过的被拒请求） |
| 注入自证 | 30 支（`ls` 现数：`_e2e/` 14 + `z-lc-admin-ui/e2e/` 16），带锁 25 支 | 本窗新跑并收线的两支：`mutate_provision_contract_guard.py` **K1–K8**（java 契约层，8 支注入认领 11 条具名红、每轮分母钉 55 条、恢复后 55 全绿、源码 md5 与开跑前逐字节一致）；`mutate_provision_browser_guard.py` **P1–P6**（真浏览器层，基线 184/0 → 六支各自只红预期那几条、零连带红 → 恢复后 184/0，`RESULT: 11d 的 8 条各自钉住一件事；6 条按未覆盖记账`）。另外 `mutate_replay_guard.py` 本窗**补上了锁**（它改的 `SchemaAdminBizService.java` 正是 provision 那几支也在就地改写的文件），并实测过"锁被占时 `exit 2` 且一个源文件都不碰" |

⚠ K4 那一支（摘掉「这张表不是引擎建的」那道闸）的读数是本轮最值钱的意外：我**预期**它会"把坏的那支变成好的、
让 `allOk` 翻 true"，实测它仍然判 `FAILED`、汇总看上去一模一样 —— 真正的差别只在**库里多了一列**（别人的表被动了）。
也就是说这一条保证不是被"报告一致性"守住的，是被那一条读物理列的断言守住的。
预期红集写错时**改的是账，不是断言**：这里改的是 K4 的预期（连带把 T1 的第一条红从"不归这份定义管的表"改到
"没说是哪一类缺列"），而 K1/K6 那两处偏差是量具的 bug —— JUnit 报的是 assert **调用**那一行，
长消息参数常在续行上，差一行就会把一条正当的红读成"红在另一条"（脚本里 `statement_start()` 归这一口）。

⚠ **提交归属记一笔，因为我自己写错了一次**：`8bbe067` 的正文列了三处"没量过的归属"被改成现数的，
而 `git show --stat 8bbe067` 只有 **1 行** —— 前两处（接口层段名、浏览器 11d 的分母）实际随
`13250a1` 一起推走了，后一笔只带了 #47 那一格的 25→24。**已推送的正文不 amend**，所以真相记在这里：
一句描述"这次改了什么"的话，如果没有任何尺会去读它，它就会一路错下去 —— 与门禁红不红无关，
和这一整窗被修掉的那几处是同一类缺陷（`[15t]` 那个不存在的段名同样没有任何尺会读）。

**09-26 02:0x – 02:1x 这一窗（#42 收线）四道闸串行同轮实跑，四个退出码一起落在
`~/.cache/zlc42/gates.status`，日志各自在 `~/.cache/zlc42/gates/{java,api,check,browser}.log`：**

| 层 | 命令 | 实测（本轮现读日志，不是沿用） |
|---|---|---|
| Java | `mvn -o -B test` | **BUILD SUCCESS**、**4627** 个用例 0 红 0 错 0 跳（分母按 surefire 模块汇总行现加 = 2701+525+**1241**+112+48；`JAVA_EXIT=0`。上一记 4617 → 4627 的 **+10 全部来自 #42 那三个类**（`git show HEAD:<文件> | grep -c "@Test"` 与盘上现数各量一次：
`PipelineStagesTest` **17 → 23**、`PipelineConfigServiceTest` **23 → 26**、`PipelineWriteChainTest` **13 → 14**，
6+3+1 = 10 对得上；模块级分母 z-lc-core 1231 → **1241**） |
| 接口 E2E | `python3 _e2e/e2e_api_test.py` | **353/353**，`E2E RESULT: 353/353 passed` 与 `API_EXIT=0` 两行**都在同一份日志里逐字存在**（342 → 353 = +11：`[15p]` 这一节从 58 涨到 **69**，加的是 #42 那三条被拒路径 + 它们的"文案点名要什么"孪生断言 + update 侧的探针）。打在 P 战役收线后重新构建并重启的 18090 fat jar 上 |
| 前端 | `cd z-lc-admin-ui && npm run check` | tsc 0 / lint 0 warnings / **vitest 223/223（24 个文件）** / build 绿，产物 `index-DiIAeQ4C.js`，`CHECK_EXIT=0`（221 → 223 = `pipelineVocabulary.test.ts` 里 #42 那两条：词表同源 + 不许有能填不生效的参数框） |
| 真浏览器 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **三轮各自 `PASS 160 / FAIL 0`**，`BROWSER_EXIT=0`（158 → 160 新增的 2 条住在 11a：`每一档都还在说这一档有没有参数` 与 `阶段参数不再是一个能填的框`；这一节自己从 21 → **23** 条，同样是**从绿日志的 PASS 行反数**，不是 `grep -c check(`）。⚠ 这一轮 5274 的 preview 是我这条链自己起的，起完留在原地 —— 浏览器层的注入量具 afterward 拒绝在"不是我起的 preview"上跑（实测报 `!! :5274 已经有东西在伺服`），这个拒跑是对的，别当成脚本坏了。**⚠ 02:2x 起这台的 preview 已经不在了**（我为了跑 P 战役把它关掉）：`lsof -nP -iTCP:5274 -sTCP:LISTEN` 现在空，18090 上是 P 战役收线时重建的那份原始字节件（pid 1270、`/api/lc/health` 200）。要再跑浏览器层得先 `npm run build && npm run preview:e2e`，否则产物指纹守卫会以 `BROWSER_EXIT=2` 拒跑（它不会拿"读不到"当绿） |
| 注入自证 | 23 支里的流水线那四支 | 单测 **U1–U6** 全 OK（`RESULT: #42 那六支各自打掉一句保证；恢复态 63 条全绿`）；前端 **F1–F13** 十三支全 OK（`RESULT: 前端口径注入自证完成`，`F_FULL_EXIT=0`，13 条 `OK` 行数出来的）；部署件 **P13/P14** 各按具名红收线（9 条 / 4 条），**P1–P14 整族**在本窗重跑 —— 第一次跑出 **3 处 MISMATCH**，三条都是量具的预期账目过期（P1 少算 #42 新增的那条单测、P4/P6 少算 update 探针带来的连带红），已按名字与机理补齐再重跑 —— **第二次整族跑完 `exit=0`，14/14 全 OK、日志里 `MISMATCH` 出现 0 次**
（`~/.cache/zlc42/deploy_mut/full14_r2.log`：baseline 353/353、fp `9f7d56976a25`，preflight 现数
**80 条预期红 + 4 条预期连带红**，收线时"重建成原始字节再跑一遍"回到 353/353、`[15p]` 69、
`restored sources: clean`）；浏览器 **S9**（160/0 → 158/2 恰好那两条 → 160/0） |

⚠ 那 3 处 MISMATCH 值得单独记一句：**它们全是"我预测的红集不全"，不是产品坏了** ——
同一轮里基线 353/353、恢复后 353/353、`restored sources: clean` 三个读数都没变。
把预期红集改宽是最容易做错的动作（改完就绿 = 自证失效），所以每一处补账都要写出机理：
P4/P6 是因为 **update 侧探针让创建闸失效的波及面变宽**（坏配置真写进行里，后面所有"按配置链执行"的检查
读到的就是它），P1 是因为 **`resolve()` 压根没被调用，那句 warn 也就没了**。

下面那四格（上一窗）：

| 层 | 命令 | 实测 |
|---|---|---|
| Java 单测 + 真 HTTP 集成测试 | `mvn -o -B test` | **BUILD SUCCESS**，**4617 个用例 0 红 0 错 0 跳**（23:1x 那一窗**整条重跑**，`MVN_EXIT=0` 逐字落在日志末行；20:07 那一窗记的 4573 是上一轮的：`Total time 01:50`。⚠ 上一版这格写的是 `Total time 12.7s` —— 本轮**没能复现**那个数，而"12.7 秒跑完 4573 个用例 + 43 条真 HTTP"这个量级本身就该让我怀疑它：那是单模块 `-pl` 的耗时形状，被当成全量门禁的耗时记进来了。耗时不是判据，但**记一个对不上的耗时，说明那一行不是整轮实测**。分母按 surefire 的模块汇总行现算 = 2701+525+**1231**+112+48，出自 5 个有用例的模块；reactor 里另有父 POM 与 `z-lc-bootstrap` 两项没有用例，别把"七个模块"当成七个分母。z-lc-web 那 48 个里 `LcHttpContractTest` 46 个是真 HTTP）。⚠ 上一轮那句"六批全是前端改动、后端一行没碰"本轮**不成立**这一条到这一轮仍然适用，且指向的是**这一轮自己**：`DynamicSqlBuilder`（多维 `groupFields`）与 `AggregateQueryDTO`（新增该字段）真改了，z-lc-core 因此 1176 → **1190**（+14 条 `DynamicSqlBuilderAggregateTest`），z-lc-web 42 → **45**（+3 条 `LcHttpContractTest` 真 HTTP，打两维 `/aggregate` 与 `/runtime/shape` 的 pivot 步）。上一轮（09-23）那 +7 条是字段编码闸那一族（`SchemaAdminBizService` / `DbTableMapperService`），别把两轮的增量混成一笔。**⚠ 但这一轮（19:5x 之后那一段）后端一行没再动**：新加的是 `[15j]` 那 33 项与两支注入量具，Java 分母停在 4573 是**符合预期**的，不是"没跑"） |
| 后端接口 E2E | `python3 _e2e/e2e_api_test.py` | **342/342**（22:5x 那一窗整条重跑，日志末尾**真有** `E2E RESULT: 342/342 passed` 与 `API_EXIT=0` 两行；20:20 那一窗记的是 283；打在 17:52 重新 install 并重启过的 18090 fat jar 上，跑完 server 仍 200）。⚠ **上一版这格写的是"19:02 与 19:06 各实跑一次，两次都 `API_EXIT=0`" —— 那句话的退出码部分是错的**：那两份日志（`/tmp/zlc_api_15j_b.log`、`/tmp/zlc_api_15j_c.log`）里只有 `E2E RESULT: 283/283 passed`，**没有任何 `API_EXIT=` 行**，marker 是我替它们**补上去的**而不是量出来的（20:20 这次才是真落码的那一轮：`python3 … > 日志 2>&1; echo "API_EXIT=$?" >> 日志`）。这是"把希望的证据当成已有的证据"，比重跑一次贵得多 —— 从现在起这一格只认日志里逐字存在的 marker。分母 283 → **342** = 本轮新增的一段 `[15p] 流水线配置决定执行链`，**58** 项（段内计数按 harness 自己的 `[NNx]` 横幅现数，`[11]` 那一段同时从 12 → 13），其余 28 段一项没动；上一轮那 33 项是 `[15j] 字段编码撞引擎自建列 / 非法列名`，其余段一条没动：5 个保留列 × 2（拒 + 文案要列出那五个保留列且不泄物理细节）、大写 `ID` × 2（盯 `toLowerCase`，MySQL/H2 列名不分大小写）、4 个非法编码变体 × 2（`2bad` / `我的字段` / `has space` / 空串，拒 + **文案必须指名是哪一列**）、"上面那些被拒的提交一行都没落库"、合法三列照常建成 → provision → `/runtime/list` 读到 `total=0`（这条是"闸没把正常路径一起按住"的反向证据）、`updateEntity` 第二个写入口 × 2、逆向映射 × 3（跳过自建列 / 用户列一列不少 / 跳过的清单写进 description）。⚠ 上一版这行记的是"250 项里**没有**一条专门打保留列 → 缺口见 #30"，**这一轮那个缺口由 `[15j]` 关闭**，别再照抄旧警告。写这段时修了三处自己的量具问题，都记在代码注释里：update 探针从"紧跟创建"移到**段尾**（放前面的话它一旦被放过，`fc_clean` 元数据变 `[deleted]`，后面 provision/`/runtime/list`/`/table/import` 一起翻车，一个 bug 有 6 种红法，注入实验就指不回调用点了）；`跳过引擎自建列` 那条原来是 `all(c not in RESERVED for c in map_codes)` —— **空清单会让 `all()` 真空为真**，注入 J5 把 `map_codes` 打成空时它照样绿，加上 `bool(map_codes) and` 才是真检查；`mapped = D(j)` 在 fail 信封下塌成 `None`，后面 `.get` 会让整段抛异常退出（"跑不完"会被误读成"没红"），改成 `D(j) or {}` |
| 前端静态 + 单测 + 构建 | `cd z-lc-admin-ui && npm run check` | tsc 0 error、lint 0/0（`--max-warnings 0` 下零输出）、**vitest 221/221（24 个文件）**、build 绿（产物 `index-D0LLXh1C.js`）。分母 211 → **221** = +10、文件 22 → **24**，是本轮新加的 `pipelineVocabulary.test.ts`（8 条）与 `PipelinesPage.test.tsx`（2 条）—— 这两个数都是从 23:2x 那一窗的 vitest 汇总行现读的，而 211 → 221 与 22 → 24 的差是从同一份日志的 `Test Files 24 passed (24)` / `Tests 221 passed (221)` 两行读出来的。**20:26 那一窗整条重跑并落退出码：`CHECK_EXIT=0`，末行 `✓ built in 9.68s`**（23:2x 那一窗再整跑一次：`CHECK_EXIT=0`、`✓ built in 6.31s`）（上一版这格报的是 `✓ built in 4.32s`，且老实写明"没有为退出码落 marker" —— 现在 marker 有了，耗时换成实跑的 9.68s，因为那一轮机器上还压着别人的 `vite build`）。⚠ 顺带量到一件有用的事：**同一份 `src/` 重构建出的 bundle hash 一模一样**（`Bhd3Gz8F` 在 20:10 与 20:26 两次构建里复现），所以"hash 变了"确实是"源码变过"的可靠指针，不是构建噪声。⚠ "vitest 不做类型检查"这个坑本轮**第 7 次**咬：`DesignerPage.tsx` 里那个只剩注入在用的 `SYSTEM_COLUMN_CODES` import 是 `npx vitest run` 全绿、`npm run check` 第一步 TS6133 报的。修法不是加 `// eslint-disable`，是把注入改成**自带字面量** —— 注入引用源码里已不存在的符号会红成"崩红"（ReferenceError），而崩红证明不了它想证明的那道闸 |
| 真浏览器门禁 | `E2E_REPEATS=3 node e2e/browser-e2e.mjs` | **PASS 158 / FAIL 0，三轮各自 158/0，`全绿轮次: 3/3`，`BROWSER_EXIT=0`**（09-26 00:2x 这一窗整轮重跑；分母 137 → **158** = 新增第 11a 段"处理流水线页"21 条，逐条见上文 #41 一节。汇总行是门禁自己打的；退出码是单独落的 marker —— 20:16 那一轮**只打了汇总行、没落 marker**，所以那一窗整轮重跑一次把 `BROWSER_EXIT=0` 真的写进日志，这一格现在两个证据各自存在）。打的就是本轮 `index-D0LLXh1C.js`：产物指纹守卫开跑前打印 `产物指纹：index-D0LLXh1C.js，比 src 里最新的文件新 176s`（`src/` 里最新的是 20:03 的 `columnRules.ts`，`dist/` 那份是 20:10 的构建；19:00 那一轮同一件产物、当时报 1319s。这一层的"打的是刚构建的那份"是它自己验的；`vite preview` 只绑 `[::1]:5274`，用 `127.0.0.1` 连不上）。分母 130 → **137** = 新增第 11c 段"字段编码那道闸：界面真的按得住保存"7 条：撞自建列时说的是「撞了引擎自建列」而不是「不合法」（`id` 本身是合法标识符，只说"不合法"是把两种错混成一种）、文案点名是哪一列且带出那五个自建列、**那一栏自己标红**（不是页面底部一句泛泛的警告）、保存按钮被按住、**拦住 = 一次写请求都没发出去**（新挂的 `entityWriteReqs` 监听器数 POST `/admin/app/entity/create` 与 PUT `/admin/entity`）、改回干净编码后闸**立刻松开**（警告消失且按钮可点，防"粘住"）、松开之后仍然不偷发写请求。**上一版那行"#34 那道闸在浏览器里没有一条'输入 `id` 就拦保存'的检查"的警告，本轮由这 7 条关闭** —— 但它关的是"界面有没有把住"，"服务端的闸在不在"仍然由 `[15j]` 与注入层负责。⚠ 上一版还留着的那条更一般的话仍然成立："照样全绿"不代表新缺陷被看见了。设计器那 4 条（#29）与交叉表那 20 条（上一版的 110 → 130，静态 21 个 `check(` 里只有 20 个是真检查 —— **静态数不等于分母**）形状不变；11a 那 21 条给"**静态数不等于分母**"补了第二个反向样本 —— 段内静态 `check(` 只有 **20** 个，真跑出来是 **21**：`readDropdown()` 里那句带 `${label}` 的模板被三个调用点各展开一次（+2），而 catch 分支里那句 `check('处理流水线页的顺序与拒绝', false, …)` 只在失败时打（−1）。所以这一节的分母是**从绿日志的 PASS 行反数**出来的，不是从源码 `grep -c` 来的 |
| 注入缺陷自证 | 上面那 **22** 支 `mutate_*.py`（`ls` 现数，09-26 00:2x 重敲的两个计数：`_e2e/` **8** 支后端 + `z-lc-admin-ui/e2e/` **14** 支前端；带锁的 **17** 支 = `grep -l _mutlock` 数出来） | 全部"预期全红、无未预期红"；**本轮（09-25 深夜 — 09-26 凌晨）新跑的三支**：前端 `mutate_pipeline_wiring_guard.py`（F1–F10 十支，见 #41 一节）、浏览器 `mutate_pipeline_browser_guard.py`（S1–S8 八支，11a 那 21 条各判一件事）、以及 F1–F10 那轮的产物 `index-D0LLXh1C.js` 后来就是 158 那三轮的被测件。上一窗新跑的五支**：后端 `mutate_group_fields_guard.py`（G1–G7 七个注入，判据分母钉成 `DynamicSqlBuilderAggregateTest` 这一个类的 16 条，每个注入跑两遍、7/7 与预期吻合、零连带红，恢复后复跑 16 全绿，`MUT_EXIT=0`）；前端 `mutate_pivot_guards.py`（P1–P10 十个注入各跑两遍 = 20 条 OK，判据分母 = `pivotModel.test.ts` 16 + `PivotView.test.tsx` 10 这 26 条，十个红集合互不相同、零连带红，恢复后 26 全绿）；前端 `mutate_pivot_browser_guard.py`（**第一次把注入自证做到浏览器层**：一支脚本自己 build、自己起 `vite preview`、自己跑整轮，基线 130/0 → 注入 P8 后 127/3 且三条全是具名红 → 恢复后 130/0；bundle hash 全程跟着变（`Bhd3Gz8F` → `B6JS1BiJ` → `Bhd3Gz8F`）说明被测件真的换过，`pivotModel.ts` 恢复后 md5 与注入前逐字节一致）。⚠ 这第三支**第一次跑时把自己报成了 FAILED**：它拿 `FAIL <名字>   << <读数>` 的整行去比 `EXPECTED_RED`，读数没剥掉 → 同一批名字**同时**判成"预期却没红"和"预期外的红"。修完解析是**整支重跑**通过的，不是拿归档日志离线重判（离线重判只能证明解析对了，证明不了那一轮真绿）。**第四、第五支把同一件事做到了字段编码闸的两层上**：后端 `mutate_field_code_deployed_guard.py`（**J1–J7 打在 18090 部署件上**：每支都要先 `mvn install` 换件、重启、再跑整份 `[15j]`，七个注入各自的预期红集合**互不相同**、分母恒为 283、`failures outside [15j]: (none)`，收线打印 `RESULT: deployed-layer falsification done`，恢复后 class 级指纹回到 pristine `bb52bb4ce511`。J4 的读数是 **0 条红 —— 这一支在部署件层按设计不可见**，它的红落在 Java 单测层，这条不是失败而是"哪一层看不见什么"的实测书证）；前端 `mutate_field_code_browser_guard.py`（**B1–B5 打在真浏览器上**：基线 137/0 → 五个注入分别红 4/1/1/1/2 条且全是具名红、bundle hash 五跳各不相同、恢复后回到 137/0 零连带）。上一轮新跑的两支：后端 `mutate_field_code_guard.py` 4 个注入（I1 摘掉整闸 / I2 丢 `toLowerCase` / I3、I4 逐个摘调用点）各跑两遍，**8/8 与预期吻合**；前端 `mutate_designer_field_code.py` 9 个注入 F1–F9 **各跑两遍 = 18 条 OK**，除刻意配对的 F8/F9 之外 7 支红集合两两不同（F8/F9 同标题不同断言，已在归档 `failureMessages` 里核，见 #36）。看板焦点桥那一支 4 个注入**各跑两遍**、四个预期红集合互不相同且零预期外的红（见「看板「移到」的焦点桥」一节）；表格这一支 2 个注入各跑两遍、零连带红（`ALL MUTANTS BEHAVED AS CLAIMED`，基线与恢复后各 9 全绿，`GridView.tsx` md5 字节校验）；列设置抽屉这一支 3 个注入（还原 schema 顺序 / 还原尾巴追加 / 写回时抹掉邻居配置）各跑两遍，三个预期红集合**互不相同**（{ORDER,HIDE} / {WIDTH} / {HIDE}）、零连带红、恢复后 3/3 绿。⚠ 判定口径七处记录在案：① 记录侧的重查注入允许连带红（理由见「记录侧」一节）；② **有一处守卫注入测不出来** —— `loading` 初值在 jsdom 里改坏不会红，见「设计器侧栏」一节，这条按"未覆盖"记账而不是抹掉；③ **churn 类注入必须按测试文件跑**，按全量跑会把 vitest 挂住 13 分钟而没有结论，见「表格视图的计数守卫」一节；④ **两支不能同时在飞**（上一轮实测把对方注入读成自己的基线红），锁的覆盖在**那一窗**是 **17 支**（`grep -l _mutlock` 现数；09-26 02:3x 再数是 **18 支**，本窗新增的 `mutate_pipeline_config_guard.py` 也带锁）：`z-lc-admin-ui/e2e/` 那 **14 支前端脚本全部**共用 `e2e/_mutlock.py`，后端接了 **3 支** —— `mutate_group_fields_guard.py`、`mutate_field_code_deployed_guard.py` 与本窗新增的 `mutate_pipeline_wiring_guard.py`（它们跑 mvn，不争 5274，但仍不能和另一支同时改源码）；`_e2e/` 下其余 **5 支后端脚本仍未接**，见上文那条警告；⑤ **浏览器层的注入量具必须自己 build、自己起 preview、自己收尾**，不能假设有人在跑 —— 那两条产物指纹守卫（bundle hash 一致 + 不比 `src/` 里最新文件旧）本轮就是被这支脚本**顺带验实在开火**的：注入后重建出 `index-B6JS1BiJ.js`，若不重建，预览仍在伺服修复态的 `Bhd3Gz8F`，那三条红永远不会出现，量具会安静地报"零红、无结论"。**本轮这条守卫又开了两次火**：一次拦下"我的 readiness 探针自己撒了谎"（用了 macOS 上不存在的 `setsid`，`vite preview` 根本没起，而循环照样打印"responding after ~120s"）→ 门禁以 `BROWSER_EXIT=2` 拒跑而不是报绿；⑥ **战役脚本不许被 import 就跑起来**（本轮最贵）：`mutate_field_code_deployed_guard.py` 结尾原本是一行裸 `sys.exit(main())`，我用 `python3 -c "import ...; artifact_fingerprint()"` "只想读一个函数"，结果它当场抢锁、改坏源码、重建 jar、起了第二个 JVM —— 我以为在只读，其实开了第二支写手。修法是 `if __name__ == "__main__":`（本轮 4 支一起补：deployed 那支 + 三支 duplicate 守卫），并且**那几分钟窗口里量出来的 283/283 一律作废重跑**，没有记成结论；⑦ **按位解包的 `_` 占位会静默绑错槽**：`for _, _, _, e, _, _ in RUNS` 把 `e` 绑到了第 4 槽的**替换文本**上，于是"两支的预期红集合不得相同"这条前置检查变成**永远通过**（实测旧写法 5/5 全不相同、从不警告），而我新加的"预期红的名字必须存在于清单里"则在字符串上逐字符迭代、报 `' ' 出现 18860 次`、会把每一次真跑都拦死。修法是逐个具名解包（`_expected_of()`），并且**新前置检查要双向实测**：造一个该拦的样本 + 一个该放的样本，只测一个方向的守卫等于没写 |

真浏览器门禁已经从「诊断工具」升格成**可信门禁**，靠的是两件事而不是等它自己变稳：
① 每个 check 独立 try/except + 失败截图与时间线，一次超时不再吞掉整轮；
② 内联编辑那个间歇缺陷找到了真因并修掉（见上文「字典 join fan-out」），`KNOWN_FLAKY` 豁免机制删除 ——
顺带把我最初那份"焦点被抢"的成因假设也否证了（打时间戳后成功样本的链路是干净的），
所以那三次基于假设的改动（rAF 再聚焦 / `onBlur` 宽限 / `onMouseDown`+`preventDefault`）全部回退，
仓库里没有残留半修好的改动。教训：**失败样本没抓到手时，按假设改代码就是瞎试。**
`E2E_REPEATS=N` 保留，用途是把通过率打出来；单次绿仍然不算证据。

**下一轮如果要继续，按这个优先级**：
1. ~~**仪表盘**~~ 已落地（2026-09-22，见上文「四、还没做的」里那条）。
2. ~~**把几何检查推广到另外三种画法**~~ 柱、折线、**看板、日历**全部落地（见上文两节：浏览器侧
   `checkDrawn` + 卡片/事件归属检查，单测侧 `axisRuler` + `KanbanView.test.tsx` 的落列断言）。
   四种视图现在都有"渲染器量出来的"断言。剩下的同类风险是**新视图**：以后加甘特/树形时，
   第一版就该带归属检查，别只数元素个数。
3. ~~**图表数据点多于轴标签时的抽样规则要能被证伪**~~ 已落（`ChartView.test.tsx` 第 14 条 +
   浏览器侧 `isOrderedSubset`/逐标签配对，反证见上文第 4 次）。
4. 服务端批量导入的事务化（连带解决主池连接泄漏的"半提交"风险）、字典值域严格开关
   （现在预览里只 warn 不拒）。
5. ~~基础设施两笔未定位账~~：**泄漏源已定位并修掉**（逆向映射 `DbTableMapperService` 借连接不还，
   见上文缺陷 #23 与反证脚本），剩下的那笔是 `RuntimeCrudExecutor` 自建 JdbcTemplate 导致
   `@Transactional` 失效（现在靠有限 maxWait + 补偿回滚兜着）。
6. 生产 MySQL migration（`z-opc/_doc/004_sql/migrations/2026-09-20_z_lc_relation_type_width_and_app_icon.sql`）
   逻辑在 H2 上验过，但**从未在真 MySQL 执行过**。
7. 能力面还差的：undo 栈深裁剪与选择性撤销、字段级权限（`z_lc_permission` 只到 app/entity 粒度）、
   webhook。
8. ~~**导入结果的账目**~~ 已落（2026-09-22，见上文「导入向导的账要对得上库里的真值」：
   单测 5 条 + 浏览器 11 条，四类口径各自反证过）。
   ⚠ 当时顺手写的"同类风险：`bulkDelete` 也在拿列出来的条数当发生的条数"是**错的** ——
   `UndoHistoryDrawer` 先看 `outcome.applied` 才敢报成功，账目本来就是干净的；
   真正剩下的那件事（**批量删除没有服务端批量端点**，`deleteRecords` 是 N 个串行 HTTP 请求，
   中途失败留下"删了一半"且没有补偿回滚）**同一晚已经落地**：
   `/runtime/delete-batch` + 整批预检 + 补偿回滚 + 逐条 undo 登记，见上文「批量删除」一节。
   `deleteRecords` 已从 `src/api/runtime.ts` 删除，别再照着旧注释找它。
   下一步同类形态的活是上面第 4、5 条（导入事务化 / 那两笔基础设施账）。
9. ~~**同一类谎话还剩多少处没接**~~：8 个管理页**已全部查完并修掉**（见上文「管理页列表」一节，
   7 个列表页 + 应用选择器，八注入反证）；workspace 侧那三处已定位的出口（`WorkspaceLayout` 侧边栏、
   `AppOverviewPage` 统计位与空态、`useEntityOptions` 及其下游 `AiModelingPage` 的 toast）
   已修并八注入反证（见上文「workspace 侧的同一类谎」）；最后三处 —— `GalleryView` 的无条件
   「暂无数据」、`CalendarView` 那句三秒就消失的 toast + 一整月空格子、`UndoHistoryDrawer`
   的「这个实体还没有数据变更」—— **2026-09-23 也接上五态口径并十二注入反证**
   （见上文「记录侧的同一类谎」）。
   ⚠ **收尾时把全仓库的空态出口又扫了一遍**（`grep` 所有渲染 `Empty` / `emptyText` / "还没有" 的位置，
   含 designer 与 apps 两个目录）：`DesignerIndexPage`、`AppListPage`、`WorkspaceViewPage` 都是干净的
   （失败支在前、空态在后）；`KanbanView`/`ChartView`/`CalendarView` 那几句"这个实体没有可分组的字段 /
   需要日期字段"是**客户端从字段定义算出来的**，上游 meta 已按 `read` 门禁，不是同一类谎。
   ✅ 最后那一处也补完了（**2026-09-23，本轮**）：`DesignerPage` 侧栏接上 `StateBlock` 五态，
   7 条用例 + 七个注入反证（见上文「设计器侧栏的同一类谎」）。至此**这一族已定位的出口全部查完**，
   浏览器层同时换掉了设计器那句 `includes('字段')` 式的假断言。
   查法仍然一样：先写下"这个接口挂了时界面会说的那句谎"具体是哪句话，再补注入式反证。
9b. ⚠ **"一次挂载只发一次请求"是 UI 断言抓不到的一类缺陷**（日历无限重查 43 次/300ms，画面完全正常）。
   ✅ **本轮收口**：现在的覆盖是日历 + 画廊 + 设计器 + **表格**，浏览器侧 5 条计数检查
   （表格两条、日历一条、设计器两条）。
   **本轮把剩下的取数点逐个查过，结论比"都补一条"更省事**：能自激重查的只有手写
   `useCallback(...) + useEffect(...)` 那一族 —— 而 `GridView.load`（依赖里带 `page/pageSize/conditions/
   conjunction/state.sorts`，是这个仓库里最容易被改出循环的一处）**此前没有计数守卫**，现在有了。
   而 `KanbanView` 的 rows、`ChartView`/`DashboardPage` 的 `chartQuery`、`reference` 字段的候选、
   `UndoHistoryDrawer` 都走 react-query：queryKey 是**结构化 hash** 的，数组每次换身份不会多打一次请求
   —— 给它们补计数用例是在测 react-query，不是测我们。
   ⚠ 同一轮的另一条账是**脚本自己**：churn 注入按全量跑会把 vitest 挂住 13 分钟而没有结论（#31），
   所以这类注入按测试文件跑。以后新增视图（甘特/树形）若用手写 effect 取数，第一版就该带计数用例。
10. 两条没定口径的接口账：`/dict/list` 忽略传入的 `appCode`（返回租户下全部字典，跨应用可见）；
    `/api/lc/admin/app/create` 与 `/api/lc/app/create` 对同一个错误回的 HTTP 形状不一致
    （前者 200 + `success:false`，后者 400）—— `[15i]` 里为后者专门放宽过断言形状，
    这个不一致本身就是待办。

**所有改动仍未 commit（`git status --porcelain` 51 个条目 / `git add -A --dry-run | wc -l` = **202 个文件**，
全在工作区，用户未要求 commit）**：
其中 30 条是已跟踪文件的修改，21 条是未跟踪项 —— 而 `z-lc-admin-ui/`、`_e2e/`、`core/undo/`
这些是**整个目录算一条**，所以"45/51 项"从来不是文件数。要报工作量就报 202 这个数，
别拿 porcelain 的行数当文件数。HEAD 仍是 `65ca433`。
（196 → 198 → 199 → 200 → **202** 的增量 = `DesignerEntityStates.test.tsx` + `mutate_designer_entity_guard.py`
+ `mutate_grid_refetch_guard.py` + `mutate_kanban_keyboard_guards.py` + 本轮的
`ColumnManagerDrawer.test.tsx` 与 `mutate_column_order_guards.py`；porcelain 仍是 51 行，
因为这些新文件都落在本来就未跟踪的目录里。）

