package com.zifang.z.lc.common.enums;

/**
 * 应用包类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.AppPackageType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于应用包导入/导出操作的方向标识.
 *
 * @author zifang
 */
public enum ZLcAppPackageType {

    /** 应用包导入（从外部 zip 等格式加载） */
    IMPORT("import"),

    /** 应用包导出（生成 zip 等格式供备份/迁移） */
    OUTPUT("output");

    private final String code;

    ZLcAppPackageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ZLcAppPackageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcAppPackageType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
