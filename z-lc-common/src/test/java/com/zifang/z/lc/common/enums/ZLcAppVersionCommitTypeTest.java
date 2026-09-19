package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAppVersionCommitType 单元测试
 *
 * @author zifang
 */
class ZLcAppVersionCommitTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcAppVersionCommitType.values()).hasSize(3);
    }

    @Test
    void shouldHaveCreateType() {
        assertThat(ZLcAppVersionCommitType.CREATE.getType()).isEqualTo("create");
    }

    @Test
    void shouldHaveRemoveType() {
        assertThat(ZLcAppVersionCommitType.REMOVE.getType()).isEqualTo("remove");
    }

    @Test
    void shouldHaveUpdateType() {
        assertThat(ZLcAppVersionCommitType.UPDATE.getType()).isEqualTo("update");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcAppVersionCommitType.fromType("create")).isEqualTo(ZLcAppVersionCommitType.CREATE);
        assertThat(ZLcAppVersionCommitType.fromType("remove")).isEqualTo(ZLcAppVersionCommitType.REMOVE);
        assertThat(ZLcAppVersionCommitType.fromType("update")).isEqualTo(ZLcAppVersionCommitType.UPDATE);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcAppVersionCommitType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcAppVersionCommitType.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcAppVersionCommitType.valueOf("CREATE")).isEqualTo(ZLcAppVersionCommitType.CREATE);
        assertThat(ZLcAppVersionCommitType.valueOf("REMOVE")).isEqualTo(ZLcAppVersionCommitType.REMOVE);
        assertThat(ZLcAppVersionCommitType.valueOf("UPDATE")).isEqualTo(ZLcAppVersionCommitType.UPDATE);
    }
}
