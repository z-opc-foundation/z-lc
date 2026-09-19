package com.zifang.z.lc.core.materialize.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterializationMapper MyBatis Mapper 接口单元测试
 * <p>
 * 包含 listByApp / getByExportVersion / updateStatus 三个自定义方法的 @Select / @Update 注解校验.
 */
public class MaterializationMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(MaterializationMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("MaterializationMapper 应当继承 BaseMapper<MaterializationEntity>",
                BaseMapper.class.isAssignableFrom(MaterializationMapper.class));
    }

    @Test
    public void shouldBeAnnotatedWithMapper() {
        assertNotNull("MaterializationMapper 应当标注 @Mapper",
                MaterializationMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.materialize.mapper",
                MaterializationMapper.class.getPackage().getName());
    }

    @Test
    public void listByAppShouldReturnList() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod("listByApp", String.class, String.class, int.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void listByAppShouldHaveSelectAnnotation() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod("listByApp", String.class, String.class, int.class);
        Select select = m.getAnnotation(Select.class);
        assertNotNull(select);
        assertTrue("SQL 应包含 z_lc_materialization",
                select.value()[0].contains("z_lc_materialization"));
        assertTrue("SQL 应包含 ORDER BY",
                select.value()[0].contains("ORDER BY"));
        assertTrue("SQL 应包含 LIMIT",
                select.value()[0].contains("LIMIT"));
    }

    @Test
    public void getByExportVersionShouldReturnEntity() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod("getByExportVersion", String.class);
        assertEquals(MaterializationEntity.class, m.getReturnType());
    }

    @Test
    public void getByExportVersionShouldHaveSelectAnnotation() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod("getByExportVersion", String.class);
        Select select = m.getAnnotation(Select.class);
        assertNotNull(select);
        assertTrue("SQL 应包含 export_version",
                select.value()[0].contains("export_version"));
    }

    @Test
    public void updateStatusShouldReturnInt() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod(
                "updateStatus", Long.class, String.class, Integer.class, String.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void updateStatusShouldHaveUpdateAnnotation() throws NoSuchMethodException {
        Method m = MaterializationMapper.class.getMethod(
                "updateStatus", Long.class, String.class, Integer.class, String.class);
        Update update = m.getAnnotation(Update.class);
        assertNotNull(update);
        assertTrue("SQL 应包含 UPDATE z_lc_materialization",
                update.value()[0].contains("UPDATE z_lc_materialization"));
    }
}