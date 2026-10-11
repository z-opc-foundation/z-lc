package com.zifang.z.report.common.schema;

import java.util.List;
import java.util.Map;

/**
 * 引擎查询结果 (渲染/预览/接口的通用数据载体):
 * 列头元数据 + 行数据 (列名 → 值)。
 */
public class QueryResult {

    private List<FieldDef> columns;
    private List<Map<String, Object>> rows;
    private long total;

    public QueryResult() {
    }

    public QueryResult(List<FieldDef> columns, List<Map<String, Object>> rows) {
        this.columns = columns;
        this.rows = rows;
        this.total = rows == null ? 0 : rows.size();
    }

    public static QueryResult of(List<FieldDef> columns, List<Map<String, Object>> rows) {
        return new QueryResult(columns, rows);
    }

    public List<FieldDef> getColumns() {
        return columns;
    }

    public void setColumns(List<FieldDef> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
