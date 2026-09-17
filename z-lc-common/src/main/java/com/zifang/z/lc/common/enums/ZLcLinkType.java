package com.zifang.z.lc.common.enums;

/**
 * 链接类型枚举 — 蒸馏自 ace-platform-core
 * {@code LinkTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"二维码 / 链接"分享场景:
 *
 * <ul>
 *   <li>{@link #QR_CODE} — 二维码</li>
 *   <li>{@link #URL_LINK} — URL 链接</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcLinkType {

    /** 二维码. */
    QR_CODE("qrCode", "二维码"),

    /** URL 链接. */
    URL_LINK("urlLink", "url连接");

    private final String type;
    private final String name;

    ZLcLinkType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ZLcLinkType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcLinkType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}