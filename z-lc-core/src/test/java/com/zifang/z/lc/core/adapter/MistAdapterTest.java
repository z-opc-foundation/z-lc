package com.zifang.z.lc.core.adapter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MistAdapter 单元测试
 * <p>
 * MistAdapter 调用 z-mist 的密钥服务. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class MistAdapterTest {

    private MistAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MistAdapter();
        Field baseUrlField = MistAdapter.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("MistAdapter 应当标注 @Component", MistAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("MistAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(MistAdapter.class));
    }

    @Test
    public void nameShouldBeMist() {
        assertEquals("mist", adapter.name());
        assertEquals("mist", MistAdapter.NAME);
    }

    @Test
    public void priorityShouldBeThirty() {
        assertEquals(30, adapter.priority());
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
    public void decryptShouldReturnNullForNullKey() {
        assertNull(adapter.decrypt(null, "default", "default"));
    }

    @Test
    public void decryptShouldReturnNullForEmptyKey() {
        assertNull(adapter.decrypt("", "default", "default"));
    }
}