package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAppPackageType 单元测试
 *
 * @author zifang
 */
class ZLcAppPackageTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcAppPackageType.values()).hasSize(2);
    }

    @Test
    void shouldHaveImportCode() {
        assertThat(ZLcAppPackageType.IMPORT.getCode()).isEqualTo("import");
    }

    @Test
    void shouldHaveOutputCode() {
        assertThat(ZLcAppPackageType.OUTPUT.getCode()).isEqualTo("output");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcAppPackageType.fromCode("import")).isEqualTo(ZLcAppPackageType.IMPORT);
        assertThat(ZLcAppPackageType.fromCode("output")).isEqualTo(ZLcAppPackageType.OUTPUT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcAppPackageType.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcAppPackageType.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcAppPackageType.valueOf("IMPORT")).isEqualTo(ZLcAppPackageType.IMPORT);
        assertThat(ZLcAppPackageType.valueOf("OUTPUT")).isEqualTo(ZLcAppPackageType.OUTPUT);
    }
}
