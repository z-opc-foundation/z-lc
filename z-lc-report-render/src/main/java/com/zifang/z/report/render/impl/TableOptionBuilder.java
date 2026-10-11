package com.zifang.z.report.render.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;

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
    public ObjectNode build(WidgetSpec widget, Table data, ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "table");
        List<String> columns = widget.getEncode() != null && widget.getEncode().getColumns() != null
                ? widget.getEncode().getColumns()
                : data.columnNames();
        ArrayNode cols = root.putArray("columns");
        for (String c : columns) {
            ObjectNode col = cols.addObject();
            col.put("key", c);
            col.put("label", c);
        }
        ArrayNode rows = root.putArray("rows");
        for (int i = 0; i < data.nRows(); i++) {
            ObjectNode row = rows.addObject();
            for (String c : columns) {
                row.set(c, mapper.valueToTree(data.get(i, c)));
            }
        }
        return root;
    }
}
