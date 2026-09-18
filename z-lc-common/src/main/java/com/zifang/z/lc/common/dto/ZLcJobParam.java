package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 任务执行参数 — 蒸馏自 ace-platform-core
 * {@code JobParam} ({@code com.c2f.ace.core.middleware.param}).
 *
 * <p>封装异步任务执行时所需的上下文参数: 任务编码、组织 ID、员工 ID、账号等.
 * 蒸馏时移除了 ace 对 Lombok @Data 的依赖, 改为手写 getter/setter.
 *
 * @author zifang
 */
public class ZLcJobParam implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskCode;
    private Long orgId;
    private Long staffId;
    private String jobAccountNo;

    public ZLcJobParam() {
    }

    public ZLcJobParam(String taskCode, Long orgId, Long staffId, String jobAccountNo) {
        this.taskCode = taskCode;
        this.orgId = orgId;
        this.staffId = staffId;
        this.jobAccountNo = jobAccountNo;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getJobAccountNo() {
        return jobAccountNo;
    }

    public void setJobAccountNo(String jobAccountNo) {
        this.jobAccountNo = jobAccountNo;
    }
}
