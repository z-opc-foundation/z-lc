package com.zifang.z.report.dataset.engine;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 内存表达式 (M1 内置最小实现): 列引用/字面量/四则运算/比较/逻辑。
 * <p>
 * 定位: 仅覆盖过滤与计算列的常用语义; 复杂表达式 (函数调用/嵌套字段等)
 * 后续通过实现本接口桥接 z-util-expr (EL/JS/SQL/Groovy/Lua 聚合引擎),
 * 本接口即切换缝 —— 上层 (filter/assign) 只依赖 Expr, 不感知实现来源。
 * <p>
 * 空值语义 (对齐 SQL 三值逻辑的简化):
 * - 算术: 任一操作数为 null 结果为 null
 * - 比较: 任一侧为 null 结果为 false
 * - 逻辑: 按布尔短路, null 视为 false
 */
public interface Expr {

    /** 对一行数据求值 (行视图: 列名 → 值) */
    Object eval(Map<String, Object> row);

    // ---------- 工厂 ----------

    static Expr col(String name) {
        return new Col(name);
    }

    static Expr lit(Object value) {
        return new Lit(value);
    }

    static Expr add(Expr l, Expr r) {
        return new Arith(l, r, Arith.Op.ADD);
    }

    static Expr sub(Expr l, Expr r) {
        return new Arith(l, r, Arith.Op.SUB);
    }

    static Expr mul(Expr l, Expr r) {
        return new Arith(l, r, Arith.Op.MUL);
    }

    static Expr div(Expr l, Expr r) {
        return new Arith(l, r, Arith.Op.DIV);
    }

    static Expr eq(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.EQ);
    }

    static Expr ne(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.NE);
    }

    static Expr gt(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.GT);
    }

    static Expr ge(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.GE);
    }

    static Expr lt(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.LT);
    }

    static Expr le(Expr l, Expr r) {
        return new Cmp(l, r, Cmp.Op.LE);
    }

    static Expr and(Expr l, Expr r) {
        return new Logic(l, r, true);
    }

    static Expr or(Expr l, Expr r) {
        return new Logic(l, r, false);
    }

    static Expr not(Expr e) {
        return new Not(e);
    }

    static Expr isNull(String col) {
        return new IsNull(col, true);
    }

    static Expr isNotNull(String col) {
        return new IsNull(col, false);
    }

    /** 字符串包含语义 (M1 的 LIKE 退化) */
    static Expr contains(String col, String part) {
        return new Contains(col, part);
    }

    static Expr in(String col, Collection<?> values) {
        return new In(col, values);
    }

    // ---------- 实现 ----------

    final class Col implements Expr {
        private final String name;

        Col(String name) {
            this.name = Objects.requireNonNull(name, "col name");
        }

        /** 列名 (解析器识别列操作数用) */
        String name() {
            return name;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            return row.get(name);
        }
    }

    final class Lit implements Expr {
        private final Object value;

        Lit(Object value) {
            this.value = value;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            return value;
        }
    }

    final class Arith implements Expr {
        enum Op {ADD, SUB, MUL, DIV}

        private final Expr left;
        private final Expr right;
        private final Op op;

        Arith(Expr left, Expr right, Op op) {
            this.left = left;
            this.right = right;
            this.op = op;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            Object a = left.eval(row);
            Object b = right.eval(row);
            if (!(a instanceof Number) || !(b instanceof Number)) {
                return null;
            }
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            switch (op) {
                case ADD:
                    return x + y;
                case SUB:
                    return x - y;
                case MUL:
                    return x * y;
                case DIV:
                    return y == 0 ? null : x / y;
                default:
                    return null;
            }
        }
    }

    final class Cmp implements Expr {
        enum Op {EQ, NE, GT, GE, LT, LE}

        private final Expr left;
        private final Expr right;
        private final Op op;

        Cmp(Expr left, Expr right, Op op) {
            this.left = left;
            this.right = right;
            this.op = op;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            Object a = left.eval(row);
            Object b = right.eval(row);
            if (a == null || b == null) {
                return false; // SQL: null 比较结果为未知, 简化为 false
            }
            int c = Values.compare(a, b);
            switch (op) {
                case EQ:
                    return c == 0;
                case NE:
                    return c != 0;
                case GT:
                    return c > 0;
                case GE:
                    return c >= 0;
                case LT:
                    return c < 0;
                case LE:
                    return c <= 0;
                default:
                    return false;
            }
        }
    }

    final class Logic implements Expr {
        private final Expr left;
        private final Expr right;
        private final boolean isAnd;

        Logic(Expr left, Expr right, boolean isAnd) {
            this.left = left;
            this.right = right;
            this.isAnd = isAnd;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            boolean l = Boolean.TRUE.equals(left.eval(row));
            boolean r = Boolean.TRUE.equals(right.eval(row));
            return isAnd ? (l && r) : (l || r);
        }
    }

    final class Not implements Expr {
        private final Expr inner;

        Not(Expr inner) {
            this.inner = inner;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            return !Boolean.TRUE.equals(inner.eval(row));
        }
    }

    final class IsNull implements Expr {
        private final String col;
        private final boolean expectNull;

        IsNull(String col, boolean expectNull) {
            this.col = col;
            this.expectNull = expectNull;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            return expectNull == (row.get(col) == null);
        }
    }

    final class Contains implements Expr {
        private final String col;
        private final String part;

        Contains(String col, String part) {
            this.col = col;
            this.part = part;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            Object v = row.get(col);
            return v != null && String.valueOf(v).contains(part);
        }
    }

    final class In implements Expr {
        private final String col;
        private final Collection<?> values;

        In(String col, Collection<?> values) {
            this.col = col;
            this.values = values == null ? null : values;
        }

        @Override
        public Object eval(Map<String, Object> row) {
            if (values == null) {
                return false;
            }
            Object v = row.get(col);
            if (v == null) {
                return false;
            }
            for (Object candidate : values) {
                if (candidate != null && Values.compare(v, candidate) == 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
