# z-lc

> 低代码平台（Low-Code）—— DB 驱动的通用 CRUD 引擎（Phase 1）+ 模型/SPI 设计框架

它解决的是"每个业务应用都要重抄一遍 建表→字段类型→列表/表单→权限→流程→部署 这条链"的问题：
应用与实体定义落在元数据表里，运行时由 `z-lc-core` 的动态 SQL 与字段类型注册表把 CRUD、聚合、
导入导出、视图配置、物化、撤销、流水线、流程绑定这套动作真的执行出来；`z-lc-sdk` 用 SPI 把
表单/审批/任务/印章/流程的业务钩子留给宿主实现，`z-lc-design` 用 `@LowCodeModel` /
`@LowCodeModelService` 注解 + 收集器把"一个 Java 类"变成"平台上可配的一个模型"。
对外是一层 `/api/lc/*` 的 HTTP 面（**25 个 Controller**，分三组前缀：`z-lc-web` 的 `/api/lc/*` 主体、
`z-lc-report-web` 的 `/api/lc/report/*`、`z-lc-design` 的 `/api/lc/design`），前端是独立的
`z-lc-admin-ui`（React + antd）。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-lc`（`git@github.com:z-opc-foundation/z-lc.git`，分支 `main`） |
| **Maven 坐标** | `io.github.yuku123:z-lc:1.0.0`（根 POM `<packaging>pom</packaging>`） |
| **当前版本** | `1.0.3` —— 根 POM 写的是**字面 `<version>1.0.3</version>`，不是 `${revision}`**；`de33a9e`（2026-09-30）把 `com.zifang:z-opc:1.0.0-SNAPSHOT` 幻影父链与 `1.0.0-SNAPSHOT` 一起换掉的 |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.4`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘）；它的父是 `io.github.yuku123:z-boot:1.0.19`，**`z-boot-parent:1.2.4` 自身不再 import `z-boot-fleet`**（现读 repo1 上那份 pom：地板由 `netty-bom` / `junit-bom` / `jackson-bom` / `spring-framework-bom` / `spring-boot-dependencies` 五条 BOM import 供给，兄弟仓版本改由它自己的 `<properties>` 下发） |
| **Maven Central（ranged GET 实测）** | 已发布：`z-lc:1.0.0`（pom）与 `z-lc-common` / `z-lc-sdk` / `z-lc-core` / `z-lc-design` / `z-lc-web` 的 `1.0.0` jar 全部 `200`；`z-lc-admin:1.0.0` = `404`（`central` profile 的 `excludeArtifacts` 把它排除，胖 jar 只作本地/部署演练用）。**7 支 `z-lc-report-*` 在 repo1 上全 404**（pom+jar 都是）—— 它们 2026-10-11 才进 reactor，而 `1.0.0` 是 2026-09-30 发的，还没另发版本号 |
| **默认端口** | 默认 profile **不设** `server.port`（[`z-lc-admin/src/main/resources/application.yml`](z-lc-admin/src/main/resources/application.yml) 第一行就写了这一格已移除）；`dev` profile 用 `SERVER_PORT`，缺省 **18090** |
| **context-path** | 无。全部路由是绝对路径 `/api/lc/...`（`application.yml` 里那句 `--server.servlet.context-path=/meta` 是从 z-meta 抄来的残留注释，本仓没有 `/meta` 前缀） |
| **运行口径** | Java 17（`maven.compiler.source/target=17` 由 parent 下发）· Spring Boot 3.3.13（地板面值；本仓 `pluginManagement` 刻意把 `spring-boot-maven-plugin` 留在字面 `2.7.12`） |
| **最近更新** | 2026-09-30 |

### ⚠ 关于"本仓必须用 JDK 17 编译"这一条：实测**不成立**

组织里确实有仓（例如 z-webide 用 `String.repeat`、z-util-jdbc 的某些测试）需要 JDK ≥ 11/17 才能编过。
本仓逐条查过，**结论是 Java 8 口径真实可用，没有任何 JDK 17 硬要求**，证据：

- 14 份 POM（根 + 13 模块）里 `<release>`、`maven-enforcer-plugin`、`requireJavaVersion`、
  `maven-toolchains`、抬 Java 级别的 profile —— 全部 **0 命中**；也没有 `.mvn/jvm.config`。
  编译级别只有 parent 下发的 `source/target 8`。
- 本仓自己的产物：`z-lc-common` / `-sdk` / `-core` / `-design` / `-web` / `-admin` 的
  `target/classes` 各抽一支 class 读头 8 字节，major 全是 **52（Java 8）**。
- 上游关键件同为 major 52（2026-10-20 对 repo1 上那几支 jar 各抽一支 class 读头 8 字节实测）：
  `z-llm-gateway-core:0.1.7`（本仓吃的是改过名的新坐标）、`z-script-engine:1.0.2`、
  `z-util-core:1.2.4`（本仓根 DM 直钉这一格）、`z-boot-datasource-starter:1.1.0`、
  `okhttp:4.12.0`、`caffeine:2.9.3`（3.x 才要 Java 11，本仓吃的是 2.9.3）。
- 源码里没有 Java 9+ API：`bc4993f fix(java8)` 正是把 `ZLcIOUtil` 与 4 支测试里的 Java 9+ API 摘掉；
  现在再扫 `List.of` / `Map.of` / `String.repeat` / `isBlank` 只落在 javadoc 注释里，
  `Files.readAllBytes` 是 Java 7 的 NIO2。
- 仓内留下的实测读数也是 JDK 8 跑的：[`z-lc-core/pom.xml`](z-lc-core/pom.xml) 记
  `warm clean test，JAVA_HOME=corretto-1.8.0_492`。

唯一要留神的是反向的坑：parent 供的是 `source/target 8` 而**不是 `--release 8`**，
所以在 JDK 17 上编本仓也能过，但会静默收下 Java 9+ API、把 Java 8 运行期炸点带出去。
新增代码请按 Java 8 语言级与 API 面写，别指望编译期兜。

---

## 🎯 能力清单（都能对应到类或端点）

| 能力 | 入口 | 说明 |
|------|------|------|
| 运行时通用 CRUD | `RuntimeCrudController` `/api/lc/runtime` | `list` / `get` / `create` / `update` / `delete` / `delete-batch` / `aggregate` / `shape` / `import/preview` / `import/commit`，落到 `z-lc-core` 的 `DynamicSqlBuilder` |
| 字段类型处理 | `core/fieldtype`（17 支 handler + `FieldTypeRegistry`） | Int/Decimal/Date/DateTime/Boolean… 的读写与校验 |
| 建模与库表管理 | `SchemaAdminController` `/api/lc/admin`、`DbTableController` `/api/lc/admin/db` | 应用/实体/字段定义、物理表与列校对 |
| 应用与事件 | `AppAdminController`、`EventController`（同挂 `/api/lc/app`） | 应用管理、事件登记与折叠 |
| 字典 | `DictAdminController` `/api/lc/dict` | 字典与值域 |
| 关系 | `RelationController` `/api/lc/relation` | 实体间关系 |
| 视图配置 | `ViewConfigController` `/api/lc/view-config` | 列表/看板/透视等视图配置 |
| 物化 | `MaterializationController` `/api/lc/app/materialize` | 物化任务与 `MaterializationEventService` |
| 部署 | `DeploymentController` `/api/lc/deployment` | 定义版本落库与部署态对账 |
| 撤销 | `UndoController` `/api/lc/undo` | 快照式 undo |
| 处理流水线 | `PipelineConfigController` `/api/lc/pipeline-config` | `core/pipeline` 的 processor 链真的执行 |
| 流程绑定与触发 | `WorkflowBindingController` `/api/lc/workflow-binding`、`WorkflowTriggerDispatcher`、`CamudaAdapter` | 绑定关系 + 触发派发；远端流程引擎（z-camuda 退役后为 z-wf-admin 兼容层）的审批中心路径由 `z-lc.adapter.camuda.base-url` 指 |
| 权限 | `PermissionController` `/api/lc/permission` | 授权/回收（`PermissionService`） |
| 元数据与探活 | `MetaController` `/api/lc/meta`、`HealthController` `/api/lc/health` | `/health` 的 `data.status` 由每个连接池真探一次推出来（`DataSourceHealthProber`），配置不成串就拒起 |
| AI 建模建议 | `AiModelingController` `/api/lc/ai`、`core/ai/AiModelingService` | 走 `io.github.yuku123:z-llm-core` 的 `ChatGatewayService.chat(UnifiedRequest)`；`optional` 依赖，容器里没有该 bean 时退回模板建议；`apply-suggestion` 复用 schema / relation 服务**真落库**（口径见下） |
| 模型/页面设计框架 | `z-lc-design`：`@LowCodeModel`、`@LowCodeModelService`、`LowCodeModelServiceCollector`、`PageTemplateController` `/api/lc/design` | 注解 + 收集器把类注册成平台模型；knife4j OpenAPI 也在这层开 |
| SPI 扩展点 | `z-lc-sdk` 的 `spi/{form,approve,model,task,sign,workflow}` | `rg 'public interface' z-lc-sdk/src/main/java/com/zifang/z/lc/sdk/spi` 现数 **27** 支接口 + 抽象基类与 RPC 适配器 |
| 外部系统适配 | `z-lc-core/adapter`（12 个类：`CtcAdapter` / `MetaAdapter` / `ScriptAdapter` / `MistAdapter` / `OssAdapter` / `MqAdapter` / `CamudaAdapter` / `AdapterRegistry` / `JwtRelayInterceptor` …） | 每个 adapter 的地址走 `z-lc.adapter.<name>.base-url`，缺省 `http://localhost:8888`；`MetaAdapter`/`CtcAdapter` 用 Caffeine 2.9.3 做本地缓存 |

`z-lc-web` 的 `META-INF/spring.factories` 注册 `LcAutoConfiguration` 与 `LcModuleDataSource`，
所以宿主只需 embed `z-lc-web` 就能拿到全部 Controller；`z-lc-admin` 只是把这条链真的跑起来的入口。

### AI 建模建议接口契约（`/api/lc/ai`）

- `POST /model-suggestion`：LLM 生成实体 / 字段**草案**（`optional` 依赖缺失时退回模板建议），只出建议、不写库。
- `POST /apply-suggestion`：**真正落库**。复用 `SchemaAdminService.createEntity` 与
  `RelationService.createRelation`，把请求体的 `entities`（或 `suggestedEntities`）与
  `suggestedRelations` 写进 `z_lc_entity` / `z_lc_field` / `z_lc_relation`；`tenantCode` 取请求体，
  缺省 `default`。**不** provision 物理表 —— 建表由模型设计器单独触发。
- **返回契约**：`success` 仅在所有项真实落库（或幂等跳过）且无异常时为 `true`；
  `createdEntities` / `createdFields` / `createdRelations` 只含真正创建成功的项；
  `skippedEntities` / `skippedRelations` 为幂等跳过的项；失败时 `success=false` +
  `reason` / `failedItem`，且**不返回任何 createdXxx**。
- 已存在的实体 / 关系按编码幂等跳过；非法建议（缺编码、非法字段编码、结构错误）或落库异常一律如实失败，
  **绝不出现「解析了名字就报成功」**。每个实体 / 关系由落库服务自身事务独立提交，不保证跨项原子性。

---

## 🏗️ 项目结构

```
z-lc/
├── pom.xml                # 根聚合 POM：挂 z-boot-parent:1.2.4，版本字面 1.0.0，常开 flatten(oss) 1.5.0
├── z-lc-common/           # 公共层：DTO / 枚举 / 38 支 utils / 域模型（258 main + 261 test java）
├── z-lc-sdk/              # SPI 框架：27 支接口 + 注解 + 抽象基类，纯 API 无业务实现（62 + 55）
├── z-lc-core/             # 引擎：CRUD / fieldtype / adapter / pipeline / event / materialize /
│                          #   deployment / undo / permission / workflow / ai（122 + 106）
├── z-lc-design/           # 设计框架：@LowCodeModel 族、PageTemplate、collector、knife4j（10 + 10）
├── z-lc-web/              # HTTP 层：17 个 Controller（/api/lc/* 主体）+ 健康探测 + 自动配置（24 + 12）
├── z-lc-report-common/    # 报表域模型 / JSON Schema 契约：ViewSchema / DatasetSchema / WidgetSpec（20 + 0）
├── z-lc-report-datasource/# 报表多数据源 SPI：接入 / 注册 / 读取（8 + 4）
├── z-lc-report-dataset/   # 报表核心：内存 typed 列式引擎，多键 join / groupby / 表达式 / 过滤（11 + 5）
├── z-lc-report-render/    # WidgetSpec → echarts option 服务端组装（9 + 1）
├── z-lc-report-book/      # ReportBook 树状组织 / 整体发布快照（3 + 0）
├── z-lc-report-ai/        # LlmClient SPI + OpenAI 兼容实现，NL2Report（5 + 1）
├── z-lc-report-web/       # 报表 HTTP 层：7 个 Controller（/api/lc/report/*）（11 + 1）
├── z-lc-admin/            # 独立启动入口（pom 的 <name> = `z-lc :: admin`；历史上叫 z-lc-bootstrap）：3 个类、2 个测试类、
│                          #   spring-boot repackage 出胖 jar，不进 Maven Central
├── z-lc-admin-ui/         # React 18 + antd 5.29.3 + Vite 的管理台（独立 npm 工程，不在 reactor）
├── _e2e/                  # 端到端与注入自证脚本 + 250 部署演练，见 _e2e/README.md
├── _doc/                  # 项目文档，见文末「文档目录」
├── log/                   # 运行期输出目录（`**/*.log` 被 .gitignore 排除，不是源码）
├── LICENSE                # MIT
└── README.md
```

Maven reactor 里是 **13 个模块**（根 POM `<modules>` 实测：`z-lc-common`、`z-lc-sdk`、`z-lc-core`、
`z-lc-design`、`z-lc-web`、`z-lc-report-common`、`z-lc-report-datasource`、`z-lc-report-dataset`、
`z-lc-report-render`、`z-lc-report-book`、`z-lc-report-ai`、`z-lc-report-web`、`z-lc-admin`）。
其中 `z-lc-report-*` 那 7 支是 2026-10-11 CEO 拍板自 `z-report` 仓收编进来的报表能力域。
工作区里还有两个**不在 reactor、也没有任何被 git 跟踪的文件**的目录，别当模块读：

- `z-lc-bootstrap/` —— 只剩 `target/`；那是 `z-lc-admin` 改名前的旧壳（admin 的 `<name>` 与
  `spring.application.name` 至今仍是 `z-lc-bootstrap`）。
- `z-lc-spring-boot-starter/` —— 只剩 `target/`；该 starter 已迁到 `z-boot/z-boot-integration-starters`
  （根 POM 的 `dependencyManagement` 注释里点明了这件事）。

`z-lc-admin` 留在 reactor 是为了享受统一构建，但它 `repackage` 出的胖 jar 是端到端入口件，
`central` profile 用 `excludeArtifacts` 把它挡在发布之外 ⇒ **不会上 Central**（repo1 实测 404）。

---

## 与邻居的边界（防功能漂移）

<!-- D5-BOUNDARY-BEGIN：本块与 z-etl/README.md、z-lc/README.md 的同名小节**逐字一致**，改一处必须改另一处 -->

三条分工边界（2026-10-12 D5 拍板）—— 每条都配一个「这个需求该找谁」的判例，
防止后来者按感觉选仓。

| # | 主题 | 谁管 | 边界一句话 |
|---|---|---|---|
| 1 | 运行时事件流 | `z-mq` | 事件**产生即消费**；**实时流不归 ETL**，ETL 只做**存量与批量**。 |
| 2 | 应用内处理器链 | `z-lc` pipeline | 表单 / 数据流的**应用内**处理器链归 z-lc；**ETL 不做应用内处理**，只做**跨系统、跨库**搬运。 |
| 3 | 取数渲染 vs 物化落地 | `z-lc` 报表子域 dataset（原 z-report） | dataset 管**取数渲染（查询态）**；**ETL 管物化落地（存储态）**。 |

### 判例：这个需求该找谁

| # | 需求（具体形态） | 找谁 | 不找谁 | 为什么 |
|---|---|---|---|---|
| 1 | 订单支付成功后，下游要**秒级**收到通知并立刻处理（持续流量） | `z-mq`（`z-mq-client` 发 / `z-mq-broker` 消费） | `z-etl` | 产生即消费的运行时事件流是 z-mq 的活；ETL 只做存量与批量，不做实时流。 |
| 2 | 表单提交前，按配置依次做**字典解析、类型转换、必填校验、外键引用检查**（都在同一个应用内） | `z-lc` 的 `core/pipeline`（processor 链；配置端点 `PipelineConfigController` `/api/lc/pipeline-config`） | `z-etl` | 应用内处理器链归 z-lc pipeline；ETL 是跨系统、跨库搬运，不承接应用内处理。 |
| 3 | 每晚把 A 库的订单**历史全量**搬进 B 库的汇总表并**落库** | `z-etl`（`z-etl-core` 的 reader → transform → writer 作业；管控端点 `EtlJobController` `/api/etl/jobs`） | `z-lc` 的 `z-lc-report-dataset`（`/api/lc/report/dataset`） | 物化落地是存储态，归 ETL；dataset 只做查询态取数渲染，不负责把结果写回存储。 |

<!-- D5-BOUNDARY-END -->

本仓在这三条边界上各自的位置（具体落点见上文「能力清单」与「API 一览」）：

| 边界 | 本仓（z-lc）落点 | 不在本仓的落点 |
|---|---|---|
| 1 运行时事件流 | `z-lc-core/adapter` 的 `MqAdapter` 只是**发消息 / 订阅**这一侧 | 消息中间件本体在 `z-mq`；ETL 不接实时流 |
| 2 应用内处理器链 | `z-lc-core` 的 `core/pipeline`（`Pipeline` / `FieldProcessor` / 5 支 processor），配置端点 `/api/lc/pipeline-config` | 跨系统、跨库搬运在 `z-etl` |
| 3 取数渲染 vs 物化落地 | `z-lc-report-dataset` 的 `DatasetExecutor` / `CachingDatasetExecutor`（内存 typed 列式引擎），端点 `/api/lc/report/dataset` | 物化落地（写进目标存储）在 `z-etl` |

---

## 🔧 技术栈（全部取自 POM 实测）

| 层级 | 技术 |
|------|------|
| 语言 / 编译 | Java 17（`source/target 8`，非 `--release`），UTF-8，`-parameters` |
| 框架 | Spring Boot 2.7.18（`z-boot-parent:1.2.4` import 的 `spring-boot-dependencies:${spring-boot.version}`，其 `spring-boot.version` = 2.7.18） |
| 打包插件 | `spring-boot-maven-plugin` 本仓 `pluginManagement` 字面留 **2.7.12**（刻意分歧，动它等于换 fat jar loader） |
| 兄弟仓版本权威 | 由 `z-boot-parent:1.2.4` 的 `<properties>` 下发（**不再是 fleet**）：`z-util.version` **1.0.19**（本仓根 DM 另把 `z-util-core` / `-parser-json` / `-dsl-kernel` 直钉 **1.2.4**，就近优先）、`z-llm.version` **0.1.7**（`z-llm-gateway-core`）、`z-script.version` **1.0.2**（`z-script-engine`） |
| 数据源 | `z-boot-datasource-starter:1.1.0`（含 Druid；吃 parent 的 `${z-boot.version}` = 1.1.0。本仓不再直引 mybatis-plus）+ `spring-jdbc` |
| 驱动 | MySQL 驱动 `com.mysql:mysql-connector-j:8.0.33`（根 DM 顶住地板的 8.4.0，并整条抄回 `protobuf-java` exclusion）；`dev`/测试用 H2 内存库 |
| 其它受管 | `org.yaml:snakeyaml:2.0`（地板不供这一坐标，直钉顶住 CVE-2022-1471 那格）、`caffeine:2.9.3`、`okhttp`（地板 4.12.0） |
| 接口文档 | knife4j-openapi3-spring-boot-starter **4.1.0**（地板直钉同值） |
| 日志 | 五个库模块走地板 log4j2 2.25.4 + slf4j-api 1.7.36，并从 `z-util-*` 上摘掉 `log4j-slf4j2-impl`；`z-lc-admin` 是**刻意的相反口径**：`log4j2.version=2.20.0` + `slf4j.version=2.0.7`（修 `Log4jLoggerFactory.<init>` 的 NoSuchMethodError） |
| 前端 | React 18.3 + antd 5.29.3 + `@ant-design/pro-components` 2.8.10 + `@yuku123/render` ^1.0.1 + Zustand + React Query；Vite / vitest / Playwright 层脚本；Node `>=18.18` |
| 构建 | Maven（后端，flatten-maven-plugin 1.5.0 常开出**自包含** pom）· npm（`z-lc-admin-ui`）· Python（`_e2e/` 量具） |
| 发布 | `central` profile：`central-publishing-maven-plugin 0.7.0` + source 3.3.0 + javadoc 3.11.2 + GPG 3.2.7，`distributionManagement` 指 central.sonatype.com（凭据只在 `~/.m2/settings.xml`，不落仓） |

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

第三方与兄弟仓版本全由 `z-boot-parent:1.2.4` 供给（地板是它 import 的五条 BOM，兄弟仓版本是它的
`<properties>`），模块 POM 里不该再有字面版本钉；构建报找不到 parent 时，
先确认能解析到 `io.github.yuku123:z-boot-parent:1.2.4`（在 repo1，不在磁盘相对路径上）。

### 本地跑起来（不需要 MySQL、不需要 Docker）

```bash
mvn -B -DskipTests install -pl z-lc-admin -am
java -jar z-lc-admin/target/z-lc-admin-1.0.0.jar --spring.profiles.active=dev
# -> http://localhost:18090
#    健康检查 http://localhost:18090/api/lc/health
#    API 文档 http://localhost:18090/doc.html （knife4j，application.yml 里 enable: true）
#    H2 控制台 http://localhost:18090/h2-console（dev profile 打开）
```

`dev` profile（[`application-dev.yml`](z-lc-admin/src/main/resources/application-dev.yml)）吃
`jdbc:h2:mem:zlc;MODE=MySQL;…`，建表脚本是
[`z-lc-admin/src/main/resources/db/schema-h2.sql`](z-lc-admin/src/main/resources/db/schema-h2.sql)，
并把 `z-lc.adapter.*` 全指向自己，所以零外部依赖。

### profile 差异

| profile | 数据源 | 端口 | 说明 |
|---------|--------|------|------|
| 默认（不设 `SPRING_PROFILES_ACTIVE`） | `spring.datasource` 走占位符，缺省值是字面 `HIDE_IN_REPO` | 不写（由宿主/main-starter 决定） | 必须先注入环境变量，否则连不上 |
| `dev` | H2 内存库（`spring.datasource` 与 `z.base.db.lc.*`/`z.base.db.default.*` 三处必须同一实例同一凭证，否则 Druid 报 Wrong password） | `SERVER_PORT`，缺省 18090 | 端到端验证用 |
| `local` | 仓库里**只有** [`application-local.yml.example`](z-lc-admin/src/main/resources/application-local.yml.example)，真文件被 `.gitignore` 排除 | — | 示例文件里的 RDS 主机/账号只是形状，本 README 不复述其值；口令必须换成你自己的 |

### 环境变量与配置键（只列名字，不列值）

| 类别 | 名称 |
|------|------|
| Spring 数据源 | `SPRING_PROFILES_ACTIVE`、`SERVER_PORT`、`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_DATASOURCE_DRIVER` |
| 适配层地址（配置键 `z-lc.adapter.<name>.base-url`） | `Z_CTC_BASE_URL`、`Z_META_BASE_URL`、`Z_SCRIPT_BASE_URL`；`mist` / `oss` / `mq`（`z-lc.adapter.mq.broker-url`）/ `wf` 只有配置键、yml 里未给环境变量别名 |
| 模块数据源解析顺序 | `z.base.db.lc.jdbc-url` → `z.base.db.default.jdbc-url` → `spring.datasource.url`；三处都不是 `jdbc:` 串时，只有显式给了 `z.base.db.lc.host` + `database` 才按模板拼，否则**拒绝启动**（`LcModuleDataSource`） |
| 部署演练（`_e2e/deploy_250.sh`） | `ZLC_DEPLOY_HOST`、`ZLC_DEPLOY_DIR`、`ZLC_APP_PORT`、`ZLC_TUNNEL_PORT`、`ZLC_EXTRA_ENV` |

**所有凭据必须经环境变量或仓外配置文件注入，禁止写进 yml / jar / 镜像层。**

---

## 🔌 API 一览

`rg --no-ignore -o "@RequestMapping\(\"[^\"]*\"\)" --glob '*.java'` 实测得到的类级前缀（**25 个 Controller**，
24 个不同前缀 —— `/api/lc/app` 上挂了 `AppAdminController` 与 `EventController` 两个）：

| 路径 | Controller | 模块 |
|------|------------|------|
| `/api/lc/runtime` | `RuntimeCrudController` | z-lc-web |
| `/api/lc/admin` | `SchemaAdminController` | z-lc-web |
| `/api/lc/admin/db` | `DbTableController` | z-lc-web |
| `/api/lc/app` | `AppAdminController`、`EventController` | z-lc-web |
| `/api/lc/app/materialize` | `MaterializationController` | z-lc-web |
| `/api/lc/dict` | `DictAdminController` | z-lc-web |
| `/api/lc/relation` | `RelationController` | z-lc-web |
| `/api/lc/view-config` | `ViewConfigController` | z-lc-web |
| `/api/lc/pipeline-config` | `PipelineConfigController` | z-lc-web |
| `/api/lc/workflow-binding` | `WorkflowBindingController` | z-lc-web |
| `/api/lc/deployment` | `DeploymentController` | z-lc-web |
| `/api/lc/undo` | `UndoController` | z-lc-web |
| `/api/lc/permission` | `PermissionController` | z-lc-web |
| `/api/lc/meta` | `MetaController` | z-lc-web |
| `/api/lc/health` | `HealthController` | z-lc-web |
| `/api/lc/ai` | `AiModelingController` | z-lc-web |
| `/api/lc/design` | `PageTemplateController` | z-lc-design |
| `/api/lc/report/ai` | `AiController` | z-lc-report-web |
| `/api/lc/report/book` | `BookController` | z-lc-report-web |
| `/api/lc/report/datasource` | `DataSourceController` | z-lc-report-web |
| `/api/lc/report/dataset` | `DatasetController` | z-lc-report-web |
| `/api/lc/report/render` | `RenderController` | z-lc-report-web |
| `/api/lc/report/templates` | `TemplateController` | z-lc-report-web |
| `/api/lc/report/view` | `ViewController` | z-lc-report-web |

三组前缀的关系要写清，它们**不是**三张独立的面：对外仍是**一层** `/api/lc/*`，
`/api/lc/report/*` 是它下面的一组二级前缀（报表能力域），`/api/lc/design` 是另一支二级前缀。
计数口径：`find . -name '*Controller.java' -path '*/src/main/*'` = **25** 支 ——
`z-lc-web` 17、`z-lc-report-web` 7、`z-lc-design` 1。

`/api/approval-center` **不是**本仓的端点，它是 `CamudaAdapter` 调用远端流程引擎（z-camuda 退役后为 z-wf-admin 兼容层）的路径（只出现在注释里）。

---

## 🧪 测试

```bash
mvn -o -B clean install          # Java 层全量（surefire）
python3 _e2e/e2e_api_test.py     # 接口层：打真在跑的 server（dev profile 起 18090）
cd z-lc-admin-ui && npm run check # tsc --noEmit + eslint --max-warnings 0 + vitest + vite build
```

用例分布（`rg -c '^[[:space:]]*@Test'` 现数，注释里的 `@Test` 已避开）：
`z-lc-common` 2607、`z-lc-core` 1390、`z-lc-web` 118、`z-lc-sdk` 525、`z-lc-design` 110、
`z-lc-report-dataset` 37、`z-lc-report-datasource` 20、`z-lc-report-ai` 10、`z-lc-report-render` 5、
`z-lc-report-web` 1、`z-lc-admin` 6、`z-lc-report-common` 0、`z-lc-report-book` 0。
`z-lc-web` 那 118 条里 `LcHttpContractTest` 是真 HTTP 集成测试（自己起 server、用 OS 分配端口）。

注入自证（"闸能不能红"的负控）共 **43 支**：`_e2e/` 22 支后端 + `z-lc-admin-ui/e2e/` 21 支前端；
带互斥锁的那批共用 `z-lc-admin-ui/e2e/_mutlock.py`，两支不能同时在飞。

如实说明跑不过/要注意的：

- `mvn` 全量要能解析到 repo1 上的 `z-boot-parent:1.2.4` 与 fleet 点名的兄弟仓构件，离线且
  `~/.m2` 不全时会红在建 pom 阶段而不是在建逻辑。
- `_e2e/README.md` 里的分母读数（Java 4790 / 浏览器 285 / 接口 555 等）出自 **2026-09-27 那一窗**，
  本 README 没有重跑，只按 `_e2e` 自己的规矩当"上一窗账"读，别当当前绿灯。
- [`_e2e/deploy_250.sh`](_e2e/deploy_250.sh) 的 `JAR=` 仍指向换号前的
  `z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar`，而 `de33a9e` 之后实际产物是
  `z-lc-admin-1.0.0.jar` ⇒ 跑部署演练前要先改这一行，否则脚本在本地就找不到包。

---

## 🐳 部署

本仓**没有** `Dockerfile`、`docker-compose*.yml`、`k8s/`、`Makefile`（`find` 实测：仓库里连一份
Dockerfile 都没有），所以这里不写"docker build -t z-lc:latest ."这种命令。真实的部署形状是两条：

1. **胖 jar 直跑**：`java -jar z-lc-admin/target/z-lc-admin-1.0.0.jar --spring.profiles.active=dev`
   （H2，演练/联调用）或注入 `SPRING_DATASOURCE_*` 后接 MySQL 8。
2. **250 部署演练**（对真 MySQL 8 起服务并自证真的连着它）：

```bash
bash _e2e/deploy_250.sh all      # sync → db → schema → env → start → verify
bash _e2e/deploy_250.sh gates    # 三道硬闸各咬一次（每道都带负控，闸要能红才算存在）
```

可用子命令见脚本头部注释（`sync` / `db` / `schema` / `env` / `start` / `verify` / `collate` / `repair` /
`status` / `stop` / `gates` / `tunnel` / `api`）；远端动作在 `_e2e/deploy_250_remote.sh`，
只吃 stdin、不在远端留副本。主机上的凭据目录在 `~/.config/z-lc-deploy/`（0600），
仓库里一个字都不留。建表 SQL 在
[`z-lc-admin/src/main/resources/db/schema-h2.sql`](z-lc-admin/src/main/resources/db/schema-h2.sql)
—— 注意 `application-dev.yml` 的注释说它"从 `_doc/004_sql` 抽取"，而 **`_doc/004_sql` 这个目录现在不存在**。

Maven Central 发布走根 POM 的 `central` profile（`-Pcentral`，凭据来自 `~/.m2/settings.xml` 的
`central` server 与 `-Dgpg.passphrase`；先 `-Pcentral,central-dryrun` 验形状）。

---

## 📄 License

MIT，见根 [`LICENSE`](LICENSE)（`Copyright (c) 2026 z-opc-foundation`）；根 POM 的 `<licenses>` 同样声明
MIT License。旧 README 里"Internal use only / 版权属于 z-biz"的说法与仓内文件不符，已按实测更正。

_Maintained by the z-opc-foundation organization._

---

## 文档目录

- [`_doc/008_troubleshooting/`](_doc/008_troubleshooting/) — 踩坑与失败要点（DB schema 契约与已知坑）
本项目文档统一收口在 `_doc/` 下，以下逐条按 `find _doc -mindepth 1` 的**真实存在**列出：

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档，**目前 2 份**：
  - [`00-overview.md`](_doc/001_arch/00-overview.md) — 项目总览：模块架构、核心功能、对外集成与上下游、
    技术特点、目录结构。现状以本文件为准。
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md) — 2026-09-06 从
    `idea_workplace/z-opc/z-lc/` 拆仓时写的说明。⚠ 它的"父项目 `../pom.xml`
    （`com.zifang:z-opc:1.0.0-SNAPSHOT`）、单独 clone 无法 Maven build"那两条**已不成立**
    （`8361aea` 换挂 `z-boot-parent:1.2.4`、`c8041e6` 抬到 `1.0.21`，`<relativePath/>` 留空），
    读它当历史，别当现状；现状以本 README 与根 POM 为准。
    规范里常见的 `02-api.md` / `03-db-schema.md` / `05-frontend.md` / `07-roadmap.md`
    在本仓 **都不存在**，没有对应实现或文档就不写链接。
- [`_doc/003_script/`](_doc/003_script/) — 构建与发布脚本（GPG passphrase 注入器）；
  `_e2e/` 下的端到端测试件仍留在仓根（被测试与部署流程直接引用）。
- `002_deploy` / `004_skill` — **槽位不建**：部署 SQL 实际在
  `z-lc-admin/src/main/resources/db/`，本仓没有真 SKILL 定义。
- [`_doc/007_backlog/`](_doc/007_backlog/) — 待办与缺陷覆盖面，如实登记：
  - [`_doc/007_backlog/README.md`](_doc/007_backlog/README.md) — 待办索引与取数命令
  - [`feature001_workflow_binding_fires/TASK.md`](_doc/007_backlog/feature001_workflow_binding_fires/TASK.md) — 缺陷 #61：流程绑定这条链其余四层覆盖面
  - [`feature002_http_status_not_checked/TASK.md`](_doc/007_backlog/feature002_http_status_not_checked/TASK.md) — 缺陷 #62：`isSuccess()` 不含状态码
  - [`feature003_dead_envelope_parses/TASK.md`](_doc/007_backlog/feature003_dead_envelope_parses/TASK.md) — 缺陷 #63：5 处 `fromJson` 结构上必抛
  - [`feature004_codegen_identifier_crash/TASK.md`](_doc/007_backlog/feature004_codegen_identifier_crash/TASK.md) — 缺陷 #64：`camelCase` 在合法字段码上抛异常，一个坏字段让整批物化零产出
- 私有演进稿 `_doc/001_arch/model-evolution-plan.md` **本机存在但被 `.gitignore`（第 82 行）排除**，
  不在 git 索引里、clone 下来看不到，所以这里不给链接；按规范口径这一层等于**无已提交文档**。

跨文档之外的验证面另见 [`_e2e/README.md`](_e2e/README.md)（端到端续跑清单、四层闸门的读数与坑）。

---

<!-- z-opc-foundation-sibling-registry:v1 -->
## 🔗 兄弟仓全景（z-opc-foundation 组织）

本仓隶属于 [z-opc-foundation](https://github.com/orgs/z-opc-foundation/repositories) 组织——一人公司基座的全部自研件（现存 61 仓，含私有仓；口径 2026-11-24 GitHub org API 实读）。

| 仓 | 一句话说明 |
|---|---|
| [z-agent](https://github.com/z-opc-foundation/z-agent) | 云端 Agent 平台的库与 starter——ReAct/Plan 实现 + 注册中心 + REST 网关 + 只读控制面 |
| [z-agent-kernel](https://github.com/z-opc-foundation/z-agent-kernel) | Agent 框架基座——消息/工具/记忆/provider/会话 SPI + 轻量默认实现（纯 POJO 无 Spring） |
| [z-asset](https://github.com/z-opc-foundation/z-asset) | 企业资产台账——实物设备全生命周期 + 域名/证书/许可到期提醒 |
| [z-audit](https://github.com/z-opc-foundation/z-audit) | 审计与合规留痕——事件模型 + 哈希链 + 事件出口 SPI + 留存期策略 |
| [z-biz](https://github.com/z-opc-foundation/z-biz) | 商务外壳域——客户/合同/票据/对账四聚合 |
| [z-biz-erp](https://github.com/z-opc-foundation/z-biz-erp) | 进销存 ERP——商品/客商/采购/销售/库存流水/应收应付核销（z-biz 业务线） |
| [z-biz-security](https://github.com/z-opc-foundation/z-biz-security) | 业务数据安全防护入口——场景化策略注册 + 安全线四段流水线对外编排 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | Spring Boot Starter 聚合仓 + 版本权威（火车制）——唯一消费入口 z-boot-parent |
| [z-bot](https://github.com/z-opc-foundation/z-bot) | 本地 agent 应用——终端 CLI + HTTP/SSE web 控制台 + 多通道 gateway（飞书/钉钉/webhook） |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | RESP2 协议兼容的分布式内存缓存——Java 8 + Netty，Redis 客户端零修改直连 |
| [z-camuda](https://github.com/z-opc-foundation/z-camuda) | 【已退役·归档】基于 Camunda 的工作流封装层（原 z-wf 改名，与现行 z-wf 无共同历史） |
| [z-cognition](https://github.com/z-opc-foundation/z-cognition) | 业务认知域/认知舱——术语/概念/证据链 + memory/kb/ontology 渐进式模块 |
| [z-config](https://github.com/z-opc-foundation/z-config) | 分布式配置中心 + 服务注册中心（类 Nacos）——Netty 长连接推送 + React 控制台 |
| [z-ctc](https://github.com/z-opc-foundation/z-ctc) | 4A 中心——认证/授权/账户/审计，多租户 JWT + SSO 跨子域 |
| [z-dms](https://github.com/z-opc-foundation/z-dms) | 数据源管控底座——统一登记、统一建池、统一探活、变更统一推送 |
| [z-env](https://github.com/z-opc-foundation/z-env) | 本地中间件与基础设施环境——31 个 docker-compose 场景 + make 一键运维 |
| [z-etl](https://github.com/z-opc-foundation/z-etl) | ETL 引擎——DataX 式线性作业 + Flink 式 DAG 流管道（键控状态/水位/窗口） |
| [z-ext](https://github.com/z-opc-foundation/z-ext) | Java 扩展点框架——@ExtPoint/@ExtImpl 运行期选实现，五档侵入方式 |
| [z-frontend-common](https://github.com/z-opc-foundation/z-frontend-common) | 全仓共享前端基座——AppLayout/LoginPage 等前端规范的唯一承载 |
| [z-frontend-markdown](https://github.com/z-opc-foundation/z-frontend-markdown) | Markdown 通用共享组件包（npm）——MarkdownLiveEditor/NoteMarkdown/MermaidBlock 单一真源 |
| [z-frontend-render](https://github.com/z-opc-foundation/z-frontend-render) | 平台共享前端渲染引擎——后端 schema 是唯一事实源，报表与低代码共用 |
| [z-graph](https://github.com/z-opc-foundation/z-graph) | 独立图数据库——MVCC 版本链存储 + OpenCypher 子集 + Bolt 风格协议 + React 控制台 |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | 自研高性能 API 网关——Netty 手写运行时，Route/Predicate/Filter 模型可嵌 Spring Boot |
| [z-indexer](https://github.com/z-opc-foundation/z-indexer) | 代码索引服务——仓库注册表 + 定时重建索引 + 符号/调用链查询 |
| [z-introduce](https://github.com/z-opc-foundation/z-introduce) | 组织仓库展示站——全仓实测元数据汇总成可搜索静态站点 |
| [z-lc](https://github.com/z-opc-foundation/z-lc) | 低代码平台——DB 驱动通用 CRUD 引擎 + 报表/模型设计框架 **（本仓）** |
| [z-llm-agent](https://github.com/z-opc-foundation/z-llm-agent) | CLI Agent 服务化——把本机 CLI agent（CC/Hermes/Qoder 等）暴露成 OpenAI 兼容对话 API |
| [z-llm-gateway](https://github.com/z-opc-foundation/z-llm-gateway) | LLM 网关——OpenAI/Anthropic 双协议面 + 多 vendor 凭据池、限流与 token 记账 |
| [z-llm-msghub](https://github.com/z-opc-foundation/z-llm-msghub) | msgHub 中心消息路由——WebSocket 中心枢纽 + Agent 侧客户端 SDK |
| [z-mask](https://github.com/z-opc-foundation/z-mask) | 脱敏与遮蔽引擎——按敏感命中位置遮蔽，策略即数据 |
| [z-mcp](https://github.com/z-opc-foundation/z-mcp) | MCP 服务端 + 上游聚合网关——Java 8 手写协议层，Spring Boot 自动装配 |
| [z-meta](https://github.com/z-opc-foundation/z-meta) | 元数据管理平台——租户/应用/接口清单登记 + @BizModel 业务模型采集 |
| [z-mill](https://github.com/z-opc-foundation/z-mill) | 资料磨坊——全格式文档转 Markdown/JSON/纯文本/HTML（对标 MinerU） |
| [z-mist](https://github.com/z-opc-foundation/z-mist) | KMS 分布式密钥/秘密管理平台——集中加密存储 + Netty 协议 + starter 嵌入 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 自研分布式消息队列——NameServer+Broker+Store，形状对标 RocketMQ、自有 Netty 协议 |
| [z-msg](https://github.com/z-opc-foundation/z-msg) | 多通道消息 + WebSocket 实时下发基础设施——短信/邮件/站内信/Webhook/IM 平台 |
| [z-observer](https://github.com/z-opc-foundation/z-observer) | 可观测性域——指标/日志/链路 + 告警引擎 |
| [z-opc](https://github.com/z-opc-foundation/z-opc) | 一人公司统一后端+统一前端——单 JVM all-in-one 承载协作/Agent/RPA/交易/资产等业务域与自研中间件 |
| [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) | 组织治理大脑——事实表、强制规范、跨仓工具集、新机器恢复钥匙（本清单权威持有处） |
| [z-opcs](https://github.com/z-opc-foundation/z-opcs) | 任务撮合/服务市场——用户付费发布诉求，持有者接单完成，金额自动瓜分 |
| [z-ops](https://github.com/z-opc-foundation/z-ops) | 个人公司运维平台——部署单元/镜像构建/WebShell/K8s 巡检 + Ops Agent 执行器 |
| [z-oss](https://github.com/z-opc-foundation/z-oss) | 对象存储服务——自持 local 后端 + aliyun/s3 对接层，starter 注入 OssTemplate |
| [z-product](https://github.com/z-opc-foundation/z-product) | 产品设计信息汇总仓（开发域 PM 工件） |
| [z-qa](https://github.com/z-opc-foundation/z-qa) | 步骤式 API 链路测试平台——用例/计划/执行/报告/质量看板（对标 TestRail·MeterSphere） |
| [z-rpa](https://github.com/z-opc-foundation/z-rpa) | RPA 产品皮——应用市场/需求中心（执行归 z-script，触发归 z-schedule） |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | Java 8 + Netty RPC 框架——微内核 + SPI 插件化，形状参照 Dubbo/gRPC |
| [z-schedule](https://github.com/z-opc-foundation/z-schedule) | 分布式任务调度中枢（对标 XXL-Job）——选主 + 时间环排期，starter 嵌入宿主 |
| [z-script](https://github.com/z-opc-foundation/z-script) | 多 DSL 脚本平台 + Mock 平台——沙箱执行/版本灰度/可注册 MCP Tool，starter 内嵌 |
| [z-security](https://github.com/z-opc-foundation/z-security) | 数据安全域编排层——场景→策略→识别→脱敏→水印→审计 四段流水线编排 |
| [z-sense](https://github.com/z-opc-foundation/z-sense) | 敏感识别与分类分级引擎——数据安全线底座（规则目录 + 可插拔识别器 + 分类分级） |
| [z-skill](https://github.com/z-opc-foundation/z-skill) | Skill 平台聚合器——归一化各家 Skill 形状，再按各家 API 形状发回 |
| [z-surl](https://github.com/z-opc-foundation/z-surl) | 短网址服务——短码生成/解析/命中统计，starter 一行集成 |
| [z-task](https://github.com/z-opc-foundation/z-task) | 任务协作平台——项目空间/看板/任务卡片（看板范式，对标 Trello·Teambition） |
| [z-team](https://github.com/z-opc-foundation/z-team) | 数字员工协同域——全栈独立仓，自 z-opc 毕业 |
| [z-trade](https://github.com/z-opc-foundation/z-trade) | 交易域独立仓——自 z-opc reactor 迁出 |
| [z-util](https://github.com/z-opc-foundation/z-util) | 底层 Java 工具库——89 个模块 POM，Java 8/17 口径，按需引用按需升级 |
| [z-vector](https://github.com/z-opc-foundation/z-vector) | 自研 Java 向量数据库——嵌入式+可独立部署，ANN+Payload 过滤，兼容 Qdrant REST |
| [z-voice-demo](https://github.com/z-opc-foundation/z-voice-demo) | 语音链路 demo——流式识别→kernel LLM→流式 TTS→可打断 的契约验证实现 |
| [z-watermark](https://github.com/z-opc-foundation/z-watermark) | 水印引擎——显式文字/图片隐写/盲水印，三种技术成对「打+验」 |
| [z-webide](https://github.com/z-opc-foundation/z-webide) | 企业级 WebIDE 后端——Git 仓/共享目录秒变 code-server 实例，全生命周期管理 |
| [z-wf](https://github.com/z-opc-foundation/z-wf) | 自研流程引擎——不依赖 Camunda/Flowable，定义层与运行时全自研 |

**已退役仓**（迁至 [z-trash](https://github.com/z-trash) 组织，archived 只读）：

| 仓 | 说明 |
|---|---|
| [z-agent-proxy](https://github.com/z-trash/z-agent-proxy) | 三方 coding agent 适配代理（Aider/Cursor → kernel Agent SPI），2026-10-26 退役 |
| [z-kb](https://github.com/z-trash/z-kb) | 自研知识库系统（对标 LightRAG/RAGFlow），2026-11 并入 z-cognition 认知舱后退役 |
| [z-ontology](https://github.com/z-trash/z-ontology) | 本体层（TBox/ABox/RDFS 推理），并入 z-cognition 后退役 |

另有 [z-report](https://github.com/z-opc-foundation/z-report)（报表域，2026-10-11 撤销并入 z-lc（远端已删除））。

总清单权威持有处在 [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) `008_组织规范/005_兄弟仓清单.md`。
