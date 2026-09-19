package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealSignResultBO 单元测试
 *
 * @author zifang
 */
class ZLcSealSignResultBOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        assertThat(bo).isNotNull();
    }

    @Test
    void shouldSetAndGetStatusCode() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setStatusCode("200");
        assertThat(bo.getStatusCode()).isEqualTo("200");
    }

    @Test
    void shouldSetAndGetStatusInfo() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setStatusInfo("签章成功");
        assertThat(bo.getStatusInfo()).isEqualTo("签章成功");
    }

    @Test
    void shouldSetAndGetSuccess() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setSuccess(true);
        assertThat(bo.isSuccess()).isTrue();
    }

    @Test
    void shouldSetAndGetOriginalFileUrl() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setOriginalFileUrl("https://example.com/original.pdf");
        assertThat(bo.getOriginalFileUrl()).isEqualTo("https://example.com/original.pdf");
    }

    @Test
    void shouldSetAndGetSignedFileUrl() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setSignedFileUrl("https://example.com/signed.pdf");
        assertThat(bo.getSignedFileUrl()).isEqualTo("https://example.com/signed.pdf");
    }

    @Test
    void shouldSetAndGetPdfSize() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setPdfSize(1024);
        assertThat(bo.getPdfSize()).isEqualTo(1024);
    }

    @Test
    void shouldSetAndGetFileName() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setFileName("document.pdf");
        assertThat(bo.getFileName()).isEqualTo("document.pdf");
    }

    @Test
    void shouldSetAndGetSourceFileType() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setSourceFileType("PDF");
        assertThat(bo.getSourceFileType()).isEqualTo("PDF");
    }

    @Test
    void shouldSetAndGetSignMode() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setSignMode("ACROSS");
        assertThat(bo.getSignMode()).isEqualTo("ACROSS");
    }

    @Test
    void shouldSetAndGetPageCount() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        bo.setPageCount(5);
        assertThat(bo.getPageCount()).isEqualTo(5);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSealSignResultBO bo = new ZLcSealSignResultBO();
        assertThat(bo.getStatusCode()).isNull();
        assertThat(bo.getStatusInfo()).isNull();
        assertThat(bo.getOriginalFileUrl()).isNull();
        assertThat(bo.getSignedFileUrl()).isNull();
        assertThat(bo.getFileName()).isNull();
        assertThat(bo.getSourceFileType()).isNull();
        assertThat(bo.getSignMode()).isNull();
        assertThat(bo.getPageCount()).isNull();
    }
}
