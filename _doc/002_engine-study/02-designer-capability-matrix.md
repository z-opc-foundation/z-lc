# 设计器能力对照与拖拽内核说明（2026-11-17 重做）

> 背景：P4 那版设计器「能拖但不好用」——用户实测反馈「怎么拖拽都有问题，样子也很丑」。
> 本文记录**根因**、**重做方案**、**与参考引擎的能力对照**，以及**明确没做的部分**。
> 对应产物：`@yuku123/z-frontend-render@1.3.3` 的 `./designer` 子路径。

## 1. 三个真根因（不是审美问题）

| # | 问题 | 现象 | 根因 |
|---|---|---|---|
| 1 | 画布所有节点都渲染成「[未注册物料]」 | 拖进去看不见东西，像坏了 | `Canvas.tsx` 用 `createMaterialRegistry()` **没传任何物料**；`BUILTIN_MATERIALS` 从未注册 |
| 2 | 已有节点**根本不能拖** | 只能从左侧拖新的，画布内节点纹丝不动 | `DesignNode` 只挂了 `useDroppable`，**从未挂 `useDraggable`**；`handleDragEnd` 里那支 `node:` 分支是死代码 |
| 3 | 只能「追加到容器末尾」 | 想在两个组件之间插入插不进去；拖着没有预览、没有指示线 | 落点取 dnd-kit 默认 `rectIntersection`：容器矩形天然包含子节点矩形 ⇒ 永远命中面积最大的祖先；且无 `DragOverlay` |

三条叠加，就是「拖不动 + 看不出效果 + 很丑」。

## 2. 重做后的拖拽内核

核心是把「落在哪」从 dnd-kit 的碰撞结果里拿出来，自己解算（`src/designer/kernel/dropResolver.ts`，纯函数、可单测）：

```
指针 (clientX, clientY) + 所有可投放矩形快照
   → 取包含指针的矩形，按面积升序 → 第一个 = 最深的那个（真目标）
   → 目标是容器？
        上边缘(≤14px) → 插到容器之前
        下边缘(≤14px) → 插到容器之后
        中间          → 放进容器，再按直接子节点的中线决定下标
   → 目标是原子节点（按钮/输入框）？ → 只能落到它前/后，成为兄弟
   → 具名插槽投放区 → 整体算「放进去」
   → 指针不在任何节点内 → 画布兜底：追加到根末尾
```

配套约束：

- **排除拖拽源子树**：`collectSubtreeIds` 在 dragStart 时算出被拖节点及其后代，解算时整段剔除 ⇒ 不可能拖进自己的子树（store 侧还有第二道 `moveTo` 守卫）。
- **下标修正**：同容器内「往后挪」时，先摘除再插入会让目标下标左移一位，`moveTo` 里按 `from.index < index` 回退。
- **指示线用 `position: fixed`**：画布可滚动、可缩放，fixed 坐标不受影响。
- **画布与大纲树共用同一套解算**：两边元素都挂 `data-dnode*` 属性（id/父级/插槽/下标/能否容纳子节点/深度），`snapshotDropRects()` 抓一份矩形表即可，不需要两套实现。

## 3. 能力对照（参考引擎 vs 现在）

| 能力域 | 参考引擎（mk-page-solution） | 现在 | 说明 |
|---|---|---|---|
| 物料面板（分类/搜索/图标） | left-menu + material-plugins | ✅ 22 个物料，4 组，搜索，点击追加兜底 | 物料同时是 `view` + `editConfig` + 事件/变量声明 |
| 画布渲染 | 自研 renderer + 插件 | ✅ `TreeRenderer` 同构（elements + layout.structure + 具名插槽） | 设计态与运行态同一份物料实现 |
| 拖拽插入 | 有（引擎内建） | ✅ 前/后/内部 + 精确下标 | 见第 2 节 |
| 拖拽排序 | 有 | ✅ 同级 / 跨容器 / 大纲树内拖拽 | |
| 拖拽预览 | 有（ghost） | ✅ `DragOverlay` 跟随光标 | |
| 落点指示 | 有（插入线） | ✅ 横线（前后）+ 虚线框（放入容器） | |
| 空容器占位 | 有 | ✅ | |
| 大纲树 | left-page-tree | ✅ 摊平行 + 展开/折叠 + 拖拽 + 删除 | 只做单选，无多选 |
| 属性面板 | 设置器体系 + 分组 | ✅ 属性/样式/事件 三页签，14 类 setter | `editConfig.group` 决定归属 |
| 撤销/重做 | 有 | ✅ 100 步，⌘Z / ⌘⇧Z | |
| 复制/粘贴/再制/删除 | 有 | ✅ ⌘C/⌘V/⌘D/Delete | 子树整体深拷贝并重映射 id |
| 快捷键 | 有 | ✅ 上述 + Esc 取消选中 + ⌥↑ 选父级 | |
| 逻辑编排 | flow-editor（图编辑器） | ⚠️ 线性链编辑器 | 21 类节点与后端 `DefaultNodes` 对齐，可增删改排序；**没有连线画布** |
| 事件绑定 | 有 | ✅ 组件事件 → 逻辑流（`logic.flowBinds`），键为 `节点id.事件名` | 与 `SchemaValidator` 口径一致 |
| 全局变量 | global-variable-editor | ✅ 变量声明（名称/类型/说明） | 无「变量与组件属性联动」的可视化绑定 |
| Schema 查看/导入 | schema-page / code-editor | ✅ JSON 查看 + 复制 + 导入替换（带结构校验） | **没有出码（源码生成）** |
| 版本差异 | diff-tools | ✅ 结构化 diff（节点增删改） | 三方合并仍在后端 `z-lc-schema` |
| 设备宽度 / 缩放 | 有 | ✅ 桌面/平板/手机 + 50%–150% | |
| 预览 | 有 | ✅ 切到运行时 `TreeRenderer`，交互激活 | |
| 草稿持久化 | 有（服务端） | ⚠️ localStorage 自动存 + 保存按钮 | **没有接 z-lc 页面表**（需要 Page API，未做） |
| 多选 / 成组 / 组合 | 有 | ❌ 未做 | 单选模型 |
| 内联编辑文案 | 有 | ❌ 未做 | 改文案走右侧属性面板 |
| 条件渲染 / 循环渲染 | 有 | ❌ 未做 | schema 里没有 v-if/v-for 位，需先扩契约 |
| 接口驱动搭建（matchInfo） | 有（model-2-form-table-plugin） | ❌ 契约有 `matchInfo`，UI 未用 | 属于「选接口自动生成表单」那一层 |
| 代码编辑器 / 出码 | code-editor | ❌ 不做 | 产物即 Schema，无独立代码形态 |
| 多人协同 / 模板市场 / AI 生成 | 有 | ❌ 不做 | 非当前产品范围 |

一句话结论：**组件树搭建这条主链（物料 → 拖拽 → 属性 → 事件 → 逻辑 → 差异）能力对齐；
多选/内联编辑/条件循环渲染/接口驱动/出码这几块是明确没做的**，已在表里标出，不当成已完成。

## 4. 落到代码的位置

设计器已从两边各自的「手写版」**上收到共享包**（此前 z-lc 与 z-opc 各有一份，行为不一致）：

```
z-render/src/designer/
├── DesignerPage.tsx      壳：顶栏 + 三栏 + DndContext 编排 + 快捷键 + 草稿
├── Canvas.tsx            设计态递归渲染（装饰不进布局：outline + 绝对定位）
├── OutlinePanel.tsx      大纲树（摊平行，与画布共用 data-dnode 契约）
├── MaterialPanel.tsx     物料面板
├── PropertyPanel.tsx     属性/样式/事件
├── LogicDrawer.tsx       逻辑流编辑（21 类节点，重建 pre/next 防断链）
├── SchemaDrawer.tsx      JSON / 变量 / 页面 三页签
├── DiffDrawer.tsx        结构化版本差异
├── store.ts              PageSchema + 选中 + 历史 + 剪贴板（位置语义统一为 parent/slot/index）
├── materials/index.tsx   22 个宿主物料（antd 渲染，designMode 时降级为静态）
├── setters/index.tsx     14 类 setter（宿主实现 SetterProps 协议）
├── kernel/dropResolver.ts  落点解算（纯函数）
├── kernel/rects.ts       DOM → 矩形快照
├── antdCompat.ts         antd 5/6 差异收敛（Drawer width/size）
└── designer.css          设计器样式（lib 构建出 dist/designer.css）
```

消费方（都只留一个薄壳）：

- `z-lc/z-lc-admin-ui/src/views/designer2/index.ts` → 转出 `DesignerPage` + 引样式
- `z-opc/.../pages/lc-designer/index.jsx` → `<DesignerPage appCode pageCode persistence onPreview />`，
  `persistence` 由宿主注入（宿主才认识自己的 HTTP 接口），共享包不认 HTTP

## 5. 验证方式

| 层级 | 方式 | 结果 |
|---|---|---|
| 落点算法 | `kernel/dropResolver.test.ts` 13 条（含最深优先/边缘/插槽/排除子树/环） | 绿 |
| 状态内核 | `store.test.ts` 21 条（按位置插入、下标修正、跨容器、复制粘贴、逻辑写入） | 绿 |
| 壳装配 | `DesignerPage.test.tsx` 9 条 | 绿 |
| 共享包全量 | `npx vitest run` | 105 绿 / 14 文件 |
| admin-ui 全量 | `npx vitest run` | 267 绿 / 31 文件 |
| 真浏览器拖拽 | `z-lc-admin-ui/e2e/designer-dnd.mjs`（生产构建 + preview，Chrome） | 16 项全过 |
| 主壳冒烟 | `z-lc-admin-ui/e2e/designer-smoke.mjs`（`E2E_BASE=3000 E2E_PATH=/lc-designer`） | 7 项全过 |
| 造系统端到端 | `z-lc-admin-ui/e2e/lc-system.mjs`（打主壳 3000，跑真 Chrome） | 16 项全过 |
| 保存落库链路 | `z-lc-core` `PageSaveChainTest`（8 条，Proxy 伪造 Mapper） | 绿 |
| z-lc-core 全量 | `mvn -pl z-lc-core test` | 1425 绿 |

真浏览器用例覆盖：点击追加、拖入画布空白、拖到节点上边缘插到之前、拖进容器中部、同级排序、
拖入自身子树被拒、大纲树拖拽、落点指示出现、属性写回、预览模式、⌘Z 撤销。
jsdom 里 `getBoundingClientRect` 恒为 0，所以拖拽不靠单测，靠这套脚本。

## 6. 「造一个系统」的完整链路

`lc-system.mjs` 验的是这条链，不是单个组件：

```
设计器搭页面 → DesignerPersistence.save → POST /api/lc/page/{create|update}
   → PageSchemaValidator（单一入口，canonical 与 legacy 都收）
   → z_lc_page 表 → GET /api/lc/runtime/page/{app}/{page}（免登录）
   → TreeRenderer + createMaterialRegistry → 用户看到的页面
```

页面之间的串联靠按钮的 `link`（"跳转页面编码"）字段，运行态 `handleEvent` 命中后
`navigate('/lc/app/{app}/{link}')`。DEMO 应用 `tickets` 有两个页面：
`list`（统计 + 表格 + 新建按钮）→ `new`（表单 + 提交/返回）。

### 鉴权是双口径，不是一套

| 口径 | 前缀 | 登录 | 用途 |
|---|---|---|---|
| 设计态 | `/api/lc/page/**` | 需要（SSO 拦 302） | 读写页面定义 |
| 运行态 | `/api/lc/runtime/page/**` | 免登录（在 `sso.exclude-paths`） | 终端用户看页面 |

前端 `lcPage.js` 的 `readResult` 专门处理"未登录被重定向回 200 HTML"这种情况——
先看 `text` 首字符是不是 `<`，是就报"接口返回了页面而不是 JSON"，而不是让 `JSON.parse` 炸掉。

设计器读页面失败时**不静默**：画布顶部出红色横条 `[data-designer-load-error]`，
否则空白画布一保存就把线上页面覆盖了。

### 本机自动化的边界（诚实记录）

本机跑 E2E 没有 SSO 登录态，`/api/lc/page/**` 必然 302。因此：

- **已验证**：设计器点保存确实把请求打到 `/api/lc/page`（E2E 断言请求发出、错误正确呈现）；
  保存落库链路由 `PageSaveChainTest` 在 Service 层钉死（含坏结构不落库、重复页面拒绝、
  旧写法漏 datasetId 仍被拒）。
- **未由自动化验证**：SSO 放行后 Controller 到库那一段。DEMO 数据由
  `z-opc/e2e/seed-lc-demo.py` 按设计器同形 schema 直接落库补齐。
- 真人登录后点保存走的是同一条写链路，可用 `z-opc/e2e/seed-lc-demo.py --verify` 对比库内内容确认。

## 7. 遗留

1. **逻辑编排是线性链**：条件分支目前靠 `condition` 节点 + 单出口表达，真正的分支树编辑器没做。
2. **多选/内联编辑/条件循环渲染**未做（第 3 节标 ❌）。
3. `packages` 侧 lint 未纳入 CI（本次只跑了 typecheck + vitest）。
4. **认证态的保存未进 CI**：需要一个可用的测试账号 + 验证码通道，或给 E2E 开一条
   dev-only 的登录态注入（注意别把后门口子留在生产配置里）。
