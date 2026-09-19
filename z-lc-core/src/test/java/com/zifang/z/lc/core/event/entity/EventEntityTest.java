package com.zifang.z.lc.core.event.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * EventEntity 单元测试
 *
 * @author zifang
 */
public class EventEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        EventEntity entity = new EventEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        EventEntity entity = new EventEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        EventEntity entity = new EventEntity();
        entity.setId(1L);
        assertEquals(Long.valueOf(1L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        EventEntity entity = new EventEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetEventId() {
        EventEntity entity = new EventEntity();
        entity.setEventId("evt-001");
        assertEquals("evt-001", entity.getEventId());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        EventEntity entity = new EventEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        EventEntity entity = new EventEntity();
        entity.setEntityCode("user");
        assertEquals("user", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetEventType() {
        EventEntity entity = new EventEntity();
        entity.setEventType("CREATE");
        assertEquals("CREATE", entity.getEventType());
    }

    @Test
    public void shouldSetAndGetEventData() {
        EventEntity entity = new EventEntity();
        entity.setEventData("{\"id\":1}");
        assertEquals("{\"id\":1}", entity.getEventData());
    }

    @Test
    public void shouldSetAndGetSource() {
        EventEntity entity = new EventEntity();
        entity.setSource("USER");
        assertEquals("USER", entity.getSource());
    }

    @Test
    public void shouldSetAndGetParentEventId() {
        EventEntity entity = new EventEntity();
        entity.setParentEventId("parent-001");
        assertEquals("parent-001", entity.getParentEventId());
    }

    @Test
    public void shouldSetAndGetApplySeq() {
        EventEntity entity = new EventEntity();
        entity.setApplySeq(100L);
        assertEquals(Long.valueOf(100L), entity.getApplySeq());
    }

    @Test
    public void shouldSetAndGetApplyTime() {
        EventEntity entity = new EventEntity();
        Date now = new Date();
        entity.setApplyTime(now);
        assertEquals(now, entity.getApplyTime());
    }

    @Test
    public void shouldHandleNullValues() {
        EventEntity entity = new EventEntity();
        assertNull(entity.getId());
        assertNull(entity.getEventId());
        assertNull(entity.getEventData());
        assertNull(entity.getApplyTime());
    }
}