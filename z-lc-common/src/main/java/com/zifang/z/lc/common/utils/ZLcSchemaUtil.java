package com.zifang.z.lc.common.utils;

/**
 * 数据库 Schema 工具 — 蒸馏自 ace-platform-core
 * {@code SchemaUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台数据库 Schema 相关的常量和辅助方法.
 * 蒸馏时移除了 ace 对 hutool / fastjson 的依赖, 仅保留核心常量和轻量方法.
 *
 * <p>典型场景：
 * <ul>
 *   <li>模型表名前缀管理 (ods_/ads_/data_)</li>
 *   <li>系统字段名定义 (id/gmt_create/gmt_modified/deleted)</li>
 *   <li>数据库类型判断 (MySQL/Doris/StarRocks)</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcSchemaUtil {

    private ZLcSchemaUtil() {
    }

    // ==================== 表名前缀 ====================

    /** ODS 原始层表前缀. */
    public static final String ODS_PREFIX = "ods_";

    /** ADS 聚合层表前缀. */
    public static final String ADS_PREFIX = "ads_";

    /** ADS 原始层表前缀. */
    public static final String ADS_ORIGIN_PREFIX = "ads_origin_";

    /** 数据模型列表页面前缀. */
    public static final String DATA_LIST_PREFIX = "data_list@@@";

    /** 数据模型详情页面前缀. */
    public static final String DATA_DETAIL_PREFIX = "data_detail@@@";

    // ==================== 系统字段名 ====================

    /** 主键字段名. */
    public static final String FIELD_ID = "id";

    /** 创建时间字段名. */
    public static final String FIELD_GMT_CREATE = "gmt_create";

    /** 修改时间字段名. */
    public static final String FIELD_GMT_MODIFIED = "gmt_modified";

    /** 删除标记字段名. */
    public static final String FIELD_DELETED = "deleted";

    /** 创建人字段名. */
    public static final String FIELD_CREATED_BY = "created_by";

    /** 修改人字段名. */
    public static final String FIELD_MODIFIED_BY = "modified_by";

    // ==================== 辅助方法 ====================

    /**
     * 判断字段名是否为系统内置字段.
     *
     * @param fieldName 字段名
     * @return 是否为系统字段
     */
    public static boolean isSystemField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        return FIELD_ID.equals(lower)
                || FIELD_GMT_CREATE.equals(lower)
                || FIELD_GMT_MODIFIED.equals(lower)
                || FIELD_DELETED.equals(lower)
                || FIELD_CREATED_BY.equals(lower)
                || FIELD_MODIFIED_BY.equals(lower);
    }

    /**
     * 判断表名是否为 ODS 层表.
     *
     * @param tableName 表名
     * @return 是否为 ODS 层表
     */
    public static boolean isOdsTable(String tableName) {
        return tableName != null && tableName.startsWith(ODS_PREFIX);
    }

    /**
     * 判断表名是否为 ADS 层表.
     *
     * @param tableName 表名
     * @return 是否为 ADS 层表
     */
    public static boolean isAdsTable(String tableName) {
        return tableName != null && (tableName.startsWith(ADS_PREFIX) || tableName.startsWith(ADS_ORIGIN_PREFIX));
    }
}
