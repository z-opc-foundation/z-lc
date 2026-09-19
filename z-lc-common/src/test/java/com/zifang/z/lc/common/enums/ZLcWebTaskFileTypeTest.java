package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebTaskFileType 单元测试
 *
 * @author zifang
 */
class ZLcWebTaskFileTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebTaskFileType.values()).hasSize(2);
    }

    @Test
    void shouldHaveSqlFileType() {
        assertThat(ZLcWebTaskFileType.SQL_FILE.getType()).isEqualTo("SQL");
    }

    @Test
    void shouldHaveSqlFileDesc() {
        assertThat(ZLcWebTaskFileType.SQL_FILE.getDesc()).isEqualTo("SQL文件");
    }

    @Test
    void shouldHaveExcelFileType() {
        assertThat(ZLcWebTaskFileType.EXCEL_FILE.getType()).isEqualTo("EXCEL");
    }

    @Test
    void shouldHaveExcelFileDesc() {
        assertThat(ZLcWebTaskFileType.EXCEL_FILE.getDesc()).isEqualTo("EXCEL文件");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebTaskFileType.fromType("SQL")).isEqualTo(ZLcWebTaskFileType.SQL_FILE);
        assertThat(ZLcWebTaskFileType.fromType("EXCEL")).isEqualTo(ZLcWebTaskFileType.EXCEL_FILE);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebTaskFileType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebTaskFileType.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebTaskFileType.valueOf("SQL_FILE")).isEqualTo(ZLcWebTaskFileType.SQL_FILE);
        assertThat(ZLcWebTaskFileType.valueOf("EXCEL_FILE")).isEqualTo(ZLcWebTaskFileType.EXCEL_FILE);
    }
}
