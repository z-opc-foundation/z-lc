package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSpelUtil 单元测试
 *
 * @author zifang
 */
class ZLcSpelUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcSpelUtil> constructor = ZLcSpelUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldEvaluateBooleanExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 15000);

        boolean result = ZLcSpelUtil.evaluate("#amount > 10000", variables);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForFalseBooleanExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 5000);

        boolean result = ZLcSpelUtil.evaluate("#amount > 10000", variables);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForInvalidExpression() {
        boolean result = ZLcSpelUtil.evaluate("invalid expression", null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldEvaluateWithNullVariables() {
        boolean result = ZLcSpelUtil.evaluate("1 > 0", null);
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateStringExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "test");
        variables.put("type", "user");

        String result = ZLcSpelUtil.evaluateToString("#name + '_' + #type", variables);
        assertThat(result).isEqualTo("test_user");
    }

    @Test
    void shouldReturnNullForInvalidStringExpression() {
        String result = ZLcSpelUtil.evaluateToString("invalid", null);
        assertThat(result).isNull();
    }

    @Test
    void shouldEvaluateObjectExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("value", 42);

        Object result = ZLcSpelUtil.evaluateToObject("#value", variables);
        assertThat(result).isEqualTo(42);
    }

    @Test
    void shouldReturnNullForInvalidObjectExpression() {
        Object result = ZLcSpelUtil.evaluateToObject("invalid", null);
        assertThat(result).isNull();
    }
}