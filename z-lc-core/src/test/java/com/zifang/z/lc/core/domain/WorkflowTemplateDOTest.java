package com.zifang.z.lc.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WorkflowTemplateDO 单元测试
 *
 * @author zifang
 */
public class WorkflowTemplateDOTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        assertNotNull(entity);
    }

    @Test
    public void shouldExtendBaseDTO() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        assertTrue(entity instanceof BaseDTO);
    }

    @Test
    public void shouldImplementSerializable() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setModelCode("user");
        assertEquals("user", entity.getModelCode());
    }

    @Test
    public void shouldSetAndGetWorkflowDefinitionKey() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowDefinitionKey("user-approval");
        assertEquals("user-approval", entity.getWorkflowDefinitionKey());
    }

    @Test
    public void shouldSetAndGetWorkflowDefinitionId() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowDefinitionId("flow-001");
        assertEquals("flow-001", entity.getWorkflowDefinitionId());
    }

    @Test
    public void shouldSetAndGetWorkflowName() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowName("User Approval");
        assertEquals("User Approval", entity.getWorkflowName());
    }

    @Test
    public void shouldSetAndGetWorkflowDesc() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowDesc("Approval workflow");
        assertEquals("Approval workflow", entity.getWorkflowDesc());
    }

    @Test
    public void shouldSetAndGetWorkflowDesignDefinition() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowDesignDefinition("{\"nodes\":[]}");
        assertEquals("{\"nodes\":[]}", entity.getWorkflowDesignDefinition());
    }

    @Test
    public void shouldSetAndGetWorkflowRuntimeDefinition() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowRuntimeDefinition("<definitions></definitions>");
        assertEquals("<definitions></definitions>", entity.getWorkflowRuntimeDefinition());
    }

    @Test
    public void shouldSetAndGetWorkflowConfig() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowConfig("{\"notify\":true}");
        assertEquals("{\"notify\":true}", entity.getWorkflowConfig());
    }

    @Test
    public void shouldSetAndGetStatus() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setStatus(1);
        assertEquals(Integer.valueOf(1), entity.getStatus());
    }

    @Test
    public void shouldSetAndGetWorkflowTreeNodeCode() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setWorkflowTreeNodeCode("root");
        assertEquals("root", entity.getWorkflowTreeNodeCode());
    }

    @Test
    public void shouldReturnFalseForPublishedWhenNull() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        assertFalse(entity.isPublished());
    }

    @Test
    public void shouldReturnFalseForPublishedWhenZero() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setStatus(0);
        assertFalse(entity.isPublished());
    }

    @Test
    public void shouldReturnTrueForPublishedWhenOne() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        entity.setStatus(1);
        assertTrue(entity.isPublished());
    }

    @Test
    public void shouldHandleNullValues() {
        WorkflowTemplateDO entity = new WorkflowTemplateDO();
        assertNull(entity.getAppCode());
        assertNull(entity.getWorkflowDefinitionKey());
        assertNull(entity.getStatus());
    }
}