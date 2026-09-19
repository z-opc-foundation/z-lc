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
 * FormDataSubmitPostHandlerService SPI 接口单元测试
 */
public class FormDataSubmitPostHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataSubmitPostHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataSubmitPostHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单提交后置处理", m.name());
        assertEquals("FormDataSubmitPostHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePostHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataSubmitPostHandlerService.class.getMethod("postHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldRecordAudit() {
        FormDataSubmitPostHandlerService impl = new FormDataSubmitPostHandlerService() {
            @Override
            public Result<Map<String, Object>> postHandler(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("auditRecorded", true);
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.postHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData().get("auditRecorded"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataSubmitPostHandlerService.class.getPackage().getName());
    }
}