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
 * FormDataSubmitPreHandlerService SPI 接口单元测试
 */
public class FormDataSubmitPreHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataSubmitPreHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataSubmitPreHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单提交前置处理", m.name());
        assertEquals("FormDataSubmitPreHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePreHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataSubmitPreHandlerService.class.getMethod("preHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldModifyData() {
        FormDataSubmitPreHandlerService impl = new FormDataSubmitPreHandlerService() {
            @Override
            public Result<Map<String, Object>> preHandler(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("ts", 123L);
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.preHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(123L, r.getData().get("ts"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataSubmitPreHandlerService.class.getPackage().getName());
    }
}