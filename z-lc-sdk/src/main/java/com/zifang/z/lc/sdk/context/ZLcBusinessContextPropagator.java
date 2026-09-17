package com.zifang.z.lc.sdk.context;

import java.util.Map;

/**
 * 业务上下文推送到下游的策略 SPI — 蒸馏自 ace-platform-engine
 * {@code BusinessContextAspect} （{@code com.c2f.ace.engine.common.aspect}}，
 * 行为完全对齐.
 *
 * <p>ace 原实现是 {@code @Aspect} 拦截所有 {@code com.c2f..*.save(...)} 调用，
 * 在调用后把 {@link ZLcEngineInvokerContext#getBusinessContext()} 写入 RPC attachment.
 *
 * <p>z-lc 蒸馏为通用接口，业务方按使用的 RPC 框架（z-rpc / Dubbo / gRPC / HTTP）实现：
 * <ul>
 *   <li>{@link #propagate(Map)} — 把当前 ThreadLocal 中的 businessContext 推送到下游 RPC</li>
 *   <li>{@link #clear()} — 调用结束后清理</li>
 * </ul>
 *
 * <p>典型用法（Spring AOP 形式）：
 * <pre>{@code
 *   @Aspect
 *   @Component
 *   public class BusinessContextAspect {
 *       @Autowired private ZLcBusinessContextPropagator propagator;
 *
 *       @AfterReturning("execution(* com.zifang..*.save(..))")
 *       public void afterSave() {
 *           Map<String, Object> ctx = ZLcEngineInvokerContext.getBusinessContext();
 *           if (ctx != null) propagator.propagate(ctx);
 *       }
 *   }
 * }</pre>
 *
 * @author zifang
 */
public interface ZLcBusinessContextPropagator {

    /**
     * 把业务上下文推送到下游 RPC attachment — 调用方在拦截到 save/调用业务后触发.
     *
     * @param businessContext 业务上下文（来自 {@link ZLcEngineInvokerContext#getBusinessContext()}）
     */
    void propagate(Map<String, Object> businessContext);

    /**
     * 调用结束后清理 — 防止线程复用污染（典型场景：Web Filter finally 块调用）.
     */
    void clear();

    /** attachment key 与 {@link ZLcBdpInvokerFilter#KEY_BUSINESS_CONTEXT} 保持一致. */
    String KEY = "business_context";
}
