package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSecurityLevel 单元测试
 *
 * @author zifang
 */
class ZLcSecurityLevelTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcSecurityLevel.values()).hasSize(5);
    }

    @Test
    void shouldHaveL1Code() {
        assertThat(ZLcSecurityLevel.L1.getCode()).isEqualTo("one_week");
    }

    @Test
    void shouldHaveL1Description() {
        assertThat(ZLcSecurityLevel.L1.getDescription()).isEqualTo("L1(完全公开)");
    }

    @Test
    void shouldHaveL1Level() {
        assertThat(ZLcSecurityLevel.L1.getLevel()).isEqualTo(1);
    }

    @Test
    void shouldHaveL2Code() {
        assertThat(ZLcSecurityLevel.L2.getCode()).isEqualTo("one_month");
    }

    @Test
    void shouldHaveL2Description() {
        assertThat(ZLcSecurityLevel.L2.getDescription()).isEqualTo("L2(较大范围公开)");
    }

    @Test
    void shouldHaveL2Level() {
        assertThat(ZLcSecurityLevel.L2.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldHaveL3Code() {
        assertThat(ZLcSecurityLevel.L3.getCode()).isEqualTo("three_month");
    }

    @Test
    void shouldHaveL3Description() {
        assertThat(ZLcSecurityLevel.L3.getDescription()).isEqualTo("L3(中等范围公开)");
    }

    @Test
    void shouldHaveL3Level() {
        assertThat(ZLcSecurityLevel.L3.getLevel()).isEqualTo(3);
    }

    @Test
    void shouldHaveL4Code() {
        assertThat(ZLcSecurityLevel.L4.getCode()).isEqualTo("six_month");
    }

    @Test
    void shouldHaveL4Description() {
        assertThat(ZLcSecurityLevel.L4.getDescription()).isEqualTo("L4(较小范围公开)");
    }

    @Test
    void shouldHaveL4Level() {
        assertThat(ZLcSecurityLevel.L4.getLevel()).isEqualTo(4);
    }

    @Test
    void shouldHaveL5Code() {
        assertThat(ZLcSecurityLevel.L5.getCode()).isEqualTo("one_year");
    }

    @Test
    void shouldHaveL5Description() {
        assertThat(ZLcSecurityLevel.L5.getDescription()).isEqualTo("L5(极小范围公开)");
    }

    @Test
    void shouldHaveL5Level() {
        assertThat(ZLcSecurityLevel.L5.getLevel()).isEqualTo(5);
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcSecurityLevel.fromCode("one_week")).isEqualTo(ZLcSecurityLevel.L1);
        assertThat(ZLcSecurityLevel.fromCode("one_month")).isEqualTo(ZLcSecurityLevel.L2);
        assertThat(ZLcSecurityLevel.fromCode("three_month")).isEqualTo(ZLcSecurityLevel.L3);
        assertThat(ZLcSecurityLevel.fromCode("six_month")).isEqualTo(ZLcSecurityLevel.L4);
        assertThat(ZLcSecurityLevel.fromCode("one_year")).isEqualTo(ZLcSecurityLevel.L5);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcSecurityLevel.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcSecurityLevel.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcSecurityLevel.valueOf("L1")).isEqualTo(ZLcSecurityLevel.L1);
        assertThat(ZLcSecurityLevel.valueOf("L2")).isEqualTo(ZLcSecurityLevel.L2);
        assertThat(ZLcSecurityLevel.valueOf("L3")).isEqualTo(ZLcSecurityLevel.L3);
        assertThat(ZLcSecurityLevel.valueOf("L4")).isEqualTo(ZLcSecurityLevel.L4);
        assertThat(ZLcSecurityLevel.valueOf("L5")).isEqualTo(ZLcSecurityLevel.L5);
    }
}
