package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataModelTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class DataModelTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelId() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setModelId(123L);
        assertThat(attachment.getModelId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetModelCode() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setModelCode("model-001");
        assertThat(attachment.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetModelDesc() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setModelDesc("模型描述");
        assertThat(attachment.getModelDesc()).isEqualTo("模型描述");
    }

    @Test
    void shouldSetAndGetModelName() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setModelName("模型名称");
        assertThat(attachment.getModelName()).isEqualTo("模型名称");
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldSetAndGetPhysicalFlag() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setPhysicalFlag(1);
        assertThat(attachment.getPhysicalFlag()).isEqualTo(1);
    }

    @Test
    void shouldSetAndGetExtend() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        Map<String, Object> extend = new HashMap<>();
        extend.put("key1", "value1");
        extend.put("key2", 123);
        attachment.setExtend(extend);
        assertThat(attachment.getExtend()).isEqualTo(extend);
    }

    @Test
    void shouldHandleNullValues() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelId()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getModelDesc()).isNull();
        assertThat(attachment.getModelName()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
        assertThat(attachment.getPhysicalFlag()).isNull();
        assertThat(attachment.getExtend()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setModelCode("");
        attachment.setModelDesc("");
        attachment.setModelName("");
        attachment.setTreeNodeId("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getModelCode()).isEmpty();
        assertThat(attachment.getModelDesc()).isEmpty();
        assertThat(attachment.getModelName()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setModelCode(null);
        attachment.setModelDesc(null);
        attachment.setModelName(null);
        attachment.setTreeNodeId(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getModelDesc()).isNull();
        assertThat(attachment.getModelName()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldSetEmptyExtend() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setExtend(new HashMap<>());
        assertThat(attachment.getExtend()).isEmpty();
    }

    @Test
    void shouldSetNullExtend() {
        DataModelTreeNodeAttachment attachment = new DataModelTreeNodeAttachment();
        attachment.setExtend(null);
        assertThat(attachment.getExtend()).isNull();
    }
}