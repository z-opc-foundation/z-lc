package com.zifang.z.lc.core.permission.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PermissionEntity 单元测试
 *
 * @author zifang
 */
public class PermissionEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        PermissionEntity entity = new PermissionEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        PermissionEntity entity = new PermissionEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        PermissionEntity entity = new PermissionEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        PermissionEntity entity = new PermissionEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetEntityCode() {
        PermissionEntity entity = new PermissionEntity();
        entity.setEntityCode("user");
        assertEquals("user", entity.getEntityCode());
    }

    @Test
    public void shouldSetAndGetRoleCode() {
        PermissionEntity entity = new PermissionEntity();
        entity.setRoleCode("admin");
        assertEquals("admin", entity.getRoleCode());
    }

    @Test
    public void shouldSetAndGetPermission() {
        PermissionEntity entity = new PermissionEntity();
        entity.setPermission("READ");
        assertEquals("READ", entity.getPermission());
    }

    @Test
    public void shouldSupportAllPermissionLevels() {
        PermissionEntity entity = new PermissionEntity();
        String[] permissions = {"READ", "WRITE", "DELETE", "ADMIN"};
        for (String p : permissions) {
            entity.setPermission(p);
            assertEquals(p, entity.getPermission());
        }
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        PermissionEntity entity = new PermissionEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetCreateTime() {
        PermissionEntity entity = new PermissionEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        assertEquals(now, entity.getCreateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        PermissionEntity entity = new PermissionEntity();
        assertNull(entity.getId());
        assertNull(entity.getAppCode());
        assertNull(entity.getEntityCode());
        assertNull(entity.getRoleCode());
        assertNull(entity.getPermission());
        assertNull(entity.getTenantCode());
        assertNull(entity.getCreateTime());
    }

    @Test
    public void shouldHaveSerialVersionUid() throws Exception {
        java.lang.reflect.Field field = PermissionEntity.class.getDeclaredField("serialVersionUID");
        assertTrue("serialVersionUID should be static", java.lang.reflect.Modifier.isStatic(field.getModifiers()));
        assertTrue("serialVersionUID should be final", java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    }
}