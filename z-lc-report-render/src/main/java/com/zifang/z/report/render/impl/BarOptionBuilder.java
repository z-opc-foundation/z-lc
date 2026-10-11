package com.zifang.z.report.render.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;

/**
 * 柱状图: groupBy(x).agg(y) → xAxis + series[bar]。结构与折线一致, 仅 type 不同。
 */
public class BarOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.BAR;
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
        series.put("type", "bar");
        ArrayNode values = series.putArray("data");
        for (Object v : at.aggValues()) {
            values.add(mapper.valueToTree(v));
        }
        return root;
    }
}
