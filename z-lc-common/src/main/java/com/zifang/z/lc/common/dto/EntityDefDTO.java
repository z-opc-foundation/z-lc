package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 实体定义 (z-lc 中: 一个 entity = 一张可被通用 CRUD 引擎驱动的逻辑表)
 */
public class EntityDefDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实体 id
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 实体名
     */
    private String entityName;

    /**
     * 物理表名 (运行时 CRUD 引擎拼 SQL 用的表名)
     */
    private String tableName;

    /**
     * 描述
     */
    private String description;

    /**
     * 乐观锁
     */
    private Long currentVersion;

    /**
     * 字段列表
     */
    private List<FieldDefDTO> fields;

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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<FieldDefDTO> getFields() {
        return fields;
    }

    public void setFields(List<FieldDefDTO> fields) {
        this.fields = fields;
    }
}
