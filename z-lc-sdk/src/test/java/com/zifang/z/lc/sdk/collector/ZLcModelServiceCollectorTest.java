package com.zifang.z.lc.sdk.collector;

import com.zifang.z.lc.sdk.abstracts.AbstractDataModelService;
import com.zifang.z.lc.sdk.annotation.DataModel;
import com.zifang.z.lc.sdk.annotation.DataModelServiceInfo;
import com.zifang.z.lc.sdk.define.DataModelService;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ZLcModelServiceCollector 单元测试
 *
 * 使用 Spring 容器测试 bean 收集与索引功能.
 */
public class ZLcModelServiceCollectorTest {

    @Test
    public void shouldBePublicClass() {
        assertTrue(Modifier.isPublic(ZLcModelServiceCollector.class.getModifiers()));
    }

    @Test
    public void shouldHaveComponentAnnotation() {
        assertTrue(ZLcModelServiceCollector.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    public void shouldImplementApplicationContextAware() {
        assertTrue("应当实现 ApplicationContextAware",
                org.springframework.context.ApplicationContextAware.class
                        .isAssignableFrom(ZLcModelServiceCollector.class));
    }

    @Test
    public void shouldImplementInitializingBean() {
        assertTrue("应当实现 InitializingBean",
                org.springframework.beans.factory.InitializingBean.class
                        .isAssignableFrom(ZLcModelServiceCollector.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.collector",
                ZLcModelServiceCollector.class.getPackage().getName());
    }

    @Test
    public void shouldHaveByAppModelField() throws NoSuchFieldException {
        Field f = ZLcModelServiceCollector.class.getDeclaredField("byAppModel");
        assertNotNull(f);
        assertTrue(Modifier.isPrivate(f.getModifiers()));
        assertTrue(Modifier.isFinal(f.getModifiers()));
    }

    @Test
    public void freshCollectorShouldHaveEmptyRegistry() {
        ZLcModelServiceCollector c = new ZLcModelServiceCollector();
        try {
            java.lang.reflect.Method aim = ZLcModelServiceCollector.class.getMethod("aim",
                    String.class, String.class, java.util.Map.class);
            Object result = aim.invoke(c, "any", "any", java.util.Collections.emptyMap());
            assertNull(result);
        } catch (Exception e) {
            // 反射调用失败也算失败
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldRegisterAnnotatedBean() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, CustomerServiceImpl.class);
        ZLcModelServiceCollector c = ctx.getBean(ZLcModelServiceCollector.class);

        assertEquals(1, c.size());
        assertTrue(c.allKeys().contains("crm:customer"));
        assertNotNull(c.getByAppModel("crm", "customer"));
    }

    @Test
    public void shouldReturnNullForUnknownAppModel() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, CustomerServiceImpl.class);
        ZLcModelServiceCollector c = ctx.getBean(ZLcModelServiceCollector.class);

        assertNull(c.getByAppModel("unknown", "unknown"));
    }

    @Test
    public void shouldReturnAllRegisteredServices() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, CustomerServiceImpl.class);
        ZLcModelServiceCollector c = ctx.getBean(ZLcModelServiceCollector.class);

        assertEquals(1, c.all().size());
        assertTrue(c.all().get(0) instanceof CustomerServiceImpl);
    }

    @Test
    public void clearShouldRemoveAllRegistrations() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, CustomerServiceImpl.class);
        ZLcModelServiceCollector c = ctx.getBean(ZLcModelServiceCollector.class);
        assertEquals(1, c.size());

        c.clear();
        assertEquals(0, c.size());
        assertTrue(c.all().isEmpty());
    }

    @Test
    public void shouldThrowOnDuplicateRegistration() {
        try {
            new AnnotationConfigApplicationContext(
                    CollectorConfig.class,
                    CustomerServiceImpl.class,
                    DuplicateServiceImpl.class);
            org.junit.Assert.fail("重复注册应抛异常");
        } catch (IllegalStateException expected) {
            assertTrue("异常消息应包含重复 appCode/modelCode",
                    expected.getMessage().contains("Duplicate") ||
                            expected.getMessage().contains("crm:customer"));
        } catch (Exception other) {
            // Spring 启动失败也算合理
            Throwable cause = other.getCause();
            assertNotNull(cause);
        }
    }

    @Test
    public void shouldNotImplementDataModelService() {
        // ZLcModelServiceCollector 是 Collector 不是 Service, 不实现 DataModelService
        assertFalse("ZLcModelServiceCollector 不应实现 DataModelService",
                DataModelService.class.isAssignableFrom(ZLcModelServiceCollector.class));
    }

    // --- helpers ---

    @org.springframework.context.annotation.Configuration
    static class CollectorConfig {
        @org.springframework.context.annotation.Bean
        public ZLcModelServiceCollector collector() {
            return new ZLcModelServiceCollector();
        }
    }

    @DataModel(appCode = "crm", modelCode = "customer")
    public static class CustomerPojo {
    }

    @DataModelServiceInfo(appCode = "crm", modelCode = "customer")
    public static class CustomerServiceImpl extends AbstractDataModelService<CustomerPojo> {
        public CustomerServiceImpl() {
            super(new Object(), CustomerPojo.class);
        }

        @Override public Long save(CustomerPojo t) { return 1L; }
        @Override public Long save(CustomerPojo t, Integer mode) { return 1L; }
        @Override public void delete(Long id) {}
        @Override public void delete(CustomerPojo t) {}
        @Override public CustomerPojo queryById(Long pkId, boolean deep) { return null; }
        @Override public java.util.List<CustomerPojo> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return java.util.Collections.emptyList(); }
        @Override public com.zifang.util.core.meta.page.PageResult<CustomerPojo> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
        @Override public CustomerPojo dataCopy(CustomerPojo t) { return t; }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode) { return java.util.Collections.emptyMap(); }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
        @Override public java.util.Map<String, Object> aiInit(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
    }

    @DataModelServiceInfo(appCode = "crm", modelCode = "customer") // same key
    public static class DuplicateServiceImpl extends AbstractDataModelService<CustomerPojo> {
        public DuplicateServiceImpl() {
            super(new Object(), CustomerPojo.class);
        }

        @Override public Long save(CustomerPojo t) { return 1L; }
        @Override public Long save(CustomerPojo t, Integer mode) { return 1L; }
        @Override public void delete(Long id) {}
        @Override public void delete(CustomerPojo t) {}
        @Override public CustomerPojo queryById(Long pkId, boolean deep) { return null; }
        @Override public java.util.List<CustomerPojo> queryList(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return java.util.Collections.emptyList(); }
        @Override public com.zifang.util.core.meta.page.PageResult<CustomerPojo> queryPageable(com.zifang.z.lc.common.dto.RuntimeQueryDTO dto, boolean deep) { return null; }
        @Override public CustomerPojo dataCopy(CustomerPojo t) { return t; }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode) { return java.util.Collections.emptyMap(); }
        @Override public java.util.Map<String, Object> init(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
        @Override public java.util.Map<String, Object> aiInit(String appCode, String modelCode, java.util.Map<String, Object> data) { return data; }
    }
}