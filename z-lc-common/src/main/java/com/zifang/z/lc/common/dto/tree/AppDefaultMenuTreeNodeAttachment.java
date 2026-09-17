package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.List;

/**
 * 应用默认菜单树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code AppDefaultMenuTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「应用初始化菜单」树节点 — 应用首次部署时自动生成的菜单结构.
 *
 * @author zifang
 */
public class AppDefaultMenuTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型标识.
     */
    private String modelCode;

    /**
     * 菜单路径.
     */
    private String menuPath;

    /**
     * 菜单名称.
     */
    private String menuName;

    /**
     * 菜单图标.
     */
    private String menuIcon;

    /**
     * 子菜单列表.
     */
    private List<AppDefaultMenuTreeNodeAttachment> children;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getMenuPath() {
        return menuPath;
    }

    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    public List<AppDefaultMenuTreeNodeAttachment> getChildren() {
        return children;
    }

    public void setChildren(List<AppDefaultMenuTreeNodeAttachment> children) {
        this.children = children;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }
}
