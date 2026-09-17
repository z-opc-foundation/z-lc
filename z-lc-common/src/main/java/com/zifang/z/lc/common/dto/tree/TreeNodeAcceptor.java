package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * 树节点访问器（Visitor 模式）.
 *
 * <p>蒸馏自 ace-platform-client {@code TreeNodeAcceptor}
 * （{@code com.c2f.ace.client.dto.tree}）。
 *
 * <p>用于递归遍历 {@link Tree} 时回调处理每个节点 — 比手写 for 循环更优雅，
 * 尤其在节点上要做「查找 / 统计 / 转换」等通用操作时.
 *
 * <p>典型用法（统计总节点数）：
 * <pre>{@code
 *   tree.accept(new TreeNodeAcceptor<MyInfo>() {
 *       public void accept(TreeNode<MyInfo> node) {
 *           count.incrementAndGet();
 *       }
 *   });
 * }</pre>
 *
 * @param <INFO> 节点附带信息的类型
 * @author zifang
 */
@FunctionalInterface
public interface TreeNodeAcceptor<INFO> extends Serializable {

    /**
     * 处理单个树节点.
     *
     * @param treeNode 当前访问到的节点
     */
    void accept(TreeNode<INFO> treeNode);
}
