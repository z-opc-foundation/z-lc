package com.zifang.z.lc.common.enums;

/**
 * 应用版本提交类型 — 蒸馏自 ace-platform-core
 * {@code Enums.AppVersionCommitType} ({@code com.c2f.ace.core.common}).
 *
 * <p>用于标识低代码平台中"应用版本"在 git 仓库或包管理系统中的变更类型.
 *
 * <ul>
 *   <li>{@link #CREATE} — 新增版本</li>
 *   <li>{@link #REMOVE} — 删除版本</li>
 *   <li>{@link #UPDATE} — 更新版本</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcAppVersionCommitType {

    /** 新增. */
    CREATE("create"),

    /** 删除. */
    REMOVE("remove"),

    /** 更新. */
    UPDATE("update");

    private final String type;

    ZLcAppVersionCommitType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ZLcAppVersionCommitType fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcAppVersionCommitType v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }
}