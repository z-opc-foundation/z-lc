package com.zifang.z.lc.core.adapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * AdapterRegistry 单元测试
 *
 * @author zifang
 */
public class AdapterRegistryTest {

    @Test
    public void shouldCreateWithEmptyAdapters() {
        AdapterRegistry registry = new AdapterRegistry(null);
        assertNotNull(registry);
        assertEquals(0, registry.all().size());
    }

    @Test
    public void shouldCreateWithEmptyList() {
        AdapterRegistry registry = new AdapterRegistry(Collections.emptyList());
        assertEquals(0, registry.all().size());
    }

    @Test
    public void shouldRegisterAdapters() {
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList(
                new TestAdapter("a", 1),
                new TestAdapter("b", 2)
        ));
        assertEquals(2, registry.all().size());
    }

    @Test
    public void shouldSortAdaptersByPriority() {
        Adapter a1 = new TestAdapter("a", 30);
        Adapter a2 = new TestAdapter("b", 10);
        Adapter a3 = new TestAdapter("c", 20);
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList(a1, a2, a3));
        List<Adapter> all = registry.all();
        assertEquals("b", all.get(0).name());
        assertEquals("c", all.get(1).name());
        assertEquals("a", all.get(2).name());
    }

    @Test
    public void shouldGetByName() {
        Adapter a = new TestAdapter("foo", 1);
        AdapterRegistry registry = new AdapterRegistry(Collections.singletonList(a));
        assertSame(a, registry.get("foo"));
    }

    @Test
    public void shouldReturnNullForUnknownName() {
        AdapterRegistry registry = new AdapterRegistry(Collections.singletonList(new TestAdapter("foo", 1)));
        assertNull(registry.get("bar"));
    }

    @Test
    public void shouldReturnNullForGetOnEmptyRegistry() {
        AdapterRegistry registry = new AdapterRegistry(null);
        assertNull(registry.get("any"));
    }

    @Test
    public void shouldThrowForDuplicateAdapterName() {
        try {
            new AdapterRegistry(Arrays.asList(
                    new TestAdapter("dup", 1),
                    new TestAdapter("dup", 2)
            ));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("dup"));
        }
    }

    @Test
    public void shouldReturnUnmodifiableList() {
        AdapterRegistry registry = new AdapterRegistry(Arrays.asList(new TestAdapter("a", 1)));
        List<Adapter> all = registry.all();
        try {
            all.add(new TestAdapter("b", 2));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    private static class TestAdapter implements Adapter {
        private final String name;
        private final int priority;

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
            // no-op
        }
    }
}