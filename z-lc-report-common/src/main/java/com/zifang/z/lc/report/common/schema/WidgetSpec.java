package com.zifang.z.lc.report.common.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 图表组件规格: 绑定数据集 + 视觉编码 + 过滤。
 * 渲染链路: WidgetSpec + DatasetSchema → 引擎取数/聚合 → echarts option。
 * <p>
 * ignoreUnknown: 页面域 (V2/V3) 的部件 schema 是本类的超集 (如 events.onClick 绑 vApi),
 * 渲染链只消费本类认识的字段——扩展字段不卡渲染。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WidgetSpec {

    private String id;
    private ChartType type;
    private String title;
    private String datasetId;
    private WidgetEncode encode;
    /** 度量聚合方式, 默认 SUM */
    private AggType agg;
    private List<WidgetFilter> filters;
    /**
     * 对象整形程序 (z-util-expr-obj 的 JSON 形状 spec): 把数据集的二维结果抬成任意高维结构。
     * <p>
     * 挂在部件上而不是数据集上: 数据集是二维的、可被多个部件共享与缓存, 而饼图要的扁平结构与
     * 树图要的嵌套结构是各部件自己的事。type=RAW 时产出即 {@code {type:'raw', value: <任意结构>}}。
     */
    private Object shape;

    public WidgetSpec() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChartType getType() {
        return type;
    }

    public void setType(ChartType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public WidgetEncode getEncode() {
        return encode;
    }

    public void setEncode(WidgetEncode encode) {
        this.encode = encode;
    }

    public AggType getAgg() {
        return agg;
    }

    public void setAgg(AggType agg) {
        this.agg = agg;
    }

    public List<WidgetFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<WidgetFilter> filters) {
        this.filters = filters;
    }

    public Object getShape() {
        return shape;
    }

    public void setShape(Object shape) {
        this.shape = shape;
    }
}
