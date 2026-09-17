package com.zifang.z.lc.common.enums;

/**
 * Web 构建记录类型枚举 — 蒸馏自 ace-platform-core
 * {@code WebBuildRecordTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"Web 应用构建"记录的类型:
 *
 * <ul>
 *   <li>{@link #EXPORT} — 导出记录</li>
 *   <li>{@link #IMPORT} — 导入记录</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebBuildRecordType {

    /** 导出. */
    EXPORT("export", "导出"),

    /** 导入. */
    IMPORT("import", "导入");

    private final String type;
    private final String desc;

    ZLcWebBuildRecordType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebBuildRecordType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebBuildRecordType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }

    /** 是否导出. */
    public boolean isExport() {
        return this == EXPORT;
    }

    /** 是否导入. */
    public boolean isImport() {
        return this == IMPORT;
    }
}