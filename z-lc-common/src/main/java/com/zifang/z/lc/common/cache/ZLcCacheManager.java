package com.zifang.z.lc.common.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器 — 蒸馏自 ace-platform-core
 * {@code CacheManager} ({@code com.c2f.ace.core.middleware.cache}).
 *
 * <p>按 Class 注册 {@link ZLcCache} 实例, 业务方通过 {@link #fetchCache(Class)}
 * 拿到对应类型的缓存容器. 使用 {@link ConcurrentHashMap} 保证多线程安全.
 *
 * <p>典型用法:
 * <pre>{@code
 * // 1. 构建 / 注册缓存
 * ZLcCache<MyDTO> cache = ZLcCacheManager.build(MyDTO.class);
 *
 * // 2. 写入
 * cache.inject("user:1", new MyDTO(...));
 *
 * // 3. 读取
 * MyDTO dto = ZLcCacheManager.fetchCache(MyDTO.class).pick("user:1");
 * }</pre>
 *
 * @author zifang
 */
public final class ZLcCacheManager {

    private static final ConcurrentHashMap<String, ZLcCache<?>> CACHES = new ConcurrentHashMap<>();

    private ZLcCacheManager() {
        // utility class
    }

    /**
     * 构建 / 注册 {@link ZLcCache} — 已存在同 Class 的缓存则返回已有实例.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> ZLcCache<T> build(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }
        ZLcCache<T> cache = (ZLcCache<T>) CACHES.get(clazz.getName());
        if (cache == null) {
            cache = new ZLcCache<>();
            cache.setClazz(clazz);
            CACHES.put(clazz.getName(), cache);
        }
        return cache;
    }

    /**
     * 获取已注册的 {@link ZLcCache} — 未注册时返回 {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ZLcCache<T> fetchCache(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        return (ZLcCache<T>) CACHES.get(clazz.getName());
    }

    /**
     * 移除指定 Class 的缓存容器.
     */
    public static synchronized void evict(Class<?> clazz) {
        if (clazz != null) {
            CACHES.remove(clazz.getName());
        }
    }

    /**
     * 清理所有已注册的缓存容器.
     */
    public static synchronized void cleanAll() {
        CACHES.clear();
    }

    /** 当前已注册缓存数量. */
    public static int size() {
        return CACHES.size();
    }
}