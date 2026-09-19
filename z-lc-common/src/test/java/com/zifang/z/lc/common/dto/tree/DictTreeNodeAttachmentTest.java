package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DictTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class DictTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetDictName() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setDictName("性别");
        assertThat(attachment.getDictName()).isEqualTo("性别");
    }

    @Test
    void shouldSetAndGetDictCode() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setDictCode("GENDER");
        assertThat(attachment.getDictCode()).isEqualTo("GENDER");
    }

    @Test
    void shouldSetAndGetDictDesc() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setDictDesc("性别字典");
        assertThat(attachment.getDictDesc()).isEqualTo("性别字典");
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldSetAndGetExtend() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setExtend("{\"key\":\"value\"}");
        assertThat(attachment.getExtend()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void shouldHandleNullValues() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getDictName()).isNull();
        assertThat(attachment.getDictCode()).isNull();
        assertThat(attachment.getDictDesc()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
        assertThat(attachment.getExtend()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setDictName("");
        attachment.setDictCode("");
        attachment.setDictDesc("");
        attachment.setTreeNodeId("");
        attachment.setExtend("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getDictName()).isEmpty();
        assertThat(attachment.getDictCode()).isEmpty();
        assertThat(attachment.getDictDesc()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
        assertThat(attachment.getExtend()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DictTreeNodeAttachment attachment = new DictTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setDictName(null);
        attachment.setDictCode(null);
        attachment.setDictDesc(null);
        attachment.setTreeNodeId(null);
        attachment.setExtend(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getDictName()).isNull();
        assertThat(attachment.getDictCode()).isNull();
        assertThat(attachment.getDictDesc()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
        assertThat(attachment.getExtend()).isNull();
    }
}