package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用树节点.
 *
 * <p>蒸馏自 ace-platform-client {@code TreeNode}
 * （{@code com.c2f.ace.client.dto.tree}），但去除 lombok 依赖改为手写 getter/setter，
 * 字段语义完全对齐.
 *
 * <p>设计要点：
 * <ul>
 *   <li>双 id 设计：{@link #id}/{@link #parentId} 是业务实体的主键；{@link #treeNodeId}/{@link #parentTreeNodeId}
 *       是树节点 id（可与业务 id 不同 — 同一实体可在多棵树中以不同 id 出现）</li>
 *   <li>{@link #info} 携带节点附带信息（POJO / DTO / 任意类型）</li>
 *   <li>{@link #extend} 携带节点附加扩展信息（任意 Object — 通常是 JSON Map）</li>
 *   <li>{@link #childTreeNodes} 子节点列表</li>
 * </ul>
 *
 * <p>用法：
 * <pre>{@code
 *   TreeNode<DictDTO> root = new TreeNode<>();
 *   root.setTreeNodeId("0");
 *   root.setTreeNodeName("字典根");
 *
 *   TreeNode<DictDTO> child = TreeNode.of("性别", dictInfo);
 *   child.setTreeNodeId("dict_gender");
 *   child.setParentTreeNodeId("0");
 *   root.addTreeNode(child);
 * }</pre>
 *
 * @param <INFO> 节点附带信息类型
 * @author zifang
 */
public class TreeNode<INFO> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字符串型业务 id.
     */
    private String id;

    /**
     * 字符串型业务父级 id.
     */
    private String parentId;

    /**
     * 树节点 id（在所属树内唯一）.
     */
    private String treeNodeId;

    /**
     * 树节点显示名称.
     */
    private String treeNodeName;

    /**
     * 父树节点 id；空或 {@code ROOT_CODE_ID} 表示根节点.
     */
    private String parentTreeNodeId;

    /**
     * 节点附带信息.
     */
    private INFO info;

    /**
     * 节点附带扩展信息（任意 Object — 通常是 JSON 反序列化后的 Map）.
     */
    private Object extend;

    /**
     * 是否是文件性质（叶子节点标记 — 用于 UI 上区分文件夹 vs 文件图标）.
     */
    private boolean fileFlag;

    /**
     * 是否默认勾选（前端多选树使用）.
     */
    private boolean selectedFlag;

    /**
     * 子节点列表.
     */
    private List<TreeNode<INFO>> childTreeNodes;

    /**
     * 自定义标签（前端打 tag 用 — 比如"内置""草稿""已发布"等）.
     */
    private List<String> customTags;

    /**
     * 工厂方法 — 用「名称 + 信息」快速构建节点.
     */
    public static <INFO> TreeNode<INFO> of(String treeName, INFO info) {
        TreeNode<INFO> n = new TreeNode<>();
        n.setTreeNodeName(treeName);
        n.setInfo(info);
        return n;
    }

    /**
     * 工厂方法 — 用「名称 + 信息 + 文件标志」快速构建节点.
     */
    public static <INFO> TreeNode<INFO> of(String treeName, INFO info, Boolean fileFlag) {
        TreeNode<INFO> n = new TreeNode<>();
        n.setTreeNodeName(treeName);
        n.setInfo(info);
        n.setFileFlag(fileFlag);
        return n;
    }

    /**
     * 累加本节点（含子节点）下所有「可用」节点数（带 info 的节点）.
     * <p>用于前端懒加载场景 — 判断是否还有子节点可展开.
     */
    public int availableNodeCount() {
        int sum = 0;
        if (info != null) {
            if (info instanceof List) {
                sum += ((List<?>) info).size();
            } else {
                sum += 1;
            }
        }
        if (childTreeNodes != null && !childTreeNodes.isEmpty()) {
            for (TreeNode<INFO> t : childTreeNodes) {
                sum += t.availableNodeCount();
            }
        }
        return sum;
    }

    /**
     * 递归清理空子树（保留至少有一个 info 节点的路径）.
     */
    public void clean() {
        if (childTreeNodes != null && !childTreeNodes.isEmpty()) {
            childTreeNodes.removeIf(t -> t.availableNodeCount() == 0);
            for (TreeNode<INFO> t : childTreeNodes) {
                t.clean();
            }
        }
    }

    /**
     * 添加子节点（链式调用）.
     */
    public TreeNode<INFO> addTreeNode(TreeNode<INFO> treeNode) {
        if (this.childTreeNodes == null) {
            childTreeNodes = new ArrayList<>();
        }
        childTreeNodes.add(treeNode);
        return this;
    }

    /**
     * 递归遍历自身 + 所有子节点 — consumer.accept 每个节点.
     */
    public void accept(java.util.function.Consumer<TreeNode<INFO>> consumer) {
        consumer.accept(this);
        if (this.childTreeNodes != null) {
            for (TreeNode<INFO> node : childTreeNodes) {
                node.accept(consumer);
            }
        }
    }

    // —— getter / setter ——

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }

    public String getTreeNodeName() {
        return treeNodeName;
    }

    public void setTreeNodeName(String treeNodeName) {
        this.treeNodeName = treeNodeName;
    }

    public String getParentTreeNodeId() {
        return parentTreeNodeId;
    }

    public void setParentTreeNodeId(String parentTreeNodeId) {
        this.parentTreeNodeId = parentTreeNodeId;
    }

    public INFO getInfo() {
        return info;
    }

    public void setInfo(INFO info) {
        this.info = info;
    }

    public Object getExtend() {
        return extend;
    }

    public void setExtend(Object extend) {
        this.extend = extend;
    }

    public boolean isFileFlag() {
        return fileFlag;
    }

    public void setFileFlag(boolean fileFlag) {
        this.fileFlag = fileFlag;
    }

    public boolean isSelectedFlag() {
        return selectedFlag;
    }

    public void setSelectedFlag(boolean selectedFlag) {
        this.selectedFlag = selectedFlag;
    }

    public List<TreeNode<INFO>> getChildTreeNodes() {
        return childTreeNodes;
    }

    public void setChildTreeNodes(List<TreeNode<INFO>> childTreeNodes) {
        this.childTreeNodes = childTreeNodes;
    }

    public List<String> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(List<String> customTags) {
        this.customTags = customTags;
    }
}
