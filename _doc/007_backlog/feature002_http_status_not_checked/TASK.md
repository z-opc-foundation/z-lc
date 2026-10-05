# feature002 · 缺陷 #62：`isSuccess()` 不看状态码，404/500 在 10 处被读成"远端受理了"

登记时间：2026-09-26 23:1x。性质：**已定方向、只欠工程量**（不需要拍板，但要一批配 10 支带猎物的测试）。
本轮已经在 `CamudaAdapter` 这一条链上修好并钉住，**其余 10 处原样保留**。

## 1. 库侧事实（不是我推的，是库自己写明的）

```bash
sed -n '43,50p' z-util/z-util-http/src/main/java/com/zifang/util/http/client/HttpExecutionResult.java
```

`ok(int status, …)` 里那句话逐字是：**"因此 success 始终为 true，5xx 不会让 isSuccess()=false"**。
⇒ `isSuccess()` 只表示"这一趟没有传输层错误"；HTTP 状态在 `getStatus()` 里，没人读就没人判。

## 2. 本轮实测到的两个直接后果

| 后果 | 怎么量出来的 |
|---|---|
| `CtcAdapter.ping()` 这类健康检查，对端回 404 也报 UP | `CtcAdapter.java` 的 `return res.isSuccess();` —— 与已闭的缺陷 #52（`/api/lc/health` 硬编码 UP）同一形状，只是这次是"问了但没看答" |
| 打 404 的绑定链会被报成"应答里没有布尔型的 success 这一格" —— 把引擎撇干净、把原因指向对端 JSON | 本轮真实发生过：`CamudaAdapterTest.httpFailureCarriesStatusAndPath` 在只判 `isSuccess()` 的实现下，收到的失败消息是"…没有布尔型的 success…"，而不是"http=404"。**假原因比没原因更难查** |

## 3. 还欠的 10 处（行号会漂，一律现取）

```bash
grep -n "res.isSuccess()" z-lc/z-lc-core/src/main/java/com/zifang/z/lc/core/adapter/*.java
```

09-26 23:1x 实测 10 处（不含 `httpAccepted` 自己那行）：
`CtcAdapter` 2（fetchContext / ping）、`MetaAdapter` 2、`ScriptAdapter` 1、`MistAdapter` 2、`OssAdapter` 3。

修法已经写好：`CtcAdapter.httpAccepted(res)`（`isSuccess() && 200 <= status < 300`），逐个替换即可。

## 4. 必带的猎物（每处都要，否则这一支等于没修）

对每个 adapter 至少两支，成对写：
- **正向**：桩回 200 + 合法信封 ⇒ 走成功路径（证明闸没把好路堵死）；
- **反向**：桩回 500 **且 body 是一份看起来成功的信封** ⇒ 必须判失败/必须报 unreachable，
  且失败原因里要点名 http 状态。
  这一支是关键：只测 404 + 空 body 的话，把状态码闸摘掉也照样绿（因为 body 解析那一步会先红），
  量不到这一支的牙齿。已在 `CamudaAdapterTest.non2xxIsRefusedEvenWhenTheBodyLooksLikeASuccessEnvelope` 打过样。

⚠ 复用 `CamudaStubServer`（`z-lc-core/src/test/.../adapter/CamudaStubServer.java`），它支持 `.status(500).body(...)`；
不要引进 JDK8 那个 `com.sun.net.httpserver.HttpServer`（本仓有 `stop()` 卡 preClose0 挂死整个 fork 的先例）。

## 5. 顺带要一起看的（同一次改动会撞见）

`ScriptLifecycleService` 里那句 `result.isSuccess()` 是 **z-script 的 `ApiExecutionResult`**，
不是 HTTP 结果 —— 别顺手改，改了是另一件事（要单独证伪它的语义）。
