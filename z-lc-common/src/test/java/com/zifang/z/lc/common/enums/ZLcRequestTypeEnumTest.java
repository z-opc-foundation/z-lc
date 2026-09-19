package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcRequestTypeEnum 单元测试
 *
 * @author zifang
 */
class ZLcRequestTypeEnumTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcRequestTypeEnum.values()).hasSize(2);
    }

    @Test
    void shouldHaveHttpType() {
        assertThat(ZLcRequestTypeEnum.HTTP.getType()).isEqualTo(1);
    }

    @Test
    void shouldHaveHttpName() {
        assertThat(ZLcRequestTypeEnum.HTTP.getName()).isEqualTo("http");
    }

    @Test
    void shouldHaveRpcType() {
        assertThat(ZLcRequestTypeEnum.RPC.getType()).isEqualTo(2);
    }

    @Test
    void shouldHaveRpcName() {
        assertThat(ZLcRequestTypeEnum.RPC.getName()).isEqualTo("rpc");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcRequestTypeEnum.fromType(1)).isEqualTo(ZLcRequestTypeEnum.HTTP);
        assertThat(ZLcRequestTypeEnum.fromType(2)).isEqualTo(ZLcRequestTypeEnum.RPC);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcRequestTypeEnum.fromType(999)).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcRequestTypeEnum.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcRequestTypeEnum.valueOf("HTTP")).isEqualTo(ZLcRequestTypeEnum.HTTP);
        assertThat(ZLcRequestTypeEnum.valueOf("RPC")).isEqualTo(ZLcRequestTypeEnum.RPC);
    }
}
