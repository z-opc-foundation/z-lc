package com.zifang.z.lc.common.constance;

/**
 * 权限类型常量.
 *
 * <p>蒸馏自 ace-platform-client {@code PermissionType}
 * （{@code com.c2f.ace.client.constance}），字段语义完全对齐.
 *
 * <p>用于 {@code AuthorityAttachment.permissionType} 字段 — 区分「菜单 / 按钮 / API」三种权限.
 *
 * @author zifang
 */
public final class PermissionType {

    /**
     * 菜单权限.
     */
    public static final Integer MENU = 1;

    /**
     * 按钮权限.
     */
    public static final Integer BUTTON = 2;

    /**
     * 功能权限 / API 权限.
     */
    public static final Integer API = 3;

    private PermissionType() {
        // 工具类，禁止实例化
    }
}
