package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcFooterInfoEnum 单元测试
 *
 * @author zifang
 */
class ZLcFooterInfoEnumTest {

    @Test
    void fromCode_shouldReturnORG_NAME_When1() {
        ZLcFooterInfoEnum result = ZLcFooterInfoEnum.fromCode(1);
        assertThat(result).isEqualTo(ZLcFooterInfoEnum.ORG_NAME);
    }

    @Test
    void fromCode_shouldReturnACCOUNT_NAME_When2() {
        ZLcFooterInfoEnum result = ZLcFooterInfoEnum.fromCode(2);
        assertThat(result).isEqualTo(ZLcFooterInfoEnum.ACCOUNT_NAME);
    }

    @Test
    void fromCode_shouldReturnUSER_NAME_When3() {
        ZLcFooterInfoEnum result = ZLcFooterInfoEnum.fromCode(3);
        assertThat(result).isEqualTo(ZLcFooterInfoEnum.USER_NAME);
    }

    @Test
    void fromCode_shouldReturnNull_WhenInvalid() {
        ZLcFooterInfoEnum result = ZLcFooterInfoEnum.fromCode(99);
        assertThat(result).isNull();
    }

    @Test
    void fromCode_shouldReturnNull_WhenNull() {
        ZLcFooterInfoEnum result = ZLcFooterInfoEnum.fromCode(null);
        assertThat(result).isNull();
    }

    @Test
    void getCode_shouldReturnCorrectValue() {
        assertThat(ZLcFooterInfoEnum.ORG_NAME.getCode()).isEqualTo(1);
        assertThat(ZLcFooterInfoEnum.ACCOUNT_NAME.getCode()).isEqualTo(2);
        assertThat(ZLcFooterInfoEnum.USER_NAME.getCode()).isEqualTo(3);
    }

    @Test
    void getValue_shouldReturnCorrectValue() {
        assertThat(ZLcFooterInfoEnum.ORG_NAME.getValue()).isEqualTo("组织名称");
        assertThat(ZLcFooterInfoEnum.ACCOUNT_NAME.getValue()).isEqualTo("账套名称");
        assertThat(ZLcFooterInfoEnum.USER_NAME.getValue()).isEqualTo("登录用户");
    }
}
