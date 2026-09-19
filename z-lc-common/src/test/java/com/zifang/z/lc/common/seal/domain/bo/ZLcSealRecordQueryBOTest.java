package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealRecordQueryBO 单元测试
 *
 * @author zifang
 */
class ZLcSealRecordQueryBOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcSealRecordQueryBO bo = new ZLcSealRecordQueryBO();
        assertThat(bo).isNotNull();
    }

    @Test
    void shouldSetAndGetBizNo() {
        ZLcSealRecordQueryBO bo = new ZLcSealRecordQueryBO();
        bo.setBizNo("biz-001");
        assertThat(bo.getBizNo()).isEqualTo("biz-001");
    }

    @Test
    void shouldSetAndGetFileUrl() {
        ZLcSealRecordQueryBO bo = new ZLcSealRecordQueryBO();
        bo.setFileUrl("https://example.com/file.pdf");
        assertThat(bo.getFileUrl()).isEqualTo("https://example.com/file.pdf");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcSealRecordQueryBO bo = new ZLcSealRecordQueryBO();
        assertThat(bo.getBizNo()).isNull();
        assertThat(bo.getFileUrl()).isNull();
    }
}
