# z-lc-suit

z-lc 低代码平台的**独立运行壳**（lead 005 §9.1 suit）：把「报表模板画廊 / 视图渲染 / 发布分享 / AI 一句话生成报表 /
服务状态 / 登录」这一整套报表低代码页面收敛成一个可独立启动的 React 应用，产物由 `z-lc-admin` 在同一 context-path 下托管。

- **npm 包名**：`@yuku123/z-lc-suit`（`private: true`，**不发布 npm**；只随 z-lc 后端一起构建部署）
- **宿主接入**：无（独立壳）。仓内对外出口是 `src/index.js`（页面清单 + 文档页）
- **后端**：`/api/lc/report/**`（由 `z-lc-report-web` 提供），dev proxy 指 `z-lc-admin` :18090；另有 Spring Boot Actuator 探针
- **技术栈**：React 19 + antd 6 + react-router-dom 7 + `@yuku123/z-frontend-render`（通用渲染器）+ vite（**应用层**，基座内联进 bundle）

> ⚠ 本仓同时存在 `z-lc-component`，但它是**另一个包**：目前只承载 zustand ui store 与骨架路由
> （见 `z-lc-component/src/pages-manifest.tsx` 内的说明），**报表域页面代码仍住在本壳 `src/` 下**。
> 因此本文档描述的是**本壳自身的能力**，不是 `z-lc-component` 的能力面。

---

## 一、能力概览

路由前缀 `/z-lc`，菜单与路由都在 `src/pages-manifest.jsx` 里声明，`src/App.jsx` 读数组自己组装。

| 页面 | 路由 | 源文件 | 能力 |
|------|------|--------|------|
| 首页 | `/z-lc/home` | `src/HomePage.jsx` | 欢迎语 + 按 `menuItems` 渲染的快捷入口卡片 |
| 服务状态 | `/z-lc/status` | `src/StatusPage.jsx` + `src/Status.jsx` | 读 actuator 探针显示健康状态 / 构建信息，10 秒轮询 |
| 报表模板 | `/z-lc/report` | `src/report/Home.jsx` | 内置模板画廊，「使用模板创建」→ 落视图页 |
| AI 报表 | `/z-lc/report/ai` | `src/report/AiPage.jsx` | 一句话 → 数据集 → 视图，带执行日志 |
| 报表视图（参数页，不进菜单） | `/z-lc/report/view/:viewId` | `src/report/ViewPage.jsx` | 渲染视图；带 `?bookId=` 时显示「整体发布」并给出分享链接 |
| 分享观看页（参数页，不进菜单） | `/z-lc/report/share/:publishId` | `src/report/SharePage.jsx` | 匿名读发布快照并渲染第一个 PAGE 节点 |
| 登录 | `/z-lc/login` | `src/LoginPage.jsx` | mock 登录（**直接在 `App.jsx` 里注册路由，不在 `routes` 数组**） |
| 组件文档 | `/z-lc/docs` | `src/pages/ComponentDoc.jsx` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

## 二、导出面

```js
// 仓内出口（src/index.js）
import {menuItems, routes, appMeta, ComponentDocPage} from './src/index.js'

// 单个页面（直接引源文件）
import HomePage from './src/HomePage.jsx'
import ReportHome from './src/report/Home.jsx'
```

`src/index.js` 是本壳**唯一的 barrel**：把 `src/pages-manifest.jsx` 的清单整份转发，再具名再导出 `ComponentDocPage`
（与其它仓「文档页统一叫 `ComponentDocPage`」的口径一致，宿主/主壳可以按同一个名字取）。

## 三、API 端点

下表路径是 `src/report/api.js`、`src/report/AiPage.jsx`、`src/Status.jsx` 里**实际调用**的路径。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/lc/report/templates` | 内置模板清单 |
| POST | `/api/lc/report/templates/{tplId}/apply` | 应用模板，返回 `{bookId, pageId, viewId, datasetId, sourceId}` |
| GET | `/api/lc/report/render/view/{viewId}` | 整页渲染（ViewSchema → `{viewId, title, version, layout, options}`） |
| POST | `/api/lc/report/render/widget` | 单图表渲染（`renderWidget`，**已实现但页面未调用**） |
| GET | `/api/lc/report/book/{bookId}/tree` | 书节点树（`bookTree`，**已实现但页面未调用**） |
| POST | `/api/lc/report/book/{bookId}/publish` | 整体发布，返回 `PublicationSnapshot`（含 `publishId` / `shareToken`） |
| GET | `/api/lc/report/book/publication/{publishId}?token=…` | 读发布快照（观看侧，token 必带） |
| POST | `/api/lc/report/ai/dataset` | NL → 数据集定义（body `{requirement, sourceId}`） |
| POST | `/api/lc/report/ai/view` | NL → 视图页（body `{requirement}`） |
| GET | `/api/lc/report/datasource` | 已注册数据源清单（`AiPage` 内直接 `fetch`） |
| GET | `/actuator/health` | 健康探针（`Status` 内直接 `fetch`） |
| GET | `/actuator/info` | 构建元信息（`Status` 内直接 `fetch`） |
| GET | `/actuator/mappings` | HTTP 路由清单（`Status` 页仅提供跳转链接） |
| GET | `/actuator/env` | 环境变量与配置属性（`Status` 页仅提供跳转链接） |

## 四、页面说明

### 4.1 首页

登录后着陆页。顶部欢迎语读 `localStorage.userInfo`，下方按 `menuItems`（剔除自身）渲染快捷入口卡片，点击即 `navigate(m.key)`。

### 4.2 服务状态

读 `/actuator/health` + `/actuator/info`，展示健康状态、构建版本、构建时间，并列出 actuator 常用端点跳转。
**后端未起时优雅降级**（loading skeleton + warn，不抛红）；每 10 秒轮询一次。

### 4.3 报表模板

内置模板画廊。点「使用模板创建」调 `applyTemplate`，成功后跳 `/z-lc/report/view/{viewId}?bookId={bookId}`；
底部提供「AI 一句话生成 →」入口。

### 4.4 报表视图

调 `renderView(viewId)` 拿到渲染数据交给 `@yuku123/z-frontend-render` 的 `UniversalRenderer` 渲染。
URL 带 `?bookId=` 时额外显示「整体发布」按钮，发布成功后给出分享链接
（`{origin}/z-lc/report/share/{publishId}?token={shareToken}`）。

### 4.5 分享观看页

匿名入口。读发布快照 → 过滤出 `nodeType === 'PAGE'` 且有 `viewId` 的节点 → 渲染第一个 PAGE 节点。
快照与编辑态隔离：这里看到的是**发布时刻**的书树（整书发布语义）。

### 4.6 AI 报表

两步走：先选数据源（清单来自 `/api/lc/report/datasource`，拉取失败不阻塞手输 `sourceId`）→ `aiDataset` 生成数据集 →
`aiView` 生成视图 → 跳视图渲染页。页面下方 `<pre>` 显示执行日志。后端 LLM 未配置时后端返回配置指引错误，前端原样透出。

### 4.7 登录

**mock 登录**：硬编码 `admin / 123456`，成功后写 `localStorage.token`（值形如 `mock-admin`）与
`localStorage.userInfo`，再整页跳 `/z-lc/home`。 CTC/SSO 接入后应换共享 `LoginPage` 的 `loginApi`。

### 4.8 组件文档

本文件。内容以 `?raw` 在编译期内联，零额外请求；左侧 240px TOC 随滚动联动。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| dev proxy `/api/lc` | `http://localhost:18090` | 报表 / 低代码 API（必须排在通配 `/api` 之前） |
| dev proxy `/api` | `http://localhost:8888` | 其余后端 API |
| dev server port | `3010` | `vite.config.js` 的 `server.port` |
| `LOCAL_SIBLINGS=1` | 关 | 开启后把 `@yuku123/z-lc-component` 别名到 `../z-lc-component/src`（本壳当前未 import 该包） |
| token 存储键 | `token` | `localStorage`，与基座默认一致 |
| userInfo 存储键 | `userInfo` | `localStorage`，JSON 串；解析失败时降级为 `{name: raw}` |
| `appTitle` | `z-lc 低代码平台` | `App.jsx` 里 `AppLayout` 的标题 |

## 六、已知限制

- **登录是硬编码 mock**（`admin / 123456`），没有真实鉴权；`App.jsx` 的 `ProtectedShell` 只判断 `localStorage.token` 是否存在。
- **`src/report/api.js` 的 `request` 不带任何鉴权头**（只发 `Content-Type`），也没有统一解包；
  后端异常响应 `{status, error, message}` 里取 `message`/`error` 作为提示，缺省回落成 `HTTP {status}`。
- `renderWidget`（单图表渲染）与 `bookTree`（书节点树）**已实现但没有任何页面调用**。
- **分享 token 走 query 明文**（`?token=…`），会进浏览器历史与 Referer。
- AI 生成依赖后端 `z.report.llm.*` 配置；未配置时后端返回的是**配置指引错误**而不是空结果，前端不做兜底。
- `Status` 页用相对路径 `fetch('/actuator/health')`，**要求前后端同域托管**；分域部署会 404（此时降级为 UNKNOWN）。
- `Status` 页 10 秒轮询一次 actuator，页面常开时会持续打后端。
- 本壳没有 `z-lc-component` 的页面能力（工作台 / 流水线 / 权限等仍在 component 包侧，页面代码未在本壳）。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.0（未发布） | 新增组件文档页 `ComponentDocPage`（lead 008 §7）：`src/DOC.md` + 基座 `ComponentDoc` 薄包装 + 菜单末项 |
| 0.1.0 | 首版：报表模板 / 视图渲染 / 发布分享 / AI 生成 / 服务状态页面集 + manifest + mock 登录 |
