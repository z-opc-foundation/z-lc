package com.zifang.z.report.common.schema;

/**
 * 图表类型 (M1 五件套 + RAW)。
 * TABLE/KPI 非原生 echarts 类型, 由前端通用渲染器按约定结构解释;
 * RAW 连约定结构都不要: 结构由 widget.shape 决定, 产出什么形态就渲染什么形态。
 */
public enum ChartType {
    LINE,
    BAR,
    PIE,
    TABLE,
    KPI,
    RAW
}
