package com.zifang.z.lc.core.deployment.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * DeploymentEntity 单元测试
 *
 * @author zifang
 */
public class DeploymentEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        DeploymentEntity entity = new DeploymentEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        DeploymentEntity entity = new DeploymentEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldHaveStatusConstants() {
        assertEquals("PENDING", DeploymentEntity.STATUS_PENDING);
        assertEquals("RUNNING", DeploymentEntity.STATUS_RUNNING);
        assertEquals("SUCCESS", DeploymentEntity.STATUS_SUCCESS);
        assertEquals("FAILED", DeploymentEntity.STATUS_FAILED);
    }

    @Test
    public void shouldHaveTypeConstants() {
        assertEquals("HOT_LOAD", DeploymentEntity.TYPE_HOT_LOAD);
        assertEquals("DOCKER", DeploymentEntity.TYPE_DOCKER);
        assertEquals("GIT_PUSH", DeploymentEntity.TYPE_GIT_PUSH);
    }

    @Test
    public void shouldSetAndGetId() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setId(100L);
        assertEquals(Long.valueOf(100L), entity.getId());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetMaterializationId() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setMaterializationId(200L);
        assertEquals(Long.valueOf(200L), entity.getMaterializationId());
    }

    @Test
    public void shouldSetAndGetDeployType() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setDeployType(DeploymentEntity.TYPE_DOCKER);
        assertEquals(DeploymentEntity.TYPE_DOCKER, entity.getDeployType());
    }

    @Test
    public void shouldSetAndGetStatus() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setStatus(DeploymentEntity.STATUS_SUCCESS);
        assertEquals(DeploymentEntity.STATUS_SUCCESS, entity.getStatus());
    }

    @Test
    public void shouldSetAndGetDeployLog() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setDeployLog("Deploy completed successfully");
        assertEquals("Deploy completed successfully", entity.getDeployLog());
    }

    @Test
    public void shouldSetAndGetVersion() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setVersion("1.0.0");
        assertEquals("1.0.0", entity.getVersion());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        DeploymentEntity entity = new DeploymentEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        DeploymentEntity entity = new DeploymentEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        DeploymentEntity entity = new DeploymentEntity();
        assertNull(entity.getId());
        assertNull(entity.getAppCode());
        assertNull(entity.getDeployLog());
    }
}