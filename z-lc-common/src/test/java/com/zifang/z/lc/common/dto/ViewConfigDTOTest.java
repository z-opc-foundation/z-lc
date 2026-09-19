package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ViewConfigDTO 单元测试
 *
 * @author zifang
 */
class ViewConfigDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ViewConfigDTO dto = new ViewConfigDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetViewId() {
        ViewConfigDTO dto = new ViewConfigDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetViewType() {
        ViewConfigDTO dto = new ViewConfigDTO();
        dto.setViewType("list");
        assertThat(dto.getViewType()).isEqualTo("list");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        ViewConfigDTO dto = new ViewConfigDTO();
        dto.setEntityCode("user");
        assertThat(dto.getEntityCode()).isEqualTo("user");
    }

    @Test
    void shouldSetAndGetConfig() {
        ViewConfigDTO dto = new ViewConfigDTO();
        dto.setConfig("{\"columns\":[\"name\",\"age\"]}");
        assertThat(dto.getConfig()).isEqualTo("{\"columns\":[\"name\",\"age\"]}");
    }

    @Test
    void shouldHandleNullValues() {
        ViewConfigDTO dto = new ViewConfigDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getViewType()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getConfig()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ViewConfigDTO dto = new ViewConfigDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
