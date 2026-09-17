package com.zifang.z.lc.common.dto.task;

import java.io.Serializable;
import java.util.List;

/**
 * 任务信息 DTO — 蒸馏自 ace-platform-engine {@code TaskInfoDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：描述流程 / 表单任务的关键属性 — 任务 id、接收人列表、流程定义 key、
 * 当前任务名称、流程实例 id、上一步操作类型. {@link MessageDTO} 嵌套引用.
 *
 * @author xuhf (distilled by zifang)
 */
public class TaskInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 id（Flowable task id 或业务自定义 id）.
     */
    private String taskId;

    /**
     * 任务接收人列表（staffId 列表）.
     */
    private List<Long> assigneeList;

    /**
     * 任务定义 key（Flowable taskDefinitionKey）.
     */
    private String taskDefinitionKey;

    /**
     * 当前任务名称（Flowable task name）.
     */
    private String currentTaskName;

    /**
     * 流程实例 id.
     */
    private String processInstanceId;

    /**
     * 上一步操作类型（agree / reject / revoke / assign / delegate 等）.
     */
    private String operateType;

    public TaskInfoDTO() {
    }

    public TaskInfoDTO(String taskId, List<Long> assigneeList, String taskDefinitionKey,
                       String currentTaskName, String processInstanceId, String operateType) {
        this.taskId = taskId;
        this.assigneeList = assigneeList;
        this.taskDefinitionKey = taskDefinitionKey;
        this.currentTaskName = currentTaskName;
        this.processInstanceId = processInstanceId;
        this.operateType = operateType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<Long> getAssigneeList() {
        return assigneeList;
    }

    public void setAssigneeList(List<Long> assigneeList) {
        this.assigneeList = assigneeList;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getCurrentTaskName() {
        return currentTaskName;
    }

    public void setCurrentTaskName(String currentTaskName) {
        this.currentTaskName = currentTaskName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getOperateType() {
        return operateType;
    }

    public void setOperateType(String operateType) {
        this.operateType = operateType;
    }
}
