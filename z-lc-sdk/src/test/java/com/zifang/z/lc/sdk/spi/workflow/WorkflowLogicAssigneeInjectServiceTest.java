package com.zifang.z.lc.sdk.spi.workflow;

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
 * WorkflowLogicAssigneeInjectService SPI 接口单元测试
 */
public class WorkflowLogicAssigneeInjectServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(WorkflowLogicAssigneeInjectService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = WorkflowLogicAssigneeInjectService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("工作流审批人注入", m.name());
        assertEquals("WorkflowLogicAssigneeInjectService", m.code());
        assertEquals("工作流", m.group());
    }

    @Test
    public void shouldDeclareInjectAssigneesMethod() throws NoSuchMethodException {
        Method m = WorkflowLogicAssigneeInjectService.class.getMethod("injectAssignees",
                ExtensionServiceContext.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldInjectAssignees() {
        WorkflowLogicAssigneeInjectService impl = new WorkflowLogicAssigneeInjectService() {
            @Override
            public Result<List<String>> injectAssignees(ExtensionServiceContext context, Map<String, Object> nodeConfig) {
                List<String> result = new java.util.ArrayList<>();
                result.add("alice");
                result.add("bob");
                if (nodeConfig.containsKey("addReviewer")) {
                    result.add((String) nodeConfig.get("addReviewer"));
                }
                return Result.success(result);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("addReviewer", "charlie");
        Result<List<String>> r = impl.injectAssignees(ctx, nodeConfig);
        assertTrue(r.isSuccess());
        assertEquals(3, r.getData().size());
        assertTrue(r.getData().containsAll(Arrays.asList("alice", "bob", "charlie")));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.workflow",
                WorkflowLogicAssigneeInjectService.class.getPackage().getName());
    }
}