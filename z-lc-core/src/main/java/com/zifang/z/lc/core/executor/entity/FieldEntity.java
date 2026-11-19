package com.zifang.z.lc.core.executor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_field 表实体
 */
@BizModel(name = "z_lc_field 表实体", module = "z-lc", description = "z_lc_field 表实体")
@TableName("z_lc_field")
public class FieldEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("entity_id")
    @BizField(name = "entityId", type = BizField.FieldType.INTEGER)
    private Long entityId;

    @TableField("field_code")
    @BizField(name = "fieldCode", type = BizField.FieldType.STRING)
    private String fieldCode;

    @TableField("field_name")
    @BizField(name = "fieldName", type = BizField.FieldType.STRING)
    private String fieldName;

    @TableField("field_type")
    @BizField(name = "fieldType", type = BizField.FieldType.STRING)
    private String fieldType;

    @BizField(name = "required", type = BizField.FieldType.INTEGER)
    private Integer required;

    @TableField("default_value")
    @BizField(name = "defaultValue", type = BizField.FieldType.STRING)
    private String defaultValue;

    @TableField("dict_code")
    @BizField(name = "dictCode", type = BizField.FieldType.STRING)
    private String dictCode;

    @TableField("ref_entity")
    @BizField(name = "refEntity", type = BizField.FieldType.STRING)
    private String refEntity;

    @TableField("field_length")
    @BizField(name = "fieldLength", type = BizField.FieldType.INTEGER)
    private Integer fieldLength;

    @BizField(name = "scale", type = BizField.FieldType.INTEGER)
    private Integer scale;

    @TableField("sort_order")
    @BizField(name = "sortOrder", type = BizField.FieldType.INTEGER)
    private Integer sortOrder;

    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;

    @BizField(name = "deleted", type = BizField.FieldType.INTEGER)
    private Integer deleted;

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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public Integer getRequired() {
        return required;
    }

    public void setRequired(Integer required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getRefEntity() {
        return refEntity;
    }

    public void setRefEntity(String refEntity) {
        this.refEntity = refEntity;
    }

    public Integer getFieldLength() {
        return fieldLength;
    }

    public void setFieldLength(Integer fieldLength) {
        this.fieldLength = fieldLength;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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



