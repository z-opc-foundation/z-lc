package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStarRocksConfigDTO 单元测试
 *
 * @author zifang
 */
class ZLcStarRocksConfigDTOTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldSetAndGetHost() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setHost("192.168.1.100");
        assertThat(config.getHost()).isEqualTo("192.168.1.100");
    }

    @Test
    void shouldSetAndGetPort() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setPort(8030);
        assertThat(config.getPort()).isEqualTo(8030);
    }

    @Test
    void shouldSetAndGetUser() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setUser("root");
        assertThat(config.getUser()).isEqualTo("root");
    }

    @Test
    void shouldSetAndGetPassword() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setPassword("secret123");
        assertThat(config.getPassword()).isEqualTo("secret123");
    }

    @Test
    void shouldSetAndGetDb() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setDb("analytics");
        assertThat(config.getDb()).isEqualTo("analytics");
    }

    @Test
    void shouldSetAndGetTable() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        config.setTable("user_events");
        assertThat(config.getTable()).isEqualTo("user_events");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        assertThat(config.getHost()).isNull();
        assertThat(config.getPort()).isNull();
        assertThat(config.getUser()).isNull();
        assertThat(config.getPassword()).isNull();
        assertThat(config.getDb()).isNull();
        assertThat(config.getTable()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcStarRocksConfigDTO config = new ZLcStarRocksConfigDTO();
        assertThat(config).isInstanceOf(java.io.Serializable.class);
    }
}
