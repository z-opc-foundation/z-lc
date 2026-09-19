package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcLogicFolderType 单元测试
 *
 * @author zifang
 */
class ZLcLogicFolderTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcLogicFolderType.values()).hasSize(2);
    }

    @Test
    void shouldHaveCodeFolderType() {
        assertThat(ZLcLogicFolderType.CODE_FOLDER.getFolderType()).isEqualTo("code");
    }

    @Test
    void shouldHaveResourceFolderType() {
        assertThat(ZLcLogicFolderType.RESOURCE_FOLDER.getFolderType()).isEqualTo("resources");
    }

    @Test
    void shouldFromFolderType() {
        assertThat(ZLcLogicFolderType.fromFolderType("code")).isEqualTo(ZLcLogicFolderType.CODE_FOLDER);
        assertThat(ZLcLogicFolderType.fromFolderType("resources")).isEqualTo(ZLcLogicFolderType.RESOURCE_FOLDER);
    }

    @Test
    void shouldReturnNullForUnknownFromFolderType() {
        assertThat(ZLcLogicFolderType.fromFolderType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromFolderType() {
        assertThat(ZLcLogicFolderType.fromFolderType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcLogicFolderType.valueOf("CODE_FOLDER")).isEqualTo(ZLcLogicFolderType.CODE_FOLDER);
        assertThat(ZLcLogicFolderType.valueOf("RESOURCE_FOLDER")).isEqualTo(ZLcLogicFolderType.RESOURCE_FOLDER);
    }
}
