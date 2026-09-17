package com.zifang.z.lc.common.enums;

/**
 * 作业状态枚举 — 蒸馏自 ace-platform-core
 * {@code JobStateEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台调度作业 (定时任务 / 异步任务) 的运行状态:
 *
 * <ul>
 *   <li>{@link #WAITING} — 等待执行</li>
 *   <li>{@link #EXECUTING} — 执行中</li>
 *   <li>{@link #FINISH} — 执行完成</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcJobState {

    /** 等待执行. */
    WAITING(0, "等待执行"),

    /** 执行中. */
    EXECUTING(1, "执行中"),

    /** 执行完成. */
    FINISH(2, "执行完成");

    private final Integer code;
    private final String description;

    ZLcJobState(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcJobState fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcJobState v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (FINISH 视为终态). */
    public boolean isTerminal() {
        return this == FINISH;
    }

    /** 是否运行中 (WAITING / EXECUTING). */
    public boolean isRunning() {
        return this == WAITING || this == EXECUTING;
    }
}