package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常映射的单测。
 * <p>
 * 唯一键冲突这条映射值得单独测：IT 侧每个 create 现在都先做了预检，正常情况下已经走不到
 * DuplicateKeyException（只剩并发撞车），所以在 HTTP 层它测不出来 —— 但它是别的表出问题时
 * 唯一还拦得住泄密的口子，不能因为"没测到"就删掉。
 */
class LcExceptionHandlerTest {

    private final LcExceptionHandler advice = new LcExceptionHandler();

    /** Spring 翻译 H2 唯一索引冲突时的真实原文形状。 */
    private static DuplicateKeyException h2UniqueViolation(String index, String table, String columns) {
        return new DuplicateKeyException("Statement;\n**java.sql.SQLException: Unique index or primary key violation: \""
                + index + " ON public." + table + "(" + columns + ") VALUES ( /* key:5 */ ...)\"");
    }

    @Test
    @DisplayName("DuplicateKeyException → 400，且索引名/表名/列名一个都不许出现在响应里")
    void duplicateKeyBecomesReadableBadRequest() {
        ResponseEntity<Result<Object>> response = advice.handleDuplicateKey(
                h2UniqueViolation("public.uk_relation_tenant_code_INDEX_B", "z_lc_relation", "tenant_code, app_code"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "撞唯一键不是服务器故障");
        Result<Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess(), "必须是失败信封");
        assertEquals(400, body.getCode());

        String message = String.valueOf(body.getMessage());
        assertTrue(message.contains("已存在"), "要让人看懂是重复了: " + message);
        String upper = message.toUpperCase();
        assertFalse(upper.contains("UK_"), "索引名透给了前端: " + message);
        assertFalse(upper.contains("Z_LC_"), "物理表名透给了前端: " + message);
        assertFalse(upper.contains("UNIQUE INDEX"), "驱动原文透给了前端: " + message);
    }

    @Test
    @DisplayName("兜底分支仍是 500，且不含 SQL 文本")
    void unknownRuntimeStillFailsAsServerError() {
        ResponseEntity<Result<Object>> response = advice.handleAny(new RuntimeException(
                "PreparedStatementCallback; [SQL SELECT f.field_code FROM z_lc_field f]; boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = String.valueOf(response.getBody().getMessage());
        assertFalse(message.toUpperCase().contains("SELECT"), "SQL 原文透给了前端: " + message);
    }
}
