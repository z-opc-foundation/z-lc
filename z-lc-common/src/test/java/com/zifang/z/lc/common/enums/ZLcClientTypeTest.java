package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcClientType 单元测试
 *
 * @author zifang
 */
class ZLcClientTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcClientType.values()).hasSize(2);
    }

    @Test
    void shouldHavePcType() {
        assertThat(ZLcClientType.PC.getType()).isEqualTo("pc");
    }

    @Test
    void shouldHavePcName() {
        assertThat(ZLcClientType.PC.getName()).isEqualTo("电脑端");
    }

    @Test
    void shouldHaveMobileType() {
        assertThat(ZLcClientType.MOBILE.getType()).isEqualTo("mobile");
    }

    @Test
    void shouldHaveMobileName() {
        assertThat(ZLcClientType.MOBILE.getName()).isEqualTo("移动端");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcClientType.fromType("pc")).isEqualTo(ZLcClientType.PC);
        assertThat(ZLcClientType.fromType("mobile")).isEqualTo(ZLcClientType.MOBILE);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcClientType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcClientType.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcClientType.valueOf("PC")).isEqualTo(ZLcClientType.PC);
        assertThat(ZLcClientType.valueOf("MOBILE")).isEqualTo(ZLcClientType.MOBILE);
    }
}
