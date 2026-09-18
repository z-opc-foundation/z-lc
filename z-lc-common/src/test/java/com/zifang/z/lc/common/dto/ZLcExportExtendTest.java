package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExportExtend 单元测试
 *
 * @author zifang
 */
class ZLcExportExtendTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcExportExtend extend = new ZLcExportExtend();
        assertThat(extend).isNotNull();
    }

    @Test
    void shouldSetAndGetToken() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setToken("token-456");
        assertThat(extend.getToken()).isEqualTo("token-456");
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
        extend.setBatchSize(200);
        assertThat(extend.getBatchSize()).isEqualTo(200);
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
    void shouldSetAndGetPageRetryCount() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setPageRetryCount(3);
        assertThat(extend.getPageRetryCount()).isEqualTo(3);
    }

    @Test
    void shouldSetAndGetCheckpointLastPk() {
        ZLcExportExtend extend = new ZLcExportExtend();
        extend.setCheckpointLastPk("pk-123");
        assertThat(extend.getCheckpointLastPk()).isEqualTo("pk-123");
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

    @Test
    void shouldImplementSerializable() {
        ZLcExportExtend extend = new ZLcExportExtend();
        assertThat(extend).isInstanceOf(java.io.Serializable.class);
    }
}
