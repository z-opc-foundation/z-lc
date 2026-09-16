package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 字典项 DTO (F035 T6: Dict Management)
 * <p>
 * 扩展自原有 DictItemDTO, 增加 id/tenantCode/dictCode/description 字段
 */
public class DictItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenantCode;

    private String dictCode;

    /**
     * 字典项编码
     */
    private String itemCode;

    /**
     * 字典项标签 (UI 显示用)
     */
    private String itemLabel;

    /**
     * 字典项值 (存储值)
     */
    private String itemValue;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 描述
     */
    private String description;

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
}
