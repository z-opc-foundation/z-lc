package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 页面 DTO (V2 页面域, lead 013 §四: Page 一等公民实体)。
 * <p>
 * schema 即 z-frontend-render 组件树 JSON: {version, datasets, layout, widgets}，
 * 渲染产物契约与报表域 RenderData 逐字段对齐（见 z-frontend-render contract/view.ts）。
 */
public class PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String appCode;

    private String pageCode;

    private String pageName;

    /** 页面 schema JSON（z-frontend-render 组件树 + FORM 绑定实体） */
    private String schema;

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
