package com.zifang.z.lc.sdk.context;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcBusinessContextPropagator SPI 接口单元测试
 */
public class ZLcBusinessContextPropagatorTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ZLcBusinessContextPropagator.class.isInterface());
    }

    @Test
    public void shouldDeclarePropagateMethod() throws NoSuchMethodException {
        Method m = ZLcBusinessContextPropagator.class.getMethod("propagate", Map.class);
        assertNotNull(m);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareClearMethod() throws NoSuchMethodException {
        Method m = ZLcBusinessContextPropagator.class.getMethod("clear");
        assertNotNull(m);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldHaveKeyConstant() {
        assertEquals("business_context", ZLcBusinessContextPropagator.KEY);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.context",
                ZLcBusinessContextPropagator.class.getPackage().getName());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        final java.util.List<String> calls = new java.util.ArrayList<>();
        ZLcBusinessContextPropagator impl = new ZLcBusinessContextPropagator() {
            @Override
            public void propagate(Map<String, Object> businessContext) {
                calls.add("propagate");
            }

            @Override
            public void clear() {
                calls.add("clear");
            }
        };
        impl.propagate(new HashMap<>());
        impl.clear();
        assertEquals(2, calls.size());
        assertTrue(calls.contains("propagate"));
        assertTrue(calls.contains("clear"));
    }

    @Test
    public void propagateMethodShouldAcceptNull() {
        ZLcBusinessContextPropagator impl = new ZLcBusinessContextPropagator() {
            @Override
            public void propagate(Map<String, Object> businessContext) {}
            @Override
            public void clear() {}
        };
        impl.propagate(null);  // 不抛异常即视为成功
    }

    @Test
    public void interfaceMethodsShouldBePublic() throws NoSuchMethodException {
        Method propagate = ZLcBusinessContextPropagator.class.getMethod("propagate", Map.class);
        Method clear = ZLcBusinessContextPropagator.class.getMethod("clear");
        assertTrue(Modifier.isPublic(propagate.getModifiers()));
        assertTrue(Modifier.isPublic(clear.getModifiers()));
        assertTrue(Modifier.isAbstract(propagate.getModifiers()));
        assertTrue(Modifier.isAbstract(clear.getModifiers()));
    }
}