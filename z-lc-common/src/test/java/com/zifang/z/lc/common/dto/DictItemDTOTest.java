package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DictItemDTO 单元测试
 *
 * @author zifang
 */
class DictItemDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DictItemDTO dto = new DictItemDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetItemId() {
        DictItemDTO dto = new DictItemDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetItemCode() {
        DictItemDTO dto = new DictItemDTO();
        dto.setItemCode("code1");
        assertThat(dto.getItemCode()).isEqualTo("code1");
    }

    @Test
    void shouldSetAndGetItemLabel() {
        DictItemDTO dto = new DictItemDTO();
        dto.setItemLabel("选项1");
        assertThat(dto.getItemLabel()).isEqualTo("选项1");
    }

    @Test
    void shouldSetAndGetItemValue() {
        DictItemDTO dto = new DictItemDTO();
        dto.setItemValue("value1");
        assertThat(dto.getItemValue()).isEqualTo("value1");
    }

    @Test
    void shouldSetAndGetSortOrder() {
        DictItemDTO dto = new DictItemDTO();
        dto.setSortOrder(10);
        assertThat(dto.getSortOrder()).isEqualTo(10);
    }

    @Test
    void shouldHandleNullValues() {
        DictItemDTO dto = new DictItemDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getItemCode()).isNull();
        assertThat(dto.getItemLabel()).isNull();
        assertThat(dto.getItemValue()).isNull();
        assertThat(dto.getSortOrder()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DictItemDTO dto = new DictItemDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
