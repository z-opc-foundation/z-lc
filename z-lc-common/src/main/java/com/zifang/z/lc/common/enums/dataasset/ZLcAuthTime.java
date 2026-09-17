package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产授权时长枚举 — 蒸馏自 ace-platform-core
 * {@code AuthTimeEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产"模块中字段授权的有效时长:
 *
 * <ul>
 *   <li>{@link #ONE_WEEK} — 七天</li>
 *   <li>{@link #ONE_MONTH} — 一个月</li>
 *   <li>{@link #THREE_MONTH} — 一个季度</li>
 *   <li>{@link #SIX_MONTH} — 半年</li>
 *   <li>{@link #ONE_YEAR} — 一年</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcAuthTime {

    /** 七天. */
    ONE_WEEK("one_week", "七天"),

    /** 一个月. */
    ONE_MONTH("one_month", "一个月"),

    /** 一个季度. */
    THREE_MONTH("three_month", "一个季度"),

    /** 半年. */
    SIX_MONTH("six_month", "半年"),

    /** 一年. */
    ONE_YEAR("one_year", "一年");

    private final String code;
    private final String description;

    ZLcAuthTime(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcAuthTime fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcAuthTime v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}