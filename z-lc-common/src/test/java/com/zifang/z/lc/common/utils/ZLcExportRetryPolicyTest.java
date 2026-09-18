package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExportRetryPolicy 单元测试
 *
 * @author zifang
 */
class ZLcExportRetryPolicyTest {

    @Test
    void halvePageSize_shouldReturnHalf_WhenGreaterThanMin() {
        long result = ZLcExportRetryPolicy.halvePageSize(1000L);
        assertThat(result).isEqualTo(500L);
    }

    @Test
    void halvePageSize_shouldReturnMinPageSize_WhenAtMin() {
        long result = ZLcExportRetryPolicy.halvePageSize(ZLcExportRetryPolicy.MIN_PAGE_SIZE);
        assertThat(result).isEqualTo(ZLcExportRetryPolicy.MIN_PAGE_SIZE);
    }

    @Test
    void halvePageSize_shouldReturnMinPageSize_WhenBelowMin() {
        long result = ZLcExportRetryPolicy.halvePageSize(100L);
        assertThat(result).isEqualTo(ZLcExportRetryPolicy.MIN_PAGE_SIZE);
    }

    @Test
    void recoverPageSizeIfNeeded_shouldReturnOriginal_WhenSuccessPagesLessThanThreshold() {
        long result = ZLcExportRetryPolicy.recoverPageSizeIfNeeded(200L, 2000L, 1);
        assertThat(result).isEqualTo(200L);
    }

    @Test
    void recoverPageSizeIfNeeded_shouldReturnDoubled_WhenSuccessPagesReachThreshold() {
        long result = ZLcExportRetryPolicy.recoverPageSizeIfNeeded(200L, 2000L, 2);
        assertThat(result).isEqualTo(400L);
    }

    @Test
    void recoverPageSizeIfNeeded_shouldNotExceedOriginal_WhenRecovering() {
        long result = ZLcExportRetryPolicy.recoverPageSizeIfNeeded(1500L, 2000L, 2);
        assertThat(result).isLessThanOrEqualTo(2000L);
    }

    @Test
    void shouldSuspend_shouldReturnTrue_WhenRetryCountExceedsMax() {
        boolean result = ZLcExportRetryPolicy.shouldSuspend(4);
        assertThat(result).isTrue();
    }

    @Test
    void shouldSuspend_shouldReturnFalse_WhenRetryCountBelowMax() {
        boolean result = ZLcExportRetryPolicy.shouldSuspend(2);
        assertThat(result).isFalse();
    }

    @Test
    void shouldFailPermanently_shouldReturnTrue_WhenResumeRoundExceedsMax() {
        boolean result = ZLcExportRetryPolicy.shouldFailPermanently(2);
        assertThat(result).isTrue();
    }

    @Test
    void shouldFailPermanently_shouldReturnFalse_WhenResumeRoundBelowMax() {
        boolean result = ZLcExportRetryPolicy.shouldFailPermanently(0);
        assertThat(result).isFalse();
    }

    @Test
    void constants_shouldHaveCorrectValues() {
        assertThat(ZLcExportRetryPolicy.MIN_PAGE_SIZE).isEqualTo(200L);
        assertThat(ZLcExportRetryPolicy.MAX_PAGE_RETRIES).isEqualTo(3);
        assertThat(ZLcExportRetryPolicy.RECOVERY_SUCCESS_PAGES).isEqualTo(2);
        assertThat(ZLcExportRetryPolicy.MAX_RESUME_ROUNDS).isEqualTo(1);
        assertThat(ZLcExportRetryPolicy.RETRY_WAIT_MS).isEqualTo(10_000L);
    }
}
