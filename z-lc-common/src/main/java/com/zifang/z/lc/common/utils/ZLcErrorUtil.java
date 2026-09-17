package com.zifang.z.lc.common.utils;

import com.zifang.z.lc.common.exception.ZLcPermissionLimitException;

/**
 * 错误校验工具 — 蒸馏自 ace-platform-core
 * {@code ErrorUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供便捷的业务校验方法: 当条件为 true 时抛出异常.
 * 蒸馏时移除了 ace 对 BusinessException / AceStatusCode 的依赖,
 * 改为抛出 z-lc 自定义异常.
 *
 * <p>典型场景：
 * <ul>
 *   <li>表单必填字段校验</li>
 *   <li>流程操作权限校验</li>
 *   <li>数据唯一性校验</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcErrorUtil {

    private ZLcErrorUtil() {
    }

    /**
     * 校验条件, 条件为 true 时抛出业务异常.
     *
     * @param condition 校验条件 (true = 不通过)
     * @param message   错误消息
     * @throws ZLcPermissionLimitException 条件为 true 时抛出
     */
    public static void check(boolean condition, String message) {
        if (condition) {
            throw new ZLcPermissionLimitException(message);
        }
    }

    /**
     * 校验条件, 条件为 true 时抛出业务异常.
     *
     * @param condition 校验条件 (true = 不通过)
     * @param message   错误消息
     * @param errorLog  错误日志 (可为 null)
     * @throws ZLcPermissionLimitException 条件为 true 时抛出
     */
    public static void check(boolean condition, String message, String errorLog) {
        if (condition) {
            if (errorLog != null) {
                System.err.println("[ZLcError] " + errorLog);
            }
            throw new ZLcPermissionLimitException(message);
        }
    }

    /**
     * 校验对象非空.
     *
     * @param obj     待校验对象
     * @param message 错误消息
     * @throws IllegalArgumentException 对象为 null 时抛出
     */
    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
