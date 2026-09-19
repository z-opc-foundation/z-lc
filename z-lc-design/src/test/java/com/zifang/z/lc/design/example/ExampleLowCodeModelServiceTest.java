package com.zifang.z.lc.design.example;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.design.annotation.LowCodeModelService;
import com.zifang.z.lc.sdk.context.ExtensionServiceContextHolder;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import com.zifang.z.lc.sdk.spi.form.FormDataInitService;
import com.zifang.z.lc.sdk.spi.form.FormDataSubmitPreHandlerService;
import com.zifang.z.lc.sdk.spi.form.FormDataValidateService;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExampleLowCodeModelService 单元测试
 */
public class ExampleLowCodeModelServiceTest {

    @After
    public void cleanup() {
        ExtensionServiceContextHolder.clear();
    }

    @Test
    public void outerClassShouldBePublic() {
        assertTrue(Modifier.isPublic(ExampleLowCodeModelService.class.getModifiers()));
    }

    @Test
    public void threadLocalUsageDemoShouldSetAndClear() {
        ExampleLowCodeModelService.threadLocalUsageDemo();
        assertNull(ExtensionServiceContextHolder.get());
    }

    @Test
    public void threadLocalUsageDemoShouldBeStatic() throws NoSuchMethodException {
        assertTrue(Modifier.isStatic(
                ExampleLowCodeModelService.class.getMethod("threadLocalUsageDemo").getModifiers()));
    }

    @Test
    public void formDataInitServiceShouldInjectDefaults() {
        ExampleLowCodeModelService.ExampleFormDataInitService svc =
                new ExampleLowCodeModelService.ExampleFormDataInitService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Map<String, Object> data = new HashMap<>();
        Result<Map<String, Object>> r = svc.init(ctx, data);

        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
        assertTrue(r.getData().containsKey("createdBy"));
        assertTrue(r.getData().containsKey("createdAt"));
        assertEquals("z-lc-demo", r.getData().get("createdBy"));
    }

    @Test
    public void formDataInitServiceShouldHandleNullData() {
        ExampleLowCodeModelService.ExampleFormDataInitService svc =
                new ExampleLowCodeModelService.ExampleFormDataInitService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Result<Map<String, Object>> r = svc.init(ctx, null);
        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
    }

    @Test
    public void formDataInitServiceShouldHaveLowCodeModelAnnotation() {
        assertTrue(ExampleLowCodeModelService.ExampleFormDataInitService.class
                .isAnnotationPresent(LowCodeModelService.class));
    }

    @Test
    public void formDataInitServiceShouldImplementSPI() {
        assertTrue(FormDataInitService.class.isAssignableFrom(
                ExampleLowCodeModelService.ExampleFormDataInitService.class));
    }

    @Test
    public void formDataValidateServiceShouldPassWhenNameProvided() {
        ExampleLowCodeModelService.ExampleFormDataValidateService svc =
                new ExampleLowCodeModelService.ExampleFormDataValidateService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Alice");

        Result<Boolean> r = svc.validate(ctx, data);
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData());
    }

    @Test
    public void formDataValidateServiceShouldFailWhenNameMissing() {
        ExampleLowCodeModelService.ExampleFormDataValidateService svc =
                new ExampleLowCodeModelService.ExampleFormDataValidateService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Result<Boolean> r = svc.validate(ctx, new HashMap<>());
        assertFalse(r.isSuccess());
    }

    @Test
    public void formDataValidateServiceShouldFailWhenDataNull() {
        ExampleLowCodeModelService.ExampleFormDataValidateService svc =
                new ExampleLowCodeModelService.ExampleFormDataValidateService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Result<Boolean> r = svc.validate(ctx, null);
        assertFalse(r.isSuccess());
    }

    @Test
    public void formDataValidateServiceShouldFailWhenNameEmpty() {
        ExampleLowCodeModelService.ExampleFormDataValidateService svc =
                new ExampleLowCodeModelService.ExampleFormDataValidateService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "");

        Result<Boolean> r = svc.validate(ctx, data);
        assertFalse(r.isSuccess());
    }

    @Test
    public void formDataValidateServiceShouldHaveLowCodeModelAnnotation() {
        assertTrue(ExampleLowCodeModelService.ExampleFormDataValidateService.class
                .isAnnotationPresent(LowCodeModelService.class));
    }

    @Test
    public void formDataValidateServiceShouldImplementSPI() {
        assertTrue(FormDataValidateService.class.isAssignableFrom(
                ExampleLowCodeModelService.ExampleFormDataValidateService.class));
    }

    @Test
    public void formDataSubmitPreHandlerShouldAddSubmittedAt() {
        ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService svc =
                new ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Map<String, Object> data = new HashMap<>();
        Result<Map<String, Object>> r = svc.preHandler(ctx, data);
        assertTrue(r.isSuccess());
        assertTrue(r.getData().containsKey("submittedAt"));
    }

    @Test
    public void formDataSubmitPreHandlerShouldHandleNullData() {
        ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService svc =
                new ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService();

        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm").modelCode("customer").build();

        Result<Map<String, Object>> r = svc.preHandler(ctx, null);
        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
    }

    @Test
    public void formDataSubmitPreHandlerShouldHaveLowCodeModelAnnotation() {
        assertTrue(ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService.class
                .isAnnotationPresent(LowCodeModelService.class));
    }

    @Test
    public void formDataSubmitPreHandlerShouldImplementSPI() {
        assertTrue(FormDataSubmitPreHandlerService.class.isAssignableFrom(
                ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService.class));
    }

    @Test
    public void allInnerClassesShouldBeStatic() {
        assertTrue(Modifier.isStatic(
                ExampleLowCodeModelService.ExampleFormDataInitService.class.getModifiers()));
        assertTrue(Modifier.isStatic(
                ExampleLowCodeModelService.ExampleFormDataValidateService.class.getModifiers()));
        assertTrue(Modifier.isStatic(
                ExampleLowCodeModelService.ExampleFormDataSubmitPreHandlerService.class.getModifiers()));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.design.example",
                ExampleLowCodeModelService.class.getPackage().getName());
    }
}