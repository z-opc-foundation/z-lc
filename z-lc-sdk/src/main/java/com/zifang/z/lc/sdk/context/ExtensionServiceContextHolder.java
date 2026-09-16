package com.zifang.z.lc.sdk.context;

import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;

/**
 * SPI 上下文 ThreadLocal 持有者.
 * <p>
 * 设计哲学:
 * 业务调用方在请求开始时 set, 引擎回调 SPI 时 get — 让 SPI 无需感知上游调用链.
 * <p>
 * 用法:
 * <pre>{@code
 *   ExtensionServiceContextHolder.set(ctx);
 *   try { ... } finally { ExtensionServiceContextHolder.clear(); }
 * }</pre>
 */
public final class ExtensionServiceContextHolder {

    private static final ThreadLocal<ExtensionServiceContext> CTX = new ThreadLocal<>();

    private ExtensionServiceContextHolder() {
    }

    public static void set(ExtensionServiceContext ctx) {
        CTX.set(ctx);
    }

    public static ExtensionServiceContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }

    public static String currentAppCode() {
        ExtensionServiceContext c = CTX.get();
        return c == null ? null : c.getAppCode();
    }

    public static String currentModelCode() {
        ExtensionServiceContext c = CTX.get();
        return c == null ? null : c.getModelCode();
    }
}
