package com.zifang.z.lc.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁住 JwtRelayInterceptor 的绑定路径: 所有走 z-lc adapter outbound HTTP 的请求
 * 必须能拿到当前 servlet request, 否则 Authorization / X-Tenant-Code 跨线程就断了.
 * <p>
 * 路径改了 (例如变成 /api/** 或漏了 /api/lc/**) 都不行: 前者会让非 z-lc 路径也走绑定
 * (多线程副作用扩散到本不该走的请求), 后者会让所有 z-lc 业务路径拿不到当前 request.
 * <p>
 * 注: 不调用 {@code InterceptorRegistration.getPathPatterns()} —— 5.3.39 上该类根本没有这个方法
 * （javap 过，公开的只有 addPathPatterns / excludePathPatterns / pathMatcher / order）。
 * 改为反射读私有字段，且**按候选名依次尝试**：{@code includePatterns}（Spring 5.x 真实字段名）
 * 与 {@code pathPatterns}（Spring 4.x）。原实现只认后者并声称"跨版本稳定"，
 * 那个前提是错的 —— 见 PATTERN_FIELDS 的注释。
 */
class LcWebMvcConfigTest {

    @Test
    @DisplayName("/api/lc/** 必须注册拦截器, 否则 JwtRelayInterceptor 永远不会被 bindCurrentRequest")
    void registersInterceptorOnLcApiPath() {
        assertTrue(extractFirstPath(new LcWebMvcConfig())
                        .map(p -> p.contains("/api/lc/"))
                        .orElse(false),
                "缺 /api/lc/** 路径模式: z-lc 全部业务请求将拿不到 JwtRelayInterceptor 的 request 绑定");
    }

    @Test
    @DisplayName("拦截器只锁 z-lc 自己的前缀, 不污染其他模块的 /api/** 路径")
    void doesNotRegisterOnRootApiPath() {
        boolean hasRoot = extractFirstPath(new LcWebMvcConfig())
                .map(p -> p.equals("/api/**"))
                .orElse(false);
        assertFalse(hasRoot,
                "变成 /api/** 会让非 z-lc 请求也走 JwtRelayInterceptor.bindCurrentRequest, 多线程副作用扩散");
    }

    private static java.util.Optional<String> extractFirstPath(LcWebMvcConfig cfg) {
        java.util.List<InterceptorRegistration> regs = new java.util.ArrayList<InterceptorRegistration>();
        InterceptorRegistry registry = new InterceptorRegistry() {
            @Override
            public InterceptorRegistration addInterceptor(org.springframework.web.servlet.HandlerInterceptor i) {
                InterceptorRegistration r = super.addInterceptor(i);
                regs.add(r);
                return r;
            }
        };
        cfg.addInterceptors(registry);
        if (regs.isEmpty()) {
            return java.util.Optional.empty();
        }
        for (String name : PATTERN_FIELDS) {
            try {
                java.lang.reflect.Field f = InterceptorRegistration.class.getDeclaredField(name);
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<String> patterns = (java.util.List<String>) f.get(regs.get(0));
                return patterns.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(patterns.get(0));
            } catch (NoSuchFieldException skip) {
                // 这个 Spring 版本用的是另一个字段名，换下一个候选
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("反射读 InterceptorRegistration." + name + " 失败: " + ex, ex);
            }
        }
        throw new IllegalStateException(
                "InterceptorRegistration 上一个叫 \"路径模式\" 的私有字段都没找到(试过 "
                        + String.join(" / ", PATTERN_FIELDS) + ")。这说明 Spring 又改了内部字段名 —— "
                        + "查证命令: javap -p -cp <spring-webmvc-x.y.z.jar> "
                        + "org.springframework.web.servlet.config.annotation.InterceptorRegistration");
    }

    /**
     * 各 Spring 行的真实字段名（2026-10-04 用 javap 逐版核对过，别凭印象改）：
     * <ul>
     *   <li>Spring 4.x：{@code private List<String> pathPatterns} + {@code excludedPathPatterns}</li>
     *   <li><b>Spring 5.3.39（本仓实测）：{@code private List<String> includePatterns} + {@code excludePatterns}</b>，
     *       {@code pathPatterns} 只是公开方法 {@code addPathPatterns(...)} 的名字，不是字段</li>
     * </ul>
     * 原实现只认 {@code pathPatterns} 一个名字，并注释说"反射读字段跨版本稳定"——
     * 那个前提是错的，它实际是照着 Spring 4.x 写的，从未在 5.3 上跑通。
     * 这条测试之所以红这么久没人发现，是因为它此前从未被执行过（见 z-lc 全量构建）。
     * <p>
     * 更稳的那条路也存在但没走：{@code InterceptorRegistry.getInterceptors()} 是包私有，
     * 要反射一次才能拿到 {@code MappedInterceptor}，而它上面的 {@code getPathPatterns()} 是公开的。
     * 同样是反射，只是不再依赖字段名 —— 下次 Spring 改内部结构时，优先考虑切过去。
     */
    private static final String[] PATTERN_FIELDS = {"includePatterns", "pathPatterns"};
}
