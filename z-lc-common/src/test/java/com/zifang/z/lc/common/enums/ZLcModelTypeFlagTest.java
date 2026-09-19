package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcModelTypeFlag 单元测试
 *
 * @author zifang
 */
class ZLcModelTypeFlagTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcModelTypeFlag.values()).hasSize(2);
    }

    @Test
    void shouldHavePhysicalTypeCode() {
        assertThat(ZLcModelTypeFlag.PHYSICAL_TYPE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveVirtualTypeCode() {
        assertThat(ZLcModelTypeFlag.VIRTUAL_TYPE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcModelTypeFlag.fromCode(1)).isEqualTo(ZLcModelTypeFlag.PHYSICAL_TYPE);
        assertThat(ZLcModelTypeFlag.fromCode(0)).isEqualTo(ZLcModelTypeFlag.VIRTUAL_TYPE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcModelTypeFlag.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcModelTypeFlag.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcModelTypeFlag.valueOf("PHYSICAL_TYPE")).isEqualTo(ZLcModelTypeFlag.PHYSICAL_TYPE);
        assertThat(ZLcModelTypeFlag.valueOf("VIRTUAL_TYPE")).isEqualTo(ZLcModelTypeFlag.VIRTUAL_TYPE);
    }
}
