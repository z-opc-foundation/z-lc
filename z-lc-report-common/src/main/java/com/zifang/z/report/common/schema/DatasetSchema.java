package com.zifang.z.report.common.schema;

import java.util.List;

/**
 * 数据集定义 (语义层, 对标 Superset Dataset):
 * 主源 + 可选多源内存 join + 计算字段 + 过滤 + 字段语义标注。
 * <p>
 * 执行: DatasetExecutor 按 sourceId 读主源为内存 Table,
 * 依序应用 joins (hash join) → filters, computedFields 由表达式引擎负责 (M2)。
 */
public class DatasetSchema {

    private String id;
    private String name;
    private String sourceId;
    /** 主源内的基础数据单元 (表名 / 文件名 / 源内标识) */
    private String baseTable;
    private List<JoinDef> joins;
    private List<WidgetFilter> filters;
    private List<ComputedField> computedFields;
    /** 字段语义标注 (维度/度量), 供渲染与 AI 生成使用 */
    private List<FieldDef> fields;

    public DatasetSchema() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getBaseTable() {
        return baseTable;
    }

    public void setBaseTable(String baseTable) {
        this.baseTable = baseTable;
    }

    public List<JoinDef> getJoins() {
        return joins;
    }

    public void setJoins(List<JoinDef> joins) {
        this.joins = joins;
    }

    public List<WidgetFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<WidgetFilter> filters) {
        this.filters = filters;
    }

    public List<ComputedField> getComputedFields() {
        return computedFields;
    }

    public void setComputedFields(List<ComputedField> computedFields) {
        this.computedFields = computedFields;
    }

    public List<FieldDef> getFields() {
        return fields;
    }

    public void setFields(List<FieldDef> fields) {
        this.fields = fields;
    }
}
