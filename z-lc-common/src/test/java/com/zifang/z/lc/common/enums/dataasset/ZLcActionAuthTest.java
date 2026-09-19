package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcActionAuth 单元测试
 *
 * @author zifang
 */
class ZLcActionAuthTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcActionAuth.values()).hasSize(2);
    }

    @Test
    void shouldHaveDecryptActionType() {
        assertThat(ZLcActionAuth.DECRYPT.getActionType()).isEqualTo("decrypt");
    }

    @Test
    void shouldHaveDecryptDescription() {
        assertThat(ZLcActionAuth.DECRYPT.getDescription()).isEqualTo("解密");
    }

    @Test
    void shouldHaveDesensitizeActionType() {
        assertThat(ZLcActionAuth.DESENSITIZE.getActionType()).isEqualTo("desensitize");
    }

    @Test
    void shouldHaveDesensitizeDescription() {
        assertThat(ZLcActionAuth.DESENSITIZE.getDescription()).isEqualTo("脱敏");
    }

    @Test
    void shouldFromActionType() {
        assertThat(ZLcActionAuth.fromActionType("decrypt")).isEqualTo(ZLcActionAuth.DECRYPT);
        assertThat(ZLcActionAuth.fromActionType("desensitize")).isEqualTo(ZLcActionAuth.DESENSITIZE);
    }

    @Test
    void shouldReturnNullForUnknownFromActionType() {
        assertThat(ZLcActionAuth.fromActionType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromActionType() {
        assertThat(ZLcActionAuth.fromActionType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcActionAuth.valueOf("DECRYPT")).isEqualTo(ZLcActionAuth.DECRYPT);
        assertThat(ZLcActionAuth.valueOf("DESENSITIZE")).isEqualTo(ZLcActionAuth.DESENSITIZE);
    }
}
