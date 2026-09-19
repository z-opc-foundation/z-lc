package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealRecordBO 单元测试
 */
class ZLcSealRecordBOTest {

    @Test
    void shouldCreateEmpty() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        assertThat(bo).isNotNull();
        assertThat(bo.getId()).isNull();
        assertThat(bo.getBizNo()).isNull();
        assertThat(bo.getFileName()).isNull();
    }

    @Test
    void shouldSetAndGetId() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setId(1L);
        assertThat(bo.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetBizNo() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setBizNo("BIZ-001");
        assertThat(bo.getBizNo()).isEqualTo("BIZ-001");
    }

    @Test
    void shouldSetAndGetFileInfo() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setFileName("file.pdf");
        bo.setFileVersion("v1");
        bo.setSourceFileUrl("https://src");
        bo.setSignedFileUrl("https://signed");
        assertThat(bo.getFileName()).isEqualTo("file.pdf");
        assertThat(bo.getFileVersion()).isEqualTo("v1");
        assertThat(bo.getSourceFileUrl()).isEqualTo("https://src");
        assertThat(bo.getSignedFileUrl()).isEqualTo("https://signed");
    }

    @Test
    void shouldSetAndGetSealInfo() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setSealType("ACROSS");
        bo.setSealName("Official");
        assertThat(bo.getSealType()).isEqualTo("ACROSS");
        assertThat(bo.getSealName()).isEqualTo("Official");
    }

    @Test
    void shouldSetAndGetSignInfo() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setSignStatus("SUCCESS");
        bo.setSignUserId(100L);
        bo.setSignTime(LocalDateTime.now());
        bo.setSignFailReason("oops");
        assertThat(bo.getSignStatus()).isEqualTo("SUCCESS");
        assertThat(bo.getSignUserId()).isEqualTo(100L);
        assertThat(bo.getSignTime()).isNotNull();
        assertThat(bo.getSignFailReason()).isEqualTo("oops");
    }

    @Test
    void shouldSetAndGetVerifyInfo() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setVerifyStatus("FAIL");
        bo.setVerifyUserId(200L);
        bo.setVerifyTime(LocalDateTime.now());
        bo.setVerifyFailReason("bad sig");
        assertThat(bo.getVerifyStatus()).isEqualTo("FAIL");
        assertThat(bo.getVerifyUserId()).isEqualTo(200L);
        assertThat(bo.getVerifyTime()).isNotNull();
        assertThat(bo.getVerifyFailReason()).isEqualTo("bad sig");
    }

    @Test
    void shouldSetAndGetOrgAndRemark() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        bo.setOrgId(100L);
        bo.setRemark("Test");
        assertThat(bo.getOrgId()).isEqualTo(100L);
        assertThat(bo.getRemark()).isEqualTo("Test");
    }

    @Test
    void shouldSetAndGetTimes() {
        ZLcSealRecordBO bo = new ZLcSealRecordBO();
        LocalDateTime now = LocalDateTime.now();
        bo.setGmtCreate(now);
        bo.setGmtModify(now);
        assertThat(bo.getGmtCreate()).isEqualTo(now);
        assertThat(bo.getGmtModify()).isEqualTo(now);
    }
}