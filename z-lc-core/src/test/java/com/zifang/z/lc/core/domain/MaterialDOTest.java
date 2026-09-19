package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterialDO 单元测试
 *
 * @author zifang
 */
public class MaterialDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        MaterialDO entity = new MaterialDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        MaterialDO entity = new MaterialDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        MaterialDO entity = new MaterialDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        MaterialDO entity = new MaterialDO();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetBelongType() {
        MaterialDO entity = new MaterialDO();
        entity.setBelongType("public");
        assertEquals("public", entity.getBelongType());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        MaterialDO entity = new MaterialDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetName() {
        MaterialDO entity = new MaterialDO();
        entity.setName("logo.png");
        assertEquals("logo.png", entity.getName());
    }

    @Test
    public void shouldSetAndGetDescription() {
        MaterialDO entity = new MaterialDO();
        entity.setDescription("Site logo");
        assertEquals("Site logo", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetOssUrl() {
        MaterialDO entity = new MaterialDO();
        entity.setOssUrl("https://oss.example.com/logo.png");
        assertEquals("https://oss.example.com/logo.png", entity.getOssUrl());
    }

    @Test
    public void shouldHandleNullValues() {
        MaterialDO entity = new MaterialDO();
        assertNull(entity.getTenantCode());
        assertNull(entity.getBelongType());
        assertNull(entity.getName());
        assertNull(entity.getOssUrl());
    }
}