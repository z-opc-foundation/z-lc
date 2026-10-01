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
 * 注: 不调用 {@code InterceptorRegistration.getPathPatterns()} —— 该方法在不同 Spring 版本里
 * 存在性不一致 (5.3+ 才加), 我们改用反射读 {@code pathPatterns} 字段, 跨版本稳定.
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
        try {
            java.lang.reflect.Field f = InterceptorRegistration.class.getDeclaredField("pathPatterns");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<String> patterns = (java.util.List<String>) f.get(regs.get(0));
            return patterns.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(patterns.get(0));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "反射读 InterceptorRegistration.pathPatterns 失败, Spring 版本字段名变更: " + ex, ex);
        }
    }
}
