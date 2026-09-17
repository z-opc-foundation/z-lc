package com.zifang.z.lc.common.enums;

/**
 * 监听器类型枚举 — 蒸馏自 ace-platform-core
 * {@code ListenerTypeEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台中"监听器"的种类:
 *
 * <ul>
 *   <li>{@link #SUCCESS} — 成功监听器 (任务完成后触发)</li>
 *   <li>{@link #FAIL} — 失败监听器 (任务失败时触发)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcListenerType {

    /** 成功监听器. */
    SUCCESS(1, "成功"),

    /** 失败监听器. */
    FAIL(0, "失败");

    private final Integer code;
    private final String desc;

    ZLcListenerType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcListenerType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcListenerType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}