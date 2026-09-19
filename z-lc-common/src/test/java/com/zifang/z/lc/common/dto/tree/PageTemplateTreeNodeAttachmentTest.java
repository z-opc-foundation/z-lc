package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageTemplateTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class PageTemplateTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setModelCode("model-001");
        assertThat(attachment.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetPageType() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setPageType("list");
        assertThat(attachment.getPageType()).isEqualTo("list");
    }

    @Test
    void shouldSetAndGetPageCode() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setPageCode("page-001");
        assertThat(attachment.getPageCode()).isEqualTo("page-001");
    }

    @Test
    void shouldSetAndGetPageName() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setPageName("页面名称");
        assertThat(attachment.getPageName()).isEqualTo("页面名称");
    }

    @Test
    void shouldSetAndGetPageDesc() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setPageDesc("页面描述");
        assertThat(attachment.getPageDesc()).isEqualTo("页面描述");
    }

    @Test
    void shouldSetAndGetWorkflowDefinitionKey() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setWorkflowDefinitionKey("workflow-key-123");
        assertThat(attachment.getWorkflowDefinitionKey()).isEqualTo("workflow-key-123");
    }

    @Test
    void shouldSetAndGetCustomTags() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        List<String> customTags = Arrays.asList("tag1", "tag2");
        attachment.setCustomTags(customTags);
        assertThat(attachment.getCustomTags()).isEqualTo(customTags);
    }

    @Test
    void shouldSetAndGetImageUrl() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setImageUrl("https://example.com/icon.png");
        assertThat(attachment.getImageUrl()).isEqualTo("https://example.com/icon.png");
    }

    @Test
    void shouldSetAndGetCreateTime() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        LocalDateTime createTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        attachment.setCreateTime(createTime);
        assertThat(attachment.getCreateTime()).isEqualTo(createTime);
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getPageType()).isNull();
        assertThat(attachment.getPageCode()).isNull();
        assertThat(attachment.getPageName()).isNull();
        assertThat(attachment.getPageDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getCustomTags()).isNull();
        assertThat(attachment.getImageUrl()).isNull();
        assertThat(attachment.getCreateTime()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setModelCode("");
        attachment.setPageType("");
        attachment.setPageCode("");
        attachment.setPageName("");
        attachment.setPageDesc("");
        attachment.setWorkflowDefinitionKey("");
        attachment.setImageUrl("");
        attachment.setTreeNodeId("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getModelCode()).isEmpty();
        assertThat(attachment.getPageType()).isEmpty();
        assertThat(attachment.getPageCode()).isEmpty();
        assertThat(attachment.getPageName()).isEmpty();
        assertThat(attachment.getPageDesc()).isEmpty();
        assertThat(attachment.getWorkflowDefinitionKey()).isEmpty();
        assertThat(attachment.getImageUrl()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setModelCode(null);
        attachment.setPageType(null);
        attachment.setPageCode(null);
        attachment.setPageName(null);
        attachment.setPageDesc(null);
        attachment.setWorkflowDefinitionKey(null);
        attachment.setImageUrl(null);
        attachment.setTreeNodeId(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getPageType()).isNull();
        assertThat(attachment.getPageCode()).isNull();
        assertThat(attachment.getPageName()).isNull();
        assertThat(attachment.getPageDesc()).isNull();
        assertThat(attachment.getWorkflowDefinitionKey()).isNull();
        assertThat(attachment.getImageUrl()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldSetEmptyCustomTags() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setCustomTags(Arrays.asList());
        assertThat(attachment.getCustomTags()).isEmpty();
    }

    @Test
    void shouldSetNullCustomTags() {
        PageTemplateTreeNodeAttachment attachment = new PageTemplateTreeNodeAttachment();
        attachment.setCustomTags(null);
        assertThat(attachment.getCustomTags()).isNull();
    }
}