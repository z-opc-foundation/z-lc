package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 写入事件请求 (POST /api/lc/app/{appCode}/event body)
 */
public class EventAppendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String entityCode;
    private String eventType;       // CREATE/UPDATE/DELETE/MATERIALIZE
    private String eventData;       // JSON
    private String source;          // USER/AGENT/SYSTEM
    private String parentEventId;   // 因果链前驱 (必填)

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
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
}
