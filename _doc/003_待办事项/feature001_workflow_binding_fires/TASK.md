# feature001 · 缺陷 #61 流程绑定：剩下没做完的工程量与等拍板的五问

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

- [x] Playwright 那一层对 `WorkflowsPage` 的断言（新量具 `z-lc-admin-ui/e2e/mutate_workflow_browser_guard.py`，
      套件里第 11w 段）。09-27 03:5x 实测：**基线一轮 PASS 279 / FAIL 0**，11w 段内 **59 条**检查
      （`validate()` 扫源码取的分母，不抄清单；其中 23 条过 `wfCheck` 那道"桥没通就不算绿"的守卫）。
      测的面：
      - 词表 → 界面：下拉里只有 `/vocabulary` 给的时机、个数与词表相等、被拒清单连同引擎那句理由摆在窗里、
        词表读失败时那一行标「时机未校对」而不是「引擎不兑现」+ 横幅给重试入口 + 新建按钮收住；
      - 写：400 时接口那句原因**原样**出现在 toast 里（不是笼统"保存失败"）、保存后那一行三格齐、
        与库里读回的那一条逐字相同；
      - 账（抽屉）：一行 STARTED 的实例号/记录号/不写失败原因各按**格**读（不按整行 blob —— 见下面被证伪的那条）、
        桩不可达那一条 FAILED 说得出为什么、FAILED 不覆盖上一条 STARTED、三行两成一败；
      - (7b) 引擎**答了但没成**两种形状：200 + `success:false`（理由那句原样落到「为什么」）与
        502 + 成功样的 body（`ghost-should-not-be-kept` 那一个号不许进账）—— 桩里这两个分支此前没人翻过旗，
        于是"实例号必须是空的"那句只在"收不到 body"的成因下测过，是个没有猎物的字样；
      - 读失败不许画成"没有"：`page.route` 把 `/fires` 打断后抽屉报"没有读到"、旧行不能继续亮着；
      - 收尾：解绑真的让行下去 + 「已删除」+ 本节种的应用收掉 + 解绑与删应用一句都不发（清理前 4 句钉死）。
      注入自证 **18 支**，每支只摘一句保证、红集合互不相同；另有 **27 条按未覆盖记账**，分三类理由
      （(a) 夹具/桥/阳性对照，(b) 会让整节级联的写形状 —— 牙在 vitest 与 deployed 层，(c) 数据在服务端那一侧）。
      W4/W5 是**成对**的：判据落在差集恰为一条「读失败那一屏也不留下三行账的假象」上，
      单独一支 W4 证明那条不是它的替身。
      跑一轮实测的红数（第二轮 04:3x–04:4x，`~/.cache/zlc61/browser_guard/run2.out`；读数由**独立判读脚本**
      `~/.cache/zlc61/judge_run2.py` 从每轮原始日志现量，不拿预期清单当读数）：基线 **279 条 / 红 0**；
      W1 3、W2 3、W3 1、W4 1、W5 2、W6 2、W7 3、W8 1、W9 1、W10 2、W11 2、**W12 6**、W13 5、W14 1、
      W15 1、W16 1、W17 1、W18 3 ⇒ **18 支全部与账目逐字对上**（每支红的条数 = 记账条数，无一支空跑、
      无一支多红）；W4/W5 的成对判据实测成立：`W5\W4 差集恰 1 条`（「读失败那一屏也不留下"三行账"的假象」）
      且 `W4\W5 反向 0 条`。
      第二轮收出来的两处不符都不是产品的账，一处改预期、一处修量具（断言一条没改软）：
      4. W12 多红「引擎那句拒绝理由原样落在「为什么」那一格」⇒ **预期漏记**：那条断言的合取里确实写着
         `wfRow4[1] === 'FAILED'`（`browser-e2e.mjs:3137`），而 W12 摘的正是那一格的字样；读数里「为什么」
         那一格完好 ⇒ 不是替身红。按实测把 W12 的账从 5 改到 6，合取保留（它钉的是"200 而 success=false
         这一行得自称 FAILED"）。同一条断言同时挂在 W12/W13/W18 名下 = 它读了三格，而三支的**整集**
         6/5/3 互不相同，判据仍分得开。
      5. 「恢复后复跑」那一轮 `总 2 / 红 1（基线 279）` ⇒ **量具自己撞名**，被报成了产品红：
         `seedTestData()` 的 appCode 是 `uitest${Date.now().toString().slice(-6)}`（= epoch ms mod 1e6，
         **每 1000s 回绕**），而物理表名取 `ui_task${appCode.slice(-4)}`（**每 100s 回绕**）；连跑 20 轮
         （>3000s）后恢复轮的 `uitest429924` 撞在基线轮留下的 `uitest239924` 已占的 `ui_task9924` 上 ⇒
         服务端拒建实体，而 `seed` 只 `console.error` 一行就继续跑 ⇒ 界面塌下来的是「表格渲染」那一条超时红、
         后面整节没有数据可读。05:02 复测：这一对撞名**此刻还在盘上**（`app/list` 里 `uitest429924` 与
         `uitest239924` 后 4 位同为 `9924`）。修在量具侧三处：appCode 加 4 位随机后缀、表名从完整 appCode
         派生、实体建不成立刻 `throw`（"这一轮作废（环境残留不是产品缺陷）"）而不是一行日志放行。
         源码恢复本身是干净的（`WorkflowsPage.tsx` 回填后 md5 `87b30e33161f27089623d260151e12fd`、
         产物指纹回到基线 `index-ruBdvAdn.js`）⇒ 那一轮的红只归到 seed 撞名，不归到"变异没还原"。
      - [ ] **残留另记一笔**：seed 的应用从来没人删（05:0x 实测 `app/list` 共 **99** 个应用，其中 `uitest*`
            **50** 个）—— 清了能降撞名概率，但堵掉"撞名必假红"这条路的是上面那处量具修复，不是清理；
            两者别混。**这一条不属于 #61**，是给套件收活的人留的。
      - [ ] 同一把尺的第二处（09-27 05:5x 现读，**记为"不是缺陷"而不是新缺陷**）：`11w` 那节自己建应用用的名字
            是 `wfui${Date.now().toString().slice(-6)}`（`browser-e2e.mjs:2703`）、物理表名取
            `ui_wf${wfApp.slice(-5)}`（:2712 = ms mod 1e5，**每 100s 回绕**）—— 形状和上面撞过的那对一模一样。
            区别在结局：这一节的应用/实体/provision 三步任一失败都当场 `throw` 并写明
            「11w 建应用失败 / 建实体失败 / 表没建成」（:2708/:2718/:2720-2722），**不会穿着产品红的外衣进账**。
            ⇒ 代价是白跑一轮，不是假绿/假红，本轮不改（改它要动在飞量具的源，见下）。收活的人若要根治，
            和 `uitest` 那处一起换成同一套带熵的命名即可。
      - [x] 第三轮（18 支整族，跑在改完账 + 修好撞名的量具上，05:02:05–06:00，
            `~/.cache/zlc61/browser_guard/run3.out`，日志已归档到同目录 `run3_logs/` 44 份）落盘读数：
            **`OK 18/18`、具名红 39 条、`出现预期之外的红` 0 次、`本轮分母` 不符 0 次、
            每轮分母都是 279、`RESULT: 1 problem(s)`**。
            39 这个数是量出来的不是加的：`grep -oE "预期 [0-9]+ 条全红" | uniq -c` = 8×1 + 4×2 + 4×3 + 1×5 + 1×6
            = 39，与 `grep -c "RED (预期)"` 的 39 相互对上。
            **那 1 条问题不是产品坏了、也不是变异没还原 —— 是我这只尺自己没有猎物（缺陷 #69，见 §2.11）**。
            第二轮欠的那处「恢复轮」到这里闭了：撞名修好后恢复轮是 `PASS+FAIL 279 / 红 0`（基线也是 279），
            恢复轮的源码 `WorkflowsPage.tsx` md5 与第二轮逐字节相同（`87b30e33161f27089623d260151e12fd`）。
      第一轮（03:0x–03:2x，`~/.cache/zlc61/browser_guard/run1.out`）**RESULT: 3 problem(s)**，三条全是
      我这只量具自己的毛病，没有一条是产品坏了，也没有一条靠加白名单抹平：
      1. W13（「记录」那一列画事件码）预期红 2 条却红 0 条 —— 那两条当时写的是"整行 blob 里
         `includes(String(recordId))`"，而时间那一格里全是数字、记录号是 1/2/3 这种小数，一蹭就中
         ⇒ 检查是空的。改成按 `td` 读第一格后 W13 才有猎物（顺带把 W13 的红集扩到 5 条：靠记录号认领行的
         那三条一起塌 —— 认不出行就没有"哪一行的理由"）。
      2. W12（结果那一格画「—」）多红了一条没预期的「FAILED 那一行不许留下流程实例号」—— 当时靠行里的
         `FAILED` 字样认领那一行，摘掉字样就"找不到那一行"而红：**替身红**。改成按
         `data-testid="fire-status-FAILED"` 认行、再按格读第三格 ⇒ W12 不再红它，它归到 W7 名下。
      3. W16 第一版（`!== 'ready'` → `!== 'loading'`）红 0 条：ready 态下上面 `implemented.includes(event)`
         先命中，这一支结构上走不到 ⇒ **等价变异**。换成"未校对挂到了已兑现那一行"才打得到恢复态那条。
      ⚠ 仍未覆盖的一条，浏览器层动不到：`/fires` 没有分页参数，而 `WorkflowBindingService.listFires`
      结尾是 `orderByDesc("id").last("LIMIT 200")` ⇒ 第 201 条账**静默消失**（界面上"这个实体还没有
      发起记录"那一类空态永远撞不到它）。要修得先给接口加 page/size，那是另一支的活。

### 2.6 250 真库（本轮实测的缺口）

- [x] `z_lc_workflow_fire` **已建进 250 的真 MySQL 8**（09-27 05:0x 走 `deploy_250.sh schema`：
      先把 `z-lc-admin/src/main/resources/db/schema-h2.sql` 单独 scp 上去并**逐字节对账**
      （local=remote=`c7898003549cf25d447b723bdf559fa9`，`grep -c` 现量 15 条 `CREATE TABLE`、0 条 `DROP`），
      再灌 —— 库里表数 **71 → 72**；`information_schema` 回读这张表 **14 栏**
      （id/tenant_code/app_code/entity_code/record_id/binding_id/trigger_event/process_definition_key/
      status/instance_id/detail/create_time/update_time/deleted），校对随库为 `utf8mb4_general_ci`，
      与 `z_lc_workflow_binding` 一致 ⇒ 闸 3 不会因这一张新开洞。
      （"15 条 DROP 不可照跑"说的是另一个文件 `init.sql`。）
      ⚠ 只 scp schema 不 scp jar：那一次 `sync` 会把 `target/` 里**正被 18090 进程按需读取**的 jar 原地重写，
      边跑边换会让在飞的测量读出与代码无关的红 —— 所以 `sync` 走 jar 那条路要等在飞的那轮收线。
- [ ] **250 的 `:8888` 不是 z-wf，是别人的服务**（09-27 04:0x 实测：`ss -ltnp` 显示 `*:8888` 由 pid 1622
      那个 java 持有 —— `z-opc-main-starter`，已跑 1 天 16 小时；对它 POST `/api/approval-center/processes/start`
      回的是 **404 + 一段 Tomcat HTML**）。而 `z-lc.adapter.wf.base-url` 的默认值正是 `http://localhost:8888`
      ⇒ **把当前这一版 jar 部署上去而不动这个参数，就等于每写一条记录都往别人在跑的服务发一次 POST**，
      账上还落一条"应答不是可解析的 JSON/404"的 FAILED —— 那个原因不是产品的结论，是我打错了门。
      ⇒ 250 这一腿开火前必须显式带 `--z-lc.adapter.wf.base-url=<自己起的桩>`（或真 z-wf），
      README 里写死是哪一种，不许留默认值。
      **落点已定位**（09-27 04:1x 现读）：`_e2e/deploy_250_remote.sh:163-176` 那段 `nohup java -jar` 里
      `ctc` / `meta` / `script` 三个 adapter 都显式给了 `http://localhost:$APP_PORT`，**唯独 wf 没有**
      ⇒ 今天这份部署脚本原样跑，就是把 wf 留在默认值上。补一行 `--z-lc.adapter.wf.base-url="$ZLC_WF_BASE_URL"`，
      并且**没有默认值就 die**（"忘了带参数"要红在部署当场，而不是红成一条 FAILED 账）。
- [ ] 04:1x–04:2x 复测（同一把尺第二次落到盘上）：`z_lc` 里仍是 **71 张表 / 只有 `z_lc_workflow_binding`**、
      `*:8888` 仍在监听、**18888 空着**（桩可以起在这一格，不跟 8888 上那个别人的 starter 抢）。
- [ ] 250 现在跑的是 **缺陷 #61 之前**的 jar ⇒ §2.6 的测试必须先从**当前提交树**重打 jar 再部署（并同步抬
      z-boot 1.0.16 的依赖，见下面"取数命令"旁边的注意），否则测的是旧行为。
      05:1x 复测（**换了尺**）：`~/zlc-deploy/lib/z-lc-admin-1.0.0-SNAPSHOT.jar` md5 仍是
      `bb48651152a723dc2651ecd3aeb28ba4`、mtime 09-26 18:56；把它里面那个 **嵌套的**
      `BOOT-INF/lib/z-lc-core-1.0.0-SNAPSHOT.jar` 抽出来 `unzip -l` ⇒ Workflow 类只有
      `WorkflowBindingEntity/Mapper/Service` + `WorkflowTemplateDO`，
      `grep -cE "WorkflowTriggerDispatcher|WorkflowFireEntity"` = **0**；
      而本机 `target/` 那个 01:43 的 jar 同一把尺量出来是**有**这两个类的（数到 5 个匹配）。
      ⚠ 上面这条"抽嵌套 jar"的动作不是洁癖：**前一版这条账记错了尺** —— `unzip -l 外层 fat.jar | grep -c 类名`
      对 Spring Boot fat jar **结构上看不见任何应用类**（它们都在 `BOOT-INF/lib/*.jar` 里，外层清单只有那些
      嵌套 jar 的名字），所以它对外层数出的永远是 0 ⇒ 这条判据**永远不可能判"已经部署了"**，
      是个没有猎物的尺。仓里已提交的六支 deployed 守卫本来就都对嵌套 jar 取字节（`mutate_*.py` 里那句
      `namelist() startswith "BOOT-INF/lib/z-lc-"`），是我这条临时命令没跟着仓库的做法走。
      结论方向没变（旧构件是真），证据换成可判正的那一把。
    - **09-27 06:0x 量"要不要为这一腿重打 jar"：不用 —— 本机 `target/` 那支就是当前提交树的 java**。
          四条各自独立：① `git status --porcelain` 里 `.java` 一条都没有（树 == HEAD `ce81f49`）；
          ② `git log --since="2026-09-27 01:44:00" -- '*.java'` **空**（jar 建好之后没有任何 java 提交，
          那一窗只进了一支 `ce81f49`，动的是文档与量具）；③ `find -name '*.java' -newer <jar>` 点到的 4 支
          mtime 齐刷刷是 **01:44:34** —— 那是 java 侧 deployed 守卫的**还原步**（`~/.cache/zlc61/deployed-guard-0927-013839.log`
          尾行逐字 `restored sources: clean`），不是有人改了内容 ⇒ **别拿 mtime 当"内容变了"的尺**，
          我差点这么误判；④ 决定性那条来自守卫自己：它的最后一步是 `restore: rebuild pristine, restart, re-run`，
          并把**现在还在跑的 pid 72030** 归因成 `z-lc-admin-1.0.0-SNAPSHOT.jar`、写着
          `artifact 回到基线字节: True`、`e2e = 532/532 本节 63 failures=(none)`。
          我再独立读了一次运行时行为（只读 GET）：`/api/lc/workflow-binding/vocabulary` 回
          `implemented:["AFTER_CREATE"]` + 六条带理由的 `rejected` —— W6 那一支注入（改 `WorkflowTriggers.java`）
          若留在盘上，这一句不可能长这样。
          尺换对之后的数（这次用嵌套 jar 那一把）：fat `af13088981968b1fc662fc202e0bc4c9`、
          嵌套 `BOOT-INF/lib/z-lc-core-1.0.0-SNAPSHOT.jar` = `e4a21211a943319fd151e6bd4355d71f`、
          其中 workflow 类 5 个匹配，逐条原文（都盖着 `09-27-2026 01:43` 的编译时刻）：
          `workflow/WorkflowTriggerDispatcher.class`、`…Dispatcher$1.class`、`…Dispatcher$2.class`、
          `workflow/entity/WorkflowFireEntity.class`、`workflow/WorkflowTriggers.class`
          —— 对照 250 上那支 `bb48651152a723dc2651ecd3aeb28ba4`（09-26 18:56）
          ⇒ md5 不同，`sync` 会真的搬东西过去。**这一腿省掉一次全量 `mvn package`**（它既要抢 CPU、
          又会把在飞浏览器门禁脚下的 jar 换掉）。
    - 🔴 **06:2x 复测：250 仍旧 blocked，但读数比 05:5x 那一趟更具体**（`python3` 逐个 `connect()` 现量，
          区分"被拒"与"超时"是这一条的全部价值 —— 05:5x 那趟只有 `nc -z`，两种失败分不开）：
          `22 CONNECTED`（接 TCP，但 `ssh` 仍旧 `kex_exchange_identification: read: Connection reset by peer`，两试同果）、
          `18098 CONNECTED`（z-schedule 那套常驻还在服务）、
          `18090 REFUSED`（我部署的 z-lc 没了）、`33061 REFUSED`（我的 `z-lc-deploy-mysql` 没了）、
          `33060 REFUSED`（**别人的** `z-schedule-e2e-mysql` 也没了）、`18888 REFUSED`（我的桩没了）。
          `ping -c 4` = 4 包丢 1（25%），rtt 稳在 `1.416/1.477/1.538 ms`。对照：同一时刻
          `192.168.31.136:22` 连通且能执行（`06:28:00 up 5:42, load 0.21`）⇒ 我这侧网没事，250 难受。
          **形状读法（这是观察，不是归因）**：宿主机上的 java 还活着（18098）而**两个容器口一起被拒**
          ⇒ 像是容器运行时/那一层出事，不像是整机重启；我不写死，因为拿不到管理面就没法证。
          ⚠ 明确不归因到我这一窗：我在 250 上最后一次动作是 05:2x 那几支负控，
          `deploy_250_remote.sh` 的 `pkill` 只打 z-lc 自己的进程名，从不碰 33060 那一套；
          本机群里同时有别的战役在打 250（z-mq W2h 04:07 派、z-rpc/z-graph 都在 250 跑全量）。
          **这一条属于用户的基建，不由我代修，也不由我重启。**
          对闸 6 的直接影响，把 05:5x 那句"桩还等着我收"改掉：18888 现在是被拒的 ⇒
          **机器缓过来之后先看的不是"清掉我的桩"，而是"我的桩、我的 app、我的库容器三个都不在了"**
          —— 这一腿要从 `sync` → `env` → 起桩 → `start` 起重做，不是接着跑 `fireprobe`。
          仍成立的只有那句：闸 6 正向那一跑（期望 23 条全绿）**没跑，也跑不了**，
          这一条不许被任何"部署已验"的话覆盖。

    - 🔴 **250 这条腿现在整体 blocked（09-27 05:5x–06:0x 实测，不是"我没排上"）**
          —— 下面是那一趟的原账，保留作出处；**06:2x 的复测在它上面那一格**（两口新读数与"我这侧网是干净的"对照都在那里）。
          `ssh 250` 四次全在密钥交换阶段被重置 —— 逐字 `kex_exchange_identification: read: Connection reset by peer`
          / `Connection reset by 192.168.31.250 port 22`。TCP 22 本身接得住（`nc -z -G 5` succeeded），
          而我认识的那三个服务口这一趟全是 closed/filtered：`18090`（部署的 z-lc）、`33061`（我的
          `z-lc-deploy-mysql` 容器）、`18098`（z-schedule 常驻）。⇒ 拿不到管理面，`sync`/`start`/`fireprobe` 一步都跑不了。
          **归因排除在盘外**：同一张网卡（en0）同时打另外两台 —— `192.168.31.136` 10 包 0 丢、
          `min/avg/max/stddev = 0.466/0.640/0.724/0.085 ms`；路由器 `.1` 6 包 0 丢、`0.471/0.522/0.597/0.041`；
          而 `192.168.31.250` 20 包**丢 1（5%）**、`1.306/40.914/255.763/61.382 ms`。
          我这侧的链路是干净的，抖动与丢包只在 250 身上 ⇒ 机器难受，不是网难受。
          后果写清：**闸 6 正向那一跑（期望 23 条全绿）没有跑，也跑不了**；这一条不许被任何"部署已验"的话覆盖，
          §2.6.3 那处"未闭合"里属于 250 的部分照旧未闭合。我留在 250 上的桩（pid 11408、`127.0.0.1:18888`）
          现在既关不掉也够不着 —— 机器缓过来之后第一件事是 `ss -ltnp | grep ":18888 "` 确认它还在，
          然后按 §2.6.3 的收尾把它收掉（它是我的进程，别留给下一跑当"别人的端口"）。
          ⚠ 这是本机群里第二台出现"ping 得到、管理面进不去"的盒子（前例：zifang002 硬死机，
          靠 `arm-watchdog.service` 才 69s 自愈）。**这一条属于用户的基建，不由我代修，也不该由我重启。**
- [ ] **`deploy_250.sh api` 这一条走不通，且不通在"桥"上**（09-27 05:0x 现读 `_e2e/e2e_api_test.py`）：
      第 `[15w]` 节的桩是**测试进程自己**起的 —— `WF_PORT = int(os.environ.get("LC_WF_STUB_PORT","8888"))`
      (:2134)、`_WfStub(WF_PORT)` (:2193/:2434) 绑在**跑脚本这台机**的端口上，而 app 的 BASE 是 `sys.argv[1]`
      (:21，默认 `http://localhost:18090`，250 腿会换成隧道口)。把 BASE 指到 250 之后，250 上那个 jar 发的是
      **它自己的** `localhost:8888`（也就是 pid 1622 那个别人的 starter），永远打不到我本地的桩 ⇒
      `WF_LIVE = bool(WF) and wf_count() == 1` (:2315) 恒假 ⇒ 这一节所有走 `wfc()` 的断言整排判红、
      detail 写「桥没通（桩没起来或 jar 没打过来）⇒ 这一条没有判定，不算绿」。
      记清两件事：①**这是真红不是假绿** —— :2121 那句注释定的规矩就是"桥没通就没有绿"，
      负断言（"一条都没发"）在这种形状下不会蒙过去；②但**63 条整排红不是 250 这条腿的结论**，
      它只说明这把尺的桩和 app 不在同一台机上。⇒ 250 这一腿**不要**跑整节 `[15w]`，
      改跑一支聚焦的真库探针：桩起在 250（`_e2e/wf_stub.py`，见 §2.6.2）、`ZLC_WF_BASE_URL` 显式指它、
      写一条记录后**从 MySQL 自己**读回 `z_lc_workflow_fire` 的行（STARTED 与 FAILED 各一），
      再经 `/fires` 读回同一份，最后清场。
- [ ] ⚠ **09-27 05:4x 把"线上那套库在哪"量成了一个否定结论**：本会话早先有一条账写着"`z_opc_lc` 是线上库、
      里面有 29 行绑定、它在 136 上"。今天按这条线索去复跑，四把尺都落空：
      ① `nc -z -G3 192.168.31.136 3306` → rc=1（端口不通本机）；
      ② `ssh zifang@192.168.31.136`（这台机就是 `zifang002`，`uptime` 显示 00:45 才起来）
         → 无 `docker`（`/usr/bin/docker`、`/usr/local/bin/docker` 都不存在）、无 `/opt/zopc`、
         `ss -ltn` 上没有任何 3306/1809x/8080 监听，running units 里只有 `k3s-agent`；
      ③ 250 上 `docker ps` 只有 `z-lc-deploy-mysql`(33061) 与 `z-schedule-e2e-mysql`(33060)，
         前者的 `show databases` 是 `z_lc` 一个（05:4x 实测），没有 `z_opc_lc`；
      ④ 250 的 k8s 全集群 `kubectl get pods -A` 共 **3** 个 pod，名字里没有 mysql/zopc。
      另注一条量具账：我中途把 ③ 之前的一次 `ss -ltn | grep -c ":3306"` 读成"250 上有 3306"，
      那是**子串假阳性**（`:33060`、`:33061` 都含 `:3306`）—— 换成 `grep ":3306 "` 与 `docker ps` 的端口列
      同时看，250 上并没有对外暴露在 3306 的引擎。
      ⇒ 所以"**#61 的 DDL 要落到哪一库**"今天**没有正面答案**，只有一张否定结论的账；能演示的 MySQL 8
      就是 250 上我起的这一套（§2.6.2/§2.6.3）。这一问归用户：线上/常驻的那套 z-lc 到底部署在哪台机、
      哪一库，我不去猜着 ALTER 别人的库。

#### 2.6.1 "起真 z-wf 还是只跑桩"这一问，04:2x 已经量到答案的一半

工单原文要求"别留成谜"。09-27 04:2x 实测：**真 z-wf 在同一.foundation 里，而且契约是对得上的**：

- `z-opc-foundation/z-wf`（4 模块 admin/core/starter/web，git HEAD `cc1a21b`）里
  `z-wf-web/.../ApprovalCenterController.java:617` 就是 `@PostMapping("/processes/start")`，
  `:620` 收 `StartProcessRequestDTO`、回 `Result<Map<String,String>>`，`:664` 往 map 里放的是
  `processInstanceId` ⇒ 与 `WfAdapter` 请求/解析的那几个字段同名（不是"看着像"，是逐字段读出来的）。
- **桩的 `reject` 那一支是从真引擎抄来的**：`:672-673` `catch (Exception e) { return Result.fail("流程启动失败: " + e.getMessage()); }`
  ⇒ 失败时 HTTP **200** + 信封 `success=false` + message 前缀 `流程启动失败: `。这正是浏览器层 (7b)
  与 deployed 层 W 系列钉的那个形状。**这一支不是桩的发明**，所以"200 而 success=false 时记录照写、
  界面把原话摆出来"两条测的是真结局。
- 反过来，`http5xx`（502 + body 里带一个成功样的实例号）**不是** z-wf 这段代码能产出的形状 ——
  它是"网关/容器层把连接掐了但留了个 body"那一族。留着它有独立价值（判"看状态码还是看信封"），
  但账要记清：它证的是我们适配器的判据，不是 z-wf 的行为。

- [ ] 于是 250 这一腿的口径定为：**部署期显式指一个我自己起的桩**（专门端口，绝不留默认 `localhost:8888`），
      为的是量 MySQL 8 上那张 `z_lc_workflow_fire` 账真写得进、读得出；
      **"跟真 z-wf 端到端打通"另立一票**（要在 250 上起 Camunda + 它自己的库 —— 上面那条契约证据说明
      那一票是**部署活**，不是改代码的活）。两种口径都不要把默认值留在配置文件里。

#### 2.6.2 桩已建，且它自己的冒烟是量过的（09-27 05:0x）

`_e2e/wf_stub.py`（新增，为 250 这条腿造的）：只绑 `127.0.0.1`（不对外开洞），`--port` 默认 18888
（避开 pid 1622 那个 starter 的 8888），`--hit-file` 逐行落 JSONL（部署腿要证"发出去的那一句长什么样"），
`--pid-file` 给收尾用；`GET /__mode` 读/翻旗、`GET /__hits` 数收到几句、未知模式名 **400**（不许静默收下 ——
那等于"我翻了旗"是假的）。四种模式的形状各有出处：`ok` = `data.processInstanceId`（真 z-wf `:664`）、
`reject` = HTTP **200** + `success:false` + 前缀「流程启动失败: 」（真 z-wf `:672-673`）、
`http5xx` = 502 + body 里带一个成功样的号（**不冒充引擎行为**，它证的是适配器"看状态码还是看信封"的判据）、
`hang` = 不答（超时那一支；单线程 `HTTPServer` 下会串行卡住后面的请求，文档里写明了）。

冒烟脚本 `~/.cache/zlc61/stub_smoke.py` 的判据不是"起得住"而是**八条具名读数**（05:0x 实测 `SMOKE_OK fails=0`）：
用 `bind(0)` 挑空闲端口起桩（不占固定端口，撞不到别人的服务）→ `/__mode` 回 `ok` 且是自己（不是别人的应答）
→ 四种模式逐一验 **状态码 + 信封形状**（`curl` 对 404 也返回 0，所以只判 rc 不算证据）→ 未知模式 400
→ `/__hits` 数出恰好 3 句（第 4 次是 GET，不该进账 —— 这一条钉"计数只数 POST"）→ hit 文件逐行是真报文
→ terminate 后端口真的没人答（阳性对照：收尾不留一个别人会被 redirect 打到的口）。

第一版冒烟**是红的**，且红在桩自己身上：`FAIL 桩在挑到的空闲端口上起住了… << mode=None` 而手工复现看到
`wf_stub listening 127.0.0.1:20889 mode=ok`、`curl` 拿回的却是 `http=000` —— 进程在听、也"答过了"，
客户端只会等到超时。根因是 `_send` 里 `send_response`/`send_header` 之后**漏了 `end_headers()`**，
头没写完就送 body ⇒ 应答永远不完整。补上那一句后八条全绿；这一处记进台账，是因为"服务在监听而客户端拿不到
应答"这个形状，只判"端口起住了"的冒烟是抓不到的。

- [ ] 部署脚本那一侧的闸已同步加硬（`_e2e/deploy_250_remote.sh`）：`require_app_env` 现在要求**显式**
      `ZLC_WF_BASE_URL`，没带就 die（"忘了带参数"红在部署当场，而不是红成一条 FAILED 账）；`step_env`
      把它写进 `app.env`；`step_start` 的 `nohup java -jar` 那一串里补了 `--z-lc.adapter.wf.base-url=`
      （ctc/meta/script 三个 adapter 原本都有、唯独 wf 缺）。双向实测过：`bash -n` 通过，
      在临时 CONF 上**不带**该变量 ⇒ rc=1 且报的是那一句具名 die，**带上** ⇒ rc=0。

#### 2.6.3 闸 6 `step_fireprobe`：250 这条腿的判据改由 MySQL 自己承认（09-27 05:1x）

`_e2e/deploy_250.sh fireprobe`（新）→ `deploy_250_remote.sh:step_fireprobe`，**21 个 `fp_check` 调用点、
一趟实跑 23 条具名读数**（数目 05:31 用一段 `python` 现量的：从 `step_fireprobe()` 截到闭合 `}`、
数 `fp_check "` 的调用点 = 21，其中实体那一组在 `for pair` 循环里、一趟实跑两遍 ⇒ +2 = 23），
每条带读数、不 fail-fast（一本账只报第一处坏就等于把其余的坏藏起来）。四段归因先于判定：
端口上的 pid == `app.pid`（构件）⇒ **那个进程的 argv 里真有 `--z-lc.adapter.wf.base-url=` 且等于桩**
（不看 `app.env`：进程是上一次 start 起来的，env 写了而 argv 没带是两种不同的事实）⇒ `ZLC_WF_BASE_URL`
指向的端口就是探针翻旗/数 hits 那个口（wf 这一腿）⇒ **那个端口上听的就是我自己起的桩**
（`ss` 的 pid == `$DIR/wf_stub.pid`，见下面"起点旗标"那一条）。之后才判据：物理表在 `information_schema` 里查得到、
`/vocabulary` 的 `implemented` 含 `AFTER_CREATE`、写一条记录 ⇒ 桩**正好**收到一句、那一句带登记的 KEY
且 `businessKey` 能定位回这条记录、**库里**多一行 `STARTED` 且 `instance_id` 等于桩回的那个号、
`/fires` 读回同一行、翻 `reject` 后多一行 `FAILED` 且 `detail` 带引擎那句原话、两行各记各的账、
没绑定的实体一句都不发且账上不多行。

- [x] **起点旗标由探针自己钉（05:2x 发现的量具形状，不是产品的账）**：探针第二幕会把桩翻到
      `reject` 且**翻完不还原**，而清场走的是 `die` ⇒ 一跑红过之后桩就停在 reject。05:26 在 250 上实测：
      `/__mode` 答 `{"mode": "reject"}`、`/__hits` 的 `count` 是**本进程**的计数（重启后归 0，与
      `wf_hits.jsonl` 里那两行旧证据不同基）—— 也就是说，若直接打正向那一跑，桩会回
      `200 + success=false`，`STARTED=0`、`instance_id 对不上`、`/fires 没有 STARTED` 一串红**看着像
      "这版构件里派发器没跑"**，而真相是我的替身还停在上一幕。这正是 #55 那一族（"有进程"≠"端口上就是它"）
      在替身状态上的形状。改法两处：`fp_clean`（EXIT trap 里）末尾把桩还原成 ok，且 `step_fireprobe`
      **起点**主动 `?mode=ok` 并读回来判一条 —— 判据不依赖"上一跑很规矩"。
- [x] **负控跑过两遍，而且是白捡的**：250 上此刻还是 #61 之前那个 jar（pid 25776、argv 里没有 wf 那一项），
      同一支探针打上去 —— 第一遍（05:16，provision 还没补）`rc=1` **14 条红 / 7 条 ok**；
      第二遍（05:18，补完 provision + 改了 detail）`rc=1` **12 条红 / 9 条 ok**。
      **少掉的那两条红就是我自己那处坏**（两个实体的物理表：现在 provision 真调用、表真在，所以转 ok），
      而该红的 12 条一条没少，红的位置正是该有的位置：
      `argv 里那一句=<没有这一项>`、`implemented=<空>`、`rid=1 桩收到 0 句（应为 1）`（记录这次**写成了**，
      派发器却没跑 —— 这一条才是它的猎物）、`STARTED=0 全部行: `、
      `/fires` 回 **404 `{"path":"/api/lc/workflow-binding/fires"}`**、
      `桩多收 0 句（应为 0）、账上共 0 行（应为 2）`。⇒ 这把尺**能红**，且红落在被测物上而不是落在尺上。
      ⚠ 上面那两条新加的（桩的 pid 归因、起点旗标）是 05:2x 之后加的，**这两条在负控里没有账**：
      负控那两跑跑在旧版探针上。它们各自的猎物是"端口上换了别人进程"与"上一跑留下 reject"，
      下一跑（正向）会给出它们的第一次读数，账在那时记。
      ⚠ 有一条负向判据在负控里是**空跑的 ok**：「FAILED 那一行不许留下实例号」当时一行都没有，它当然找不到
      `^FAILED|wf250`。它的牙由同支里那条存在式（`STARTED=1 且 FAILED=1 且总行数=2`）撑着，
      **别把这一条单独当证据** —— 记账是为了下一跑别误读。
- [x] 这次负控顺手抓出**探针自己**两处坏（不是产品的账）：
      ① 建实体后我**没调** `/api/lc/admin/entity/provision` ⇒ 物理表不在、"写一条记录"红成 `rid=None`，
         那条红会被读成"派发器没跑"而真相是我的探针没建表；补上 provision 并让它单独判一条。
      ② 最后一条把两个判据（桩句数 / 账行数）写在一个 detail 里，负控里报的是「桩收到 0 句（应为 0）」
         而真正不成立的是行数那半 ⇒ 读数改成两半各报一个数。
- [x] 清场实测（05:2x，判红那条路上）：`z_lc_workflow_binding=0`、`z_lc_workflow_fire=0`、
      `e2e_fp%` 物理表 `=0`、`deploy_fp%` 应用与实体 `=0` —— `trap … EXIT` 在 `die` 这条路上真走得通
      （闸 4 当年记过 `RETURN` 留脏的坑，这一支照它的修法挂 EXIT）。
- [ ] 正向那一跑（换成 #61 之后的 jar 再打一次，期望 23 条全绿）**还没跑**：要等浏览器层的注入自证
      收线才能重打 jar —— 05:31 实测本机 18090 上是 `java -jar …/z-lc-admin/target/z-lc-admin-1.0.0-SNAPSHOT.jar`
      （pid 见 `lsof`），`mvn package` 会原地重写那个文件，而 JVM 是按需加载类的，边跑边换会让那一轮
      读出与代码无关的红。**别为抢这一跑去打断在飞的测量**。
      编号避开：`healthproof` 早就是"闸 5"（缺陷 #52），这一支记作**闸 6**。

#### 2.6.4 顺带修掉的部署量具缺陷 **#64**（原先记作 #59，撞号已改）：`status` 的归因行从 #55 起就没打印过

⚠ **编号这一段是查号时发现的，别照抄"59"**：`git grep` 实测 #59/#60 已被 `4fee620`（#52 那一窗，
09-26 19:23）占走 —— 那两号指的是"`healthproof` 不许写死端口"与"`trap` 的最后一条状态当退出码"；
#62/#63 是另一会话的 adapter 战役（`_doc/003_待办事项/README.md` 里两行），#61 是本轮。
下一个真空号是 **#64**（`#64` 在全仓只以 `&#64;` 的 HTML 转义出现，不是编号），这一支记作 #64。

`step_status` 里那句归因（`app.pid=… 端口 18090 上=…`）尾随一个 `| sed "s/\$/APP_PORT/$APP_PORT/"`：
双引号内 `\$` 先出 `$`，表达式变成 `s/$APP_PORT/18090/` —— sed 的分隔符是 `/`，第三个 `/` 让它当场报
`unknown option to 's'` 并回 1（05:1x 在 250 上实测复现），整套脚本跑在 `set -euo pipefail` 下，
于是 `deploy_250.sh status` **非零退出**、而那行归因一个字都没落盘。这一行正是缺陷 #55 为了
"别把旧进程的应答认成这次部署的"专门加的 —— 加了之后从没打印过。
去掉那句多余的 sed（`echo` 里的 `$APP_PORT` 本来就展开），空/非空改用 `${have:-<无>}`
（原先两种都尾随一个 `<无>`，端口上真有 pid 时读起来反而像"没有"）。修后实测 `rc=0` 且打印
`app.pid=25776  端口 18090 上=25776`。
**这一支在提交树里仍是坏的**（05:31 用 `git show HEAD:… | awk '/^step_status/,/docker ps/'` 回读，
HEAD=`ce81f49` 那版第 9–10 行仍是 `| sed "s/\$/APP_PORT/$APP_PORT/"`）—— 即修前每跑一次 `status`
都会非零退出，而这条退出码此前没人当判据用，所以它藏了一整窗。

### 2.7 新撞到的欠账：`/fires` 静默截到 200 条，界面一处指示都没有（缺陷 **#65**）

读侧那一句是 `WorkflowBindingService.java:55` —— `q.orderByDesc("id").last("LIMIT 200")`（05:3x 现读，
方法注释里也写着"最多 200 条"）。方向是**新的在前**，所以被丢掉的是更早的发起结局 —— 这一点比
"最新的被丢了"温和，但仍然是"一份不完整的清单长得像完整"。量到的三处事实：

- `grep -rn "LIMIT 500\|LIMIT 200" z-lc-core/src/main/java` 全仓**只有这一条**命中 ⇒ 没有第二处可以照抄的
  既有写法（同仓的变更审计那条 `LIMIT 500` 在 `mybatis/DbTableMapper.xml:56`，走的是另一条读侧，
  而且它的 javadoc 只说"只留最近 500 条，防无限增长"，同样没有任何一侧知道自己被截了）。
- `grep -n "200\|更多\|还有\|截断\|分页" z-lc-admin-ui/src/views/admin/WorkflowsPage.tsx` **0 命中**；
  再在 `fires` 的渲染行里找 `length\|slice\|count\|条` 也 **0 命中** ⇒ 界面拿到 200 行与拿到 5 行长得一模一样。
- 200 这个数**只存在于 java 那一行**，界面无法知道 ⇒ 就算今天补一句"仅最近 200 条"的文案，
  下一窗把 LIMIT 改成 100，那句文案会静默变成假话（这正是本轮 #61 的形状：广告与兑现两处各长各的）。

**打算这么修**（写下来是为了下一窗不必重新论证）：把上限做成服务里的一个具名常量，并从
`/vocabulary` 一起报出来（那个口本轮刚刚成为"部署的构件自己承认支持什么"的唯一面 —— §2.2、W17 都钉过它），
界面按"行数 == 报上来的上限"才加一行"更早的没列出"。成对的注入自证跟着走：
① 改服务里的常量而不动 vocabulary ⇒ 必须有红（两处不同步 = 广告与兑现分家）；
② 去掉界面那一行 ⇒ 浏览器层红。**不选**"把响应从数组换成 `{rows,truncated}` 对象"那一案 ——
它要动的是已被 63 条部署件探针 + 契约层 + 浏览器层三方各自 grep 的形状，改一格的代价大于一格的收益。

⚠ 为什么现在不动手：这一支要同时改 `WorkflowsPage.tsx` 与 java，而
① 那个文件此刻正被在飞的第三轮注入自证改写（05:3x 实测它带 ` M`，diff 内容正是 W 系列摘 `canDraft`
那一句：`disabled={!appCode || !canDraft}` → `disabled={!appCode}`）；
② java 一改就要重打 jar，而本机 18090 正 `java -jar` 着 `target/z-lc-admin-1.0.0-SNAPSHOT.jar`
（05:31 `lsof` 实测 pid 72030）。两件事都要等那一轮收线，和 §2.6.3 正向那一跑排在同一批里做。

#### 2.7.1 ✅ 09-27 10:5x 收线（修法**推翻了我自己在上面选的方案**，代价也一起收了）

上面那句"**不选**把响应从数组换成 `{rows,truncated}` 对象那一案"是错的，实际走的就是那一案
（`PageResult{records,total,pageNum,pageSize}`）。三个理由：

1. "上限做成具名常量 + 从 `/vocabulary` 一起报出来"仍然把**广告与兑现分在两个口**：常量改了、
   vocabulary 那一路忘了，界面那句"更早的没列出"就成了假话 —— 这正是 #61 整族在打的病，
   用"两处各自报"去修"一处不报"，等于把缺陷换了个形状留下。
2. 布尔 `truncated` 说不出**少了多少**。用户的问句是"这张账一共几笔、这一屏几笔、还有几笔没读"，
   一格布尔只能答其中半个，而 #65 的原罪恰恰是"一处指示"被省成了零处。
3. 服务器分页之后，"读全"这件事从"客户端一次拉 200 条自己切"变成"带页码再问一次" ——
   这才是 201 条那天唯一不撒谎的走法。

**上面预言的代价是真的，而且当场兑现**：换形状之后 `WorkflowsPage.tsx` 抽屉的数据从
`fires: WorkflowFireEntity[]` 变成分页信封 `ledger`，于是 **#61 那支浏览器注入量具的两支锚点失效**
（`A5` 里那句 `setFires([])` 在盘上出现 0 次、`R4` 替换体里的 `fires` 已不在作用域）。
四道闸**一条都不会红**（注入量具不在门禁里），只有 `validate()` 抓得到 —— 修完锚点并把这件事
写进 `R4` 上方的注释。**连带**：`11w` 段必须整族复测（W1–W18 那 18 支的红集是 04:4x 量的、
量的是旧形状那棵树），本窗已按"跑之前不许引用"处理。

**本窗量到的账（逐字出处见 `_e2e/README.md` 的 `### ✅ 缺陷 #65` 那一节）**：
java 层 6 支（F1–F6，`guard-0927-104304.log` 末行 `RESULT: fire-window falsification done`、
台账 `bad=0 / restored=true / rerun_green=true`、分母 `[30,15]` 与 `555/555`、`[15w]` 本节 **72**）；
vitest 层 8 支（U1–U8，`RESULT: fire-window UI falsification done`、`denominators {'tests': 8}`，
U6/U7 红集逐字相同 ⇒ 在 `PAIRS` 里明写成**一条**守卫）。

**09-27 12:5x 补：上面那两格都收掉了，而且第二格收的方式和预想不一样**
① java 层的带预期复跑已做：`~/.cache/zlc65mut/pinned_rerun_0927_1234.out`（起跑 12:34:28，量具 mtime
`10:57:51` ⇒ "起跑时 `EXPECT_*` 已非空"是可证的而不是主张），六支 reactor 红 **3/2/2/3/1/12**、
`deployed` F2 `553/555` 红 2 / F6 `534/555` 红 21，**全部与转录的预期逐字同**，整跑零条 `!!`，
末行 `RESULT: fire-window falsification done`，台账 `ran_by zifang@0927-123428 / bad=0 / rerun_green=true`。
② 浏览器层的 **(d)** 那一格已清空：`not_covered()` 里那一类删掉，换成 **W19–W22** 四支真注入
（摘 `{ledger.total}`→`{rows.length}` / 差额开关翻恒假 / 依赖里去掉 `page` / `FIRE_PAGE_SIZE` 20→200），
加上线上失效后被修的 W4/W5，`11w` 现在是 **65 条检查、22 支注入、覆盖 37 条、按未覆盖记账 27 条**
（旧口径"18 支 / 覆盖 32 / 记账 32"作废）。四支的实测红集：W19 **2** / W20 **1** / W21 **2** / W22 **5**，
逐字 `OK … 预期 N 条全红，无一条连带红`。
⚠ **两处按实测改账、没有改软任何断言**：
（a）**W5 的"注入打错了地方"其实打的是"有两个守点"** —— 摘 `A5`（关抽屉不清账）红 2、换摘 `A19`
（catch 不清账）还是红 2，同一句 `!! …预期变红却没红` 两跑逐字重现 ⇒ 那条性质由两处独立守住，
**单站点摘除是等价变异**；于是 W5 一次摘三个补丁（`A4`+`A5`+`A19`），12:2x 实测红 **3** 逐字 OK。
残余缺口按实记在 `not_covered()` 的注释里：浏览器层现在只守得住"两处一起摘"，**单点回归抓不到**，
要拆回去得先给套件加一条 loading 窗口采样（本窗没加）。
（b）**W22 第一次跑把整节撞没了**：分母 `285 → 273`、量具逐字 `!! … 后面那些「没红」是「没跑到」`。
根因不在产品也不在判据，在**套件**那颗翻页是裸 `.click()`（新缺陷 **#75**，见 §2.16）。
修完那一处点击之后 W22 才量出上面那个 5。

### 2.8 新撞到的欠账：`update` 那条查重**结构上打不到任何行**（缺陷 **#66**，机制待运行时证）

同一处查重要求"同一 (租户, 应用, 实体, 事件, 流程 KEY) 只留一份"，理由是它自己在 javadoc 里写的那句
—— 重复登记会让一条记录同时发起 N 个流程实例。**create 那一路是真的**（`create()` 在 `validateForWrite`
之前控制器已经把租户钉成 `default`）。**update 那一路读起来是死的**，三段都在盘上（05:4x 现读）：

1. `WorkflowBindingController.java:76` —— `/update` 进门第一件事是 `entity.setTenantCode(null)`
   （注释写着"归一化而不是覆盖：update 里租户仍取库里那一份，service 负责"）；
2. `WorkflowBindingService.java:130` —— `requireNotDuplicate(entity, entity.getId())`；
3. `WorkflowBindingService.java:134` —— `entity.setTenantCode(existing.getTenantCode())`，**在第 130 行之后**。

⇒ 查重时 `candidate.getTenantCode()` 是 `null`，`requireNotDuplicate` 走的 `listByEvent` 拼的是
`new QueryWrapper<>().eq("tenant_code", null)`。这一句我**还没有跑过**，所以按两条静态尺先记着：
`grep -rn "FieldFill" WorkflowBindingEntity.java` 无填充器、全仓 `grep -rln "TenantLineHandler\|TenantLineInnerInterceptor"`
**0 命中**（没有租户拦截器会替我把 null 换成值）⇒ 没有任何一层会救这个 null。
若 `eq(col, null)` 真按 MyBatis-Plus 的无条件拼法出 `tenant_code = NULL`，SQL 里这一谓词对任何行都是 UNKNOWN
⇒ **查重恒查 0 行 ⇒ 恒不报重复**。那样的话"把 B 改成和 A 同 KEY"这条路能造出两条都满足 `listByEvent`
的绑定，一条记录写成功 ⇒ 发 N 个并行流程实例 —— 正是这支守卫声称要挡的那件事。

下一批要做的（顺序即判据，别跳）：
- [x] **先证机制再改** —— 07:2x 已在运行时证完，机制照原假设成立：把 B 改成 A 的 KEY 这一句 `update`
      在改之前**放行**，应答逐字是 `{"data":{"id":4,…,"processDefinitionKey":"p_first",…},"success":true,"code":200}`
      （日志 `~/.cache/zlc66/web_66_injected.log`，那一支测试红在"HTTP 状态应当是 400"上，把这条应答整份带了出来）。
      ⇒ `eq(col, null)` 那一问也一并答了：**条件照拼，值绑的是 null** ⇒ 恒不匹配（不是我先前担心的
      "MyBatis-Plus 把 null 当'不加条件'"，那样反而是安全的）。
      ⚠ 06:3x 那一句"改动与测试已写进工作树但一条都没跑"到 07:1x 为止是真的（整族注入占着 CPU）；
      07:2x 起全部跑完，读数在下面两张表里。
- [x] 修法已落工作树（07:2x 已跑已绿）：`WorkflowBindingService` 新增
      `normalizeBeforeJudgement(entity)`（只剪 `triggerEvent` / `processDefinitionKey` 的空白，null 留给
      `validateForWrite` 去指名），create 与 update **都排在 `validateForWrite` + `requireNotDuplicate` 之前**；
      update 里 `entity.setTenantCode(existing.getTenantCode())` 从原来的第 134 行（查重之后）提到**查重之前**；
      `requireNotDuplicate` 再加一道"候选的租户是 null 就当场说话"（`IllegalArgumentException`），
      理由写在那一格的注释里：没有租户的查重在 SQL 上是恒不匹配，而它自己不知道。
- [x] 测试已写五支（core）+ 一支（HTTP 契约层），**全部未跑**：
      `WorkflowBindingServiceTest.updateShouldRefuseDuplicateEvenWhenTheCallerSendsNoTenant`（送 null，走真接线形状）、
      `…AcrossWhitespaceInTheEventAndKey`、`createShouldRefuseADuplicateWhoseKeyOnlyDiffersByWhitespace`、
      `requireNotDuplicateRefusesToJudgeWithoutATenant`（钉那道 null 哨兵真的响、报的是这一句；
      ⚠ 这一支的可达性在测试注释里写死了：DDL 上 `tenant_code NOT NULL` 且两个调用方都不送 null，
      所以它钉"哨兵会响"，不钉"生产上有这条路径"），
      以及既有那支 `updateShouldRefuseDuplicates` 原样保留（它绿的原因是替身不符接线，见上面第 1 条，不删它是因为
      "带租户也要拒"本身是一条真要求）；
      `WorkflowTriggerContractTest.updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice`
      走 `/update`：钉 400 + `message` 含「已经绑定过」**且含 `id=`**、`/list` 读回那一行 KEY 仍是 `p_second`
      且租户没被搬走；再写一条记录，钉 `STUB.count()==2` **且 `"processKey":"p_first"` 只出现一次**
      （`occurrences(STUB.allBodies(), …)`）+ 发起账两行。
      ⚠ **我在这格原先写的是"钉 `STUB.count()==1`"，那是错的，06:4x 改回来了**：这一条例子里活着的是
      两条*不重复*的绑定（`p_first`、`p_second`），派发器对每条绑定各发一句 ⇒ 正确的句数就是 2；
      病灶形状不是"两句"而是"两句都是 `p_first`"（同一 KEY 两行 ⇒ 同一条记录把同一个流程起两个并行实例）。
      数总句数在修法前后都是 2，**结构上抓不到这个缺陷** —— 这就是"判据要问哪个 KEY 被发了两次"而不是"发了几次"。
- [x] 成对注入自证（**07:2x 跑完了，四跑一台账**）：注入方式是"拿 `git show HEAD:` 的那份字节整份 `cp` 上去"，
      不是手改 —— 那样得到的才是历史形状本身。备份/还原全用 `cp` 逐字节对账（`~/.cache/zlc66/WBS.java.fixed`
      = 修好的那份，md5 `e2936acb…`；`WBS.java.head` = 历史那份，md5 `e5102180…`），
      **一次都没有用 `git checkout --`**（工作树里还有别的批的未提交改动）。

      | 跑 | `WorkflowBindingService`（main） | core 那层的替身 | core 结果 | HTTP 契约层结果 |
      |---|---|---|---|---|
      | b | 修好的 | 宽松的（旧写法） | **29/0 绿** | — |
      | I1 | 历史的（`HEAD` 那份字节） | 宽松的 | **2 支具名红**：`requireNotDuplicateRefusesToJudgeWithoutATenant`、`…AcrossWhitespaceInTheEventAndKey` | **1 支红**：`updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice` 撞在 `HTTP 状态应当是 400`，应答逐字 `{"data":{"id":4,…,"processDefinitionKey":"p_first",…},"success":true,"code":200}` |
      | a | 修好的 |  faithful（新写法） | **29/0 绿** | **14/0 绿**（整类 14 条） |
      | I3 | 历史的 | faithful | **3 支具名红** = I1 那两支 + `…EvenWhenTheCallerSendsNoTenant` | — |
      | I2 | 修好的 | 把 `eq()` 换回宽松写法 | **1 支红** = `fixtureReallyAppliesWrapperPredicates`，读数 `expected:\<[]> but was:\<[keep, foreign-tenant]>` | — |
      | restored | 修好的（`cp` 回来，md5 对上） | faithful | **29/0 绿** | — |

      **我上面对 I1 的预测被实测否掉了一部分，这就是"跑一遍"的价值**：我写的是"两步一起摘 ⇒ 那两支 + HTTP 同时红"，
      实测 I1 只有 **2** 支红，`…EvenWhenTheCallerSendsNoTenant` 在历史代码上**照样绿**。
      查下去不是测试写错，是**量具（core 那层的替身）自己比真库宽松**：`eq()` 旧写法拿
      `predicateValue(...) == null` 同时表示"这一列上没有谓词"和"谓词绑的是 null"，于是
      `tenant_code = NULL` 在替身里被当成"不筛租户"，跨租户的行照抄出来（I2 那一条读数就是它）。
      ⇒ 真库里那句恒不匹配、替身里恒全通过 —— **这就是 #66 在 4677 条全绿底下活着的第三层原因**
      （前两层：单测候选自带租户、控制器抹租户；这一层是"替身连 null 语义都不对"）。
      修法：`eq()` 拆成"有没有谓词"（`predicateKey`）与"绑的是什么"两问，绑 null ⇒ 谁都别想通过；
      并在 `fixtureReallyAppliesWrapperPredicates` 里给它配上**对偶的自证**（绑 null 该 0 行 + 没有谓词不许筛空），
      于是 I2 这一跑就是量具自己的阳性对照。

      ⚠ 还有一格要如实记：**新增那支 `createShouldRefuseADuplicateWhoseKeyOnlyDiffersByWhitespace` 在 I1/I3 下都是绿的**
      —— 历史代码的比较两侧本来就 `.trim()` 过（`git show HEAD:` 那份 `requireNotDuplicate` 里逐字看得见）。
      ⇒ 它不是这一支缺陷的猎物，是一根防"以后有人把比较里的 trim 摘掉"的回归桩。记账时不许把它算进"具名红"。
      ⚠ 但"回归桩"不等于"空桩"：07:5x 那一支 **I5**（只摘 `create()` 里的 `requireNotDuplicate` 调用）把它**测成红**了
      （见上面 ② 那段）—— 所以它对 create 这一路是真有牙的，只是它的牙不咬 update 那一族的缺陷。

      复算命令（**`-Dsurefire.failIfNoSpecifiedTests=false` 是必需的**：`-am` 会把 `z-lc-common`/`z-lc-sdk`
      一起拉进 reactor，那里没有同名测试，早先我写在这格的那两条命令会在 `z-lc-common` 直接 BUILD FAILURE，
      报 `No tests matching pattern "WorkflowBindingServiceTest" were executed!`）：
      `mvn -o -pl z-lc-core -am -Dtest=WorkflowBindingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
      `mvn -o -pl z-lc-web -am -Dtest=WorkflowTriggerContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
      日志（07:2x 本轮全在盘上）：`core_66_b.log`(修好/宽松·绿) `core_66_injected.log`(I1) `web_66_injected.log`(I1·HTTP)
      `core_66_injected_faithful.log`(I3) `core_66_faithful.log`(a) `core_66_loose_eq.log`(I2) `core_66_restored.log`，
      都在 `~/.cache/zlc66/`。
- [x] **07:2x 四层同轮复测**（跑 #66 这批改动之后重量，逐层现读）：
      java `mvn -o -B clean install` = **4774 例 / 0 失败 / 0 错误 / 0 跳过**（7 模块全 SUCCESS，
      总耗时 16.9s；分模块 2701 + 525 + 1330 + 112 + 106，`install_full.log`）；
      前端 `npm run check` rc=0 = **28 文件 / 252 例**，产物 `index-ruBdvAdn.js`（⚠ 名字不是身份，见 #69）；
      接口层 `python3 _e2e/e2e_api_test.py http://localhost:18090` = **532/532**（`API_RC=0`，
      分母双向：`^  PASS ` 行数也 532；打的是 07:24:32 那支新 jar —— 旧常驻 72030 已 `kill -TERM`、
      等端口真释放后由 pid **64499**（`ps -o lstart` = 07:26:08）起新件，`lsof` 现读监听者就是它，#55 那一族的口径）；
      浏览器层 = **279 / 0**（`BROWSER_RC=0`，一轮，07:27:31→07:30:19；`INFO 非 2xx 2 个` 是那两条故意的负控）。

**06:2x 现读，补两条"为什么它在全绿测试底下活着"**（都不改上面那句"待运行时证"，只是把嫌疑收窄）：

1. **单测那一支是绿的，而它绿的形状不是接线的形状**：`WorkflowBindingServiceTest.java:385 updateShouldRefuseDuplicates`
   的候选行由 `edit(storedRow(1))` 造，而 `edit()` 在 `:132` 逐字写 `binding(stored.getTenantCode(), …)`
   ⇒ 候选**带着** `"default"` ⇒ `listByEvent` 查得到行 ⇒ 查重真的红。
   而 HTTP 那一口先进 `WorkflowBindingController.java:76` 的 `setTenantCode(null)`。
   ⇒ 这一对差别就是老教训的形状：**测试替身没走生产接线，洞在测试里永远绿**。
   所以新增那一条必须走 `/update`（HTTP 契约层），不是给 core 单测再加一支带租户的候选。
2. **界面也躲不掉，不是只有裸调 API 才碰得到**：`WorkflowsPage.tsx:44` 的 payload 明明写着
   `tenantCode: DEFAULT_TENANT_CODE`，到控制器照样被第 76 行抹成 `null` ⇒ 从界面上"把第二条改成和第一条同 KEY"
   这条路今天走得通，走完库里就是两条都满足 `listByEvent` 的绑定 ⇒ 一条记录写成功发 N 个并行流程实例。
   （另有一处二阶：`WorkflowBindingService.java:135` 的 `setTriggerEvent(….trim())` 也在第 130 行之后，
   带空白的 event 同样会让那一句 `eq("trigger_event", …)` 打空；这条不是主因，一起收进同一个归一化入口就行。）

- [x] 修法（**07:2x 已落盘并跑绿**，见上面那张六跑矩阵）：`normalizeBeforeJudgement(entity)` 提到 `validateForWrite` +
      `requireNotDuplicate` 之前，`setTenantCode(existing.getTenantCode())` 提到查重之前，create/update 走同一条归一化入口；
      另加一条 null 哨兵（`tenantCode == null` ⇒ 拒判而不是"查不到就当没重复"）。
      ⚠ 上面 620–632 那段里的行号（`:385` `:132` `:76` `:135` `:130` `:44`）是 **06:2x 现读的**，本批改动之后已经位移 ——
      按名字找，别按号找（这条也是本仓既有纪律：票面与台账都不钉静态行号）。
- [x] 成对注入自证（07:2x 六跑 + **07:5x 补 I5**，日志全在 `~/.cache/zlc66/`）：
      ① 归一化 + 租户两句一起退回历史形状 = **I1**（core 2 支具名红 + web 1 支红，实测把预测的"是哪两支"否了一半，见上面那段）；
      同一历史形状配 faithful 尺 = **I3**（core 3 支）；只把尺退回宽松、生产码仍是修好的 = **I2**
      （`fixtureReallyAppliesWrapperPredicates` 红 = 量具自己的阳性对照）。
      ② **摘掉 `requireNotDuplicate`（只摘 `create()` 里那一处调用，`update()` 那句不动）= I5** —— 原工单点的就是这一支，
      先前那六跑没有打过它，07:5x 补跑：
      core `core_66_no_dedupe.log` **Failures: 2** = `createShouldRefuseADuplicateOfTheSameEventAndProcessKey`
      + `createShouldRefuseADuplicateWhoseKeyOnlyDiffersByWhitespace` ⇒ **那根"回归钉"是有猎物的，不是空桩**；
      web `web_66_no_dedupe.log` **Failures: 1** = `duplicateBindingIsRefusedWhileTheFirstOneStillFires`
      撞在 `HTTP 状态应当是 400`（应答里 `"id":2` 就是那条本该被拒的重复绑定）；
      还原只从 `~/.cache/zlc66/WBS.java.fixed` `cp` 回来 ⇒ md5 逐字节对回 `e2936acb…`、`grep -c "MUT(I5)"` = **0**，
      复跑 `core_66_restored_i5.log` **29/0、BUILD SUCCESS、rc=0**。
      ⚠ 顺序也要认下来：I5 是在下面那条"07:2x 四层同轮复测**之后**"跑的，所以四层那几个数（4774 / 532 / 252 / 279）
      量的不是 I5 那一份树 —— 靠的正是上面那次 `cp` + md5 对账把它逐字节退回被量过的那份；下一窗若要引用那几个数，
      先 `md5 -q` 比一下 `e2936acbdc7e52ac34037cdf67d54b5c`。
- [x] 契约层那条正向：`/update` 改成重复 KEY ⇒ 400 且 `message` 说清是哪条 id 挡的
      （就是 `updateIntoADuplicateIsRefusedAtTheHttpDoorAndNoKeyFiresTwice`；本窗实测历史形状下这一口回的是
      `expected: <400> but was: <200>` 而 body 里 `"processDefinitionKey":"p_first"` —— 缺陷本体在 HTTP 层的形状）。

### 2.9 新撞到的欠账：绑定钉在 `default`，派发却按记录自己的租户查（缺陷 **#67**，静态读出）

同一条链的两端用的是**两个不同的租户**（05:4x 现读，尚未跑过）：

- 写侧：`WorkflowBindingController.java:67` —— `create` 进门第一件事 `entity.setTenantCode(DEFAULT_TENANT)`，
  所以**任何**绑定都落在 `"default"` 这一格（这一条不是疏忽，是 #48 那一窗为堵"把绑定写到别人租户下"
  刻意钉的，javadoc 里写着"body 里带来的 tenantCode 一律不用"）。
- 读侧：`RuntimeCrudController.java:188` —— `afterCreate(body.getTenantCode(), …)`，派发器拿的是
  **请求体里那条记录自己的租户**；`WorkflowTriggerDispatcher.java:147` 用它去
  `bindingService.listByEvent(tenantCode, …)`，而 `listByEvent` 第一条件就是 `.eq("tenant_code", tenantCode)`。

⇒ 一条落在非 `default` 租户的记录，**永远查不到任何绑定 ⇒ 一句都不发**，而写入口、`/list`、界面全都不说话。
`z_lc_workflow_fire` 也不会有行（没发起就没有账），所以这一支连 §2.6.3 那条"库自己承认"的尺都照不到 ——
它的形状是"沉默的不兑现"，也就是 #61 的原病灶换了一个接缝。
运行时这一层确实支持多租户：同一个方法里 `resolveEntity(appCode, entityCode, tenantCode)` 是按租户找定义的
（不是硬编码），所以这不是"理论上可能的形状"，是接口面明摆着能走到的路径。

**两条修法，方向相反，要拍的是产品口径而不是工程量**（所以它同时进 §3 当第五问）：
- (甲) **绑定按实体所属租户存**：写侧不再钉 `default`，改成"按 `(appCode, entityCode)` 去元数据里读它真正的租户"。
  这才是多租户下对的做法，但它把 #48 当初钉掉的那道闸又打开一半 —— 没有认证之前，任何人都能往别人的租户登记绑定。
  ⇒ 前置是"管理面得先有租户归属判定"，那不是这一批的量。
- (乙) **登记当场拒**（我倾向这一条，因为它把沉默变响、且不加新权限面）：`create` 在校验前先按
  `(appCode, entityCode)` 读元数据租户，**不等于 `default` 就 400**，话要说全：
  "这条绑定登记在 `default`，而实体 `<x>` 属于租户 `<y>` ⇒ 记录写成功也不会发起流程"。
  配合成对注入自证：① 摘掉这一句 ⇒ 新增那条契约测试必须红；② 元数据租户读成 null 时不许放行
  （否则"读不到租户"会变成第三条偷偷通过的路）。
- 无论哪一条，**§2.6.3 的闸 6 都要加一幕**：在非 `default` 租户下建一条记录，判"桩收到 0 句"这一条
  到底是"设计上不发"还是"链断了"——今天的探针只在 `default` 上跑，这一格对它俩**分不开**。

### 2.10 新撞到的欠账：同一个派发器里，读侧吞、写侧不吞（缺陷 **#68**，静态读出）

`WorkflowTriggerDispatcher.afterCreate` 那 30 行里两半的失败处理是**不对称**的（05:4x 现读，行号相对方法首行）：

- 查绑定那一半有 catch：`catch (RuntimeException ex) { log.warn("流程绑定查询失败…"); return 0; }`
  ⇒ 绑定的元数据表读不出来（**没建表**、库不可达、列漂了）＝ 一条 `warn` 日志 + 静默不发单，
  用户的记录照样写成功。部署期"库里没这张表"是**真实存在过的状态**（250 上那张 `z_lc_workflow_fire`
  是我这一窗才补进去的，见 §2.6 的 schema 那条），所以这一支不是假想。
- 写结局那一半没 catch：`record(binding, start, …)` 直接暴露在外（方法体里 `fire(...)` 与 `record(...)`
  两句都不在 try 内）。⇒ 如果 `z_lc_workflow_binding` 在而 `z_lc_workflow_fire` **不在**（半套迁移，
  最容易发生的那种），记录已经写成了、流程也已经**真的发出去了**，然后写账一抛 ⇒ 一路抛出到
  `RuntimeCrudController:188` 之后（那两句在 controller 的 try/catch **之外**，我核过：catch 块在
  `:176-180`，`create`/`afterCreate` 在 `:182-188`）⇒ 用户拿到的是**裸 500**，而账上什么都没有。

⇒ 两个坏结局撞在同一处缺表上，一个"什么都不说"、一个"说得很响但指错了方向"。这跟 #47 是同一族
（"运行时每次读都是裸 500"）而根因更靠前：**部署期就该发现表没建齐**。

**倾向的修法**：不在 `record()` 外面套一个 catch 把它变哑 —— 那只是把 500 换成日志，仍然撒谎。
按 #52/#57 的既有口径办：**启动当场红**。仓里已有 `DataSourceConfigGuard`（#52 那批进的）与
`LcModuleDataSource`，让它对 LC 库要求的表清单里带上 `z_lc_workflow_binding` + `z_lc_workflow_fire`，
缺表 ⇒ 进程拒起，而不是等第一条记录去撞。读侧那句 `log.warn(... ) return 0` 也一并重判：
表**存在**而查询失败才是"降级不发"，表**不存在**属于配置错误，两种情况今天被并成同一句 warn。

- [ ] 运行时证（下一批，按这个顺序做才有牙）：
      ① 在一套只有 `z_lc_workflow_binding`、没有 `z_lc_workflow_fire` 的库里，登记绑定 + 写一条记录 ⇒
        今天应当复现"记录写成了 + 接口裸 500 + 账上 0 行"三件事同时成立（少一件就说明我对调用链的读法错了）；
      ② 修完之后同一跑 ⇒ 进程**起不来**，且拒起那句话点名缺的是哪张表；
      ③ 成对注入：把新加的启动校验摘掉 ⇒ ② 必须回到"能起来"（否则那条校验是空的）；
      ④ 部署腿同步：`deploy_250.sh schema` 那一支要把两张表都建（今天只钉了 `TABLES_MIN=14` 这类**计数**，
        计数不点名 ⇒ 少一张多的另一张它能绿）。⚠ 这一条是闸 6 的正向跑之前必须落的地基，
        否则 21/23 条读数里那些"表在不在"的判据测的是我自己 scp 上去的手工动作。

### 2.11 新撞到的量具缺陷：恢复判据拿"产物逐字节相等"当尺，而本机 rollup 不逐字节可复现（缺陷 **#69**，09-27 06:0x 已修）

  - **症状**：第三轮 `RESULT: 1 problem(s)`，那一条逐字是
    `!! 恢复后产物 index-ruBdvAdn.js|d6d15714e665b683385238301be28116 != 基线 index-DBJrrqb7.js|5f2afecc45687a775eafb5cfdf21e571 —— 源码回来了而产物没回来`。
    号没撞（06:04 全库 `grep -rn "#69" --include=*.md --include=*.py --include=*.tsx --include=*.mjs --include=*.java .` = 零命中，
    本窗新号 #65–#69 里它是最后一支；**这一条是 06:0x 这一窗的**）。
  - **它是"没有猎物的尺"，不是"抓到了的东西"**：三轮里 `未恢复到原始内容` 那句 0 次触发；
    跑完之后的 `WorkflowsPage.tsx` md5 = `87b30e33161f27089623d260151e12fd`，与第二轮恢复轮的记录逐字节相同；
    而产物名在同一份源码上给过两个值 —— 六次干净 src 构建、两个名字、方向还反了一次，
    逐条时刻与出处落在 `_e2e/README.md`「缺陷 #69」那一节的表里（每一行都指得到盘上还存在的日志）。
    ⚠ 我早先在这格写过"06:02–06:06 连跑三次 build 全是 `index-DBJrrqb7.js`"—— **这句删了**：
    06:2x 回查时 `~/.cache/zlc61/` 里那个时刻没有任何 build 日志，三次这个数字拿不出来，
    而结论不靠它（靠的是 03:44 / 05:02 / 06:00 / 06:09 / 06:15 / 06:19 这六条现可回读的读数）。
    两次构建的 chunk 表逐行只差在名字上，`antd-*.js` 甚至差 `1,269.60` vs `1,269.64` kB
    ⇒ **产物字节在本机不是源码的确定函数**（rollup 的 chunk 哈希会漂）。
    仓里另一支守卫 `mutate_permission_browser_guard.py` 早就为这件事改用 src 指纹（README:1761 记着
    "同一棵树三次构建跑出两套 index-*.js 名字"），这一支却照抄了"产物相等"⇒ 一条必红而红不到任何东西的判据。
  - **改法（不是删判据，是换成抓得住猎物的那一把）**：`e2e/mutate_workflow_browser_guard.py`
    ① 新增 `src_digest()`（整棵 `src/` 按路径排序取字节 md5）；② 每支注入写盘后判
    `src_digest() == base_src` ⇒ 红（**锚点落空、变异没进树**这一整类假绿从此有牙）；③ 每支收尾除
    PAGE 文本相等外再判一次 src 指纹（补上"改的是 PAGE 之外的文件"那个瞎处）；④ 恢复轮判据换成
    src 指纹，产物只打印不判红，并写明为什么（漂移要看得见，不许当判据）。
    顺带把「注入有没有进产物」这一格记清：它从前的产物相等判据名义上管、实际管不住（产物本来就会漂，
    相等几乎不发生 ⇒ 空跑），现在由"这一轮预期那几条必须红"behavioral 地管 —— 改动没落到界面上就红。
  - **自证（同轮，注入到量具自己身上，不 build、不抢端口）**：`python3 e2e/mutate_workflow_browser_guard.py --selftest`
    06:0x 实测 `SELFTEST RESULT: 0 problem(s)` / `SELFTEST_EXIT=0`，三支成对：
    `OK A` 往 `PAGE` 之外加一个 src 文件 ⇒ 指纹动而 PAGE 逐字不变（旧判据在这一处是瞎的）；
    `OK B` 撤掉探针 ⇒ 指纹回到基线；
    `OK C` 拿真注入的锚点接一个源码里不存在的尾巴造出"replace 静默落空"⇒ 写盘后指纹不动
    ⇒ 证明新加的**那条红打得出来**（`main()` 里 `src_digest() == base_src` 这一支不是死码）。
    跑完 `WorkflowsPage.tsx` md5 仍是 `87b30e33…`、`src/__guard_probe__.ts` 不存在（无残留）。
  - **波及面已普查（06:2x，别再重查一遍）**：`z-lc-admin-ui/e2e/` 下六支浏览器注入量具，
    拿 src 字节指纹判还原的只有两支 —— `mutate_permission_browser_guard.py`（早就为这件事改的）与这一支（#69 改的）。
    另外四支（`field_code` / `pipeline` / `pivot` / `provision`）**没有一条拿产物名判红**：
    `mutate_pipeline_browser_guard.py` 判的是逐个文件"不是原始内容"（`:336`、`:347`，比指纹更直接），
    余下三支 `grep -cE '产物.*(!=|回到基线|一致)'` 也是 0。⇒ **#69 不是一张欠账清单，是这一支独有的病**，
    收口时不许写"这一族都改完了"（没得改），也不许写"另外四支待修"（没坏）。
    ⚠ 别被 `_e2e/README.md:1408` 那格的括号骗到：`三次构建的产物 index-DiIAeQ4C.js → index-DED1Bq_b.js → index-DiIAeQ4C.js
    （回到原 hash 才叫"被测件换回来过"）` —— 那是**我上一窗写文档时的叙述**，不是那支量具的判据；
    它实际判的是源码内容相等（上面那两行）。产物名在这族里能来回漂，"回到原值"从来不是证据。
  - **还欠的**：`main()` 里改过的那几行没在 19 轮整族里跑过。为了不为这一件事烧 58 分钟，加了
    `--only <题号>` 收窄档（只跑基线 + 那支 + 恢复轮，并在日志里写死"这一跑不能当整族自证的账"），
    06:09 起的是 `--only W12`（选 W12 因为它红集最大、6 条），落盘
    `~/.cache/zlc61/browser_guard/run4_narrow.out`。**收窄跑已收，读数（`NARROW_EXIT=0`，06:17 落盘）**：
    基线 `00_baseline: PASS+FAIL 279 / 红 0` → W12 `12_W12: PASS+FAIL 279 / 红 6`，六条**全部标 `RED (预期)`、
    `OK W12 …: 预期 6 条全红，无一条连带红`** → 恢复轮 `99_restored: PASS+FAIL 279 / 红 0`。
    两件事是这一跑要证的，都证到了：
    ① 新判据在真 build + 真浏览器轮次里跑通且**没有再造假红** —— 这一轮照样漂
    （基线 `index-ruBdvAdn.js` → 恢复轮 `index-DBJrrqb7.js`，日志里那行 `（信息）产物 … != 基线 … —— 产物漂移，不作判据`
    逐字在，而 `!!` 一行都没有），旧判据在这一轮会给出第二条红；
    ② 收窄跑与整族第三轮**测的是同一件事**：两支的 W12 红集把行尾墙上时钟归一后 `diff` 为空
    （`diff <(grep '^  FAIL' run3_logs/12_W12.log | sed -E 's/2026-09-27 [0-9:]+//g') <(grep '^  FAIL' 12_W12.log | …)` 无输出），
    即"红 6 条"不是这一跑偶然撞出来的形状。
    ⚠ 但收窄跑**不顶整族的账**（横幅自己写着），所以 06:20 起在飞的是不带 `--only` 的整族那一跑
    （`run5_family.out`，18 支 + 基线 + 恢复轮）—— 这一跑的读数**等它落盘再记**，别提前写进任何格子。
    另记一条这一窗才看清的**读数口径**：这一族量具的 `RESULT: N problem(s)` 那行**只在 N>0 时才打印**
    （`e2e/mutate_workflow_browser_guard.py` 收尾那段写作 `if bad: print(f"\nRESULT: {bad} problem(s)")` 然后
    `return 1`，绿的时候走到下一条 `print`），
    绿的时候尾行是 `RESULT: 11w 的 31 条各自钉住一件事（18 支注入）；27 条按未覆盖记账…` + 退出码 0。
    ⇒ 读这一族的账要认**退出码 + 族汇总行**，不要 grep "0 problem(s)"（那是 `--selftest` 的格式，两支尺口径不同）。
    `mutate_provision_browser_guard.py` 同形，`mutate_pipeline_browser_guard.py` 则是把两种读数塞进同一行
    （`RESULT: {'11a 那九支各自打掉一句保证' if bad == 0 else f'{bad} problem(s)'}{tail}`）—— 抄数前先看清是哪一支。
    ⚠ 这三处我一开始都写的行号（674 / 314 / 361），写完发现改 docstring 已经把第一处挪到 681 ⇒ **一律改写成可 grep 的构造**，
    别在这一族里发行号。

### 2.12 缺陷 **#70** 已闭合：部署中心的三个"部署方式"当场一个都不执行（09-27 08:3x–09:22 实测并修）

**当时盘面（逐条现查，不是推理）**

- `DeploymentServiceImpl.createDeployment` = `insert` 一行 `PENDING` + 一句 `// TODO: 异步执行物化/部署逻辑` + return。
- `updateDeploymentStatus` 在生产代码里**零调用者** ⇒ `PENDING` 在这张表里是**终态**，没有任何人会把那一行推到 SUCCESS/FAILED。
- `DeploymentsPage.tsx` 手抄三个选项（热加载 / Docker 镜像 / Git 推送），而三个都不执行 ⇒ 选哪个都一样。
- 界面后果：toast「部署已创建」、状态列永远灰、`deploy_log` 抽屉永远「（暂无日志）」。
- 同一趟写入口全收：不存在的 `appCode`、不属于本应用/本租户的 `materializationId`、以及 `deployType` 缺失时撞 NOT NULL 列的**裸 500**。
- 接口层量具在这一格只有两句 `ok()`（只看 200）⇒ 那 532 条里没有一条说过"选哪一种有区别"。

**改了什么（口径与 #41/#61 逐字同形）**

- 新增 `DeploymentTypes`：`HOT_LOAD` 是唯一有执行器的一种；`DOCKER` / `GIT_PUSH` / `SQL` 各自带一条**说得清为什么不会执行**的原因。
- 写入口 `requireExecutable`：缺 `appCode`、词表外的方式、应用不存在、批次不属于这个应用这个租户 ⇒ `IllegalArgumentException` → 400，消息点名要改哪一格。**一条永远不会执行的部署记录不该出生。**
- 真的执行：`create` 同步跑 `schemaAdminService.provisionAllTables(tenant, app)`，把 `SUCCESS`/`FAILED` 与逐实体汇总写回那一行，**返回的是执行后的真状态**。没有照 `MaterializationService.trigger` 那个"先 insert PENDING 再调 @Async"的形状抄 —— 理由见 §2.13（#71）。
- `/deployment/vocabulary` 成界面唯一来源（`executable` + `rejected[{type,reason}]`），`_deployment.ts` 从它长出选项，`DeploymentsPage` 不再手抄；前端词表测试机械比对 Java 侧那份清单。
- 钉租户：`DeploymentController.create` 强制 `DEFAULT_TENANT`（与 #48 那六个口同口径）。

**四道闸（本窗逐层现量，全部同一轮）**

| 闸 | 跑法 | 读数 |
| --- | --- | --- |
| ① java | `mvn -o -B clean install` | BUILD SUCCESS、rc=0、**4788 例 / 0 失败**（`Total time: 17.972s`，`Finished at 09:15:59`）；fat jar md5 `37402c05baa6b1e53c0435f110ac55f9`，inode 141692193，落盘 09:15:58 |
| ② 接口层（发出去的 jar） | `python3 -u _e2e/e2e_api_test.py http://localhost:18090` | 09:18:18–09:18:22 rc=0 ⇒ **`E2E RESULT: 546/546 passed`**，`^  FAIL` 计数 **0**；伺服进程 pid 57406（09:17:53 起），`lsof` 的 NODE=141692193 与①那一格**同一个 inode** |
| ③ 前端 | `cd z-lc-admin-ui && npm run check` | 09:21:50–09:22:19 rc=0，`Test Files 30 passed (30)` / `Tests 259 passed (259)`，`✓ built in 2.65s`（tsc + eslint --max-warnings 0 + vitest + build 四步都在这一条命令里） |
| ④ 浏览器 | `node e2e/browser-e2e.mjs`（自带 src/bundle 新鲜度闸） | 09:18:50–09:21:38 rc=0 ⇒ **`=> PASS 279 / FAIL 0`**、`全绿轮次: 1/1` |

**注入自证 `_e2e/mutate_deployment_guard.py`（新，7 支 / 19 条具名红）**

09:14:03–09:15:10 rc=0，末行 `RESULT 7 支判定，其中 MISMATCH: 无`；台账 `~/.cache/zlc70/mut/ledger.json`（`ts: 2026-09-27 09:15:02`）：

| 支 | 摘掉的是什么 | core | web | ui |
| --- | --- | --- | --- | --- |
| J1a | 执行那一步（只 insert 不 provision） | 2 | 3 | — |
| J1b | 物化批次那道归属门 | 1 | 1 | — |
| J2 | 应用存在性预检 | 1 | 1 | — |
| J3 | 结局回写（`updateById`） | 2 | 2 | — |
| F1 | 页面按部署的真结局分叉 | — | — | 2 |
| F2 | 下拉项回到"页面源码里" | — | — | 2 |
| F3 | 词表形状不对也照旧渲染 | — | — | 2 |

分母每轮钉死 `core 19 / web 7 / ui 7`；开跑前与收尾各复跑一次基线，三层都 **0 红**（`logs/{baseline,after}-{core,web,ui}.log`）。
残留自查：`grep -r "mutant J1a\|…\|System.nanoTime() < 0" z-lc-core/src z-lc-web/src z-lc-admin-ui/src` = **0 命中**，同一批命令里对量具自身 `grep -c "mutant"` = 11（阳性对照）。

**这一窗在量具自己身上抓到的四件事** —— 它们都曾长得像"绿"：

1. **`.class` 归因检查写反过**：拿**跑之前**的 `.class` md5 去比上一轮的 md5，而那一刻 maven 根本还没编译 ⇒ 恒等于旧字节，那句"编译确实量了这份变异"是空的。改成注入后跑层、再判 `.class` mtime 晚于注入写入时刻；每支逐条打印 md5+时刻。
2. **ui 层结构上永远报 0 红**：vitest 的状态词是 `failed`，判红筛的是 `fail` ⇒ F1/F2/F3 一度各报"红 0 条"，而那句"ui 基线 0 红"是**空跑**。加 `norm_status()` 归一，并拿 vitest 自报的 `numFailedTests` 与解析出的红数对账，不等就当场 FATAL（这一条在当日日志上双向实测：新代码 2==2 放行，旧代码 0!=2 红）。
3. **core 层的假 mapper 存的是同一个对象引用**：摘掉 `updateById` 在单测里**观测不到**（服务改的就是库里那一格），所以 J3 的 core 半边报 0 红 —— 这不是断言软，是替身把"没回写"模仿成了"回写了"。改成写入/读出都 `snapshot` 一份，J3 随即打出它的 2 条具名红。
4. **`call(..., expect_http=200)` 是一个从没被读过、也从没生效过的参数**（它广告的是"钉 HTTP 状态码"）。删掉；这件事现在由 14 支新探针里显式的 `_s == 400` 做。

**接口层这一格从两句 `ok()` 长成 14 支判据**（每条都带正反两臂，一台恒答 400 的服务器过不了）：词表口 200 与形状、`executable` 非空、`rejected` 每条带原因、`executable ∩ rejected == ∅`、逐种被拒的方式 `/create` 必须 400 且点名这一种、**库里行数不许涨**（拒了不许留账）、正向臂用词表里那一种真创建（状态必须落 `SUCCESS`/`FAILED`，且不许是请求体里伪造进来的 `PENDING`）、`deployLog` 带"个实体"那句汇总、`/detail` 读回来**等于**返回的那一行（界面状态列读的是库）、缺 `deployType` 400、批次不存在 400 并回显那个 id、`appCode` 不存在 400 且说「没有应用」。

**没做的 / 边界**：250 那条腿仍被网络卡住（`_e2e/deploy_250.sh`，见 §2.6 与任务 #67）；`DOCKER`/`GIT_PUSH` 不是"被禁"，是服务器没有那个能力 —— 真加了执行器就把它写进 `DeploymentTypes.IMPLEMENTED`，界面与文档跟着这份数据长。

### 2.13 新撞到的欠账：`@Async` 在这个仓里没有任何机制，物化 trigger 回答 PENDING 而库里那一行已是终态（缺陷 **#71**，09-27 08:32:32 实测）

修 #70 时顺手读了 `MaterializationService.trigger` 的形状，发现它和 #70 是**同一族的另一半**：
#70 是"回答 PENDING 而永远没人执行"，这一条是"回答 PENDING 而其实已经当场跑完了"。两句都不是真话。

静态读出（09-27 08:2x，三条都是现查的）：
1. 全仓 `@EnableAsync` **0 处**（`rg -n 'EnableAsync' --glob '*.java'` 只剩 `DeploymentServiceImpl` 那段解释为什么不走这个形状的注释）
   ⇒ 没有 `AsyncAnnotationBeanPostProcessor`，`@Async` 注解挂在一个没人处理的注解上；
2. 唯一带 `@Async` 的方法是 `MaterializationService.runAsync`（`MaterializationService.java` 的 `@Async` 那一段），
   而它的调用点是**同一个对象里** `trigger` 直接调 `runAsync(entity.getId())` ⇒ 即便补上 `@EnableAsync`，
   自调用不走代理，那句还是装饰；
3. `trigger` 在 `runAsync(...)` 之后 `return toResp(entity, null)` —— 返回的是 insert 时那个**旧对象**，
   它的 `status` 仍是 `STATUS_PENDING`、`fileCount` 仍是 0，而 `run(...)` 是 `selectById` 重读一行再改状态的。

接口上真兑现成什么样（09-27 08:32:32，dev jar 18090，两次调用在同一条命令里连着发）：
```
$ curl -s -X POST 'http://localhost:18090/api/lc/app/materialize?appCode=uiprov316393' \
       -H 'Content-Type: application/json' -d '{"materializationPath":"/tmp/zlc71_probe"}'
  → "id":2, "status":"PENDING", "fileCount":0, "createTime":"2026-09-27 08:32:32", "updateTime":"2026-09-27 08:32:32"
$ curl -s 'http://localhost:18090/api/lc/app/materialize/status?id=2'      # 同一秒
  → "id":2, "status":"FAILED", "errorMessage":"Absolute path not allowed: /tmp/zlc71_probe …"
```
响应写着"还没跑"，而库里同一秒已经是终态 —— 控制器那句注释「异步执行, 立即返回… 可用于后续 status 查询」
（`MaterializationController` 的 `@Operation(summary = "触发物化导出 (异步)")` 同一处）是**广告**，不是兑现。

代价具体是哪三样（不是"文案不好看"）：
- 调用方拿不到结局：`status` 恒 PENDING、`fileCount` 恒 0、`files` 恒空 ⇒ 想知道跑没跑完只能二次轮询；
- "异步"承诺的**不阻塞**没有：整个导出跑在 HTTP 请求线程上 ⇒ 大应用物化时 servlet 线程被占住，
  超时/并发上限全在这条路上，而不是在真正的执行器上；
- `/status` 那条"后续查询"路径于是成了唯一说真话的地方 —— 而**前端的部署页当时并没有读它**（#70 的另一半）。

修法（**尚未动手，等排产**）——三条路各自都有代价，先记口径：
- 甲 承认它是同步的：删掉 `@Async` 与"异步"文案，跑完再回答（像 #70 那样把真结局写回那一行并原样返回）。
  最便宜，且和 #70 的处置**逐字同形**；风险是大应用的请求耗时。
- 乙 把异步做出来：`@EnableAsync` + 把 `runAsync` 拆到另一个 bean（或注入自身代理）⇒ 真线程池。
  要一并回答"响应里给什么、谁去轮询、页面怎么显示 RUNNING"（现在 `DeploymentEntity.STATUS_RUNNING` 那个常量
  在本仓**零写入者**，界面也没有 RUNNING 这一档的显示口径）。
- 丙 只做接口诚实：保持同步执行，但 `trigger` 返回**重读后**的那一行。这是甲的最低配。

我的建议：**先走甲/丙这一类**（一句话也不比现在更假，且不引入线程池这一整片新面），
把乙作为"确实需要并行导出"时的独立一项再拍。裁定前不动实现。

关联：[[#2.12 缺陷 #70 部署中心]]（同族另一半）、`_e2e/README.md` 的"部署演练"那一节（250 上跑的就是这条链路）。

### 2.14 新撞到的量具缺陷：接口层那个 z-wf 桩只听 IPv4，而 jar 里的 JVM 往 IPv6 连 ⇒ `[15w]` 整节量不到，报的却是"jar 没打过来"（缺陷 **#72**，09-27 09:0x 实测并修）

**症状（现查）**：加上 #70 那 14 支探针之后重跑接口层，`516/546`、**30 条红**，全部落在 `[15w]`（#61 那一族），消息一律是「桥没通（桩没起来或 jar 没打过来）⇒ 这一条没有判定，不算绿」；而我新加的 14 支 #70 探针逐条 PASS。同一份 jar、同一个量具，08:50:51 那一跑还是 `532/532`。

**归因（三步，每步一格证据，不是推的）**

1. 拿一个**全新的** app/实体/绑定手工走一遍（`dbgwf090159`）：写记录返回 `data:1`，桩 `count=0` ⇒ 与 `SUF`、与累计状态都无关，桥此刻就是断的。
2. 读 `GET /api/lc/workflow-binding/fires`：那一行是 `FAILED`，`detail` 写着
   `POST /api/approval-center/processes/start http=0 err=Execute failed: Failed to connect to localhost/[0:0:0:0:0:0:0:1]:8888`（fire 行 id 49/50）
   ⇒ **派发发生了**，只是连不上；同一时刻 `lsof` 显示桩只在 `IPv4 127.0.0.1:8888`，而 `nc -z ::1 8888` = closed。
3. 换成一根**同时**听得见 `::1` 与 `127.0.0.1` 的 loopback 桩，再写一条 ⇒ fire 行 id **51 `STARTED` / `PI-DIAG-1`**。

**根因（能证到的那一层）**：`WfAdapter` 的默认 `base-url` 是 `http://localhost:8888`，传输是 z-util-http 的 `HttpExecutor`（okhttp 4.12，栈顶 `RealConnection.connectSocket`）；量具 `_WfStub` 只绑 `("127.0.0.1", port)`。这台 Mac 上 JVM 把 `localhost` 连到了 `::1`。

**⚠ 没解释清的那一半（写明，别顺嘴编成因）**：同一根 v4-only 的桩在 08:50:51 那一跑是**通的**（`532/532`，`[15w]` 逐条 PASS），08:54:13 起就不通了 —— 同一个进程、同一份配置。中间"翻了一下"的那一下我没量到（JVM 侧 `localhost` 的解析/路由顺序是唯二候选，但我手里没有任何一把尺能回看当时的路由选择）。所以这条只记结论：**修完之后不依赖它翻哪一边**。

**改了什么（`_e2e/e2e_api_test.py`）**

- `_WfStub` 不再是"一个 HTTPServer"，而是 `_WfState`（一本账）+ 两根 `_WfEndpoint`（`127.0.0.1` 与 `::1` 各一，`ThreadingHTTPServer` 子类，`address_family` 在建 socket 之前按 host 定）。两族各绑一个 **loopback** 地址，而不是 `::` + `IPV6_V6ONLY=0`：后者一根就够，但那等于把一个"能发起流程的端口"暴露到局域网（`lsof` 会显示 `*:8888`）。
- 只剩一族可用（禁了 IPv6 的容器）时照跑，但把缺掉的那一族写进"桩绑得上"那条读数里 —— 不许再沉默一次。
- `WF.requests / mode / hang_seconds` 改走 `property`，本节其余 20 多处读写点一字不动。
- **红消息带回原因**：新增 `wf_bridge_words()`，桥没通时去 `/fires` 把 jar 自己写下的那句 `detail` 抄进判红消息。这一次从"30 条红 + 一句不指向的话"到定位隔了 15 分钟，而那句话一直都在库里。

**双向实测**：改前那一跑 30 条具名红（`~/.cache/zlc70/gate2-api-70.out`，`516/546`）；改后同一台机器、同一份 jar（重建后 inode 141692193）**两跑逐字同数** `546/546`、`^  FAIL` 计数 0（`gate2-api-dual.log` 09:08:40、`gate2-final.log` 09:18:22）。

**同形状还留着的两处（只登记，本轮不动）**

- `_e2e/wf_stub.py` 里那句 `HTTPServer(("127.0.0.1", args.port), Handler)` 是 250 部署腿用的桩，同一个形状。本机量不到 250 ⇒ **改一条不可复现的链路是拿猜测换代码**，等 §2.6 那条腿通了第一件事就是"绑两族 + 断言 `::1` 也答"。
- `z-lc-admin-ui/e2e/browser-e2e.mjs` 的 bundle 新鲜度闸拿 **mtime** 比（本轮被它挡下：`src 里最新的文件比 index-BMjMtCQR.js 新 274s —— 这份 bundle 不是当前源码构建出来的`）。注入量具按字节还原源码，而还原会把 mtime 刷新 ⇒ 这把尺会**误报**"bundle 不是当前源码建的"。它错得保守（拒跑，不产假绿），所以本轮只重新 `npm run build` 过闸、**没动它**；真要修是换成"src 内容哈希 vs bundle 内嵌哈希"，与 #69 是同一件事。

---

### 2.15 新撞到的量具缺陷：量具用 `Popen` 起 jar 却不起新会话 ⇒ 量具一退出 jar 跟着收 SIGTERM，而它收完**不退**（缺陷 **#74**，09-27 10:4x–11:0x 实测并修）

编号依据（现读，不是接着上一节顺推）：`grep -ro "#7[0-9]" --include="*.md" _doc README.md` 里最大的是
**#72**，**#73** 已被"部署中心在真浏览器层零断言"那一支候选占着（任务 #75）⇒ 取 **#74**。

**症状**：`_e2e/mutate_fire_window_guard.py` 收线之后（10:47:15 打完原始字节那份 jar），
10:56 现读 18090 仍被 pid 30561 占着，而它的 `/health` 回的是
`{"status":"DOWN", "detail":"NoClassDefFoundError: com/alibaba/druid/stat/JdbcStatManager"}`
—— HTTP 200、端口在听、**类加载器已经关掉**。`~/.cache/zlc65mut/mut_boot.log` 里
10:47:24 那一行是 `SpringApplicationShutdownHook`，此后 11 分钟没有一条活动日志 ⇒
它是一个"已经决定要死、但没死成"的半死件。

**根因**：`restart()` 里 `subprocess.Popen(["java", "-jar", ...])` **没有 `start_new_session=True`** ⇒
那个 JVM 属于量具自己的进程组。量具一退出它就跟着被信号带走；而它**收 SIGTERM 不收干净**
（druid 的建连线程在 shutdown 里被 interrupt，`close()` 又撞 `NoClassDefFoundError`）⇒ 端口留着。
后果不是"难看"，是**下一轮起不来**：任何一支带端口归因判据的量具都会撞上
`端口 18090 在 60s 之后仍被占着，不敢起新件`（这一句是对的，但它是被上一支量具的遗留顶出来的）。

**修法（两处，8 个站点）**：
① Popen 一律 `start_new_session=True`；② 那一支的停旧件路径：SIGTERM 等 60s 不效 ⇒
对**已归因的那一个 pid**（argv 里就是本次这份 jar）升 SIGKILL，再等 30s，仍不空就抛。
扫出来的站点：`for f in _e2e/*.py` 里 `Popen("java","-jar"` 共 **8** 处，其中 7 处兄弟量具
（`mutate_workflow_deployed_guard` / `mutate_duplicate_guard_deployed` / `mutate_edit_path_deployed_guard` /
`mutate_field_code_deployed_guard` / `mutate_permission_deployed_guard` / `mutate_pipeline_wiring_guard` /
`mutate_provision_deployed_guard`）与 fire_window 同一形状，一次改齐；
`mutate_health_honesty_guard.py` 那处 `Popen` 打的是 `mvn` 管道、不是 jar ⇒ **不动**。
改后 8 个文件逐个 `py_compile` 过，重扫 `start_new_session=True` 命中 **8/8**。

**验的是"修法有效"，不是"整族重跑"**（10:58:45 直接在那台真卡住的样本上复跑修好的 `restart()`）：
`旧进程已收干净（等了 15s, 端口 18090 空）` → `归因成立：18090 的监听者 pid=35401，argv 里就是
z-lc-admin-1.0.0-SNAPSHOT.jar`；11:04:25 回读：`pgrep -f mutate_fire_window_guard.py` **空**（量具早退了），
而 pid 35401 `etime 05:40` 还活着、`/health` 回 `UP` ⇒ 解耦成立。

⚠ **这一格欠的那一半要写清**：没做注入自证。原因是它不是能被断言的产品性质，而是进程生命周期 ——
"摘掉 `start_new_session=True` 就复现"这一支在结构上要打的是**上一轮已经付过的那次现场**
（真卡住的 JVM 只能造一次，除非写一支专门把 jar 起在子进程组里再杀父进程的探针）。
按"覆盖缺口"记账，不算已闭合。那 7 处兄弟量具本窗**一支都没有为此重跑** —— 它们与 fire_window 逐字同形，
但"同形"是我 grep 出来的，不是跑出来的。

### 2.16 新撞到的套件缺陷：一次注入把界面推进"没有第 2 页可点"的形状，那一节 12 条检查跟着点击一起消失（缺陷 **#75**，09-27 12:1x 实测并修）

**这不是"量具没抓到"——量具抓到了**：`mutate_workflow_browser_guard.py` 每轮都把分母钉成基线那条检查数，
W22（`FIRE_PAGE_SIZE` 20→200）那一跑逐字报出
`22_W22: PASS+FAIL 273 / 红 4` → `!! W22 …: 本轮分母 273 != 基线 285 —— 某一节中途没跑完，「没红」不能算通过`
→ `!! W22 …: 整节中途抛错（流程绑定的发起账（#61 浏览器层） 红了）—— 这一轮的归因不成立，后面那些「没红」是「没跑到」`，
末行 `RESULT: 3 problem(s)`。**病在套件**：`e2e/browser-e2e.mjs` 的 `8b` 段点翻页那一颗按钮写的是
裸 `.click()`，而 25 条账全落在一页里时 `.ant-pagination-item-2` **根本不存在** ⇒ Playwright 默认超时抛出
⇒ 被 `11w` 整节的 `catch` 接住 ⇒ 那一节剩下的检查一条没跑。

**为什么只有这一处**：全仓扫 `.ant-pagination-item` 命中 **1** 处（就是这一处）。同一份套件里
"点了可能点不到"的交互此前已经一律写成 `.click(...).catch(() => {})` —— 现数
**带 `.catch` 的 click 站点 = 6 处**（含刚修的这颗：`.zlc-cal-*` 色块那颗、`.ant-modal-footer` 取消那颗 ×2、
`.ant-drawer-close` 那颗、`span` 上那颗），也就是说这颗是**唯一的漏网**，不是惯例缺失。
**没有把其余 62 处不带 `.catch` 的 click 全改掉**：那些点的是"必然存在"的控件，改它们是替不存在的风险写代码。

**修法与实测**：那一处补 `.catch(() => {})`，让"点不到"退回成**它下面那几条检查各自红**而不是整节消失。
修完复跑 `--only W5,W22`（`~/.cache/zlc61/browser_guard/narrow_w5w22b_0927_1221.out`）：
基线轮 `PASS+FAIL 285 / 红 0` ⇒ **这一改对绿跑零影响**；W22 `285 / 红 5` 逐字
`OK … 预期 5 条全红，无一条连带红`；W5 `285 / 红 3` 同样 OK；还原轮 `285 / 红 0`；
末行 `RESULT: 11w 的 37 条各自钉住一件事（22 支注入）；27 条按未覆盖记账（三类理由在文档与 NOT_COVERED 里）`。
⚠ 顺带把 W22 的**预期红集**从推出来的 4 条改成实测的 5 条（漏的那一格是"翻页那次请求"）——
若不是它先崩成 273，我会把"预期 4 / 实跑 4"当成对齐：**预期红集也要有猎物**，与 #74 那一格是同一课。

⚠ **仍欠的那一半**：这一族（"注入把一个可点击控件变没了 ⇒ 整节消失"）在其余 5 支浏览器量具里
**没有各自的复跑**。它们的分母检查与 `11w` 这支同一形状（各钉各的基线数），所以症状出现时会同样报
`本轮分母 != 基线` —— 但这句是**从这一支的行为推那一族的**，不是跑出来的。按覆盖缺口记账。

---

### 2.17 缺陷 **#73** 收线：部署中心在真浏览器层从零断言到 28 条 —— 这一窗的三处返工都坏在我自己的尺上（09-27 14:5x–16:3x 实测）

**交付**：`z-lc-admin-ui/e2e/browser-e2e.mjs` 新增 `11x` 段（28 条 `check` + 1 条整节兜底标题），
浏览器层分母 **285 → 313**；永久量具 `z-lc-admin-ui/e2e/mutate_deployment_browser_guard.py`
（21 支注入，每支只摘一句保证，红集互不相同）。这一段量的是「词表 → 下拉与被拒理由 → 点一次真执行 →
表里那一行 → 运行时库里那张表 → 抽屉原话 → 两处"读不到"」，正是 #70 把部署中心改成"当场执行"之后
**只在 vitest 里被 stub 词表证明过**的那件事。

**run1（整族 21 支，`~/.cache/zlc73/guard_run1.out` + `~/.cache/zlc73/browser_guard/*.log`）**：
静态体检 `11x 分母 29 条（扫自源码）、21 支注入`；基线轮 `313 / 红 0`；**16 支 OK，5 problem(s)**。
那五支分两类，两类都是量具的账而不是产品的：
1. **预期红集是推出来的**：D11（把词表闸写反）推 3 条、实跑红 **19** 条；D21（点一次发两个 create）
   推 2 条、实跑红 **3** 条。多出来的名字全部在 11x 段内 ⇒ 按实测改账，**断言一个字没动**。
   D11 从此在源码注释里写明它是**粗筛不判别**（把「新建部署」永久 disable 会带走整节），
   判别那一支是 D10（只摘闸 ⇒ 恰好 1 条红）。
2. **红在 11x 之外**：D7/D8/D10 三轮里出现 `解绑与删应用本身不发流程（…）` 这类**别的节（`11w` 的桩计数）**
   的红，run1 里 3/22 轮。这一格**既没写成预期也没加白名单** —— 加了新判据：
   `红不在 29 条分母里 ⇒ 本轮归因不成立，点名 --only <tag> 重跑`（我的注入只改 `DeploymentsPage.tsx`，
   而分母是扫 `browser-e2e.mjs` 得到的，所以"属不属于这一节"是机械可判的）。

**run2（收窄复跑那五支，`~/.cache/zlc73/guard_run2.out` + `~/.cache/zlc73/browser_guard_run2/`）**：
基线 `313 / 红 0`；`D7 红 1`、`D8 红 7`、`D10 红 1`、`D11 红 19`、`D21 红 3` 逐字
`OK … 预期 N 条全红，无一条连带红`；恢复轮 `313 / 红 0`。
⇒ 改过的预期与真浏览器轮次对齐；而 run1 那三轮的**外来红一次都没复现**（1/22 → 0/5）。
这一格照 #76 记为**未归因**，不写"已排除"。

** RESULT 那一行自己也是账，它当时在撒谎**：run2 印的是「11x 的 **24** 条各自钉住一件事；**7** 条按未覆盖
记账」= 31 条，而分母只有 29 —— 因为三条名字同时进着 D11 的预期红集又躺在未覆盖账上，被我**重复计了两笔**。
改成三堆并加了一条加和闸（`专属 21 + 连带 3 + 纯记账 4 + 兜底 1 = 29`，对不上就 `RESULT` 判错）。
阴性对照跑过：把整节兜底那一条也记进未覆盖账 ⇒ 印出 30，当场被这条闸拦下。
顺带把记账口径掰开：「**没有专属猎物注入**」≠「**从来没红过**」—— `not_covered()` (a) 那三条是实测
22 轮零命中（复算命令写在那格理由里，阳性对照 = `恢复之后再读一次` 同一条 grep 命中 2）。

**提交树被污染那一起（另立缺陷 #77）**：本窗 reflog 里 13:43–14:38 有六个 `chore(sync): 收口工作树未提交改动`，
`DeploymentsPage.tsx` 的字节逐个不同，其中 `19c20c8` 那份 md5 = `22e5cc8e…` **正是 D20 的变异**，而且已经推走
⇒ `origin/main` 里进过注入态产品字节。修 = 新提交 `6a09b12` 把字节归位（不 amend、不 force-push），
并在这支量具里加了开跑/收线各读一次的 `git_blob()` 对账（HEAD 那一版 vs 工作树，同一 sha1 可直比）：
真猎物现读得到 —— 本量具自己 `HEAD e9fb231a… != work ece056d3…`（我改过没提交 ⇒ 起点那一行后面多印
「不同 ⇒ 工作树有未提交改动，这一跑量的是工作树不是提交树」这一句），
`DeploymentsPage.tsx` 现读 `HEAD == work == 696258f3ed43`。

**改完尺之后的三跑收窄复跑，加上它们自己撞出的缺陷 #78**（09-27 16:0x–16:3x 实测）：
run3（`--only D10`，`~/.cache/zlc73/guard_run3.out`）基线 `313 / 红 0` → `10_D10: 313 / 红 1` 逐字
`OK D10 摘掉「新建部署」的词表闸: 预期 1 条全红，无一条连带红` → 恢复轮 `313 / 红 0`，
末行 `RESULT: 21 支注入把 11x 的 21 条各自钉住一件事；另 3 条没有专属猎物、只在别人的粗暴注入里连带红过；
4 条从没红过的格按未覆盖记账（合计 7 条、5 类理由在文档与 not_covered() 里）` ⇒ **三堆加和那把新闸在真轮次里过**。
run4（同收窄一支，`guard_run4.out`，16:05–16:17）产品读数与 run3 逐字同，但它**是第一跑带着 `git_blob()`
进真轮次的**，而那把尺的收线那一行**一个字都没印**：`grep -c "字节对账" guard_run4.out` = **1**（只有起点行）、
`grep -c "字节对账收线"` = **0**（现读）。⇒ 另立 **缺陷 #78**：收线读数与它自己那两条 `!!` 判据写在同一个 `if` 里
= **只在出问题时才印** ⇒ "这一跑期间提交树没被推进"这句在日志里没有产出物，事后想记账只能另开 git 命令复算，
而那已经不是这一跑的证物了。与 **#64**（`status` 归因行从 #55 起从未打印）、与 ① 的"缺席读数"同一形状。
修 = ① 收线行**无条件**印（字节相等时结论句自己写「与起点逐字相同 ⇒ 还原回到了我测量的那一版，且这一跑期间
提交树没被推进」）；② 整段搬进 `finally` —— 原来"基线有红"与"数不出分母"那两条 `return 2` 中止路径
根本不走到对账，而那恰恰是最该知道提交树动没动的时刻。
四臂桩跑 `~/.cache/zlc73/probe_reconcile_lines.py`（末行 `PROBE RESULT: 0 problem(s)`；把 `build`/`preview`/
`run_round` 换成桩、`PAGE` 指到 `tempfile.mkdtemp` 副本 ⇒ **不 build、不开浏览器、真工作树一个字节没动**，
收工 `md5 -q DeploymentsPage.tsx` = `c1f518bbeb68…` 与开跑前同值）四臂各一猎物：
①字节相等 ⇒ 印结论句且**不出** `!!`（这一臂是 ③ 那把新闸的阳性对照）；②工作树被换 ⇒ `!! 收线时工作树字节 … != 开跑时 …`；
③HEAD 被推成别人的字节 ⇒ `!! … 提交树被推进过`；④HEAD 被推而进去的正是我这版 ⇒ 走 `⚠ … 没污染` 而**不**出 `!!`
（③/④ 互斥，各是对方那一支的负控）。
⚠ 桩跑只证"收线三岔有猎物"；真轮次那一跑 = **run5**（`--only D20`，正是 #77 里把注入态带进 HEAD 的那一支题号；
`guard_run5.out`，16:21:11 起、末次写日志 `16:30:57`，末行 `RUN5_EXIT=0`）：基线 `313 / 红 0` →
`20_D20: 313 / 红 1` 逐字 `OK D20 挂载时多读一次列表: 预期 1 条全红，无一条连带红` → 恢复轮 `313 / 红 0`，
`grep -c "字节对账" guard_run5.out` = **2**（run4 是 1）而收线那一行逐字
`字节对账收线：提交树 DeploymentsPage.tsx blob=696258f3ed43… / 工作树=696258f3ed43… —— 与起点逐字相同
⇒ 还原回到了我测量的那一版，且这一跑期间提交树没被推进` ⇒ **#78 的两半（无条件印 + `finally`）在真 build/真浏览器轮次里成立**。
⚠ 同一跑期间现场读 `md5 -q DeploymentsPage.tsx` = `22e5cc8ed67f…` = **上次被 `19c20c8` 带进 HEAD 的那一份变异字节**
⇒ 我这窗的提交步一律等它回到 `c1f518bbeb68…` 之后才做。这只是**缓解**，不是 #77 的修法（见下面那一格第 3 条）。

**四道闸同轮重测（15:37:00–15:48:30，一条串行链，`~/.cache/zlc73gates/run_gates.out`）**：
闸1 `mvn -o -B clean install` rc=0、`BUILD SUCCESS`、`Total time: 24.696 s`、
reactor **4790** 例 / 失败 0 / 错误 0 / 跳过 0（模块汇总 2701 + 525 + 1338 + 112 + 114），jar 时刻 `15:37:25`；
闸2 接口层本机 H2 **555/555**（rc=0）；闸3 `npm run check` rc=0（tsc + `eslint --max-warnings 0` +
vitest `Test Files 30 passed` / `Tests 264 passed` + `built in 2.94s`）；
闸4 真浏览器 `E2E_REPEATS=2` rc=0，两轮各 `PASS 313 / FAIL 0`、`全绿轮次: 2/2`，
并且 **11x 那 28 条逐字出现在 PASS 标题集里**（这条不是"总数对上了"就算：阳性对照 = `11w` 那条也在同一集合里，
阴性对照 = 现编的名字不在）。整条链前后 `DeploymentsPage.tsx` 的 md5 都是 `c1f518bbeb68…` ⇒ 闸 4 量的确实是修复态。

⚠ **仍欠的四格，按未覆盖记账**：
1. `11w` 那两条计数检查的**外来红**没有归因（缺陷 **#76**：桩收到比创建次数多的请求，而它记了 path+body
   却一个字都不印，所以那一句幽灵请求属于哪个 app/businessKey 无从指认）；
2. 提交树那一层只在**这一支**量具里有了对账，其余 **42 支**没有 —— 现数
   `ls z-lc-admin-ui/e2e/mutate_*.py | wc -l` = 21、`ls _e2e/mutate_*.py | wc -l` = 22；
   而 `chore(sync)` 不挑分支，它扫的是整个工作树；
3. **而且 #77 至今没有结构性修法**：`git_blob()` 是**事后检出**污染，不是防止污染 —— 变异态在产品文件里躺着的
   那几分钟（每轮 build + 一轮浏览器 = 数分钟量级），别人的 `git add -A` 随时可以把它扫进提交树。
   本窗又一实证：run5 的 D20 轮期间现场读 `md5 -q DeploymentsPage.tsx` = `22e5cc8ed67f…`，
   **正是上次被 `19c20c8` 带进 HEAD 的那一份字节**（所以我这一窗的提交步一律等还原 + md5 回 `c1f518bbeb68…` 之后才做）。
   下一支的候选做法（**本窗没做，别当已完成**）：把浏览器注入族改到**独立工作树**里跑
   （`git worktree add` 一份临时目录 + 把 `node_modules` 软链过去，vite/preview 都在那份里起），
   这样别人的 `git add -A` 结构上看不见变异态，而对账只需保留"我这版字节进没进提交树"那一层；
   代价是这一族 21 支都要改路径解析（`UI`/`DIST_INDEX`/`LOG_DIR` 三处），且要先证"软链 `node_modules` 起得来 vite"。
4. 250 那半条腿这一窗仍未重测（#51/#67 那条线上的闸 6 还挂着）。
   ⚠ **已被下一窗（09-27 16:39 – 17:0x）补上并闭合，见下面 §2.18**；这一句按原样留着，因为它记的是
   "当时那一窗没量"这件事本身，而它当时的理由（"06:40 起失联"）后来被证明是有保质期的。

---

### 2.18 250 那条腿重测：闸 6 第一次有正向读数（闭掉 §2.17 欠账第 4 条），而撞出的四件事**全长在量具上**（缺陷 **#79/#80/#81/#82**，09-27 16:39 – 17:0x 实测）

**先说被推翻的前提。** §2.17 末尾那句"250 那半条腿这一窗仍未重测"的理由是"06:40 起失联签名已判完"。
它只对 06:40 – 14:03 那一段成立：另一会话 14:03 重启过 250，本窗 16:39 现读 `ssh 250` 会话起得来、
`ss -lntp` 看得见 mysqld。纪律本身没错（"判完失联签名就停，别敲 ssh"），错在**把某一时刻的签名当成永久状态** ——
没有任何尺会告诉我那个签名过期了。⇒ 补一条：**引用"某机失联"之前先量一次；量不出来就别写"失联"**，
写"上一窗量到的是失联"。

**这一窗量到的**（逐字读数在 `~/.cache/zlc250_0927/`；全文见 `_e2e/README.md`「部署演练怎么跑」那节末尾的
**第二次真跑 250** 小节 + 交接表最前面那张 **09-27 16:39 – 17:0x** 的表）：

- 部署链 `sync → db → schema → env → start → verify → collate` 全绿：jar 字节对账
  `2ad715845a94eda1d8c2ddfd5e688859`（**= 本机 15:37:25 构建那一份**，本窗 java 零改动 ⇒ 部署腿量的是上一窗的件，
  这一条要写明，别让人以为本窗重编过）、`库 z_lc 里的表数：72`、两池 `database` 逐字 `MySQL 8.0.26`、
  `z_lc_app` / `z_lc_event` 各 `rows=1`。
- 四道闸 + healthproof（闸 5）：`gates_fixed.log` **14 条 ✓、`RC=0`**，每一道都在同一次运行里先具名红过。
- **闸 6 `fireprobe` 第一次有正向读数**：`fireprobe2.log` **24 条 ok、红 0、`FIREPROBE2_RC=0`**，末行
  `✓ 流程发起的账在真 MySQL 8 上写得进、读得出：STARTED/FAILED 各一行由库自己承认，桩收到句数与账对齐`。
  ⇒ §2.17 欠账第 4 条**这一格闭**；#67 那条"闸 6 从来没在任何窗跑成过正向"的账也一起结。
- 接口层打 250：修完之后**两跑各 `555/555`**（`api250_bridge.log`、`api250_bridge2.log`，后者是现字节那一版、
  `API250_BRIDGE2_RC=0`、17:08:05 落盘），其中 `[15w]` 本节 **72 条 PASS / 0 条 FAIL** —— 这一节第一次真打在
  部署件 + 真 MySQL 8 上。改之前那一跑是 `515/555`，40 条红全在这一节（下面 #82）。

**四件事的形状各不同**（`git status --short` 本窗只有 `_e2e/deploy_250.sh`、`_e2e/deploy_250_remote.sh`、
`_e2e/e2e_api_test.py` 三个文件 ⇒ **产品代码零改动**，四件全是量具/部署脚本的；每一件都留有"先红的那一跑"可复算）：

| # | 红的那一跑（逐字） | 根因一句话 |
|---|---|---|
| #79 | `db.log`：`docker: Error response from daemon: Conflict. The container name "/z-lc-deploy-mysql" is already in use…`；负控另有一跑 `db_port_red.log`：`!! 容器 z-lc-deploy-mysql 映射在 127.0.0.1:33061，而这里要说的是 33999` | 机器重启后容器是"存在而停着"的，脚本只有"在跑/不在"两支 ⇒ 走到 `docker run` 必撞名；而"就绪"那一圈用 `docker exec mysqladmin ping`，**根本不碰宿主端口**，映射被改了查不出来 |
| #80 | `gates.log`：`!! 闸1: SPRING_DATASOURCE_URL source 之后是空的 —— 配置文件里的值没加引号`、`!! 闸1: ZLC_WF_BASE_URL 是空的…`、`GATES_RC=1`（敲的就是 README 那条标题命令） | 闸 1 的"恢复"一步是重跑 `step_env`，而 `step_env` 要的 `ZLC_WF_BASE_URL` 只在调用者的环境里 ⇒ 还原步自己卡住，整批红在**量具自己留下的脏状态**上。改成按**本轮参照**还原字节 + `cmp` 对账 + 过一遍自己的校验才举旗 |
| #81 | `fireprobe.log`：**21 ok / 2 红**、`!! 闸6: 2 条没过关`、`FIREPROBE_RC=1`；两条红分别印着 `key=None 期望=fp_expense_29115 body={… "processKey":"fp_expense_29115" …}` 与 `FAILED||z-wf ????: ??????: business key ???` | 三合一：判据读了 **DTO 侧的 `processDefinitionKey`** 而线上那一格叫 **`processKey`**；`docker exec mysql` 没有 LANG ⇒ 中文按 latin1 读成一串 `?`；"引擎原话"的期望串没带归属前缀 = **期望没有猎物** |
| #82 | `api250.log`：`=== E2E RESULT: 515/555 passed ===`，40 条红逐条 `桥没通（桩没起来或 jar 没打过来）⇒ 这一条没有判定，不算绿`，而**同一段里 `PASS z-wf 桩绑得上 8888` 是绿的** | 那 40 条读的是**测试进程自己内存里**那本桩账，jar 在 250 上打的是它自己 `app.env` 点名的 18888 ⇒ **本地端口绑上 ≠ 桥通**。修 = `api` 默认建反向隧道（250:127.0.0.1:18899 → 本机同端口），把部署件的 wf 目标临时指过去，跑完按保存的原值还原并重启；不复用 8888 是因为 250 上 `*:8888` 是别人的 java（`ss -lntp` 现读 pid 1655） |

**这一窗我自己又踩到 / 又看清的两条**（会复发，所以写进制度而不是只写进日志）：
1. **"我验的是部署件"这句必须能在日志里被反驳**。#82 那一跑里，绿的那格 check 断言的只是"本机这个端口我绑上了"，
   而它括号里写着"jar 的默认 base-url 就去这里" —— 一个量具在**它结构上不知道的事情**上举了旗。
   ⇒ 桩/端点类 check 的文案要跟着真实形状分叉（本窗已改：默认分支印"就是它默认指向的那个端口"，
   隧道分支印"由部署件显式指过来，不是默认值"），并且**默认分支那一格的文案改动要有冒烟**：
   `api_dev_afteredit.log` 复跑证明改字之后的默认分支与改前**逐字同**（任何按名字比红集的旧账不会因它错位）。
2. **自清理的量具不要事后取证**。闸 6 的 `trap 'fp_clean' EXIT` 会删掉探针行，跑完再 `docker exec mysql` 去查
   "那一行到底存不存在"必然是空 —— 那不是反证，是清场。⇒ 判别器要做进闸里（这一窗就是这么加的：
   同一句原话由 **CLI** 与**应用那条 JDBC 读路**在两把尺上互证，且落在同一次运行），别指望事后手查。

**没做的事（别当已完成）**：
- java / 前端 / 真浏览器三层本窗**没重测**（三个改动文件它们读不到）；当前数仍见 README 那张 **09-27 15:37 – 15:48** 的表。
- 反向隧道这一格只**自己**验了"250 那侧洞在听"（`bridge_up` 现读 `ss`）；"洞对面那个东西就是**我这个**桩"
  是由后面那 72 条判的，不是由 `bridge_up` 判的。要把它做成独立一道闸，缺的是"洞起来时桩还不该活着"那一支负控。
- `--restart unless-stopped` **没有**加到 `z-lc-deploy-mysql` 上（那台机上容器不只我的在用）；本窗只走 `docker start` 那一支。
- 闸 6 的 `fp_check` 里"发出去那一句带的是登记的 KEY"这一条，**修完之后读的仍是 body 的 `processKey`**：
  也就是说线上字段名与 DTO 字段名**不同名**这件事仍然在，只是量具改读对了那一格。要不要统一命名是产品决定，
  本窗不擅自改（改了会同时动 java + 界面 + 契约层三处断言）。

---

## 3. 等你（主编/CEO）拍的五问 —— 我先按默认值做了，但默认值**不算裁定**
（"五"是 06:2x 现数的：`awk 'NR>=659 && NR<=700 && /^[0-9]+\./' TASK.md` = 5 行（第 661/664/665/668/671 行）。
上一版标题写"四问"，是加第 5 问（租户那一问）时忘了改计数 —— 标题里的数字也是一句断言。）

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
5. **流程绑定算不算多租户特性**（§2.9 / 缺陷 #67）？今天的实况是"写侧钉 `default`、读侧按记录自己的租户"
   ⇒ 非 `default` 租户的记录**永远不发单且一声不响**。
   ⇒ (甲) 绑定按实体所属租户存（对，但要先把管理面的租户归属/认证补上）／(乙) 登记当场拒（我倾向这一条）
   ／(丙) 明确宣布"低代码运行时这一版只支持 `default` 租户"，那就该在**运行时的写入口**拒非 default 的 create，
   而不是只在流程这一格静默跳过。**这一问不给答案，我就按 (乙) 做**（把沉默变响，不新增权限面）。

### 3.5 我对其中前四问的建议（第 1–4 问；**第 5 问（租户）的默认值写在它自己那一行里，本节没有替它建议**（09-27 05:3x 写；**建议不是裁定**，拍板前不动实现）

1. **范围：建议本轮就接 `AFTER_UPDATE`，`AFTER_DELETE` 另立一票。**
   理由分两层。① 界面上那两个选项是本轮 §2.2 亲手从词表长出来的，"词表说支持、写入口 400"这个形状
   本身正是缺陷 #61 的病灶 —— 我在 java 层 W17 钉的就是"部署的构件不承认的选项不许出现在界面上"，
   那么反过来"构件承认却没接线"同样不该留在词表里；要么接，要么从词表里摘下去，不能停在中间。
   ② `update` 那条挂点比 `delete` 干净得多：记录已存在、business key 稳定、撤销路径的语义清楚
   （撤销一次更新＝回到旧值，不该再发一次流程）。而 `delete` 要和**逻辑删除**一起判
   （`deleted=0` 那一栏：删了还能恢复 ⇒ 流程发不发？恢复算不算一次新建？）—— 这三个问题里每一个都是
   产品裁定而不是工程量，硬接上去只会造出下一个"存了但按你想的语义不一样"。
   ⇒ 所以我的默认值是：接 `AFTER_UPDATE`（含它自己的成对注入），`AFTER_DELETE` 留在词表外
   （界面不再给这一项），等这一问拍完再单独开一票。

2. **批量导入 / 撤销重做：建议维持"不算触发点"，但把"不算"变成看得见的。**
   现在它只在 §2.1 钉成守卫（写入口拒），这是对的；但要补一条：导入的结果摘要里必须写
   "N 条已写入，流程未发起（批量导入不是触发点）" —— 否则运维会以为审批流跑了，而这是
   与 #61 同族的"沉默的不兑现"。工程量小（导入摘要那一行本来就有），值得同批做。

3. **`autoSubmit=0`：建议**把这一列从界面上彻底摘掉**，并在 java 侧把它读成"永远派发"。**
   三个候选里另两个都更坏："写入口拒"（今天的默认）等于把一条**已经存在的历史配置**变成
   一具谁都不能碰的尸体 —— 用户看到 400 而不知道它从哪来；"存着但永不派发"就是缺陷 #61 复刻。
   摘掉 + 读成恒真，是**唯一**一个让存量行和增量行语义一致、且界面不再撒谎的做法。
   ⚠ 这一条改的是既有语义，所以必须配一次数据体检：250/136 那两套库里
   `select count(*) from z_lc_workflow_binding where auto_submit=0` 到底有几行 —— **0 行**则这一改
   纯清理，**>0 行**则要先给运维一条迁移说明（哪些绑定会突然开始发流程，是要人知道的）。
   我没有量过这个数，所以这条建议的前置是"先量它"。

4. **发起失败不回滚：建议维持不回滚。** 外部引擎不可用把用户已经填好的表单一起带走，
   是比"审批没发出去"更差的用户结局，且和 `z_lc_workflow_fire` 这张账表的设计意图一致
   （存在就是为了"这一单没成也留痕"，§2.6.3 的正向那一跑钉的就是 FAILED 行带引擎原话）。
   但补一条硬的：**界面必须把 FAILED 那一行摆出来**，不能只在库里 —— 这一条本轮 §2.2/§2.5 已经在做
   （`/fires` 回读 + 浏览器层 59 条），所以拍板时可以说"兑现面已存在"，不是空头承诺。

---

## 4. 收口口径（照 #41/#48 那一族的既有标准）

**本窗新撞的四支，先在此归堆**（免得收口时只当"§2.6 那条腿没跑完"）：
#64 部署量具 `status` 归因行（§2.6.4，**已修**，修后 rc=0 实测）、
#65 `/fires` 静默截 200 无指示（§2.7，未修）、
#66 `update` 的查重结构上打不到行（§2.8，未修，机制待运行时证）、
#67 绑定钉 `default` 而派发按记录租户（§2.9，未修，等 §3 第五问裁定）。
其中 #65/#66/#67 三支都**不在**本轮 W1–W18 那 18 支的猎物清单里 —— 也就是说注入自证跑绿
不等于这三行没有红可报；这一句写在这里，是为了下一窗别把"18 支全对"读成"流程这一族全证过"。

- 六层同窗重测：java 全量（本轮 `z-lc-core` **1326 例 / 0 失败 / 0 错误 / BUILD SUCCESS**，以最新一跑为准）、
  契约层、vitest、`_e2e` 接口层、浏览器层、注入自证 —— **同一窗口内都出数才算闭**，跨窗口相减不算。
- 250 那条腿必须再跑一次（§2.6），且 `healthproof` + `gates` 两步不能跳。
- `_e2e/README.md` 加一行 #61，数字现数；项目记忆 `project-z-lc-lowcode-state.md` 同步
  （#52 与**它那一窗的** #59/#60 已闭、本轮新撞的 #64 见 §2.6.4、#61 状态、HEAD 现测。
  ⚠ #59/#60 这两个号在两扇窗里被用过两次，写进记忆时要带"哪一窗的"，否则下一条窗读不回来）。
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
