package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ServiceItemDO 单元测试
 *
 * @author zifang
 */
public class ServiceItemDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        ServiceItemDO entity = new ServiceItemDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        ServiceItemDO entity = new ServiceItemDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        ServiceItemDO entity = new ServiceItemDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetServiceName() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceName("Weather API");
        assertEquals("Weather API", entity.getServiceName());
    }

    @Test
    public void shouldSetAndGetServiceCode() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceCode("weather-api");
        assertEquals("weather-api", entity.getServiceCode());
    }

    @Test
    public void shouldSetAndGetServiceDesc() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceDesc("Get weather data");
        assertEquals("Get weather data", entity.getServiceDesc());
    }

    @Test
    public void shouldSetAndGetServiceType() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("HTTP");
        assertEquals("HTTP", entity.getServiceType());
    }

    @Test
    public void shouldSetAndGetHttpInterfaceConfig() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setHttpInterfaceConfig("{\"url\":\"https://api.example.com\"}");
        assertEquals("{\"url\":\"https://api.example.com\"}", entity.getHttpInterfaceConfig());
    }

    @Test
    public void shouldSetAndGetOpenPlatformInterfaceConfig() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setOpenPlatformInterfaceConfig("{\"appKey\":\"key\"}");
        assertEquals("{\"appKey\":\"key\"}", entity.getOpenPlatformInterfaceConfig());
    }

    @Test
    public void shouldSetAndGetRequestParams() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setRequestParams("[{\"name\":\"city\"}]");
        assertEquals("[{\"name\":\"city\"}]", entity.getRequestParams());
    }

    @Test
    public void shouldSetAndGetResponseSchema() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setResponseSchema("{\"type\":\"object\"}");
        assertEquals("{\"type\":\"object\"}", entity.getResponseSchema());
    }

    @Test
    public void shouldSetAndGetTreeNodeCode() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setTreeNodeCode("root");
        assertEquals("root", entity.getTreeNodeCode());
    }

    @Test
    public void shouldReturnFalseForHttpWhenNull() {
        ServiceItemDO entity = new ServiceItemDO();
        assertFalse(entity.isHttp());
    }

    @Test
    public void shouldReturnTrueForHttp() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("HTTP");
        assertTrue(entity.isHttp());
    }

    @Test
    public void shouldReturnTrueForHttpCaseInsensitive() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("http");
        assertTrue(entity.isHttp());
    }

    @Test
    public void shouldReturnTrueForOpenPlatform() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("OPEN_PLATFORM");
        assertTrue(entity.isOpenPlatform());
    }

    @Test
    public void shouldReturnTrueForInner() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("INNER");
        assertTrue(entity.isInner());
    }

    @Test
    public void shouldReturnFalseForHttpWhenOtherType() {
        ServiceItemDO entity = new ServiceItemDO();
        entity.setServiceType("OPEN_PLATFORM");
        assertFalse(entity.isHttp());
    }

    @Test
    public void shouldHandleNullValues() {
        ServiceItemDO entity = new ServiceItemDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getServiceCode());
        assertNull(entity.getServiceType());
    }
}