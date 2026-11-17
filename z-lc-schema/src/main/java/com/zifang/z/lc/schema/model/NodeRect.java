package com.zifang.z.lc.schema.model;

/**
 * 节点在绝对定位布局里的栅格单元（x/y/w/h）。
 *
 * <p>组件树页面走流式排版，不写 rect；报表形态的页面（图表/表格按 12 栅格摆放）
 * 就是「带 rect 的节点」。两种排布共用同一份 schema，不再分两套模型。</p>
 */
public class NodeRect {

    private Integer x = 0;
    private Integer y = 0;
    private Integer w = 12;
    private Integer h = 4;

    public NodeRect() {
    }

    public NodeRect(Integer x, Integer y, Integer w, Integer h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getW() {
        return w;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public Integer getH() {
        return h;
    }

    public void setH(Integer h) {
        this.h = h;
    }
}
