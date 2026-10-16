package com.zifang.z.lc.report.render.impl;

import com.zifang.z.lc.report.common.schema.ChartType;
import com.zifang.z.lc.report.common.schema.WidgetSpec;
import com.zifang.z.lc.report.dataset.engine.Table;
import com.zifang.z.lc.report.render.api.ChartOptionBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.List;

/**
 * 表格 (非原生 echarts, 渲染器约定结构):
 * {type:'table', columns:[{key,label}], rows:[...]} → 前端用 antd Table 解释。
 */
public class TableOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.TABLE;
    }

    @Override
    public Map<String, Object> build(WidgetSpec widget, Table data) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "table");
        List<String> columns = widget.getEncode() != null && widget.getEncode().getColumns() != null
                ? widget.getEncode().getColumns()
                : data.columnNames();
        List<Object> cols = new ArrayList<>();
        for (String c : columns) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("key", c);
            col.put("label", c);
            cols.add(col);
        }
        root.put("columns", cols);
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < data.nRows(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String c : columns) {
                row.put(c, data.get(i, c));
            }
            rows.add(row);
        }
        root.put("rows", rows);
        return root;
    }
}
