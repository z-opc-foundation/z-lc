package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcModelFieldType 单元测试
 */
class ZLcModelFieldTypeTest {

    @Test
    void shouldHaveFiveValues() {
        assertThat(ZLcModelFieldType.values()).hasSize(5);
    }

    @Test
    void shouldHaveCorrectCodes() {
        assertThat(ZLcModelFieldType.TIME.getCode()).isEqualTo("Time");
        assertThat(ZLcModelFieldType.NUMBER.getCode()).isEqualTo("Number");
        assertThat(ZLcModelFieldType.TEXT.getCode()).isEqualTo("Text");
        assertThat(ZLcModelFieldType.OBJECT.getCode()).isEqualTo("Object");
        assertThat(ZLcModelFieldType.ARRAY.getCode()).isEqualTo("Array");
    }

    @Test
    void sysFieldTypesShouldContainBasicTypes() {
        assertThat(ZLcModelFieldType.SYS_FIELD_TYPES).contains("Time", "Number", "Text");
        assertThat(ZLcModelFieldType.SYS_FIELD_TYPES).hasSize(3);
    }

    @Test
    void sysFieldTypesShouldBeUnmodifiable() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ZLcModelFieldType.SYS_FIELD_TYPES.add("New"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isSysFieldTypeShouldReturnTrueForSys() {
        assertThat(ZLcModelFieldType.isSysFieldType("Time")).isTrue();
        assertThat(ZLcModelFieldType.isSysFieldType("Number")).isTrue();
        assertThat(ZLcModelFieldType.isSysFieldType("Text")).isTrue();
    }

    @Test
    void isSysFieldTypeShouldReturnFalseForCustom() {
        assertThat(ZLcModelFieldType.isSysFieldType("Object")).isFalse();
        assertThat(ZLcModelFieldType.isSysFieldType("Array")).isFalse();
        assertThat(ZLcModelFieldType.isSysFieldType("unknown")).isFalse();
    }

    @Test
    void isSysFieldTypeShouldHandleNull() {
        assertThat(ZLcModelFieldType.isSysFieldType(null)).isFalse();
    }

    @Test
    void fromCodeShouldReturnMatchingCaseInsensitive() {
        assertThat(ZLcModelFieldType.fromCode("time")).isEqualTo(ZLcModelFieldType.TIME);
        assertThat(ZLcModelFieldType.fromCode("TIME")).isEqualTo(ZLcModelFieldType.TIME);
        assertThat(ZLcModelFieldType.fromCode("Time")).isEqualTo(ZLcModelFieldType.TIME);
    }

    @Test
    void fromCodeShouldReturnNullForUnknown() {
        assertThat(ZLcModelFieldType.fromCode("unknown")).isNull();
    }

    @Test
    void fromCodeShouldReturnNullForNull() {
        assertThat(ZLcModelFieldType.fromCode(null)).isNull();
    }
}