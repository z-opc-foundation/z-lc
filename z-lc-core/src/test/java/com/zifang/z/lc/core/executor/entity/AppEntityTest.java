package com.zifang.z.lc.core.executor.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AppEntity 单元测试
 *
 * @author zifang
 */
public class AppEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        AppEntity entity = new AppEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        AppEntity entity = new AppEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        AppEntity entity = new AppEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        AppEntity entity = new AppEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        AppEntity entity = new AppEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetAppName() {
        AppEntity entity = new AppEntity();
        entity.setAppName("MyApp");
        assertEquals("MyApp", entity.getAppName());
    }

    @Test
    public void shouldSetAndGetDescription() {
        AppEntity entity = new AppEntity();
        entity.setDescription("Application description");
        assertEquals("Application description", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetIcon() {
        AppEntity entity = new AppEntity();
        entity.setIcon("app-icon.png");
        assertEquals("app-icon.png", entity.getIcon());
    }

    @Test
    public void shouldSetAndGetStatus() {
        AppEntity entity = new AppEntity();
        entity.setStatus("ACTIVE");
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    public void shouldSetAndGetCurrentVersion() {
        AppEntity entity = new AppEntity();
        entity.setCurrentVersion(1L);
        assertEquals(Long.valueOf(1L), entity.getCurrentVersion());
    }

    @Test
    public void shouldSetAndGetTimes() {
        AppEntity entity = new AppEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        AppEntity entity = new AppEntity();
        assertNull(entity.getId());
        assertNull(entity.getAppCode());
        assertNull(entity.getAppName());
        assertNull(entity.getCreateTime());
    }
}