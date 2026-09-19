package com.zifang.z.lc.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWebTaskType 单元测试
 *
 * @author zifang
 */
class ZLcWebTaskTypeTest {

    @Test
    void shouldHaveAllEnumValues() {
        assertThat(ZLcWebTaskType.values()).hasSize(5);
    }

    @Test
    void shouldHaveAppReleaseType() {
        assertThat(ZLcWebTaskType.APP_RELEASE.getType()).isEqualTo("appRelease");
    }

    @Test
    void shouldHaveAppReleaseDesc() {
        assertThat(ZLcWebTaskType.APP_RELEASE.getDesc()).isEqualTo("应用发布");
    }

    @Test
    void shouldHavePermissionConfigType() {
        assertThat(ZLcWebTaskType.PERMISSION_CONFIG.getType()).isEqualTo("permConfig");
    }

    @Test
    void shouldHavePermissionConfigDesc() {
        assertThat(ZLcWebTaskType.PERMISSION_CONFIG.getDesc()).isEqualTo("权限配置");
    }

    @Test
    void shouldHaveAceConfigType() {
        assertThat(ZLcWebTaskType.ACE_CONFIG.getType()).isEqualTo("aceConfig");
    }

    @Test
    void shouldHaveAceConfigDesc() {
        assertThat(ZLcWebTaskType.ACE_CONFIG.getDesc()).isEqualTo("简易搭配置");
    }

    @Test
    void shouldHaveDataInitType() {
        assertThat(ZLcWebTaskType.DATA_INIT.getType()).isEqualTo("dataInit");
    }

    @Test
    void shouldHaveDataInitDesc() {
        assertThat(ZLcWebTaskType.DATA_INIT.getDesc()).isEqualTo("数据初始化");
    }

    @Test
    void shouldHaveOtherType() {
        assertThat(ZLcWebTaskType.OTHER.getType()).isEqualTo("other");
    }

    @Test
    void shouldHaveOtherDesc() {
        assertThat(ZLcWebTaskType.OTHER.getDesc()).isEqualTo("其他");
    }

    @Test
    void shouldFromType() {
        assertThat(ZLcWebTaskType.fromType("appRelease")).isEqualTo(ZLcWebTaskType.APP_RELEASE);
        assertThat(ZLcWebTaskType.fromType("permConfig")).isEqualTo(ZLcWebTaskType.PERMISSION_CONFIG);
        assertThat(ZLcWebTaskType.fromType("aceConfig")).isEqualTo(ZLcWebTaskType.ACE_CONFIG);
        assertThat(ZLcWebTaskType.fromType("dataInit")).isEqualTo(ZLcWebTaskType.DATA_INIT);
        assertThat(ZLcWebTaskType.fromType("other")).isEqualTo(ZLcWebTaskType.OTHER);
    }

    @Test
    void shouldReturnNullForUnknownFromType() {
        assertThat(ZLcWebTaskType.fromType("unknown")).isNull();
    }

    @Test
    void shouldReturnNullForNullFromType() {
        assertThat(ZLcWebTaskType.fromType(null)).isNull();
    }

    @Test
    void shouldValueOf() {
        assertThat(ZLcWebTaskType.valueOf("APP_RELEASE")).isEqualTo(ZLcWebTaskType.APP_RELEASE);
        assertThat(ZLcWebTaskType.valueOf("PERMISSION_CONFIG")).isEqualTo(ZLcWebTaskType.PERMISSION_CONFIG);
        assertThat(ZLcWebTaskType.valueOf("ACE_CONFIG")).isEqualTo(ZLcWebTaskType.ACE_CONFIG);
        assertThat(ZLcWebTaskType.valueOf("DATA_INIT")).isEqualTo(ZLcWebTaskType.DATA_INIT);
        assertThat(ZLcWebTaskType.valueOf("OTHER")).isEqualTo(ZLcWebTaskType.OTHER);
    }
}
