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
 * 折线图: groupBy(x).agg(y) → xAxis + series[line]。
 * 多系列 (encode.series) 为 M2 事项。
 */
public class LineOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.LINE;
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
        series.put("type", "line");
        series.put("data", values);
        List<Object> seriesList = new ArrayList<>();
        seriesList.add(series);
        root.put("series", seriesList);
        return root;
    }
}
