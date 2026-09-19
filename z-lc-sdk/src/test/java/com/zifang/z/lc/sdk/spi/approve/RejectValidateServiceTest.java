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
 * RejectValidateService SPI 接口单元测试
 */
public class RejectValidateServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(RejectValidateService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = RejectValidateService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("驳回校验", m.name());
        assertEquals("RejectValidateService", m.code());
        assertEquals("审批", m.group());
    }

    @Test
    public void shouldDeclareValidateRejectMethod() throws NoSuchMethodException {
        Method m = RejectValidateService.class.getMethod("validateReject",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldPassOnNonTerminal() {
        RejectValidateService impl = new RejectValidateService() {
            @Override
            public Result<Boolean> validateReject(ExtensionServiceContext context, Map<String, Object> data) {
                String state = (String) data.get("state");
                if ("TERMINAL".equals(state)) {
                    return Result.fail("已是终态, 无法驳回");
                }
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> data = new HashMap<>();
        data.put("state", "ACTIVE");
        Result<Boolean> r = impl.validateReject(ctx, data);
        assertTrue(r.isSuccess());
    }

    @Test
    public void anonymousImplShouldRejectOnTerminal() {
        RejectValidateService impl = new RejectValidateService() {
            @Override
            public Result<Boolean> validateReject(ExtensionServiceContext context, Map<String, Object> data) {
                String state = (String) data.get("state");
                if ("TERMINAL".equals(state)) {
                    return Result.fail("已是终态, 无法驳回");
                }
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> data = new HashMap<>();
        data.put("state", "TERMINAL");
        Result<Boolean> r = impl.validateReject(ctx, data);
        assertFalse(r.isSuccess());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.approve",
                RejectValidateService.class.getPackage().getName());
    }
}