# feature001 · 缺陷 #61 流程绑定：剩下没做完的工程量与等拍板的四问

登记时间：2026-09-26 23:12（`date` 现测）。仓库 HEAD 见文末"取数命令"，**别信本文任何数字，按命令现测**。

一句话现状：**java 侧这条链已经通了，本轮新写/重写的四支测试合计 68 例全绿
（`WfAdapterTest` 18 + `WorkflowBindingServiceTest` 25 + `WorkflowTriggerDispatcherTest` 17 + `WorkflowTriggersTest` 8，
同窗 `z-lc-core` 全量 1326 例 / 0 失败 / BUILD SUCCESS），
但从 HTTP 契约层到浏览器层到 250 真库，还没有一层碰过它** —— 也就是说
"绑定行真的换出一次流程实例"这件事，目前只在进程内的单测里发生过。

---

## 1. 本轮已经做完并量过的（不用重做，但收口要重跑）

| 已闭的部分 | 证据（本轮 23:0x–23:1x 实测） |
|---|---|
| 触发词表：引擎只兑现 `AFTER_CREATE`，其余事件写入口当场拒绝并点名 | `WorkflowTriggers` + `WorkflowTriggersTest` **8 例全绿** |
| 绑定行的读侧钉租户、按注册顺序、软删不可见；写侧拒重复/拒不可兑现 | `WorkflowBindingServiceTest` **25 例全绿** |
| 写后派发：2 槽有界、不排队、任何失败都不抛，每次都落一行账 | `WorkflowTriggerDispatcherTest` **17 例全绿** |
| 证据面：新表 `z_lc_workflow_fire`（STARTED/FAILED + instance_id + detail） | 两份 schema 均已追加（`z_lc_workflow_fire` 在 `git show --stat ae07610` 里可见） |
| 接线点：`RuntimeCrudController` 在记录写成功之后调 `workflowTriggerDispatcher.afterCreate(...)` | 同一提交 |
| `WfAdapter` 三处契约错（路径 / body 键名 / 应答读法）已按 z-wf 源码改对 | `WfAdapterTest` **18 例全绿**（本轮 23:10 复测） |
| 出站 HTTP 从未真的发出去过：`HttpRequestDefinition` 是裸 POJO，`getHttpRequestHeader()` 返回 null ⇒ 每个带头的请求 NPE | `CtcAdapter.doRequest` 改为先 `new HttpRequestHeader()`；猎物＝`WfAdapterTest.headersReachTheWire` |
| 应答只能逐格读：`JsonUtil.fromJson(body, Result.class)` 恒抛（`Result` 默认构造 private，引擎不 setAccessible），`TypeReference<Result<…>>` 恒抛 ClassCastException | `WfAdapter` 改用 `JsonUtil.parseObject` + `getBoolean("success")`/`getJsonObject("data")` |
| 状态码不看就等于"远端受理了"：`HttpExecutionResult:47` 库自己写明"success 始终为 true，5xx 不会让 isSuccess()=false" | 新增 `CtcAdapter.httpAccepted(res)`，`WfAdapter` 已接；猎物＝`non2xxIsRefusedEvenWhenTheBodyLooksLikeASuccessEnvelope`（500 + 一份成功信封 ⇒ 必须判失败） |

⇒ **本文件 §2 之后所有事项的前提都已经成立**，剩下的是覆盖面、界面兑现和拍板。

---

## 2. 还欠的工程量（按层给判据，逐层打勾）

### 2.1 HTTP 契约层：`LcHttpContractTest` 一个字都没写过 workflow

实测：`z-lc-web/src/test/java/com/zifang/z/lc/web/it/LcHttpContractTest.java` 共 2389 行，
`grep -c workflow` ⇒ **0**。全仓 java 测试里 `"workflow-binding"` 也 **0 命中**：

```bash
cd z-lc/z-lc-web/src/test/java/com/zifang/z/lc/web/it && wc -l LcHttpContractTest.java && grep -c workflow LcHttpContractTest.java
grep -rln workflow-binding --include='*.java' z-lc-web/src/test z-lc-admin/src   # 期望：无输出
```

要补的断言（一条都不能少，且每条要有反向猎物）：

- [x] 起服务时带 `--z-lc.adapter.wf.base-url=<本地桩>`，**走真 HTTP**：`/api/lc/workflow-binding/create` 存下绑定
      → 运行时 `/create` 写一条记录 → 桩确实收到那一句话（路径逐字 `/api/approval-center/processes/start`、
      body 里 `processKey`/`businessKey`/`initiator`/`title` 四格齐）
      → `GET /api/lc/workflow-binding/fires` 回读出一行 `STARTED` 且 `instanceId` 就是桩给的那个 id。
- [x] 拒绝面逐个回读：`AFTER_UPDATE` / `AFTER_DELETE` / `status_change` / 空 `processDefinitionKey` /
      `autoSubmit=0` / 重复绑定 / 外租户绑定 ⇒ 400 且 reason 说清是哪一格；**运行时随后写记录 ⇒ 桩收到 0 次**。
- [x] `GET /vocabulary` 的形状：可兑现事件列表 == `WorkflowTriggers` 的 implemented 列表（不是常量表）。
- [x] 边界：批量导入、撤销/重做 **不发起**流程 —— 这三条是 §3 第 2 问的既成事实，先钉成守卫再说要不要改。
- [x] 两份 schema 对账：`z_lc_workflow_fire` 在 `z-lc-admin/src/main/resources/db/schema-h2.sql`
      和 `z-lc-web/src/test/resources/schema.sql` 里列集合一致（缺陷 #51/#57 那族"只在真库才红"的前置）。
- [x] `@MapperScan` 覆盖 `com.zifang.z.lc.mapper.workflow` —— 这条只能在契约层量（`WorkflowFireMapper`
      在 `z-lc-core` 里被 `LcModuleDataSource` 的扫描名单漏掉时，单测不会红，只有真起服务才红）。

### 2.2 前端：界面还在给三个兑现不了的选项，且两个回读口零调用

实测（09-26 23:1x）：

```bash
cd z-lc/z-lc-admin-ui/src && grep -rn "AFTER_" views/admin/WorkflowsPage.tsx
# :20 AFTER_CREATE / :21 AFTER_UPDATE / :22 AFTER_DELETE  ⇒ 下拉里仍有后两个
grep -rn "fires\|vocabulary" --include='*.ts*' . | grep -i workflow   # ⇒ 0 命中
```

要改的：

- [x] 触发时机下拉改成**从 `/vocabulary` 派生**（照 `src/api/pipelineVocabulary.test.ts` 的路子，
      新建 `workflowVocabulary.test.ts`）；`AFTER_UPDATE`/`AFTER_DELETE` 若仍在词表里就显示成"无挂接点"，
      不许当可选项 —— 参照 #41 流水线那一族已定形状（`PipelinesPage.test.tsx:193` 断言的正是 `'AFTER_CREATE 无挂接点'`）。
- [x] 列表里的 `autoSubmit` 开关（`WorkflowsPage.tsx:180-181`，默认 `editing.autoSubmit ?? 0`）
      和 `:124` 的 `autoSubmit: 0` 初始值 ⇒ 引擎侧对 `autoSubmit=0` 的判定见 §3 第 3 问，先别自便。
- [x] 写后回读：绑定保存成功但 400 时要把 reason 显示出来（现在 `workflowBinding.ts` 的 4 个口
      只有 list/create/update/delete，**没有** `/fires`、没有 `/vocabulary`）⇒ 补一个"发起账"抽屉。
- [x] 页面文案要写清边界：只有"新建单条记录"会发起，导入/撤销/重做不会。
- [x] 以上都要配 vitest 覆盖。实测：`ls z-lc-admin-ui/src/views/admin/ | grep -i workflow` ⇒ 只有
      `WorkflowsPage.tsx` 一个文件，**没有 `WorkflowsPage.test.tsx`**；
      阳性对照同一条命令口径下 `PipelinesPage.test.tsx` 在（#41 那一族配过）。

本窗实测（09-27 00:16–00:31；首次 vitest "Start at 00:16:28"、mvn 起点 00:28:14、注入台账落盘 00:30:56，都是当轮读到的）：

- 契约层 12 条 = `z-lc-web/.../WorkflowTriggerContractTest`，注入自证 4 支（I1–I4）全出点名红，
  台账在 `~/.cache/zlc_probe/inject61.out`；这一批已随 `3632a59` 推送。
- 界面层新增两份文件 + 一个拆出来的词表模块：
  `src/api/workflowVocabulary.test.ts`（4 条，跨语言对 `WorkflowTriggers.java` 逐字对表）、
  `src/views/admin/WorkflowsPage.test.tsx`（3 条真渲染）、
  `src/views/admin/_workflow.ts`（中文名表 + `useWorkflowVocabulary`，从页面拆出以满足 `react-refresh`）。
  接口层补 `getWorkflowVocabulary` / `listWorkflowFires` / `readVocabulary`（形状不对**抛**，不降级成空清单）。
- 注入自证 9 支（U1–U9）全部出点名红，量具 `~/.cache/zlc_probe/inject61ui.py`、台账 `inject61ui.ledger.json`：
  U1 手抄时机清单复活 / U2 草稿 autoSubmit 退回 0 / U3 400 原因不回显 / U4 摘掉"词表没读到"那道分级 /
  U5 发起记录读失败走空表 / U6 `readVocabulary` 降级成空词表 / U7 中文名表塞进不兑现的事件 /
  U8 词表 hook 咽掉读失败 / U9 删掉边界文案。每支还原后 md5 与原文件逐字节一致。
- 四道闸同轮读数：`tsc --noEmit` rc=0；`eslint src --max-warnings 0` rc=0（改前该条 rc=1、两条
  `react-refresh` 告警，是这条闸的阳性对照）；`vitest run` 全量 **29 文件 / 259 测试 / 259 绿 / 0 红 / 0 跳过**
  （workflow 那两份 7 条在其中）；`vite build` rc=0（3181 modules）；`mvn -o test` rc=0，
  surefire 443 份报告 **4768 测试 / 0 失败 / 0 错误 / 0 跳过**（core 1326 + web 104 与上一窗逐格相同）。

### 2.3 注入自证（缺这一支就不算闭）

- [x] 已建 `_e2e/mutate_workflow_trigger_guard.py`，沿用既有 `mutate_*.py` 的形状（在仓内、带 `_mutlock`、
      台账落 `~/.cache/zlc61/mut/`）。六支 ①–⑥ = 量具里的 M1–M6，**每一支都出具名红**，
      09-27 01:06 整轮实测（`~/.cache/zlc61/mut/logs/guard-0927-010646.log`）：
      `RESULT: java-layer falsification done | 本轮 30 条具名红 / 分母 {'core': 68, 'web': 13}`、
      `restored sources: clean`。逐支读数（core / web）：
      ① M1 摘掉派发 0/10 —— 派发点在 web，core 层结构上看不见它（那条 0 就是一条判定，不是没跑）；
      ② M2 塞 `AFTER_UPDATE` 进 implemented 5/2；
      ③ M3 摘 `listByEvent` 的租户条件 3/1；
      ④ M4 路径改回 `/approval-center/process/start` 2/1；
      ⑤ M5 状态码闸摘回 `!res.isSuccess()` 2/1；
      ⑥ M6 实例 id 读回成 `data.toString()` 2/1。
      还原后两层复跑 68+13 全绿；每支都核过被改文件的 `.class` 指纹相对基线**变了**（编译器真的量了这份变异）。
- 两处**实测推翻预期**的地方，按"改账不改软"处理，都记在量具的文件注释里：
  * M2 第一跑多红一条 `WorkflowBindingServiceTest.updateShouldNotLetAValidRowBeTurnedIntoAnUnhonorableOne`
    —— create 与 update 共用 `validateForWrite`，写入口的两个门一起漏。按名字认领，没有加白名单。
  * M5 第一跑 **web 0 红**：契约层的桩只有"200 + success=false"一种拒绝形状，而 `HttpExecutionResult.isSuccess()`
    对任何完成的响应都为真 ⇒ "判成功看状态码还是看信封"这个决定在运行时无处被检验。
    这一格不是"注入不干净"，是**被测面缺一整格形状**，于是补了
    `WorkflowTriggerContractTest.http5xxWithASuccessfulLookingBodyIsNotAFire`
    （502 + 成功 body ⇒ 必须一行 FAILED、detail 带 `http=502`、不许留下 body 里那个实例号，
    另配"状态码换回 200、body 一字不动 ⇒ STARTED"的阳性对照），契约层从 12 条变 13 条，
    复跑 M5 才出上面那条 web 1 红。
  * 顺带抓出一条**软断言**（已按实测记账，未改）：`WfAdapterTest.httpFailureCarriesStatusAndPath` 里
    "要说清是 http 几"在 M5 下**假绿** —— 失败消息把整个 body 抄进文案，而那个 body 里正好有 "404"；
    真红的是同一条方法里"要带上打的是哪条路径"。文案里混入回显内容 = 断言被写软。
- 量具规则（原文照抄在上）已按实测口径实现：原始字节读进内存、还原只从内存写回 + 逐文件 md5 对账
  （等价于"从副本还原"，且不受 /tmp 被扫影响；**没有**用 `git checkout --`）；锚点逐字唯一否则 SKIPPED；
  分母每轮相等；XML 缺失或 0 用例直接抛（空参照集会打印"满分"）；复跑期间不改被测源码；
  台账 `~/.cache/zlc61/mut/ledger.json` 记 `ran_by`（用户@主机 + pid）与每支 `injected_at`/`mutant_md5`。

### 2.4 接口层 E2E（部署在跑的那个 jar，不是测试进程）

- [x] `_e2e/e2e_api_test.py` 的 `[15w]` 一节（09-27 01:0x–01:3x 写完并量过）。
      开局实测：**18090 上跑的 jar 是 #61 之前的**（nested `z-lc-core` 09-26 18:51、`WfAdapter.class` 4969B、
      根本没有 `WorkflowTriggerDispatcher`）⇒ 从 HEAD 重打（`78aeba1b…`、`WfAdapter.class` 7794B），
      并且**先按字节比对 fat jar 里那个 class 与 `target/classes/` 的**，再信任何读数。
      分母：全量 **532**（其中 `[15w]` **63**，63 = 532−469 与加这一节之前的基线逐条对齐）。
      这一节自己起 z-wf 桩（`http.server` 指 8888，模式 ok/reject/http5xx/hang），断言的形状：
      桥（写一条 ⇒ 桩正好收到一句）+ 报文（路径 / 剪过空白的 processKey / businessKey / title /
      initiator / variables 带 lc* 坐标）+ `/fires` 回读 STARTED 与实例号 + 登记本身不发单 +
      未登记实体一句不发 + 6 次写入口被拒（每次带"点名为什么兑现不了"，且**一行都没落库**）+
      引擎说不了（success=false）/ 5xx 而 body 写成功 / 不可达 三种结局各落一行 FAILED 带原话 +
      换回桩的阳性对照 + 挂死 7s 撞默认 3000ms 预算（实测 3003ms 判 FAILED 且原因点名预算）+
      一次超时不传染下一次 + 批量导入不发单。
      **"桥没通"与"桥通了但断言红"分开记账**：依赖那条链的断言一律走 `wfc()`，桥断了它们打的是
      "这一条没有判定"，不算绿 —— 否则桩起不来时那 30 条会集体开绿灯。
      接口层不托管"跨租户绑定"那一支：`WorkflowBindingController` 把租户归一成 `default`，
      从这个口根本造不出 foreign 行（写进注释，不假造）。
- [x] 同轮注入自证 `_e2e/mutate_workflow_deployed_guard.py`（新量具，第 38 支）。每支都重新 build fat jar、
      用**它**重启 18090、再跑整份接口层；台账 `~/.cache/zlc61/deployed_ledger.json`：
      基线 532/532、分母钉 `e2e_total=532 / section=63`、`bad=0`、`restored=true`、`rerun_green=true`。

      | 注入 | 摘掉的是什么 | 实测红（全部落在 `[15w]` 内） |
      |---|---|---|
      | W1 `RuntimeCrudController` | `afterCreate(...)` 调用点 ⇒ `int fired = 0` | 501/532，**31 条** |
      | W2 `WfAdapter.START_PATH` | 退回 #61 之前那条路径 | 531/532，**1 条**（只红路径那一条：形状对、门牌错） |
      | W3 `CtcAdapter.httpAccepted(res)` | 退回 `res.isSuccess()`（任意完成的响应都算成功） | 530/532，**2 条**（5xx 被当成功那一族） |
      | W4 `data.getString("processInstanceId")` | 退回 `data.toString()` | 531/532，**1 条**（实例号那一格） |
      | W5 派发预算 `future.get(timeoutMs)` | 放大 60 倍（180s） | 528/532，**4 条**（"到点判 FAILED 并点名预算"那一族） |
      | W6 `WorkflowTriggers` 的 `autoSubmit != 1` | 摘成 `if (false)` | 529/532，**3 条**（含"一行都没落库"） |

      两支**记账而非缺陷**的读法：
      ① W5 原本要打的形状是"无上限的 `future.get()`"，**javac 直接拒**（`catch (TimeoutException)`
      变不可达，01:32 那一轮整场战役崩在这里）⇒ 语言替这个洞上了一道闸；改成"预算放大 60 倍"之后
      "到点判 FAILED"红，而"写入口在预算内返回"**没红** —— 因为它另有一根独立的桩（`WfAdapter`
      传输层 socket = `timeoutMs + 500ms`，W5 碰不到）。两道界各名下各的检查，不是量具漏判。
      ② W6 摘闸后落库那行 `auto_submit=0` 被 `listByEvent` 的 `auto_submit = 1` 挡在发起之外 ⇒
      写入口那道管"别让装饰进库"、读侧那道管"别让它发单"，两层各有名。
      量具规则照 §2.3 那份：字节快照 + md5 对账（**不用** `git checkout --` 当还原步）、anchor 逐字唯一、
      每轮先证明 fat jar 里**恰好一个 `.class` 变了**（指纹没变=这一轮什么都证明不了，记坏账）、
      分母漂了当场判"没红"不成立、还原轮必须回到基线字节并 532/532 回绿、`_mutlock` 全局互斥、
      崩溃走 `except` 记账而不是被 `finally` 洗成 "RESULT: done"。
      一个新量的到的教训（写进 README）：**检查的名字必须是稳定身份** —— 头两轮有两条名字里插了
      本轮才有的值（时间后缀、实测毫秒数），"预期红集"就没法逐字比对；把值挪进 detail，名字常量化。

### 2.5 浏览器层

- [ ] Playwright 那一层目前对 `WorkflowsPage` 零断言。要测：下拉里**没有**兑现不了的选项、
      保存 400 时 reason 看得见、"发起账"抽屉能读出那一行。

### 2.6 250 真库（本轮实测的缺口）

- [ ] `z_lc_workflow_fire` **在 250 的真 MySQL 8 里还不存在**：
      `select table_name … where table_name like 'z_lc_workflow%'` ⇒ 只有 `z_lc_workflow_binding`（09-26 23:1x 实测）。
      要么重跑 `deploy_250.sh schema`，要么补一条 `CREATE TABLE IF NOT EXISTS` 上线脚本 —— 走哪条要按 #51 的口径定，
      注意 `init.sql` 里有 15 条 `DROP`，不可照跑。
- [ ] **250 上没有 z-wf 在跑**（`docker ps` 实测：只有 mysql/z-ctc/z-vector/z-graph/registry），
      而 `z-lc.adapter.wf.base-url` 默认 `http://localhost:8888` ⇒ 现在部署起来这条链**必然**落 `FAILED` 行。
      这一支是好事（能证明账是真的），但要把"起一个真 z-wf"还是"部署期只跑桩"当结论写进 README，别留成谜。

---

## 3. 等你（主编/CEO）拍的四问 —— 我先按默认值做了，但默认值**不算裁定**

1. **范围**：本轮只兑现"记录新建之后"（`AFTER_CREATE`），`AFTER_UPDATE`/`AFTER_DELETE` 写入口直接 400。
   ⇒ 接受"配置项比引擎能力少"，还是要我把更新/删除两条派发点也接上？
   接的代价：`update`/`delete` 都要在事务提交后挂一次外部调用，且撤销/重做路径要一起判（否则删一条记录会误发一次流程）。
2. **批量导入 / 撤销重做算不算触发点**？默认**不算**，并已在 §2.1 里钉成守卫。
3. **`autoSubmit=0` 的语义**。库里这一格是历史遗留（界面默认 0、引擎读出来只当"这条绑定不自动发起"）。
   默认处理：`autoSubmit=0` 的绑定在写入口就被拒（"这条绑定不会自动发起，请删掉它"）。
   ⇒ 还是"存着但永远不派发"（那就退化成缺陷 #61 的同一形状，我不建议）？或者把这列从界面上彻底摘掉？
4. **发起失败要不要回滚用户那条记录**？默认**不回滚**（外部引擎不可用不该把用户的写入带走），
   账落在 `z_lc_workflow_fire` 的 `FAILED` 行里。
   ⇒ 若要求"审批流是硬约束"，就得改成"发起失败 ⇒ 写入口 400 且记录不落"，那是另一种产品形状。

---

## 4. 收口口径（照 #41/#48 那一族的既有标准）

- 六层同窗重测：java 全量（本轮 `z-lc-core` **1326 例 / 0 失败 / 0 错误 / BUILD SUCCESS**，以最新一跑为准）、
  契约层、vitest、`_e2e` 接口层、浏览器层、注入自证 —— **同一窗口内都出数才算闭**，跨窗口相减不算。
- 250 那条腿必须再跑一次（§2.6），且 `healthproof` + `gates` 两步不能跳。
- `_e2e/README.md` 加一行 #61，数字现数；项目记忆 `project-z-lc-lowcode-state.md` 同步（#52/#59/#60 已闭、
  #61 状态、HEAD 现测）。
- 提交只 `git add` 本批自己的路径（共享工作树），**不要**带 `z-lc-admin-ui/pnpm-lock.yaml` / `pnpm-workspace.yaml`。
  ⚠ `ae07610` 那一笔（另一会话 20:09 代提交）已经把这两个文件推进了仓库，要不要回退单独问用户。

## 取数命令

```bash
cd z-lc && git rev-parse --short HEAD
mvn -o -B -pl z-lc-core test 2>&1 | grep -E "^\[INFO\] Tests run: [0-9]+, Fail" | tail -1
grep -c workflow z-lc-web/src/test/java/com/zifang/z/lc/web/it/LcHttpContractTest.java
grep -rn "AFTER_" z-lc-admin-ui/src/views/admin/WorkflowsPage.tsx
ssh 192.168.31.250 'docker ps --format "{{.Names}}"; '   # 看 z-wf 在不在
```
