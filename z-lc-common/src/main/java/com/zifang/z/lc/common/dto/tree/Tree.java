package com.zifang.z.lc.common.dto.tree;

import com.zifang.z.lc.common.constance.WebBuildConstance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用树容器 — 提供从扁平节点列表构造树、扁平化、剔除深度、挑选子树等能力.
 *
 * <p>蒸馏自 ace-platform-client {@code Tree}
 * （{@code com.c2f.ace.client.dto.tree}），行为完全对齐：
 * <ul>
 *   <li>{@link #build()} 把 {@code originNode} 按 {@code parentTreeNodeId} 拼装成树，
 *       并把根节点列表存入 {@link #roots}</li>
 *   <li>{@link #accept(TreeNodeAcceptor)} 深度遍历所有根节点</li>
 *   <li>{@link #pick(String)} / {@link #pick(List)} 从原节点里挑出子树</li>
 *   <li>{@link #evictDepth(Integer)} 限制树的展示深度（多余子节点置 null）</li>
 *   <li>{@link #flatByTreeNodeId()} 把原节点铺平为 Map（按 treeNodeId 索引）</li>
 * </ul>
 *
 * <p>典型用法（前端「模型树」展示）：
 * <pre>{@code
 *   Tree<DataModelDTO> tree = new Tree<>();
 *   for (DataModelDTO m : models) {
 *       TreeNode<DataModelDTO> n = new TreeNode<>();
 *       n.setTreeNodeId(m.getModelCode());
 *       n.setParentTreeNodeId(m.getParentCode() == null ? WebBuildConstance.ROOT_CODE_ID : m.getParentCode());
 *       n.setTreeNodeName(m.getModelName());
 *       n.setInfo(m);
 *       tree.addTreeNode(n);
 *   }
 *   tree.build();
 *   tree.evictDepth(3); // 前端最多展示 3 层
 *   return tree.getRoots();
 * }</pre>
 *
 * @param <INFO> 节点附带信息类型
 * @author zifang
 */
public class Tree<INFO> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原始节点列表（构造前的扁平集合）.
     */
    private List<TreeNode<INFO>> originNode;

    /**
     * 构造后的根节点列表 — {@link #build()} 调用后填充.
     */
    private List<TreeNode<INFO>> roots;

    /**
     * 把 {@link #originNode} 按 {@code parentTreeNodeId} 拼装成树结构.
     *
     * @return 根节点列表（同时赋值给 {@link #roots}）
     */
    public List<TreeNode<INFO>> build() {
        if (originNode == null) {
            roots = new ArrayList<>();
            return new ArrayList<>();
        }

        Map<String, List<TreeNode<INFO>>> byParent = originNode.stream()
                .filter(e -> e.getParentTreeNodeId() != null && !e.getParentTreeNodeId().isEmpty())
                .collect(Collectors.groupingBy(TreeNode::getParentTreeNodeId));

        originNode.forEach(e -> e.setChildTreeNodes(byParent.get(e.getTreeNodeId())));

        roots = originNode.stream()
                .filter(e -> e.getParentTreeNodeId() == null
                        || e.getParentTreeNodeId().isEmpty()
                        || WebBuildConstance.ROOT_CODE_ID.equals(e.getParentTreeNodeId()))
                .collect(Collectors.toList());

        return roots;
    }

    /**
     * 添加一个原始节点 — 调用 {@link #build()} 之前逐个添加.
     */
    public void addTreeNode(TreeNode<INFO> node) {
        if (originNode == null) {
            originNode = new ArrayList<>();
        }
        originNode.add(node);
    }

    /**
     * 按 treeNodeId 铺平为 Map — 便于 O(1) 查找节点.
     */
    public Map<String, TreeNode<INFO>> flatByTreeNodeId() {
        if (originNode == null) {
            return new LinkedHashMap<>();
        }
        return originNode.stream().collect(Collectors.toMap(TreeNode::getTreeNodeId, e -> e, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 深度遍历所有根节点并回调 acceptor.
     */
    public void accept(TreeNodeAcceptor<INFO> acceptor) {
        if (roots == null) {
            build();
        }
        for (TreeNode<INFO> root : roots) {
            acceptInner(root, acceptor);
        }
    }

    private void acceptInner(TreeNode<INFO> root, TreeNodeAcceptor<INFO> acceptor) {
        acceptor.accept(root);
        if (root.getChildTreeNodes() != null) {
            for (TreeNode<INFO> node : root.getChildTreeNodes()) {
                acceptInner(node, acceptor);
            }
        }
    }

    /**
     * 从原节点中挑出指定 id 的节点作为新的唯一根节点.
     */
    public void pick(String treeNodeId) {
        if (originNode == null) {
            roots = new ArrayList<>();
            return;
        }
        Map<String, TreeNode<INFO>> map = originNode.stream()
                .collect(Collectors.toMap(TreeNode::getTreeNodeId, e -> e, (a, b) -> a, LinkedHashMap::new));
        TreeNode<INFO> picked = map.get(treeNodeId);
        roots = picked == null ? new ArrayList<>() : Collections.singletonList(picked);
    }

    /**
     * 从原节点中挑出多个 id 作为新的根节点列表.
     */
    public void pick(List<String> treeNodeIds) {
        if (originNode == null || treeNodeIds == null) {
            roots = new ArrayList<>();
            return;
        }
        Map<String, TreeNode<INFO>> map = originNode.stream()
                .collect(Collectors.toMap(TreeNode::getTreeNodeId, e -> e, (a, b) -> a, LinkedHashMap::new));
        roots = new ArrayList<>();
        for (String id : treeNodeIds) {
            TreeNode<INFO> node = map.get(id);
            if (node != null) {
                roots.add(node);
            }
        }
    }

    /**
     * 限制树的展示深度（{@code depth=0} 时所有节点的 childTreeNodes 都被置 null；
     * {@code depth=1} 时保留直接子节点，第 2 层及更深被截断；依此类推）.
     */
    public void evictDepth(Integer depth) {
        if (roots == null) {
            return;
        }
        for (TreeNode<INFO> root : roots) {
            evictInner(root, depth, 0);
        }
    }

    private void evictInner(TreeNode<INFO> root, Integer depth, int level) {
        if (depth == null || depth == 0) {
            root.setChildTreeNodes(null);
            return;
        }
        if (level + 1 >= depth) {
            root.setChildTreeNodes(null);
            return;
        }
        if (root.getChildTreeNodes() != null) {
            for (TreeNode<INFO> item : root.getChildTreeNodes()) {
                evictInner(item, depth, level + 1);
            }
        }
    }

    // —— getter / setter ——

    public List<TreeNode<INFO>> getOriginNode() {
        return originNode;
    }

    public void setOriginNode(List<TreeNode<INFO>> originNode) {
        this.originNode = originNode;
    }

    public List<TreeNode<INFO>> getRoots() {
        return roots;
    }

    public void setRoots(List<TreeNode<INFO>> roots) {
        this.roots = roots;
    }
}
