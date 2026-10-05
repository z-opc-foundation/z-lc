# feature004 · 缺陷 #64：`camelCase` 在合法字段码上抛异常，一个坏字段让整批物化零产出

登记时间：2026-10-05 22:1x。性质：**已定方向、只欠工程量**（崩溃已修并钉住；§3 的可编译性问题等拍板）。

## 1. 实测到的崩溃

`CodeTemplateEngine.camelCase` 旧写法：

```java
String[] parts = s.toLowerCase().split("_");
StringBuilder sb = new StringBuilder(parts[0]);
for (int i = 1; i < parts.length; i++) {
    sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
}
```

JDK 8 实测（JDK 17 结果一致）：

| 输入 | 是否合法字段码 | 旧写法实测结果 |
|---|---|---|
| `a__b` | **合法** | `StringIndexOutOfBoundsException: String index out of range: 0` |
| `user__id` | **合法** | 同上 |
| `a___b` | **合法** | 同上 |
| `a_b_c` | 合法 | 正常（`aBC`） |
| `_a` / `__x` | 非法（首字符非字母） | 同上 |

## 2. 为什么"合法输入"是重点

`SchemaAdminBizService.FIELD_CODE_RE = ^[A-Za-z][A-Za-z0-9_]*$` —— `A-Za-z0-9_` 里含 `_`，
所以**连续下划线完全合法**。这不是"非法输入的容错"，是正常输入的崩溃。

实体码更松：`SchemaAdminBizService` 里**搜不到任何针对 `entityCode` 的正则校验**，
而 `camelCase(entity.getEntityCode())` 被 `renderController`（URL 段）和 `renderReactPage` 用着。

## 3. 影响面比"一个字段生成不出来"大得多

`MaterializationService.run` 是**逐 entity 循环生成、循环结束后才统一写文件**：

```java
for (EntityEntity e : entities) {
    List<FieldEntity> fields = loadFields(...);
    allFiles.addAll(generateForEntity(e, fields, basePath));   // ← 这里抛
}
// ……
for (MaterializationResp.GeneratedFile gf : allFiles) {         // ← 到这里才写盘
```

⇒ **任何一个 entity 里的一个坏字段码，会让整批物化零产出**（不是只少那一个文件）。

异常本身**没有被静默吞掉**：`runAsync` 有 catch，会把状态置 `STATUS_FAILED` 并写 `errorMessage`。
但写进去的是 `String index out of range: 0` —— **看不出是哪个字段、哪个实体**，排障只能自己猜。

## 4. 本轮已修

`camelCase` 改为跳过空段；`toLowerCase()` 加 `Locale.ROOT`。

- 变异验证：退回旧写法后**首跑 6 条红**（4 Errors + 2 Failures），7 道新测试命中 6 道；
  第 7 道（`toPascalCaseToleratesConsecutiveUnderscores`）保持绿是正确的——它钉的是本来就没坏的行为。
- Locale 那条有直接证据：变异体上 `camelCaseIsLocaleIndependent` 报
  `expected:<[i]ndexCode> but was:<[ı]ndexCode>`，**土耳其无点 i 字面出现**。
- 顺带钉住一处**有意的行为变更**：`_a` 过去返回 `"A"`（`parts[0]` 是空串、抬首字母从 index 1 开始），
  现在返回 `"a"`。取 `a` 是对的——本方法三处用法（Java 字段名 / Controller URL 段 / React prop 名）
  都要求首字母小写。

## 5. 等拍板：生成出来的 Java 仍然可能编不过（本次**未修**）

同属"合法字段码"，但旧写法只是崩、不会生成坏代码——所以下面这些**修好 #64 之后依然存在**，
而它们是另一个性质的问题（产物不可编译，不是崩溃）：

| 输入 | 是否合法字段码 | 生成结果 |
|---|---|---|
| `class` / `new` / `int` / `this` | **合法** | `private String class;` ⇒ javac 报错 |
| `a_b` 与 `a__b` 同时存在 | **都合法** | 两者都 camelCase 成 `aB` ⇒ POJO 里两个同名字段 |
| 实体码 `1order` | 实体码无任何校验 | `public class 1Order` ⇒ javac 报错 |

⇒ **要你拍的**：物化引擎对标识符的合法集合要不要比 `FIELD_CODE_RE` 更严？
（选项：物化时校验并拒绝 / 生成时转义 / 在 doc 里声明"字段码不能是 Java 关键字"。）
现在这一层是**没有任何声明**的，而 §4 修完之后它会从"崩"退化成"安静地生成编不过的代码"——
后者更难发现，所以这一条比 #64 本身更值得定。

## 6. 复现命令

```bash
# 一次性探针（JDK 8 需 javac 编译后运行：单文件源码模式是 JDK 11+ 才有的）
# 逐字照抄旧 camelCase，对 "a__b" / "user__id" / "a___b" 调用
```
