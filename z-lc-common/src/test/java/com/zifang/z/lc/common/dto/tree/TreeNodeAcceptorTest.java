package com.zifang.z.lc.common.dto.tree;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TreeNodeAcceptor 单元测试
 *
 * @author zifang
 */
class TreeNodeAcceptorTest {

    @Test
    void shouldBeFunctionalInterface() {
        assertThat(TreeNodeAcceptor.class.isAnnotationPresent(FunctionalInterface.class)).isTrue();
    }

    @Test
    void shouldImplementSerializable() {
        TreeNodeAcceptor<String> acceptor = new TreeNodeAcceptor<String>() {
            @Override
            public void accept(TreeNode<String> treeNode) {
                // do nothing
            }
        };
        assertThat(acceptor).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldAcceptTreeNode() {
        AtomicInteger counter = new AtomicInteger(0);
        TreeNodeAcceptor<String> acceptor = node -> counter.incrementAndGet();

        TreeNode<String> node = TreeNode.of("node-1", "test");
        acceptor.accept(node);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldWorkWithLambda() {
        TreeNodeAcceptor<Integer> acceptor = node -> {
            // 使用节点
            assertThat(node).isNotNull();
        };

        TreeNode<Integer> node = TreeNode.of("node-1", 100);
        acceptor.accept(node);
    }

    @Test
    void shouldAcceptMultipleNodes() {
        AtomicInteger counter = new AtomicInteger(0);
        TreeNodeAcceptor<String> acceptor = node -> counter.incrementAndGet();

        TreeNode<String> node1 = TreeNode.of("node-1", "a");
        TreeNode<String> node2 = TreeNode.of("node-2", "b");
        TreeNode<String> node3 = TreeNode.of("node-3", "c");

        acceptor.accept(node1);
        acceptor.accept(node2);
        acceptor.accept(node3);

        assertThat(counter.get()).isEqualTo(3);
    }
}
