package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产是否启用解密枚举 — 蒸馏自 ace-platform-core
 * {@code DecryptEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于标识低代码平台"数据资产"中字段是否启用解密处理:
 *
 * <ul>
 *   <li>{@link #ENABLE} — 启用解密 (1 是)</li>
 *   <li>{@link #UN_ENABLE} — 不启用 (0 否)</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcDecrypt {

    /** 启用. */
    ENABLE(1, "是"),

    /** 不启用. */
    UN_ENABLE(0, "否");

    private final Integer code;
    private final String description;

    ZLcDecrypt(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcDecrypt fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcDecrypt v : values()) {
            if (v.code.equals(code)) {
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