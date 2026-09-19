package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelStatus 单元测试
 *
 * @author zifang
 */
class ZLcLabelStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelStatus.values()).hasSize(2);
    }

    @Test
    void shouldHaveEnableCode() {
        assertThat(ZLcLabelStatus.ENABLE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveEnableDescription() {
        assertThat(ZLcLabelStatus.ENABLE.getDescription()).isEqualTo("启用");
    }

    @Test
    void shouldHaveUnEnableCode() {
        assertThat(ZLcLabelStatus.UN_ENABLE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveUnEnableDescription() {
        assertThat(ZLcLabelStatus.UN_ENABLE.getDescription()).isEqualTo("停用");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelStatus.fromCode(1)).isEqualTo(ZLcLabelStatus.ENABLE);
        assertThat(ZLcLabelStatus.fromCode(0)).isEqualTo(ZLcLabelStatus.UN_ENABLE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelStatus.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelStatus.fromCode(null)).isNull();
    }

    @Test
    void shouldIsEnabledReturnTrueForEnable() {
        assertThat(ZLcLabelStatus.ENABLE.isEnabled()).isTrue();
    }

    @Test
    void shouldIsEnabledReturnFalseForUnEnable() {
        assertThat(ZLcLabelStatus.UN_ENABLE.isEnabled()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelStatus.valueOf("ENABLE")).isEqualTo(ZLcLabelStatus.ENABLE);
        assertThat(ZLcLabelStatus.valueOf("UN_ENABLE")).isEqualTo(ZLcLabelStatus.UN_ENABLE);
    }
}
