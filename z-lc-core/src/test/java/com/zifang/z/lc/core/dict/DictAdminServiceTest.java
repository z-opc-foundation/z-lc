package com.zifang.z.lc.core.dict;

import com.zifang.z.lc.common.dto.DictDTO;
import com.zifang.z.lc.common.dto.DictItemDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DictAdminService 服务接口契约测试
 */
public class DictAdminServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DictAdminService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateDict() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("createDict", DictDTO.class);
        assertEquals(DictDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateDict() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("updateDict", DictDTO.class);
        assertEquals(DictDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteDict() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("deleteDict", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListDicts() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("listDicts", String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetDictByCode() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("getDictByCode", String.class, String.class);
        assertEquals(DictDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareSaveItems() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("saveItems", String.class, String.class, List.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListItems() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("listItems", String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteItem() throws NoSuchMethodException {
        Method m = DictAdminService.class.getMethod("deleteItem", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.dict",
                DictAdminService.class.getPackage().getName());
    }
}