package com.zifang.z.lc.common.seal.domain.bo;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前机构 CA 签章配置 BO — 蒸馏自 ace-platform-core
 * {@code SealConfigBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>用于低代码平台"电子签章"模块 — 按机构维度存储 CA 签章服务地址、
 * 授权用户列表、骑缝章规则配置、普通章规则配置.
 *
 * @author zifang
 */
public class ZLcSealConfigBO {

    private Long id;
    private Long orgId;
    private boolean enabled;
    private String caAddresses;
    private List<String> authorizedUserIds = new ArrayList<>();
    private List<ZLcSealRuleConfigBO> acrossSealConfigList = new ArrayList<>();
    private List<ZLcSealRuleConfigBO> commonSealConfigList = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCaAddresses() { return caAddresses; }
    public void setCaAddresses(String caAddresses) { this.caAddresses = caAddresses; }

    public List<String> getAuthorizedUserIds() { return authorizedUserIds; }
    public void setAuthorizedUserIds(List<String> authorizedUserIds) { this.authorizedUserIds = authorizedUserIds; }

    public List<ZLcSealRuleConfigBO> getAcrossSealConfigList() { return acrossSealConfigList; }
    public void setAcrossSealConfigList(List<ZLcSealRuleConfigBO> acrossSealConfigList) { this.acrossSealConfigList = acrossSealConfigList; }

    public List<ZLcSealRuleConfigBO> getCommonSealConfigList() { return commonSealConfigList; }
    public void setCommonSealConfigList(List<ZLcSealRuleConfigBO> commonSealConfigList) { this.commonSealConfigList = commonSealConfigList; }
}