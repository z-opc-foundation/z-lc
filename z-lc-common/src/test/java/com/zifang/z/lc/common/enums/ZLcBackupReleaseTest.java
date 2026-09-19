package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBackupRelease 单元测试
 *
 * @author zifang
 */
class ZLcBackupReleaseTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcBackupRelease.values()).hasSize(3);
    }

    @Test
    void shouldHaveTempSaveCode() {
        assertThat(ZLcBackupRelease.TEMP_SAVE.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveTempSaveName() {
        assertThat(ZLcBackupRelease.TEMP_SAVE.getName()).isEqualTo("仅保存未发布");
    }

    @Test
    void shouldHaveReleaseCode() {
        assertThat(ZLcBackupRelease.RELEASE.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveReleaseName() {
        assertThat(ZLcBackupRelease.RELEASE.getName()).isEqualTo("已发布");
    }

    @Test
    void shouldHaveHistReleaseCode() {
        assertThat(ZLcBackupRelease.HIST_RELEASE.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHaveHistReleaseName() {
        assertThat(ZLcBackupRelease.HIST_RELEASE.getName()).isEqualTo("历史已发布");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcBackupRelease.fromCode(0)).isEqualTo(ZLcBackupRelease.TEMP_SAVE);
        assertThat(ZLcBackupRelease.fromCode(1)).isEqualTo(ZLcBackupRelease.RELEASE);
        assertThat(ZLcBackupRelease.fromCode(2)).isEqualTo(ZLcBackupRelease.HIST_RELEASE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcBackupRelease.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcBackupRelease.fromCode(null)).isNull();
    }

    @Test
    void shouldIsReleasedReturnTrueForRelease() {
        assertThat(ZLcBackupRelease.RELEASE.isReleased()).isTrue();
    }

    @Test
    void shouldIsReleasedReturnTrueForHistRelease() {
        assertThat(ZLcBackupRelease.HIST_RELEASE.isReleased()).isTrue();
    }

    @Test
    void shouldIsReleasedReturnFalseForTempSave() {
        assertThat(ZLcBackupRelease.TEMP_SAVE.isReleased()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcBackupRelease.valueOf("TEMP_SAVE")).isEqualTo(ZLcBackupRelease.TEMP_SAVE);
        assertThat(ZLcBackupRelease.valueOf("RELEASE")).isEqualTo(ZLcBackupRelease.RELEASE);
        assertThat(ZLcBackupRelease.valueOf("HIST_RELEASE")).isEqualTo(ZLcBackupRelease.HIST_RELEASE);
    }
}
