package com.zifang.z.lc.common.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间转换工具 — 蒸馏自 ace-platform-core
 * {@code TimeUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 Date / LocalDateTime / String / epoch 之间的时间格式转换.
 * 蒸馏时移除了 ace 对 fastjson 的依赖, 改为纯 JDK 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>GenericHttpServiceCallListener 中流程启动时间格式化</li>
 *   <li>GenericMsgNotifyListener 中消息发送时间格式化</li>
 *   <li>历史流程记录中时间字段标准化</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcTimeUtil {

    private ZLcTimeUtil() {
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * LocalDateTime → Date.
     */
    public static Date fromDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * Date → LocalDateTime.
     */
    public static LocalDateTime fromDate(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Date → "yyyy-MM-dd HH:mm:ss" 字符串.
     */
    public static String formatFromDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * LocalDateTime → "yyyy-MM-dd HH:mm:ss" 字符串.
     */
    public static String formatFromLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.format(DATETIME_FMT);
    }

    /**
     * LocalDate → "yyyy-MM-dd" 字符串.
     */
    public static String formatDateFromLocalDate(LocalDate ld) {
        if (ld == null) {
            return null;
        }
        return ld.format(DATE_FMT);
    }

    /**
     * epoch 毫秒 → "yyyy-MM-dd HH:mm:ss" 字符串.
     */
    public static String formatFromEpochMillis(long millis) {
        LocalDateTime ldt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return formatFromLocalDateTime(ldt);
    }

    /**
     * epoch 毫秒 → "yyyy-MM-dd" 字符串 (仅日期).
     */
    public static String formatDateFromEpochMillis(long millis) {
        LocalDateTime ldt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return formatDateFromLocalDate(ldt.toLocalDate());
    }

    /**
     * 今天 00:00:00 字符串.
     */
    public static String todayStart() {
        return LocalDate.now().toString() + " 00:00:00";
    }

    /**
     * 今天 23:59:59 字符串.
     */
    public static String todayEnd() {
        return LocalDate.now().toString() + " 23:59:59";
    }
}
