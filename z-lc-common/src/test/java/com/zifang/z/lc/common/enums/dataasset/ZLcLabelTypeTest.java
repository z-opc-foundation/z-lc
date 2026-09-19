package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelType 单元测试
 *
 * @author zifang
 */
class ZLcLabelTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelType.values()).hasSize(2);
    }

    @Test
    void shouldHaveImportCode() {
        assertThat(ZLcLabelType.IMPORT.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveImportName() {
        assertThat(ZLcLabelType.IMPORT.getName()).isEqualTo("导入");
    }

    @Test
    void shouldHaveBusinessCode() {
        assertThat(ZLcLabelType.BUSINESS.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveBusinessName() {
        assertThat(ZLcLabelType.BUSINESS.getName()).isEqualTo("业务");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelType.fromCode(1)).isEqualTo(ZLcLabelType.IMPORT);
        assertThat(ZLcLabelType.fromCode(2)).isEqualTo(ZLcLabelType.BUSINESS);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelType.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelType.fromCode(null)).isNull();
    }

    @Test
    void shouldIsImportReturnTrueForImport() {
        assertThat(ZLcLabelType.IMPORT.isImport()).isTrue();
    }

    @Test
    void shouldIsImportReturnFalseForBusiness() {
        assertThat(ZLcLabelType.BUSINESS.isImport()).isFalse();
    }

    @Test
    void shouldIsBusinessReturnTrueForBusiness() {
        assertThat(ZLcLabelType.BUSINESS.isBusiness()).isTrue();
    }

    @Test
    void shouldIsBusinessReturnFalseForImport() {
        assertThat(ZLcLabelType.IMPORT.isBusiness()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelType.valueOf("IMPORT")).isEqualTo(ZLcLabelType.IMPORT);
        assertThat(ZLcLabelType.valueOf("BUSINESS")).isEqualTo(ZLcLabelType.BUSINESS);
    }
}
