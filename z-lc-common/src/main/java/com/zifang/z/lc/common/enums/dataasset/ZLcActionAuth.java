package com.zifang.z.lc.common.enums.dataasset;

/**
 * 数据资产动作权限枚举 — 蒸馏自 ace-platform-core
 * {@code ActionAuthEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于标识低代码平台"数据资产"模块对字段可执行的动作权限:
 *
 * <ul>
 *   <li>{@link #DECRYPT} — 解密动作</li>
 *   <li>{@link #DESENSITIZE} — 脱敏动作</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcActionAuth {

    /** 解密. */
    DECRYPT("decrypt", "解密"),

    /** 脱敏. */
    DESENSITIZE("desensitize", "脱敏");

    private final String actionType;
    private final String description;

    ZLcActionAuth(String actionType, String description) {
        this.actionType = actionType;
        this.description = description;
    }

    public String getActionType() {
        return actionType;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcActionAuth fromActionType(String actionType) {
        if (actionType == null) {
            return null;
        }
        for (ZLcActionAuth v : values()) {
            if (v.actionType.equals(actionType)) {
                return v;
            }
        }
        return null;
    }
}