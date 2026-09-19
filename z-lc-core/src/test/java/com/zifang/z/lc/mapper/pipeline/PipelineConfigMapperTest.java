package com.zifang.z.lc.mapper.pipeline;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.pipeline.config.entity.PipelineConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PipelineConfigMapper MyBatis Mapper 接口单元测试
 */
public class PipelineConfigMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(PipelineConfigMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("PipelineConfigMapper 应当继承 BaseMapper<PipelineConfigEntity>",
                BaseMapper.class.isAssignableFrom(PipelineConfigMapper.class));
    }

    @Test
    public void shouldBeAnnotatedWithMapper() {
        assertNotNull("PipelineConfigMapper 应当标注 @Mapper",
                PipelineConfigMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.pipeline",
                PipelineConfigMapper.class.getPackage().getName());
    }
}