package com.zifang.z.lc.common.permission;

/**
 * 权限定义常量 — 蒸馏自 ace-platform-core
 * {@code PermissionDefinitions} ({@code com.c2f.ace.core.common.permission}).
 *
 * <p>用于集中声明 z-lc 平台级权限编码常量:
 * <ul>
 *   <li>{@link #PRODUCT_CODE} — 产品编码 (用于权限前缀/审计来源标识)</li>
 *   <li>{@link #PLATFORM_APP_MANAGER} — 平台应用管理权限编码</li>
 * </ul>
 *
 * <p>业务应用级权限请使用 {@link ZLcAcePermissionDefinitions}.
 *
 * @author zifang
 */
public final class ZLcPermissionDefinitions {

    /** z-lc 产品编码 — 用于审计/日志/权限前缀. */
    public static final String PRODUCT_CODE = "z-lc";

    /** 平台应用管理权限编码 — 具备此权限可访问 z-lc 平台"应用管理"菜单. */
    public static final String PLATFORM_APP_MANAGER = "z-lc:platform:app:manager";

    private ZLcPermissionDefinitions() {
        // constant holder
    }
}