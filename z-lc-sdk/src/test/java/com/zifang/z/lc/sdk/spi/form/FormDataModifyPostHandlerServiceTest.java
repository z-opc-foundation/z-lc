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
 * FormDataModifyPostHandlerService SPI 接口单元测试
 */
public class FormDataModifyPostHandlerServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataModifyPostHandlerService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataModifyPostHandlerService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单修改后置处理", m.name());
        assertEquals("FormDataModifyPostHandlerService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclarePostHandlerMethod() throws NoSuchMethodException {
        Method m = FormDataModifyPostHandlerService.class.getMethod("postHandler",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldMarkModified() {
        FormDataModifyPostHandlerService impl = new FormDataModifyPostHandlerService() {
            @Override
            public Result<Map<String, Object>> postHandler(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("modified", true);
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.postHandler(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(Boolean.TRUE, r.getData().get("modified"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataModifyPostHandlerService.class.getPackage().getName());
    }
}