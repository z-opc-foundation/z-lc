package com.zifang.z.lc.core.datasource.mysql;

import com.zifang.z.lc.common.dto.datasource.DatasourceSaveDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceDO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableColumnDTO;
import com.zifang.z.lc.common.dto.datasource.DataSourceTableDTO;
import com.zifang.z.lc.core.datasource.DataModelConvertSqlStrategy;
import com.zifang.z.lc.core.datasource.DataModelConvertStrategyRegistrar.ZLcDialect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 方言策略实现.
 *
 * <p>蒸馏自 ace-platform-core {@code DataModelConvertMysqlSqlStrategy}
 * （{@code com.c2f.ace.core.service.data.source.mysql}），精简到 z-lc 必需的功能：
 * <ul>
 *   <li>{@link #tryConnect} — 测试连接</li>
 *   <li>{@link #fetchDataSource} — 构造 DruidDataSource（带缓存）</li>
 *   <li>{@link #fetchTableInfo} — 扫表</li>
 *   <li>{@link #fetchTableColumnInfo} — 扫列</li>
 * </ul>
 *
 * <p>自动通过 {@link ZLcDialect} 注解注册到 {@code DataModelConvertSqlDispatch}.
 *
 * @author xuhf (distilled by zifang)
 */
@ZLcDialect("mysql")
public class DataModelConvertMysqlSqlStrategy implements DataModelConvertSqlStrategy {

    private static final Logger log = LogManager.getLogger(DataModelConvertMysqlSqlStrategy.class);
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_JDBC_PARAM =
            "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL";

    /**
     * DataSource 缓存（key = jdbcUrl，避免每次新建）.
     */
    private static final Map<String, DataSource> DATA_SOURCE_CACHE = new LinkedHashMap<>();

    @Override
    public Boolean tryConnect(DatasourceSaveDTO dto) {
        if (dto == null || dto.getJdbcUrl() == null) {
            return false;
        }
        try {
            Class.forName(MYSQL_DRIVER);
            try (Connection conn = DriverManager.getConnection(
                    dto.getJdbcUrl(), dto.getUsername(), dto.getPassword())) {
                if (conn != null) {
                    log.info("MySQL connection OK: {}", dto.getJdbcUrl());
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("MySQL JDBC driver not found", e);
        } catch (SQLException e) {
            log.error("MySQL connection failed: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public DataSource fetchDataSource(DataSourceDO dataSourceDO) {
        if (dataSourceDO == null || dataSourceDO.getJdbcUrl() == null) {
            return null;
        }
        synchronized (DATA_SOURCE_CACHE) {
            DataSource cached = DATA_SOURCE_CACHE.get(dataSourceDO.getJdbcUrl());
            if (cached != null) {
                return cached;
            }
            DataSource ds = buildDruidDataSource(dataSourceDO);
            if (ds != null) {
                DATA_SOURCE_CACHE.put(dataSourceDO.getJdbcUrl(), ds);
            }
            return ds;
        }
    }

    private DataSource buildDruidDataSource(DataSourceDO dataSourceDO) {
        // 简化实现：直接构造 HikariDataSource（Druid 需要额外依赖）
        // 业务方如有 Druid 需求，覆写本方法即可
        try {
            Class<?> dsClass = Class.forName("com.zaxxer.hikari.HikariDataSource");
            DataSource ds = (DataSource) dsClass.getDeclaredConstructor().newInstance();
            dsClass.getMethod("setJdbcUrl", String.class).invoke(ds, ensureParams(dataSourceDO.getJdbcUrl()));
            dsClass.getMethod("setUsername", String.class).invoke(ds, dataSourceDO.getUsername());
            dsClass.getMethod("setPassword", String.class).invoke(ds, dataSourceDO.getPassword());
            dsClass.getMethod("setDriverClassName", String.class).invoke(ds, MYSQL_DRIVER);
            dsClass.getMethod("setMaximumPoolSize", int.class).invoke(ds, 20);
            dsClass.getMethod("setMinimumIdle", int.class).invoke(ds, 5);
            log.info("MySQL DataSource built: {}", dataSourceDO.getDatasourceCode());
            return ds;
        } catch (ClassNotFoundException e) {
            log.error("HikariDataSource not found, please add HikariCP dependency", e);
            return null;
        } catch (Exception e) {
            log.error("Failed to build MySQL DataSource", e);
            return null;
        }
    }

    /**
     * 给 JDBC URL 追加默认参数（如未提供）.
     */
    private String ensureParams(String url) {
        if (url == null) {
            return null;
        }
        if (url.contains("?")) {
            return url + "&" + DEFAULT_JDBC_PARAM;
        }
        return url + "?" + DEFAULT_JDBC_PARAM;
    }

    @Override
    public List<DataSourceTableDTO> fetchTableInfo(DataSource dataSource, String schemaMark) {
        List<DataSourceTableDTO> result = new ArrayList<>();
        if (dataSource == null) {
            return result;
        }
        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getTables(schemaMark, "%", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    DataSourceTableDTO t = new DataSourceTableDTO();
                    t.setTableName(rs.getString("TABLE_NAME"));
                    t.setDescriptions(rs.getString("REMARKS"));
                    t.setDatasourceCode(schemaMark);
                    result.add(t);
                }
            }
            log.info("Scanned {} tables for schema {}", result.size(), schemaMark);
        } catch (SQLException e) {
            log.error("fetchTableInfo failed for schema {}", schemaMark, e);
        }
        return result;
    }

    @Override
    public List<DataSourceTableColumnDTO> fetchTableColumnInfo(DataSource dataSource, String schemaMark, String tableName) {
        List<DataSourceTableColumnDTO> result = new ArrayList<>();
        if (dataSource == null || tableName == null) {
            return result;
        }
        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.getMetaData().getColumns(schemaMark, "%", tableName, null)) {
                while (rs.next()) {
                    DataSourceTableColumnDTO col = new DataSourceTableColumnDTO();
                    col.setTableName(tableName);
                    col.setDatasourceCode(schemaMark);
                    col.setColumnName(rs.getString("COLUMN_NAME"));
                    col.setColumnType(rs.getString("TYPE_NAME"));
                    col.setColumnLength(buildColumnLength(rs));
                    col.setColumnComment(rs.getString("REMARKS"));
                    result.add(col);
                }
            }
            log.debug("Scanned {} columns for {}.{}", result.size(), schemaMark, tableName);
        } catch (SQLException e) {
            log.error("fetchTableColumnInfo failed for {}.{}", schemaMark, tableName, e);
        }
        return result;
    }

    /**
     * 从 ResultSet 拼装 columnLength（精度 / 标度）.
     */
    private String buildColumnLength(ResultSet rs) throws SQLException {
        int precision = rs.getInt("COLUMN_SIZE");
        int scale = rs.getInt("DECIMAL_DIGITS");
        if (rs.wasNull()) {
            return null;
        }
        if (scale > 0) {
            return precision + "," + scale;
        }
        return String.valueOf(precision);
    }

    @Override
    public DataSourceTableDTO fetchTableInfo(DataSource dataSource, String schemaMark, String tableName) {
        if (dataSource == null || tableName == null) {
            return null;
        }
        List<DataSourceTableDTO> tables = fetchTableInfo(dataSource, schemaMark);
        for (DataSourceTableDTO t : tables) {
            if (tableName.equalsIgnoreCase(t.getTableName())) {
                // 顺便把 columns 也填进去
                t.setColumns(fetchTableColumnInfo(dataSource, schemaMark, tableName));
                return t;
            }
        }
        return null;
    }
}
