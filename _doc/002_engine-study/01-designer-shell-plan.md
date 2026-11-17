# P4 可视化设计器壳落地规划

> 本文是 P4 前端部分的设计规划（后端 SchemaDiff/三方合并已在 `z-lc-schema` 落地并通过测试）。
> 目标：把 `z-lc-admin-ui` 的页面编排从「填坐标表单」升级为「拖拽画布 + 物料面板 + 属性面板」的完整设计器。

## 1. 现状与目标

**现状**：`z-lc-admin-ui/src/views/pages/PagesPage.tsx` 用 antd Table 填 `x/y/w/h` 坐标，代码注释自述"拖拽留到接入 dnd 后再加"。`@dnd-kit` 已在依赖里但只用于表格列排序。

**目标**：三层解耦，不重造画布内核：

```
设计器壳 (self-built)     ← 顶栏/左物料区/中间画布/右属性面板
   │ 消费
渲染内核 (z-frontend-render ./tree)  ← TreeRenderer 递归渲染 + 插件生命周期 + 物料注册表
   │ 编辑
Schema 契约 (z-lc-schema)  ← elements + layout 树 + logic，后端唯一事实源
```

## 2. 与参考引擎的对位

| 参考引擎设计 | 本次落地方案 |
|---|---|
| 壳/内核解耦，画布外包给 `mk-base-editor` | 不自研拖放内核；`TreeRenderer` 做渲染，画布拖放用 `@dnd-kit`（已有依赖）或成熟开源内核 |
| IPlugin 双钩子（EditorHooks/ContextHooks） | 定义 `DesignerPlugin`：物料插入前/后、属性变更、编辑面板定制四个切面 |
| 物料表与逻辑节点表分离 | `MaterialRegistry`（`z-render/tree`）与逻辑节点注册表（`z-lc-logic`）各自注入 |
| 三方 diff + 业务优先合并 | 已落地在 `z-lc-schema` 的 `SchemaDiffer` / `SchemaMerger`；设计器只做可视化展示 |
| 属性面板声明式 editConfig | 复用 `z-render/tree` 的 `EditField` 协议 + setter 注册表 |

## 3. 组件拆分

```
z-lc-admin-ui/src/views/designer2/
├── DesignerPage.tsx          # 外壳：顶栏 + 三栏布局 + 底部状态栏
├── MaterialPanel.tsx         # 左侧物料区：按 group 分组，拖拽源
├── Canvas.tsx                # 中间画布：TreeRenderer 渲染 + dnd 落点
├── PropertyPanel.tsx         # 右侧属性面板：按 editConfig 渲染 setter
├── LogicDrawer.tsx           # 逻辑流抽屉：事件-条件-动作编排（可视化）
├── setters/                  # setter 组件：string/number/select/boolean/function...
├── plugins/                  # 设计器插件：弹窗自动关流、模型转表单等
└── store.ts                  # schema 状态 + 选中节点 + undo/redo
```

## 4. 数据闭环

1. 后端 `PageTemplateController` 补齐 CRUD/版本/发布-预览-回滚（当前只有 sample 演示接口）
2. 设计器编辑产出统一 `PageSchema`，经 `SchemaCodec` 与后端交互
3. 预览走 `TreeRenderer` + `LogicExecutor`（后端）或前端逻辑执行适配层
4. 发布时后端做 `SchemaValidator` 校验 + `SchemaDiff` 生成版本差异

## 5. 实施顺序（依赖驱动）

1. **S1 属性面板**：先做 `PropertyPanel` + setter 注册表（无拖放，纯表单），验证 `EditField` 协议
2. **S2 渲染接入**：`Canvas` 接 `TreeRenderer`，物料注册表接 `z-render/tree`，静态 schema 能渲染
3. **S3 拖放**：接入 dnd-kit，物料面板 → 画布落点，维护 `elements + layout.structure`
4. **S4 逻辑编排**：事件绑定 → 逻辑流图编辑器 → 编译走 `z-lc-logic`
5. **S5 diff 可视化**：接 `SchemaDiff` 展示版本差异与三方合并预览

## 6. 去品牌化检查

设计器所有命名/文案/组件 key 使用中性词（node/flow/widget/material），
不出现任何外部引擎品牌、包名、内网地址。自检：
`grep -rniE "cffe|hbos|大圣|mk-|hyper|gitlab.cfuture|come-future" z-lc-admin-ui/src/views/designer2` 应无命中。
