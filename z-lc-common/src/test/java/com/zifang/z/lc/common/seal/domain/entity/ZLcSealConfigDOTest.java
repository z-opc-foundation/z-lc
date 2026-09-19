package com.zifang.z.lc.common.seal.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcSealConfigDO 单元测试
 */
class ZLcSealConfigDOTest {

    @Test
    void shouldCreateEmpty() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getEnabled()).isNull();
        assertThat(entity.getCaAddresses()).isNull();
        assertThat(entity.getAuthorizedUserIds()).isNull();
        assertThat(entity.getExtend()).isNull();
        assertThat(entity.getOrgId()).isNull();
        assertThat(entity.getDeleted()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        assertThat(entity).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldSetAndGetId() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setId(1L);
        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    void shouldSetAndGetEnabled() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setEnabled(1);
        assertThat(entity.getEnabled()).isEqualTo(1);
    }

    @Test
    void shouldSetAndGetCaAddresses() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setCaAddresses("https://ca1.example.com,https://ca2.example.com");
        assertThat(entity.getCaAddresses())
                .isEqualTo("https://ca1.example.com,https://ca2.example.com");
    }

    @Test
    void shouldSetAndGetAuthorizedUserIds() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setAuthorizedUserIds("1,2,3");
        assertThat(entity.getAuthorizedUserIds()).isEqualTo("1,2,3");
    }

    @Test
    void shouldSetAndGetExtend() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setExtend("{\"key\":\"value\"}");
        assertThat(entity.getExtend()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void shouldSetAndGetOrgId() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setOrgId(100L);
        assertThat(entity.getOrgId()).isEqualTo(100L);
    }

    @Test
    void shouldSetAndGetDeleted() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setDeleted(1);
        assertThat(entity.getDeleted()).isEqualTo(1);
    }

    @Test
    void isEnabledShouldReturnFalseWhenNull() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    void isEnabledShouldReturnFalseWhenZero() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setEnabled(0);
        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    void isEnabledShouldReturnTrueWhenOne() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setEnabled(1);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    void isDeletedShouldReturnFalseWhenNull() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void isDeletedShouldReturnFalseWhenZero() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setDeleted(0);
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void isDeletedShouldReturnTrueWhenOne() {
        ZLcSealConfigDO entity = new ZLcSealConfigDO();
        entity.setDeleted(1);
        assertThat(entity.isDeleted()).isTrue();
    }
}