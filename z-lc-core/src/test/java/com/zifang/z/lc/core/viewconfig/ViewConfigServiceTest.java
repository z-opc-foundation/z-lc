package com.zifang.z.lc.core.viewconfig;

import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ViewConfigService 服务接口契约测试
 */
public class ViewConfigServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ViewConfigService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateViewConfig() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod("createViewConfig", ViewConfigCreateReq.class);
        assertEquals(ViewConfigDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateViewConfig() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod("updateViewConfig", ViewConfigUpdateReq.class);
        assertEquals(ViewConfigDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteViewConfig() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod("deleteViewConfig", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetViewConfig() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod(
                "getViewConfig", String.class, String.class, String.class, String.class);
        assertEquals(ViewConfigDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListViewConfigs() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod(
                "listViewConfigs", String.class, String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListViewConfigsByApp() throws NoSuchMethodException {
        Method m = ViewConfigService.class.getMethod("listViewConfigsByApp", String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.viewconfig",
                ViewConfigService.class.getPackage().getName());
    }
}