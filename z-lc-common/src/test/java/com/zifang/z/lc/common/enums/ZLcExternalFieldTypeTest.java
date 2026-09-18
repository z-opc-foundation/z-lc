package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExternalFieldType 单元测试
 *
 * @author zifang
 */
class ZLcExternalFieldTypeTest {

    @Test
    void getByCode_shouldReturnTEXT_When1() {
        ZLcExternalFieldType result = ZLcExternalFieldType.getByCode(1);
        assertThat(result).isEqualTo(ZLcExternalFieldType.TEXT);
    }

    @Test
    void getByCode_shouldReturnNUMBER_When2() {
        ZLcExternalFieldType result = ZLcExternalFieldType.getByCode(2);
        assertThat(result).isEqualTo(ZLcExternalFieldType.NUMBER);
    }

    @Test
    void getByCode_shouldReturnDATE_When3() {
        ZLcExternalFieldType result = ZLcExternalFieldType.getByCode(3);
        assertThat(result).isEqualTo(ZLcExternalFieldType.DATE);
    }

    @Test
    void getByCode_shouldReturnDECIMAL_When4() {
        ZLcExternalFieldType result = ZLcExternalFieldType.getByCode(4);
        assertThat(result).isEqualTo(ZLcExternalFieldType.DECIMAL);
    }

    @Test
    void getByCode_shouldReturnTEXT_WhenInvalid() {
        ZLcExternalFieldType result = ZLcExternalFieldType.getByCode(99);
        assertThat(result).isEqualTo(ZLcExternalFieldType.TEXT);
    }

    @Test
    void getCode_shouldReturn1_WhenTEXT() {
        assertThat(ZLcExternalFieldType.TEXT.getCode()).isEqualTo(1);
    }

    @Test
    void getCode_shouldReturn2_WhenNUMBER() {
        assertThat(ZLcExternalFieldType.NUMBER.getCode()).isEqualTo(2);
    }

    @Test
    void getCode_shouldReturn3_WhenDATE() {
        assertThat(ZLcExternalFieldType.DATE.getCode()).isEqualTo(3);
    }

    @Test
    void getCode_shouldReturn4_WhenDECIMAL() {
        assertThat(ZLcExternalFieldType.DECIMAL.getCode()).isEqualTo(4);
    }

    @Test
    void getName_shouldReturnCorrectValue() {
        assertThat(ZLcExternalFieldType.TEXT.getName()).isEqualTo("文本");
        assertThat(ZLcExternalFieldType.NUMBER.getName()).isEqualTo("数值");
    }

    @Test
    void getFieldLength_shouldReturnCorrectValue() {
        assertThat(ZLcExternalFieldType.TEXT.getFieldLength()).isEqualTo("varchar(255)");
        assertThat(ZLcExternalFieldType.NUMBER.getFieldLength()).isEqualTo("bigint(20)");
    }
}
