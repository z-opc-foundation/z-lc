package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcImportExtend 单元测试
 *
 * @author zifang
 */
class ZLcImportExtendTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcImportExtend extend = new ZLcImportExtend();
        assertThat(extend).isNotNull();
    }

    @Test
    void shouldSetAndGetToken() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setToken("token-123");
        assertThat(extend.getToken()).isEqualTo("token-123");
    }

    @Test
    void shouldSetAndGetPeriodId() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setPeriodId(100L);
        assertThat(extend.getPeriodId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetSuccessRows() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setSuccessRows(95L);
        assertThat(extend.getSuccessRows()).isEqualTo(95L);
    }

    @Test
    void shouldSetAndGetFailRows() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setFailRows(5L);
        assertThat(extend.getFailRows()).isEqualTo(5L);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcImportExtend extend = new ZLcImportExtend();
        assertThat(extend.getToken()).isNull();
        assertThat(extend.getPeriodId()).isNull();
        assertThat(extend.getSuccessRows()).isNull();
        assertThat(extend.getFailRows()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcImportExtend extend = new ZLcImportExtend();
        assertThat(extend).isInstanceOf(java.io.Serializable.class);
    }
}
