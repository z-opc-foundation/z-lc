package com.zifang.z.lc.common.workflow.model;

import java.time.LocalDateTime;

/**
 * 工作流数据模型 DTO — 蒸馏自 ace-platform-core
 * {@code WorkFlowDataModel} ({@code com.c2f.ace.core.extenssion.model.define}).
 *
 * <p>用于低代码平台"流程引擎"对外消息 (MQ / WebSocket / 待办中心 等) 的统一载体.
 * 描述一个工作流实例 / 当前任务的关键信息, 用于跨系统传递.
 *
 * <p>蒸馏说明：ace 原版用 fastjson {@code @JSONField} 做字段重命名,
 * 蒸馏版移除 fastjson 依赖, 字段名直接用 camelCase.
 * 若需 snake_case 序列化, 业务方自行加 Jackson {@code @JsonProperty} 或全局配置.
 *
 * @author zifang
 */
public class ZLcWorkFlowDataModel {

    /** 工作流实例 ID. */
    private String processInstanceId;

    /** 工作流实例名称. */
    private String processInstanceName;

    /** 工作流模型标识. */
    private String processDefinitionKey;

    /** 工作流模型名称. */
    private String processDefinitionName;

    /** 实例创建人. */
    private String initiator;

    /** 实例创建人名字. */
    private String initiatorName;

    /** 实例发起时间. */
    private LocalDateTime startTime;

    /** 实例完成时间. */
    private LocalDateTime endTime;

    /** 工作流实例状态. */
    private String processInstanceStatus;

    /** 当前任务名称. */
    private String currentTaskName;

    /** 执行人 ID. */
    private String assignee;

    /** 执行人名字. */
    private String assigneeName;

    /** 任务 ID. */
    private String taskId;

    /** 执行 ID. */
    private String executionId;

    /** 任务发起时间. */
    private LocalDateTime taskStartedTime;

    /** 任务完成时间. */
    private LocalDateTime taskFinishedTime;

    /** 预定的任务到期时间. */
    private LocalDateTime dueDate;

    /** BPMN 中该节点的 ID. */
    private String taskDefinitionKey;

    /** BPMN 中的节点名称. */
    private String taskDefinitionName;

    /** 流程实例状态. */
    private String workflowInstanceStatus;

    /** 业务任务扩展信息 (业务方自定义). */
    private Object taskExtendInfo;

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getProcessInstanceName() { return processInstanceName; }
    public void setProcessInstanceName(String processInstanceName) { this.processInstanceName = processInstanceName; }

    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }

    public String getProcessDefinitionName() { return processDefinitionName; }
    public void setProcessDefinitionName(String processDefinitionName) { this.processDefinitionName = processDefinitionName; }

    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }

    public String getInitiatorName() { return initiatorName; }
    public void setInitiatorName(String initiatorName) { this.initiatorName = initiatorName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getProcessInstanceStatus() { return processInstanceStatus; }
    public void setProcessInstanceStatus(String processInstanceStatus) { this.processInstanceStatus = processInstanceStatus; }

    public String getCurrentTaskName() { return currentTaskName; }
    public void setCurrentTaskName(String currentTaskName) { this.currentTaskName = currentTaskName; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public LocalDateTime getTaskStartedTime() { return taskStartedTime; }
    public void setTaskStartedTime(LocalDateTime taskStartedTime) { this.taskStartedTime = taskStartedTime; }

    public LocalDateTime getTaskFinishedTime() { return taskFinishedTime; }
    public void setTaskFinishedTime(LocalDateTime taskFinishedTime) { this.taskFinishedTime = taskFinishedTime; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }

    public String getTaskDefinitionName() { return taskDefinitionName; }
    public void setTaskDefinitionName(String taskDefinitionName) { this.taskDefinitionName = taskDefinitionName; }

    public String getWorkflowInstanceStatus() { return workflowInstanceStatus; }
    public void setWorkflowInstanceStatus(String workflowInstanceStatus) { this.workflowInstanceStatus = workflowInstanceStatus; }

    public Object getTaskExtendInfo() { return taskExtendInfo; }
    public void setTaskExtendInfo(Object taskExtendInfo) { this.taskExtendInfo = taskExtendInfo; }
}