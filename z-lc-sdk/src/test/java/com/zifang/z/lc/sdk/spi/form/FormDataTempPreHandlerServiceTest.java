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
 * FormDataTempPreHandlerService SPI 接口单元测试
 */
public class FormDataTempPreHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataTempPreHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataTempPreHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单暂存前置处理", m.name());
        assertEquals("FormDataTempPreHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePreHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataTempPreHandlerService.class.getMethod("preHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldAddTimestamp() {
        FormDataTempPreHandlerService impl = new FormDataTempPreHandlerService() {
            @Override
            public Result<Map<String, Object>> preHandler(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("savedAt", System.currentTimeMillis());
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.preHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertNotNull(r.getData().get("savedAt"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataTempPreHandlerService.class.getPackage().getName());
    }
}