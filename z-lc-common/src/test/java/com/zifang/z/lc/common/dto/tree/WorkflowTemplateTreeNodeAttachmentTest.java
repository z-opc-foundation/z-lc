package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowTemplateTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class WorkflowTemplateTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetWorkflowName() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setWorkflowName("审批流程");
        assertThat(attachment.getWorkflowName()).isEqualTo("审批流程");
    }

    @Test
    void shouldSetAndGetWorkflowDesc() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setWorkflowDesc("流程描述");
        assertThat(attachment.getWorkflowDesc()).isEqualTo("流程描述");
    }

    @Test
    void shouldSetAndGetWorkflowDefinitionKey() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setWorkflowDefinitionKey("workflow-key-123");
        assertThat(attachment.getWorkflowDefinitionKey()).isEqualTo("workflow-key-123");
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getWorkflowName()).isNull();
        assertThat(attachment.getWorkflowDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setWorkflowName("");
        attachment.setWorkflowDesc("");
        attachment.setWorkflowDefinitionKey("");
        attachment.setTreeNodeId("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getWorkflowName()).isEmpty();
        assertThat(attachment.getWorkflowDesc()).isEmpty();
        assertThat(attachment.getWorkflowDefinitionKey()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        WorkflowTemplateTreeNodeAttachment attachment = new WorkflowTemplateTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setWorkflowName(null);
        attachment.setWorkflowDesc(null);
        attachment.setWorkflowDefinitionKey(null);
        attachment.setTreeNodeId(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getWorkflowName()).isNull();
        assertThat(attachment.getWorkflowDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }
}