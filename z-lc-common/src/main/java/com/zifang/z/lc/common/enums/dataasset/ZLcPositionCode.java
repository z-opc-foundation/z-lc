package com.zifang.z.lc.common.enums.dataasset;

/**
 * 岗位编码枚举 — 蒸馏自 ace-platform-core
 * {@code PositionCodeEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中标识各种岗位编码:
 *
 * <ul>
 *   <li>{@link #LABEL_AUTHORIZATION_POSITION} — 标签授权岗位</li>
 *   <li>{@link #LABEL_EDIT_POSITION} — 标签编辑岗位</li>
 *   <li>{@link #LABEL_VIEW_POSITION} — 标签查看岗位</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcPositionCode {

    /** 标签授权岗位. */
    LABEL_AUTHORIZATION_POSITION("label_authorization_position", "标签授权岗位"),

    /** 标签编辑岗位. */
    LABEL_EDIT_POSITION("label_edit_position", "标签编辑岗位"),

    /** 标签查看岗位. */
    LABEL_VIEW_POSITION("label_view_position", "标签查看岗位");

    private final String code;
    private final String message;

    ZLcPositionCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ZLcPositionCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcPositionCode v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}