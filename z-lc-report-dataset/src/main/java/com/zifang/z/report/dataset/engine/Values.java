package com.zifang.z.report.dataset.engine;

/**
 * 值工具: 跨类型比较 / 数值化。
 * 引擎内部共用, 规则与 Expr 的比较语义一致。
 */
final class Values {

    private Values() {
    }

    /**
     * 跨类型比较:
     * 1) 双方为 Number → 数值比较
     * 2) 同类型 Comparable → 自然序
     * 3) 其他 → 字符串形式比较 (M1 宽松兜底)
     */
    static int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a.getClass() == b.getClass() && a instanceof Comparable) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            int c = ((Comparable) a).compareTo(b);
            return c;
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /** 数值化: Number 直接取值, 数字字面量字符串可解析, 其余 null */
    static Double toDouble(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof Boolean) {
            return (Boolean) v ? 1.0 : 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 整数值 → long（不经 double，避免 53 位尾数舍入）。
     * <p>
     * 与 {@link #toDouble} 的区别就在这里：{@code ((Number) 9007199254740993L).doubleValue()}
     * 得到 9007199254740992.0，再转回 long 已经是错的数。报表里以分为单位的累计额、
     * 纳秒/微秒时间戳求和都会踩到，故单独提供不经过 double 的通道。
     */
    static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Byte || v instanceof Short || v instanceof Integer || v instanceof Long) {
            return ((Number) v).longValue();
        }
        if (v instanceof java.math.BigInteger) {
            return ((java.math.BigInteger) v).longValue();
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof Boolean) {
            return (Boolean) v ? 1L : 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            try {
                return (long) Double.parseDouble(String.valueOf(v).trim());
            } catch (NumberFormatException e2) {
                return 0L;
            }
        }
    }

    static boolean isInteger(Object v) {
        return v instanceof Byte || v instanceof Short || v instanceof Integer || v instanceof Long;
    }
}
