package com.zifang.z.lc.common.seal.domain.bo;

/**
 * 签章记录查询 BO — 蒸馏自 ace-platform-core
 * {@code SealRecordQueryBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章"模块 — 按业务编号 / 文件 URL 查询签章记录的入参.
 *
 * @author zifang
 */
public class ZLcSealRecordQueryBO {

    private String bizNo;
    private String fileUrl;

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}