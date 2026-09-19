package com.zifang.z.lc.common.seal.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealRecordDO 单元测试
 */
class ZLcSealRecordDOTest {

    @Test
    void shouldCreateEmpty() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getBizNo()).isNull();
        assertThat(entity.getFileName()).isNull();
        assertThat(entity.getSignStatus()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        assertThat(entity).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetAndGetId() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setId(1L);
        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetBizNo() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setBizNo("BIZ-001");
        assertThat(entity.getBizNo()).isEqualTo("BIZ-001");
    }

    @Test
    void shouldSetAndGetFileInfo() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setFileName("contract.pdf");
        entity.setFileVersion("v1.0");
        entity.setSourceFileUrl("https://example.com/source.pdf");
        entity.setSignedFileUrl("https://example.com/signed.pdf");

        assertThat(entity.getFileName()).isEqualTo("contract.pdf");
        assertThat(entity.getFileVersion()).isEqualTo("v1.0");
        assertThat(entity.getSourceFileUrl()).isEqualTo("https://example.com/source.pdf");
        assertThat(entity.getSignedFileUrl()).isEqualTo("https://example.com/signed.pdf");
    }

    @Test
    void shouldSetAndGetSealInfo() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setSealType("ACROSS");
        entity.setSealName("Official Seal");

        assertThat(entity.getSealType()).isEqualTo("ACROSS");
        assertThat(entity.getSealName()).isEqualTo("Official Seal");
    }

    @Test
    void shouldSetAndGetSignStatus() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setSignStatus("SUCCESS");
        entity.setSignUserId(100L);
        entity.setSignTime(LocalDateTime.now());
        entity.setSignFailReason(null);

        assertThat(entity.getSignStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getSignUserId()).isEqualTo(100L);
        assertThat(entity.getSignTime()).isNotNull();
        assertThat(entity.getSignFailReason()).isNull();
    }

    @Test
    void shouldSetAndGetVerifyStatus() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setVerifyStatus("FAIL");
        entity.setVerifyUserId(200L);
        entity.setVerifyTime(LocalDateTime.now());
        entity.setVerifyFailReason("Invalid signature");

        assertThat(entity.getVerifyStatus()).isEqualTo("FAIL");
        assertThat(entity.getVerifyUserId()).isEqualTo(200L);
        assertThat(entity.getVerifyTime()).isNotNull();
        assertThat(entity.getVerifyFailReason()).isEqualTo("Invalid signature");
    }

    @Test
    void shouldSetAndGetAuditInfo() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setCreateBy(1L);
        entity.setCreateOrgId(10L);
        entity.setGmtCreate(LocalDateTime.now());
        entity.setGmtModify(LocalDateTime.now());
        entity.setModifyBy(2L);
        entity.setModifyOrgId(20L);
        entity.setOrgId(100L);

        assertThat(entity.getCreateBy()).isEqualTo(1L);
        assertThat(entity.getCreateOrgId()).isEqualTo(10L);
        assertThat(entity.getGmtCreate()).isNotNull();
        assertThat(entity.getGmtModify()).isNotNull();
        assertThat(entity.getModifyBy()).isEqualTo(2L);
        assertThat(entity.getModifyOrgId()).isEqualTo(20L);
        assertThat(entity.getOrgId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetExtendAndRemark() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setExtend("{\"k\":\"v\"}");
        entity.setRemark("Test remark");
        assertThat(entity.getExtend()).isEqualTo("{\"k\":\"v\"}");
        assertThat(entity.getRemark()).isEqualTo("Test remark");
    }

    @Test
    void shouldSetAndGetDeleted() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        entity.setDeleted(0);
        assertThat(entity.isDeleted()).isFalse();
        entity.setDeleted(1);
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void isDeletedShouldReturnFalseWhenNull() {
        ZLcSealRecordDO entity = new ZLcSealRecordDO();
        assertThat(entity.isDeleted()).isFalse();
    }
}