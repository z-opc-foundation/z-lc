package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签类型枚举 — 蒸馏自 ace-platform-core
 * {@code LabelTypeEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中区分标签来源:
 *
 * <ul>
 *   <li>{@link #IMPORT} — 导入标签 (外部数据源导入)</li>
 *   <li>{@link #BUSINESS} — 业务标签 (业务方手动创建 / 系统生成)</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcLabelType {

    /** 导入标签. */
    IMPORT(1, "导入"),

    /** 业务标签. */
    BUSINESS(2, "业务");

    private final Integer code;
    private final String name;

    ZLcLabelType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcLabelType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否导入. */
    public boolean isImport() {
        return this == IMPORT;
    }

    /** 是否业务. */
    public boolean isBusiness() {
        return this == BUSINESS;
    }
}