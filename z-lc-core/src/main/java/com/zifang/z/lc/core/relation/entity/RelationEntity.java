package com.zifang.z.lc.core.relation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_relation 表实体 (F035 T2: Entity Relation)
 */
@TableName("z_lc_relation")
public class RelationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("relation_code")
    private String relationCode;

    @TableField("relation_name")
    private String relationName;

    @TableField("source_entity_code")
    private String sourceEntityCode;

    @TableField("target_entity_code")
    private String targetEntityCode;

    @TableField("relation_type")
    private String relationType;

    @TableField("source_field_code")
    private String sourceFieldCode;

    @TableField("through_table")
    private String throughTable;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("app_code")
    private String appCode;

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
