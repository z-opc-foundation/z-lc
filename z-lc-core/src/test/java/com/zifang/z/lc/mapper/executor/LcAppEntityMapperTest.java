package com.zifang.z.lc.mapper.executor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * LcAppEntityMapper MyBatis Mapper 接口单元测试
 * <p>
 * 命名特殊: 因 z-ctc 已有 AppMapper, 本类改名避免 Spring Bean 冲突.
 */
public class LcAppEntityMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(LcAppEntityMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("LcAppEntityMapper 应当继承 BaseMapper<AppEntity>",
                BaseMapper.class.isAssignableFrom(LcAppEntityMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.executor",
                LcAppEntityMapper.class.getPackage().getName());
    }

    @Test
    public void simpleClassNameShouldBeLcAppEntityMapper() {
        assertEquals("LcAppEntityMapper", LcAppEntityMapper.class.getSimpleName());
    }
}