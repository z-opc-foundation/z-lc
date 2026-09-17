package com.zifang.z.lc.common.seal.domain.bo;

/**
 * 验章操作结果 BO — 蒸馏自 ace-platform-core
 * {@code SealVerifyResultBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章验章"模块 — 验章接口返回值.
 *
 * @author zifang
 */
public class ZLcSealVerifyResultBO {

    private String statusCode;
    private String statusInfo;
    private boolean success;
    private String verifyResult;

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getStatusInfo() { return statusInfo; }
    public void setStatusInfo(String statusInfo) { this.statusInfo = statusInfo; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getVerifyResult() { return verifyResult; }
    public void setVerifyResult(String verifyResult) { this.verifyResult = verifyResult; }
}