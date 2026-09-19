package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AppTagDO 单元测试
 *
 * @author zifang
 */
public class AppTagDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        AppTagDO entity = new AppTagDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        AppTagDO entity = new AppTagDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        AppTagDO entity = new AppTagDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        AppTagDO entity = new AppTagDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetTagName() {
        AppTagDO entity = new AppTagDO();
        entity.setTagName("Urgent");
        assertEquals("Urgent", entity.getTagName());
    }

    @Test
    public void shouldSetAndGetTagCode() {
        AppTagDO entity = new AppTagDO();
        entity.setTagCode("urgent");
        assertEquals("urgent", entity.getTagCode());
    }

    @Test
    public void shouldSetAndGetTagDesc() {
        AppTagDO entity = new AppTagDO();
        entity.setTagDesc("High priority tag");
        assertEquals("High priority tag", entity.getTagDesc());
    }

    @Test
    public void shouldSetAndGetTagType() {
        AppTagDO entity = new AppTagDO();
        entity.setTagType("priority");
        assertEquals("priority", entity.getTagType());
    }

    @Test
    public void shouldSetAndGetTagGroupCode() {
        AppTagDO entity = new AppTagDO();
        entity.setTagGroupCode("priority-group");
        assertEquals("priority-group", entity.getTagGroupCode());
    }

    @Test
    public void shouldCreateViaFactoryMethod() {
        AppTagDO entity = AppTagDO.of("app-001", "priority-group", "urgent", "Urgent", "High priority tag", "priority");
        assertNotNull(entity);
        assertEquals("app-001", entity.getAppCode());
        assertEquals("priority-group", entity.getTagGroupCode());
        assertEquals("urgent", entity.getTagCode());
        assertEquals("Urgent", entity.getTagName());
        assertEquals("High priority tag", entity.getTagDesc());
        assertEquals("priority", entity.getTagType());
    }

    @Test
    public void shouldHandleNullValues() {
        AppTagDO entity = new AppTagDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getTagName());
        assertNull(entity.getTagCode());
        assertNull(entity.getTagDesc());
        assertNull(entity.getTagType());
        assertNull(entity.getTagGroupCode());
    }
}