package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcPrimaryKeyEntity 单元测试
 *
 * @author zifang
 */
class ZLcPrimaryKeyEntityTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void shouldSetAndGetId() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        entity.setId(123L);
        assertThat(entity.getId()).isEqualTo(123L);
    }

    @Test
    void shouldHandleNullId() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        assertThat(entity).isInstanceOf(Serializable.class);
    }

    @Test
    void shouldToString() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        entity.setId(123L);
        assertThat(entity.toString()).isEqualTo("ZLcPrimaryKeyEntity{id=123}");
    }

    @Test
    void shouldToStringWithNullId() {
        ZLcPrimaryKeyEntity entity = new ZLcPrimaryKeyEntity();
        assertThat(entity.toString()).isEqualTo("ZLcPrimaryKeyEntity{id=null}");
    }
}
