package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 实体关系更新请求 DTO (F035 T2: Entity Relation)
 */
public class RelationUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String relationName;

    private String relationType;

    private String sourceFieldCode;

    private String throughTable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
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
}
