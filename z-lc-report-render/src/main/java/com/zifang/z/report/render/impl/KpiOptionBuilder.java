package com.zifang.z.report.render.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;

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
    public ObjectNode build(WidgetSpec widget, Table data, ObjectMapper mapper) {
        AggTable at = AggTable.of(widget, data);
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "kpi");
        Object v = at.singleValue();
        if (v == null) {
            root.putNull("value");
        } else {
            root.set("value", mapper.valueToTree(v));
        }
        root.put("label", widget.getTitle() == null ? at.measure() : widget.getTitle());
        return root;
    }
}
