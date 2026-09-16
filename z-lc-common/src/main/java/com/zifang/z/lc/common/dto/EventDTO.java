package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 事件记录 (z_lc_event 表对应 DTO)
 */
public class EventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增 id
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 防重放 hash (uuid 或 sha1)
     */
    private String eventId;

    /**
     * 应用编码 (app 级事件可空)
     */
    private String appCode;

    /**
     * 实体编码 (app 级事件可空)
     */
    private String entityCode;

    /**
     * 事件类型:
     * CREATE / UPDATE / DELETE / MATERIALIZE
     */
    private String eventType;

    /**
     * 本次变更 JSON 快照
     */
    private String eventData;

    /**
     * 事件源:
     * USER / AGENT / SYSTEM / GENERATOR
     */
    private String source;

    /**
     * 因果链前驱事件 id (用于"不允许冲突"校验)
     */
    private String parentEventId;

    /**
     * 单调递增序号 (= 时间序)
     */
    private Long applySeq;

    /**
     * 应用时间
     */
    private java.util.Date applyTime;

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

    public java.util.Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(java.util.Date applyTime) {
        this.applyTime = applyTime;
    }
}
