package com.zifang.z.lc.core.viewconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_view_config 表实体 (F035 T5: View Config)
 */
@BizModel(name = "z_lc_view_config 表实体 (F035 T5: View Config)", module = "z-lc", description = "z_lc_view_config 表实体 (F035 T5: View Config)")
@TableName("z_lc_view_config")
public class ViewConfigEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("entity_code")
    @BizField(name = "entityCode", type = BizField.FieldType.STRING)
    private String entityCode;

    @TableField("app_code")
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("view_type")
    @BizField(name = "viewType", type = BizField.FieldType.STRING)
    private String viewType;

    @TableField("config")
    @BizField(name = "config", type = BizField.FieldType.STRING)
    private String config;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("create_time")
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;

    @TableField("update_time")
    @BizField(name = "updateTime", type = BizField.FieldType.DATETIME)
    private Date updateTime;

    @BizField(name = "deleted", type = BizField.FieldType.INTEGER)
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}



