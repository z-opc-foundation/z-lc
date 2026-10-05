package com.zifang.z.lc.core.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_workflow_fire 表实体: 每一次「记录写成功后去发起流程」的实际结局.
 * <p>
 * 为什么要有这张表（缺陷 #61）：绑定行只能说明"用户希望发生什么"，说明不了"发生过什么"。
 * 发起流程是一次用户看不见的对外调用 —— 只往日志里写一句的话，"已保存绑定"就会重新变成
 * 这一族最常见的那个谎：配置在、界面在、什么也没发生。有了这张表，
 * {@code GET /api/lc/workflow-binding/fires} 能把"发起了 / 没发起、为什么"直接回读出来
 * （与 #43/#47 的"provision 之后回读库作证"同一口径）。
 */
@TableName("z_lc_workflow_fire")
public class WorkflowFireEntity implements Serializable {

    /** 流程真的起来了，instance_id 是 z-camuda 给的实例 id。 */
    public static final String STATUS_STARTED = "STARTED";
    /** 尝试过但没起来（外部引擎不可达/拒绝/超时/并发上限），detail 里点名为什么。 */
    public static final String STATUS_FAILED = "FAILED";

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("app_code")
    private String appCode;

    @TableField("entity_code")
    private String entityCode;

    /** 触发这次发起的那条业务记录的主键. */
    @TableField("record_id")
    private Long recordId;

    /** 是哪条绑定发的（绑定可能已被删掉，所以只是指针不是外键）. */
    @TableField("binding_id")
    private Long bindingId;

    @TableField("trigger_event")
    private String triggerEvent;

    @TableField("process_definition_key")
    private String processDefinitionKey;

    /** STARTED / FAILED. */
    private String status;

    /** z-camuda 的流程实例 id；FAILED 时为 null. */
    @TableField("instance_id")
    private String instanceId;

    /** 失败原因（http 状态/超时/引擎的拒绝消息）；STARTED 时为 null. */
    private String detail;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getBindingId() {
        return bindingId;
    }

    public void setBindingId(Long bindingId) {
        this.bindingId = bindingId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
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
