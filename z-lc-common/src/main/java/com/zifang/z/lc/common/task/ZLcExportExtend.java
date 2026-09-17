package com.zifang.z.lc.common.task;

/**
 * 数据导出扩展字段 DTO — 蒸馏自 ace-platform-core
 * {@code ExportExtend} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>用于低代码平台"数据导出"任务 — 导出过程中的运行时扩展字段:
 *
 * <ul>
 *   <li>分页控制 — initialPageSize / currentPageSize / consecutiveSuccessPages</li>
 *   <li>断点续导 — checkpointLastPk / nextOffset / resumeRoundCount / pageRetryCount</li>
 *   <li>上下文 — token / deptId / batchSize / onlyRoot / localFilePath</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcExportExtend {

    private String token;
    private Long deptId;
    private Integer batchSize;
    private boolean onlyRoot = false;

    /** 本地半成品 Excel 路径. */
    private String localFilePath;

    /** 初始分页大小. */
    private Integer initialPageSize;

    /** 当前实际分页大小. */
    private Integer currentPageSize;

    /** 连续成功页数 (用于逐步回升). */
    private Integer consecutiveSuccessPages;

    /** 当前逻辑页已重试次数. */
    private Integer pageRetryCount;

    /** 任务级挂起续导轮次. */
    private Integer resumeRoundCount;

    /** HDOS keyset 续导游标 (上一批最后一条主键值). */
    private String checkpointLastPk;

    /** offset 分页续导位置. */
    private Long nextOffset;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public boolean isOnlyRoot() { return onlyRoot; }
    public void setOnlyRoot(boolean onlyRoot) { this.onlyRoot = onlyRoot; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public Integer getInitialPageSize() { return initialPageSize; }
    public void setInitialPageSize(Integer initialPageSize) { this.initialPageSize = initialPageSize; }

    public Integer getCurrentPageSize() { return currentPageSize; }
    public void setCurrentPageSize(Integer currentPageSize) { this.currentPageSize = currentPageSize; }

    public Integer getConsecutiveSuccessPages() { return consecutiveSuccessPages; }
    public void setConsecutiveSuccessPages(Integer consecutiveSuccessPages) { this.consecutiveSuccessPages = consecutiveSuccessPages; }

    public Integer getPageRetryCount() { return pageRetryCount; }
    public void setPageRetryCount(Integer pageRetryCount) { this.pageRetryCount = pageRetryCount; }

    public Integer getResumeRoundCount() { return resumeRoundCount; }
    public void setResumeRoundCount(Integer resumeRoundCount) { this.resumeRoundCount = resumeRoundCount; }

    public String getCheckpointLastPk() { return checkpointLastPk; }
    public void setCheckpointLastPk(String checkpointLastPk) { this.checkpointLastPk = checkpointLastPk; }

    public Long getNextOffset() { return nextOffset; }
    public void setNextOffset(Long nextOffset) { this.nextOffset = nextOffset; }
}