package com.zifang.z.report.render.impl;

import com.zifang.z.report.common.schema.AggType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Expr;
import com.zifang.z.report.dataset.engine.Filters;
import com.zifang.z.report.dataset.engine.Table;

import java.util.List;

/**
 * 维度/度量聚合视图 (LINE/BAR/PIE/KPI 共用):
 * 应用 widget 过滤 → groupBy(维度).agg(度量) 的轻量封装。
 */
final class AggTable {

    private final Table table;
    private final String dim;
    private final String measure;

    private AggTable(Table table, String dim, String measure) {
        this.table = table;
        this.dim = dim;
        this.measure = measure;
    }

    static AggTable of(WidgetSpec widget, Table data) {
        String dim = widget.getEncode().getX() != null
                ? widget.getEncode().getX() : widget.getEncode().getCategory();
        String measure = widget.getEncode().getY() != null
                ? widget.getEncode().getY() : widget.getEncode().getValue();
        if (measure == null) {
            throw new IllegalArgumentException("widget encode requires measure: " + widget.getId());
        }
        Expr filter = Filters.toExpr(widget.getFilters());
        Table filtered = filter == null ? data : data.filter(filter);
        AggType agg = widget.getAgg() == null ? AggType.SUM : widget.getAgg();
        if (dim == null) {
            // 无维度 (KPI 指标卡语义): 全表聚合为单行
            Object value = filtered.aggregate(measure, agg);
            Table single = Table.of(
                    java.util.Collections.singletonList(measure),
                    java.util.Collections.singletonList(inferType(value)),
                    java.util.Collections.singletonList(
                            java.util.Collections.singletonList(value)));
            return new AggTable(single, null, measure);
        }
        Table grouped = filtered.groupBy(dim).agg(measure, measure, agg).toTable();
        return new AggTable(grouped, dim, measure);
    }

    private static com.zifang.z.report.common.schema.FieldType inferType(Object v) {
        if (v instanceof Float || v instanceof Double) {
            return com.zifang.z.report.common.schema.FieldType.DOUBLE;
        }
        if (v instanceof Number) {
            return com.zifang.z.report.common.schema.FieldType.INT;
        }
        return com.zifang.z.report.common.schema.FieldType.STRING;
    }

    String dim() {
        return dim;
    }

    String measure() {
        return measure;
    }

    List<Object> dimValues() {
        return table.colValues(dim);
    }

    List<Object> aggValues() {
        return table.colValues(measure);
    }

    Object singleValue() {
        return table.nRows() == 0 ? null : table.get(0, measure);
    }
}
