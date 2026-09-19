package com.zifang.z.lc.mapper.dict;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.dict.entity.DictEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DictMapper MyBatis Mapper 接口单元测试
 */
public class DictMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DictMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("DictMapper 应当继承 BaseMapper<DictEntity>",
                BaseMapper.class.isAssignableFrom(DictMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.dict",
                DictMapper.class.getPackage().getName());
    }
}