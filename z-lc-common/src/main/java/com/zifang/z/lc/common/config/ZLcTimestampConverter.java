package com.zifang.z.lc.common.config;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Timestamp 类型转换器 — 蒸馏自 ace-platform-core
 * {@code TimestampConverter} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>将 {@link Timestamp} 转换为 yyyy-MM-dd HH:mm:ss 格式的字符串.
 * 蒸馏时移除了 ace 对 EasyExcel Converter 接口的依赖,
 * 改为纯 JDK 实现. 业务层可自行适配 EasyExcel/POI 转换器.
 *
 * @author zifang
 */
public final class ZLcTimestampConverter {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ZLcTimestampConverter() {
    }

    /**
     * 将 Timestamp 转换为标准日期字符串.
     *
     * @param timestamp 时间戳
     * @return 格式化后的字符串; null 时返回空字符串
     */
    public static String format(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        synchronized (SDF) {
            return SDF.format(timestamp);
        }
    }

    /**
     * 将 Timestamp 转换为标准日期字符串.
     *
     * @param timestamp 时间戳
     * @return 格式化后的字符串; null 时返回 null
     */
    public static String formatOrNull(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        synchronized (SDF) {
            return SDF.format(timestamp);
        }
    }
}
