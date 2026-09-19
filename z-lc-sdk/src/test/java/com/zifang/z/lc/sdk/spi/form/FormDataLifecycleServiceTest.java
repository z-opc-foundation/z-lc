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
 * FormDataLifecycleService SPI 接口单元测试
 */
public class FormDataLifecycleServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FormDataLifecycleService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = FormDataLifecycleService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("表单生命周期管理", m.name());
        assertEquals("FormDataLifecycleService", m.code());
        assertEquals("表单", m.group());
    }

    @Test
    public void shouldDeclareOnLifecycleMethod() throws NoSuchMethodException {
        Method m = FormDataLifecycleService.class.getMethod("onLifecycle",
                ExtensionServiceContext.class, String.class, Map.class);
        assertNotNull(m);
        assertEquals(3, m.getParameterTypes().length);
    }

    @Test
    public void anonymousImplShouldReceiveEvent() {
        final String[] capturedEvent = {null};
        FormDataLifecycleService impl = new FormDataLifecycleService() {
            @Override
            public Result<Boolean> onLifecycle(ExtensionServiceContext context, String event, Map<String, Object> data) {
                capturedEvent[0] = event;
                return Result.success(true);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Boolean> r = impl.onLifecycle(ctx, "submit", new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("submit", capturedEvent[0]);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.form",
                FormDataLifecycleService.class.getPackage().getName());
    }
}