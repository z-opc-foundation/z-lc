package com.zifang.z.lc.common.dto.datasource;

import java.io.Serializable;

/**
 * 数据源表列元数据 DTO.
 *
 * <p>蒸馏自 ace-platform-core {@code DataSourceTableColumnDTO}
 * （{@code com.c2f.ace.core.service.app.dto}），字段语义完全对齐.
 *
 * <p>用途：「数据库管理 → 模型」链路中，扫描 {@code INFORMATION_SCHEMA.COLUMNS}
 * 后产出的列元数据；后续转换为 z-lc 的 {@code FieldDefDTO}.
 *
 * <p>两个指纹方法：
 * <ul>
 *   <li>{@link #nativeSignature()} — 数据库原生签名（{@code tableName:columnType:columnComment}），
 *       用于检测 schema 变更</li>
 *   <li>{@link #physicalSignature()} — 物理签名（含长度信息），用于 CREATE TABLE / ALTER TABLE</li>
 * </ul>
 *
 * @author zifang
 */
public class DataSourceTableColumnDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 列 id.
     */
    private Long datasourceTableColumnId;

    /**
     * 数据源编码.
     */
    private String datasourceCode;

    /**
     * 归属表名.
     */
    private String tableName;

    /**
     * 列名称.
     */
    private String columnName;

    /**
     * 列类型（如 {@code varchar} / {@code bigint} / {@code datetime}）.
     */
    private String columnType;

    /**
     * 列长度（如 {@code 255} / {@code 18,2}）.
     */
    private String columnLength;

    /**
     * 列注释.
     */
    private String columnComment;

    /**
     * 是否主键.
     */
    private Boolean primaryKey;

    /**
     * 原生签名（{@code tableName:columnType:columnComment}）— 用于 schema 变更检测.
     */
    public String nativeSignature() {
        return tableName + ":" + columnType + ":" + columnComment;
    }

    /**
     * 物理签名（含长度）— 用于 DDL.
     */
    public String physicalSignature() {
        return tableName + ":" + columnType + ":" + columnComment;
    }

    public Long getDatasourceTableColumnId() {
        return datasourceTableColumnId;
    }

    public void setDatasourceTableColumnId(Long datasourceTableColumnId) {
        this.datasourceTableColumnId = datasourceTableColumnId;
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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getColumnLength() {
        return columnLength;
    }

    public void setColumnLength(String columnLength) {
        this.columnLength = columnLength;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
}
