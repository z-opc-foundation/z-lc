package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.common.schema.AggType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 分组聚合 (链式): table.groupBy(keys...).agg(out, src, type)...toTable()
 * <p>
 * 语义: 分组键 null 归为一组 (对齐 SQL GROUP BY);
 * agg 的 srcCol 为 "*" 时执行 COUNT(*)。
 */
public class GroupBy {

    /** COUNT(*) 约定列名 */
    public static final String STAR = "*";

    private final Table table;
    private final String[] keys;
    private final List<AggSpec> aggs = new ArrayList<>();

    GroupBy(Table table, String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("groupBy requires at least one key");
        }
        this.table = table;
        this.keys = keys;
    }

    public GroupBy agg(String outName, String srcCol, AggType type) {
        Objects.requireNonNull(type, "agg type");
        if (AggType.COUNT != type && !STAR.equals(srcCol)) {
            table.column(srcCol); // 提前校验列存在
        }
        aggs.add(new AggSpec(outName, srcCol, type));
        return this;
    }

    public Table toTable() {
        // 分组: key tuple → 行索引 (保序)
        Map<List<Object>, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < table.nRows(); i++) {
            List<Object> key = new ArrayList<>(keys.length);
            for (String k : keys) {
                key.add(table.get(i, k));
            }
            List<Integer> bucket = groups.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                groups.put(key, bucket);
            }
            bucket.add(i);
        }
        // 输出
        List<String> names = new ArrayList<>(keys.length + aggs.size());
        List<FieldType> types = new ArrayList<>(names.size() + aggs.size());
        for (String k : keys) {
            names.add(k);
            types.add(table.column(k).type());
        }
        for (AggSpec a : aggs) {
            names.add(a.outName);
            types.add(a.outType());
        }
        List<List<Object>> rows = new ArrayList<>(groups.size());
        for (Map.Entry<List<Object>, List<Integer>> g : groups.entrySet()) {
            List<Object> row = new ArrayList<>(names.size());
            row.addAll(g.getKey());
            List<Integer> idxs = g.getValue();
            for (AggSpec a : aggs) {
                row.add(aggregateFor(a, idxs));
            }
            rows.add(row);
        }
        return Table.of(names, types, rows);
    }

    private Object aggregateFor(AggSpec a, List<Integer> idxs) {
        if (AggType.COUNT == a.type && STAR.equals(a.srcCol)) {
            return (long) idxs.size();
        }
        List<Object> vals = new ArrayList<>(idxs.size());
        for (int i : idxs) {
            Object v = table.get(i, a.srcCol);
            if (v != null) {
                vals.add(v); // 聚合忽略 null (对齐 SQL)
            }
        }
        return Aggs.aggregate(a.type, vals);
    }

    private final class AggSpec {
        final String outName;
        final String srcCol;
        final AggType type;

        AggSpec(String outName, String srcCol, AggType type) {
            this.outName = outName;
            this.srcCol = srcCol;
            this.type = type;
        }

        FieldType outType() {
            switch (type) {
                case COUNT:
                    return FieldType.INT;
                case SUM:
                    return FieldType.DOUBLE; // 实际值由聚合器决定 Long/Double, 类型标注取宽
                case AVG:
                    return FieldType.DOUBLE;
                case MIN:
                case MAX:
                default:
                    return STAR.equals(srcCol) ? FieldType.INT : table.column(srcCol).type();
            }
        }
    }
}
