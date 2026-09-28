package com.zifang.z.lc.common.utils;

import java.time.LocalDate;

/**
 * 时间工具 — 仅保留 z-util 未覆盖的部分.
 *
 * <p>本类原有的 Date / LocalDateTime / LocalDate / epoch 互转与格式化方法
 * 已与 {@code com.zifang.util.core.time.DateUtil} /
 * {@link com.zifang.util.core.time.LocalDateTimeUtil} /
 * {@link com.zifang.util.core.time.LocalDateUtil} 逐输入实测等价
 * （含 null 入参），已于 2026-09 收口时删除, 调用点直指 z-util：
 * <table border="1">
 *   <caption>被删除方法与 z-util 对应实现</caption>
 *   <tr><th>原方法</th><th>z-util 对应</th></tr>
 *   <tr><td>fromDate(LocalDateTime)</td><td>DateUtil.fromLocalDateTime</td></tr>
 *   <tr><td>fromDate(Date)</td><td>DateUtil.toLocalDateTime</td></tr>
 *   <tr><td>formatFromDate(Date)</td><td>DateUtil.format</td></tr>
 *   <tr><td>formatFromLocalDateTime</td><td>LocalDateTimeUtil.format</td></tr>
 *   <tr><td>formatDateFromLocalDate</td><td>LocalDateUtil.format</td></tr>
 *   <tr><td>formatFromEpochMillis</td><td>DateUtil.format(DateUtil.fromEpochMilli(..))</td></tr>
 *   <tr><td>formatDateFromEpochMillis</td><td>LocalDateUtil.format(..toLocalDate())</td></tr>
 * </table>
 *
 * <p>下面两个方法<b>不能</b>换成 z-util：{@code DateUtil.getTodayStartStr()} 与
 * {@code getTodayEndStr()} 实测都返回"当前时刻"（两者实现相同, 都是
 * {@code format(new Date(), PATTERN_DEFAULT)}）, 并不是当天的 00:00:00 / 23:59:59.
 * 在 z-util 修正这两个方法之前, 本类是全站唯一正确的"当日起止字符串"来源.
 * 该差异由 {@code ZLcUtilDedupEquivalenceTest} 锁定.
 *
 * @author zifang
 */
public final class ZLcTimeUtil {

    private ZLcTimeUtil() {
    }

    /**
     * 今天 00:00:00 字符串 (yyyy-MM-dd HH:mm:ss).
     */
    public static String todayStart() {
        return LocalDate.now().toString() + " 00:00:00";
    }

    /**
     * 今天 23:59:59 字符串 (yyyy-MM-dd HH:mm:ss).
     */
    public static String todayEnd() {
        return LocalDate.now().toString() + " 23:59:59";
    }
}
