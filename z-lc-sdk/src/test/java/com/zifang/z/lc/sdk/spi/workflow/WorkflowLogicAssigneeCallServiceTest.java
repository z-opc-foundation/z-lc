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
 * WorkflowLogicAssigneeCallService SPI 接口单元测试
 */
public class WorkflowLogicAssigneeCallServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(WorkflowLogicAssigneeCallService.class.isInterface());
    }

    @Test
    public void shouldHaveInterfaceMappingAnnotation() {
        InterfaceMapping m = WorkflowLogicAssigneeCallService.class.getAnnotation(InterfaceMapping.class);
        assertNotNull(m);
        assertEquals("工作流审批人回调", m.name());
        assertEquals("WorkflowLogicAssigneeCallService", m.code());
        assertEquals("工作流", m.group());
    }

    @Test
    public void shouldDeclareCallAssigneesMethod() throws NoSuchMethodException {
        Method m = WorkflowLogicAssigneeCallService.class.getMethod("callAssignees",
                ExtensionServiceContext.class, List.class, Map.class);
        assertNotNull(m);
    }

    @Test
    public void anonymousImplShouldAddDelegate() {
        WorkflowLogicAssigneeCallService impl = new WorkflowLogicAssigneeCallService() {
            @Override
            public Result<List<String>> callAssignees(ExtensionServiceContext context, List<String> defaultAssignees, Map<String, Object> nodeConfig) {
                // 默认加一个 delegate (转办)
                java.util.List<String> result = new java.util.ArrayList<>(defaultAssignees);
                if (nodeConfig.containsKey("delegateTo")) {
                    result.add((String) nodeConfig.get("delegateTo"));
                }
                return Result.success(result);
            }
        };
        ExtensionServiceContext ctx = ExtensionServiceContext.builder().appCode("a").build();
        Map<String, Object> nodeConfig = new HashMap<>();
        nodeConfig.put("delegateTo", "alice");
        Result<List<String>> r = impl.callAssignees(ctx, Arrays.asList("bob"), nodeConfig);
        assertTrue(r.isSuccess());
        assertEquals(2, r.getData().size());
        assertTrue(r.getData().contains("alice"));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.workflow",
                WorkflowLogicAssigneeCallService.class.getPackage().getName());
    }
}