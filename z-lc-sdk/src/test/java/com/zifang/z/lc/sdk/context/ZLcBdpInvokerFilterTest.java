package com.zifang.z.lc.sdk.context;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcBdpInvokerFilter SPI 接口单元测试
 */
public class ZLcBdpInvokerFilterTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ZLcBdpInvokerFilter.class.isInterface());
    }

    @Test
    public void shouldDeclareBeforeInvokeMethod() throws NoSuchMethodException {
        Method m = ZLcBdpInvokerFilter.class.getMethod("beforeInvoke");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareAfterInvokeMethod() throws NoSuchMethodException {
        Method m = ZLcBdpInvokerFilter.class.getMethod("afterInvoke");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareOnReceiveMethod() throws NoSuchMethodException {
        Method m = ZLcBdpInvokerFilter.class.getMethod("onReceive");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void shouldHaveAttachmentKeyConstants() throws Exception {
        assertEquals("uuid", ZLcBdpInvokerFilter.KEY_UUID);
        assertEquals("mode", ZLcBdpInvokerFilter.KEY_MODE);
        assertEquals("business_context", ZLcBdpInvokerFilter.KEY_BUSINESS_CONTEXT);
        assertEquals("appCode", ZLcBdpInvokerFilter.KEY_APP_CODE);
        assertEquals("modelCode", ZLcBdpInvokerFilter.KEY_MODEL_CODE);
        assertEquals("pageCode", ZLcBdpInvokerFilter.KEY_PAGE_CODE);
        assertEquals("SAVE_FLAG", ZLcBdpInvokerFilter.KEY_SAVE_FLAG);
        assertEquals("currentTaskDefKey", ZLcBdpInvokerFilter.KEY_CURRENT_TASK_DEF_KEY);
        assertEquals("isMobile", ZLcBdpInvokerFilter.KEY_IS_MOBILE);
        assertEquals("tags", ZLcBdpInvokerFilter.KEY_TAGS);
    }

    @Test
    public void constantsShouldBePublicStaticFinal() throws Exception {
        for (Field f : ZLcBdpInvokerFilter.class.getDeclaredFields()) {
            if (f.isSynthetic()) continue;
            assertTrue(Modifier.isPublic(f.getModifiers()));
            assertTrue(Modifier.isStatic(f.getModifiers()));
            assertTrue(Modifier.isFinal(f.getModifiers()));
        }
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.context",
                ZLcBdpInvokerFilter.class.getPackage().getName());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        final java.util.List<String> calls = new java.util.ArrayList<>();
        ZLcBdpInvokerFilter impl = new ZLcBdpInvokerFilter() {
            @Override
            public void beforeInvoke() { calls.add("before"); }
            @Override
            public void afterInvoke() { calls.add("after"); }
            @Override
            public void onReceive() { calls.add("receive"); }
        };
        impl.beforeInvoke();
        impl.afterInvoke();
        impl.onReceive();
        assertEquals(3, calls.size());
        assertTrue(calls.contains("before"));
        assertTrue(calls.contains("after"));
        assertTrue(calls.contains("receive"));
    }

    @Test
    public void shouldHavePublicAbstractMethods() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(ZLcBdpInvokerFilter.class.getMethod("beforeInvoke").getModifiers()));
        assertTrue(Modifier.isPublic(ZLcBdpInvokerFilter.class.getMethod("afterInvoke").getModifiers()));
        assertTrue(Modifier.isPublic(ZLcBdpInvokerFilter.class.getMethod("onReceive").getModifiers()));
        assertTrue(Modifier.isAbstract(ZLcBdpInvokerFilter.class.getMethod("beforeInvoke").getModifiers()));
        assertTrue(Modifier.isAbstract(ZLcBdpInvokerFilter.class.getMethod("afterInvoke").getModifiers()));
        assertTrue(Modifier.isAbstract(ZLcBdpInvokerFilter.class.getMethod("onReceive").getModifiers()));
    }

    @Test
    public void keyCountShouldBeTen() {
        int count = 0;
        for (Field f : ZLcBdpInvokerFilter.class.getDeclaredFields()) {
            if (!f.isSynthetic() && f.getType() == String.class) {
                count++;
            }
        }
        assertNotNull("至少应当有 10 个 String 常量", count > 0);
        assertEquals(10, count);
    }
}