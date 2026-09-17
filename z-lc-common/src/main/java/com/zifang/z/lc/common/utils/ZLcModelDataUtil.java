package com.zifang.z.lc.common.utils;

import java.util.Map;

/**
 * 模型数据工具 — 蒸馏自 ace-platform-core
 * {@code ModelDataUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台模型数据的常用操作: 主键提取、字段类型转换等.
 *
 * <p>典型场景：
 * <ul>
 *   <li>从表单数据 Map 中提取主键 ID</li>
 *   <li>模型字段值类型安全转换</li>
 *   <li>数据导入导出时的主键处理</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcModelDataUtil {

    private ZLcModelDataUtil() {
    }

    /**
     * 从数据 Map 中提取主键 ID.
     * 支持 Double/Long/String/Integer 类型自动转换.
     *
     * @param data 数据 Map (应包含 "id" 或 "ID" 键)
     * @return 主键 ID (Long); 无法提取时返回 null
     */
    public static Long pickPrimaryKey(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        // 尝试多种可能的主键名
        Object idValue = data.get("id");
        if (idValue == null) {
            idValue = data.get("ID");
        }
        if (idValue == null) {
            idValue = data.get("primaryId");
        }

        return toLong(idValue);
    }

    /**
     * 将值安全转换为 Long.
     *
     * @param value 待转换值
     * @return Long 值; 转换失败时返回 null
     */
    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将值安全转换为 String.
     *
     * @param value 待转换值
     * @return 字符串; null 返回 null
     */
    public static String toString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 将值安全转换为 Integer.
     *
     * @param value 待转换值
     * @return Integer 值; 转换失败时返回 null
     */
    public static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
