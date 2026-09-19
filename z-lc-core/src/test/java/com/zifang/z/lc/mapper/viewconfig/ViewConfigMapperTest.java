package com.zifang.z.lc.mapper.viewconfig;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.viewconfig.entity.ViewConfigEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ViewConfigMapper MyBatis Mapper 接口单元测试
 */
public class ViewConfigMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(ViewConfigMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("ViewConfigMapper 应当继承 BaseMapper<ViewConfigEntity>",
                BaseMapper.class.isAssignableFrom(ViewConfigMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.viewconfig",
                ViewConfigMapper.class.getPackage().getName());
    }
}