package com.zifang.z.report.datasource.impl;

import com.zifang.util.core.lang.exception.BusinessException;
import com.zifang.util.db.context.DataSourceRegistry;
import com.zifang.util.db.dialect.SqlType;
import com.zifang.util.db.meta.DataSourceDTO;
import com.zifang.util.db.query.DynamicQuery;
import com.zifang.util.db.query.Query;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.datasource.spi.ReportDataSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * JDBC 数据源：连接池、方言与取数全部委托 z-util-jdbc，本类只做报表侧的适配。
 * <p>
 * z-util-jdbc 负责的部分：Druid 建池与保活、注册前探活、按方言拼 URL 与引用标识符、
 * 表/列元数据读取、行数上限。因此 MySQL / PostgreSQL / H2 走同一条代码路径。
 * <p>
 * properties (对齐 z-boot 的 z.base.db.* 约定)：
 * <ul>
 *   <li>{@code host} {@code port} {@code database} {@code username} {@code password} — 拼装式连接</li>
 *   <li>{@code url} — 整条 JDBC 地址，给出后方言按地址识别，前三项忽略</li>
 *   <li>{@code dialect} — mysql(默认) / postgres / h2</li>
 *   <li>{@code max-rows} — 进入内存的行数上限，默认 100000</li>
 * </ul>
 * 凭证由部署方注入 (REST 注册或配置中心)，不落代码库。
 */
public class JdbcReportDataSource implements ReportDataSource, AutoCloseable {

    public static final String PROP_HOST = "host";
    public static final String PROP_PORT = "port";
    public static final String PROP_DATABASE = "database";
    public static final String PROP_USERNAME = "username";
    public static final String PROP_PASSWORD = "password";
    public static final String PROP_URL = "url";
    public static final String PROP_DIALECT = "dialect";
    public static final String PROP_MAX_ROWS = "max-rows";

    private static final String DEFAULT_DIALECT = "mysql";
    private static final int DEFAULT_MAX_ROWS = 100_000;

    private final DataSourceDef def;
    private final DataSourceRegistry sources;
    private final DynamicQuery query;

    public JdbcReportDataSource(DataSourceDef def) {
        Objects.requireNonNull(def, "def");
        if (def.getType() != DataSourceType.JDBC) {
            throw new IllegalArgumentException("JdbcReportDataSource requires type=JDBC, got: " + def.getType());
        }
        Map<String, String> p = def.getProperties() == null
                ? Collections.<String, String>emptyMap() : def.getProperties();
        this.def = def;
        String code = def.getId() == null || def.getId().trim().isEmpty() ? "jdbc" : def.getId().trim();
        this.sources = new DataSourceRegistry();
        try {
            this.sources.register(toDefinition(code, p));
        } catch (BusinessException e) {
            // register 即探活：连不上按数据源不可用报出，与迁移前的失败语义一致
            throw new IllegalStateException("数据源不可用 [" + code + "]: " + e.getMessage(), e);
        }
        this.query = new DynamicQuery(this.sources.require(code), this.sources.dialect(code))
                .maxRows(Integer.parseInt(p.getOrDefault(PROP_MAX_ROWS, String.valueOf(DEFAULT_MAX_ROWS))));
    }

    /** 报表侧属性 → z-util-jdbc 数据源定义 */
    private DataSourceDTO toDefinition(String code, Map<String, String> p) {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setDatasourceCode(code);
        dto.setDatasourceName(def.getName());
        dto.setDatasourceType(p.getOrDefault(PROP_DIALECT, DEFAULT_DIALECT));
        dto.setJdbcUrl(trimToNull(p.get(PROP_URL)));
        dto.setUserName(p.get(PROP_USERNAME));
        dto.setPw(p.get(PROP_PASSWORD));
        if (dto.getJdbcUrl() == null) {
            // host 可否省略由方言决定 (H2 内存库就没有 host), 缺了会在拼串时报出来
            dto.setDatasourceUrl(trimToNull(p.get(PROP_HOST)));
            dto.setSchemaMark(requireProp(p, PROP_DATABASE));
            dto.setUserName(requireProp(p, PROP_USERNAME));
            // 端口缺省交给方言：MySQL 3306、PG 5432，避免在此硬编码某一家
            String port = trimToNull(p.get(PROP_PORT));
            dto.setPortNumber(port == null ? null : Integer.valueOf(port));
        }
        return dto;
    }

    @Override
    public DataSourceDef def() {
        return def;
    }

    @Override
    public List<String> tables() {
        return probe("list tables", () -> query.tables());
    }

    @Override
    public Map<String, FieldType> schema(String table) {
        Map<String, SqlType> columns = probe("schema " + table, () -> query.columns(table));
        Map<String, FieldType> typed = new LinkedHashMap<>();
        for (Map.Entry<String, SqlType> column : columns.entrySet()) {
            typed.put(column.getKey(), JdbcTypeMapping.toFieldType(column.getValue()));
        }
        return typed;
    }

    @Override
    public Table read(String table) {
        // 列序与类型取库侧元数据，行取结果集；值域按报表引擎的宽松约定归一
        LinkedHashMap<String, FieldType> columns = new LinkedHashMap<>(schema(table));
        if (columns.isEmpty()) {
            throw new IllegalStateException("表不存在或无列: [" + def.getId() + "." + table + "]");
        }
        List<Map<String, Object>> rows = probe("read " + table, () -> query.list(Query.select().from(table)));
        for (Map<String, Object> row : rows) {
            row.replaceAll((column, value) -> JdbcTypeMapping.normalize(value));
        }
        return Table.fromRows(rows, columns);
    }

    @Override
    public void close() {
        sources.close();
    }

    /** 标识符非法仍抛 IllegalArgumentException (由调用方映射为 400)，库侧失败补上数据源上下文 */
    private <T> T probe(String unit, Supplier<T> call) {
        try {
            return call.get();
        } catch (BusinessException e) {
            throw new IllegalStateException("[" + def.getId() + "] " + unit + " failed: " + e.getMessage(), e);
        }
    }

    private static String requireProp(Map<String, String> p, String key) {
        String v = trimToNull(p.get(key));
        if (v == null) {
            throw new IllegalArgumentException("dataSource property required: " + key);
        }
        return v;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
