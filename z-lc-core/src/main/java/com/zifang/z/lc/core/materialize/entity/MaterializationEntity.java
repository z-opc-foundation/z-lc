package com.zifang.z.lc.core.materialize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_materialization 表实体 (FEATURE006 T1: 代码物化).
 */
@TableName("z_lc_materialization")
public class MaterializationEntity implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_GENERATING = "GENERATING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";
    public static final String SOURCE_USER = "USER";
    public static final String SOURCE_AGENT = "AGENT";
    public static final String SOURCE_SYSTEM = "SYSTEM";
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("app_code")
    private String appCode;

    @TableField("materialization_path")
    private String materializationPath;

    @TableField("entity_codes")
    private String entityCodes;

    @TableField("export_version")
    private String exportVersion;

    private String status;

    @TableField("file_count")
    private Integer fileCount;

    private String description;

    @TableField("error_message")
    private String errorMessage;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("event_id")
    private String eventId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String v) {
        this.tenantCode = v;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String v) {
        this.appCode = v;
    }

    public String getMaterializationPath() {
        return materializationPath;
    }

    public void setMaterializationPath(String v) {
        this.materializationPath = v;
    }

    public String getEntityCodes() {
        return entityCodes;
    }

    public void setEntityCodes(String v) {
        this.entityCodes = v;
    }

    public String getExportVersion() {
        return exportVersion;
    }

    public void setExportVersion(String v) {
        this.exportVersion = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer v) {
        this.fileCount = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String v) {
        this.errorMessage = v;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String v) {
        this.triggerSource = v;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String v) {
        this.eventId = v;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date v) {
        this.createTime = v;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date v) {
        this.updateTime = v;
    }
}
