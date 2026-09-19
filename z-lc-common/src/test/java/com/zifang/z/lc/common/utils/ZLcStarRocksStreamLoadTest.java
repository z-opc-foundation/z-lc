package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ZLcStarRocksStreamLoad 单元测试
 *
 * @author zifang
 */
class ZLcStarRocksStreamLoadTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcStarRocksStreamLoad> constructor = ZLcStarRocksStreamLoad.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidPort() {
        Map<String, String> config = new HashMap<>();
        config.put("host", "127.0.0.1");
        config.put("port", "invalid_port");
        config.put("db", "test_db");
        config.put("table", "test_table");
        config.put("user", "root");
        config.put("password", "pass");

        assertThatThrownBy(() -> ZLcStarRocksStreamLoad.sendData("[]", config))
                .isInstanceOf(NumberFormatException.class);
    }
}