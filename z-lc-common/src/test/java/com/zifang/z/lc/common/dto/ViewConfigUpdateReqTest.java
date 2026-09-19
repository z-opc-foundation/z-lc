package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ViewConfigUpdateReq 单元测试
 *
 * @author zifang
 */
class ViewConfigUpdateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setId(123L);
        assertThat(req.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetViewType() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setViewType("LIST");
        assertThat(req.getViewType()).isEqualTo("LIST");
    }

    @Test
    void shouldSetAndGetConfig() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setConfig("{\"columns\":[\"id\",\"name\"]}");
        assertThat(req.getConfig()).isEqualTo("{\"columns\":[\"id\",\"name\"]}");
    }

    @Test
    void shouldHandleNullValues() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        assertThat(req.getId()).isNull();
        assertThat(req.getViewType()).isNull();
        assertThat(req.getConfig()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setViewType("");
        req.setConfig("");
        
        assertThat(req.getViewType()).isEmpty();
        assertThat(req.getConfig()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        ViewConfigUpdateReq req = new ViewConfigUpdateReq();
        req.setViewType(null);
        req.setConfig(null);
        
        assertThat(req.getViewType()).isNull();
        assertThat(req.getConfig()).isNull();
    }
}