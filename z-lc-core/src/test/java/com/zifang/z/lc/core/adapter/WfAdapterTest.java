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
 * WfAdapter 单元测试
 * <p>
 * WfAdapter 调用 z-wf 的流程引擎 API. 通过反射注入 baseUrl 避免真实网络依赖.
 */
public class WfAdapterTest {

    private WfAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new WfAdapter();
        Field f = WfAdapter.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(adapter, "http://localhost:8888");
    }

    @Test
    public void shouldBeAnnotatedWithComponent() {
        assertNotNull("WfAdapter 应当标注 @Component", WfAdapter.class.getAnnotation(Component.class));
    }

    @Test
    public void shouldImplementAdapterInterface() {
        assertTrue("WfAdapter 应当实现 Adapter",
                Adapter.class.isAssignableFrom(WfAdapter.class));
    }

    @Test
    public void nameShouldBeWf() {
        assertEquals("wf", adapter.name());
        assertEquals("wf", WfAdapter.NAME);
    }

    @Test
    public void priorityShouldBeTwentyFive() {
        assertEquals(25, adapter.priority());
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
    public void startProcessShouldReturnNullForNullProcessDefKey() {
        assertNull(adapter.startProcess(null, "biz-1", null));
    }

    @Test
    public void startProcessShouldReturnNullForEmptyProcessDefKey() {
        assertNull(adapter.startProcess("", "biz-1", null));
    }

    @Test
    public void suspendProcessShouldReturnFalseForNullInstanceId() {
        assertFalse(adapter.suspendProcess(null));
    }
}