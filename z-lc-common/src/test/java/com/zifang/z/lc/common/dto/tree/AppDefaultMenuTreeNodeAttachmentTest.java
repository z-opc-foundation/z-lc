package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppDefaultMenuTreeNodeAttachment 单元测试
 *
 * @author zifang
 */
class AppDefaultMenuTreeNodeAttachmentTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        assertThat(attachment).isNotNull();
    }

    @Test
    void shouldSetAndGetAppCode() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setAppCode("app-001");
        assertThat(attachment.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetModelCode() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setModelCode("model-001");
        assertThat(attachment.getModelCode()).isEqualTo("model-001");
    }

    @Test
    void shouldSetAndGetMenuPath() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setMenuPath("/menu/path");
        assertThat(attachment.getMenuPath()).isEqualTo("/menu/path");
    }

    @Test
    void shouldSetAndGetMenuName() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setMenuName("菜单名称");
        assertThat(attachment.getMenuName()).isEqualTo("菜单名称");
    }

    @Test
    void shouldSetAndGetMenuIcon() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setMenuIcon("icon-home");
        assertThat(attachment.getMenuIcon()).isEqualTo("icon-home");
    }

    @Test
    void shouldSetAndGetChildren() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        AppDefaultMenuTreeNodeAttachment child1 = new AppDefaultMenuTreeNodeAttachment();
        child1.setMenuName("子菜单1");
        AppDefaultMenuTreeNodeAttachment child2 = new AppDefaultMenuTreeNodeAttachment();
        child2.setMenuName("子菜单2");
        
        List<AppDefaultMenuTreeNodeAttachment> children = Arrays.asList(child1, child2);
        attachment.setChildren(children);
        
        assertThat(attachment.getChildren()).hasSize(2);
        assertThat(attachment.getChildren().get(0).getMenuName()).isEqualTo("子菜单1");
        assertThat(attachment.getChildren().get(1).getMenuName()).isEqualTo("子菜单2");
    }

    @Test
    void shouldSetAndGetTreeNodeId() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setTreeNodeId("tree-node-123");
        assertThat(attachment.getTreeNodeId()).isEqualTo("tree-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getMenuPath()).isNull();
        assertThat(attachment.getMenuName()).isNull();
        assertThat(attachment.getMenuIcon()).isNull();
        assertThat(attachment.getChildren()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        assertThat(attachment).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSupportRecursiveStructure() {
        AppDefaultMenuTreeNodeAttachment root = new AppDefaultMenuTreeNodeAttachment();
        root.setMenuName("根菜单");
        root.setTreeNodeId("root");
        
        AppDefaultMenuTreeNodeAttachment child = new AppDefaultMenuTreeNodeAttachment();
        child.setMenuName("子菜单");
        child.setTreeNodeId("child");
        
        AppDefaultMenuTreeNodeAttachment grandchild = new AppDefaultMenuTreeNodeAttachment();
        grandchild.setMenuName("孙子菜单");
        grandchild.setTreeNodeId("grandchild");
        
        child.setChildren(Arrays.asList(grandchild));
        root.setChildren(Arrays.asList(child));
        
        assertThat(root.getChildren()).hasSize(1);
        assertThat(root.getChildren().get(0).getChildren()).hasSize(1);
        assertThat(root.getChildren().get(0).getChildren().get(0).getMenuName()).isEqualTo("孙子菜单");
    }

    @Test
    void shouldSetEmptyStrings() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setAppCode("");
        attachment.setModelCode("");
        attachment.setMenuPath("");
        attachment.setMenuName("");
        attachment.setMenuIcon("");
        attachment.setTreeNodeId("");
        
        assertThat(attachment.getAppCode()).isEmpty();
        assertThat(attachment.getModelCode()).isEmpty();
        assertThat(attachment.getMenuPath()).isEmpty();
        assertThat(attachment.getMenuName()).isEmpty();
        assertThat(attachment.getMenuIcon()).isEmpty();
        assertThat(attachment.getTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setAppCode(null);
        attachment.setModelCode(null);
        attachment.setMenuPath(null);
        attachment.setMenuName(null);
        attachment.setMenuIcon(null);
        attachment.setTreeNodeId(null);
        
        assertThat(attachment.getAppCode()).isNull();
        assertThat(attachment.getModelCode()).isNull();
        assertThat(attachment.getMenuPath()).isNull();
        assertThat(attachment.getMenuName()).isNull();
        assertThat(attachment.getMenuIcon()).isNull();
        assertThat(attachment.getTreeNodeId()).isNull();
    }

    @Test
    void shouldSetEmptyChildren() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setChildren(Arrays.asList());
        assertThat(attachment.getChildren()).isEmpty();
    }

    @Test
    void shouldSetNullChildren() {
        AppDefaultMenuTreeNodeAttachment attachment = new AppDefaultMenuTreeNodeAttachment();
        attachment.setChildren(null);
        assertThat(attachment.getChildren()).isNull();
    }
}