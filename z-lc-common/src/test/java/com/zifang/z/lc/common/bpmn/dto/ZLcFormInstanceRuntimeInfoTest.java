package com.zifang.z.lc.common.bpmn.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFormInstanceRuntimeInfo 单元测试
 *
 * @author zifang
 */
class ZLcFormInstanceRuntimeInfoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        assertThat(info).isNotNull();
    }

    @Test
    void shouldSetAndGetEventType() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setEventType("TASK_CREATED");
        assertThat(info.getEventType()).isEqualTo("TASK_CREATED");
    }

    @Test
    void shouldSetAndGetOperateType() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setOperateType("APPROVE");
        assertThat(info.getOperateType()).isEqualTo("APPROVE");
    }

    @Test
    void shouldSetAndGetIsAuto() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setIsAuto(true);
        assertThat(info.getIsAuto()).isTrue();
    }

    @Test
    void shouldIsAutoReturnTrueWhenIsAutoTrue() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setIsAuto(true);
        assertThat(info.isAuto()).isTrue();
    }

    @Test
    void shouldIsAutoReturnFalseWhenIsAutoFalse() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setIsAuto(false);
        assertThat(info.isAuto()).isFalse();
    }

    @Test
    void shouldIsAutoReturnFalseWhenIsAutoNull() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        assertThat(info.isAuto()).isFalse();
    }

    @Test
    void shouldSetAndGetRefusedTarget() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setRefusedTarget("user-001");
        assertThat(info.getRefusedTarget()).isEqualTo("user-001");
    }

    @Test
    void shouldSetAndGetFormData() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        ZLcFormData formData = new ZLcFormData();
        formData.setAppCode("app-001");
        info.setFormData(formData);
        assertThat(info.getFormData()).isNotNull();
        assertThat(info.getFormData().getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetWorkflowData() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        ZLcWorkflowData workflowData = new ZLcWorkflowData();
        workflowData.setProcessInstanceId("process-001");
        info.setWorkflowData(workflowData);
        assertThat(info.getWorkflowData()).isNotNull();
        assertThat(info.getWorkflowData().getProcessInstanceId()).isEqualTo("process-001");
    }

    @Test
    void shouldSetAndGetOrgId() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setOrgId(100L);
        assertThat(info.getOrgId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetCampusId() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setCampusId(200L);
        assertThat(info.getCampusId()).isEqualTo(200L);
    }

    @Test
    void shouldSetAndGetOperatorId() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setOperatorId(300L);
        assertThat(info.getOperatorId()).isEqualTo(300L);
    }

    @Test
    void shouldSetAndGetTaskDefinitionKey() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setTaskDefinitionKey("task-key-001");
        assertThat(info.getTaskDefinitionKey()).isEqualTo("task-key-001");
    }

    @Test
    void shouldSetAndGetFlowInstanceContext() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        Map<String, Object> context = new HashMap<>();
        context.put("key1", "value1");
        info.setFlowInstanceContext(context);
        assertThat(info.getFlowInstanceContext()).containsEntry("key1", "value1");
    }

    @Test
    void shouldSetAndGetComment() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        info.setComment("同意");
        assertThat(info.getComment()).isEqualTo("同意");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcFormInstanceRuntimeInfo info = new ZLcFormInstanceRuntimeInfo();
        assertThat(info.getEventType()).isNull();
        assertThat(info.getOperateType()).isNull();
        assertThat(info.getIsAuto()).isNull();
        assertThat(info.getRefusedTarget()).isNull();
        assertThat(info.getFormData()).isNull();
        assertThat(info.getWorkflowData()).isNull();
        assertThat(info.getOrgId()).isNull();
        assertThat(info.getCampusId()).isNull();
        assertThat(info.getOperatorId()).isNull();
        assertThat(info.getTaskDefinitionKey()).isNull();
        assertThat(info.getFlowInstanceContext()).isNull();
        assertThat(info.getComment()).isNull();
    }
}
