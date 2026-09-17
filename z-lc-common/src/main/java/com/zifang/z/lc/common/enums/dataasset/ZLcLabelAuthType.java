package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签授权类型枚举 — 蒸馏自 ace-platform-core
 * {@code LabelAuthTypeEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中标识授权类型:
 *
 * <ul>
 *   <li>{@link #VIEW} — 查看权限</li>
 *   <li>{@link #EDIT} — 编辑权限</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcLabelAuthType {

    /** 查看. */
    VIEW(1, "查看"),

    /** 编辑. */
    EDIT(0, "编辑");

    private final Integer code;
    private final String description;

    ZLcLabelAuthType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcLabelAuthType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelAuthType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否可编辑 (含编辑权限). */
    public boolean canEdit() {
        return this == EDIT;
    }

    /** 是否仅查看. */
    public boolean isViewOnly() {
        return this == VIEW;
    }
}