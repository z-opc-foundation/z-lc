package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAuthTime 单元测试
 *
 * @author zifang
 */
class ZLcAuthTimeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcAuthTime.values()).hasSize(5);
    }

    @Test
    void shouldHaveOneWeekCode() {
        assertThat(ZLcAuthTime.ONE_WEEK.getCode()).isEqualTo("one_week");
    }

    @Test
    void shouldHaveOneWeekDescription() {
        assertThat(ZLcAuthTime.ONE_WEEK.getDescription()).isEqualTo("七天");
    }

    @Test
    void shouldHaveOneMonthCode() {
        assertThat(ZLcAuthTime.ONE_MONTH.getCode()).isEqualTo("one_month");
    }

    @Test
    void shouldHaveOneMonthDescription() {
        assertThat(ZLcAuthTime.ONE_MONTH.getDescription()).isEqualTo("一个月");
    }

    @Test
    void shouldHaveThreeMonthCode() {
        assertThat(ZLcAuthTime.THREE_MONTH.getCode()).isEqualTo("three_month");
    }

    @Test
    void shouldHaveThreeMonthDescription() {
        assertThat(ZLcAuthTime.THREE_MONTH.getDescription()).isEqualTo("一个季度");
    }

    @Test
    void shouldHaveSixMonthCode() {
        assertThat(ZLcAuthTime.SIX_MONTH.getCode()).isEqualTo("six_month");
    }

    @Test
    void shouldHaveSixMonthDescription() {
        assertThat(ZLcAuthTime.SIX_MONTH.getDescription()).isEqualTo("半年");
    }

    @Test
    void shouldHaveOneYearCode() {
        assertThat(ZLcAuthTime.ONE_YEAR.getCode()).isEqualTo("one_year");
    }

    @Test
    void shouldHaveOneYearDescription() {
        assertThat(ZLcAuthTime.ONE_YEAR.getDescription()).isEqualTo("一年");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcAuthTime.fromCode("one_week")).isEqualTo(ZLcAuthTime.ONE_WEEK);
        assertThat(ZLcAuthTime.fromCode("one_month")).isEqualTo(ZLcAuthTime.ONE_MONTH);
        assertThat(ZLcAuthTime.fromCode("three_month")).isEqualTo(ZLcAuthTime.THREE_MONTH);
        assertThat(ZLcAuthTime.fromCode("six_month")).isEqualTo(ZLcAuthTime.SIX_MONTH);
        assertThat(ZLcAuthTime.fromCode("one_year")).isEqualTo(ZLcAuthTime.ONE_YEAR);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcAuthTime.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcAuthTime.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcAuthTime.valueOf("ONE_WEEK")).isEqualTo(ZLcAuthTime.ONE_WEEK);
        assertThat(ZLcAuthTime.valueOf("ONE_MONTH")).isEqualTo(ZLcAuthTime.ONE_MONTH);
        assertThat(ZLcAuthTime.valueOf("THREE_MONTH")).isEqualTo(ZLcAuthTime.THREE_MONTH);
        assertThat(ZLcAuthTime.valueOf("SIX_MONTH")).isEqualTo(ZLcAuthTime.SIX_MONTH);
        assertThat(ZLcAuthTime.valueOf("ONE_YEAR")).isEqualTo(ZLcAuthTime.ONE_YEAR);
    }
}
