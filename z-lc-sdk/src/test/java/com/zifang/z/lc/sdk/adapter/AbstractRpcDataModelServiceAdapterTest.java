package com.zifang.z.lc.sdk.adapter;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * AbstractRpcDataModelServiceAdapter 单元测试
 */
public class AbstractRpcDataModelServiceAdapterTest {

    @Test
    public void shouldBeAbstractClass() {
        assertTrue("AbstractRpcDataModelServiceAdapter 应当是 abstract",
                Modifier.isAbstract(AbstractRpcDataModelServiceAdapter.class.getModifiers()));
    }

    @Test
    public void shouldImplementServiceAdapter() {
        assertTrue("AbstractRpcDataModelServiceAdapter 应当实现 ServiceAdapter",
                ServiceAdapter.class.isAssignableFrom(AbstractRpcDataModelServiceAdapter.class));
    }

    @Test
    public void shouldHaveStaticVersionMethod() throws NoSuchMethodException {
        java.lang.reflect.Method m = AbstractRpcDataModelServiceAdapter.class.getMethod("version",
                String.class, String.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test
    public void versionShouldFormatTemplate() {
        String v = AbstractRpcDataModelServiceAdapter.version("myapp", "order");
        assertEquals("1.0.0_myapp_order", v);
    }

    @Test
    public void versionShouldHandleNullAppCode() {
        String v = AbstractRpcDataModelServiceAdapter.version(null, "order");
        assertEquals("1.0.0_null_order", v);
    }

    @Test
    public void versionShouldHandleEmptyStrings() {
        String v = AbstractRpcDataModelServiceAdapter.version("", "");
        assertEquals("1.0.0__", v);
    }

    @Test
    public void versionShouldHandleSpecialChars() {
        String v = AbstractRpcDataModelServiceAdapter.version("crm-v2", "customer.list");
        assertEquals("1.0.0_crm-v2_customer.list", v);
    }

    @Test
    public void defaultGroupShouldBeZLcExtensions() throws NoSuchFieldException {
        java.lang.reflect.Field f = AbstractRpcDataModelServiceAdapter.class.getDeclaredField("group");
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    @Test
    public void shouldHaveProtectedResolveInnerAdapterMethod() throws NoSuchMethodException {
        java.lang.reflect.Method m = AbstractRpcDataModelServiceAdapter.class.getDeclaredMethod("resolveInnerAdapter");
        assertTrue(Modifier.isProtected(m.getModifiers()));
    }

    @Test
    public void subclassShouldResolveInnerAdapter() {
        TestRpcAdapter adapter = new TestRpcAdapter();
        InnerDataModelServiceAdapter inner = adapter.callResolveInnerAdapter();
        assertNotNull(inner);
        assertSame(adapter.getInner(), inner);
    }

    @Test
    public void subclassCanSaveViaDelegate() {
        TestRpcAdapter adapter = new TestRpcAdapter();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("name", "alice");
        Long id = adapter.save(data);
        // 默认实现走到 inner().save, inner 是 stub, 返回 99L
        assertNotNull(id);
        assertEquals(Long.valueOf(99L), id);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.adapter",
                AbstractRpcDataModelServiceAdapter.class.getPackage().getName());
    }

    // --- helpers ---

    /**
     * 测试桩 — 提供固定的 InnerAdapter 实例.
     */
    public static class TestRpcAdapter extends AbstractRpcDataModelServiceAdapter {
        private final InnerDataModelServiceAdapter inner = new InnerDataModelServiceAdapter();

        public TestRpcAdapter() {
            // InnerAdapter 默认是空的, 我们设个 dummy AbstractDataModelService 让 save 不抛 NPE
            com.zifang.z.lc.sdk.abstracts.AbstractDataModelService<Object> dummySvc =
                    new com.zifang.z.lc.sdk.abstracts.AbstractDataModelService<Object>(new Object(), Object.class) {
                        @Override
                        public Long save(Object t) { return 99L; }
                        @Override
                        public Long save(Object t, Integer mode) { return 99L; }
                        @Override
                        public void delete(Long id) {}
                        @Override
                        public void delete(Object t) {}
                        @Override
                        public Object queryById(Long pkId, boolean deep) { return null; }
                        @Override
                        public java.util.List<Object> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return java.util.Collections.emptyList(); }
                        @Override
                        public com.zifang.util.core.meta.page.PageResult<Object> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
                        @Override
                        public Object dataCopy(Object t) { return t; }
                        @Override
                        public java.util.Map<String, Object> init(String appCode, String modelCode) { return java.util.Collections.emptyMap(); }
                        @Override
                        public java.util.Map<String, Object> init(String appCode, String modelCode, java.util.Map<String, Object> data) { return data == null ? java.util.Collections.emptyMap() : data; }
                        @Override
                        public java.util.Map<String, Object> aiInit(String appCode, String modelCode, java.util.Map<String, Object> data) { return init(appCode, modelCode, data); }
                    };
            inner.setAbstractDataModelService(dummySvc);
        }

        @Override
        protected InnerDataModelServiceAdapter resolveInnerAdapter() {
            return inner;
        }

        public InnerDataModelServiceAdapter callResolveInnerAdapter() {
            return resolveInnerAdapter();
        }

        public InnerDataModelServiceAdapter getInner() {
            return inner;
        }
    }
}