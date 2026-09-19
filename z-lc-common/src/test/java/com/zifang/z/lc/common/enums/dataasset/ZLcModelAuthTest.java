package com.zifang.z.lc.common.enums.dataasset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcModelAuth 单元测试
 *
 * @author zifang
 */
class ZLcModelAuthTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcModelAuth.values()).hasSize(1);
    }

    @Test
    void shouldHaveDataAssetModelAuthPositionCode() {
        assertThat(ZLcModelAuth.DATA_ASSET_MODEL_AUTH_POSITION.getCode()).isEqualTo("data_asset_model_auth_position");
    }

    @Test
    void shouldHaveDataAssetModelAuthPositionMessage() {
        assertThat(ZLcModelAuth.DATA_ASSET_MODEL_AUTH_POSITION.getMessage()).isEqualTo("模型权限管理岗位");
    }

    @Test
    void shouldFromCode() {
        assertThat(ZLcModelAuth.fromCode("data_asset_model_auth_position")).isEqualTo(ZLcModelAuth.DATA_ASSET_MODEL_AUTH_POSITION);
    }

    @Test
    void shouldReturnNullForUnknownFromCode() {
        assertThat(ZLcModelAuth.fromCode("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromCode() {
        assertThat(ZLcModelAuth.fromCode(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcModelAuth.valueOf("DATA_ASSET_MODEL_AUTH_POSITION")).isEqualTo(ZLcModelAuth.DATA_ASSET_MODEL_AUTH_POSITION);
    }
}
