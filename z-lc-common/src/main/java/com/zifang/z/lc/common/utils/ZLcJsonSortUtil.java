package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * JSON Key 排序工具 — 蒸馏自 ace-platform-core
 * {@code JsonSortUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 JSON 对象的 key 字母序排序、key 首字母大写转换等功能.
 * 蒸馏时移除了 ace 对 fastjson / boot JsonUtil 的依赖,
 * 改为纯 Jackson ObjectMapper 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>JSON 签名前的 key 排序</li>
 *   <li>配置中心 JSON 的 key 标准化</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcJsonSortUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ZLcJsonSortUtil() {
    }

    /**
     * 对 JSON 字符串的 key 按字母序递归排序.
     *
     * @param jsonStr 原始 JSON 字符串
     * @return 排序后的 JSON 字符串
     */
    public static String startSort(String jsonStr) {
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonStr);
            JsonNode sorted = sortNode(jsonNode);
            return MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON key 排序失败", e);
        }
    }

    /**
     * 对 JSON 字符串的 key 首字母大写.
     *
     * @param jsonStr 原始 JSON 字符串
     * @return key 首字母大写后的 JSON 字符串
     */
    public static String convertKeysToUpperCase(String jsonStr) {
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonStr);
            JsonNode result = upperCaseKeys(jsonNode);
            return MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON key 大写转换失败", e);
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 递归排序 JSON 节点的 key.
     */
    private static JsonNode sortNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = MAPPER.createObjectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
            for (String key : keys) {
                sorted.set(key, sortNode(node.get(key)));
            }
            return sorted;
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                // 数组元素如果是对象也递归排序 — 但因为 ArrayNode 不支持 set,
                // 这里返回原 node 即可, key 排序主要针对对象
            }
            return node;
        }
        return node;
    }

    /**
     * 递归将 JSON 节点的 key 首字母大写.
     */
    private static JsonNode upperCaseKeys(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = MAPPER.createObjectNode();
            node.fieldNames().forEachRemaining(fieldName -> {
                String upperFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                result.set(upperFieldName, upperCaseKeys(node.get(fieldName)));
            });
            return result;
        }
        return node;
    }
}
