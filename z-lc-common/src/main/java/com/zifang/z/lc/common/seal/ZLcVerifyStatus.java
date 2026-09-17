package com.zifang.z.lc.common.seal;

/**
 * 验章状态枚举 — 蒸馏自 ace-platform-core
 * {@code SealRecordEnums.VerifyStatus} ({@code com.c2f.ace.core.seal.support}).
 *
 * <p>用于低代码平台"电子签章验章"模块标识验章状态:
 *
 * <ul>
 *   <li>{@link #UNVERIFIED} — 未验章</li>
 *   <li>{@link #VERIFYING} — 验章中</li>
 *   <li>{@link #SUCCESS} — 验章成功</li>
 *   <li>{@link #FAIL} — 验章失败</li>
 * </ul>
 *
 * @author zifang
 */
enum ZLcVerifyStatus {
    /** 未验章. */
    UNVERIFIED("UNVERIFIED", "未验章"),

    /** 验章中. */
    VERIFYING("VERIFYING", "验章中"),

    /** 验章成功. */
    SUCCESS("SUCCESS", "验章成功"),

    /** 验章失败. */
    FAIL("FAIL", "验章失败");

    private final String code;
    private final String description;

    ZLcVerifyStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcVerifyStatus ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcVerifyStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    public static String getDescriptionByCode(String code) {
        ZLcVerifyStatus v = ofCode(code);
        return v == null ? null : v.description;
    }

    /** 是否终态 (SUCCESS/FAIL 视为终态). */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAIL;
    }

    /** 是否验章成功. */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}