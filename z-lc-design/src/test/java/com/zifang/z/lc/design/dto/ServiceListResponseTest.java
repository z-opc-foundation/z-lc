package com.zifang.z.lc.design.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ServiceListResponse 单元测试
 *
 * @author zifang
 */
public class ServiceListResponseTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        ServiceListResponse r = new ServiceListResponse();
        assertNotNull(r);
        assertNotNull(r.getServices());
        assertEquals(0, r.getTotal());
        assertTrue(r.getServices().isEmpty());
    }

    @Test
    public void shouldCreateWithFullConstructor() {
        ServiceListResponse.ServiceEntry entry = new ServiceListResponse.ServiceEntry(
                "com.example.MyService", "default", "my-service", "My Service");
        ServiceListResponse r = new ServiceListResponse(1, Arrays.asList(entry));
        assertEquals(1, r.getTotal());
        assertEquals(1, r.getServices().size());
    }

    @Test
    public void shouldSetAndGetTotal() {
        ServiceListResponse r = new ServiceListResponse();
        r.setTotal(10);
        assertEquals(10, r.getTotal());
    }

    @Test
    public void shouldSetAndGetServices() {
        ServiceListResponse r = new ServiceListResponse();
        r.setServices(Arrays.asList(new ServiceListResponse.ServiceEntry()));
        assertEquals(1, r.getServices().size());
    }

    @Test
    public void serviceEntryShouldCreateWithDefaultConstructor() {
        ServiceListResponse.ServiceEntry entry = new ServiceListResponse.ServiceEntry();
        assertNotNull(entry);
    }

    @Test
    public void serviceEntryShouldCreateWithFullConstructor() {
        ServiceListResponse.ServiceEntry entry = new ServiceListResponse.ServiceEntry(
                "class", "group", "code", "name");
        assertEquals("class", entry.getClazz());
        assertEquals("group", entry.getGroup());
        assertEquals("code", entry.getCode());
        assertEquals("name", entry.getName());
    }

    @Test
    public void serviceEntryShouldSupportSetters() {
        ServiceListResponse.ServiceEntry entry = new ServiceListResponse.ServiceEntry();
        entry.setClazz("c");
        entry.setGroup("g");
        entry.setCode("cd");
        entry.setName("n");

        assertEquals("c", entry.getClazz());
        assertEquals("g", entry.getGroup());
        assertEquals("cd", entry.getCode());
        assertEquals("n", entry.getName());
    }
}