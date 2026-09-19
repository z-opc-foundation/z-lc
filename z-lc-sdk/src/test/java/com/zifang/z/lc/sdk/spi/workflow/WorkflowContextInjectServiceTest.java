package com.zifang.z.lc.sdk.spi.workflow;

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
 * WorkflowContextInjectService SPI 接口单元测试
 */
public class WorkflowContextInjectServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(WorkflowContextInjectService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = WorkflowContextInjectService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("工作流上下文注入", m.name());
        assertEquals("WorkflowContextInjectService", m.code());
        assertEquals("工作流", m.group());
    }

    @Test
    public void shouldDeclareInjectMethod() throws NoSuchMethodException {
        Method m = WorkflowContextInjectService.class.getMethod("inject",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldInjectVariables() {
        WorkflowContextInjectService impl = new WorkflowContextInjectService() {
            @Override
            public Result<Map<String, Object>> inject(ExtensionServiceContext context, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("var1", "value1");
                data.put("var2", 42);
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.inject(ctx, new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("value1", r.getData().get("var1"));
        assertEquals(42, r.getData().get("var2"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.workflow",
                WorkflowContextInjectService.class.getPackage().getName());
    }
}