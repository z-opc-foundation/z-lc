package com.zifang.z.lc.common.enums;

/**
 * 字段关系类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.FieldRelateType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于标注模型字段间的关系：
 * <ul>
 *   <li>{@link #O2O} — 1-1 关系（如「用户 ↔ 身份证」）</li>
 *   <li>{@link #O2L} — 1-N 关系（如「订单 ↔ 订单项」）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcFieldRelateType {

    /** 1-1 关系 */
    O2O("oneToOne"),

    /** 1-N 关系 */
    O2L("oneToMany");

    private final String code;

    ZLcFieldRelateType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ZLcFieldRelateType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcFieldRelateType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
