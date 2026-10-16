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
 * 指标卡 (非原生 echarts, 渲染器约定结构):
 * {type:'kpi', value, label} → 前端大数字组件解释。
 */
public class KpiOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.KPI;
    }

    @Override
    public Map<String, Object> build(WidgetSpec widget, Table data) {
        AggTable at = AggTable.of(widget, data);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "kpi");
        root.put("value", at.singleValue());
        root.put("label", widget.getTitle() == null ? at.measure() : widget.getTitle());
        return root;
    }
}
