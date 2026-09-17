package com.zifang.z.lc.core.datasource;

import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;

import javax.sql.DataSource;
import java.util.List;

/**
 * 数据源 SQL 转换策略接口 — 不同方言的元数据扫描 + DDL/DML 生成.
 *
 * <p>蒸馏自 ace-platform-core {@code DataModelConvertSqlStrategy}
 * （{@code com.c2f.ace.core.service.data.source}），精简为 z-lc 所需核心 5 个方法.
 *
 * <p>设计哲学：
 * <ul>
 *   <li>每个数据库方言（MySQL / Doris / DM / StarRocks / Kingbase）实现一份策略</li>
 *   <li>{@link DataModelConvertSqlDispatch} 按 {@code datasourceType} 路由到对应策略</li>
 *   <li>业务方新增方言：实现本接口 + 在 dispatch 注册（{@code @Resource}）即可</li>
 * </ul>
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #fetchTableInfo} — 扫描 {@code INFORMATION_SCHEMA.TABLES} 拿所有表</li>
 *   <li>{@link #fetchTableColumnInfo} — 扫描 {@code INFORMATION_SCHEMA.COLUMNS} 拿列</li>
 *   <li>{@link #tryConnect} — 测试连接（保存前预校验）</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public interface DataModelConvertSqlStrategy {

    /**
     * 测试连接 — 用户保存数据源时调用，false 拒绝保存.
     *
     * @param dto 连接参数
     * @return true 连接成功，false 失败
     */
    Boolean tryConnect(DatasourceSaveDTO dto);

    /**
     * 把 DataSourceDO 转换为可用的 JDBC DataSource.
     * <p>具体实现可以是 DruidDataSource / HikariDataSource.
     *
     * @param dataSourceDO 数据源 DO
     * @return JDBC DataSource
     */
    DataSource fetchDataSource(DataSourceDO dataSourceDO);

    /**
     * 扫描指定 schema 下所有表元数据.
     *
     * @param dataSource 已建连的 JDBC DataSource
     * @param schemaMark schema 标识（数据库名 / schema 名）
     * @return 表元数据列表
     */
    List<DataSourceTableDTO> fetchTableInfo(DataSource dataSource, String schemaMark);

    /**
     * 扫描指定表的所有列元数据.
     *
     * @param dataSource 已建连的 JDBC DataSource
     * @param schemaMark schema 标识
     * @param tableName  表名
     * @return 列元数据列表
     */
    List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName);

    /**
     * 获取单个表的元数据 — 等同于 {@code fetchTableInfo(schema).stream().filter(t -> t.tableName == tableName).findFirst()}.
     *
     * @param dataSource 已建连的 JDBC DataSource
     * @param schemaMark schema 标识
     * @param tableName  表名
     * @return 单个表元数据
     */
    DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName);
}
