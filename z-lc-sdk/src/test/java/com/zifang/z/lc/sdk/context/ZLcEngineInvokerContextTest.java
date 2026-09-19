package com.zifang.z.lc.sdk.context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcEngineInvokerContext ThreadLocal 上下文单元测试
 */
public class ZLcEngineInvokerContextTest {

    @Before
    public void cleanupBefore() {
        ZLcEngineInvokerContext.clean();
    }

    @After
    public void cleanup() {
        ZLcEngineInvokerContext.clean();
    }

    @Test
    public void shouldBeFinalClass() {
        assertTrue("ZLcEngineInvokerContext 应当是 final",
                Modifier.isFinal(ZLcEngineInvokerContext.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcEngineInvokerContext> ctor =
                ZLcEngineInvokerContext.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.context",
                ZLcEngineInvokerContext.class.getPackage().getName());
    }

    @Test
    public void shouldHaveFieldConstants() {
        assertEquals("uuid", ZLcEngineInvokerContext.UUID_CODE);
        assertEquals("mode", ZLcEngineInvokerContext.MODE_CODE);
        assertEquals("business_context", ZLcEngineInvokerContext.BUSINESS_CONTEXT);
        assertEquals("appCode", ZLcEngineInvokerContext.APP_CODE);
        assertEquals("modelCode", ZLcEngineInvokerContext.MODEL_CODE);
        assertEquals("pageCode", ZLcEngineInvokerContext.PAGE_CODE);
        assertEquals("SAVE_FLAG", ZLcEngineInvokerContext.SAVE_FLAG);
        assertEquals("currentTaskDefKey", ZLcEngineInvokerContext.CURRENT_TASK_DEF_KEY);
        assertEquals("isMobile", ZLcEngineInvokerContext.IS_MOBILE);
    }

    @Test
    public void setAndGetUUIDShouldWork() {
        ZLcEngineInvokerContext.setUUID("u-001");
        assertEquals("u-001", ZLcEngineInvokerContext.getUUID());
    }

    @Test
    public void setAndGetModeShouldWork() {
        ZLcEngineInvokerContext.setMode(2);
        Integer mode = ZLcEngineInvokerContext.getMode();
        assertNotNull(mode);
        assertEquals(Integer.valueOf(2), mode);
    }

    @Test
    public void setAndGetBusinessContextShouldWork() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("k", "v");
        ZLcEngineInvokerContext.setBusinessContext(ctx);

        Map<String, Object> retrieved = ZLcEngineInvokerContext.getBusinessContext();
        assertNotNull(retrieved);
        assertEquals("v", retrieved.get("k"));
    }

    @Test
    public void setAndGetAppCodeShouldWork() {
        ZLcEngineInvokerContext.setAppCode("crm");
        assertEquals("crm", ZLcEngineInvokerContext.getAppCode());
    }

    @Test
    public void setAndGetModelCodeShouldWork() {
        ZLcEngineInvokerContext.setModelCode("customer");
        assertEquals("customer", ZLcEngineInvokerContext.getModelCode());
    }

    @Test
    public void setAndGetPageCodeShouldWork() {
        ZLcEngineInvokerContext.setPageCode("customer-list");
        assertEquals("customer-list", ZLcEngineInvokerContext.getPageCode());
    }

    @Test
    public void setAndGetSaveFlagShouldWork() {
        ZLcEngineInvokerContext.setSaveFlag("SUCCESS");
        assertEquals("SUCCESS", ZLcEngineInvokerContext.getSaveFlag());
    }

    @Test
    public void setAndGetCurrentTaskDefKeyShouldWork() {
        ZLcEngineInvokerContext.setCurrentTaskDefKey("approveTask");
        assertEquals("approveTask", ZLcEngineInvokerContext.getCurrentTaskDefKey());
    }

    @Test
    public void setAndGetIsMobileShouldWork() {
        ZLcEngineInvokerContext.setIsMobile(true);
        assertEquals(Boolean.TRUE, ZLcEngineInvokerContext.getIsMobile());
    }

    @Test
    public void getIsMobileShouldReturnFalseWhenNull() {
        // 默认值是 false (因为 v != null && (Boolean) v; v 是 null 时返回 false)
        assertEquals(Boolean.FALSE, ZLcEngineInvokerContext.getIsMobile());
    }

    @Test
    public void setAndGetTagsShouldWork() {
        List<String> tags = Arrays.asList("gray", "vip");
        ZLcEngineInvokerContext.setTags(tags);

        List<String> retrieved = ZLcEngineInvokerContext.getTags();
        assertNotNull(retrieved);
        assertEquals(2, retrieved.size());
    }

    @Test
    public void setContextMapShouldOverrideAll() {
        Map<String, Object> initial = new HashMap<>();
        initial.put("a", 1);
        ZLcEngineInvokerContext.setContextMap(initial);

        Map<String, Object> replacement = new HashMap<>();
        replacement.put("b", 2);
        ZLcEngineInvokerContext.setContextMap(replacement);

        assertEquals(2, ZLcEngineInvokerContext.getContextMap().get("b"));
    }

    @Test
    public void setContextMapNullShouldBeNoOp() {
        ZLcEngineInvokerContext.setUUID("u-001");
        ZLcEngineInvokerContext.setContextMap(null);

        // null 不应清除, 已有值保留
        assertEquals("u-001", ZLcEngineInvokerContext.getUUID());
    }

    @Test
    public void getContextMapShouldAutoInitWhenNull() {
        // 第一次访问会自动 new 一个空 Map
        Map<String, Object> map = ZLcEngineInvokerContext.getContextMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void getUUIDShouldReturnNullForEmpty() {
        assertNull(ZLcEngineInvokerContext.getUUID());
    }

    @Test
    public void getAppCodeShouldReturnNullForEmpty() {
        assertNull(ZLcEngineInvokerContext.getAppCode());
    }

    @Test
    public void getModeShouldReturnNullForEmpty() {
        assertNull(ZLcEngineInvokerContext.getMode());
    }

    @Test
    public void getBusinessContextShouldReturnNullForEmpty() {
        assertNull(ZLcEngineInvokerContext.getBusinessContext());
    }

    @Test
    public void getTagsShouldReturnNullForEmpty() {
        assertNull(ZLcEngineInvokerContext.getTags());
    }

    @Test
    public void cleanShouldRemoveAll() {
        ZLcEngineInvokerContext.setUUID("u-001");
        ZLcEngineInvokerContext.setAppCode("crm");
        ZLcEngineInvokerContext.setModelCode("customer");
        ZLcEngineInvokerContext.setMode(1);

        ZLcEngineInvokerContext.clean();

        assertNull(ZLcEngineInvokerContext.getUUID());
        assertNull(ZLcEngineInvokerContext.getAppCode());
        assertNull(ZLcEngineInvokerContext.getModelCode());
        assertNull(ZLcEngineInvokerContext.getMode());
    }

    @Test
    public void shouldHandleUUIDNotString() {
        // 通过 contextMap 直接塞非 String 值, getUUID 应走 toString 路径
        Map<String, Object> ctx = ZLcEngineInvokerContext.getContextMap();
        ctx.put(ZLcEngineInvokerContext.UUID_CODE, 123);
        assertEquals("123", ZLcEngineInvokerContext.getUUID());
    }

    @Test
    public void getContextMapShouldReturnSameReference() {
        Map<String, Object> first = ZLcEngineInvokerContext.getContextMap();
        first.put("x", 1);
        Map<String, Object> second = ZLcEngineInvokerContext.getContextMap();
        assertEquals(Integer.valueOf(1), second.get("x"));
    }
}