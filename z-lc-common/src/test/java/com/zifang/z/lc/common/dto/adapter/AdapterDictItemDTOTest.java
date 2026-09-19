package com.zifang.z.lc.common.dto.adapter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AdapterDictItemDTO 单元测试
 *
 * @author zifang
 */
class AdapterDictItemDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSourceDictItemIds() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        List<Long> sourceIds = Arrays.asList(1L, 2L, 3L);
        dto.setSourceDictItemIds(sourceIds);
        assertThat(dto.getSourceDictItemIds()).isEqualTo(sourceIds);
    }

    @Test
    void shouldSetAndGetTargetDictTreeNodeId() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        dto.setTargetDictTreeNodeId("target-node-123");
        assertThat(dto.getTargetDictTreeNodeId()).isEqualTo("target-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        assertThat(dto.getSourceDictItemIds()).isNull();
        assertThat(dto.getTargetDictTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptySourceDictItemIds() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        dto.setSourceDictItemIds(Arrays.asList());
        assertThat(dto.getSourceDictItemIds()).isEmpty();
    }

    @Test
    void shouldSetNullSourceDictItemIds() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        dto.setSourceDictItemIds(null);
        assertThat(dto.getSourceDictItemIds()).isNull();
    }

    @Test
    void shouldSetEmptyTargetDictTreeNodeId() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        dto.setTargetDictTreeNodeId("");
        assertThat(dto.getTargetDictTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullTargetDictTreeNodeId() {
        AdapterDictItemDTO dto = new AdapterDictItemDTO();
        dto.setTargetDictTreeNodeId(null);
        assertThat(dto.getTargetDictTreeNodeId()).isNull();
    }
}