package com.zifang.z.lc.mapper.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PermissionMapper MyBatis Mapper 接口单元测试
 */
public class PermissionMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(PermissionMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("PermissionMapper 应当继承 BaseMapper<PermissionEntity>",
                BaseMapper.class.isAssignableFrom(PermissionMapper.class));
    }

    @Test
    public void shouldBeAnnotatedWithMapper() {
        assertNotNull("PermissionMapper 应当标注 @Mapper",
                PermissionMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.permission",
                PermissionMapper.class.getPackage().getName());
    }
}