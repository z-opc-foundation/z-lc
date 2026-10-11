package com.zifang.z.report.datasource.impl;

import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.datasource.spi.ReportDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 内存 SQL 源: 基表来自已注册源, 数据单元是一条跨源 SELECT。
 * 基表用内存源模拟, 与 JDBC 基表走的是同一条 {@link ReportDataSource} 通道。
 */
class InMemoryReportDataSourceTest {

    private static final String JOIN_SQL = "SELECT u.dept, COUNT(*) AS cnt, SUM(o.amount) AS total"
            + " FROM t_order o INNER JOIN t_user u ON o.user_id = u.id GROUP BY u.dept";

    private final DataSourceRegistry registry = new DataSourceRegistry();

    @BeforeEach
    void registerBaseSources() {
        registry.register(memorySource("crm", "t_user",
                Arrays.asList(
                        row("id", 1L, "name", "老王", "dept", "华东"),
                        row("id", 2L, "name", "老张", "dept", "华南"))));
        registry.register(memorySource("erp", "t_order",
                // user_id=9 没有对应的人, INNER JOIN 应当把它丢掉
                Arrays.asList(
                        row("id", 10L, "user_id", 1L, "amount", 120.50),
                        row("id", 11L, "user_id", 1L, "amount", 80.00),
                        row("id", 12L, "user_id", 2L, "amount", 999.99),
                        row("id", 13L, "user_id", 9L, "amount", 12.00))));
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }

    private static ReportDataSource memorySource(String id, String table, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        tables.put(table, rows);
        return new MemoryDataSource(new DataSourceDef(id, id, DataSourceType.MEMORY, null), tables, null);
    }

    private InMemoryReportDataSource sqlSource(Map<String, String> sqlByUnit,
                                               Map<String, Map<String, FieldType>> colTypes) {
        Map<String, List<String>> bases = new LinkedHashMap<>();
        bases.put("crm", Arrays.asList("t_user"));
        bases.put("erp", Arrays.asList("t_order"));
        return new InMemoryReportDataSource(
                new DataSourceDef("derived", "跨源派生", DataSourceType.SQL, null),
                bases, sqlByUnit, colTypes, registry);
    }

    private static Map<String, String> unit(String name, String sql) {
        Map<String, String> sqlByUnit = new LinkedHashMap<>();
        sqlByUnit.put(name, sql);
        return sqlByUnit;
    }

    @Test
    void joinAndGroupByRunInMemoryAcrossSources() {
        InMemoryReportDataSource src = sqlSource(unit("dept_stat", JOIN_SQL), null);
        assertEquals(Arrays.asList("dept_stat"), src.tables());

        Table result = src.read("dept_stat");
        assertEquals(Arrays.asList("dept", "cnt", "total"), result.columnNames());
        assertEquals(2, result.nRows());
        List<Object> depts = result.colValues("dept");
        List<Object> totals = result.colValues("total");
        // 华东 = 120.50 + 80.00; 华南 = 999.99; user_id=9 的 12.00 被 INNER JOIN 丢掉
        for (int i = 0; i < depts.size(); i++) {
            double expected = "华东".equals(depts.get(i)) ? 200.50 : 999.99;
            assertEquals(expected, ((Number) totals.get(i)).doubleValue(), 0.001);
        }
        // 推断出的类型让引擎按数值/字符串分别处理
        assertEquals(FieldType.STRING, result.columnTypes().get(0));
        assertEquals(FieldType.DOUBLE, result.columnTypes().get(2));
    }

    @Test
    void declaredColumnTypesOverrideInference() {
        Map<String, Map<String, FieldType>> colTypes = new LinkedHashMap<>();
        Map<String, FieldType> declared = new LinkedHashMap<>();
        declared.put("dept", FieldType.STRING);
        declared.put("cnt", FieldType.INT);
        declared.put("total", FieldType.STRING);
        colTypes.put("dept_stat", declared);

        Table result = sqlSource(unit("dept_stat", JOIN_SQL), colTypes).read("dept_stat");
        assertEquals(FieldType.INT, result.columnTypes().get(1));
        assertEquals(FieldType.STRING, result.columnTypes().get(2));
    }

    @Test
    void baseDataIsReadFreshOnEveryQuery() {
        InMemoryReportDataSource src = sqlSource(unit("orders", "SELECT COUNT(*) AS cnt FROM t_order"), null);
        assertEquals(4L, ((Number) src.read("orders").get(0, "cnt")).longValue());

        // 换掉基表源, 不重注册派生源: 下次取数即读到新数据
        List<Map<String, Object>> more = new ArrayList<>();
        more.add(row("id", 20L, "user_id", 1L, "amount", 1.00));
        registry.register(memorySource("erp", "t_order", more));
        assertEquals(1L, ((Number) src.read("orders").get(0, "cnt")).longValue());
    }

    @Test
    void unknownUnitAndMissingBaseAreRejected() {
        InMemoryReportDataSource src = sqlSource(unit("dept_stat", JOIN_SQL), null);
        assertThrows(IllegalArgumentException.class, () -> src.read("nope"));

        Map<String, List<String>> bases = new LinkedHashMap<>();
        bases.put("missing-source", Arrays.asList("t_user"));
        InMemoryReportDataSource dangling = new InMemoryReportDataSource(
                new DataSourceDef("derived2", "d", DataSourceType.SQL, null),
                bases, unit("x", "SELECT * FROM t_user"), null, registry);
        assertThrows(IllegalArgumentException.class, () -> dangling.read("x"));
    }

    @Test
    void defMustDeclareSqlTypeAndCarryContent() {
        Map<String, List<String>> bases = new LinkedHashMap<>();
        bases.put("crm", Arrays.asList("t_user"));
        DataSourceDef wrong = new DataSourceDef("s", "s", DataSourceType.JDBC, null);
        assertThrows(IllegalArgumentException.class, () -> new InMemoryReportDataSource(
                wrong, bases, unit("x", "SELECT 1"), null, registry));

        DataSourceDef def = new DataSourceDef("s", "s", DataSourceType.SQL, null);
        assertThrows(IllegalArgumentException.class, () -> new InMemoryReportDataSource(
                def, bases, new LinkedHashMap<String, String>(), null, registry));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryReportDataSource(
                def, new LinkedHashMap<String, List<String>>(), unit("x", "SELECT 1"), null, registry));
    }
}
