package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventDTO 单元测试
 *
 * @author zifang
 */
class EventDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        EventDTO dto = new EventDTO();
        assertThat(dto).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        EventDTO dto = new EventDTO();
        dto.setId(123L);
        assertThat(dto.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetTenantCode() {
        EventDTO dto = new EventDTO();
        dto.setTenantCode("tenant-001");
        assertThat(dto.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetEventId() {
        EventDTO dto = new EventDTO();
        dto.setEventId("event-uuid-123");
        assertThat(dto.getEventId()).isEqualTo("event-uuid-123");
    }

    @Test
    void shouldSetAndGetAppCode() {
        EventDTO dto = new EventDTO();
        dto.setAppCode("app-001");
        assertThat(dto.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        EventDTO dto = new EventDTO();
        dto.setEntityCode("entity-001");
        assertThat(dto.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetEventType() {
        EventDTO dto = new EventDTO();
        dto.setEventType("CREATE");
        assertThat(dto.getEventType()).isEqualTo("CREATE");
    }

    @Test
    void shouldSetAndGetEventData() {
        EventDTO dto = new EventDTO();
        dto.setEventData("{\"key\":\"value\"}");
        assertThat(dto.getEventData()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void shouldSetAndGetSource() {
        EventDTO dto = new EventDTO();
        dto.setSource("USER");
        assertThat(dto.getSource()).isEqualTo("USER");
    }

    @Test
    void shouldSetAndGetParentEventId() {
        EventDTO dto = new EventDTO();
        dto.setParentEventId("parent-event-123");
        assertThat(dto.getParentEventId()).isEqualTo("parent-event-123");
    }

    @Test
    void shouldSetAndGetApplySeq() {
        EventDTO dto = new EventDTO();
        dto.setApplySeq(1001L);
        assertThat(dto.getApplySeq()).isEqualTo(1001L);
    }

    @Test
    void shouldSetAndGetApplyTime() {
        EventDTO dto = new EventDTO();
        Date now = new Date();
        dto.setApplyTime(now);
        assertThat(dto.getApplyTime()).isEqualTo(now);
    }

    @Test
    void shouldHandleNullValues() {
        EventDTO dto = new EventDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getEventId()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getEventType()).isNull();
        assertThat(dto.getEventData()).isNull();
        assertThat(dto.getSource()).isNull();
        assertThat(dto.getParentEventId()).isNull();
        assertThat(dto.getApplySeq()).isNull();
        assertThat(dto.getApplyTime()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        EventDTO dto = new EventDTO();
        assertThat(dto).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        EventDTO dto = new EventDTO();
        dto.setTenantCode("");
        dto.setEventId("");
        dto.setAppCode("");
        dto.setEntityCode("");
        dto.setEventType("");
        dto.setEventData("");
        dto.setSource("");
        dto.setParentEventId("");
        
        assertThat(dto.getTenantCode()).isEmpty();
        assertThat(dto.getEventId()).isEmpty();
        assertThat(dto.getAppCode()).isEmpty();
        assertThat(dto.getEntityCode()).isEmpty();
        assertThat(dto.getEventType()).isEmpty();
        assertThat(dto.getEventData()).isEmpty();
        assertThat(dto.getSource()).isEmpty();
        assertThat(dto.getParentEventId()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        EventDTO dto = new EventDTO();
        dto.setTenantCode(null);
        dto.setEventId(null);
        dto.setAppCode(null);
        dto.setEntityCode(null);
        dto.setEventType(null);
        dto.setEventData(null);
        dto.setSource(null);
        dto.setParentEventId(null);
        
        assertThat(dto.getTenantCode()).isNull();
        assertThat(dto.getEventId()).isNull();
        assertThat(dto.getAppCode()).isNull();
        assertThat(dto.getEntityCode()).isNull();
        assertThat(dto.getEventType()).isNull();
        assertThat(dto.getEventData()).isNull();
        assertThat(dto.getSource()).isNull();
        assertThat(dto.getParentEventId()).isNull();
    }
}