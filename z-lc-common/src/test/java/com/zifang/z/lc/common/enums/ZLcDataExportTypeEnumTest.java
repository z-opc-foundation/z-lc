package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcDataExportTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcDataExportTypeEnumTest {

    @Test
    void fromCode_shouldReturnCURRENT_PAGE_When1() {
        ZLcDataExportTypeEnum result = ZLcDataExportTypeEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcDataExportTypeEnum.CURRENT_PAGE);
    }

    @Test
    void fromCode_shouldReturnALL_When2() {
        ZLcDataExportTypeEnum result = ZLcDataExportTypeEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcDataExportTypeEnum.ALL);
    }

    @Test
    void fromCode_shouldReturnCLIENT_PROVIDED_When3() {
        ZLcDataExportTypeEnum result = ZLcDataExportTypeEnum.fromCode(3);
        assertThat(result).isEqualTo(ZLcDataExportTypeEnum.CLIENT_PROVIDED);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcDataExportTypeEnum result = ZLcDataExportTypeEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcDataExportTypeEnum result = ZLcDataExportTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturn1_WhenCURRENT_PAGE() {
        assertThat(ZLcDataExportTypeEnum.CURRENT_PAGE.getCode()).isEqualTo(1);
    }

    @Test
    void getCode_shouldReturn2_WhenALL() {
        assertThat(ZLcDataExportTypeEnum.ALL.getCode()).isEqualTo(2);
    }

    @Test
    void getCode_shouldReturn3_WhenCLIENT_PROVIDED() {
        assertThat(ZLcDataExportTypeEnum.CLIENT_PROVIDED.getCode()).isEqualTo(3);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcDataExportTypeEnum.CURRENT_PAGE.getValue()).isEqualTo("当前页导出");
        assertThat(ZLcDataExportTypeEnum.ALL.getValue()).isEqualTo("全量导出");
        assertThat(ZLcDataExportTypeEnum.CLIENT_PROVIDED.getValue()).isEqualTo("客户端数据导出");
    }
}
