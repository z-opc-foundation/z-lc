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
 * TaskService SPI 接口单元测试
 */
public class TaskServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(TaskService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = TaskService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("任务扩展服务", m.name());
        assertEquals("TaskService", m.code());
        assertEquals("模型", m.group());
    }

    @Test
    public void shouldDeclareHandlerMethod() throws NoSuchMethodException {
        Method m = TaskService.class.getMethod("handler",
                ExtensionServiceContext.class, String.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldModifyTask() {
        TaskService impl = new TaskService() {
            @Override
            public Result<Map<String, Object>> handler(ExtensionServiceContext context, String action, Map<String, Object> data) {
                if (data == null) data = new HashMap<>();
                data.put("taskStatus", "ACTIVE");
                return Result.success(data);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Result<Map<String, Object>> r = impl.handler(ctx, "create", new HashMap<>());
        assertTrue(r.isSuccess());
        assertEquals("ACTIVE", r.getData().get("taskStatus"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.model",
                TaskService.class.getPackage().getName());
    }
}