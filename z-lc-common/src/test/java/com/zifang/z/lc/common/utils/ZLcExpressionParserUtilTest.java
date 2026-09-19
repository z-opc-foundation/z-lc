package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExpressionParserUtil 单元测试
 *
 * @author zifang
 */
class ZLcExpressionParserUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcExpressionParserUtil> constructor = ZLcExpressionParserUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullExtractExpression() {
        assertThat(ZLcExpressionParserUtil.extractExpression(null)).isNull();
    }

    @Test
    void shouldReturnNullForEmptyExtractExpression() {
        assertThat(ZLcExpressionParserUtil.extractExpression("")).isNull();
    }

    @Test
    void shouldExtractExpressionFromBpmn() {
        assertThat(ZLcExpressionParserUtil.extractExpression("${days > 3}")).isEqualTo("days > 3");
    }

    @Test
    void shouldReturnOriginalWhenNoPlaceholder() {
        assertThat(ZLcExpressionParserUtil.extractExpression("days > 3")).isEqualTo("days > 3");
    }

    @Test
    void shouldEvaluateExpressionTrue() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 15000);

        boolean result = ZLcExpressionParserUtil.evaluateExpression("#amount > 10000", variables);
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateExpressionFalse() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 5000);

        boolean result = ZLcExpressionParserUtil.evaluateExpression("#amount > 10000", variables);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForInvalidExpression() {
        boolean result = ZLcExpressionParserUtil.evaluateExpression("invalid expression", null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldEvaluateToObject() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "test");

        Object result = ZLcExpressionParserUtil.evaluateToObject("#name", variables);
        assertThat(result).isEqualTo("test");
    }

    @Test
    void shouldReturnNullForInvalidObjectExpression() {
        Object result = ZLcExpressionParserUtil.evaluateToObject("invalid", null);
        assertThat(result).isNull();
    }

    @Test
    void shouldEvaluateWithNullVariables() {
        boolean result = ZLcExpressionParserUtil.evaluateExpression("1 > 0", null);
        assertThat(result).isTrue();
    }
}