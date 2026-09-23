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
mvn -B test                      # 4556 个 Java 测试（含 z-lc-web 42 个，其中 40 个真 HTTP 集成测试）
python3 _e2e/e2e_api_test.py     # 231/231 项断言，打真在跑的 server
python3 _e2e/probe_stats.py      # 非数值统计与字典值域 warning 的即席探针（要 server 在跑）
cd z-lc-admin-ui && npm run check   # tsc + eslint --max-warnings 0 + vitest 185 + vite build
```

注入缺陷自证（"补的测试到底钉不钉得住"唯一的答案，见下文各节）。14 支，前端 9 + 后端 5：

```bash
python3 _e2e/mutate_duplicate_guard.py            # 单测层：预检回到 deleted=0 口径
python3 _e2e/mutate_duplicate_guard_http.py       # IT 层：预检 / advice 分别注入
python3 _e2e/mutate_duplicate_guard_deployed.py   # 部署件层：A1..A4 + B，带 class 级指纹
python3 _e2e/mutate_connection_leak.py            # 逆向映射的连接归还
python3 _e2e/mutate_field_code_guard.py           # 后端字段编码闸：I1..I4（保留列/大小写/调用点）
cd z-lc-admin-ui && python3 e2e/mutate_degradation_guards.py     # 元数据降级口径 M1..M5
cd z-lc-admin-ui && python3 e2e/mutate_admin_list_guards.py      # 管理页列表五态 A..H
cd z-lc-admin-ui && python3 e2e/mutate_workspace_entity_guards.py# workspace 侧出口 A1..D1
cd z-lc-admin-ui && python3 e2e/mutate_record_fetch_guards.py    # 记录侧三出口 12 个注入
cd z-lc-admin-ui && python3 e2e/mutate_designer_entity_guard.py  # 设计器侧栏 D1..D7
cd z-lc-admin-ui && python3 e2e/mutate_grid_refetch_guard.py     # 表格计数守卫（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_kanban_keyboard_guards.py # 看板焦点桥 M1..M4（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_column_order_guards.py    # 列设置抽屉顺序契约 I1..I3（按文件跑）
cd z-lc-admin-ui && python3 e2e/mutate_designer_field_code.py    # 设计器字段编码闸 + 新建实体通路 F1..F9（按目录跑）
```

⚠ 每一支都自己报 `ALL MUTANTS BEHAVED AS CLAIMED` 才算数；退出码 0 而没跑完一整轮不等于通过。
⚠ 只有**产品源码**里注入的缺陷才算反证；改测试让它变红是在测测试，两件事别混着报。
⚠ **两支不能同时在飞**：它们都就地改写源文件，A 的"按字节还原"会把 B 正在判定的那份源码换掉。
本轮实测踩到 —— 后台那支还没收线就前台再开一支，基线报出 1 条红
（`字段表里不该预置引擎自建列: expected 3 to be 0`），那是**另一支的注入形状**，不是产品坏了。
假红还能回头查，假绿更糟（期待的注入被对方悄悄还原）。九支前端脚本现在共用 `e2e/_mutlock.py`：
拿不到锁直接 `exit 2` 并且**一个源文件都不碰**（已实测这一条）。

> `_e2e/e2e_api_test.py` 需要 server 已起且**是本次运行新起的**：H2 是内存库，重启即空，
> 脚本自己会建 app/entity/字典并 provision，所以可以直接反复跑。若指向一个已被重启过的
> appCode 会看到「实体不存在」的空态。

### dev 环境为什么能起来（三处非显然的坑）

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

### 已知语义空洞：字典列不校验值域
实测 `stage="NOPE"`（不在字典里）能通过校验并写进去，单条 create 与批量导入口径一致。
后果：网格只能回落显示原始码、看板/统计会冒出幽灵分组。
**故意没有直接改成拒绝** —— 字典项后来被删会让存量数据瞬间"非法"，存量库导入也常带未登记的码。
该做的是：preview 里把"值不在字典中"作为 **warning** 单独回传（不阻断），
再决定要不要在写路径上加严格开关。别顺手改成硬校验。

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

**注入 14 支里的两支新的**（都跑两遍、判定只看具名结果）：
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

**仍未覆盖**（按记账，不算做完）：接口层 231 项里没有一条打"保留列 → 400"，浏览器层没有一条
"输入 `id` 就拦保存"；两者都在任务 #30 待办里。逆向映射「从数据库导入」跳过自建列这条路，
目前只有后端单测钉着。

### 真浏览器 E2E 怎么跑

    cd z-lc-admin-ui
    npm run build && (npm run preview:e2e &)   # 生产构建 + preview，端口 5274
    node e2e/browser-e2e.mjs                   # 默认打 http://localhost:5274

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
`z-lc-admin-ui` 现在有 20 个测试文件 185 个用例（`npm run test`），已进 `npm run check`
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
- 目录命名：`z-opc/AGENTS.md` 约定子模块前端叫 `_frontend/`（如 `z-task/_frontend/`），
  当前是 `z-lc-admin-ui/`。要么改名，要么在 AGENTS.md 记一笔。
- 前端 `antd` chunk 1.26MB 已配 `chunkSizeWarningLimit: 1400` 并接受（应用本体只有 ~130kB，
  vendor 单独缓存），真要再小就得按组件引 antd。
- 本轮所有改动**都没有 commit**，全在工作区。

---

## 交接状态（本轮收尾时实测，不是回忆）

四层门禁当前状态（2026-09-23 08:00 一轮实测，非回忆；四层**同轮全部实跑**，没有沿用任何一行）：

| 层 | 命令 | 实测 |
|---|---|---|
| Java 单测 + 真 HTTP 集成测试 | `mvn -o -B test` | **BUILD SUCCESS**，**4556 个用例 0 红 0 错 0 跳**（本轮**实跑**：Total time 22.5s。分母按 surefire 的模块汇总行现算 = 2701+525+**1176**+112+42，出自 5 个有用例的模块；reactor 里另有父 POM 与 `z-lc-bootstrap` 两项没有用例，别把"七个模块"当成七个分母。z-lc-web 那 42 个里 `LcHttpContractTest` 40 个是真 HTTP）。⚠ 上一轮那句"六批全是前端改动、后端一行没碰"本轮**不成立**：`SchemaAdminBizService`（字段编码闸）与 `DbTableMapperService`（逆向映射跳过自建列）真改了，z-lc-core 因此 1169 → 1176（+7 条 `SchemaAdminBizServiceTest`） |
| 后端接口 E2E | `python3 _e2e/e2e_api_test.py` | **231/231**（本轮**实跑**，`API_EXIT=0`，打在**重新 install 并重启过**的 18090 fat jar 上 —— 新闸会在 `entity/create` 多拒几种 400，指旧件等于没测；跑完 server 仍 200；`[15i]` 唯一编码 24 项、`[15h]` 批量删除 28 项）。⚠ 这 231 项里**没有**一条专门打"保留列/非法编码 → 400"，那条只有单测层与注入层覆盖，见 #30 待办 |
| 前端静态 + 单测 + 构建 | `cd z-lc-admin-ui && npm run check` | tsc 0 error、lint 0/0、**vitest 185/185（20 个文件）**、build 绿（`CHECK_EXIT=0`；产物 `index-DBS7vap9.js`）。⚠ "vitest 不做类型检查"这个坑本轮**第 7 次**咬：`DesignerPage.tsx` 里那个只剩注入在用的 `SYSTEM_COLUMN_CODES` import 是 `npx vitest run` 全绿、`npm run check` 第一步 TS6133 报的。修法不是加 `// eslint-disable`，是把注入改成**自带字面量** —— 注入引用源码里已不存在的符号会红成"崩红"（ReferenceError），而崩红证明不了它想证明的那道闸 |
| 真浏览器门禁 | `E2E_REPEATS=2 node e2e/browser-e2e.mjs` | **PASS 110 / FAIL 0，全绿轮次 2/2**（打的就是本轮 `index-DBS7vap9.js`，preview 回读 `/` 与 `/assets/index-DBS7vap9.js` 都是 200，同一个 hash；`BROWSER_EXIT=0`）。⚠ 分母与上一轮**完全相同 = 浏览器层这一轮一条都没加**，"照样全绿"不代表新缺陷被看见了：#34 那道闸在浏览器里还**没有**一条"输入 `id` 就拦保存"的检查（记在 #30 待办），它现在只被单测层与注入层钉住。设计器那 4 条（#29）仍是上一轮的形状 |
| 注入缺陷自证 | 上面那 **14** 支 `mutate_*.py`（前端 9 + 后端 5） | 全部"预期全红、无未预期红"；本轮新跑的两支：后端 `mutate_field_code_guard.py` 4 个注入（I1 摘掉整闸 / I2 丢 `toLowerCase` / I3、I4 逐个摘调用点）各跑两遍，**8/8 与预期吻合**；前端 `mutate_designer_field_code.py` 9 个注入 F1–F9 **各跑两遍 = 18 条 OK**，除刻意配对的 F8/F9 之外 7 支红集合两两不同（F8/F9 同标题不同断言，已在归档 `failureMessages` 里核，见 #36）。看板焦点桥那一支 4 个注入**各跑两遍**、四个预期红集合互不相同且零预期外的红（见「看板「移到」的焦点桥」一节）；表格这一支 2 个注入各跑两遍、零连带红（`ALL MUTANTS BEHAVED AS CLAIMED`，基线与恢复后各 9 全绿，`GridView.tsx` md5 字节校验）；列设置抽屉这一支 3 个注入（还原 schema 顺序 / 还原尾巴追加 / 写回时抹掉邻居配置）各跑两遍，三个预期红集合**互不相同**（{ORDER,HIDE} / {WIDTH} / {HIDE}）、零连带红、恢复后 3/3 绿。⚠ 判定口径四处记录在案：① 记录侧的重查注入允许连带红（理由见「记录侧」一节）；② **有一处守卫注入测不出来** —— `loading` 初值在 jsdom 里改坏不会红，见「设计器侧栏」一节，这条按"未覆盖"记账而不是抹掉；③ **churn 类注入必须按测试文件跑**，按全量跑会把 vitest 挂住 13 分钟而没有结论，见「表格视图的计数守卫」一节；④ **两支不能同时在飞**（本轮实测把对方注入读成自己的基线红），九支前端脚本已共用 `e2e/_mutlock.py`，见上文那条警告 |

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

