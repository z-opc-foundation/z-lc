package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppUpdateReq 单元测试
 *
 * @author zifang
 */
class AppUpdateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AppUpdateReq req = new AppUpdateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        AppUpdateReq req = new AppUpdateReq();
        req.setId(123L);
        assertThat(req.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetAppName() {
        AppUpdateReq req = new AppUpdateReq();
        req.setAppName("测试应用");
        assertThat(req.getAppName()).isEqualTo("测试应用");
    }

    @Test
    void shouldSetAndGetDescription() {
        AppUpdateReq req = new AppUpdateReq();
        req.setDescription("应用描述");
        assertThat(req.getDescription()).isEqualTo("应用描述");
    }

    @Test
    void shouldSetAndGetIcon() {
        AppUpdateReq req = new AppUpdateReq();
        req.setIcon("icon-app");
        assertThat(req.getIcon()).isEqualTo("icon-app");
    }

    @Test
    void shouldHandleNullValues() {
        AppUpdateReq req = new AppUpdateReq();
        assertThat(req.getId()).isNull();
        assertThat(req.getAppName()).isNull();
        assertThat(req.getDescription()).isNull();
        assertThat(req.getIcon()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AppUpdateReq req = new AppUpdateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        AppUpdateReq req = new AppUpdateReq();
        req.setAppName("");
        req.setDescription("");
        req.setIcon("");
        
        assertThat(req.getAppName()).isEmpty();
        assertThat(req.getDescription()).isEmpty();
        assertThat(req.getIcon()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        AppUpdateReq req = new AppUpdateReq();
        req.setAppName(null);
        req.setDescription(null);
        req.setIcon(null);
        
        assertThat(req.getAppName()).isNull();
        assertThat(req.getDescription()).isNull();
        assertThat(req.getIcon()).isNull();
    }
}