package com.zifang.z.lc.core.materialize.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterializationEntity 单元测试
 *
 * @author zifang
 */
public class MaterializationEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        MaterializationEntity entity = new MaterializationEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        MaterializationEntity entity = new MaterializationEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldDefineStatusConstants() {
        assertEquals("PENDING", MaterializationEntity.STATUS_PENDING);
        assertEquals("GENERATING", MaterializationEntity.STATUS_GENERATING);
        assertEquals("READY", MaterializationEntity.STATUS_READY);
        assertEquals("FAILED", MaterializationEntity.STATUS_FAILED);
    }

    @Test
    public void shouldDefineSourceConstants() {
        assertEquals("USER", MaterializationEntity.SOURCE_USER);
        assertEquals("AGENT", MaterializationEntity.SOURCE_AGENT);
        assertEquals("SYSTEM", MaterializationEntity.SOURCE_SYSTEM);
    }

    @Test
    public void shouldSetAndGetId() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setId(1L);
        assertEquals(Long.valueOf(1L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetMaterializationPath() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setMaterializationPath("/tmp/materialize");
        assertEquals("/tmp/materialize", entity.getMaterializationPath());
    }

    @Test
    public void shouldSetAndGetEntityCodes() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setEntityCodes("user,order");
        assertEquals("user,order", entity.getEntityCodes());
    }

    @Test
    public void shouldSetAndGetExportVersion() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setExportVersion("v1.0");
        assertEquals("v1.0", entity.getExportVersion());
    }

    @Test
    public void shouldSetAndGetStatus() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setStatus(MaterializationEntity.STATUS_READY);
        assertEquals(MaterializationEntity.STATUS_READY, entity.getStatus());
    }

    @Test
    public void shouldSetAndGetFileCount() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setFileCount(42);
        assertEquals(Integer.valueOf(42), entity.getFileCount());
    }

    @Test
    public void shouldSetAndGetDescription() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setDescription("Test materialization");
        assertEquals("Test materialization", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetErrorMessage() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setErrorMessage("Build failed");
        assertEquals("Build failed", entity.getErrorMessage());
    }

    @Test
    public void shouldSetAndGetTriggerSource() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setTriggerSource(MaterializationEntity.SOURCE_AGENT);
        assertEquals(MaterializationEntity.SOURCE_AGENT, entity.getTriggerSource());
    }

    @Test
    public void shouldSetAndGetEventId() {
        MaterializationEntity entity = new MaterializationEntity();
        entity.setEventId("evt-001");
        assertEquals("evt-001", entity.getEventId());
    }

    @Test
    public void shouldSetAndGetTimes() {
        MaterializationEntity entity = new MaterializationEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        MaterializationEntity entity = new MaterializationEntity();
        assertNull(entity.getId());
        assertNull(entity.getMaterializationPath());
        assertNull(entity.getStatus());
        assertNull(entity.getCreateTime());
    }
}