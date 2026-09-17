package com.zifang.z.lc.common.seal.domain.bo;

/**
 * 骑缝签章入参 BO — 蒸馏自 ace-platform-core
 * {@code SealAcrossPageSignBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章"模块 — 跨页 (骑缝章) 签章接口入参.
 *
 * @author zifang
 */
public class ZLcSealAcrossPageSignBO {

    private String fileUrl;
    private String bizNo;

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
}