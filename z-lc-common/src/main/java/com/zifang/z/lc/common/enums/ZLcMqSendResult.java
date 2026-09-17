package com.zifang.z.lc.common.enums;

/**
 * MQ 发送结果枚举 — 蒸馏自 ace-platform-core
 * {@code MqSendResultEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台异步消息发送回调的结果判定:
 *
 * <ul>
 *   <li>{@link #SUCCESS} — 发送成功 (Broker 已 ack)</li>
 *   <li>{@link #FAIL} — 发送失败 (网络异常 / Broker 拒绝 / 超时 等)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcMqSendResult {

    /** 发送成功. */
    SUCCESS(1, "成功"),

    /** 发送失败. */
    FAIL(0, "失败");

    private final Integer code;
    private final String desc;

    ZLcMqSendResult(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcMqSendResult fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcMqSendResult v : values()) {
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