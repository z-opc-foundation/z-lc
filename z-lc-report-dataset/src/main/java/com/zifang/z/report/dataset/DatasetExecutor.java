package com.zifang.z.report.dataset;

import com.zifang.z.report.common.schema.ComputedField;
import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.JoinDef;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.dataset.engine.Expr;
import com.zifang.z.report.dataset.engine.ExprParser;
import com.zifang.z.report.dataset.engine.Filters;
import com.zifang.z.report.dataset.engine.Table;

import java.util.List;

/**
 * 数据集执行器: DatasetSchema (契约) → 内存 Table → QueryResult。
 * <p>
 * 执行序: 主源读取 → 依序多源 hash join → (计算字段 M2: 接 z-util-expr 字符串解析,
 * 解析结果必须经白名单校验, 禁止动态执行任意代码) → 过滤。
 * <p>
 * 源解析经 TableSourceResolver 注入 (依赖倒置, dataset 不感知 datasource 模块)。
 * M1 为全量内存执行; 缓存与下推优化见方案文档 5.2。
 */
public class DatasetExecutor {

    private final TableSourceResolver sources;

    public DatasetExecutor(TableSourceResolver sources) {
        this.sources = sources;
    }

    public QueryResult execute(DatasetSchema schema) {
        return executeTable(schema).toQueryResult();
    }

    public Table executeTable(DatasetSchema schema) {
        Table t = sources.resolve(schema.getSourceId(), schema.getBaseTable());
        if (t == null) {
            throw new IllegalArgumentException("unknown source or table: " + schema.getSourceId() + "." + schema.getBaseTable());
        }

        List<JoinDef> joins = schema.getJoins();
        if (joins != null) {
            for (JoinDef j : joins) {
                if (j.getTable() == null) {
                    throw new IllegalArgumentException("join.table required (source=" + j.getSourceId() + ")");
                }
                Table right = sources.resolve(j.getSourceId(), j.getTable());
                if (right == null) {
                    throw new IllegalArgumentException("unknown join source or table: " + j.getSourceId() + "." + j.getTable());
                }
                t = t.join(right, j.getType(), j.getLeftKeys(), j.getRightKeys());
            }
        }

        List<ComputedField> computed = schema.getComputedFields();
        if (computed != null && !computed.isEmpty()) {
            // 白名单受限求值: ExprParser 只生成结构化 Expr 对象树 (列/字面量/运算符),
            // 无函数调用面, 求值走 Expr.eval; 复杂表达式 (函数/日期) 由 z-util-expr 接入 (M2+)
            for (ComputedField cf : computed) {
                if (cf.getName() == null || cf.getExpr() == null) {
                    throw new IllegalArgumentException("computedField requires name and expr");
                }
                t = t.assign(cf.getName(),
                        cf.getType() == null ? com.zifang.z.report.common.schema.FieldType.DOUBLE : cf.getType(),
                        ExprParser.parse(cf.getExpr()));
            }
        }

        Expr filter = Filters.toExpr(schema.getFilters());
        if (filter != null) {
            t = t.filter(filter);
        }
        return t;
    }
}
