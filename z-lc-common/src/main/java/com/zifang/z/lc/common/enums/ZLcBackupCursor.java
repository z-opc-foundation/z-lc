package com.zifang.z.lc.common.enums;

/**
 * 备份游标枚举 — 蒸馏自 ace-platform-core
 * {@code BackupCursorEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"版本快照 / 历史回溯"场景下某条记录是否为当前激活版本:
 *
 * <ul>
 *   <li>{@link #NOT_CURRENT} — 非当前 (历史快照)</li>
 *   <li>{@link #CURRENT} — 当前 (最新激活版本)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcBackupCursor {

    /** 非当前. */
    NOT_CURRENT(0, "非当前"),

    /** 当前. */
    CURRENT(1, "当前");

    private final Integer code;
    private final String name;

    ZLcBackupCursor(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcBackupCursor fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcBackupCursor v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否当前激活版本. */
    public boolean isCurrent() {
        return this == CURRENT;
    }
}