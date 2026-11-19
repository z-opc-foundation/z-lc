package com.zifang.z.lc.core.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_dict_item 表实体 (F035 T6: Dict Management)
 */
@BizModel(name = "z_lc_dict_item 表实体 (F035 T6: Dict Management)", module = "z-lc", description = "z_lc_dict_item 表实体 (F035 T6: Dict Management)")
@TableName("z_lc_dict_item")
public class DictItemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("dict_code")
    @BizField(name = "dictCode", type = BizField.FieldType.STRING)
    private String dictCode;

    @TableField("item_code")
    @BizField(name = "itemCode", type = BizField.FieldType.STRING)
    private String itemCode;

    @TableField("item_label")
    @BizField(name = "itemLabel", type = BizField.FieldType.STRING)
    private String itemLabel;

    @TableField("item_value")
    @BizField(name = "itemValue", type = BizField.FieldType.STRING)
    private String itemValue;

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

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
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



