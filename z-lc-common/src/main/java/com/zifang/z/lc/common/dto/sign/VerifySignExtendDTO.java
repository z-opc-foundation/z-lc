package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;

/**
 * 验签 DTO — 蒸馏自 ace-platform-engine {@code VerifySignExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：CA 签名验证 — 用 {@code signResult} 配合 {@code signCert} 做证书链验证.
 *
 * @author zifang
 */
public class VerifySignExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 签名数据 id. */
    private String signDataId;

    /** Base64 签名结果. */
    private String signResult;

    /** Base64 证书. */
    private String signCert;

    /** 时间戳. */
    private String timeStampValue;

    /** 待签名原文（hash 后） */
    private String originalDataHash;

    public String getSignDataId() {
        return signDataId;
    }

    public void setSignDataId(String signDataId) {
        this.signDataId = signDataId;
    }

    public String getSignResult() {
        return signResult;
    }

    public void setSignResult(String signResult) {
        this.signResult = signResult;
    }

    public String getSignCert() {
        return signCert;
    }

    public void setSignCert(String signCert) {
        this.signCert = signCert;
    }

    public String getTimeStampValue() {
        return timeStampValue;
    }

    public void setTimeStampValue(String timeStampValue) {
        this.timeStampValue = timeStampValue;
    }

    public String getOriginalDataHash() {
        return originalDataHash;
    }

    public void setOriginalDataHash(String originalDataHash) {
        this.originalDataHash = originalDataHash;
    }
}
