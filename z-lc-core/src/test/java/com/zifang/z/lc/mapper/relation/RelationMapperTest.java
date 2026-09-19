package com.zifang.z.lc.mapper.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.relation.entity.RelationEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * RelationMapper MyBatis Mapper 接口单元测试
 */
public class RelationMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(RelationMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("RelationMapper 应当继承 BaseMapper<RelationEntity>",
                BaseMapper.class.isAssignableFrom(RelationMapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.relation",
                RelationMapper.class.getPackage().getName());
    }
}