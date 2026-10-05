# z-lc · 待办事项索引

这里登记 z-lc 这一轮**没做完的事**：等拍板的、只欠工程量的、以及"已定方向但要排产"的。
一条一个 `featureNNN_<名字>/TASK.md`（文件名用 `TASK.md`，是 09-26 主编点名要的形状；
z-mq 那边同类登记面用 `001_*.md`，两边都是"一事项一目录"）。

约定（照 z-mq 那一族已跑通的规矩）：

- **数字一律现场测**，本文档里的任何读数都写了取数命令，不许抄。
- **负向读数要带阳性对照**："grep 没命中"必须同时给出"这条 grep 在应该有命中的地方确实有命中"。
- 拍完 ≠ 做完：拍完之后按各文档"还欠的工程量"那节开工，收口要 java + 契约 + vitest + 接口层 + 浏览器层 + 注入自证
  **在同一窗口内**都出数。
- 纯缺陷排队不在这里（不需要拍板的走战役卡）；这里只收"等裁定"和"一轮做不完的成串工程"。

| 目录 | 事项 | 性质 | 卡住的范围 |
|---|---|---|---|
| `feature001_workflow_binding_fires/` | 缺陷 #61：流程绑定这条链的其余四层（契约/前端/注入/250） | 已定方向，欠 6 层覆盖面 + 4 问待拍 | `WorkflowTriggerDispatcher` 只在单测里真的发过流程；契约层 2389 行对 workflow **0 命中**；界面上仍有 2 个兑现不了的选项 |
| `feature002_http_status_not_checked/` | 缺陷 #62：`isSuccess()` 不含状态码 ⇒ 404/500 被读成"远端受理了" | 已定方向，欠 10 处替换 + 每处成对猎物 | 6 个 adapter 的读 body / `ping()`；与已闭的 #52 同一形状 |
| `feature003_dead_envelope_parses/` | 缺陷 #63：5 处 `JsonUtil.fromJson` 结构上必抛 + 3 个 adapter 零调用者 | 前半已定方向；后半（要不要宣称支持加密/附件/脚本）等拍板 | ctc 鉴权上下文、字典值域解析、脚本 eval、解密、下载链接 |

## 250 侧现状（09-26 23:1x 实测，会漂，重取见各文档命令）

- 新表 `z_lc_workflow_fire` 在 250 的真 MySQL 8 里**还不存在**（同库里只有 `z_lc_workflow_binding`）。
- 250 上**没有 z-camuda 在跑**（`docker ps` 只有 mysql×2、z-ctc、z-vector、z-graph×2、registry×2），
  而 `z-lc.adapter.camuda.base-url` 默认 `http://localhost:8888` ⇒ 现在部署起来这条链必然落 `FAILED` 行。

## 归属

战役工单在 `z-opc-foundation-lead/002_项目需求/…`；本目录只是"这一轮没做完 / 等裁定"的登记面。
提交口径：**只 add 自己的路径**（共享工作树），`z-lc-admin-ui/pnpm-lock.yaml`、`pnpm-workspace.yaml`
不是本战役的改动面（注意 `ae07610` 那一笔另一会话已代提交，是否回退单独问）。
