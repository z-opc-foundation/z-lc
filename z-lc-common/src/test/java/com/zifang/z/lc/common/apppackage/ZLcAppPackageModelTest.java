package com.zifang.z.lc.common.apppackage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcAppPackageModel 单元测试
 *
 * @author zifang
 */
class ZLcAppPackageModelTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        assertThat(model).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        model.setId(123L);
        assertThat(model.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetPackageName() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        model.setPackageName("用户管理应用");
        assertThat(model.getPackageName()).isEqualTo("用户管理应用");
    }

    @Test
    void shouldSetAndGetPackageCode() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        model.setPackageCode("USER_MGMT");
        assertThat(model.getPackageCode()).isEqualTo("USER_MGMT");
    }

    @Test
    void shouldSetAndGetPackageDesc() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        model.setPackageDesc("用户管理应用包");
        assertThat(model.getPackageDesc()).isEqualTo("用户管理应用包");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcAppPackageModel model = new ZLcAppPackageModel();
        assertThat(model.getId()).isNull();
        assertThat(model.getPackageName()).isNull();
        assertThat(model.getPackageCode()).isNull();
        assertThat(model.getPackageDesc()).isNull();
    }
}
