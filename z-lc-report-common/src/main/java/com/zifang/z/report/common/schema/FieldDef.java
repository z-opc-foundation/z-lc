package com.zifang.z.report.common.schema;

/**
 * 字段定义: 名称 + 展示名 + 类型 + 语义角色。
 * 用于 DataSource 元数据探测结果、Dataset 字段标注、QueryResult 列头。
 */
public class FieldDef {

    private String name;
    private String label;
    private FieldType type;
    private FieldRole role;

    public FieldDef() {
    }

    public FieldDef(String name, String label, FieldType type, FieldRole role) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.role = role;
    }

    /** 仅名称的便捷工厂 (类型/角色未知时) */
    public static FieldDef of(String name, FieldType type) {
        return new FieldDef(name, name, type, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public FieldRole getRole() {
        return role;
    }

    public void setRole(FieldRole role) {
        this.role = role;
    }
}
