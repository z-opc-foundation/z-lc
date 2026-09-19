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
 * FormDataTempPostHandlerService SPI 接口单元测试
 */
public class FormDataTempPostHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataTempPostHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataTempPostHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单暂存后置处理", m.name());
        assertEquals("FormDataTempPostHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePostHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataTempPostHandlerService.class.getMethod("postHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldSetDraftId() {
        FormDataTempPostHandlerService impl = new FormDataTempPostHandlerService() {
            @Override
            public Result<Map<String, Object>> postHandler(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("draftId", "D-" + System.currentTimeMillis());
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.postHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertNotNull(r.getData().get("draftId"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataTempPostHandlerService.class.getPackage().getName());
    }
}