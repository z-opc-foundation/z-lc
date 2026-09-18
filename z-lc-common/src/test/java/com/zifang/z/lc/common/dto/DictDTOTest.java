package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DictDTO 单元测试
 *
 * @author zifang
 */
class DictDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DictDTO dto = new DictDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetDictId() {
        DictDTO dto = new DictDTO();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetDictName() {
        DictDTO dto = new DictDTO();
        dto.setDictName("测试字典");
        assertThat(dto.getDictName()).isEqualTo("测试字典");
    }

    @Test
    void shouldSetAndGetDictCode() {
        DictDTO dto = new DictDTO();
        dto.setDictCode("test_dict");
        assertThat(dto.getDictCode()).isEqualTo("test_dict");
    }

    @Test
    void shouldSetAndGetDescription() {
        DictDTO dto = new DictDTO();
        dto.setDescription("字典描述");
        assertThat(dto.getDescription()).isEqualTo("字典描述");
    }

    @Test
    void shouldSetAndGetStatus() {
        DictDTO dto = new DictDTO();
        dto.setStatus("active");
        assertThat(dto.getStatus()).isEqualTo("active");
    }

    @Test
    void shouldSetAndGetItems() {
        DictDTO dto = new DictDTO();
        List<DictItemDTO> items = Arrays.asList(new DictItemDTO());
        dto.setItems(items);
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    void shouldHandleNullValues() {
        DictDTO dto = new DictDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getDictName()).isNull();
        assertThat(dto.getDictCode()).isNull();
        assertThat(dto.getItems()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        DictDTO dto = new DictDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }
}
