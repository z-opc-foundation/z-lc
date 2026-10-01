package com.zifang.z.lc.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 烟雾测试: ZLcBootstrapApplication 必须能被反射实例化且 main 方法签名稳定,
 * 否则 z-lc 独立启动入口 (z-lc-admin) 启动失败时就只剩 jstack 排障一条路.
 * <p>
 * 这里不真启 Spring 容器 (那是 z-lc-e2e 的活), 只钉住最小契约.
 */
class ZLcBootstrapApplicationTest {

    @Test
    @DisplayName("类上有 @SpringBootApplication 注解, Boot 启动器才会识别它")
    void hasSpringBootApplicationAnnotation() {
        assertNotNull(ZLcBootstrapApplication.class.getAnnotation(SpringBootApplication.class),
                "@SpringBootApplication 缺失会让 z-lc-admin 启动失败");
    }

    @Test
    @DisplayName("main(String[]) 方法存在, Boot 重启/IDE 直接 run 都靠它")
    void mainMethodSignature() throws NoSuchMethodException {
        Method main = ZLcBootstrapApplication.class.getDeclaredMethod("main", String[].class);
        assertNotNull(main);
        assertDoesNotThrow(() -> main.setAccessible(true),
                "main 必须可访问");
    }
}
