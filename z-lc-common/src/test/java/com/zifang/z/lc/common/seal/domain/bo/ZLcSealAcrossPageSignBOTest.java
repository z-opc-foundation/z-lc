package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealAcrossPageSignBO 单元测试
 */
class ZLcSealAcrossPageSignBOTest {

    @Test
    void shouldCreateEmpty() {
        ZLcSealAcrossPageSignBO bo = new ZLcSealAcrossPageSignBO();
        assertThat(bo).isNotNull();
        assertThat(bo.getFileUrl()).isNull();
        assertThat(bo.getBizNo()).isNull();
    }

    @Test
    void shouldSetAndGetFileUrl() {
        ZLcSealAcrossPageSignBO bo = new ZLcSealAcrossPageSignBO();
        bo.setFileUrl("https://example.com/file.pdf");
        assertThat(bo.getFileUrl()).isEqualTo("https://example.com/file.pdf");
    }

    @Test
    void shouldSetAndGetBizNo() {
        ZLcSealAcrossPageSignBO bo = new ZLcSealAcrossPageSignBO();
        bo.setBizNo("BIZ-001");
        assertThat(bo.getBizNo()).isEqualTo("BIZ-001");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSealAcrossPageSignBO bo = new ZLcSealAcrossPageSignBO();
        bo.setFileUrl(null);
        bo.setBizNo(null);
        assertThat(bo.getFileUrl()).isNull();
        assertThat(bo.getBizNo()).isNull();
    }
}