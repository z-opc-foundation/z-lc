package com.zifang.z.lc.sdk.reflect;

import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import com.zifang.z.lc.sdk.annotation.DataModel;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * LcReflectHelper 反射工具类单元测试
 */
public class LcReflectHelperTest {

    @Test
    public void shouldBeFinalClass() {
        assertTrue("LcReflectHelper 应当是 final",
                Modifier.isFinal(LcReflectHelper.class.getModifiers()));
    }

    @Test
    public void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<LcReflectHelper> ctor = LcReflectHelper.class.getDeclaredConstructor();
        assertTrue("构造函数应当是 private",
                Modifier.isPrivate(ctor.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.reflect",
                LcReflectHelper.class.getPackage().getName());
    }

    @Test
    public void resolveGenericTypeShouldReturnNullForNullService() {
        assertNull(LcReflectHelper.resolveGenericType(null));
    }

    @Test
    public void resolveGenericTypeFromClassShouldReturnNullForNull() {
        assertNull(LcReflectHelper.resolveGenericTypeFromClass(null));
    }

    @Test
    public void resolveGenericTypeFromClassShouldReturnNullForObjectClass() {
        assertNull(LcReflectHelper.resolveGenericTypeFromClass(Object.class));
    }

    @Test
    public void resolveGenericTypeShouldReturnDirectGeneric() {
        TestDataModelService svc = new TestDataModelService(new Object(), TestPojo.class);
        Class<?> t = LcReflectHelper.resolveGenericType(svc);
        assertEquals(TestPojo.class, t);
    }

    @Test
    public void resolveGenericTypeShouldWalkUpHierarchy() {
        DerivedService svc = new DerivedService();
        Class<?> t = LcReflectHelper.resolveGenericType(svc);
        assertEquals(TestPojo.class, t);
    }

    @Test
    public void checkAnnotatedWithDataModelShouldPassForAnnotatedClass() {
        LcReflectHelper.checkAnnotatedWithDataModel(AnnotatedPojo.class);
    }

    @Test
    public void checkAnnotatedWithDataModelShouldThrowForNull() {
        try {
            LcReflectHelper.checkAnnotatedWithDataModel(null);
            fail("null genericType 应抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("is null"));
        }
    }

    @Test
    public void checkAnnotatedWithDataModelShouldThrowForUnannotatedClass() {
        try {
            LcReflectHelper.checkAnnotatedWithDataModel(PlainPojo.class);
            fail("未标注 @DataModel 应抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("@DataModel"));
        }
    }

    @Test
    public void extractDataModelAnnotationShouldReturnAnnotation() {
        DataModel dm = LcReflectHelper.extractDataModelAnnotation(AnnotatedPojo.class);
        assertNotNull(dm);
        assertEquals("crm", dm.appCode());
        assertEquals("customer", dm.modelCode());
    }

    @Test
    public void extractDataModelAnnotationShouldReturnNullForNull() {
        assertNull(LcReflectHelper.extractDataModelAnnotation(null));
    }

    @Test
    public void extractDataModelAnnotationShouldReturnNullForUnannotated() {
        assertNull(LcReflectHelper.extractDataModelAnnotation(PlainPojo.class));
    }

    @Test
    public void resolveAppAndModelShouldReturnPair() {
        String[] am = LcReflectHelper.resolveAppAndModel(AnnotatedPojo.class);
        assertNotNull(am);
        assertEquals(2, am.length);
        assertEquals("crm", am[0]);
        assertEquals("customer", am[1]);
    }

    @Test
    public void resolveAppAndModelShouldThrowForUnannotated() {
        try {
            LcReflectHelper.resolveAppAndModel(PlainPojo.class);
            fail("未标注 @DataModel 应抛 IllegalStateException");
        } catch (IllegalStateException expected) {
            // OK
        }
    }

    @Test
    public void shouldHaveStaticMethods() throws NoSuchMethodException {
        java.lang.reflect.Method resolve = LcReflectHelper.class.getMethod("resolveGenericType",
                com.zifang.z.lc.sdk.define.DataModelService.class);
        assertTrue(Modifier.isStatic(resolve.getModifiers()));
    }

    // --- helpers ---

    public static class TestPojo {
    }

    @DataModel(appCode = "crm", modelCode = "customer")
    public static class AnnotatedPojo {
    }

    public static class PlainPojo {
    }

    /**
     * 抽象基类的子类 — 泛型继承场景.
     */
    public static class TestDataModelService extends AbstractDataModelService<TestPojo> {
        public TestDataModelService(Object crudExecutor, Class<?> genericType) {
            super(crudExecutor, genericType);
        }

        @Override public Long save(TestPojo t) { return 1L; }
        @Override public Long save(TestPojo t, Integer mode) { return 1L; }
        @Override public void delete(Long id) {}
        @Override public void delete(TestPojo t) {}
        @Override public TestPojo queryById(Long pkId, boolean deep) { return null; }
        @Override public java.util.List<TestPojo> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return java.util.Collections.emptyList(); }
        @Override public com.zifang.util.core.meta.page.PageResult<TestPojo> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
        @Override public TestPojo dataCopy(TestPojo t) { return t; }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode) { return java.util.Collections.emptyMap(); }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
        @Override public java.util.Map<String, Object> aiInit(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
    }

    /**
     * 中间类 - 保留泛型 — 用于多层继承测试.
     */
    public static class BaseService<T> extends AbstractDataModelService<T> {
        public BaseService() {
            super(new Object(), (Class<?>) null);
        }

        @Override public Long save(T t) { return 1L; }
        @Override public Long save(T t, Integer mode) { return 1L; }
        @Override public void delete(Long id) {}
        @Override public void delete(T t) {}
        @Override public T queryById(Long pkId, boolean deep) { return null; }
        @Override public java.util.List<T> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return java.util.Collections.emptyList(); }
        @Override public com.zifang.util.core.meta.page.PageResult<T> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
        @Override public T dataCopy(T t) { return t; }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode) { return java.util.Collections.emptyMap(); }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
        @Override public java.util.Map<String, Object> aiInit(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
    }

    /**
     * 多层继承的最终子类 - BaseService<TestPojo> -> AbstractDataModelService<TestPojo>.
     */
    public static class DerivedService extends BaseService<TestPojo> {
        // 不覆写, 测试向上递归
    }
}