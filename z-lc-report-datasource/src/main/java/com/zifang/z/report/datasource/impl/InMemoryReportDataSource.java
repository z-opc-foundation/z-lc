package com.zifang.z.report.datasource.impl;

import com.zifang.util.db.memory.InMemoryTables;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.datasource.spi.ReportDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 内存 SQL 数据源：把已注册源的表搬进内存 SQL 引擎，数据单元就是一条 SELECT。
 * <p>
 * 解决的问题：单个 JDBC/CSV 源只能整表读，跨源、跨实例的 join 在库侧要么做不了（不同实例），
 * 要么不该做（源库只读、方言不支持、要拉的数据本就要二次加工）。取回内存后按标准 SQL 加工，
 * 产出的仍是报表引擎的 {@link Table}，dataset / render / AI 链路零改动。
 * <p>
 * 基表在每次取数时从源侧重新读入，不做常驻缓存 (结果缓存已有 CachingDatasetExecutor 负责)，
 * 因此源侧数据变更不会被这里缓存放大。表名取基表声明里的最后一段。
 */
public class InMemoryReportDataSource implements ReportDataSource {

    private final DataSourceDef def;
    /** 数据单元 → SELECT */
    private final Map<String, String> sqlByUnit;
    /** 源 id → 要搬进内存的表 */
    private final Map<String, List<String>> baseTables;
    /** 数据单元 → 列类型声明, 缺省时按首行值推断 */
    private final Map<String, Map<String, FieldType>> colTypes;
    private final DataSourceRegistry sources;

    public InMemoryReportDataSource(DataSourceDef def,
                                    Map<String, List<String>> baseTables,
                                    Map<String, String> sqlByUnit,
                                    Map<String, Map<String, FieldType>> colTypes,
                                    DataSourceRegistry sources) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(baseTables, "baseTables");
        Objects.requireNonNull(sqlByUnit, "sqlByUnit");
        if (def.getType() != DataSourceType.SQL) {
            throw new IllegalArgumentException("InMemoryReportDataSource requires type=SQL, got: " + def.getType());
        }
        if (sqlByUnit.isEmpty()) {
            throw new IllegalArgumentException("至少声明一个数据单元 (SELECT)");
        }
        if (baseTables.isEmpty()) {
            throw new IllegalArgumentException("至少声明一张基表");
        }
        this.def = def;
        this.baseTables = new LinkedHashMap<>(baseTables);
        this.sqlByUnit = new LinkedHashMap<>(sqlByUnit);
        this.colTypes = colTypes == null ? Collections.emptyMap() : colTypes;
        this.sources = Objects.requireNonNull(sources, "sources");
    }

    @Override
    public DataSourceDef def() {
        return def;
    }

    @Override
    public List<String> tables() {
        return new ArrayList<>(sqlByUnit.keySet());
    }

    @Override
    public Map<String, FieldType> schema(String unit) {
        Map<String, FieldType> declared = declared(unit);
        return declared != null ? declared : infer(requireSql(unit));
    }

    @Override
    public Table read(String unit) {
        // 一次 SELECT 只跑一遍: 声明了列类型就用声明, 否则从结果首行推断
        Map<String, FieldType> declared = declared(unit);
        List<Map<String, Object>> rows = requireSql(unit);
        LinkedHashMap<String, FieldType> columns =
                new LinkedHashMap<>(declared != null ? declared : infer(rows));
        return Table.fromRows(rows, columns);
    }

    private Map<String, FieldType> declared(String unit) {
        Map<String, FieldType> declared = colTypes.get(unit);
        return declared == null || declared.isEmpty() ? null : declared;
    }

    private static Map<String, FieldType> infer(List<Map<String, Object>> rows) {
        Map<String, FieldType> inferred = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            for (Map.Entry<String, Object> column : rows.get(0).entrySet()) {
                inferred.put(column.getKey(), ValueInference.infer(column.getValue()));
            }
        }
        return inferred;
    }

    /** 基表每次现取, 不常驻: 常驻会让源侧变更被缓存放大, 而结果缓存已由 CachingDatasetExecutor 负责 */
    private List<Map<String, Object>> requireSql(String unit) {
        String sql = sqlByUnit.get(unit);
        if (sql == null) {
            throw new IllegalArgumentException("data unit not found in source [" + def.getId() + "]: " + unit);
        }
        InMemoryTables memory = new InMemoryTables();
        for (Map.Entry<String, List<String>> base : baseTables.entrySet()) {
            ReportDataSource source = sources.require(base.getKey());
            for (String table : base.getValue()) {
                memory.load(shortName(table), source.read(table).toRows());
            }
        }
        return memory.sql(sql);
    }

    /** 内存表名取最后一段, 让 {@code db.t_order} 这类限定名在 SQL 里仍写作 t_order */
    private static String shortName(String table) {
        int dot = table.lastIndexOf('.');
        return dot < 0 ? table : table.substring(dot + 1);
    }
}
