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
 * FormDataInitService SPI 接口单元测试
 */
public class FormDataInitServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataInitService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataInitService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单初始化", m.name());
        assertEquals("FormDataInitService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclareInitMethod() throws NoSuchMethodException {
        Method m = FormDataInitService.class.getMethod("init",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
        assertTrue(m.getParameterTypes().length == 2);
    }

    @Test
    public void initShouldReturnResultOfMap() throws NoSuchMethodException {
        Method m = FormDataInitService.class.getMethod("init",
                ExtensionServiceContext.class, Map.class);
        assertEquals(Result.class, m.getReturnType());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        FormDataInitService impl = new FormDataInitService() {
            @Override
            public Result<Map<String, Object>> init(ExtensionServiceContext context, Map<String, Object> data) {
                return Result.success(data);
            }
        };
        assertNotNull(impl);
    }

    @Test
    public void anonymousImplInitShouldBeCallable() {
        FormDataInitService impl = new FormDataInitService() {
            @Override
            public Result<Map<String, Object>> init(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("k", "v");
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.init(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("v", r.getData().get("k"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataInitService.class.getPackage().getName());
    }
}