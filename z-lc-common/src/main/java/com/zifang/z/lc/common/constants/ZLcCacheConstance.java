package com.zifang.z.lc.common.constants;

/**
 * z-lc 缓存常量 — 蒸馏自 ace-platform-core
 * {@code CacheConstance} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>本类集中定义 z-lc 引擎用到的缓存 key 前缀与固定 key，
 * 避免散落在各处的字符串字面量难以维护.
 *
 * @author zifang
 */
public final class ZLcCacheConstance {

    /**
     * 表结构数据缓存 key 前缀（完整格式：{@code 前缀 + 库名}） —
     * 用于数据源扫描结果缓存，避免重复扫描大库.
     */
    public static final String TABLE_RESULT_SET_CACHE_KEY_PREFIX = "TABLE_RESULT_SET_CACHE:";

    /**
     * 通用模型元数据缓存前缀.
     */
    public static final String MODEL_META_CACHE_KEY_PREFIX = "MODEL_META_CACHE:";

    /**
     * 字典项缓存前缀.
     */
    public static final String DICT_ITEM_CACHE_KEY_PREFIX = "DICT_ITEM_CACHE:";

    /**
     * 流程定义缓存前缀.
     */
    public static final String WORKFLOW_DEF_CACHE_KEY_PREFIX = "WORKFLOW_DEF_CACHE:";

    /**
     * 应用级用户权限缓存前缀.
     */
    public static final String APP_PERMISSION_CACHE_KEY_PREFIX = "APP_PERMISSION_CACHE:";

    /**
     * 默认缓存过期时间（秒） — 1 小时.
     */
    public static final long DEFAULT_CACHE_EXPIRE_SECONDS = 3600L;

    /**
     * 元数据缓存过期时间（秒） — 30 分钟（变动不频繁）.
     */
    public static final long META_CACHE_EXPIRE_SECONDS = 1800L;

    private ZLcCacheConstance() {
        // 工具类，禁止实例化
    }
}
