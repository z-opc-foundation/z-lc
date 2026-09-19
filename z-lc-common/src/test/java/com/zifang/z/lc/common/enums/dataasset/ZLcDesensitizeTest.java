package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDesensitize 单元测试
 *
 * @author zifang
 */
class ZLcDesensitizeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcDesensitize.values()).hasSize(2);
    }

    @Test
    void shouldHaveEnableCode() {
        assertThat(ZLcDesensitize.ENABLE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveEnableDescription() {
        assertThat(ZLcDesensitize.ENABLE.getDescription()).isEqualTo("是");
    }

    @Test
    void shouldHaveUnEnableCode() {
        assertThat(ZLcDesensitize.UN_ENABLE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveUnEnableDescription() {
        assertThat(ZLcDesensitize.UN_ENABLE.getDescription()).isEqualTo("否");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcDesensitize.fromCode(1)).isEqualTo(ZLcDesensitize.ENABLE);
        assertThat(ZLcDesensitize.fromCode(0)).isEqualTo(ZLcDesensitize.UN_ENABLE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcDesensitize.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcDesensitize.fromCode(null)).isNull();
    }

    @Test
    void shouldIsEnabledReturnTrueForEnable() {
        assertThat(ZLcDesensitize.ENABLE.isEnabled()).isTrue();
    }

    @Test
    void shouldIsEnabledReturnFalseForUnEnable() {
        assertThat(ZLcDesensitize.UN_ENABLE.isEnabled()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcDesensitize.valueOf("ENABLE")).isEqualTo(ZLcDesensitize.ENABLE);
        assertThat(ZLcDesensitize.valueOf("UN_ENABLE")).isEqualTo(ZLcDesensitize.UN_ENABLE);
    }
}
