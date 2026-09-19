package com.zifang.z.lc.core.materialize.dto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * MaterializationReq 单元测试
 *
 * @author zifang
 */
public class MaterializationReqTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        MaterializationReq req = new MaterializationReq();
        assertNotNull(req);
    }

    @Test
    public void shouldSetAndGetAppCode() {
        MaterializationReq req = new MaterializationReq();
        req.setAppCode("app-001");
        assertEquals("app-001", req.getAppCode());
    }

    @Test
    public void shouldSetAndGetMaterializationPath() {
        MaterializationReq req = new MaterializationReq();
        req.setMaterializationPath("/tmp/export");
        assertEquals("/tmp/export", req.getMaterializationPath());
    }

    @Test
    public void shouldSetAndGetEntityCodes() {
        MaterializationReq req = new MaterializationReq();
        req.setEntityCodes(Arrays.asList("user", "order"));
        assertEquals(2, req.getEntityCodes().size());
        assertEquals("user", req.getEntityCodes().get(0));
    }

    @Test
    public void shouldSetAndGetEmptyEntityCodes() {
        MaterializationReq req = new MaterializationReq();
        req.setEntityCodes(Collections.emptyList());
        assertEquals(0, req.getEntityCodes().size());
    }

    @Test
    public void shouldSetAndGetDescription() {
        MaterializationReq req = new MaterializationReq();
        req.setDescription("Test materialization");
        assertEquals("Test materialization", req.getDescription());
    }

    @Test
    public void shouldSetAndGetTriggerSource() {
        MaterializationReq req = new MaterializationReq();
        req.setTriggerSource("USER");
        assertEquals("USER", req.getTriggerSource());
    }

    @Test
    public void shouldHandleNullValues() {
        MaterializationReq req = new MaterializationReq();
        assertNull(req.getAppCode());
        assertNull(req.getMaterializationPath());
        assertNull(req.getEntityCodes());
        assertNull(req.getTriggerSource());
    }
}