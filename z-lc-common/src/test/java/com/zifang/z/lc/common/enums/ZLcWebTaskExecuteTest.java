package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebTaskExecute 单元测试
 *
 * @author zifang
 */
class ZLcWebTaskExecuteTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebTaskExecute.values()).hasSize(4);
    }

    @Test
    void shouldHaveSqlExportModeType() {
        assertThat(ZLcWebTaskExecute.SQL_EXPORT_MODE.getType()).isEqualTo("sqlExport");
    }

    @Test
    void shouldHaveSqlExportModeDesc() {
        assertThat(ZLcWebTaskExecute.SQL_EXPORT_MODE.getDesc()).isEqualTo("SQL导出模式");
    }

    @Test
    void shouldHaveMiddleTableModeType() {
        assertThat(ZLcWebTaskExecute.MIDDLE_TABLE_MODE.getType()).isEqualTo("midTable");
    }

    @Test
    void shouldHaveMiddleTableModeDesc() {
        assertThat(ZLcWebTaskExecute.MIDDLE_TABLE_MODE.getDesc()).isEqualTo("中间表模式");
    }

    @Test
    void shouldHaveCustomInitModeType() {
        assertThat(ZLcWebTaskExecute.CUSTOM_INIT_MODE.getType()).isEqualTo("customInit");
    }

    @Test
    void shouldHaveCustomInitModeDesc() {
        assertThat(ZLcWebTaskExecute.CUSTOM_INIT_MODE.getDesc()).isEqualTo("定制初始化");
    }

    @Test
    void shouldHaveDocumentModeType() {
        assertThat(ZLcWebTaskExecute.DOCUMENT_MODE.getType()).isEqualTo("document");
    }

    @Test
    void shouldHaveDocumentModeDesc() {
        assertThat(ZLcWebTaskExecute.DOCUMENT_MODE.getDesc()).isEqualTo("文档模式");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebTaskExecute.fromType("sqlExport")).isEqualTo(ZLcWebTaskExecute.SQL_EXPORT_MODE);
        assertThat(ZLcWebTaskExecute.fromType("midTable")).isEqualTo(ZLcWebTaskExecute.MIDDLE_TABLE_MODE);
        assertThat(ZLcWebTaskExecute.fromType("customInit")).isEqualTo(ZLcWebTaskExecute.CUSTOM_INIT_MODE);
        assertThat(ZLcWebTaskExecute.fromType("document")).isEqualTo(ZLcWebTaskExecute.DOCUMENT_MODE);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebTaskExecute.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebTaskExecute.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebTaskExecute.valueOf("SQL_EXPORT_MODE")).isEqualTo(ZLcWebTaskExecute.SQL_EXPORT_MODE);
        assertThat(ZLcWebTaskExecute.valueOf("MIDDLE_TABLE_MODE")).isEqualTo(ZLcWebTaskExecute.MIDDLE_TABLE_MODE);
        assertThat(ZLcWebTaskExecute.valueOf("CUSTOM_INIT_MODE")).isEqualTo(ZLcWebTaskExecute.CUSTOM_INIT_MODE);
        assertThat(ZLcWebTaskExecute.valueOf("DOCUMENT_MODE")).isEqualTo(ZLcWebTaskExecute.DOCUMENT_MODE);
    }
}
