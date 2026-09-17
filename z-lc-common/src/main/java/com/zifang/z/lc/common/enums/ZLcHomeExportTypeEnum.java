package com.zifang.z.lc.common.enums;

/**
 * 首页导出类型枚举 — 蒸馏自 ace-platform-core
 * {@code HomeExportTypeEnum} ({@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「首页」数据导出操作时区分导出对象：
 * <ul>
 *   <li>{@link #TODO_TASK} — 待办任务导出</li>
 *   <li>{@link #DONE_TASK} — 已办任务导出</li>
 *   <li>{@link #DONE_INITIATE} — 已发起任务导出</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcHomeExportTypeEnum {

    /** 待办任务. */
    TODO_TASK(1, "待办"),

    /** 已办任务. */
    DONE_TASK(2, "已办"),

    /** 已发起任务. */
    DONE_INITIATE(3, "已发");

    private final Integer code;
    private final String value;

    ZLcHomeExportTypeEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static ZLcHomeExportTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcHomeExportTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}