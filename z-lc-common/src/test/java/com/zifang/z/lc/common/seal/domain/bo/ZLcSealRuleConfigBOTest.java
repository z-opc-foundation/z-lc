package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealRuleConfigBO 单元测试
 */
class ZLcSealRuleConfigBOTest {

    @Test
    void shouldCreateEmpty() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        assertThat(bo).isNotNull();
        assertThat(bo.getRuleNum()).isNull();
        assertThat(bo.isPrimary()).isFalse();
        assertThat(bo.isAcrossPage()).isFalse();
        assertThat(bo.isOfficial()).isFalse();
    }

    @Test
    void shouldSetAndGetBasicFields() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setRuleNum("rule-001");
        bo.setSealName("Official Seal");
        bo.setSealType("OFFICIAL");
        bo.setServerSealNum("server-001");
        bo.setSignPolicyNum("policy-001");
        bo.setPrimary(true);

        assertThat(bo.getRuleNum()).isEqualTo("rule-001");
        assertThat(bo.getSealName()).isEqualTo("Official Seal");
        assertThat(bo.getSealType()).isEqualTo("OFFICIAL");
        assertThat(bo.getServerSealNum()).isEqualTo("server-001");
        assertThat(bo.getSignPolicyNum()).isEqualTo("policy-001");
        assertThat(bo.isPrimary()).isTrue();
    }

    @Test
    void shouldSetAndGetAcrossPageFields() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setAcrossPagePattern("PATTERN_1");
        bo.setAcrossStartWidth(10);
        bo.setAcrossEachPixel(20);
        bo.setAcrossPosCoord(30);
        bo.setAcrossMoveSize(40);

        assertThat(bo.getAcrossPagePattern()).isEqualTo("PATTERN_1");
        assertThat(bo.getAcrossStartWidth()).isEqualTo(10);
        assertThat(bo.getAcrossEachPixel()).isEqualTo(20);
        assertThat(bo.getAcrossPosCoord()).isEqualTo(30);
        assertThat(bo.getAcrossMoveSize()).isEqualTo(40);
    }

    @Test
    void shouldSetAndGetSinglePageFields() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSinglePageSealLeft(1.0f);
        bo.setSinglePageSealTop(2.0f);
        bo.setSinglePageSealRight(3.0f);
        bo.setSinglePageSealBottom(4.0f);

        assertThat(bo.getSinglePageSealLeft()).isEqualTo(1.0f);
        assertThat(bo.getSinglePageSealTop()).isEqualTo(2.0f);
        assertThat(bo.getSinglePageSealRight()).isEqualTo(3.0f);
        assertThat(bo.getSinglePageSealBottom()).isEqualTo(4.0f);
    }

    @Test
    void isAcrossPageShouldReturnTrueForAcross() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSealType("ACROSS");
        assertThat(bo.isAcrossPage()).isTrue();
        assertThat(bo.isOfficial()).isFalse();
    }

    @Test
    void isAcrossPageShouldReturnTrueCaseInsensitive() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSealType("across");
        assertThat(bo.isAcrossPage()).isTrue();
    }

    @Test
    void isOfficialShouldReturnTrueForOfficial() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSealType("OFFICIAL");
        assertThat(bo.isOfficial()).isTrue();
        assertThat(bo.isAcrossPage()).isFalse();
    }

    @Test
    void isOfficialShouldReturnTrueCaseInsensitive() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSealType("official");
        assertThat(bo.isOfficial()).isTrue();
    }

    @Test
    void isAcrossPageShouldReturnFalseForNullSealType() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        assertThat(bo.isAcrossPage()).isFalse();
        assertThat(bo.isOfficial()).isFalse();
    }

    @Test
    void isAcrossPageShouldReturnFalseForUnknownSealType() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setSealType("UNKNOWN");
        assertThat(bo.isAcrossPage()).isFalse();
        assertThat(bo.isOfficial()).isFalse();
    }

    @Test
    void primaryShouldToggle() {
        ZLcSealRuleConfigBO bo = new ZLcSealRuleConfigBO();
        bo.setPrimary(true);
        assertThat(bo.isPrimary()).isTrue();
        bo.setPrimary(false);
        assertThat(bo.isPrimary()).isFalse();
    }
}