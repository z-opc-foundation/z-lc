package com.zifang.z.lc.common.enums;

/**
 * 监听器发送结果枚举 — 蒸馏自 ace-platform-core
 * {@code ListenerSendResultEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"流程监听器"或"业务监听器"的发送结果判定:
 *
 * <ul>
 *   <li>{@link #SUCCESS} — 监听器执行成功</li>
 *   <li>{@link #FAIL} — 监听器执行失败</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcListenerSendResult {

    /** 成功. */
    SUCCESS(1, "成功"),

    /** 失败. */
    FAIL(0, "失败");

    private final Integer code;
    private final String desc;

    ZLcListenerSendResult(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcListenerSendResult fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcListenerSendResult v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否成功. */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}