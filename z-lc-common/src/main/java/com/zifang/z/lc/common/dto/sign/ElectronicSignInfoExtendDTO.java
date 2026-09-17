package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;

/**
 * 电子签名信息 DTO — 蒸馏自 ace-platform-engine {@code ElectronicSignInfoExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：CA 电子签名服务返回的签名结果 — 签名数据 id、时间戳、签名结果、
 * 证书、印章图片、印章密码等. jobStatus 字段描述签名任务状态：
 * <ul>
 *   <li>{@code UNSIGN} — 待签</li>
 *   <li>{@code FINISH} — 已签</li>
 *   <li>{@code EXPIRE} — 过期</li>
 *   <li>{@code REVOKE} — 服务端撤销</li>
 *   <li>{@code REFUSE} — 客户端拒绝</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public class ElectronicSignInfoExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 签名数据 id. */
    private String signDataId;

    /** 时间戳. */
    private String timeStampValue;

    /** Base64 格式签名结果. */
    private String signResult;

    /** Base64 格式证书. */
    private String signCert;

    /** 状态（UNSIGN / FINISH / EXPIRE / REVOKE / REFUSE）. */
    private String jobStatus;

    /** 用户编号 id. */
    private String msspId;

    /** 印章图片 base64. */
    private String signImage;

    /** 印章密码. */
    private String signPassword;

    public String getSignDataId() {
        return signDataId;
    }

    public void setSignDataId(String signDataId) {
        this.signDataId = signDataId;
    }

    public String getTimeStampValue() {
        return timeStampValue;
    }

    public void setTimeStampValue(String timeStampValue) {
        this.timeStampValue = timeStampValue;
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

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getMsspId() {
        return msspId;
    }

    public void setMsspId(String msspId) {
        this.msspId = msspId;
    }

    public String getSignImage() {
        return signImage;
    }

    public void setSignImage(String signImage) {
        this.signImage = signImage;
    }

    public String getSignPassword() {
        return signPassword;
    }

    public void setSignPassword(String signPassword) {
        this.signPassword = signPassword;
    }

    public boolean isSigned() {
        return "FINISH".equalsIgnoreCase(jobStatus);
    }
}
