package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBackupCursor 单元测试
 *
 * @author zifang
 */
class ZLcBackupCursorTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcBackupCursor.values()).hasSize(2);
    }

    @Test
    void shouldHaveNotCurrentCode() {
        assertThat(ZLcBackupCursor.NOT_CURRENT.getCode()).isEqualTo(0);
    }

    @Test
    void shouldHaveNotCurrentName() {
        assertThat(ZLcBackupCursor.NOT_CURRENT.getName()).isEqualTo("非当前");
    }

    @Test
    void shouldHaveCurrentCode() {
        assertThat(ZLcBackupCursor.CURRENT.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHaveCurrentName() {
        assertThat(ZLcBackupCursor.CURRENT.getName()).isEqualTo("当前");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcBackupCursor.fromCode(0)).isEqualTo(ZLcBackupCursor.NOT_CURRENT);
        assertThat(ZLcBackupCursor.fromCode(1)).isEqualTo(ZLcBackupCursor.CURRENT);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcBackupCursor.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcBackupCursor.fromCode(null)).isNull();
    }

    @Test
    void shouldIsCurrentReturnTrueForCurrent() {
        assertThat(ZLcBackupCursor.CURRENT.isCurrent()).isTrue();
    }

    @Test
    void shouldIsCurrentReturnFalseForNotCurrent() {
        assertThat(ZLcBackupCursor.NOT_CURRENT.isCurrent()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcBackupCursor.valueOf("NOT_CURRENT")).isEqualTo(ZLcBackupCursor.NOT_CURRENT);
        assertThat(ZLcBackupCursor.valueOf("CURRENT")).isEqualTo(ZLcBackupCursor.CURRENT);
    }
}
