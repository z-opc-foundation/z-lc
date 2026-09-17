package com.zifang.z.lc.common.bpmn.dto;

import java.util.List;

/**
 * 流程数据 DTO — 蒸馏自 ace-platform-core
 * {@code WorkflowData} ({@code com.c2f.ace.core.bpmn.callable.dto}).
 *
 * <p>用于低代码平台"流程引擎"对外传递 — 流程实例 + 当前任务的统一载体.
 * 通常作为 MQ 消息 / WebSocket 推送的载荷部分.
 *
 * @author zifang
 */
public class ZLcWorkflowData {

    private String processInstanceId;
    private String processInstanceName;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String initiator;
    private String initiatorName;
    private String startTime;
    private String endTime;
    private String processInstanceStatus;
    private String currentTaskName;
    private String assignee;
    private List<String> candidateAssignees;
    private String assigneeName;
    private List<String> tag;
    private String taskId;
    private String executionId;
    private String taskStartedTime;
    private String taskFinishedTime;
    private String dueDate;
    private String taskDefinitionKey;
    private String taskDefinitionName;
    private String targetTaskDefinitionKey;
    private String targetTaskDefinitionName;
    private String parentTaskId;
    private List<String> assigneeIds;
    private String businessKey;

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

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getProcessInstanceStatus() { return processInstanceStatus; }
    public void setProcessInstanceStatus(String processInstanceStatus) { this.processInstanceStatus = processInstanceStatus; }

    public String getCurrentTaskName() { return currentTaskName; }
    public void setCurrentTaskName(String currentTaskName) { this.currentTaskName = currentTaskName; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public List<String> getCandidateAssignees() { return candidateAssignees; }
    public void setCandidateAssignees(List<String> candidateAssignees) { this.candidateAssignees = candidateAssignees; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public List<String> getTag() { return tag; }
    public void setTag(List<String> tag) { this.tag = tag; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getTaskStartedTime() { return taskStartedTime; }
    public void setTaskStartedTime(String taskStartedTime) { this.taskStartedTime = taskStartedTime; }

    public String getTaskFinishedTime() { return taskFinishedTime; }
    public void setTaskFinishedTime(String taskFinishedTime) { this.taskFinishedTime = taskFinishedTime; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }

    public String getTaskDefinitionName() { return taskDefinitionName; }
    public void setTaskDefinitionName(String taskDefinitionName) { this.taskDefinitionName = taskDefinitionName; }

    public String getTargetTaskDefinitionKey() { return targetTaskDefinitionKey; }
    public void setTargetTaskDefinitionKey(String targetTaskDefinitionKey) { this.targetTaskDefinitionKey = targetTaskDefinitionKey; }

    public String getTargetTaskDefinitionName() { return targetTaskDefinitionName; }
    public void setTargetTaskDefinitionName(String targetTaskDefinitionName) { this.targetTaskDefinitionName = targetTaskDefinitionName; }

    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }

    public List<String> getAssigneeIds() { return assigneeIds; }
    public void setAssigneeIds(List<String> assigneeIds) { this.assigneeIds = assigneeIds; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
}