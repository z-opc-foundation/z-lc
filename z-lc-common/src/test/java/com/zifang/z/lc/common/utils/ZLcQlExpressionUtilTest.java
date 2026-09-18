package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcQlExpressionUtil 单元测试
 *
 * @author zifang
 */
class ZLcQlExpressionUtilTest {

    @Test
    void executeRule_shouldReturnFalse_WhenRuleIsNull() {
        boolean result = ZLcQlExpressionUtil.executeRule(new HashMap<>(), null);
        assertThat(result).isFalse();
    }

    @Test
    void executeRule_shouldReturnFalse_WhenRuleIsEmpty() {
        boolean result = ZLcQlExpressionUtil.executeRule(new HashMap<>(), "");
        assertThat(result).isFalse();
    }

    @Test
    void executeRule_shouldReturnFalse_WhenRuleIsBlank() {
        boolean result = ZLcQlExpressionUtil.executeRule(new HashMap<>(), "  ");
        assertThat(result).isFalse();
    }

    @Test
    void executeRule_shouldReturnTrue_WhenSimpleTrueExpression() {
        boolean result = ZLcQlExpressionUtil.executeRule(new HashMap<>(), "true");
        assertThat(result).isTrue();
    }

    @Test
    void executeRule_shouldReturnFalse_WhenSimpleFalseExpression() {
        boolean result = ZLcQlExpressionUtil.executeRule(new HashMap<>(), "false");
        assertThat(result).isFalse();
    }

    @Test
    void executeRule_shouldEvaluateVariableComparison() {
        Map<String, Object> params = new HashMap<>();
        params.put("age", 25);
        boolean result = ZLcQlExpressionUtil.executeRule(params, "#age > 18");
        assertThat(result).isTrue();
    }

    @Test
    void executeRule_shouldEvaluateStringComparison() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "active");
        boolean result = ZLcQlExpressionUtil.executeRule(params, "#status == 'active'");
        assertThat(result).isTrue();
    }

    @Test
    void executeRule_shouldEvaluateAndExpression() {
        Map<String, Object> params = new HashMap<>();
        params.put("a", 10);
        params.put("b", 20);
        boolean result = ZLcQlExpressionUtil.executeRule(params, "#a > 5 and #b > 15");
        assertThat(result).isTrue();
    }

    @Test
    void executeRule_shouldEvaluateOrExpression() {
        Map<String, Object> params = new HashMap<>();
        params.put("a", 2);
        params.put("b", 30);
        boolean result = ZLcQlExpressionUtil.executeRule(params, "#a > 5 or #b > 15");
        assertThat(result).isTrue();
    }

    @Test
    void executeRule_shouldReturnFalse_WhenNullParams() {
        boolean result = ZLcQlExpressionUtil.executeRule(null, "true");
        assertThat(result).isTrue();
    }
}
