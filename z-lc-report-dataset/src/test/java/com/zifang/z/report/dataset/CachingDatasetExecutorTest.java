package com.zifang.z.report.dataset;

import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.WidgetFilter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** TTL 结果缓存执行器测试: 命中复用/键变化重算/TTL=0 直通 */
class CachingDatasetExecutorTest {

    /** 计数 resolver: 每次 resolve 记一次, 验证缓存是否跳过执行 */
    private static class CountingResolver implements TableSourceResolver {
        final Map<String, com.zifang.z.report.dataset.engine.Table> tables = new LinkedHashMap<>();
        final AtomicInteger reads = new AtomicInteger();

        @Override
        public com.zifang.z.report.dataset.engine.Table resolve(String sourceId, String table) {
            reads.incrementAndGet();
            return tables.get(sourceId + "." + table);
        }
    }

    private DatasetSchema schema() {
        DatasetSchema s = new DatasetSchema();
        s.setId("ds-1");
        s.setSourceId("mem");
        s.setBaseTable("orders");
        return s;
    }

    @Test
    void cacheHit_skipsExecution() {
        CountingResolver resolver = new CountingResolver();
        resolver.tables.put("mem.orders", com.zifang.z.report.dataset.engine.Table.of(
                Arrays.asList("city", "amount"),
                Arrays.asList(com.zifang.z.report.common.schema.FieldType.STRING, com.zifang.z.report.common.schema.FieldType.INT),
                Collections.singletonList(Arrays.asList("hz", 100))));
        CachingDatasetExecutor exec = new CachingDatasetExecutor(resolver, 60_000, 16);

        DatasetSchema s = schema();
        com.zifang.z.report.common.schema.QueryResult first = exec.execute(s);
        com.zifang.z.report.common.schema.QueryResult second = exec.execute(s);
        assertSame(first, second, "TTL 内应返回同一共享实例");
        assertEquals(1, resolver.reads.get(), "同 schema 只应读源一次");
    }

    @Test
    void differentContent_recomputes() {
        CountingResolver resolver = new CountingResolver();
        resolver.tables.put("mem.orders", com.zifang.z.report.dataset.engine.Table.of(
                Arrays.asList("city", "amount"),
                Arrays.asList(com.zifang.z.report.common.schema.FieldType.STRING, com.zifang.z.report.common.schema.FieldType.INT),
                Arrays.asList(Arrays.asList("hz", 100), Arrays.asList("sh", 200))));
        CachingDatasetExecutor exec = new CachingDatasetExecutor(resolver, 60_000, 16);

        DatasetSchema s1 = schema();
        DatasetSchema s2 = schema();
        WidgetFilter f = new WidgetFilter();
        f.setField("city");
        f.setOp(com.zifang.z.report.common.schema.FilterOp.EQ);
        f.setValue("hz");
        s2.setFilters(new ArrayList<>(Collections.singletonList(f)));

        exec.execute(s1);
        com.zifang.z.report.common.schema.QueryResult filtered = exec.execute(s2);
        assertNotSame(exec.execute(s1), filtered, "内容不同的 schema 缓存键应不同");
        assertEquals(2, resolver.reads.get(), "两种内容各读源一次");
        assertEquals(1, filtered.getRows().size(), "过滤后仅 hz 一行");
    }

    @Test
    void ttlZero_passThrough() {
        CountingResolver resolver = new CountingResolver();
        resolver.tables.put("mem.orders", com.zifang.z.report.dataset.engine.Table.of(
                Arrays.asList("city", "amount"),
                Arrays.asList(com.zifang.z.report.common.schema.FieldType.STRING, com.zifang.z.report.common.schema.FieldType.INT),
                Collections.singletonList(Arrays.asList("hz", 100))));
        CachingDatasetExecutor exec = new CachingDatasetExecutor(resolver, 0, 16);
        exec.execute(schema());
        exec.execute(schema());
        assertEquals(2, resolver.reads.get(), "TTL=0 直通不缓存");
    }
}
