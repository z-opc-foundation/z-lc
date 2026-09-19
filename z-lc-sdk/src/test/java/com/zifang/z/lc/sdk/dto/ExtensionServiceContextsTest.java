package com.zifang.z.lc.sdk.dto;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExtensionServiceContexts 工厂类单元测试
 */
public class ExtensionServiceContextsTest {

    @Test
    public void shouldBeFinalClass() {
        assertTrue("ExtensionServiceContexts 应当是 final 类",
                Modifier.isFinal(ExtensionServiceContexts.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        java.lang.reflect.Constructor<ExtensionServiceContexts> ctor =
                ExtensionServiceContexts.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.dto",
                ExtensionServiceContexts.class.getPackage().getName());
    }

    @Test
    public void shouldNotBeInstantiable() throws Exception {
        java.lang.reflect.Constructor<ExtensionServiceContexts> ctor =
                ExtensionServiceContexts.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ExtensionServiceContexts instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    public void ofWithTwoArgsShouldReturnContext() {
        ExtensionServiceContext ctx = ExtensionServiceContexts.of("crm", "customer");
        assertNotNull(ctx);
        assertEquals("crm", ctx.getAppCode());
        assertEquals("customer", ctx.getModelCode());
        assertNull(ctx.getPageCode());
    }

    @Test
    public void ofWithThreeArgsShouldReturnContext() {
        ExtensionServiceContext ctx = ExtensionServiceContexts.of("crm", "customer", "customer-list");
        assertNotNull(ctx);
        assertEquals("crm", ctx.getAppCode());
        assertEquals("customer", ctx.getModelCode());
        assertEquals("customer-list", ctx.getPageCode());
    }

    @Test
    public void ofWithNullArgsShouldStillConstruct() {
        ExtensionServiceContext ctx = ExtensionServiceContexts.of(null, null);
        assertNotNull(ctx);
        assertNull(ctx.getAppCode());
        assertNull(ctx.getModelCode());
    }

    @Test
    public void ofWithEmptyStringArgs() {
        ExtensionServiceContext ctx = ExtensionServiceContexts.of("", "");
        assertEquals("", ctx.getAppCode());
        assertEquals("", ctx.getModelCode());
    }

    @Test
    public void ofWithTwoArgsAndNullPageCode() {
        ExtensionServiceContext ctx = ExtensionServiceContexts.of("crm", "customer", null);
        assertNull(ctx.getPageCode());
    }

    @Test
    public void shouldHaveStaticOfMethods() throws NoSuchMethodException {
        ExtensionServiceContexts.class.getMethod("of", String.class, String.class);
        ExtensionServiceContexts.class.getMethod("of", String.class, String.class, String.class);
    }

    @Test
    public void ofShouldReturnDifferentInstances() {
        ExtensionServiceContext a = ExtensionServiceContexts.of("crm", "customer");
        ExtensionServiceContext b = ExtensionServiceContexts.of("crm", "customer");
        // 即使参数相同, 也返回不同实例
        assertNotNull(a);
        assertNotNull(b);
    }
}