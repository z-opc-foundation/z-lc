package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealVerifyResultBO 单元测试
 *
 * @author zifang
 */
class ZLcSealVerifyResultBOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        assertThat(bo).isNotNull();
    }

    @Test
    void shouldSetAndGetStatusCode() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        bo.setStatusCode("200");
        assertThat(bo.getStatusCode()).isEqualTo("200");
    }

    @Test
    void shouldSetAndGetStatusInfo() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        bo.setStatusInfo("验章成功");
        assertThat(bo.getStatusInfo()).isEqualTo("验章成功");
    }

    @Test
    void shouldSetAndGetSuccess() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        bo.setSuccess(true);
        assertThat(bo.isSuccess()).isTrue();
    }

    @Test
    void shouldSetAndGetVerifyResult() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        bo.setVerifyResult("验章通过");
        assertThat(bo.getVerifyResult()).isEqualTo("验章通过");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSealVerifyResultBO bo = new ZLcSealVerifyResultBO();
        assertThat(bo.getStatusCode()).isNull();
        assertThat(bo.getStatusInfo()).isNull();
        assertThat(bo.getVerifyResult()).isNull();
    }
}
