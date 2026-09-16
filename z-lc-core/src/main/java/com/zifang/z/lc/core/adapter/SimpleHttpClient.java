package com.zifang.z.lc.core.adapter;

/**
 * 占位类 — 实际逻辑已全部迁移到 z-util-http HttpExecutor.
 * <p>
 * 历史背景: z-lc 蒸馏时 z-util-http 1.0.2-SNAPSHOT 本地仓库 jar 缺 HttpExecutor /
 * HttpExecutionResult 类, 临时用 JDK HttpURLConnection 内联了 SimpleHttpClient.
 * 后续 mvn install z-util-http 拿到完整 jar 后, 4 个 Adapter 已切回 z-util-http.
 * <p>
 * 本类保留为 0 字节占位以避免引用方编译失败, 业务方不应再使用 — 请改用:
 * <ul>
 *   <li>CtcAdapter.doGet / CtcAdapter.doPostJson (z-util-http 共享工具方法)</li>
 *   <li>JwtAwareHttpSupport.currentAuthHeaders (header 透传)</li>
 * </ul>
 *
 * @deprecated since 2026-06-16, 计划下个迭代删除. 由 z-lc 蒸馏维护者统一清理.
 */
@Deprecated
public final class SimpleHttpClient {

    private SimpleHttpClient() {
    }
}
