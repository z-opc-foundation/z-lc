package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTreeUtil 单元测试
 *
 * @author zifang
 */
class ZLcTreeUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcTreeUtil> constructor = ZLcTreeUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        List<TestTreeNode> result = ZLcTreeUtil.buildTree(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        List<TestTreeNode> result = ZLcTreeUtil.buildTree(new ArrayList<TestTreeNode>());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldBuildTreeFromFlatList() {
        List<TestTreeNode> flatList = Arrays.asList(
                new TestTreeNode("1", null),
                new TestTreeNode("2", "1"),
                new TestTreeNode("3", "1"),
                new TestTreeNode("4", "2")
        );

        List<TestTreeNode> roots = ZLcTreeUtil.buildTree(flatList);

        assertThat(roots).hasSize(1);
        TestTreeNode root = roots.get(0);
        assertThat(root.getId()).isEqualTo("1");
        assertThat(root.getChildren()).hasSize(2);
    }

    @Test
    void shouldBuildTreeWithMultipleRoots() {
        List<TestTreeNode> flatList = Arrays.asList(
                new TestTreeNode("1", null),
                new TestTreeNode("2", null),
                new TestTreeNode("3", "1"),
                new TestTreeNode("4", "2")
        );

        List<TestTreeNode> roots = ZLcTreeUtil.buildTree(flatList);

        assertThat(roots).hasSize(2);
    }

    @Test
    void shouldCollectLeaves() {
        List<TestTreeNode> flatList = Arrays.asList(
                new TestTreeNode("1", null),
                new TestTreeNode("2", "1"),
                new TestTreeNode("3", "1"),
                new TestTreeNode("4", "2")
        );

        List<TestTreeNode> leaves = ZLcTreeUtil.collectLeaves(flatList);

        assertThat(leaves).hasSize(2);
    }

    @Test
    void shouldFlattenTree() {
        List<TestTreeNode> flatList = Arrays.asList(
                new TestTreeNode("1", null),
                new TestTreeNode("2", "1"),
                new TestTreeNode("3", "1"),
                new TestTreeNode("4", "2")
        );

        List<TestTreeNode> roots = ZLcTreeUtil.buildTree(flatList);
        List<TestTreeNode> flattened = ZLcTreeUtil.flatten(roots);

        assertThat(flattened).hasSize(4);
    }

    @Test
    void shouldReturnEmptyForNullFlatten() {
        List<TestTreeNode> result = ZLcTreeUtil.flatten(null);
        assertThat(result).isEmpty();
    }

    // 测试用的内部类
    static class TestTreeNode implements ZLcTreeUtil.TreeNode<TestTreeNode> {
        private String id;
        private String parentId;
        private List<TestTreeNode> children;

        public TestTreeNode(String id, String parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getParentId() {
            return parentId;
        }

        @Override
        public List<TestTreeNode> getChildren() {
            return children;
        }

        @Override
        public void setChildren(List<TestTreeNode> children) {
            this.children = children;
        }
    }
}