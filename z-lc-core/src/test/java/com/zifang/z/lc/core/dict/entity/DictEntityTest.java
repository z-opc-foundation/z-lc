package com.zifang.z.lc.core.dict.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DictEntity 单元测试
 *
 * @author zifang
 */
public class DictEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DictEntity entity = new DictEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        DictEntity entity = new DictEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        DictEntity entity = new DictEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        DictEntity entity = new DictEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetDictCode() {
        DictEntity entity = new DictEntity();
        entity.setDictCode("dict_user_status");
        assertEquals("dict_user_status", entity.getDictCode());
    }

    @Test
    public void shouldSetAndGetDictName() {
        DictEntity entity = new DictEntity();
        entity.setDictName("用户状态字典");
        assertEquals("用户状态字典", entity.getDictName());
    }

    @Test
    public void shouldSetAndGetDescription() {
        DictEntity entity = new DictEntity();
        entity.setDescription("字典描述");
        assertEquals("字典描述", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetStatus() {
        DictEntity entity = new DictEntity();
        entity.setStatus("ACTIVE");
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    public void shouldSetAndGetTimes() {
        DictEntity entity = new DictEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetDeleted() {
        DictEntity entity = new DictEntity();
        entity.setDeleted(0);
        assertEquals(Integer.valueOf(0), entity.getDeleted());
    }

    @Test
    public void shouldHandleNullValues() {
        DictEntity entity = new DictEntity();
        assertNull(entity.getId());
        assertNull(entity.getDictCode());
        assertNull(entity.getDictName());
    }
}