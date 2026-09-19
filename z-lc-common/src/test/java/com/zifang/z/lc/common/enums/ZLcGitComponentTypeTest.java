package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcGitComponentType 单元测试
 *
 * @author zifang
 */
class ZLcGitComponentTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcGitComponentType.values()).hasSize(3);
    }

    @Test
    void shouldHaveFolderComponentType() {
        assertThat(ZLcGitComponentType.FOLDER.getComponentType()).isEqualTo("folder");
    }

    @Test
    void shouldHaveFileComponentType() {
        assertThat(ZLcGitComponentType.FILE.getComponentType()).isEqualTo("file");
    }

    @Test
    void shouldHaveUnSupportComponentType() {
        assertThat(ZLcGitComponentType.UN_SUPPORT.getComponentType()).isEqualTo("unSupport");
    }

    @Test
    void shouldFromComponentType() {
        assertThat(ZLcGitComponentType.fromComponentType("folder")).isEqualTo(ZLcGitComponentType.FOLDER);
        assertThat(ZLcGitComponentType.fromComponentType("file")).isEqualTo(ZLcGitComponentType.FILE);
        assertThat(ZLcGitComponentType.fromComponentType("unSupport")).isEqualTo(ZLcGitComponentType.UN_SUPPORT);
    }

    @Test
    void shouldReturnNullForUnknownFromComponentType() {
        assertThat(ZLcGitComponentType.fromComponentType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromComponentType() {
        assertThat(ZLcGitComponentType.fromComponentType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcGitComponentType.valueOf("FOLDER")).isEqualTo(ZLcGitComponentType.FOLDER);
        assertThat(ZLcGitComponentType.valueOf("FILE")).isEqualTo(ZLcGitComponentType.FILE);
        assertThat(ZLcGitComponentType.valueOf("UN_SUPPORT")).isEqualTo(ZLcGitComponentType.UN_SUPPORT);
    }
}
