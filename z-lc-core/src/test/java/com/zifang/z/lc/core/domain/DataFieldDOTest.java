package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DataFieldDO 单元测试
 *
 * @author zifang
 */
public class DataFieldDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DataFieldDO field = new DataFieldDO();
        assertNotNull(field);
    }

    @Test
    public void shouldExtendBaseDTO() {
        DataFieldDO field = new DataFieldDO();
        assertTrue(field instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        DataFieldDO field = new DataFieldDO();
        assertTrue(field instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        DataFieldDO field = new DataFieldDO();
        field.setAppCode("app-001");
        assertEquals("app-001", field.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        DataFieldDO field = new DataFieldDO();
        field.setModelCode("user");
        assertEquals("user", field.getModelCode());
    }

    @Test
    public void shouldSetAndGetFieldName() {
        DataFieldDO field = new DataFieldDO();
        field.setFieldName("用户名");
        assertEquals("用户名", field.getFieldName());
    }

    @Test
    public void shouldSetAndGetFieldCode() {
        DataFieldDO field = new DataFieldDO();
        field.setFieldCode("user_name");
        assertEquals("user_name", field.getFieldCode());
    }

    @Test
    public void shouldSetAndGetFieldIsRequired() {
        DataFieldDO field = new DataFieldDO();
        field.setFieldIsRequired(1);
        assertEquals(Integer.valueOf(1), field.getFieldIsRequired());
    }

    @Test
    public void shouldSetAndGetFieldValidationRule() {
        DataFieldDO field = new DataFieldDO();
        field.setFieldValidationRule("^[a-zA-Z]+$");
        assertEquals("^[a-zA-Z]+$", field.getFieldValidationRule());
    }

    @Test
    public void shouldSetAndGetFieldDesc() {
        DataFieldDO field = new DataFieldDO();
        field.setFieldDesc("字段描述");
        assertEquals("字段描述", field.getFieldDesc());
    }

    @Test
    public void shouldInheritBaseDTOBehavior() {
        DataFieldDO field = new DataFieldDO();
        field.setId(100L);
        field.setTenantCode("tenant-001");

        assertEquals(Long.valueOf(100L), field.getId());
        assertEquals("tenant-001", field.getTenantCode());
    }

    @Test
    public void shouldHandleNullValues() {
        DataFieldDO field = new DataFieldDO();
        assertNull(field.getAppCode());
        assertNull(field.getFieldCode());
        assertNull(field.getFieldName());
    }
}