package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventAppendRequest 单元测试
 *
 * @author zifang
 */
class EventAppendRequestTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        EventAppendRequest request = new EventAppendRequest();
        assertThat(request).isNotNull();
    }

    @Test
    void shouldSetAndGetTenantCode() {
        EventAppendRequest request = new EventAppendRequest();
        request.setTenantCode("tenant-001");
        assertThat(request.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldSetAndGetEntityCode() {
        EventAppendRequest request = new EventAppendRequest();
        request.setEntityCode("entity-001");
        assertThat(request.getEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetEventType() {
        EventAppendRequest request = new EventAppendRequest();
        request.setEventType("CREATE");
        assertThat(request.getEventType()).isEqualTo("CREATE");
    }

    @Test
    void shouldSetAndGetEventData() {
        EventAppendRequest request = new EventAppendRequest();
        request.setEventData("{\"key\":\"value\"}");
        assertThat(request.getEventData()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void shouldSetAndGetSource() {
        EventAppendRequest request = new EventAppendRequest();
        request.setSource("USER");
        assertThat(request.getSource()).isEqualTo("USER");
    }

    @Test
    void shouldSetAndGetParentEventId() {
        EventAppendRequest request = new EventAppendRequest();
        request.setParentEventId("event-123");
        assertThat(request.getParentEventId()).isEqualTo("event-123");
    }

    @Test
    void shouldHandleNullValues() {
        EventAppendRequest request = new EventAppendRequest();
        assertThat(request.getTenantCode()).isNull();
        assertThat(request.getEntityCode()).isNull();
        assertThat(request.getEventType()).isNull();
        assertThat(request.getEventData()).isNull();
        assertThat(request.getSource()).isNull();
        assertThat(request.getParentEventId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        EventAppendRequest request = new EventAppendRequest();
        assertThat(request).isInstanceOf(java.io.Serializable.class);
    }
}
