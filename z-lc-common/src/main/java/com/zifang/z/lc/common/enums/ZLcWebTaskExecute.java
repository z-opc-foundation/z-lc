package com.zifang.z.lc.common.enums;

/**
 * Web 任务执行模式枚举 — 蒸馏自 ace-platform-core
 * {@code WebTaskExecuteEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"Web 任务"执行时选择执行模式:
 *
 * <ul>
 *   <li>{@link #SQL_EXPORT_MODE} — SQL 导出模式 (生成 SQL 脚本)</li>
 *   <li>{@link #MIDDLE_TABLE_MODE} — 中间表模式 (生成中转表 + 数据导入导出)</li>
 *   <li>{@link #CUSTOM_INIT_MODE} — 定制初始化模式 (业务方自定义逻辑)</li>
 *   <li>{@link #DOCUMENT_MODE} — 文档模式 (生成可读文档)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebTaskExecute {

    /** SQL 导出模式. */
    SQL_EXPORT_MODE("sqlExport", "SQL导出模式"),

    /** 中间表模式. */
    MIDDLE_TABLE_MODE("midTable", "中间表模式"),

    /** 定制初始化模式. */
    CUSTOM_INIT_MODE("customInit", "定制初始化"),

    /** 文档模式. */
    DOCUMENT_MODE("document", "文档模式");

    private final String type;
    private final String desc;

    ZLcWebTaskExecute(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebTaskExecute fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebTaskExecute v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}