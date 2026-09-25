package com.zifang.z.lc.core.schema;

import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SchemaAdminService 服务接口契约测试
 */
public class SchemaAdminServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(SchemaAdminService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateApp() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("createApp", AppDTO.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListApps() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("listApps", String.class, int.class, int.class);
        assertEquals(PageResult.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetApp() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("getApp", Long.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetAppByCode() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("getAppByCode", String.class, String.class);
        assertEquals(AppDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateApp() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("updateApp", Long.class, AppDTO.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteApp() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("deleteApp", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareCreateEntity() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod(
                "createEntity", String.class, String.class, EntityDefDTO.class);
        assertEquals(EntityDefDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListEntities() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("listEntities", String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetEntity() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("getEntity", Long.class);
        assertEquals(EntityDefDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateEntity() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("updateEntity", Long.class, EntityDefDTO.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteEntity() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("deleteEntity", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    /**
     * 返回类型不是风格问题：旧的 {@code String} 返回的是那条 DDL，而建没建成只有库里知道，
     * 所以调用方拿到返回值也没法判断成功 —— 见缺陷 #43。
     */
    @Test
    public void shouldDeclareProvisionTable() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("provisionTable", Long.class);
        assertEquals(ProvisionReport.Item.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareProvisionAllTables() throws NoSuchMethodException {
        Method m = SchemaAdminService.class.getMethod("provisionAllTables", String.class, String.class);
        assertEquals(ProvisionReport.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.schema",
                SchemaAdminService.class.getPackage().getName());
    }
}