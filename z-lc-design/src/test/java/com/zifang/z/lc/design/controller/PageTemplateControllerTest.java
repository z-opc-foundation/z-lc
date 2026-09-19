package com.zifang.z.lc.design.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.design.collector.LowCodeModelServiceCollector;
import com.zifang.z.lc.design.dto.ServiceListResponse;
import com.zifang.z.lc.design.model.PageTemplate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PageTemplateController 单元测试
 *
 * 通过 Spring 容器创建真实的 LowCodeModelServiceCollector 来测试 controller.
 */
public class PageTemplateControllerTest {

    private PageTemplateController controller;
    private LowCodeModelServiceCollector collector;

    @Before
    public void setUp() throws Exception {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                TestConfig.class, FormInitServiceImpl.class);
        collector = ctx.getBean("collector", LowCodeModelServiceCollector.class);

        controller = new PageTemplateController();
        java.lang.reflect.Field f = PageTemplateController.class.getDeclaredField("collector");
        f.setAccessible(true);
        f.set(controller, collector);
    }

    @Test
    public void shouldReturnSuccessResultFromListServices() {
        Result<ServiceListResponse> r = controller.listServices();
        assertNotNull(r);
        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
        Integer total = Integer.valueOf(r.getData().getTotal());
        assertEquals(Integer.valueOf(1), total);
        Integer svcSize = Integer.valueOf(r.getData().getServices().size());
        assertEquals(Integer.valueOf(1), svcSize);
    }

    @Test
    public void shouldReturnServicesWithInterfaceMappingMetadata() {
        Result<ServiceListResponse> r = controller.listServices();
        ServiceListResponse.ServiceEntry entry = r.getData().getServices().get(0);
        assertTrue(entry.getClazz().contains("FormInitServiceImpl"));
        assertEquals("test-group", entry.getGroup());
        assertEquals("test-code", entry.getCode());
        assertEquals("Test Service", entry.getName());
    }

    @Test
    public void shouldReturnNotFoundForUnknownPick() {
        Result<Map<String, String>> r = controller.pickService("unknown-group", "unknown-code");
        assertTrue(r.isSuccess());
        assertEquals("not_found", r.getData().get("status"));
    }

    @Test
    public void shouldReturnFoundForKnownPick() {
        Result<Map<String, String>> r = controller.pickService("test-group", "test-code");
        assertTrue(r.isSuccess());
        assertEquals("found", r.getData().get("status"));
        assertEquals(FormInitServiceImpl.class.getName(), r.getData().get("class"));
    }

    @Test
    public void shouldReturnNotFoundForPartialMatch() {
        Result<Map<String, String>> r = controller.pickService("test-group", "wrong-code");
        assertEquals("not_found", r.getData().get("status"));
    }

    @Test
    public void sampleTemplateShouldCreateFormPage() {
        Result<PageTemplate> r = controller.sampleTemplate("crm", "customer");
        assertTrue(r.isSuccess());
        PageTemplate t = r.getData();
        assertNotNull(t);
        assertEquals("crm", t.getAppCode());
        assertEquals("customer", t.getModelCode());
        assertEquals("form", t.getPageType());
        assertEquals("未命名页面", t.getPageName());
        Integer status = Integer.valueOf(t.getStatus());
        assertEquals(Integer.valueOf(0), status);
        assertNotNull(t.getViewJson());
    }

    @Test
    public void sampleTemplateShouldHandleDifferentAppCodes() {
        Result<PageTemplate> r1 = controller.sampleTemplate("app1", "model1");
        Result<PageTemplate> r2 = controller.sampleTemplate("app2", "model2");
        assertEquals("app1", r1.getData().getAppCode());
        assertEquals("model1", r1.getData().getModelCode());
        assertEquals("app2", r2.getData().getAppCode());
        assertEquals("model2", r2.getData().getModelCode());
    }

    @Test
    public void shouldHaveRequestMappingAnnotation() {
        org.springframework.web.bind.annotation.RequestMapping rm =
                PageTemplateController.class.getAnnotation(
                        org.springframework.web.bind.annotation.RequestMapping.class);
        assertNotNull(rm);
        boolean found = false;
        for (String v : rm.value()) {
            if ("/api/lc/design".equals(v)) {
                found = true;
                break;
            }
        }
        assertTrue("应当映射到 /api/lc/design", found);
    }

    @Test
    public void shouldHaveRestControllerAnnotation() {
        assertTrue(PageTemplateController.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestController.class));
    }

    @Test
    public void shouldHaveTagAnnotation() {
        assertTrue(PageTemplateController.class.isAnnotationPresent(
                io.swagger.v3.oas.annotations.tags.Tag.class));
    }

    @Test
    public void listServicesShouldBePublic() throws NoSuchMethodException {
        assertTrue(java.lang.reflect.Modifier.isPublic(
                PageTemplateController.class.getMethod("listServices").getModifiers()));
    }

    @Test
    public void pickServiceShouldBePublic() throws NoSuchMethodException {
        assertTrue(java.lang.reflect.Modifier.isPublic(
                PageTemplateController.class.getMethod("pickService", String.class, String.class)
                        .getModifiers()));
    }

    @Test
    public void sampleTemplateShouldBePublic() throws NoSuchMethodException {
        assertTrue(java.lang.reflect.Modifier.isPublic(
                PageTemplateController.class.getMethod("sampleTemplate", String.class, String.class)
                        .getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.design.controller",
                PageTemplateController.class.getPackage().getName());
    }

    @Test
    public void shouldNotBeAbstract() {
        assertFalse(java.lang.reflect.Modifier.isAbstract(
                PageTemplateController.class.getModifiers()));
    }

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public LowCodeModelServiceCollector collector() {
            return new LowCodeModelServiceCollector();
        }
    }

    @com.zifang.z.lc.sdk.annotation.InterfaceMapping(
            group = "test-group", code = "test-code", name = "Test Service")
    public interface FormInitService {
    }

    @com.zifang.z.lc.design.annotation.LowCodeModelService
    public static class FormInitServiceImpl implements FormInitService {
    }
}