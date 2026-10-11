package com.zifang.z.report.common.schema;

/**
 * 计算字段: 在内存表上追加派生列。
 * <p>
 * M1: expr 为表达式字符串, 暂不在执行链解析 (Table.assign(Expr) 已支持对象式计算列);
 * 字符串解析器后续接 z-util-expr (EL/JS/SQL 多引擎聚合), 本字段即为其入参契约。
 */
public class ComputedField {

    private String name;
    private FieldType type;
    private String expr;

    public ComputedField() {
    }

    public ComputedField(String name, FieldType type, String expr) {
        this.name = name;
        this.type = type;
        this.expr = expr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }
}
