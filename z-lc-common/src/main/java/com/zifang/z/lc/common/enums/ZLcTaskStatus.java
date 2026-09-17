package com.zifang.z.lc.common.enums;

/**
 * 任务状态枚举 — 蒸馏自 ace-platform-core
 * {@code TaskStatusEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"任务"管理 — 标识任务在生命周期中的状态:
 *
 * <ul>
 *   <li>{@link #UNDO} — 未开始</li>
 *   <li>{@link #DOING} — 进行中</li>
 *   <li>{@link #DONE} — 已完成</li>
 *   <li>{@link #ERROR} — 异常终止</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcTaskStatus {

    /** 未开始. */
    UNDO("undo", "未开始"),

    /** 进行中. */
    DOING("doing", "进行中"),

    /** 已完成. */
    DONE("done", "已完成"),

    /** 异常终止. */
    ERROR("error", "异常节点");

    private final String code;
    private final String desc;

    ZLcTaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcTaskStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcTaskStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (DONE/ERROR). */
    public boolean isTerminal() {
        return this == DONE || this == ERROR;
    }

    /** 是否进行中 (UNDO/DOING). */
    public boolean isRunning() {
        return this == UNDO || this == DOING;
    }
}