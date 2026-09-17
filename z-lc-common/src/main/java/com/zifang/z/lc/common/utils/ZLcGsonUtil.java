package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;

/**
 * JSON 序列化/反序列化工具 — 蒸馏自 ace-platform-core
 * {@code GsonUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>虽然 ace 原类名为 GsonUtil, 实际内部已全部使用 Jackson ObjectMapper.
 * 蒸馏时移除了 ace 对 hutool StrUtil / LoggerUtil / Spring StringUtils 的依赖,
 * 改为纯 JDK + Jackson 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>对象 ↔ JSON 字符串互转</li>
 *   <li>Map ↔ POJO 转换</li>
 *   <li>嵌套 Map / List 的深度比较</li>
 *   <li>JSON 类型判断与相等性比较</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcGsonUtil {

    private ZLcGsonUtil() {
    }

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ==================== 序列化 ====================

    /**
     * 对象 → JSON 字符串.
     */
    public static <T> String objectToJsonStr(T object) {
        if (object == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对象 → JSON 字符串 (别名).
     */
    public static <T> String otj(T object) {
        return objectToJsonStr(object);
    }

    // ==================== 反序列化 ====================

    /**
     * JSON 字符串 → 对象.
     */
    public static <T> T fromJson(String str, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JSON 字符串 → 对象 (空值安全).
     */
    public static <T> T jsonStrToObject(String jsonStr, Class<T> classOfT) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        return fromJson(jsonStr, classOfT);
    }

    /**
     * JSON 字符串 → 对象 (空值时返回空实例).
     */
    public static <T> T jsto(String jsonStr, Class<T> classOfT) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            try {
                return classOfT.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("无法创建空实例: " + classOfT.getName(), e);
            }
        }
        return jsonStrToObject(jsonStr, classOfT);
    }

    /**
     * 安全反序列化 — 解析失败时返回 null.
     */
    public static <T> T safeJsto(String jsonStr, Class<T> classOfT) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            try {
                return classOfT.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, classOfT);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 对象转换 — 先序列化再反序列化为子类.
     */
    public static <T> T changeToSubClass(Object o, Class<T> t) {
        return jsonStrToObject(objectToJsonStr(o), t);
    }

    /**
     * Map → 对象.
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    // ==================== Map 转换 ====================

    /**
     * 对象 → Map.
     */
    public static Map<String, Object> toMap(Object o) {
        return OBJECT_MAPPER.convertValue(o, Map.class);
    }

    /**
     * JSON 字符串 → Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * JSON 字符串 → List&lt;Map&gt;.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> toList(String json) {
        return (List<Map<String, Object>>) jsonStrToObject(json, List.class);
    }

    // ==================== Map 比较 ====================

    /**
     * 深度比较两个 Map 是否相同.
     */
    @SuppressWarnings("unchecked")
    public static boolean isSame(Map<String, Object> m1, Map<String, Object> m2) {
        if (m1 == null && m2 == null) {
            return true;
        }
        if (m1 == null || m2 == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : m1.entrySet()) {
            if (!m2.containsKey(entry.getKey())) {
                return false;
            }
            Object val1 = entry.getValue();
            Object val2 = m2.get(entry.getKey());
            if (val1 instanceof List) {
                if (!(val2 instanceof List)) {
                    return false;
                }
                List<Object> l1 = (List<Object>) val1;
                List<Object> l2 = (List<Object>) val2;
                if (l2.size() != l1.size() || !isSameList(l1, l2)) {
                    return false;
                }
            } else if (val1 instanceof Map) {
                if (!(val2 instanceof Map)) {
                    return false;
                }
                if (!isSame((Map<String, Object>) val1, (Map<String, Object>) val2)) {
                    return false;
                }
            } else {
                if (!isSameValue(val1, val2)) {
                    return false;
                }
            }
        }
        // m1 中不存在但 m2 中存在的非空字段
        for (Map.Entry<String, Object> entry : m2.entrySet()) {
            if (!m1.containsKey(entry.getKey()) && entry.getValue() != null
                    && !"".equals(String.valueOf(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    // ==================== JSON 类型判断 ====================

    /**
     * 判断字符串是否可反序列化为目标类型.
     */
    public static <T> boolean isJson(String str, Class<T> clazz) {
        try {
            OBJECT_MAPPER.readValue(str, clazz);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 JSON (对象或数组).
     */
    public static boolean isTypeJSON(String str) {
        return isTypeJSONObject(str) || isTypeJSONArray(str);
    }

    /**
     * 判断字符串是否为 JSON 数组.
     */
    public static boolean isTypeJSONArray(String str) {
        if (isBlank(str)) {
            return false;
        }
        String trimmed = str.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    /**
     * 判断字符串是否为 JSON 对象.
     */
    public static boolean isTypeJSONObject(String str) {
        if (isBlank(str)) {
            return false;
        }
        String trimmed = str.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    /**
     * 比较两个 JSON 字符串是否语义相等.
     */
    public static boolean areJsonEqual(String jsonStr1, String jsonStr2) {
        try {
            JsonNode json1 = OBJECT_MAPPER.readTree(jsonStr1);
            JsonNode json2 = OBJECT_MAPPER.readTree(jsonStr2);
            return json1.equals(json2);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 内部辅助 ====================

    @SuppressWarnings("unchecked")
    private static boolean isSameList(List<Object> l1, List<Object> l2) {
        List<Object> remain = new ArrayList<>(l2);
        for (Object left : l1) {
            boolean matched = false;
            for (int i = 0; i < remain.size(); i++) {
                if (isSameElement(left, remain.get(i))) {
                    remain.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remain.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static boolean isSameElement(Object left, Object right) {
        if (left instanceof Map && right instanceof Map) {
            return isSame((Map<String, Object>) left, (Map<String, Object>) right);
        }
        return isSameValue(left, right);
    }

    private static boolean isSameValue(Object o1, Object o2) {
        if (o1 instanceof Number || o2 instanceof Number) {
            String s1 = o1 instanceof Number ? String.valueOf(((Number) o1).longValue()) : String.valueOf(o1);
            String s2 = o2 instanceof Number ? String.valueOf(((Number) o2).longValue()) : String.valueOf(o2);
            return s1.equals(s2);
        }
        return String.valueOf(o1).equals(String.valueOf(o2));
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
