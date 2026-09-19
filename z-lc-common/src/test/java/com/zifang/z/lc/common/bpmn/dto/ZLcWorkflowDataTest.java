package com.zifang.z.lc.common.bpmn.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWorkflowData 单元测试
 *
 * @author zifang
 */
class ZLcWorkflowDataTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        assertThat(data).isNotNull();
    }

    @Test
    void shouldSetAndGetProcessInstanceId() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setProcessInstanceId("process-001");
        assertThat(data.getProcessInstanceId()).isEqualTo("process-001");
    }

    @Test
    void shouldSetAndGetProcessInstanceName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setProcessInstanceName("请假流程");
        assertThat(data.getProcessInstanceName()).isEqualTo("请假流程");
    }

    @Test
    void shouldSetAndGetProcessDefinitionKey() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setProcessDefinitionKey("leave_approval");
        assertThat(data.getProcessDefinitionKey()).isEqualTo("leave_approval");
    }

    @Test
    void shouldSetAndGetProcessDefinitionName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setProcessDefinitionName("请假审批流程");
        assertThat(data.getProcessDefinitionName()).isEqualTo("请假审批流程");
    }

    @Test
    void shouldSetAndGetInitiator() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setInitiator("user-001");
        assertThat(data.getInitiator()).isEqualTo("user-001");
    }

    @Test
    void shouldSetAndGetInitiatorName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setInitiatorName("张三");
        assertThat(data.getInitiatorName()).isEqualTo("张三");
    }

    @Test
    void shouldSetAndGetStartTime() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setStartTime("2024-01-01 10:00:00");
        assertThat(data.getStartTime()).isEqualTo("2024-01-01 10:00:00");
    }

    @Test
    void shouldSetAndGetEndTime() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setEndTime("2024-01-01 12:00:00");
        assertThat(data.getEndTime()).isEqualTo("2024-01-01 12:00:00");
    }

    @Test
    void shouldSetAndGetProcessInstanceStatus() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setProcessInstanceStatus("COMPLETED");
        assertThat(data.getProcessInstanceStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldSetAndGetCurrentTaskName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setCurrentTaskName("审批任务");
        assertThat(data.getCurrentTaskName()).isEqualTo("审批任务");
    }

    @Test
    void shouldSetAndGetAssignee() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setAssignee("admin");
        assertThat(data.getAssignee()).isEqualTo("admin");
    }

    @Test
    void shouldSetAndGetCandidateAssignees() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        List<String> candidates = Arrays.asList("user-001", "user-002");
        data.setCandidateAssignees(candidates);
        assertThat(data.getCandidateAssignees()).containsExactly("user-001", "user-002");
    }

    @Test
    void shouldSetAndGetAssigneeName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setAssigneeName("管理员");
        assertThat(data.getAssigneeName()).isEqualTo("管理员");
    }

    @Test
    void shouldSetAndGetTag() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        List<String> tags = Arrays.asList("紧急", "重要");
        data.setTag(tags);
        assertThat(data.getTag()).containsExactly("紧急", "重要");
    }

    @Test
    void shouldSetAndGetTaskId() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTaskId("task-001");
        assertThat(data.getTaskId()).isEqualTo("task-001");
    }

    @Test
    void shouldSetAndGetExecutionId() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setExecutionId("execution-001");
        assertThat(data.getExecutionId()).isEqualTo("execution-001");
    }

    @Test
    void shouldSetAndGetTaskStartedTime() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTaskStartedTime("2024-01-01 10:30:00");
        assertThat(data.getTaskStartedTime()).isEqualTo("2024-01-01 10:30:00");
    }

    @Test
    void shouldSetAndGetTaskFinishedTime() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTaskFinishedTime("2024-01-01 11:00:00");
        assertThat(data.getTaskFinishedTime()).isEqualTo("2024-01-01 11:00:00");
    }

    @Test
    void shouldSetAndGetDueDate() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setDueDate("2024-01-02");
        assertThat(data.getDueDate()).isEqualTo("2024-01-02");
    }

    @Test
    void shouldSetAndGetTaskDefinitionKey() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTaskDefinitionKey("approval_task");
        assertThat(data.getTaskDefinitionKey()).isEqualTo("approval_task");
    }

    @Test
    void shouldSetAndGetTaskDefinitionName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTaskDefinitionName("审批任务");
        assertThat(data.getTaskDefinitionName()).isEqualTo("审批任务");
    }

    @Test
    void shouldSetAndGetTargetTaskDefinitionKey() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTargetTaskDefinitionKey("manager_approval");
        assertThat(data.getTargetTaskDefinitionKey()).isEqualTo("manager_approval");
    }

    @Test
    void shouldSetAndGetTargetTaskDefinitionName() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setTargetTaskDefinitionName("经理审批");
        assertThat(data.getTargetTaskDefinitionName()).isEqualTo("经理审批");
    }

    @Test
    void shouldSetAndGetParentTaskId() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setParentTaskId("parent-task-001");
        assertThat(data.getParentTaskId()).isEqualTo("parent-task-001");
    }

    @Test
    void shouldSetAndGetAssigneeIds() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        List<String> assigneeIds = Arrays.asList("user-001", "user-002");
        data.setAssigneeIds(assigneeIds);
        assertThat(data.getAssigneeIds()).containsExactly("user-001", "user-002");
    }

    @Test
    void shouldSetAndGetBusinessKey() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        data.setBusinessKey("biz-001");
        assertThat(data.getBusinessKey()).isEqualTo("biz-001");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcWorkflowData data = new ZLcWorkflowData();
        assertThat(data.getProcessInstanceId()).isNull();
        assertThat(data.getProcessInstanceName()).isNull();
        assertThat(data.getProcessDefinitionKey()).isNull();
        assertThat(data.getProcessDefinitionName()).isNull();
        assertThat(data.getInitiator()).isNull();
        assertThat(data.getInitiatorName()).isNull();
        assertThat(data.getStartTime()).isNull();
        assertThat(data.getEndTime()).isNull();
        assertThat(data.getProcessInstanceStatus()).isNull();
        assertThat(data.getCurrentTaskName()).isNull();
        assertThat(data.getAssignee()).isNull();
        assertThat(data.getCandidateAssignees()).isNull();
        assertThat(data.getAssigneeName()).isNull();
        assertThat(data.getTag()).isNull();
        assertThat(data.getTaskId()).isNull();
        assertThat(data.getExecutionId()).isNull();
        assertThat(data.getTaskStartedTime()).isNull();
        assertThat(data.getTaskFinishedTime()).isNull();
        assertThat(data.getDueDate()).isNull();
        assertThat(data.getTaskDefinitionKey()).isNull();
        assertThat(data.getTaskDefinitionName()).isNull();
        assertThat(data.getTargetTaskDefinitionKey()).isNull();
        assertThat(data.getTargetTaskDefinitionName()).isNull();
        assertThat(data.getParentTaskId()).isNull();
        assertThat(data.getAssigneeIds()).isNull();
        assertThat(data.getBusinessKey()).isNull();
    }
}
