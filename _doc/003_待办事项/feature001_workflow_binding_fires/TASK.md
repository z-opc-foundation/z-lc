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

- [ ] 起服务时带 `--z-lc.adapter.wf.base-url=<本地桩>`，**走真 HTTP**：`/api/lc/workflow-binding/create` 存下绑定
      → 运行时 `/create` 写一条记录 → 桩确实收到那一句话（路径逐字 `/api/approval-center/processes/start`、
      body 里 `processKey`/`businessKey`/`initiator`/`title` 四格齐）
      → `GET /api/lc/workflow-binding/fires` 回读出一行 `STARTED` 且 `instanceId` 就是桩给的那个 id。
- [ ] 拒绝面逐个回读：`AFTER_UPDATE` / `AFTER_DELETE` / `status_change` / 空 `processDefinitionKey` /
      `autoSubmit=0` / 重复绑定 / 外租户绑定 ⇒ 400 且 reason 说清是哪一格；**运行时随后写记录 ⇒ 桩收到 0 次**。
- [ ] `GET /vocabulary` 的形状：可兑现事件列表 == `WorkflowTriggers` 的 implemented 列表（不是常量表）。
- [ ] 边界：批量导入、撤销/重做 **不发起**流程 —— 这三条是 §3 第 2 问的既成事实，先钉成守卫再说要不要改。
- [ ] 两份 schema 对账：`z_lc_workflow_fire` 在 `z-lc-admin/src/main/resources/db/schema-h2.sql`
      和 `z-lc-web/src/test/resources/schema.sql` 里列集合一致（缺陷 #51/#57 那族"只在真库才红"的前置）。
- [ ] `@MapperScan` 覆盖 `com.zifang.z.lc.mapper.workflow` —— 这条只能在契约层量（`WorkflowFireMapper`
      在 `z-lc-core` 里被 `LcModuleDataSource` 的扫描名单漏掉时，单测不会红，只有真起服务才红）。

### 2.2 前端：界面还在给三个兑现不了的选项，且两个回读口零调用

实测（09-26 23:1x）：

```bash
cd z-lc/z-lc-admin-ui/src && grep -rn "AFTER_" views/admin/WorkflowsPage.tsx
# :20 AFTER_CREATE / :21 AFTER_UPDATE / :22 AFTER_DELETE  ⇒ 下拉里仍有后两个
grep -rn "fires\|vocabulary" --include='*.ts*' . | grep -i workflow   # ⇒ 0 命中
```

要改的：

- [ ] 触发时机下拉改成**从 `/vocabulary` 派生**（照 `src/api/pipelineVocabulary.test.ts` 的路子，
      新建 `workflowVocabulary.test.ts`）；`AFTER_UPDATE`/`AFTER_DELETE` 若仍在词表里就显示成"无挂接点"，
      不许当可选项 —— 参照 #41 流水线那一族已定形状（`PipelinesPage.test.tsx:193` 断言的正是 `'AFTER_CREATE 无挂接点'`）。
- [ ] 列表里的 `autoSubmit` 开关（`WorkflowsPage.tsx:180-181`，默认 `editing.autoSubmit ?? 0`）
      和 `:124` 的 `autoSubmit: 0` 初始值 ⇒ 引擎侧对 `autoSubmit=0` 的判定见 §3 第 3 问，先别自便。
- [ ] 写后回读：绑定保存成功但 400 时要把 reason 显示出来（现在 `workflowBinding.ts` 的 4 个口
      只有 list/create/update/delete，**没有** `/fires`、没有 `/vocabulary`）⇒ 补一个"发起账"抽屉。
- [ ] 页面文案要写清边界：只有"新建单条记录"会发起，导入/撤销/重做不会。
- [ ] 以上都要配 vitest 覆盖。实测：`ls z-lc-admin-ui/src/views/admin/ | grep -i workflow` ⇒ 只有
      `WorkflowsPage.tsx` 一个文件，**没有 `WorkflowsPage.test.tsx`**；
      阳性对照同一条命令口径下 `PipelinesPage.test.tsx` 在（#41 那一族配过）。

### 2.3 注入自证（缺这一支就不算闭）

- [ ] 新建 `_e2e/mutate_workflow_trigger_guard.py`，沿用既有 18 支 `mutate_*.py` 的形状。
      至少这几支，**每支都要出红**：
      ① 摘掉 `RuntimeCrudController` 里的 `afterCreate(...)` 那一行 ⇒ 派发账 0 行（这一支同时是"接线还在"的复证）；
      ② 把 `WorkflowTriggers` 的 implemented 列表塞进 `AFTER_UPDATE` ⇒ 词表/拒绝面两支同时红；
      ③ 摘掉 `listByEvent` 的租户条件 ⇒ 外租户越界那支红；
      ④ 把 `WfAdapter` 的路径改回 `/approval-center/process/start` ⇒ 路径那支红；
      ⑤ 把状态码闸摘回 `!res.isSuccess()` ⇒ 500-信封那支红；
      ⑥ 把 `data.processInstanceId` 读回成整个 map 的 `toString()` ⇒ 实例 id 那支红。
- 量具规则：注入前先 `cp` 副本、还原只从副本 `cp` + `md5` 对账（**不许用 `git checkout --` 当还原步**），
  复跑期间不改被测源码，台账要记"谁跑的"。

### 2.4 接口层 E2E（部署在跑的那个 jar，不是测试进程）

- [ ] `_e2e/e2e_api_test.py` 目前 `grep -c workflow` ⇒ **0**。要补：真 jar 上 create 绑定 → 写记录 →
      `/fires` 回读 STARTED；桩指向本机一个端口，验证"桩不可达时 FAILED 行落得下来、而记录仍然写成功"。

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
