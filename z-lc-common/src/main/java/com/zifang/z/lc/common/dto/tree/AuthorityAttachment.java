package com.zifang.z.lc.common.dto.tree;

import com.zifang.z.lc.common.constance.PermissionType;

import java.io.Serializable;

/**
 * 权限树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code AuthorityAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「权限管理」树节点 — 承载菜单 / 按钮 / 数据权限三类节点的属性.
 *
 * @author zifang
 */
public class AuthorityAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用标识.
     */
    private String appCode;

    /**
     * 权限 code.
     */
    private String permissionCode;

    /**
     * 资源 code（数据权限时使用 — 通常是 modelCode）.
     */
    private String resourceCode;

    /**
     * 资源名称.
     */
    private String resourceName;

    /**
     * 属性标识（数据权限时使用 — 通常是 fieldCode）.
     */
    private String attrCode;

    /**
     * 权限名称.
     */
    private String permissionName;

    /**
     * 权限类型（{@link PermissionType#MENU} / {@link PermissionType#BUTTON} / {@link PermissionType#API}）.
     */
    private Integer permissionType;

    /**
     * 父权限 code（用于构造权限树）.
     */
    private String parentPermissionCode;

    /**
     * 绑定菜单 URL（菜单权限时使用）.
     */
    private String menuUrl;

    /**
     * 已被勾选（前端多选树使用）.
     */
    private boolean selectedFlag;

    /**
     * 已被修改过（脏标记 — 用于「保存权限」时 diff 提交）.
     */
    private boolean modifiedFlag;

    /**
     * 工厂方法 — 构造菜单权限附件.
     */
    public static AuthorityAttachment ofMenu(String menuName, String menuUrl, String permissionCode) {
        AuthorityAttachment a = new AuthorityAttachment();
        a.setPermissionName(menuName);
        a.setMenuUrl(menuUrl);
        a.setPermissionType(PermissionType.MENU);
        a.setPermissionCode(permissionCode);
        return a;
    }

    /**
     * 工厂方法 — 构造 API 权限附件.
     */
    public static AuthorityAttachment ofAPI(String permissionName, String permissionCode) {
        AuthorityAttachment a = new AuthorityAttachment();
        a.setPermissionType(PermissionType.API);
        a.setPermissionName(permissionName);
        a.setPermissionCode(permissionCode);
        return a;
    }

    /**
     * 工厂方法 — 构造数据权限附件.
     */
    public static AuthorityAttachment ofData(String resourceName, String resourceCode, String attrCode) {
        AuthorityAttachment a = new AuthorityAttachment();
        a.setResourceCode(resourceCode);
        a.setResourceName(resourceName);
        a.setAttrCode(attrCode);
        return a;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getAttrCode() {
        return attrCode;
    }

    public void setAttrCode(String attrCode) {
        this.attrCode = attrCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public Integer getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(Integer permissionType) {
        this.permissionType = permissionType;
    }

    public String getParentPermissionCode() {
        return parentPermissionCode;
    }

    public void setParentPermissionCode(String parentPermissionCode) {
        this.parentPermissionCode = parentPermissionCode;
    }

    public String getMenuUrl() {
        return menuUrl;
    }

    public void setMenuUrl(String menuUrl) {
        this.menuUrl = menuUrl;
    }

    public boolean isSelectedFlag() {
        return selectedFlag;
    }

    public void setSelectedFlag(boolean selectedFlag) {
        this.selectedFlag = selectedFlag;
    }

    public boolean isModifiedFlag() {
        return modifiedFlag;
    }

    public void setModifiedFlag(boolean modifiedFlag) {
        this.modifiedFlag = modifiedFlag;
    }
}
