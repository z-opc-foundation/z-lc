package com.zifang.z.lc.common.param;

/**
 * 调度任务参数 DTO — 蒸馏自 ace-platform-core
 * {@code JobParam} ({@code com.c2f.ace.core.middleware.param}).
 *
 * <p>用于低代码平台"调度任务 / 异步任务"模块 — 任务执行时的上下文参数.
 * 通常作为 MQ 消息体 / 定时任务 trigger data 在调度链路中传递.
 *
 * <p>字段语义:
 * <ul>
 *   <li>{@code taskCode} — 任务编码 (用于关联具体任务定义)</li>
 *   <li>{@code orgId} — 机构 ID</li>
 *   <li>{@code staffId} — 操作员 ID</li>
 *   <li>{@code jobAccountNo} — 任务账号 (用于多账号隔离)</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcJobParam {

    /** 任务编码. */
    private String taskCode;

    /** 机构 ID. */
    private Long orgId;

    /** 操作员 ID. */
    private Long staffId;

    /** 任务账号. */
    private String jobAccountNo;

    public ZLcJobParam() {
    }

    public ZLcJobParam(String taskCode, Long orgId, Long staffId, String jobAccountNo) {
        this.taskCode = taskCode;
        this.orgId = orgId;
        this.staffId = staffId;
        this.jobAccountNo = jobAccountNo;
    }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public String getJobAccountNo() { return jobAccountNo; }
    public void setJobAccountNo(String jobAccountNo) { this.jobAccountNo = jobAccountNo; }
}