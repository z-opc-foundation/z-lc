package com.zifang.z.report.common.schema;

import java.util.List;

/**
 * 图表字段编码 (视觉通道 → 数据字段绑定):
 * LINE/BAR: x=维度, y=度量; PIE: category=维度, value=度量;
 * TABLE: columns 为展示列; KPI: value=度量。
 */
public class WidgetEncode {

    private String x;
    private String y;
    private String series;
    private String category;
    private String value;
    private List<String> columns;

    public WidgetEncode() {
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
