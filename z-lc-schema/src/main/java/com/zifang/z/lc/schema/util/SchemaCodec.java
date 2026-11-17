package com.zifang.z.lc.schema.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.schema.model.PageSchema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema JSON 编解码：统一 ObjectMapper 口径，反序列化后自动 normalize。
 */
public final class SchemaCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SchemaCodec() {
    }

    public static String toJson(PageSchema schema) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new SchemaException("序列化 schema 失败", e);
        }
    }

    public static PageSchema fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new PageSchema();
        }
        try {
            return SchemaNormalizer.normalize(MAPPER.readValue(json, PageSchema.class));
        } catch (JsonProcessingException e) {
            throw new SchemaException("反序列化 schema 失败", e);
        }
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** JsonNode → Map（datasets 这类自由结构用它，避免为每种取数器建 POJO） */
    public static Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return new LinkedHashMap<>();
        }
        return MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() { });
    }

    /** JsonNode → 普通 Java 值（String/Number/Boolean/List/Map），用于塞进 props */
    public static Object toJava(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return MAPPER.convertValue(node, Object.class);
    }

    /** 普通 Java 值 → JsonNode */
    public static JsonNode toNode(Object value) {
        return MAPPER.valueToTree(value);
    }

    public static class SchemaException extends RuntimeException {
        public SchemaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
