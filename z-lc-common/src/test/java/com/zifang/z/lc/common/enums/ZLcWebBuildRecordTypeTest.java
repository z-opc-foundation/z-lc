package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebBuildRecordType 单元测试
 *
 * @author zifang
 */
class ZLcWebBuildRecordTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebBuildRecordType.values()).hasSize(2);
    }

    @Test
    void shouldHaveExportType() {
        assertThat(ZLcWebBuildRecordType.EXPORT.getType()).isEqualTo("export");
    }

    @Test
    void shouldHaveExportDesc() {
        assertThat(ZLcWebBuildRecordType.EXPORT.getDesc()).isEqualTo("导出");
    }

    @Test
    void shouldHaveImportType() {
        assertThat(ZLcWebBuildRecordType.IMPORT.getType()).isEqualTo("import");
    }

    @Test
    void shouldHaveImportDesc() {
        assertThat(ZLcWebBuildRecordType.IMPORT.getDesc()).isEqualTo("导入");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebBuildRecordType.fromType("export")).isEqualTo(ZLcWebBuildRecordType.EXPORT);
        assertThat(ZLcWebBuildRecordType.fromType("import")).isEqualTo(ZLcWebBuildRecordType.IMPORT);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebBuildRecordType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebBuildRecordType.fromType(null)).isNull();
    }

    @Test
    void shouldIsExportReturnTrueForExport() {
        assertThat(ZLcWebBuildRecordType.EXPORT.isExport()).isTrue();
    }

    @Test
    void shouldIsExportReturnFalseForImport() {
        assertThat(ZLcWebBuildRecordType.IMPORT.isExport()).isFalse();
    }

    @Test
    void shouldIsImportReturnTrueForImport() {
        assertThat(ZLcWebBuildRecordType.IMPORT.isImport()).isTrue();
    }

    @Test
    void shouldIsImportReturnFalseForExport() {
        assertThat(ZLcWebBuildRecordType.EXPORT.isImport()).isFalse();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebBuildRecordType.valueOf("EXPORT")).isEqualTo(ZLcWebBuildRecordType.EXPORT);
        assertThat(ZLcWebBuildRecordType.valueOf("IMPORT")).isEqualTo(ZLcWebBuildRecordType.IMPORT);
    }
}
