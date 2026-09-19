package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ViewConfigCreateReq 单元测试
 *
 * @author zifang
 */
class ViewConfigCreateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetEntityCode() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setEntityCode("entity-001");
        assertThat(req.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetAppCode() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setAppCode("app-001");
        assertThat(req.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetViewType() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setViewType("LIST");
        assertThat(req.getViewType()).isEqualTo("LIST");
    }

    @Test
    void shouldSetAndGetConfig() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setConfig("{\"columns\":[\"id\",\"name\"]}");
        assertThat(req.getConfig()).isEqualTo("{\"columns\":[\"id\",\"name\"]}");
    }

    @Test
    void shouldSetAndGetTenantCode() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setTenantCode("tenant-001");
        assertThat(req.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldHandleNullValues() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        assertThat(req.getEntityCode()).isNull();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getViewType()).isNull();
        assertThat(req.getConfig()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setEntityCode("");
        req.setAppCode("");
        req.setViewType("");
        req.setConfig("");
        req.setTenantCode("");
        
        assertThat(req.getEntityCode()).isEmpty();
        assertThat(req.getAppCode()).isEmpty();
        assertThat(req.getViewType()).isEmpty();
        assertThat(req.getConfig()).isEmpty();
        assertThat(req.getTenantCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ViewConfigCreateReq req = new ViewConfigCreateReq();
        req.setEntityCode(null);
        req.setAppCode(null);
        req.setViewType(null);
        req.setConfig(null);
        req.setTenantCode(null);
        
        assertThat(req.getEntityCode()).isNull();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getViewType()).isNull();
        assertThat(req.getConfig()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }
}