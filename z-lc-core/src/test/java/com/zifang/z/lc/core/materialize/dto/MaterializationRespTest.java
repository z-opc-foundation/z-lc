package com.zifang.z.lc.core.materialize.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * MaterializationResp 单元测试
 *
 * @author zifang
 */
public class MaterializationRespTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        MaterializationResp resp = new MaterializationResp();
        assertNotNull(resp);
        assertNotNull(resp.getFiles());
    }

    @Test
    public void shouldSetAndGetId() {
        MaterializationResp resp = new MaterializationResp();
        resp.setId(1L);
        assertEquals(Long.valueOf(1L), resp.getId());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        MaterializationResp resp = new MaterializationResp();
        resp.setAppCode("app-001");
        assertEquals("app-001", resp.getAppCode());
    }

    @Test
    public void shouldSetAndGetMaterializationPath() {
        MaterializationResp resp = new MaterializationResp();
        resp.setMaterializationPath("/tmp/export");
        assertEquals("/tmp/export", resp.getMaterializationPath());
    }

    @Test
    public void shouldSetAndGetExportVersion() {
        MaterializationResp resp = new MaterializationResp();
        resp.setExportVersion("v1.0");
        assertEquals("v1.0", resp.getExportVersion());
    }

    @Test
    public void shouldSetAndGetStatus() {
        MaterializationResp resp = new MaterializationResp();
        resp.setStatus("READY");
        assertEquals("READY", resp.getStatus());
    }

    @Test
    public void shouldSetAndGetFileCount() {
        MaterializationResp resp = new MaterializationResp();
        resp.setFileCount(10);
        assertEquals(Integer.valueOf(10), resp.getFileCount());
    }

    @Test
    public void shouldSetAndGetDescription() {
        MaterializationResp resp = new MaterializationResp();
        resp.setDescription("desc");
        assertEquals("desc", resp.getDescription());
    }

    @Test
    public void shouldSetAndGetErrorMessage() {
        MaterializationResp resp = new MaterializationResp();
        resp.setErrorMessage("error");
        assertEquals("error", resp.getErrorMessage());
    }

    @Test
    public void shouldSetAndGetTriggerSource() {
        MaterializationResp resp = new MaterializationResp();
        resp.setTriggerSource("AGENT");
        assertEquals("AGENT", resp.getTriggerSource());
    }

    @Test
    public void shouldSetAndGetTimes() {
        MaterializationResp resp = new MaterializationResp();
        resp.setCreateTime("2026-01-01");
        resp.setUpdateTime("2026-01-02");
        assertEquals("2026-01-01", resp.getCreateTime());
        assertEquals("2026-01-02", resp.getUpdateTime());
    }

    @Test
    public void shouldSetAndGetFiles() {
        MaterializationResp resp = new MaterializationResp();
        resp.setFiles(Arrays.asList(new MaterializationResp.GeneratedFile()));
        assertEquals(1, resp.getFiles().size());
    }

    @Test
    public void shouldHaveInitializedEmptyFilesList() {
        MaterializationResp resp = new MaterializationResp();
        assertTrue(resp.getFiles().isEmpty());
    }

    @Test
    public void shouldHandleNullValues() {
        MaterializationResp resp = new MaterializationResp();
        assertNull(resp.getId());
        assertNull(resp.getAppCode());
        assertNull(resp.getStatus());
    }

    @Test
    public void generatedFileShouldCreateWithDefaultConstructor() {
        MaterializationResp.GeneratedFile file = new MaterializationResp.GeneratedFile();
        assertNotNull(file);
        assertEquals(0L, file.getSizeBytes());
    }

    @Test
    public void generatedFileShouldCreateWithContentConstructor() {
        MaterializationResp.GeneratedFile file = new MaterializationResp.GeneratedFile(
                "user", "Entity", "/tmp/user.java", "class User {}");
        assertEquals("user", file.getEntityCode());
        assertEquals("Entity", file.getType());
        assertEquals("/tmp/user.java", file.getRelativePath());
        assertEquals("class User {}", file.getContent());
        assertEquals(13L, file.getSizeBytes());
    }

    @Test
    public void generatedFileShouldHandleNullContent() {
        MaterializationResp.GeneratedFile file = new MaterializationResp.GeneratedFile(
                "user", "Entity", "/tmp/user.java", null);
        assertEquals(0L, file.getSizeBytes());
    }

    @Test
    public void generatedFileShouldSupportSetters() {
        MaterializationResp.GeneratedFile file = new MaterializationResp.GeneratedFile();
        file.setEntityCode("order");
        file.setType("Mapper");
        file.setRelativePath("/tmp/OrderMapper.java");
        file.setContent("content");
        file.setSizeBytes(100L);

        assertEquals("order", file.getEntityCode());
        assertEquals("Mapper", file.getType());
        assertEquals("/tmp/OrderMapper.java", file.getRelativePath());
        assertEquals("content", file.getContent());
        assertEquals(100L, file.getSizeBytes());
    }
}