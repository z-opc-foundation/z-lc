package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;
import java.util.Map;

/**
 * 签名数据查询 DTO — 蒸馏自 ace-platform-engine {@code AssignDataExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：CA 服务查询「待签名数据」时携带的入参 — 流程实例 id + 任务 id + 表单数据.
 *
 * @author zifang
 */
public class AssignDataExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程实例 id. */
    private String workflowInstanceId;

    /** 任务 id. */
    private String taskId;

    /** 表单实例数据. */
    private Map<String, Object> formData;

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }
}
