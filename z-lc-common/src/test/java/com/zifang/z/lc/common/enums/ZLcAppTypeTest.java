package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAppType 单元测试
 *
 * @author zifang
 */
class ZLcAppTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcAppType.values()).hasSize(2);
    }

    @Test
    void shouldHaveLCCode() {
        assertThat(ZLcAppType.LC.getCode()).isEqualTo("LC");
    }

    @Test
    void shouldHaveNCCode() {
        assertThat(ZLcAppType.NC.getCode()).isEqualTo("NC");
    }

    @Test
    void shouldContainLC() {
        assertThat(ZLcAppType.contains("LC")).isTrue();
    }

    @Test
    void shouldContainNC() {
        assertThat(ZLcAppType.contains("NC")).isTrue();
    }

    @Test
    void shouldNotContainUnknown() {
        assertThat(ZLcAppType.contains("unknown")).isFalse();
    }

    @Test
    void shouldNotContainNull() {
        assertThat(ZLcAppType.contains(null)).isFalse();
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcAppType.fromCode("LC")).isEqualTo(ZLcAppType.LC);
        assertThat(ZLcAppType.fromCode("NC")).isEqualTo(ZLcAppType.NC);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcAppType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcAppType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcAppType.valueOf("LC")).isEqualTo(ZLcAppType.LC);
        assertThat(ZLcAppType.valueOf("NC")).isEqualTo(ZLcAppType.NC);
    }
}
