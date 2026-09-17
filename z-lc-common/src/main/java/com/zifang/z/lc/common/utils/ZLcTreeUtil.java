package com.zifang.z.lc.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 树形结构构建工具 — 蒸馏自 ace-platform-core
 * {@code TreeUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>将扁平 List 构建为树形结构. 泛型设计, 业务方只需提供
 * ID 提取器和父 ID 提取器即可复用.
 *
 * <p>典型场景：
 * <ul>
 *   <li>菜单树 / 组织架构树 / 数据字典树 构建</li>
 *   <li>模型字段层级关系展示</li>
 *   <li>应用 → 模型 → 页面 的层级导航</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcTreeUtil {

    private ZLcTreeUtil() {
    }

    /**
     * 树节点接口 — 业务节点需实现此接口.
     *
     * @param <T> 节点类型
     */
    public interface TreeNode<T> {
        /** 获取节点 ID. */
        String getId();

        /** 获取父节点 ID (根节点返回 null 或空串). */
        String getParentId();

        /** 获取子节点列表. */
        List<T> getChildren();

        /** 设置子节点列表. */
        void setChildren(List<T> children);
    }

    /**
     * 构建树形结构.
     *
     * @param nodes 扁平节点列表
     * @param <T>   节点类型 (需实现 {@link TreeNode})
     * @return 根节点列表 (可能包含多个根)
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 找出根节点 (parentId 为空)
        List<T> roots = new ArrayList<>();
        // 2. 建立 parentId → children 映射
        Map<String, List<T>> childrenMap = new HashMap<>();

        for (T node : nodes) {
            String parentId = node.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                roots.add(node);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
            }
        }

        // 3. 递归挂载子节点
        for (T root : roots) {
            attachChildren(root, childrenMap);
        }

        return roots;
    }

    /**
     * 递归挂载子节点.
     */
    private static <T extends TreeNode<T>> void attachChildren(T node, Map<String, List<T>> childrenMap) {
        List<T> children = childrenMap.get(node.getId());
        if (children != null) {
            node.setChildren(children);
            for (T child : children) {
                attachChildren(child, childrenMap);
            }
        } else {
            node.setChildren(new ArrayList<>());
        }
    }

    /**
     * 从扁平列表中收集所有叶子节点 (无子节点的节点).
     *
     * @param nodes 扁平节点列表
     * @param <T>   节点类型
     * @return 叶子节点列表
     */
    public static <T extends TreeNode<T>> List<T> collectLeaves(List<T> nodes) {
        List<T> roots = buildTree(nodes);
        List<T> leaves = new ArrayList<>();
        for (T root : roots) {
            collectLeavesRecursive(root, leaves);
        }
        return leaves;
    }

    private static <T extends TreeNode<T>> void collectLeavesRecursive(T node, List<T> leaves) {
        List<T> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            leaves.add(node);
        } else {
            for (T child : children) {
                collectLeavesRecursive(child, leaves);
            }
        }
    }

    /**
     * 将树形结构展平为列表 (深度优先).
     *
     * @param roots 根节点列表
     * @param <T>   节点类型
     * @return 展平后的列表
     */
    public static <T extends TreeNode<T>> List<T> flatten(List<T> roots) {
        List<T> result = new ArrayList<>();
        if (roots != null) {
            for (T root : roots) {
                flattenRecursive(root, result);
            }
        }
        return result;
    }

    private static <T extends TreeNode<T>> void flattenRecursive(T node, List<T> result) {
        result.add(node);
        List<T> children = node.getChildren();
        if (children != null) {
            for (T child : children) {
                flattenRecursive(child, result);
            }
        }
    }
}
