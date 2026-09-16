package com.zifang.z.lc.core.adapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT/Tenant 透传支持 (z-lc 内部).
 * <p>
 * 设计哲学:
 * 业务方调 Adapter 时, 通过 {@link #currentAuthHeaders()} 一行拿当前 servlet
 * 请求的 Authorization / X-Tenant-Code 头, 传给 z-util-http HttpRequestDefinition —
 * 走 z-opc 自己的 z-util-http, 不再依赖 SimpleHttpClient.
 * <p>
 * 保留本类作为 facade: 业务侧可以统一 import JwtAwareHttpSupport 拿到当前线程的 auth headers.
 */
public final class JwtAwareHttpSupport {

    private JwtAwareHttpSupport() {
    }

    /**
     * 取当前 servlet 请求的 Authorization / X-Tenant-Code 头 Map.
     * <p>
     * 若当前线程未绑定 servlet 请求 (例如后台任务 / 定时任务), 返回空 Map.
     */
    public static Map<String, String> currentAuthHeaders() {
        javax.servlet.http.HttpServletRequest src = JwtRelayInterceptor.currentRequest();
        if (src == null) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>();
        String auth = src.getHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            out.put("Authorization", auth);
        }
        String tenant = src.getHeader("X-Tenant-Code");
        if (tenant == null || tenant.isEmpty()) {
            tenant = src.getHeader("X-Tenant");
        }
        if (tenant != null && !tenant.isEmpty()) {
            out.put("X-Tenant-Code", tenant);
        }
        return out;
    }
}
