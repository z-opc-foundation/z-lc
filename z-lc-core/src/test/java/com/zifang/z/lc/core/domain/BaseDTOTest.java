package com.zifang.z.lc.core.domain;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * BaseDTO 单元测试
 *
 * @author zifang
 */
public class BaseDTOTest {

    @Test
    public void shouldBeAbstract() {
        assertTrue(java.lang.reflect.Modifier.isAbstract(BaseDTO.class.getModifiers()));
    }

    @Test
    public void shouldImplementSerializable() {
        BaseDTO dto = new TestBaseDTO();
        assertTrue(dto instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetId() {
        BaseDTO dto = new TestBaseDTO();
        dto.setId(100L);
        assertEquals(Long.valueOf(100L), dto.getId());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        BaseDTO dto = new TestBaseDTO();
        dto.setTenantCode("tenant-001");
        assertEquals("tenant-001", dto.getTenantCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        BaseDTO dto = new TestBaseDTO();
        Date now = new Date();
        dto.setCreateTime(now);
        dto.setUpdateTime(now);

        assertEquals(now, dto.getCreateTime());
        assertEquals(now, dto.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetCreatedBy() {
        BaseDTO dto = new TestBaseDTO();
        dto.setCreateBy("user-001");
        dto.setUpdateBy("user-002");

        assertEquals("user-001", dto.getCreateBy());
        assertEquals("user-002", dto.getUpdateBy());
    }

    @Test
    public void shouldSetAndGetDeleted() {
        BaseDTO dto = new TestBaseDTO();
        dto.setDeleted(0);
        assertEquals(Integer.valueOf(0), dto.getDeleted());
    }

    @Test
    public void shouldSetAndGetExtend() {
        BaseDTO dto = new TestBaseDTO();
        dto.setExtend("{\"key\":\"value\"}");
        assertEquals("{\"key\":\"value\"}", dto.getExtend());
    }

    @Test
    public void shouldHandleNullValues() {
        BaseDTO dto = new TestBaseDTO();
        assertNull(dto.getId());
        assertNull(dto.getTenantCode());
        assertNull(dto.getExtend());
    }

    // 测试用的具体子类
    static class TestBaseDTO extends BaseDTO {
    }
}