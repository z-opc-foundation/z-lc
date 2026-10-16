package com.zifang.z.lc.report.render.api;

import com.zifang.z.lc.report.common.schema.ChartType;
import com.zifang.z.lc.report.common.schema.WidgetSpec;
import com.zifang.z.lc.report.dataset.engine.Table;

import java.util.Map;

/**
 * 图表 option 构建器 SPI: 一种 ChartType 一个实现。
 * 新增图表 = 新增实现 + 注册, 渲染主链路零改动。
 */
public interface ChartOptionBuilder {

    ChartType type();

    /**
     * @param widget 图表规格 (encode 绑定 + agg)
     * @param data   已过滤的明细数据 (聚合由实现内部完成)
     * @return echarts option (TABLE/KPI 为渲染器约定结构)
     */
    Map<String, Object> build(WidgetSpec widget, Table data);
}
