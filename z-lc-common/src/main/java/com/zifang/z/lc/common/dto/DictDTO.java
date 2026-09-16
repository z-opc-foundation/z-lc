package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 字典 DTO (F035 T6: Dict Management)
 */
public class DictDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantCode;
    private String dictCode;
    private String dictName;
    private String description;
    private String status;
    private Date createTime;
    private Date updateTime;

    /**
     * 字典项列表 (查询详情时填充)
     */
    private List<DictItemDTO> items;

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

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<DictItemDTO> getItems() {
        return items;
    }

    public void setItems(List<DictItemDTO> items) {
        this.items = items;
    }
}
