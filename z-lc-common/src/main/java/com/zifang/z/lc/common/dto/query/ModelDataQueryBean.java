package com.zifang.z.lc.common.dto.query;

import java.io.Serializable;

/**
 * 模型数据查询 bean — 单字段条件.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModelDataQueryBean}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>相比 {@link ModelDataQueryCondition.FieldCondition} 的递归树结构，
 * 本 bean 是更简单的「单字段 + 单规则 + 单值」表示 — 用于 {@link ModelDataQueryDTO}
 * 的扁平 queryBeans 列表场景（前端「普通筛选」UI 直接产出）.
 *
 * <p>两个字段足够：
 * <ul>
 *   <li>{@link #fieldCode} — 字段编码（与 {@code FieldDefDTO.fieldCode} 对齐）</li>
 *   <li>{@link #mark} — 比较规则（{@code =} / {@code !=} / {@code like} / {@code in} /
 *       {@code >} / {@code <} / {@code >=} / {@code <=}）</li>
 *   <li>{@link #value} — 字段值（基本类型 / String / 集合 — {@code in} 时用 List）</li>
 * </ul>
 *
 * @author zifang
 */
public class ModelDataQueryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段 code.
     */
    private String fieldCode;

    /**
     * 操作条件（{@code =} / {@code !=} / {@code like} / {@code in} 等）.
     */
    private String mark;

    /**
     * 字段值.
     */
    private Object value;

    public ModelDataQueryBean() {
    }

    public ModelDataQueryBean(String fieldCode, String mark, Object value) {
        this.fieldCode = fieldCode;
        this.mark = mark;
        this.value = value;
    }

    /**
     * 工厂方法 — 快速构造等值条件.
     */
    public static ModelDataQueryBean eq(String fieldCode, Object value) {
        return new ModelDataQueryBean(fieldCode, "=", value);
    }

    /**
     * 工厂方法 — 快速构造 LIKE 条件.
     */
    public static ModelDataQueryBean like(String fieldCode, Object value) {
        return new ModelDataQueryBean(fieldCode, "like", value);
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
