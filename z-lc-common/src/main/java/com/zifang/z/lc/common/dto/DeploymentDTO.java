package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 部署 DTO (F035 T7: Deployment)
 */
public class DeploymentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String appCode;

    private Long materializationId;

    /**
     * HOT_LOAD / DOCKER / GIT_PUSH
     */
    private String deployType;

    /**
     * PENDING / RUNNING / SUCCESS / FAILED
     */
    private String status;

    private String deployLog;

    private String version;

    private String tenantCode;

    private Date createTime;

    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public Long getMaterializationId() {
        return materializationId;
    }

    public void setMaterializationId(Long materializationId) {
        this.materializationId = materializationId;
    }

    public String getDeployType() {
        return deployType;
    }

    public void setDeployType(String deployType) {
        this.deployType = deployType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeployLog() {
        return deployLog;
    }

    public void setDeployLog(String deployLog) {
        this.deployLog = deployLog;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
