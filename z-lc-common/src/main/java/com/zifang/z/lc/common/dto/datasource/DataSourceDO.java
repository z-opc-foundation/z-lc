package com.zifang.z.lc.common.dto.datasource;

import java.io.Serializable;

/**
 * 数据源 DO（精简版）— 蒸馏自 ace-platform-core {@code DataSourceDO}.
 *
 * <p>字段语义对齐 ace 原生，但去除 lombok + 去除冗余字段（保留核心 8 个）.
 *
 * <p>用途：「数据库管理」模块的实体持久化对象 — datasource 表的内存表示.
 *
 * @author zifang
 */
public class DataSourceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 id.
     */
    private Long id;

    /**
     * 租户编码.
     */
    private String tenantCode;

    /**
     * 数据源编码（业务唯一）.
     */
    private String datasourceCode;

    /**
     * 数据源类型（{@code mysql} / {@code doris} / {@code dm} / {@code sr} / {@code kingbase}）.
     */
    private String datasourceType;

    /**
     * JDBC URL.
     */
    private String jdbcUrl;

    /**
     * 用户名.
     */
    private String username;

    /**
     * 密码.
     */
    private String password;

    /**
     * schema 标识.
     */
    private String schemaMark;

    /**
     * 描述.
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

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchemaMark() {
        return schemaMark;
    }

    public void setSchemaMark(String schemaMark) {
        this.schemaMark = schemaMark;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
