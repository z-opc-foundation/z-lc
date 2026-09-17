package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;
import java.util.Map;

/**
 * 添加签名 DTO — 蒸馏自 ace-platform-engine {@code AddSignExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：业务方发起签名任务的入参 — 携带流程实例 id、任务 id、签名数据、
 * 员工 id、客户端类型、业务主键等.
 *
 * @author xuhf (distilled by zifang)
 */
public class AddSignExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程实例 id. */
    private String workflowInstanceId;

    /** 任务 id. */
    private String taskId;

    /** 签名数据. */
    private Map<String, Object> signData;

    /** 签名信息（已知签名数据时传入） */
    private ElectronicSignInfoExtendDTO electronicSignInfoDTO;

    /** 员工 id. */
    private Long staffId;

    /** 员工工号. */
    private String jobNumber;

    /** 客户端类型（PC / MOBILE / PAD 等） */
    private String clientType;

    /** 业务主键. */
    private String businessKey;

    /** 页面名称. */
    private String pageName;

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Map<String, Object> getSignData() {
        return signData;
    }

    public void setSignData(Map<String, Object> signData) {
        this.signData = signData;
    }

    public ElectronicSignInfoExtendDTO getElectronicSignInfoDTO() {
        return electronicSignInfoDTO;
    }

    public void setElectronicSignInfoDTO(ElectronicSignInfoExtendDTO electronicSignInfoDTO) {
        this.electronicSignInfoDTO = electronicSignInfoDTO;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
}
