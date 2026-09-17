package com.zifang.z.lc.common.exception;

/**
 * 权限受限异常 — 蒸馏自 ace-platform-core
 * {@code PermissionLimitException} ({@code com.c2f.ace.core.exception}).
 *
 * <p>在低代码平台操作鉴权逻辑中，当用户无权执行当前操作时抛出此异常.
 * 典型场景：用户尝试修改/删除非本人创建的模型、页面、流程，
 * 或尝试访问无权限的应用数据.
 *
 * @author zifang
 */
public class ZLcPermissionLimitException extends RuntimeException {

    public ZLcPermissionLimitException(String message) {
        super(message);
    }

    public ZLcPermissionLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
