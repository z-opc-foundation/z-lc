package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;

/**
 * 添加签名任务结果 DTO — 蒸馏自 ace-platform-engine {@code AddSignJobResultExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：CA 服务发起签名任务后返回的「扫码参数」或「小程序链接参数」.
 *
 * @author xuhf (distilled by zifang)
 */
public class AddSignJobResultExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 签名数据 id. */
    private String signDataId;

    /** 二维码（base64 编码图片）— linkType=1 时使用. */
    private String qrCode;

    /** 用户唯一 id. */
    private String msspId;

    /** CA 平台的 appid. */
    private String appid;

    /** CA 业务端 URL — 用于验证 appid 和证书，并签名. */
    private String serUrl;

    /** 连接类型 — 1: 二维码 / 2: 微信小程序 url. */
    private String linkType;

    /** 连接地址（小程序 URL — linkType=2 时使用） */
    private String urlLink;

    public String getSignDataId() {
        return signDataId;
    }

    public void setSignDataId(String signDataId) {
        this.signDataId = signDataId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getMsspId() {
        return msspId;
    }

    public void setMsspId(String msspId) {
        this.msspId = msspId;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSerUrl() {
        return serUrl;
    }

    public void setSerUrl(String serUrl) {
        this.serUrl = serUrl;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getUrlLink() {
        return urlLink;
    }

    public void setUrlLink(String urlLink) {
        this.urlLink = urlLink;
    }

    /**
     * 是否二维码扫码模式.
     */
    public boolean isQrCodeMode() {
        return "1".equals(linkType);
    }

    /**
     * 是否小程序 URL 模式.
     */
    public boolean isMiniProgramMode() {
        return "2".equals(linkType);
    }
}
