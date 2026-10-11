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
 * 饼图: groupBy(category).agg(value) → data[{name, value}]。
 */
public class PieOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.PIE;
    }

    @Override
    public ObjectNode build(WidgetSpec widget, Table data, ObjectMapper mapper) {
        AggTable at = AggTable.of(widget, data);
        ObjectNode root = mapper.createObjectNode();
        ArrayNode series = root.putArray("series");
        ObjectNode s = series.addObject();
        s.put("type", "pie");
        List<Object> names = at.dimValues();
        List<Object> values = at.aggValues();
        ArrayNode pieData = s.putArray("data");
        for (int i = 0; i < names.size(); i++) {
            ObjectNode item = pieData.addObject();
            item.put("name", String.valueOf(names.get(i)));
            item.set("value", mapper.valueToTree(values.get(i)));
        }
        return root;
    }
}
