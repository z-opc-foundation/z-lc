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
 * FormDataQueryPreHandlerService SPI 接口单元测试
 */
public class FormDataQueryPreHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataQueryPreHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataQueryPreHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单查询前置处理", m.name());
        assertEquals("FormDataQueryPreHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePreHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataQueryPreHandlerService.class.getMethod("preHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldAddTenantFilter() {
        FormDataQueryPreHandlerService impl = new FormDataQueryPreHandlerService() {
            @Override
            public Result<Map<String, Object>> preHandler(ExtensionServiceContext context, Map<String, Object> query) {
                if (query == null) query = new HashMap<>();
                query.put("tenantCode", context.getAppCode());
                return Result.success(query);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("tnt-001").build();
        Result<Map<String, Object>> r = impl.preHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("tnt-001", r.getData().get("tenantCode"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataQueryPreHandlerService.class.getPackage().getName());
    }
}