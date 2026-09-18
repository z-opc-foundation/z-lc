package com.zifang.z.lc.common.constants;

/**
 * 缓存常量 — 蒸馏自 ace-platform-core
 * {@code CacheConstance} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义缓存键前缀等通用缓存常量.
 *
 * @author zifang
 */
public final class ZLcCacheConstance {

    private ZLcCacheConstance() {
    }

    /**
     * 表结构数据缓存 key 前缀. 完整 key = 前缀 + 库名.
     */
    public static final String TABLE_RESULT_SET_CACHE_KEY_PREFIX = "TABLE_RESULT_SET_CACHE:";
}
