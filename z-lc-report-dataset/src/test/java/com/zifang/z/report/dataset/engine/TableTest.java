package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.common.schema.JoinType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest {

    private Table sales() {
        return Table.of(
                Arrays.asList("city", "amount", "qty"),
                Arrays.asList(FieldType.STRING, FieldType.DOUBLE, FieldType.INT),
                Arrays.asList(
                        Arrays.<Object>asList("hangzhou", 100.5, 2),
                        Arrays.<Object>asList("shanghai", 200.0, 3),
                        Arrays.<Object>asList("hangzhou", 50.0, 1),
                        Arrays.<Object>asList(null, 10.0, 1)
                ));
    }

    @Test
    void ofAndMeta() {
        Table t = sales();
        assertEquals(4, t.nRows());
        assertEquals(3, t.nCols());
        assertEquals(Arrays.asList("city", "amount", "qty"), t.columnNames());
        assertThrows(IllegalArgumentException.class, () -> t.column("nope"));
    }

    @Test
    void duplicateColumnRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Table.of(Arrays.asList("a", "a"),
                        Arrays.asList(FieldType.INT, FieldType.INT), null));
    }

    @Test
    void filterWithExpr() {
        Table t = sales().filter(Expr.gt(Expr.col("amount"), Expr.lit(60.0)));
        assertEquals(2, t.nRows());
        assertTrue(t.toRows().stream().allMatch(r -> ((Number) r.get("amount")).doubleValue() > 60.0));
    }

    @Test
    void filterNullCompareExcluded() {
        // city 为 null 的行在任何 city 比较中均为 false (SQL 语义)
        Table t = sales().filter(Expr.eq(Expr.col("city"), Expr.lit("hangzhou")));
        assertEquals(2, t.nRows());
    }

    @Test
    void selectAndLimit() {
        Table t = sales().select("city", "qty").limit(2);
        assertEquals(2, t.nCols());
        assertEquals(2, t.nRows());
    }

    @Test
    void sortNullAlwaysLast() {
        Table asc = sales().sort("city", true);
        assertNull(asc.get(asc.nRows() - 1, "city"));
        assertEquals("hangzhou", asc.get(0, "city"));

        Table desc = sales().sort("city", false);
        assertNull(desc.get(desc.nRows() - 1, "city"));
        assertEquals("shanghai", desc.get(0, "city"));
    }

    @Test
    void sortNumeric() {
        Table desc = sales().sort("qty", false);
        assertEquals(3, ((Number) desc.get(0, "qty")).intValue());
    }

    @Test
    void assignComputedColumn() {
        Table t = sales().assign("amount_x2", FieldType.DOUBLE,
                Expr.mul(Expr.col("amount"), Expr.lit(2)));
        assertEquals(4, t.nCols());
        assertEquals(201.0, (Double) t.get(0, "amount_x2"), 1e-9);
        // null 传递: amount 为 null 的行计算列亦为 null (sales 表第 4 行 city=null 但 amount=10,
        // 其 amount_x2 = 20.0 属正常计算; null 传递用独立数据验证)
        Table t2 = Table.of(
                Arrays.asList("amount"),
                Arrays.asList(FieldType.DOUBLE),
                java.util.Collections.singletonList(java.util.Collections.singletonList(null)));
        Table t3 = t2.assign("x2", FieldType.DOUBLE,
                Expr.mul(Expr.col("amount"), Expr.lit(2)));
        assertNull(t3.get(0, "x2"));
    }

    @Test
    void innerJoinMultiKeys() {
        Table left = Table.of(
                Arrays.asList("uid", "amt"),
                Arrays.asList(FieldType.INT, FieldType.DOUBLE),
                Arrays.asList(
                        Arrays.<Object>asList(1, 10.0),
                        Arrays.<Object>asList(2, 20.0),
                        Arrays.<Object>asList(3, 30.0)
                ));
        Table right = Table.of(
                Arrays.asList("id", "name"),
                Arrays.asList(FieldType.INT, FieldType.STRING),
                Arrays.asList(
                        Arrays.<Object>asList(1, "alice"),
                        Arrays.<Object>asList(2, "bob"),
                        Arrays.<Object>asList(2, "bob2")
                ));
        Table joined = left.join(right, JoinType.INNER,
                new String[]{"uid"}, new String[]{"id"});
        assertEquals(3, joined.nRows()); // uid=1 匹配 1 行, uid=2 匹配 2 行, uid=3 无匹配
        assertEquals(Arrays.asList("uid", "amt", "id", "name"), joined.columnNames());
    }

    @Test
    void leftJoinKeepsUnmatchedAndDuplicateColumnSuffixed() {
        Table left = Table.of(
                Arrays.asList("uid", "name"),
                Arrays.asList(FieldType.INT, FieldType.STRING),
                Arrays.asList(
                        Arrays.<Object>asList(1, "o1"),
                        Arrays.<Object>asList(9, "o9")
                ));
        Table right = Table.of(
                Arrays.asList("id", "name"),
                Arrays.asList(FieldType.INT, FieldType.STRING),
                Arrays.asList(
                        Arrays.<Object>asList(1, "alice")
                ));
        Table joined = left.join(right, JoinType.LEFT,
                new String[]{"uid"}, new String[]{"id"});
        assertEquals(2, joined.nRows());
        assertEquals(Arrays.asList("uid", "name", "id", "name_r"), joined.columnNames());
        assertEquals("alice", joined.get(0, "name_r"));
        assertNull(joined.get(1, "name_r")); // uid=9 无匹配, 右侧补 null
    }

    @Test
    void joinNullKeyNeverMatches() {
        Table left = Table.of(
                Arrays.asList("k", "v"),
                Arrays.asList(FieldType.STRING, FieldType.INT),
                Arrays.<List<Object>>asList(Arrays.<Object>asList(null, 1)));
        Table right = Table.of(
                Arrays.asList("k", "w"),
                Arrays.asList(FieldType.STRING, FieldType.INT),
                Arrays.<List<Object>>asList(Arrays.<Object>asList(null, 2)));
        Table joined = left.join(right, JoinType.INNER, new String[]{"k"}, new String[]{"k"});
        assertEquals(0, joined.nRows());
    }

    @Test
    void groupByAggregations() {
        Table grouped = sales().groupBy("city")
                .agg("total", "amount", com.zifang.z.report.common.schema.AggType.SUM)
                .agg("cnt", GroupBy.STAR, com.zifang.z.report.common.schema.AggType.COUNT)
                .agg("avgQty", "qty", com.zifang.z.report.common.schema.AggType.AVG)
                .toTable();
        // hangzhou: 100.5+50=150.5, cnt=2, avg=1.5; shanghai: 200, 1, 3.0; null 组: 10, 1, 1.0
        assertEquals(3, grouped.nRows());
        Map<String, Object> hz = byKey(grouped, "city", "hangzhou");
        assertEquals(150.5, (Double) hz.get("total"), 1e-9);
        assertEquals(2L, ((Number) hz.get("cnt")).longValue());
        assertEquals(1.5, (Double) hz.get("avgQty"), 1e-9);
        Map<String, Object> nullGroup = byKey(grouped, "city", null);
        assertEquals(10.0, (Double) nullGroup.get("total"), 1e-9);
    }

    @Test
    void sumAllIntegerReturnsLong() {
        Table t = Table.of(
                Arrays.asList("g", "n"),
                Arrays.asList(FieldType.STRING, FieldType.INT),
                Arrays.asList(
                        Arrays.<Object>asList("a", 1),
                        Arrays.<Object>asList("a", 2)
                ));
        Map<String, Object> row = t.groupBy("g")
                .agg("s", "n", com.zifang.z.report.common.schema.AggType.SUM)
                .toTable().toRows().get(0);
        assertEquals(3L, row.get("s")); // 全整型 SUM → Long
    }

    @Test
    void fromRowsRoundTrip() {
        LinkedHashMap<String, FieldType> defs = new LinkedHashMap<>();
        defs.put("a", FieldType.INT);
        defs.put("b", FieldType.STRING);
        Table t = Table.fromRows(Arrays.asList(
                rowOf("a", 1, "b", "x"),
                rowOf("a", 2, "b", "y")), defs);
        assertEquals(2, t.nRows());
        assertEquals(1, ((Number) t.get(0, "a")).intValue());
    }

    private Map<String, Object> byKey(Table t, String keyCol, Object keyVal) {
        for (Map<String, Object> r : t.toRows()) {
            Object v = r.get(keyCol);
            if (keyVal == null ? v == null : keyVal.equals(v)) {
                return r;
            }
        }
        throw new AssertionError("group not found: " + keyVal);
    }

    private Map<String, Object> rowOf(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
