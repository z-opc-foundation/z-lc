package com.zifang.z.lc.design;

import com.zifang.util.json.JsonUtil;

import java.util.Map;

/**
 * z-lc 统一 JSON 序列化/反序列化工具.
 *
 * <p>蒸馏自 ace-platform-engine-lc/Json.java，
 * 包路径由 适配为 {@code com.zifang.z.lc.design}。
 * 底层换成 z-util 的 {@link JsonUtil}，不再自持 {@code ObjectMapper}：
 * <ul>
 *   <li>忽略多余字段（兼容 DB 返回带额外列）</li>
 *   <li>{@link #sto} 解析失败回落空对象，调用方拿到非空 bean（DB 里 viewJson 为
 *       null 或写坏时不该让整个 Controller 抛 500）</li>
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
     * 把任意 POJO 转 Map（通常用于 LowCodeModelServiceCollector 返回 JSON 形式元数据）。
     *
     * @param o 源对象
     * @return Map 表示
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object o) {
        Object plain = JsonUtil.toPlainValue(JsonUtil.parse(JsonUtil.toJson(o)));
        return plain == null ? null : (Map<String, Object>) plain;
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
        return JsonUtil.fromJson(JsonUtil.toJson(data), genericType);
    }

    /**
     * JSON 字符串 → 泛型对象（反序列化）。
     * <p>解析失败时 fallback 到 {@code genericType} 的无参实例（空对象），
     * 保证调用方拿到非空 bean（典型场景：DB 里 viewJson 字段为 null 或解析错误，
     * 不应让整个 Controller 抛 500，而是返回空 PageTemplate 让前端兜底）。
     *
     * @param json JSON 字符串
     * @param genericType 目标类型
     * @param <T> 泛型
     * @return 转换后的对象
     */
    public static <T> T sto(String json, Class<T> genericType) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        try {
            // 空串 JsonUtil 不抛而是返 null, 走不到下面的回落分支, 得先自己挡一道
            return json.trim().isEmpty() ? newInstance(genericType) : JsonUtil.fromJson(json, genericType);
        } catch (RuntimeException e) {
            return newInstance(genericType);
        }
    }

    private static <T> T newInstance(Class<T> genericType) {
        try {
            return genericType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
