package com.zifang.z.lc.common.dto.datasource;

import java.io.Serializable;
import java.util.List;

/**
 * 数据源表元数据 DTO — 描述「一个数据源下的一张表及其所有列」.
 *
 * <p>蒸馏自 ace-platform-core {@code DataSourceTableDTO}
 * （{@code com.c2f.ace.core.service.app.dto}），字段语义完全对齐.
 *
 * <p>用途：「数据库管理 → 模型」链路中，元数据扫描阶段产出的中间对象；
 * 之后通过 {@code DataModelManagerService} 把每个表转换为 z-lc 的 {@code EntityDefDTO}.
 *
 * @author zifang
 */
public class DataSourceTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源下表 id（持久化时由平台分配的 id）.
     */
    private Long datasourceTableId;

    /**
     * 数据源编码.
     */
    private String datasourceCode;

    /**
     * 表名.
     */
    private String tableName;

    /**
     * 表描述 / 注释.
     */
    private String descriptions;

    /**
     * 表下所有列元数据.
     */
    private List<DataSourceTableColumnDTO> columns;

    /**
     * 组件编码 — 用于模型树节点唯一标识（{@code datasourceCode:tableName}）.
     */
    public String getComponentCode() {
        return datasourceCode + ":" + tableName;
    }

    public Long getDatasourceTableId() {
        return datasourceTableId;
    }

    public void setDatasourceTableId(Long datasourceTableId) {
        this.datasourceTableId = datasourceTableId;
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

    public List<DataSourceTableColumnDTO> getColumns() {
        return columns;
    }

    public void setColumns(List<DataSourceTableColumnDTO> columns) {
        this.columns = columns;
    }
}
