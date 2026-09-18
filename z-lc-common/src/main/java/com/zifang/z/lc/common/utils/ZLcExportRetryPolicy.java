package com.zifang.z.lc.common.utils;

/**
 * 导出分页重试与逐步回升策略 — 蒸馏自 ace-platform-core
 * {@code ExportRetryPolicy} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>纯逻辑工具类, 不依赖任何外部框架, 便于单测.
 * 定义数据导出时的分页大小调整、重试次数、挂起判断等策略常量与方法.
 *
 * <p>典型场景：
 * <ul>
 *   <li>大数据量导出时的动态分页大小调整</li>
 *   <li>导出失败时的重试与挂起续导决策</li>
 *   <li>连续成功后的分页大小回升</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcExportRetryPolicy {

    /** 最小分页大小. */
    public static final long MIN_PAGE_SIZE = 200L;

    /** 单逻辑页最大重试次数. */
    public static final int MAX_PAGE_RETRIES = 3;

    /** 触发分页大小回升的连续成功页数阈值. */
    public static final int RECOVERY_SUCCESS_PAGES = 2;

    /** 最大续导轮次. */
    public static final int MAX_RESUME_ROUNDS = 1;

    /** 重试等待时间 (毫秒). */
    public static final long RETRY_WAIT_MS = 10_000L;

    /** count 查询最大重试次数. */
    public static final int COUNT_QUERY_MAX_RETRY = 2;

    private ZLcExportRetryPolicy() {
    }

    /**
     * 将当前分页大小减半 (不低于最小分页大小).
     *
     * @param currentPageSize 当前分页大小
     * @return 减半后的分页大小
     */
    public static long halvePageSize(long currentPageSize) {
        return Math.max(currentPageSize / 2, MIN_PAGE_SIZE);
    }

    /**
     * 连续成功足够页数后翻倍分页, 上限为初始分页.
     *
     * @param currentPageSize        当前分页大小
     * @param initialPageSize        初始分页大小
     * @param consecutiveSuccessPages 连续成功页数
     * @return 新的 currentPageSize; 若未触发回升则返回原值
     */
    public static long recoverPageSizeIfNeeded(long currentPageSize, long initialPageSize,
                                               int consecutiveSuccessPages) {
        if (consecutiveSuccessPages >= RECOVERY_SUCCESS_PAGES) {
            return Math.min(currentPageSize * 2, initialPageSize);
        }
        return currentPageSize;
    }

    /**
     * 判断是否应挂起任务 (重试次数耗尽).
     *
     * @param pageRetryCount 当前逻辑页已重试次数
     * @return 是否应挂起
     */
    public static boolean shouldSuspend(int pageRetryCount) {
        return pageRetryCount >= MAX_PAGE_RETRIES;
    }

    /**
     * 判断是否应永久失败 (续导轮次耗尽).
     *
     * @param resumeRoundCount 当前续导轮次
     * @return 是否应永久失败
     */
    public static boolean shouldFailPermanently(int resumeRoundCount) {
        return resumeRoundCount >= MAX_RESUME_ROUNDS;
    }
}
