package com.zifang.z.lc.mapper.executor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * FieldMapper MyBatis Mapper 接口单元测试
 */
public class FieldMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(FieldMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("FieldMapper 应当继承 BaseMapper<FieldEntity>",
                BaseMapper.class.isAssignableFrom(FieldMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.executor",
                FieldMapper.class.getPackage().getName());
    }
}