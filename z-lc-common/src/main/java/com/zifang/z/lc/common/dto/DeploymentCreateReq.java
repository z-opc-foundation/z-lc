package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 部署触发请求 DTO (F035 T7: Deployment)
 */
public class DeploymentCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appCode;

    private Long materializationId;

    /**
     * HOT_LOAD / DOCKER / GIT_PUSH
     */
    private String deployType;

    private String version;

    private String tenantCode;

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
}
