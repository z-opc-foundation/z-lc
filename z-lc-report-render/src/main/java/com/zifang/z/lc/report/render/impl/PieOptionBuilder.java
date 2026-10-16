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
 * 饼图: groupBy(category).agg(value) → data[{name, value}]。
 */
public class PieOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.PIE;
    }

    @Override
    public Map<String, Object> build(WidgetSpec widget, Table data) {
        AggTable at = AggTable.of(widget, data);
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> names = at.dimValues();
        List<Object> values = at.aggValues();
        List<Object> pieData = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(names.get(i)));
            item.put("value", values.get(i));
            pieData.add(item);
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "pie");
        s.put("data", pieData);
        List<Object> series = new ArrayList<>();
        series.add(s);
        root.put("series", series);
        return root;
    }
}
