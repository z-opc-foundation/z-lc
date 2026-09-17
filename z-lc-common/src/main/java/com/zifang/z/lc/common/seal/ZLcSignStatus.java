package com.zifang.z.lc.common.seal;

/**
 * 签章状态枚举 — 蒸馏自 ace-platform-core
 * {@code SealRecordEnums.SignStatus} ({@code com.c2f.ace.core.seal.support}).
 *
 * <p>用于低代码平台"电子签章"模块标识签章记录的状态:
 *
 * <ul>
 *   <li>{@link #SIGNING} — 签章中</li>
 *   <li>{@link #SUCCESS} — 签章成功</li>
 *   <li>{@link #FAIL} — 签章失败</li>
 *   <li>{@link #INVALID} — 无效签章记录</li>
 * </ul>
 *
 * @author zifang
 */
enum ZLcSignStatus {
    /** 签章中. */
    SIGNING("SIGNING", "签章中"),

    /** 签章成功. */
    SUCCESS("SUCCESS", "签章成功"),

    /** 签章失败. */
    FAIL("FAIL", "签章失败"),

    /** 无效签章记录. */
    INVALID("INVALID", "无效签章记录");

    private final String code;
    private final String description;

    ZLcSignStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcSignStatus ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcSignStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    public static String getDescriptionByCode(String code) {
        ZLcSignStatus v = ofCode(code);
        return v == null ? null : v.description;
    }

    /** 是否终态 (SUCCESS/FAIL/INVALID 视为终态). */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAIL || this == INVALID;
    }

    /** 是否成功. */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}