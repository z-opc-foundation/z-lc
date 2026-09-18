package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPhysicalColTransferDataModelEnum 单元测试
 *
 * @author zifang
 */
class ZLcPhysicalColTransferDataModelEnumTest {

    @Test
    void getDataFieldTypeByPhysicalColumnType_shouldReturnNumber_WhenBigint() {
        String result = ZLcPhysicalColTransferDataModelEnum.getDataFieldTypeByPhysicalColumnType("bigint");
        assertThat(result).isEqualTo("Number");
    }

    @Test
    void getDataFieldTypeByPhysicalColumnType_shouldReturnText_WhenVarchar() {
        String result = ZLcPhysicalColTransferDataModelEnum.getDataFieldTypeByPhysicalColumnType("varchar(255)");
        assertThat(result).isEqualTo("Text");
    }

    @Test
    void getDataFieldTypeByPhysicalColumnType_shouldReturnTime_WhenDatetime() {
        String result = ZLcPhysicalColTransferDataModelEnum.getDataFieldTypeByPhysicalColumnType("datetime");
        assertThat(result).isEqualTo("Time");
    }

    @Test
    void getDataFieldTypeByPhysicalColumnType_shouldReturnText_WhenNull() {
        String result = ZLcPhysicalColTransferDataModelEnum.getDataFieldTypeByPhysicalColumnType(null);
        assertThat(result).isEqualTo("Text");
    }

    @Test
    void getClassTypeByModelType_shouldReturnLongClass_WhenNumber() {
        Class<?> result = ZLcPhysicalColTransferDataModelEnum.getClassTypeByModelType("Number");
        assertThat(result).isEqualTo(Long.class);
    }

    @Test
    void getClassTypeByModelType_shouldReturnStringClass_WhenText() {
        Class<?> result = ZLcPhysicalColTransferDataModelEnum.getClassTypeByModelType("Text");
        assertThat(result).isEqualTo(String.class);
    }

    @Test
    void getClassTypeByModelType_shouldReturnLocalDateClass_WhenTime() {
        Class<?> result = ZLcPhysicalColTransferDataModelEnum.getClassTypeByModelType("Time");
        assertThat(result).isEqualTo(java.time.LocalDate.class);
    }

    @Test
    void getPhysicalColumnType_shouldReturnCorrectValue() {
        assertThat(ZLcPhysicalColTransferDataModelEnum.BIGINT.getPhysicalColumnType()).isEqualTo("bigint");
        assertThat(ZLcPhysicalColTransferDataModelEnum.VARCHAR.getPhysicalColumnType()).isEqualTo("varchar");
    }

    @Test
    void getDataFieldType_shouldReturnCorrectValue() {
        assertThat(ZLcPhysicalColTransferDataModelEnum.BIGINT.getDataFieldType()).isEqualTo("Number");
        assertThat(ZLcPhysicalColTransferDataModelEnum.VARCHAR.getDataFieldType()).isEqualTo("Text");
    }

    @Test
    void getJavaAttribute_shouldReturnCorrectValue() {
        assertThat(ZLcPhysicalColTransferDataModelEnum.BIGINT.getJavaAttribute()).isEqualTo("Long");
        assertThat(ZLcPhysicalColTransferDataModelEnum.VARCHAR.getJavaAttribute()).isEqualTo("String");
    }
}
