package com.zifang.z.lc.common.seal.domain.bo;

/**
 * 签章操作结果 BO — 蒸馏自 ace-platform-core
 * {@code SealSignResultBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章"模块 — 签章接口返回值.
 *
 * @author zifang
 */
public class ZLcSealSignResultBO {

    private String statusCode;
    private String statusInfo;
    private boolean success;
    private String originalFileUrl;
    private String signedFileUrl;
    private int pdfSize;
    private String fileName;
    private String sourceFileType;
    private String signMode;
    private Integer pageCount;

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getStatusInfo() { return statusInfo; }
    public void setStatusInfo(String statusInfo) { this.statusInfo = statusInfo; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getOriginalFileUrl() { return originalFileUrl; }
    public void setOriginalFileUrl(String originalFileUrl) { this.originalFileUrl = originalFileUrl; }

    public String getSignedFileUrl() { return signedFileUrl; }
    public void setSignedFileUrl(String signedFileUrl) { this.signedFileUrl = signedFileUrl; }

    public int getPdfSize() { return pdfSize; }
    public void setPdfSize(int pdfSize) { this.pdfSize = pdfSize; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getSourceFileType() { return sourceFileType; }
    public void setSourceFileType(String sourceFileType) { this.sourceFileType = sourceFileType; }

    public String getSignMode() { return signMode; }
    public void setSignMode(String signMode) { this.signMode = signMode; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
}