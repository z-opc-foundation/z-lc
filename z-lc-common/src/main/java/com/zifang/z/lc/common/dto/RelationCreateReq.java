package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 实体关系创建请求 DTO (F035 T2: Entity Relation)
 */
public class RelationCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String relationCode;

    private String relationName;

    private String sourceEntityCode;

    private String targetEntityCode;

    /**
     * ONE_TO_MANY / MANY_TO_ONE / MANY_TO_MANY
     */
    private String relationType;

    /**
     * 外键字段
     */
    private String sourceFieldCode;

    /**
     * N:N 中间表
     */
    private String throughTable;

    private String appCode;

    private String tenantCode;

    public String getRelationCode() {
        return relationCode;
    }

    public void setRelationCode(String relationCode) {
        this.relationCode = relationCode;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public String getSourceEntityCode() {
        return sourceEntityCode;
    }

    public void setSourceEntityCode(String sourceEntityCode) {
        this.sourceEntityCode = sourceEntityCode;
    }

    public String getTargetEntityCode() {
        return targetEntityCode;
    }

    public void setTargetEntityCode(String targetEntityCode) {
        this.targetEntityCode = targetEntityCode;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getSourceFieldCode() {
        return sourceFieldCode;
    }

    public void setSourceFieldCode(String sourceFieldCode) {
        this.sourceFieldCode = sourceFieldCode;
    }

    public String getThroughTable() {
        return throughTable;
    }

    public void setThroughTable(String throughTable) {
        this.throughTable = throughTable;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
