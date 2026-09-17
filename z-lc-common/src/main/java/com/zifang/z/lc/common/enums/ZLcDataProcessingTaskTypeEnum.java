package com.zifang.z.lc.common.enums;

/**
 * 数据处理任务类型枚举 — 蒸馏自 ace-platform-core
 * {@code DataProcessingTaskTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「数据导入/导出」任务的类型分类.
 *
 * @author zifang
 */
public enum ZLcDataProcessingTaskTypeEnum {

    /** 数据导入任务. */
    IMPORT_DATA_TASK(1, "数据导入任务"),

    /** 数据导出任务. */
    EXPORT_DATA_TASK(2, "数据导出任务");

    private final Integer code;
    private final String value;

    ZLcDataProcessingTaskTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcDataProcessingTaskTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcDataProcessingTaskTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
