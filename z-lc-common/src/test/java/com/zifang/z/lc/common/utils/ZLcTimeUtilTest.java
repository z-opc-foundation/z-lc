package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTimeUtil 单元测试
 *
 * @author zifang
 */
class ZLcTimeUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcTimeUtil> constructor = ZLcTimeUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReturnNullForNullLocalDateTimeToDate() {
        assertThat(ZLcTimeUtil.fromDate((LocalDateTime) null)).isNull();
    }

    @Test
    void shouldConvertLocalDateTimeToDate() {
        LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Date date = ZLcTimeUtil.fromDate(ldt);
        assertThat(date).isNotNull();
    }

    @Test
    void shouldReturnNullForNullDateToLocalDateTime() {
        assertThat(ZLcTimeUtil.fromDate((Date) null)).isNull();
    }

    @Test
    void shouldConvertDateToLocalDateTime() {
        Date date = new Date();
        LocalDateTime ldt = ZLcTimeUtil.fromDate(date);
        assertThat(ldt).isNotNull();
    }

    @Test
    void shouldReturnNullForNullFormatFromDate() {
        assertThat(ZLcTimeUtil.formatFromDate(null)).isNull();
    }

    @Test
    void shouldFormatDate() {
        Date date = new Date(0);
        String result = ZLcTimeUtil.formatFromDate(date);
        assertThat(result).isNotNull();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void shouldReturnNullForNullFormatFromLocalDateTime() {
        assertThat(ZLcTimeUtil.formatFromLocalDateTime(null)).isNull();
    }

    @Test
    void shouldFormatLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        String result = ZLcTimeUtil.formatFromLocalDateTime(ldt);
        assertThat(result).isEqualTo("2024-01-15 10:30:00");
    }

    @Test
    void shouldReturnNullForNullFormatDateFromLocalDate() {
        assertThat(ZLcTimeUtil.formatDateFromLocalDate(null)).isNull();
    }

    @Test
    void shouldFormatLocalDate() {
        LocalDate ld = LocalDate.of(2024, 1, 15);
        String result = ZLcTimeUtil.formatDateFromLocalDate(ld);
        assertThat(result).isEqualTo("2024-01-15");
    }

    @Test
    void shouldFormatFromEpochMillis() {
        long millis = 0L;
        String result = ZLcTimeUtil.formatFromEpochMillis(millis);
        assertThat(result).isNotNull();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void shouldFormatDateFromEpochMillis() {
        long millis = 0L;
        String result = ZLcTimeUtil.formatDateFromEpochMillis(millis);
        assertThat(result).isNotNull();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void shouldReturnTodayStart() {
        String result = ZLcTimeUtil.todayStart();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} 00:00:00");
    }

    @Test
    void shouldReturnTodayEnd() {
        String result = ZLcTimeUtil.todayEnd();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} 23:59:59");
    }
}