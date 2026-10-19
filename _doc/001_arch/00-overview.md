# 项目总览

## 1. 项目简介

z-lc 是 z-opc-foundation 的**低代码平台（Low-Code）**：把「建表 → 字段类型 → 列表/表单 → 权限 → 流程 → 部署」
这条每个业务应用都要重抄一遍的链，收进元数据表 + 运行时引擎。应用与实体定义落库后，由 `z-lc-core` 的
动态 SQL 与字段类型注册表把 CRUD、聚合、导入导出、视图配置、物化、撤销、流水线、流程绑定这些动作
真的执行出来。

**三种扩展口：**

- `z-lc-sdk` —— SPI，把表单 / 审批 / 任务 / 印章 / 流程的业务钩子留给宿主实现。
- `z-lc-design` —— `@LowCodeModel` / `@LowCodeModelService` 注解 + 收集器，把「一个 Java 类」变成
  「平台上可配的一个模型」。
- `z-lc-report-*` —— 报表能力域（数据源 / 数据集 / 渲染 / 报表本 / AI 解读），2026-10-11 自 z-report 仓收编。

**Maven 坐标：** `io.github.yuku123:z-lc:1.0.0`（根 POM `packaging=pom`），
父项目 `io.github.yuku123:z-boot-parent:1.1.0`（`<relativePath/>` 留空）。

**技术栈：** Java 8 + Spring Boot 2.7.18 + Maven 多模块（13 个 reactor 模块）；
前端两套 —— `_frontend/z-lc-suit`（React 19 + antd 6）与 `z-lc-admin-ui`（React 18 + antd 5 + vitest）。

## 2. 模块架构

```
z-lc/
├── pom.xml                      # 聚合 POM：父 z-boot-parent:1.1.0，13 个 reactor 模块
├── z-lc-common/                 # 公共层（258 文件 / 19561 行，本仓最大）：注解 / aspect / bpmn / cache /
│                                #   config / dto / enums / exception / param / permission / seal / task / utils
├── z-lc-sdk/                    # 业务钩子 SPI + 抽象基类 + 收集器（62 文件）
│                                #   spi/{form,approve,task,workflow,model,sign} + adapter + collector + annotation
├── z-lc-core/                   # 运行时引擎（122 文件 / 16407 行）
│                                #   fieldtype（17 个文件）/ schema / mapper / importer / viewconfig /
│                                #   pipeline / relation / workflow / materialize / undo / deployment / permission / ai
├── z-lc-design/                 # 建模注解与收集器：@LowCodeModel / @LowCodeModelService + 自动装配（10 文件）
├── z-lc-web/                    # REST 层（24 文件）：17 个 Controller + 1 个全局异常处理器
│                                #   路由统一在 /api/lc/* 下；controller/materialize/ 单独放物化端点
├── z-lc-report-common/          # 报表契约：DataSourceDef / DatasetSchema / WidgetSpec / ViewSchema（20 文件）
├── z-lc-report-datasource/      # 报表数据源：JDBC / CSV / 内存 + 类型映射与取值推断（8 文件）
├── z-lc-report-dataset/         # 报表数据集：过滤、聚合、表达式解析、表来源解析（11 文件）
├── z-lc-report-render/          # 报表渲染：ECharts option 构造器（KPI / 柱 / 饼 / 线 / 表格 / 原始）（9 文件）
├── z-lc-report-book/            # 报表本：BookService + 内存实现 + 发布快照（3 文件）
├── z-lc-report-ai/              # 报表 AI 解读：OpenAI 兼容 LLM 客户端 + AiReportService（5 文件）
├── z-lc-report-web/             # 报表 REST：7 个 Controller + ApiErrorAdvice（11 文件）
├── z-lc-admin/                  # standalone 启动器 ZLcBootstrapApplication（3 文件）+ H2 schema
├── z-lc-admin-ui/               # 独立前端（React 18 + antd 5 + vitest，仓根下非 Maven 模块）
├── _frontend/
│   ├── z-lc-suit/               # 管理台壳（React 19 + antd 6 + zustand + @yuku123/z-frontend-common）
│   └── z-lc-component/          # 页面层组件（api / components / hooks / layouts / store）
├── _e2e/                       # 端到端门禁脚本（25 个 Python 脚本 + 2 个 Shell），含 14 个变异注入 guard
├── _doc/                       # 文档目录：001_arch / 007_backlog / 008_troubleshooting
└── LICENSE
```

## 3. 核心功能

### 3.1 运行时通用 CRUD（`z-lc-core` + `RuntimeCrudController`）

`/api/lc/runtime` 下提供 `list` / `get` / `create` / `update` / `delete` / `delete-batch` /
`aggregate` / `shape` / `import-preview` / `import-commit`，底层落到 `DynamicSqlBuilder` 动态拼 SQL。
实体定义在元数据表里，运行时按定义生成查询与写入，不需要每张表写一份 Mapper。

### 3.2 字段类型注册表（`z-lc-core/fieldtype`）

17 个文件：14 支 `*TypeHandler` 定义（其中 `AbstractFieldTypeHandler` / `AbstractDateTimeTypeHandler`
是抽象基类）、`FieldTypeRegistry` 注册表、`CellValueType`、`ValidationError`。
Int / Long / Decimal / Date / DateTime / Boolean / String / Text / Json / Ref 等类型的读写与校验都走这里。

### 3.3 建模与库表管理

- `SchemaAdminController`（`/api/lc/admin`）：应用、实体、字段定义。
- `DbTableController`（`/api/lc/admin/db`）：物理表与列的校对。
- `z-lc-design`：`@LowCodeModel` / `@LowCodeModelService` 标注的类由 `LowCodeModelServiceCollector` 收集，
  一个 Java 类即可成为平台上可配的一个模型；`PageTemplate` / `PageTemplateController` 管页面模板。

### 3.4 流程与流水线

`z-lc-core/pipeline`（配置 + 处理器）、`z-lc-core/workflow`、`z-lc-core/relation` 三块构成可配流程：
`PipelineConfigController`（`/api/lc/pipeline-config`）、`WorkflowBindingController`（`/api/lc/workflow-binding`）、
`RelationController`（`/api/lc/relation`）。`z-lc-sdk` 的 `spi/workflow`、`spi/approve`、`spi/form`、
`spi/task`、`spi/model`、`spi/sign` 六个子包是宿主实现这些钩子的接口。

### 3.5 撤销 / 物化 / 部署 / 视图配置

- 撤销：`UndoController`（`/api/lc/undo`）+ `z-lc-core/undo`。
- 物化：`MaterializationController`（`/api/lc/app/materialize`）+ `z-lc-core/materialize`。
- 部署：`DeploymentController`（`/api/lc/deployment`）+ `z-lc-core/deployment`。
- 视图配置：`ViewConfigController`（`/api/lc/view-config`）+ `z-lc-core/viewconfig`。
- 权限 / 字典 / 事件 / AI 建模：各自有 `PermissionController`、`DictAdminController`、`EventController`、
  `AiModelingController`。

### 3.6 报表能力域（`z-lc-report-*`，7 个模块）

数据源（JDBC / CSV / 内存）→ 数据集（过滤 / 聚合 / 表达式）→ 渲染（ECharts option）→ 报表本（发布快照）
→ AI 解读（OpenAI 兼容客户端）。契约在 `z-lc-report-common`：
`DataSourceDef` / `DatasetSchema` / `WidgetSpec` / `ViewSchema` / `ComputedField` 等 20 个类。

### 3.7 端到端门禁（`_e2e/`）

25 个 Python 脚本 + 2 个 Shell，覆盖接口链路与 22 个变异注入 guard（重复导入、字段编码、部署、权限、
流程绑定、连接泄漏、字符序、健康诚实等），用于验证「不该放行的路径确实不放行」。

## 4. 对外集成 / 上下游

**对外 HTTP 边界：** 全部路由是绝对路径 `/api/lc/*`（无 context-path 前缀）。
`z-lc-web` 17 个 Controller 的前缀实测为：`/api/lc/runtime`、`/api/lc/admin`、`/api/lc/admin/db`、
`/api/lc/app`、`/api/lc/app/materialize`、`/api/lc/deployment`、`/api/lc/dict`、`/api/lc/health`、
`/api/lc/meta`、`/api/lc/permission`、`/api/lc/pipeline-config`、`/api/lc/relation`、`/api/lc/undo`、
`/api/lc/view-config`、`/api/lc/workflow-binding`、`/api/lc/ai`。报表侧另有 `z-lc-report-web` 的 7 个 Controller。

**默认端口：** 根 `application.yml` 不设 `server.port`；`dev` profile 用 `SERVER_PORT`，缺省 **`18090`**
（`z-lc-admin/src/main/resources/application-dev.yml`）。

**数据库：** `z-lc-admin/src/main/resources/db/schema-h2.sql` 提供 H2 建表脚本（standalone 走 H2、零外部依赖）；
生产接入由宿主应用的数据源配置决定。

**上游依赖：** 父链 `z-boot-parent` → 地板 `z-boot-dependencies` + 兄弟仓权威表 `z-boot-fleet`；
`spring-boot-starter-web`、`mybatis-plus` 等第三方件由父链下发。

**下游消费方：** 宿主应用引 `z-lc-sdk` 实现业务钩子、配合 `z-lc-core` 跑运行时；
`z-lc-admin` 作为 standalone 宿主供端到端验证与本地演练。

## 5. 技术特点

1. **元数据驱动而非代码驱动：** 实体与字段定义落在元数据表，`DynamicSqlBuilder` 在运行时按定义生成
   SQL —— 新增一张表不需要新增一个 Mapper 类。
2. **SPI 与注解两套扩展口分工明确：** `z-lc-sdk` 的 SPI 管「宿主要在流程节点里插入自己的业务动作」，
   `z-lc-design` 的注解管「把一个类登记成平台上可配的模型」，两者不互相侵入。
3. **Java 8 是硬口径且有实测证据：** 本仓产物 class 文件头 major 全是 52，parent 下发的是
   `source/target 8` 而**不是** `--release 8` —— 在 JDK 17 上编也能过，但会静默收下 Java 9+ API。
4. **standalone 与生产两条路并存：** `z-lc-admin` 用 H2 + `dev` profile 缺省 `18090` 跑通全链路，
   生产集成仍走宿主应用；`z-lc-admin` 已在根 POM 恢复进 reactor（原为排除）。
5. **门禁靠变异注入而不是只跑通：** `_e2e/` 的 22 个 guard 各自对应一条「必须被拒」的路径，
   断言的是错误行为被拦住，不只是正确行为能通过。
6. **报表域整块收编而非半并入：** `z-lc-report-*` 7 个模块自带契约层（`z-lc-report-common`）、
   自己的数据源与渲染实现，starter / bootstrap 不迁 —— `z-lc-admin` 即宿主。
7. **前端两套并存各有分工：** `_frontend/z-lc-suit` 是 React 19 + antd 6 的新壳层，
   `z-lc-admin-ui` 是 React 18 + antd 5 的 standalone 管理台（含 vitest 用例与 `check` 串联脚本）。

## 6. 目录结构

本仓 Java 包根为 `com.zifang.z.lc`，按 Maven 模块切分：

```
com.zifang.z.lc
├── common/           # z-lc-common：annotation / aspect / apppackage / bpmn / cache / config /
│                     #   constance(s) / context / dto / enums / exception / param / permission /
│                     #   seal / task / utils / validation / workflow
├── sdk/              # z-lc-sdk
│                     #   spi/{form,approve,task,task.adapter,workflow,model,sign}
│                     #   + adapter / annotation / collector / context / define / dto / reflect
├── core/             # z-lc-core
│                     #   fieldtype / schema / importer / viewconfig / pipeline / relation / workflow /
│                     #   materialize / undo / deployment / permission / dict / event / executor /
│                     #   app / datasource(+mysql) / lifecycle / deleter / domain / ai
├── mapper/           # z-lc-core 的 MyBatis Mapper：deployment / dict / event / executor / permission /
│                     #   pipeline / relation / undo / viewconfig / workflow（10 个子包）
├── design/           # z-lc-design：annotation（@LowCodeModel / @LowCodeModelService）+ 收集器
├── web/              # z-lc-web：controller/(+materialize) / config / exception / health / support
├── report/           # z-lc-report-*：common 契约 / datasource / dataset / render / book / ai / web
└── bootstrap/        # z-lc-admin：ZLcBootstrapApplication
```

**规模（`src/main/java` 实测）：** common 258 文件 / 19561 行，core 122 文件 / 16407 行，
sdk 62 文件 / 4333 行，web 24 文件 / 3065 行，report-common 20 文件 / 922 行，
report-dataset 11 文件 / 1620 行，report-web 11 文件 / 946 行，design 10 文件 / 805 行，
report-datasource 8 文件 / 747 行，report-render 9 文件 / 423 行，report-ai 5 文件 / 391 行，
report-book 3 文件 / 235 行，admin 3 文件 / 193 行。
