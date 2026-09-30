# z-lc

> 低代码平台（Low-Code）—— DB 驱动的通用 CRUD 引擎（Phase 1）+ 模型/SPI 设计框架

它解决的是"每个业务应用都要重抄一遍 建表→字段类型→列表/表单→权限→流程→部署 这条链"的问题：
应用与实体定义落在元数据表里，运行时由 `z-lc-core` 的动态 SQL 与字段类型注册表把 CRUD、聚合、
导入导出、视图配置、物化、撤销、流水线、流程绑定这套动作真的执行出来；`z-lc-sdk` 用 SPI 把
表单/审批/任务/印章/流程的业务钩子留给宿主实现，`z-lc-design` 用 `@LowCodeModel` /
`@LowCodeModelService` 注解 + 收集器把"一个 Java 类"变成"平台上可配的一个模型"。
对外只有一层 `/api/lc/*` 的 HTTP 面（18 个 Controller），前端是独立的 `z-lc-admin-ui`（React + antd）。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-lc`（`git@github.com:z-opc-foundation/z-lc.git`，分支 `main`） |
| **Maven 坐标** | `io.github.yuku123:z-lc:1.0.0`（根 POM `<packaging>pom</packaging>`） |
| **当前版本** | `1.0.0` —— 根 POM 写的是**字面 `<version>1.0.0</version>`，不是 `${revision}`**；`de33a9e`（2026-09-30）把 `com.zifang:z-opc:1.0.0-SNAPSHOT` 幻影父链与 `1.0.0-SNAPSHOT` 一起换掉的 |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘）；其父为地板 `z-boot-dependencies:1.0.20`，并 import 兄弟仓权威表 `z-boot-fleet:1.0.1` |
| **Maven Central（ranged GET 实测）** | 已发布：`z-lc:1.0.0`（pom）与 `z-lc-common` / `z-lc-sdk` / `z-lc-core` / `z-lc-design` / `z-lc-web` 的 `1.0.0` jar 全部 `200`；`z-lc-admin:1.0.0` = `404`（`central` profile 的 `excludeArtifacts` 把它排除，胖 jar 只作本地/部署演练用） |
| **默认端口** | 默认 profile **不设** `server.port`（[`z-lc-admin/src/main/resources/application.yml`](z-lc-admin/src/main/resources/application.yml) 第一行就写了这一格已移除）；`dev` profile 用 `SERVER_PORT`，缺省 **18090** |
| **context-path** | 无。全部路由是绝对路径 `/api/lc/...`（`application.yml` 里那句 `--server.servlet.context-path=/meta` 是从 z-meta 抄来的残留注释，本仓没有 `/meta` 前缀） |
| **运行口径** | Java 8（`maven.compiler.source/target=8` 由 parent 下发）· Spring Boot 2.7.18（地板面值；本仓 `pluginManagement` 刻意把 `spring-boot-maven-plugin` 留在字面 `2.7.12`） |
| **最近更新** | 2026-09-30 |

### ⚠ 关于"本仓必须用 JDK 17 编译"这一条：实测**不成立**

组织里确实有仓（例如 z-webide 用 `String.repeat`、z-util-jdbc 的某些测试）需要 JDK ≥ 11/17 才能编过。
本仓逐条查过，**结论是 Java 8 口径真实可用，没有任何 JDK 17 硬要求**，证据：

- 7 份 POM（根 + 6 模块）里 `<release>`、`maven-enforcer-plugin`、`requireJavaVersion`、
  `maven-toolchains`、抬 Java 级别的 profile —— 全部 **0 命中**；也没有 `.mvn/jvm.config`。
  编译级别只有 parent 下发的 `source/target 8`。
- 本仓自己的产物：`z-lc-common` / `-sdk` / `-core` / `-design` / `-web` / `-admin` 的
  `target/classes` 各抽一支 class 读头 8 字节，major 全是 **52（Java 8）**。
- 上游关键件同为 major 52：`z-llm-core:0.1.7`、`z-llm-api:0.1.7`、`z-script-engine:1.0.1`、
  `z-util-core:1.0.14`（fleet 现钉这一格）、`z-boot-datasource-starter:1.0.21`、
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
| 流程绑定与触发 | `WorkflowBindingController` `/api/lc/workflow-binding`、`WorkflowTriggerDispatcher`、`WfAdapter` | 绑定关系 + 触发派发；远端 z-wf 的审批中心路径由 `z-lc.adapter.wf.base-url` 指 |
| 权限 | `PermissionController` `/api/lc/permission` | 授权/回收（`PermissionService`） |
| 元数据与探活 | `MetaController` `/api/lc/meta`、`HealthController` `/api/lc/health` | `/health` 的 `data.status` 由每个连接池真探一次推出来（`DataSourceHealthProber`），配置不成串就拒起 |
| AI 建模建议 | `AiModelingController` `/api/lc/ai`、`core/ai/AiModelingService` | 走 `io.github.yuku123:z-llm-core` 的 `ChatGatewayService.chat(UnifiedRequest)`；`optional` 依赖，容器里没有该 bean 时退回模板建议 |
| 模型/页面设计框架 | `z-lc-design`：`@LowCodeModel`、`@LowCodeModelService`、`LowCodeModelServiceCollector`、`PageTemplateController` `/api/lc/design` | 注解 + 收集器把类注册成平台模型；knife4j OpenAPI 也在这层开 |
| SPI 扩展点 | `z-lc-sdk` 的 `spi/{form,approve,model,task,sign,workflow}` | `rg 'public interface' z-lc-sdk/src/main/java/com/zifang/z/lc/sdk/spi` 现数 **27** 支接口 + 抽象基类与 RPC 适配器 |
| 外部系统适配 | `z-lc-core/adapter`（12 个类：`CtcAdapter` / `MetaAdapter` / `ScriptAdapter` / `MistAdapter` / `OssAdapter` / `MqAdapter` / `WfAdapter` / `AdapterRegistry` / `JwtRelayInterceptor` …） | 每个 adapter 的地址走 `z-lc.adapter.<name>.base-url`，缺省 `http://localhost:8888`；`MetaAdapter`/`CtcAdapter` 用 Caffeine 2.9.3 做本地缓存 |

`z-lc-web` 的 `META-INF/spring.factories` 注册 `LcAutoConfiguration` 与 `LcModuleDataSource`，
所以宿主只需 embed `z-lc-web` 就能拿到全部 Controller；`z-lc-admin` 只是把这条链真的跑起来的入口。

---

## 🏗️ 项目结构

```
z-lc/
├── pom.xml                # 根聚合 POM：挂 z-boot-parent:1.0.21，版本字面 1.0.0，常开 flatten(oss) 1.5.0
├── z-lc-common/           # 公共层：DTO / 枚举 / 38 支 utils / 域模型（267 main + 270 test java）
├── z-lc-sdk/              # SPI 框架：27 支接口 + 注解 + 抽象基类，纯 API 无业务实现（62 + 55）
├── z-lc-core/             # 引擎：CRUD / fieldtype / adapter / pipeline / event / materialize /
│                          #   deployment / undo / permission / workflow / ai（122 + 103）
├── z-lc-design/           # 设计框架：@LowCodeModel 族、PageTemplate、collector、knife4j（10 + 10）
├── z-lc-web/              # HTTP 层：18 个 Controller 里的 17 个 + 健康探测 + 自动配置（24 + 10）
├── z-lc-admin/            # 独立启动入口（pom 的 <name> 仍叫 z-lc-bootstrap）：3 个类、0 测试、
│                          #   spring-boot repackage 出胖 jar，不进 Maven Central
├── z-lc-admin-ui/         # React 18 + antd 5.29.3 + Vite 的管理台（独立 npm 工程，不在 reactor）
├── _e2e/                  # 端到端与注入自证脚本 + 250 部署演练，见 _e2e/README.md
├── _doc/                  # 项目文档，见文末「文档目录」
├── log/                   # 运行期输出目录（`**/*.log` 被 .gitignore 排除，不是源码）
├── LICENSE                # MIT
└── README.md
```

Maven reactor 里只有 **6 个模块**（根 POM `<modules>`：`z-lc-common`、`z-lc-sdk`、`z-lc-core`、
`z-lc-design`、`z-lc-web`、`z-lc-admin`）。工作区里还有两个**不在 reactor、也没有任何被 git 跟踪的文件**
的目录，别当模块读：

- `z-lc-bootstrap/` —— 只剩 `target/`；那是 `z-lc-admin` 改名前的旧壳（admin 的 `<name>` 与
  `spring.application.name` 至今仍是 `z-lc-bootstrap`）。
- `z-lc-spring-boot-starter/` —— 只剩 `target/`；该 starter 已迁到 `z-boot/z-boot-integration-starters`
  （根 POM 的 `dependencyManagement` 注释里点明了这件事）。

`z-lc-admin` 留在 reactor 是为了享受统一构建，但它 `repackage` 出的胖 jar 是端到端入口件，
`central` profile 用 `excludeArtifacts` 把它挡在发布之外 ⇒ **不会上 Central**（repo1 实测 404）。

---

## 🔧 技术栈（全部取自 POM 实测）

| 层级 | 技术 |
|------|------|
| 语言 / 编译 | Java 8（`source/target 8`，非 `--release`），UTF-8，`-parameters` |
| 框架 | Spring Boot 2.7.18（地板 `z-boot-dependencies:1.0.20` 的 `spring-boot.version`） |
| 打包插件 | `spring-boot-maven-plugin` 本仓 `pluginManagement` 字面留 **2.7.12**（刻意分歧，动它等于换 fat jar loader） |
| 兄弟仓版本权威 | `z-boot-fleet:1.0.1`：`z-util` 一族 **1.0.14**、`z-llm` **0.1.7**、`z-script-engine` **1.0.1** |
| 数据源 | `z-boot-datasource-starter:1.0.21`（含 Druid；本仓不再直引 mybatis-plus）+ `spring-jdbc` |
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

第三方与兄弟仓版本全由 `z-boot-parent:1.0.21` → 地板 `z-boot-dependencies:1.0.20` +
`z-boot-fleet:1.0.1` 供给，模块 POM 里不该再有字面版本钉；构建报找不到 parent 时，
先确认能解析到 `io.github.yuku123:z-boot-parent:1.0.21`（在 repo1，不在磁盘相对路径上）。

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

`rg --no-ignore -o "@RequestMapping\(\"[^\"]*\"\)" --glob '*.java'` 实测得到的类级前缀（18 个 Controller）：

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

`/api/approval-center` **不是**本仓的端点，它是 `WfAdapter` 调用远端 z-wf 的路径（只出现在注释里）。

---

## 🧪 测试

```bash
mvn -o -B clean install          # Java 层全量（surefire）
python3 _e2e/e2e_api_test.py     # 接口层：打真在跑的 server（dev profile 起 18090）
cd z-lc-admin-ui && npm run check # tsc --noEmit + eslint --max-warnings 0 + vitest + vite build
```

用例分布（`rg -c '^[[:space:]]*@Test'` 现数，注释里的 `@Test` 已避开）：
`z-lc-common` 2709、`z-lc-sdk` 525、`z-lc-core` 1338、`z-lc-design` 112、`z-lc-web` 114、`z-lc-admin` 0。
`z-lc-web` 那 114 条里 `LcHttpContractTest` 是真 HTTP 集成测试（自己起 server、用 OS 分配端口）。

注入自证（"闸能不能红"的负控）共 **43 支**：`_e2e/` 22 支后端 + `z-lc-admin-ui/e2e/` 21 支前端；
带互斥锁的那批共用 `z-lc-admin-ui/e2e/_mutlock.py`，两支不能同时在飞。

如实说明跑不过/要注意的：

- `mvn` 全量要能解析到 repo1 上的 `z-boot-parent:1.0.21` 与 fleet 点名的兄弟仓构件，离线且
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

本项目文档统一收口在 `_doc/` 下，以下逐条按 `find _doc -mindepth 1` 的**真实存在**列出：

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档，**目前只有 1 份**：
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md) — 2026-09-06 从
    `idea_workplace/z-opc/z-lc/` 拆仓时写的说明。⚠ 它的"父项目 `../pom.xml`
    （`com.zifang:z-opc:1.0.0-SNAPSHOT`）、单独 clone 无法 Maven build"那两条**已不成立**
    （`8361aea` 换挂 `z-boot-parent:1.0.19`、`c8041e6` 抬到 `1.0.21`，`<relativePath/>` 留空），
    读它当历史，别当现状；现状以本 README 与根 POM 为准。
    规范里常见的 `00-overview.md` / `02-api.md` / `03-db-schema.md` / `05-frontend.md` / `07-roadmap.md`
    在本仓 **都不存在**，没有对应实现或文档就不写链接。
- `002_deploy` / `003_script` / `004_skill` — **槽位不建**：部署 SQL 实际在
  `z-lc-admin/src/main/resources/db/`，脚本实际在 `_e2e/`，本仓没有真 SKILL 定义。
- [`_doc/007_backlog/`](_doc/007_backlog/) — 待办与缺陷覆盖面，如实登记：
  - [`_doc/007_backlog/README.md`](_doc/007_backlog/README.md) — 待办索引与取数命令
  - [`feature001_workflow_binding_fires/TASK.md`](_doc/007_backlog/feature001_workflow_binding_fires/TASK.md) — 缺陷 #61：流程绑定这条链其余四层覆盖面
  - [`feature002_http_status_not_checked/TASK.md`](_doc/007_backlog/feature002_http_status_not_checked/TASK.md) — 缺陷 #62：`isSuccess()` 不含状态码
  - [`feature003_dead_envelope_parses/TASK.md`](_doc/007_backlog/feature003_dead_envelope_parses/TASK.md) — 缺陷 #63：5 处 `fromJson` 结构上必抛
- 私有演进稿 `_doc/001_arch/model-evolution-plan.md` **本机存在但被 `.gitignore`（第 82 行）排除**，
  不在 git 索引里、clone 下来看不到，所以这里不给链接；按规范口径这一层等于**无已提交文档**。

跨文档之外的验证面另见 [`_e2e/README.md`](_e2e/README.md)（端到端续跑清单、四层闸门的读数与坑）。
