package com.zifang.z.lc.sdk.spi.model;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.sdk.annotation.InterfaceMapping;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AssignService SPI 接口单元测试
 */
public class AssignServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AssignService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = AssignService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("分派扩展服务", m.name());
        assertEquals("AssignService", m.code());
        assertEquals("模型", m.group());
    }

    @Test
    public void shouldDeclareAssignMethod() throws NoSuchMethodException {
        Method m = AssignService.class.getMethod("assign",
                ExtensionServiceContext.class, String.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldReturnAssignees() {
        AssignService impl = new AssignService() {
            @Override
            public Result<List<String>> assign(ExtensionServiceContext context, String action, Map<String, Object> data) {
                return Result.success(Arrays.asList("user1", "user2"));
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<List<String>> r = impl.assign(ctx, "submit", new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals(2, r.getData().size());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.model",
                AssignService.class.getPackage().getName());
    }
}