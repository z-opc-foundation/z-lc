package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RelationUpdateReq 单元测试
 *
 * @author zifang
 */
class RelationUpdateReqTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        RelationUpdateReq req = new RelationUpdateReq();
        assertThat(req).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setId(123L);
        assertThat(req.getId()).isEqualTo(123L);
    }

    @Test
    void shouldSetAndGetRelationName() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setRelationName("关系名称");
        assertThat(req.getRelationName()).isEqualTo("关系名称");
    }

    @Test
    void shouldSetAndGetRelationType() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setRelationType("ONE_TO_MANY");
        assertThat(req.getRelationType()).isEqualTo("ONE_TO_MANY");
    }

    @Test
    void shouldSetAndGetSourceFieldCode() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setSourceFieldCode("field-001");
        assertThat(req.getSourceFieldCode()).isEqualTo("field-001");
    }

    @Test
    void shouldSetAndGetThroughTable() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setThroughTable("through_table");
        assertThat(req.getThroughTable()).isEqualTo("through_table");
    }

    @Test
    void shouldHandleNullValues() {
        RelationUpdateReq req = new RelationUpdateReq();
        assertThat(req.getId()).isNull();
        assertThat(req.getRelationName()).isNull();
        assertThat(req.getRelationType()).isNull();
        assertThat(req.getSourceFieldCode()).isNull();
        assertThat(req.getThroughTable()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        RelationUpdateReq req = new RelationUpdateReq();
        assertThat(req).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetEmptyStrings() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setRelationName("");
        req.setRelationType("");
        req.setSourceFieldCode("");
        req.setThroughTable("");
        
        assertThat(req.getRelationName()).isEmpty();
        assertThat(req.getRelationType()).isEmpty();
        assertThat(req.getSourceFieldCode()).isEmpty();
        assertThat(req.getThroughTable()).isEmpty();
    }

    @Test
    void shouldSetNullStrings() {
        RelationUpdateReq req = new RelationUpdateReq();
        req.setRelationName(null);
        req.setRelationType(null);
        req.setSourceFieldCode(null);
        req.setThroughTable(null);
        
        assertThat(req.getRelationName()).isNull();
        assertThat(req.getRelationType()).isNull();
        assertThat(req.getSourceFieldCode()).isNull();
        assertThat(req.getThroughTable()).isNull();
    }
}