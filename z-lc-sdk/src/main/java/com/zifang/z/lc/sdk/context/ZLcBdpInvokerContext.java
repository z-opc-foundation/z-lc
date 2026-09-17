package com.zifang.z.lc.sdk.context;

/**
 * BDP 调用上下文 ThreadLocal — 蒸馏自 ace-platform-engine {@code BdpInvokerContext}
 * （{@code com.c2f.ace.engine.filter}），行为完全对齐.
 *
 * <p>只保存请求级 UUID — BDP（业务数据平台）链路标识，用于跨服务调用追踪.
 *
 * <p>与 {@link ZLcEngineInvokerContext} 的关系：
 * <ul>
 *   <li>{@code BdpInvokerContext} — 旧 API 保留，仅 UUID — BDP 框架内部使用</li>
 *   <li>{@code ZLcEngineInvokerContext} — 新 API，扩展到 11 个字段（UUID/mode/saveFlag/tags/...）</li>
 * </ul>
 *
 * <p>{@code @Deprecated} 标记保留 — ace 原代码同样标记为已弃用，新代码应直接使用
 * {@link ZLcEngineInvokerContext#getUUID()} / {@link ZLcEngineInvokerContext#setUUID(String)}.
 *
 * @author zifang
 */
@Deprecated
public final class ZLcBdpInvokerContext {

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    private ZLcBdpInvokerContext() {
        // 工具类，禁止实例化
    }

    /**
     * 设置 BDP 请求 UUID.
     */
    public static void setUUID(String uuid) {
        THREAD_LOCAL.set(uuid);
    }

    /**
     * 取出 BDP 请求 UUID.
     */
    public static String getUUID() {
        return THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal — 必须在请求结束 finally 块调用，避免线程复用污染.
     */
    public static void clean() {
        THREAD_LOCAL.remove();
    }
}
