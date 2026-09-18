package com.zifang.z.lc.common.dto;

import java.io.Serializable;

/**
 * 数据导入扩展参数 — 蒸馏自 ace-platform-core
 * {@code ImportExtend} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>封装数据导入任务的扩展参数: Token、数据周期 ID、成功/失败行数等.
 * 蒸馏时移除了 ace 对 Lombok @Data / Swagger @ApiModelProperty 的依赖,
 * 改为手写 getter/setter.
 *
 * @author zifang
 */
public class ZLcImportExtend implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    /** 数据周期 ID */
    private Long periodId;

    private Long successRows;
    private Long failRows;

    public ZLcImportExtend() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Long getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(Long successRows) {
        this.successRows = successRows;
    }

    public Long getFailRows() {
        return failRows;
    }

    public void setFailRows(Long failRows) {
        this.failRows = failRows;
    }
}
