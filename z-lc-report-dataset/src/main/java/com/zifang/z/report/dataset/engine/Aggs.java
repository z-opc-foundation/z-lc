package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.AggType;

import java.util.List;

/**
 * 聚合实现 (输入为剔除 null 后的值序列)。
 * SUM: 全整型 → Long, 含浮点 → Double; AVG → Double; MIN/MAX → 原类型。
 */
final class Aggs {

    private Aggs() {
    }

    static Object aggregate(AggType type, List<Object> values) {
        if (values.isEmpty()) {
            return null; // 空组聚合为 null (COUNT 不会走到这里, 由 GroupBy 直接算)
        }
        switch (type) {
            case COUNT:
                return (long) values.size();
            case SUM:
                return sum(values);
            case AVG:
                return avg(values);
            case MIN:
                return extremum(values, true);
            case MAX:
                return extremum(values, false);
            default:
                return null;
        }
    }

    private static Object sum(List<Object> values) {
        // 先判类型，再选累加器 —— 早前是"一路 double 累加、最后按 allInt 转 long"，
        // 而 double 只有 53 位尾数，超过 2^53 的整数在转 long 之前就已经被舍入掉了。
        // 实测：SUM(单笔 9007199254740993L) 返回 9007199254740992（差 1），
        // 且 BigDecimal / 以分为单位的累计额 / 纳秒时间戳累加都会踩到。
        boolean allInt = true;
        for (Object v : values) {
            if (!Values.isInteger(v)) {
                allInt = false;
                break;
            }
        }
        if (allInt) {
            // 全整数：long 累加，不经过 double，精度到 Long.MAX_VALUE
            long acc = 0L;
            for (Object v : values) {
                acc += Values.toLong(v);
            }
            return acc;
        }
        // 含浮点：double 累加（此时 53 位精度已是该类型的固有上限）
        double acc = 0;
        for (Object v : values) {
            acc += Values.toDouble(v);
        }
        return acc;
    }

    private static Object avg(List<Object> values) {
        double acc = 0;
        for (Object v : values) {
            acc += Values.toDouble(v);
        }
        return acc / values.size();
    }

    private static Object extremum(List<Object> values, boolean min) {
        Object best = values.get(0);
        for (Object v : values) {
            int c = Values.compare(v, best);
            if (min ? c < 0 : c > 0) {
                best = v;
            }
        }
        return best;
    }
}
