package com.zifang.z.lc.common.dto.datasource;

import java.io.Serializable;

/**
 * 数据源保存 / 校验 DTO.
 *
 * <p>蒸馏自 ace-platform-core {@code DatasourceSaveDTO}
 * （{@code com.c2f.ace.core.service.app.dto}），字段语义完全对齐.
 *
 * <p>用途：「数据库管理」模块的入参 — 用户新建 / 测试连接 / 刷新元数据时使用.
 *
 * @author zifang
 */
public class DatasourceSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源编码.
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
     * schema 标识（数据库 / schema 名）.
     */
    private String schemaMark;

    /**
     * 数据源描述.
     */
    private String description;

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
