package com.zifang.z.lc.common.dto;

/**
 * 整形查询入参: 聚合查询 (二维分组结果) + 对象整形程序 (二维 → 任意高维结构).
 * <p>
 * 继承 {@link AggregateQueryDTO} 而不是包一层, 是为了让"筛选/分组/聚合"这一维口径与
 * {@code /aggregate} 完全同一个契约: 前端把当前视图的聚合参数原样发过来, 再多带一个 shape 就行.
 * <p>
 * {@code shape} 刻意声明成 Object 而不是某个结构体 —— 对象整形语言 (z-util-expr-obj) 的程序
 * 就是一段普通 JSON (步骤数组或对象), 前端/低代码配置/AI 生成的都是它, 后端不解释只交给引擎.
 */
public class ShapeQueryDTO extends AggregateQueryDTO {

    private static final long serialVersionUID = 1L;

    /** 对象整形程序, 例如 {@code [{"op":"group","by":"status","as":"rows"}]} */
    private Object shape;

    public Object getShape() {
        return shape;
    }

    public void setShape(Object shape) {
        this.shape = shape;
    }
}
