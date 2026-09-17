package com.zifang.z.lc.common.seal;

/**
 * 印章类型枚举 — 蒸馏自 ace-platform-core
 * {@code SealRecordEnums.SealType} ({@code com.c2f.ace.core.seal.support}).
 *
 * <p>用于低代码平台"电子签章"模块标识印章种类:
 *
 * <ul>
 *   <li>{@link #ACROSS} — 骑缝章</li>
 *   <li>{@link #OFFICIAL} — 普通章</li>
 * </ul>
 *
 * @author zifang
 */
enum ZLcSealType {
    /** 骑缝章. */
    ACROSS("ACROSS", "骑缝章"),

    /** 普通章. */
    OFFICIAL("OFFICIAL", "普通章");

    private final String code;
    private final String description;

    ZLcSealType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据 code 反查 (大小写不敏感 + trim). */
    public static ZLcSealType ofCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String trimmed = code.trim();
        for (ZLcSealType v : values()) {
            if (v.code.equalsIgnoreCase(trimmed)) {
                return v;
            }
        }
        return null;
    }

    /** 根据 code 反查中文描述. */
    public static String getDescriptionByCode(String code) {
        ZLcSealType v = ofCode(code);
        return v == null ? null : v.description;
    }
}