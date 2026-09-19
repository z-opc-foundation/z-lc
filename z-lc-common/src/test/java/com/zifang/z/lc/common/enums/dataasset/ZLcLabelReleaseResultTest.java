package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelReleaseResult 单元测试
 *
 * @author zifang
 */
class ZLcLabelReleaseResultTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelReleaseResult.values()).hasSize(4);
    }

    @Test
    void shouldHaveUnReleaseCode() {
        assertThat(ZLcLabelReleaseResult.UN_RELEASE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveUnReleaseDescription() {
        assertThat(ZLcLabelReleaseResult.UN_RELEASE.getDescription()).isEqualTo("未发布");
    }

    @Test
    void shouldHaveReleaseIngCode() {
        assertThat(ZLcLabelReleaseResult.RELEASE_ING.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveReleaseIngDescription() {
        assertThat(ZLcLabelReleaseResult.RELEASE_ING.getDescription()).isEqualTo("发布中");
    }

    @Test
    void shouldHaveReleasedCode() {
        assertThat(ZLcLabelReleaseResult.RELEASED.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveReleasedDescription() {
        assertThat(ZLcLabelReleaseResult.RELEASED.getDescription()).isEqualTo("已发布");
    }

    @Test
    void shouldHaveReleasedFailCode() {
        assertThat(ZLcLabelReleaseResult.RELEASED_FAIL.getCode()).isEqualTo(3);
    }

    @Test
    void shouldHaveReleasedFailDescription() {
        assertThat(ZLcLabelReleaseResult.RELEASED_FAIL.getDescription()).isEqualTo("发布失败");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelReleaseResult.fromCode(0)).isEqualTo(ZLcLabelReleaseResult.UN_RELEASE);
        assertThat(ZLcLabelReleaseResult.fromCode(1)).isEqualTo(ZLcLabelReleaseResult.RELEASE_ING);
        assertThat(ZLcLabelReleaseResult.fromCode(2)).isEqualTo(ZLcLabelReleaseResult.RELEASED);
        assertThat(ZLcLabelReleaseResult.fromCode(3)).isEqualTo(ZLcLabelReleaseResult.RELEASED_FAIL);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelReleaseResult.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelReleaseResult.fromCode(null)).isNull();
    }

    @Test
    void shouldIsTerminalReturnTrueForReleased() {
        assertThat(ZLcLabelReleaseResult.RELEASED.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForReleasedFail() {
        assertThat(ZLcLabelReleaseResult.RELEASED_FAIL.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnFalseForUnRelease() {
        assertThat(ZLcLabelReleaseResult.UN_RELEASE.isTerminal()).isFalse();
    }

    @Test
    void shouldIsTerminalReturnFalseForReleaseIng() {
        assertThat(ZLcLabelReleaseResult.RELEASE_ING.isTerminal()).isFalse();
    }

    @Test
    void shouldIsSuccessReturnTrueForReleased() {
        assertThat(ZLcLabelReleaseResult.RELEASED.isSuccess()).isTrue();
    }

    @Test
    void shouldIsSuccessReturnFalseForReleasedFail() {
        assertThat(ZLcLabelReleaseResult.RELEASED_FAIL.isSuccess()).isFalse();
    }

    @Test
    void shouldIsFailReturnTrueForReleasedFail() {
        assertThat(ZLcLabelReleaseResult.RELEASED_FAIL.isFail()).isTrue();
    }

    @Test
    void shouldIsFailReturnFalseForReleased() {
        assertThat(ZLcLabelReleaseResult.RELEASED.isFail()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelReleaseResult.valueOf("UN_RELEASE")).isEqualTo(ZLcLabelReleaseResult.UN_RELEASE);
        assertThat(ZLcLabelReleaseResult.valueOf("RELEASE_ING")).isEqualTo(ZLcLabelReleaseResult.RELEASE_ING);
        assertThat(ZLcLabelReleaseResult.valueOf("RELEASED")).isEqualTo(ZLcLabelReleaseResult.RELEASED);
        assertThat(ZLcLabelReleaseResult.valueOf("RELEASED_FAIL")).isEqualTo(ZLcLabelReleaseResult.RELEASED_FAIL);
    }
}
