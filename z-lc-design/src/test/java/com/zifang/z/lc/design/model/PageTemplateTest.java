package com.zifang.z.lc.design.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PageTemplate 单元测试
 *
 * @author zifang
 */
public class PageTemplateTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        PageTemplate pt = new PageTemplate();
        assertNotNull(pt);
    }

    @Test
    public void shouldImplementSerializable() {
        PageTemplate pt = new PageTemplate();
        assertTrue(pt instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetPageTemplateId() {
        PageTemplate pt = new PageTemplate();
        pt.setPageTemplateId(1L);
        assertEquals(Long.valueOf(1L), pt.getPageTemplateId());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        PageTemplate pt = new PageTemplate();
        pt.setAppCode("app-001");
        assertEquals("app-001", pt.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        PageTemplate pt = new PageTemplate();
        pt.setModelCode("user");
        assertEquals("user", pt.getModelCode());
    }

    @Test
    public void shouldSetAndGetPageType() {
        PageTemplate pt = new PageTemplate();
        pt.setPageType("list");
        assertEquals("list", pt.getPageType());
    }

    @Test
    public void shouldSetAndGetPageName() {
        PageTemplate pt = new PageTemplate();
        pt.setPageName("User List");
        assertEquals("User List", pt.getPageName());
    }

    @Test
    public void shouldSetAndGetPageCode() {
        PageTemplate pt = new PageTemplate();
        pt.setPageCode("user-list");
        assertEquals("user-list", pt.getPageCode());
    }

    @Test
    public void shouldSetAndGetPageDesc() {
        PageTemplate pt = new PageTemplate();
        pt.setPageDesc("Page description");
        assertEquals("Page description", pt.getPageDesc());
    }

    @Test
    public void shouldSetAndGetStatus() {
        PageTemplate pt = new PageTemplate();
        pt.setStatus(1);
        assertEquals(Integer.valueOf(1), pt.getStatus());
    }

    @Test
    public void shouldSetAndGetTreeNodeId() {
        PageTemplate pt = new PageTemplate();
        pt.setTreeNodeId(100L);
        assertEquals(Long.valueOf(100L), pt.getTreeNodeId());
    }

    @Test
    public void shouldSetAndGetViewJson() {
        PageTemplate pt = new PageTemplate();
        pt.setViewJson("{\"columns\":[]}");
        assertEquals("{\"columns\":[]}", pt.getViewJson());
    }

    @Test
    public void shouldHandleNullValues() {
        PageTemplate pt = new PageTemplate();
        assertNull(pt.getPageTemplateId());
        assertNull(pt.getAppCode());
        assertNull(pt.getViewJson());
    }
}