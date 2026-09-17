package com.zifang.z.lc.common.task;

/**
 * 数据导入扩展字段 DTO — 蒸馏自 ace-platform-core
 * {@code ImportExtend} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>用于低代码平台"数据导入"任务 — 导入过程中的运行时扩展字段:
 *
 * <ul>
 *   <li>{@code token} — 鉴权 token</li>
 *   <li>{@code periodId} — 数据周期 ID</li>
 *   <li>{@code successRows} — 累计成功行数</li>
 *   <li>{@code failRows} — 累计失败行数</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcImportExtend {

    private String token;
    private Long periodId;
    private Long successRows;
    private Long failRows;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getSuccessRows() { return successRows; }
    public void setSuccessRows(Long successRows) { this.successRows = successRows; }

    public Long getFailRows() { return failRows; }
    public void setFailRows(Long failRows) { this.failRows = failRows; }

    /** 成功率 (成功 / (成功 + 失败)). */
    public double getSuccessRate() {
        long total = (successRows == null ? 0 : successRows) + (failRows == null ? 0 : failRows);
        if (total == 0) {
            return 0.0;
        }
        return (successRows == null ? 0 : successRows) * 1.0 / total;
    }
}