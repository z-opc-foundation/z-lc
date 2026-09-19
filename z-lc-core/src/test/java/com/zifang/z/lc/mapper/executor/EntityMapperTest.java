package com.zifang.z.lc.mapper.executor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.executor.entity.EntityEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * EntityMapper MyBatis Mapper 接口单元测试
 */
public class EntityMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(EntityMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("EntityMapper 应当继承 BaseMapper<EntityEntity>",
                BaseMapper.class.isAssignableFrom(EntityMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.executor",
                EntityMapper.class.getPackage().getName());
    }
}