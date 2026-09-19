package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RelationDTO 单元测试
 *
 * @author zifang
 */
class RelationDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RelationDTO dto = new RelationDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetRelationId() {
        RelationDTO dto = new RelationDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetRelationCode() {
        RelationDTO dto = new RelationDTO();
        dto.setRelationCode("user_org");
        assertThat(dto.getRelationCode()).isEqualTo("user_org");
    }

    @Test
    void shouldSetAndGetRelationName() {
        RelationDTO dto = new RelationDTO();
        dto.setRelationName("用户组织关系");
        assertThat(dto.getRelationName()).isEqualTo("用户组织关系");
    }

    @Test
    void shouldSetAndGetSourceEntityCode() {
        RelationDTO dto = new RelationDTO();
        dto.setSourceEntityCode("user");
        assertThat(dto.getSourceEntityCode()).isEqualTo("user");
    }

    @Test
    void shouldSetAndGetTargetEntityCode() {
        RelationDTO dto = new RelationDTO();
        dto.setTargetEntityCode("organization");
        assertThat(dto.getTargetEntityCode()).isEqualTo("organization");
    }

    @Test
    void shouldSetAndGetRelationType() {
        RelationDTO dto = new RelationDTO();
        dto.setRelationType("ONE_TO_MANY");
        assertThat(dto.getRelationType()).isEqualTo("ONE_TO_MANY");
    }

    @Test
    void shouldHandleNullValues() {
        RelationDTO dto = new RelationDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getRelationCode()).isNull();
        assertThat(dto.getRelationName()).isNull();
        assertThat(dto.getSourceEntityCode()).isNull();
        assertThat(dto.getTargetEntityCode()).isNull();
        assertThat(dto.getRelationType()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RelationDTO dto = new RelationDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
