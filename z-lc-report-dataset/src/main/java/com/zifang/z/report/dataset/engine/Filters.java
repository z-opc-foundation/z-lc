package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.FilterOp;
import com.zifang.z.report.common.schema.WidgetFilter;

import java.util.List;

/**
 * WidgetFilter (契约) → Expr (引擎) 转换器。
 * 多个过滤条件以 AND 连接; LIKE 退化为 contains。
 */
public final class Filters {

    private Filters() {
    }

    public static Expr toExpr(List<WidgetFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        Expr acc = toSingle(filters.get(0));
        for (int i = 1; i < filters.size(); i++) {
            acc = Expr.and(acc, toSingle(filters.get(i)));
        }
        return acc;
    }

    static Expr toSingle(WidgetFilter f) {
        String col = f.getField();
        FilterOp op = f.getOp();
        if (op == null) {
            throw new IllegalArgumentException("filter op required: " + col);
        }
        switch (op) {
            case EQ:
                return Expr.eq(Expr.col(col), Expr.lit(f.getValue()));
            case NE:
                return Expr.ne(Expr.col(col), Expr.lit(f.getValue()));
            case GT:
                return Expr.gt(Expr.col(col), Expr.lit(f.getValue()));
            case GE:
                return Expr.ge(Expr.col(col), Expr.lit(f.getValue()));
            case LT:
                return Expr.lt(Expr.col(col), Expr.lit(f.getValue()));
            case LE:
                return Expr.le(Expr.col(col), Expr.lit(f.getValue()));
            case IN:
                return Expr.in(col, f.getValues());
            case LIKE:
                return Expr.contains(col, String.valueOf(f.getValue()));
            default:
                throw new IllegalArgumentException("unsupported filter op: " + op);
        }
    }
}
