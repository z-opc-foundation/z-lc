package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataFileTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcDataFileTypeEnumTest {

    @Test
    void fromCode_shouldReturnEXCEL_When1() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcDataFileTypeEnum.EXCEL);
    }

    @Test
    void fromCode_shouldReturnCSV_When2() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcDataFileTypeEnum.CSV);
    }

    @Test
    void fromCode_shouldReturnPDF_When3() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(3);
        assertThat(result).isEqualTo(ZLcDataFileTypeEnum.PDF);
    }

    @Test
    void fromCode_shouldReturnPRINT_When4() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(4);
        assertThat(result).isEqualTo(ZLcDataFileTypeEnum.PRINT);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcDataFileTypeEnum result = ZLcDataFileTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcDataFileTypeEnum.EXCEL.getCode()).isEqualTo(1);
        assertThat(ZLcDataFileTypeEnum.CSV.getCode()).isEqualTo(2);
        assertThat(ZLcDataFileTypeEnum.PDF.getCode()).isEqualTo(3);
        assertThat(ZLcDataFileTypeEnum.PRINT.getCode()).isEqualTo(4);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcDataFileTypeEnum.EXCEL.getValue()).isEqualTo("xlsx");
        assertThat(ZLcDataFileTypeEnum.CSV.getValue()).isEqualTo("csv");
        assertThat(ZLcDataFileTypeEnum.PDF.getValue()).isEqualTo("pdf");
        assertThat(ZLcDataFileTypeEnum.PRINT.getValue()).isEqualTo("打印");
    }
}
