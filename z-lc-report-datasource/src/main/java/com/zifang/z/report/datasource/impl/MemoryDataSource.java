package com.zifang.z.report.datasource.impl;

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

/**
 * 内存数据源 (M1 演示/测试/端到端验证用):
 * tables: 源内数据单元 → 行视图列表; colTypes: 数据单元 → 列类型定义。
 * <p>
 * 类型缺省时按首行值推断 (STRING 兜底)。
 */
public class MemoryDataSource implements ReportDataSource {

    private final DataSourceDef def;
    private final Map<String, List<Map<String, Object>>> tables;
    private final Map<String, Map<String, FieldType>> colTypes;

    public MemoryDataSource(DataSourceDef def,
                            Map<String, List<Map<String, Object>>> tables,
                            Map<String, Map<String, FieldType>> colTypes) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(tables, "tables");
        if (def.getType() != DataSourceType.MEMORY) {
            throw new IllegalArgumentException("MemoryDataSource requires type=MEMORY, got: " + def.getType());
        }
        this.def = def;
        this.tables = tables;
        this.colTypes = colTypes == null ? Collections.emptyMap() : colTypes;
    }

    @Override
    public DataSourceDef def() {
        return def;
    }

    @Override
    public java.util.List<String> tables() {
        return new java.util.ArrayList<>(tables.keySet());
    }

    @Override
    public Map<String, FieldType> schema(String table) {
        Map<String, FieldType> declared = colTypes.get(table);
        if (declared != null) {
            return declared;
        }
        // 推断: 首行值类型
        Map<String, FieldType> inferred = new LinkedHashMap<>();
        List<Map<String, Object>> rows = tables.get(table);
        if (rows != null && !rows.isEmpty()) {
            for (Map.Entry<String, Object> e : rows.get(0).entrySet()) {
                inferred.put(e.getKey(), ValueInference.infer(e.getValue()));
            }
        }
        return inferred;
    }

    @Override
    public Table read(String table) {
        List<Map<String, Object>> rows = tables.get(table);
        if (rows == null) {
            throw new IllegalArgumentException("table not found in source [" + def.getId() + "]: " + table);
        }
        LinkedHashMap<String, FieldType> defs = new LinkedHashMap<>(schema(table));
        return Table.fromRows(rows, defs);
    }
}
