package com.zifang.z.report.render.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zifang.util.expr.obj.ObjEngine;
import com.zifang.z.report.common.schema.ChartType;
import com.zifang.z.report.common.schema.WidgetSpec;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.render.api.ChartOptionBuilder;

/**
 * 任意结构 (RAW): 数据集的二维明细交给对象整形语言, 产出什么形态就渲染什么形态。
 * <p>
 * 这是"内存读取数据 + 产出任意格式"这条链的出口: SQL 只能给二维表, 嵌套对象 / 键值映射 /
 * 树 / 矩阵由 widget.shape 描述, 渲染层按 {@code {type:'raw', value: <任意结构>}} 原样透传,
 * 不再假设它是 echarts option。
 */
public class RawOptionBuilder implements ChartOptionBuilder {

    @Override
    public ChartType type() {
        return ChartType.RAW;
    }

    @Override
    public ObjectNode build(WidgetSpec widget, Table data, ObjectMapper mapper) {
        if (widget.getShape() == null) {
            throw new IllegalArgumentException("RAW widget requires shape (对象整形程序): " + widget.getId());
        }
        Object doc = new ObjEngine().shape(widget.getShape(), data.toRows());
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "raw");
        root.set("value", mapper.valueToTree(doc));
        return root;
    }
}
