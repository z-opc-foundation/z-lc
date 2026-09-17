package com.zifang.z.lc.core.domain;

/**
 * 数据源表 DO — 蒸馏自 ace-platform-core
 * {@code DataSourceTableDO} （{@code com.c2f.ace.core.domain.entity}}，
 * 字段语义完全对齐.
 *
 * <p>对应 {@code datasource_table} 表 — 数据源扫描结果的表清单，
 * 用于：
 * <ul>
 *   <li>低代码平台「数据源管理」页面展示可建表的列表</li>
 *   <li>从数据源扫描表 → 抽取字段 → 生成 DataModel</li>
 *   <li>{@link #tableType} 区分业务表/字典表/系统表</li>
 * </ul>
 *
 * <p>与 z-lc-common/datasource/DataSourceTableDTO 的区别：
 * <ul>
 *   <li>{@code DataSourceTableDTO} — 业务 DTO（用于 API 入参/出参）</li>
 *   <li>{@code DataSourceTableDO} — 持久化对象</li>
 * </ul>
 *
 * @author zifang
 */
public class DataSourceTableDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 租户 code.
     */
    private String tenantCode;

    /**
     * 数据源 code（与 {@code DataSourceDO.datasourceCode} 关联）.
     */
    private String datasourceCode;

    /**
     * 表名称.
     */
    private String tableName;

    /**
     * 表描述.
     */
    private String descriptions;

    /**
     * 表类型（如 {@code "biz"} / {@code "dict"} / {@code "system"}）.
     */
    private String tableType;

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

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
}
