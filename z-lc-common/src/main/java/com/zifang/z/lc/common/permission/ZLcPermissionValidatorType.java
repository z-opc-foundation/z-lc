package com.zifang.z.lc.common.permission;

/**
 * 权限校验类型常量 — 蒸馏自 ace-platform-core
 * {@code PermissionValidatorType} ({@code com.c2f.ace.core.common.permission}).
 *
 * <p>用于 {@link ZLcPermissionValidator#validateType()} 注解参数,
 * 标识校验逻辑: 当前仅支持 {@link #ALL} (全部权限命中才通过).
 *
 * @author zifang
 */
public final class ZLcPermissionValidatorType {

    /** 全部命中 — 默认值, 业务方需具备 {@code permissions[]} 中所有权限才通过校验. */
    public static final String ALL = "all";

    private ZLcPermissionValidatorType() {
        // constant holder
    }
}