package com.zifang.z.lc.common.enums;

/**
 * 备份发布状态枚举 — 蒸馏自 ace-platform-core
 * {@code BackupReleaseEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"版本快照"的发布状态:
 *
 * <ul>
 *   <li>{@link #TEMP_SAVE} — 仅保存未发布</li>
 *   <li>{@link #RELEASE} — 当前已发布</li>
 *   <li>{@link #HIST_RELEASE} — 历史已发布 (当前非激活)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcBackupRelease {

    /** 仅保存未发布. */
    TEMP_SAVE(0, "仅保存未发布"),

    /** 当前已发布. */
    RELEASE(1, "已发布"),

    /** 历史已发布. */
    HIST_RELEASE(2, "历史已发布");

    private final Integer code;
    private final String name;

    ZLcBackupRelease(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcBackupRelease fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcBackupRelease v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否已发布 (含历史已发布). */
    public boolean isReleased() {
        return this == RELEASE || this == HIST_RELEASE;
    }
}