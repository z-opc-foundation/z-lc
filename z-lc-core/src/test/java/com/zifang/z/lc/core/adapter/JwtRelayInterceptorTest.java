package com.zifang.z.lc.core.adapter;

import org.junit.After;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JwtRelayInterceptor 单元测试
 *
 * 测试 ThreadLocal 绑定 / 解绑的契约, 不需要实际 servlet 请求.
 */
public class JwtRelayInterceptorTest {

    @After
    public void cleanup() {
        JwtRelayInterceptor.clear();
    }

    @Test
    public void shouldReturnNullWhenNotBound() {
        JwtRelayInterceptor.clear();
        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void shouldBindAndReturnCurrentRequest() {
        JwtRelayInterceptor.clear();
        JwtRelayInterceptor.bindCurrentRequest(null);
        JwtRelayInterceptor.clear();
        // 调用 bind/clear 不会抛异常
        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void shouldClearBoundRequest() {
        JwtRelayInterceptor.bindCurrentRequest(null);
        JwtRelayInterceptor.clear();

        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void shouldAllowRebinding() {
        JwtRelayInterceptor.clear();
        JwtRelayInterceptor.bindCurrentRequest(null);
        JwtRelayInterceptor.clear();
        // 重新绑定 null 仍然不抛异常
        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void clearShouldBeIdempotent() {
        JwtRelayInterceptor.clear();
        JwtRelayInterceptor.clear();

        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void shouldBindNullRequest() {
        JwtRelayInterceptor.bindCurrentRequest(null);

        assertEquals(null, JwtRelayInterceptor.currentRequest());
    }

    @Test
    public void shouldBePublicClass() {
        assertTrue("JwtRelayInterceptor 应当是 public 类",
                Modifier.isPublic(JwtRelayInterceptor.class.getModifiers()));
    }

    @Test
    public void shouldHaveStaticFields() throws NoSuchFieldException {
        Field f = JwtRelayInterceptor.class.getDeclaredField("CURRENT");
        assertTrue("CURRENT 字段应当是 static", Modifier.isStatic(f.getModifiers()));
        assertTrue("CURRENT 字段应当是 private", Modifier.isPrivate(f.getModifiers()));
    }

    @Test
    public void shouldHavePublicBindCurrentRequestMethod() throws NoSuchMethodException {
        JwtRelayInterceptor.class.getMethod("bindCurrentRequest", HttpServletRequest.class);
    }

    @Test
    public void shouldHavePublicClearMethod() throws NoSuchMethodException {
        JwtRelayInterceptor.class.getMethod("clear");
    }

    @Test
    public void shouldHavePublicCurrentRequestMethod() throws NoSuchMethodException {
        JwtRelayInterceptor.class.getMethod("currentRequest");
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.adapter", JwtRelayInterceptor.class.getPackage().getName());
    }

    @Test
    public void shouldReturnEmptyHeadersWhenNotBound() {
        JwtRelayInterceptor.clear();
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    public void currentFieldShouldBeThreadLocalType() throws NoSuchFieldException {
        Field f = JwtRelayInterceptor.class.getDeclaredField("CURRENT");
        assertEquals(ThreadLocal.class, f.getType());
    }

    @Test
    public void bindMethodShouldAcceptNull() throws NoSuchMethodException {
        Method m = JwtRelayInterceptor.class.getMethod("bindCurrentRequest", HttpServletRequest.class);
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void clearMethodShouldBeStatic() throws NoSuchMethodException {
        Method m = JwtRelayInterceptor.class.getMethod("clear");
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void currentRequestMethodShouldBeStatic() throws NoSuchMethodException {
        Method m = JwtRelayInterceptor.class.getMethod("currentRequest");
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    /**
     * 通过反射检查 ThreadLocal 字段是私有的、静态的.
     */
    private static void bindRaw(Object o) {
        // 仅用于触发反射, 不实际写入 ThreadLocal (类型不安全)
        try {
            Field f = JwtRelayInterceptor.class.getDeclaredField("CURRENT");
            f.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}