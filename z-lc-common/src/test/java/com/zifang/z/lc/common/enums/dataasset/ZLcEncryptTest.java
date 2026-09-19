package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcEncrypt 单元测试
 *
 * @author zifang
 */
class ZLcEncryptTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcEncrypt.values()).hasSize(2);
    }

    @Test
    void shouldHaveEnableCode() {
        assertThat(ZLcEncrypt.ENABLE.getCode()).isTrue();
    }

    @Test
    void shouldHaveEnableDescription() {
        assertThat(ZLcEncrypt.ENABLE.getDescription()).isEqualTo("是");
    }

    @Test
    void shouldHaveUnEnableCode() {
        assertThat(ZLcEncrypt.UN_ENABLE.getCode()).isFalse();
    }

    @Test
    void shouldHaveUnEnableDescription() {
        assertThat(ZLcEncrypt.UN_ENABLE.getDescription()).isEqualTo("否");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcEncrypt.fromCode(true)).isEqualTo(ZLcEncrypt.ENABLE);
        assertThat(ZLcEncrypt.fromCode(false)).isEqualTo(ZLcEncrypt.UN_ENABLE);
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcEncrypt.fromCode(null)).isNull();
    }

    @Test
    void shouldIsEnabledReturnTrueForEnable() {
        assertThat(ZLcEncrypt.ENABLE.isEnabled()).isTrue();
    }

    @Test
    void shouldIsEnabledReturnFalseForUnEnable() {
        assertThat(ZLcEncrypt.UN_ENABLE.isEnabled()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcEncrypt.valueOf("ENABLE")).isEqualTo(ZLcEncrypt.ENABLE);
        assertThat(ZLcEncrypt.valueOf("UN_ENABLE")).isEqualTo(ZLcEncrypt.UN_ENABLE);
    }
}
