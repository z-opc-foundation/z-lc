package com.zifang.z.lc.common.enums;

/**
 * 备份来源枚举 — 蒸馏自 ace-platform-core
 * {@code BackupOriginEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"版本快照"的产生来源:
 *
 * <ul>
 *   <li>{@link #PACKAGE_IMPORT} — 包导入触发的快照</li>
 *   <li>{@link #PAGE_SAVE} — 页面保存触发的快照</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcBackupOrigin {

    /** 包导入触发. */
    PACKAGE_IMPORT(1, "包导入"),

    /** 页面保存触发. */
    PAGE_SAVE(2, "页面保存");

    private final Integer code;
    private final String name;

    ZLcBackupOrigin(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcBackupOrigin fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcBackupOrigin v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}