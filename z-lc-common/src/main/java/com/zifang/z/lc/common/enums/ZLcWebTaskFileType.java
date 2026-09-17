package com.zifang.z.lc.common.enums;

/**
 * Web 任务文件类型枚举 — 蒸馏自 ace-platform-core
 * {@code WebTaskFileType} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"Web 任务"上传/下载时的文件类型:
 *
 * <ul>
 *   <li>{@link #SQL_FILE} — SQL 文件</li>
 *   <li>{@link #EXCEL_FILE} — EXCEL 文件</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebTaskFileType {

    /** SQL 文件. */
    SQL_FILE("SQL", "SQL文件"),

    /** EXCEL 文件. */
    EXCEL_FILE("EXCEL", "EXCEL文件");

    private final String type;
    private final String desc;

    ZLcWebTaskFileType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebTaskFileType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebTaskFileType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}