package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签授权对象类型枚举 — 蒸馏自 ace-platform-core
 * {@code LabelAuthObjectTypeEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中标识授权对象种类:
 *
 * <ul>
 *   <li>{@link #STAFF} — 人员</li>
 *   <li>{@link #DEPT} — 部门</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcLabelAuthObjectType {

    /** 人员. */
    STAFF(1, "人员"),

    /** 部门. */
    DEPT(2, "部门");

    private final Integer code;
    private final String description;

    ZLcLabelAuthObjectType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcLabelAuthObjectType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelAuthObjectType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}