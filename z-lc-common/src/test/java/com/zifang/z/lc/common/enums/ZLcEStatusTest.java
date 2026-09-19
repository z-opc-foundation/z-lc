package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcEStatus 单元测试
 *
 * @author zifang
 */
class ZLcEStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcEStatus.values()).hasSize(2);
    }

    @Test
    void shouldHaveFalseCode() {
        assertThat(ZLcEStatus.FALSE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveFalseMessage() {
        assertThat(ZLcEStatus.FALSE.getMessage()).isEqualTo("FALSE");
    }

    @Test
    void shouldHaveTrueCode() {
        assertThat(ZLcEStatus.TRUE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveTrueMessage() {
        assertThat(ZLcEStatus.TRUE.getMessage()).isEqualTo("TRUE");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcEStatus.fromCode(0)).isEqualTo(ZLcEStatus.FALSE);
        assertThat(ZLcEStatus.fromCode(1)).isEqualTo(ZLcEStatus.TRUE);
    }

    @Test
    void shouldFromCodeReturnFalseForUnknown() {
        assertThat(ZLcEStatus.fromCode(999)).isEqualTo(ZLcEStatus.FALSE);
    }

    @Test
    void shouldIsTrueReturnTrueForTrue() {
        assertThat(ZLcEStatus.TRUE.isTrue()).isTrue();
    }

    @Test
    void shouldIsTrueReturnFalseForFalse() {
        assertThat(ZLcEStatus.FALSE.isTrue()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcEStatus.valueOf("FALSE")).isEqualTo(ZLcEStatus.FALSE);
        assertThat(ZLcEStatus.valueOf("TRUE")).isEqualTo(ZLcEStatus.TRUE);
    }
}
