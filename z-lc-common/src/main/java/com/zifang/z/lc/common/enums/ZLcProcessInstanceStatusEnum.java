package com.zifang.z.lc.common.enums;

/**
 * 流程实例状态枚举 — 蒸馏自 ace-platform-core
 * {@code ProcessInstanceStatusEnum} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于低代码平台展示流程实例的运行状态：
 * <ul>
 *   <li>{@link #FINISHED} — 已结束（完成/拒绝/终止）</li>
 *   <li>{@link #UNFINISHED} — 运行中（至少有一个活跃任务）</li>
 *   <li>{@link #SUSPEND} — 暂停（挂起中）</li>
 *   <li>{@link #UNKNOWN} — 未知状态</li>
 * </ul>
 *
 * <p>对应 BPMN 引擎（Camunda/Flowable）的 ProcessInstance 状态码映射 —
 * 业务方按引擎返回的状态码查表转中文描述.
 *
 * @author zifang
 */
public enum ZLcProcessInstanceStatusEnum {

    /** 已结束. */
    FINISHED(0, "已结束"),

    /** 运行中. */
    UNFINISHED(1, "运行中"),

    /** 暂停. */
    SUSPEND(2, "暂停"),

    /** 未知. */
    UNKNOWN(3, "未知");

    private final Integer code;
    private final String description;

    ZLcProcessInstanceStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按 code 数值查找（未匹配返回 {@link #UNKNOWN}，与 ace 一致）.
     */
    public static ZLcProcessInstanceStatusEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (ZLcProcessInstanceStatusEnum s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return UNKNOWN;
    }

    /**
     * 按 code 数值查找，未匹配返回 null（用于业务方严格校验场景）.
     */
    public static ZLcProcessInstanceStatusEnum fromCodeOrNull(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcProcessInstanceStatusEnum s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }
}
