package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产生效方式枚举 — 蒸馏自 ace-platform-core
 * {@code EffectTypeStatusEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于标识低代码平台"数据资产"安全策略的生效方式:
 *
 * <ul>
 *   <li>{@link #MODEL} — 按模型属性生效</li>
 *   <li>{@link #SECURITY_CATEGORY_AND_LEVEL} — 按分级分类生效</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcEffectTypeStatus {

    /** 按模型属性. */
    MODEL("1", "按模型属性"),

    /** 按分级分类. */
    SECURITY_CATEGORY_AND_LEVEL("2", "按分级分类");

    private final String code;
    private final String description;

    ZLcEffectTypeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcEffectTypeStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcEffectTypeStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}