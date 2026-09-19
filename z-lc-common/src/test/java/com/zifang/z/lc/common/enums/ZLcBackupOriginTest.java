package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcBackupOrigin 单元测试
 *
 * @author zifang
 */
class ZLcBackupOriginTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcBackupOrigin.values()).hasSize(2);
    }

    @Test
    void shouldHavePackageImportCode() {
        assertThat(ZLcBackupOrigin.PACKAGE_IMPORT.getCode()).isEqualTo(1);
    }

    @Test
    void shouldHavePackageImportName() {
        assertThat(ZLcBackupOrigin.PACKAGE_IMPORT.getName()).isEqualTo("包导入");
    }

    @Test
    void shouldHavePageSaveCode() {
        assertThat(ZLcBackupOrigin.PAGE_SAVE.getCode()).isEqualTo(2);
    }

    @Test
    void shouldHavePageSaveName() {
        assertThat(ZLcBackupOrigin.PAGE_SAVE.getName()).isEqualTo("页面保存");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcBackupOrigin.fromCode(1)).isEqualTo(ZLcBackupOrigin.PACKAGE_IMPORT);
        assertThat(ZLcBackupOrigin.fromCode(2)).isEqualTo(ZLcBackupOrigin.PAGE_SAVE);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcBackupOrigin.fromCode(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcBackupOrigin.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcBackupOrigin.valueOf("PACKAGE_IMPORT")).isEqualTo(ZLcBackupOrigin.PACKAGE_IMPORT);
        assertThat(ZLcBackupOrigin.valueOf("PAGE_SAVE")).isEqualTo(ZLcBackupOrigin.PAGE_SAVE);
    }
}
