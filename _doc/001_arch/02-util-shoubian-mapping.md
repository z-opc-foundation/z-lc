# z-lc 工具类收编映射表（验收清单，v2：以 dedup 测试为权威）

> 修订背景：第一版基于 z-util 目标 API 的方法签名罗列, 未读 `ZLcUtilDedupEquivalenceTest`。该测试是 2026-09 写好的回归锁, 已经把多数波次一类的真实差异钉死。本版以它为权威, 重写波次一结论。

## 口径

- **收编** = 逻辑只剩 z-util 一份口径; z-lc 侧"留形状、换内核"（类保留为薄委托门面）或"直改调用方后删类"。
- **兜底** = 每个委托类配一个对拍测试（旧实现 vs z-util 目标, 逐输入断言一致），模板见 z-ctc `JwtUtil` + `JwtCompatCrossCheckTest`、本仓 `ZLcUtilDedupEquivalenceTest`。
- **权威** = `ZLcUtilDedupEquivalenceTest` 已经过逐输入对拍, 实测差异落地, 该文件说了算。

## 输入集合断言

清单由以下命令从工作树推导（2026-10-01, 以树为准）:

```bash
find z-lc -path "*/src/main/*" -name "*.java" | grep -i -E "util|utils|helper|converter"
# => 42 个
```

早期口径"61 个"把测试文件与误匹配计入了, **以本清单 42 为准**。42 个类全部标注"蒸馏自 ace-platform-core"。

## 主映射表（v2 修订）

### 第 0 组: 已完成收编（3 项, 不在本波次动作范围）

| 项 | 旧类 | 现态 | 证据 |
|---|---|---|---|
| G0-1 | ZLcMDCThreadPoolExecutor | **已删** | 工作树无此文件; `ZLcUtilDedupEquivalenceTest#mdcThreadPoolExecutorEquivalentAndSafer` 注明 "顶掉本地MDCThreadPoolExecutor" |
| G0-2 | ZLcStringUtil.isBlank / isNotBlank | **已删方法** | ZLcStringUtil.java:16-20 javadoc 自记 "已收口到 z-util... 已删除, 调用点直指 z-util"; dedup 测试已固化 |
| G0-3 | ZLcTimeUtil 7 个时间转换/格式化方法 | **已删方法** | 同类 `todayStart/todayEnd` 保留; dedup 测试 `timeConversionsEquivalentToStrippedLocalImpl` 锁住差异 |

### A. 已由 dedup 测试锁定为"保留"（12 项）

差异真实存在, 调用点不能换 z-util 同名 API 直接替换。本类应保留, 不进入本表波次动作。

| # | z-lc 类 | 差异本质 | dedup 测试方法 |
|---|---|---|---|
| 1 | ZLcArrayUtil | retainAll 原地改表+保重 vs intersection 不改+去重; merge null 处理相反 | `arrayUtilSemanticsNotEquivalent` |
| 2 | ZLcErrorUtil | check 抛 `ZLcPermissionLimitException` 而非 `IllegalArgumentException`, 业务 catch 会漏 | `errorExtendAndThreadPoolHaveNoEquivalent` |
| 3 | ZLcExtendUtil | "空/"null"/非法 JSON → 建默认实例" z-util 无对应 | `errorExtendAndThreadPoolHaveNoEquivalent` |
| 4 | ZLcExportTxtUtil | 错误契约: 静默失败 + user.home 约定 + .txt 后缀约定 | `ioAndExportErrorContractsDiffer` |
| 5 | ZLcIOUtil | 错误契约: 读流异常返回 null、写时父目录不存在抛 RuntimeException、删不存在目录返回 false | `ioAndExportErrorContractsDiffer` |
| 6 | ZLcJsonSortUtil | CASE_INSENSITIVE_ORDER vs 自然序; 数组内对象递归与否; 空串/非法 JSON 的异常类型与返回 | `jsonKeySortNotEquivalentButByteIdenticalOnPlainInputs` |
| 7 | ZLcKeyOrderedExecutor | null 任务: NPE vs 静默丢; `allowCoreThreadTimeOut` 开关 | `keyAffinityRoutingMatchesButNullTaskSemanticsDiffer` |
| 8 | ZLcPlaceholderUtil | 空 key `${}` z-util 不匹配; 多行 key `${a\nb}` z-util 能跨行本地不行 | `placeholderDiffersFromZUtilOnEmptyAndMultilineKeys` |
| 9 | ZLcRSAUtil | 密钥可互换, 但 z-util 1024 位定死/分段; 加密 charset 一致性 | `rsaLocalAndZUtilKeyStringsInterchangeableButLimitsDiffer` |
| 10 | ZLcStringUtil | 驼峰/下划线转换边界 (无下划线、连续大写、null、前导/连续下划线) 与 z-util 不互换 | `camelCaseConvertersAreNotInterchangeable` |
| 11 | ZLcTaskExecutionUtil (utils) | ignoreExceptions 极性 + cause 解包重试次数 | `taskExecutionIgnorePolarityAndCauseUnwrapDifferFromRetry` |
| 12 | ZLcTaskExecutionUtil (aspect) | ignoreExceptions 极性相反: "命中即抛" | `taskExecutionIgnorePolarityAndCauseUnwrapDifferFromRetry` |
| 13 | ZLcThreadPoolUtil | 三个预置池的容量/拒绝策略是业务口径, z-util 无同名常量 | `errorExtendAndThreadPoolHaveNoEquivalent` |

> **修订说明**：第一版把这 13 项里的大半（JsonSort/KeyOrdered/Placeholder/Tree/IO/Export）写成"1:1 命中, 委托后删除"。dedup 测试证明它们与 z-util 不互换, 应保留。第一版的波次一 = 6 类全部不能保真保留。

### B. 本轮补 dedup 测试, 锁定"保留"差异（3 项, **本轮新动作**）

这三项第一版列入波次一"零风险", 经直接读源码 + 既有 z-util API 对照, 发现真实差异, 但未在 dedup 测试中。本轮把测试补上, 确认保留。

| # | z-lc 类 | 差异本质 | 本轮验收（追加到 dedup 测试） |
|---|---|---|---|
| 14 | ZLcBase64Utils | decode(null) → null vs z-util decode(null/empty) → byte[0]; decodeToString(null) → null vs "" | `base64DecodeNullHandlingDiffersFromZUtil` |
| 15 | ZLcTimestampConverter | format(null) → "" vs z-util format(null) → null; formatOrNull(null) 一致 | `timestampConverterFormatNullDiffersFromZUtil` |
| 16 | ZLcTreeUtil | 行为对得上, 但 API 形态: TreeNode<T> 接口 vs z-util getter lambda (无接口, 业务类需自暴露 getter) | `treeUtilApiShapeIsInterfaceVsLambda` |

### C. 部分委托——本轮可动手（2 项）

差异仅在窄边, 大部分调用可委托 z-util, 但要保留少量本地行为。

| # | z-lc 类 | 委托部分 | 保留部分 | 验收 |
|---|---|---|---|---|
| 17 | ZLcBase64Utils | `encode(byte[])` `encode(String)` `encodeUrlSafe(byte[])` 全对得上 z-util, 内核换 `core.encrypt.Base64Utils` 即可（z-util 已对 null→null, 字节相同） | `decode(String)` / `decodeToString(String)` / `decodeUrlSafe(String)` —— null 时本地返 null, z-util 返 `byte[0]`/`""`, 必须本地保形状 | 同输入 null 与非空两侧字节对拍 |
| 18 | ZLcTimestampConverter | `formatOrNull` 与 z-util `TimestampUtil.format(null)→null` 行为一致, **可删类并改调用点直指 z-util** | `format(null)→""` 是本地特有语义, 保留 shape | formatOrNull 全量调用点迁移测试 |

### D. 保留——业务语义 / Spring 绑定 / 框架胶水（15 项）

同第一版 D 组, 此处不重复罗列, 仅列业务域备注:

| 域 | 类 |
|---|---|
| 流程域语义 | ZLcApprovalOpinionUtil, ZLcExpressionParserUtil (SpEL 部分) |
| Spring 绑定 (z-util 刻意无 Spring) | ZLcSpelUtil, ZLcExtensionMapAccessor |
| 低代码平台契约 | ZLcSchemaUtil, ZLcModelDataUtil (pickPrimaryKey) |
| 业务框架胶水 | ZLcExportRetryPolicy, ZLcStarRocksStreamLoad, ZLcDataExportHandlerFactory, ZLcIDataExportHandler, ZLcIDataImportOrExportProcess, ZLcDataImportExportExecutor, ZLcDataImportExportUtil, ZLcCommonTransformer, ZLcSqlUtil |
| 业务 catch 异常 | （已在 A 组 #2 ZLcErrorUtil） |
| SDK 域 | LcReflectHelper (DataModel 注解部分), QlExpressionUtil (sdk 极简自研), ZLcQlExpressionUtil (common SpEL 版, 仓内合一) |

### 仓内重复专项

- ZLcTaskExecutionUtil ×2（aspect/utils）：已在 A 组保留, 极性相反, 不合一。
- QlExpressionUtil ×2（common SpEL 版 / sdk 极简自研）：保留 D 组, 语义差异真实, **仓内合一不是本轮目标**, 待 SpEL 迁出或 expr-el 路线决策后再议。

## 缺口清单（z-util 侧, 不阻塞本轮）

| # | 缺口 | 证据 | 建议 |
|---|---|---|---|
| G1 | Base64 decode 对 null/空串应统一语义 | z-util decode(null)→byte[0], 本地 null→null | 待 PR：在 z-util 加 null → null 的可选方法或在 README 注明差异 |
| G2 | office 无 Excel 表头构建 | ZLcDataImportExportUtil.buildExcelHead 仍在 z-lc | office 模块增强项, 本轮不做 |
| G3 | `resolveGenericType` 泛型解析与 `ReflectUtil` 关系未核验 | core/lang/reflect/ReflectUtil.java 存在 | 执行 #41 时顺手比对 |

## 执行顺序（修订）

第一版波次一作废。真正的零风险顺序：

1. **波次 C（局部委托）**：先做 #18 ZLcTimestampConverter.formatOrNull 删类（差异最小, 调用方全迁即可）。再做 #17 ZLcBase64Utils.encode 三方法换内核。
2. **波次 A（回归锁固化）**：B 组 3 项补入 `ZLcUtilDedupEquivalenceTest`, 锁定差异, 确保未来不被悄悄替换。
3. **波次 B（业务改造, 非本轮）**：在确认 z-util 行为变更后批量收编 D 组里真正可迁移的部分。
4. **波次后续（保留）**：A 组 13 项保持保留, 仅在业务 catch 改造或语义调整后重评。

## 进度登记（v2 修订）

| 状态 | 数量 | 明细 |
|---|---|---|
| 已完成收编 | 3 | G0-1/2/3 |
| 已锁定为保留 | 13 | A 组（含 ZLcTaskExecutionUtil×2） |
| 本轮新锁定保留 | 3 | B 组（本轮补 dedup 测试） |
| 本轮部分委托 | 2 | C 组 |
| 保留（业务/Spring） | 15 | D 组 |
| 仓内重复专项 | 2 | QL×2（不合一） |
| **合计** | **42** | 与输入集合断言一致 |

**真实零风险收编候选**: 由第一版的 17 个, 降为本轮 **2 项局部委托**（C 组）。其余多为差异真实存在, 保留比替换安全。