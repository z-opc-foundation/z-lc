package com.zifang.z.lc.core.relation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_relation 表实体 (F035 T2: Entity Relation)
 */
@BizModel(name = "z_lc_relation 表实体 (F035 T2: Entity Relation)", module = "z-lc", description = "z_lc_relation 表实体 (F035 T2: Entity Relation)")
@TableName("z_lc_relation")
public class RelationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("relation_code")
    @BizField(name = "relationCode", type = BizField.FieldType.STRING)
    private String relationCode;

    @TableField("relation_name")
    @BizField(name = "relationName", type = BizField.FieldType.STRING)
    private String relationName;

    @TableField("source_entity_code")
    @BizField(name = "sourceEntityCode", type = BizField.FieldType.STRING)
    private String sourceEntityCode;

    @TableField("target_entity_code")
    @BizField(name = "targetEntityCode", type = BizField.FieldType.STRING)
    private String targetEntityCode;

    @TableField("relation_type")
    @BizField(name = "relationType", type = BizField.FieldType.STRING)
    private String relationType;

    @TableField("source_field_code")
    @BizField(name = "sourceFieldCode", type = BizField.FieldType.STRING)
    private String sourceFieldCode;

    @TableField("through_table")
    @BizField(name = "throughTable", type = BizField.FieldType.STRING)
    private String throughTable;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("app_code")
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("create_time")
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
    private Date createTime;

    @TableField("update_time")
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



