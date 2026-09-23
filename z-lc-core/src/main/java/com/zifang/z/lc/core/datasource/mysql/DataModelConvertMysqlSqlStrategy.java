package com.zifang.z.lc.core.datasource.mysql;

import com.zifang.util.core.lang.exception.BusinessException;
import com.zifang.util.db.context.DataSourceRegistry;
import com.zifang.util.db.meta.DataSourceDTO;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MySQL 方言策略实现.
 *
 * <p>蒸馏自 ace-platform-core {@code DataModelConvertMysqlSqlStrategy}
 * （{@code com.c2f.ace.core.service.data.source.mysql}），精简到 z-lc 必需的功能：
 * <ul>
 *   <li>{@link #tryConnect} — 测试连接</li>
 *   <li>{@link #fetchDataSource} — 取/建连接池（带缓存）</li>
 *   <li>{@link #fetchTableInfo} — 扫表</li>
 *   <li>{@link #fetchTableColumnInfo} — 扫列</li>
 * </ul>
 *
 * <p>建池、驱动选择与探活全部交给 z-util-jdbc 的 {@link DataSourceRegistry}：
 * {@code DataSourceDO.jdbcUrl} 整串接入，方言由 URL 识别（MySQL / PostgreSQL / H2 一套代码），
 * 探活通过才发布，池按 code 复用并在换绑、注销时关闭.
 *
 * <p>自动通过 {@link ZLcDialect} 注解注册到 {@code DataModelConvertSqlDispatch}.
 *
 * @author xuhf (distilled by zifang)
 */
@ZLcDialect("mysql")
public class DataModelConvertMysqlSqlStrategy implements DataModelConvertSqlStrategy, AutoCloseable {

    private static final Logger log = LogManager.getLogger(DataModelConvertMysqlSqlStrategy.class);

    /**
     * z-lc 保存数据源时的历史连接默认参数：用户存的是整串 URL，这里只在 {@code jdbc:mysql} 上补齐.
     */
    private static final String DEFAULT_JDBC_PARAM =
            "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL";

    /** 探活用的临时 code 序号：探活完即注销，不占用业务 code，也不会误关已有可用池 */
    private static final AtomicLong PROBE_SEQ = new AtomicLong();

    private final DataSourceRegistry sources = new DataSourceRegistry();

    @Override
    public Boolean tryConnect(DatasourceSaveDTO dto) {
        if (dto == null || dto.getJdbcUrl() == null) {
            return false;
        }
        String code = "__probe__" + PROBE_SEQ.incrementAndGet();
        try {
            sources.register(toDefinition(code, dto.getJdbcUrl(), dto.getUsername(), dto.getPassword(),
                    dto.getSchemaMark()));
            log.info("数据源探活通过: {}", dto.getJdbcUrl());
            return true;
        } catch (BusinessException e) {
            log.error("数据源探活失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("数据源探活失败: {}", dto.getJdbcUrl(), e);
            return false;
        } finally {
            sources.unregister(code);
        }
    }

    @Override
    public DataSource fetchDataSource(DataSourceDO dataSourceDO) {
        if (dataSourceDO == null || dataSourceDO.getJdbcUrl() == null) {
            return null;
        }
        DataSourceDTO def = toDefinition(codeOf(dataSourceDO), dataSourceDO.getJdbcUrl(),
                dataSourceDO.getUsername(), dataSourceDO.getPassword(), dataSourceDO.getSchemaMark());
        String code = def.getDatasourceCode();
        // 同 code 且定义未变 → 复用既有池；改了地址/账号/密码 → rebind 换池（探活不过则保持原池不变）
        DataSource cached = sources.get(code);
        return cached != null && def.equals(sources.def(code)) ? cached : sources.rebind(def);
    }

    /**
     * 池的缓存键：业务 code 优先，没有 code 的旧数据退回整串 URL（与迁移前一致）.
     */
    private static String codeOf(DataSourceDO dataSourceDO) {
        String code = dataSourceDO.getDatasourceCode();
        return code == null || code.trim().isEmpty() ? dataSourceDO.getJdbcUrl() : code.trim();
    }

    private static DataSourceDTO toDefinition(String code, String jdbcUrl, String username, String password,
                                              String schemaMark) {
        DataSourceDTO def = new DataSourceDTO();
        def.setDatasourceCode(code);
        def.setJdbcUrl(withDefaults(jdbcUrl));
        def.setUserName(username);
        def.setPw(password);
        def.setSchemaMark(schemaMark);
        return def;
    }

    /**
     * 给 MySQL 协议的 URL 追加默认参数（如未提供）；非 MySQL 协议原样交给方言识别.
     */
    static String withDefaults(String url) {
        if (url == null || !url.startsWith("jdbc:mysql")) {
            return url;
        }
        return url.contains("?") ? url + "&" + DEFAULT_JDBC_PARAM : url + "?" + DEFAULT_JDBC_PARAM;
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

    /**
     * 释放本策略建出的全部连接池.
     */
    @Override
    public void close() {
        sources.close();
    }
}
