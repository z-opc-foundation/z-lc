package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TreeNode 单元测试
 */
class TreeNodeTest {

    @Test
    void shouldCreateEmptyNode() {
        TreeNode<String> node = new TreeNode<>();
        assertThat(node).isNotNull();
        assertThat(node.getTreeNodeId()).isNull();
        assertThat(node.getChildTreeNodes()).isNull();
        assertThat(node.isFileFlag()).isFalse();
        assertThat(node.isSelectedFlag()).isFalse();
    }

    @Test
    void shouldImplementSerializable() {
        TreeNode<String> node = new TreeNode<>();
        assertThat(node).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetAndGetFields() {
        TreeNode<String> node = new TreeNode<>();
        node.setId("id1");
        node.setParentId("p1");
        node.setTreeNodeId("t1");
        node.setTreeNodeName("Root");
        node.setParentTreeNodeId("0");

        assertThat(node.getId()).isEqualTo("id1");
        assertThat(node.getParentId()).isEqualTo("p1");
        assertThat(node.getTreeNodeId()).isEqualTo("t1");
        assertThat(node.getTreeNodeName()).isEqualTo("Root");
        assertThat(node.getParentTreeNodeId()).isEqualTo("0");
    }

    @Test
    void shouldSetAndGetInfo() {
        TreeNode<String> node = new TreeNode<>();
        node.setInfo("info");
        assertThat(node.getInfo()).isEqualTo("info");
    }

    @Test
    void shouldSetAndGetExtend() {
        TreeNode<String> node = new TreeNode<>();
        node.setExtend("extend");
        assertThat(node.getExtend()).isEqualTo("extend");
    }

    @Test
    void shouldSetAndGetFileFlag() {
        TreeNode<String> node = new TreeNode<>();
        node.setFileFlag(true);
        assertThat(node.isFileFlag()).isTrue();
    }

    @Test
    void shouldSetAndGetSelectedFlag() {
        TreeNode<String> node = new TreeNode<>();
        node.setSelectedFlag(true);
        assertThat(node.isSelectedFlag()).isTrue();
    }

    @Test
    void shouldSetAndGetCustomTags() {
        TreeNode<String> node = new TreeNode<>();
        node.setCustomTags(Arrays.asList("tag1", "tag2"));
        assertThat(node.getCustomTags()).hasSize(2);
    }

    @Test
    void shouldCreateViaOfFactory() {
        TreeNode<String> node = TreeNode.of("MyNode", "info");
        assertThat(node.getTreeNodeName()).isEqualTo("MyNode");
        assertThat(node.getInfo()).isEqualTo("info");
    }

    @Test
    void shouldCreateViaOfFactoryWithFileFlag() {
        TreeNode<String> node = TreeNode.of("FileNode", "info", true);
        assertThat(node.getTreeNodeName()).isEqualTo("FileNode");
        assertThat(node.getInfo()).isEqualTo("info");
        assertThat(node.isFileFlag()).isTrue();
    }

    @Test
    void ofFactoryWithFileFlagNullShouldThrowNpe() {
        // The factory unboxes Boolean; null fileFlag will NPE.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                TreeNode.of("Node", "info", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldAddTreeNode() {
        TreeNode<String> parent = new TreeNode<>();
        TreeNode<String> child = new TreeNode<>();
        TreeNode<String> result = parent.addTreeNode(child);

        assertThat(result).isSameAs(parent);
        assertThat(parent.getChildTreeNodes()).contains(child);
    }

    @Test
    void shouldAppendToExistingChildList() {
        TreeNode<String> parent = new TreeNode<>();
        parent.addTreeNode(new TreeNode<>());
        parent.addTreeNode(new TreeNode<>());
        assertThat(parent.getChildTreeNodes()).hasSize(2);
    }

    @Test
    void availableNodeCountShouldCountInfoNodes() {
        TreeNode<String> root = TreeNode.of("root", "info1");
        root.addTreeNode(TreeNode.of("c1", "info2"));
        root.addTreeNode(TreeNode.of("c2", null));
        assertThat(root.availableNodeCount()).isEqualTo(2);
    }

    @Test
    void availableNodeCountShouldHandleListInfo() {
        TreeNode<List<String>> root = new TreeNode<>();
        root.setInfo(Arrays.asList("a", "b", "c"));
        assertThat(root.availableNodeCount()).isEqualTo(3);
    }

    @Test
    void availableNodeCountShouldHandleNullInfo() {
        TreeNode<String> root = new TreeNode<>();
        root.setInfo(null);
        assertThat(root.availableNodeCount()).isZero();
    }

    @Test
    void cleanShouldRemoveEmptySubtrees() {
        TreeNode<String> root = TreeNode.of("root", "info");
        TreeNode<String> child1 = TreeNode.of("c1", "info");
        TreeNode<String> child2 = new TreeNode<>();  // no info
        root.addTreeNode(child1);
        root.addTreeNode(child2);
        root.clean();
        assertThat(root.getChildTreeNodes()).hasSize(1);
    }

    @Test
    void cleanShouldHandleNullChildren() {
        TreeNode<String> root = TreeNode.of("root", "info");
        root.clean();  // should not throw
    }

    @Test
    void acceptShouldVisitAllNodes() {
        TreeNode<String> root = TreeNode.of("root", "r");
        root.addTreeNode(TreeNode.of("c1", "c1"));
        root.addTreeNode(TreeNode.of("c2", "c2"));

        List<String> names = new ArrayList<>();
        root.accept(n -> names.add(n.getTreeNodeName()));

        assertThat(names).containsExactly("root", "c1", "c2");
    }

    @Test
    void acceptShouldHandleNullChildren() {
        TreeNode<String> root = TreeNode.of("root", "r");
        List<String> names = new ArrayList<>();
        root.accept(n -> names.add(n.getTreeNodeName()));
        assertThat(names).containsExactly("root");
    }
}