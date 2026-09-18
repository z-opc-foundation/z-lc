package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcInitPackageRecordTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcInitPackageRecordTypeEnumTest {

    @Test
    void fromCode_shouldReturnEXPORT_WhenExport() {
        ZLcInitPackageRecordTypeEnum result = ZLcInitPackageRecordTypeEnum.fromCode("export");
        assertThat(result).isEqualTo(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_EXPORT);
    }

    @Test
    void fromCode_shouldReturnIMPORT_WhenImport() {
        ZLcInitPackageRecordTypeEnum result = ZLcInitPackageRecordTypeEnum.fromCode("import");
        assertThat(result).isEqualTo(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_IMPORT);
    }

    @Test
    void fromCode_shouldReturnNull_WhenUnknown() {
        ZLcInitPackageRecordTypeEnum result = ZLcInitPackageRecordTypeEnum.fromCode("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcInitPackageRecordTypeEnum result = ZLcInitPackageRecordTypeEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_EXPORT.getCode()).isEqualTo("export");
        assertThat(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_IMPORT.getCode()).isEqualTo("import");
    }

    @Test
    void getDesc_shouldReturnCorrectValue() {
        assertThat(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_EXPORT.getDesc()).isEqualTo("应用初始化包导出");
        assertThat(ZLcInitPackageRecordTypeEnum.APP_INIT_PACKAGE_IMPORT.getDesc()).isEqualTo("应用初始化包导入");
    }
}
