package com.zifang.z.lc.common.enums;

/**
 * 状态枚举 — 蒸馏自 ace-platform-core
 * {@code EStatus} ({@code com.c2f.ace.core.utils}).
 *
 * <p>通用布尔状态枚举, 用于低代码平台的标志位字段 (如是否启用、是否完成等).
 * 蒸馏时移除了 ace 对 StatusCode 接口的依赖, 改为纯枚举实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>模型字段的启用/禁用状态</li>
 *   <li>审批任务的完成/未完成状态</li>
 *   <li>API 返回的布尔标志位</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcEStatus {

    FALSE(0, "FALSE"),
    TRUE(1, "TRUE");

    private final Integer code;
    private final String message;

    ZLcEStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码.
     */
    public int getCode() {
        return this.code;
    }

    /**
     * 获取状态消息.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * 根据 code 获取枚举值.
     *
     * @param code 状态码
     * @return 对应枚举值; 找不到时返回 FALSE
     */
    public static ZLcEStatus fromCode(int code) {
        for (ZLcEStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return FALSE;
    }

    /**
     * 判断当前状态是否为 TRUE.
     */
    public boolean isTrue() {
        return this == TRUE;
    }
}
