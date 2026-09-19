package com.zifang.z.lc.core.workflow.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WorkflowBindingEntity 单元测试
 *
 * @author zifang
 */
public class WorkflowBindingEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setEntityCode("leave");
        assertEquals("leave", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetTriggerEvent() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setTriggerEvent("AFTER_CREATE");
        assertEquals("AFTER_CREATE", entity.getTriggerEvent());
    }

    @Test
    public void shouldSetAndGetProcessDefinitionKey() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setProcessDefinitionKey("leave-process");
        assertEquals("leave-process", entity.getProcessDefinitionKey());
    }

    @Test
    public void shouldSetAndGetAutoSubmit() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setAutoSubmit(1);
        assertEquals(Integer.valueOf(1), entity.getAutoSubmit());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetDeleted() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        entity.setDeleted(0);
        assertEquals(Integer.valueOf(0), entity.getDeleted());
    }

    @Test
    public void shouldHandleNullValues() {
        WorkflowBindingEntity entity = new WorkflowBindingEntity();
        assertNull(entity.getId());
        assertNull(entity.getEntityCode());
        assertNull(entity.getProcessDefinitionKey());
        assertNull(entity.getCreateTime());
    }
}