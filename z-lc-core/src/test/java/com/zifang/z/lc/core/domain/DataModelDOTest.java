package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DataModelDO 单元测试
 *
 * @author zifang
 */
public class DataModelDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DataModelDO model = new DataModelDO();
        assertNotNull(model);
    }

    @Test
    public void shouldExtendBaseDTO() {
        DataModelDO model = new DataModelDO();
        assertTrue(model instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        DataModelDO model = new DataModelDO();
        assertTrue(model instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        DataModelDO model = new DataModelDO();
        model.setAppCode("app-001");
        assertEquals("app-001", model.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelName() {
        DataModelDO model = new DataModelDO();
        model.setModelName("用户");
        assertEquals("用户", model.getModelName());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        DataModelDO model = new DataModelDO();
        model.setModelCode("user");
        assertEquals("user", model.getModelCode());
    }

    @Test
    public void shouldSetAndGetModelDesc() {
        DataModelDO model = new DataModelDO();
        model.setModelDesc("用户数据模型");
        assertEquals("用户数据模型", model.getModelDesc());
    }

    @Test
    public void shouldSetAndGetPhysicalFlag() {
        DataModelDO model = new DataModelDO();
        model.setPhysicalFlag(1);
        assertEquals(Integer.valueOf(1), model.getPhysicalFlag());
    }

    @Test
    public void shouldSetAndGetTreeNodeCode() {
        DataModelDO model = new DataModelDO();
        model.setTreeNodeCode("user_tree");
        assertEquals("user_tree", model.getTreeNodeCode());
    }

    @Test
    public void shouldInheritBaseDTOBehavior() {
        DataModelDO model = new DataModelDO();
        model.setId(100L);
        model.setTenantCode("tenant-001");

        assertEquals(Long.valueOf(100L), model.getId());
        assertEquals("tenant-001", model.getTenantCode());
    }

    @Test
    public void shouldHandleNullValues() {
        DataModelDO model = new DataModelDO();
        assertNull(model.getAppCode());
        assertNull(model.getModelName());
        assertNull(model.getModelCode());
    }
}