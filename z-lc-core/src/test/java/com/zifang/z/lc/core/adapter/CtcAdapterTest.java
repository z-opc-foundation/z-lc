package com.zifang.z.lc.core.adapter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * CtcAdapter 单元测试
 * <p>
 * CtcAdapter 调用 z-ctc 的鉴权 API (HTTP), 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class CtcAdapterTest {

    private CtcAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new CtcAdapter();
        Field baseUrlField = CtcAdapter.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("CtcAdapter 应当标注 @Component", CtcAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("CtcAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(CtcAdapter.class));
    }

    @Test
    public void nameShouldBeCtc() {
        assertEquals("ctc", adapter.name());
        assertEquals("ctc", CtcAdapter.NAME);
    }

    @Test
    public void priorityShouldBeTwenty() {
        assertEquals(20, adapter.priority());
    }

    @Test
    public void initShouldNotThrow() {
        try {
            adapter.init();
        } catch (Exception ex) {
            assertFalse("init 不应抛异常: " + ex.getMessage(), true);
        }
    }

    @Test
    public void checkAuthShouldReturnTrueForNullPermission() {
        assertTrue(adapter.checkAuth(null, null));
    }

    @Test
    public void checkAuthShouldReturnFalseForNullRequestWithPermission() {
        assertFalse(adapter.checkAuth(null, "read"));
    }

    @Test
    public void asStringShouldHandleNull() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asString", Object.class);
        m.setAccessible(true);
        assertEquals(null, m.invoke(null, (Object) null));
    }

    @Test
    public void asStringShouldConvertValue() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asString", Object.class);
        m.setAccessible(true);
        assertEquals("42", m.invoke(null, 42));
    }

    @Test
    public void asStringListShouldHandleNull() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(null, (Object) null);
        assertNotNull(result);
        assertTrue(((java.util.List<?>) result).isEmpty());
    }

    @Test
    public void asStringListShouldConvertListItems() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        java.util.List<Integer> input = new java.util.ArrayList<>();
        input.add(1);
        input.add(2);
        input.add(null);
        input.add(3);
        Object result = m.invoke(null, (Object) input);
        assertNotNull(result);
        java.util.List<?> out = (java.util.List<?>) result;
        assertEquals(3, out.size());
        assertEquals("1", out.get(0));
        assertEquals("2", out.get(1));
        assertEquals("3", out.get(2));
    }

    @Test
    public void asStringListShouldReturnEmptyForNonList() throws Exception {
        java.lang.reflect.Method m = CtcAdapter.class.getDeclaredMethod("asStringList", Object.class);
        m.setAccessible(true);
        Object result = m.invoke(null, "not-a-list");
        assertNotNull(result);
        assertTrue(((java.util.List<?>) result).isEmpty());
    }

    @Test
    public void currentContextShouldReturnNullForNullRequest() {
        assertEquals(null, adapter.currentContext(null));
    }
}