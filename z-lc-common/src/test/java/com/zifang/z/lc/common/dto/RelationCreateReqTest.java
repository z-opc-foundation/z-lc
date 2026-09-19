package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RelationCreateReq 单元测试
 *
 * @author zifang
 */
class RelationCreateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RelationCreateReq req = new RelationCreateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetRelationCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode("rel-001");
        assertThat(req.getRelationCode()).isEqualTo("rel-001");
    }

    @Test
    void shouldSetAndGetRelationName() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationName("关系名称");
        assertThat(req.getRelationName()).isEqualTo("关系名称");
    }

    @Test
    void shouldSetAndGetSourceEntityCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setSourceEntityCode("entity-001");
        assertThat(req.getSourceEntityCode()).isEqualTo("entity-001");
    }

    @Test
    void shouldSetAndGetTargetEntityCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setTargetEntityCode("entity-002");
        assertThat(req.getTargetEntityCode()).isEqualTo("entity-002");
    }

    @Test
    void shouldSetAndGetRelationType() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationType("ONE_TO_MANY");
        assertThat(req.getRelationType()).isEqualTo("ONE_TO_MANY");
    }

    @Test
    void shouldSetAndGetSourceFieldCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setSourceFieldCode("field-001");
        assertThat(req.getSourceFieldCode()).isEqualTo("field-001");
    }

    @Test
    void shouldSetAndGetThroughTable() {
        RelationCreateReq req = new RelationCreateReq();
        req.setThroughTable("through_table");
        assertThat(req.getThroughTable()).isEqualTo("through_table");
    }

    @Test
    void shouldSetAndGetAppCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setAppCode("app-001");
        assertThat(req.getAppCode()).isEqualTo("app-001");
    }

    @Test
    void shouldSetAndGetTenantCode() {
        RelationCreateReq req = new RelationCreateReq();
        req.setTenantCode("tenant-001");
        assertThat(req.getTenantCode()).isEqualTo("tenant-001");
    }

    @Test
    void shouldHandleNullValues() {
        RelationCreateReq req = new RelationCreateReq();
        assertThat(req.getRelationCode()).isNull();
        assertThat(req.getRelationName()).isNull();
        assertThat(req.getSourceEntityCode()).isNull();
        assertThat(req.getTargetEntityCode()).isNull();
        assertThat(req.getRelationType()).isNull();
        assertThat(req.getSourceFieldCode()).isNull();
        assertThat(req.getThroughTable()).isNull();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RelationCreateReq req = new RelationCreateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode("");
        req.setRelationName("");
        req.setSourceEntityCode("");
        req.setTargetEntityCode("");
        req.setRelationType("");
        req.setSourceFieldCode("");
        req.setThroughTable("");
        req.setAppCode("");
        req.setTenantCode("");
        
        assertThat(req.getRelationCode()).isEmpty();
        assertThat(req.getRelationName()).isEmpty();
        assertThat(req.getSourceEntityCode()).isEmpty();
        assertThat(req.getTargetEntityCode()).isEmpty();
        assertThat(req.getRelationType()).isEmpty();
        assertThat(req.getSourceFieldCode()).isEmpty();
        assertThat(req.getThroughTable()).isEmpty();
        assertThat(req.getAppCode()).isEmpty();
        assertThat(req.getTenantCode()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        RelationCreateReq req = new RelationCreateReq();
        req.setRelationCode(null);
        req.setRelationName(null);
        req.setSourceEntityCode(null);
        req.setTargetEntityCode(null);
        req.setRelationType(null);
        req.setSourceFieldCode(null);
        req.setThroughTable(null);
        req.setAppCode(null);
        req.setTenantCode(null);
        
        assertThat(req.getRelationCode()).isNull();
        assertThat(req.getRelationName()).isNull();
        assertThat(req.getSourceEntityCode()).isNull();
        assertThat(req.getTargetEntityCode()).isNull();
        assertThat(req.getRelationType()).isNull();
        assertThat(req.getSourceFieldCode()).isNull();
        assertThat(req.getThroughTable()).isNull();
        assertThat(req.getAppCode()).isNull();
        assertThat(req.getTenantCode()).isNull();
    }
}