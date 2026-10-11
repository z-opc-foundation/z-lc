package com.zifang.z.report.dataset.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Aggs#sum} 的整数精度。
 * <p>
 * 病灶：早前实现是「一路 double 累加、最后按 allInt 转 long」。double 只有 53 位尾数，
 * 超过 2^53 的整数<b>在转 long 之前就已经被舍入掉</b>。
 * 实测：{@code SUM(单笔 9007199254740993L)} 返回 9007199254740992（差 1）。
 * 报表里以分为单位的累计额、纳秒/微秒时间戳求和都会踩到。
 */
class AggsSumPrecisionTest {

    private static Object sumOf(Object... vs) {
        return Aggs.aggregate(com.zifang.z.report.common.schema.AggType.SUM,
                new ArrayList<>(Arrays.asList(vs)));
    }

    @Test
    void singleValueBeyond2Pow53IsExact() {
        long v = 9_007_199_254_740_993L;   // 2^53 + 1
        assertEquals(v, sumOf(v),
                "单值就超 2^53 的整数必须精确 —— 早前实现经 double 中转丢了 1");
    }

    @Test
    void commonBusinessMagnitudesAreExact() {
        // 以分为单位的累计额：一笔 1 亿元 = 100_000_000 分
        assertEquals(10_000_000_000L, sumOf(1_000_000_000L, 9_000_000_000L));
        // 单笔 1 万亿分
        assertEquals(1_000_000_000_000_000L, sumOf(1_000_000_000_000_000L));
    }

    @Test
    void manySmallValuesAccumulateExactly() {
        List<Object> v = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            v.add(3_000_000_000L);
        }
        assertEquals(3_000_000_000_000L, sumOf(v.toArray()));
    }

    @Test
    void longMaxValueDoesNotOverflow() {
        assertEquals(Long.MAX_VALUE, sumOf(Long.MAX_VALUE));
    }

    @Test
    void mixedTypesStillProduceDouble() {
        // 含浮点时应继续返回 Double（契约：SUM 全整型→Long, 含浮点→Double）
        Object r = sumOf(1L, 2L, 3.5d);
        assertTrue(r instanceof Double, "含浮点应返回 Double，实际 " + r.getClass().getSimpleName());
        assertEquals(6.5d, (Double) r, 1e-9);
    }

    @Test
    void allIntegerStaysLong() {
        Object r = sumOf(1L, 2L, 3L);
        assertTrue(r instanceof Long, "全整型应返回 Long，实际 " + r.getClass().getSimpleName());
        assertEquals(6L, r);
    }

    @Test
    void integerTypesOtherThanLongAreSummed() {
        assertEquals(6L, sumOf((byte) 1, (short) 2, 3));
    }

    @Test
    void emptyGroupAggregatesToNull() {
        assertEquals(null, Aggs.aggregate(
                com.zifang.z.report.common.schema.AggType.SUM, Collections.emptyList()));
    }

    @Test
    void valuesToLongDoesNotGoThroughDouble() {
        long v = 9_007_199_254_740_993L;
        assertEquals(v, Values.toLong(v), "toLong 必须不经 double");
        assertEquals(7L, Values.toLong("7"));
        assertEquals(0L, Values.toLong(null));
        assertEquals(1L, Values.toLong(Boolean.TRUE));
        assertEquals(0L, Values.toLong(Boolean.FALSE));
    }
}
