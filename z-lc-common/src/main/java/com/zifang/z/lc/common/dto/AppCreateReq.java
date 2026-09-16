package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 应用创建请求 DTO (F035 T1)
 */
public class AppCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String appCode;
    private String appName;
    private String description;
    private String icon;

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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
