package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcColumnTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcColumnTypeEnumTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcColumnTypeEnum.values()).hasSize(25);
    }

    @Test
    void shouldHaveAllTypeCodes() {
        assertThat(ZLcColumnTypeEnum.ALL_TYPE_CODES).hasSize(25);
        assertThat(ZLcColumnTypeEnum.ALL_TYPE_CODES).contains("int", "varchar", "datetime", "text");
    }

    @Test
    void shouldHaveDefaultLengthMap() {
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("int", "10");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("varchar", "255");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("double", "14,2");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("float", "14,2");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("bigint", "19");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("smallint", "5");
        assertThat(ZLcColumnTypeEnum.DEFAULT_LENGTH_MAP).containsEntry("decimal", "14,2");
    }

    @Test
    void shouldHaveNeverLengthType() {
        assertThat(ZLcColumnTypeEnum.NEVER_LENGTH_TYPE).contains("enum", "set", "json", "date", "time", "datetime",
                "tinyblob", "blob", "longblob", "mediumblob", "tinytext", "text", "mediumtext", "longtext");
    }

    @Test
    void shouldHaveColumnTypeRegex() {
        assertThat(ZLcColumnTypeEnum.COLUMN_TYPE_REGEX).isNotEmpty();
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcColumnTypeEnum.fromCode(1)).isEqualTo(ZLcColumnTypeEnum.BIGINT);
        assertThat(ZLcColumnTypeEnum.fromCode(11)).isEqualTo(ZLcColumnTypeEnum.INT);
        assertThat(ZLcColumnTypeEnum.fromCode(25)).isEqualTo(ZLcColumnTypeEnum.VARCHAR);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcColumnTypeEnum.fromCode(999)).isNull();
    }

    @Test
    void shouldFromTypeName() {
        assertThat(ZLcColumnTypeEnum.fromTypeName("int")).isEqualTo(ZLcColumnTypeEnum.INT);
        assertThat(ZLcColumnTypeEnum.fromTypeName("VARCHAR")).isEqualTo(ZLcColumnTypeEnum.VARCHAR);
        assertThat(ZLcColumnTypeEnum.fromTypeName("Datetime")).isEqualTo(ZLcColumnTypeEnum.DATETIME);
    }

    @Test
    void shouldReturnNullForUnknownFromTypeName() {
        assertThat(ZLcColumnTypeEnum.fromTypeName("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromTypeName() {
        assertThat(ZLcColumnTypeEnum.fromTypeName(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcColumnTypeEnum.valueOf("INT")).isEqualTo(ZLcColumnTypeEnum.INT);
        assertThat(ZLcColumnTypeEnum.valueOf("VARCHAR")).isEqualTo(ZLcColumnTypeEnum.VARCHAR);
        assertThat(ZLcColumnTypeEnum.valueOf("DATETIME")).isEqualTo(ZLcColumnTypeEnum.DATETIME);
    }
}
