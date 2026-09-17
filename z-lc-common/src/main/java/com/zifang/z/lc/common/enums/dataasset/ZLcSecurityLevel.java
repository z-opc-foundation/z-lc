package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产安全等级枚举 — 蒸馏自 ace-platform-core
 * {@code SecurityLevelEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产"安全等级 (L1~L5, 公开范围由大到小):
 *
 * <ul>
 *   <li>{@link #L1} — L1 完全公开</li>
 *   <li>{@link #L2} — L2 较大范围公开</li>
 *   <li>{@link #L3} — L3 中等范围公开</li>
 *   <li>{@link #L4} — L4 较小范围公开</li>
 *   <li>{@link #L5} — L5 极小范围公开</li>
 * </ul>
 *
 * <p>注意：ace 原代码字段值复用了 {@link ZLcAuthTime} 的 code (one_week/one_month 等),
 * 蒸馏版按业务语义保留原值, 业务方反查时按字符串匹配.
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcSecurityLevel {

    /** L1 完全公开. */
    L1("one_week", "L1(完全公开)"),

    /** L2 较大范围公开. */
    L2("one_month", "L2(较大范围公开)"),

    /** L3 中等范围公开. */
    L3("three_month", "L3(中等范围公开)"),

    /** L4 较小范围公开. */
    L4("six_month", "L4(较小范围公开)"),

    /** L5 极小范围公开. */
    L5("one_year", "L5(极小范围公开)");

    private final String code;
    private final String description;

    ZLcSecurityLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcSecurityLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcSecurityLevel v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 获取安全等级数值 (1~5). */
    public int getLevel() {
        return ordinal() + 1;
    }
}