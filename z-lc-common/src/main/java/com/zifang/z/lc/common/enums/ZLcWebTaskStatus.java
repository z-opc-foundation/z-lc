package com.zifang.z.lc.common.enums;

/**
 * Web 任务状态枚举 — 蒸馏自 ace-platform-core
 * {@code WebTaskStatusEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"Web 任务"在生命周期中的状态:
 *
 * <ul>
 *   <li>{@link #WAIT_EXECUTE} — 待执行</li>
 *   <li>{@link #EXECUTED} — 已执行</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebTaskStatus {

    /** 待执行. */
    WAIT_EXECUTE("wait", "待执行"),

    /** 已执行. */
    EXECUTED("executed", "已执行");

    private final String type;
    private final String desc;

    ZLcWebTaskStatus(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebTaskStatus fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebTaskStatus v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }

    /** 是否已执行 (终态). */
    public boolean isExecuted() {
        return this == EXECUTED;
    }
}