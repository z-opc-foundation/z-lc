package com.zifang.z.lc.core.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_event 表对应实体类 (事件管理)
 *
 * @author zifang
 * @since 1.0.0
 */
@BizModel(name = "z_lc_event 表对应实体类 (事件管理)", module = "z-lc", description = "z_lc_event 表对应实体类 (事件管理)")
@TableName("z_lc_event")
public class EventEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @BizField(name = "主键ID", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 租户编码
     */
    @TableField("tenant_code")
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode;

    /**
     * 事件ID
     */
    @TableField("event_id")
    @BizField(name = "事件ID", type = BizField.FieldType.STRING)
    private String eventId;

    /**
     * 应用编码
     */
    @TableField("app_code")
    @BizField(name = "应用编码", type = BizField.FieldType.STRING)
    private String appCode;

    /**
     * 实体编码
     */
    @TableField("entity_code")
    @BizField(name = "实体编码", type = BizField.FieldType.STRING)
    private String entityCode;

    /**
     * 事件类型
     */
    @TableField("event_type")
    @BizField(name = "事件类型", type = BizField.FieldType.STRING)
    private String eventType;

    /**
     * 事件数据
     */
    @TableField("event_data")
    @BizField(name = "事件数据", type = BizField.FieldType.STRING)
    private String eventData;

    /**
     * 事件来源
     */
    @TableField("source")
    @BizField(name = "事件来源", type = BizField.FieldType.STRING)
    private String source;

    /**
     * 父事件ID
     */
    @TableField("parent_event_id")
    @BizField(name = "父事件ID", type = BizField.FieldType.STRING)
    private String parentEventId;

    /**
     * 申请序列号
     */
    @TableField("apply_seq")
    @BizField(name = "申请序列号", type = BizField.FieldType.INTEGER)
    private Long applySeq;

    /**
     * 申请时间
     */
    @TableField("apply_time")
    @BizField(name = "申请时间", type = BizField.FieldType.DATETIME)
    private Date applyTime;

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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getParentEventId() {
        return parentEventId;
    }

    public void setParentEventId(String parentEventId) {
        this.parentEventId = parentEventId;
    }

    public Long getApplySeq() {
        return applySeq;
    }

    public void setApplySeq(Long applySeq) {
        this.applySeq = applySeq;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }
}



