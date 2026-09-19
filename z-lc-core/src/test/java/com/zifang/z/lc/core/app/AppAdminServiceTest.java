package com.zifang.z.lc.core.app;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppCreateReq;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.AppUpdateReq;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * AppAdminService 服务接口契约测试
 */
public class AppAdminServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AppAdminService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateApp() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("createApp", AppCreateReq.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateApp() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("updateApp", AppUpdateReq.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteApp() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("deleteApp", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListApps() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("listApps", String.class, int.class, int.class);
        assertEquals(PageResult.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetAppByCode() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("getAppByCode", String.class, String.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclarePublishApp() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("publishApp", String.class, String.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareArchiveApp() throws NoSuchMethodException {
        Method m = AppAdminService.class.getMethod("archiveApp", String.class, String.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.app",
                AppAdminService.class.getPackage().getName());
    }
}