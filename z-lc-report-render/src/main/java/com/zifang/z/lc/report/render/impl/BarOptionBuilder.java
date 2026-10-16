package com.zifang.z.lc.report.render.impl;

import com.zifang.z.lc.report.common.schema.ChartType;
import com.zifang.z.lc.report.common.schema.WidgetSpec;
import com.zifang.z.lc.report.dataset.engine.Table;
import com.zifang.z.lc.report.render.api.ChartOptionBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 柱状图: groupBy(x).agg(y) → xAxis + series[bar]。结构与折线一致, 仅 type 不同。
 */
public class BarOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.BAR;
    }

    @Override
    public Map<String, Object> build(WidgetSpec widget, Table data) {
        AggTable at = AggTable.of(widget, data);
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> xAxis = new ArrayList<>();
        for (Object v : at.dimValues()) {
            xAxis.add(String.valueOf(v));
        }
        root.put("xAxis", xAxis);
        List<Object> values = new ArrayList<>();
        values.addAll(at.aggValues());
        Map<String, Object> series = new LinkedHashMap<>();
        series.put("name", at.measure());
        series.put("type", "bar");
        series.put("data", values);
        List<Object> seriesList = new ArrayList<>();
        seriesList.add(series);
        root.put("series", seriesList);
        return root;
    }
}
