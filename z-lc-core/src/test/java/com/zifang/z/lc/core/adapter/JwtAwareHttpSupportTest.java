package com.zifang.z.lc.core.adapter;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JwtAwareHttpSupport 单元测试
 *
 * 侧重测试当未绑定 servlet 请求时返回空 Map 的契约;
 * 不引入 mockito / servlet 桩, 通过反射直接控制 ThreadLocal 状态.
 */
public class JwtAwareHttpSupportTest {

    @After
    public void cleanup() {
        JwtRelayInterceptor.clear();
    }

    @Test
    public void shouldReturnEmptyHeadersWhenNoRequest() {
        JwtRelayInterceptor.clear();
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();

        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    public void shouldReturnNonNullResultAlways() {
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        assertNotNull(headers);
    }

    @Test
    public void shouldReturnSameEmptyMapWhenNoRequestBound() {
        JwtRelayInterceptor.clear();
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        assertEquals(Collections.emptyMap(), headers);
    }

    @Test
    public void shouldSupportMultipleInvocations() {
        JwtRelayInterceptor.clear();
        Map<String, String> first = JwtAwareHttpSupport.currentAuthHeaders();
        Map<String, String> second = JwtAwareHttpSupport.currentAuthHeaders();

        assertEquals(first, second);
    }

    @Test
    public void classShouldBeFinal() {
        assertTrue("JwtAwareHttpSupport 应当为 final",
                Modifier.isFinal(JwtAwareHttpSupport.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<JwtAwareHttpSupport> ctor =
                JwtAwareHttpSupport.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.adapter", JwtAwareHttpSupport.class.getPackage().getName());
    }

    @Test
    public void shouldHaveCurrentAuthHeadersStaticMethod() throws NoSuchMethodException {
        Method m = JwtAwareHttpSupport.class.getMethod("currentAuthHeaders");
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(Map.class, m.getReturnType());
    }

    @Test
    public void shouldHaveNoInstanceMethods() {
        for (Method m : JwtAwareHttpSupport.class.getDeclaredMethods()) {
            if (m.isSynthetic()) continue;
            assertTrue("JwtAwareHttpSupport 的方法应当是 static: " + m.getName(),
                    Modifier.isStatic(m.getModifiers()));
        }
    }

    @Test
    public void shouldHaveNoInstanceFields() {
        for (Field f : JwtAwareHttpSupport.class.getDeclaredFields()) {
            if (f.isSynthetic()) continue;
            assertTrue("JwtAwareHttpSupport 的字段应当是 static: " + f.getName(),
                    Modifier.isStatic(f.getModifiers()));
        }
    }

    @Test
    public void headersShouldBeImmutableView() {
        JwtRelayInterceptor.clear();
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        try {
            headers.put("forbidden", "value");
            // Collections.emptyMap() 是不可变的, 应当抛 UnsupportedOperationException
            org.junit.Assert.fail("应当不可修改");
        } catch (UnsupportedOperationException expected) {
            // OK
        }
    }

    @Test
    public void shouldReturnEmptyMapAfterClear() {
        bindStubAndClear();
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    /**
     * 通过反射直接写 ThreadLocal 后清理, 模拟"曾经绑定 / 现在未绑定"场景.
     */
    private static void bindStubAndClear() {
        try {
            Field f = JwtRelayInterceptor.class.getDeclaredField("CURRENT");
            f.setAccessible(true);
            ThreadLocal<Object> tl = (ThreadLocal<Object>) f.get(null);
            tl.set(new Object());
            JwtRelayInterceptor.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}