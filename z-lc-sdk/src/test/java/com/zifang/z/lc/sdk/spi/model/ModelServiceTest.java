package com.zifang.z.lc.sdk.spi.model;

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
 * ModelService SPI 接口单元测试
 */
public class ModelServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ModelService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = ModelService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("模型增删改查扩展", m.name());
        assertEquals("ModelService", m.code());
        assertEquals("模型", m.group());
    }

    @Test
    public void shouldDeclareHandlerMethod() throws NoSuchMethodException {
        Method m = ModelService.class.getMethod("handler",
                ExtensionServiceContext.class, String.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldProcessAction() {
        final String[] capturedAction = {null};
        ModelService impl = new ModelService() {
            @Override
            public Result<Object> handler(ExtensionServiceContext context, String action, Map<String, Object> data) {
                capturedAction[0] = action;
                return Result.success("ok");
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Object> r = impl.handler(ctx, "save", new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("save", capturedAction[0]);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.model",
                ModelService.class.getPackage().getName());
    }
}