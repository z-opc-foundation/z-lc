package com.zifang.z.lc.report.datasource.impl;

import com.zifang.z.lc.report.common.schema.DataSourceDef;
import com.zifang.z.lc.report.common.schema.DataSourceType;
import com.zifang.z.lc.report.common.schema.FieldType;
import com.zifang.z.lc.report.dataset.engine.Table;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外部池构造器（2026-10-26 报表域收口）：池由外部目录（z-dms 桥接）递进来，
 * 本类只做适配 —— 不自建池、close() 不关外部池、方言从连接元数据识别。
 * 用 H2 的 JdbcDataSource 当"外部池"替身（单连接语义足以验证契约）。
 */
class JdbcReportDataSourceExternalPoolTest {

    private static final String MEM_DB = "jdbc:h2:mem:extpool;DB_CLOSE_DELAY=-1"
            + ";DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";

    @BeforeAll
    static void seed() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL(MEM_DB);
        try (Connection conn = h2.getConnection(); Statement st = conn.createStatement()) {
            st.execute("drop table if exists t_ext");
            st.execute("create table t_ext(id int primary key, name varchar(32))");
            st.execute("insert into t_ext values (1, 'a'), (2, 'b')");
        }
    }

    private JdbcDataSource externalPool() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL(MEM_DB);
        return h2;
    }

    private DataSourceDef def(Map<String, String> props) {
        return new DataSourceDef("dms-ext", "外部池源", DataSourceType.JDBC, props);
    }

    @Test
    void tables_schema_read_viaExternalPool() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(null), externalPool())) {
            assertTrue(src.tables().contains("t_ext"), "应能探到表清单, 实际: " + src.tables());

            Map<String, FieldType> schema = src.schema("t_ext");
            assertEquals(2, schema.size());
            assertEquals(FieldType.INT, schema.get("id"));
            assertEquals(FieldType.STRING, schema.get("name"));

            Table table = src.read("t_ext");
            assertEquals(2, table.nRows());
        }
    }

    @Test
    void close_doesNotCloseExternalPool() {
        JdbcDataSource pool = externalPool();
        JdbcReportDataSource src = new JdbcReportDataSource(
                def(Collections.singletonMap(JdbcReportDataSource.PROP_MAX_ROWS, "10")), pool);
        src.read("t_ext");
        src.close();
        // close 只声明不归自己管的生命周期；外部池必须仍然可用
        assertEquals(2, src.read("t_ext").nRows());
    }

    @Test
    void maxRows_optionHonored() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(
                def(Collections.singletonMap(JdbcReportDataSource.PROP_MAX_ROWS, "1")), externalPool())) {
            assertEquals(1, src.read("t_ext").nRows());
        }
    }

    @Test
    void nonJdbcTypeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JdbcReportDataSource(
                new DataSourceDef("x", "x", DataSourceType.MEMORY, null), externalPool()));
    }
}
