package com.zifang.z.lc.core.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * EventConflictException 单元测试
 *
 * @author zifang
 */
public class EventConflictExceptionTest {

    @Test
    public void shouldCreateWithMessage() {
        EventConflictException ex = new EventConflictException("expected-001", "actual-002");
        assertNotNull(ex);
        assertEquals("expected-001", ex.getExpectedParentEventId());
        assertEquals("actual-002", ex.getActualLastEventId());
    }

    @Test
    public void shouldExtendRuntimeException() {
        EventConflictException ex = new EventConflictException("expected-001", "actual-002");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    public void shouldGenerateDescriptiveMessage() {
        EventConflictException ex = new EventConflictException("expected-001", "actual-002");
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue("Message should mention expected", msg.contains("expected-001"));
        assertTrue("Message should mention actual", msg.contains("actual-002"));
    }

    @Test
    public void shouldImplementSerializable() {
        EventConflictException ex = new EventConflictException("e", "a");
        assertTrue(ex instanceof java.io.Serializable);
    }

    @Test
    public void shouldBeThrowable() {
        EventConflictException ex = new EventConflictException("e", "a");
        try {
            throw ex;
        } catch (EventConflictException caught) {
            assertEquals("e", caught.getExpectedParentEventId());
            assertEquals("a", caught.getActualLastEventId());
        }
    }
}