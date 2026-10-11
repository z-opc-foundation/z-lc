package com.zifang.z.report.render.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;

/**
 * 折线图: groupBy(x).agg(y) → xAxis + series[line]。
 * 多系列 (encode.series) 为 M2 事项。
 */
public class LineOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.LINE;
    }

    @Override
    public ObjectNode build(WidgetSpec widget, Table data, ObjectMapper mapper) {
        AggTable at = AggTable.of(widget, data);
        ObjectNode root = mapper.createObjectNode();
        ArrayNode xAxis = root.putArray("xAxis");
        for (Object v : at.dimValues()) {
            xAxis.add(String.valueOf(v));
        }
        ObjectNode series = root.putArray("series").addObject();
        series.put("name", at.measure());
        series.put("type", "line");
        ArrayNode values = series.putArray("data");
        for (Object v : at.aggValues()) {
            values.add(mapper.valueToTree(v));
        }
        return root;
    }
}
