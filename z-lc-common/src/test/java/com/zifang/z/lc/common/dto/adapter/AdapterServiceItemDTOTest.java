package com.zifang.z.lc.common.dto.adapter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AdapterServiceItemDTO 单元测试
 *
 * @author zifang
 */
class AdapterServiceItemDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetSourceItemIds() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        List<Long> sourceIds = Arrays.asList(1L, 2L, 3L);
        dto.setSourceItemIds(sourceIds);
        assertThat(dto.getSourceItemIds()).isEqualTo(sourceIds);
    }

    @Test
    void shouldSetAndGetTargetTreeNodeId() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        dto.setTargetTreeNodeId("target-node-123");
        assertThat(dto.getTargetTreeNodeId()).isEqualTo("target-node-123");
    }

    @Test
    void shouldHandleNullValues() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        assertThat(dto.getSourceItemIds()).isNull();
        assertThat(dto.getTargetTreeNodeId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptySourceItemIds() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        dto.setSourceItemIds(Arrays.asList());
        assertThat(dto.getSourceItemIds()).isEmpty();
    }

    @Test
    void shouldSetNullSourceItemIds() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        dto.setSourceItemIds(null);
        assertThat(dto.getSourceItemIds()).isNull();
    }

    @Test
    void shouldSetEmptyTargetTreeNodeId() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        dto.setTargetTreeNodeId("");
        assertThat(dto.getTargetTreeNodeId()).isEmpty();
    }

    @Test
    void shouldSetNullTargetTreeNodeId() {
        AdapterServiceItemDTO dto = new AdapterServiceItemDTO();
        dto.setTargetTreeNodeId(null);
        assertThat(dto.getTargetTreeNodeId()).isNull();
    }
}