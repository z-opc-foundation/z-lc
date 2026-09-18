package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExtensionServiceEnum 单元测试
 *
 * @author zifang
 */
class ZLcExtensionServiceEnumTest {

    @Test
    void fromCode_shouldReturnINIT_EXTENSION_WhenInit() {
        ZLcExtensionServiceEnum result = ZLcExtensionServiceEnum.fromCode("init");
        assertThat(result).isEqualTo(ZLcExtensionServiceEnum.INIT_EXTENSION);
    }

    @Test
    void fromCode_shouldReturnSUBMIT_VALIDATE_EXTENSION_WhenSubmitValidate() {
        ZLcExtensionServiceEnum result = ZLcExtensionServiceEnum.fromCode("submitValidate");
        assertThat(result).isEqualTo(ZLcExtensionServiceEnum.SUBMIT_VALIDATE_EXTENSION);
    }

    @Test
    void fromCode_shouldReturnQUERY_PRE_EXTENSION_WhenQueryPre() {
        ZLcExtensionServiceEnum result = ZLcExtensionServiceEnum.fromCode("queryPre");
        assertThat(result).isEqualTo(ZLcExtensionServiceEnum.QUERY_PRE_EXTENSION);
    }

    @Test
    void fromCode_shouldReturnNull_WhenUnknown() {
        ZLcExtensionServiceEnum result = ZLcExtensionServiceEnum.fromCode("unknown");
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcExtensionServiceEnum result = ZLcExtensionServiceEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcExtensionServiceEnum.INIT_EXTENSION.getCode()).isEqualTo("init");
        assertThat(ZLcExtensionServiceEnum.SUBMIT_VALIDATE_EXTENSION.getCode()).isEqualTo("submitValidate");
        assertThat(ZLcExtensionServiceEnum.QUERY_PRE_EXTENSION.getCode()).isEqualTo("queryPre");
    }

    @Test
    void getDescription_shouldReturnCorrectValue() {
        assertThat(ZLcExtensionServiceEnum.INIT_EXTENSION.getDescription()).isEqualTo("初始化扩展");
        assertThat(ZLcExtensionServiceEnum.SUBMIT_VALIDATE_EXTENSION.getDescription()).isEqualTo("提交前校验扩展");
    }

    @Test
    void shouldHaveAllExtensionPoints() {
        assertThat(ZLcExtensionServiceEnum.values().length).isEqualTo(16);
    }
}
