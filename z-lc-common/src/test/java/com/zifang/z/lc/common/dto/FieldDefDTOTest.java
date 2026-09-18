package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FieldDefDTO 单元测试
 *
 * @author zifang
 */
class FieldDefDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        FieldDefDTO dto = new FieldDefDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetFieldId() {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetFieldCode() {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setFieldCode("user_name");
        assertThat(dto.getFieldCode()).isEqualTo("user_name");
    }

    @Test
    void shouldSetAndGetFieldName() {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setFieldName("用户名");
        assertThat(dto.getFieldName()).isEqualTo("用户名");
    }

    @Test
    void shouldSetAndGetFieldType() {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setFieldType("Text");
        assertThat(dto.getFieldType()).isEqualTo("Text");
    }

    @Test
    void shouldHandleNullValues() {
        FieldDefDTO dto = new FieldDefDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getFieldCode()).isNull();
        assertThat(dto.getFieldName()).isNull();
        assertThat(dto.getFieldType()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        FieldDefDTO dto = new FieldDefDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
