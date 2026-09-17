package com.zifang.z.lc.core.domain;

/**
 * 应用菜单配置 DO — 蒸馏自 ace-platform-core
 * {@code AppMenuConfigDO} （{@code com.c2f.ace.core.domain.entity}}，
 * 字段语义完全对齐.
 *
 * <p>对应 {@code app_menu_config} 表 — 应用下页面级菜单的权限/层级配置,
 * 引擎用其构建左侧导航菜单树 + 路由权限拦截.
 *
 * <p>典型用途：
 * <ul>
 *   <li>低代码 UI 渲染时，按 appCode 拉菜单列表 → 构建树</li>
 *   <li>权限校验 — 用户访问 pageCode 时检查 permissionCode 是否在用户权限范围</li>
 *   <li>父子菜单层级 — parentMenuId 字段</li>
 * </ul>
 *
 * @author zifang
 */
public class AppMenuConfigDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 页面 code.
     */
    private String pageCode;

    /**
     * 权限 code.
     */
    private String permissionCode;

    /**
     * 父级权限 code.
     */
    private String parentPermissionCode;

    /**
     * 菜单路径.
     */
    private String menuUrl;

    /**
     * 菜单名称.
     */
    private String menuName;

    /**
     * 父菜单 id.
     */
    private Long parentMenuId;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
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

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Long getParentMenuId() {
        return parentMenuId;
    }

    public void setParentMenuId(Long parentMenuId) {
        this.parentMenuId = parentMenuId;
    }
}
