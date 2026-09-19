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
 * ScriptAdapter 单元测试
 * <p>
 * ScriptAdapter 调用 z-script 表达式执行 API. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class ScriptAdapterTest {

    private ScriptAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new ScriptAdapter();
        Field f = ScriptAdapter.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("ScriptAdapter 应当标注 @Component", ScriptAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("ScriptAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(ScriptAdapter.class));
    }

    @Test
    public void nameShouldBeScript() {
        assertEquals("script", adapter.name());
        assertEquals("script", ScriptAdapter.NAME);
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

    @Test(expected = IllegalArgumentException.class)
    public void evalShouldThrowForNullScriptCode() {
        adapter.eval(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void evalShouldThrowForEmptyScriptCode() {
        adapter.eval("", null);
    }
}