package com.zifang.z.lc.design.collector;

import com.zifang.z.lc.design.annotation.LowCodeModelService;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * LowCodeModelServiceCollector 单元测试
 *
 * @author zifang
 */
public class LowCodeModelServiceCollectorTest {

    @Test
    public void shouldCreateWithEmptyRegistry() {
        LowCodeModelServiceCollector collector = new LowCodeModelServiceCollector();
        assertEquals(0, collector.size());
    }

    @Test
    public void shouldReturnNullForUnknownLookup() {
        LowCodeModelServiceCollector collector = new LowCodeModelServiceCollector();
        assertNull(collector.pickByGroupAndCode("unknown", "code"));
        assertNull(collector.pickByType(String.class));
    }

    @Test
    public void shouldReturnEmptyListForAll() {
        LowCodeModelServiceCollector collector = new LowCodeModelServiceCollector();
        List<Object> all = collector.all();
        assertNotNull(all);
        assertEquals(0, all.size());
    }

    @Test
    public void shouldRegisterBeansWithAnnotation() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, FormInitServiceImpl.class, DataInitServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        assertEquals(2, collector.size());
    }

    @Test
    public void shouldIndexByGroupAndCode() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, FormInitServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        Object bean = collector.pickByGroupAndCode("form", "init");
        assertNotNull(bean);
        assertTrue(bean instanceof FormInitServiceImpl);
    }

    @Test
    public void shouldIndexByType() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, FormInitServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        FormInitServiceImpl bean = collector.pickByType(FormInitServiceImpl.class);
        assertNotNull(bean);
        assertTrue(bean instanceof FormInitServiceImpl);
    }

    @Test
    public void shouldListAllRegisteredBeans() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, FormInitServiceImpl.class, DataInitServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        List<Object> all = collector.all();
        assertEquals(2, all.size());
    }

    @Test
    public void shouldNotIndexBeanWithoutInterfaceMapping() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, NoMappingServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        // Bean is registered (by type) but not indexed by group/code
        assertEquals(1, collector.size());
        assertNull(collector.pickByGroupAndCode("any", "any"));
    }

    @Test
    public void shouldPickByTypeReturnsSameInstance() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                CollectorConfig.class, FormInitServiceImpl.class);
        LowCodeModelServiceCollector collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);
        FormInitServiceImpl bean1 = collector.pickByType(FormInitServiceImpl.class);
        FormInitServiceImpl bean2 = collector.pickByType(FormInitServiceImpl.class);
        assertSame(bean1, bean2);
    }

    // --- Test helpers ---

    @org.springframework.context.annotation.Configuration
    static class CollectorConfig {
        @org.springframework.context.annotation.Bean
        public LowCodeModelServiceCollector collector() {
            return new LowCodeModelServiceCollector();
        }
    }

    @InterfaceMapping(group = "form", code = "init")
    public interface FormInitService {
    }

    @InterfaceMapping(group = "data", code = "init")
    public interface DataInitService {
    }

    @LowCodeModelService
    public static class FormInitServiceImpl implements FormInitService {
    }

    @LowCodeModelService
    public static class DataInitServiceImpl implements DataInitService {
    }

    @LowCodeModelService
    public static class NoMappingServiceImpl {
    }
}