package com.zifang.z.report.common.schema;

import java.util.List;

/**
 * 视图页 schema (通用渲染器的单一事实源):
 * 三个生产者 (AI 一句话生成 / 拖拽设计器 / 模板库), 一个消费者 (UniversalRenderer)。
 */
public class ViewSchema {

    private String id;
    private String title;
    private int version = 1;
    private List<WidgetSpec> widgets;
    private List<WidgetLayout> layout;

    public ViewSchema() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<WidgetSpec> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<WidgetSpec> widgets) {
        this.widgets = widgets;
    }

    public List<WidgetLayout> getLayout() {
        return layout;
    }

    public void setLayout(List<WidgetLayout> layout) {
        this.layout = layout;
    }
}
