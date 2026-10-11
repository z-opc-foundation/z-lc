package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.common.schema.FieldDef;
import com.zifang.z.report.common.schema.QueryResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 内存 typed 列式表 (z-report 引擎核心)。
 * <p>
 * 设计要点 (方案决策①):
 * - 类型化列: STRING/INT/DOUBLE/BOOL/DATE, 值允许 null (空值语义)
 * - API 形态对齐 pandas: filter/sort/select/assign/join/groupBy
 * - join: 多键等值 hash join, INNER/LEFT; 键含 null 不参与匹配 (对齐 SQL)
 * - 重名列: 右表列自动改名 col, col_r, col_r2 ...
 * <p>
 * 性能边界: M1 按 10 万行级/数据集设计 (行级 Map 视图求值);
 * 向量化与列式直构优化见 TODO, 数据量级超出时引入物化结果表。
 */
public class Table {

    private final LinkedHashMap<String, Column> columns;

    private Table(LinkedHashMap<String, Column> columns) {
        this.columns = columns;
    }

    // ==================== 构造 ====================

    /** 按列定义 + 行数据构造 (行内值顺序与 names 一致) */
    public static Table of(List<String> names, List<FieldType> types, List<List<Object>> rows) {
        Objects.requireNonNull(names, "names");
        Objects.requireNonNull(types, "types");
        if (names.size() != types.size()) {
            throw new IllegalArgumentException("names/types size mismatch: " + names.size() + " vs " + types.size());
        }
        LinkedHashMap<String, Column> cols = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (cols.containsKey(name)) {
                throw new IllegalArgumentException("duplicate column: " + name);
            }
            cols.put(name, new Column(name, types.get(i)));
        }
        if (rows != null) {
            for (List<Object> row : rows) {
                if (row.size() != names.size()) {
                    throw new IllegalArgumentException("row width mismatch: " + row.size() + " vs " + names.size());
                }
                for (int i = 0; i < row.size(); i++) {
                    cols.get(names.get(i)).values().add(row.get(i));
                }
            }
        }
        return new Table(cols);
    }

    /** 从行视图构造 (类型推断; QueryResult/数据源适配用) */
    public static Table fromRows(List<Map<String, Object>> rows, LinkedHashMap<String, FieldType> colDefs) {
        List<String> names = new ArrayList<>(colDefs.keySet());
        List<FieldType> types = new ArrayList<>(colDefs.values());
        List<List<Object>> data = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                List<Object> r = new ArrayList<>(names.size());
                for (String n : names) {
                    r.add(row.get(n));
                }
                data.add(r);
            }
        }
        return of(names, types, data);
    }

    // ==================== 元信息 ====================

    public int nRows() {
        return columns.isEmpty() ? 0 : columns.values().iterator().next().values().size();
    }

    public int nCols() {
        return columns.size();
    }

    public List<String> columnNames() {
        return new ArrayList<>(columns.keySet());
    }

    public List<FieldType> columnTypes() {
        List<FieldType> types = new ArrayList<>(columns.size());
        for (Column c : columns.values()) {
            types.add(c.type());
        }
        return types;
    }

    public Column column(String name) {
        Column c = columns.get(name);
        if (c == null) {
            throw new IllegalArgumentException("no such column: " + name);
        }
        return c;
    }

    public Object get(int rowIdx, String col) {
        return column(col).values().get(rowIdx);
    }

    /** 整列值快照 (跨模块读取用) */
    public List<Object> colValues(String col) {
        return new ArrayList<>(column(col).values());
    }

    // ==================== 变换 (返回新 Table) ====================

    /** 投影 */
    public Table select(String... cols) {
        LinkedHashMap<String, Column> picked = new LinkedHashMap<>();
        for (String c : cols) {
            Column src = column(c);
            picked.put(src.name(), src.copy());
        }
        return new Table(picked);
    }

    /** 表达式过滤 (行级求值; TODO: 向量化短路) */
    public Table filter(Expr expr) {
        Objects.requireNonNull(expr, "expr");
        List<List<Object>> kept = new ArrayList<>();
        for (int i = 0; i < nRows(); i++) {
            if (Boolean.TRUE.equals(expr.eval(rowView(i)))) {
                kept.add(rowValues(i));
            }
        }
        return of(columnNames(), columnTypes(), kept);
    }

    /** 排序: null 恒排最后 (与升降序无关) */
    public Table sort(final String col, final boolean asc) {
        column(col); // 校验存在
        List<Integer> idx = new ArrayList<>(nRows());
        for (int i = 0; i < nRows(); i++) {
            idx.add(i);
        }
        idx.sort((a, b) -> {
            Object va = get(a, col);
            Object vb = get(b, col);
            // null 恒排最后 (与升降序无关)
            if (va == null || vb == null) {
                return va == null && vb == null ? 0 : (va == null ? 1 : -1);
            }
            int c = Values.compare(va, vb);
            return asc ? c : -c;
        });
        List<List<Object>> rows = new ArrayList<>(nRows());
        for (int i : idx) {
            rows.add(rowValues(i));
        }
        return of(columnNames(), columnTypes(), rows);
    }

    /** 追加计算列 */
    public Table assign(String name, FieldType type, Expr expr) {
        Objects.requireNonNull(expr, "expr");
        if (columns.containsKey(name)) {
            throw new IllegalArgumentException("column already exists: " + name);
        }
        LinkedHashMap<String, Column> next = copyColumns();
        Column add = new Column(name, type);
        for (int i = 0; i < nRows(); i++) {
            add.values().add(expr.eval(rowView(i)));
        }
        next.put(name, add);
        return new Table(next);
    }

    /** 截取前 n 行 */
    public Table limit(int n) {
        if (n < 0 || n >= nRows()) {
            return this;
        }
        List<List<Object>> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rows.add(rowValues(i));
        }
        return of(columnNames(), columnTypes(), rows);
    }

    /**
     * 多键等值 hash join。
     * 输出列 = 左表全列 + 右表全列 (重名自动后缀 _r)。
     * INNER: 双方匹配; LEFT: 左表保留, 右侧补 null。
     * RIGHT = swap 后的 LEFT, FULL 暂不支持 (见 JoinType 注释)。
     */
    public Table join(Table right, com.zifang.z.report.common.schema.JoinType type,
                      String[] leftKeys, String[] rightKeys) {
        Objects.requireNonNull(type, "join type");
        if (leftKeys == null || rightKeys == null || leftKeys.length != rightKeys.length) {
            throw new IllegalArgumentException("join keys mismatch");
        }
        // 右表键索引
        Map<List<Object>, List<Integer>> index = new HashMap<>();
        for (int i = 0; i < right.nRows(); i++) {
            List<Object> key = keyTuple(right, rightKeys, i);
            if (keyHasNull(key)) {
                continue; // null 键不参与匹配
            }
            List<Integer> bucket = index.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                index.put(key, bucket);
            }
            bucket.add(i);
        }
        // 输出列定义 (重名后缀)
        List<String> names = new ArrayList<>();
        List<FieldType> types = new ArrayList<>();
        names.addAll(columnNames());
        types.addAll(columnTypes());
        Map<String, String> rightRename = new LinkedHashMap<>();
        for (int i = 0; i < right.nCols(); i++) {
            String rn = right.columnNames().get(i);
            String outName = uniqueName(rn, names);
            rightRename.put(rn, outName);
            names.add(outName);
            types.add(right.columnTypes().get(i));
        }
        // 探测
        List<List<Object>> rows = new ArrayList<>();
        boolean leftJoin = type == com.zifang.z.report.common.schema.JoinType.LEFT;
        for (int i = 0; i < nRows(); i++) {
            List<Object> key = keyTuple(this, leftKeys, i);
            List<Integer> matched = keyHasNull(key) ? null : index.get(key);
            if (matched != null && !matched.isEmpty()) {
                for (int r : matched) {
                    rows.add(mergeRow(i, right, r));
                }
            } else if (leftJoin) {
                List<Object> row = new ArrayList<>(rowValues(i));
                for (int k = 0; k < right.nCols(); k++) {
                    row.add(null);
                }
                rows.add(row);
            }
        }
        return of(names, types, rows);
    }

    private List<Object> mergeRow(int leftIdx, Table right, int rightIdx) {
        List<Object> row = new ArrayList<>(nCols() + right.nCols());
        row.addAll(rowValues(leftIdx));
        for (String rn : right.columnNames()) {
            row.add(right.get(rightIdx, rn));
        }
        return row;
    }

    private static List<Object> keyTuple(Table t, String[] keys, int rowIdx) {
        List<Object> key = new ArrayList<>(keys.length);
        for (String k : keys) {
            key.add(t.get(rowIdx, k));
        }
        return key;
    }

    private static boolean keyHasNull(List<Object> key) {
        for (Object o : key) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    private static String uniqueName(String base, List<String> existing) {
        if (!existing.contains(base)) {
            return base;
        }
        String candidate = base + "_r";
        int seq = 2;
        while (existing.contains(candidate)) {
            candidate = base + "_r" + (seq++);
        }
        return candidate;
    }

    // ==================== 聚合 ====================
    
    /** 分组聚合入口: table.groupBy("dept").agg("total","amount",AggType.SUM).toTable() */
    public GroupBy groupBy(String... keys) {
        return new GroupBy(this, keys);
    }
    
    /**
     * 全表单列聚合 (无维度, KPI 指标卡语义)。
     * col 为 "*" 时仅支持 COUNT。
     */
    public Object aggregate(String col, com.zifang.z.report.common.schema.AggType type) {
        Objects.requireNonNull(type, "agg type");
        if (com.zifang.z.report.common.schema.AggType.COUNT == type && GroupBy.STAR.equals(col)) {
            return (long) nRows();
        }
        List<Object> vals = new ArrayList<>();
        for (Object v : column(col).values()) {
            if (v != null) {
                vals.add(v);
            }
        }
        return Aggs.aggregate(type, vals);
    }

    // ==================== 导出 ====================

    public List<Map<String, Object>> toRows() {
        List<Map<String, Object>> rows = new ArrayList<>(nRows());
        for (int i = 0; i < nRows(); i++) {
            rows.add(rowView(i));
        }
        return rows;
    }

    public QueryResult toQueryResult() {
        List<FieldDef> cols = new ArrayList<>(nCols());
        for (Column c : columns.values()) {
            cols.add(FieldDef.of(c.name(), c.type()));
        }
        return QueryResult.of(cols, toRows());
    }

    // ==================== 内部 ====================

    private LinkedHashMap<String, Column> copyColumns() {
        LinkedHashMap<String, Column> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Column> e : columns.entrySet()) {
            copy.put(e.getKey(), e.getValue().copy());
        }
        return copy;
    }

    private List<Object> rowValues(int i) {
        List<Object> row = new ArrayList<>(nCols());
        for (Column c : columns.values()) {
            row.add(c.values().get(i));
        }
        return row;
    }

    private Map<String, Object> rowView(int i) {
        Map<String, Object> view = new LinkedHashMap<>(nCols() * 2);
        for (Map.Entry<String, Column> e : columns.entrySet()) {
            view.put(e.getKey(), e.getValue().values().get(i));
        }
        return view;
    }

    LinkedHashMap<String, Column> rawColumns() {
        return columns;
    }
}
