package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.event.EventConflictException;
import com.zifang.z.lc.core.pipeline.Pipeline.PipelineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Pattern;

/**
 * z-lc 控制器全局异常处理:
 * <ul>
 *   <li>EventConflictException → 409 (设计: 不允许冲突)</li>
 *   <li>PipelineException / IllegalArgumentException → 400 (字段校验失败)</li>
 *   <li>DuplicateKeyException → 400 (唯一键冲突, 不带索引名/列名)</li>
 *   <li>BadSqlGrammarException 且是"这一栏物理表里没有" → 400 并点名那一栏 + 指向 provision</li>
 *   <li>其它 RuntimeException → 500 (兜底)</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "com.zifang.z.lc.web")
public class LcExceptionHandler {

    private static final Logger log = LogManager.getLogger(LcExceptionHandler.class);

    /** H2 说 {@code Column "t.foo" not found}，MySQL 说 {@code Unknown column 't.foo' in 'field list'}。 */
    private static final Pattern MISSING_COLUMN = Pattern.compile(
            "(?i)Column\\s+\"([^\"]+)\"\\s+not found|Unknown column\\s+'([^']+)'");

    @ExceptionHandler(EventConflictException.class)
    public ResponseEntity<Result<Object>> handleConflict(EventConflictException ex) {
        log.warn("EventConflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.fail(ex.getMessage()).code(409));
    }

    @ExceptionHandler(PipelineException.class)
    public ResponseEntity<Result<Object>> handlePipeline(PipelineException ex) {
        log.warn("Pipeline[{}]: {}", ex.getProcessorName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(ex.getMessage()).code(400));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleBadArg(IllegalArgumentException ex) {
        log.warn("BadArg: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(ex.getMessage()).code(400));
    }

    /**
     * 唯一键冲突。
     * <p>
     * 交给兜底分支会泄密：H2 的原文是 {@code Unique index or primary key violation:
     * "public.uk_relation_tenant_code_INDEX_B ON public.z_lc_relation(tenant_code ...)}，
     * 里面既没有 SELECT 也没有 FROM，{@link #clientSafe} 拦不住，等于把索引名和列名发给浏览器
     * （实测 POST /api/lc/relation/create 重复编码就是这样）。这里收敛成一条看得懂的业务错误，
     * 真实键名留给上面那行 log。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Object>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("DuplicateKey: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail("记录已存在：同一租户下的编码必须唯一，请换一个编码").code(400));
    }

    /**
     * 定义里的列在物理表里不存在 (缺陷 #47)。走兜底分支会得到一个裸 500 +
     * {@code Column "t.xxx" not found}：用户既不知道该做什么，也没有任何"下一步"的指向，
     * 而这件事是**可以自助**的 —— 设计器里 provision 一次就好。
     * <p>
     * ⚠ 类型是按实测选的：这条路径上 Spring 给的是 {@link BadSqlGrammarException}
     * （H2/MySQL 的 42S22/1054 都翻成它），{@code UncategorizedSQLException} 是它的**兄弟**不是父亲，
     * 挂在那上面会一个都不接。所以这里只加窄分支，其余 SQL 语法错误照旧落兜底 500 ——
     * 认不出列名就把真·库故障包装成"去点一下 provision"，比 500 更坏。
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<Result<Object>> handleBadSqlGrammar(BadSqlGrammarException ex) {
        log.error("BadSqlGrammar", ex);
        String column = missingColumnName(ex);
        if (column == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail(clientSafe(ex)).code(500));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(
                "这一栏在物理表里还不存在: " + column + "。定义已经改到表前面了，去设计器点一次「provision」补列"
                        + "（没补之前这张表的读都会失败）").code(400));
    }

    /** 从 JDBC 原文里只捞出那个列名（把 `t.` 这层别名前缀去掉）；形状不对就返回 null，交给兜底。 */
    private static String missingColumnName(Throwable ex) {
        for (Throwable t = ex; t != null && t != t.getCause(); t = t.getCause()) {
            String raw = t.getMessage();
            if (raw == null) {
                continue;
            }
            java.util.regex.Matcher m = MISSING_COLUMN.matcher(raw);
            if (m.find()) {
                String hit = m.group(1) != null ? m.group(1) : m.group(2);
                int dot = hit.indexOf('.');
                return dot >= 0 ? hit.substring(dot + 1) : hit;
            }
        }
        return null;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleAny(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(clientSafe(ex)).code(500));
    }

    /**
     * 回给前端的错误摘要: 只留最深一层的原因, 并且绝不带 SQL 文本.
     * <p>
     * JDBC 异常的 message 形如 {@code PreparedStatementCallback; SQL [SELECT ... FROM `ui_task1234` ...]};
     * 原样透出等于把物理表名、列名和语句结构发给浏览器 —— 前端只需要知道"这次查询失败了",
     * 定位靠上面那行 log.error 打出的完整堆栈。
     * <p>
     * ⚠ 这里"不带 SQL 文本"是对**表名和语句**说的；缺列那一类由
     * {@link #handleBadSqlGrammar} 单独点名处理，因为它回的是**用户自己定义里的栏名**，
     * 不透出去反而没人能自助修。
     */
    private static String clientSafe(RuntimeException ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String raw = root.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "internal error";
        }

        String hint = raw.split("(?i);\\s*SQL|\\[SQL|SQL statement|nested exception|\\R", 2)[0].trim();
        if (hint.isEmpty()) {
            hint = raw.trim().split("\\R", 2)[0].trim();
        }
        if (hint.toUpperCase().contains("SELECT") || hint.toUpperCase().contains("FROM ")) {
            hint = "database access failed";
        }
        return hint.length() > 160 ? hint.substring(0, 160) : hint;
    }
}
