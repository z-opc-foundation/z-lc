package com.zifang.z.lc.core.deployment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_deployment 表实体 (F035 T7: Deployment)
 */
@BizModel(name = "z_lc_deployment 表实体 (F035 T7: Deployment)", module = "z-lc", description = "z_lc_deployment 表实体 (F035 T7: Deployment)")
@TableName("z_lc_deployment")
public class DeploymentEntity implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String TYPE_HOT_LOAD = "HOT_LOAD";
    public static final String TYPE_DOCKER = "DOCKER";
    public static final String TYPE_GIT_PUSH = "GIT_PUSH";
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("app_code")
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("materialization_id")
    @BizField(name = "materializationId", type = BizField.FieldType.INTEGER)
    private Long materializationId;

    @TableField("deploy_type")
    @BizField(name = "deployType", type = BizField.FieldType.STRING)
    private String deployType;

    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;

    @TableField("deploy_log")
    @BizField(name = "deployLog", type = BizField.FieldType.STRING)
    private String deployLog;

    @BizField(name = "version", type = BizField.FieldType.STRING)
    private String version;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeployLog() {
        return deployLog;
    }

    public void setDeployLog(String deployLog) {
        this.deployLog = deployLog;
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



