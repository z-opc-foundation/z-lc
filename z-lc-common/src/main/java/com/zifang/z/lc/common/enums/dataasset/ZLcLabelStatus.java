package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签状态枚举 — 蒸馏自 ace-platform-core
 * {@code LabelStatusEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中标签的启用状态:
 *
 * <ul>
 *   <li>{@link #ENABLE} — 启用</li>
 *   <li>{@link #UN_ENABLE} — 停用</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcLabelStatus {

    /** 启用. */
    ENABLE(1, "启用"),

    /** 停用. */
    UN_ENABLE(0, "停用");

    private final Integer code;
    private final String description;

    ZLcLabelStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcLabelStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelStatus v : values()) {
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