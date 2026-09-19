package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * FormDataValidateService SPI 接口单元测试
 */
public class FormDataValidateServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataValidateService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataValidateService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单校验", m.name());
        assertEquals("FormDataValidateService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclareValidateMethod() throws NoSuchMethodException {
        Method m = FormDataValidateService.class.getMethod("validate",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void validateShouldReturnResultOfBoolean() throws NoSuchMethodException {
        Method m = FormDataValidateService.class.getMethod("validate",
                ExtensionServiceContext.class, Map.class);
        assertEquals(Result.class, m.getReturnType());
    }

    @Test
    public void anonymousImplShouldReturnSuccess() {
        FormDataValidateService impl = new FormDataValidateService() {
            @Override
            public Result<Boolean> validate(ExtensionServiceContext context, Map<String, Object> data) {
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.validate(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData());
    }

    @Test
    public void anonymousImplShouldReturnFailure() {
        FormDataValidateService impl = new FormDataValidateService() {
            @Override
            public Result<Boolean> validate(ExtensionServiceContext context, Map<String, Object> data) {
                return Result.fail("validation failed");
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.validate(ctx, new HashMap<>());
        assertFalse(r.isSuccess());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataValidateService.class.getPackage().getName());
    }
}