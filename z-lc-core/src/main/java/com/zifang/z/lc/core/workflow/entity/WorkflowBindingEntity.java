package com.zifang.z.lc.core.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

@BizModel(name = "z_lc_workflow_binding", module = "z-lc", description = "z_lc_workflow_binding")
@TableName("z_lc_workflow_binding")
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WorkflowBindingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "entityCode", type = BizField.FieldType.STRING)
    private String entityCode;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    /**
     * AFTER_CREATE / AFTER_UPDATE / status_change
     */
    @BizField(name = "triggerEvent", type = BizField.FieldType.STRING)
    private String triggerEvent;
    @BizField(name = "processDefinitionKey", type = BizField.FieldType.STRING)
    private String processDefinitionKey;
    @BizField(name = "autoSubmit", type = BizField.FieldType.INTEGER)
    private Integer autoSubmit;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;
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

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public Integer getAutoSubmit() {
        return autoSubmit;
    }

    public void setAutoSubmit(Integer autoSubmit) {
        this.autoSubmit = autoSubmit;
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



