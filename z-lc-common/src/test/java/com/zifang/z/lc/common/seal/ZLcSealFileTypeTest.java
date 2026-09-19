package com.zifang.z.lc.common.seal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealFileType 单元测试
 */
class ZLcSealFileTypeTest {

    @Test
    void shouldHaveThreeValues() {
        assertThat(ZLcSealFileType.values()).hasSize(3);
    }

    @Test
    void shouldHavePdfValue() {
        assertThat(ZLcSealFileType.PDF.name()).isEqualTo("PDF");
    }

    @Test
    void shouldHaveWordValue() {
        assertThat(ZLcSealFileType.WORD.name()).isEqualTo("WORD");
    }

    @Test
    void shouldHaveUnknownValue() {
        assertThat(ZLcSealFileType.UNKNOWN.name()).isEqualTo("UNKNOWN");
    }

    @Test
    void valueOfShouldReturnMatching() {
        assertThat(ZLcSealFileType.valueOf("PDF")).isEqualTo(ZLcSealFileType.PDF);
        assertThat(ZLcSealFileType.valueOf("WORD")).isEqualTo(ZLcSealFileType.WORD);
        assertThat(ZLcSealFileType.valueOf("UNKNOWN")).isEqualTo(ZLcSealFileType.UNKNOWN);
    }
}