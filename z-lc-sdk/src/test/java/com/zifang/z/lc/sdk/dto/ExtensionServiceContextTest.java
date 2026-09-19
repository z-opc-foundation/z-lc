package com.zifang.z.lc.sdk.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExtensionServiceContext 单元测试
 *
 * @author zifang
 */
public class ExtensionServiceContextTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        assertNotNull(ctx);
    }

    @Test
    public void shouldImplementSerializable() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        assertTrue(ctx instanceof java.io.Serializable);
    }

    @Test
    public void shouldCreateWithFullConstructor() {
        ExtensionServiceContext ctx = new ExtensionServiceContext(
                "app-001", "model-001", "wf-001", "form-001", "page-001",
                "proc-001", Arrays.asList("tag1", "tag2"));

        assertEquals("app-001", ctx.getAppCode());
        assertEquals("model-001", ctx.getModelCode());
        assertEquals("wf-001", ctx.getWorkflowDefinitionKey());
        assertEquals("form-001", ctx.getFormCode());
        assertEquals("page-001", ctx.getPageCode());
        assertEquals("proc-001", ctx.getProcessInstanceId());
        assertEquals(Arrays.asList("tag1", "tag2"), ctx.getCustomTags());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setAppCode("app-001");
        assertEquals("app-001", ctx.getAppCode());
    }

    @Test
    public void shouldSetAndGetModelCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setModelCode("user");
        assertEquals("user", ctx.getModelCode());
    }

    @Test
    public void shouldSetAndGetWorkflowDefinitionKey() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setWorkflowDefinitionKey("leave-process");
        assertEquals("leave-process", ctx.getWorkflowDefinitionKey());
    }

    @Test
    public void shouldSetAndGetFormCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setFormCode("leave-form");
        assertEquals("leave-form", ctx.getFormCode());
    }

    @Test
    public void shouldSetAndGetPageCode() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setPageCode("leave-page");
        assertEquals("leave-page", ctx.getPageCode());
    }

    @Test
    public void shouldSetAndGetProcessInstanceId() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setProcessInstanceId("proc-001");
        assertEquals("proc-001", ctx.getProcessInstanceId());
    }

    @Test
    public void shouldSetAndGetCustomTags() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        ctx.setCustomTags(Arrays.asList("tag1"));
        assertEquals(Arrays.asList("tag1"), ctx.getCustomTags());
    }

    @Test
    public void shouldBuildViaBuilder() {
        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("app-001")
                .modelCode("model-001")
                .workflowDefinitionKey("wf-001")
                .formCode("form-001")
                .pageCode("page-001")
                .processInstanceId("proc-001")
                .customTags(Arrays.asList("tag1"))
                .build();

        assertEquals("app-001", ctx.getAppCode());
        assertEquals("model-001", ctx.getModelCode());
    }

    @Test
    public void shouldReturnBuilderFromStaticMethod() {
        ExtensionServiceContext.Builder builder = ExtensionServiceContext.builder();
        assertNotNull(builder);
    }

    @Test
    public void shouldHandleNullValues() {
        ExtensionServiceContext ctx = new ExtensionServiceContext();
        assertNull(ctx.getAppCode());
        assertNull(ctx.getModelCode());
        assertNull(ctx.getCustomTags());
    }
}