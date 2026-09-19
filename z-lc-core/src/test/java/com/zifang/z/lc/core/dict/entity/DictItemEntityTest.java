package com.zifang.z.lc.core.dict.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DictItemEntity 单元测试
 *
 * @author zifang
 */
public class DictItemEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DictItemEntity entity = new DictItemEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        DictItemEntity entity = new DictItemEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        DictItemEntity entity = new DictItemEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        DictItemEntity entity = new DictItemEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetDictCode() {
        DictItemEntity entity = new DictItemEntity();
        entity.setDictCode("dict_user_status");
        assertEquals("dict_user_status", entity.getDictCode());
    }

    @Test
    public void shouldSetAndGetItemCode() {
        DictItemEntity entity = new DictItemEntity();
        entity.setItemCode("ACTIVE");
        assertEquals("ACTIVE", entity.getItemCode());
    }

    @Test
    public void shouldSetAndGetItemLabel() {
        DictItemEntity entity = new DictItemEntity();
        entity.setItemLabel("激活");
        assertEquals("激活", entity.getItemLabel());
    }

    @Test
    public void shouldSetAndGetItemValue() {
        DictItemEntity entity = new DictItemEntity();
        entity.setItemValue("1");
        assertEquals("1", entity.getItemValue());
    }

    @Test
    public void shouldSetAndGetSortOrder() {
        DictItemEntity entity = new DictItemEntity();
        entity.setSortOrder(10);
        assertEquals(Integer.valueOf(10), entity.getSortOrder());
    }

    @Test
    public void shouldSetAndGetDescription() {
        DictItemEntity entity = new DictItemEntity();
        entity.setDescription("字典项描述");
        assertEquals("字典项描述", entity.getDescription());
    }

    @Test
    public void shouldSetAndGetTimes() {
        DictItemEntity entity = new DictItemEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        DictItemEntity entity = new DictItemEntity();
        assertNull(entity.getId());
        assertNull(entity.getDictCode());
        assertNull(entity.getItemCode());
        assertNull(entity.getItemLabel());
    }
}