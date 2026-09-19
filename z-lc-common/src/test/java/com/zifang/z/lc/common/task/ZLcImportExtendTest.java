package com.zifang.z.lc.common.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcImportExtend (task包版本) 单元测试
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
        extend.setToken("test-token");
        assertThat(extend.getToken()).isEqualTo("test-token");
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
        extend.setSuccessRows(50L);
        assertThat(extend.getSuccessRows()).isEqualTo(50L);
    }

    @Test
    void shouldSetAndGetFailRows() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setFailRows(5L);
        assertThat(extend.getFailRows()).isEqualTo(5L);
    }

    @Test
    void shouldCalculateSuccessRate() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setSuccessRows(80L);
        extend.setFailRows(20L);

        double rate = extend.getSuccessRate();
        assertThat(rate).isEqualTo(0.8);
    }

    @Test
    void shouldReturnZeroSuccessRateForNullValues() {
        ZLcImportExtend extend = new ZLcImportExtend();
        assertThat(extend.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnZeroSuccessRateWhenTotalIsZero() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setSuccessRows(0L);
        extend.setFailRows(0L);

        assertThat(extend.getSuccessRate()).isEqualTo(0.0);
    }

    @Test
    void shouldHandlePartialNullValues() {
        ZLcImportExtend extend = new ZLcImportExtend();
        extend.setSuccessRows(10L);

        double rate = extend.getSuccessRate();
        assertThat(rate).isEqualTo(1.0);
    }
}