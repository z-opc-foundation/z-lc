package com.zifang.z.lc.common.seal.domain.bo;

import java.time.LocalDateTime;

/**
 * 签章记录业务对象 BO — 蒸馏自 ace-platform-core
 * {@code SealRecordBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章"模块中签章记录的运行时传输.
 * 比 {@code SealRecordDO} 字段略简 (业务字段, 不含审计字段).
 *
 * @author zifang
 */
public class ZLcSealRecordBO {

    private Long id;
    private String bizNo;
    private String fileName;
    private String fileVersion;
    private String sourceFileUrl;
    private String signedFileUrl;
    private String sealType;
    private String sealName;
    private String signStatus;
    private Long signUserId;
    private LocalDateTime signTime;
    private String signFailReason;
    private String verifyStatus;
    private Long verifyUserId;
    private LocalDateTime verifyTime;
    private String verifyFailReason;
    private Long orgId;
    private String remark;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModify;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileVersion() { return fileVersion; }
    public void setFileVersion(String fileVersion) { this.fileVersion = fileVersion; }

    public String getSourceFileUrl() { return sourceFileUrl; }
    public void setSourceFileUrl(String sourceFileUrl) { this.sourceFileUrl = sourceFileUrl; }

    public String getSignedFileUrl() { return signedFileUrl; }
    public void setSignedFileUrl(String signedFileUrl) { this.signedFileUrl = signedFileUrl; }

    public String getSealType() { return sealType; }
    public void setSealType(String sealType) { this.sealType = sealType; }

    public String getSealName() { return sealName; }
    public void setSealName(String sealName) { this.sealName = sealName; }

    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String signStatus) { this.signStatus = signStatus; }

    public Long getSignUserId() { return signUserId; }
    public void setSignUserId(Long signUserId) { this.signUserId = signUserId; }

    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }

    public String getSignFailReason() { return signFailReason; }
    public void setSignFailReason(String signFailReason) { this.signFailReason = signFailReason; }

    public String getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(String verifyStatus) { this.verifyStatus = verifyStatus; }

    public Long getVerifyUserId() { return verifyUserId; }
    public void setVerifyUserId(Long verifyUserId) { this.verifyUserId = verifyUserId; }

    public LocalDateTime getVerifyTime() { return verifyTime; }
    public void setVerifyTime(LocalDateTime verifyTime) { this.verifyTime = verifyTime; }

    public String getVerifyFailReason() { return verifyFailReason; }
    public void setVerifyFailReason(String verifyFailReason) { this.verifyFailReason = verifyFailReason; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModify() { return gmtModify; }
    public void setGmtModify(LocalDateTime gmtModify) { this.gmtModify = gmtModify; }
}