package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppCreateReq 单元测试
 *
 * @author zifang
 */
class AppCreateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AppCreateReq req = new AppCreateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetTenantCode() {
        AppCreateReq req = new AppCreateReq();
        req.setTenantCode("tenant-001");
        assertThat(req.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        AppCreateReq req = new AppCreateReq();
        req.setAppCode("test_app");
        assertThat(req.getAppCode()).isEqualTo("test_app");
    }

    @Test
    void shouldSetAndGetAppName() {
        AppCreateReq req = new AppCreateReq();
        req.setAppName("测试应用");
        assertThat(req.getAppName()).isEqualTo("测试应用");
    }

    @Test
    void shouldSetAndGetDescription() {
        AppCreateReq req = new AppCreateReq();
        req.setDescription("应用描述");
        assertThat(req.getDescription()).isEqualTo("应用描述");
    }

    @Test
    void shouldSetAndGetIcon() {
        AppCreateReq req = new AppCreateReq();
        req.setIcon("icon.png");
        assertThat(req.getIcon()).isEqualTo("icon.png");
    }

    @Test
    void shouldHandleNullValues() {
        AppCreateReq req = new AppCreateReq();
        assertThat(req.getTenantCode()).isNull();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getAppName()).isNull();
        assertThat(req.getDescription()).isNull();
        assertThat(req.getIcon()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AppCreateReq req = new AppCreateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }
}
