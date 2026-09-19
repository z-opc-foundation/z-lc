package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tree 单元测试
 *
 * @author zifang
 */
class TreeTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        Tree<String> tree = new Tree<>();
        assertThat(tree).isNotNull();
    }

    @Test
    void shouldSetAndGetOriginNode() {
        Tree<String> tree = new Tree<>();
        TreeNode<String> node = new TreeNode<>();
        node.setTreeNodeId("node-1");
        node.setParentTreeNodeId("0");
        
        List<TreeNode<String>> originNode = Arrays.asList(node);
        tree.setOriginNode(originNode);
        
        assertThat(tree.getOriginNode()).isEqualTo(originNode);
    }

    @Test
    void shouldSetAndGetRoots() {
        Tree<String> tree = new Tree<>();
        TreeNode<String> root = new TreeNode<>();
        root.setTreeNodeId("root-1");
        
        List<TreeNode<String>> roots = Arrays.asList(root);
        tree.setRoots(roots);
        
        assertThat(tree.getRoots()).isEqualTo(roots);
    }

    @Test
    void shouldHandleNullOriginNode() {
        Tree<String> tree = new Tree<>();
        List<TreeNode<String>> roots = tree.build();
        assertThat(roots).isEmpty();
    }

    @Test
    void shouldBuildSimpleTree() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> root = new TreeNode<>();
        root.setTreeNodeId("root-1");
        root.setParentTreeNodeId("0");
        
        TreeNode<String> child = new TreeNode<>();
        child.setTreeNodeId("child-1");
        child.setParentTreeNodeId("root-1");
        
        tree.setOriginNode(Arrays.asList(root, child));
        
        List<TreeNode<String>> roots = tree.build();
        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getChildTreeNodes()).hasSize(1);
        assertThat(roots.get(0).getChildTreeNodes().get(0).getTreeNodeId()).isEqualTo("child-1");
    }

    @Test
    void shouldAddTreeNode() {
        Tree<String> tree = new Tree<>();
        TreeNode<String> node = new TreeNode<>();
        node.setTreeNodeId("node-1");
        
        tree.addTreeNode(node);
        
        assertThat(tree.getOriginNode()).hasSize(1);
        assertThat(tree.getOriginNode().get(0).getTreeNodeId()).isEqualTo("node-1");
    }

    @Test
    void shouldFlatByTreeNodeId() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> node1 = new TreeNode<>();
        node1.setTreeNodeId("node-1");
        
        TreeNode<String> node2 = new TreeNode<>();
        node2.setTreeNodeId("node-2");
        
        tree.setOriginNode(Arrays.asList(node1, node2));
        
        Map<String, TreeNode<String>> flatMap = tree.flatByTreeNodeId();
        assertThat(flatMap).hasSize(2);
        assertThat(flatMap.get("node-1")).isEqualTo(node1);
        assertThat(flatMap.get("node-2")).isEqualTo(node2);
    }

    @Test
    void shouldHandleNullOriginNodeForFlat() {
        Tree<String> tree = new Tree<>();
        Map<String, TreeNode<String>> flatMap = tree.flatByTreeNodeId();
        assertThat(flatMap).isEmpty();
    }

    @Test
    void shouldAcceptAllNodes() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> root = new TreeNode<>();
        root.setTreeNodeId("root-1");
        root.setParentTreeNodeId("0");
        
        TreeNode<String> child = new TreeNode<>();
        child.setTreeNodeId("child-1");
        child.setParentTreeNodeId("root-1");
        
        tree.setOriginNode(Arrays.asList(root, child));
        
        AtomicInteger count = new AtomicInteger(0);
        tree.accept(node -> count.incrementAndGet());
        
        assertThat(count.get()).isEqualTo(2);
    }

    @Test
    void shouldPickSingleNode() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> node1 = new TreeNode<>();
        node1.setTreeNodeId("node-1");
        
        TreeNode<String> node2 = new TreeNode<>();
        node2.setTreeNodeId("node-2");
        
        tree.setOriginNode(Arrays.asList(node1, node2));
        
        tree.pick("node-1");
        
        assertThat(tree.getRoots()).hasSize(1);
        assertThat(tree.getRoots().get(0).getTreeNodeId()).isEqualTo("node-1");
    }

    @Test
    void shouldPickMultipleNodes() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> node1 = new TreeNode<>();
        node1.setTreeNodeId("node-1");
        
        TreeNode<String> node2 = new TreeNode<>();
        node2.setTreeNodeId("node-2");
        
        TreeNode<String> node3 = new TreeNode<>();
        node3.setTreeNodeId("node-3");
        
        tree.setOriginNode(Arrays.asList(node1, node2, node3));
        
        tree.pick(Arrays.asList("node-1", "node-3"));
        
        assertThat(tree.getRoots()).hasSize(2);
        assertThat(tree.getRoots().get(0).getTreeNodeId()).isEqualTo("node-1");
        assertThat(tree.getRoots().get(1).getTreeNodeId()).isEqualTo("node-3");
    }

    @Test
    void shouldEvictDepth() {
        Tree<String> tree = new Tree<>();
        
        TreeNode<String> root = new TreeNode<>();
        root.setTreeNodeId("root-1");
        root.setParentTreeNodeId("0");
        
        TreeNode<String> child = new TreeNode<>();
        child.setTreeNodeId("child-1");
        child.setParentTreeNodeId("root-1");
        
        TreeNode<String> grandchild = new TreeNode<>();
        grandchild.setTreeNodeId("grandchild-1");
        grandchild.setParentTreeNodeId("child-1");
        
        tree.setOriginNode(Arrays.asList(root, child, grandchild));
        tree.build();
        
        tree.evictDepth(1);
        
        assertThat(tree.getRoots().get(0).getChildTreeNodes()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        Tree<String> tree = new Tree<>();
        assertThat(tree).isInstanceOf(java.io.Serializable.class);
    }
}