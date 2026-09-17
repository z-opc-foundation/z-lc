package com.zifang.z.lc.common.enums;

/**
 * 应用构件类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.AppComponentType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>低代码平台应用的「构件」分类 — 一个应用包含若干构件，对应不同的业务能力.
 *
 * @author zifang
 */
public enum ZLcAppComponentType {

    /** 模型构件. */
    MODEL("model"),

    /** 页面构件. */
    PAGE("page"),

    /** 字典构件. */
    DICT("dict"),

    /** 流程构件. */
    WORKFLOW("workflow"),

    /** 服务构件. */
    SERVICE("service"),

    /** 业务表构件. */
    TABLE("table"),

    /** 模型服务构件（高级查询等）. */
    MODEL_SERVICE("modelService"),

    /** 通用构件. */
    COMMON("common"),

    /** 打印构件. */
    PRINT("print");

    private final String code;

    ZLcAppComponentType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ZLcAppComponentType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcAppComponentType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
