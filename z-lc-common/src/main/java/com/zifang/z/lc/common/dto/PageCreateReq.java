package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 页面创建请求 DTO (V2 页面域)。
 */
public class PageCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appCode;

    private String pageCode;

    private String pageName;

    /** 页面 schema JSON（保存前经 PageSchemaValidator 校验） */
    private String schema;

    private String tenantCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
