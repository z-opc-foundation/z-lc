package com.zifang.z.lc.sdk.abstracts;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.context.ExtensionServiceContextHolder;
import com.zifang.z.lc.sdk.context.ZLcBdpInvokerContext;
import com.zifang.z.lc.sdk.context.ZLcEngineInvokerContext;
import com.zifang.z.lc.sdk.define.DataModelService;
import com.zifang.z.lc.sdk.define.ModelDataSaveMode;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * AbstractDataModelService 抽象基类单元测试
 */
public class AbstractDataModelServiceTest {

    @After
    public void cleanup() {
        ExtensionServiceContextHolder.clear();
    }

    @Test
    public void shouldBeAbstractClass() {
        assertTrue("AbstractDataModelService 应当是 abstract",
                Modifier.isAbstract(AbstractDataModelService.class.getModifiers()));
    }

    @Test
    public void shouldImplementDataModelService() {
        assertTrue("应当实现 DataModelService 接口",
                DataModelService.class.isAssignableFrom(AbstractDataModelService.class));
    }

    @Test
    public void shouldHaveProtectedConstructor() throws NoSuchMethodException {
        java.lang.reflect.Constructor<?> ctor = AbstractDataModelService.class
                .getDeclaredConstructor(Object.class, Class.class);
        assertTrue("构造函数应当是 protected",
                Modifier.isProtected(ctor.getModifiers()));
    }

    @Test
    public void subclassCanBeConstructed() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        assertNotNull(svc);
        assertSame(TestPojo.class, svc.getGenericType());
    }

    @Test
    public void setGenericTypeShouldUpdateField() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        svc.setGenericType(String.class);
        assertEquals(String.class, svc.getGenericType());
    }

    @Test
    public void resolveAppAndModelShouldReadFromThreadLocal() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();
        ExtensionServiceContextHolder.set(ctx);

        String[] am = svc.resolveAppAndModel();
        assertEquals("crm", am[0]);
        assertEquals("customer", am[1]);
    }

    @Test
    public void resolveAppAndModelShouldFallBackToAnnotation() {
        TestModelService svc = new TestModelService(new Object(), AnnotatedPojo.class);
        ExtensionServiceContextHolder.clear();

        String[] am = svc.resolveAppAndModel();
        assertEquals("crm", am[0]);
        assertEquals("customer", am[1]);
    }

    @Test
    public void resolveAppAndModelShouldFallBackToClassNameLowercase() {
        TestModelService svc = new TestModelService(new Object(), PlainPojo.class);
        ExtensionServiceContextHolder.clear();

        String[] am = svc.resolveAppAndModel();
        assertEquals("", am[0]);
        assertEquals("plainpojo", am[1]);
    }

    @Test
    public void defaultSaveShouldThrowUnsupported() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        try {
            svc.save(new TestPojo());
            org.junit.Assert.fail("save 应抛 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not bound to executor"));
        }
    }

    @Test
    public void defaultSaveWithModeShouldDelegateToSave() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        try {
            svc.save(new TestPojo(), ModelDataSaveMode.SAVE_COMMON);
            org.junit.Assert.fail("save 应抛 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not bound to executor"));
        }
    }

    @Test
    public void defaultDeleteByIdShouldThrowUnsupported() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        try {
            svc.delete(1L);
            org.junit.Assert.fail("delete 应抛 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not bound to executor"));
        }
    }

    @Test
    public void defaultDeleteByEntityShouldThrowUnsupported() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        try {
            svc.delete(new TestPojo());
            org.junit.Assert.fail("delete(T) 应抛 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("requires reading 'id' field"));
        }
    }

    @Test
    public void defaultQueryByIdShouldThrowUnsupported() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        try {
            svc.queryById(1L, false);
            org.junit.Assert.fail("queryById 应抛 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("not bound to executor"));
        }
    }

    @Test
    public void defaultQueryListShouldReturnEmptyList() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        List<TestPojo> list = svc.queryList(new RuntimeQueryDTO(), false);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void defaultQueryPageableShouldReturnEmptyPage() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        RuntimeQueryDTO q = new RuntimeQueryDTO();
        q.setPage(2);
        q.setSize(50);
        PageResult<TestPojo> page = svc.queryPageable(q, false);
        assertNotNull(page);
        Long total = page.getTotal();
        assertEquals(Long.valueOf(0L), total);
        assertEquals(2, page.getPageNum());
        assertEquals(50, page.getPageSize());
    }

    @Test
    public void defaultQueryPageableShouldDefaultToPage1Size20() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        PageResult<TestPojo> page = svc.queryPageable(null, false);
        assertEquals(1, page.getPageNum());
        assertEquals(20, page.getPageSize());
    }

    @Test
    public void defaultDataCopyShouldReturnSelf() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        TestPojo p = new TestPojo();
        assertSame(p, svc.dataCopy(p));
    }

    @Test
    public void defaultInitShouldReturnEmptyMap() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        Map<String, Object> init = svc.init("crm", "customer");
        assertNotNull(init);
        assertTrue(init.isEmpty());
    }

    @Test
    public void defaultInitWithDataShouldReturnDataItself() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        Map<String, Object> data = new HashMap<>();
        data.put("k", "v");
        Map<String, Object> result = svc.init("crm", "customer", data);
        assertSame(data, result);
    }

    @Test
    public void defaultInitWithNullDataShouldReturnEmptyMap() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        Map<String, Object> result = svc.init("crm", "customer", null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void defaultAiInitShouldDelegateToInit() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> result = svc.aiInit("crm", "customer", data);
        // 默认实现走 init, init 返回 data 自身
        assertSame(data, result);
    }

    @Test
    public void isTempSaveShouldReturnTrueForTempMode() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        assertTrue(svc.callIsTempSave(ModelDataSaveMode.SAVE_TEMP));
    }

    @Test
    public void isTempSaveShouldReturnFalseForOtherModes() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        assertFalse(svc.callIsTempSave(ModelDataSaveMode.SAVE_COMMON));
        assertFalse(svc.callIsTempSave(ModelDataSaveMode.SAVE_IN_PROCESS));
        assertFalse(svc.callIsTempSave(null));
    }

    @Test
    public void isInProcessSaveShouldReturnTrueForInProcessMode() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        assertTrue(svc.callIsInProcessSave(ModelDataSaveMode.SAVE_IN_PROCESS));
    }

    @Test
    public void isInProcessSaveShouldReturnFalseForOtherModes() {
        TestModelService svc = new TestModelService(new Object(), TestPojo.class);
        assertFalse(svc.callIsInProcessSave(ModelDataSaveMode.SAVE_COMMON));
        assertFalse(svc.callIsInProcessSave(ModelDataSaveMode.SAVE_TEMP));
        assertFalse(svc.callIsInProcessSave(null));
    }

    @Test
    public void newCrudBodyShouldReturnCrudDTO() {
        TestModelService svc = new TestModelService(new Object(), AnnotatedPojo.class);
        Map<String, Object> values = new HashMap<>();
        values.put("k", "v");
        com.zifang.z.lc.common.dto.RuntimeCrudDTO body = svc.callNewCrudBody(values);
        assertNotNull(body);
        assertEquals("crm", body.getAppCode());
        assertEquals("customer", body.getEntityCode());
        assertSame(values, body.getFieldValues());
    }

    @Test
    public void newQueryShouldReturnQueryDTO() {
        TestModelService svc = new TestModelService(new Object(), AnnotatedPojo.class);
        RuntimeQueryDTO q = svc.callNewQuery();
        assertNotNull(q);
        assertEquals("crm", q.getAppCode());
        assertEquals("customer", q.getEntityCode());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.abstracts",
                AbstractDataModelService.class.getPackage().getName());
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
     * 暴露 protected 方法的桩实现.
     */
    public static class TestModelService extends AbstractDataModelService<TestPojo> {
        public TestModelService(Object crudExecutor, Class<?> genericType) {
            super(crudExecutor, genericType);
        }

        public String[] resolveAppAndModel() {
            return super.resolveAppAndModel();
        }

        public boolean callIsTempSave(Integer mode) {
            return isTempSave(mode);
        }

        public boolean callIsInProcessSave(Integer mode) {
            return isInProcessSave(mode);
        }

        public com.zifang.z.lc.common.dto.RuntimeCrudDTO callNewCrudBody(Map<String, Object> values) {
            return newCrudBody(values);
        }

        public RuntimeQueryDTO callNewQuery() {
            return newQuery();
        }
    }
}