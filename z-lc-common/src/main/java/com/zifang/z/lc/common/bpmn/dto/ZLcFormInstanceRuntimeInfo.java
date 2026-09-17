package com.zifang.z.lc.common.bpmn.dto;

import java.util.Map;

/**
 * 表单实例运行时信息载体 — 蒸馏自 ace-platform-core
 * {@code FormInstanceRuntimeInfo} ({@code com.c2f.ace.core.bpmn.callable.dto}).
 *
 * <p>用于低代码平台"流程引擎"对外传递 — 把表单数据 + 流程数据 + 操作上下文打包
 * 成一个统一对象, 便于 MQ/WebSocket 一次性推送完整状态.
 *
 * @author zifang
 */
public class ZLcFormInstanceRuntimeInfo {

    private String eventType;
    private String operateType;
    private Boolean isAuto;
    private String refusedTarget;
    private ZLcFormData formData;
    private ZLcWorkflowData workflowData;
    private Long orgId;
    private Long campusId;
    private Long operatorId;
    private String taskDefinitionKey;
    private Map<String, Object> flowInstanceContext;
    private String comment;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getOperateType() { return operateType; }
    public void setOperateType(String operateType) { this.operateType = operateType; }

    public Boolean getIsAuto() { return isAuto; }
    public void setIsAuto(Boolean isAuto) { this.isAuto = isAuto; }

    public String getRefusedTarget() { return refusedTarget; }
    public void setRefusedTarget(String refusedTarget) { this.refusedTarget = refusedTarget; }

    public ZLcFormData getFormData() { return formData; }
    public void setFormData(ZLcFormData formData) { this.formData = formData; }

    public ZLcWorkflowData getWorkflowData() { return workflowData; }
    public void setWorkflowData(ZLcWorkflowData workflowData) { this.workflowData = workflowData; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }

    public Map<String, Object> getFlowInstanceContext() { return flowInstanceContext; }
    public void setFlowInstanceContext(Map<String, Object> flowInstanceContext) { this.flowInstanceContext = flowInstanceContext; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    /** 是否自动审批. */
    public boolean isAuto() {
        return Boolean.TRUE.equals(isAuto);
    }
}