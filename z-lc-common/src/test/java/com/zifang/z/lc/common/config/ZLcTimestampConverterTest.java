package com.zifang.z.lc.common.config;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTimestampConverter 单元测试
 *
 * @author zifang
 */
class ZLcTimestampConverterTest {

    @Test
    void format_shouldReturnEmptyString_WhenNull() {
        String result = ZLcTimestampConverter.format(null);
        assertThat(result).isEmpty();
    }

    @Test
    void format_shouldReturnFormattedString_WhenTimestamp() {
        Timestamp ts = Timestamp.valueOf("2024-01-15 10:30:45");
        String result = ZLcTimestampConverter.format(ts);
        assertThat(result).isEqualTo("2024-01-15 10:30:45");
    }

    @Test
    void formatOrNull_shouldReturnNull_WhenNull() {
        String result = ZLcTimestampConverter.formatOrNull(null);
        assertThat(result).isNull();
    }

    @Test
    void formatOrNull_shouldReturnFormattedString_WhenTimestamp() {
        Timestamp ts = Timestamp.valueOf("2024-06-20 14:25:30");
        String result = ZLcTimestampConverter.formatOrNull(ts);
        assertThat(result).isEqualTo("2024-06-20 14:25:30");
    }

    @Test
    void formatOrNull_shouldReturnNull_WhenTimestampIsNull() {
        Timestamp ts = null;
        String result = ZLcTimestampConverter.formatOrNull(ts);
        assertThat(result).isNull();
    }

    @Test
    void format_shouldHandleMidnight() {
        Timestamp ts = Timestamp.valueOf("2024-12-31 00:00:00");
        String result = ZLcTimestampConverter.format(ts);
        assertThat(result).isEqualTo("2024-12-31 00:00:00");
    }

    @Test
    void format_shouldHandleEndOfDay() {
        Timestamp ts = Timestamp.valueOf("2024-12-31 23:59:59");
        String result = ZLcTimestampConverter.format(ts);
        assertThat(result).isEqualTo("2024-12-31 23:59:59");
    }

    @Test
    void format_shouldHandleLeapYear() {
        Timestamp ts = Timestamp.valueOf("2024-02-29 12:00:00");
        String result = ZLcTimestampConverter.format(ts);
        assertThat(result).isEqualTo("2024-02-29 12:00:00");
    }
}
