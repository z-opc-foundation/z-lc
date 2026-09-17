package com.zifang.z.lc.common.seal.domain.bo;

/**
 * CA 印章规则配置 BO — 蒸馏自 ace-platform-core
 * {@code SealRuleConfigBO} ({@code com.c2f.ace.core.seal.domain.bo}).
 *
 * <p>兼容骑缝章与单页普通章参数 — 通过 {@link #sealType} (ACROSS/OFFICIAL)
 * 区分. 具体字段含义:
 *
 * <ul>
 *   <li>基础字段 — ruleNum / sealName / sealType / serverSealNum / signPolicyNum / primary</li>
 *   <li>骑缝章字段 (sealType=ACROSS) — acrossPagePattern / acrossStartWidth /
 *       acrossEachPixel / acrossPosCoord / acrossMoveSize</li>
 *   <li>单页普通章字段 (sealType=OFFICIAL) — singlePageSealLeft / Top / Right / Bottom</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcSealRuleConfigBO {

    private String ruleNum;
    private String sealName;
    private String sealType;
    private String serverSealNum;
    private String signPolicyNum;
    private boolean primary;

    private String acrossPagePattern;
    private Integer acrossStartWidth;
    private Integer acrossEachPixel;
    private Integer acrossPosCoord;
    private Integer acrossMoveSize;

    private Float singlePageSealLeft;
    private Float singlePageSealTop;
    private Float singlePageSealRight;
    private Float singlePageSealBottom;

    public String getRuleNum() { return ruleNum; }
    public void setRuleNum(String ruleNum) { this.ruleNum = ruleNum; }

    public String getSealName() { return sealName; }
    public void setSealName(String sealName) { this.sealName = sealName; }

    public String getSealType() { return sealType; }
    public void setSealType(String sealType) { this.sealType = sealType; }

    public String getServerSealNum() { return serverSealNum; }
    public void setServerSealNum(String serverSealNum) { this.serverSealNum = serverSealNum; }

    public String getSignPolicyNum() { return signPolicyNum; }
    public void setSignPolicyNum(String signPolicyNum) { this.signPolicyNum = signPolicyNum; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public String getAcrossPagePattern() { return acrossPagePattern; }
    public void setAcrossPagePattern(String acrossPagePattern) { this.acrossPagePattern = acrossPagePattern; }

    public Integer getAcrossStartWidth() { return acrossStartWidth; }
    public void setAcrossStartWidth(Integer acrossStartWidth) { this.acrossStartWidth = acrossStartWidth; }

    public Integer getAcrossEachPixel() { return acrossEachPixel; }
    public void setAcrossEachPixel(Integer acrossEachPixel) { this.acrossEachPixel = acrossEachPixel; }

    public Integer getAcrossPosCoord() { return acrossPosCoord; }
    public void setAcrossPosCoord(Integer acrossPosCoord) { this.acrossPosCoord = acrossPosCoord; }

    public Integer getAcrossMoveSize() { return acrossMoveSize; }
    public void setAcrossMoveSize(Integer acrossMoveSize) { this.acrossMoveSize = acrossMoveSize; }

    public Float getSinglePageSealLeft() { return singlePageSealLeft; }
    public void setSinglePageSealLeft(Float singlePageSealLeft) { this.singlePageSealLeft = singlePageSealLeft; }

    public Float getSinglePageSealTop() { return singlePageSealTop; }
    public void setSinglePageSealTop(Float singlePageSealTop) { this.singlePageSealTop = singlePageSealTop; }

    public Float getSinglePageSealRight() { return singlePageSealRight; }
    public void setSinglePageSealRight(Float singlePageSealRight) { this.singlePageSealRight = singlePageSealRight; }

    public Float getSinglePageSealBottom() { return singlePageSealBottom; }
    public void setSinglePageSealBottom(Float singlePageSealBottom) { this.singlePageSealBottom = singlePageSealBottom; }

    /** 是否骑缝章 (sealType=ACROSS). */
    public boolean isAcrossPage() {
        return "ACROSS".equalsIgnoreCase(sealType);
    }

    /** 是否普通章 (sealType=OFFICIAL). */
    public boolean isOfficial() {
        return "OFFICIAL".equalsIgnoreCase(sealType);
    }
}