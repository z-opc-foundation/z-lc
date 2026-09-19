package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AppDO 单元测试
 *
 * @author zifang
 */
public class AppDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        AppDO app = new AppDO();
        assertNotNull(app);
    }

    @Test
    public void shouldExtendBaseDTO() {
        AppDO app = new AppDO();
        assertTrue(app instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        AppDO app = new AppDO();
        assertTrue(app instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        AppDO app = new AppDO();
        app.setAppCode("app-001");
        assertEquals("app-001", app.getAppCode());
    }

    @Test
    public void shouldSetAndGetAppName() {
        AppDO app = new AppDO();
        app.setAppName("MyApp");
        assertEquals("MyApp", app.getAppName());
    }

    @Test
    public void shouldSetAndGetAppDesc() {
        AppDO app = new AppDO();
        app.setAppDesc("应用描述");
        assertEquals("应用描述", app.getAppDesc());
    }

    @Test
    public void shouldSetAndGetAppIcon() {
        AppDO app = new AppDO();
        app.setAppIcon("icon.png");
        assertEquals("icon.png", app.getAppIcon());
    }

    @Test
    public void shouldSetAndGetHomeUrl() {
        AppDO app = new AppDO();
        app.setHomeUrl("/home");
        assertEquals("/home", app.getHomeUrl());
    }

    @Test
    public void shouldSetAndGetTheme() {
        AppDO app = new AppDO();
        app.setTheme("dark");
        assertEquals("dark", app.getTheme());
    }

    @Test
    public void shouldSetAndGetStatus() {
        AppDO app = new AppDO();
        app.setStatus(1);
        assertEquals(Integer.valueOf(1), app.getStatus());
    }

    @Test
    public void shouldSetAndGetDefaultDatasourceCode() {
        AppDO app = new AppDO();
        app.setDefaultDatasourceCode("ds-001");
        assertEquals("ds-001", app.getDefaultDatasourceCode());
    }

    @Test
    public void shouldInheritBaseDTOBehavior() {
        AppDO app = new AppDO();
        app.setId(100L);
        app.setTenantCode("tenant-001");

        assertEquals(Long.valueOf(100L), app.getId());
        assertEquals("tenant-001", app.getTenantCode());
    }

    @Test
    public void shouldHandleNullValues() {
        AppDO app = new AppDO();
        assertNull(app.getAppCode());
        assertNull(app.getAppName());
        assertNull(app.getAppDesc());
    }
}