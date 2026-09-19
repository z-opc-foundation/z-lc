package com.zifang.z.lc.sdk.context;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcBdpInvokerContext ThreadLocal 上下文单元测试
 */
public class ZLcBdpInvokerContextTest {

    @After
    public void cleanup() {
        ZLcBdpInvokerContext.clean();
    }

    @Test
    public void shouldBeFinalClass() {
        assertTrue("ZLcBdpInvokerContext 应当是 final",
                Modifier.isFinal(ZLcBdpInvokerContext.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcBdpInvokerContext> ctor =
                ZLcBdpInvokerContext.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeDeprecated() {
        assertTrue("ZLcBdpInvokerContext 应当标注 @Deprecated",
                ZLcBdpInvokerContext.class.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.context",
                ZLcBdpInvokerContext.class.getPackage().getName());
    }

    @Test
    public void getUUIDShouldReturnNullByDefault() {
        assertNull(ZLcBdpInvokerContext.getUUID());
    }

    @Test
    public void setAndGetUUIDShouldWork() {
        ZLcBdpInvokerContext.setUUID("uuid-001");
        assertEquals("uuid-001", ZLcBdpInvokerContext.getUUID());
    }

    @Test
    public void setUUIDShouldOverridePrevious() {
        ZLcBdpInvokerContext.setUUID("uuid-001");
        ZLcBdpInvokerContext.setUUID("uuid-002");
        assertEquals("uuid-002", ZLcBdpInvokerContext.getUUID());
    }

    @Test
    public void setUUIDNullShouldStoreNull() {
        ZLcBdpInvokerContext.setUUID("uuid-001");
        ZLcBdpInvokerContext.setUUID(null);
        assertNull(ZLcBdpInvokerContext.getUUID());
    }

    @Test
    public void cleanShouldRemoveUUID() {
        ZLcBdpInvokerContext.setUUID("uuid-001");
        ZLcBdpInvokerContext.clean();
        assertNull(ZLcBdpInvokerContext.getUUID());
    }

    @Test
    public void cleanShouldBeIdempotent() {
        ZLcBdpInvokerContext.clean();
        ZLcBdpInvokerContext.clean();
        assertNull(ZLcBdpInvokerContext.getUUID());
    }
}