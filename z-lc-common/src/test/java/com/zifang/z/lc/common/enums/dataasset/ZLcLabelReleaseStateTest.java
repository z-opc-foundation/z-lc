package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLabelReleaseState 单元测试
 *
 * @author zifang
 */
class ZLcLabelReleaseStateTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLabelReleaseState.values()).hasSize(4);
    }

    @Test
    void shouldHaveUnReleaseCode() {
        assertThat(ZLcLabelReleaseState.UN_RELEASE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveUnReleaseDescription() {
        assertThat(ZLcLabelReleaseState.UN_RELEASE.getDescription()).isEqualTo("未发布");
    }

    @Test
    void shouldHaveReleaseIngCode() {
        assertThat(ZLcLabelReleaseState.RELEASE_ING.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveReleaseIngDescription() {
        assertThat(ZLcLabelReleaseState.RELEASE_ING.getDescription()).isEqualTo("发布中");
    }

    @Test
    void shouldHaveReleasedCode() {
        assertThat(ZLcLabelReleaseState.RELEASED.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveReleasedDescription() {
        assertThat(ZLcLabelReleaseState.RELEASED.getDescription()).isEqualTo("已发布");
    }

    @Test
    void shouldHaveReleasedFailCode() {
        assertThat(ZLcLabelReleaseState.RELEASED_FAIL.getCode()).isEqualTo(3);
    }

    @Test
    void shouldHaveReleasedFailDescription() {
        assertThat(ZLcLabelReleaseState.RELEASED_FAIL.getDescription()).isEqualTo("发布失败");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcLabelReleaseState.fromCode(0)).isEqualTo(ZLcLabelReleaseState.UN_RELEASE);
        assertThat(ZLcLabelReleaseState.fromCode(1)).isEqualTo(ZLcLabelReleaseState.RELEASE_ING);
        assertThat(ZLcLabelReleaseState.fromCode(2)).isEqualTo(ZLcLabelReleaseState.RELEASED);
        assertThat(ZLcLabelReleaseState.fromCode(3)).isEqualTo(ZLcLabelReleaseState.RELEASED_FAIL);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcLabelReleaseState.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcLabelReleaseState.fromCode(null)).isNull();
    }

    @Test
    void shouldIsTerminalReturnTrueForReleased() {
        assertThat(ZLcLabelReleaseState.RELEASED.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnTrueForReleasedFail() {
        assertThat(ZLcLabelReleaseState.RELEASED_FAIL.isTerminal()).isTrue();
    }

    @Test
    void shouldIsTerminalReturnFalseForUnRelease() {
        assertThat(ZLcLabelReleaseState.UN_RELEASE.isTerminal()).isFalse();
    }

    @Test
    void shouldIsTerminalReturnFalseForReleaseIng() {
        assertThat(ZLcLabelReleaseState.RELEASE_ING.isTerminal()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLabelReleaseState.valueOf("UN_RELEASE")).isEqualTo(ZLcLabelReleaseState.UN_RELEASE);
        assertThat(ZLcLabelReleaseState.valueOf("RELEASE_ING")).isEqualTo(ZLcLabelReleaseState.RELEASE_ING);
        assertThat(ZLcLabelReleaseState.valueOf("RELEASED")).isEqualTo(ZLcLabelReleaseState.RELEASED);
        assertThat(ZLcLabelReleaseState.valueOf("RELEASED_FAIL")).isEqualTo(ZLcLabelReleaseState.RELEASED_FAIL);
    }
}
