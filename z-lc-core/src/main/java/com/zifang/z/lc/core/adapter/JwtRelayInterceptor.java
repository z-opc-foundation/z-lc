package com.zifang.z.lc.core.adapter;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前 servlet 请求绑定器 (z-lc JWT 透传基础设施).
 * <p>
 * 设计哲学:
 * z-lc 业务方调 Adapter 时, 需要透传当前请求的 Authorization / X-Tenant-Code —
 * 通过 ThreadLocal 持有当前请求, Adapter 出口处取.
 * <p>
 * 用法:
 * <ul>
 *   <li>Controller 入口: {@link #bindCurrentRequest(HttpServletRequest)}</li>
 *   <li>Adapter 出站: {@link #currentRequest()} → 读 header</li>
 *   <li>Controller 出口: {@link #clear()}</li>
 * </ul>
 */
public class JwtRelayInterceptor {

    private static final ThreadLocal<HttpServletRequest> CURRENT = new ThreadLocal<>();

    /**
     * Controller 入口绑定当前 servlet 请求 (供 Adapter 调用).
     */
    public static void bindCurrentRequest(HttpServletRequest req) {
        CURRENT.set(req);
    }

    /**
     * Controller 出口清理 ThreadLocal, 防止线程复用泄漏.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 取当前线程绑定的 servlet 请求. 若未绑定 (例如后台任务), 返回 null.
     */
    public static HttpServletRequest currentRequest() {
        return CURRENT.get();
    }
}
