package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产安全分类枚举 — 蒸馏自 ace-platform-core
 * {@code SecurityCategoryEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产"安全分级分类 (C1~C6):
 *
 * <ul>
 *   <li>{@link #C1} — 个人属性数据</li>
 *   <li>{@link #C2} — 健康状况数据</li>
 *   <li>{@link #C3} — 医疗应用数据</li>
 *   <li>{@link #C4} — 医疗支付数据</li>
 *   <li>{@link #C5} — 卫生资源数据</li>
 *   <li>{@link #C6} — 公共卫生数据</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcSecurityCategory {

    /** 个人属性数据. */
    C1("C1", "个人属性数据"),

    /** 健康状况数据. */
    C2("C2", "健康状况数据"),

    /** 医疗应用数据. */
    C3("C3", "医疗应用数据"),

    /** 医疗支付数据. */
    C4("C4", "医疗支付数据"),

    /** 卫生资源数据. */
    C5("C5", "卫生资源数据"),

    /** 公共卫生数据. */
    C6("C6", "公共卫生数据");

    private final String code;
    private final String description;

    ZLcSecurityCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcSecurityCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcSecurityCategory v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}