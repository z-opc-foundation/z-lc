package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class TreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setModelCode("model-001");
        assertThat(attachment.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetPageType() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setPageType("list");
        assertThat(attachment.getPageType()).isEqualTo("list");
    }

    @Test
    void shouldSetAndGetPageCode() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setPageCode("page-001");
        assertThat(attachment.getPageCode()).isEqualTo("page-001");
    }

    @Test
    void shouldSetAndGetPageName() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setPageName("页面名称");
        assertThat(attachment.getPageName()).isEqualTo("页面名称");
    }

    @Test
    void shouldSetAndGetPageDesc() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setPageDesc("页面描述");
        assertThat(attachment.getPageDesc()).isEqualTo("页面描述");
    }

    @Test
    void shouldSetAndGetWorkflowDefinitionKey() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setWorkflowDefinitionKey("workflow-key-123");
        assertThat(attachment.getWorkflowDefinitionKey()).isEqualTo("workflow-key-123");
    }

    @Test
    void shouldSetAndGetTagCode() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setTagCode("tag-001");
        assertThat(attachment.getTagCode()).isEqualTo("tag-001");
    }

    @Test
    void shouldHandleNullValues() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getPageType()).isNull();
        assertThat(attachment.getPageCode()).isNull();
        assertThat(attachment.getPageName()).isNull();
        assertThat(attachment.getPageDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getTagCode()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setModelCode("");
        attachment.setPageType("");
        attachment.setPageCode("");
        attachment.setPageName("");
        attachment.setPageDesc("");
        attachment.setWorkflowDefinitionKey("");
        attachment.setTagCode("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getModelCode()).isEmpty();
        assertThat(attachment.getPageType()).isEmpty();
        assertThat(attachment.getPageCode()).isEmpty();
        assertThat(attachment.getPageName()).isEmpty();
        assertThat(attachment.getPageDesc()).isEmpty();
        assertThat(attachment.getWorkflowDefinitionKey()).isEmpty();
        assertThat(attachment.getTagCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        TreeNodeAttachment attachment = new TreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setModelCode(null);
        attachment.setPageType(null);
        attachment.setPageCode(null);
        attachment.setPageName(null);
        attachment.setPageDesc(null);
        attachment.setWorkflowDefinitionKey(null);
        attachment.setTagCode(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getPageType()).isNull();
        assertThat(attachment.getPageCode()).isNull();
        assertThat(attachment.getPageName()).isNull();
        assertThat(attachment.getPageDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getTagCode()).isNull();
    }
}