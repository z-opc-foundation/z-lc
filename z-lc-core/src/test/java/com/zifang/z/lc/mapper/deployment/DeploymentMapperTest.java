package com.zifang.z.lc.mapper.deployment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.deployment.entity.DeploymentEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DeploymentMapper MyBatis Mapper 接口单元测试
 */
public class DeploymentMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DeploymentMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("DeploymentMapper 应当继承 BaseMapper<DeploymentEntity>",
                BaseMapper.class.isAssignableFrom(DeploymentMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.deployment",
                DeploymentMapper.class.getPackage().getName());
    }
}