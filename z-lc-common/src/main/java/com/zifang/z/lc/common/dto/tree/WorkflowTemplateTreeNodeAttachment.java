package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * 流程模板树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code WorkflowTemplateTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「流程设计器」树节点的附加信息 — workflowName / workflowDesc /
 * workflowDefinitionKey（Flowable BPMN 模型 key）.
 *
 * @author zifang
 */
public class WorkflowTemplateTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 流程名称.
     */
    private String workflowName;

    /**
     * 流程描述.
     */
    private String workflowDesc;

    /**
     * 流程模板标识（Flowable BPMN model key）.
     */
    private String workflowDefinitionKey;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
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

    public String getWorkflowDefinitionKey() {
        return workflowDefinitionKey;
    }

    public void setWorkflowDefinitionKey(String workflowDefinitionKey) {
        this.workflowDefinitionKey = workflowDefinitionKey;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }
}
