package com.zifang.z.lc.common.seal.domain.entity;

import java.io.Serializable;

/**
 * CA 签章配置 DO — 蒸馏自 ace-platform-core
 * {@code SealConfigDO} ({@code com.c2f.ace.core.seal.domain.entity}).
 *
 * <p>用于低代码平台"电子签章"模块 — 签章配置持久化对象 (ca_seal_config 表).
 *
 * <p>蒸馏说明: ace 原版用 MyBatis Plus 注解 (@TableName/@TableId/@TableLogic),
 * 蒸馏版只保留字段, 不依赖 MyBatis Plus. 业务方在使用时自行添加 ORM 注解.
 *
 * @author zifang
 */
public class ZLcSealConfigDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键. */
    private Long id;

    /** 是否启用 (0/1). */
    private Integer enabled;

    /** CA 服务地址 (多地址逗号分隔). */
    private String caAddresses;

    /** 授权用户 ID 列表 (逗号分隔). */
    private String authorizedUserIds;

    /** 扩展字段 (JSON 字符串). */
    private String extend;

    /** 机构 ID. */
    private Long orgId;

    /** 逻辑删除标记 (0 未删/1 已删). */
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }

    public String getCaAddresses() { return caAddresses; }
    public void setCaAddresses(String caAddresses) { this.caAddresses = caAddresses; }

    public String getAuthorizedUserIds() { return authorizedUserIds; }
    public void setAuthorizedUserIds(String authorizedUserIds) { this.authorizedUserIds = authorizedUserIds; }

    public String getExtend() { return extend; }
    public void setExtend(String extend) { this.extend = extend; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    /** 是否启用 (enabled=1). */
    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    /** 是否已逻辑删除. */
    public boolean isDeleted() {
        return deleted != null && deleted == 1;
    }
}