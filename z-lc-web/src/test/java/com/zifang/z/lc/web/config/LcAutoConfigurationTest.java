package com.zifang.z.lc.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁住 LcAutoConfiguration 的扫描拓扑:
 * common / core (排除 mapper) / core.mapper / design / sdk / web (排除 LcModuleDataSource).
 * <p>
 * 历史 bug: 早期逐个枚举了 16 个 core.* 子包, 新增子包 (如 core.fieldtype) 不会被扫到,
 * 表现为 "编译通过、bean 注入失败、启动直接挂", 而且报错完全不指向本文件.
 * 测试目的: 有人误改回逐个枚举时立刻看见红.
 */
class LcAutoConfigurationTest {

    @Test
    @DisplayName("类上有 @Configuration + @ComponentScans, Boot 自动注册才生效")
    void hasConfigAndComponentScans() {
        assertNotNull(LcAutoConfiguration.class.getAnnotation(Configuration.class),
                "@Configuration 缺失: 类变成普通 bean, 自动配置失效");
        ComponentScans cs = LcAutoConfiguration.class.getAnnotation(ComponentScans.class);
        assertNotNull(cs, "@ComponentScans 缺失: 退回默认扫描, z-lc 路径规则不生效");
        assertTrue(cs.value().length >= 4,
                "至少要扫 common / core / core.mapper / web 四族, 当前扫描数 = " + cs.value().length);
    }

    @Test
    @DisplayName("core 包扫描必须排除 mapper 子包 (避免与 LcModuleDataSource 的 @MapperScan 冲突)")
    void coreScanExcludesMapperSubpackage() throws Exception {
        ComponentScan[] scans = LcAutoConfiguration.class.getAnnotation(ComponentScans.class).value();
        ComponentScan coreScan = findCoreScan(scans);
        assertNotNull(coreScan, "core 包扫描缺失");
        ComponentScan.Filter[] filters = coreScan.excludeFilters();
        assertNotNull(filters);
        assertTrue(filters.length >= 1,
                "core 包扫描必须有 excludeFilters, 否则与 @MapperScan 撞同名 bean");
        boolean hasMapperExclude = false;
        for (ComponentScan.Filter f : filters) {
            if (f.type() == org.springframework.context.annotation.FilterType.REGEX
                    && f.pattern() != null && f.pattern().length >= 1
                    && f.pattern()[0].contains("mapper")) {
                hasMapperExclude = true;
                break;
            }
        }
        assertTrue(hasMapperExclude, "core 包扫描的 REGEX exclude 必须覆盖 mapper 子包");
    }

    private static ComponentScan findCoreScan(ComponentScan[] scans) {
        for (ComponentScan s : scans) {
            for (String pkg : s.basePackages()) {
                if ("com.zifang.z.lc.core".equals(pkg)) {
                    return s;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static void touch(Field f) {
        // 防止 import Field 被清理
    }
}
