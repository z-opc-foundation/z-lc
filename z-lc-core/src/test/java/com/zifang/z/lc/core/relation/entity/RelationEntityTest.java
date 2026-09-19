package com.zifang.z.lc.core.relation.entity;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * RelationEntity 单元测试
 *
 * @author zifang
 */
public class RelationEntityTest {

    @Test
    public void shouldCreateWithDefaultConstructor() {
        RelationEntity entity = new RelationEntity();
        assertNotNull(entity);
    }

    @Test
    public void shouldImplementSerializable() {
        RelationEntity entity = new RelationEntity();
        assertTrue(entity instanceof java.io.Serializable);
    }

    @Test
    public void shouldSetAndGetRelationCode() {
        RelationEntity entity = new RelationEntity();
        entity.setRelationCode("user_role");
        assertEquals("user_role", entity.getRelationCode());
    }

    @Test
    public void shouldSetAndGetRelationName() {
        RelationEntity entity = new RelationEntity();
        entity.setRelationName("用户角色");
        assertEquals("用户角色", entity.getRelationName());
    }

    @Test
    public void shouldSetAndGetEntityCodes() {
        RelationEntity entity = new RelationEntity();
        entity.setSourceEntityCode("user");
        entity.setTargetEntityCode("role");

        assertEquals("user", entity.getSourceEntityCode());
        assertEquals("role", entity.getTargetEntityCode());
    }

    @Test
    public void shouldSetAndGetRelationType() {
        RelationEntity entity = new RelationEntity();
        entity.setRelationType("ONE_TO_MANY");
        assertEquals("ONE_TO_MANY", entity.getRelationType());
    }

    @Test
    public void shouldSetAndGetSourceFieldCode() {
        RelationEntity entity = new RelationEntity();
        entity.setSourceFieldCode("role_id");
        assertEquals("role_id", entity.getSourceFieldCode());
    }

    @Test
    public void shouldSetAndGetThroughTable() {
        RelationEntity entity = new RelationEntity();
        entity.setThroughTable("user_role");
        assertEquals("user_role", entity.getThroughTable());
    }

    @Test
    public void shouldSetAndGetTenantCode() {
        RelationEntity entity = new RelationEntity();
        entity.setTenantCode("tenant-001");
        assertEquals("tenant-001", entity.getTenantCode());
    }

    @Test
    public void shouldSetAndGetAppCode() {
        RelationEntity entity = new RelationEntity();
        entity.setAppCode("app-001");
        assertEquals("app-001", entity.getAppCode());
    }

    @Test
    public void shouldSetAndGetTimes() {
        RelationEntity entity = new RelationEntity();
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
    }

    @Test
    public void shouldHandleNullValues() {
        RelationEntity entity = new RelationEntity();
        assertNull(entity.getId());
        assertNull(entity.getRelationCode());
        assertNull(entity.getSourceEntityCode());
    }
}