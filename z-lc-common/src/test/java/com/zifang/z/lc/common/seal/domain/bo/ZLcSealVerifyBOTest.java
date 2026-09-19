package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealVerifyBO 单元测试
 *
 * @author zifang
 */
class ZLcSealVerifyBOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcSealVerifyBO bo = new ZLcSealVerifyBO();
        assertThat(bo).isNotNull();
    }

    @Test
    void shouldSetAndGetFileUrl() {
        ZLcSealVerifyBO bo = new ZLcSealVerifyBO();
        bo.setFileUrl("https://example.com/file.pdf");
        assertThat(bo.getFileUrl()).isEqualTo("https://example.com/file.pdf");
    }

    @Test
    void shouldSetAndGetBizNo() {
        ZLcSealVerifyBO bo = new ZLcSealVerifyBO();
        bo.setBizNo("biz-001");
        assertThat(bo.getBizNo()).isEqualTo("biz-001");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSealVerifyBO bo = new ZLcSealVerifyBO();
        assertThat(bo.getFileUrl()).isNull();
        assertThat(bo.getBizNo()).isNull();
    }
}
