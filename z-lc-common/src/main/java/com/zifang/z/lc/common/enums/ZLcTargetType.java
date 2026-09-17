package com.zifang.z.lc.common.enums;

/**
 * 命名转换目标类型枚举 — 蒸馏自 ace-platform-core
 * {@code TargetTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"模型生成代码"时的命名风格选择:
 *
 * <ul>
 *   <li>{@link #CAMEL_CASE} — 驼峰命名 (userName)</li>
 *   <li>{@link #UNDERLINE} — 下划线命名 (user_name)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcTargetType {

    /** 驼峰命名. */
    CAMEL_CASE("camelCase", "驼峰"),

    /** 下划线命名. */
    UNDERLINE("underline", "下划线");

    private final String code;
    private final String description;

    ZLcTargetType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcTargetType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcTargetType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}