package com.zifang.z.lc.common.enums;

import java.util.Objects;

/**
 * 数据操作类型枚举 — 蒸馏自 ace-platform-core
 * {@code ActionTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台标注模型数据的变更类型 — 审计 / 数据变更记录 / 同步下游等场景.
 *
 * @author zifang
 */
public enum ZLcActionTypeEnum {

    /** 新增. */
    CREATE("create", "新增"),

    /** 修改. */
    UPDATE("update", "修改"),

    /** 删除. */
    DELETE("delete", "删除");

    private final String actionType;
    private final String actionTypeName;

    ZLcActionTypeEnum(String actionType, String actionTypeName) {
        this.actionType = actionType;
        this.actionTypeName = actionTypeName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getActionTypeName() {
        return actionTypeName;
    }

    /**
     * 按 actionType 反查中文名（未匹配返回 null）.
     */
    public static String getNameByType(String actionType) {
        for (ZLcActionTypeEnum value : values()) {
            if (Objects.equals(value.getActionType(), actionType)) {
                return value.getActionTypeName();
            }
        }
        return null;
    }

    public static ZLcActionTypeEnum fromActionType(String actionType) {
        if (actionType == null) {
            return null;
        }
        for (ZLcActionTypeEnum v : values()) {
            if (v.actionType.equals(actionType)) {
                return v;
            }
        }
        return null;
    }
}
