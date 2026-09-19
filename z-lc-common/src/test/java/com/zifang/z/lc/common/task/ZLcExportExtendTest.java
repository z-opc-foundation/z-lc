package com.zifang.z.lc.common.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExportExtend (task包版本) 单元测试
 *
 * @author zifang
 */
class ZLcExportExtendTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcExportExtend extend = new ZLcExportExtend();
        assertThat(extend).isNotNull();
        assertThat(extend.isOnlyRoot()).isFalse();
    }

    @Test
    void shouldSetAndGetToken() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setToken("test-token");
        assertThat(extend.getToken()).isEqualTo("test-token");
    }

    @Test
    void shouldSetAndGetDeptId() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setDeptId(100L);
        assertThat(extend.getDeptId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetBatchSize() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setBatchSize(500);
        assertThat(extend.getBatchSize()).isEqualTo(500);
    }

    @Test
    void shouldSetAndGetOnlyRoot() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setOnlyRoot(true);
        assertThat(extend.isOnlyRoot()).isTrue();
    }

    @Test
    void shouldSetAndGetLocalFilePath() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setLocalFilePath("/tmp/export.xlsx");
        assertThat(extend.getLocalFilePath()).isEqualTo("/tmp/export.xlsx");
    }

    @Test
    void shouldSetAndGetInitialPageSize() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setInitialPageSize(1000);
        assertThat(extend.getInitialPageSize()).isEqualTo(1000);
    }

    @Test
    void shouldSetAndGetCurrentPageSize() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setCurrentPageSize(500);
        assertThat(extend.getCurrentPageSize()).isEqualTo(500);
    }

    @Test
    void shouldSetAndGetConsecutiveSuccessPages() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setConsecutiveSuccessPages(3);
        assertThat(extend.getConsecutiveSuccessPages()).isEqualTo(3);
    }

    @Test
    void shouldSetAndGetPageRetryCount() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setPageRetryCount(2);
        assertThat(extend.getPageRetryCount()).isEqualTo(2);
    }

    @Test
    void shouldSetAndGetResumeRoundCount() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setResumeRoundCount(1);
        assertThat(extend.getResumeRoundCount()).isEqualTo(1);
    }

    @Test
    void shouldSetAndGetCheckpointLastPk() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setCheckpointLastPk("last-pk");
        assertThat(extend.getCheckpointLastPk()).isEqualTo("last-pk");
    }

    @Test
    void shouldSetAndGetNextOffset() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setNextOffset(1000L);
        assertThat(extend.getNextOffset()).isEqualTo(1000L);
    }

    @Test
    void shouldHandleNullValues() {
        ZLcExportExtend extend = new ZLcExportExtend();
        assertThat(extend.getToken()).isNull();
        assertThat(extend.getDeptId()).isNull();
        assertThat(extend.getBatchSize()).isNull();
        assertThat(extend.getLocalFilePath()).isNull();
    }
}