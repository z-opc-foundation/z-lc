package com.zifang.z.lc.common.enums;

/**
 * Web 任务类型枚举 — 蒸馏自 ace-platform-core
 * {@code WebTaskTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"Web 任务"的种类:
 *
 * <ul>
 *   <li>{@link #APP_RELEASE} — 应用发布</li>
 *   <li>{@link #PERMISSION_CONFIG} — 权限配置</li>
 *   <li>{@link #ACE_CONFIG} — 简易搭配置</li>
 *   <li>{@link #DATA_INIT} — 数据初始化</li>
 *   <li>{@link #OTHER} — 其他</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebTaskType {

    /** 应用发布. */
    APP_RELEASE("appRelease", "应用发布"),

    /** 权限配置. */
    PERMISSION_CONFIG("permConfig", "权限配置"),

    /** 简易搭配置. */
    ACE_CONFIG("aceConfig", "简易搭配置"),

    /** 数据初始化. */
    DATA_INIT("dataInit", "数据初始化"),

    /** 其他. */
    OTHER("other", "其他");

    private final String type;
    private final String desc;

    ZLcWebTaskType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebTaskType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebTaskType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}