package com.zifang.z.report.datasource.impl;

import com.zifang.z.report.common.schema.AggType;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实库集成测试 (z-opc 同款 RDS MySQL)。
 * <p>
 * 门控: 仅在设置 ZREPORT_IT_DB_HOST / ZREPORT_IT_DB_NAME / ZREPORT_IT_DB_USER / ZREPORT_IT_DB_PASSWORD
 * 环境变量时执行 (CI 无凭证自动跳过); 连接信息不落代码库。
 */
@EnabledIfEnvironmentVariable(named = "ZREPORT_IT_DB_HOST", matches = ".+")
class JdbcDataSourceTest {

    private JdbcReportDataSource source() {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcReportDataSource.PROP_HOST, System.getenv("ZREPORT_IT_DB_HOST"));
        props.put(JdbcReportDataSource.PROP_PORT, System.getenv().getOrDefault("ZREPORT_IT_DB_PORT", "3306"));
        props.put(JdbcReportDataSource.PROP_DATABASE, System.getenv("ZREPORT_IT_DB_NAME"));
        props.put(JdbcReportDataSource.PROP_USERNAME, System.getenv("ZREPORT_IT_DB_USER"));
        props.put(JdbcReportDataSource.PROP_PASSWORD, System.getenv().getOrDefault("ZREPORT_IT_DB_PASSWORD", ""));
        return new JdbcReportDataSource(new DataSourceDef("it-mysql", "it-mysql", DataSourceType.JDBC, props));
    }

    @Test
    void listTables_andProbeSchema() {
        try (JdbcReportDataSource src = source()) {
            List<String> tables = src.tables();
            assertFalse(tables.isEmpty(), "database should contain at least one table");

            Map<String, FieldType> schema = src.schema(tables.get(0));
            assertFalse(schema.isEmpty(), "schema should not be empty");
            assertNotNull(schema.keySet().iterator().next());
        }
    }

    @Test
    void read_firstTable_intoEngineAggregate() {
        try (JdbcReportDataSource src = source()) {
            List<String> tables = src.tables();
            Table t = src.read(tables.get(0));
            assertTrue(t.nRows() >= 0);

            // 引擎消费真实库数据: 分组 COUNT 总和 == 总行数
            Table agg = t.groupBy(t.columnNames().get(0)).agg("cnt", "*", AggType.COUNT).toTable();
            assertEquals(t.nRows(), agg.colValues("cnt").stream()
                    .mapToLong(v -> ((Number) v).longValue()).sum());
        }
    }

    @Test
    void unsafeIdentifier_rejected() {
        try (JdbcReportDataSource src = source()) {
            // 标识符白名单在 z-util-jdbc (Identifiers), 非法名在任何 SQL 拼出之前就被拒
            assertThrows(IllegalArgumentException.class, () -> src.read("t; drop table x"));
            assertThrows(IllegalArgumentException.class, () -> src.schema("t_user`"));
        }
    }
}
