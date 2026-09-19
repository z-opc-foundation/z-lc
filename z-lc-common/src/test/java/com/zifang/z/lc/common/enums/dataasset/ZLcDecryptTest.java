package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDecrypt 单元测试
 *
 * @author zifang
 */
class ZLcDecryptTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcDecrypt.values()).hasSize(2);
    }

    @Test
    void shouldHaveEnableCode() {
        assertThat(ZLcDecrypt.ENABLE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveEnableDescription() {
        assertThat(ZLcDecrypt.ENABLE.getDescription()).isEqualTo("是");
    }

    @Test
    void shouldHaveUnEnableCode() {
        assertThat(ZLcDecrypt.UN_ENABLE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveUnEnableDescription() {
        assertThat(ZLcDecrypt.UN_ENABLE.getDescription()).isEqualTo("否");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcDecrypt.fromCode(1)).isEqualTo(ZLcDecrypt.ENABLE);
        assertThat(ZLcDecrypt.fromCode(0)).isEqualTo(ZLcDecrypt.UN_ENABLE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcDecrypt.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcDecrypt.fromCode(null)).isNull();
    }

    @Test
    void shouldIsEnabledReturnTrueForEnable() {
        assertThat(ZLcDecrypt.ENABLE.isEnabled()).isTrue();
    }

    @Test
    void shouldIsEnabledReturnFalseForUnEnable() {
        assertThat(ZLcDecrypt.UN_ENABLE.isEnabled()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcDecrypt.valueOf("ENABLE")).isEqualTo(ZLcDecrypt.ENABLE);
        assertThat(ZLcDecrypt.valueOf("UN_ENABLE")).isEqualTo(ZLcDecrypt.UN_ENABLE);
    }
}
