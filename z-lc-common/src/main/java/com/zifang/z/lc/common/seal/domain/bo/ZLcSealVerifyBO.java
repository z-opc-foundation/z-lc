package com.zifang.z.lc.common.seal.domain.bo;

/**
 * 验章入参 BO — 蒸馏自 ace-platform-core
 * {@code SealVerifyBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章验章"模块 — 按业务编号 + 文件 URL 发起验章.
 *
 * @author zifang
 */
public class ZLcSealVerifyBO {

    private String fileUrl;
    private String bizNo;

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
}