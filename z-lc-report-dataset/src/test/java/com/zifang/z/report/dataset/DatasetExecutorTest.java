package com.zifang.z.report.dataset;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.FilterOp;
import com.zifang.z.report.common.schema.JoinDef;
import com.zifang.z.report.common.schema.JoinType;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.common.schema.WidgetFilter;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据集执行器测试。
 * 源解析用 lambda 内存实现 (dataset 模块不依赖 datasource 模块, 依赖倒置)。
 */
class DatasetExecutorTest {

    private DatasetExecutor executor;

    @BeforeEach
    void setUp() {
        // 主源: 订单明细
        Table orders = Table.of(
                Arrays.asList("uid", "city", "amount"),
                Arrays.asList(FieldType.INT, FieldType.STRING, FieldType.INT),
                Arrays.asList(
                        Arrays.<Object>asList(1, "hangzhou", 100),
                        Arrays.<Object>asList(2, "shanghai", 200),
                        Arrays.<Object>asList(1, "hangzhou", 50),
                        Arrays.<Object>asList(3, "shenzhen", 30)));

        // 维表: 客户 (与主源重名列 uid 由 join 侧唯一化处理)
        Table customers = Table.of(
                Arrays.asList("uid", "name", "level"),
                Arrays.asList(FieldType.INT, FieldType.STRING, FieldType.STRING),
                Arrays.asList(
                        Arrays.<Object>asList(1, "alice", "A"),
                        Arrays.<Object>asList(2, "bob", "B")));

        Map<String, Table> memTables = new HashMap<>();
        memTables.put("ds-orders.orders", orders);
        memTables.put("ds-cust.customers", customers);

        executor = new DatasetExecutor((sid, table) -> memTables.get(sid + "." + table));
    }

    @Test
    void executeSingleSourceWithFilter() {
        DatasetSchema schema = new DatasetSchema();
        schema.setId("d1");
        schema.setSourceId("ds-orders");
        schema.setBaseTable("orders");
        schema.setFilters(Arrays.asList(new WidgetFilter("city", FilterOp.EQ, "hangzhou")));

        QueryResult r = executor.execute(schema);
        assertEquals(2, r.getTotal());
    }

    @Test
    void executeWithJoin() {
        DatasetSchema schema = new DatasetSchema();
        schema.setId("d2");
        schema.setSourceId("ds-orders");
        schema.setBaseTable("orders");
        schema.setJoins(Arrays.asList(new JoinDef(
                "ds-cust", "customers", JoinType.LEFT,
                new String[]{"uid"}, new String[]{"uid"})));

        QueryResult r = executor.execute(schema);
        assertEquals(4, r.getTotal()); // LEFT 保留全部订单
        // uid=3 无客户 → name 为 null
        boolean hasNullName = false;
        for (Map<String, Object> row : r.getRows()) {
            if (row.get("name") == null) {
                hasNullName = true;
            }
        }
        assertTrue(hasNullName);
    }

    @Test
    void unregisteredSourceFailsFast() {
        DatasetSchema schema = new DatasetSchema();
        schema.setSourceId("nope");
        schema.setBaseTable("orders");
        assertThrows(IllegalArgumentException.class, () -> executor.execute(schema));
    }

    @Test
    void computedFields_parsedAndApplied() {
        // 计算列: 字符串表达式 → ExprParser → Expr 对象树 (白名单受限求值)
        com.zifang.z.report.common.schema.ComputedField cf =
                new com.zifang.z.report.common.schema.ComputedField();
        cf.setName("amount_x2");
        cf.setExpr("amount * 2");
        cf.setType(com.zifang.z.report.common.schema.FieldType.DOUBLE);
        com.zifang.z.report.common.schema.ComputedField flag =
                new com.zifang.z.report.common.schema.ComputedField();
        flag.setName("big");
        flag.setExpr("amount >= 100 AND city == 'hangzhou'");
        flag.setType(com.zifang.z.report.common.schema.FieldType.BOOL);

        DatasetSchema schema = new DatasetSchema();
        schema.setSourceId("ds-orders");
        schema.setBaseTable("orders");
        schema.setComputedFields(java.util.Arrays.asList(cf, flag));

        com.zifang.z.report.common.schema.QueryResult r = executor.execute(schema);
        Map<String, Object> first = r.getRows().get(0);
        assertEquals(200.0, ((Number) first.get("amount_x2")).doubleValue(), 1e-9);
        assertEquals(Boolean.TRUE, first.get("big"));
        assertEquals(5, r.getColumns().size()); // 3 原始列 + 2 计算列
    }
}
