package com.zifang.z.lc.common.seal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealType 单元测试
 *
 * @author zifang
 */
class ZLcSealTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcSealType.values()).hasSize(2);
    }

    @Test
    void shouldHaveAcrossCode() {
        assertThat(ZLcSealType.ACROSS.getCode()).isEqualTo("ACROSS");
    }

    @Test
    void shouldHaveAcrossDescription() {
        assertThat(ZLcSealType.ACROSS.getDescription()).isEqualTo("骑缝章");
    }

    @Test
    void shouldHaveOfficialCode() {
        assertThat(ZLcSealType.OFFICIAL.getCode()).isEqualTo("OFFICIAL");
    }

    @Test
    void shouldHaveOfficialDescription() {
        assertThat(ZLcSealType.OFFICIAL.getDescription()).isEqualTo("普通章");
    }

    @Test
    void shouldOfCode() {
        assertThat(ZLcSealType.ofCode("ACROSS")).isEqualTo(ZLcSealType.ACROSS);
        assertThat(ZLcSealType.ofCode("OFFICIAL")).isEqualTo(ZLcSealType.OFFICIAL);
    }

    @Test
    void shouldOfCodeBeCaseInsensitive() {
        assertThat(ZLcSealType.ofCode("across")).isEqualTo(ZLcSealType.ACROSS);
        assertThat(ZLcSealType.ofCode("official")).isEqualTo(ZLcSealType.OFFICIAL);
    }

    @Test
    void shouldOfCodeHandleTrim() {
        assertThat(ZLcSealType.ofCode("  ACROSS  ")).isEqualTo(ZLcSealType.ACROSS);
        assertThat(ZLcSealType.ofCode("  OFFICIAL  ")).isEqualTo(ZLcSealType.OFFICIAL);
    }

    @Test
    void shouldReturnNullForUnknownOfCode() {
        assertThat(ZLcSealType.ofCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullOfCode() {
        assertThat(ZLcSealType.ofCode(null)).isNull();
    }

    @Test
    void shouldReturnNullForEmptyOfCode() {
        assertThat(ZLcSealType.ofCode("")).isNull();
    }

    @Test
    void shouldReturnNullForBlankOfCode() {
        assertThat(ZLcSealType.ofCode("   ")).isNull();
    }

    @Test
    void shouldGetDescriptionByCode() {
        assertThat(ZLcSealType.getDescriptionByCode("ACROSS")).isEqualTo("骑缝章");
        assertThat(ZLcSealType.getDescriptionByCode("OFFICIAL")).isEqualTo("普通章");
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForUnknown() {
        assertThat(ZLcSealType.getDescriptionByCode("unknown")).isNull();
    }

    @Test
    void shouldGetDescriptionByCodeReturnNullForNull() {
        assertThat(ZLcSealType.getDescriptionByCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcSealType.valueOf("ACROSS")).isEqualTo(ZLcSealType.ACROSS);
        assertThat(ZLcSealType.valueOf("OFFICIAL")).isEqualTo(ZLcSealType.OFFICIAL);
    }
}
