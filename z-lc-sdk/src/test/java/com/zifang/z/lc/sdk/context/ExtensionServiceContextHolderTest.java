package com.zifang.z.lc.sdk.context;

import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExtensionServiceContextHolder 单元测试
 *
 * @author zifang
 */
public class ExtensionServiceContextHolderTest {

    @After
    public void tearDown() {
        ExtensionServiceContextHolder.clear();
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        java.lang.reflect.Constructor<ExtensionServiceContextHolder> constructor =
                ExtensionServiceContextHolder.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    public void shouldSetAndGetContext() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setAppCode("app-001");

        ExtensionServiceContextHolder.set(ctx);

        ExtensionServiceContext retrieved = ExtensionServiceContextHolder.get();
        assertEquals(ctx, retrieved);
    }

    @Test
    public void shouldReturnNullForUnsetContext() {
        assertNull(ExtensionServiceContextHolder.get());
    }

    @Test
    public void shouldClearContext() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ExtensionServiceContextHolder.set(ctx);

        ExtensionServiceContextHolder.clear();

        assertNull(ExtensionServiceContextHolder.get());
    }

    @Test
    public void shouldReturnCurrentAppCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setAppCode("app-001");
        ExtensionServiceContextHolder.set(ctx);

        assertEquals("app-001", ExtensionServiceContextHolder.currentAppCode());
    }

    @Test
    public void shouldReturnNullAppCodeWhenContextNotSet() {
        assertNull(ExtensionServiceContextHolder.currentAppCode());
    }

    @Test
    public void shouldReturnCurrentModelCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setModelCode("model-001");
        ExtensionServiceContextHolder.set(ctx);

        assertEquals("model-001", ExtensionServiceContextHolder.currentModelCode());
    }

    @Test
    public void shouldReturnNullModelCodeWhenContextNotSet() {
        assertNull(ExtensionServiceContextHolder.currentModelCode());
    }
}