package com.zifang.z.lc.common.enums;

/**
 * 客户端类型枚举 — 蒸馏自 ace-platform-core
 * {@code ClientTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台访问来源识别 / 多端适配:
 *
 * <ul>
 *   <li>{@link #PC} — 电脑端浏览器访问</li>
 *   <li>{@link #MOBILE} — 移动端 H5 / App 访问</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcClientType {

    /** 电脑端. */
    PC("pc", "电脑端"),

    /** 移动端. */
    MOBILE("mobile", "移动端");

    private final String type;
    private final String name;

    ZLcClientType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ZLcClientType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcClientType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}