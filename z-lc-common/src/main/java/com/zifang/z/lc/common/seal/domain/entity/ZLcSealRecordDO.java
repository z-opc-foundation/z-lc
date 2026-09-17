package com.zifang.z.lc.common.seal.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CA 签章记录 DO — 蒸馏自 ace-platform-core
 * {@code SealRecordDO} ({@code com.c2f.ace.core.seal.domain.entity}).
 *
 * <p>用于低代码平台"电子签章"模块 — 签章记录持久化对象 (ca_seal_record 表).
 *
 * <p>蒸馏说明: ace 原版用 MyBatis Plus 注解 (@TableName/@TableId/@TableLogic/@TableField),
 * 蒸馏版只保留字段, 不依赖 MyBatis Plus. 业务方在使用时自行添加 ORM 注解.
 *
 * @author zifang
 */
public class ZLcSealRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键. */
    private Long id;

    /** 业务编号 (业务方系统的流水号). */
    private String bizNo;

    /** 文件名. */
    private String fileName;

    /** 文件版本. */
    private String fileVersion;

    /** 原始文件 URL. */
    private String sourceFileUrl;

    /** 签章后文件 URL. */
    private String signedFileUrl;

    /** 印章类型 (ACROSS/OFFICIAL). */
    private String sealType;

    /** 印章名称. */
    private String sealName;

    /** 签章状态 (SIGNING/SUCCESS/FAIL/INVALID). */
    private String signStatus;

    /** 签章用户 ID. */
    private Long signUserId;

    /** 签章时间. */
    private LocalDateTime signTime;

    /** 签章失败原因. */
    private String signFailReason;

    /** 验章状态 (UNVERIFIED/VERIFYING/SUCCESS/FAIL). */
    private String verifyStatus;

    /** 验章用户 ID. */
    private Long verifyUserId;

    /** 验章时间. */
    private LocalDateTime verifyTime;

    /** 验章失败原因. */
    private String verifyFailReason;

    /** 扩展字段 (JSON 字符串). */
    private String extend;

    /** 创建人. */
    private Long createBy;

    /** 创建机构. */
    private Long createOrgId;

    /** 创建时间. */
    private LocalDateTime gmtCreate;

    /** 修改时间. */
    private LocalDateTime gmtModify;

    /** 逻辑删除标记 (0 未删/1 已删). */
    private Integer deleted;

    /** 修改人. */
    private Long modifyBy;

    /** 修改人机构. */
    private Long modifyOrgId;

    /** 所属机构. */
    private Long orgId;

    /** 备注. */
    private String remark;

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

    public String getExtend() { return extend; }
    public void setExtend(String extend) { this.extend = extend; }

    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }

    public Long getCreateOrgId() { return createOrgId; }
    public void setCreateOrgId(Long createOrgId) { this.createOrgId = createOrgId; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModify() { return gmtModify; }
    public void setGmtModify(LocalDateTime gmtModify) { this.gmtModify = gmtModify; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Long getModifyBy() { return modifyBy; }
    public void setModifyBy(Long modifyBy) { this.modifyBy = modifyBy; }

    public Long getModifyOrgId() { return modifyOrgId; }
    public void setModifyOrgId(Long modifyOrgId) { this.modifyOrgId = modifyOrgId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    /** 是否已逻辑删除. */
    public boolean isDeleted() {
        return deleted != null && deleted == 1;
    }
}