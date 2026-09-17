package com.zifang.z.lc.common.enums;

/**
 * 流程任务成员类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.IdentityLinkType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>对应 Camunda/Flowable 的 IdentityLink 类型常量，用于：
 * <ul>
 *   <li>{@link #ASSIGNEE} — 任务当前被分配人</li>
 *   <li>{@link #CANDIDATE} — 任务候选候选人（可领取）</li>
 *   <li>{@link #OWNER} — 任务所有者（与 Assignee 不同）</li>
 *   <li>{@link #STARTER} — 流程发起人</li>
 *   <li>{@link #PARTICIPANT} — 流程历史参与人</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcIdentityLinkType {

    /** 任务当前被分配人. */
    public static final String ASSIGNEE = "assignee";

    /** 任务候选候选人（可领取） */
    public static final String CANDIDATE = "candidate";

    /** 任务所有者（与 Assignee 不同） */
    public static final String OWNER = "owner";

    /** 流程发起人. */
    public static final String STARTER = "starter";

    /** 流程历史参与人. */
    public static final String PARTICIPANT = "participant";

    private ZLcIdentityLinkType() {
        // 常量类，禁止实例化
    }
}
