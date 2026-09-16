package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 运行时 get/delete by id 请求
 */
public class RuntimeByIdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String appCode;
    private String entityCode;
    private Long id;
    private String parentEventId;   // delete 时用于冲突校验

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParentEventId() {
        return parentEventId;
    }

    public void setParentEventId(String parentEventId) {
        this.parentEventId = parentEventId;
    }
}
