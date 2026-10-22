# z-lc-component

z-lc 低代码平台（应用建模 / 数据集导入 / 运行时 CRUD / 视图工作区 / 后台治理）的**可复用前端组件包**。主壳
「系统工作台」按域接进来后，z-lc 域的所有页面都在这个包里。

- **npm 包名**：`@yuku123/z-lc-component`
- **宿主接入**：`import LcApp from '@yuku123/z-lc-component'`（默认导出 = 一整张 `<Routes>` 表）
  或 `import { ComponentDocPage } from '@yuku123/z-lc-component'`（主壳取文档页的统一名，lead 008 §7）
- **挂载前缀**：宿主挂 `/lc/*`（z-team 研发工作台 `main.jsx` 的 `<Route path="/lc/*">`），包内路由全是**相对路径**
- **后端**：`/api/lc/**`（z-lc 后端），租户默认 `default`，可用 `tenantCode` 覆盖

> ⚠ 本仓与 `z-lc-suit` 是**两个不同的包**：suit 是报表低代码那一套（模板画廊 / 视图渲染 / 发布分享 / AI 报表），
> `private: true` 不发布 npm，是独立壳。本包承载的是**应用建模 + 运行时工作区 + 后台治理**这一整套，
> 两者的页面与 API 都不重叠 —— 本文件描述的是**本包**的能力面。

---

## 一、能力概览

包内路由挂在宿主给的 `/lc/*` 前缀下，下表路径是**相对 `/lc` 的相对段**。

| 页面 | 相对路由 | 源文件 | 能力 |
|------|----------|--------|------|
| 应用列表 | `apps` | `views/apps/AppListPage.tsx` | 应用 CRUD、发布 / 归档、搜索 |
| 应用总览 | `:appCode` | `views/apps/AppOverviewPage.tsx` | 单应用概览：实体数、字段数、进入工作区 |
| 模型设计器（选应用） | `designer` | `views/designer/DesignerPage.tsx` | 模型设计器入口：列出应用 → 进入设计 |
| 模型设计器（设计应用） | `designer/:appCode` | `views/designer/DesignerPage.tsx` | 实体 / 字段设计（字段类型、必填、字典、排序）、落库 provision |
| 数据集导入 | `db-import` | `views/designer/DbImportPage.tsx` | 扫库表 → 预览 → 单表 / 批量导入成实体 |
| 字典管理 | `admin/dicts` | `views/admin/DictsPage.tsx` | 字典与字典项 CRUD、排序 |
| 实体关系 | `admin/relations` | `views/admin/RelationsPage.tsx` | 实体间关系 CRUD（关联 / 引用） |
| 视图配置 | `admin/views` | `views/admin/ViewConfigsPage.tsx` | 视图配置 CRUD（列表 / 看板 / 日历 / 图表 / 透视 / 画廊） |
| 流水线配置 | `admin/pipelines` | `views/admin/PipelinesPage.tsx` | 流水线阶段 CRUD、启停、按触发事件编排 |
| 工作流绑定 | `admin/workflows` | `views/admin/WorkflowsPage.tsx` | 工作流与实体的绑定 CRUD、触发记录抽屉 |
| 权限授予 | `admin/permissions` | `views/admin/PermissionsPage.tsx` | 角色 × 实体 × 权限点 的授予 / 撤销 |
| 部署管理 | `admin/deployments` | `views/admin/DeploymentsPage.tsx` | 部署记录创建 / 详情抽屉、部署类型词表 |
| AI 建模 | `admin/ai` | `views/admin/AiModelingPage.tsx` | AI 建议建模：模型建议、表单生成、数据分析、产品化 |
| 仪表盘 | `:appCode/dashboard`（可带 `:dashboardId`） | `views/workspace/DashboardPage.tsx` | 应用级仪表盘：视图配置、数据集管理 |
| 数据工作区 | `:appCode/:entityCode/:viewType`（可带 `:recordId`） | `views/workspace/WorkspaceViewPage.tsx` | 七种视图的记录 CRUD：表格 / 看板 / 画廊 / 日历 / 图表 / 透视 / 明细 |
| 组件文档 | `docs` | `pages/ComponentDoc.tsx` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

`viewType` 取值（`api/types.ts` 的 `VIEW_TYPES`）：
`LIST` / `FORM` / `DETAIL` / `KANBAN` / `GALLERY` / `CALENDAR` / `CHART` / `PIVOT`。

## 二、导出面

```ts
// 主入口（默认导出 = 整张路由表）
import LcApp from '@yuku123/z-lc-component';

// 具名导出
import { ComponentDocPage } from '@yuku123/z-lc-component';   // 主壳按统一名取文档页（lead 008 §7）

// /pages 子路径（宿主组装面）
import { routes, appMeta } from '@yuku123/z-lc-component/pages';

// /store 子路径（zustand ui store）
import { useUiStore } from '@yuku123/z-lc-component/store';
```

- `ComponentDocPage` 是**具名再导出**（`export { default as ComponentDocPage } from './pages/ComponentDoc'`），
  与默认导出 `LcRoutes` **并存** —— 宿主既可以整包挂路由，也可以单独取文档页。
- `routes` / `appMeta` 目前只有一条骨架路由 `/z-lc/home`（`src/pages-manifest.tsx`），
  主壳工作台**不用**这张表（工作台按宿主前缀 `/lc/*` 挂，路由由本包自己的 `<Routes>` 管）。

## 三、API 端点

前缀 `/api/lc`（`api/client.ts` 的 `API_BASE`），下表路径是 `src/api/**` 里**实际调用**的路径。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/app/list` | 应用清单 |
| POST | `/app/create` | 建应用 |
| POST | `/app/update` | 改应用 |
| POST | `/app/delete` | 删应用 |
| GET | `/app/detail` | 应用详情 |
| POST | `/app/publish` | 发布应用 |
| POST | `/app/archive` | 归档应用 |
| GET | `/app/schema` | 应用 schema（实体 + 字段） |
| POST | `/app/event` | 记录变更事件 |
| GET | `/app/event/last` | 最近一次变更事件 |
| POST | `/admin/app/create` | 后台建应用 |
| GET / PUT / DELETE | `/admin/app` | 后台应用详情 / 改 / 删 |
| POST | `/admin/app/entity/create` | 建实体 |
| GET | `/admin/app/entity/list` | 实体清单 |
| GET / PUT / DELETE | `/admin/entity` | 实体详情 / 改 / 删 |
| POST | `/admin/entity/provision`、`/admin/app/entity/provision` | 实体落库（单实体） |
| POST | `/admin/app/provision-all` | 应用内全部实体落库 |
| GET | `/admin/db/tables` | 扫库表（可按 prefix / schema 过滤） |
| GET | `/admin/db/table` | 单表结构 |
| POST | `/admin/db/table/import` | 单表导入成实体 |
| POST | `/admin/db/tables/batch-import` | 批量导入 |
| POST | `/runtime/list` | 记录分页查询（过滤 / 条件 / 排序，size 上限 200） |
| POST | `/runtime/get`、`/runtime/create`、`/runtime/update`、`/runtime/delete` | 记录读写 |
| POST | `/runtime/delete-batch` | 批量删 |
| POST | `/runtime/aggregate` | 聚合（透视 / 图表数据） |
| POST | `/runtime/shape` | 实体形状（字段形状） |
| POST | `/runtime/import/preview`、`/runtime/import/commit` | 记录导入预览 / 提交 |
| GET | `/dict/list`、`/dict/detail` | 字典清单 / 详情 |
| POST | `/dict/create`、`/dict/update`、`/dict/delete` | 字典维护 |
| GET | `/dict/items` | 字典项清单 |
| POST | `/dict/items/create`、`/dict/items/update`、`/dict/items/delete` | 字典项维护 |
| GET | `/view-config/list` | 视图配置清单 |
| POST | `/view-config/create`、`/view-config/update`、`/view-config/delete` | 视图配置维护 |
| GET | `/relation/list` | 实体关系清单 |
| POST | `/relation/create`、`/relation/update`、`/relation/delete` | 实体关系维护 |
| GET | `/pipeline-config/list` | 流水线配置清单 |
| POST | `/pipeline-config/create`、`/pipeline-config/update`、`/pipeline-config/delete` | 流水线配置维护 |
| POST | `/pipeline-config/toggle` | 流水线启停 |
| GET | `/workflow-binding/list` | 工作流绑定清单 |
| POST | `/workflow-binding/create`、`/workflow-binding/update`、`/workflow-binding/delete` | 工作流绑定维护 |
| GET | `/workflow-binding/vocabulary`、`/workflow-binding/fires` | 工作流词表 / 触发记录 |
| GET | `/permission/list`、`/permission/check` | 权限清单 / 鉴权判定 |
| POST | `/permission/grant`、`/permission/revoke` | 授予 / 撤销权限 |
| GET | `/deployment/list`、`/deployment/detail` | 部署清单 / 详情 |
| POST | `/deployment/create` | 建部署 |
| GET | `/deployment/vocabulary` | 部署类型词表 |
| POST | `/ai/model-suggestion` | AI 建模建议 |
| POST | `/ai/apply-suggestion` | 应用建模建议（落库） |
| POST | `/ai/generate-form` | 生成表单 |
| POST | `/ai/analyze-data` | 数据分析 |
| POST | `/ai/productize` | 产品化 |
| GET | `/ai/mcp-tools` | AI 可用 MCP 工具清单 |
| POST | `/app/materialize` | 物化（应用落地） |
| GET | `/app/materialize/status`、`/app/materialize/list`、`/app/materialize/diff` | 物化状态 / 清单 / 差异 |
| POST | `/undo/undo`、`/undo/redo` | 撤销 / 重做 |
| GET | `/undo/history` | 撤销历史 |
| GET | `/meta/bundle` | 工作区一次性元数据打包（缺失时降级到逐个端点） |
| GET | `/meta/field-types` | 字段类型描述（未部署时回落内置表） |
| GET | `/health`、`/health/adapters` | 健康探针 / 适配器清单 |
| POST | `/health/meta-ping`、`/health/ctc-ping` | meta / ctc 连通性探测 |

## 四、页面说明

### 4.1 应用列表 / 应用总览

应用列表支持建、改、删、发布、归档；总览页展示实体数、字段数，并给出进入工作区的入口。
数据源是 `/app/*` 系列。

### 4.2 模型设计器

选应用 → 编辑实体与字段（字段类型 / 必填 / 字典来源 / 顺序），改完走 **provision** 落成物理表
（`/admin/app/entity/provision`、`/admin/app/provision-all`）。字段类型清单来自基座
`@yuku123/z-frontend-render/fields`，字典选项来自 `/dict/list`。

### 4.3 数据集导入

扫库表（`/admin/db/tables`）→ 预览表结构 → 单表或批量导入成实体（`/admin/db/table/import`、
`/admin/db/tables/batch-import`）。导入结果直接可进设计器继续改字段。

### 4.4 后台治理（`admin/*`）

字典 / 关系 / 视图配置 / 流水线 / 工作流 / 权限 / 部署 / AI 建模八张管理页，共用同一套
「应用 + 实体」作用域选择器（`views/admin/_scope.tsx`）与列表页脚手架（`views/admin/_shared.tsx`）。

- **流水线**：阶段（`PipelineStage`）+ 触发事件（`TriggerEvent`）编排，可单独启停。
- **工作流**：工作流与实体的绑定关系 CRUD，附带触发记录抽屉（`/workflow-binding/fires`）。
- **权限**：权限点词表固定为 `VIEW` / `CREATE` / `UPDATE` / `DELETE` / `EXPORT`
  （`api/permission.ts` 的 `PERMISSION_KEYS`），后端在 `roleCode` 与 `entityCode` 之间**优先 roleCode**。
- **AI 建模**：给建议 → 应用建议 → 生成表单 / 分析数据 / 产品化。

### 4.5 数据工作区

`/:appCode/:entityCode/:viewType` 进入。`WorkspaceViewPage` 按 `viewType` 分发到
`GridView` / `KanbanView` / `GalleryView` / `CalendarView` / `ChartView` / `PivotView` / `DetailView`，
记录增删改走 `/runtime/*`；`GridView` 另带列管理抽屉、筛选条、CSV 导出与批量删除。
元数据优先走 `/meta/bundle` 一次拉齐，缺失时降级到 `/app/schema` + `/dict/list` +
`/view-config/list` + `/relation/list` 逐个取（`api/meta.ts`）。

### 4.6 组件文档

本文件。内容以 `?raw` 在编译期内联进 bundle（零额外请求）；渲染用基座
`@yuku123/z-frontend-common/ui` 的 `ComponentDoc`（左 240px TOC + 右正文两栏）。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| API 前缀 | `/api/lc` | `api/client.ts` 的 `API_BASE`，dev 下由宿主 proxy 到 z-lc 后端 |
| 默认租户 | `default` | `DEFAULT_TENANT_CODE`，可用 `tenantCode` 覆盖 |
| 变更归属人 | 浏览器持久化 id | `api/actor.ts`：`zlc:actor` → localStorage → `web-<uuid>`；宿主可用 `setActor()` 覆盖 |
| vite alias | `@/lc` / `@` → `src` | 仓内没有 `src/lc/` 目录，别名必须指向 `src` 根 |
| 构建 external | 基座 / react / antd / tanstack 等 | 基座与宿主共享同一份 antd，禁止打进本包 dist |

## 六、已知限制

- **后端多以 HTTP 200 + `success:false` 报业务错**，前端必须按 `code`/`message` 分支，不能只看 `status`
  （`api/client.ts` 的 `ApiError` 口径）。
- `/meta/field-types`、`/meta/bundle` 等端点**可能未部署**，调用方必须把失败当「未知」并回落内置表。
- `permission/check` 的后端口径是 **roleCode 优先于 entityCode**，两者都不给时按应用级判。
- **本包没有鉴权**：z-lc 直连模式，只做变更归属（actor），不做授权；真实用户 id 应由网关经 `X-User-Code` 传入。
- `src/pages-manifest.tsx` 的 `routes` 只有骨架路由 `/z-lc/home`，**不是**本页表格的路由表 ——
  真实路由在 `src/index.tsx` 的 `LcRoutes` 里（相对路径）。
- `z-lc-suit`（报表域）里的页面、`DOC.md` 与本包无关，两边各自维护。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.2（本次改动，版本号未抬） | 落地 lead 008 §7：新增本文件 + `src/pages/ComponentDoc.tsx` 薄包装 + `LcRoutes` 里 `docs` 相对路由（放在 catch-all `*` 之前）+ 主入口具名再导出 `ComponentDocPage` |
| 0.1.2 | 应用 / 建模 / 数据集导入 / 后台治理 / 工作区视图全套页面（TS 重写） |
