# z-lc · DB schema 契约与已知失败要点

> 本文承接三处 javadoc 里指向「失败要点与坑」的交接说明 —— 那三处注释一直指向
> `_doc/008_troubleshooting/失败要点与坑.md`，而**那份文档从未存在**（本仓 git 全历史无此路径）。
> 2026-10-19 按 lead 002 规范补齐：注释指向的文件必须真实存在，否则读者点进去是 404。
>
> 承接的三处：
> - `z-lc-report-web/.../store/SchemaStore.java` —— DB 实现约定
> - `z-lc-report-web/.../controller/AiController.java` —— 字段契约 / 元数据下推
> - `z-lc-report-dataset/.../engine/ExprParser.java` —— 白名单受限求值的边界

---

## 1. SchemaStore 的 DB 实现约定（M2 → M2.5 迁移）

| 项 | 现状 / 约定 |
|---|---|
| SPI | `SchemaStore`（M2 已接口化），内置实现 `InMemorySchemaStore`（重启即失） |
| 目标存储 | MyBatis-Plus + **JSON 列存 schema 全文**（对齐 z-lc `viewconfig` 模式） |
| 切换方式 | 提供同名 Bean 替换 `InMemorySchemaStore` 的 `@Component` 注册即可整体切换，业务代码不动 |
| 语义约定 | `require*`：不存在抛 `IllegalArgumentException`（主链路 fail-fast）；`get*`：不存在返回 `null`（幂等探测 / AI 装配） |

**表结构权威位置**：`z-lc-admin/src/main/resources/db/schema-h2.sql`（H2 2.x，MODE=MySQL，dev 与端到端验证用）。
生产 MySQL 以 z-opc 仓 `_doc/004_sql/` 下的 DDL 为准。

> ⚠️ 本仓**不建** `_doc/002_deploy/` 桶：部署 SQL 实际落在 `src/main/resources/db/`（H2 启动时自动跑），
> 按 lead 002 规范的 SQL 策略，这是允许的例外形态。

## 2. 字段契约 / 元数据下推（AI 报表视图生成）

视图生成需要**数据集列元数据**。`fields` 契约缺失时，会**实际执行一次数据集获取列头**：

- M1 全量内存语义下可接受；
- M2 改用**字段契约 / 下推元数据** —— 即由调用方给出列定义，避免为了拿列头而全量拉数据。

改动这条时注意：它同时影响 `AiController` 的生成链路与 `SchemaStore` 的 `listDatasets()` 枚举面。

## 3. 白名单受限求值的边界（ExprParser）

`ExprParser` 只生成结构化 `Expr` 对象树，求值走 `Expr.eval(row)`，**不做任何字符串代码执行**：

- 语法面即白名单：列引用 / 字面量 / 算术 / 比较 / 逻辑 / `IS [NOT] NULL` / `IN` / `LIKE`（退化为 `contains`）；
- **不支持函数调用与动态标识符** ⇒ 不存在注入面；
- 更复杂的表达式（函数 / 日期运算）由 `z-util-expr` 接入（M2+），届时**必须白名单限定可用函数** ——
  一旦放开成"任意函数名可调"，上面这条不变量立刻失效。

## 4. 其它已知要点

- `z-lc` 的 `01-module-structure.md` 是**历史**文档（写的是拆仓当时的状态），
  现状以仓根 README 与根 POM 为准 —— 那份文档自己也这么标。
