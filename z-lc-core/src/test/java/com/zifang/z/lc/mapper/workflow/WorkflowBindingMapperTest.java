package com.zifang.z.lc.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * WorkflowBindingMapper MyBatis Mapper 接口单元测试
 */
public class WorkflowBindingMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(WorkflowBindingMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("WorkflowBindingMapper 应当继承 BaseMapper<WorkflowBindingEntity>",
                BaseMapper.class.isAssignableFrom(WorkflowBindingMapper.class));
    }

    @Test
    public void shouldBeAnnotatedWithMapper() {
        assertNotNull("WorkflowBindingMapper 应当标注 @Mapper",
                WorkflowBindingMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.workflow",
                WorkflowBindingMapper.class.getPackage().getName());
    }
}