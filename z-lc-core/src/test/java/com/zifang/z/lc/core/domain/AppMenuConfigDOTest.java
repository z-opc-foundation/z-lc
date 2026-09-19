package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AppMenuConfigDO 单元测试
 *
 * @author zifang
 */
public class AppMenuConfigDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetPageCode() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setPageCode("page-001");
        assertEquals("page-001", entity.getPageCode());
    }

    @Test
    public void shouldSetAndGetPermissionCode() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setPermissionCode("perm-001");
        assertEquals("perm-001", entity.getPermissionCode());
    }

    @Test
    public void shouldSetAndGetParentPermissionCode() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setParentPermissionCode("parent-perm");
        assertEquals("parent-perm", entity.getParentPermissionCode());
    }

    @Test
    public void shouldSetAndGetMenuUrl() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setMenuUrl("/dashboard");
        assertEquals("/dashboard", entity.getMenuUrl());
    }

    @Test
    public void shouldSetAndGetMenuName() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setMenuName("Dashboard");
        assertEquals("Dashboard", entity.getMenuName());
    }

    @Test
    public void shouldSetAndGetParentMenuId() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        entity.setParentMenuId(0L);
        assertEquals(Long.valueOf(0L), entity.getParentMenuId());
    }

    @Test
    public void shouldHandleNullValues() {
        AppMenuConfigDO entity = new AppMenuConfigDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getPageCode());
        assertNull(entity.getMenuUrl());
        assertNull(entity.getParentMenuId());
    }
}