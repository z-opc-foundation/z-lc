package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DictItemValueDO 单元测试
 *
 * @author zifang
 */
public class DictItemValueDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DictItemValueDO entity = new DictItemValueDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        DictItemValueDO entity = new DictItemValueDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        DictItemValueDO entity = new DictItemValueDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetDictItemId() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setDictItemId(100L);
        assertEquals(Long.valueOf(100L), entity.getDictItemId());
    }

    @Test
    public void shouldSetAndGetDictCode() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setDictCode("gender");
        assertEquals("gender", entity.getDictCode());
    }

    @Test
    public void shouldSetAndGetName() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setName("Male");
        assertEquals("Male", entity.getName());
    }

    @Test
    public void shouldSetAndGetValue() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setValue("M");
        assertEquals("M", entity.getValue());
    }

    @Test
    public void shouldSetAndGetSortOrder() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setSortOrder(1);
        assertEquals(Integer.valueOf(1), entity.getSortOrder());
    }

    @Test
    public void shouldSetAndGetDefaultFlag() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setDefaultFlag(1);
        assertEquals(Integer.valueOf(1), entity.getDefaultFlag());
    }

    @Test
    public void shouldReturnFalseForDefaultWhenNull() {
        DictItemValueDO entity = new DictItemValueDO();
        assertFalse(entity.isDefault());
    }

    @Test
    public void shouldReturnFalseForDefaultWhenZero() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setDefaultFlag(0);
        assertFalse(entity.isDefault());
    }

    @Test
    public void shouldReturnTrueForDefaultWhenOne() {
        DictItemValueDO entity = new DictItemValueDO();
        entity.setDefaultFlag(1);
        assertTrue(entity.isDefault());
    }

    @Test
    public void shouldHandleNullValues() {
        DictItemValueDO entity = new DictItemValueDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getName());
        assertNull(entity.getValue());
        assertNull(entity.getSortOrder());
    }
}