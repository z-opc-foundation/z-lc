package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntityDefDTO 单元测试
 *
 * @author zifang
 */
class EntityDefDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        EntityDefDTO dto = new EntityDefDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetEntityId() {
        EntityDefDTO dto = new EntityDefDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetEntityCode() {
        EntityDefDTO dto = new EntityDefDTO();
        dto.setEntityCode("user");
        assertThat(dto.getEntityCode()).isEqualTo("user");
    }

    @Test
    void shouldSetAndGetEntityName() {
        EntityDefDTO dto = new EntityDefDTO();
        dto.setEntityName("用户");
        assertThat(dto.getEntityName()).isEqualTo("用户");
    }

    @Test
    void shouldSetAndGetAppCode() {
        EntityDefDTO dto = new EntityDefDTO();
        dto.setAppCode("test_app");
        assertThat(dto.getAppCode()).isEqualTo("test_app");
    }

    @Test
    void shouldHandleNullValues() {
        EntityDefDTO dto = new EntityDefDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getEntityName()).isNull();
        assertThat(dto.getAppCode()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        EntityDefDTO dto = new EntityDefDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
