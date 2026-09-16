package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 视图配置创建请求 DTO (F035 T5: View Config)
 */
public class ViewConfigCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String entityCode;

    private String appCode;

    /**
     * LIST / FORM / DETAIL
     */
    private String viewType;

    /**
     * 视图配置 JSON
     */
    private String config;

    private String tenantCode;

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
