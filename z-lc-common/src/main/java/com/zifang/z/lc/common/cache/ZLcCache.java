package com.zifang.z.lc.common.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用缓存容器 — 蒸馏自 ace-platform-core
 * {@code Cache} ({@code com.c2f.ace.core.middleware.cache}).
 *
 * <p>基于 {@link ConcurrentHashMap} 的线程安全缓存, 提供 hit/inject/evit/pick 等
 * 基本操作. 业务方可包装具体 ORM/JSON 序列化实现, 也可作为内存级缓存直接使用.
 *
 * <p>ace 原 {@code Cache} 依赖 {@code c2f.boot.starter.cache.CacheOperateUtil} (Redis 等),
 * 蒸馏版改为基于 {@link ConcurrentHashMap} 的纯 Java 实现, 不引入外部缓存依赖.
 * 业务方如有 Redis 需求可自行扩展 {@link #inject} / {@link #pick} 的实现.
 *
 * @author zifang
 */
public class ZLcCache<T> {

    /** 缓存值类型 (用于 JSON 反序列化). */
    private Class<T> clazz;

    /** 内部 ConcurrentHashMap 容器. */
    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    public ConcurrentHashMap<String, T> getCache() {
        return cache;
    }

    /**
     * 是否命中缓存.
     */
    public boolean hit(String key) {
        if (key == null) {
            return false;
        }
        return cache.containsKey(key);
    }

    /**
     * 写入缓存.
     */
    public void inject(String key, T value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            cache.remove(key);
        } else {
            cache.put(key, value);
        }
    }

    /**
     * 驱逐单个缓存项.
     */
    public void evit(String key) {
        if (key == null) {
            return;
        }
        cache.remove(key);
    }

    /**
     * 清理所有缓存.
     */
    public void clean() {
        cache.clear();
    }

    /**
     * 抠缓存值 — 不存在时返回 {@code null}.
     */
    public T pick(String key) {
        if (key == null) {
            return null;
        }
        return cache.get(key);
    }

    /** 当前缓存大小. */
    public int size() {
        return cache.size();
    }

    /** 是否空. */
    public boolean isEmpty() {
        return cache.isEmpty();
    }
}