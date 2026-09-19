package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PageTemplateDO 单元测试
 *
 * @author zifang
 */
public class PageTemplateDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        PageTemplateDO entity = new PageTemplateDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        PageTemplateDO entity = new PageTemplateDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        PageTemplateDO entity = new PageTemplateDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setModelCode("user");
        assertEquals("user", entity.getModelCode());
    }

    @Test
    public void shouldSetAndGetPageType() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setPageType("list");
        assertEquals("list", entity.getPageType());
    }

    @Test
    public void shouldSetAndGetPageCode() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setPageCode("user-list");
        assertEquals("user-list", entity.getPageCode());
    }

    @Test
    public void shouldSetAndGetPageName() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setPageName("User List");
        assertEquals("User List", entity.getPageName());
    }

    @Test
    public void shouldSetAndGetPageDesc() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setPageDesc("Page description");
        assertEquals("Page description", entity.getPageDesc());
    }

    @Test
    public void shouldSetAndGetWorkflowDefinitionKey() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setWorkflowDefinitionKey("user-approval");
        assertEquals("user-approval", entity.getWorkflowDefinitionKey());
    }

    @Test
    public void shouldSetAndGetWorkflowDefinitionId() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setWorkflowDefinitionId("flow-001");
        assertEquals("flow-001", entity.getWorkflowDefinitionId());
    }

    @Test
    public void shouldSetAndGetViewJson() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setViewJson("{\"columns\":[]}");
        assertEquals("{\"columns\":[]}", entity.getViewJson());
    }

    @Test
    public void shouldSetAndGetStatus() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setStatus(1);
        assertEquals(Integer.valueOf(1), entity.getStatus());
    }

    @Test
    public void shouldSetAndGetPageTreeNodeCode() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setPageTreeNodeCode("root");
        assertEquals("root", entity.getPageTreeNodeCode());
    }

    @Test
    public void shouldReturnFalseForPublishedWhenNull() {
        PageTemplateDO entity = new PageTemplateDO();
        assertFalse(entity.isPublished());
    }

    @Test
    public void shouldReturnFalseForPublishedWhenZero() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setStatus(0);
        assertFalse(entity.isPublished());
    }

    @Test
    public void shouldReturnTrueForPublishedWhenOne() {
        PageTemplateDO entity = new PageTemplateDO();
        entity.setStatus(1);
        assertTrue(entity.isPublished());
    }

    @Test
    public void shouldHandleNullValues() {
        PageTemplateDO entity = new PageTemplateDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getPageCode());
        assertNull(entity.getViewJson());
        assertNull(entity.getStatus());
    }
}