package com.zifang.z.report.common.schema;

/**
 * 视图页内组件的网格布局 (12 栅格, 类 BI 工具)。
 * AI 生成布局与拖拽设计器都只操作这组整数坐标。
 */
public class WidgetLayout {

    private String widgetId;
    /** 栅格列起点 0-11 */
    private int x;
    /** 栅格行起点 0.. */
    private int y;
    /** 占宽 (栅格数, 1-12) */
    private int w;
    /** 占高 (行高单位) */
    private int h;

    public WidgetLayout() {
    }

    public WidgetLayout(String widgetId, int x, int y, int w, int h) {
        this.widgetId = widgetId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }
}
