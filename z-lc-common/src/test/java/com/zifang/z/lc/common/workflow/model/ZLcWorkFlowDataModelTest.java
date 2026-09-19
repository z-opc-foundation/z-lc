package com.zifang.z.lc.common.workflow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWorkFlowDataModel 单元测试
 *
 * @author zifang
 */
class ZLcWorkFlowDataModelTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        assertThat(model).isNotNull();
    }

    @Test
    void shouldSetAndGetProcessInstanceId() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setProcessInstanceId("proc-001");
        assertThat(model.getProcessInstanceId()).isEqualTo("proc-001");
    }

    @Test
    void shouldSetAndGetProcessInstanceName() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setProcessInstanceName("请假流程");
        assertThat(model.getProcessInstanceName()).isEqualTo("请假流程");
    }

    @Test
    void shouldSetAndGetProcessDefinition() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setProcessDefinitionKey("leave-process");
        model.setProcessDefinitionName("请假流程模型");

        assertThat(model.getProcessDefinitionKey()).isEqualTo("leave-process");
        assertThat(model.getProcessDefinitionName()).isEqualTo("请假流程模型");
    }

    @Test
    void shouldSetAndGetInitiator() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setInitiator("user-001");
        model.setInitiatorName("张三");

        assertThat(model.getInitiator()).isEqualTo("user-001");
        assertThat(model.getInitiatorName()).isEqualTo("张三");
    }

    @Test
    void shouldSetAndGetTimes() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        model.setStartTime(start);
        model.setEndTime(end);

        assertThat(model.getStartTime()).isEqualTo(start);
        assertThat(model.getEndTime()).isEqualTo(end);
    }

    @Test
    void shouldSetAndGetProcessInstanceStatus() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setProcessInstanceStatus("RUNNING");
        assertThat(model.getProcessInstanceStatus()).isEqualTo("RUNNING");
    }

    @Test
    void shouldSetAndGetCurrentTaskInfo() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setCurrentTaskName("经理审批");
        model.setAssignee("user-002");
        model.setAssigneeName("李四");

        assertThat(model.getCurrentTaskName()).isEqualTo("经理审批");
        assertThat(model.getAssignee()).isEqualTo("user-002");
        assertThat(model.getAssigneeName()).isEqualTo("李四");
    }

    @Test
    void shouldSetAndGetTaskInfo() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setTaskId("task-001");
        model.setExecutionId("exec-001");
        model.setTaskDefinitionKey("manager_approval");
        model.setTaskDefinitionName("经理审批");

        assertThat(model.getTaskId()).isEqualTo("task-001");
        assertThat(model.getExecutionId()).isEqualTo("exec-001");
        assertThat(model.getTaskDefinitionKey()).isEqualTo("manager_approval");
        assertThat(model.getTaskDefinitionName()).isEqualTo("经理审批");
    }

    @Test
    void shouldSetAndGetTaskTimes() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        LocalDateTime taskStarted = LocalDateTime.now();
        LocalDateTime taskFinished = taskStarted.plusHours(1);
        LocalDateTime dueDate = taskStarted.plusDays(3);
        model.setTaskStartedTime(taskStarted);
        model.setTaskFinishedTime(taskFinished);
        model.setDueDate(dueDate);

        assertThat(model.getTaskStartedTime()).isEqualTo(taskStarted);
        assertThat(model.getTaskFinishedTime()).isEqualTo(taskFinished);
        assertThat(model.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void shouldSetAndGetWorkflowInstanceStatus() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        model.setWorkflowInstanceStatus("ACTIVE");
        assertThat(model.getWorkflowInstanceStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldSetAndGetTaskExtendInfo() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        Object extendInfo = new Object();
        model.setTaskExtendInfo(extendInfo);
        assertThat(model.getTaskExtendInfo()).isSameAs(extendInfo);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcWorkFlowDataModel model = new ZLcWorkFlowDataModel();
        assertThat(model.getProcessInstanceId()).isNull();
        assertThat(model.getInitiator()).isNull();
        assertThat(model.getStartTime()).isNull();
        assertThat(model.getTaskExtendInfo()).isNull();
    }
}