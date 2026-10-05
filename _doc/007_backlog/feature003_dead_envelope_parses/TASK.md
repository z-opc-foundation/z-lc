# feature003 · 缺陷 #63：5 处"信封读法"结构上必抛，另有 3 个 adapter 零调用者

登记时间：2026-09-26 23:1x。性质：**前 5 处已定方向、只欠工程量**；**后 3 处等拍板**。

这一支和 #61 是同一族：`CamudaAdapter` 修的时候顺手把整个 adapter 层的读法都对了一遍，
发现**同样一句话在别处还有 5 个写法，每一个都必然抛**。

> **2026-10-05：§2 的 5 处已全部改为 `JsonUtil.parseObject` 逐格读，§2 关闭；§3 仍等拍板。**
> 本轮复测（两个 JDK 都跑，结论一致）：§1 那张表的四条**全部复现**，无一例外。
> 这次顺带测到**两条原表里没有、但会让"照抄 CamudaAdapter 改法"翻车**的事实，见 §5。


---

## 1. 库侧事实（本轮实测，不是推测）

`z-util` 的 JSON 引擎（`z-util/z-util-parser/z-util-parser-json`，与解析出的 jar 同 `<revision>1.0.12`）：

| 写法 | 实测结局 | 为什么 |
|---|---|---|
| `JsonUtil.fromJson(body, Result.class)` | **恒抛** `RuntimeException: deserializePojo failed: com.zifang.util.core.meta.Result`，root `IllegalAccessException: … cannot access a member of class …Result with modifiers "private"` | 引擎建对象走 `clazz.getDeclaredConstructor().newInstance()` 且**不 setAccessible**，而 `Result` 的无参构造是 private |
| `JsonUtil.fromJson(body, new TypeReference<Result<…>>(){})` | **恒抛** `ClassCastException: ParameterizedTypeImpl cannot be cast to class java.lang.Class` | 泛型那条路径没接上 |
| `JsonUtil.fromJson(body, Map.class)` | **恒抛** `NoSuchMethodException: java.util.Map.<init>()` | 接口不能 new |
| `JsonUtil.parseObject(body)` | **可用** | 逐格读：`getBoolean(k)`（缺/类型不对 ⇒ null）、`getString(k)`（缺 ⇒ null）、`containsKey(k)`、`getJsonObject(k)`（缺 ⇒ `IllegalArgumentException("Invalid key")`，值不是对象 ⇒ `JsonTypeException`）、`size()`；垃圾输入抛 `JsonParseException`；**空串不抛而返回空对象** |

复现命令（本轮就是这么量的，一条一次性探针测试，量完即删）：

```bash
# 临时写一支 @Test 分别调用上面四种写法，看抛什么；不要在仓里留探针
```

## 2. 还活着的 5 处必抛写法（行号会漂，一律现取）

```bash
grep -rn "JsonUtil.fromJson" z-lc/z-lc-core/src/main/java/com/zifang/z/lc/core/adapter/*.java
```

09-26 23:1x 实测：

| 位置 | 写法 | 抛了以后变成什么（已读到 catch/return 那几行） |
|---|---|---|
| `CtcAdapter.fetchContext` | `TypeReference<Result<Map<String,Object>>>` | catch ⇒ `log.warn` ⇒ **返回 null** ⇒ `checkAuth` 判 false（但见 §4：`checkAuth` 本身零调用者） |
| `MetaAdapter`（字典读） | `TypeReference<Result<List<DictItemDTO>>>` | ⇒ null ⇒ 字典值域解析静默什么都不做（`DictResolveProcessor` 是唯一消费方） |
| `ScriptAdapter.eval` | `Map.class` | ⇒ **抛 RuntimeException**("Script eval response parse failed") |
| `MistAdapter`（解密读） | `Result.class` | catch ⇒ `log.warn` ⇒ null |
| `OssAdapter.generateDownloadUrl` | `Result.class` | catch ⇒ `log.warn` ⇒ null（这一处**还有第二个错**：`r.getData().toString()`，与 #61 修掉的 `CamudaAdapter` 那个"把整个 Map 的 toString 当 id"是同一笔） |

修法照 `CamudaAdapter` 本轮那一版抄：`JsonUtil.parseObject` + 逐格读，**并把"缺哪一格"写进失败原因**。

⚠ 关键教训（这条要比修本身留得久）：这几处的 `catch → log.warn → return null`
把"我压根没解析成功"伪装成了"远端说没有值"。修完之后**静默 null 变成了静默 null 的另一半**，
所以每一处都要配一支"成功信封必须解析出那个值"的正向断言，不能只配反向。

## 3. 等拍板：三个 adapter 在整个生产代码里零调用者

实测（09-26 23:1x，`grep -rn "\bMistAdapter\b" --include='*.java' */src/main` 去掉类自己和注释）：

| 类 | 非自身引用数 | 说明 |
|---|---|---|
| `MistAdapter` | **0** | 加密/解密口，没有任何 processor / controller 调它 |
| `OssAdapter` | **0** | 附件上传下载口，同上 |
| `ScriptAdapter` | **0** | 脚本 eval 口，同上（`ScriptLifecycleService` 走的是 z-script 的 `DynamicApiExecutor`，**不是**这个 adapter） |
| `MetaAdapter` | 2 处真实消费 | `DictResolveProcessor`、`HealthController` |
| `CtcAdapter` | 作为共享 HTTP 工具被大量引用 | 但 `checkAuth` 本身 **0 调用者**（见 §4） |
| `CamudaAdapter` | 1 处 | 就是 #61 本轮接上的 `WorkflowTriggerDispatcher` |

⇒ **要你拍的**：字段级加密、附件存储、脚本这三个能力，是"留着当扩展点"（那 §2 里它们的必抛写法就
不是紧急缺陷，但仍要修，因为界面一旦放出对应配置就会撞上），还是"这一版不宣称支持"（那界面/文档要同步删）。
现在 `WorkflowsPage` 这一族的形状说明：**界面放出去而引擎不兑现，是我们反复踩的那一个坑**。

## 4. 顺带发现：权限判定这条链在 java 侧其实没接线

实测：`checkAuth(` 在 `src/main` 里 **0 调用者**；`AuthContextDTO` 的读取唯一入口是 `checkAuth` 自己；
鉴权实际落在 z-ctc 的网关/过滤器上（不在本仓）。
⇒ 后果：`fetchContext` 那条死解析目前**没有可观察的损害**，别把它当成"用户登录坏了"。
   但它同时也是"低代码权限矩阵在服务端到底靠谁判"这一问的答案缺失项，归 #48/#49 那一族继续追。

## 5. 2026-10-05 补测：照抄 `CamudaAdapter` 改法之前必须先知道的两件事

§1 那张表只说了"`parseObject` 可用"，没说完**逐格读回来的值是什么形状**。这一层不量清楚，
改完会安静地引入新缺陷——两条都是本轮实际撞上的：

### 5.1 `JsonObject` 不是 `Map`，`JsonArray` 不是 `List`（两个都不实现）

z-util-parser-json（z-lc 实际解析到的是 **1.0.14**）的 `JsonObject` 只有 `implements Iterable` 一条尾巴
都没有，`JsonArray` 同理。它们**都不实现** `java.util.Map` / `java.util.List`。三个立刻踩得到的后果：

| 写法 | 静默错在哪 |
|---|---|
| `if (o instanceof List) { … }` | `JsonArray` 不满足 ⇒ 一份形状完全正确的 `"roles":["admin"]` **安静变成空列表**，日志上一行异常都没有（`CtcAdapter.asStringList` 原本就是这个形状，已补 `JsonArray` 分支） |
| `return envelope.get("data");` 直接递出去 | 调用方若 `((Map) result).get("x")` ⇒ **当场 ClassCastException**。旧写法（若它曾经能工作）交出去的是 fastjson 的 `JSONObject`，那**是**实打实的 `Map`——所以直接把 `JsonObject` 递出去是**契约回退**。已加 `CtcAdapter.toPlainJava` 把边界收成普通 `Map`/`List`/标量 |
| 想把 `JsonObject` 拷进 `LinkedHashMap` | **没有 `entrySet()`、没有 `keySet()`、没有 `values()`**。唯一的迭代入口是 `getAllKeyValue()`，返回 `List<Map.Entry<String,Object>>` |

⇒ 一句话：**逐格读解决的是"读得出来"，`toPlainJava` 解决的是"递出去的东西还是不是调用方要的类型"。**
只做前者，等于把缺陷从"静默 null"换成"静默 ClassCastException"。

### 5.2 `getString` 只对 String 生效，数字会被读成 null

`JsonObject.getString(k)` 的实现是 `v instanceof String ? (String) v : null`——**不做任何 toString**。
所以 `sortOrder: 1` 这种数字用 `getString` 读出来就是 `null`。数值字段一律走
`getInt`/`getLong`（内部是 `instanceof Number`）。

顺带把口径对齐了一处：`DictItemDTO.itemValue` 缺失时回落到 `itemCode`，
依据是 `DictResolveProcessor.matches` 本身的口径（先看 `itemValue` 再看 `itemCode`，已读源码确认），
两边一致，调用方不会因为这个回落而多出一条匹配。

