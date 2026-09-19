package com.zifang.z.lc.sdk.reflect;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * QlExpressionUtil 单元测试
 */
public class QlExpressionUtilTest {

    @Test
    public void shouldBeFinalClass() {
        assertTrue("QlExpressionUtil 应当是 final",
                Modifier.isFinal(QlExpressionUtil.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<QlExpressionUtil> ctor = QlExpressionUtil.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.reflect",
                QlExpressionUtil.class.getPackage().getName());
    }

    @Test
    public void executeRuleShouldReturnFalseForNullRule() {
        Map<String, Object> params = new HashMap<>();
        assertFalse(QlExpressionUtil.executeRule(params, null));
    }

    @Test
    public void executeRuleShouldReturnFalseForEmptyRule() {
        assertFalse(QlExpressionUtil.executeRule(new HashMap<>(), ""));
    }

    @Test
    public void executeRuleShouldReturnFalseForWhitespaceRule() {
        assertFalse(QlExpressionUtil.executeRule(new HashMap<>(), "   "));
    }

    @Test
    public void executeRuleShouldReturnFalseOnException() {
        // 无效表达式应 fail-safe 返回 false
        assertFalse(QlExpressionUtil.executeRule(new HashMap<>(), "(((invalid"));
    }

    @Test
    public void evaluateShouldReturnNullForNullRule() {
        assertEquals(null, QlExpressionUtil.evaluate(new HashMap<>(), null));
    }

    @Test
    public void evaluateShouldReturnNullForEmptyRule() {
        assertEquals(null, QlExpressionUtil.evaluate(new HashMap<>(), ""));
    }

    @Test
    public void shouldHandleNullParamMap() {
        // null paramMap 时不抛异常
        try {
            boolean r = QlExpressionUtil.executeRule(null, "1 == 1");
            // 不抛即视为成功
        } catch (Exception e) {
            org.junit.Assert.fail("executeRule(null paramMap, \"1 == 1\") 不应抛异常");
        }
    }

    @Test
    public void shouldSupportEqualityWithStringLiteral() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantCode", "a");
        assertTrue(QlExpressionUtil.executeRule(params, "tenantCode == 'a'"));
    }

    @Test
    public void shouldSupportEqualityWithDoubleQuote() {
        Map<String, Object> params = new HashMap<>();
        params.put("modelCode", "user");
        assertTrue(QlExpressionUtil.executeRule(params, "modelCode == \"user\""));
    }

    @Test
    public void shouldSupportInequality() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantCode", "b");
        assertTrue(QlExpressionUtil.executeRule(params, "tenantCode != 'a'"));
    }

    @Test
    public void shouldSupportGreaterEqual() {
        Map<String, Object> params = new HashMap<>();
        params.put("version", 2);
        assertTrue(QlExpressionUtil.executeRule(params, "version >= 2"));
    }

    @Test
    public void shouldSupportLess() {
        Map<String, Object> params = new HashMap<>();
        params.put("version", 5);
        assertTrue(QlExpressionUtil.executeRule(params, "version < 10"));
    }

    @Test
    public void shouldSupportLogicalAnd() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantCode", "a");
        params.put("version", 2);
        assertTrue(QlExpressionUtil.executeRule(params, "tenantCode == 'a' && version >= 1"));
    }

    @Test
    public void shouldSupportLogicalOr() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantCode", "b");
        assertTrue(QlExpressionUtil.executeRule(params, "tenantCode == 'a' || tenantCode == 'b'"));
    }

    @Test
    public void shouldReturnBooleanOrOtherObject() {
        Object r1 = QlExpressionUtil.evaluate(new HashMap<>(), "1 == 1");
        assertNotNull(r1);

        Object r2 = QlExpressionUtil.evaluate(new HashMap<>(), "'hello'");
        assertNotNull(r2);
    }

    @Test
    public void shouldHaveStaticExecuteRule() throws NoSuchMethodException {
        java.lang.reflect.Method m = QlExpressionUtil.class.getMethod("executeRule", Map.class, String.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void shouldHaveStaticEvaluate() throws NoSuchMethodException {
        java.lang.reflect.Method m = QlExpressionUtil.class.getMethod("evaluate", Map.class, String.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void shouldReturnFalseForEmptyParamMap() {
        // 无匹配字段, 默认 null, isTruthy(null) -> false
        assertFalse(QlExpressionUtil.executeRule(new HashMap<>(), "tenantCode == 'a'"));
    }

    @Test
    public void shouldHandleOuterParentheses() {
        Map<String, Object> params = new HashMap<>();
        params.put("v", 5);
        assertTrue(QlExpressionUtil.executeRule(params, "(v == 5)"));
    }
}