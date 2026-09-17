package com.zifang.z.lc.common.enums;

/**
 * 服务类型枚举 — 蒸馏自 ace-platform-core
 * {@code ServiceTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"服务"管理 — 标识服务调用方式:
 *
 * <ul>
 *   <li>{@link #HTTP} — 内部 HTTP 接口 (走 z-rpc / z-config 注册中心)</li>
 *   <li>{@link #OPEN_PLATFORM} — 开放平台接口 (走 OpenAPI 网关对外暴露)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcServiceType {

    /** 内部 HTTP 接口. */
    HTTP("HTTP", "HTTP接口"),

    /** 开放平台接口. */
    OPEN_PLATFORM("OPEN_PLATFORM", "开放平台接口");

    private final String code;
    private final String description;

    ZLcServiceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcServiceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcServiceType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}