package com.zifang.z.lc.common.enums;

/**
 * 应用初始化包记录类型枚举 — 蒸馏自 ace-platform-core
 * {@code InitPackageRecordTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「应用初始化包」的导入/导出记录区分.
 *
 * @author zifang
 */
public enum ZLcInitPackageRecordTypeEnum {

    /** 应用初始化包导出. */
    APP_INIT_PACKAGE_EXPORT("export", "应用初始化包导出"),

    /** 应用初始化包导入. */
    APP_INIT_PACKAGE_IMPORT("import", "应用初始化包导入");

    private final String code;
    private final String desc;

    ZLcInitPackageRecordTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcInitPackageRecordTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcInitPackageRecordTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 判断给定 code 是否为导出操作.
     */
    public static boolean isExport(String code) {
        return APP_INIT_PACKAGE_EXPORT.code.equals(code);
    }

    /**
     * 判断给定 code 是否为导入操作.
     */
    public static boolean isImport(String code) {
        return APP_INIT_PACKAGE_IMPORT.code.equals(code);
    }
}
