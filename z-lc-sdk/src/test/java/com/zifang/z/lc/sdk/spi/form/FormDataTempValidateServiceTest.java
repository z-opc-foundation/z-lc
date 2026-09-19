package com.zifang.z.lc.sdk.spi.form;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * FormDataTempValidateService SPI 接口单元测试
 */
public class FormDataTempValidateServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataTempValidateService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataTempValidateService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单暂存校验", m.name());
        assertEquals("FormDataTempValidateService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclareValidateMethod() throws NoSuchMethodException {
        Method m = FormDataTempValidateService.class.getMethod("validate",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldAcceptDraft() {
        FormDataTempValidateService impl = new FormDataTempValidateService() {
            @Override
            public Result<Boolean> validate(ExtensionServiceContext context, Map<String, Object> data) {
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.validate(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataTempValidateService.class.getPackage().getName());
    }
}