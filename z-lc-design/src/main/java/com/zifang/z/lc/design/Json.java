package com.zifang.z.lc.design;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * z-lc 统一 JSON 序列化/反序列化工具.
 *
 * <p>蒸馏自 ace-platform-engine-lc/Json.java（com.c2f.ace.engine.lc.Json），
 * 包路径由 {@code com.c2f.ace.engine.lc} 适配为 {@code com.zifang.z.lc.design}。
 * 行为完全一致：
 * <ul>
 *   <li>统一 {@link ObjectMapper} 实例，注册 JavaTimeModule + 自定义反序列化器</li>
 *   <li>{@link CustomLocalDateTimeDeserializer} 支持字符串（yyyy-MM-dd HH:mm:ss）和数字 timestamp 两种格式</li>
 *   <li>{@link FlexibleStringDeserializer} 支持把对象/数组转为 JSON 字符串（应对"期望 String 但收到对象"的反序列化场景）</li>
 *   <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} = false（兼容 DB 返回多余字段）</li>
 * </ul>
 *
 * <p>典型用法（用于 z-lc-design 内部 SPI）：
 * <pre>{@code
 *   PageTemplate pt = Json.sto(jsonString, PageTemplate.class);
 *   Map<String, Object> map = Json.toMap(entity);
 *   MyEntity entity = Json.mto(map, MyEntity.class);
 * }</pre>
 *
 * <p>z-lc-design 内部其它文件（如 {@code LowCodeModelServiceCollector}）使用本工具
 * 反序列化 classpath*:**&#47;*.json 加载的页面模板元数据。
 *
 * @author zifang
 */
public class Json {

    /**
     * 自定义 LocalDateTime 反序列化器，支持字符串和数字 timestamp
     */
    private static class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        private static final long serialVersionUID = 1L;
        private final DateTimeFormatter dateTimeFormatter;

        public CustomLocalDateTimeDeserializer(DateTimeFormatter dateTimeFormatter) {
            super(LocalDateTime.class);
            this.dateTimeFormatter = dateTimeFormatter;
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 如果是数字类型（timestamp）
            if (p.getCurrentToken().isNumeric()) {
                long timestamp = p.getLongValue();
                // 将 timestamp 转换为 LocalDateTime（使用系统默认时区）
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            }
            // 如果是字符串类型，使用原有的格式化器
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(text, dateTimeFormatter);
            } catch (Exception e) {
                // 如果解析失败，尝试作为 timestamp 字符串处理
                try {
                    long timestamp = Long.parseLong(text);
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                } catch (NumberFormatException ex) {
                    throw new IOException("无法解析 LocalDateTime: " + text, e);
                }
            }
        }
    }

    /**
     * 自定义 String 反序列化器，支持将对象和数组转换为 JSON 字符串
     * 解决期望 String 类型但收到对象时的反序列化问题
     */
    private static class FlexibleStringDeserializer extends StdDeserializer<String> {
        private static final long serialVersionUID = 1L;

        public FlexibleStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken currentToken = p.getCurrentToken();

            // 如果是对象或数组，转换为 JSON 字符串
            if (currentToken == JsonToken.START_OBJECT || currentToken == JsonToken.START_ARRAY) {
                JsonNode node = p.getCodec().readTree(p);
                return node.toString();
            }

            // 如果是字符串，直接返回
            if (currentToken == JsonToken.VALUE_STRING) {
                return p.getText();
            }

            // 如果是 null
            if (currentToken == JsonToken.VALUE_NULL) {
                return null;
            }

            // 其他类型（数字、布尔值等）转换为字符串
            return p.getValueAsString();
        }
    }

    /**
     * z-lc 统一 {@link ObjectMapper} 实例.
     * <p>所有 JSON 序列化/反序列化操作都应走此实例，避免每个模块各自 new ObjectMapper
     * 导致配置不一致（如 {@code FAIL_ON_UNKNOWN_PROPERTIES}）。
     */
    public static ObjectMapper objectMapper = null;

    static {
        objectMapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer(dateTimeFormatter));
        objectMapper.registerModule(javaTimeModule);

        // 注册 String 类型的灵活反序列化器，支持将对象/数组转换为 JSON 字符串
        SimpleModule stringModule = new SimpleModule();
        stringModule.addDeserializer(String.class, new FlexibleStringDeserializer());
        objectMapper.registerModule(stringModule);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 把任意 POJO 转 Map（通常用于 LowCodeModelServiceCollector 返回 JSON 形式元数据）。
     *
     * @param o 源对象
     * @return Map 表示
     */
    public static Map<String, Object> toMap(Object o) {
        return objectMapper.convertValue(o, Map.class);
    }

    /**
     * Map → 泛型对象（反序列化）。
     *
     * @param data Map 数据
     * @param genericType 目标类型
     * @param <T> 泛型
     * @return 转换后的对象
     */
    public static <T> T mto(Map<String, Object> data, Class<T> genericType) {
        return objectMapper.convertValue(data, genericType);
    }

    /**
     * JSON 字符串 → 泛型对象（反序列化）。
     * <p>解析失败时 fallback 到 {@code genericType.newInstance()}（空对象），
     * 保证调用方拿到非空 bean（典型场景：DB 里 viewJson 字段为 null 或解析错误，
     * 不应让整个 Controller 抛 500，而是返回空 PageTemplate 让前端兜底）。
     *
     * @param json JSON 字符串
     * @param genericType 目标类型
     * @param <T> 泛型
     * @return 转换后的对象
     */
    public static <T> T sto(String json, Class<T> genericType) {
        try {
            return objectMapper.readValue(json, genericType);
        } catch (JsonProcessingException e) {
            try {
                return genericType.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}