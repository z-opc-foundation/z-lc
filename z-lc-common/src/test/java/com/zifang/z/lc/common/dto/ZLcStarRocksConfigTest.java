package com.zifang.z.lc.common.dto;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcStarRocksConfig 单元测试
 *
 * @author zifang
 */
class ZLcStarRocksConfigTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig(
                "localhost", 9030, "root", "password", "mydb", "mytable"
        );
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(9030);
        assertThat(config.getUser()).isEqualTo("root");
        assertThat(config.getPassword()).isEqualTo("password");
        assertThat(config.getDb()).isEqualTo("mydb");
        assertThat(config.getTable()).isEqualTo("mytable");
    }

    @Test
    void shouldSetAndGetHost() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setHost("localhost");
        assertThat(config.getHost()).isEqualTo("localhost");
    }

    @Test
    void shouldSetAndGetPort() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setPort(9030);
        assertThat(config.getPort()).isEqualTo(9030);
    }

    @Test
    void shouldSetAndGetUser() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setUser("root");
        assertThat(config.getUser()).isEqualTo("root");
    }

    @Test
    void shouldSetAndGetPassword() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setPassword("password");
        assertThat(config.getPassword()).isEqualTo("password");
    }

    @Test
    void shouldSetAndGetDb() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setDb("mydb");
        assertThat(config.getDb()).isEqualTo("mydb");
    }

    @Test
    void shouldSetAndGetTable() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        config.setTable("mytable");
        assertThat(config.getTable()).isEqualTo("mytable");
    }

    @Test
    void shouldHandleNullValues() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        assertThat(config.getHost()).isNull();
        assertThat(config.getPort()).isNull();
        assertThat(config.getUser()).isNull();
        assertThat(config.getPassword()).isNull();
        assertThat(config.getDb()).isNull();
        assertThat(config.getTable()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ZLcStarRocksConfig config = new ZLcStarRocksConfig();
        assertThat(config).isInstanceOf(Serializable.class);
    }
}
