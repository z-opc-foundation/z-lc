package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DictItemDO 单元测试
 *
 * @author zifang
 */
public class DictItemDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DictItemDO entity = new DictItemDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        DictItemDO entity = new DictItemDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        DictItemDO entity = new DictItemDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        DictItemDO entity = new DictItemDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetDictName() {
        DictItemDO entity = new DictItemDO();
        entity.setDictName("Gender");
        assertEquals("Gender", entity.getDictName());
    }

    @Test
    public void shouldSetAndGetDictCode() {
        DictItemDO entity = new DictItemDO();
        entity.setDictCode("gender");
        assertEquals("gender", entity.getDictCode());
    }

    @Test
    public void shouldSetAndGetDictDesc() {
        DictItemDO entity = new DictItemDO();
        entity.setDictDesc("Gender options");
        assertEquals("Gender options", entity.getDictDesc());
    }

    @Test
    public void shouldSetAndGetDictTreeNodeCode() {
        DictItemDO entity = new DictItemDO();
        entity.setDictTreeNodeCode("root");
        assertEquals("root", entity.getDictTreeNodeCode());
    }

    @Test
    public void shouldSetAndGetRemote() {
        DictItemDO entity = new DictItemDO();
        entity.setRemote(1);
        assertEquals(Integer.valueOf(1), entity.getRemote());
    }

    @Test
    public void shouldSetAndGetBondUrl() {
        DictItemDO entity = new DictItemDO();
        entity.setBondUrl("https://api.example.com/dict");
        assertEquals("https://api.example.com/dict", entity.getBondUrl());
    }

    @Test
    public void shouldReturnFalseForRemoteWhenNull() {
        DictItemDO entity = new DictItemDO();
        assertFalse(entity.isRemote());
    }

    @Test
    public void shouldReturnFalseForRemoteWhenZero() {
        DictItemDO entity = new DictItemDO();
        entity.setRemote(0);
        assertFalse(entity.isRemote());
    }

    @Test
    public void shouldReturnTrueForRemoteWhenOne() {
        DictItemDO entity = new DictItemDO();
        entity.setRemote(1);
        assertTrue(entity.isRemote());
    }

    @Test
    public void shouldHandleNullValues() {
        DictItemDO entity = new DictItemDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getDictName());
        assertNull(entity.getBondUrl());
        assertNull(entity.getRemote());
    }
}