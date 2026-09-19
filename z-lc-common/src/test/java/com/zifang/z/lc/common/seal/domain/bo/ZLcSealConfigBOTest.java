package com.zifang.z.lc.common.seal.domain.bo;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealConfigBO 单元测试
 */
class ZLcSealConfigBOTest {

    @Test
    void shouldCreateWithEmptyLists() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        assertThat(bo).isNotNull();
        assertThat(bo.getId()).isNull();
        assertThat(bo.getOrgId()).isNull();
        assertThat(bo.isEnabled()).isFalse();
        assertThat(bo.getCaAddresses()).isNull();
        assertThat(bo.getAuthorizedUserIds()).isEmpty();
        assertThat(bo.getAcrossSealConfigList()).isEmpty();
        assertThat(bo.getCommonSealConfigList()).isEmpty();
    }

    @Test
    void shouldSetAndGetId() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        bo.setId(1L);
        assertThat(bo.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetOrgId() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        bo.setOrgId(100L);
        assertThat(bo.getOrgId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetEnabled() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        bo.setEnabled(true);
        assertThat(bo.isEnabled()).isTrue();
        bo.setEnabled(false);
        assertThat(bo.isEnabled()).isFalse();
    }

    @Test
    void shouldSetAndGetCaAddresses() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        bo.setCaAddresses("https://ca.example.com");
        assertThat(bo.getCaAddresses()).isEqualTo("https://ca.example.com");
    }

    @Test
    void shouldSetAndGetAuthorizedUserIds() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        bo.setAuthorizedUserIds(Arrays.asList("user1", "user2"));
        assertThat(bo.getAuthorizedUserIds()).containsExactly("user1", "user2");
    }

    @Test
    void shouldSetAndGetAcrossSealConfigList() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        ZLcSealRuleConfigBO rule = new ZLcSealRuleConfigBO();
        bo.setAcrossSealConfigList(Arrays.asList(rule));
        assertThat(bo.getAcrossSealConfigList()).hasSize(1);
    }

    @Test
    void shouldSetAndGetCommonSealConfigList() {
        ZLcSealConfigBO bo = new ZLcSealConfigBO();
        ZLcSealRuleConfigBO rule = new ZLcSealRuleConfigBO();
        bo.setCommonSealConfigList(Arrays.asList(rule));
        assertThat(bo.getCommonSealConfigList()).hasSize(1);
    }
}