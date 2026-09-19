package com.zifang.z.lc.core.pipeline.config.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PipelineConfigEntity 单元测试
 *
 * @author zifang
 */
public class PipelineConfigEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setEntityCode("user");
        assertEquals("user", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetTriggerEvent() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setTriggerEvent("AFTER_CREATE");
        assertEquals("AFTER_CREATE", entity.getTriggerEvent());
    }

    @Test
    public void shouldSetAndGetStages() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setStages("[{\"type\":\"type1\",\"order\":1}]");
        assertEquals("[{\"type\":\"type1\",\"order\":1}]", entity.getStages());
    }

    @Test
    public void shouldSetAndGetEnabled() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setEnabled(1);
        assertEquals(Integer.valueOf(1), entity.getEnabled());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetDeleted() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        entity.setDeleted(0);
        assertEquals(Integer.valueOf(0), entity.getDeleted());
    }

    @Test
    public void shouldHandleNullValues() {
        PipelineConfigEntity entity = new PipelineConfigEntity();
        assertNull(entity.getId());
        assertNull(entity.getEntityCode());
        assertNull(entity.getStages());
        assertNull(entity.getCreateTime());
    }
}