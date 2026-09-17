package com.zifang.z.lc.common.enums;

/**
 * 提交状态枚举 — 蒸馏自 ace-platform-core
 * {@code CommitStatusEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"业务对象 / 版本"的提交状态:
 *
 * <ul>
 *   <li>{@link #DRAFT} — 草稿态 (未提交, 可任意修改)</li>
 *   <li>{@link #COMMIT} — 提交态 (已提交, 需走审批 / 撤回 流程)</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcCommitStatus {

    /** 草稿态. */
    DRAFT(1, "草稿态"),

    /** 提交态. */
    COMMIT(2, "提交态");

    private final Integer status;
    private final String name;

    ZLcCommitStatus(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static ZLcCommitStatus fromStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (ZLcCommitStatus v : values()) {
            if (v.status.equals(status)) {
                return v;
            }
        }
        return null;
    }

    /** 是否已提交. */
    public boolean isCommitted() {
        return this == COMMIT;
    }

    /** 是否草稿. */
    public boolean isDraft() {
        return this == DRAFT;
    }
}