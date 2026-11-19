package com.zifang.z.lc.core.page.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_page 表实体 (V2 页面域: 页面一等公民, lead 013 §四)。
 * <p>
 * app 内可自由拼装页面；schema 存 z-frontend-render 组件树 JSON。
 * Airtable 固定视图（z_lc_view_config）保留为"实体视图"这一页面类型，两表互不取代。
 */
@BizModel(name = "z_lc_page 表实体 (V2 页面域: 页面一等公民, lead 013 §四)", module = "z-lc", description = "z_lc_page 表实体 (V2 页面域: 页面一等公民, lead 013 §四)。 app 内可自由拼装页面；schema 存 z-frontend-render 组件树 JSON。 Airtable 固定视图（z_lc_view_config）保留为\"实体视图\"这一页面类型，两表互不取代。")
@TableName("z_lc_page")
public class PageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("app_code")
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("page_code")
    @BizField(name = "pageCode", type = BizField.FieldType.STRING)
    private String pageCode;

    @TableField("page_name")
    @BizField(name = "pageName", type = BizField.FieldType.STRING)
    private String pageName;

    /** 页面 schema JSON（列名 schema_json: schema 是各库关键字，回避） */
    @TableField("schema_json")
    @BizField(name = "schemaJson", type = BizField.FieldType.STRING)
    private String schemaJson;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @BizField(name = "deleted", type = BizField.FieldType.INTEGER)
    private Integer deleted;

    @TableField("create_time")
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;

    @TableField("update_time")
    @BizField(name = "updateTime", type = BizField.FieldType.DATETIME)
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

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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



