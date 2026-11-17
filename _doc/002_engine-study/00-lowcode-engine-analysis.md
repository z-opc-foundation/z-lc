# 低代码引擎完整梳理 & z-lc 复刻规划

> 研究对象：`/Users/zifang/workplace/idea_workplace/mk`（React 17 低代码引擎，5 个子项目）
> 目标项目：`z-lc`（Java 17 / Spring Boot 3.3.13 + React 前端，低代码平台）
> 本文目的：先把参考引擎完全梳理清楚，再逐板块判断「学什么、怎么落到 z-lc、优先级」。
> **重要：复刻只移植设计思想、数据结构与算法，不复制任何品牌、包名、内网地址、作者信息。剥离清单见第 6 节。**

> **补充发现（2026-11-17）：组织内已有自研渲染包 `z-render`（npm `@yuku123/z-frontend-render` 1.1.0），
> 即 z-lc 三套前端正在消费的 `@yuku123/z-frontend-render`。因此 P2「渲染内核」策略由「新建」调整为「在自研包上扩展」。
> 详见第 8 节。**

---

## 0. 一页纸结论（TL;DR）

**参考引擎是什么**：一套「Schema 即契约」的 React 低代码引擎。核心是 **一份 Schema 数据结构 + 三个协作角色**：

- **渲染器**：把 Schema 递归解析成页面（运行时）
- **设计器**：可视化产出/编辑 Schema（设计态），基于第三方内核 `mk-base-editor`
- **物料库**：提供标准化组件单元（view + 属性面板 + 事件/变量声明）
- **逻辑引擎**：把流程图的「设计态图」编译成「执行态链表」，运行时驱动执行

**z-lc 现在是什么**：以「DB 驱动的 CRUD/建模后端引擎」为主体 + 已收编的 BI 报表全链 + 刚起头的 V2 页面编排（坐标表单）+ V3 虚拟接口。**重心在后端运行时与元数据，可视化搭建前端很薄，渲染器与字段物料协议依赖外部包 `@yuku123/z-frontend-render`。**

**最大差距（一句话）**：z-lc 有「引擎的后端」，缺「引擎的中枢契约与前端三件套」——统一富 Schema 协议、自研渲染内核、物料注册协议、可视化逻辑编排、拖拽画布。

**建议复刻顺序（越靠前越是地基，先做才有复用价值）**：

| 优先级 | 板块 | 一句话 | 落地到 z-lc 的位置 |
|---|---|---|---|
| **P0** | 统一 Schema 协议 | 把散在 3 处的 schema 收敛为「扁平节点表 + 树形结构 + 逻辑流」单一契约 | 新建 `z-lc-schema`（纯 Java 契约 + 校验 + 编译） |
| **P1** | 逻辑引擎（运行态） | 设计态图 → 执行态链表编译器 + Generator 执行器 | `z-lc-core/logic`，复用已有 z-script/vapi |
| **P2** | 渲染内核 + 插件生命周期 | schema→组件树递归 + `callRendererLogic` 插件注入 + 双向上下文 | `_frontend` 新建渲染器包，或内化 z-frontend-render |
| **P3** | 物料协议 + setter 体系 | 物料四件套 + 声明式属性面板 + 双注册表 | `_frontend/z-lc-component` 物料层 |
| **P4** | 可视化设计器壳 + 三方 diff | 壳/内核解耦、插件双钩子、业务优先合并 | `z-lc-admin-ui` 设计器 + `z-lc-design` |

---

## 1. 参考引擎全景

### 1.1 五个子项目与依赖关系

```
                    ┌─────────────────────────────┐
                    │  设计器 (page-solution)      │  产出 / 编辑 Schema
                    │  PageEditor + MKFlowEditor  │  ← 基于第三方内核 mk-base-editor
                    └──────┬──────────────┬───────┘
                           │ modulesMap   │ logicFragmentMap / parseFlow
                           ▼              ▼
        ┌──────────────────────┐   ┌──────────────────┐
        │ 物料库 (material)     │   │ 逻辑引擎 (logic) │
        │ 50 个标准物料         │   │ 编译 + 执行器    │
        │ view/属性面板/事件声明 │   │ 21 类节点        │
        └──────────┬───────────┘   └────────┬─────────┘
                   │ 渲染插件                │ injectCallback
                   ▼                         ▼
        ┌──────────────────────────────────────────────┐
        │  渲染器 (renderer)                            │
        │  BasicRenderer → PageView → <View>（外部内核） │
        │  6 种加载源 + 插件注入 + 异步物料 + 上下文桥   │
        └──────────────────────────────────────────────┘
                   │  Schema（唯一契约）
                   ▼
             页面 DOM
```

- `mk-materials/` 是空目录，忽略。
- **关键认知**：这个引擎本身也站在第三方内核之上——画布/拖放/属性面板来自 `mk-base-editor`，schema→React 递归来自 `mk-view`。它自己的价值在于 **Schema 契约、物料协议、加载策略、逻辑编译**。复刻时真正要学的是这些"胶水与协议"，而不是重造画布内核。

### 1.2 Schema 数据结构（整套引擎最核心的资产）

来自 `mk-renderer/src/extension-core/schema-tool.ts` 与 `mk-logic/example/mock/json1.json`：

```ts
{
  elements: {                          // ① 扁平节点表：id → { type, props }
    "atomicButton-1e9f9cdf": { type: "atomicButton", props: { text: "查询" } }
  },
  layout: {                            // ② 树形结构：与节点表分离，通过 id 关联
    root: "pageContainer-xxx",
    structure: {
      "pageContainer-xxx": ["atomicButton-1e9f9cdf", "..."],   // 普通子节点
      "tabsContainer-xxx": { children: [...], namedSlot: [...] } // 具名插槽
    }
  },
  imports: [                           // ③ 物料依赖（运行时按需加载 UMD/JSONP）
    { name, version, package, type: "component" | "schema", async?: boolean }
  ],
  context: {                           // ④ 上下文协议
    props: { variableList, eventList, contextMethods },  // 对外暴露的变量/事件/方法
    extensions: [{                     // 定制扩展点（IoC 式）
      beanName, api, spi, models,
      lifeTimes, eventListeners, observers
    }],
    global: { style, services }
  },
  logic: {                             // ⑤ 逻辑流
    flowBinds: { "atomicButton-xxx::EventEmit-onClick": ["flow-id"] }, // 事件→流绑定
    flows: {
      "flow-id": { headAddress, flowNodeMap: { [addr]: { execute, next, pre } } }
    }
  },
  meta: { jarvis: ... }                // 扩展元数据
}
```

**设计精髓**：`elements`（扁平）与 `layout.structure`（树）**分离**，节点靠 id 关联，支持具名插槽。这让「改结构」和「改属性」互不干扰，也是 diff/三方合并、定制包改 schema 的基础。

### 1.3 完整运行链路

```
设计态拖拽 → Schema JSON（含 logic 图数据）
       ↓ 版本号判断：旧版走 correctLogic() 编译图数据
运行时加载（6 种来源策略）→ schema + modulesMap
       ↓ 物料 HOC 包装 + 插件注入（callRendererLogic）
<View> 从 layout.root 递归渲染 elements，注入逻辑回调
       ↓ 事件触发
engine.runLogicFlow → Generator 执行器 → 节点 logic() → setState（memento 批量提交）
```

---

## 2. 四大模块逐一拆解

### 2.1 渲染器 renderer（"大圣通用渲染器"）

**职责**：不负责 schema→React 递归（那是外部 `mk-view`），它做「装配」——加载、注入、扩展。

**三档渲染器**：`BasicRenderer`（编排中枢）/ `BlockRenderer`（薄包装）/ `FormRenderer`（+表单实例 `useForm`）。

**6 种加载源（策略模式，最值得学）**：`mk / hbos / singleFetch / singleRequire / commonLoad / studio`，统一返回 `{schema, modulesMap, pageSchemaId}`。新增来源 = 加一个函数，上层无差别消费。

**插件生命周期**：9 个 ViewPlugin + LogicPlugin，统一在 `callRendererLogic(engine)` 阶段注入 SPI / 表单 / 上下文 / 校验 / 错误上报。

**双向上下文协议**：
- 外部 → 页面：`contextPropsVariableMap` 变化 → 触发 `PropsUpdate` 逻辑流
- 页面 → 外部：`contextMethods` 包装后经 `setContextMethods` 抛出
- 元素间：统一 key 约定 `${elementId}-variable` / `${elementId}-methods`，`contextValueParser` 递归剔除 React 元素保证可序列化

**面向 AI Agent**：扫描 `code-` 开头的逻辑流 + `feAgentTool` 标记，注册成 `window.feAgentTools`，把低代码逻辑流暴露成前端 Agent 可调用的工具。

### 2.2 物料库 material

**单个物料 = 四件套**（以 atomic-input 为例）：
- `index.tsx`：视图组件 + 最小出口 `{default, materialConfig}`
- `export-material/basic-config.tsx`：view/edit 共享元数据基类（title/isAtomic/canTableEditUse/groupName/dragType...）
- `export-material/index.tsx`：完整协议 `ModuleConfig` = `default + materialConfig + editConfig`
- 插件/工具（可选）

**ModuleConfig 协议**：
- `materialConfig`：`defaultProps / variableList / eventList / contextVariables / contextMethods / matchInfo`
- `editConfig`：**声明式属性面板**——`{groupName, label, type, field, props, CustomCom, linkageHide, info}`；`linkageHide` 实现字段联动显隐，`CustomCom` 插槽接入任意 setter

**双注册表**：`materials.tsx`（设计器用，含 editConfig）/ `components.tsx`（渲染器用，轻量纯组件，避免打入设计器依赖）。

**setter 体系**：`select-options-config / rules-config / actionable-list-config / function-editor / material-picker / field-names / size-input / color-picker ...`，统一 `{value, onChange}` 接口。`MultipleOptionsConfig` 自动补 `$$_mk_unique_id_$$` 稳定键供 diff 用；`SubConfigPanel` 复用目标物料的 editConfig 实现"表格列内嵌配置面板"。

**8 个运行时插件**：`validator`（async-validator）/ `form` / `inject-material` / `mount-form-engine` / `default-form-value` / `container-size` / `common-modules` / `table`（已废弃）。

**命名语义**：`atomic-xxx`（原子不可拆）/ `component-xxx`（页面级业务组件）/ `xxx-container`（容器，dragType 决定可拖内容）；`atomic-table-xxx-view`（表格单元格专用）。

**接口→物料自动匹配**：`matchInfo: {weight, mach(apiDocsSchema)}` 让物料声明"我能渲染哪种 API 数据类型"，为接口驱动搭建提供依据。

### 2.3 设计器 page-solution

**职责**：基于内核 `BaseEditor` 搭一个完整 IDE 外壳，自己只写「壳 + 插件 + Context」。

**UI 组成**：顶栏 `WrapperHeader`（健康报告/全局搜索/保存预览出码）+ 左栏 `LeftTab`（页面树/组件/数据源/页面配置/Schema/历史/文档）+ 中间 `TabPage → BaseEditor`（画布由内核渲染）+ 底栏 `WrapperBar`（diff/AI 入口）+ 4 个 Drawer（逻辑搭建/通用逻辑/ReadMe/代码视图）。

**插件双钩子**：实现 `IPlugin`，`editor(EditorHooks)` + `context(ContextHooks)` 两条切面：
- EditorHooks：`renderTools / renderHeaderLeft / beforeInitModulesMap`
- ContextHooks：`beforeInsertElement / afterInsertElement / beforeRemoveElement / getElementEditConfig`
典型用法：弹窗插入时自动注入关闭逻辑流、具名 slot 删除时修复索引、数据模型转表单。

**三方 Schema diff + 业务优先合并（高价值）**：
- `getSchemaDiff(new, old)` → DOM 差异（layout/structure/element/prop）+ logic 差异（flow/node/prop），"主打分类讨论"
- `mergeSchema(oldPlatform, newPlatform, newBusiness)` → `mergeSchemaDiff` 以业务为基线，平台改动合并进来、**业务优先**
- `IDiffPlugin.getDiff/mergeDiff` 策略注册表，key = `propFrom::type::propName`，表格列用稳定键 diff

**逻辑节点表与 UI 物料表分离**：`modulesMap`（UI 物料）与 `logicFragmentMap`（24 类逻辑节点）作为两个独立 prop 注入，宿主可任意替换。

**AI 分层接入（不是硬绑）**：① 问答机器人（`talkUrl` 配置）；② SSE 流式代码生成（`%%mk-code%%` 标记协议 + AbortController）；③ 承接外部 Agent（Jarvis）产物（中性字段 `meta.jarvis / feAgentTool / extensions`）。**没有"自然语言→整页 Schema"，AI 供应商可替换。**

### 2.4 逻辑引擎 logic

**定位**：以**运行态解析器/执行器为核心**；设计态节点物料 + 一个未启用的遗留编辑器壳（真实画布由 page-solution 提供）。

**三段式编译（整套引擎技术含量最高的设计）**：
```
① correctLogic()：LogicFlow 画布 {nodes, edges}
        ↓ 边的语义编码进 next[].condition
② logic { flowBinds, flows{headAddress, flowNodeMap{execute, next, pre}} }
        ↓ parseLogic → parseFlow
③ LogicFlowMap { elementId: EventFlow[] } → EventFlowNode[]（可执行）
```

**parseFlow 算法**：主干只跟 `next[0]` 线性化；条件节点把每条出边递归成 `condition[].flowNodes`（分支树）；子流程 `type:'flow'` 按 `targetId` 递归内联展开。执行期只需 `while(i < flowNodes.length)` 的简单循环。

**条件语义编码在"边"上**：`edge.properties.isIfLine / conditionType` → `next[].condition('trueLine'|'falseLine'|表达式)`，设计态"是/否线"与运行态布尔分支一一映射。

**Generator + 自研 run() 驱动器**：`function* executeLogic` + `yield` + `yield*`，一个驱动器统一处理同步值、Promise、递归子流、中断信号（`STOP_EXECUTE_LOGIC`）。

**8 类上下文作用域**：`params / state / logicFlowState / commonLogicFlowParams / contextPropsVariableMap / contextGlobalVariableMap / contextGlobalServiceMap / _stateApi`；以 `flowNode.address` 为 key 隔离每个节点抛出的值（如 `logicFlowState[addr].res`）。

**memento 批量状态提交**：`setStateInFlow` 只累积，`updateStateInFlow` 才 reduce 合并后一次性 `engine.setState`，避免一条逻辑流多次 setState 导致多次渲染。

**节点四件套 + 双注册表**：`LogicNode {title, view, model, formNode, logic, groupName, contextVariables}`；设计态表 `logicNodesMap` 与运行时表 `logicFlowNodes` 分离，`createLogicEngine({logicNodes})` 允许外部覆盖。21 类节点分 6 组：起始结构 / 条件分支 / 数据状态 / 交互反馈 / 方法调用 / 扩展调试。

**与视图引擎三条衔接路径**：`engine.injectCallback` 事件注入 / `engine.runLogicFlow` 动态调用 / `callRendererLogic` 插件钩子。

---

## 3. z-lc 现状基线

### 3.1 定位（15 个 Maven 模块，三层）

- **核心引擎 6 件套**：`common`（DTO/枚举）/ `sdk`（27 支业务 SPI）/ `core`（动态 SQL CRUD、17 fieldtype、pipeline、event、undo、permission、workflow、ai、**page V2**、**vapi V3**）/ `design`（注解 + Collector + 演示 Controller）/ `web`（19 Controller）/ `admin`（启动入口）
- **报表域 7 件套**：`report-common`（ViewSchema 契约）/ `report-datasource` / `report-dataset`（内存列式引擎：join/groupby/表达式）/ `report-render`（WidgetSpec→echarts option）/ `report-book`（发布快照）/ `report-ai`（NL2Report）/ `report-web`
- **孵化 2 个**：`datasource`（DMS 数据源）/ `vapi`（虚拟接口，桥 z-script，支持 EL/GROOVY/LUA/SQL/MOCK + 沙箱）

### 3.2 三套前端（React，无 Vue）

| 目录 | 版本 | 定位 |
|---|---|---|
| `z-lc-admin-ui` | React 18.3 + antd 5 + Vite 5 | 管理台；模型设计器 + V2 页面编排（坐标表单） |
| `_frontend/z-lc-component` | React 19 + antd 6 + Vite 6 | 应用建模/运行时/治理组件库（可发布） |
| `_frontend/z-lc-suit` | React 19 + antd 6 + Vite 6 | 报表独立运行壳 |

**关键**：schema 驱动的渲染器 `UniversalRenderer` 与字段物料注册表 `z-frontend-render/fields` 都在**外部包** `@yuku123/z-frontend-render`，z-lc 只是消费方。

### 3.3 已有的低代码能力

- DB 驱动 CRUD 引擎 + 字段类型体系（17 handler）+ 建模/provision + DDL
- Pipeline 处理链 + 事件回放 + undo + 部署 + 权限
- 报表全链：数据源 → 数据集（含 M1 表达式）→ 渲染 → 书/发布 → NL2Report
- V2 页面 schema（`widgets/layout/datasets` 三块，7 种硬编码 widget 白名单）
- V3 虚拟接口 + `onClick → vapi` 事件绑定

### 3.4 缺的能力（=复刻候选）

1. **可视化拖拽画布**：页面编排靠填 x/y/w/h 坐标，代码注释自述"拖拽留到接入 dnd 后再加"
2. **自研渲染内核**：核心渲染器与物料协议在外部包
3. **物料注册协议**：无 Material 定义/属性面板协议/组件市场；widget 类型是硬编码白名单
4. **可视化逻辑编排**：无事件-条件-动作流程图；onClick 只能绑一个 vApi，无分支/循环/子流程
5. **设计态闭环**：`z-lc-design` 的 PageTemplate 只有演示接口，注释写"CRUD 是 Phase 2B"
6. **表单设计器**：无拖拽字段/布局/联动规则
7. **富 Schema 表达力**：当前只有坐标 + 单层 widgets，无组件树嵌套/父子容器/统一数据绑定 DSL
8. **表达式引擎**：ExprParser 是 M1 最小实现，函数/日期/嵌套字段待办
9. **前端统一**：三套前端并存、页面重复，缺单一可信产物

**阶段判断**：z-lc 处于「元数据驱动的应用运行引擎 + BI 报表 + 初级 schema 页面编排」，**尚未进入成熟可视化低代码搭建平台**。

---

## 4. 板块对比矩阵

| 能力维度 | 参考引擎 | z-lc 现状 | 差距 |
|---|---|---|---|
| Schema 契约 | 扁平 elements + 树 layout + logic + context，单一契约 | 3 套 schema 分散（报表 ViewSchema / 页面 schema_json / 模板 viewJson），页面版仅坐标+单层 widgets | **大**：契约不统一、表达力弱 |
| 渲染 | schema→组件树递归 + 6 源加载 + 插件生命周期 | 外部包 UniversalRenderer，无自研内核/插件机制 | **大** |
| 物料 | 50 个标准物料 + ModuleConfig 协议 + setter 体系 + 双注册表 | 7 种硬编码 widget + 外部 fields 注册表 | **大** |
| 逻辑编排 | 图→链表编译器 + Generator 执行器 + 21 节点 + 8 类上下文 | 固定 pipeline + onClick→vApi 单绑 | **大**：无可视化、无分支 |
| 设计器 | 壳/内核解耦 + 双钩子插件 + 三方 diff | 模型设计器（表格）+ 坐标表单 | **中**：壳在，画布缺 |
| diff/合并 | 结构化 schema diff + 属性级策略 + 业务优先三方合并 | 无 | **中** |
| AI | 问答 + SSE 代码生成 + Agent 产物承载（可替换） | NL2Report + AI 生成表单建议 | **小**：方向对，缺承载协议 |
| 后端 CRUD/建模 | 弱（前端引擎为主） | 强（17 fieldtype、pipeline、undo、权限、部署） | **z-lc 领先，保持** |
| BI 报表 | 无 | 强（数据集→渲染→发布→NL2SQL 全链） | **z-lc 领先，保持** |

**结论**：z-lc 的强项（后端引擎 + 报表）恰好是参考引擎的弱项；参考引擎的强项（Schema 中枢 + 前端三件套 + 逻辑编排）恰好是 z-lc 的缺口。**互补性极强，复刻方向明确。**

---

## 5. 可复刻板块清单（按优先级）

### P0：统一 Schema 协议 —— 地基，必须先做

- **学什么**：`elements` 扁平表 + `layout.structure` 树分离 + 具名插槽；`imports / context / logic` 四大块；schema 版本 + normalize
- **怎么落地**：新建 `z-lc-schema` 模块（纯 Java 契约 + Jackson 反序列化 + 校验 + 编译器），把现有 3 套 schema 收敛到单一结构；`PageSchemaValidator` 从白名单升级为可扩展物料类型注册；提供 `SchemaCompiler`（旧图数据→执行态）
- **为什么先做**：设计器、渲染器、逻辑三者都依赖它。契约不定，后面全返工
- **去品牌化**：字段名用中性词（`nodes/structure/flowBinds/flowNodeMap`），不要 `element`/`hyper`/`mk` 前缀

### P1：逻辑引擎（运行态）—— 技术含量最高、最能拉开差距

- **学什么**：三段式编译；`parseFlow` 主干线性化 + 分支树 + 子流程内联；条件编码在边；Generator + run() 驱动器 + 中断；8 类上下文；memento 批量提交；节点四件套
- **怎么落地**：`z-lc-core/logic` 新建 `LogicCompiler`（图→执行链表）+ `LogicExecutor`（Generator 或等价状态机）+ `LogicNodeRegistry`；**复用已有 z-script 虚拟接口做"接口请求"节点**，复用 vapi 沙箱做"自定义逻辑"节点；先做运行态，画布后置
- **风险**：Java 无原生 Generator，用「显式栈 + 迭代器」或「CompletableFuture 链」等价实现；中断/异步语义要仔细对齐

### P2：渲染内核 + 插件生命周期 —— 掌握自主权

- **学什么**：多源加载策略；`callRendererLogic(engine)` 插件注入；物料 HOC 包装；异步物料骨架；双向上下文协议
- **怎么落地**：在 `_frontend` 新建自研渲染器包（或明确 `z-frontend-render` 契约边界后内化）；定义 `RenderPlugin` 接口 + 生命周期；把外部依赖收敛成可替换适配层
- **风险**：3 套前端都在用外部渲染器，需兼容过渡，建议先"契约对齐 + 适配层"，再逐步替换

### P3：物料协议 + setter 体系 —— 规模化搭建的前提

- **学什么**：物料四件套（view / 共享 config / 完整协议 / 插件）；`ModuleConfig`；`editConfig` 声明式面板 + `linkageHide`；setter 复用；双注册表；`matchInfo` 接口匹配
- **怎么落地**：`_frontend/z-lc-component` 建 `MaterialRegistry` + 协议 TS 类型；先做 10-15 个高频物料（输入/选择/表格/表单容器/按钮/日期）；setter 复用现有 antd 组件
- **去品牌化**：全新命名与目录，不复制源码文本

### P4：可视化设计器壳 + 三方 diff —— 工作量最大，最后做

- **学什么**：壳/内核解耦；IPlugin 双钩子；三方 diff + 业务优先合并；逻辑节点表与物料表分离；AI 分层接入
- **怎么落地**：`z-lc-admin-ui` 的 `PagesPage` 从坐标表单升级为拖拽画布（`@dnd-kit` 已在依赖里）；`z-lc-design` 补齐模板 CRUD/版本/发布-预览-回滚；`diff-tools` 落地到 `z-lc-design`
- **建议**：若预算有限，可采购/采用成熟画布内核（如参考引擎用的同类内核），只自研壳 + 插件 + 契约，避免重造拖放轮子

---

## 6. 去品牌化硬约束（复刻红线）

| 类别 | 必须剥离 | 替换原则 |
|---|---|---|
| 包名/坐标 | `@cffe/*`、`@hbos/*`、`@logicflow/*` 等外部商标包 | 用 z-lc 自有命名空间 `@yuku123/z-lc-*` 或 Java 包 `com.zifang.z.lc.*` |
| 名称前缀 | `mk-*`、`hyper-*`、`大圣`、`jarvis`、`feAgentTool` | 中性命名：`schema / node / flow / widget / logicNode` |
| 内网信息 | `gitlab.cfuture.shop`、`come-future.com`、作者邮箱/姓名、`10.10.x.x` | 一律不出现；仓库地址用 z-lc 自己的 |
| 源码文本 | 任何 `.ts/.tsx/.java` 文件原文复制 | **只移植设计思想、数据结构、算法思路**，用自己的代码重写 |
| 文档/注释 | 参考引擎特有术语与截图 | 用中文中性描述 |

**自检命令**（复刻完成后跑）：`grep -rniE "cffe|hbos|大圣|mk-|gitlab.cfuture|come-future" <改动目录>` 应无命中。

---

## 7. 建议的下一步

1. **先确认范围**：从 P0/P1 开始（地基 + 差异化），还是先做能快速看到效果的 P4 画布？
2. **P0 细化**：我可以先产出 `z-lc-schema` 的模块设计 + Schema 契约 Java 类 + 与现有 3 套 schema 的迁移映射表（不动现有代码）
3. **P1 可行性验证**：用 z-lc 现有 vapi 场景，做一个"接口请求 → 条件分支 → 设置变量"的最小逻辑流端到端 demo，验证 Java 版执行器可行性

> 本文件是内部研究文档，供决策使用；正式复刻产物遵守第 6 节剥离红线。

---

## 8. 补充：z-opc 体系已有的 render 包（2026-11-17 发现）

用户提示 "z-opc 下还有个 render"，经核查 lead 仓架构文档 `001_项目认知/002_z系列架构与职责边界.md` 与源码仓确认：

### 8.1 它是什么

- **源码仓**：`/Users/zifang/workplace/ceo_workplace/z-opc-foundation/z-render`
- **npm 包**：`@yuku123/z-frontend-render` **1.1.0**（旧名 `@yuku123/render`/`z-render` 已废弃，lead 文档明确"旧名零残留"）
- **定位**：组织共享的**前端渲染引擎**——后端是唯一事实源，本包只做 `schema → antd/echarts 组件` 的分发与视觉补全，**不含任何报表/低代码业务语义**
- **消费方 3 处**（全锁 `^1.1.0`）：z-opc 主壳前端 / `z-lc-admin-ui` / `_frontend/z-lc-suit`

### 8.2 已有能力（对照参考引擎 renderer）

| 能力 | z-frontend-render 现状 | 对应参考引擎 |
|---|---|---|
| schema→视图分发 | `UniversalRenderer` + `BUILTIN_WIDGETS`（LINE/BAR/PIE/KPI/TABLE/RAW），`widgetRenderers` prop 可覆盖分发，三级兜底 | ✅ 有，但是**单层扁平 widget 分发**，非递归组件树 |
| 布局 | 12 栏栅格（`GRID_COLUMNS=12`/`ROW_H=60`），坐标摆放 | ✅ 有，等同页面域坐标编排，非树形插槽 |
| 字段类型注册表 | `/fields` 子路径：9 主类型 + 3 合成键 + 7 widget 覆盖，一条目 owning 渲染/读写 7 面；`registerFieldDefinition` 可扩展 | ✅ 有，接近参考引擎物料协议的"渲染面"，但无属性面板(editConfig)/事件变量声明 |
| ECharts option 补全 | `buildChartOption` + 懒加载 + 8 色色板 | ✅ 有 |
| 大数据护栏 | MAX_RENDER_ROWS=200 / MAX_TREE_NODES=400 | ✅ 有 |
| 双入口 / peer external / antd 5+6 双认 | 工程化成熟，有 deprecation 守卫测试 | ✅ 有 |

### 8.3 关键结论：P2 策略修正

**不新建渲染器包**，改为在 `z-render` 上做**增量扩展**：

1. **新增「递归组件树渲染」能力**（新入口，不破坏现有 `UniversalRenderer`）：
   - 现有 `UniversalRenderer` 消费的是「扁平 widget + 栅格坐标」（报表/P2V 页面域）
   - 参考引擎的核心是「`elements` 扁平表 + `layout.structure` 树 + 具名插槽」的递归渲染
   - 二者数据结构不同 ⇒ 新增 `TreeRenderer`（消费 P0 定义的统一 Schema），与 `UniversalRenderer` 并存
2. **新增插件生命周期**：把字段注册表的 `registerFieldDefinition` 模式推广为 `RenderPlugin` + `callRenderLogic(engine)`，支持上下文注入
3. **物料协议对齐**：字段注册表已有「渲染面」，补 `editConfig` 声明式属性面板 + 事件/变量/方法声明，即升级为参考引擎的 `ModuleConfig`

### 8.4 影响

- P2 工作量 **大幅下降**（复用现成包 + 现成 npm 发布管线），且天然满足"用自己体系的东西、不带外部品牌"
- `z-render` 已是组织自有、发布在 npmjs 官方 registry，构建命令需 `env -u NODE_OPTIONS`（沿用既有纪律）
- 修改 `z-render` 后需抬版本重发，并同步 z-lc 三处消费方锁的版本
