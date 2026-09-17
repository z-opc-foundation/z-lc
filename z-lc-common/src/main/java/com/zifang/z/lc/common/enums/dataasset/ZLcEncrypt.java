package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产是否启用加密枚举 — 蒸馏自 ace-platform-core
 * {@code EncryptEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于标识低代码平台"数据资产"中字段是否启用加密处理 (存储加密):
 *
 * <ul>
 *   <li>{@link #ENABLE} — 启用加密 (true 是)</li>
 *   <li>{@link #UN_ENABLE} — 不启用 (false 否)</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcEncrypt {

    /** 启用. */
    ENABLE(true, "是"),

    /** 不启用. */
    UN_ENABLE(false, "否");

    private final Boolean code;
    private final String description;

    ZLcEncrypt(Boolean code, String description) {
        this.code = code;
        this.description = description;
    }

    public Boolean getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcEncrypt fromCode(Boolean code) {
        if (code == null) {
            return null;
        }
        for (ZLcEncrypt v : values()) {
            if (v.code == code) {
                return v;
            }
        }
        return null;
    }

    /** 是否启用. */
    public boolean isEnabled() {
        return this == ENABLE;
    }
}