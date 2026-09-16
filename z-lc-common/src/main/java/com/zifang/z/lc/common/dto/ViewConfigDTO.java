package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 视图配置 DTO (F035 T5: View Config)
 */
public class ViewConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

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

    private Date createTime;

    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
