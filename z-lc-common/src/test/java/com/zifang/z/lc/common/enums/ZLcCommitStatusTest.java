package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcCommitStatus 单元测试
 *
 * @author zifang
 */
class ZLcCommitStatusTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcCommitStatus.values()).hasSize(2);
    }

    @Test
    void shouldHaveDraftStatus() {
        assertThat(ZLcCommitStatus.DRAFT.getStatus()).isEqualTo(1);
    }

    @Test
    void shouldHaveDraftName() {
        assertThat(ZLcCommitStatus.DRAFT.getName()).isEqualTo("草稿态");
    }

    @Test
    void shouldHaveCommitStatus() {
        assertThat(ZLcCommitStatus.COMMIT.getStatus()).isEqualTo(2);
    }

    @Test
    void shouldHaveCommitName() {
        assertThat(ZLcCommitStatus.COMMIT.getName()).isEqualTo("提交态");
    }

    @Test
    void shouldFromStatus() {
        assertThat(ZLcCommitStatus.fromStatus(1)).isEqualTo(ZLcCommitStatus.DRAFT);
        assertThat(ZLcCommitStatus.fromStatus(2)).isEqualTo(ZLcCommitStatus.COMMIT);
    }

    @Test
    void shouldReturnNullForUnknownFromStatus() {
        assertThat(ZLcCommitStatus.fromStatus(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromStatus() {
        assertThat(ZLcCommitStatus.fromStatus(null)).isNull();
    }

    @Test
    void shouldIsCommittedReturnTrueForCommit() {
        assertThat(ZLcCommitStatus.COMMIT.isCommitted()).isTrue();
    }

    @Test
    void shouldIsCommittedReturnFalseForDraft() {
        assertThat(ZLcCommitStatus.DRAFT.isCommitted()).isFalse();
    }

    @Test
    void shouldIsDraftReturnTrueForDraft() {
        assertThat(ZLcCommitStatus.DRAFT.isDraft()).isTrue();
    }

    @Test
    void shouldIsDraftReturnFalseForCommit() {
        assertThat(ZLcCommitStatus.COMMIT.isDraft()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcCommitStatus.valueOf("DRAFT")).isEqualTo(ZLcCommitStatus.DRAFT);
        assertThat(ZLcCommitStatus.valueOf("COMMIT")).isEqualTo(ZLcCommitStatus.COMMIT);
    }
}
