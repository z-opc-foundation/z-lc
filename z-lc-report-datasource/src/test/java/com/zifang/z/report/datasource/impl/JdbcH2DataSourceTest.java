package com.zifang.z.report.datasource.impl;

import com.zifang.z.report.common.schema.AggType;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDBC 源的方言化验证, 跑在 H2 内存库上 (无需外部实例)。
 * <p>
 * 建表用小写带引号标识符, 与 z-util-jdbc 的引用方式一致。库在测试里先建后注册:
 * H2 的 mem 库带 DB_CLOSE_DELAY=-1, 建库连接关掉不影响后续连接。
 */
class JdbcH2DataSourceTest {

    private static final String DB = "report_jdbc_h2";

    private static final String URL = "jdbc:h2:mem:" + DB + ";DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void createSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE \"t_user\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(32),"
                    + " \"dept\" VARCHAR(16), \"joined\" DATE)");
            st.execute("CREATE TABLE \"t_order\" (\"id\" BIGINT PRIMARY KEY, \"user_id\" BIGINT,"
                    + " \"amount\" DECIMAL(12,2), \"status\" VARCHAR(8))");
            st.execute("INSERT INTO \"t_user\" VALUES"
                    + " (1,'老王','华东',DATE '2026-01-05'),(2,'老张','华南',DATE '2026-02-11')");
            st.execute("INSERT INTO \"t_order\" VALUES"
                    + " (10,1,120.50,'PAID'),(11,1,80.00,'NEW'),(12,2,999.99,'PAID'),(13,9,12.00,'DONE')");
        }
    }

    @AfterAll
    static void dropSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement st = conn.createStatement()) {
            // H2 2.x 不允许 DROP SCHEMA PUBLIC, DROP ALL OBJECTS 才是清库的写法
            st.execute("DROP ALL OBJECTS");
        }
    }

    private static DataSourceDef def(Map<String, String> props) {
        return new DataSourceDef("h2-source", "h2", DataSourceType.JDBC, props);
    }

    /** 只给 database 时走方言拼装 (H2 方言把 database 当内存库名) */
    private static Map<String, String> assembledProps() {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcReportDataSource.PROP_DIALECT, "h2");
        props.put(JdbcReportDataSource.PROP_DATABASE, DB);
        props.put(JdbcReportDataSource.PROP_USERNAME, "sa");
        props.put(JdbcReportDataSource.PROP_PASSWORD, "");
        return props;
    }

    private static Map<String, String> urlProps() {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcReportDataSource.PROP_URL, URL);
        props.put(JdbcReportDataSource.PROP_USERNAME, "sa");
        props.put(JdbcReportDataSource.PROP_PASSWORD, "");
        return props;
    }

    @Test
    void tablesAndSchemaComeFromMetadata() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(assembledProps()))) {
            List<String> tables = src.tables();
            assertTrue(tables.contains("t_order"), "实际: " + tables);
            assertTrue(tables.contains("t_user"), "实际: " + tables);

            Map<String, FieldType> schema = src.schema("t_user");
            assertEquals(FieldType.INT, schema.get("id"));
            assertEquals(FieldType.STRING, schema.get("name"));
            assertEquals(FieldType.DATE, schema.get("joined"));
        }
    }

    @Test
    void readProducesTypedTableThatTheEngineAggregates() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(assembledProps()))) {
            Table orders = src.read("t_order");
            assertEquals(4, orders.nRows());
            assertEquals(FieldType.DOUBLE, orders.columnTypes().get(2));

            Table byStatus = orders.groupBy("status").agg("cnt", "*", AggType.COUNT).toTable();
            assertEquals(4, byStatus.colValues("cnt").stream()
                    .mapToLong(v -> ((Number) v).longValue()).sum());
        }
    }

    /** 给了整条 url 时不再要求 host/database, 方言也从 url 识别 */
    @Test
    void urlPropertyBypassesHostAssembly() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(urlProps()))) {
            assertTrue(src.tables().contains("t_order"));
            assertEquals(2, src.read("t_user").nRows());
        }
    }

    @Test
    void maxRowsPropertyBoundsWhatComesIntoMemory() {
        Map<String, String> props = assembledProps();
        props.put(JdbcReportDataSource.PROP_MAX_ROWS, "2");
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(props))) {
            assertEquals(2, src.read("t_order").nRows());
        }
    }

    @Test
    void unsafeTableIdentifierStillRejected() {
        try (JdbcReportDataSource src = new JdbcReportDataSource(def(assembledProps()))) {
            assertThrows(IllegalArgumentException.class, () -> src.read("t_order; drop table t_user"));
            assertThrows(IllegalArgumentException.class, () -> src.schema("`t_user`"));
        }
    }

    @Test
    void incompleteDefinitionFailsAsIllegalArgument() {
        // 缺 database
        Map<String, String> noDb = new HashMap<>();
        noDb.put(JdbcReportDataSource.PROP_DIALECT, "h2");
        noDb.put(JdbcReportDataSource.PROP_USERNAME, "sa");
        assertThrows(IllegalArgumentException.class, () -> new JdbcReportDataSource(def(noDb)));

        // 缺 username
        Map<String, String> noUser = new HashMap<>();
        noUser.put(JdbcReportDataSource.PROP_DIALECT, "h2");
        noUser.put(JdbcReportDataSource.PROP_DATABASE, DB);
        assertThrows(IllegalArgumentException.class, () -> new JdbcReportDataSource(def(noUser)));

        // mysql 拼装式必须有 host (H2 内存库可以没有)
        Map<String, String> mysqlNoHost = new HashMap<>();
        mysqlNoHost.put(JdbcReportDataSource.PROP_DATABASE, "whatever");
        mysqlNoHost.put(JdbcReportDataSource.PROP_USERNAME, "sa");
        assertThrows(IllegalArgumentException.class, () -> new JdbcReportDataSource(def(mysqlNoHost)));
    }

    @Test
    void unreachableSourceFailsAtConstruction() {
        Map<String, String> props = urlProps();
        props.put(JdbcReportDataSource.PROP_URL, "jdbc:h2:tcp://127.0.0.1:1/" + DB);
        // register 即探活: 连不上不再拖到首次 read 才暴露
        assertThrows(IllegalStateException.class, () -> new JdbcReportDataSource(def(props)));
    }
}
