package com.zifang.z.lc.core.executor.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * EntityEntity 单元测试
 *
 * @author zifang
 */
public class EntityEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        EntityEntity entity = new EntityEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        EntityEntity entity = new EntityEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        EntityEntity entity = new EntityEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        EntityEntity entity = new EntityEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        EntityEntity entity = new EntityEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        EntityEntity entity = new EntityEntity();
        entity.setEntityCode("user");
        assertEquals("user", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetEntityName() {
        EntityEntity entity = new EntityEntity();
        entity.setEntityName("用户");
        assertEquals("用户", entity.getEntityName());
    }

    @Test
    public void shouldSetAndGetTableName() {
        EntityEntity entity = new EntityEntity();
        entity.setTableName("z_lc_user");
        assertEquals("z_lc_user", entity.getTableName());
    }

    @Test
    public void shouldSetAndGetDescription() {
        EntityEntity entity = new EntityEntity();
        entity.setDescription("实体描述");
        assertEquals("实体描述", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetCurrentVersion() {
        EntityEntity entity = new EntityEntity();
        entity.setCurrentVersion(1L);
        assertEquals(Long.valueOf(1L), entity.getCurrentVersion());
    }

    @Test
    public void shouldSetAndGetTimes() {
        EntityEntity entity = new EntityEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        EntityEntity entity = new EntityEntity();
        assertNull(entity.getId());
        assertNull(entity.getEntityCode());
        assertNull(entity.getTableName());
        assertNull(entity.getCreateTime());
    }
}