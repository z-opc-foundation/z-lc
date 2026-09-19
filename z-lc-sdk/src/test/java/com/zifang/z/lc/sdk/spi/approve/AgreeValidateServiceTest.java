package com.zifang.z.lc.sdk.spi.approve;

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
 * AgreeValidateService SPI 接口单元测试
 */
public class AgreeValidateServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AgreeValidateService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = AgreeValidateService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("同意校验", m.name());
        assertEquals("AgreeValidateService", m.code());
        assertEquals("审批", m.group());
    }

    @Test
    public void shouldDeclareValidateAgreeMethod() throws NoSuchMethodException {
        Method m = AgreeValidateService.class.getMethod("validateAgree",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldReturnBoolean() {
        AgreeValidateService impl = new AgreeValidateService() {
            @Override
            public Result<Boolean> validateAgree(ExtensionServiceContext context, Map<String, Object> data) {
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.validateAgree(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData());
    }

    @Test
    public void anonymousImplShouldRejectOnAmountExceed() {
        AgreeValidateService impl = new AgreeValidateService() {
            @Override
            public Result<Boolean> validateAgree(ExtensionServiceContext context, Map<String, Object> data) {
                Object amount = data.get("amount");
                if (amount != null && ((Number) amount).doubleValue() > 10000) {
                    return Result.fail("金额超限");
                }
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 50000);
        Result<Boolean> r = impl.validateAgree(ctx, data);
        assertFalse(r.isSuccess());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.approve",
                AgreeValidateService.class.getPackage().getName());
    }
}