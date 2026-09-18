package com.zifang.z.lc.common.constants;

/**
 * 权限定义常量 — 蒸馏自 ace-platform-core
 * {@code PermissionDefinitions} ({@code com.c2f.ace.core.common.permission}).
 *
 * <p>定义低代码平台的产品标识和核心权限码.
 * 蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为纯常量接口.
 *
 * @author zifang
 */
public interface ZLcPermissionDefinitions {

    /** 产品标识. */
    String PRODUCT_CODE = "z-lc";

    /** 应用管理权限. */
    String APP_MANAGER = "z-lc:app:manager";
}
