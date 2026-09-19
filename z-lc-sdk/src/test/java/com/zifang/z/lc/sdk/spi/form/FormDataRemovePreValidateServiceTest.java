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
 * FormDataRemovePreValidateService SPI 接口单元测试
 */
public class FormDataRemovePreValidateServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataRemovePreValidateService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataRemovePreValidateService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单删除前置校验", m.name());
        assertEquals("FormDataRemovePreValidateService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePreValidateMethod() throws NoSuchMethodException {
        Method m = FormDataRemovePreValidateService.class.getMethod("preValidate",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldBlockDeleteIfReferenced() {
        FormDataRemovePreValidateService impl = new FormDataRemovePreValidateService() {
            @Override
            public Result<Boolean> preValidate(ExtensionServiceContext context, Map<String, Object> data) {
                if (data != null && Boolean.TRUE.equals(data.get("referenced"))) {
                    return Result.fail("存在引用, 无法删除");
                }
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> data = new HashMap<>();
        data.put("referenced", true);
        Result<Boolean> r = impl.preValidate(ctx, data);
        assertFalse(r.isSuccess());
    }

    @Test
    public void anonymousImplShouldAllowUnreferencedDelete() {
        FormDataRemovePreValidateService impl = new FormDataRemovePreValidateService() {
            @Override
            public Result<Boolean> preValidate(ExtensionServiceContext context, Map<String, Object> data) {
                if (data != null && Boolean.TRUE.equals(data.get("referenced"))) {
                    return Result.fail("存在引用, 无法删除");
                }
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.preValidate(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataRemovePreValidateService.class.getPackage().getName());
    }
}