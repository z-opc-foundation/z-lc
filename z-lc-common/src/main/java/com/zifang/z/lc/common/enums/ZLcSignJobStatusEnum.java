package com.zifang.z.lc.common.enums;

/**
 * 签任务状态枚举 — 蒸馏自 ace-platform-core
 * {@code SignJobStatusEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台电子签功能 — 签任务生命周期状态展示:
 * <ul>
 *   <li>{@link #UNSIGN} — 待签（已发起未签）</li>
 *   <li>{@link #FINISH} — 已签（签任务完成）</li>
 *   <li>{@link #EXPIRE} — 过期（签任务超时）</li>
 *   <li>{@link #REVOKE} — 被服务端撤销</li>
 *   <li>{@link #REFUSE} — 被客户端拒绝</li>
 * </ul>
 *
 * <p>对应 {@code ElectronicSignInfoExtendDTO.signStatus} 字段.
 *
 * @author zifang
 */
public enum ZLcSignJobStatusEnum {

    /** 待签. */
    UNSIGN("UNSIGN", "待签"),

    /** 已签. */
    FINISH("FINISH", "已签"),

    /** 过期. */
    EXPIRE("EXPIRE", "过期"),

    /** 签名任务被服务端撤销. */
    REVOKE("REVOKE", "签名任务被服务端撤销"),

    /** 签名任务被客户端拒绝. */
    REFUSE("REFUSE", "签名任务被客户端拒绝");

    private final String status;
    private final String statusName;

    ZLcSignJobStatusEnum(String status, String statusName) {
        this.status = status;
        this.statusName = statusName;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusName() {
        return statusName;
    }

    public static ZLcSignJobStatusEnum fromStatus(String status) {
        if (status == null) {
            return null;
        }
        for (ZLcSignJobStatusEnum v : values()) {
            if (v.status.equals(status)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 是否已完成（FINISH / EXPIRE / REVOKE / REFUSE 视为终态）.
     */
    public boolean isCompleted() {
        return this != UNSIGN;
    }
}
