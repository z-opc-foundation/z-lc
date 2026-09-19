package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFieldRelateType 单元测试
 *
 * @author zifang
 */
class ZLcFieldRelateTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcFieldRelateType.values()).hasSize(2);
    }

    @Test
    void shouldHaveO2OCode() {
        assertThat(ZLcFieldRelateType.O2O.getCode()).isEqualTo("oneToOne");
    }

    @Test
    void shouldHaveO2LCode() {
        assertThat(ZLcFieldRelateType.O2L.getCode()).isEqualTo("oneToMany");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcFieldRelateType.fromCode("oneToOne")).isEqualTo(ZLcFieldRelateType.O2O);
        assertThat(ZLcFieldRelateType.fromCode("oneToMany")).isEqualTo(ZLcFieldRelateType.O2L);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcFieldRelateType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcFieldRelateType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcFieldRelateType.valueOf("O2O")).isEqualTo(ZLcFieldRelateType.O2O);
        assertThat(ZLcFieldRelateType.valueOf("O2L")).isEqualTo(ZLcFieldRelateType.O2L);
    }
}
