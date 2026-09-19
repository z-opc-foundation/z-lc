package com.zifang.z.lc.core.event;

import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.EventDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * EventService 服务接口契约测试
 */
public class EventServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(EventService.class.isInterface());
    }

    @Test
    public void shouldDeclareAppend() throws NoSuchMethodException {
        Method m = EventService.class.getMethod("append", String.class, EventAppendRequest.class);
        assertEquals(EventDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetLastEvent() throws NoSuchMethodException {
        Method m = EventService.class.getMethod("getLastEvent", String.class, String.class);
        assertEquals(EventDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListSince() throws NoSuchMethodException {
        Method m = EventService.class.getMethod("listSince", String.class, String.class, Long.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.event",
                EventService.class.getPackage().getName());
    }
}