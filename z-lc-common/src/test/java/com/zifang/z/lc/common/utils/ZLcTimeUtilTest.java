package com.zifang.z.lc.common.utils;

import com.zifang.util.core.time.DateUtil;
import com.zifang.util.core.time.LocalDateUtil;
import com.zifang.util.core.time.LocalDateTimeUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcTimeUtil 单元测试.
 *
 * <p>2026-09 收口：原 ZLcTimeUtil 的 7 个日期转换/格式化方法与 z-util
 * {@code DateUtil} / {@code LocalDateTimeUtil} / {@code LocalDateUtil}
 * 逐输入实测等价（含 null）, 本地实现已删除, 以下用本地类原有用例的期望值
 * 直接验证 z-util 那几个方法 —— 用例数不变, 但断言对象已是 z-util.
 * 保留的 todayStart / todayEnd 仍测本地类（z-util 的 getTodayStartStr /
 * getTodayEndStr 实测返回"当前时刻", 不是当日起止, 见
 * ZLcUtilDedupEquivalenceTest）.
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
        assertThat(DateUtil.fromLocalDateTime((LocalDateTime) null)).isNull();
    }

    @Test
    void shouldConvertLocalDateTimeToDate() {
        LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Date date = DateUtil.fromLocalDateTime(ldt);
        assertThat(date).isNotNull();
    }

    @Test
    void shouldReturnNullForNullDateToLocalDateTime() {
        assertThat(DateUtil.toLocalDateTime((Date) null)).isNull();
    }

    @Test
    void shouldConvertDateToLocalDateTime() {
        Date date = new Date();
        LocalDateTime ldt = DateUtil.toLocalDateTime(date);
        assertThat(ldt).isNotNull();
    }

    @Test
    void shouldReturnNullForNullFormatFromDate() {
        assertThat(DateUtil.format((Date) null)).isNull();
    }

    @Test
    void shouldFormatDate() {
        Date date = new Date(0);
        String result = DateUtil.format(date);
        assertThat(result).isNotNull();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void shouldReturnNullForNullFormatFromLocalDateTime() {
        assertThat(LocalDateTimeUtil.format((LocalDateTime) null)).isNull();
    }

    @Test
    void shouldFormatLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        String result = LocalDateTimeUtil.format(ldt);
        assertThat(result).isEqualTo("2024-01-15 10:30:00");
    }

    @Test
    void shouldReturnNullForNullFormatDateFromLocalDate() {
        assertThat(LocalDateUtil.format((LocalDate) null)).isNull();
    }

    @Test
    void shouldFormatLocalDate() {
        LocalDate ld = LocalDate.of(2024, 1, 15);
        String result = LocalDateUtil.format(ld);
        assertThat(result).isEqualTo("2024-01-15");
    }

    @Test
    void shouldFormatFromEpochMillis() {
        long millis = 0L;
        String result = DateUtil.format(DateUtil.fromEpochMilli(millis));
        assertThat(result).isNotNull();
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void shouldFormatDateFromEpochMillis() {
        long millis = 0L;
        LocalDate day = DateUtil.fromEpochMilli(millis).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        String result = LocalDateUtil.format(day);
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
