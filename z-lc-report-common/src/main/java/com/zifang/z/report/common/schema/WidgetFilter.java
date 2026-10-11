package com.zifang.z.report.common.schema;

import java.util.List;

/**
 * 单字段过滤条件: field op value。
 * IN 使用 values; 其余使用 value。
 */
public class WidgetFilter {

    private String field;
    private FilterOp op;
    private Object value;
    private List<Object> values;

    public WidgetFilter() {
    }

    public WidgetFilter(String field, FilterOp op, Object value) {
        this.field = field;
        this.op = op;
        this.value = value;
    }

    public static WidgetFilter in(String field, List<Object> values) {
        WidgetFilter f = new WidgetFilter();
        f.field = field;
        f.op = FilterOp.IN;
        f.values = values;
        return f;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public FilterOp getOp() {
        return op;
    }

    public void setOp(FilterOp op) {
        this.op = op;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }
}
