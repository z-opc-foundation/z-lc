package com.zifang.z.lc.mapper.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.event.entity.EventEntity;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * EventMapper MyBatis Mapper 接口单元测试
 */
public class EventMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(EventMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("EventMapper 应当继承 BaseMapper<EventEntity>",
                BaseMapper.class.isAssignableFrom(EventMapper.class));
    }

    @Test
    public void shouldDeclareMaxApplySeqMethod() throws NoSuchMethodException {
        Method m = EventMapper.class.getMethod("maxApplySeq", String.class, String.class);
        assertNotNull(m);
        assertEquals(Long.class, m.getReturnType());
    }

    @Test
    public void maxApplySeqShouldHaveSelectAnnotation() throws NoSuchMethodException {
        Method m = EventMapper.class.getMethod("maxApplySeq", String.class, String.class);
        org.apache.ibatis.annotations.Select select = m.getAnnotation(org.apache.ibatis.annotations.Select.class);
        assertNotNull(select);
        // SQL 应当包含 SELECT MAX
        String[] sqls = select.value();
        boolean hasMax = false;
        for (String s : sqls) {
            if (s.contains("MAX(apply_seq)")) {
                hasMax = true;
                break;
            }
        }
        assertTrue("SQL 应包含 MAX(apply_seq)", hasMax);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.event",
                EventMapper.class.getPackage().getName());
    }
}