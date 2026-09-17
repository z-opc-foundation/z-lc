package com.zifang.z.lc.core.domain;

/**
 * 流程模板 DO — 蒸馏自 ace-platform-core {@code WorkflowTemplateDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code workflow_template} 表 — 流程设计态 + 运行态的持久化对象.
 *
 * <p>设计态 vs 运行态：
 * <ul>
 *   <li>{@link #workflowDesignDefinition} — 设计态 JSON（前端流程设计器生成）</li>
 *   <li>{@link #workflowRuntimeDefinition} — 运行态 BPMN XML（Flowable 部署用）</li>
 * </ul>
 *
 * @author zifang
 */
public class WorkflowTemplateDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 关联模型 code.
     */
    private String modelCode;

    /**
     * 流程模板 key（Flowable BPMN model key — 业务唯一）.
     */
    private String workflowDefinitionKey;

    /**
     * 流程模板 id（Flowable deployment id）.
     */
    private String workflowDefinitionId;

    /**
     * 流程名称.
     */
    private String workflowName;

    /**
     * 流程描述.
     */
    private String workflowDesc;

    /**
     * 流程设计态 JSON（前端流程设计器生成）.
     */
    private String workflowDesignDefinition;

    /**
     * 流程运行态 BPMN XML（Flowable 部署用）.
     */
    private String workflowRuntimeDefinition;

    /**
     * 流程配置 JSON（节点审批人 / 表单绑定 / 通知规则等）.
     */
    private String workflowConfig;

    /**
     * 流程状态（0 未发布 / 1 已发布）.
     */
    private Integer status;

    /**
     * 流程树节点 code.
     */
    private String workflowTreeNodeCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowDesc() {
        return workflowDesc;
    }

    public void setWorkflowDesc(String workflowDesc) {
        this.workflowDesc = workflowDesc;
    }

    public String getWorkflowDesignDefinition() {
        return workflowDesignDefinition;
    }

    public void setWorkflowDesignDefinition(String workflowDesignDefinition) {
        this.workflowDesignDefinition = workflowDesignDefinition;
    }

    public String getWorkflowRuntimeDefinition() {
        return workflowRuntimeDefinition;
    }

    public void setWorkflowRuntimeDefinition(String workflowRuntimeDefinition) {
        this.workflowRuntimeDefinition = workflowRuntimeDefinition;
    }

    public String getWorkflowConfig() {
        return workflowConfig;
    }

    public void setWorkflowConfig(String workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getWorkflowTreeNodeCode() {
        return workflowTreeNodeCode;
    }

    public void setWorkflowTreeNodeCode(String workflowTreeNodeCode) {
        this.workflowTreeNodeCode = workflowTreeNodeCode;
    }

    public boolean isPublished() {
        return status != null && status == 1;
    }
}
