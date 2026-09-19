package com.zifang.z.lc.mapper.dict;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.dict.entity.DictItemEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DictItemMapper MyBatis Mapper 接口单元测试
 */
public class DictItemMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DictItemMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("DictItemMapper 应当继承 BaseMapper<DictItemEntity>",
                BaseMapper.class.isAssignableFrom(DictItemMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.dict",
                DictItemMapper.class.getPackage().getName());
    }
}