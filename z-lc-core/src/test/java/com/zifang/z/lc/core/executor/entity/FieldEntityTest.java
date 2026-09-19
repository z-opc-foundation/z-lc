package com.zifang.z.lc.core.executor.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * FieldEntity 单元测试
 *
 * @author zifang
 */
public class FieldEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        FieldEntity entity = new FieldEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        FieldEntity entity = new FieldEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        FieldEntity entity = new FieldEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetEntityId() {
        FieldEntity entity = new FieldEntity();
        entity.setEntityId(50L);
        assertEquals(Long.valueOf(50L), entity.getEntityId());
    }

    @Test
    public void shouldSetAndGetFieldCode() {
        FieldEntity entity = new FieldEntity();
        entity.setFieldCode("user_name");
        assertEquals("user_name", entity.getFieldCode());
    }

    @Test
    public void shouldSetAndGetFieldName() {
        FieldEntity entity = new FieldEntity();
        entity.setFieldName("用户名");
        assertEquals("用户名", entity.getFieldName());
    }

    @Test
    public void shouldSetAndGetFieldType() {
        FieldEntity entity = new FieldEntity();
        entity.setFieldType("VARCHAR");
        assertEquals("VARCHAR", entity.getFieldType());
    }

    @Test
    public void shouldSetAndGetTimes() {
        FieldEntity entity = new FieldEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        FieldEntity entity = new FieldEntity();
        assertNull(entity.getId());
        assertNull(entity.getFieldCode());
        assertNull(entity.getEntityId());
    }
}