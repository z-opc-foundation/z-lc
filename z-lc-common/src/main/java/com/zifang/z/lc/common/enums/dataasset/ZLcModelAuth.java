package com.zifang.z.lc.common.enums.dataasset;

/**
 * 模型权限枚举 — 蒸馏自 ace-platform-core
 * {@code ModelAuthEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产"模块中标识模型权限岗位:
 *
 * <ul>
 *   <li>{@link #DATA_ASSET_MODEL_AUTH_POSITION} — 模型权限管理岗位</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcModelAuth {

    /** 模型权限管理岗位. */
    DATA_ASSET_MODEL_AUTH_POSITION("data_asset_model_auth_position", "模型权限管理岗位");

    private final String code;
    private final String message;

    ZLcModelAuth(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ZLcModelAuth fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcModelAuth v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}