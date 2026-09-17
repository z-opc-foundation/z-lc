package com.zifang.z.lc.common.context;

/**
 * ThreadLocal 上下文持有器 — 蒸馏自 ace-platform-core
 * {@code FormInstanceThreadLocalHolder} ({@code com.c2f.ace.core.bpmn.config}).
 *
 * <p>通用 {@link ThreadLocal} 持有器, 用于在"流程 / 请求"线程内传递上下文对象,
 * 避免层层方法签名传递. 业务方在使用后必须调用 {@link #clean()} 以避免
 * 线程复用导致内存泄漏 (尤其在线程池场景).
 *
 * <p>典型用法:
 * <pre>{@code
 * // 1. 入口处设置上下文
 * ZLcThreadLocalHolder<MyContext> holder = new ZLcThreadLocalHolder<>();
 * holder.set(new MyContext(...));
 *
 * // 2. 业务代码中获取
 * MyContext ctx = holder.get();
 *
 * // 3. finally 块清理
 * try {
 *     ...
 * } finally {
 *     holder.clean();
 * }
 * }</pre>
 *
 * <p>ace 原 {@code FormInstanceThreadLocalHolder} 是写死 {@code FormInstanceDTO} 的静态类,
 * 蒸馏版改为泛型, 可复用于任意上下文类型 (表单实例/请求上下文/租户上下文 等).
 *
 * @author zifang
 */
public class ZLcThreadLocalHolder<T> {

    private final ThreadLocal<T> holder = new ThreadLocal<>();

    /** 设置当前线程的上下文. */
    public void set(T value) {
        holder.set(value);
    }

    /** 获取当前线程的上下文 (未设置时返回 {@code null}). */
    public T get() {
        return holder.get();
    }

    /** 清理当前线程的上下文 (建议在 {@code finally} 块中调用). */
    public void clean() {
        holder.remove();
    }

    /** 当前线程是否已设置上下文. */
    public boolean hasValue() {
        return holder.get() != null;
    }
}