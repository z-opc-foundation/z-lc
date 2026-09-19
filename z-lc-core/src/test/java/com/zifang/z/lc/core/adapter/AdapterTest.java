package com.zifang.z.lc.core.adapter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Adapter 接口单元测试 — 验证接口契约可以被实现.
 *
 * @author zifang
 */
public class AdapterTest {

    @Test
    public void shouldDefineInterfaceMethods() {
        // We can verify interface methods exist via reflection
        java.lang.reflect.Method[] methods = Adapter.class.getDeclaredMethods();
        boolean hasName = false, hasPriority = false, hasInit = false;
        for (java.lang.reflect.Method m : methods) {
            if (m.getName().equals("name")) hasName = true;
            if (m.getName().equals("priority")) hasPriority = true;
            if (m.getName().equals("init")) hasInit = true;
        }
        assertTrue("Adapter should declare name()", hasName);
        assertTrue("Adapter should declare priority()", hasPriority);
        assertTrue("Adapter should declare init()", hasInit);
    }

    @Test
    public void shouldBeImplementable() {
        TestAdapter adapter = new TestAdapter("test-adapter", 100);
        assertNotNull(adapter);
        assertEquals("test-adapter", adapter.name());
        assertEquals(100, adapter.priority());
    }

    @Test
    public void shouldCallInit() {
        TestAdapter adapter = new TestAdapter("test", 1);
        adapter.init();
        assertTrue(adapter.initialized);
    }

    private static class TestAdapter implements Adapter {
        private final String name;
        private final int priority;
        boolean initialized = false;

        TestAdapter(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public void init() {
            initialized = true;
        }
    }
}